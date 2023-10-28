package com.android.server.pm;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IStopUserCallback;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.StatsManager;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackagePartitions;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.IProgressListener;
import android.os.IUserManager;
import android.os.IUserRestrictionsListener;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.provider.Settings;
import android.security.GateKeeper;
import android.service.gatekeeper.IGateKeeperService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.IndentingPrintWriter;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.StatsEvent;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.app.IAppOpsService;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.BundleUtils;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.SystemService;
import com.android.server.am.HostingRecord;
import com.android.server.am.UserState;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.UserTypeFactory;
import com.android.server.storage.DeviceStorageMonitorInternal;
import com.android.server.utils.Slogf;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class UserManagerService extends IUserManager.Stub {
    private static final int ALLOWED_FLAGS_FOR_CREATE_USERS_PERMISSION = 5932;
    private static final String ATTR_CONVERTED_FROM_PRE_CREATED = "convertedFromPreCreated";
    private static final String ATTR_CREATION_TIME = "created";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_GUEST_TO_REMOVE = "guestToRemove";
    private static final String ATTR_ICON_PATH = "icon";
    private static final String ATTR_ID = "id";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_LAST_LOGGED_IN_FINGERPRINT = "lastLoggedInFingerprint";
    private static final String ATTR_LAST_LOGGED_IN_TIME = "lastLoggedIn";
    private static final String ATTR_MULTIPLE = "m";
    private static final String ATTR_NEXT_SERIAL_NO = "nextSerialNumber";
    private static final String ATTR_PARTIAL = "partial";
    private static final String ATTR_PRE_CREATED = "preCreated";
    private static final String ATTR_PROFILE_BADGE = "profileBadge";
    private static final String ATTR_PROFILE_GROUP_ID = "profileGroupId";
    private static final String ATTR_RESTRICTED_PROFILE_PARENT_ID = "restrictedProfileParentId";
    private static final String ATTR_SEED_ACCOUNT_NAME = "seedAccountName";
    private static final String ATTR_SEED_ACCOUNT_TYPE = "seedAccountType";
    private static final String ATTR_SERIAL_NO = "serialNumber";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_TYPE_BOOLEAN = "b";
    private static final String ATTR_TYPE_BUNDLE = "B";
    private static final String ATTR_TYPE_BUNDLE_ARRAY = "BA";
    private static final String ATTR_TYPE_INTEGER = "i";
    private static final String ATTR_TYPE_STRING = "s";
    private static final String ATTR_TYPE_STRING_ARRAY = "sa";
    private static final String ATTR_USER_TYPE_VERSION = "userTypeConfigVersion";
    private static final String ATTR_USER_VERSION = "version";
    private static final String ATTR_VALUE_TYPE = "type";
    static final boolean DBG = false;
    public static final boolean DBG_ALLOCATION = false;
    private static final boolean DBG_WITH_STACKTRACE = false;
    private static final long EPOCH_PLUS_30_YEARS = 946080000000L;
    private static final String LOG_TAG = "UserManagerService";
    static final int MAX_RECENTLY_REMOVED_IDS_SIZE = 100;
    static final int MAX_USER_ID = 21474;
    static final int MIN_USER_ID = 10;
    private static final boolean RELEASE_DELETED_USER_ID = false;
    private static final String RESTRICTIONS_FILE_PREFIX = "res_";
    private static final String TAG_ACCOUNT = "account";
    private static final String TAG_DEVICE_OWNER_USER_ID = "deviceOwnerUserId";
    private static final String TAG_DEVICE_POLICY_GLOBAL_RESTRICTIONS = "device_policy_global_restrictions";
    private static final String TAG_DEVICE_POLICY_LOCAL_RESTRICTIONS = "device_policy_local_restrictions";
    private static final String TAG_DEVICE_POLICY_RESTRICTIONS = "device_policy_restrictions";
    private static final String TAG_ENTRY = "entry";
    private static final String TAG_GLOBAL_RESTRICTION_OWNER_ID = "globalRestrictionOwnerUserId";
    private static final String TAG_GUEST_RESTRICTIONS = "guestRestrictions";
    private static final String TAG_IGNORE_PREPARE_STORAGE_ERRORS = "ignorePrepareStorageErrors";
    private static final String TAG_LAST_REQUEST_QUIET_MODE_ENABLED_CALL = "lastRequestQuietModeEnabledCall";
    private static final String TAG_NAME = "name";
    private static final String TAG_RESTRICTIONS = "restrictions";
    private static final String TAG_SEED_ACCOUNT_OPTIONS = "seedAccountOptions";
    private static final String TAG_USER = "user";
    private static final String TAG_USERS = "users";
    private static final String TAG_VALUE = "value";
    private static final String TRON_DEMO_CREATED = "users_demo_created";
    private static final String TRON_GUEST_CREATED = "users_guest_created";
    private static final String TRON_USER_CREATED = "users_user_created";
    private static final String USER_LIST_FILENAME = "userlist.xml";
    private static final String USER_PHOTO_FILENAME = "photo.png";
    private static final String USER_PHOTO_FILENAME_TMP = "photo.png.tmp";
    private static final int USER_VERSION = 9;
    static final int WRITE_USER_DELAY = 2000;
    static final int WRITE_USER_MSG = 1;
    private static final String XML_SUFFIX = ".xml";
    private static UserManagerService sInstance;
    private final String ACTION_DISABLE_QUIET_MODE_AFTER_UNLOCK;
    private IAppOpsService mAppOpsService;
    private final Object mAppRestrictionsLock;
    private final RestrictionsSet mAppliedUserRestrictions;
    private final RestrictionsSet mBaseUserRestrictions;
    private final RestrictionsSet mCachedEffectiveUserRestrictions;
    private final BroadcastReceiver mConfigurationChangeReceiver;
    private final Context mContext;
    private int mDeviceOwnerUserId;
    private final RestrictionsSet mDevicePolicyGlobalUserRestrictions;
    private final SparseArray<RestrictionsSet> mDevicePolicyLocalUserRestrictions;
    private DevicePolicyManagerInternal mDevicePolicyManagerInternal;
    private final BroadcastReceiver mDisableQuietModeCallback;
    private boolean mForceEphemeralUsers;
    private final Bundle mGuestRestrictions;
    private final Handler mHandler;
    private boolean mIsDeviceManaged;
    private final SparseBooleanArray mIsUserManaged;
    private final Configuration mLastConfiguration;
    private final LocalService mLocalService;
    private final LockPatternUtils mLockPatternUtils;
    private int mNextSerialNumber;
    private final AtomicReference<String> mOwnerName;
    private final TypedValue mOwnerNameTypedValue;
    private final Object mPackagesLock;
    private final PackageManagerService mPm;
    private PackageManagerInternal mPmInternal;
    private final LinkedList<Integer> mRecentlyRemovedIds;
    private final SparseBooleanArray mRemovingUserIds;
    private final Object mRestrictionsLock;
    private final UserSystemPackageInstaller mSystemPackageInstaller;
    public final AtomicInteger mUser0Allocations;
    private final UserDataPreparer mUserDataPreparer;
    private int[] mUserIds;
    private int[] mUserIdsIncludingPreCreated;
    private final ArrayList<UserManagerInternal.UserLifecycleListener> mUserLifecycleListeners;
    private final File mUserListFile;
    private final ArrayList<UserManagerInternal.UserRestrictionsListener> mUserRestrictionsListeners;
    private final WatchedUserStates mUserStates;
    private int mUserTypeVersion;
    private final ArrayMap<String, UserTypeDetails> mUserTypes;
    private int mUserVersion;
    private final SparseArray<UserData> mUsers;
    private final File mUsersDir;
    private final Object mUsersLock;
    private static final String USER_INFO_DIR = HostingRecord.HOSTING_TYPE_SYSTEM + File.separator + "users";
    private static final IBinder mUserRestriconToken = new Binder();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class UserData {
        String account;
        UserInfo info;
        private boolean mIgnorePrepareStorageErrors;
        private long mLastRequestQuietModeEnabledMillis;
        boolean persistSeedData;
        String seedAccountName;
        PersistableBundle seedAccountOptions;
        String seedAccountType;
        long startRealtime;
        long unlockRealtime;

        UserData() {
        }

        void setLastRequestQuietModeEnabledMillis(long millis) {
            this.mLastRequestQuietModeEnabledMillis = millis;
        }

        long getLastRequestQuietModeEnabledMillis() {
            return this.mLastRequestQuietModeEnabledMillis;
        }

        boolean getIgnorePrepareStorageErrors() {
            return this.mIgnorePrepareStorageErrors;
        }

        void setIgnorePrepareStorageErrors() {
            if (Build.VERSION.DEVICE_INITIAL_SDK_INT < 33) {
                this.mIgnorePrepareStorageErrors = true;
            } else {
                Slog.w(UserManagerService.LOG_TAG, "Not setting mIgnorePrepareStorageErrors to true since this is a new device");
            }
        }

        void clearSeedAccountData() {
            this.seedAccountName = null;
            this.seedAccountType = null;
            this.seedAccountOptions = null;
            this.persistSeedData = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.pm.UserManagerService$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!"com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK".equals(intent.getAction())) {
                return;
            }
            final IntentSender target = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
            final int userId = intent.getIntExtra("android.intent.extra.USER_ID", -10000);
            BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.pm.UserManagerService$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UserManagerService.AnonymousClass1.this.m5746lambda$onReceive$0$comandroidserverpmUserManagerService$1(userId, target);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReceive$0$com-android-server-pm-UserManagerService$1  reason: not valid java name */
        public /* synthetic */ void m5746lambda$onReceive$0$comandroidserverpmUserManagerService$1(int userId, IntentSender target) {
            UserManagerService.this.setQuietModeEnabled(userId, false, target, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DisableQuietModeUserUnlockedCallback extends IProgressListener.Stub {
        private final IntentSender mTarget;

        public DisableQuietModeUserUnlockedCallback(IntentSender target) {
            Objects.requireNonNull(target);
            this.mTarget = target;
        }

        public void onStarted(int id, Bundle extras) {
        }

        public void onProgress(int id, int progress, Bundle extras) {
        }

        public void onFinished(int id, Bundle extras) {
            UserManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.UserManagerService$DisableQuietModeUserUnlockedCallback$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UserManagerService.DisableQuietModeUserUnlockedCallback.this.m5747x2c98b1f9();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onFinished$0$com-android-server-pm-UserManagerService$DisableQuietModeUserUnlockedCallback  reason: not valid java name */
        public /* synthetic */ void m5747x2c98b1f9() {
            try {
                UserManagerService.this.mContext.startIntentSender(this.mTarget, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Slog.e(UserManagerService.LOG_TAG, "Failed to start the target in the callback", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class WatchedUserStates {
        final SparseIntArray states = new SparseIntArray();

        public WatchedUserStates() {
            invalidateIsUserUnlockedCache();
        }

        public int get(int userId) {
            return this.states.get(userId);
        }

        public int get(int userId, int fallback) {
            return this.states.indexOfKey(userId) >= 0 ? this.states.get(userId) : fallback;
        }

        public void put(int userId, int state) {
            this.states.put(userId, state);
            invalidateIsUserUnlockedCache();
        }

        public void delete(int userId) {
            this.states.delete(userId);
            invalidateIsUserUnlockedCache();
        }

        public boolean has(int userId) {
            return this.states.get(userId, -10000) != -10000;
        }

        public String toString() {
            return this.states.toString();
        }

        private void invalidateIsUserUnlockedCache() {
            UserManager.invalidateIsUserUnlockedCache();
        }
    }

    public static UserManagerService getInstance() {
        UserManagerService userManagerService;
        synchronized (UserManagerService.class) {
            userManagerService = sInstance;
        }
        return userManagerService;
    }

    /* loaded from: classes2.dex */
    public static class LifeCycle extends SystemService {
        private UserManagerService mUms;

        public LifeCycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.pm.UserManagerService$LifeCycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.pm.UserManagerService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? userManagerService = UserManagerService.getInstance();
            this.mUms = userManagerService;
            publishBinderService(UserManagerService.TAG_USER, userManagerService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mUms.cleanupPartialUsers();
                if (this.mUms.mPm.isDeviceUpgrading()) {
                    this.mUms.cleanupPreCreatedUsers();
                }
                this.mUms.registerStatsCallbacks();
            }
        }

        @Override // com.android.server.SystemService
        public void onUserStarting(SystemService.TargetUser targetUser) {
            synchronized (this.mUms.mUsersLock) {
                UserData user = this.mUms.getUserDataLU(targetUser.getUserIdentifier());
                if (user != null) {
                    user.startRealtime = SystemClock.elapsedRealtime();
                }
            }
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser targetUser) {
            synchronized (this.mUms.mUsersLock) {
                UserData user = this.mUms.getUserDataLU(targetUser.getUserIdentifier());
                if (user != null) {
                    user.unlockRealtime = SystemClock.elapsedRealtime();
                }
            }
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser targetUser) {
            synchronized (this.mUms.mUsersLock) {
                UserData user = this.mUms.getUserDataLU(targetUser.getUserIdentifier());
                if (user != null) {
                    user.startRealtime = 0L;
                    user.unlockRealtime = 0L;
                }
            }
        }
    }

    UserManagerService(Context context) {
        this(context, null, null, new Object(), context.getCacheDir());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserManagerService(Context context, PackageManagerService pm, UserDataPreparer userDataPreparer, Object packagesLock) {
        this(context, pm, userDataPreparer, packagesLock, Environment.getDataDirectory());
    }

    private UserManagerService(Context context, PackageManagerService pm, UserDataPreparer userDataPreparer, Object packagesLock, File dataDir) {
        this.mUsersLock = LockGuard.installNewLock(2);
        this.mRestrictionsLock = new Object();
        this.mAppRestrictionsLock = new Object();
        this.mUsers = new SparseArray<>();
        this.mBaseUserRestrictions = new RestrictionsSet();
        this.mCachedEffectiveUserRestrictions = new RestrictionsSet();
        this.mAppliedUserRestrictions = new RestrictionsSet();
        this.mDevicePolicyGlobalUserRestrictions = new RestrictionsSet();
        this.mDeviceOwnerUserId = -10000;
        this.mDevicePolicyLocalUserRestrictions = new SparseArray<>();
        this.mGuestRestrictions = new Bundle();
        this.mRemovingUserIds = new SparseBooleanArray();
        this.mRecentlyRemovedIds = new LinkedList<>();
        this.mUserVersion = 0;
        this.mUserTypeVersion = 0;
        this.mIsUserManaged = new SparseBooleanArray();
        this.mUserRestrictionsListeners = new ArrayList<>();
        this.mUserLifecycleListeners = new ArrayList<>();
        this.ACTION_DISABLE_QUIET_MODE_AFTER_UNLOCK = "com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK";
        this.mDisableQuietModeCallback = new AnonymousClass1();
        this.mOwnerName = new AtomicReference<>();
        this.mOwnerNameTypedValue = new TypedValue();
        this.mLastConfiguration = new Configuration();
        this.mConfigurationChangeReceiver = new BroadcastReceiver() { // from class: com.android.server.pm.UserManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (!"android.intent.action.CONFIGURATION_CHANGED".equals(intent.getAction())) {
                    return;
                }
                UserManagerService.this.invalidateOwnerNameIfNecessary(context2.getResources(), false);
            }
        };
        WatchedUserStates watchedUserStates = new WatchedUserStates();
        this.mUserStates = watchedUserStates;
        this.mContext = context;
        this.mPm = pm;
        this.mPackagesLock = packagesLock;
        this.mHandler = new MainHandler();
        this.mUserDataPreparer = userDataPreparer;
        ArrayMap<String, UserTypeDetails> userTypes = UserTypeFactory.getUserTypes();
        this.mUserTypes = userTypes;
        invalidateOwnerNameIfNecessary(context.getResources(), true);
        synchronized (packagesLock) {
            File file = new File(dataDir, USER_INFO_DIR);
            this.mUsersDir = file;
            file.mkdirs();
            File userZeroDir = new File(file, String.valueOf(0));
            userZeroDir.mkdirs();
            FileUtils.setPermissions(file.toString(), 509, -1, -1);
            this.mUserListFile = new File(file, USER_LIST_FILENAME);
            initDefaultGuestRestrictions();
            readUserListLP();
            sInstance = this;
        }
        this.mSystemPackageInstaller = new UserSystemPackageInstaller(this, userTypes);
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        LocalServices.addService(UserManagerInternal.class, localService);
        this.mLockPatternUtils = new LockPatternUtils(context);
        watchedUserStates.put(0, 0);
        this.mUser0Allocations = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemReady() {
        this.mAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        synchronized (this.mRestrictionsLock) {
            applyUserRestrictionsLR(0);
        }
        this.mContext.registerReceiver(this.mDisableQuietModeCallback, new IntentFilter("com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK"), null, this.mHandler);
        this.mContext.registerReceiver(this.mConfigurationChangeReceiver, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"), null, this.mHandler);
        markEphemeralUsersForRemoval();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserManagerInternal getInternalForInjectorOnly() {
        return this.mLocalService;
    }

    private void markEphemeralUsersForRemoval() {
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if (ui.isEphemeral() && !ui.preCreated && ui.id != 0) {
                    addRemovingUserIdLocked(ui.id);
                    ui.partial = true;
                    ui.flags |= 64;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupPartialUsers() {
        ArrayList<UserInfo> partials = new ArrayList<>();
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if ((ui.partial || ui.guestToRemove) && ui.id != 0) {
                    partials.add(ui);
                    if (!this.mRemovingUserIds.get(ui.id)) {
                        addRemovingUserIdLocked(ui.id);
                    }
                    ui.partial = true;
                }
            }
        }
        int partialsSize = partials.size();
        for (int i2 = 0; i2 < partialsSize; i2++) {
            UserInfo ui2 = partials.get(i2);
            Slog.w(LOG_TAG, "Removing partially created user " + ui2.id + " (name=" + ui2.name + ")");
            removeUserState(ui2.id);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupPreCreatedUsers() {
        ArrayList<UserInfo> preCreatedUsers;
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            preCreatedUsers = new ArrayList<>(userSize);
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if (ui.preCreated) {
                    preCreatedUsers.add(ui);
                    addRemovingUserIdLocked(ui.id);
                    ui.flags |= 64;
                    ui.partial = true;
                }
            }
        }
        int preCreatedSize = preCreatedUsers.size();
        for (int i2 = 0; i2 < preCreatedSize; i2++) {
            UserInfo ui2 = preCreatedUsers.get(i2);
            Slog.i(LOG_TAG, "Removing pre-created user " + ui2.id);
            removeUserState(ui2.id);
        }
    }

    public String getUserAccount(int userId) {
        String str;
        checkManageUserAndAcrossUsersFullPermission("get user account");
        synchronized (this.mUsersLock) {
            str = this.mUsers.get(userId).account;
        }
        return str;
    }

    public void setUserAccount(int userId, String accountName) {
        checkManageUserAndAcrossUsersFullPermission("set user account");
        UserData userToUpdate = null;
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = this.mUsers.get(userId);
                if (userData == null) {
                    Slog.e(LOG_TAG, "User not found for setting user account: u" + userId);
                    return;
                }
                String currentAccount = userData.account;
                if (!Objects.equals(currentAccount, accountName)) {
                    userData.account = accountName;
                    userToUpdate = userData;
                }
                if (userToUpdate != null) {
                    writeUserLP(userToUpdate);
                }
            }
        }
    }

    public UserInfo getPrimaryUser() {
        checkManageUsersPermission("query users");
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if (ui.isPrimary() && !this.mRemovingUserIds.get(ui.id)) {
                    return ui;
                }
            }
            return null;
        }
    }

    public List<UserInfo> getUsers(boolean excludeDying) {
        return getUsers(true, excludeDying, true);
    }

    public List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) {
        checkCreateUsersPermission("query users");
        return getUsersInternal(excludePartial, excludeDying, excludePreCreated);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<UserInfo> getUsersInternal(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) {
        ArrayList<UserInfo> users;
        synchronized (this.mUsersLock) {
            users = new ArrayList<>(this.mUsers.size());
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if ((!excludePartial || !ui.partial) && ((!excludeDying || !this.mRemovingUserIds.get(ui.id)) && (!excludePreCreated || !ui.preCreated))) {
                    users.add(userWithName(ui));
                }
            }
        }
        return users;
    }

    public List<UserInfo> getProfiles(int userId, boolean enabledOnly) {
        boolean returnFullInfo;
        List<UserInfo> profilesLU;
        if (userId != UserHandle.getCallingUserId()) {
            checkQueryOrCreateUsersPermission("getting profiles related to user " + userId);
            returnFullInfo = true;
        } else {
            returnFullInfo = hasCreateUsersPermission();
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUsersLock) {
                profilesLU = getProfilesLU(userId, null, enabledOnly, returnFullInfo);
            }
            return profilesLU;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int[] getProfileIds(int userId, boolean enabledOnly) {
        return getProfileIds(userId, null, enabledOnly);
    }

    public int[] getProfileIds(int userId, String userType, boolean enabledOnly) {
        int[] array;
        if (userId != UserHandle.getCallingUserId()) {
            checkQueryOrCreateUsersPermission("getting profiles related to user " + userId);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUsersLock) {
                array = getProfileIdsLU(userId, userType, enabledOnly).toArray();
            }
            return array;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private List<UserInfo> getProfilesLU(int userId, String userType, boolean enabledOnly, boolean fullInfo) {
        UserInfo userInfo;
        IntArray profileIds = getProfileIdsLU(userId, userType, enabledOnly);
        ArrayList<UserInfo> users = new ArrayList<>(profileIds.size());
        for (int i = 0; i < profileIds.size(); i++) {
            int profileId = profileIds.get(i);
            UserInfo userInfo2 = this.mUsers.get(profileId).info;
            if (!fullInfo) {
                userInfo = new UserInfo(userInfo2);
                userInfo.name = null;
                userInfo.iconPath = null;
            } else {
                userInfo = userWithName(userInfo2);
            }
            users.add(userInfo);
        }
        return users;
    }

    private IntArray getProfileIdsLU(int userId, String userType, boolean enabledOnly) {
        UserInfo user = getUserInfoLU(userId);
        IntArray result = new IntArray(this.mUsers.size());
        if (user == null) {
            return result;
        }
        int userSize = this.mUsers.size();
        for (int i = 0; i < userSize; i++) {
            UserInfo profile = this.mUsers.valueAt(i).info;
            if (isProfileOf(user, profile) && ((!enabledOnly || profile.isEnabled()) && !this.mRemovingUserIds.get(profile.id) && !profile.partial && (userType == null || userType.equals(profile.userType)))) {
                result.add(profile.id);
            }
        }
        return result;
    }

    public int getCredentialOwnerProfile(int userId) {
        checkManageUsersPermission("get the credential owner");
        if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId)) {
            synchronized (this.mUsersLock) {
                UserInfo profileParent = getProfileParentLU(userId);
                if (profileParent != null) {
                    return profileParent.id;
                }
            }
        }
        return userId;
    }

    public boolean isSameProfileGroup(int userId, int otherUserId) {
        if (userId == otherUserId) {
            return true;
        }
        checkQueryUsersPermission("check if in the same profile group");
        return isSameProfileGroupNoChecks(userId, otherUserId);
    }

    private boolean isSameProfileGroupNoChecks(int userId, int otherUserId) {
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            if (userInfo != null && userInfo.profileGroupId != -10000) {
                UserInfo otherUserInfo = getUserInfoLU(otherUserId);
                if (otherUserInfo != null && otherUserInfo.profileGroupId != -10000) {
                    return userInfo.profileGroupId == otherUserInfo.profileGroupId;
                }
                return false;
            }
            return false;
        }
    }

    public UserInfo getProfileParent(int userId) {
        UserInfo profileParentLU;
        if (!hasManageUsersOrPermission("android.permission.INTERACT_ACROSS_USERS")) {
            throw new SecurityException("You need MANAGE_USERS or INTERACT_ACROSS_USERS permission to get the profile parent");
        }
        synchronized (this.mUsersLock) {
            profileParentLU = getProfileParentLU(userId);
        }
        return profileParentLU;
    }

    public int getProfileParentId(int userId) {
        checkManageUsersPermission("get the profile parent");
        return this.mLocalService.getProfileParentId(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserInfo getProfileParentLU(int userId) {
        int parentUserId;
        UserInfo profile = getUserInfoLU(userId);
        if (profile == null || (parentUserId = profile.profileGroupId) == userId || parentUserId == -10000) {
            return null;
        }
        return getUserInfoLU(parentUserId);
    }

    private static boolean isProfileOf(UserInfo user, UserInfo profile) {
        return user.id == profile.id || (user.profileGroupId != -10000 && user.profileGroupId == profile.profileGroupId);
    }

    private void broadcastProfileAvailabilityChanges(UserHandle profileHandle, UserHandle parentHandle, boolean inQuietMode) {
        Intent intent = new Intent();
        if (inQuietMode) {
            intent.setAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        } else {
            intent.setAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        }
        intent.putExtra("android.intent.extra.QUIET_MODE", inQuietMode);
        intent.putExtra("android.intent.extra.USER", profileHandle);
        intent.putExtra("android.intent.extra.user_handle", profileHandle.getIdentifier());
        getDevicePolicyManagerInternal().broadcastIntentToManifestReceivers(intent, parentHandle, true);
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, parentHandle);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1158=5] */
    public boolean requestQuietModeEnabled(String callingPackage, boolean enableQuietMode, int userId, IntentSender target, int flags) {
        Objects.requireNonNull(callingPackage);
        if (!enableQuietMode || target == null) {
            boolean dontAskCredential = (flags & 2) != 0;
            boolean onlyIfCredentialNotRequired = (flags & 1) != 0;
            if (dontAskCredential && onlyIfCredentialNotRequired) {
                throw new IllegalArgumentException("invalid flags: " + flags);
            }
            ensureCanModifyQuietMode(callingPackage, Binder.getCallingUid(), userId, target != null, dontAskCredential);
            if (onlyIfCredentialNotRequired && callingPackage.equals(getPackageManagerInternal().getSystemUiServiceComponent().getPackageName())) {
                throw new SecurityException("SystemUI is not allowed to set QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED");
            }
            long identity = Binder.clearCallingIdentity();
            try {
                if (enableQuietMode) {
                    setQuietModeEnabled(userId, true, target, callingPackage);
                    return true;
                }
                if (this.mLockPatternUtils.isManagedProfileWithUnifiedChallenge(userId)) {
                    KeyguardManager km = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
                    if (!km.isDeviceLocked(this.mLocalService.getProfileParentId(userId)) || onlyIfCredentialNotRequired) {
                        this.mLockPatternUtils.tryUnlockWithCachedUnifiedChallenge(userId);
                    }
                }
                boolean needToShowConfirmCredential = (dontAskCredential || !this.mLockPatternUtils.isSecure(userId) || StorageManager.isUserKeyUnlocked(userId)) ? false : true;
                if (!needToShowConfirmCredential) {
                    setQuietModeEnabled(userId, false, target, callingPackage);
                    return true;
                } else if (onlyIfCredentialNotRequired) {
                    return false;
                } else {
                    showConfirmCredentialToDisableQuietMode(userId, target);
                    return false;
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        throw new IllegalArgumentException("target should only be specified when we are disabling quiet mode.");
    }

    private void ensureCanModifyQuietMode(String callingPackage, int callingUid, int targetUserId, boolean startIntent, boolean dontAskCredential) {
        verifyCallingPackage(callingPackage, callingUid);
        if (hasManageUsersPermission()) {
            return;
        }
        if (startIntent) {
            throw new SecurityException("MANAGE_USERS permission is required to start intent after disabling quiet mode.");
        }
        if (dontAskCredential) {
            throw new SecurityException("MANAGE_USERS permission is required to disable quiet mode without credentials.");
        }
        if (!isSameProfileGroupNoChecks(UserHandle.getUserId(callingUid), targetUserId)) {
            throw new SecurityException("MANAGE_USERS permission is required to modify quiet mode for a different profile group.");
        }
        boolean hasModifyQuietModePermission = hasPermissionGranted("android.permission.MODIFY_QUIET_MODE", callingUid);
        if (hasModifyQuietModePermission) {
            return;
        }
        ShortcutServiceInternal shortcutInternal = (ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class);
        if (shortcutInternal != null) {
            boolean isForegroundLauncher = shortcutInternal.isForegroundDefaultLauncher(callingPackage, callingUid);
            if (isForegroundLauncher) {
                return;
            }
        }
        throw new SecurityException("Can't modify quiet mode, caller is neither foreground default launcher nor has MANAGE_USERS/MODIFY_QUIET_MODE permission");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setQuietModeEnabled(int userId, boolean enableQuietMode, IntentSender target, String callingPackage) {
        synchronized (this.mUsersLock) {
            UserInfo profile = getUserInfoLU(userId);
            UserInfo parent = getProfileParentLU(userId);
            if (profile == null || !profile.isManagedProfile()) {
                throw new IllegalArgumentException("User " + userId + " is not a profile");
            }
            if (profile.isQuietModeEnabled() == enableQuietMode) {
                Slog.i(LOG_TAG, "Quiet mode is already " + enableQuietMode);
                return;
            }
            profile.flags ^= 128;
            UserData profileUserData = getUserDataLU(profile.id);
            synchronized (this.mPackagesLock) {
                writeUserLP(profileUserData);
            }
            DisableQuietModeUserUnlockedCallback disableQuietModeUserUnlockedCallback = null;
            try {
                if (enableQuietMode) {
                    ActivityManager.getService().stopUser(userId, true, (IStopUserCallback) null);
                    ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).killForegroundAppsForUser(userId);
                } else {
                    if (target != null) {
                        disableQuietModeUserUnlockedCallback = new DisableQuietModeUserUnlockedCallback(target);
                    }
                    ActivityManager.getService().startUserInBackgroundWithListener(userId, disableQuietModeUserUnlockedCallback);
                }
                logQuietModeEnabled(userId, enableQuietMode, callingPackage);
            } catch (RemoteException e) {
                e.rethrowAsRuntimeException();
            }
            broadcastProfileAvailabilityChanges(profile.getUserHandle(), parent.getUserHandle(), enableQuietMode);
        }
    }

    private void logQuietModeEnabled(int userId, boolean enableQuietMode, String callingPackage) {
        UserData userData;
        long period;
        Slogf.i(LOG_TAG, "requestQuietModeEnabled called by package %s, with enableQuietMode %b.", callingPackage, Boolean.valueOf(enableQuietMode));
        synchronized (this.mUsersLock) {
            userData = getUserDataLU(userId);
        }
        if (userData == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (userData.getLastRequestQuietModeEnabledMillis() != 0) {
            period = now - userData.getLastRequestQuietModeEnabledMillis();
        } else {
            period = now - userData.info.creationTime;
        }
        DevicePolicyEventLogger.createEvent(55).setStrings(new String[]{callingPackage}).setBoolean(enableQuietMode).setTimePeriod(period).write();
        userData.setLastRequestQuietModeEnabledMillis(now);
    }

    public boolean isQuietModeEnabled(int userId) {
        UserInfo info;
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                info = getUserInfoLU(userId);
            }
            if (info != null && info.isManagedProfile()) {
                return info.isQuietModeEnabled();
            }
            return false;
        }
    }

    private void showConfirmCredentialToDisableQuietMode(int userId, IntentSender target) {
        KeyguardManager km = (KeyguardManager) this.mContext.getSystemService("keyguard");
        Intent unlockIntent = km.createConfirmDeviceCredentialIntent(null, null, userId);
        if (unlockIntent == null) {
            return;
        }
        Intent callBackIntent = new Intent("com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK");
        if (target != null) {
            callBackIntent.putExtra("android.intent.extra.INTENT", target);
        }
        callBackIntent.putExtra("android.intent.extra.USER_ID", userId);
        callBackIntent.setPackage(this.mContext.getPackageName());
        callBackIntent.addFlags(268435456);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, callBackIntent, 1409286144);
        unlockIntent.putExtra("android.intent.extra.INTENT", pendingIntent.getIntentSender());
        unlockIntent.setFlags(276824064);
        this.mContext.startActivity(unlockIntent);
    }

    public void setUserEnabled(int userId) {
        UserInfo info;
        checkManageUsersPermission("enable user");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                info = getUserInfoLU(userId);
            }
            if (info != null && !info.isEnabled()) {
                info.flags ^= 64;
                writeUserLP(getUserDataLU(info.id));
            }
        }
    }

    public void setUserAdmin(int userId) {
        UserInfo info;
        checkManageUserAndAcrossUsersFullPermission("set user admin");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                info = getUserInfoLU(userId);
            }
            if (info != null && !info.isAdmin()) {
                info.flags ^= 2;
                writeUserLP(getUserDataLU(info.id));
            }
        }
    }

    public void evictCredentialEncryptionKey(int userId) {
        checkManageUsersPermission("evict CE key");
        IActivityManager am = ActivityManagerNative.getDefault();
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                am.restartUserInBackground(userId);
            } catch (RemoteException re) {
                throw re.rethrowAsRuntimeException();
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean isUserOfType(int userId, String userType) {
        checkQueryOrCreateUsersPermission("check user type");
        return userType != null && userType.equals(getUserTypeNoChecks(userId));
    }

    private String getUserTypeNoChecks(int userId) {
        String str;
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            str = userInfo != null ? userInfo.userType : null;
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserTypeDetails getUserTypeDetailsNoChecks(int userId) {
        String typeStr = getUserTypeNoChecks(userId);
        if (typeStr != null) {
            return this.mUserTypes.get(typeStr);
        }
        return null;
    }

    private UserTypeDetails getUserTypeDetails(UserInfo userInfo) {
        String typeStr = userInfo != null ? userInfo.userType : null;
        if (typeStr != null) {
            return this.mUserTypes.get(typeStr);
        }
        return null;
    }

    public UserInfo getUserInfo(int userId) {
        UserInfo userWithName;
        checkQueryOrCreateUsersPermission("query user");
        synchronized (this.mUsersLock) {
            userWithName = userWithName(getUserInfoLU(userId));
        }
        return userWithName;
    }

    private UserInfo userWithName(UserInfo orig) {
        if (orig != null && orig.name == null) {
            String name = null;
            if (orig.id == 0) {
                name = getOwnerName();
            } else if (orig.isGuest()) {
                name = getGuestName();
            }
            if (name != null) {
                UserInfo withName = new UserInfo(orig);
                withName.name = name;
                return withName;
            }
        }
        return orig;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUserTypeSubtypeOfFull(String userType) {
        UserTypeDetails userTypeDetails = this.mUserTypes.get(userType);
        return userTypeDetails != null && userTypeDetails.isFull();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUserTypeSubtypeOfProfile(String userType) {
        UserTypeDetails userTypeDetails = this.mUserTypes.get(userType);
        return userTypeDetails != null && userTypeDetails.isProfile();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUserTypeSubtypeOfSystem(String userType) {
        UserTypeDetails userTypeDetails = this.mUserTypes.get(userType);
        return userTypeDetails != null && userTypeDetails.isSystem();
    }

    public boolean hasBadge(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "hasBadge");
        UserTypeDetails userTypeDetails = getUserTypeDetailsNoChecks(userId);
        return userTypeDetails != null && userTypeDetails.hasBadge();
    }

    public int getUserBadgeLabelResId(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "getUserBadgeLabelResId");
        UserInfo userInfo = getUserInfoNoChecks(userId);
        UserTypeDetails userTypeDetails = getUserTypeDetails(userInfo);
        if (userInfo == null || userTypeDetails == null || !userTypeDetails.hasBadge()) {
            Slog.e(LOG_TAG, "Requested badge label for non-badged user " + userId);
            return 0;
        }
        int badgeIndex = userInfo.profileBadge;
        return userTypeDetails.getBadgeLabel(badgeIndex);
    }

    public int getUserBadgeColorResId(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "getUserBadgeColorResId");
        UserInfo userInfo = getUserInfoNoChecks(userId);
        UserTypeDetails userTypeDetails = getUserTypeDetails(userInfo);
        if (userInfo == null || userTypeDetails == null || !userTypeDetails.hasBadge()) {
            Slog.e(LOG_TAG, "Requested badge dark color for non-badged user " + userId);
            return 0;
        }
        return userTypeDetails.getBadgeColor(userInfo.profileBadge);
    }

    public int getUserBadgeDarkColorResId(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "getUserBadgeDarkColorResId");
        UserInfo userInfo = getUserInfoNoChecks(userId);
        UserTypeDetails userTypeDetails = getUserTypeDetails(userInfo);
        if (userInfo == null || userTypeDetails == null || !userTypeDetails.hasBadge()) {
            Slog.e(LOG_TAG, "Requested badge color for non-badged user " + userId);
            return 0;
        }
        return userTypeDetails.getDarkThemeBadgeColor(userInfo.profileBadge);
    }

    public int getUserIconBadgeResId(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "getUserIconBadgeResId");
        UserTypeDetails userTypeDetails = getUserTypeDetailsNoChecks(userId);
        if (userTypeDetails == null || !userTypeDetails.hasBadge()) {
            Slog.e(LOG_TAG, "Requested icon badge for non-badged user " + userId);
            return 0;
        }
        return userTypeDetails.getIconBadge();
    }

    public int getUserBadgeResId(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "getUserBadgeResId");
        UserTypeDetails userTypeDetails = getUserTypeDetailsNoChecks(userId);
        if (userTypeDetails == null || !userTypeDetails.hasBadge()) {
            Slog.e(LOG_TAG, "Requested badge for non-badged user " + userId);
            return 0;
        }
        return userTypeDetails.getBadgePlain();
    }

    public int getUserBadgeNoBackgroundResId(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "getUserBadgeNoBackgroundResId");
        UserTypeDetails userTypeDetails = getUserTypeDetailsNoChecks(userId);
        if (userTypeDetails == null || !userTypeDetails.hasBadge()) {
            Slog.e(LOG_TAG, "Requested badge (no background) for non-badged user " + userId);
            return 0;
        }
        return userTypeDetails.getBadgeNoBackground();
    }

    public boolean isProfile(int userId) {
        boolean z;
        checkQueryOrInteractPermissionIfCallerInOtherProfileGroup(userId, "isProfile");
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            z = userInfo != null && userInfo.isProfile();
        }
        return z;
    }

    public boolean isDualProfile(int userId) {
        boolean z;
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            z = userInfo != null && userInfo.isDualProfile();
        }
        return z;
    }

    public String getProfileType(int userId) {
        checkQueryOrInteractPermissionIfCallerInOtherProfileGroup(userId, "getProfileType");
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            if (userInfo != null) {
                return userInfo.isProfile() ? userInfo.userType : "";
            }
            return null;
        }
    }

    public boolean isMediaSharedWithParent(int userId) {
        boolean z;
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "isMediaSharedWithParent");
        synchronized (this.mUsersLock) {
            UserTypeDetails userTypeDetails = getUserTypeDetailsNoChecks(userId);
            z = false;
            if (userTypeDetails != null && userTypeDetails.isProfile() && userTypeDetails.isMediaSharedWithParent()) {
                z = true;
            }
        }
        return z;
    }

    public boolean isCredentialSharableWithParent(int userId) {
        boolean z;
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "isCredentialSharableWithParent");
        synchronized (this.mUsersLock) {
            UserTypeDetails userTypeDetails = getUserTypeDetailsNoChecks(userId);
            z = userTypeDetails != null && userTypeDetails.isProfile() && userTypeDetails.isCredentialSharableWithParent();
        }
        return z;
    }

    public boolean isUserUnlockingOrUnlocked(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "isUserUnlockingOrUnlocked");
        return this.mLocalService.isUserUnlockingOrUnlocked(userId);
    }

    public boolean isUserUnlocked(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "isUserUnlocked");
        return this.mLocalService.isUserUnlocked(userId);
    }

    public boolean isUserRunning(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "isUserRunning");
        return this.mLocalService.isUserRunning(userId);
    }

    public boolean isUserForeground(int userId) {
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId != userId && !hasManageUsersOrPermission("android.permission.INTERACT_ACROSS_USERS")) {
            throw new SecurityException("Caller from user " + callingUserId + " needs MANAGE_USERS or INTERACT_ACROSS_USERS permission to check if another user (" + userId + ") is running in the foreground");
        }
        int currentUser = ((Integer) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.UserManagerService$$ExternalSyntheticLambda0
            public final Object getOrThrow() {
                Integer valueOf;
                valueOf = Integer.valueOf(ActivityManager.getCurrentUser());
                return valueOf;
            }
        })).intValue();
        return currentUser == userId;
    }

    public String getUserName() {
        int callingUid = Binder.getCallingUid();
        if (!hasQueryOrCreateUsersPermission() && !hasPermissionGranted("android.permission.GET_ACCOUNTS_PRIVILEGED", callingUid)) {
            throw new SecurityException("You need MANAGE_USERS, CREATE_USERS, QUERY_USERS, or GET_ACCOUNTS_PRIVILEGED permissions to: get user name");
        }
        int userId = UserHandle.getUserId(callingUid);
        synchronized (this.mUsersLock) {
            UserInfo userInfo = userWithName(getUserInfoLU(userId));
            if (userInfo != null && userInfo.name != null) {
                return userInfo.name;
            }
            return "";
        }
    }

    public long getUserStartRealtime() {
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mUsersLock) {
            UserData user = getUserDataLU(userId);
            if (user != null) {
                return user.startRealtime;
            }
            return 0L;
        }
    }

    public long getUserUnlockRealtime() {
        synchronized (this.mUsersLock) {
            UserData user = getUserDataLU(UserHandle.getUserId(Binder.getCallingUid()));
            if (user != null) {
                return user.unlockRealtime;
            }
            return 0L;
        }
    }

    private void checkManageOrInteractPermissionIfCallerInOtherProfileGroup(int userId, String name) {
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId == userId || isSameProfileGroupNoChecks(callingUserId, userId) || hasManageUsersPermission() || hasPermissionGranted("android.permission.INTERACT_ACROSS_USERS", Binder.getCallingUid())) {
            return;
        }
        throw new SecurityException("You need INTERACT_ACROSS_USERS or MANAGE_USERS permission to: check " + name);
    }

    private void checkQueryOrInteractPermissionIfCallerInOtherProfileGroup(int userId, String name) {
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId == userId || isSameProfileGroupNoChecks(callingUserId, userId) || hasQueryUsersPermission() || hasPermissionGranted("android.permission.INTERACT_ACROSS_USERS", Binder.getCallingUid())) {
            return;
        }
        throw new SecurityException("You need INTERACT_ACROSS_USERS, MANAGE_USERS, or QUERY_USERS permission to: check " + name);
    }

    public boolean isDemoUser(int userId) {
        boolean z;
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId != userId && !hasManageUsersPermission()) {
            throw new SecurityException("You need MANAGE_USERS permission to query if u=" + userId + " is a demo user");
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            z = userInfo != null && userInfo.isDemo();
        }
        return z;
    }

    public boolean isPreCreated(int userId) {
        boolean z;
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "isPreCreated");
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            z = userInfo != null && userInfo.preCreated;
        }
        return z;
    }

    public boolean isRestricted(int userId) {
        boolean isRestricted;
        if (userId != UserHandle.getCallingUserId()) {
            checkCreateUsersPermission("query isRestricted for user " + userId);
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            isRestricted = userInfo == null ? false : userInfo.isRestricted();
        }
        return isRestricted;
    }

    public boolean canHaveRestrictedProfile(int userId) {
        checkManageUsersPermission("canHaveRestrictedProfile");
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            boolean z = false;
            if (userInfo != null && userInfo.canHaveProfile()) {
                if (!userInfo.isAdmin()) {
                    return false;
                }
                if (!this.mIsDeviceManaged && !this.mIsUserManaged.get(userId)) {
                    z = true;
                }
                return z;
            }
            return false;
        }
    }

    public boolean hasRestrictedProfiles(int userId) {
        checkManageUsersPermission("hasRestrictedProfiles");
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo profile = this.mUsers.valueAt(i).info;
                if (userId != profile.id && profile.restrictedProfileParentId == userId) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserInfo getUserInfoLU(int userId) {
        UserData userData = this.mUsers.get(userId);
        if (userData != null && userData.info.partial && !this.mRemovingUserIds.get(userId)) {
            Slog.w(LOG_TAG, "getUserInfo: unknown user #" + userId);
            return null;
        } else if (userData != null) {
            return userData.info;
        } else {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserData getUserDataLU(int userId) {
        UserData userData = this.mUsers.get(userId);
        if (userData != null && userData.info.partial && !this.mRemovingUserIds.get(userId)) {
            return null;
        }
        return userData;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserInfo getUserInfoNoChecks(int userId) {
        UserInfo userInfo;
        synchronized (this.mUsersLock) {
            UserData userData = this.mUsers.get(userId);
            userInfo = userData != null ? userData.info : null;
        }
        return userInfo;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserData getUserDataNoChecks(int userId) {
        UserData userData;
        synchronized (this.mUsersLock) {
            userData = this.mUsers.get(userId);
        }
        return userData;
    }

    public boolean exists(int userId) {
        return this.mLocalService.exists(userId);
    }

    public void setUserName(int userId, String name) {
        checkManageUsersPermission("rename users");
        boolean changed = false;
        synchronized (this.mPackagesLock) {
            UserData userData = getUserDataNoChecks(userId);
            if (userData != null && !userData.info.partial) {
                if (name != null && !name.equals(userData.info.name)) {
                    userData.info.name = name;
                    writeUserLP(userData);
                    changed = true;
                }
                if (changed) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        sendUserInfoChangedBroadcast(userId);
                        return;
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
                return;
            }
            Slog.w(LOG_TAG, "setUserName: unknown user #" + userId);
        }
    }

    public void setUserIcon(int userId, Bitmap bitmap) {
        try {
            checkManageUsersPermission("update users");
            enforceUserRestriction("no_set_user_icon", userId, "Cannot set user icon");
            this.mLocalService.setUserIcon(userId, bitmap);
        } catch (UserManager.CheckedUserOperationException e) {
            throw e.toServiceSpecificException();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendUserInfoChangedBroadcast(int userId) {
        Intent changedIntent = new Intent("android.intent.action.USER_INFO_CHANGED");
        changedIntent.putExtra("android.intent.extra.user_handle", userId);
        changedIntent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(changedIntent, UserHandle.ALL);
    }

    public ParcelFileDescriptor getUserIcon(int targetUserId) {
        if (!hasManageUsersOrPermission("android.permission.GET_ACCOUNTS_PRIVILEGED")) {
            throw new SecurityException("You need MANAGE_USERS or GET_ACCOUNTS_PRIVILEGED permissions to: get user icon");
        }
        synchronized (this.mPackagesLock) {
            UserInfo targetUserInfo = getUserInfoNoChecks(targetUserId);
            if (targetUserInfo != null && !targetUserInfo.partial) {
                int callingUserId = UserHandle.getCallingUserId();
                int callingGroupId = getUserInfoNoChecks(callingUserId).profileGroupId;
                int targetGroupId = targetUserInfo.profileGroupId;
                boolean sameGroup = callingGroupId != -10000 && callingGroupId == targetGroupId;
                if (callingUserId != targetUserId && !sameGroup) {
                    checkManageUsersPermission("get the icon of a user who is not related");
                }
                if (targetUserInfo.iconPath == null) {
                    return null;
                }
                String iconPath = targetUserInfo.iconPath;
                try {
                    return ParcelFileDescriptor.open(new File(iconPath), 268435456);
                } catch (FileNotFoundException e) {
                    Slog.e(LOG_TAG, "Couldn't find icon file", e);
                    return null;
                }
            }
            Slog.w(LOG_TAG, "getUserIcon: unknown user #" + targetUserId);
            return null;
        }
    }

    public void makeInitialized(int userId) {
        checkManageUsersPermission("makeInitialized");
        boolean scheduleWriteUser = false;
        synchronized (this.mUsersLock) {
            UserData userData = this.mUsers.get(userId);
            if (userData != null && !userData.info.partial) {
                if ((userData.info.flags & 16) == 0) {
                    userData.info.flags |= 16;
                    scheduleWriteUser = true;
                }
                if (scheduleWriteUser) {
                    scheduleWriteUser(userData);
                    return;
                }
                return;
            }
            Slog.w(LOG_TAG, "makeInitialized: unknown user #" + userId);
        }
    }

    private void initDefaultGuestRestrictions() {
        synchronized (this.mGuestRestrictions) {
            if (this.mGuestRestrictions.isEmpty()) {
                UserTypeDetails guestType = this.mUserTypes.get("android.os.usertype.full.GUEST");
                if (guestType == null) {
                    Slog.wtf(LOG_TAG, "Can't set default guest restrictions: type doesn't exist.");
                    return;
                }
                guestType.addDefaultRestrictionsTo(this.mGuestRestrictions);
            }
        }
    }

    public Bundle getDefaultGuestRestrictions() {
        Bundle bundle;
        checkManageUsersPermission("getDefaultGuestRestrictions");
        synchronized (this.mGuestRestrictions) {
            bundle = new Bundle(this.mGuestRestrictions);
        }
        return bundle;
    }

    public void setDefaultGuestRestrictions(Bundle restrictions) {
        checkManageUsersPermission("setDefaultGuestRestrictions");
        synchronized (this.mGuestRestrictions) {
            this.mGuestRestrictions.clear();
            this.mGuestRestrictions.putAll(restrictions);
        }
        synchronized (this.mPackagesLock) {
            writeUserListLP();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDevicePolicyUserRestrictionsInner(int originatingUserId, Bundle global, RestrictionsSet local, boolean isDeviceOwner) {
        boolean globalChanged;
        List<Integer> updatedLocalTargetUserIds;
        boolean localChanged;
        synchronized (this.mRestrictionsLock) {
            globalChanged = this.mDevicePolicyGlobalUserRestrictions.updateRestrictions(originatingUserId, global);
            updatedLocalTargetUserIds = getUpdatedTargetUserIdsFromLocalRestrictions(originatingUserId, local);
            localChanged = updateLocalRestrictionsForTargetUsersLR(originatingUserId, local, updatedLocalTargetUserIds);
            if (isDeviceOwner) {
                this.mDeviceOwnerUserId = originatingUserId;
            } else if (this.mDeviceOwnerUserId == originatingUserId) {
                this.mDeviceOwnerUserId = -10000;
            }
        }
        synchronized (this.mPackagesLock) {
            if (globalChanged || localChanged) {
                if (updatedLocalTargetUserIds.size() == 1 && updatedLocalTargetUserIds.contains(Integer.valueOf(originatingUserId))) {
                    writeUserLP(getUserDataNoChecks(originatingUserId));
                } else {
                    if (globalChanged) {
                        writeUserLP(getUserDataNoChecks(originatingUserId));
                    }
                    if (localChanged) {
                        for (Integer num : updatedLocalTargetUserIds) {
                            int targetUserId = num.intValue();
                            writeAllTargetUsersLP(targetUserId);
                        }
                    }
                }
            }
        }
        synchronized (this.mRestrictionsLock) {
            try {
                if (globalChanged) {
                    applyUserRestrictionsForAllUsersLR();
                } else if (localChanged) {
                    for (Integer num2 : updatedLocalTargetUserIds) {
                        int targetUserId2 = num2.intValue();
                        applyUserRestrictionsLR(targetUserId2);
                    }
                }
            } finally {
            }
        }
    }

    private List<Integer> getUpdatedTargetUserIdsFromLocalRestrictions(int originatingUserId, RestrictionsSet local) {
        List<Integer> targetUserIds = new ArrayList<>();
        for (int i = 0; i < local.size(); i++) {
            targetUserIds.add(Integer.valueOf(local.keyAt(i)));
        }
        for (int i2 = 0; i2 < this.mDevicePolicyLocalUserRestrictions.size(); i2++) {
            int targetUserId = this.mDevicePolicyLocalUserRestrictions.keyAt(i2);
            RestrictionsSet restrictionsSet = this.mDevicePolicyLocalUserRestrictions.valueAt(i2);
            if (!local.containsKey(targetUserId) && restrictionsSet.containsKey(originatingUserId)) {
                targetUserIds.add(Integer.valueOf(targetUserId));
            }
        }
        return targetUserIds;
    }

    private boolean updateLocalRestrictionsForTargetUsersLR(int originatingUserId, RestrictionsSet local, List<Integer> updatedTargetUserIds) {
        boolean changed = false;
        for (Integer num : updatedTargetUserIds) {
            int targetUserId = num.intValue();
            Bundle restrictions = local.getRestrictions(targetUserId);
            if (restrictions == null) {
                restrictions = new Bundle();
            }
            if (getDevicePolicyLocalRestrictionsForTargetUserLR(targetUserId).updateRestrictions(originatingUserId, restrictions)) {
                changed = true;
            }
        }
        return changed;
    }

    private RestrictionsSet getDevicePolicyLocalRestrictionsForTargetUserLR(int targetUserId) {
        RestrictionsSet result = this.mDevicePolicyLocalUserRestrictions.get(targetUserId);
        if (result == null) {
            RestrictionsSet result2 = new RestrictionsSet();
            this.mDevicePolicyLocalUserRestrictions.put(targetUserId, result2);
            return result2;
        }
        return result;
    }

    private Bundle computeEffectiveUserRestrictionsLR(int userId) {
        Bundle baseRestrictions = UserRestrictionsUtils.nonNull(this.mBaseUserRestrictions.getRestrictions(userId));
        Bundle global = this.mDevicePolicyGlobalUserRestrictions.mergeAll();
        RestrictionsSet local = getDevicePolicyLocalRestrictionsForTargetUserLR(userId);
        if (BundleUtils.isEmpty(global) && local.isEmpty()) {
            return baseRestrictions;
        }
        Bundle effective = BundleUtils.clone(baseRestrictions);
        UserRestrictionsUtils.merge(effective, global);
        UserRestrictionsUtils.merge(effective, local.mergeAll());
        return effective;
    }

    private void invalidateEffectiveUserRestrictionsLR(int userId) {
        this.mCachedEffectiveUserRestrictions.remove(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Bundle getEffectiveUserRestrictions(int userId) {
        Bundle restrictions;
        synchronized (this.mRestrictionsLock) {
            restrictions = this.mCachedEffectiveUserRestrictions.getRestrictions(userId);
            if (restrictions == null) {
                restrictions = computeEffectiveUserRestrictionsLR(userId);
                this.mCachedEffectiveUserRestrictions.updateRestrictions(userId, restrictions);
            }
        }
        return restrictions;
    }

    public boolean hasUserRestriction(String restrictionKey, int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "hasUserRestriction");
        return this.mLocalService.hasUserRestriction(restrictionKey, userId);
    }

    public boolean hasUserRestrictionOnAnyUser(String restrictionKey) {
        if (UserRestrictionsUtils.isValidRestriction(restrictionKey)) {
            List<UserInfo> users = getUsers(true);
            for (int i = 0; i < users.size(); i++) {
                int userId = users.get(i).id;
                Bundle restrictions = getEffectiveUserRestrictions(userId);
                if (restrictions != null && restrictions.getBoolean(restrictionKey)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean isSettingRestrictedForUser(String setting, int userId, String value, int callingUid) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Non-system caller");
        }
        return UserRestrictionsUtils.isSettingRestrictedForUser(this.mContext, setting, userId, value, callingUid);
    }

    public void addUserRestrictionsListener(final IUserRestrictionsListener listener) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Non-system caller");
        }
        this.mLocalService.addUserRestrictionsListener(new UserManagerInternal.UserRestrictionsListener() { // from class: com.android.server.pm.UserManagerService$$ExternalSyntheticLambda3
            @Override // com.android.server.pm.UserManagerInternal.UserRestrictionsListener
            public final void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2) {
                UserManagerService.lambda$addUserRestrictionsListener$1(listener, i, bundle, bundle2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addUserRestrictionsListener$1(IUserRestrictionsListener listener, int userId, Bundle newRestrict, Bundle prevRestrict) {
        try {
            listener.onUserRestrictionsChanged(userId, newRestrict, prevRestrict);
        } catch (RemoteException re) {
            Slog.e("IUserRestrictionsListener", "Unable to invoke listener: " + re.getMessage());
        }
    }

    public int getUserRestrictionSource(String restrictionKey, int userId) {
        List<UserManager.EnforcingUser> enforcingUsers = getUserRestrictionSources(restrictionKey, userId);
        int result = 0;
        for (int i = enforcingUsers.size() - 1; i >= 0; i--) {
            result |= enforcingUsers.get(i).getUserRestrictionSource();
        }
        return result;
    }

    public List<UserManager.EnforcingUser> getUserRestrictionSources(String restrictionKey, int userId) {
        checkQueryUsersPermission("call getUserRestrictionSources.");
        if (!hasUserRestriction(restrictionKey, userId)) {
            return Collections.emptyList();
        }
        List<UserManager.EnforcingUser> result = new ArrayList<>();
        if (hasBaseUserRestriction(restrictionKey, userId)) {
            result.add(new UserManager.EnforcingUser(-10000, 1));
        }
        synchronized (this.mRestrictionsLock) {
            result.addAll(getDevicePolicyLocalRestrictionsForTargetUserLR(userId).getEnforcingUsers(restrictionKey, this.mDeviceOwnerUserId));
            result.addAll(this.mDevicePolicyGlobalUserRestrictions.getEnforcingUsers(restrictionKey, this.mDeviceOwnerUserId));
        }
        return result;
    }

    public Bundle getUserRestrictions(int userId) {
        checkManageOrInteractPermissionIfCallerInOtherProfileGroup(userId, "getUserRestrictions");
        return BundleUtils.clone(getEffectiveUserRestrictions(userId));
    }

    public boolean hasBaseUserRestriction(String restrictionKey, int userId) {
        checkCreateUsersPermission("hasBaseUserRestriction");
        boolean z = false;
        if (UserRestrictionsUtils.isValidRestriction(restrictionKey)) {
            synchronized (this.mRestrictionsLock) {
                Bundle bundle = this.mBaseUserRestrictions.getRestrictions(userId);
                if (bundle != null && bundle.getBoolean(restrictionKey, false)) {
                    z = true;
                }
            }
            return z;
        }
        return false;
    }

    public void setUserRestriction(String key, boolean value, int userId) {
        checkManageUsersPermission("setUserRestriction");
        if (!UserRestrictionsUtils.isValidRestriction(key)) {
            return;
        }
        synchronized (this.mRestrictionsLock) {
            Bundle newRestrictions = BundleUtils.clone(this.mBaseUserRestrictions.getRestrictions(userId));
            newRestrictions.putBoolean(key, value);
            updateUserRestrictionsInternalLR(newRestrictions, userId);
        }
    }

    private void updateUserRestrictionsInternalLR(Bundle newBaseRestrictions, final int userId) {
        Bundle prevAppliedRestrictions = UserRestrictionsUtils.nonNull(this.mAppliedUserRestrictions.getRestrictions(userId));
        if (newBaseRestrictions != null) {
            Bundle prevBaseRestrictions = this.mBaseUserRestrictions.getRestrictions(userId);
            Preconditions.checkState(prevBaseRestrictions != newBaseRestrictions);
            Preconditions.checkState(this.mCachedEffectiveUserRestrictions.getRestrictions(userId) != newBaseRestrictions);
            if (this.mBaseUserRestrictions.updateRestrictions(userId, newBaseRestrictions)) {
                scheduleWriteUser(getUserDataNoChecks(userId));
            }
        }
        final Bundle effective = computeEffectiveUserRestrictionsLR(userId);
        this.mCachedEffectiveUserRestrictions.updateRestrictions(userId, effective);
        if (this.mAppOpsService != null) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.pm.UserManagerService.3
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        UserManagerService.this.mAppOpsService.setUserRestrictions(effective, UserManagerService.mUserRestriconToken, userId);
                    } catch (RemoteException e) {
                        Slog.w(UserManagerService.LOG_TAG, "Unable to notify AppOpsService of UserRestrictions");
                    }
                }
            });
        }
        propagateUserRestrictionsLR(userId, effective, prevAppliedRestrictions);
        this.mAppliedUserRestrictions.updateRestrictions(userId, new Bundle(effective));
    }

    private void propagateUserRestrictionsLR(final int userId, Bundle newRestrictions, Bundle prevRestrictions) {
        if (UserRestrictionsUtils.areEqual(newRestrictions, prevRestrictions)) {
            return;
        }
        final Bundle newRestrictionsFinal = new Bundle(newRestrictions);
        final Bundle prevRestrictionsFinal = new Bundle(prevRestrictions);
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.UserManagerService.4
            @Override // java.lang.Runnable
            public void run() {
                UserManagerInternal.UserRestrictionsListener[] listeners;
                UserRestrictionsUtils.applyUserRestrictions(UserManagerService.this.mContext, userId, newRestrictionsFinal, prevRestrictionsFinal);
                synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                    listeners = new UserManagerInternal.UserRestrictionsListener[UserManagerService.this.mUserRestrictionsListeners.size()];
                    UserManagerService.this.mUserRestrictionsListeners.toArray(listeners);
                }
                for (UserManagerInternal.UserRestrictionsListener userRestrictionsListener : listeners) {
                    userRestrictionsListener.onUserRestrictionsChanged(userId, newRestrictionsFinal, prevRestrictionsFinal);
                }
                Intent broadcast = new Intent("android.os.action.USER_RESTRICTIONS_CHANGED").setFlags(1073741824);
                UserManagerService.this.mContext.sendBroadcastAsUser(broadcast, UserHandle.of(userId));
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyUserRestrictionsLR(int userId) {
        updateUserRestrictionsInternalLR(null, userId);
    }

    private void applyUserRestrictionsForAllUsersLR() {
        this.mCachedEffectiveUserRestrictions.removeAllRestrictions();
        Runnable r = new Runnable() { // from class: com.android.server.pm.UserManagerService.5
            @Override // java.lang.Runnable
            public void run() {
                try {
                    int[] runningUsers = ActivityManager.getService().getRunningUserIds();
                    synchronized (UserManagerService.this.mRestrictionsLock) {
                        for (int i : runningUsers) {
                            UserManagerService.this.applyUserRestrictionsLR(i);
                        }
                    }
                } catch (RemoteException e) {
                    Slog.w(UserManagerService.LOG_TAG, "Unable to access ActivityManagerService");
                }
            }
        };
        this.mHandler.post(r);
    }

    private boolean isUserLimitReached() {
        int count;
        synchronized (this.mUsersLock) {
            count = getAliveUsersExcludingGuestsCountLU();
        }
        return count >= UserManager.getMaxSupportedUsers();
    }

    private boolean canAddMoreUsersOfType(UserTypeDetails userTypeDetails) {
        if (userTypeDetails.isEnabled()) {
            int max = userTypeDetails.getMaxAllowed();
            return max == -1 || getNumberOfUsersOfType(userTypeDetails.getName()) < max;
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x003d A[Catch: all -> 0x007c, TryCatch #0 {, blocks: (B:9:0x001b, B:11:0x0028, B:14:0x002f, B:16:0x0036, B:18:0x003d, B:20:0x004b, B:29:0x0057, B:33:0x005d, B:35:0x005f, B:39:0x0071, B:40:0x007a, B:38:0x0068), top: B:46:0x001b }] */
    /* JADX WARN: Removed duplicated region for block: B:33:0x005d A[Catch: all -> 0x007c, DONT_GENERATE, TryCatch #0 {, blocks: (B:9:0x001b, B:11:0x0028, B:14:0x002f, B:16:0x0036, B:18:0x003d, B:20:0x004b, B:29:0x0057, B:33:0x005d, B:35:0x005f, B:39:0x0071, B:40:0x007a, B:38:0x0068), top: B:46:0x001b }] */
    /* JADX WARN: Removed duplicated region for block: B:35:0x005f A[Catch: all -> 0x007c, TryCatch #0 {, blocks: (B:9:0x001b, B:11:0x0028, B:14:0x002f, B:16:0x0036, B:18:0x003d, B:20:0x004b, B:29:0x0057, B:33:0x005d, B:35:0x005f, B:39:0x0071, B:40:0x007a, B:38:0x0068), top: B:46:0x001b }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int getRemainingCreatableUserCount(String userType) {
        int result;
        boolean z;
        checkQueryOrCreateUsersPermission("get the remaining number of users that can be added.");
        UserTypeDetails type = this.mUserTypes.get(userType);
        if (type == null || !type.isEnabled()) {
            return 0;
        }
        synchronized (this.mUsersLock) {
            int userCount = getAliveUsersExcludingGuestsCountLU();
            int i = Integer.MAX_VALUE;
            if (!UserManager.isUserTypeGuest(userType) && !UserManager.isUserTypeDemo(userType)) {
                result = UserManager.getMaxSupportedUsers() - userCount;
                if (type.isManagedProfile()) {
                    if (!this.mContext.getPackageManager().hasSystemFeature("android.software.managed_users")) {
                        return 0;
                    }
                    boolean z2 = true;
                    if (result > 0) {
                        z = false;
                    } else {
                        z = true;
                    }
                    if (userCount != 1) {
                        z2 = false;
                    }
                    if (z2 & z) {
                        result = 1;
                    }
                }
                if (result > 0) {
                    return 0;
                }
                if (type.getMaxAllowed() != -1) {
                    i = type.getMaxAllowed() - getNumberOfUsersOfType(userType);
                }
                return Math.max(0, Math.min(result, i));
            }
            result = Integer.MAX_VALUE;
            if (type.isManagedProfile()) {
            }
            if (result > 0) {
            }
        }
    }

    private int getNumberOfUsersOfType(String userType) {
        int count = 0;
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserInfo user = this.mUsers.valueAt(i).info;
                if (user.userType.equals(userType) && !user.guestToRemove && !this.mRemovingUserIds.get(user.id) && !user.preCreated) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean canAddMoreUsersOfType(String userType) {
        checkCreateUsersPermission("check if more users can be added.");
        UserTypeDetails userTypeDetails = this.mUserTypes.get(userType);
        return userTypeDetails != null && canAddMoreUsersOfType(userTypeDetails);
    }

    public boolean isUserTypeEnabled(String userType) {
        checkCreateUsersPermission("check if user type is enabled.");
        UserTypeDetails userTypeDetails = this.mUserTypes.get(userType);
        return userTypeDetails != null && userTypeDetails.isEnabled();
    }

    public boolean canAddMoreManagedProfiles(int userId, boolean allowedToRemoveOne) {
        return canAddMoreProfilesToUser("android.os.usertype.profile.MANAGED", userId, allowedToRemoveOne);
    }

    public boolean canAddMoreProfilesToUser(String userType, int userId, boolean allowedToRemoveOne) {
        return getRemainingCreatableProfileCount(userType, userId, allowedToRemoveOne) > 0;
    }

    public int getRemainingCreatableProfileCount(String userType, int userId) {
        return getRemainingCreatableProfileCount(userType, userId, false);
    }

    private int getRemainingCreatableProfileCount(String userType, int userId, boolean allowedToRemoveOne) {
        int profilesRemovedCount;
        checkQueryOrCreateUsersPermission("get the remaining number of profiles that can be added to the given user.");
        UserTypeDetails type = this.mUserTypes.get(userType);
        if (type == null || !type.isEnabled()) {
            return 0;
        }
        boolean isManagedProfile = type.isManagedProfile();
        if (isManagedProfile && !this.mContext.getPackageManager().hasSystemFeature("android.software.managed_users")) {
            return 0;
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            if (userInfo != null && userInfo.canHaveProfile()) {
                int userTypeCount = getProfileIds(userId, userType, false).length;
                if (userTypeCount <= 0 || !allowedToRemoveOne) {
                    profilesRemovedCount = 0;
                } else {
                    profilesRemovedCount = 1;
                }
                int usersCountAfterRemoving = (getAliveUsersExcludingGuestsCountLU() - profilesRemovedCount) - checkDualProfileCount(userId);
                boolean isManagedDualProfile = UserManager.isUserTypeDualProfile(userType);
                int result = UserManager.getMaxSupportedUsers() - usersCountAfterRemoving;
                if (result <= 0 && ((isManagedProfile || isManagedDualProfile) && usersCountAfterRemoving == 1)) {
                    result = 1;
                }
                int maxUsersOfType = getMaxUsersOfTypePerParent(type);
                if (maxUsersOfType != -1) {
                    result = Math.min(result, maxUsersOfType - (userTypeCount - profilesRemovedCount));
                }
                if (result <= 0) {
                    return 0;
                }
                if (type.getMaxAllowed() != -1) {
                    result = Math.min(result, type.getMaxAllowed() - (getNumberOfUsersOfType(userType) - profilesRemovedCount));
                }
                return Math.max(0, result);
            }
            return 0;
        }
    }

    private int getAliveUsersExcludingGuestsCountLU() {
        int aliveUserCount = 0;
        int totalUserCount = this.mUsers.size();
        for (int i = 0; i < totalUserCount; i++) {
            UserInfo user = this.mUsers.valueAt(i).info;
            if (!this.mRemovingUserIds.get(user.id) && !user.isGuest() && !user.preCreated) {
                aliveUserCount++;
            }
        }
        return aliveUserCount;
    }

    private static final void checkManageUserAndAcrossUsersFullPermission(String message) {
        int uid = Binder.getCallingUid();
        if (uid == 1000 || uid == 0) {
            return;
        }
        if (hasPermissionGranted("android.permission.MANAGE_USERS", uid) && hasPermissionGranted("android.permission.INTERACT_ACROSS_USERS_FULL", uid)) {
            return;
        }
        throw new SecurityException("You need MANAGE_USERS and INTERACT_ACROSS_USERS_FULL permission to: " + message);
    }

    private static boolean hasPermissionGranted(String permission, int uid) {
        return ActivityManager.checkComponentPermission(permission, uid, -1, true) == 0;
    }

    private static final void checkManageUsersPermission(String message) {
        if (!hasManageUsersPermission()) {
            throw new SecurityException("You need MANAGE_USERS permission to: " + message);
        }
    }

    private static final void checkCreateUsersPermission(String message) {
        if (!hasCreateUsersPermission()) {
            throw new SecurityException("You either need MANAGE_USERS or CREATE_USERS permission to: " + message);
        }
    }

    private static final void checkQueryUsersPermission(String message) {
        if (!hasQueryUsersPermission()) {
            throw new SecurityException("You either need MANAGE_USERS or QUERY_USERS permission to: " + message);
        }
    }

    private static final void checkQueryOrCreateUsersPermission(String message) {
        if (!hasQueryOrCreateUsersPermission()) {
            throw new SecurityException("You either need MANAGE_USERS, CREATE_USERS, or QUERY_USERS permission to: " + message);
        }
    }

    private static final void checkCreateUsersPermission(int creationFlags) {
        if ((creationFlags & (-5933)) == 0) {
            if (!hasCreateUsersPermission()) {
                throw new SecurityException("You either need MANAGE_USERS or CREATE_USERS permission to create an user with flags: " + creationFlags);
            }
        } else if (!hasManageUsersPermission()) {
            throw new SecurityException("You need MANAGE_USERS permission to create an user  with flags: " + creationFlags);
        }
    }

    private static final boolean hasManageUsersPermission() {
        int callingUid = Binder.getCallingUid();
        return hasManageUsersPermission(callingUid);
    }

    private static boolean hasManageUsersPermission(int callingUid) {
        return UserHandle.isSameApp(callingUid, 1000) || callingUid == 0 || hasPermissionGranted("android.permission.MANAGE_USERS", callingUid);
    }

    private static final boolean hasManageUsersOrPermission(String alternativePermission) {
        int callingUid = Binder.getCallingUid();
        return hasManageUsersPermission(callingUid) || hasPermissionGranted(alternativePermission, callingUid);
    }

    private static final boolean hasCreateUsersPermission() {
        return hasManageUsersOrPermission("android.permission.CREATE_USERS");
    }

    private static final boolean hasQueryUsersPermission() {
        return hasManageUsersOrPermission("android.permission.QUERY_USERS");
    }

    private static final boolean hasQueryOrCreateUsersPermission() {
        return hasCreateUsersPermission() || hasPermissionGranted("android.permission.QUERY_USERS", Binder.getCallingUid());
    }

    private static void checkSystemOrRoot(String message) {
        int uid = Binder.getCallingUid();
        if (!UserHandle.isSameApp(uid, 1000) && uid != 0) {
            throw new SecurityException("Only system may: " + message);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeBitmapLP(UserInfo info, Bitmap bitmap) {
        try {
            File dir = new File(this.mUsersDir, Integer.toString(info.id));
            File file = new File(dir, USER_PHOTO_FILENAME);
            File tmp = new File(dir, USER_PHOTO_FILENAME_TMP);
            if (!dir.exists()) {
                dir.mkdir();
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            FileOutputStream os = new FileOutputStream(tmp);
            if (bitmap.compress(compressFormat, 100, os) && tmp.renameTo(file) && SELinux.restorecon(file)) {
                info.iconPath = file.getAbsolutePath();
            }
            try {
                os.close();
            } catch (IOException e) {
            }
            tmp.delete();
        } catch (FileNotFoundException e2) {
            Slog.w(LOG_TAG, "Error setting photo for user ", e2);
        }
    }

    public int[] getUserIds() {
        int[] iArr;
        synchronized (this.mUsersLock) {
            iArr = this.mUserIds;
        }
        return iArr;
    }

    public int[] getUserIdsIncludingPreCreated() {
        int[] iArr;
        synchronized (this.mUsersLock) {
            iArr = this.mUserIdsIncludingPreCreated;
        }
        return iArr;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2994=4] */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00d0, code lost:
        if (r2.getName().equals(com.android.server.pm.UserManagerService.TAG_RESTRICTIONS) == false) goto L46;
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00d2, code lost:
        r9 = r13.mGuestRestrictions;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00d4, code lost:
        monitor-enter(r9);
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x00d5, code lost:
        com.android.server.pm.UserRestrictionsUtils.readRestrictions(r2, r13.mGuestRestrictions);
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x00da, code lost:
        monitor-exit(r9);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void readUserListLP() {
        TypedXmlPullParser parser;
        int type;
        if (!this.mUserListFile.exists()) {
            fallbackToSingleUserLP();
            return;
        }
        FileInputStream fis = null;
        AtomicFile userListFile = new AtomicFile(this.mUserListFile);
        try {
            try {
                fis = userListFile.openRead();
                parser = Xml.resolvePullParser(fis);
                while (true) {
                    type = parser.next();
                    if (type == 2 || type == 1) {
                        break;
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                fallbackToSingleUserLP();
            }
            if (type != 2) {
                Slog.e(LOG_TAG, "Unable to read user list");
                fallbackToSingleUserLP();
                return;
            }
            this.mNextSerialNumber = -1;
            if (parser.getName().equals("users")) {
                this.mNextSerialNumber = parser.getAttributeInt((String) null, ATTR_NEXT_SERIAL_NO, this.mNextSerialNumber);
                this.mUserVersion = parser.getAttributeInt((String) null, ATTR_USER_VERSION, this.mUserVersion);
                this.mUserTypeVersion = parser.getAttributeInt((String) null, ATTR_USER_TYPE_VERSION, this.mUserTypeVersion);
            }
            Bundle oldDevicePolicyGlobalUserRestrictions = null;
            while (true) {
                int type2 = parser.next();
                if (type2 == 1) {
                    break;
                } else if (type2 == 2) {
                    String name = parser.getName();
                    if (name.equals(TAG_USER)) {
                        UserData userData = readUserLP(parser.getAttributeInt((String) null, ATTR_ID));
                        if (userData != null) {
                            synchronized (this.mUsersLock) {
                                this.mUsers.put(userData.info.id, userData);
                                int i = this.mNextSerialNumber;
                                if (i < 0 || i <= userData.info.id) {
                                    this.mNextSerialNumber = userData.info.id + 1;
                                }
                            }
                        }
                    } else if (name.equals(TAG_GUEST_RESTRICTIONS)) {
                        while (true) {
                            int type3 = parser.next();
                            if (type3 != 1 && type3 != 3) {
                                if (type3 == 2) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        if (!name.equals(TAG_DEVICE_OWNER_USER_ID) && !name.equals(TAG_GLOBAL_RESTRICTION_OWNER_ID)) {
                            if (name.equals(TAG_DEVICE_POLICY_RESTRICTIONS)) {
                                oldDevicePolicyGlobalUserRestrictions = UserRestrictionsUtils.readRestrictions(parser);
                            }
                        }
                        this.mDeviceOwnerUserId = parser.getAttributeInt((String) null, ATTR_ID, this.mDeviceOwnerUserId);
                    }
                }
            }
            updateUserIds();
            upgradeIfNecessaryLP(oldDevicePolicyGlobalUserRestrictions);
        } finally {
            IoUtils.closeQuietly((AutoCloseable) null);
        }
    }

    private void upgradeIfNecessaryLP(Bundle oldGlobalUserRestrictions) {
        upgradeIfNecessaryLP(oldGlobalUserRestrictions, this.mUserVersion, this.mUserTypeVersion);
    }

    void upgradeIfNecessaryLP(Bundle oldGlobalUserRestrictions, int userVersion, int userTypeVersion) {
        int i;
        Set<Integer> userIdsToWrite = new ArraySet<>();
        int originalVersion = this.mUserVersion;
        int originalUserTypeVersion = this.mUserTypeVersion;
        if (userVersion < 1) {
            UserData userData = getUserDataNoChecks(0);
            if ("Primary".equals(userData.info.name)) {
                userData.info.name = this.mContext.getResources().getString(17040948);
                userIdsToWrite.add(Integer.valueOf(userData.info.id));
            }
            userVersion = 1;
        }
        if (userVersion < 2) {
            UserData userData2 = getUserDataNoChecks(0);
            if ((userData2.info.flags & 16) == 0) {
                userData2.info.flags |= 16;
                userIdsToWrite.add(Integer.valueOf(userData2.info.id));
            }
            userVersion = 2;
        }
        if (userVersion < 4) {
            userVersion = 4;
        }
        if (userVersion < 5) {
            initDefaultGuestRestrictions();
            userVersion = 5;
        }
        if (userVersion < 6) {
            boolean splitSystemUser = UserManager.isSplitSystemUser();
            synchronized (this.mUsersLock) {
                for (int i2 = 0; i2 < this.mUsers.size(); i2++) {
                    UserData userData3 = this.mUsers.valueAt(i2);
                    if (!splitSystemUser && userData3.info.isRestricted() && userData3.info.restrictedProfileParentId == -10000) {
                        userData3.info.restrictedProfileParentId = 0;
                        userIdsToWrite.add(Integer.valueOf(userData3.info.id));
                    }
                }
            }
            userVersion = 6;
        }
        if (userVersion < 7) {
            synchronized (this.mRestrictionsLock) {
                if (!BundleUtils.isEmpty(oldGlobalUserRestrictions) && (i = this.mDeviceOwnerUserId) != -10000) {
                    this.mDevicePolicyGlobalUserRestrictions.updateRestrictions(i, oldGlobalUserRestrictions);
                }
                UserRestrictionsUtils.moveRestriction("ensure_verify_apps", this.mDevicePolicyLocalUserRestrictions, this.mDevicePolicyGlobalUserRestrictions);
            }
            UserInfo currentGuestUser = findCurrentGuestUser();
            if (currentGuestUser != null && !hasUserRestriction("no_config_wifi", currentGuestUser.id)) {
                setUserRestriction("no_config_wifi", true, currentGuestUser.id);
            }
            userVersion = 7;
        }
        if (userVersion < 8) {
            synchronized (this.mUsersLock) {
                UserData userData4 = this.mUsers.get(0);
                userData4.info.flags |= 2048;
                if (!UserManager.isHeadlessSystemUserMode()) {
                    userData4.info.flags |= 1024;
                }
                userIdsToWrite.add(Integer.valueOf(userData4.info.id));
                for (int i3 = 1; i3 < this.mUsers.size(); i3++) {
                    UserData userData5 = this.mUsers.valueAt(i3);
                    if ((userData5.info.flags & 32) == 0) {
                        userData5.info.flags |= 1024;
                        userIdsToWrite.add(Integer.valueOf(userData5.info.id));
                    }
                }
            }
            userVersion = 8;
        }
        if (userVersion < 9) {
            synchronized (this.mUsersLock) {
                for (int i4 = 0; i4 < this.mUsers.size(); i4++) {
                    UserData userData6 = this.mUsers.valueAt(i4);
                    int flags = userData6.info.flags;
                    if ((flags & 2048) != 0) {
                        if ((flags & 1024) != 0) {
                            userData6.info.userType = "android.os.usertype.full.SYSTEM";
                        } else {
                            userData6.info.userType = "android.os.usertype.system.HEADLESS";
                        }
                    } else {
                        try {
                            userData6.info.userType = UserInfo.getDefaultUserType(flags);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalStateException("Cannot upgrade user with flags " + Integer.toHexString(flags) + " because it doesn't correspond to a valid user type.", e);
                        }
                    }
                    UserTypeDetails userTypeDetails = this.mUserTypes.get(userData6.info.userType);
                    if (userTypeDetails == null) {
                        throw new IllegalStateException("Cannot upgrade user with flags " + Integer.toHexString(flags) + " because " + userData6.info.userType + " isn't defined on this device!");
                    }
                    userData6.info.flags |= userTypeDetails.getDefaultUserInfoFlags();
                    userIdsToWrite.add(Integer.valueOf(userData6.info.id));
                }
            }
            userVersion = 9;
        }
        int newUserTypeVersion = UserTypeFactory.getUserTypeVersion();
        if (newUserTypeVersion > userTypeVersion) {
            synchronized (this.mUsersLock) {
                upgradeUserTypesLU(UserTypeFactory.getUserTypeUpgrades(), this.mUserTypes, userTypeVersion, userIdsToWrite);
            }
        }
        if (userVersion < 9) {
            Slog.w(LOG_TAG, "User version " + this.mUserVersion + " didn't upgrade as expected to 9");
            return;
        }
        this.mUserVersion = userVersion;
        this.mUserTypeVersion = newUserTypeVersion;
        if (originalVersion < userVersion || originalUserTypeVersion < newUserTypeVersion) {
            for (Integer num : userIdsToWrite) {
                int userId = num.intValue();
                UserData userData7 = getUserDataNoChecks(userId);
                if (userData7 != null) {
                    writeUserLP(userData7);
                }
            }
            writeUserListLP();
        }
    }

    private void upgradeUserTypesLU(List<UserTypeFactory.UserTypeUpgrade> upgradeOps, ArrayMap<String, UserTypeDetails> userTypes, int formerUserTypeVersion, Set<Integer> userIdsToWrite) {
        for (UserTypeFactory.UserTypeUpgrade userTypeUpgrade : upgradeOps) {
            if (formerUserTypeVersion <= userTypeUpgrade.getUpToVersion()) {
                for (int i = 0; i < this.mUsers.size(); i++) {
                    UserData userData = this.mUsers.valueAt(i);
                    if (userTypeUpgrade.getFromType().equals(userData.info.userType)) {
                        UserTypeDetails newUserType = userTypes.get(userTypeUpgrade.getToType());
                        if (newUserType == null) {
                            throw new IllegalStateException("Upgrade destination user type not defined: " + userTypeUpgrade.getToType());
                        }
                        upgradeProfileToTypeLU(userData.info, newUserType);
                        userIdsToWrite.add(Integer.valueOf(userData.info.id));
                    }
                }
                continue;
            }
        }
    }

    void upgradeProfileToTypeLU(UserInfo userInfo, UserTypeDetails newUserType) {
        int oldFlags;
        Slog.i(LOG_TAG, "Upgrading user " + userInfo.id + " from " + userInfo.userType + " to " + newUserType.getName());
        if (!userInfo.isProfile()) {
            throw new IllegalStateException("Can only upgrade profile types. " + userInfo.userType + " is not a profile type.");
        }
        if (!canAddMoreProfilesToUser(newUserType.getName(), userInfo.profileGroupId, false)) {
            Slog.w(LOG_TAG, "Exceeded maximum profiles of type " + newUserType.getName() + " for user " + userInfo.id + ". Maximum allowed= " + newUserType.getMaxAllowedPerParent());
        }
        UserTypeDetails oldUserType = this.mUserTypes.get(userInfo.userType);
        if (oldUserType != null) {
            oldFlags = oldUserType.getDefaultUserInfoFlags();
        } else {
            oldFlags = 4096;
        }
        userInfo.userType = newUserType.getName();
        userInfo.flags = newUserType.getDefaultUserInfoFlags() | (userInfo.flags ^ oldFlags);
        synchronized (this.mRestrictionsLock) {
            if (!BundleUtils.isEmpty(newUserType.getDefaultRestrictions())) {
                Bundle newRestrictions = BundleUtils.clone(this.mBaseUserRestrictions.getRestrictions(userInfo.id));
                UserRestrictionsUtils.merge(newRestrictions, newUserType.getDefaultRestrictions());
                updateUserRestrictionsInternalLR(newRestrictions, userInfo.id);
            }
        }
        userInfo.profileBadge = getFreeProfileBadgeLU(userInfo.profileGroupId, userInfo.userType);
    }

    private void fallbackToSingleUserLP() {
        String systemUserType = UserManager.isHeadlessSystemUserMode() ? "android.os.usertype.system.HEADLESS" : "android.os.usertype.full.SYSTEM";
        int flags = 2067 | this.mUserTypes.get(systemUserType).getDefaultUserInfoFlags();
        UserInfo system = new UserInfo(0, (String) null, (String) null, flags, systemUserType);
        UserData userData = putUserInfo(system);
        this.mNextSerialNumber = 10;
        this.mUserVersion = 9;
        this.mUserTypeVersion = UserTypeFactory.getUserTypeVersion();
        Bundle restrictions = new Bundle();
        try {
            String[] defaultFirstUserRestrictions = this.mContext.getResources().getStringArray(17236019);
            for (String userRestriction : defaultFirstUserRestrictions) {
                if (UserRestrictionsUtils.isValidRestriction(userRestriction)) {
                    restrictions.putBoolean(userRestriction, true);
                }
            }
        } catch (Resources.NotFoundException e) {
            Slog.e(LOG_TAG, "Couldn't find resource: config_defaultFirstUserRestrictions", e);
        }
        if (!restrictions.isEmpty()) {
            synchronized (this.mRestrictionsLock) {
                this.mBaseUserRestrictions.updateRestrictions(0, restrictions);
            }
        }
        updateUserIds();
        initDefaultGuestRestrictions();
        writeUserLP(userData);
        writeUserListLP();
    }

    private String getOwnerName() {
        return this.mOwnerName.get();
    }

    private String getGuestName() {
        return this.mContext.getString(17040435);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invalidateOwnerNameIfNecessary(Resources res, boolean forceUpdate) {
        int configChanges = this.mLastConfiguration.updateFrom(res.getConfiguration());
        if (forceUpdate || (this.mOwnerNameTypedValue.changingConfigurations & configChanges) != 0) {
            res.getValue(17040948, this.mOwnerNameTypedValue, true);
            CharSequence ownerName = this.mOwnerNameTypedValue.coerceToString();
            this.mOwnerName.set(ownerName != null ? ownerName.toString() : null);
        }
    }

    private void scheduleWriteUser(UserData userData) {
        if (!this.mHandler.hasMessages(1, userData)) {
            Message msg = this.mHandler.obtainMessage(1, userData);
            this.mHandler.sendMessageDelayed(msg, 2000L);
        }
    }

    private void writeAllTargetUsersLP(int originatingUserId) {
        for (int i = 0; i < this.mDevicePolicyLocalUserRestrictions.size(); i++) {
            int targetUserId = this.mDevicePolicyLocalUserRestrictions.keyAt(i);
            RestrictionsSet restrictionsSet = this.mDevicePolicyLocalUserRestrictions.valueAt(i);
            if (restrictionsSet.containsKey(originatingUserId)) {
                writeUserLP(getUserDataNoChecks(targetUserId));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeUserLP(UserData userData) {
        FileOutputStream fos = null;
        AtomicFile userFile = new AtomicFile(new File(this.mUsersDir, userData.info.id + XML_SUFFIX));
        try {
            fos = userFile.startWrite();
            writeUserLP(userData, fos);
            userFile.finishWrite(fos);
        } catch (Exception ioe) {
            Slog.e(LOG_TAG, "Error writing user info " + userData.info.id, ioe);
            userFile.failWrite(fos);
        }
    }

    void writeUserLP(UserData userData, OutputStream os) throws IOException, XmlPullParserException {
        TypedXmlSerializer serializer = Xml.resolveSerializer(os);
        serializer.startDocument((String) null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        UserInfo userInfo = userData.info;
        serializer.startTag((String) null, TAG_USER);
        serializer.attributeInt((String) null, ATTR_ID, userInfo.id);
        serializer.attributeInt((String) null, ATTR_SERIAL_NO, userInfo.serialNumber);
        serializer.attributeInt((String) null, ATTR_FLAGS, userInfo.flags);
        serializer.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, userInfo.userType);
        serializer.attributeLong((String) null, ATTR_CREATION_TIME, userInfo.creationTime);
        serializer.attributeLong((String) null, ATTR_LAST_LOGGED_IN_TIME, userInfo.lastLoggedInTime);
        if (userInfo.lastLoggedInFingerprint != null) {
            serializer.attribute((String) null, ATTR_LAST_LOGGED_IN_FINGERPRINT, userInfo.lastLoggedInFingerprint);
        }
        if (userInfo.iconPath != null) {
            serializer.attribute((String) null, ATTR_ICON_PATH, userInfo.iconPath);
        }
        if (userInfo.partial) {
            serializer.attributeBoolean((String) null, ATTR_PARTIAL, true);
        }
        if (userInfo.preCreated) {
            serializer.attributeBoolean((String) null, ATTR_PRE_CREATED, true);
        }
        if (userInfo.convertedFromPreCreated) {
            serializer.attributeBoolean((String) null, ATTR_CONVERTED_FROM_PRE_CREATED, true);
        }
        if (userInfo.guestToRemove) {
            serializer.attributeBoolean((String) null, ATTR_GUEST_TO_REMOVE, true);
        }
        if (userInfo.profileGroupId != -10000) {
            serializer.attributeInt((String) null, ATTR_PROFILE_GROUP_ID, userInfo.profileGroupId);
        }
        serializer.attributeInt((String) null, ATTR_PROFILE_BADGE, userInfo.profileBadge);
        if (userInfo.restrictedProfileParentId != -10000) {
            serializer.attributeInt((String) null, ATTR_RESTRICTED_PROFILE_PARENT_ID, userInfo.restrictedProfileParentId);
        }
        if (userData.persistSeedData) {
            if (userData.seedAccountName != null) {
                serializer.attribute((String) null, ATTR_SEED_ACCOUNT_NAME, userData.seedAccountName);
            }
            if (userData.seedAccountType != null) {
                serializer.attribute((String) null, ATTR_SEED_ACCOUNT_TYPE, userData.seedAccountType);
            }
        }
        if (userInfo.name != null) {
            serializer.startTag((String) null, "name");
            serializer.text(userInfo.name);
            serializer.endTag((String) null, "name");
        }
        synchronized (this.mRestrictionsLock) {
            UserRestrictionsUtils.writeRestrictions(serializer, this.mBaseUserRestrictions.getRestrictions(userInfo.id), TAG_RESTRICTIONS);
            getDevicePolicyLocalRestrictionsForTargetUserLR(userInfo.id).writeRestrictions(serializer, TAG_DEVICE_POLICY_LOCAL_RESTRICTIONS);
            UserRestrictionsUtils.writeRestrictions(serializer, this.mDevicePolicyGlobalUserRestrictions.getRestrictions(userInfo.id), TAG_DEVICE_POLICY_GLOBAL_RESTRICTIONS);
        }
        if (userData.account != null) {
            serializer.startTag((String) null, TAG_ACCOUNT);
            serializer.text(userData.account);
            serializer.endTag((String) null, TAG_ACCOUNT);
        }
        if (userData.persistSeedData && userData.seedAccountOptions != null) {
            serializer.startTag((String) null, TAG_SEED_ACCOUNT_OPTIONS);
            userData.seedAccountOptions.saveToXml(serializer);
            serializer.endTag((String) null, TAG_SEED_ACCOUNT_OPTIONS);
        }
        if (userData.getLastRequestQuietModeEnabledMillis() != 0) {
            serializer.startTag((String) null, TAG_LAST_REQUEST_QUIET_MODE_ENABLED_CALL);
            serializer.text(String.valueOf(userData.getLastRequestQuietModeEnabledMillis()));
            serializer.endTag((String) null, TAG_LAST_REQUEST_QUIET_MODE_ENABLED_CALL);
        }
        serializer.startTag((String) null, TAG_IGNORE_PREPARE_STORAGE_ERRORS);
        serializer.text(String.valueOf(userData.getIgnorePrepareStorageErrors()));
        serializer.endTag((String) null, TAG_IGNORE_PREPARE_STORAGE_ERRORS);
        serializer.endTag((String) null, TAG_USER);
        serializer.endDocument();
    }

    private void writeUserListLP() {
        int[] userIdsToWrite;
        FileOutputStream fos = null;
        AtomicFile userListFile = new AtomicFile(this.mUserListFile);
        try {
            fos = userListFile.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(fos);
            serializer.startDocument((String) null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag((String) null, "users");
            serializer.attributeInt((String) null, ATTR_NEXT_SERIAL_NO, this.mNextSerialNumber);
            serializer.attributeInt((String) null, ATTR_USER_VERSION, this.mUserVersion);
            serializer.attributeInt((String) null, ATTR_USER_TYPE_VERSION, this.mUserTypeVersion);
            serializer.startTag((String) null, TAG_GUEST_RESTRICTIONS);
            synchronized (this.mGuestRestrictions) {
                UserRestrictionsUtils.writeRestrictions(serializer, this.mGuestRestrictions, TAG_RESTRICTIONS);
            }
            serializer.endTag((String) null, TAG_GUEST_RESTRICTIONS);
            serializer.startTag((String) null, TAG_DEVICE_OWNER_USER_ID);
            serializer.attributeInt((String) null, ATTR_ID, this.mDeviceOwnerUserId);
            serializer.endTag((String) null, TAG_DEVICE_OWNER_USER_ID);
            synchronized (this.mUsersLock) {
                userIdsToWrite = new int[this.mUsers.size()];
                for (int i = 0; i < userIdsToWrite.length; i++) {
                    UserInfo user = this.mUsers.valueAt(i).info;
                    userIdsToWrite[i] = user.id;
                }
            }
            for (int id : userIdsToWrite) {
                serializer.startTag((String) null, TAG_USER);
                serializer.attributeInt((String) null, ATTR_ID, id);
                serializer.endTag((String) null, TAG_USER);
            }
            serializer.endTag((String) null, "users");
            serializer.endDocument();
            userListFile.finishWrite(fos);
        } catch (Exception e) {
            userListFile.failWrite(fos);
            Slog.e(LOG_TAG, "Error writing user list");
        }
    }

    private UserData readUserLP(int id) {
        FileInputStream fis = null;
        try {
            try {
                try {
                    AtomicFile userFile = new AtomicFile(new File(this.mUsersDir, Integer.toString(id) + XML_SUFFIX));
                    fis = userFile.openRead();
                    return readUserLP(id, fis);
                } catch (IOException e) {
                    Slog.e(LOG_TAG, "Error reading user list");
                    IoUtils.closeQuietly(fis);
                    return null;
                }
            } catch (XmlPullParserException e2) {
                Slog.e(LOG_TAG, "Error reading user list");
                IoUtils.closeQuietly(fis);
                return null;
            }
        } finally {
            IoUtils.closeQuietly(fis);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:107:0x0320
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3707=6] */
    com.android.server.pm.UserManagerService.UserData readUserLP(int r55, java.io.InputStream r56) throws java.io.IOException, org.xmlpull.v1.XmlPullParserException {
        /*
            r54 = this;
            r1 = r54
            r8 = r55
            r0 = 0
            r2 = 0
            r3 = r55
            r4 = 0
            r5 = 0
            r6 = 0
            r9 = 0
            r11 = 0
            r13 = 0
            r7 = 0
            r15 = -10000(0xffffffffffffd8f0, float:NaN)
            r16 = 0
            r17 = -10000(0xffffffffffffd8f0, float:NaN)
            r18 = 0
            r19 = 0
            r20 = 0
            r21 = 0
            r22 = 0
            r23 = 0
            r24 = 0
            r25 = 0
            r26 = 0
            r27 = 0
            r28 = 0
            r29 = 0
            r30 = 1
            r31 = r9
            android.util.TypedXmlPullParser r9 = android.util.Xml.resolvePullParser(r56)
        L38:
            int r10 = r9.next()
            r33 = r10
            r34 = r0
            r0 = 2
            if (r10 == r0) goto L4b
            r10 = r33
            r0 = 1
            if (r10 == r0) goto L4d
            r0 = r34
            goto L38
        L4b:
            r10 = r33
        L4d:
            r0 = 2
            if (r10 == r0) goto L6e
            java.lang.String r0 = "UserManagerService"
            r36 = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r37 = r3
            java.lang.String r3 = "Unable to read user "
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r8)
            java.lang.String r2 = r2.toString()
            android.util.Slog.e(r0, r2)
            r0 = 0
            return r0
        L6e:
            r36 = r2
            r37 = r3
            r0 = 0
            r2 = 2
            if (r10 != r2) goto L230
            java.lang.String r2 = r9.getName()
            java.lang.String r3 = "user"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L230
            java.lang.String r2 = "id"
            r3 = -1
            int r2 = r9.getAttributeInt(r0, r2, r3)
            if (r2 == r8) goto L95
            java.lang.String r3 = "UserManagerService"
            java.lang.String r0 = "User id does not match the file name"
            android.util.Slog.e(r3, r0)
            r0 = 0
            return r0
        L95:
            java.lang.String r3 = "serialNumber"
            int r3 = r9.getAttributeInt(r0, r3, r8)
            r33 = r2
            java.lang.String r2 = "flags"
            r37 = r3
            r3 = 0
            int r2 = r9.getAttributeInt(r0, r2, r3)
            java.lang.String r3 = "type"
            java.lang.String r3 = r9.getAttributeValue(r0, r3)
            if (r3 == 0) goto Lb5
            java.lang.String r0 = r3.intern()
            goto Lb6
        Lb5:
            r0 = 0
        Lb6:
            java.lang.String r3 = "icon"
            r34 = r0
            r0 = 0
            java.lang.String r6 = r9.getAttributeValue(r0, r3)
            java.lang.String r3 = "created"
            r35 = r4
            r39 = r5
            r4 = 0
            long r31 = r9.getAttributeLong(r0, r3, r4)
            java.lang.String r3 = "lastLoggedIn"
            long r11 = r9.getAttributeLong(r0, r3, r4)
            java.lang.String r3 = "lastLoggedInFingerprint"
            java.lang.String r7 = r9.getAttributeValue(r0, r3)
            java.lang.String r3 = "profileGroupId"
            r4 = -10000(0xffffffffffffd8f0, float:NaN)
            int r15 = r9.getAttributeInt(r0, r3, r4)
            java.lang.String r3 = "profileBadge"
            r5 = 0
            int r16 = r9.getAttributeInt(r0, r3, r5)
            java.lang.String r3 = "restrictedProfileParentId"
            int r17 = r9.getAttributeInt(r0, r3, r4)
            java.lang.String r3 = "partial"
            boolean r18 = r9.getAttributeBoolean(r0, r3, r5)
            java.lang.String r3 = "preCreated"
            boolean r19 = r9.getAttributeBoolean(r0, r3, r5)
            java.lang.String r3 = "convertedFromPreCreated"
            boolean r20 = r9.getAttributeBoolean(r0, r3, r5)
            java.lang.String r3 = "guestToRemove"
            boolean r21 = r9.getAttributeBoolean(r0, r3, r5)
            java.lang.String r3 = "seedAccountName"
            java.lang.String r23 = r9.getAttributeValue(r0, r3)
            java.lang.String r3 = "seedAccountType"
            java.lang.String r24 = r9.getAttributeValue(r0, r3)
            if (r23 != 0) goto L11b
            if (r24 == 0) goto L11d
        L11b:
            r22 = 1
        L11d:
            int r0 = r9.getDepth()
            r4 = r35
            r5 = r39
        L125:
            int r3 = r9.next()
            r10 = r3
            r38 = r2
            r2 = 1
            if (r3 == r2) goto L1fb
            r3 = 3
            if (r10 != r3) goto L138
            int r2 = r9.getDepth()
            if (r2 <= r0) goto L1fb
        L138:
            if (r10 == r3) goto L1f7
            r2 = 4
            if (r10 != r2) goto L13f
            goto L1f7
        L13f:
            java.lang.String r3 = r9.getName()
            java.lang.String r2 = "name"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L15d
            int r2 = r9.next()
            r10 = 4
            if (r2 != r10) goto L15a
            java.lang.String r4 = r9.getText()
            r10 = r2
            goto L1f3
        L15a:
            r10 = r2
            goto L1f3
        L15d:
            java.lang.String r2 = "restrictions"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L16c
            android.os.Bundle r26 = com.android.server.pm.UserRestrictionsUtils.readRestrictions(r9)
            goto L1f3
        L16c:
            java.lang.String r2 = "device_policy_restrictions"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L17a
            android.os.Bundle r27 = com.android.server.pm.UserRestrictionsUtils.readRestrictions(r9)
            goto L1f3
        L17a:
            java.lang.String r2 = "device_policy_local_restrictions"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L18a
            java.lang.String r2 = "device_policy_local_restrictions"
            com.android.server.pm.RestrictionsSet r28 = com.android.server.pm.RestrictionsSet.readRestrictions(r9, r2)
            goto L1f3
        L18a:
            java.lang.String r2 = "device_policy_global_restrictions"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L197
            android.os.Bundle r29 = com.android.server.pm.UserRestrictionsUtils.readRestrictions(r9)
            goto L1f3
        L197:
            java.lang.String r2 = "account"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L1ae
            int r2 = r9.next()
            r10 = 4
            if (r2 != r10) goto L1ac
            java.lang.String r5 = r9.getText()
            r10 = r2
            goto L1f3
        L1ac:
            r10 = r2
            goto L1f3
        L1ae:
            java.lang.String r2 = "seedAccountOptions"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L1be
            android.os.PersistableBundle r25 = android.os.PersistableBundle.restoreFromXml(r9)
            r22 = 1
            goto L1f3
        L1be:
            java.lang.String r2 = "lastRequestQuietModeEnabledCall"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L1d9
            int r2 = r9.next()
            r10 = 4
            if (r2 != r10) goto L1d7
            java.lang.String r10 = r9.getText()
            long r13 = java.lang.Long.parseLong(r10)
            r10 = r2
            goto L1f3
        L1d7:
            r10 = r2
            goto L1f3
        L1d9:
            java.lang.String r2 = "ignorePrepareStorageErrors"
            boolean r2 = r2.equals(r3)
            if (r2 == 0) goto L1f3
            int r2 = r9.next()
            r10 = 4
            if (r2 != r10) goto L1f2
            java.lang.String r10 = r9.getText()
            boolean r30 = java.lang.Boolean.parseBoolean(r10)
            r10 = r2
            goto L1f3
        L1f2:
            r10 = r2
        L1f3:
            r2 = r38
            goto L125
        L1f7:
            r2 = r38
            goto L125
        L1fb:
            r35 = r4
            r33 = r10
            r3 = r15
            r2 = r16
            r8 = r18
            r1 = r19
            r40 = r22
            r41 = r23
            r42 = r24
            r43 = r25
            r44 = r26
            r45 = r27
            r46 = r28
            r47 = r29
            r36 = r34
            r10 = r37
            r34 = r38
            r16 = r9
            r18 = r17
            r9 = r21
            r17 = r5
            r4 = r13
            r14 = r31
            r13 = r7
            r52 = r11
            r12 = r6
            r6 = r52
            r11 = r20
            goto L262
        L230:
            r35 = r4
            r39 = r5
            r33 = r10
            r4 = r13
            r3 = r15
            r2 = r16
            r8 = r18
            r1 = r19
            r40 = r22
            r41 = r23
            r42 = r24
            r43 = r25
            r44 = r26
            r45 = r27
            r46 = r28
            r47 = r29
            r14 = r31
            r10 = r37
            r13 = r7
            r16 = r9
            r18 = r17
            r9 = r21
            r17 = r39
            r52 = r11
            r12 = r6
            r6 = r52
            r11 = r20
        L262:
            android.content.pm.UserInfo r0 = new android.content.pm.UserInfo
            r48 = r2
            r2 = r0
            r49 = r3
            r3 = r55
            r50 = r4
            r4 = r35
            r5 = r12
            r20 = r11
            r19 = r12
            r11 = r6
            r6 = r34
            r7 = r36
            r2.<init>(r3, r4, r5, r6, r7)
            r2.serialNumber = r10
            r2.creationTime = r14
            r2.lastLoggedInTime = r11
            r2.lastLoggedInFingerprint = r13
            r2.partial = r8
            r2.preCreated = r1
            r3 = r20
            r2.convertedFromPreCreated = r3
            r2.guestToRemove = r9
            r4 = r49
            r2.profileGroupId = r4
            r5 = r48
            r2.profileBadge = r5
            r6 = r18
            r2.restrictedProfileParentId = r6
            com.android.server.pm.UserManagerService$UserData r0 = new com.android.server.pm.UserManagerService$UserData
            r0.<init>()
            r7 = r0
            r7.info = r2
            r18 = r1
            r1 = r17
            r7.account = r1
            r1 = r41
            r7.seedAccountName = r1
            r20 = r1
            r1 = r42
            r7.seedAccountType = r1
            r21 = r1
            r1 = r40
            r7.persistSeedData = r1
            r22 = r1
            r1 = r43
            r7.seedAccountOptions = r1
            r24 = r1
            r23 = r2
            r1 = r50
            r7.setLastRequestQuietModeEnabledMillis(r1)
            if (r30 == 0) goto L2cc
            r7.setIgnorePrepareStorageErrors()
        L2cc:
            r50 = r1
            r1 = r54
            java.lang.Object r2 = r1.mRestrictionsLock
            monitor-enter(r2)
            r25 = r3
            r3 = r44
            if (r3 == 0) goto L301
            com.android.server.pm.RestrictionsSet r0 = r1.mBaseUserRestrictions     // Catch: java.lang.Throwable -> L2f0
            r26 = r8
            r8 = r55
            r0.updateRestrictions(r8, r3)     // Catch: java.lang.Throwable -> L2e3
            goto L305
        L2e3:
            r0 = move-exception
            r27 = r3
            r49 = r4
            r3 = r45
            r28 = r46
            r4 = r47
            goto L362
        L2f0:
            r0 = move-exception
            r26 = r8
            r8 = r55
            r27 = r3
            r49 = r4
            r3 = r45
            r28 = r46
            r4 = r47
            goto L362
        L301:
            r26 = r8
            r8 = r55
        L305:
            r27 = r3
            r3 = r46
            if (r3 == 0) goto L333
            android.util.SparseArray<com.android.server.pm.RestrictionsSet> r0 = r1.mDevicePolicyLocalUserRestrictions     // Catch: java.lang.Throwable -> L329
            r0.put(r8, r3)     // Catch: java.lang.Throwable -> L329
            r28 = r3
            r3 = r45
            if (r3 == 0) goto L326
            java.lang.String r0 = "UserManagerService"
            r49 = r4
            java.lang.String r4 = "Seeing both legacy and current local restrictions in xml"
            android.util.Slog.wtf(r0, r4)     // Catch: java.lang.Throwable -> L353
            goto L357
        L320:
            r0 = move-exception
            r49 = r4
            r4 = r47
            goto L362
        L326:
            r49 = r4
            goto L357
        L329:
            r0 = move-exception
            r28 = r3
            r49 = r4
            r3 = r45
            r4 = r47
            goto L362
        L333:
            r28 = r3
            r49 = r4
            r3 = r45
            if (r3 == 0) goto L357
            boolean r0 = r3.isEmpty()     // Catch: java.lang.Throwable -> L353
            if (r0 == 0) goto L347
            com.android.server.pm.RestrictionsSet r0 = new com.android.server.pm.RestrictionsSet     // Catch: java.lang.Throwable -> L353
            r0.<init>()     // Catch: java.lang.Throwable -> L353
            goto L34c
        L347:
            com.android.server.pm.RestrictionsSet r0 = new com.android.server.pm.RestrictionsSet     // Catch: java.lang.Throwable -> L353
            r0.<init>(r8, r3)     // Catch: java.lang.Throwable -> L353
        L34c:
            android.util.SparseArray<com.android.server.pm.RestrictionsSet> r4 = r1.mDevicePolicyLocalUserRestrictions     // Catch: java.lang.Throwable -> L353
            r4.put(r8, r0)     // Catch: java.lang.Throwable -> L353
            goto L357
        L353:
            r0 = move-exception
            r4 = r47
            goto L362
        L357:
            r4 = r47
            if (r4 == 0) goto L360
            com.android.server.pm.RestrictionsSet r0 = r1.mDevicePolicyGlobalUserRestrictions     // Catch: java.lang.Throwable -> L364
            r0.updateRestrictions(r8, r4)     // Catch: java.lang.Throwable -> L364
        L360:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L364
            return r7
        L362:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L364
            throw r0
        L364:
            r0 = move-exception
            goto L362
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.UserManagerService.readUserLP(int, java.io.InputStream):com.android.server.pm.UserManagerService$UserData");
    }

    private static boolean cleanAppRestrictionsForPackageLAr(String pkg, int userId) {
        File dir = Environment.getUserSystemDirectory(userId);
        File resFile = new File(dir, packageToRestrictionsFileName(pkg));
        if (resFile.exists()) {
            resFile.delete();
            return true;
        }
        return false;
    }

    public UserInfo createProfileForUserWithThrow(String name, String userType, int flags, int userId, String[] disallowedPackages) throws ServiceSpecificException {
        checkCreateUsersPermission(flags);
        try {
            return createUserInternal(name, userType, flags, userId, disallowedPackages);
        } catch (UserManager.CheckedUserOperationException e) {
            throw e.toServiceSpecificException();
        }
    }

    public UserInfo createProfileForUserEvenWhenDisallowedWithThrow(String name, String userType, int flags, int userId, String[] disallowedPackages) throws ServiceSpecificException {
        checkCreateUsersPermission(flags);
        try {
            return createUserInternalUnchecked(name, userType, flags, userId, false, disallowedPackages, null);
        } catch (UserManager.CheckedUserOperationException e) {
            throw e.toServiceSpecificException();
        }
    }

    public UserInfo createUserWithThrow(String name, String userType, int flags) throws ServiceSpecificException {
        checkCreateUsersPermission(flags);
        try {
            return createUserInternal(name, userType, flags, -10000, null);
        } catch (UserManager.CheckedUserOperationException e) {
            throw e.toServiceSpecificException();
        }
    }

    public UserInfo preCreateUserWithThrow(String userType) throws ServiceSpecificException {
        UserTypeDetails userTypeDetails = this.mUserTypes.get(userType);
        int flags = userTypeDetails != null ? userTypeDetails.getDefaultUserInfoFlags() : 0;
        checkCreateUsersPermission(flags);
        Preconditions.checkArgument(isUserTypeEligibleForPreCreation(userTypeDetails), "cannot pre-create user of type " + userType);
        Slog.i(LOG_TAG, "Pre-creating user of type " + userType);
        try {
            return createUserInternalUnchecked(null, userType, flags, -10000, true, null, null);
        } catch (UserManager.CheckedUserOperationException e) {
            throw e.toServiceSpecificException();
        }
    }

    public UserHandle createUserWithAttributes(String userName, String userType, int flags, Bitmap userIcon, String accountName, String accountType, PersistableBundle accountOptions) {
        checkCreateUsersPermission(flags);
        if (someUserHasAccountNoChecks(accountName, accountType)) {
            throw new ServiceSpecificException(7);
        }
        try {
            UserInfo userInfo = createUserInternal(userName, userType, flags, -10000, null);
            if (userInfo == null) {
                throw new ServiceSpecificException(1);
            }
            if (userIcon != null) {
                this.mLocalService.setUserIcon(userInfo.id, userIcon);
            }
            setSeedAccountDataNoChecks(userInfo.id, accountName, accountType, accountOptions, true);
            return userInfo.getUserHandle();
        } catch (UserManager.CheckedUserOperationException e) {
            throw e.toServiceSpecificException();
        }
    }

    private UserInfo createUserInternal(String name, String userType, int flags, int parentId, String[] disallowedPackages) throws UserManager.CheckedUserOperationException {
        String restriction = "no_add_user";
        if (UserManager.isUserTypeCloneProfile(userType)) {
            restriction = "no_add_clone_profile";
        } else if (UserManager.isUserTypeManagedProfile(userType)) {
            restriction = "no_add_managed_profile";
        }
        enforceUserRestriction(restriction, UserHandle.getCallingUserId(), "Cannot add user");
        return createUserInternalUnchecked(name, userType, flags, parentId, false, disallowedPackages, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserInfo createUserInternalUnchecked(String name, String userType, int flags, int parentId, boolean preCreate, String[] disallowedPackages, Object token) throws UserManager.CheckedUserOperationException {
        int nextProbableUserId = getNextAvailableId();
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("createUser-" + flags);
        long sessionId = logUserCreateJourneyBegin(nextProbableUserId, userType, flags);
        try {
            UserInfo newUser = createUserInternalUncheckedNoTracing(name, userType, flags, parentId, preCreate, disallowedPackages, t, token);
            logUserCreateJourneyFinish(sessionId, nextProbableUserId, newUser != null);
            t.traceEnd();
            return newUser;
        } catch (Throwable th) {
            logUserCreateJourneyFinish(sessionId, nextProbableUserId, 0 != 0);
            t.traceEnd();
            throw th;
        }
    }

    private int checkAndGetDualProfileUserId(int defUserId, boolean isUserTypeDualProfile) {
        if (isUserTypeDualProfile) {
            return 999;
        }
        if (defUserId == 999) {
            return defUserId + 1;
        }
        return defUserId;
    }

    private int checkDualProfileCount(int userId) {
        int aliveUserCount = 0;
        int totalUserCount = this.mUsers.size();
        for (int i = 0; i < totalUserCount; i++) {
            UserInfo user = this.mUsers.valueAt(i).info;
            if (!this.mRemovingUserIds.get(user.id) && user.isDualProfile()) {
                aliveUserCount++;
            }
        }
        return aliveUserCount;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4132=4, 4049=4, 4067=5] */
    private UserInfo createUserInternalUncheckedNoTracing(String name, String userType, int flags, int parentId, boolean preCreate, String[] disallowedPackages, TimingsTraceAndSlog t, Object token) throws UserManager.CheckedUserOperationException {
        Object obj;
        UserData parent;
        UserData parent2;
        Object obj2;
        UserTypeDetails userTypeDetails = this.mUserTypes.get(userType);
        if (userTypeDetails == null) {
            Slog.e(LOG_TAG, "Cannot create user of invalid user type: " + userType);
            return null;
        }
        String userType2 = userType.intern();
        int flags2 = flags | userTypeDetails.getDefaultUserInfoFlags();
        if (!checkUserTypeConsistency(flags2)) {
            Slog.e(LOG_TAG, "Cannot add user. Flags (" + Integer.toHexString(flags2) + ") and userTypeDetails (" + userType2 + ") are inconsistent.");
            return null;
        } else if ((flags2 & 2048) != 0) {
            Slog.e(LOG_TAG, "Cannot add user. Flags (" + Integer.toHexString(flags2) + ") indicated SYSTEM user, which cannot be created.");
            return null;
        } else {
            if (!userTypeDetails.isEnabled()) {
                throwCheckedUserOperationException("Cannot add a user of disabled type " + userType2 + ".", 6);
            }
            synchronized (this.mUsersLock) {
                try {
                    if (this.mForceEphemeralUsers) {
                        flags2 |= 256;
                    }
                } catch (Throwable th) {
                    th = th;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    throw th;
                }
            }
            if (!preCreate && parentId < 0 && isUserTypeEligibleForPreCreation(userTypeDetails)) {
                UserInfo preCreatedUser = convertPreCreatedUserIfPossible(userType2, flags2, name, token);
                if (preCreatedUser != null) {
                    return preCreatedUser;
                }
            }
            DeviceStorageMonitorInternal dsm = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
            if (dsm.isMemoryLow()) {
                throwCheckedUserOperationException("Cannot add user. Not enough space on disk.", 5);
            }
            boolean isProfile = userTypeDetails.isProfile();
            boolean isGuest = UserManager.isUserTypeGuest(userType2);
            boolean isRestricted = UserManager.isUserTypeRestricted(userType2);
            boolean isDemo = UserManager.isUserTypeDemo(userType2);
            boolean isManagedProfile = UserManager.isUserTypeManagedProfile(userType2);
            long ident = Binder.clearCallingIdentity();
            try {
                Object obj3 = this.mPackagesLock;
                synchronized (obj3) {
                    try {
                        try {
                            if (parentId != -10000) {
                                try {
                                    synchronized (this.mUsersLock) {
                                        parent = getUserDataLU(parentId);
                                    }
                                    if (parent == null) {
                                        throwCheckedUserOperationException("Cannot find user data for parent user " + parentId, 1);
                                    }
                                    parent2 = parent;
                                } catch (Throwable th3) {
                                    th = th3;
                                    obj = obj3;
                                    throw th;
                                }
                            } else {
                                parent2 = null;
                            }
                            if (!preCreate && !canAddMoreUsersOfType(userTypeDetails)) {
                                throwCheckedUserOperationException("Cannot add more users of type " + userType2 + ". Maximum number of that type already exists.", 6);
                            }
                            if (UserManager.isUserTypeDualProfile(userType2) && this.mRemovingUserIds.get(999)) {
                                throwCheckedUserOperationException("Cannot add dual profile when removing " + userType2 + " for user " + parentId, 1);
                            }
                            if (!UserManager.isUserTypeDualProfile(userType2) && !isGuest && !isManagedProfile && !isDemo && isUserLimitReached()) {
                                throwCheckedUserOperationException("Cannot add user. Maximum user limit is reached.", 6);
                            }
                            if (isProfile && !canAddMoreProfilesToUser(userType2, parentId, false)) {
                                throwCheckedUserOperationException("Cannot add more profiles of type " + userType2 + " for user " + parentId, 6);
                            }
                            if (isRestricted && !UserManager.isSplitSystemUser() && parentId != 0) {
                                throwCheckedUserOperationException("Cannot add restricted profile - parent user must be owner", 1);
                            }
                            if (isRestricted && UserManager.isSplitSystemUser()) {
                                if (parent2 == null) {
                                    throwCheckedUserOperationException("Cannot add restricted profile - parent user must be specified", 1);
                                }
                                if (!parent2.info.canHaveProfile()) {
                                    throwCheckedUserOperationException("Cannot add restricted profile - profiles cannot be created for the specified parent user id " + parentId, 1);
                                }
                            }
                            int userId = checkAndGetDualProfileUserId(getNextAvailableId(), UserManager.isUserTypeDualProfile(userType2));
                            Slog.i(LOG_TAG, "Creating user " + userId + " of type " + userType2);
                            Environment.getUserSystemDirectory(userId).mkdirs();
                            Object obj4 = this.mUsersLock;
                            synchronized (obj4) {
                                if (parent2 != null) {
                                    try {
                                        if (parent2.info.isEphemeral()) {
                                            flags2 |= 256;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                        obj2 = obj4;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th5) {
                                                th = th5;
                                            }
                                        }
                                        throw th;
                                    }
                                }
                                if (preCreate) {
                                    flags2 &= -257;
                                }
                                try {
                                    UserData parent3 = parent2;
                                    obj2 = obj4;
                                    obj = obj3;
                                    try {
                                        UserInfo userInfo = new UserInfo(userId, name, (String) null, flags2, userType2);
                                        int i = this.mNextSerialNumber;
                                        this.mNextSerialNumber = i + 1;
                                        userInfo.serialNumber = i;
                                        userInfo.creationTime = getCreationTime();
                                        userInfo.partial = true;
                                        userInfo.preCreated = preCreate;
                                        userInfo.lastLoggedInFingerprint = PackagePartitions.FINGERPRINT;
                                        if (userTypeDetails.hasBadge() && parentId != -10000) {
                                            try {
                                                userInfo.profileBadge = getFreeProfileBadgeLU(parentId, userType2);
                                            } catch (Throwable th6) {
                                                th = th6;
                                                while (true) {
                                                    break;
                                                    break;
                                                }
                                                throw th;
                                            }
                                        }
                                        UserData userData = new UserData();
                                        userData.info = userInfo;
                                        this.mUsers.put(userId, userData);
                                        try {
                                            writeUserLP(userData);
                                            writeUserListLP();
                                            if (parent3 != null) {
                                                if (isProfile) {
                                                    if (parent3.info.profileGroupId == -10000) {
                                                        parent3.info.profileGroupId = parent3.info.id;
                                                        writeUserLP(parent3);
                                                    }
                                                    userInfo.profileGroupId = parent3.info.profileGroupId;
                                                } else if (isRestricted) {
                                                    if (parent3.info.restrictedProfileParentId == -10000) {
                                                        parent3.info.restrictedProfileParentId = parent3.info.id;
                                                        writeUserLP(parent3);
                                                    }
                                                    userInfo.restrictedProfileParentId = parent3.info.restrictedProfileParentId;
                                                }
                                            }
                                            try {
                                                t.traceBegin("createUserKey");
                                                StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
                                                if (Build.IS_DEBUG_ENABLE) {
                                                    Slog.i(LOG_TAG, "create User Key  for user：" + userId);
                                                }
                                                storage.createUserKey(userId, userInfo.serialNumber, userInfo.isEphemeral());
                                                t.traceEnd();
                                                t.traceBegin("prepareUserData");
                                                this.mUserDataPreparer.prepareUserData(userId, userInfo.serialNumber, 3);
                                                t.traceEnd();
                                                Set<String> userTypeInstallablePackages = this.mSystemPackageInstaller.getInstallablePackagesForUserType(userType2);
                                                t.traceBegin("PM.createNewUser");
                                                try {
                                                    this.mPm.createNewUser(userId, userTypeInstallablePackages, disallowedPackages);
                                                    t.traceEnd();
                                                    userInfo.partial = false;
                                                    synchronized (this.mPackagesLock) {
                                                        writeUserLP(userData);
                                                    }
                                                    updateUserIds();
                                                    Bundle restrictions = new Bundle();
                                                    if (isGuest) {
                                                        synchronized (this.mGuestRestrictions) {
                                                            restrictions.putAll(this.mGuestRestrictions);
                                                        }
                                                    } else {
                                                        userTypeDetails.addDefaultRestrictionsTo(restrictions);
                                                    }
                                                    synchronized (this.mRestrictionsLock) {
                                                        this.mBaseUserRestrictions.updateRestrictions(userId, restrictions);
                                                    }
                                                    t.traceBegin("PM.onNewUserCreated-" + userId);
                                                    this.mPm.onNewUserCreated(userId, false);
                                                    t.traceEnd();
                                                    applyDefaultUserSettings(userTypeDetails, userId);
                                                    setDefaultCrossProfileIntentFilters(userId, userTypeDetails, restrictions, parentId);
                                                    if (preCreate) {
                                                        Slog.i(LOG_TAG, "starting pre-created user " + userInfo.toFullString());
                                                        IActivityManager am = ActivityManager.getService();
                                                        try {
                                                            am.startUserInBackground(userId);
                                                        } catch (RemoteException e) {
                                                            Slog.w(LOG_TAG, "could not start pre-created user " + userId, e);
                                                        }
                                                    } else {
                                                        dispatchUserAdded(userInfo, token);
                                                    }
                                                    Binder.restoreCallingIdentity(ident);
                                                    return userInfo;
                                                } catch (Throwable th7) {
                                                    th = th7;
                                                    Binder.restoreCallingIdentity(ident);
                                                    throw th;
                                                }
                                            } catch (Throwable th8) {
                                                th = th8;
                                            }
                                        } catch (Throwable th9) {
                                            th = th9;
                                            throw th;
                                        }
                                    } catch (Throwable th10) {
                                        th = th10;
                                    }
                                } catch (Throwable th11) {
                                    th = th11;
                                    obj2 = obj4;
                                }
                            }
                        } catch (Throwable th12) {
                            th = th12;
                        }
                    } catch (Throwable th13) {
                        th = th13;
                    }
                }
            } catch (Throwable th14) {
                th = th14;
            }
        }
    }

    private void applyDefaultUserSettings(UserTypeDetails userTypeDetails, int userId) {
        Bundle systemSettings = userTypeDetails.getDefaultSystemSettings();
        Bundle secureSettings = userTypeDetails.getDefaultSecureSettings();
        if (systemSettings.isEmpty() && secureSettings.isEmpty()) {
            return;
        }
        int systemSettingsSize = systemSettings.size();
        String[] systemSettingsArray = (String[]) systemSettings.keySet().toArray(new String[systemSettingsSize]);
        for (int i = 0; i < systemSettingsSize; i++) {
            String setting = systemSettingsArray[i];
            if (!Settings.System.putStringForUser(this.mContext.getContentResolver(), setting, systemSettings.getString(setting), userId)) {
                Slog.e(LOG_TAG, "Failed to insert default system setting: " + setting);
            }
        }
        int secureSettingsSize = secureSettings.size();
        String[] secureSettingsArray = (String[]) secureSettings.keySet().toArray(new String[secureSettingsSize]);
        for (int i2 = 0; i2 < secureSettingsSize; i2++) {
            String setting2 = secureSettingsArray[i2];
            if (!Settings.Secure.putStringForUser(this.mContext.getContentResolver(), setting2, secureSettings.getString(setting2), userId)) {
                Slog.e(LOG_TAG, "Failed to insert default secure setting: " + setting2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDefaultCrossProfileIntentFilters(int profileUserId, UserTypeDetails profileDetails, Bundle profileRestrictions, int parentUserId) {
        if (profileDetails != null && profileDetails.isProfile()) {
            List<DefaultCrossProfileIntentFilter> filters = profileDetails.getDefaultCrossProfileIntentFilters();
            if (filters.isEmpty()) {
                return;
            }
            boolean disallowSharingIntoProfile = profileRestrictions.getBoolean("no_sharing_into_profile", false);
            int size = profileDetails.getDefaultCrossProfileIntentFilters().size();
            for (int i = 0; i < size; i++) {
                DefaultCrossProfileIntentFilter filter = profileDetails.getDefaultCrossProfileIntentFilters().get(i);
                if (!disallowSharingIntoProfile || !filter.letsPersonalDataIntoProfile) {
                    if (filter.direction == 0) {
                        PackageManagerService packageManagerService = this.mPm;
                        packageManagerService.addCrossProfileIntentFilter(packageManagerService.snapshotComputer(), filter.filter, this.mContext.getOpPackageName(), profileUserId, parentUserId, filter.flags);
                    } else {
                        PackageManagerService packageManagerService2 = this.mPm;
                        packageManagerService2.addCrossProfileIntentFilter(packageManagerService2.snapshotComputer(), filter.filter, this.mContext.getOpPackageName(), parentUserId, profileUserId, filter.flags);
                    }
                }
            }
        }
    }

    private UserInfo convertPreCreatedUserIfPossible(String userType, int flags, String name, final Object token) {
        UserData preCreatedUserData;
        synchronized (this.mUsersLock) {
            preCreatedUserData = getPreCreatedUserLU(userType);
        }
        if (preCreatedUserData == null) {
            return null;
        }
        synchronized (this.mUserStates) {
            if (this.mUserStates.has(preCreatedUserData.info.id)) {
                Slog.w(LOG_TAG, "Cannot reuse pre-created user " + preCreatedUserData.info.id + " because it didn't stop yet");
                return null;
            }
            final UserInfo preCreatedUser = preCreatedUserData.info;
            int newFlags = preCreatedUser.flags | flags;
            if (!checkUserTypeConsistency(newFlags)) {
                Slog.wtf(LOG_TAG, "Cannot reuse pre-created user " + preCreatedUser.id + " of type " + userType + " because flags are inconsistent. Flags (" + Integer.toHexString(flags) + "); preCreatedUserFlags ( " + Integer.toHexString(preCreatedUser.flags) + ").");
                return null;
            }
            Slog.i(LOG_TAG, "Reusing pre-created user " + preCreatedUser.id + " of type " + userType + " and bestowing on it flags " + UserInfo.flagsToString(flags));
            preCreatedUser.name = name;
            preCreatedUser.flags = newFlags;
            preCreatedUser.preCreated = false;
            preCreatedUser.convertedFromPreCreated = true;
            preCreatedUser.creationTime = getCreationTime();
            synchronized (this.mPackagesLock) {
                writeUserLP(preCreatedUserData);
                writeUserListLP();
            }
            updateUserIds();
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.UserManagerService$$ExternalSyntheticLambda2
                public final void runOrThrow() {
                    UserManagerService.this.m5744x11e50e25(preCreatedUser, token);
                }
            });
            return preCreatedUser;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$convertPreCreatedUserIfPossible$2$com-android-server-pm-UserManagerService  reason: not valid java name */
    public /* synthetic */ void m5744x11e50e25(UserInfo preCreatedUser, Object token) throws Exception {
        this.mPm.onNewUserCreated(preCreatedUser.id, true);
        dispatchUserAdded(preCreatedUser, token);
    }

    static boolean checkUserTypeConsistency(int flags) {
        return isAtMostOneFlag(flags & 4620) && isAtMostOneFlag(flags & 5120) && isAtMostOneFlag(flags & 6144);
    }

    private static boolean isAtMostOneFlag(int flags) {
        return ((flags + (-1)) & flags) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean installWhitelistedSystemPackages(boolean isFirstBoot, boolean isUpgrade, ArraySet<String> existingPackages) {
        return this.mSystemPackageInstaller.installWhitelistedSystemPackages(isFirstBoot, isUpgrade, existingPackages);
    }

    public String[] getPreInstallableSystemPackages(String userType) {
        checkCreateUsersPermission("getPreInstallableSystemPackages");
        Set<String> installableSystemPackages = this.mSystemPackageInstaller.getInstallablePackagesForUserType(userType);
        if (installableSystemPackages == null) {
            return null;
        }
        return (String[]) installableSystemPackages.toArray(new String[installableSystemPackages.size()]);
    }

    private long getCreationTime() {
        long now = System.currentTimeMillis();
        if (now > EPOCH_PLUS_30_YEARS) {
            return now;
        }
        return 0L;
    }

    private void dispatchUserAdded(UserInfo userInfo, Object token) {
        String str;
        synchronized (this.mUserLifecycleListeners) {
            for (int i = 0; i < this.mUserLifecycleListeners.size(); i++) {
                this.mUserLifecycleListeners.get(i).onUserCreated(userInfo, token);
            }
        }
        Intent addedIntent = new Intent("android.intent.action.USER_ADDED");
        addedIntent.addFlags(16777216);
        addedIntent.putExtra("android.intent.extra.user_handle", userInfo.id);
        addedIntent.putExtra("android.intent.extra.USER", UserHandle.of(userInfo.id));
        this.mContext.sendBroadcastAsUser(addedIntent, UserHandle.ALL, "android.permission.MANAGE_USERS");
        Context context = this.mContext;
        if (userInfo.isGuest()) {
            str = TRON_GUEST_CREATED;
        } else {
            str = userInfo.isDemo() ? TRON_DEMO_CREATED : TRON_USER_CREATED;
        }
        MetricsLogger.count(context, str, 1);
        if (!userInfo.isProfile() && Settings.Global.getString(this.mContext.getContentResolver(), "user_switcher_enabled") == null) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "user_switcher_enabled", 1);
        }
    }

    private UserData getPreCreatedUserLU(String userType) {
        int userSize = this.mUsers.size();
        for (int i = 0; i < userSize; i++) {
            UserData user = this.mUsers.valueAt(i);
            if (user.info.preCreated && !user.info.partial && user.info.userType.equals(userType)) {
                if (!user.info.isInitialized()) {
                    Slog.w(LOG_TAG, "found pre-created user of type " + userType + ", but it's not initialized yet: " + user.info.toFullString());
                } else {
                    return user;
                }
            }
        }
        return null;
    }

    private static boolean isUserTypeEligibleForPreCreation(UserTypeDetails userTypeDetails) {
        return (userTypeDetails == null || userTypeDetails.isProfile() || userTypeDetails.getName().equals("android.os.usertype.full.RESTRICTED")) ? false : true;
    }

    private long logUserCreateJourneyBegin(int userId, String userType, int flags) {
        return logUserJourneyBegin(4, userId, userType, flags);
    }

    private void logUserCreateJourneyFinish(long sessionId, int userId, boolean finish) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.USER_LIFECYCLE_EVENT_OCCURRED, sessionId, userId, 3, finish ? 2 : 0);
    }

    private long logUserRemoveJourneyBegin(int userId, String userType, int flags) {
        return logUserJourneyBegin(6, userId, userType, flags);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logUserRemoveJourneyFinish(long sessionId, int userId, boolean finish) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.USER_LIFECYCLE_EVENT_OCCURRED, sessionId, userId, 8, finish ? 2 : 0);
    }

    private long logUserJourneyBegin(int journey, int userId, String userType, int flags) {
        int event;
        long sessionId = ThreadLocalRandom.current().nextLong(1L, JobStatus.NO_LATEST_RUNTIME);
        FrameworkStatsLog.write(264, sessionId, journey, -1, userId, UserManager.getUserTypeForStatsd(userType), flags);
        switch (journey) {
            case 4:
                event = 3;
                break;
            case 5:
            default:
                throw new IllegalArgumentException("Journey " + journey + " not expected.");
            case 6:
                event = 8;
                break;
        }
        FrameworkStatsLog.write((int) FrameworkStatsLog.USER_LIFECYCLE_EVENT_OCCURRED, sessionId, userId, event, 1);
        return sessionId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerStatsCallbacks() {
        StatsManager statsManager = (StatsManager) this.mContext.getSystemService(StatsManager.class);
        statsManager.setPullAtomCallback((int) FrameworkStatsLog.USER_INFO, (StatsManager.PullAtomMetadata) null, BackgroundThread.getExecutor(), new StatsManager.StatsPullAtomCallback() { // from class: com.android.server.pm.UserManagerService$$ExternalSyntheticLambda4
            public final int onPullAtom(int i, List list) {
                int onPullAtom;
                onPullAtom = UserManagerService.this.onPullAtom(i, list);
                return onPullAtom;
            }
        });
    }

    /* JADX DEBUG: Incorrect finally slice size: {[MOVE, CONST] complete}, expected: {[MOVE] complete} */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Finally extract failed */
    public int onPullAtom(int atomTag, List<StatsEvent> data) {
        boolean isUserRunningUnlocked;
        List<UserInfo> users;
        boolean z = true;
        if (atomTag != 10152) {
            Slogf.e(LOG_TAG, "Unexpected atom tag: %d", Integer.valueOf(atomTag));
            return 1;
        }
        List<UserInfo> users2 = getUsersInternal(true, true, true);
        int size = users2.size();
        int idx = 0;
        while (idx < size) {
            UserInfo user = users2.get(idx);
            if (user.id == 0) {
                users = users2;
            } else {
                int userTypeStandard = UserManager.getUserTypeForStatsd(user.userType);
                String userTypeCustom = userTypeStandard == 0 ? user.userType : null;
                synchronized (this.mUserStates) {
                    try {
                        isUserRunningUnlocked = this.mUserStates.get(user.id, -1) == 3 ? z : false;
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                }
                users = users2;
                data.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.USER_INFO, user.id, userTypeStandard, userTypeCustom, user.flags, user.creationTime, user.lastLoggedInTime, isUserRunningUnlocked));
            }
            idx++;
            users2 = users;
            z = true;
        }
        return 0;
    }

    UserData putUserInfo(UserInfo userInfo) {
        UserData userData = new UserData();
        userData.info = userInfo;
        synchronized (this.mUsersLock) {
            this.mUsers.put(userInfo.id, userData);
        }
        return userData;
    }

    void removeUserInfo(int userId) {
        synchronized (this.mUsersLock) {
            this.mUsers.remove(userId);
        }
    }

    public UserInfo createRestrictedProfileWithThrow(String name, int parentUserId) {
        checkCreateUsersPermission("setupRestrictedProfile");
        UserInfo user = createProfileForUserWithThrow(name, "android.os.usertype.full.RESTRICTED", 0, parentUserId, null);
        if (user == null) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            setUserRestriction("no_modify_accounts", true, user.id);
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "location_mode", 0, user.id);
            setUserRestriction("no_share_location", true, user.id);
            return user;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public UserInfo findCurrentGuestUser() {
        checkManageUsersPermission("findCurrentGuestUser");
        synchronized (this.mUsersLock) {
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserInfo user = this.mUsers.valueAt(i).info;
                if (user.isGuest() && !user.guestToRemove && !user.preCreated && !this.mRemovingUserIds.get(user.id)) {
                    return user;
                }
            }
            return null;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4590=4] */
    public boolean markGuestForDeletion(int userId) {
        checkManageUsersPermission("Only the system can remove users");
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean("no_remove_user", false)) {
            Slog.w(LOG_TAG, "Cannot remove user. DISALLOW_REMOVE_USER is enabled.");
            return false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    UserData userData = this.mUsers.get(userId);
                    if (userId != 0 && userData != null && !this.mRemovingUserIds.get(userId)) {
                        if (userData.info.isGuest()) {
                            userData.info.guestToRemove = true;
                            userData.info.flags |= 64;
                            writeUserLP(userData);
                            return true;
                        }
                        return false;
                    }
                    return false;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean removeUser(int userId) {
        Slog.i(LOG_TAG, "removeUser u" + userId);
        checkCreateUsersPermission("Only the system can remove users");
        String restriction = getUserRemovalRestriction(userId);
        if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean(restriction, false)) {
            Slog.w(LOG_TAG, "Cannot remove user. " + restriction + " is enabled.");
            return false;
        }
        return removeUserUnchecked(userId);
    }

    public boolean removeUserEvenWhenDisallowed(int userId) {
        checkCreateUsersPermission("Only the system can remove users");
        return removeUserUnchecked(userId);
    }

    private String getUserRemovalRestriction(int userId) {
        UserInfo userInfo;
        synchronized (this.mUsersLock) {
            userInfo = getUserInfoLU(userId);
        }
        boolean isManagedProfile = userInfo != null && userInfo.isManagedProfile();
        return isManagedProfile ? "no_remove_managed_profile" : "no_remove_user";
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4719=7] */
    /* JADX INFO: Access modifiers changed from: private */
    public boolean removeUserUnchecked(int userId) {
        long ident = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            if (currentUser == userId) {
                Slog.w(LOG_TAG, "Current user cannot be removed.");
                return false;
            }
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    UserData userData = this.mUsers.get(userId);
                    if (userId == 0) {
                        Slog.e(LOG_TAG, "System user cannot be removed.");
                        return false;
                    } else if (userData == null) {
                        Slog.e(LOG_TAG, String.format("Cannot remove user %d, invalid user id provided.", Integer.valueOf(userId)));
                        return false;
                    } else if (this.mRemovingUserIds.get(userId)) {
                        Slog.e(LOG_TAG, String.format("User %d is already scheduled for removal.", Integer.valueOf(userId)));
                        return false;
                    } else {
                        Slog.i(LOG_TAG, "Removing user " + userId);
                        addRemovingUserIdLocked(userId);
                        userData.info.partial = true;
                        userData.info.flags |= 64;
                        writeUserLP(userData);
                        final long sessionId = logUserRemoveJourneyBegin(userId, userData.info.userType, userData.info.flags);
                        try {
                            this.mAppOpsService.removeUser(userId);
                        } catch (RemoteException e) {
                            Slog.w(LOG_TAG, "Unable to notify AppOpsService of removing user.", e);
                        }
                        if (userData.info.profileGroupId != -10000 && (userData.info.isManagedProfile() || userData.info.isDualProfile())) {
                            sendProfileRemovedBroadcast(userData.info.profileGroupId, userData.info.id);
                        }
                        try {
                            int res = ActivityManager.getService().stopUser(userId, true, new IStopUserCallback.Stub() { // from class: com.android.server.pm.UserManagerService.6
                                public void userStopped(int userIdParam) {
                                    UserManagerService.this.finishRemoveUser(userIdParam);
                                    UserManagerService.this.logUserRemoveJourneyFinish(sessionId, userIdParam, true);
                                }

                                public void userStopAborted(int userIdParam) {
                                    UserManagerService.this.logUserRemoveJourneyFinish(sessionId, userIdParam, false);
                                }
                            });
                            return res == 0;
                        } catch (RemoteException e2) {
                            Slog.w(LOG_TAG, "Failed to stop user during removal.", e2);
                            return false;
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void addRemovingUserIdLocked(int userId) {
        this.mRemovingUserIds.put(userId, true);
        this.mRecentlyRemovedIds.add(Integer.valueOf(userId));
        if (this.mRecentlyRemovedIds.size() > 100) {
            this.mRecentlyRemovedIds.removeFirst();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4792=5] */
    public int removeUserWhenPossible(int userId, boolean overrideDevicePolicy) {
        checkCreateUsersPermission("Only the system can remove users");
        if (!overrideDevicePolicy) {
            String restriction = getUserRemovalRestriction(userId);
            if (getUserRestrictions(UserHandle.getCallingUserId()).getBoolean(restriction, false)) {
                Slog.w(LOG_TAG, "Cannot remove user. " + restriction + " is enabled.");
                return -2;
            }
        }
        if (userId == 0) {
            Slog.e(LOG_TAG, "System user cannot be removed.");
            return -4;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    UserData userData = this.mUsers.get(userId);
                    if (userData == null) {
                        Slog.e(LOG_TAG, "Cannot remove user " + userId + ", invalid user id provided.");
                        return -3;
                    } else if (this.mRemovingUserIds.get(userId)) {
                        Slog.e(LOG_TAG, "User " + userId + " is already scheduled for removal.");
                        return 2;
                    } else {
                        int currentUser = ActivityManager.getCurrentUser();
                        if (currentUser == userId || !removeUserUnchecked(userId)) {
                            Slog.i(LOG_TAG, "Unable to immediately remove user " + userId + " (current user is " + currentUser + "). User is set as ephemeral and will be removed on user switch or reboot.");
                            userData.info.flags |= 256;
                            writeUserLP(userData);
                            return 1;
                        }
                        return 0;
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishRemoveUser(final int userId) {
        UserInfo user;
        Slog.i(LOG_TAG, "finishRemoveUser " + userId);
        synchronized (this.mUsersLock) {
            user = getUserInfoLU(userId);
        }
        if (user != null && user.preCreated) {
            Slog.i(LOG_TAG, "Removing a pre-created user with user id: " + userId);
            ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).onUserStopped(userId);
            removeUserState(userId);
            return;
        }
        synchronized (this.mUserLifecycleListeners) {
            for (int i = 0; i < this.mUserLifecycleListeners.size(); i++) {
                this.mUserLifecycleListeners.get(i).onUserRemoved(user);
            }
        }
        long ident = Binder.clearCallingIdentity();
        try {
            Intent removedIntent = new Intent("android.intent.action.USER_REMOVED");
            removedIntent.addFlags(16777216);
            removedIntent.putExtra("android.intent.extra.user_handle", userId);
            removedIntent.putExtra("android.intent.extra.USER", UserHandle.of(userId));
            this.mContext.sendOrderedBroadcastAsUser(removedIntent, UserHandle.ALL, "android.permission.MANAGE_USERS", new BroadcastReceiver() { // from class: com.android.server.pm.UserManagerService.7
                /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.pm.UserManagerService$7$1] */
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    new Thread() { // from class: com.android.server.pm.UserManagerService.7.1
                        @Override // java.lang.Thread, java.lang.Runnable
                        public void run() {
                            ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).onUserRemoved(userId);
                            UserManagerService.this.removeUserState(userId);
                        }
                    }.start();
                }
            }, null, -1, null, null);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeUserState(int userId) {
        Slog.i(LOG_TAG, "Removing user state of user " + userId);
        try {
            if (Build.IS_DEBUG_ENABLE) {
                Slog.i(LOG_TAG, "destroy User Key  for user：" + userId);
            }
            ((StorageManager) this.mContext.getSystemService(StorageManager.class)).destroyUserKey(userId);
        } catch (IllegalStateException e) {
            Slog.i(LOG_TAG, "Destroying key for user " + userId + " failed, continuing anyway", e);
        }
        try {
            IGateKeeperService gk = GateKeeper.getService();
            if (gk != null) {
                gk.clearSecureUserId(userId);
            }
        } catch (Exception e2) {
            Slog.w(LOG_TAG, "unable to clear GK secure user id");
        }
        this.mPm.cleanUpUser(this, userId);
        this.mUserDataPreparer.destroyUserData(userId, 3);
        synchronized (this.mUsersLock) {
            this.mUsers.remove(userId);
            this.mIsUserManaged.delete(userId);
        }
        synchronized (this.mUserStates) {
            this.mUserStates.delete(userId);
        }
        synchronized (this.mRestrictionsLock) {
            this.mBaseUserRestrictions.remove(userId);
            this.mAppliedUserRestrictions.remove(userId);
            this.mCachedEffectiveUserRestrictions.remove(userId);
            this.mDevicePolicyLocalUserRestrictions.delete(userId);
            boolean changed = false;
            for (int i = 0; i < this.mDevicePolicyLocalUserRestrictions.size(); i++) {
                int targetUserId = this.mDevicePolicyLocalUserRestrictions.keyAt(i);
                changed |= getDevicePolicyLocalRestrictionsForTargetUserLR(targetUserId).remove(userId);
            }
            if (changed | this.mDevicePolicyGlobalUserRestrictions.remove(userId)) {
                applyUserRestrictionsForAllUsersLR();
            }
        }
        synchronized (this.mPackagesLock) {
            writeUserListLP();
        }
        AtomicFile userFile = new AtomicFile(new File(this.mUsersDir, userId + XML_SUFFIX));
        userFile.delete();
        updateUserIds();
        if (userId == 999) {
            synchronized (this.mUsersLock) {
                this.mRemovingUserIds.delete(userId);
            }
        }
    }

    private void sendProfileRemovedBroadcast(int parentUserId, int removedUserId) {
        Intent managedProfileIntent = new Intent("android.intent.action.MANAGED_PROFILE_REMOVED");
        managedProfileIntent.putExtra("android.intent.extra.USER", new UserHandle(removedUserId));
        managedProfileIntent.putExtra("android.intent.extra.user_handle", removedUserId);
        UserHandle parentHandle = new UserHandle(parentUserId);
        getDevicePolicyManagerInternal().broadcastIntentToManifestReceivers(managedProfileIntent, parentHandle, false);
        managedProfileIntent.addFlags(1342177280);
        this.mContext.sendBroadcastAsUser(managedProfileIntent, parentHandle, null);
    }

    public Bundle getApplicationRestrictions(String packageName) {
        return getApplicationRestrictionsForUser(packageName, UserHandle.getCallingUserId());
    }

    public Bundle getApplicationRestrictionsForUser(String packageName, int userId) {
        Bundle readApplicationRestrictionsLAr;
        if (UserHandle.getCallingUserId() != userId || !UserHandle.isSameApp(Binder.getCallingUid(), getUidForPackage(packageName))) {
            checkSystemOrRoot("get application restrictions for other user/app " + packageName);
        }
        synchronized (this.mAppRestrictionsLock) {
            readApplicationRestrictionsLAr = readApplicationRestrictionsLAr(packageName, userId);
        }
        return readApplicationRestrictionsLAr;
    }

    public void setApplicationRestrictions(String packageName, Bundle restrictions, int userId) {
        boolean changed;
        checkSystemOrRoot("set application restrictions");
        String validationResult = validateName(packageName);
        if (validationResult != null) {
            if (packageName.contains("../")) {
                EventLog.writeEvent(1397638484, "239701237", -1, "");
            }
            throw new IllegalArgumentException("Invalid package name: " + validationResult);
        }
        if (restrictions != null) {
            restrictions.setDefusable(true);
        }
        synchronized (this.mAppRestrictionsLock) {
            if (restrictions != null) {
                if (!restrictions.isEmpty()) {
                    writeApplicationRestrictionsLAr(packageName, restrictions, userId);
                    changed = true;
                }
            }
            changed = cleanAppRestrictionsForPackageLAr(packageName, userId);
        }
        if (!changed) {
            return;
        }
        Intent changeIntent = new Intent("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED");
        changeIntent.setPackage(packageName);
        changeIntent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(changeIntent, UserHandle.of(userId));
    }

    static String validateName(String name) {
        int n = name.length();
        boolean front = true;
        for (int i = 0; i < n; i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                front = false;
            } else {
                if (!front) {
                    if ((c < '0' || c > '9') && c != '_') {
                        if (c == '.') {
                            front = true;
                        }
                    }
                }
                return "bad character '" + c + "'";
            }
        }
        return null;
    }

    private int getUidForPackage(String packageName) {
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mContext.getPackageManager().getApplicationInfo(packageName, 4194304).uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private static Bundle readApplicationRestrictionsLAr(String packageName, int userId) {
        AtomicFile restrictionsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(userId), packageToRestrictionsFileName(packageName)));
        return readApplicationRestrictionsLAr(restrictionsFile);
    }

    static Bundle readApplicationRestrictionsLAr(AtomicFile restrictionsFile) {
        TypedXmlPullParser parser;
        Bundle restrictions = new Bundle();
        ArrayList<String> values = new ArrayList<>();
        if (!restrictionsFile.getBaseFile().exists()) {
            return restrictions;
        }
        FileInputStream fis = null;
        try {
            try {
                fis = restrictionsFile.openRead();
                parser = Xml.resolvePullParser(fis);
                XmlUtils.nextElement(parser);
            } catch (IOException | XmlPullParserException e) {
                Slog.w(LOG_TAG, "Error parsing " + restrictionsFile.getBaseFile(), e);
            }
            if (parser.getEventType() != 2) {
                Slog.e(LOG_TAG, "Unable to read restrictions file " + restrictionsFile.getBaseFile());
                return restrictions;
            }
            while (parser.next() != 1) {
                readEntry(restrictions, values, parser);
            }
            return restrictions;
        } finally {
            IoUtils.closeQuietly((AutoCloseable) null);
        }
    }

    private static void readEntry(Bundle restrictions, ArrayList<String> values, TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() == 2 && parser.getName().equals(TAG_ENTRY)) {
            String key = parser.getAttributeValue((String) null, ATTR_KEY);
            String valType = parser.getAttributeValue((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE);
            int count = parser.getAttributeInt((String) null, ATTR_MULTIPLE, -1);
            if (count != -1) {
                values.clear();
                while (count > 0) {
                    int type = parser.next();
                    if (type == 1) {
                        break;
                    } else if (type == 2 && parser.getName().equals(TAG_VALUE)) {
                        values.add(parser.nextText().trim());
                        count--;
                    }
                }
                String[] valueStrings = new String[values.size()];
                values.toArray(valueStrings);
                restrictions.putStringArray(key, valueStrings);
            } else if (ATTR_TYPE_BUNDLE.equals(valType)) {
                restrictions.putBundle(key, readBundleEntry(parser, values));
            } else if (ATTR_TYPE_BUNDLE_ARRAY.equals(valType)) {
                int outerDepth = parser.getDepth();
                ArrayList<Bundle> bundleList = new ArrayList<>();
                while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                    Bundle childBundle = readBundleEntry(parser, values);
                    bundleList.add(childBundle);
                }
                restrictions.putParcelableArray(key, (Parcelable[]) bundleList.toArray(new Bundle[bundleList.size()]));
            } else {
                String value = parser.nextText().trim();
                if (ATTR_TYPE_BOOLEAN.equals(valType)) {
                    restrictions.putBoolean(key, Boolean.parseBoolean(value));
                } else if (ATTR_TYPE_INTEGER.equals(valType)) {
                    restrictions.putInt(key, Integer.parseInt(value));
                } else {
                    restrictions.putString(key, value);
                }
            }
        }
    }

    private static Bundle readBundleEntry(TypedXmlPullParser parser, ArrayList<String> values) throws IOException, XmlPullParserException {
        Bundle childBundle = new Bundle();
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            readEntry(childBundle, values, parser);
        }
        return childBundle;
    }

    private static void writeApplicationRestrictionsLAr(String packageName, Bundle restrictions, int userId) {
        AtomicFile restrictionsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(userId), packageToRestrictionsFileName(packageName)));
        writeApplicationRestrictionsLAr(restrictions, restrictionsFile);
    }

    static void writeApplicationRestrictionsLAr(Bundle restrictions, AtomicFile restrictionsFile) {
        FileOutputStream fos = null;
        try {
            fos = restrictionsFile.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(fos);
            serializer.startDocument((String) null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag((String) null, TAG_RESTRICTIONS);
            writeBundle(restrictions, serializer);
            serializer.endTag((String) null, TAG_RESTRICTIONS);
            serializer.endDocument();
            restrictionsFile.finishWrite(fos);
        } catch (Exception e) {
            restrictionsFile.failWrite(fos);
            Slog.e(LOG_TAG, "Error writing application restrictions list", e);
        }
    }

    private static void writeBundle(Bundle restrictions, TypedXmlSerializer serializer) throws IOException {
        for (String key : restrictions.keySet()) {
            Object value = restrictions.get(key);
            serializer.startTag((String) null, TAG_ENTRY);
            serializer.attribute((String) null, ATTR_KEY, key);
            if (value instanceof Boolean) {
                serializer.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, ATTR_TYPE_BOOLEAN);
                serializer.text(value.toString());
            } else if (value instanceof Integer) {
                serializer.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, ATTR_TYPE_INTEGER);
                serializer.text(value.toString());
            } else if (value == null || (value instanceof String)) {
                serializer.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, ATTR_TYPE_STRING);
                serializer.text(value != null ? (String) value : "");
            } else if (value instanceof Bundle) {
                serializer.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, ATTR_TYPE_BUNDLE);
                writeBundle((Bundle) value, serializer);
            } else {
                int i = 0;
                if (value instanceof Parcelable[]) {
                    serializer.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, ATTR_TYPE_BUNDLE_ARRAY);
                    Parcelable[] array = (Parcelable[]) value;
                    int length = array.length;
                    while (i < length) {
                        Parcelable parcelable = array[i];
                        if (!(parcelable instanceof Bundle)) {
                            throw new IllegalArgumentException("bundle-array can only hold Bundles");
                        }
                        serializer.startTag((String) null, TAG_ENTRY);
                        serializer.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, ATTR_TYPE_BUNDLE);
                        writeBundle((Bundle) parcelable, serializer);
                        serializer.endTag((String) null, TAG_ENTRY);
                        i++;
                    }
                    continue;
                } else {
                    serializer.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, ATTR_TYPE_STRING_ARRAY);
                    String[] values = (String[]) value;
                    serializer.attributeInt((String) null, ATTR_MULTIPLE, values.length);
                    int length2 = values.length;
                    while (i < length2) {
                        String choice = values[i];
                        serializer.startTag((String) null, TAG_VALUE);
                        serializer.text(choice != null ? choice : "");
                        serializer.endTag((String) null, TAG_VALUE);
                        i++;
                    }
                }
            }
            serializer.endTag((String) null, TAG_ENTRY);
        }
    }

    public int getUserSerialNumber(int userId) {
        int i;
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            i = userInfo != null ? userInfo.serialNumber : -1;
        }
        return i;
    }

    public boolean isUserNameSet(int userId) {
        boolean z;
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        if (!hasQueryOrCreateUsersPermission() && (callingUserId != userId || !hasPermissionGranted("android.permission.GET_ACCOUNTS_PRIVILEGED", callingUid))) {
            throw new SecurityException("You need MANAGE_USERS, CREATE_USERS, QUERY_USERS, or GET_ACCOUNTS_PRIVILEGED permissions to: get whether user name is set");
        }
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            z = (userInfo == null || userInfo.name == null) ? false : true;
        }
        return z;
    }

    public int getUserHandle(int userSerialNumber) {
        int[] iArr;
        synchronized (this.mUsersLock) {
            for (int userId : this.mUserIds) {
                UserInfo info = getUserInfoLU(userId);
                if (info != null && info.serialNumber == userSerialNumber) {
                    return userId;
                }
            }
            return -1;
        }
    }

    public long getUserCreationTime(int userId) {
        int callingUserId = UserHandle.getCallingUserId();
        UserInfo userInfo = null;
        synchronized (this.mUsersLock) {
            if (callingUserId == userId) {
                userInfo = getUserInfoLU(userId);
            } else {
                UserInfo parent = getProfileParentLU(userId);
                if (parent != null && parent.id == callingUserId) {
                    userInfo = getUserInfoLU(userId);
                }
            }
        }
        if (userInfo == null) {
            throw new SecurityException("userId can only be the calling user or a profile associated with this user");
        }
        return userInfo.creationTime;
    }

    private void updateUserIds() {
        int num = 0;
        int numIncludingPreCreated = 0;
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo userInfo = this.mUsers.valueAt(i).info;
                if (!userInfo.partial) {
                    numIncludingPreCreated++;
                    if (!userInfo.preCreated) {
                        num++;
                    }
                }
            }
            int[] newUsers = new int[num];
            int[] newUsersIncludingPreCreated = new int[numIncludingPreCreated];
            int n = 0;
            int n2 = 0;
            for (int i2 = 0; i2 < userSize; i2++) {
                UserInfo userInfo2 = this.mUsers.valueAt(i2).info;
                if (!userInfo2.partial) {
                    int userId = this.mUsers.keyAt(i2);
                    int nIncludingPreCreated = n2 + 1;
                    newUsersIncludingPreCreated[n2] = userId;
                    if (userInfo2.preCreated) {
                        n2 = nIncludingPreCreated;
                    } else {
                        newUsers[n] = userId;
                        n++;
                        n2 = nIncludingPreCreated;
                    }
                }
            }
            this.mUserIds = newUsers;
            this.mUserIdsIncludingPreCreated = newUsersIncludingPreCreated;
        }
    }

    public void onBeforeStartUser(int userId) {
        UserInfo userInfo = getUserInfo(userId);
        if (userInfo == null) {
            return;
        }
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("onBeforeStartUser-" + userId);
        int userSerial = userInfo.serialNumber;
        boolean migrateAppsData = !PackagePartitions.FINGERPRINT.equals(userInfo.lastLoggedInFingerprint);
        t.traceBegin("prepareUserData");
        this.mUserDataPreparer.prepareUserData(userId, userSerial, 1);
        t.traceEnd();
        t.traceBegin("reconcileAppsData");
        getPackageManagerInternal().reconcileAppsData(userId, 1, migrateAppsData);
        t.traceEnd();
        if (userId != 0) {
            t.traceBegin("applyUserRestrictions");
            synchronized (this.mRestrictionsLock) {
                applyUserRestrictionsLR(userId);
            }
            t.traceEnd();
        }
        t.traceEnd();
    }

    public void onBeforeUnlockUser(int userId) {
        UserInfo userInfo = getUserInfo(userId);
        if (userInfo == null) {
            return;
        }
        int userSerial = userInfo.serialNumber;
        boolean migrateAppsData = !PackagePartitions.FINGERPRINT.equals(userInfo.lastLoggedInFingerprint);
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("prepareUserData-" + userId);
        this.mUserDataPreparer.prepareUserData(userId, userSerial, 2);
        t.traceEnd();
        StorageManagerInternal smInternal = (StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class);
        smInternal.markCeStoragePrepared(userId);
        t.traceBegin("reconcileAppsData-" + userId);
        getPackageManagerInternal().reconcileAppsData(userId, 2, migrateAppsData);
        t.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reconcileUsers(String volumeUuid) {
        this.mUserDataPreparer.reconcileUsers(volumeUuid, getUsers(true, true, false));
    }

    public void onUserLoggedIn(int userId) {
        UserData userData = getUserDataNoChecks(userId);
        if (userData == null || userData.info.partial) {
            Slog.w(LOG_TAG, "userForeground: unknown user #" + userId);
            return;
        }
        long now = System.currentTimeMillis();
        if (now > EPOCH_PLUS_30_YEARS) {
            userData.info.lastLoggedInTime = now;
        }
        userData.info.lastLoggedInFingerprint = PackagePartitions.FINGERPRINT;
        scheduleWriteUser(userData);
    }

    int getNextAvailableId() {
        synchronized (this.mUsersLock) {
            int nextId = scanNextAvailableIdLocked();
            if (nextId >= 0) {
                return nextId;
            }
            if (this.mRemovingUserIds.size() > 0) {
                Slog.i(LOG_TAG, "All available IDs are used. Recycling LRU ids.");
                this.mRemovingUserIds.clear();
                Iterator<Integer> it = this.mRecentlyRemovedIds.iterator();
                while (it.hasNext()) {
                    Integer recentlyRemovedId = it.next();
                    this.mRemovingUserIds.put(recentlyRemovedId.intValue(), true);
                }
                nextId = scanNextAvailableIdLocked();
            }
            UserManager.invalidateStaticUserProperties();
            if (nextId < 0) {
                throw new IllegalStateException("No user id available!");
            }
            return nextId;
        }
    }

    private int scanNextAvailableIdLocked() {
        for (int i = 10; i < MAX_USER_ID; i++) {
            if (this.mUsers.indexOfKey(i) < 0 && !this.mRemovingUserIds.get(i)) {
                return i;
            }
        }
        return -1;
    }

    private static String packageToRestrictionsFileName(String packageName) {
        return RESTRICTIONS_FILE_PREFIX + packageName + XML_SUFFIX;
    }

    public void setSeedAccountData(int userId, String accountName, String accountType, PersistableBundle accountOptions, boolean persist) {
        checkManageUsersPermission("set user seed account data");
        setSeedAccountDataNoChecks(userId, accountName, accountType, accountOptions, persist);
    }

    private void setSeedAccountDataNoChecks(int userId, String accountName, String accountType, PersistableBundle accountOptions, boolean persist) {
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = getUserDataLU(userId);
                if (userData == null) {
                    Slog.e(LOG_TAG, "No such user for settings seed data u=" + userId);
                    return;
                }
                userData.seedAccountName = accountName;
                userData.seedAccountType = accountType;
                userData.seedAccountOptions = accountOptions;
                userData.persistSeedData = persist;
                if (persist) {
                    writeUserLP(userData);
                }
            }
        }
    }

    public String getSeedAccountName(int userId) throws RemoteException {
        String str;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            UserData userData = getUserDataLU(userId);
            str = userData == null ? null : userData.seedAccountName;
        }
        return str;
    }

    public String getSeedAccountType(int userId) throws RemoteException {
        String str;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            UserData userData = getUserDataLU(userId);
            str = userData == null ? null : userData.seedAccountType;
        }
        return str;
    }

    public PersistableBundle getSeedAccountOptions(int userId) throws RemoteException {
        PersistableBundle persistableBundle;
        checkManageUsersPermission("Cannot get seed account information");
        synchronized (this.mUsersLock) {
            UserData userData = getUserDataLU(userId);
            persistableBundle = userData == null ? null : userData.seedAccountOptions;
        }
        return persistableBundle;
    }

    public void clearSeedAccountData(int userId) throws RemoteException {
        checkManageUsersPermission("Cannot clear seed account information");
        synchronized (this.mPackagesLock) {
            synchronized (this.mUsersLock) {
                UserData userData = getUserDataLU(userId);
                if (userData == null) {
                    return;
                }
                userData.clearSeedAccountData();
                writeUserLP(userData);
            }
        }
    }

    public boolean someUserHasSeedAccount(String accountName, String accountType) {
        checkManageUsersPermission("check seed account information");
        return someUserHasSeedAccountNoChecks(accountName, accountType);
    }

    private boolean someUserHasSeedAccountNoChecks(String accountName, String accountType) {
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserData data = this.mUsers.valueAt(i);
                if (!data.info.isInitialized() && !this.mRemovingUserIds.get(data.info.id) && data.seedAccountName != null && data.seedAccountName.equals(accountName) && data.seedAccountType != null && data.seedAccountType.equals(accountType)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean someUserHasAccount(String accountName, String accountType) {
        checkCreateUsersPermission("check seed account information");
        return someUserHasAccountNoChecks(accountName, accountType);
    }

    private boolean someUserHasAccountNoChecks(final String accountName, final String accountType) {
        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
            return false;
        }
        final Account account = new Account(accountName, accountType);
        return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.UserManagerService$$ExternalSyntheticLambda1
            public final Object getOrThrow() {
                return UserManagerService.this.m5745x7e9585bd(account, accountName, accountType);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$someUserHasAccountNoChecks$3$com-android-server-pm-UserManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m5745x7e9585bd(Account account, String accountName, String accountType) throws Exception {
        return Boolean.valueOf(AccountManager.get(this.mContext).someUserHasAccount(account) || someUserHasSeedAccountNoChecks(accountName, accountType));
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.pm.UserManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new Shell().exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes2.dex */
    private final class Shell extends ShellCommand {
        private Shell() {
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("User manager (user) commands:");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println();
            pw.println("  list [-v | --verbose] [--all]");
            pw.println("    Prints all users on the system.");
            pw.println();
            pw.println("  report-system-user-package-whitelist-problems [-v | --verbose] [--critical-only] [--mode MODE]");
            pw.println("    Reports all issues on user-type package allowlist XML files. Options:");
            pw.println("    -v | --verbose: shows extra info, like number of issues");
            pw.println("    --critical-only: show only critical issues, excluding warnings");
            pw.println("    --mode MODE: shows what errors would be if device used mode MODE");
            pw.println("      (where MODE is the allowlist mode integer as defined by config_userTypePackageWhitelistMode)");
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            boolean z;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            try {
                switch (cmd.hashCode()) {
                    case 3322014:
                        if (cmd.equals("list")) {
                            z = false;
                            break;
                        }
                        z = true;
                        break;
                    case 1085270974:
                        if (cmd.equals("report-system-user-package-whitelist-problems")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        return runList();
                    case true:
                        return runReportPackageAllowlistProblems();
                    default:
                        return handleDefaultCommands(cmd);
                }
            } catch (RemoteException e) {
                getOutPrintWriter().println("Remote exception: " + e);
                return -1;
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private int runList() throws RemoteException {
            int currentUser;
            boolean all;
            int i;
            char c;
            Shell shell = this;
            PrintWriter pw = getOutPrintWriter();
            boolean verbose = false;
            boolean all2 = false;
            while (true) {
                String opt = getNextOption();
                int i2 = 0;
                int i3 = 1;
                if (opt != null) {
                    switch (opt.hashCode()) {
                        case 1513:
                            if (opt.equals("-v")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case 42995713:
                            if (opt.equals("--all")) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1737088994:
                            if (opt.equals("--verbose")) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                        case 1:
                            verbose = true;
                            break;
                        case 2:
                            all2 = true;
                            break;
                        default:
                            pw.println("Invalid option: " + opt);
                            return -1;
                    }
                } else {
                    IActivityManager am = ActivityManager.getService();
                    List<UserInfo> users = UserManagerService.this.getUsers(!all2, false, !all2);
                    if (users == null) {
                        pw.println("Error: couldn't get users");
                        return 1;
                    }
                    int size = users.size();
                    if (verbose) {
                        pw.printf("%d users:\n\n", Integer.valueOf(size));
                        int currentUser2 = am.getCurrentUser().id;
                        currentUser = currentUser2;
                    } else {
                        pw.println("Users:");
                        currentUser = -10000;
                    }
                    int i4 = 0;
                    while (i4 < size) {
                        UserInfo user = users.get(i4);
                        boolean running = am.isUserRunning(user.id, i2);
                        int i5 = user.id == currentUser ? i3 : i2;
                        int i6 = (user.profileGroupId == user.id || user.profileGroupId == -10000) ? i2 : i3;
                        if (verbose) {
                            DevicePolicyManagerInternal dpm = UserManagerService.this.getDevicePolicyManagerInternal();
                            String deviceOwner = "";
                            String profileOwner = "";
                            if (dpm != null) {
                                long ident = Binder.clearCallingIdentity();
                                try {
                                    if (dpm.getDeviceOwnerUserId() == user.id) {
                                        deviceOwner = " (device-owner)";
                                    }
                                    if (dpm.getProfileOwnerAsUser(user.id) != null) {
                                        profileOwner = " (profile-owner)";
                                    }
                                } finally {
                                    Binder.restoreCallingIdentity(ident);
                                }
                            }
                            Object[] objArr = new Object[13];
                            objArr[0] = Integer.valueOf(i4);
                            objArr[1] = Integer.valueOf(user.id);
                            objArr[2] = user.name;
                            all = all2;
                            objArr[3] = user.userType.replace("android.os.usertype.", "");
                            objArr[4] = UserInfo.flagsToString(user.flags);
                            objArr[5] = i6 != 0 ? " (parentId=" + user.profileGroupId + ")" : "";
                            objArr[6] = running ? " (running)" : "";
                            objArr[7] = user.partial ? " (partial)" : "";
                            objArr[8] = user.preCreated ? " (pre-created)" : "";
                            objArr[9] = user.convertedFromPreCreated ? " (converted)" : "";
                            objArr[10] = deviceOwner;
                            objArr[11] = profileOwner;
                            objArr[12] = i5 != 0 ? " (current)" : "";
                            pw.printf("%d: id=%d, name=%s, type=%s, flags=%s%s%s%s%s%s%s%s%s\n", objArr);
                            i = 1;
                        } else {
                            all = all2;
                            Object[] objArr2 = new Object[2];
                            objArr2[0] = user;
                            i = 1;
                            objArr2[1] = running ? " running" : "";
                            pw.printf("\t%s%s\n", objArr2);
                        }
                        i4++;
                        shell = this;
                        i3 = i;
                        all2 = all;
                        i2 = 0;
                    }
                    return 0;
                }
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:15:0x0033, code lost:
            if (r4.equals("-v") != false) goto L9;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private int runReportPackageAllowlistProblems() {
            PrintWriter pw = getOutPrintWriter();
            boolean verbose = false;
            boolean criticalOnly = false;
            int mode = -1000;
            while (true) {
                String opt = getNextOption();
                char c = 0;
                if (opt != null) {
                    switch (opt.hashCode()) {
                        case -1362766982:
                            if (opt.equals("--critical-only")) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1513:
                            break;
                        case 1333227331:
                            if (opt.equals("--mode")) {
                                c = 3;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1737088994:
                            if (opt.equals("--verbose")) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                        case 1:
                            verbose = true;
                            break;
                        case 2:
                            criticalOnly = true;
                            break;
                        case 3:
                            mode = Integer.parseInt(getNextArgRequired());
                            break;
                        default:
                            pw.println("Invalid option: " + opt);
                            return -1;
                    }
                } else {
                    Slog.d(UserManagerService.LOG_TAG, "runReportPackageAllowlistProblems(): verbose=" + verbose + ", criticalOnly=" + criticalOnly + ", mode=" + UserSystemPackageInstaller.modeToString(mode));
                    IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
                    try {
                        UserManagerService.this.mSystemPackageInstaller.dumpPackageWhitelistProblems(ipw, mode, verbose, criticalOnly);
                        ipw.close();
                        return 0;
                    } catch (Throwable th) {
                        try {
                            ipw.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, pw)) {
            long now = System.currentTimeMillis();
            long nowRealtime = SystemClock.elapsedRealtime();
            StringBuilder sb = new StringBuilder();
            if (args != null && args.length > 0 && args[0].equals("--user")) {
                dumpUser(pw, UserHandle.parseUserArg(args[1]), sb, now, nowRealtime);
                return;
            }
            ActivityManagerInternal amInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            pw.print("Current user: ");
            if (amInternal != null) {
                pw.println(amInternal.getCurrentUserId());
            } else {
                pw.println("N/A");
            }
            pw.println();
            synchronized (this.mPackagesLock) {
                synchronized (this.mUsersLock) {
                    pw.println("Users:");
                    for (int i = 0; i < this.mUsers.size(); i++) {
                        UserData userData = this.mUsers.valueAt(i);
                        if (userData != null) {
                            dumpUserLocked(pw, userData, sb, now, nowRealtime);
                        }
                    }
                }
                pw.println();
                pw.println("Device properties:");
                pw.println("  Device owner id:" + this.mDeviceOwnerUserId);
                pw.println();
                pw.println("  Guest restrictions:");
                synchronized (this.mGuestRestrictions) {
                    UserRestrictionsUtils.dumpRestrictions(pw, "    ", this.mGuestRestrictions);
                }
                synchronized (this.mUsersLock) {
                    pw.println();
                    pw.println("  Device managed: " + this.mIsDeviceManaged);
                    if (this.mRemovingUserIds.size() > 0) {
                        pw.println();
                        pw.println("  Recently removed userIds: " + this.mRecentlyRemovedIds);
                    }
                }
                synchronized (this.mUserStates) {
                    pw.print("  Started users state: [");
                    int size = this.mUserStates.states.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        int userId = this.mUserStates.states.keyAt(i2);
                        int state = this.mUserStates.states.valueAt(i2);
                        pw.print(userId);
                        pw.print('=');
                        pw.print(UserState.stateToString(state));
                        if (i2 != size - 1) {
                            pw.print(", ");
                        }
                    }
                    pw.println(']');
                }
                synchronized (this.mUsersLock) {
                    pw.print("  Cached user IDs: ");
                    pw.println(Arrays.toString(this.mUserIds));
                    pw.print("  Cached user IDs (including pre-created): ");
                    pw.println(Arrays.toString(this.mUserIdsIncludingPreCreated));
                }
            }
            pw.println();
            pw.print("  Max users: " + UserManager.getMaxSupportedUsers());
            pw.println(" (limit reached: " + isUserLimitReached() + ")");
            pw.println("  Supports switchable users: " + UserManager.supportsMultipleUsers());
            pw.println("  All guests ephemeral: " + Resources.getSystem().getBoolean(17891674));
            pw.println("  Force ephemeral users: " + this.mForceEphemeralUsers);
            pw.println("  Is split-system user: " + UserManager.isSplitSystemUser());
            pw.println("  Is headless-system mode: " + UserManager.isHeadlessSystemUserMode());
            pw.println("  User version: " + this.mUserVersion);
            pw.println("  Owner name: " + getOwnerName());
            pw.println();
            pw.println("Number of listeners for");
            synchronized (this.mUserRestrictionsListeners) {
                pw.println("  restrictions: " + this.mUserRestrictionsListeners.size());
            }
            synchronized (this.mUserLifecycleListeners) {
                pw.println("  user lifecycle events: " + this.mUserLifecycleListeners.size());
            }
            pw.println();
            pw.println("User types version: " + this.mUserTypeVersion);
            pw.println("User types (" + this.mUserTypes.size() + " types):");
            for (int i3 = 0; i3 < this.mUserTypes.size(); i3++) {
                pw.println("    " + this.mUserTypes.keyAt(i3) + ": ");
                this.mUserTypes.valueAt(i3).dump(pw, "        ");
            }
            IndentingPrintWriter ipw = new IndentingPrintWriter(pw);
            try {
                ipw.println();
                this.mSystemPackageInstaller.dump(ipw);
                ipw.close();
            } catch (Throwable th) {
                try {
                    ipw.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    private void dumpUser(PrintWriter pw, int userId, StringBuilder sb, long now, long nowRealtime) {
        int userId2;
        if (userId != -2) {
            userId2 = userId;
        } else {
            ActivityManagerInternal amInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            if (amInternal == null) {
                pw.println("Cannot determine current user");
                return;
            }
            userId2 = amInternal.getCurrentUserId();
        }
        synchronized (this.mUsersLock) {
            UserData userData = this.mUsers.get(userId2);
            if (userData == null) {
                pw.println("User " + userId2 + " not found");
            } else {
                dumpUserLocked(pw, userData, sb, now, nowRealtime);
            }
        }
    }

    private void dumpUserLocked(PrintWriter pw, UserData userData, StringBuilder tempStringBuilder, long now, long nowRealtime) {
        int state;
        UserInfo userInfo = userData.info;
        int userId = userInfo.id;
        pw.print("  ");
        pw.print(userInfo);
        pw.print(" serialNo=");
        pw.print(userInfo.serialNumber);
        pw.print(" isPrimary=");
        pw.print(userInfo.isPrimary());
        if (userInfo.profileGroupId != userInfo.id && userInfo.profileGroupId != -10000) {
            pw.print(" parentId=");
            pw.print(userInfo.profileGroupId);
        }
        if (this.mRemovingUserIds.get(userId)) {
            pw.print(" <removing> ");
        }
        if (userInfo.partial) {
            pw.print(" <partial>");
        }
        if (userInfo.preCreated) {
            pw.print(" <pre-created>");
        }
        if (userInfo.convertedFromPreCreated) {
            pw.print(" <converted>");
        }
        pw.println();
        pw.print("    Type: ");
        pw.println(userInfo.userType);
        pw.print("    Flags: ");
        pw.print(userInfo.flags);
        pw.print(" (");
        pw.print(UserInfo.flagsToString(userInfo.flags));
        pw.println(")");
        pw.print("    State: ");
        synchronized (this.mUserStates) {
            state = this.mUserStates.get(userId, -1);
        }
        pw.println(UserState.stateToString(state));
        pw.print("    Created: ");
        dumpTimeAgo(pw, tempStringBuilder, now, userInfo.creationTime);
        pw.print("    Last logged in: ");
        dumpTimeAgo(pw, tempStringBuilder, now, userInfo.lastLoggedInTime);
        pw.print("    Last logged in fingerprint: ");
        pw.println(userInfo.lastLoggedInFingerprint);
        pw.print("    Start time: ");
        dumpTimeAgo(pw, tempStringBuilder, nowRealtime, userData.startRealtime);
        pw.print("    Unlock time: ");
        dumpTimeAgo(pw, tempStringBuilder, nowRealtime, userData.unlockRealtime);
        pw.print("    Has profile owner: ");
        pw.println(this.mIsUserManaged.get(userId));
        pw.println("    Restrictions:");
        synchronized (this.mRestrictionsLock) {
            UserRestrictionsUtils.dumpRestrictions(pw, "      ", this.mBaseUserRestrictions.getRestrictions(userInfo.id));
            pw.println("    Device policy global restrictions:");
            UserRestrictionsUtils.dumpRestrictions(pw, "      ", this.mDevicePolicyGlobalUserRestrictions.getRestrictions(userInfo.id));
            pw.println("    Device policy local restrictions:");
            getDevicePolicyLocalRestrictionsForTargetUserLR(userInfo.id).dumpRestrictions(pw, "      ");
            pw.println("    Effective restrictions:");
            UserRestrictionsUtils.dumpRestrictions(pw, "      ", this.mCachedEffectiveUserRestrictions.getRestrictions(userInfo.id));
        }
        if (userData.account != null) {
            pw.print("    Account name: " + userData.account);
            pw.println();
        }
        if (userData.seedAccountName != null) {
            pw.print("    Seed account name: " + userData.seedAccountName);
            pw.println();
            if (userData.seedAccountType != null) {
                pw.print("         account type: " + userData.seedAccountType);
                pw.println();
            }
            if (userData.seedAccountOptions != null) {
                pw.print("         account options exist");
                pw.println();
            }
        }
        pw.println("    Ignore errors preparing storage: " + userData.getIgnorePrepareStorageErrors());
    }

    private static void dumpTimeAgo(PrintWriter pw, StringBuilder sb, long nowTime, long time) {
        if (time == 0) {
            pw.println("<unknown>");
            return;
        }
        sb.setLength(0);
        TimeUtils.formatDuration(nowTime - time, sb);
        sb.append(" ago");
        pw.println(sb);
    }

    /* loaded from: classes2.dex */
    final class MainHandler extends Handler {
        MainHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    removeMessages(1, msg.obj);
                    synchronized (UserManagerService.this.mPackagesLock) {
                        int userId = ((UserData) msg.obj).info.id;
                        UserData userData = UserManagerService.this.getUserDataNoChecks(userId);
                        if (userData != null) {
                            UserManagerService.this.writeUserLP(userData);
                        } else {
                            Slog.i(UserManagerService.LOG_TAG, "handle(WRITE_USER_MSG): no data for user " + userId + ", it was probably removed before handler could handle it");
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    boolean isUserInitialized(int userId) {
        return this.mLocalService.isUserInitialized(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class LocalService extends UserManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void setDevicePolicyUserRestrictions(int originatingUserId, Bundle global, RestrictionsSet local, boolean isDeviceOwner) {
            UserManagerService.this.setDevicePolicyUserRestrictionsInner(originatingUserId, global, local, isDeviceOwner);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean getUserRestriction(int userId, String key) {
            return UserManagerService.this.getUserRestrictions(userId).getBoolean(key);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void addUserRestrictionsListener(UserManagerInternal.UserRestrictionsListener listener) {
            synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                UserManagerService.this.mUserRestrictionsListeners.add(listener);
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void removeUserRestrictionsListener(UserManagerInternal.UserRestrictionsListener listener) {
            synchronized (UserManagerService.this.mUserRestrictionsListeners) {
                UserManagerService.this.mUserRestrictionsListeners.remove(listener);
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void addUserLifecycleListener(UserManagerInternal.UserLifecycleListener listener) {
            synchronized (UserManagerService.this.mUserLifecycleListeners) {
                UserManagerService.this.mUserLifecycleListeners.add(listener);
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void removeUserLifecycleListener(UserManagerInternal.UserLifecycleListener listener) {
            synchronized (UserManagerService.this.mUserLifecycleListeners) {
                UserManagerService.this.mUserLifecycleListeners.remove(listener);
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void setDeviceManaged(boolean isManaged) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mIsDeviceManaged = isManaged;
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean isDeviceManaged() {
            boolean z;
            synchronized (UserManagerService.this.mUsersLock) {
                z = UserManagerService.this.mIsDeviceManaged;
            }
            return z;
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void setUserManaged(int userId, boolean isManaged) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mIsUserManaged.put(userId, isManaged);
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean isUserManaged(int userId) {
            boolean z;
            synchronized (UserManagerService.this.mUsersLock) {
                z = UserManagerService.this.mIsUserManaged.get(userId);
            }
            return z;
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void setUserIcon(int userId, Bitmap bitmap) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (UserManagerService.this.mPackagesLock) {
                    UserData userData = UserManagerService.this.getUserDataNoChecks(userId);
                    if (userData != null && !userData.info.partial) {
                        UserManagerService.this.writeBitmapLP(userData.info, bitmap);
                        UserManagerService.this.writeUserLP(userData);
                        UserManagerService.this.sendUserInfoChangedBroadcast(userId);
                        return;
                    }
                    Slog.w(UserManagerService.LOG_TAG, "setUserIcon: unknown user #" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void setForceEphemeralUsers(boolean forceEphemeralUsers) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserManagerService.this.mForceEphemeralUsers = forceEphemeralUsers;
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void removeAllUsers() {
            if (ActivityManager.getCurrentUser() == 0) {
                UserManagerService.this.removeNonSystemUsers();
                return;
            }
            BroadcastReceiver userSwitchedReceiver = new BroadcastReceiver() { // from class: com.android.server.pm.UserManagerService.LocalService.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    if (userId != 0) {
                        return;
                    }
                    UserManagerService.this.mContext.unregisterReceiver(this);
                    UserManagerService.this.removeNonSystemUsers();
                }
            };
            IntentFilter userSwitchedFilter = new IntentFilter();
            userSwitchedFilter.addAction("android.intent.action.USER_SWITCHED");
            UserManagerService.this.mContext.registerReceiver(userSwitchedReceiver, userSwitchedFilter, null, UserManagerService.this.mHandler);
            ActivityManager am = (ActivityManager) UserManagerService.this.mContext.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
            am.switchUser(0);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void onEphemeralUserStop(int userId) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo userInfo = UserManagerService.this.getUserInfoLU(userId);
                if (userInfo != null && userInfo.isEphemeral()) {
                    userInfo.flags |= 64;
                    if (userInfo.isGuest()) {
                        userInfo.guestToRemove = true;
                    }
                }
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public UserInfo createUserEvenWhenDisallowed(String name, String userType, int flags, String[] disallowedPackages, Object token) throws UserManager.CheckedUserOperationException {
            return UserManagerService.this.createUserInternalUnchecked(name, userType, flags, -10000, false, disallowedPackages, token);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean removeUserEvenWhenDisallowed(int userId) {
            return UserManagerService.this.removeUserUnchecked(userId);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean isUserRunning(int userId) {
            boolean z;
            synchronized (UserManagerService.this.mUserStates) {
                z = UserManagerService.this.mUserStates.get(userId, -1) >= 0;
            }
            return z;
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void setUserState(int userId, int userState) {
            synchronized (UserManagerService.this.mUserStates) {
                UserManagerService.this.mUserStates.put(userId, userState);
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void removeUserState(int userId) {
            synchronized (UserManagerService.this.mUserStates) {
                UserManagerService.this.mUserStates.delete(userId);
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public int[] getUserIds() {
            return UserManagerService.this.getUserIds();
        }

        @Override // com.android.server.pm.UserManagerInternal
        public List<UserInfo> getUsers(boolean excludeDying) {
            return getUsers(true, excludeDying, true);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) {
            return UserManagerService.this.getUsersInternal(excludePartial, excludeDying, excludePreCreated);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean isUserUnlockingOrUnlocked(int userId) {
            int state;
            synchronized (UserManagerService.this.mUserStates) {
                state = UserManagerService.this.mUserStates.get(userId, -1);
            }
            if (state == 4 || state == 5) {
                return StorageManager.isUserKeyUnlocked(userId);
            }
            return state == 2 || state == 3;
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean isUserUnlocked(int userId) {
            int state;
            synchronized (UserManagerService.this.mUserStates) {
                state = UserManagerService.this.mUserStates.get(userId, -1);
            }
            if (state == 4 || state == 5) {
                return StorageManager.isUserKeyUnlocked(userId);
            }
            return state == 3;
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean isUserInitialized(int userId) {
            return (getUserInfo(userId).flags & 16) != 0;
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean exists(int userId) {
            return UserManagerService.this.getUserInfoNoChecks(userId) != null;
        }

        /* JADX WARN: Code restructure failed: missing block: B:26:0x003c, code lost:
            return false;
         */
        /* JADX WARN: Code restructure failed: missing block: B:30:0x005c, code lost:
            android.util.Slog.w(com.android.server.pm.UserManagerService.LOG_TAG, r10 + " for disabled profile " + r9 + " from " + r8);
         */
        /* JADX WARN: Code restructure failed: missing block: B:34:0x0086, code lost:
            android.util.Slog.w(com.android.server.pm.UserManagerService.LOG_TAG, r10 + " for another profile " + r9 + " from " + r8);
         */
        /* JADX WARN: Code restructure failed: missing block: B:36:0x00ad, code lost:
            return false;
         */
        @Override // com.android.server.pm.UserManagerInternal
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public boolean isProfileAccessible(int callingUserId, int targetUserId, String debugMsg, boolean throwSecurityException) {
            if (targetUserId == callingUserId) {
                return true;
            }
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo callingUserInfo = UserManagerService.this.getUserInfoLU(callingUserId);
                if (callingUserInfo != null && !callingUserInfo.isProfile()) {
                    UserInfo targetUserInfo = UserManagerService.this.getUserInfoLU(targetUserId);
                    if (targetUserInfo != null && targetUserInfo.isEnabled()) {
                        if (targetUserInfo.profileGroupId != -10000 && targetUserInfo.profileGroupId == callingUserInfo.profileGroupId) {
                            return true;
                        }
                        throw new SecurityException(debugMsg + " for unrelated profile " + targetUserId);
                    }
                    return false;
                }
                throw new SecurityException(debugMsg + " for another profile " + targetUserId + " from " + callingUserId);
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public int getProfileParentId(int userId) {
            synchronized (UserManagerService.this.mUsersLock) {
                UserInfo profileParent = UserManagerService.this.getProfileParentLU(userId);
                if (profileParent == null) {
                    return userId;
                }
                return profileParent.id;
            }
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean isSettingRestrictedForUser(String setting, int userId, String value, int callingUid) {
            return UserManagerService.this.isSettingRestrictedForUser(setting, userId, value, callingUid);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean hasUserRestriction(String restrictionKey, int userId) {
            Bundle restrictions;
            return UserRestrictionsUtils.isValidRestriction(restrictionKey) && (restrictions = UserManagerService.this.getEffectiveUserRestrictions(userId)) != null && restrictions.getBoolean(restrictionKey);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public UserInfo getUserInfo(int userId) {
            UserData userData;
            synchronized (UserManagerService.this.mUsersLock) {
                userData = (UserData) UserManagerService.this.mUsers.get(userId);
            }
            if (userData == null) {
                return null;
            }
            return userData.info;
        }

        @Override // com.android.server.pm.UserManagerInternal
        public UserInfo[] getUserInfos() {
            UserInfo[] allInfos;
            synchronized (UserManagerService.this.mUsersLock) {
                int userSize = UserManagerService.this.mUsers.size();
                allInfos = new UserInfo[userSize];
                for (int i = 0; i < userSize; i++) {
                    allInfos[i] = ((UserData) UserManagerService.this.mUsers.valueAt(i)).info;
                }
            }
            return allInfos;
        }

        @Override // com.android.server.pm.UserManagerInternal
        public void setDefaultCrossProfileIntentFilters(int parentUserId, int profileUserId) {
            UserTypeDetails userTypeDetails = UserManagerService.this.getUserTypeDetailsNoChecks(profileUserId);
            Bundle restrictions = UserManagerService.this.getEffectiveUserRestrictions(profileUserId);
            UserManagerService.this.setDefaultCrossProfileIntentFilters(profileUserId, userTypeDetails, restrictions, parentUserId);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean isDualProfile(int userId) {
            return UserManagerService.this.isDualProfile(userId);
        }

        @Override // com.android.server.pm.UserManagerInternal
        public boolean shouldIgnorePrepareStorageErrors(int userId) {
            boolean z;
            synchronized (UserManagerService.this.mUsersLock) {
                UserData userData = (UserData) UserManagerService.this.mUsers.get(userId);
                z = userData != null && userData.getIgnorePrepareStorageErrors();
            }
            return z;
        }
    }

    private void enforceUserRestriction(String restriction, int userId, String message) throws UserManager.CheckedUserOperationException {
        if (hasUserRestriction(restriction, userId)) {
            String errorMessage = (message != null ? message + ": " : "") + restriction + " is enabled.";
            Slog.w(LOG_TAG, errorMessage);
            throw new UserManager.CheckedUserOperationException(errorMessage, 1);
        }
    }

    private void throwCheckedUserOperationException(String message, int userOperationResult) throws UserManager.CheckedUserOperationException {
        Slog.e(LOG_TAG, message);
        throw new UserManager.CheckedUserOperationException(message, userOperationResult);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeNonSystemUsers() {
        ArrayList<UserInfo> usersToRemove = new ArrayList<>();
        synchronized (this.mUsersLock) {
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo ui = this.mUsers.valueAt(i).info;
                if (ui.id != 0) {
                    usersToRemove.add(ui);
                }
            }
        }
        Iterator<UserInfo> it = usersToRemove.iterator();
        while (it.hasNext()) {
            removeUser(it.next().id);
        }
    }

    private static void debug(String message) {
        Slog.d(LOG_TAG, message + "");
    }

    int getMaxUsersOfTypePerParent(String userType) {
        UserTypeDetails type = this.mUserTypes.get(userType);
        if (type == null) {
            return 0;
        }
        return getMaxUsersOfTypePerParent(type);
    }

    private static int getMaxUsersOfTypePerParent(UserTypeDetails userTypeDetails) {
        int defaultMax = userTypeDetails.getMaxAllowedPerParent();
        if (!Build.IS_DEBUGGABLE) {
            return defaultMax;
        }
        if (userTypeDetails.isManagedProfile()) {
            return SystemProperties.getInt("persist.sys.max_profiles", defaultMax);
        }
        return defaultMax;
    }

    int getFreeProfileBadgeLU(int parentUserId, String userType) {
        Set<Integer> usedBadges = new ArraySet<>();
        int userSize = this.mUsers.size();
        for (int i = 0; i < userSize; i++) {
            UserInfo ui = this.mUsers.valueAt(i).info;
            if (ui.userType.equals(userType) && ui.profileGroupId == parentUserId && !this.mRemovingUserIds.get(ui.id)) {
                usedBadges.add(Integer.valueOf(ui.profileBadge));
            }
        }
        int maxUsersOfType = getMaxUsersOfTypePerParent(userType);
        if (maxUsersOfType == -1) {
            maxUsersOfType = Integer.MAX_VALUE;
        }
        for (int i2 = 0; i2 < maxUsersOfType; i2++) {
            if (!usedBadges.contains(Integer.valueOf(i2))) {
                return i2;
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasProfile(int userId) {
        synchronized (this.mUsersLock) {
            UserInfo userInfo = getUserInfoLU(userId);
            int userSize = this.mUsers.size();
            for (int i = 0; i < userSize; i++) {
                UserInfo profile = this.mUsers.valueAt(i).info;
                if (userId != profile.id && isProfileOf(userInfo, profile)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void verifyCallingPackage(String callingPackage, int callingUid) {
        int packageUid = this.mPm.snapshotComputer().getPackageUid(callingPackage, 0L, UserHandle.getUserId(callingUid));
        if (packageUid != callingUid) {
            throw new SecurityException("Specified package " + callingPackage + " does not match the calling uid " + callingUid);
        }
    }

    private PackageManagerInternal getPackageManagerInternal() {
        if (this.mPmInternal == null) {
            this.mPmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }
        return this.mPmInternal;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DevicePolicyManagerInternal getDevicePolicyManagerInternal() {
        if (this.mDevicePolicyManagerInternal == null) {
            this.mDevicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        }
        return this.mDevicePolicyManagerInternal;
    }
}
