package com.android.server.accounts;

import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.AccountManagerInternal;
import android.accounts.AccountManagerResponse;
import android.accounts.AuthenticatorDescription;
import android.accounts.CantAddAccountActivity;
import android.accounts.ChooseAccountActivity;
import android.accounts.GrantCredentialsPermissionActivity;
import android.accounts.IAccountAuthenticator;
import android.accounts.IAccountAuthenticatorResponse;
import android.accounts.IAccountManager;
import android.accounts.IAccountManagerResponse;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteStatement;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.accounts.TokenCache;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
/* loaded from: classes.dex */
public class AccountManagerService extends IAccountManager.Stub implements RegisteredServicesCacheListener<AuthenticatorDescription> {
    private static final Intent ACCOUNTS_CHANGED_INTENT;
    private static final Account[] EMPTY_ACCOUNT_ARRAY;
    private static final int MESSAGE_COPY_SHARED_ACCOUNT = 4;
    private static final int MESSAGE_TIMED_OUT = 3;
    private static final String PRE_N_DATABASE_NAME = "accounts.db";
    private static final int SIGNATURE_CHECK_MATCH = 1;
    private static final int SIGNATURE_CHECK_MISMATCH = 0;
    private static final int SIGNATURE_CHECK_UID_MATCH = 2;
    private static final String TAG = "AccountManagerService";
    private static AtomicReference<AccountManagerService> sThis;
    private final AppOpsManager mAppOpsManager;
    private final IAccountAuthenticatorCache mAuthenticatorCache;
    final Context mContext;
    final MessageHandler mHandler;
    private final Injector mInjector;
    private final PackageManager mPackageManager;
    private UserManager mUserManager;
    private final LinkedHashMap<String, Session> mSessions = new LinkedHashMap<>();
    private final SparseArray<UserAccounts> mUsers = new SparseArray<>();
    private final SparseBooleanArray mLocalUnlockedUsers = new SparseBooleanArray();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private CopyOnWriteArrayList<AccountManagerInternal.OnAppPermissionChangeListener> mAppPermissionChangeListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public static class Lifecycle extends SystemService {
        private AccountManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.accounts.AccountManagerService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.accounts.AccountManagerService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? accountManagerService = new AccountManagerService(new Injector(getContext()));
            this.mService = accountManagerService;
            publishBinderService("account", accountManagerService);
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            this.mService.onUnlockUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser user) {
            Slog.i(AccountManagerService.TAG, "onStopUser " + user);
            this.mService.purgeUserData(user.getUserIdentifier());
        }
    }

    static {
        Intent intent = new Intent("android.accounts.LOGIN_ACCOUNTS_CHANGED");
        ACCOUNTS_CHANGED_INTENT = intent;
        intent.setFlags(AudioFormat.HE_AAC_V1);
        sThis = new AtomicReference<>();
        EMPTY_ACCOUNT_ARRAY = new Account[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class UserAccounts {
        final HashMap<String, Account[]> accountCache;
        private final TokenCache accountTokenCaches;
        final AccountsDb accountsDb;
        private final Map<Account, Map<String, String>> authTokenCache;
        final Object cacheLock;
        final Object dbLock;
        private final Map<String, Map<String, Integer>> mReceiversForType;
        private final HashMap<Account, AtomicReference<String>> previousNameCache;
        private final Map<Account, Map<String, String>> userDataCache;
        private final int userId;
        private final Map<Account, Map<String, Integer>> visibilityCache;
        private final HashMap<Pair<Pair<Account, String>, Integer>, NotificationId> credentialsPermissionNotificationIds = new HashMap<>();
        private final HashMap<Account, NotificationId> signinRequiredNotificationIds = new HashMap<>();

        UserAccounts(Context context, int userId, File preNDbFile, File deDbFile) {
            Object obj = new Object();
            this.cacheLock = obj;
            Object obj2 = new Object();
            this.dbLock = obj2;
            this.accountCache = new LinkedHashMap();
            this.userDataCache = new HashMap();
            this.authTokenCache = new HashMap();
            this.accountTokenCaches = new TokenCache();
            this.visibilityCache = new HashMap();
            this.mReceiversForType = new HashMap();
            this.previousNameCache = new HashMap<>();
            this.userId = userId;
            synchronized (obj2) {
                synchronized (obj) {
                    this.accountsDb = AccountsDb.create(context, userId, preNDbFile, deDbFile);
                }
            }
        }
    }

    public static AccountManagerService getSingleton() {
        return sThis.get();
    }

    /* JADX WARN: Type inference failed for: r1v14, types: [com.android.server.accounts.AccountManagerService$3] */
    public AccountManagerService(Injector injector) {
        this.mInjector = injector;
        Context context = injector.getContext();
        this.mContext = context;
        PackageManager packageManager = context.getPackageManager();
        this.mPackageManager = packageManager;
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mAppOpsManager = appOpsManager;
        MessageHandler messageHandler = new MessageHandler(injector.getMessageHandlerLooper());
        this.mHandler = messageHandler;
        IAccountAuthenticatorCache accountAuthenticatorCache = injector.getAccountAuthenticatorCache();
        this.mAuthenticatorCache = accountAuthenticatorCache;
        accountAuthenticatorCache.setListener(this, messageHandler);
        sThis.set(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        context.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.accounts.AccountManagerService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context1, Intent intent) {
                if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    final String removedPackageName = intent.getData().getSchemeSpecificPart();
                    Runnable purgingRunnable = new Runnable() { // from class: com.android.server.accounts.AccountManagerService.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            AccountManagerService.this.purgeOldGrantsAll();
                            AccountManagerService.this.removeVisibilityValuesForPackage(removedPackageName);
                        }
                    };
                    AccountManagerService.this.mHandler.post(purgingRunnable);
                }
            }
        }, intentFilter);
        injector.addLocalService(new AccountManagerInternalImpl());
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        context.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.accounts.AccountManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                int userId;
                String action = intent.getAction();
                if (!"android.intent.action.USER_REMOVED".equals(action) || (userId = intent.getIntExtra("android.intent.extra.user_handle", -1)) < 1) {
                    return;
                }
                Slog.i(AccountManagerService.TAG, "User " + userId + " removed");
                AccountManagerService.this.purgeUserData(userId);
            }
        }, UserHandle.ALL, userFilter, null, null);
        new PackageMonitor() { // from class: com.android.server.accounts.AccountManagerService.3
            public void onPackageAdded(String packageName, int uid) {
                AccountManagerService.this.cancelAccountAccessRequestNotificationIfNeeded(uid, true);
            }

            public void onPackageUpdateFinished(String packageName, int uid) {
                AccountManagerService.this.cancelAccountAccessRequestNotificationIfNeeded(uid, true);
            }
        }.register(context, messageHandler.getLooper(), UserHandle.ALL, true);
        appOpsManager.startWatchingMode(62, (String) null, (AppOpsManager.OnOpChangedListener) new AppOpsManager.OnOpChangedInternalListener() { // from class: com.android.server.accounts.AccountManagerService.4
            public void onOpChanged(int op, String packageName) {
                try {
                    int userId = ActivityManager.getCurrentUser();
                    int uid = AccountManagerService.this.mPackageManager.getPackageUidAsUser(packageName, userId);
                    int mode = AccountManagerService.this.mAppOpsManager.checkOpNoThrow(62, uid, packageName);
                    if (mode == 0) {
                        long identity = Binder.clearCallingIdentity();
                        AccountManagerService.this.cancelAccountAccessRequestNotificationIfNeeded(packageName, uid, true);
                        Binder.restoreCallingIdentity(identity);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        });
        packageManager.addOnPermissionsChangeListener(new PackageManager.OnPermissionsChangedListener() { // from class: com.android.server.accounts.AccountManagerService$$ExternalSyntheticLambda3
            public final void onPermissionsChanged(int i) {
                AccountManagerService.this.m812lambda$new$0$comandroidserveraccountsAccountManagerService(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-accounts-AccountManagerService  reason: not valid java name */
    public /* synthetic */ void m812lambda$new$0$comandroidserveraccountsAccountManagerService(int uid) {
        AccountManager.invalidateLocalAccountsDataCaches();
        Account[] accounts = null;
        String[] packageNames = this.mPackageManager.getPackagesForUid(uid);
        if (packageNames != null) {
            int userId = UserHandle.getUserId(uid);
            long identity = Binder.clearCallingIdentity();
            try {
                for (String packageName : packageNames) {
                    if (this.mPackageManager.checkPermission("android.permission.GET_ACCOUNTS", packageName) == 0) {
                        if (accounts == null) {
                            accounts = getAccountsAsUser(null, userId, PackageManagerService.PLATFORM_PACKAGE_NAME);
                            if (ArrayUtils.isEmpty(accounts)) {
                                return;
                            }
                        }
                        for (Account account : accounts) {
                            cancelAccountAccessRequestNotificationIfNeeded(account, uid, packageName, true);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getBindInstantServiceAllowed(int userId) {
        return this.mAuthenticatorCache.getBindInstantServiceAllowed(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBindInstantServiceAllowed(int userId, boolean allowed) {
        this.mAuthenticatorCache.setBindInstantServiceAllowed(userId, allowed);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAccountAccessRequestNotificationIfNeeded(int uid, boolean checkAccess) {
        Account[] accounts = getAccountsAsUser(null, UserHandle.getUserId(uid), PackageManagerService.PLATFORM_PACKAGE_NAME);
        for (Account account : accounts) {
            cancelAccountAccessRequestNotificationIfNeeded(account, uid, checkAccess);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAccountAccessRequestNotificationIfNeeded(String packageName, int uid, boolean checkAccess) {
        Account[] accounts = getAccountsAsUser(null, UserHandle.getUserId(uid), PackageManagerService.PLATFORM_PACKAGE_NAME);
        for (Account account : accounts) {
            cancelAccountAccessRequestNotificationIfNeeded(account, uid, packageName, checkAccess);
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(Account account, int uid, boolean checkAccess) {
        String[] packageNames = this.mPackageManager.getPackagesForUid(uid);
        if (packageNames != null) {
            for (String packageName : packageNames) {
                cancelAccountAccessRequestNotificationIfNeeded(account, uid, packageName, checkAccess);
            }
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(Account account, int uid, String packageName, boolean checkAccess) {
        if (!checkAccess || hasAccountAccess(account, packageName, UserHandle.getUserHandleForUid(uid))) {
            cancelNotification(getCredentialPermissionNotificationId(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", uid), UserHandle.getUserHandleForUid(uid));
        }
    }

    public boolean addAccountExplicitlyWithVisibility(Account account, String password, Bundle extras, Map packageToVisibility, String opPackageName) {
        Bundle.setDefusable(extras, true);
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccountExplicitly: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Objects.requireNonNull(account, "account cannot be null");
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot explicitly add accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return addAccountInternal(accounts, account, password, extras, callingUid, packageToVisibility, opPackageName);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public Map<Account, Integer> getAccountsAndVisibilityForPackage(String packageName, String accountType) {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        boolean isSystemUid = UserHandle.isSameApp(callingUid, 1000);
        List<String> managedTypes = getTypesForCaller(callingUid, userId, isSystemUid);
        if ((accountType != null && !managedTypes.contains(accountType)) || (accountType == null && !isSystemUid)) {
            throw new SecurityException("getAccountsAndVisibilityForPackage() called from unauthorized uid " + callingUid + " with packageName=" + packageName);
        }
        if (accountType != null) {
            managedTypes = new ArrayList();
            managedTypes.add(accountType);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return getAccountsAndVisibilityForPackage(packageName, managedTypes, Integer.valueOf(callingUid), accounts);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private Map<Account, Integer> getAccountsAndVisibilityForPackage(String packageName, List<String> accountTypes, Integer callingUid, UserAccounts accounts) {
        if (!packageExistsForUser(packageName, accounts.userId)) {
            Log.w(TAG, "getAccountsAndVisibilityForPackage#Package not found " + packageName);
            return new LinkedHashMap();
        }
        Map<Account, Integer> result = new LinkedHashMap<>();
        for (String accountType : accountTypes) {
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    Account[] accountsOfType = accounts.accountCache.get(accountType);
                    if (accountsOfType != null) {
                        for (Account account : accountsOfType) {
                            result.put(account, resolveAccountVisibility(account, packageName, accounts));
                        }
                    }
                }
            }
        }
        return filterSharedAccounts(accounts, result, callingUid.intValue(), packageName);
    }

    public Map<String, Integer> getPackagesAndVisibilityForAccount(Account account) {
        Map<String, Integer> packagesAndVisibilityForAccountLocked;
        Objects.requireNonNull(account, "account cannot be null");
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId) && !isSystemUid(callingUid)) {
            String msg = String.format("uid %s cannot get secrets for account %s", Integer.valueOf(callingUid), account);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    packagesAndVisibilityForAccountLocked = getPackagesAndVisibilityForAccountLocked(account, accounts);
                }
            }
            return packagesAndVisibilityForAccountLocked;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private Map<String, Integer> getPackagesAndVisibilityForAccountLocked(Account account, UserAccounts accounts) {
        Map<String, Integer> accountVisibility = (Map) accounts.visibilityCache.get(account);
        if (accountVisibility == null) {
            Log.d(TAG, "Visibility was not initialized");
            HashMap hashMap = new HashMap();
            accounts.visibilityCache.put(account, hashMap);
            AccountManager.invalidateLocalAccountsDataCaches();
            return hashMap;
        }
        return accountVisibility;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [634=6] */
    public int getAccountVisibility(Account account, String packageName) {
        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(packageName, "packageName cannot be null");
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId) && !isSystemUid(callingUid)) {
            String msg = String.format("uid %s cannot get secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            if ("android:accounts:key_legacy_visible".equals(packageName)) {
                int visibility = getAccountVisibilityFromCache(account, packageName, accounts);
                if (visibility != 0) {
                    return visibility;
                }
                return 2;
            } else if ("android:accounts:key_legacy_not_visible".equals(packageName)) {
                int visibility2 = getAccountVisibilityFromCache(account, packageName, accounts);
                if (visibility2 != 0) {
                    return visibility2;
                }
                return 4;
            } else {
                return resolveAccountVisibility(account, packageName, accounts).intValue();
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private int getAccountVisibilityFromCache(Account account, String packageName, UserAccounts accounts) {
        int intValue;
        synchronized (accounts.cacheLock) {
            Map<String, Integer> accountVisibility = getPackagesAndVisibilityForAccountLocked(account, accounts);
            Integer visibility = accountVisibility.get(packageName);
            intValue = visibility != null ? visibility.intValue() : 0;
        }
        return intValue;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Integer resolveAccountVisibility(Account account, String packageName, UserAccounts accounts) {
        int signatureCheckResult;
        int visibility;
        Objects.requireNonNull(packageName, "packageName cannot be null");
        try {
            long identityToken = clearCallingIdentity();
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, accounts.userId);
            restoreCallingIdentity(identityToken);
            if (!UserHandle.isSameApp(uid, 1000) && (signatureCheckResult = checkPackageSignature(account.type, uid, accounts.userId)) != 2) {
                int visibility2 = getAccountVisibilityFromCache(account, packageName, accounts);
                if (visibility2 != 0) {
                    return Integer.valueOf(visibility2);
                }
                boolean isPrivileged = isPermittedForPackage(packageName, accounts.userId, "android.permission.GET_ACCOUNTS_PRIVILEGED");
                if (isProfileOwner(uid)) {
                    return 1;
                }
                boolean preO = isPreOApplication(packageName);
                if (signatureCheckResult != 0 || ((preO && checkGetAccountsPermission(packageName, accounts.userId)) || ((checkReadContactsPermission(packageName, accounts.userId) && accountTypeManagesContacts(account.type, accounts.userId)) || isPrivileged))) {
                    visibility = getAccountVisibilityFromCache(account, "android:accounts:key_legacy_visible", accounts);
                    if (visibility == 0) {
                        visibility = 2;
                    }
                } else {
                    visibility = getAccountVisibilityFromCache(account, "android:accounts:key_legacy_not_visible", accounts);
                    if (visibility == 0) {
                        visibility = 4;
                    }
                }
                return Integer.valueOf(visibility);
            }
            return 1;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "resolveAccountVisibility#Package not found " + e.getMessage());
            return 3;
        }
    }

    private boolean isPreOApplication(String packageName) {
        try {
            long identityToken = clearCallingIdentity();
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 0);
            restoreCallingIdentity(identityToken);
            if (applicationInfo == null) {
                return true;
            }
            int version = applicationInfo.targetSdkVersion;
            return version < 26;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "isPreOApplication#Package not found " + e.getMessage());
            return true;
        }
    }

    public boolean setAccountVisibility(Account account, String packageName, int newVisibility) {
        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(packageName, "packageName cannot be null");
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId) && !isSystemUid(callingUid)) {
            String msg = String.format("uid %s cannot get secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return setAccountVisibility(account, packageName, newVisibility, true, accounts);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private boolean isVisible(int visibility) {
        return visibility == 1 || visibility == 2;
    }

    private boolean setAccountVisibility(Account account, String packageName, int newVisibility, boolean notify, UserAccounts accounts) {
        Map<String, Integer> packagesToVisibility;
        List<String> accountRemovedReceivers;
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                if (notify) {
                    if (isSpecialPackageKey(packageName)) {
                        packagesToVisibility = getRequestingPackages(account, accounts);
                        accountRemovedReceivers = getAccountRemovedReceivers(account, accounts);
                    } else if (!packageExistsForUser(packageName, accounts.userId)) {
                        return false;
                    } else {
                        packagesToVisibility = new HashMap<>();
                        packagesToVisibility.put(packageName, resolveAccountVisibility(account, packageName, accounts));
                        accountRemovedReceivers = new ArrayList<>();
                        if (shouldNotifyPackageOnAccountRemoval(account, packageName, accounts)) {
                            accountRemovedReceivers.add(packageName);
                        }
                    }
                } else if (!isSpecialPackageKey(packageName) && !packageExistsForUser(packageName, accounts.userId)) {
                    return false;
                } else {
                    packagesToVisibility = Collections.emptyMap();
                    accountRemovedReceivers = Collections.emptyList();
                }
                if (!updateAccountVisibilityLocked(account, packageName, newVisibility, accounts)) {
                    return false;
                }
                if (notify) {
                    for (Map.Entry<String, Integer> packageToVisibility : packagesToVisibility.entrySet()) {
                        int oldVisibility = packageToVisibility.getValue().intValue();
                        int currentVisibility = resolveAccountVisibility(account, packageName, accounts).intValue();
                        if (isVisible(oldVisibility) != isVisible(currentVisibility)) {
                            notifyPackage(packageToVisibility.getKey(), accounts);
                        }
                    }
                    for (String packageNameToNotify : accountRemovedReceivers) {
                        sendAccountRemovedBroadcast(account, packageNameToNotify, accounts.userId);
                    }
                    sendAccountsChangedBroadcast(accounts.userId);
                }
                return true;
            }
        }
    }

    private boolean updateAccountVisibilityLocked(Account account, String packageName, int newVisibility, UserAccounts accounts) {
        long accountId = accounts.accountsDb.findDeAccountId(account);
        if (accountId < 0) {
            return false;
        }
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            if (accounts.accountsDb.setAccountVisibility(accountId, packageName, newVisibility)) {
                StrictMode.setThreadPolicy(oldPolicy);
                Map<String, Integer> accountVisibility = getPackagesAndVisibilityForAccountLocked(account, accounts);
                accountVisibility.put(packageName, Integer.valueOf(newVisibility));
                AccountManager.invalidateLocalAccountsDataCaches();
                return true;
            }
            return false;
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    public void registerAccountListener(String[] accountTypes, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            registerAccountListener(accountTypes, opPackageName, accounts);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void registerAccountListener(String[] accountTypes, String opPackageName, UserAccounts accounts) {
        synchronized (accounts.mReceiversForType) {
            if (accountTypes == null) {
                accountTypes = new String[]{null};
            }
            for (String type : accountTypes) {
                Map<String, Integer> receivers = (Map) accounts.mReceiversForType.get(type);
                if (receivers == null) {
                    receivers = new HashMap<>();
                    accounts.mReceiversForType.put(type, receivers);
                }
                Integer cnt = receivers.get(opPackageName);
                int i = 1;
                if (cnt != null) {
                    i = 1 + cnt.intValue();
                }
                receivers.put(opPackageName, Integer.valueOf(i));
            }
        }
    }

    public void unregisterAccountListener(String[] accountTypes, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            unregisterAccountListener(accountTypes, opPackageName, accounts);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void unregisterAccountListener(String[] accountTypes, String opPackageName, UserAccounts accounts) {
        synchronized (accounts.mReceiversForType) {
            if (accountTypes == null) {
                accountTypes = new String[]{null};
            }
            for (String type : accountTypes) {
                Map<String, Integer> receivers = (Map) accounts.mReceiversForType.get(type);
                if (receivers == null || receivers.get(opPackageName) == null) {
                    throw new IllegalArgumentException("attempt to unregister wrong receiver");
                }
                Integer cnt = receivers.get(opPackageName);
                if (cnt.intValue() == 1) {
                    receivers.remove(opPackageName);
                } else {
                    receivers.put(opPackageName, Integer.valueOf(cnt.intValue() - 1));
                }
            }
        }
    }

    private void sendNotificationAccountUpdated(Account account, UserAccounts accounts) {
        Map<String, Integer> packagesToVisibility = getRequestingPackages(account, accounts);
        for (Map.Entry<String, Integer> packageToVisibility : packagesToVisibility.entrySet()) {
            if (packageToVisibility.getValue().intValue() != 3 && packageToVisibility.getValue().intValue() != 4) {
                notifyPackage(packageToVisibility.getKey(), accounts);
            }
        }
    }

    private void notifyPackage(String packageName, UserAccounts accounts) {
        Intent intent = new Intent("android.accounts.action.VISIBLE_ACCOUNTS_CHANGED");
        intent.setPackage(packageName);
        intent.setFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(accounts.userId));
    }

    private Map<String, Integer> getRequestingPackages(Account account, UserAccounts accounts) {
        Set<String> packages = new HashSet<>();
        synchronized (accounts.mReceiversForType) {
            String[] strArr = {account.type, null};
            for (int i = 0; i < 2; i++) {
                String type = strArr[i];
                Map<String, Integer> receivers = (Map) accounts.mReceiversForType.get(type);
                if (receivers != null) {
                    packages.addAll(receivers.keySet());
                }
            }
        }
        Map<String, Integer> result = new HashMap<>();
        for (String packageName : packages) {
            result.put(packageName, resolveAccountVisibility(account, packageName, accounts));
        }
        return result;
    }

    private List<String> getAccountRemovedReceivers(Account account, UserAccounts accounts) {
        Intent intent = new Intent("android.accounts.action.ACCOUNT_REMOVED");
        intent.setFlags(16777216);
        List<ResolveInfo> receivers = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 0, accounts.userId);
        List<String> result = new ArrayList<>();
        if (receivers == null) {
            return result;
        }
        for (ResolveInfo resolveInfo : receivers) {
            String packageName = resolveInfo.activityInfo.applicationInfo.packageName;
            int visibility = resolveAccountVisibility(account, packageName, accounts).intValue();
            if (visibility == 1 || visibility == 2) {
                result.add(packageName);
            }
        }
        return result;
    }

    private boolean shouldNotifyPackageOnAccountRemoval(Account account, String packageName, UserAccounts accounts) {
        int visibility = resolveAccountVisibility(account, packageName, accounts).intValue();
        if (visibility == 1 || visibility == 2) {
            Intent intent = new Intent("android.accounts.action.ACCOUNT_REMOVED");
            intent.setFlags(16777216);
            intent.setPackage(packageName);
            List<ResolveInfo> receivers = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 0, accounts.userId);
            return receivers != null && receivers.size() > 0;
        }
        return false;
    }

    private boolean packageExistsForUser(String packageName, int userId) {
        try {
            long identityToken = clearCallingIdentity();
            this.mPackageManager.getPackageUidAsUser(packageName, userId);
            restoreCallingIdentity(identityToken);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isSpecialPackageKey(String packageName) {
        return "android:accounts:key_legacy_visible".equals(packageName) || "android:accounts:key_legacy_not_visible".equals(packageName);
    }

    private void sendAccountsChangedBroadcast(int userId) {
        StringBuilder append = new StringBuilder().append("the accounts changed, sending broadcast of ");
        Intent intent = ACCOUNTS_CHANGED_INTENT;
        Log.i(TAG, append.append(intent.getAction()).toString());
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(userId));
    }

    private void sendAccountRemovedBroadcast(Account account, String packageName, int userId) {
        Intent intent = new Intent("android.accounts.action.ACCOUNT_REMOVED");
        intent.setFlags(16777216);
        intent.setPackage(packageName);
        intent.putExtra("authAccount", account.name);
        intent.putExtra("accountType", account.type);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(userId));
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException) && !(e instanceof IllegalArgumentException)) {
                Slog.wtf(TAG, "Account Manager Crash", e);
            }
            throw e;
        }
    }

    private UserManager getUserManager() {
        if (this.mUserManager == null) {
            this.mUserManager = UserManager.get(this.mContext);
        }
        return this.mUserManager;
    }

    public void validateAccounts(int userId) {
        UserAccounts accounts = getUserAccounts(userId);
        validateAccountsInternal(accounts, true);
    }

    /* JADX WARN: Removed duplicated region for block: B:99:0x02ed A[Catch: all -> 0x02fd, TRY_ENTER, TryCatch #9 {all -> 0x02fd, blocks: (B:99:0x02ed, B:101:0x02f5, B:90:0x02da, B:91:0x02e1, B:104:0x02fb), top: B:130:0x0072 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void validateAccountsInternal(UserAccounts accounts, boolean invalidateAuthenticatorCache) {
        Iterator<Map.Entry<Long, Account>> it;
        HashMap<String, Integer> knownAuth;
        boolean userUnlocked;
        HashMap<String, Integer> knownAuth2;
        SparseBooleanArray knownUids;
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "validateAccountsInternal " + accounts.userId + " isCeDatabaseAttached=" + accounts.accountsDb.isCeDatabaseAttached() + " userLocked=" + this.mLocalUnlockedUsers.get(accounts.userId));
        }
        if (invalidateAuthenticatorCache) {
            this.mAuthenticatorCache.invalidateCache(accounts.userId);
        }
        HashMap<String, Integer> knownAuth3 = getAuthenticatorTypeAndUIDForUser(this.mAuthenticatorCache, accounts.userId);
        boolean userUnlocked2 = isLocalUnlockedUser(accounts.userId);
        synchronized (accounts.dbLock) {
            try {
                try {
                    synchronized (accounts.cacheLock) {
                        boolean accountDeleted = false;
                        try {
                            try {
                                AccountsDb accountsDb = accounts.accountsDb;
                                Map<String, Integer> metaAuthUid = accountsDb.findMetaAuthUid();
                                HashSet<String> obsoleteAuthType = Sets.newHashSet();
                                SparseBooleanArray knownUids2 = null;
                                for (Map.Entry<String, Integer> authToUidEntry : metaAuthUid.entrySet()) {
                                    try {
                                        String type = authToUidEntry.getKey();
                                        int uid = authToUidEntry.getValue().intValue();
                                        Integer knownUid = knownAuth3.get(type);
                                        if (knownUid != null && uid == knownUid.intValue()) {
                                            knownAuth3.remove(type);
                                        } else {
                                            if (knownUids2 == null) {
                                                knownUids = getUidsOfInstalledOrUpdatedPackagesAsUser(accounts.userId);
                                            } else {
                                                knownUids = knownUids2;
                                            }
                                            if (!knownUids.get(uid)) {
                                                obsoleteAuthType.add(type);
                                                accountsDb.deleteMetaByAuthTypeAndUid(type, uid);
                                            }
                                            knownUids2 = knownUids;
                                        }
                                    } catch (Throwable th) {
                                        th = th;
                                        throw th;
                                    }
                                }
                                for (Map.Entry<String, Integer> entry : knownAuth3.entrySet()) {
                                    accountsDb.insertOrReplaceMetaAuthTypeAndUid(entry.getKey(), entry.getValue().intValue());
                                }
                                Map<Long, Account> accountsMap = accountsDb.findAllDeAccounts();
                                try {
                                    accounts.accountCache.clear();
                                    HashMap<String, Integer> accountNamesByType = new LinkedHashMap<>();
                                    Iterator<Map.Entry<Long, Account>> it2 = accountsMap.entrySet().iterator();
                                    while (it2.hasNext()) {
                                        Map.Entry<Long, Account> accountEntry = it2.next();
                                        long accountId = accountEntry.getKey().longValue();
                                        Account account = accountEntry.getValue();
                                        if (obsoleteAuthType.contains(account.type)) {
                                            it = it2;
                                            HashMap<String, Integer> accountNamesByType2 = accountNamesByType;
                                            Slog.w(TAG, "deleting account " + account.toSafeString() + " because type " + account.type + "'s registered authenticator no longer exist.");
                                            Map<String, Integer> packagesToVisibility = getRequestingPackages(account, accounts);
                                            List<String> accountRemovedReceivers = getAccountRemovedReceivers(account, accounts);
                                            accountsDb.beginTransaction();
                                            try {
                                                accountsDb.deleteDeAccount(accountId);
                                                if (userUnlocked2) {
                                                    try {
                                                        accountsDb.deleteCeAccount(accountId);
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        accountsDb.endTransaction();
                                                        throw th;
                                                    }
                                                }
                                                accountsDb.setTransactionSuccessful();
                                                accountsDb.endTransaction();
                                                try {
                                                    knownAuth = knownAuth3;
                                                    knownAuth2 = accountNamesByType2;
                                                    userUnlocked = userUnlocked2;
                                                    try {
                                                        logRecord(AccountsDb.DEBUG_ACTION_AUTHENTICATOR_REMOVE, "accounts", accountId, accounts);
                                                        accounts.userDataCache.remove(account);
                                                        accounts.authTokenCache.remove(account);
                                                        accounts.accountTokenCaches.remove(account);
                                                        accounts.visibilityCache.remove(account);
                                                        for (Map.Entry<String, Integer> packageToVisibility : packagesToVisibility.entrySet()) {
                                                            if (isVisible(packageToVisibility.getValue().intValue())) {
                                                                notifyPackage(packageToVisibility.getKey(), accounts);
                                                            }
                                                        }
                                                        for (String packageName : accountRemovedReceivers) {
                                                            sendAccountRemovedBroadcast(account, packageName, accounts.userId);
                                                        }
                                                        accountDeleted = true;
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        accountDeleted = true;
                                                        if (accountDeleted) {
                                                            sendAccountsChangedBroadcast(accounts.userId);
                                                        }
                                                        throw th;
                                                    }
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    accountDeleted = true;
                                                }
                                            } catch (Throwable th5) {
                                                th = th5;
                                            }
                                        } else {
                                            try {
                                                it = it2;
                                                knownAuth = knownAuth3;
                                                userUnlocked = userUnlocked2;
                                                knownAuth2 = accountNamesByType;
                                                ArrayList<String> accountNames = (ArrayList) knownAuth2.get(account.type);
                                                if (accountNames == null) {
                                                    accountNames = new ArrayList<>();
                                                    knownAuth2.put(account.type, accountNames);
                                                }
                                                accountNames.add(account.name);
                                            } catch (Throwable th6) {
                                                th = th6;
                                                if (accountDeleted) {
                                                }
                                                throw th;
                                            }
                                        }
                                        accountNamesByType = knownAuth2;
                                        it2 = it;
                                        userUnlocked2 = userUnlocked;
                                        knownAuth3 = knownAuth;
                                    }
                                    Iterator<Map.Entry<String, Integer>> it3 = accountNamesByType.entrySet().iterator();
                                    while (it3.hasNext()) {
                                        Map.Entry<String, Integer> next = it3.next();
                                        String accountType = next.getKey();
                                        ArrayList<String> accountNames2 = (ArrayList) next.getValue();
                                        Account[] accountsForType = new Account[accountNames2.size()];
                                        int i = 0;
                                        while (i < accountsForType.length) {
                                            accountsForType[i] = new Account(accountNames2.get(i), accountType, UUID.randomUUID().toString());
                                            i++;
                                            it3 = it3;
                                            next = next;
                                        }
                                        accounts.accountCache.put(accountType, accountsForType);
                                        it3 = it3;
                                    }
                                    accounts.visibilityCache.putAll(accountsDb.findAllVisibilityValues());
                                    AccountManager.invalidateLocalAccountsDataCaches();
                                    if (accountDeleted) {
                                        sendAccountsChangedBroadcast(accounts.userId);
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                        }
                    }
                } catch (Throwable th10) {
                    th = th10;
                    throw th;
                }
            } catch (Throwable th11) {
                th = th11;
                throw th;
            }
        }
    }

    private SparseBooleanArray getUidsOfInstalledOrUpdatedPackagesAsUser(int userId) {
        List<PackageInfo> pkgsWithData = this.mPackageManager.getInstalledPackagesAsUser(8192, userId);
        SparseBooleanArray knownUids = new SparseBooleanArray(pkgsWithData.size());
        for (PackageInfo pkgInfo : pkgsWithData) {
            if (pkgInfo.applicationInfo != null && (pkgInfo.applicationInfo.flags & 8388608) != 0) {
                knownUids.put(pkgInfo.applicationInfo.uid, true);
            }
        }
        return knownUids;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HashMap<String, Integer> getAuthenticatorTypeAndUIDForUser(Context context, int userId) {
        AccountAuthenticatorCache authCache = new AccountAuthenticatorCache(context);
        return getAuthenticatorTypeAndUIDForUser(authCache, userId);
    }

    private static HashMap<String, Integer> getAuthenticatorTypeAndUIDForUser(IAccountAuthenticatorCache authCache, int userId) {
        HashMap<String, Integer> knownAuth = new LinkedHashMap<>();
        for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> service : authCache.getAllServices(userId)) {
            knownAuth.put(((AuthenticatorDescription) service.type).type, Integer.valueOf(service.uid));
        }
        return knownAuth;
    }

    private UserAccounts getUserAccountsForCaller() {
        return getUserAccounts(UserHandle.getCallingUserId());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public UserAccounts getUserAccounts(int userId) {
        try {
            return getUserAccountsNotChecked(userId);
        } catch (RuntimeException e) {
            if (!this.mPackageManager.hasSystemFeature("android.hardware.type.automotive")) {
                throw e;
            }
            Slog.wtf(TAG, "Removing user " + userId + " due to exception (" + e + ") reading its account database");
            if (userId == ActivityManager.getCurrentUser() && userId != 0) {
                Slog.i(TAG, "Switching to system user first");
                try {
                    ActivityManager.getService().switchUser(0);
                } catch (RemoteException re) {
                    Slog.e(TAG, "Could not switch to 0: " + re);
                }
            }
            if (!getUserManager().removeUserEvenWhenDisallowed(userId)) {
                Slog.e(TAG, "could not remove user " + userId);
            }
            throw e;
        }
    }

    private UserAccounts getUserAccountsNotChecked(int userId) {
        UserAccounts accounts;
        synchronized (this.mUsers) {
            accounts = this.mUsers.get(userId);
            boolean validateAccounts = false;
            if (accounts == null) {
                File preNDbFile = new File(this.mInjector.getPreNDatabaseName(userId));
                File deDbFile = new File(this.mInjector.getDeDatabaseName(userId));
                accounts = new UserAccounts(this.mContext, userId, preNDbFile, deDbFile);
                this.mUsers.append(userId, accounts);
                purgeOldGrants(accounts);
                AccountManager.invalidateLocalAccountsDataCaches();
                validateAccounts = true;
            }
            if (!accounts.accountsDb.isCeDatabaseAttached() && this.mLocalUnlockedUsers.get(userId)) {
                Log.i(TAG, "User " + userId + " is unlocked - opening CE database");
                synchronized (accounts.dbLock) {
                    synchronized (accounts.cacheLock) {
                        File ceDatabaseFile = new File(this.mInjector.getCeDatabaseName(userId));
                        accounts.accountsDb.attachCeDatabase(ceDatabaseFile);
                    }
                }
                syncDeCeAccountsLocked(accounts);
            }
            if (validateAccounts) {
                validateAccountsInternal(accounts, true);
            }
        }
        return accounts;
    }

    private void syncDeCeAccountsLocked(UserAccounts accounts) {
        Preconditions.checkState(Thread.holdsLock(this.mUsers), "mUsers lock must be held");
        List<Account> accountsToRemove = accounts.accountsDb.findCeAccountsNotInDe();
        if (!accountsToRemove.isEmpty()) {
            Slog.i(TAG, accountsToRemove.size() + " accounts were previously deleted while user " + accounts.userId + " was locked. Removing accounts from CE tables");
            logRecord(accounts, AccountsDb.DEBUG_ACTION_SYNC_DE_CE_ACCOUNTS, "accounts");
            for (Account account : accountsToRemove) {
                removeAccountInternal(accounts, account, 1000);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void purgeOldGrantsAll() {
        synchronized (this.mUsers) {
            for (int i = 0; i < this.mUsers.size(); i++) {
                purgeOldGrants(this.mUsers.valueAt(i));
            }
        }
    }

    private void purgeOldGrants(UserAccounts accounts) {
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                List<Integer> uids = accounts.accountsDb.findAllUidGrants();
                for (Integer num : uids) {
                    int uid = num.intValue();
                    boolean packageExists = this.mPackageManager.getPackagesForUid(uid) != null;
                    if (!packageExists) {
                        Log.d(TAG, "deleting grants for UID " + uid + " because its package is no longer installed");
                        accounts.accountsDb.deleteGrantsByUid(uid);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeVisibilityValuesForPackage(String packageName) {
        if (isSpecialPackageKey(packageName)) {
            return;
        }
        synchronized (this.mUsers) {
            int numberOfUsers = this.mUsers.size();
            for (int i = 0; i < numberOfUsers; i++) {
                UserAccounts accounts = this.mUsers.valueAt(i);
                try {
                    this.mPackageManager.getPackageUidAsUser(packageName, accounts.userId);
                } catch (PackageManager.NameNotFoundException e) {
                    accounts.accountsDb.deleteAccountVisibilityForPackage(packageName);
                    synchronized (accounts.dbLock) {
                        synchronized (accounts.cacheLock) {
                            for (Account account : accounts.visibilityCache.keySet()) {
                                Map<String, Integer> accountVisibility = getPackagesAndVisibilityForAccountLocked(account, accounts);
                                accountVisibility.remove(packageName);
                            }
                            AccountManager.invalidateLocalAccountsDataCaches();
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void purgeUserData(int userId) {
        UserAccounts accounts;
        synchronized (this.mUsers) {
            accounts = this.mUsers.get(userId);
            this.mUsers.remove(userId);
            this.mLocalUnlockedUsers.delete(userId);
            AccountManager.invalidateLocalAccountsDataCaches();
        }
        if (accounts != null) {
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    accounts.accountsDb.closeDebugStatement();
                    accounts.accountsDb.close();
                }
            }
        }
    }

    void onUserUnlocked(Intent intent) {
        onUnlockUser(intent.getIntExtra("android.intent.extra.user_handle", -1));
    }

    void onUnlockUser(final int userId) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "onUserUnlocked " + userId);
        }
        synchronized (this.mUsers) {
            this.mLocalUnlockedUsers.put(userId, true);
        }
        if (userId < 1) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.accounts.AccountManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AccountManagerService.this.m813xfc56ed02(userId);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: syncSharedAccounts */
    public void m813xfc56ed02(int userId) {
        int parentUserId;
        Account[] sharedAccounts = getSharedAccountsAsUser(userId);
        if (sharedAccounts == null || sharedAccounts.length == 0) {
            return;
        }
        Account[] accounts = getAccountsAsUser(null, userId, this.mContext.getOpPackageName());
        if (UserManager.isSplitSystemUser()) {
            parentUserId = getUserManager().getUserInfo(userId).restrictedProfileParentId;
        } else {
            parentUserId = 0;
        }
        if (parentUserId < 0) {
            Log.w(TAG, "User " + userId + " has shared accounts, but no parent user");
            return;
        }
        for (Account sa : sharedAccounts) {
            if (!ArrayUtils.contains(accounts, sa)) {
                copyAccountToUser(null, sa, parentUserId, userId);
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    public void onServiceChanged(AuthenticatorDescription desc, int userId, boolean removed) {
        UserInfo user = getUserManager().getUserInfo(userId);
        if (user == null) {
            Log.w(TAG, "onServiceChanged: ignore removed user " + userId);
        } else {
            validateAccountsInternal(getUserAccounts(userId), false);
        }
    }

    public String getPassword(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getPassword: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot get secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return readPasswordInternal(accounts, account);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private String readPasswordInternal(UserAccounts accounts, Account account) {
        String findAccountPasswordByNameAndType;
        if (account == null) {
            return null;
        }
        if (!isLocalUnlockedUser(accounts.userId)) {
            Log.w(TAG, "Password is not available - user " + accounts.userId + " data is locked");
            return null;
        }
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                findAccountPasswordByNameAndType = accounts.accountsDb.findAccountPasswordByNameAndType(account.name, account.type);
            }
        }
        return findAccountPasswordByNameAndType;
    }

    public String getPreviousName(Account account) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getPreviousName: " + account + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        Objects.requireNonNull(account, "account cannot be null");
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return readPreviousNameInternal(accounts, account);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private String readPreviousNameInternal(UserAccounts accounts, Account account) {
        if (account == null) {
            return null;
        }
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                AtomicReference<String> previousNameRef = (AtomicReference) accounts.previousNameCache.get(account);
                if (previousNameRef == null) {
                    String previousName = accounts.accountsDb.findDeAccountPreviousName(account);
                    accounts.previousNameCache.put(account, new AtomicReference<>(previousName));
                    return previousName;
                }
                return previousNameRef.get();
            }
        }
    }

    public String getUserData(Account account, String key) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String msg = String.format("getUserData( account: %s, key: %s, callerUid: %s, pid: %s", account, key, Integer.valueOf(callingUid), Integer.valueOf(Binder.getCallingPid()));
            Log.v(TAG, msg);
        }
        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg2 = String.format("uid %s cannot get user data for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg2);
        } else if (!isLocalUnlockedUser(userId)) {
            Log.w(TAG, "User " + userId + " data is locked. callingUid " + callingUid);
            return null;
        } else {
            long identityToken = clearCallingIdentity();
            try {
                UserAccounts accounts = getUserAccounts(userId);
                if (accountExistsCache(accounts, account)) {
                    return readUserDataInternal(accounts, account, key);
                }
                return null;
            } finally {
                restoreCallingIdentity(identityToken);
            }
        }
    }

    public AuthenticatorDescription[] getAuthenticatorTypes(int userId) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAuthenticatorTypes: for user id " + userId + " caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s tying to get authenticator types for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        long identityToken = clearCallingIdentity();
        try {
            return getAuthenticatorTypesInternal(userId);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private AuthenticatorDescription[] getAuthenticatorTypesInternal(int userId) {
        this.mAuthenticatorCache.updateServices(userId);
        Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> authenticatorCollection = this.mAuthenticatorCache.getAllServices(userId);
        AuthenticatorDescription[] types = new AuthenticatorDescription[authenticatorCollection.size()];
        int i = 0;
        for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> authenticator : authenticatorCollection) {
            types[i] = (AuthenticatorDescription) authenticator.type;
            i++;
        }
        return types;
    }

    private boolean isCrossUser(int callingUid, int userId) {
        return (userId == UserHandle.getCallingUserId() || callingUid == 1000 || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) ? false : true;
    }

    public boolean addAccountExplicitly(Account account, String password, Bundle extras, String opPackageName) {
        return addAccountExplicitlyWithVisibility(account, password, extras, null, opPackageName);
    }

    public void copyAccountToUser(final IAccountManagerResponse response, final Account account, final int userFrom, int userTo) {
        int callingUid = Binder.getCallingUid();
        if (isCrossUser(callingUid, -1)) {
            throw new SecurityException("Calling copyAccountToUser requires android.permission.INTERACT_ACROSS_USERS_FULL");
        }
        UserAccounts fromAccounts = getUserAccounts(userFrom);
        final UserAccounts toAccounts = getUserAccounts(userTo);
        if (fromAccounts != null && toAccounts != null) {
            Slog.d(TAG, "Copying account " + account.toSafeString() + " from user " + userFrom + " to user " + userTo);
            long identityToken = clearCallingIdentity();
            try {
                try {
                    new Session(fromAccounts, response, account.type, false, false, account.name, false) { // from class: com.android.server.accounts.AccountManagerService.5
                        @Override // com.android.server.accounts.AccountManagerService.Session
                        protected String toDebugString(long now) {
                            return super.toDebugString(now) + ", getAccountCredentialsForClone, " + account.type;
                        }

                        @Override // com.android.server.accounts.AccountManagerService.Session
                        public void run() throws RemoteException {
                            this.mAuthenticator.getAccountCredentialsForCloning(this, account);
                        }

                        @Override // com.android.server.accounts.AccountManagerService.Session
                        public void onResult(Bundle result) {
                            Bundle.setDefusable(result, true);
                            if (result != null && result.getBoolean("booleanResult", false)) {
                                AccountManagerService.this.completeCloningAccount(response, result, account, toAccounts, userFrom);
                            } else {
                                super.onResult(result);
                            }
                        }
                    }.bind();
                    restoreCallingIdentity(identityToken);
                    return;
                } catch (Throwable th) {
                    th = th;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (response != null) {
            Bundle result = new Bundle();
            result.putBoolean("booleanResult", false);
            try {
                response.onResult(result);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to report error back to the client." + e);
            }
        }
    }

    public boolean accountAuthenticated(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            String msg = String.format("accountAuthenticated( account: %s, callerUid: %s)", account, Integer.valueOf(callingUid));
            Log.v(TAG, msg);
        }
        Objects.requireNonNull(account, "account cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg2 = String.format("uid %s cannot notify authentication for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg2);
        } else if (canUserModifyAccounts(userId, callingUid) && canUserModifyAccountsForType(userId, account.type, callingUid)) {
            long identityToken = clearCallingIdentity();
            try {
                getUserAccounts(userId);
                return updateLastAuthenticatedTime(account);
            } finally {
                restoreCallingIdentity(identityToken);
            }
        } else {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateLastAuthenticatedTime(Account account) {
        boolean updateAccountLastAuthenticatedTime;
        UserAccounts accounts = getUserAccountsForCaller();
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                updateAccountLastAuthenticatedTime = accounts.accountsDb.updateAccountLastAuthenticatedTime(account);
            }
        }
        return updateAccountLastAuthenticatedTime;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void completeCloningAccount(IAccountManagerResponse response, final Bundle accountCredentials, final Account account, UserAccounts targetUser, final int parentUserId) {
        Bundle.setDefusable(accountCredentials, true);
        long id = clearCallingIdentity();
        try {
            new Session(targetUser, response, account.type, false, false, account.name, false) { // from class: com.android.server.accounts.AccountManagerService.6
                @Override // com.android.server.accounts.AccountManagerService.Session
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", getAccountCredentialsForClone, " + account.type;
                }

                @Override // com.android.server.accounts.AccountManagerService.Session
                public void run() throws RemoteException {
                    Account[] accounts;
                    AccountManagerService accountManagerService = AccountManagerService.this;
                    for (Account acc : accountManagerService.getAccounts(parentUserId, accountManagerService.mContext.getOpPackageName())) {
                        if (acc.equals(account)) {
                            this.mAuthenticator.addAccountFromCredentials(this, account, accountCredentials);
                            return;
                        }
                    }
                }

                @Override // com.android.server.accounts.AccountManagerService.Session
                public void onResult(Bundle result) {
                    Bundle.setDefusable(result, true);
                    super.onResult(result);
                }

                @Override // com.android.server.accounts.AccountManagerService.Session
                public void onError(int errorCode, String errorMessage) {
                    super.onError(errorCode, errorMessage);
                }
            }.bind();
        } finally {
            restoreCallingIdentity(id);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1892=7] */
    private boolean addAccountInternal(UserAccounts accounts, Account account, String password, Bundle extras, int callingUid, Map<String, Integer> packageToVisibility, String opPackageName) {
        long accountId;
        Bundle.setDefusable(extras, true);
        if (account == null) {
            return false;
        }
        if (account.name != null && account.name.length() > 200) {
            Log.w(TAG, "Account cannot be added - Name longer than 200 chars");
            return false;
        } else if (account.type != null && account.type.length() > 200) {
            Log.w(TAG, "Account cannot be added - Name longer than 200 chars");
            return false;
        } else if (!isLocalUnlockedUser(accounts.userId)) {
            Log.w(TAG, "Account " + account.toSafeString() + " cannot be added - user " + accounts.userId + " is locked. callingUid=" + callingUid);
            return false;
        } else {
            synchronized (accounts.dbLock) {
                try {
                    try {
                        try {
                            synchronized (accounts.cacheLock) {
                                try {
                                    accounts.accountsDb.beginTransaction();
                                    if (accounts.accountsDb.findCeAccountId(account) >= 0) {
                                        Log.w(TAG, "insertAccountIntoDatabase: " + account.toSafeString() + ", skipping since the account already exists");
                                        accounts.accountsDb.endTransaction();
                                        return false;
                                    } else if (accounts.accountsDb.findAllDeAccounts().size() > 100) {
                                        Log.w(TAG, "insertAccountIntoDatabase: " + account.toSafeString() + ", skipping since more than 50 accounts on device exist");
                                        accounts.accountsDb.endTransaction();
                                        return false;
                                    } else {
                                        long accountId2 = accounts.accountsDb.insertCeAccount(account, password);
                                        if (accountId2 < 0) {
                                            Log.w(TAG, "insertAccountIntoDatabase: " + account.toSafeString() + ", skipping the DB insert failed");
                                            accounts.accountsDb.endTransaction();
                                            return false;
                                        } else if (accounts.accountsDb.insertDeAccount(account, accountId2) < 0) {
                                            Log.w(TAG, "insertAccountIntoDatabase: " + account.toSafeString() + ", skipping the DB insert failed");
                                            accounts.accountsDb.endTransaction();
                                            return false;
                                        } else {
                                            if (extras != null) {
                                                for (String key : extras.keySet()) {
                                                    String value = extras.getString(key);
                                                    if (accounts.accountsDb.insertExtra(accountId2, key, value) < 0) {
                                                        Log.w(TAG, "insertAccountIntoDatabase: " + account.toSafeString() + ", skipping since insertExtra failed for key " + key);
                                                        accounts.accountsDb.endTransaction();
                                                        return false;
                                                    }
                                                    AccountManager.invalidateLocalAccountUserDataCaches();
                                                }
                                            }
                                            if (packageToVisibility != null) {
                                                for (Map.Entry<String, Integer> entry : packageToVisibility.entrySet()) {
                                                    setAccountVisibility(account, entry.getKey(), entry.getValue().intValue(), false, accounts);
                                                    accountId2 = accountId2;
                                                }
                                                accountId = accountId2;
                                            } else {
                                                accountId = accountId2;
                                            }
                                            accounts.accountsDb.setTransactionSuccessful();
                                            logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_ADD, "accounts", accountId, accounts, callingUid);
                                            insertAccountIntoCacheLocked(accounts, account);
                                            accounts.accountsDb.endTransaction();
                                            if (getUserManager().getUserInfo(accounts.userId).canHaveProfile()) {
                                                addAccountToLinkedRestrictedUsers(account, accounts.userId);
                                            }
                                            sendNotificationAccountUpdated(account, accounts);
                                            sendAccountsChangedBroadcast(accounts.userId);
                                            logAddAccountExplicitlyMetrics(opPackageName, account.type, packageToVisibility);
                                            return true;
                                        }
                                    }
                                } catch (Throwable th) {
                                    throw th;
                                }
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        }
    }

    private void logAddAccountExplicitlyMetrics(String callerPackage, String accountType, Map<String, Integer> accountVisibility) {
        DevicePolicyEventLogger.createEvent(203).setStrings(TextUtils.emptyIfNull(accountType), TextUtils.emptyIfNull(callerPackage), findPackagesPerVisibility(accountVisibility)).write();
    }

    private String[] findPackagesPerVisibility(Map<String, Integer> accountVisibility) {
        Map<Integer, Set<String>> packagesPerVisibility = new HashMap<>();
        if (accountVisibility != null) {
            for (Map.Entry<String, Integer> entry : accountVisibility.entrySet()) {
                if (!packagesPerVisibility.containsKey(entry.getValue())) {
                    packagesPerVisibility.put(entry.getValue(), new HashSet<>());
                }
                packagesPerVisibility.get(entry.getValue()).add(entry.getKey());
            }
        }
        String[] packagesPerVisibilityStr = {getPackagesForVisibilityStr(0, packagesPerVisibility), getPackagesForVisibilityStr(1, packagesPerVisibility), getPackagesForVisibilityStr(2, packagesPerVisibility), getPackagesForVisibilityStr(3, packagesPerVisibility), getPackagesForVisibilityStr(4, packagesPerVisibility)};
        return packagesPerVisibilityStr;
    }

    private String getPackagesForVisibilityStr(int visibility, Map<Integer, Set<String>> packagesPerVisibility) {
        String str;
        StringBuilder append = new StringBuilder().append(visibility).append(":");
        if (packagesPerVisibility.containsKey(Integer.valueOf(visibility))) {
            str = TextUtils.join(",", packagesPerVisibility.get(Integer.valueOf(visibility)));
        } else {
            str = "";
        }
        return append.append(str).toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isLocalUnlockedUser(int userId) {
        boolean z;
        synchronized (this.mUsers) {
            z = this.mLocalUnlockedUsers.get(userId);
        }
        return z;
    }

    private void addAccountToLinkedRestrictedUsers(Account account, int parentUserId) {
        List<UserInfo> users = getUserManager().getUsers();
        for (UserInfo user : users) {
            if (user.isRestricted() && parentUserId == user.restrictedProfileParentId) {
                addSharedAccountAsUser(account, user.id);
                if (isLocalUnlockedUser(user.id)) {
                    MessageHandler messageHandler = this.mHandler;
                    messageHandler.sendMessage(messageHandler.obtainMessage(4, parentUserId, user.id, account));
                }
            }
        }
    }

    public void hasFeatures(IAccountManagerResponse response, Account account, String[] features, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "hasFeatures: " + account + ", response " + response + ", features " + Arrays.toString(features) + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkArgument(account != null, "account cannot be null");
        Preconditions.checkArgument(response != null, "response cannot be null");
        Preconditions.checkArgument(features != null, "features cannot be null");
        int userId = UserHandle.getCallingUserId();
        checkReadAccountsPermitted(callingUid, account.type, userId, opPackageName);
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            new TestFeaturesSession(accounts, response, account, features).bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    /* loaded from: classes.dex */
    private class TestFeaturesSession extends Session {
        private final Account mAccount;
        private final String[] mFeatures;

        public TestFeaturesSession(UserAccounts accounts, IAccountManagerResponse response, Account account, String[] features) {
            super(AccountManagerService.this, accounts, response, account.type, false, true, account.name, false);
            this.mFeatures = features;
            this.mAccount = account;
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        public void run() throws RemoteException {
            try {
                if (this.mAuthenticator != null) {
                    this.mAuthenticator.hasFeatures(this, this.mAccount, this.mFeatures);
                }
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        public void onResult(Bundle result) {
            Bundle.setDefusable(result, true);
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                try {
                    if (result == null) {
                        response.onError(5, "null bundle");
                        return;
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
                    }
                    Bundle newResult = new Bundle();
                    newResult.putBoolean("booleanResult", result.getBoolean("booleanResult", false));
                    response.onResult(newResult);
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                    }
                }
            }
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        protected String toDebugString(long now) {
            StringBuilder append = new StringBuilder().append(super.toDebugString(now)).append(", hasFeatures, ").append(this.mAccount).append(", ");
            String[] strArr = this.mFeatures;
            return append.append(strArr != null ? TextUtils.join(",", strArr) : null).toString();
        }
    }

    public void renameAccount(IAccountManagerResponse response, Account accountToRename, String newName) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "renameAccount: " + accountToRename + " -> " + newName + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (accountToRename == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (newName != null && newName.length() > 200) {
            Log.e(TAG, "renameAccount failed - account name longer than 200");
            throw new IllegalArgumentException("account name longer than 200");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(accountToRename.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot rename accounts of type: %s", Integer.valueOf(callingUid), accountToRename.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            Account resultingAccount = renameAccountInternal(accounts, accountToRename, newName);
            Bundle result = new Bundle();
            result.putString("authAccount", resultingAccount.name);
            result.putString("accountType", resultingAccount.type);
            result.putString("accountAccessId", resultingAccount.getAccessId());
            try {
                response.onResult(result);
            } catch (RemoteException e) {
                Log.w(TAG, e.getMessage());
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2162=5] */
    private Account renameAccountInternal(UserAccounts accounts, Account accountToRename, String newName) {
        Account renamedAccount;
        cancelNotification(getSigninRequiredNotificationId(accounts, accountToRename), new UserHandle(accounts.userId));
        synchronized (accounts.credentialsPermissionNotificationIds) {
            for (Pair<Pair<Account, String>, Integer> pair : accounts.credentialsPermissionNotificationIds.keySet()) {
                if (accountToRename.equals(((Pair) pair.first).first)) {
                    NotificationId id = (NotificationId) accounts.credentialsPermissionNotificationIds.get(pair);
                    cancelNotification(id, new UserHandle(accounts.userId));
                }
            }
        }
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                List<String> accountRemovedReceivers = getAccountRemovedReceivers(accountToRename, accounts);
                accounts.accountsDb.beginTransaction();
                Account renamedAccount2 = new Account(newName, accountToRename.type);
                if (accounts.accountsDb.findCeAccountId(renamedAccount2) >= 0) {
                    Log.e(TAG, "renameAccount failed - account with new name already exists");
                    accounts.accountsDb.endTransaction();
                    return null;
                }
                long accountId = accounts.accountsDb.findDeAccountId(accountToRename);
                if (accountId < 0) {
                    Log.e(TAG, "renameAccount failed - old account does not exist");
                    accounts.accountsDb.endTransaction();
                    return null;
                }
                accounts.accountsDb.renameCeAccount(accountId, newName);
                if (!accounts.accountsDb.renameDeAccount(accountId, newName, accountToRename.name)) {
                    Log.e(TAG, "renameAccount failed");
                    accounts.accountsDb.endTransaction();
                    return null;
                }
                accounts.accountsDb.setTransactionSuccessful();
                accounts.accountsDb.endTransaction();
                Account renamedAccount3 = insertAccountIntoCacheLocked(accounts, renamedAccount2);
                Map<String, String> tmpData = (Map) accounts.userDataCache.get(accountToRename);
                Map<String, String> tmpTokens = (Map) accounts.authTokenCache.get(accountToRename);
                Map<String, Integer> tmpVisibility = (Map) accounts.visibilityCache.get(accountToRename);
                removeAccountFromCacheLocked(accounts, accountToRename);
                accounts.userDataCache.put(renamedAccount3, tmpData);
                accounts.authTokenCache.put(renamedAccount3, tmpTokens);
                accounts.visibilityCache.put(renamedAccount3, tmpVisibility);
                accounts.previousNameCache.put(renamedAccount3, new AtomicReference(accountToRename.name));
                int parentUserId = accounts.userId;
                if (canHaveProfile(parentUserId)) {
                    List<UserInfo> users = getUserManager().getAliveUsers();
                    for (UserInfo user : users) {
                        if (user.isRestricted()) {
                            renamedAccount = renamedAccount3;
                            if (user.restrictedProfileParentId == parentUserId) {
                                renameSharedAccountAsUser(accountToRename, newName, user.id);
                            }
                        } else {
                            renamedAccount = renamedAccount3;
                        }
                        renamedAccount3 = renamedAccount;
                    }
                }
                sendNotificationAccountUpdated(renamedAccount3, accounts);
                sendAccountsChangedBroadcast(accounts.userId);
                for (String packageName : accountRemovedReceivers) {
                    sendAccountRemovedBroadcast(accountToRename, packageName, accounts.userId);
                }
                AccountManager.invalidateLocalAccountsDataCaches();
                AccountManager.invalidateLocalAccountUserDataCaches();
                return renamedAccount3;
            }
        }
    }

    private boolean canHaveProfile(int parentUserId) {
        UserInfo userInfo = getUserManager().getUserInfo(parentUserId);
        return userInfo != null && userInfo.canHaveProfile();
    }

    public void removeAccountAsUser(IAccountManagerResponse response, Account account, boolean expectActivityLaunch, int userId) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "removeAccount: " + account + ", response " + response + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid() + ", for user id " + userId);
        }
        Preconditions.checkArgument(account != null, "account cannot be null");
        Preconditions.checkArgument(response != null, "response cannot be null");
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s tying remove account for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        UserHandle user = UserHandle.of(userId);
        if (!isAccountManagedByCaller(account.type, callingUid, user.getIdentifier()) && !isSystemUid(callingUid) && !isProfileOwner(callingUid)) {
            String msg = String.format("uid %s cannot remove accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        } else if (canUserModifyAccounts(userId, callingUid)) {
            if (!canUserModifyAccountsForType(userId, account.type, callingUid)) {
                try {
                    response.onError(101, "User cannot modify accounts of this type (policy).");
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            long identityToken = clearCallingIdentity();
            UserAccounts accounts = getUserAccounts(userId);
            cancelNotification(getSigninRequiredNotificationId(accounts, account), user);
            synchronized (accounts.credentialsPermissionNotificationIds) {
                try {
                    for (Pair<Pair<Account, String>, Integer> pair : accounts.credentialsPermissionNotificationIds.keySet()) {
                        try {
                            if (account.equals(((Pair) pair.first).first)) {
                                NotificationId id = (NotificationId) accounts.credentialsPermissionNotificationIds.get(pair);
                                cancelNotification(id, user);
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
                    long accountId = accounts.accountsDb.findDeAccountId(account);
                    logRecord(AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_REMOVE, "accounts", accountId, accounts, callingUid);
                    try {
                        new RemoveAccountSession(accounts, response, account, expectActivityLaunch).bind();
                    } finally {
                        restoreCallingIdentity(identityToken);
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        } else {
            try {
                response.onError(100, "User cannot modify accounts");
            } catch (RemoteException e2) {
            }
        }
    }

    public boolean removeAccountExplicitly(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "removeAccountExplicitly: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        int userId = Binder.getCallingUserHandle().getIdentifier();
        if (account == null) {
            Log.e(TAG, "account is null");
            return false;
        } else if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot explicitly remove accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        } else {
            UserAccounts accounts = getUserAccountsForCaller();
            long accountId = accounts.accountsDb.findDeAccountId(account);
            logRecord(AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_REMOVE, "accounts", accountId, accounts, callingUid);
            long identityToken = clearCallingIdentity();
            try {
                return removeAccountInternal(accounts, account, callingUid);
            } finally {
                restoreCallingIdentity(identityToken);
            }
        }
    }

    /* loaded from: classes.dex */
    private class RemoveAccountSession extends Session {
        final Account mAccount;

        public RemoveAccountSession(UserAccounts accounts, IAccountManagerResponse response, Account account, boolean expectActivityLaunch) {
            super(AccountManagerService.this, accounts, response, account.type, expectActivityLaunch, true, account.name, false);
            this.mAccount = account;
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        protected String toDebugString(long now) {
            return super.toDebugString(now) + ", removeAccount, account " + this.mAccount;
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        public void run() throws RemoteException {
            this.mAuthenticator.getAccountRemovalAllowed(this, this.mAccount);
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        public void onResult(Bundle result) {
            Bundle.setDefusable(result, true);
            if (result != null && result.containsKey("booleanResult") && !result.containsKey("intent")) {
                boolean removalAllowed = result.getBoolean("booleanResult");
                if (removalAllowed) {
                    AccountManagerService.this.removeAccountInternal(this.mAccounts, this.mAccount, getCallingUid());
                }
                IAccountManagerResponse response = getResponseAndClose();
                if (response != null) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
                    }
                    try {
                        response.onResult(result);
                    } catch (RemoteException e) {
                        Slog.e(AccountManagerService.TAG, "Error calling onResult()", e);
                    }
                }
            }
            super.onResult(result);
        }
    }

    protected void removeAccountInternal(Account account) {
        removeAccountInternal(getUserAccountsForCaller(), account, getCallingUid());
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2448=4, 2426=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public boolean removeAccountInternal(UserAccounts accounts, final Account account, int callingUid) {
        long accountId;
        boolean isChanged;
        boolean userUnlocked = isLocalUnlockedUser(accounts.userId);
        if (!userUnlocked) {
            Slog.i(TAG, "Removing account " + account.toSafeString() + " while user " + accounts.userId + " is still locked. CE data will be removed later");
        }
        synchronized (accounts.dbLock) {
            try {
                synchronized (accounts.cacheLock) {
                    try {
                        Map<String, Integer> packagesToVisibility = getRequestingPackages(account, accounts);
                        List<String> accountRemovedReceivers = getAccountRemovedReceivers(account, accounts);
                        accounts.accountsDb.beginTransaction();
                        try {
                            accountId = accounts.accountsDb.findDeAccountId(account);
                            if (accountId >= 0) {
                                try {
                                    boolean isChanged2 = accounts.accountsDb.deleteDeAccount(accountId);
                                    isChanged = isChanged2;
                                } catch (Throwable th) {
                                    th = th;
                                    accounts.accountsDb.endTransaction();
                                    throw th;
                                }
                            } else {
                                isChanged = false;
                            }
                            if (userUnlocked) {
                                try {
                                    long ceAccountId = accounts.accountsDb.findCeAccountId(account);
                                    if (ceAccountId >= 0) {
                                        accounts.accountsDb.deleteCeAccount(ceAccountId);
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    accounts.accountsDb.endTransaction();
                                    throw th;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                        try {
                            accounts.accountsDb.setTransactionSuccessful();
                            try {
                                accounts.accountsDb.endTransaction();
                                if (isChanged) {
                                    try {
                                        removeAccountFromCacheLocked(accounts, account);
                                        for (Map.Entry<String, Integer> packageToVisibility : packagesToVisibility.entrySet()) {
                                            if (packageToVisibility.getValue().intValue() == 1 || packageToVisibility.getValue().intValue() == 2) {
                                                notifyPackage(packageToVisibility.getKey(), accounts);
                                            }
                                        }
                                        sendAccountsChangedBroadcast(accounts.userId);
                                        for (String packageName : accountRemovedReceivers) {
                                            sendAccountRemovedBroadcast(account, packageName, accounts.userId);
                                        }
                                        String action = userUnlocked ? AccountsDb.DEBUG_ACTION_ACCOUNT_REMOVE : AccountsDb.DEBUG_ACTION_ACCOUNT_REMOVE_DE;
                                        logRecord(action, "accounts", accountId, accounts);
                                    } catch (Throwable th4) {
                                        th = th4;
                                        throw th;
                                    }
                                }
                                try {
                                    long id = Binder.clearCallingIdentity();
                                    try {
                                        int parentUserId = accounts.userId;
                                        if (canHaveProfile(parentUserId)) {
                                            List<UserInfo> users = getUserManager().getAliveUsers();
                                            for (UserInfo user : users) {
                                                if (user.isRestricted() && parentUserId == user.restrictedProfileParentId) {
                                                    try {
                                                        removeSharedAccountAsUser(account, user.id, callingUid);
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        Binder.restoreCallingIdentity(id);
                                                        throw th;
                                                    }
                                                }
                                            }
                                        }
                                        Binder.restoreCallingIdentity(id);
                                        if (isChanged) {
                                            synchronized (accounts.credentialsPermissionNotificationIds) {
                                                for (Pair<Pair<Account, String>, Integer> key : accounts.credentialsPermissionNotificationIds.keySet()) {
                                                    if (account.equals(((Pair) key.first).first) && "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE".equals(((Pair) key.first).second)) {
                                                        final int uid = ((Integer) key.second).intValue();
                                                        this.mHandler.post(new Runnable() { // from class: com.android.server.accounts.AccountManagerService$$ExternalSyntheticLambda2
                                                            @Override // java.lang.Runnable
                                                            public final void run() {
                                                                AccountManagerService.this.m814xa7895b61(account, uid);
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        }
                                        AccountManager.invalidateLocalAccountUserDataCaches();
                                        return isChanged;
                                    } catch (Throwable th6) {
                                        th = th6;
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    throw th;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            accounts.accountsDb.endTransaction();
                            throw th;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                    }
                }
            } catch (Throwable th11) {
                th = th11;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeAccountInternal$2$com-android-server-accounts-AccountManagerService  reason: not valid java name */
    public /* synthetic */ void m814xa7895b61(Account account, int uid) {
        cancelAccountAccessRequestNotificationIfNeeded(account, uid, false);
    }

    public void invalidateAuthToken(String accountType, String authToken) {
        int callerUid = Binder.getCallingUid();
        Objects.requireNonNull(accountType, "accountType cannot be null");
        Objects.requireNonNull(authToken, "authToken cannot be null");
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "invalidateAuthToken: accountType " + accountType + ", caller's uid " + callerUid + ", pid " + Binder.getCallingPid());
        }
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            synchronized (accounts.dbLock) {
                accounts.accountsDb.beginTransaction();
                List<Pair<Account, String>> deletedTokens = invalidateAuthTokenLocked(accounts, accountType, authToken);
                accounts.accountsDb.setTransactionSuccessful();
                accounts.accountsDb.endTransaction();
                synchronized (accounts.cacheLock) {
                    for (Pair<Account, String> tokenInfo : deletedTokens) {
                        Account act = (Account) tokenInfo.first;
                        String tokenType = (String) tokenInfo.second;
                        writeAuthTokenIntoCacheLocked(accounts, act, tokenType, null);
                    }
                    accounts.accountTokenCaches.remove(accountType, authToken);
                }
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private List<Pair<Account, String>> invalidateAuthTokenLocked(UserAccounts accounts, String accountType, String authToken) {
        List<Pair<Account, String>> results = new ArrayList<>();
        Cursor cursor = accounts.accountsDb.findAuthtokenForAllAccounts(accountType, authToken);
        while (cursor.moveToNext()) {
            try {
                String authTokenId = cursor.getString(0);
                String accountName = cursor.getString(1);
                String authTokenType = cursor.getString(2);
                accounts.accountsDb.deleteAuthToken(authTokenId);
                results.add(Pair.create(new Account(accountName, accountType), authTokenType));
            } finally {
                cursor.close();
            }
        }
        return results;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveCachedToken(UserAccounts accounts, Account account, String callerPkg, byte[] callerSigDigest, String tokenType, String token, long expiryMillis) {
        if (account == null || tokenType == null || callerPkg == null) {
            return;
        }
        if (callerSigDigest == null) {
            return;
        }
        cancelNotification(getSigninRequiredNotificationId(accounts, account), UserHandle.of(accounts.userId));
        synchronized (accounts.cacheLock) {
            accounts.accountTokenCaches.put(account, token, tokenType, callerPkg, callerSigDigest, expiryMillis);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2586=4, 2587=4, 2588=4, 2589=4, 2590=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public boolean saveAuthTokenToDatabase(UserAccounts accounts, Account account, String type, String authToken) {
        if (account == null || type == null) {
            return false;
        }
        cancelNotification(getSigninRequiredNotificationId(accounts, account), UserHandle.of(accounts.userId));
        synchronized (accounts.dbLock) {
            accounts.accountsDb.beginTransaction();
            long accountId = accounts.accountsDb.findDeAccountId(account);
            if (accountId < 0) {
                accounts.accountsDb.endTransaction();
                if (0 != 0) {
                    synchronized (accounts.cacheLock) {
                        try {
                            writeAuthTokenIntoCacheLocked(accounts, account, type, authToken);
                        } catch (Throwable th) {
                            th = th;
                        }
                    }
                }
                return false;
            }
            accounts.accountsDb.deleteAuthtokensByAccountIdAndType(accountId, type);
            if (accounts.accountsDb.insertAuthToken(accountId, type, authToken) >= 0) {
                accounts.accountsDb.setTransactionSuccessful();
                accounts.accountsDb.endTransaction();
                if (1 != 0) {
                    synchronized (accounts.cacheLock) {
                        writeAuthTokenIntoCacheLocked(accounts, account, type, authToken);
                    }
                }
                return true;
            }
            accounts.accountsDb.endTransaction();
            if (0 != 0) {
                synchronized (accounts.cacheLock) {
                    try {
                        writeAuthTokenIntoCacheLocked(accounts, account, type, authToken);
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            }
            return false;
            throw th;
        }
    }

    public String peekAuthToken(Account account, String authTokenType) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "peekAuthToken: " + account + ", authTokenType " + authTokenType + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(authTokenType, "authTokenType cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot peek the authtokens associated with accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        } else if (!isLocalUnlockedUser(userId)) {
            Log.w(TAG, "Authtoken not available - user " + userId + " data is locked. callingUid " + callingUid);
            return null;
        } else {
            long identityToken = clearCallingIdentity();
            try {
                UserAccounts accounts = getUserAccounts(userId);
                return readAuthTokenInternal(accounts, account, authTokenType);
            } finally {
                restoreCallingIdentity(identityToken);
            }
        }
    }

    public void setAuthToken(Account account, String authTokenType, String authToken) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setAuthToken: " + account + ", authTokenType " + authTokenType + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(authTokenType, "authTokenType cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot set auth tokens associated with accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            saveAuthTokenToDatabase(accounts, account, authTokenType, authToken);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setPassword(Account account, String password) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setAuthToken: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Objects.requireNonNull(account, "account cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot set secrets for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            setPasswordInternal(accounts, account, password, callingUid);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void setPasswordInternal(UserAccounts accounts, Account account, String password, int callingUid) {
        String action;
        if (account == null) {
            return;
        }
        boolean isChanged = false;
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                accounts.accountsDb.beginTransaction();
                try {
                    long accountId = accounts.accountsDb.findDeAccountId(account);
                    if (accountId >= 0) {
                        accounts.accountsDb.updateCeAccountPassword(accountId, password);
                        accounts.accountsDb.deleteAuthTokensByAccountId(accountId);
                        accounts.authTokenCache.remove(account);
                        accounts.accountTokenCaches.remove(account);
                        accounts.accountsDb.setTransactionSuccessful();
                        if (password != null) {
                            try {
                                if (password.length() != 0) {
                                    action = AccountsDb.DEBUG_ACTION_SET_PASSWORD;
                                    logRecord(action, "accounts", accountId, accounts, callingUid);
                                    isChanged = true;
                                }
                            } catch (Throwable th) {
                                th = th;
                                isChanged = true;
                                accounts.accountsDb.endTransaction();
                                if (isChanged) {
                                    sendNotificationAccountUpdated(account, accounts);
                                    sendAccountsChangedBroadcast(accounts.userId);
                                }
                                throw th;
                            }
                        }
                        action = AccountsDb.DEBUG_ACTION_CLEAR_PASSWORD;
                        logRecord(action, "accounts", accountId, accounts, callingUid);
                        isChanged = true;
                    }
                    accounts.accountsDb.endTransaction();
                    if (isChanged) {
                        sendNotificationAccountUpdated(account, accounts);
                        sendAccountsChangedBroadcast(accounts.userId);
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    }

    public void clearPassword(Account account) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "clearPassword: " + account + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        Objects.requireNonNull(account, "account cannot be null");
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot clear passwords for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            setPasswordInternal(accounts, account, null, callingUid);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setUserData(Account account, String key, String value) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "setUserData: " + account + ", key " + key + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(account.type, callingUid, userId)) {
            String msg = String.format("uid %s cannot set user data for accounts of type: %s", Integer.valueOf(callingUid), account.type);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            if (!accountExistsCache(accounts, account)) {
                return;
            }
            setUserdataInternal(accounts, account, key, value);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private boolean accountExistsCache(UserAccounts accounts, Account account) {
        Account[] accountArr;
        synchronized (accounts.cacheLock) {
            if (accounts.accountCache.containsKey(account.type)) {
                for (Account acc : accounts.accountCache.get(account.type)) {
                    if (acc.name.equals(account.name)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2813=5] */
    private void setUserdataInternal(UserAccounts accounts, Account account, String key, String value) {
        synchronized (accounts.dbLock) {
            accounts.accountsDb.beginTransaction();
            long accountId = accounts.accountsDb.findDeAccountId(account);
            if (accountId < 0) {
                accounts.accountsDb.endTransaction();
                return;
            }
            long extrasId = accounts.accountsDb.findExtrasIdByAccountId(accountId, key);
            if (extrasId < 0) {
                if (accounts.accountsDb.insertExtra(accountId, key, value) < 0) {
                    accounts.accountsDb.endTransaction();
                    return;
                }
            } else if (!accounts.accountsDb.updateExtra(extrasId, value)) {
                accounts.accountsDb.endTransaction();
                return;
            }
            accounts.accountsDb.setTransactionSuccessful();
            accounts.accountsDb.endTransaction();
            synchronized (accounts.cacheLock) {
                writeUserDataIntoCacheLocked(accounts, account, key, value);
                AccountManager.invalidateLocalAccountUserDataCaches();
            }
        }
    }

    private void onResult(IAccountManagerResponse response, Bundle result) {
        if (result == null) {
            Log.e(TAG, "the result is unexpectedly null", new Exception());
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
        }
        try {
            response.onResult(result);
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
        }
    }

    public void getAuthTokenLabel(IAccountManagerResponse response, final String accountType, final String authTokenType) throws RemoteException {
        Preconditions.checkArgument(accountType != null, "accountType cannot be null");
        Preconditions.checkArgument(authTokenType != null, "authTokenType cannot be null");
        int callingUid = getCallingUid();
        clearCallingIdentity();
        if (UserHandle.getAppId(callingUid) != 1000) {
            throw new SecurityException("can only call from system");
        }
        int userId = UserHandle.getUserId(callingUid);
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            new Session(accounts, response, accountType, false, false, null, false) { // from class: com.android.server.accounts.AccountManagerService.7
                @Override // com.android.server.accounts.AccountManagerService.Session
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", getAuthTokenLabel, " + accountType + ", authTokenType " + authTokenType;
                }

                @Override // com.android.server.accounts.AccountManagerService.Session
                public void run() throws RemoteException {
                    this.mAuthenticator.getAuthTokenLabel(this, authTokenType);
                }

                @Override // com.android.server.accounts.AccountManagerService.Session
                public void onResult(Bundle result) {
                    Bundle.setDefusable(result, true);
                    if (result != null) {
                        String label = result.getString("authTokenLabelKey");
                        Bundle bundle = new Bundle();
                        bundle.putString("authTokenLabelKey", label);
                        super.onResult(bundle);
                        return;
                    }
                    super.onResult(result);
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3124=6] */
    public void getAuthToken(IAccountManagerResponse response, final Account account, final String authTokenType, final boolean notifyOnAuthFailure, boolean expectActivityLaunch, final Bundle loginOptions) {
        int callerUid;
        String callerPkg;
        String[] callerOwnedPackageNames;
        int callerUid2;
        UserAccounts accounts;
        Bundle.setDefusable(loginOptions, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAuthToken: " + account + ", response " + response + ", authTokenType " + authTokenType + ", notifyOnAuthFailure " + notifyOnAuthFailure + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkArgument(response != null, "response cannot be null");
        try {
            if (account == null) {
                Slog.w(TAG, "getAuthToken called with null account");
                response.onError(7, "account is null");
            } else if (authTokenType == null) {
                Slog.w(TAG, "getAuthToken called with null authTokenType");
                response.onError(7, "authTokenType is null");
            } else {
                int userId = UserHandle.getCallingUserId();
                long ident2 = Binder.clearCallingIdentity();
                try {
                    UserAccounts accounts2 = getUserAccounts(userId);
                    RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> authenticatorInfo = this.mAuthenticatorCache.getServiceInfo(AuthenticatorDescription.newKey(account.type), accounts2.userId);
                    final boolean customTokens = authenticatorInfo != null && ((AuthenticatorDescription) authenticatorInfo.type).customTokens;
                    int callerUid3 = Binder.getCallingUid();
                    final boolean permissionGranted = customTokens || permissionIsGranted(account, authTokenType, callerUid3, userId);
                    final String callerPkg2 = loginOptions.getString("androidPackageName");
                    ident2 = Binder.clearCallingIdentity();
                    try {
                        String[] callerOwnedPackageNames2 = this.mPackageManager.getPackagesForUid(callerUid3);
                        if (callerPkg2 == null || callerOwnedPackageNames2 == null) {
                            callerUid = callerUid3;
                            callerPkg = callerPkg2;
                        } else if (ArrayUtils.contains(callerOwnedPackageNames2, callerPkg2)) {
                            loginOptions.putInt("callerUid", callerUid3);
                            loginOptions.putInt("callerPid", Binder.getCallingPid());
                            if (notifyOnAuthFailure) {
                                loginOptions.putBoolean("notifyOnAuthFailure", true);
                            }
                            long identityToken = clearCallingIdentity();
                            try {
                                final byte[] callerPkgSigDigest = calculatePackageSignatureDigest(callerPkg2);
                                if (customTokens || !permissionGranted) {
                                    callerOwnedPackageNames = callerOwnedPackageNames2;
                                } else {
                                    try {
                                        String authToken = readAuthTokenInternal(accounts2, account, authTokenType);
                                        callerOwnedPackageNames = callerOwnedPackageNames2;
                                        if (authToken != null) {
                                            try {
                                                logGetAuthTokenMetrics(callerPkg2, account.type);
                                                Bundle result = new Bundle();
                                                result.putString("authtoken", authToken);
                                                result.putString("authAccount", account.name);
                                                result.putString("accountType", account.type);
                                                onResult(response, result);
                                                restoreCallingIdentity(identityToken);
                                                return;
                                            } catch (Throwable th) {
                                                th = th;
                                                restoreCallingIdentity(identityToken);
                                                throw th;
                                            }
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                }
                                if (customTokens) {
                                    callerUid2 = callerUid3;
                                    accounts = accounts2;
                                    try {
                                        TokenCache.Value cachedToken = readCachedTokenInternal(accounts2, account, authTokenType, callerPkg2, callerPkgSigDigest);
                                        if (cachedToken != null) {
                                            logGetAuthTokenMetrics(callerPkg2, account.type);
                                            if (Log.isLoggable(TAG, 2)) {
                                                Log.v(TAG, "getAuthToken: cache hit ofr custom token authenticator.");
                                            }
                                            Bundle result2 = new Bundle();
                                            result2.putString("authtoken", cachedToken.token);
                                            result2.putLong("android.accounts.expiry", cachedToken.expiryEpochMillis);
                                            result2.putString("authAccount", account.name);
                                            result2.putString("accountType", account.type);
                                            onResult(response, result2);
                                            restoreCallingIdentity(identityToken);
                                            return;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        restoreCallingIdentity(identityToken);
                                        throw th;
                                    }
                                } else {
                                    callerUid2 = callerUid3;
                                    accounts = accounts2;
                                }
                                try {
                                    final int i = callerUid2;
                                    final UserAccounts userAccounts = accounts;
                                    try {
                                        new Session(accounts, response, account.type, expectActivityLaunch, false, account.name, false) { // from class: com.android.server.accounts.AccountManagerService.8
                                            @Override // com.android.server.accounts.AccountManagerService.Session
                                            protected String toDebugString(long now) {
                                                Bundle bundle = loginOptions;
                                                if (bundle != null) {
                                                    bundle.keySet();
                                                }
                                                return super.toDebugString(now) + ", getAuthToken, " + account.toSafeString() + ", authTokenType " + authTokenType + ", loginOptions " + loginOptions + ", notifyOnAuthFailure " + notifyOnAuthFailure;
                                            }

                                            @Override // com.android.server.accounts.AccountManagerService.Session
                                            public void run() throws RemoteException {
                                                if (!permissionGranted) {
                                                    this.mAuthenticator.getAuthTokenLabel(this, authTokenType);
                                                    return;
                                                }
                                                this.mAuthenticator.getAuthToken(this, account, authTokenType, loginOptions);
                                                AccountManagerService.this.logGetAuthTokenMetrics(callerPkg2, account.type);
                                            }

                                            @Override // com.android.server.accounts.AccountManagerService.Session
                                            public void onResult(Bundle result3) {
                                                Bundle.setDefusable(result3, true);
                                                if (result3 != null) {
                                                    if (result3.containsKey("authTokenLabelKey")) {
                                                        Intent intent = AccountManagerService.this.newGrantCredentialsPermissionIntent(account, null, i, new AccountAuthenticatorResponse((IAccountAuthenticatorResponse) this), authTokenType, true);
                                                        Bundle bundle = new Bundle();
                                                        bundle.putParcelable("intent", intent);
                                                        onResult(bundle);
                                                        return;
                                                    }
                                                    String authToken2 = result3.getString("authtoken");
                                                    if (authToken2 != null) {
                                                        String name = result3.getString("authAccount");
                                                        String type = result3.getString("accountType");
                                                        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(name)) {
                                                            onError(5, "the type and name should not be empty");
                                                            return;
                                                        }
                                                        Account resultAccount = new Account(name, type);
                                                        if (!customTokens) {
                                                            AccountManagerService.this.saveAuthTokenToDatabase(this.mAccounts, resultAccount, authTokenType, authToken2);
                                                        }
                                                        long expiryMillis = result3.getLong("android.accounts.expiry", 0L);
                                                        if (customTokens && expiryMillis > System.currentTimeMillis()) {
                                                            AccountManagerService.this.saveCachedToken(this.mAccounts, account, callerPkg2, callerPkgSigDigest, authTokenType, authToken2, expiryMillis);
                                                        }
                                                    }
                                                    Intent intent2 = (Intent) result3.getParcelable("intent", Intent.class);
                                                    if (intent2 != null && notifyOnAuthFailure && !customTokens) {
                                                        if (!checkKeyIntent(Binder.getCallingUid(), result3)) {
                                                            onError(5, "invalid intent in bundle returned");
                                                            return;
                                                        }
                                                        AccountManagerService.this.doNotification(this.mAccounts, account, result3.getString("authFailedMessage"), intent2, PackageManagerService.PLATFORM_PACKAGE_NAME, userAccounts.userId);
                                                    }
                                                }
                                                super.onResult(result3);
                                            }
                                        }.bind();
                                        restoreCallingIdentity(identityToken);
                                        return;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        restoreCallingIdentity(identityToken);
                                        throw th;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                            }
                        } else {
                            callerUid = callerUid3;
                            callerPkg = callerPkg2;
                        }
                        String msg = String.format("Uid %s is attempting to illegally masquerade as package %s!", Integer.valueOf(callerUid), callerPkg);
                        throw new SecurityException(msg);
                    } finally {
                    }
                } finally {
                }
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to report error back to the client." + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logGetAuthTokenMetrics(String callerPackage, String accountType) {
        DevicePolicyEventLogger.createEvent(204).setStrings(new String[]{TextUtils.emptyIfNull(callerPackage), TextUtils.emptyIfNull(accountType)}).write();
    }

    private byte[] calculatePackageSignatureDigest(String callerPkg) {
        MessageDigest digester;
        Signature[] signatureArr;
        try {
            digester = MessageDigest.getInstance("SHA-256");
            PackageInfo pkgInfo = this.mPackageManager.getPackageInfo(callerPkg, 64);
            for (Signature sig : pkgInfo.signatures) {
                digester.update(sig.toByteArray());
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Could not find packageinfo for: " + callerPkg);
            digester = null;
        } catch (NoSuchAlgorithmException x) {
            Log.wtf(TAG, "SHA-256 should be available", x);
            digester = null;
        }
        if (digester == null) {
            return null;
        }
        return digester.digest();
    }

    private void createNoCredentialsPermissionNotification(Account account, Intent intent, String packageName, int userId) {
        String title;
        String subtitle;
        int uid = intent.getIntExtra(WatchlistLoggingHandler.WatchlistEventKeys.UID, -1);
        String authTokenType = intent.getStringExtra("authTokenType");
        String titleAndSubtitle = this.mContext.getString(17041139, getApplicationLabel(packageName, userId), account.name);
        int index = titleAndSubtitle.indexOf(10);
        if (index <= 0) {
            title = titleAndSubtitle;
            subtitle = "";
        } else {
            String title2 = titleAndSubtitle.substring(0, index);
            String subtitle2 = titleAndSubtitle.substring(index + 1);
            title = title2;
            subtitle = subtitle2;
        }
        UserHandle user = UserHandle.of(userId);
        Context contextForUser = getContextForUser(user);
        Notification n = new Notification.Builder(contextForUser, SystemNotificationChannels.ACCOUNT).setSmallIcon(17301642).setWhen(0L).setColor(contextForUser.getColor(17170460)).setContentTitle(title).setContentText(subtitle).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, AudioFormat.AAC_ADIF, null, user)).build();
        installNotification(getCredentialPermissionNotificationId(account, authTokenType, uid), n, PackageManagerService.PLATFORM_PACKAGE_NAME, user.getIdentifier());
    }

    private String getApplicationLabel(String packageName, int userId) {
        try {
            PackageManager packageManager = this.mPackageManager;
            return packageManager.getApplicationLabel(packageManager.getApplicationInfoAsUser(packageName, 0, userId)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Intent newGrantCredentialsPermissionIntent(Account account, String packageName, int uid, AccountAuthenticatorResponse response, String authTokenType, boolean startInNewTask) {
        Intent intent = new Intent(this.mContext, GrantCredentialsPermissionActivity.class);
        if (startInNewTask) {
            intent.setFlags(268435456);
        }
        intent.addCategory(getCredentialPermissionNotificationId(account, authTokenType, uid).mTag + (packageName != null ? packageName : ""));
        intent.putExtra("account", account);
        intent.putExtra("authTokenType", authTokenType);
        intent.putExtra("response", response);
        intent.putExtra(WatchlistLoggingHandler.WatchlistEventKeys.UID, uid);
        return intent;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public NotificationId getCredentialPermissionNotificationId(Account account, String authTokenType, int uid) {
        NotificationId nId;
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(uid));
        synchronized (accounts.credentialsPermissionNotificationIds) {
            Pair<Pair<Account, String>, Integer> key = new Pair<>(new Pair(account, authTokenType), Integer.valueOf(uid));
            nId = (NotificationId) accounts.credentialsPermissionNotificationIds.get(key);
            if (nId == null) {
                String tag = "AccountManagerService:38:" + account.hashCode() + ":" + authTokenType.hashCode() + ":" + uid;
                nId = new NotificationId(tag, 38);
                accounts.credentialsPermissionNotificationIds.put(key, nId);
            }
        }
        return nId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public NotificationId getSigninRequiredNotificationId(UserAccounts accounts, Account account) {
        NotificationId nId;
        synchronized (accounts.signinRequiredNotificationIds) {
            nId = (NotificationId) accounts.signinRequiredNotificationIds.get(account);
            if (nId == null) {
                String tag = "AccountManagerService:37:" + account.hashCode();
                nId = new NotificationId(tag, 37);
                accounts.signinRequiredNotificationIds.put(account, nId);
            }
        }
        return nId;
    }

    public void addAccount(IAccountManagerResponse response, String accountType, String authTokenType, String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn) {
        Bundle.setDefusable(optionsIn, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccount: accountType " + accountType + ", response " + response + ", authTokenType " + authTokenType + ", requiredFeatures " + Arrays.toString(requiredFeatures) + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (!canUserModifyAccounts(userId, uid)) {
            try {
                response.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
        } else if (!canUserModifyAccountsForType(userId, accountType, uid)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
        } else {
            addAccountAndLogMetrics(response, accountType, authTokenType, requiredFeatures, expectActivityLaunch, optionsIn, userId);
        }
    }

    public void addAccountAsUser(IAccountManagerResponse response, String accountType, String authTokenType, String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn, int userId) {
        boolean z;
        boolean z2;
        Bundle.setDefusable(optionsIn, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "addAccount: accountType " + accountType + ", response " + response + ", authTokenType " + authTokenType + ", requiredFeatures " + Arrays.toString(requiredFeatures) + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid() + ", for user id " + userId);
        }
        if (response != null) {
            z = true;
        } else {
            z = false;
        }
        Preconditions.checkArgument(z, "response cannot be null");
        if (accountType != null) {
            z2 = true;
        } else {
            z2 = false;
        }
        Preconditions.checkArgument(z2, "accountType cannot be null");
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s trying to add account for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        if (!canUserModifyAccounts(userId, callingUid)) {
            try {
                response.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
        } else if (!canUserModifyAccountsForType(userId, accountType, callingUid)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
        } else {
            addAccountAndLogMetrics(response, accountType, authTokenType, requiredFeatures, expectActivityLaunch, optionsIn, userId);
        }
    }

    private void addAccountAndLogMetrics(IAccountManagerResponse response, final String accountType, final String authTokenType, final String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn, int userId) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        final Bundle options = optionsIn == null ? new Bundle() : optionsIn;
        options.putInt("callerUid", uid);
        options.putInt("callerPid", pid);
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            logRecordWithUid(accounts, AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_ADD, "accounts", uid);
            try {
                new Session(accounts, response, accountType, expectActivityLaunch, true, null, false, true) { // from class: com.android.server.accounts.AccountManagerService.9
                    @Override // com.android.server.accounts.AccountManagerService.Session
                    public void run() throws RemoteException {
                        if (this.mAuthenticator != null) {
                            this.mAuthenticator.addAccount(this, this.mAccountType, authTokenType, requiredFeatures, options);
                        }
                        String callerPackage = options.getString("androidPackageName");
                        AccountManagerService.this.logAddAccountMetrics(callerPackage, accountType, requiredFeatures, authTokenType);
                    }

                    @Override // com.android.server.accounts.AccountManagerService.Session
                    protected String toDebugString(long now) {
                        String str;
                        StringBuilder append = new StringBuilder().append(super.toDebugString(now)).append(", addAccount, accountType ").append(accountType).append(", requiredFeatures ");
                        String[] strArr = requiredFeatures;
                        if (strArr != null) {
                            str = TextUtils.join(",", strArr);
                        } else {
                            str = null;
                        }
                        return append.append(str).toString();
                    }
                }.bind();
                restoreCallingIdentity(identityToken);
            } catch (Throwable th) {
                th = th;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logAddAccountMetrics(String callerPackage, String accountType, String[] requiredFeatures, String authTokenType) {
        String join;
        DevicePolicyEventLogger createEvent = DevicePolicyEventLogger.createEvent(202);
        String[] strArr = new String[4];
        strArr[0] = TextUtils.emptyIfNull(accountType);
        strArr[1] = TextUtils.emptyIfNull(callerPackage);
        strArr[2] = TextUtils.emptyIfNull(authTokenType);
        if (requiredFeatures == null) {
            join = "";
        } else {
            join = TextUtils.join(";", requiredFeatures);
        }
        strArr[3] = join;
        createEvent.setStrings(strArr).write();
    }

    public void startAddAccountSession(IAccountManagerResponse response, final String accountType, final String authTokenType, final String[] requiredFeatures, boolean expectActivityLaunch, Bundle optionsIn) {
        Bundle.setDefusable(optionsIn, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "startAddAccountSession: accountType " + accountType + ", response " + response + ", authTokenType " + authTokenType + ", requiredFeatures " + Arrays.toString(requiredFeatures) + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        Preconditions.checkArgument(response != null, "response cannot be null");
        Preconditions.checkArgument(accountType != null, "accountType cannot be null");
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (!canUserModifyAccounts(userId, uid)) {
            try {
                response.onError(100, "User is not allowed to add an account!");
            } catch (RemoteException e) {
            }
            showCantAddAccount(100, userId);
        } else if (!canUserModifyAccountsForType(userId, accountType, uid)) {
            try {
                response.onError(101, "User cannot modify accounts of this type (policy).");
            } catch (RemoteException e2) {
            }
            showCantAddAccount(101, userId);
        } else {
            int pid = Binder.getCallingPid();
            final Bundle options = optionsIn == null ? new Bundle() : optionsIn;
            options.putInt("callerUid", uid);
            options.putInt("callerPid", pid);
            final String callerPkg = options.getString("androidPackageName");
            boolean isPasswordForwardingAllowed = checkPermissionAndNote(callerPkg, uid, "android.permission.GET_PASSWORD");
            long identityToken = clearCallingIdentity();
            try {
                UserAccounts accounts = getUserAccounts(userId);
                logRecordWithUid(accounts, AccountsDb.DEBUG_ACTION_CALLED_START_ACCOUNT_ADD, "accounts", uid);
                try {
                    new StartAccountSession(accounts, response, accountType, expectActivityLaunch, null, false, true, isPasswordForwardingAllowed) { // from class: com.android.server.accounts.AccountManagerService.10
                        @Override // com.android.server.accounts.AccountManagerService.Session
                        public void run() throws RemoteException {
                            this.mAuthenticator.startAddAccountSession(this, this.mAccountType, authTokenType, requiredFeatures, options);
                            AccountManagerService.this.logAddAccountMetrics(callerPkg, accountType, requiredFeatures, authTokenType);
                        }

                        @Override // com.android.server.accounts.AccountManagerService.Session
                        protected String toDebugString(long now) {
                            String requiredFeaturesStr = TextUtils.join(",", requiredFeatures);
                            return super.toDebugString(now) + ", startAddAccountSession, accountType " + accountType + ", requiredFeatures " + (requiredFeatures != null ? requiredFeaturesStr : null);
                        }
                    }.bind();
                    restoreCallingIdentity(identityToken);
                } catch (Throwable th) {
                    th = th;
                    restoreCallingIdentity(identityToken);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* loaded from: classes.dex */
    private abstract class StartAccountSession extends Session {
        private final boolean mIsPasswordForwardingAllowed;

        public StartAccountSession(UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, String accountName, boolean authDetailsRequired, boolean updateLastAuthenticationTime, boolean isPasswordForwardingAllowed) {
            super(accounts, response, accountType, expectActivityLaunch, true, accountName, authDetailsRequired, updateLastAuthenticationTime);
            this.mIsPasswordForwardingAllowed = isPasswordForwardingAllowed;
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        public void onResult(Bundle result) {
            IAccountManagerResponse response;
            Bundle.setDefusable(result, true);
            this.mNumResults++;
            if (result != null && !checkKeyIntent(Binder.getCallingUid(), result)) {
                onError(5, "invalid intent in bundle returned");
                return;
            }
            if (this.mExpectActivityLaunch && result != null && result.containsKey("intent")) {
                response = this.mResponse;
            } else {
                response = getResponseAndClose();
            }
            if (response == null) {
                return;
            }
            if (result == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onError() on response " + response);
                }
                AccountManagerService.this.sendErrorResponse(response, 5, "null bundle returned");
            } else if (result.getInt("errorCode", -1) > 0 && 0 == 0) {
                AccountManagerService.this.sendErrorResponse(response, result.getInt("errorCode"), result.getString("errorMessage"));
            } else {
                if (!this.mIsPasswordForwardingAllowed) {
                    result.remove("password");
                }
                result.remove("authtoken");
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
                }
                Bundle sessionBundle = result.getBundle("accountSessionBundle");
                if (sessionBundle != null) {
                    String accountType = sessionBundle.getString("accountType");
                    if (TextUtils.isEmpty(accountType) || !this.mAccountType.equalsIgnoreCase(accountType)) {
                        Log.w(AccountManagerService.TAG, "Account type in session bundle doesn't match request.");
                    }
                    sessionBundle.putString("accountType", this.mAccountType);
                    try {
                        CryptoHelper cryptoHelper = CryptoHelper.getInstance();
                        Bundle encryptedBundle = cryptoHelper.encryptBundle(sessionBundle);
                        result.putBundle("accountSessionBundle", encryptedBundle);
                    } catch (GeneralSecurityException e) {
                        if (Log.isLoggable(AccountManagerService.TAG, 3)) {
                            Log.v(AccountManagerService.TAG, "Failed to encrypt session bundle!", e);
                        }
                        AccountManagerService.this.sendErrorResponse(response, 5, "failed to encrypt session bundle");
                        return;
                    }
                }
                AccountManagerService.this.sendResponse(response, result);
            }
        }
    }

    public void finishSessionAsUser(IAccountManagerResponse response, Bundle sessionBundle, boolean expectActivityLaunch, Bundle appInfo, int userId) {
        Bundle.setDefusable(sessionBundle, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "finishSession: response " + response + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + callingUid + ", caller's user id " + UserHandle.getCallingUserId() + ", pid " + Binder.getCallingPid() + ", for user id " + userId);
        }
        Preconditions.checkArgument(response != null, "response cannot be null");
        if (sessionBundle == null || sessionBundle.size() == 0) {
            throw new IllegalArgumentException("sessionBundle is empty");
        }
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s trying to finish session for %s without cross user permission", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        if (!canUserModifyAccounts(userId, callingUid)) {
            sendErrorResponse(response, 100, "User is not allowed to add an account!");
            showCantAddAccount(100, userId);
            return;
        }
        int pid = Binder.getCallingPid();
        try {
            CryptoHelper cryptoHelper = CryptoHelper.getInstance();
            final Bundle decryptedBundle = cryptoHelper.decryptBundle(sessionBundle);
            try {
                if (decryptedBundle == null) {
                    sendErrorResponse(response, 8, "failed to decrypt session bundle");
                    return;
                }
                final String accountType = decryptedBundle.getString("accountType");
                if (TextUtils.isEmpty(accountType)) {
                    sendErrorResponse(response, 7, "accountType is empty");
                    return;
                }
                if (appInfo != null) {
                    decryptedBundle.putAll(appInfo);
                }
                decryptedBundle.putInt("callerUid", callingUid);
                decryptedBundle.putInt("callerPid", pid);
                if (!canUserModifyAccountsForType(userId, accountType, callingUid)) {
                    sendErrorResponse(response, 101, "User cannot modify accounts of this type (policy).");
                    showCantAddAccount(101, userId);
                    return;
                }
                long identityToken = clearCallingIdentity();
                try {
                    UserAccounts accounts = getUserAccounts(userId);
                    logRecordWithUid(accounts, AccountsDb.DEBUG_ACTION_CALLED_ACCOUNT_SESSION_FINISH, "accounts", callingUid);
                    try {
                        new Session(accounts, response, accountType, expectActivityLaunch, true, null, false, true) { // from class: com.android.server.accounts.AccountManagerService.11
                            @Override // com.android.server.accounts.AccountManagerService.Session
                            public void run() throws RemoteException {
                                this.mAuthenticator.finishSession(this, this.mAccountType, decryptedBundle);
                            }

                            @Override // com.android.server.accounts.AccountManagerService.Session
                            protected String toDebugString(long now) {
                                return super.toDebugString(now) + ", finishSession, accountType " + accountType;
                            }
                        }.bind();
                        restoreCallingIdentity(identityToken);
                    } catch (Throwable th) {
                        th = th;
                        restoreCallingIdentity(identityToken);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (GeneralSecurityException e) {
                e = e;
                if (Log.isLoggable(TAG, 3)) {
                    Log.v(TAG, "Failed to decrypt session bundle!", e);
                }
                sendErrorResponse(response, 8, "failed to decrypt session bundle");
            }
        } catch (GeneralSecurityException e2) {
            e = e2;
        }
    }

    private void showCantAddAccount(int errorCode, int userId) {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        Intent intent = null;
        if (dpmi == null) {
            intent = getDefaultCantAddAccountIntent(errorCode);
        } else if (errorCode == 100) {
            intent = dpmi.createUserRestrictionSupportIntent(userId, "no_modify_accounts");
        } else if (errorCode == 101) {
            intent = dpmi.createShowAdminSupportIntent(userId, false);
        }
        if (intent == null) {
            intent = getDefaultCantAddAccountIntent(errorCode);
        }
        long identityToken = clearCallingIdentity();
        try {
            this.mContext.startActivityAsUser(intent, new UserHandle(userId));
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private Intent getDefaultCantAddAccountIntent(int errorCode) {
        Intent cantAddAccount = new Intent(this.mContext, CantAddAccountActivity.class);
        cantAddAccount.putExtra("android.accounts.extra.ERROR_CODE", errorCode);
        cantAddAccount.addFlags(268435456);
        return cantAddAccount;
    }

    public void confirmCredentialsAsUser(IAccountManagerResponse response, final Account account, final Bundle options, boolean expectActivityLaunch, int userId) {
        Bundle.setDefusable(options, true);
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "confirmCredentials: " + account + ", response " + response + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (isCrossUser(callingUid, userId)) {
            throw new SecurityException(String.format("User %s trying to confirm account credentials for %s", Integer.valueOf(UserHandle.getCallingUserId()), Integer.valueOf(userId)));
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            try {
                new Session(accounts, response, account.type, expectActivityLaunch, true, account.name, true, true) { // from class: com.android.server.accounts.AccountManagerService.12
                    @Override // com.android.server.accounts.AccountManagerService.Session
                    public void run() throws RemoteException {
                        this.mAuthenticator.confirmCredentials(this, account, options);
                    }

                    @Override // com.android.server.accounts.AccountManagerService.Session
                    protected String toDebugString(long now) {
                        return super.toDebugString(now) + ", confirmCredentials, " + account.toSafeString();
                    }
                }.bind();
                restoreCallingIdentity(identityToken);
            } catch (Throwable th) {
                th = th;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public void updateCredentials(IAccountManagerResponse response, final Account account, final String authTokenType, boolean expectActivityLaunch, final Bundle loginOptions) {
        Bundle.setDefusable(loginOptions, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "updateCredentials: " + account + ", response " + response + ", authTokenType " + authTokenType + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            try {
                new Session(accounts, response, account.type, expectActivityLaunch, true, account.name, false, true) { // from class: com.android.server.accounts.AccountManagerService.13
                    @Override // com.android.server.accounts.AccountManagerService.Session
                    public void run() throws RemoteException {
                        this.mAuthenticator.updateCredentials(this, account, authTokenType, loginOptions);
                    }

                    @Override // com.android.server.accounts.AccountManagerService.Session
                    protected String toDebugString(long now) {
                        Bundle bundle = loginOptions;
                        if (bundle != null) {
                            bundle.keySet();
                        }
                        return super.toDebugString(now) + ", updateCredentials, " + account.toSafeString() + ", authTokenType " + authTokenType + ", loginOptions " + loginOptions;
                    }
                }.bind();
                restoreCallingIdentity(identityToken);
            } catch (Throwable th) {
                th = th;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public void startUpdateCredentialsSession(IAccountManagerResponse response, final Account account, final String authTokenType, boolean expectActivityLaunch, final Bundle loginOptions) {
        Bundle.setDefusable(loginOptions, true);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "startUpdateCredentialsSession: " + account + ", response " + response + ", authTokenType " + authTokenType + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        String callerPkg = loginOptions.getString("androidPackageName");
        boolean isPasswordForwardingAllowed = checkPermissionAndNote(callerPkg, uid, "android.permission.GET_PASSWORD");
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            try {
                new StartAccountSession(accounts, response, account.type, expectActivityLaunch, account.name, false, true, isPasswordForwardingAllowed) { // from class: com.android.server.accounts.AccountManagerService.14
                    @Override // com.android.server.accounts.AccountManagerService.Session
                    public void run() throws RemoteException {
                        this.mAuthenticator.startUpdateCredentialsSession(this, account, authTokenType, loginOptions);
                    }

                    @Override // com.android.server.accounts.AccountManagerService.Session
                    protected String toDebugString(long now) {
                        Bundle bundle = loginOptions;
                        if (bundle != null) {
                            bundle.keySet();
                        }
                        return super.toDebugString(now) + ", startUpdateCredentialsSession, " + account.toSafeString() + ", authTokenType " + authTokenType + ", loginOptions " + loginOptions;
                    }
                }.bind();
                restoreCallingIdentity(identityToken);
            } catch (Throwable th) {
                th = th;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public void isCredentialsUpdateSuggested(IAccountManagerResponse response, final Account account, final String statusToken) {
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "isCredentialsUpdateSuggested: " + account + ", response " + response + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is null");
        }
        if (TextUtils.isEmpty(statusToken)) {
            throw new IllegalArgumentException("status token is empty");
        }
        int usrId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(usrId);
            new Session(accounts, response, account.type, false, false, account.name, false) { // from class: com.android.server.accounts.AccountManagerService.15
                @Override // com.android.server.accounts.AccountManagerService.Session
                protected String toDebugString(long now) {
                    return super.toDebugString(now) + ", isCredentialsUpdateSuggested, " + account.toSafeString();
                }

                @Override // com.android.server.accounts.AccountManagerService.Session
                public void run() throws RemoteException {
                    this.mAuthenticator.isCredentialsUpdateSuggested(this, account, statusToken);
                }

                @Override // com.android.server.accounts.AccountManagerService.Session
                public void onResult(Bundle result) {
                    Bundle.setDefusable(result, true);
                    IAccountManagerResponse response2 = getResponseAndClose();
                    if (response2 == null) {
                        return;
                    }
                    if (result == null) {
                        AccountManagerService.this.sendErrorResponse(response2, 5, "null bundle");
                        return;
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response2);
                    }
                    if (result.getInt("errorCode", -1) > 0) {
                        AccountManagerService.this.sendErrorResponse(response2, result.getInt("errorCode"), result.getString("errorMessage"));
                    } else if (!result.containsKey("booleanResult")) {
                        AccountManagerService.this.sendErrorResponse(response2, 5, "no result in response");
                    } else {
                        Bundle newResult = new Bundle();
                        newResult.putBoolean("booleanResult", result.getBoolean("booleanResult", false));
                        AccountManagerService.this.sendResponse(response2, newResult);
                    }
                }
            }.bind();
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void editProperties(IAccountManagerResponse response, final String accountType, boolean expectActivityLaunch) {
        int callingUid = Binder.getCallingUid();
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "editProperties: accountType " + accountType + ", response " + response + ", expectActivityLaunch " + expectActivityLaunch + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int userId = UserHandle.getCallingUserId();
        if (!isAccountManagedByCaller(accountType, callingUid, userId) && !isSystemUid(callingUid)) {
            String msg = String.format("uid %s cannot edit authenticator properites for account type: %s", Integer.valueOf(callingUid), accountType);
            throw new SecurityException(msg);
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            try {
                new Session(accounts, response, accountType, expectActivityLaunch, true, null, false) { // from class: com.android.server.accounts.AccountManagerService.16
                    @Override // com.android.server.accounts.AccountManagerService.Session
                    public void run() throws RemoteException {
                        this.mAuthenticator.editProperties(this, this.mAccountType);
                    }

                    @Override // com.android.server.accounts.AccountManagerService.Session
                    protected String toDebugString(long now) {
                        return super.toDebugString(now) + ", editProperties, accountType " + accountType;
                    }
                }.bind();
                restoreCallingIdentity(identityToken);
            } catch (Throwable th) {
                th = th;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public boolean hasAccountAccess(Account account, String packageName, UserHandle userHandle) {
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("Can be called only by system UID");
        }
        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(packageName, "packageName cannot be null");
        Objects.requireNonNull(userHandle, "userHandle cannot be null");
        int userId = userHandle.getIdentifier();
        Preconditions.checkArgumentInRange(userId, 0, Integer.MAX_VALUE, "user must be concrete");
        try {
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
            return hasAccountAccess(account, packageName, uid);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "hasAccountAccess#Package not found " + e.getMessage());
            return false;
        }
    }

    private String getPackageNameForUid(int uid) {
        int version;
        String[] packageNames = this.mPackageManager.getPackagesForUid(uid);
        if (ArrayUtils.isEmpty(packageNames)) {
            return null;
        }
        String packageName = packageNames[0];
        if (packageNames.length == 1) {
            return packageName;
        }
        int oldestVersion = Integer.MAX_VALUE;
        for (String name : packageNames) {
            try {
                ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(name, 0);
                if (applicationInfo != null && (version = applicationInfo.targetSdkVersion) < oldestVersion) {
                    oldestVersion = version;
                    packageName = name;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return packageName;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasAccountAccess(Account account, String packageName, int uid) {
        if (packageName == null && (packageName = getPackageNameForUid(uid)) == null) {
            return false;
        }
        if (permissionIsGranted(account, null, uid, UserHandle.getUserId(uid))) {
            return true;
        }
        int visibility = resolveAccountVisibility(account, packageName, getUserAccounts(UserHandle.getUserId(uid))).intValue();
        return visibility == 1 || visibility == 2;
    }

    public IntentSender createRequestAccountAccessIntentSenderAsUser(Account account, String packageName, UserHandle userHandle) {
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("Can be called only by system UID");
        }
        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(packageName, "packageName cannot be null");
        Objects.requireNonNull(userHandle, "userHandle cannot be null");
        int userId = userHandle.getIdentifier();
        Preconditions.checkArgumentInRange(userId, 0, Integer.MAX_VALUE, "user must be concrete");
        try {
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
            Intent intent = newRequestAccountAccessIntent(account, packageName, uid, null);
            long identity = Binder.clearCallingIdentity();
            try {
                return PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1409286144, null, new UserHandle(userId)).getIntentSender();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Unknown package " + packageName);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Intent newRequestAccountAccessIntent(final Account account, String packageName, final int uid, final RemoteCallback callback) {
        return newGrantCredentialsPermissionIntent(account, packageName, uid, new AccountAuthenticatorResponse((IAccountAuthenticatorResponse) new IAccountAuthenticatorResponse.Stub() { // from class: com.android.server.accounts.AccountManagerService.17
            public void onResult(Bundle value) throws RemoteException {
                handleAuthenticatorResponse(true);
            }

            public void onRequestContinued() {
            }

            public void onError(int errorCode, String errorMessage) throws RemoteException {
                handleAuthenticatorResponse(false);
            }

            private void handleAuthenticatorResponse(boolean accessGranted) throws RemoteException {
                AccountManagerService accountManagerService = AccountManagerService.this;
                accountManagerService.cancelNotification(accountManagerService.getCredentialPermissionNotificationId(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", uid), UserHandle.getUserHandleForUid(uid));
                if (callback != null) {
                    Bundle result = new Bundle();
                    result.putBoolean("booleanResult", accessGranted);
                    callback.sendResult(result);
                }
            }
        }), "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", false);
    }

    public boolean someUserHasAccount(Account account) {
        if (!UserHandle.isSameApp(1000, Binder.getCallingUid())) {
            throw new SecurityException("Only system can check for accounts across users");
        }
        long token = Binder.clearCallingIdentity();
        try {
            AccountAndUser[] allAccounts = getAllAccountsForSystemProcess();
            for (int i = allAccounts.length - 1; i >= 0; i--) {
                if (allAccounts[i].account.equals(account)) {
                    return true;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* loaded from: classes.dex */
    private class GetAccountsByTypeAndFeatureSession extends Session {
        private volatile Account[] mAccountsOfType;
        private volatile ArrayList<Account> mAccountsWithFeatures;
        private final int mCallingUid;
        private volatile int mCurrentAccount;
        private final String[] mFeatures;
        private final boolean mIncludeManagedNotVisible;
        private final String mPackageName;

        public GetAccountsByTypeAndFeatureSession(UserAccounts accounts, IAccountManagerResponse response, String type, String[] features, int callingUid, String packageName, boolean includeManagedNotVisible) {
            super(AccountManagerService.this, accounts, response, type, false, true, null, false);
            this.mAccountsOfType = null;
            this.mAccountsWithFeatures = null;
            this.mCurrentAccount = 0;
            this.mCallingUid = callingUid;
            this.mFeatures = features;
            this.mPackageName = packageName;
            this.mIncludeManagedNotVisible = includeManagedNotVisible;
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        public void run() throws RemoteException {
            this.mAccountsOfType = AccountManagerService.this.getAccountsFromCache(this.mAccounts, this.mAccountType, this.mCallingUid, this.mPackageName, this.mIncludeManagedNotVisible);
            this.mAccountsWithFeatures = new ArrayList<>(this.mAccountsOfType.length);
            this.mCurrentAccount = 0;
            checkAccount();
        }

        public void checkAccount() {
            if (this.mCurrentAccount >= this.mAccountsOfType.length) {
                sendResult();
                return;
            }
            IAccountAuthenticator accountAuthenticator = this.mAuthenticator;
            if (accountAuthenticator == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, "checkAccount: aborting session since we are no longer connected to the authenticator, " + toDebugString());
                    return;
                }
                return;
            }
            try {
                accountAuthenticator.hasFeatures(this, this.mAccountsOfType[this.mCurrentAccount], this.mFeatures);
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        public void onResult(Bundle result) {
            Bundle.setDefusable(result, true);
            this.mNumResults++;
            if (result == null) {
                onError(5, "null bundle");
                return;
            }
            if (result.getBoolean("booleanResult", false)) {
                this.mAccountsWithFeatures.add(this.mAccountsOfType[this.mCurrentAccount]);
            }
            this.mCurrentAccount++;
            checkAccount();
        }

        public void sendResult() {
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                try {
                    Account[] accounts = new Account[this.mAccountsWithFeatures.size()];
                    for (int i = 0; i < accounts.length; i++) {
                        accounts[i] = this.mAccountsWithFeatures.get(i);
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
                    }
                    Bundle result = new Bundle();
                    result.putParcelableArray("accounts", accounts);
                    response.onResult(result);
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                    }
                }
            }
        }

        @Override // com.android.server.accounts.AccountManagerService.Session
        protected String toDebugString(long now) {
            StringBuilder append = new StringBuilder().append(super.toDebugString(now)).append(", getAccountsByTypeAndFeatures, ");
            String[] strArr = this.mFeatures;
            return append.append(strArr != null ? TextUtils.join(",", strArr) : null).toString();
        }
    }

    public Account[] getAccounts(int userId, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        List<String> visibleAccountTypes = getTypesVisibleToCaller(callingUid, userId, opPackageName);
        if (visibleAccountTypes.isEmpty()) {
            return EMPTY_ACCOUNT_ARRAY;
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return getAccountsInternal(accounts, callingUid, opPackageName, visibleAccountTypes, false);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public AccountAndUser[] getRunningAccountsForSystem() {
        try {
            int[] runningUserIds = ActivityManager.getService().getRunningUserIds();
            return getAccountsForSystem(runningUserIds);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public AccountAndUser[] getAllAccountsForSystemProcess() {
        List<UserInfo> users = getUserManager().getAliveUsers();
        int[] userIds = new int[users.size()];
        for (int i = 0; i < userIds.length; i++) {
            userIds[i] = users.get(i).id;
        }
        return getAccountsForSystem(userIds);
    }

    private AccountAndUser[] getAccountsForSystem(int[] userIds) {
        ArrayList<AccountAndUser> runningAccounts = Lists.newArrayList();
        for (int userId : userIds) {
            UserAccounts userAccounts = getUserAccounts(userId);
            if (userAccounts != null) {
                Account[] accounts = getAccountsFromCache(userAccounts, null, Binder.getCallingUid(), PackageManagerService.PLATFORM_PACKAGE_NAME, false);
                for (Account account : accounts) {
                    runningAccounts.add(new AccountAndUser(account, userId));
                }
            }
        }
        AccountAndUser[] accountsArray = new AccountAndUser[runningAccounts.size()];
        return (AccountAndUser[]) runningAccounts.toArray(accountsArray);
    }

    public Account[] getAccountsAsUser(String type, int userId, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        return getAccountsAsUserForPackage(type, userId, opPackageName, -1, opPackageName, false);
    }

    private Account[] getAccountsAsUserForPackage(String type, int userId, String callingPackage, int packageUid, String opPackageName, boolean includeUserManagedNotVisible) {
        String opPackageName2;
        int callingUid;
        List<String> visibleAccountTypes;
        int callingUid2 = Binder.getCallingUid();
        if (userId != UserHandle.getCallingUserId() && callingUid2 != 1000 && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            throw new SecurityException("User " + UserHandle.getCallingUserId() + " trying to get account for " + userId);
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccounts: accountType " + type + ", caller's uid " + Binder.getCallingUid() + ", pid " + Binder.getCallingPid());
        }
        List<String> managedTypes = getTypesManagedByCaller(callingUid2, UserHandle.getUserId(callingUid2));
        if (packageUid != -1 && (UserHandle.isSameApp(callingUid2, 1000) || (type != null && managedTypes.contains(type)))) {
            callingUid = packageUid;
            opPackageName2 = callingPackage;
        } else {
            opPackageName2 = opPackageName;
            callingUid = callingUid2;
        }
        List<String> visibleAccountTypes2 = getTypesVisibleToCaller(callingUid, userId, opPackageName2);
        if (visibleAccountTypes2.isEmpty() || (type != null && !visibleAccountTypes2.contains(type))) {
            return EMPTY_ACCOUNT_ARRAY;
        }
        if (!visibleAccountTypes2.contains(type)) {
            visibleAccountTypes = visibleAccountTypes2;
        } else {
            List<String> arrayList = new ArrayList<>();
            arrayList.add(type);
            visibleAccountTypes = arrayList;
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts accounts = getUserAccounts(userId);
            return getAccountsInternal(accounts, callingUid, opPackageName2, visibleAccountTypes, includeUserManagedNotVisible);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private Account[] getAccountsInternal(UserAccounts userAccounts, int callingUid, String callingPackage, List<String> visibleAccountTypes, boolean includeUserManagedNotVisible) {
        ArrayList<Account> visibleAccounts = new ArrayList<>();
        for (String visibleType : visibleAccountTypes) {
            Account[] accountsForType = getAccountsFromCache(userAccounts, visibleType, callingUid, callingPackage, includeUserManagedNotVisible);
            if (accountsForType != null) {
                visibleAccounts.addAll(Arrays.asList(accountsForType));
            }
        }
        Account[] result = new Account[visibleAccounts.size()];
        for (int i = 0; i < visibleAccounts.size(); i++) {
            result[i] = visibleAccounts.get(i);
        }
        return result;
    }

    public void addSharedAccountsFromParentUser(int parentUserId, int userId, String opPackageName) {
        checkManageOrCreateUsersPermission("addSharedAccountsFromParentUser");
        Account[] accounts = getAccountsAsUser(null, parentUserId, opPackageName);
        for (Account account : accounts) {
            addSharedAccountAsUser(account, userId);
        }
    }

    private boolean addSharedAccountAsUser(Account account, int userId) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        accounts.accountsDb.deleteSharedAccount(account);
        long accountId = accounts.accountsDb.insertSharedAccount(account);
        if (accountId < 0) {
            Log.w(TAG, "insertAccountIntoDatabase: " + account.toSafeString() + ", skipping the DB insert failed");
            return false;
        }
        logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_ADD, "shared_accounts", accountId, accounts);
        return true;
    }

    public boolean renameSharedAccountAsUser(Account account, String newName, int userId) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        long sharedTableAccountId = accounts.accountsDb.findSharedAccountId(account);
        int r = accounts.accountsDb.renameSharedAccount(account, newName);
        if (r > 0) {
            int callingUid = getCallingUid();
            logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_RENAME, "shared_accounts", sharedTableAccountId, accounts, callingUid);
            renameAccountInternal(accounts, account, newName);
        }
        return r > 0;
    }

    public boolean removeSharedAccountAsUser(Account account, int userId) {
        return removeSharedAccountAsUser(account, userId, getCallingUid());
    }

    private boolean removeSharedAccountAsUser(Account account, int userId, int callingUid) {
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        long sharedTableAccountId = accounts.accountsDb.findSharedAccountId(account);
        boolean deleted = accounts.accountsDb.deleteSharedAccount(account);
        if (deleted) {
            logRecord(AccountsDb.DEBUG_ACTION_ACCOUNT_REMOVE, "shared_accounts", sharedTableAccountId, accounts, callingUid);
            removeAccountInternal(accounts, account, callingUid);
        }
        return deleted;
    }

    public Account[] getSharedAccountsAsUser(int userId) {
        Account[] accountArray;
        UserAccounts accounts = getUserAccounts(handleIncomingUser(userId));
        synchronized (accounts.dbLock) {
            List<Account> accountList = accounts.accountsDb.getSharedAccounts();
            accountArray = new Account[accountList.size()];
            accountList.toArray(accountArray);
        }
        return accountArray;
    }

    public Account[] getAccountsForPackage(String packageName, int uid, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        if (!UserHandle.isSameApp(callingUid, 1000)) {
            throw new SecurityException("getAccountsForPackage() called from unauthorized uid " + callingUid + " with uid=" + uid);
        }
        return getAccountsAsUserForPackage(null, UserHandle.getCallingUserId(), packageName, uid, opPackageName, true);
    }

    public Account[] getAccountsByTypeForPackage(String type, String packageName, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        try {
            int packageUid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
            if (!UserHandle.isSameApp(callingUid, 1000) && type != null && !isAccountManagedByCaller(type, callingUid, userId)) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            if (!UserHandle.isSameApp(callingUid, 1000) && type == null) {
                return getAccountsAsUserForPackage(type, userId, packageName, packageUid, opPackageName, false);
            }
            return getAccountsAsUserForPackage(type, userId, packageName, packageUid, opPackageName, true);
        } catch (PackageManager.NameNotFoundException re) {
            Slog.e(TAG, "Couldn't determine the packageUid for " + packageName + re);
            return EMPTY_ACCOUNT_ARRAY;
        }
    }

    private boolean needToStartChooseAccountActivity(Account[] accounts, String callingPackage) {
        if (accounts.length < 1) {
            return false;
        }
        if (accounts.length > 1) {
            return true;
        }
        Account account = accounts[0];
        UserAccounts userAccounts = getUserAccounts(UserHandle.getCallingUserId());
        int visibility = resolveAccountVisibility(account, callingPackage, userAccounts).intValue();
        return visibility == 4;
    }

    private void startChooseAccountActivityWithAccounts(IAccountManagerResponse response, Account[] accounts, String callingPackage) {
        Intent intent = new Intent(this.mContext, ChooseAccountActivity.class);
        intent.putExtra("accounts", accounts);
        intent.putExtra("accountManagerResponse", (Parcelable) new AccountManagerResponse(response));
        intent.putExtra("androidPackageName", callingPackage);
        this.mContext.startActivityAsUser(intent, UserHandle.of(UserHandle.getCallingUserId()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleGetAccountsResult(IAccountManagerResponse response, Account[] accounts, String callingPackage) {
        if (needToStartChooseAccountActivity(accounts, callingPackage)) {
            startChooseAccountActivityWithAccounts(response, accounts, callingPackage);
        } else if (accounts.length == 1) {
            Bundle bundle = new Bundle();
            bundle.putString("authAccount", accounts[0].name);
            bundle.putString("accountType", accounts[0].type);
            onResult(response, bundle);
        } else {
            onResult(response, new Bundle());
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4720=4] */
    public void getAccountByTypeAndFeatures(final IAccountManagerResponse response, String accountType, String[] features, final String opPackageName) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccount: accountType " + accountType + ", response " + response + ", features " + Arrays.toString(features) + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(userId);
            if (ArrayUtils.isEmpty(features)) {
                try {
                    Account[] accountsWithManagedNotVisible = getAccountsFromCache(userAccounts, accountType, callingUid, opPackageName, true);
                    handleGetAccountsResult(response, accountsWithManagedNotVisible, opPackageName);
                    restoreCallingIdentity(identityToken);
                    return;
                } catch (Throwable th) {
                    th = th;
                }
            } else {
                try {
                    new GetAccountsByTypeAndFeatureSession(userAccounts, new IAccountManagerResponse.Stub() { // from class: com.android.server.accounts.AccountManagerService.18
                        public void onResult(Bundle value) throws RemoteException {
                            Parcelable[] parcelables = value.getParcelableArray("accounts");
                            Account[] accounts = new Account[parcelables.length];
                            for (int i = 0; i < parcelables.length; i++) {
                                accounts[i] = (Account) parcelables[i];
                            }
                            AccountManagerService.this.handleGetAccountsResult(response, accounts, opPackageName);
                        }

                        public void onError(int errorCode, String errorMessage) throws RemoteException {
                        }
                    }, accountType, features, callingUid, opPackageName, true).bind();
                    restoreCallingIdentity(identityToken);
                    return;
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        } catch (Throwable th3) {
            th = th3;
        }
        restoreCallingIdentity(identityToken);
        throw th;
    }

    public void getAccountsByFeatures(IAccountManagerResponse response, String type, String[] features, String opPackageName) {
        int callingUid = Binder.getCallingUid();
        this.mAppOpsManager.checkPackage(callingUid, opPackageName);
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "getAccounts: accountType " + type + ", response " + response + ", features " + Arrays.toString(features) + ", caller's uid " + callingUid + ", pid " + Binder.getCallingPid());
        }
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (type == null) {
            throw new IllegalArgumentException("accountType is null");
        }
        int userId = UserHandle.getCallingUserId();
        List<String> visibleAccountTypes = getTypesVisibleToCaller(callingUid, userId, opPackageName);
        if (!visibleAccountTypes.contains(type)) {
            Bundle result = new Bundle();
            result.putParcelableArray("accounts", EMPTY_ACCOUNT_ARRAY);
            try {
                response.onResult(result);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot respond to caller do to exception.", e);
                return;
            }
        }
        long identityToken = clearCallingIdentity();
        try {
            UserAccounts userAccounts = getUserAccounts(userId);
            try {
                if (features != null && features.length != 0) {
                    new GetAccountsByTypeAndFeatureSession(userAccounts, response, type, features, callingUid, opPackageName, false).bind();
                    restoreCallingIdentity(identityToken);
                    return;
                }
                Account[] accounts = getAccountsFromCache(userAccounts, type, callingUid, opPackageName, false);
                Bundle result2 = new Bundle();
                result2.putParcelableArray("accounts", accounts);
                onResult(response, result2);
                restoreCallingIdentity(identityToken);
            } catch (Throwable th) {
                th = th;
                restoreCallingIdentity(identityToken);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public void onAccountAccessed(String token) throws RemoteException {
        Account[] accounts;
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) == 1000) {
            return;
        }
        int userId = UserHandle.getCallingUserId();
        long identity = Binder.clearCallingIdentity();
        try {
            for (Account account : getAccounts(userId, this.mContext.getOpPackageName())) {
                if (Objects.equals(account.getAccessId(), token) && !hasAccountAccess(account, (String) null, uid)) {
                    updateAppPermission(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", uid, true);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.accounts.AccountManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new AccountManagerServiceShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public abstract class Session extends IAccountAuthenticatorResponse.Stub implements IBinder.DeathRecipient, ServiceConnection {
        final String mAccountName;
        final String mAccountType;
        protected final UserAccounts mAccounts;
        final boolean mAuthDetailsRequired;
        IAccountAuthenticator mAuthenticator;
        final long mCreationTime;
        final boolean mExpectActivityLaunch;
        private int mNumErrors;
        private int mNumRequestContinued;
        public int mNumResults;
        IAccountManagerResponse mResponse;
        private final boolean mStripAuthTokenFromResult;
        final boolean mUpdateLastAuthenticatedTime;

        public abstract void run() throws RemoteException;

        public Session(AccountManagerService accountManagerService, UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, boolean stripAuthTokenFromResult, String accountName, boolean authDetailsRequired) {
            this(accounts, response, accountType, expectActivityLaunch, stripAuthTokenFromResult, accountName, authDetailsRequired, false);
        }

        public Session(UserAccounts accounts, IAccountManagerResponse response, String accountType, boolean expectActivityLaunch, boolean stripAuthTokenFromResult, String accountName, boolean authDetailsRequired, boolean updateLastAuthenticatedTime) {
            this.mNumResults = 0;
            this.mNumRequestContinued = 0;
            this.mNumErrors = 0;
            this.mAuthenticator = null;
            if (accountType == null) {
                throw new IllegalArgumentException("accountType is null");
            }
            this.mAccounts = accounts;
            this.mStripAuthTokenFromResult = stripAuthTokenFromResult;
            this.mResponse = response;
            this.mAccountType = accountType;
            this.mExpectActivityLaunch = expectActivityLaunch;
            this.mCreationTime = SystemClock.elapsedRealtime();
            this.mAccountName = accountName;
            this.mAuthDetailsRequired = authDetailsRequired;
            this.mUpdateLastAuthenticatedTime = updateLastAuthenticatedTime;
            synchronized (AccountManagerService.this.mSessions) {
                AccountManagerService.this.mSessions.put(toString(), this);
            }
            if (response != null) {
                try {
                    response.asBinder().linkToDeath(this, 0);
                } catch (RemoteException e) {
                    this.mResponse = null;
                    binderDied();
                }
            }
        }

        IAccountManagerResponse getResponseAndClose() {
            if (this.mResponse == null) {
                close();
                return null;
            }
            IAccountManagerResponse response = this.mResponse;
            close();
            return response;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4927=5] */
        protected boolean checkKeyIntent(int authUid, Bundle bundle) {
            if (!checkKeyIntentParceledCorrectly(bundle)) {
                EventLog.writeEvent(1397638484, "250588548", Integer.valueOf(authUid), "");
                return false;
            }
            Intent intent = (Intent) bundle.getParcelable("intent", Intent.class);
            if (intent == null) {
                return true;
            }
            if (intent.getClipData() == null) {
                intent.setClipData(ClipData.newPlainText(null, null));
            }
            long bid = Binder.clearCallingIdentity();
            try {
                PackageManager pm = AccountManagerService.this.mContext.getPackageManager();
                ResolveInfo resolveInfo = pm.resolveActivityAsUser(intent, 0, this.mAccounts.userId);
                if (resolveInfo == null) {
                    Binder.restoreCallingIdentity(bid);
                    return false;
                }
                ActivityInfo targetActivityInfo = resolveInfo.activityInfo;
                int targetUid = targetActivityInfo.applicationInfo.uid;
                PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                if (!isExportedSystemActivity(targetActivityInfo)) {
                    try {
                        if (!pmi.hasSignatureCapability(targetUid, authUid, 16)) {
                            String pkgName = targetActivityInfo.packageName;
                            String activityName = targetActivityInfo.name;
                            Log.e(AccountManagerService.TAG, String.format("KEY_INTENT resolved to an Activity (%s) in a package (%s) that does not share a signature with the supplying authenticator (%s).", activityName, pkgName, this.mAccountType));
                            Binder.restoreCallingIdentity(bid);
                            return false;
                        }
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(bid);
                        throw th;
                    }
                }
                Binder.restoreCallingIdentity(bid);
                return true;
            } catch (Throwable th2) {
                th = th2;
            }
        }

        private boolean checkKeyIntentParceledCorrectly(Bundle bundle) {
            Parcel p = Parcel.obtain();
            p.writeBundle(bundle);
            p.setDataPosition(0);
            Bundle simulateBundle = p.readBundle();
            p.recycle();
            Intent intent = (Intent) bundle.getParcelable("intent", Intent.class);
            Intent simulateIntent = (Intent) simulateBundle.getParcelable("intent", Intent.class);
            if (intent == null) {
                if (simulateIntent != null) {
                    return false;
                }
                return true;
            } else if (!intent.filterEquals(simulateIntent) || intent.getSelector() != simulateIntent.getSelector() || (simulateIntent.getFlags() & 195) != 0) {
                return false;
            } else {
                return true;
            }
        }

        private boolean isExportedSystemActivity(ActivityInfo activityInfo) {
            String className = activityInfo.name;
            return PackageManagerService.PLATFORM_PACKAGE_NAME.equals(activityInfo.packageName) && (GrantCredentialsPermissionActivity.class.getName().equals(className) || CantAddAccountActivity.class.getName().equals(className));
        }

        private void close() {
            synchronized (AccountManagerService.this.mSessions) {
                if (AccountManagerService.this.mSessions.remove(toString()) == null) {
                    return;
                }
                IAccountManagerResponse iAccountManagerResponse = this.mResponse;
                if (iAccountManagerResponse != null) {
                    iAccountManagerResponse.asBinder().unlinkToDeath(this, 0);
                    this.mResponse = null;
                }
                cancelTimeout();
                unbind();
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            this.mResponse = null;
            close();
        }

        protected String toDebugString() {
            return toDebugString(SystemClock.elapsedRealtime());
        }

        protected String toDebugString(long now) {
            return "Session: expectLaunch " + this.mExpectActivityLaunch + ", connected " + (this.mAuthenticator != null) + ", stats (" + this.mNumResults + SliceClientPermissions.SliceAuthority.DELIMITER + this.mNumRequestContinued + SliceClientPermissions.SliceAuthority.DELIMITER + this.mNumErrors + "), lifetime " + ((now - this.mCreationTime) / 1000.0d);
        }

        void bind() {
            if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "initiating bind to authenticator type " + this.mAccountType);
            }
            if (!bindToAuthenticator(this.mAccountType)) {
                Log.d(AccountManagerService.TAG, "bind attempt failed for " + toDebugString());
                onError(1, "bind failure");
            }
        }

        private void unbind() {
            if (this.mAuthenticator != null) {
                this.mAuthenticator = null;
                AccountManagerService.this.mContext.unbindService(this);
            }
        }

        public void cancelTimeout() {
            AccountManagerService.this.mHandler.removeMessages(3, this);
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.mAuthenticator = IAccountAuthenticator.Stub.asInterface(service);
            try {
                run();
            } catch (RemoteException e) {
                onError(1, "remote exception");
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            this.mAuthenticator = null;
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                try {
                    response.onError(1, "disconnected");
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onServiceDisconnected: caught RemoteException while responding", e);
                    }
                }
            }
        }

        public void onTimedOut() {
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                try {
                    response.onError(1, "timeout");
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onTimedOut: caught RemoteException while responding", e);
                    }
                }
            }
        }

        public void onResult(Bundle result) {
            IAccountManagerResponse response;
            boolean needUpdate = true;
            Bundle.setDefusable(result, true);
            this.mNumResults++;
            if (result != null) {
                boolean isSuccessfulConfirmCreds = result.getBoolean("booleanResult", false);
                boolean isSuccessfulUpdateCredsOrAddAccount = result.containsKey("authAccount") && result.containsKey("accountType");
                if (!this.mUpdateLastAuthenticatedTime || (!isSuccessfulConfirmCreds && !isSuccessfulUpdateCredsOrAddAccount)) {
                    needUpdate = false;
                }
                if (needUpdate || this.mAuthDetailsRequired) {
                    boolean accountPresent = AccountManagerService.this.isAccountPresentForCaller(this.mAccountName, this.mAccountType);
                    if (needUpdate && accountPresent) {
                        AccountManagerService.this.updateLastAuthenticatedTime(new Account(this.mAccountName, this.mAccountType));
                    }
                    if (this.mAuthDetailsRequired) {
                        long lastAuthenticatedTime = -1;
                        if (accountPresent) {
                            lastAuthenticatedTime = this.mAccounts.accountsDb.findAccountLastAuthenticatedTime(new Account(this.mAccountName, this.mAccountType));
                        }
                        result.putLong("lastAuthenticatedTime", lastAuthenticatedTime);
                    }
                }
            }
            if (result != null && !checkKeyIntent(Binder.getCallingUid(), result)) {
                onError(5, "invalid intent in bundle returned");
                return;
            }
            if (result != null && !TextUtils.isEmpty(result.getString("authtoken"))) {
                String accountName = result.getString("authAccount");
                String accountType = result.getString("accountType");
                if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                    Account account = new Account(accountName, accountType);
                    AccountManagerService accountManagerService = AccountManagerService.this;
                    accountManagerService.cancelNotification(accountManagerService.getSigninRequiredNotificationId(this.mAccounts, account), new UserHandle(this.mAccounts.userId));
                }
            }
            if (this.mExpectActivityLaunch && result != null && result.containsKey("intent")) {
                response = this.mResponse;
            } else {
                response = getResponseAndClose();
            }
            if (response != null) {
                try {
                    if (result == null) {
                        if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                            Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onError() on response " + response);
                        }
                        response.onError(5, "null bundle returned");
                        return;
                    }
                    if (this.mStripAuthTokenFromResult) {
                        result.remove("authtoken");
                    }
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onResult() on response " + response);
                    }
                    if (result.getInt("errorCode", -1) > 0 && 0 == 0) {
                        response.onError(result.getInt("errorCode"), result.getString("errorMessage"));
                    } else {
                        response.onResult(result);
                    }
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "failure while notifying response", e);
                    }
                }
            }
        }

        public void onRequestContinued() {
            this.mNumRequestContinued++;
        }

        public void onError(int errorCode, String errorMessage) {
            this.mNumErrors++;
            IAccountManagerResponse response = getResponseAndClose();
            if (response != null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, getClass().getSimpleName() + " calling onError() on response " + response);
                }
                try {
                    response.onError(errorCode, errorMessage);
                } catch (RemoteException e) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "Session.onError: caught RemoteException while responding", e);
                    }
                }
            } else if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                Log.v(AccountManagerService.TAG, "Session.onError: already closed");
            }
        }

        private boolean bindToAuthenticator(String authenticatorType) {
            RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> authenticatorInfo = AccountManagerService.this.mAuthenticatorCache.getServiceInfo(AuthenticatorDescription.newKey(authenticatorType), this.mAccounts.userId);
            if (authenticatorInfo == null) {
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, "there is no authenticator for " + authenticatorType + ", bailing out");
                }
                return false;
            } else if (!AccountManagerService.this.isLocalUnlockedUser(this.mAccounts.userId) && !authenticatorInfo.componentInfo.directBootAware) {
                Slog.w(AccountManagerService.TAG, "Blocking binding to authenticator " + authenticatorInfo.componentName + " which isn't encryption aware");
                return false;
            } else {
                Intent intent = new Intent();
                intent.setAction("android.accounts.AccountAuthenticator");
                intent.setComponent(authenticatorInfo.componentName);
                if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                    Log.v(AccountManagerService.TAG, "performing bindService to " + authenticatorInfo.componentName);
                }
                int flags = AccountManagerService.this.mAuthenticatorCache.getBindInstantServiceAllowed(this.mAccounts.userId) ? 1 | 4194304 : 1;
                if (!AccountManagerService.this.mContext.bindServiceAsUser(intent, this, flags, UserHandle.of(this.mAccounts.userId))) {
                    if (Log.isLoggable(AccountManagerService.TAG, 2)) {
                        Log.v(AccountManagerService.TAG, "bindService to " + authenticatorInfo.componentName + " failed");
                    }
                    return false;
                }
                return true;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MessageHandler extends Handler {
        MessageHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    Session session = (Session) msg.obj;
                    session.onTimedOut();
                    return;
                case 4:
                    AccountManagerService.this.copyAccountToUser(null, (Account) msg.obj, msg.arg1, msg.arg2);
                    return;
                default:
                    throw new IllegalStateException("unhandled message: " + msg.what);
            }
        }
    }

    private void logRecord(UserAccounts accounts, String action, String tableName) {
        logRecord(action, tableName, -1L, accounts);
    }

    private void logRecordWithUid(UserAccounts accounts, String action, String tableName, int uid) {
        logRecord(action, tableName, -1L, accounts, uid);
    }

    private void logRecord(String action, String tableName, long accountId, UserAccounts userAccount) {
        logRecord(action, tableName, accountId, userAccount, getCallingUid());
    }

    private void logRecord(String action, String tableName, long accountId, UserAccounts userAccount, int callingUid) {
        long insertionPoint = userAccount.accountsDb.reserveDebugDbInsertionPoint();
        if (insertionPoint != -1) {
            this.mHandler.post(new Runnable(action, tableName, accountId, userAccount, callingUid, insertionPoint) { // from class: com.android.server.accounts.AccountManagerService.1LogRecordTask
                private final long accountId;
                private final String action;
                private final int callingUid;
                private final String tableName;
                private final UserAccounts userAccount;
                private final long userDebugDbInsertionPoint;

                {
                    this.action = action;
                    this.tableName = tableName;
                    this.accountId = accountId;
                    this.userAccount = userAccount;
                    this.callingUid = callingUid;
                    this.userDebugDbInsertionPoint = insertionPoint;
                }

                @Override // java.lang.Runnable
                public void run() {
                    synchronized (this.userAccount.accountsDb.mDebugStatementLock) {
                        SQLiteStatement logStatement = this.userAccount.accountsDb.getStatementForLogging();
                        if (logStatement == null) {
                            return;
                        }
                        logStatement.bindLong(1, this.accountId);
                        logStatement.bindString(2, this.action);
                        logStatement.bindString(3, AccountManagerService.this.mDateFormat.format(new Date()));
                        logStatement.bindLong(4, this.callingUid);
                        logStatement.bindString(5, this.tableName);
                        logStatement.bindLong(6, this.userDebugDbInsertionPoint);
                        try {
                            logStatement.execute();
                        } catch (SQLiteFullException | IllegalStateException e) {
                            Slog.w(AccountManagerService.TAG, "Failed to insert a log record. accountId=" + this.accountId + " action=" + this.action + " tableName=" + this.tableName + " Error: " + e);
                        }
                        logStatement.clearBindings();
                    }
                }
            });
        }
    }

    public IBinder onBind(Intent intent) {
        return asBinder();
    }

    private static boolean scanArgs(String[] args, String value) {
        if (args != null) {
            for (String arg : args) {
                if (value.equals(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, fout)) {
            boolean isCheckinRequest = scanArgs(args, "--checkin") || scanArgs(args, "-c");
            IndentingPrintWriter ipw = new IndentingPrintWriter(fout, "  ");
            List<UserInfo> users = getUserManager().getUsers();
            for (UserInfo user : users) {
                ipw.println("User " + user + ":");
                ipw.increaseIndent();
                dumpUser(getUserAccounts(user.id), fd, ipw, args, isCheckinRequest);
                ipw.println();
                ipw.decreaseIndent();
            }
        }
    }

    private void dumpUser(UserAccounts userAccounts, FileDescriptor fd, PrintWriter fout, String[] args, boolean isCheckinRequest) {
        boolean isUserUnlocked;
        if (isCheckinRequest) {
            synchronized (userAccounts.dbLock) {
                userAccounts.accountsDb.dumpDeAccountsTable(fout);
            }
            return;
        }
        Account[] accounts = getAccountsFromCache(userAccounts, null, 1000, PackageManagerService.PLATFORM_PACKAGE_NAME, false);
        fout.println("Accounts: " + accounts.length);
        int length = accounts.length;
        for (int i = 0; i < length; i++) {
            fout.println("  " + accounts[i].toString());
        }
        fout.println();
        synchronized (userAccounts.dbLock) {
            try {
                userAccounts.accountsDb.dumpDebugTable(fout);
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
        fout.println();
        synchronized (this.mSessions) {
            try {
                long now = SystemClock.elapsedRealtime();
                fout.println("Active Sessions: " + this.mSessions.size());
                for (Session session : this.mSessions.values()) {
                    fout.println("  " + session.toDebugString(now));
                }
            } catch (Throwable th3) {
                th = th3;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
                throw th;
            }
        }
        fout.println();
        this.mAuthenticatorCache.dump(fd, fout, args, userAccounts.userId);
        synchronized (this.mUsers) {
            isUserUnlocked = isLocalUnlockedUser(userAccounts.userId);
        }
        if (!isUserUnlocked) {
            return;
        }
        fout.println();
        synchronized (userAccounts.dbLock) {
            Map<Account, Map<String, Integer>> allVisibilityValues = userAccounts.accountsDb.findAllVisibilityValues();
            fout.println("Account visibility:");
            for (Account account : allVisibilityValues.keySet()) {
                fout.println("  " + account.name);
                Map<String, Integer> visibilities = allVisibilityValues.get(account);
                for (Map.Entry<String, Integer> entry : visibilities.entrySet()) {
                    fout.println("    " + entry.getKey() + ", " + entry.getValue());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doNotification(UserAccounts accounts, Account account, CharSequence message, Intent intent, String packageName, int userId) {
        long identityToken = clearCallingIdentity();
        try {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "doNotification: " + ((Object) message) + " intent:" + intent);
            }
            if (intent.getComponent() == null || !GrantCredentialsPermissionActivity.class.getName().equals(intent.getComponent().getClassName())) {
                Context contextForUser = getContextForUser(new UserHandle(userId));
                NotificationId id = getSigninRequiredNotificationId(accounts, account);
                intent.addCategory(id.mTag);
                String notificationTitleFormat = contextForUser.getText(17040927).toString();
                Notification n = new Notification.Builder(contextForUser, SystemNotificationChannels.ACCOUNT).setWhen(0L).setSmallIcon(17301642).setColor(contextForUser.getColor(17170460)).setContentTitle(String.format(notificationTitleFormat, account.name)).setContentText(message).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, AudioFormat.AAC_ADIF, null, new UserHandle(userId))).build();
                installNotification(id, n, packageName, userId);
            } else {
                createNoCredentialsPermissionNotification(account, intent, packageName, userId);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    private void installNotification(NotificationId id, Notification notification, String packageName, int userId) {
        long token = clearCallingIdentity();
        try {
            INotificationManager notificationManager = this.mInjector.getNotificationManager();
            try {
                notificationManager.enqueueNotificationWithTag(packageName, PackageManagerService.PLATFORM_PACKAGE_NAME, id.mTag, id.mId, notification, userId);
            } catch (RemoteException e) {
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelNotification(NotificationId id, UserHandle user) {
        cancelNotification(id, this.mContext.getPackageName(), user);
    }

    private void cancelNotification(NotificationId id, String packageName, UserHandle user) {
        long identityToken = clearCallingIdentity();
        try {
            INotificationManager service = this.mInjector.getNotificationManager();
            service.cancelNotificationWithTag(packageName, PackageManagerService.PLATFORM_PACKAGE_NAME, id.mTag, id.mId, user.getIdentifier());
        } catch (RemoteException e) {
        } catch (Throwable th) {
            restoreCallingIdentity(identityToken);
            throw th;
        }
        restoreCallingIdentity(identityToken);
    }

    private boolean isPermittedForPackage(String packageName, int userId, String... permissions) {
        int opCode;
        long identity = Binder.clearCallingIdentity();
        try {
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
            IPackageManager pm = ActivityThread.getPackageManager();
            for (String perm : permissions) {
                if (pm.checkPermission(perm, packageName, userId) == 0 && ((opCode = AppOpsManager.permissionToOpCode(perm)) == -1 || this.mAppOpsManager.checkOpNoThrow(opCode, uid, packageName) == 0)) {
                    Binder.restoreCallingIdentity(identity);
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
        Binder.restoreCallingIdentity(identity);
        return false;
    }

    private boolean checkPermissionAndNote(String opPackageName, int callingUid, String... permissions) {
        for (String perm : permissions) {
            if (this.mContext.checkCallingOrSelfPermission(perm) == 0) {
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "  caller uid " + callingUid + " has " + perm);
                }
                int opCode = AppOpsManager.permissionToOpCode(perm);
                if (opCode == -1 || this.mAppOpsManager.noteOpNoThrow(opCode, callingUid, opPackageName) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int handleIncomingUser(int userId) {
        try {
            return ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, "", (String) null);
        } catch (RemoteException e) {
            return userId;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5588=4] */
    private boolean isPrivileged(int callingUid) {
        long identityToken = Binder.clearCallingIdentity();
        try {
            String[] packages = this.mPackageManager.getPackagesForUid(callingUid);
            if (packages == null) {
                Log.d(TAG, "No packages for callingUid " + callingUid);
                return false;
            }
            for (String name : packages) {
                try {
                    PackageInfo packageInfo = this.mPackageManager.getPackageInfo(name, 0);
                    if (packageInfo != null && (packageInfo.applicationInfo.privateFlags & 8) != 0) {
                        Binder.restoreCallingIdentity(identityToken);
                        return true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "isPrivileged#Package not found " + e.getMessage());
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private boolean permissionIsGranted(Account account, String authTokenType, int callerUid, int userId) {
        if (UserHandle.getAppId(callerUid) == 1000) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Access to " + account + " granted calling uid is system");
            }
            return true;
        } else if (isPrivileged(callerUid)) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Access to " + account + " granted calling uid " + callerUid + " privileged");
            }
            return true;
        } else if (account != null && isAccountManagedByCaller(account.type, callerUid, userId)) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Access to " + account + " granted calling uid " + callerUid + " manages the account");
            }
            return true;
        } else if (account != null && hasExplicitlyGrantedPermission(account, authTokenType, callerUid)) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "Access to " + account + " granted calling uid " + callerUid + " user granted access");
            }
            return true;
        } else if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "Access to " + account + " not granted for uid " + callerUid);
            return false;
        } else {
            return false;
        }
    }

    private boolean isAccountVisibleToCaller(String accountType, int callingUid, int userId, String opPackageName) {
        if (accountType == null) {
            return false;
        }
        return getTypesVisibleToCaller(callingUid, userId, opPackageName).contains(accountType);
    }

    private boolean checkGetAccountsPermission(String packageName, int userId) {
        return isPermittedForPackage(packageName, userId, "android.permission.GET_ACCOUNTS", "android.permission.GET_ACCOUNTS_PRIVILEGED");
    }

    private boolean checkReadContactsPermission(String packageName, int userId) {
        return isPermittedForPackage(packageName, userId, "android.permission.READ_CONTACTS");
    }

    private boolean accountTypeManagesContacts(String accountType, int userId) {
        if (accountType == null) {
            return false;
        }
        long identityToken = Binder.clearCallingIdentity();
        try {
            Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> serviceInfos = this.mAuthenticatorCache.getAllServices(userId);
            Binder.restoreCallingIdentity(identityToken);
            for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo : serviceInfos) {
                if (accountType.equals(((AuthenticatorDescription) serviceInfo.type).type)) {
                    return isPermittedForPackage(((AuthenticatorDescription) serviceInfo.type).packageName, userId, "android.permission.WRITE_CONTACTS");
                }
            }
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
            throw th;
        }
    }

    private int checkPackageSignature(String accountType, int callingUid, int userId) {
        if (accountType == null) {
            return 0;
        }
        long identityToken = Binder.clearCallingIdentity();
        try {
            Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> serviceInfos = this.mAuthenticatorCache.getAllServices(userId);
            Binder.restoreCallingIdentity(identityToken);
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo : serviceInfos) {
                if (accountType.equals(((AuthenticatorDescription) serviceInfo.type).type)) {
                    if (serviceInfo.uid == callingUid) {
                        return 2;
                    }
                    if (pmi.hasSignatureCapability(serviceInfo.uid, callingUid, 16)) {
                        return 1;
                    }
                }
            }
            return 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
            throw th;
        }
    }

    private boolean isAccountManagedByCaller(String accountType, int callingUid, int userId) {
        if (accountType == null) {
            return false;
        }
        return getTypesManagedByCaller(callingUid, userId).contains(accountType);
    }

    private List<String> getTypesVisibleToCaller(int callingUid, int userId, String opPackageName) {
        return getTypesForCaller(callingUid, userId, true);
    }

    private List<String> getTypesManagedByCaller(int callingUid, int userId) {
        return getTypesForCaller(callingUid, userId, false);
    }

    private List<String> getTypesForCaller(int callingUid, int userId, boolean isOtherwisePermitted) {
        List<String> managedAccountTypes = new ArrayList<>();
        long identityToken = Binder.clearCallingIdentity();
        try {
            Collection<RegisteredServicesCache.ServiceInfo<AuthenticatorDescription>> serviceInfos = this.mAuthenticatorCache.getAllServices(userId);
            Binder.restoreCallingIdentity(identityToken);
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            for (RegisteredServicesCache.ServiceInfo<AuthenticatorDescription> serviceInfo : serviceInfos) {
                if (isOtherwisePermitted || pmi.hasSignatureCapability(serviceInfo.uid, callingUid, 16)) {
                    managedAccountTypes.add(((AuthenticatorDescription) serviceInfo.type).type);
                }
            }
            return managedAccountTypes;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAccountPresentForCaller(String accountName, String accountType) {
        Account[] accountArr;
        if (getUserAccountsForCaller().accountCache.containsKey(accountType)) {
            for (Account account : getUserAccountsForCaller().accountCache.get(accountType)) {
                if (account.name.equals(accountName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void checkManageUsersPermission(String message) {
        if (ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", Binder.getCallingUid(), -1, true) != 0) {
            throw new SecurityException("You need MANAGE_USERS permission to: " + message);
        }
    }

    private static void checkManageOrCreateUsersPermission(String message) {
        if (ActivityManager.checkComponentPermission("android.permission.MANAGE_USERS", Binder.getCallingUid(), -1, true) != 0 && ActivityManager.checkComponentPermission("android.permission.CREATE_USERS", Binder.getCallingUid(), -1, true) != 0) {
            throw new SecurityException("You need MANAGE_USERS or CREATE_USERS permission to: " + message);
        }
    }

    private boolean hasExplicitlyGrantedPermission(Account account, String authTokenType, int callerUid) {
        long grantsCount;
        if (UserHandle.getAppId(callerUid) == 1000) {
            return true;
        }
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(callerUid));
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                if (authTokenType != null) {
                    grantsCount = accounts.accountsDb.findMatchingGrantsCount(callerUid, authTokenType, account);
                } else {
                    grantsCount = accounts.accountsDb.findMatchingGrantsCountAnyToken(callerUid, account);
                }
                boolean permissionGranted = grantsCount > 0;
                if (permissionGranted || !ActivityManager.isRunningInTestHarness()) {
                    return permissionGranted;
                }
                Log.d(TAG, "no credentials permission for usage of " + account.toSafeString() + ", " + authTokenType + " by uid " + callerUid + " but ignoring since device is in test harness.");
                return true;
            }
        }
    }

    private boolean isSystemUid(int callingUid) {
        long ident = Binder.clearCallingIdentity();
        try {
            String[] packages = this.mPackageManager.getPackagesForUid(callingUid);
            if (packages == null) {
                Log.w(TAG, "No known packages with uid " + callingUid);
            } else {
                for (String name : packages) {
                    try {
                        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(name, 0);
                        if (packageInfo != null && (packageInfo.applicationInfo.flags & 1) != 0) {
                            return true;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, String.format("Could not find package [%s]", name), e);
                    }
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void checkReadAccountsPermitted(int callingUid, String accountType, int userId, String opPackageName) {
        if (!isAccountVisibleToCaller(accountType, callingUid, userId, opPackageName)) {
            String msg = String.format("caller uid %s cannot access %s accounts", Integer.valueOf(callingUid), accountType);
            Log.w(TAG, "  " + msg);
            throw new SecurityException(msg);
        }
    }

    private boolean canUserModifyAccounts(int userId, int callingUid) {
        return isProfileOwner(callingUid) || !getUserManager().getUserRestrictions(new UserHandle(userId)).getBoolean("no_modify_accounts");
    }

    private boolean canUserModifyAccountsForType(int userId, String accountType, int callingUid) {
        if (isProfileOwner(callingUid)) {
            return true;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        String[] typesArray = dpm.getAccountTypesWithManagementDisabledAsUser(userId);
        if (typesArray == null) {
            return true;
        }
        for (String forbiddenType : typesArray) {
            if (forbiddenType.equals(accountType)) {
                return false;
            }
        }
        return true;
    }

    private boolean isProfileOwner(int uid) {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        return dpmi != null && (dpmi.isActiveProfileOwner(uid) || dpmi.isActiveDeviceOwner(uid));
    }

    public void updateAppPermission(Account account, String authTokenType, int uid, boolean value) throws RemoteException {
        int callingUid = getCallingUid();
        if (UserHandle.getAppId(callingUid) != 1000) {
            throw new SecurityException();
        }
        if (value) {
            grantAppPermission(account, authTokenType, uid);
        } else {
            revokeAppPermission(account, authTokenType, uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantAppPermission(final Account account, String authTokenType, final int uid) {
        if (account == null || authTokenType == null) {
            Log.e(TAG, "grantAppPermission: called with invalid arguments", new Exception());
            return;
        }
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(uid));
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                long accountId = accounts.accountsDb.findDeAccountId(account);
                if (accountId >= 0) {
                    accounts.accountsDb.insertGrant(accountId, authTokenType, uid);
                }
                cancelNotification(getCredentialPermissionNotificationId(account, authTokenType, uid), UserHandle.of(accounts.userId));
                cancelAccountAccessRequestNotificationIfNeeded(account, uid, true);
            }
        }
        Iterator<AccountManagerInternal.OnAppPermissionChangeListener> it = this.mAppPermissionChangeListeners.iterator();
        while (it.hasNext()) {
            final AccountManagerInternal.OnAppPermissionChangeListener listener = it.next();
            this.mHandler.post(new Runnable() { // from class: com.android.server.accounts.AccountManagerService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    listener.onAppPermissionChanged(account, uid);
                }
            });
        }
    }

    private void revokeAppPermission(final Account account, String authTokenType, final int uid) {
        if (account == null || authTokenType == null) {
            Log.e(TAG, "revokeAppPermission: called with invalid arguments", new Exception());
            return;
        }
        UserAccounts accounts = getUserAccounts(UserHandle.getUserId(uid));
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                accounts.accountsDb.beginTransaction();
                try {
                    long accountId = accounts.accountsDb.findDeAccountId(account);
                    if (accountId >= 0) {
                        accounts.accountsDb.deleteGrantsByAccountIdAuthTokenTypeAndUid(accountId, authTokenType, uid);
                        accounts.accountsDb.setTransactionSuccessful();
                    }
                    accounts.accountsDb.endTransaction();
                    cancelNotification(getCredentialPermissionNotificationId(account, authTokenType, uid), UserHandle.of(accounts.userId));
                } catch (Throwable th) {
                    accounts.accountsDb.endTransaction();
                    throw th;
                }
            }
        }
        Iterator<AccountManagerInternal.OnAppPermissionChangeListener> it = this.mAppPermissionChangeListeners.iterator();
        while (it.hasNext()) {
            final AccountManagerInternal.OnAppPermissionChangeListener listener = it.next();
            this.mHandler.post(new Runnable() { // from class: com.android.server.accounts.AccountManagerService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    listener.onAppPermissionChanged(account, uid);
                }
            });
        }
    }

    private void removeAccountFromCacheLocked(UserAccounts accounts, Account account) {
        Account[] oldAccountsForType = accounts.accountCache.get(account.type);
        if (oldAccountsForType != null) {
            ArrayList<Account> newAccountsList = new ArrayList<>();
            for (Account curAccount : oldAccountsForType) {
                if (!curAccount.equals(account)) {
                    newAccountsList.add(curAccount);
                }
            }
            if (newAccountsList.isEmpty()) {
                accounts.accountCache.remove(account.type);
            } else {
                Account[] newAccountsForType = new Account[newAccountsList.size()];
                accounts.accountCache.put(account.type, (Account[]) newAccountsList.toArray(newAccountsForType));
            }
        }
        accounts.userDataCache.remove(account);
        accounts.authTokenCache.remove(account);
        accounts.previousNameCache.remove(account);
        accounts.visibilityCache.remove(account);
        AccountManager.invalidateLocalAccountsDataCaches();
    }

    private Account insertAccountIntoCacheLocked(UserAccounts accounts, Account account) {
        Account[] accountsForType = accounts.accountCache.get(account.type);
        int oldLength = accountsForType != null ? accountsForType.length : 0;
        Account[] newAccountsForType = new Account[oldLength + 1];
        if (accountsForType != null) {
            System.arraycopy(accountsForType, 0, newAccountsForType, 0, oldLength);
        }
        String token = account.getAccessId() != null ? account.getAccessId() : UUID.randomUUID().toString();
        newAccountsForType[oldLength] = new Account(account, token);
        accounts.accountCache.put(account.type, newAccountsForType);
        AccountManager.invalidateLocalAccountsDataCaches();
        return newAccountsForType[oldLength];
    }

    private Account[] filterAccounts(UserAccounts accounts, Account[] unfiltered, int callingUid, String callingPackage, boolean includeManagedNotVisible) {
        String visibilityFilterPackage = callingPackage;
        if (visibilityFilterPackage == null) {
            visibilityFilterPackage = getPackageNameForUid(callingUid);
        }
        Map<Account, Integer> firstPass = new LinkedHashMap<>();
        for (Account account : unfiltered) {
            int visibility = resolveAccountVisibility(account, visibilityFilterPackage, accounts).intValue();
            if (visibility == 1 || visibility == 2 || (includeManagedNotVisible && visibility == 4)) {
                firstPass.put(account, Integer.valueOf(visibility));
            }
        }
        Map<Account, Integer> secondPass = filterSharedAccounts(accounts, firstPass, callingUid, callingPackage);
        Account[] filtered = new Account[secondPass.size()];
        return (Account[]) secondPass.keySet().toArray(filtered);
    }

    private Map<Account, Integer> filterSharedAccounts(UserAccounts userAccounts, Map<Account, Integer> unfiltered, int callingUid, String callingPackage) {
        String[] packages;
        if (getUserManager() == null || userAccounts == null || userAccounts.userId < 0 || callingUid == 1000) {
            return unfiltered;
        }
        UserInfo user = getUserManager().getUserInfo(userAccounts.userId);
        if (user != null && user.isRestricted()) {
            String[] packages2 = this.mPackageManager.getPackagesForUid(callingUid);
            int i = 0;
            if (packages2 != null) {
                packages = packages2;
            } else {
                packages = new String[0];
            }
            String visibleList = this.mContext.getResources().getString(17039883);
            for (String packageName : packages) {
                if (visibleList.contains(";" + packageName + ";")) {
                    return unfiltered;
                }
            }
            Account[] sharedAccounts = getSharedAccountsAsUser(userAccounts.userId);
            if (ArrayUtils.isEmpty(sharedAccounts)) {
                return unfiltered;
            }
            String requiredAccountType = "";
            try {
                if (callingPackage != null) {
                    PackageInfo pi = this.mPackageManager.getPackageInfo(callingPackage, 0);
                    if (pi != null && pi.restrictedAccountType != null) {
                        requiredAccountType = pi.restrictedAccountType;
                    }
                } else {
                    int length = packages.length;
                    int i2 = 0;
                    while (true) {
                        if (i2 >= length) {
                            break;
                        }
                        String packageName2 = packages[i2];
                        PackageInfo pi2 = this.mPackageManager.getPackageInfo(packageName2, 0);
                        if (pi2 == null || pi2.restrictedAccountType == null) {
                            i2++;
                        } else {
                            requiredAccountType = pi2.restrictedAccountType;
                            break;
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "filterSharedAccounts#Package not found " + e.getMessage());
            }
            Map<Account, Integer> filtered = new LinkedHashMap<>();
            for (Map.Entry<Account, Integer> entry : unfiltered.entrySet()) {
                Account account = entry.getKey();
                if (account.type.equals(requiredAccountType)) {
                    filtered.put(account, entry.getValue());
                } else {
                    boolean found = false;
                    int length2 = sharedAccounts.length;
                    int i3 = i;
                    while (true) {
                        if (i3 >= length2) {
                            break;
                        }
                        Account shared = sharedAccounts[i3];
                        if (!shared.equals(account)) {
                            i3++;
                        } else {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        filtered.put(account, entry.getValue());
                    }
                }
                i = 0;
            }
            return filtered;
        }
        return unfiltered;
    }

    protected Account[] getAccountsFromCache(UserAccounts userAccounts, String accountType, int callingUid, String callingPackage, boolean includeManagedNotVisible) {
        Account[] accounts;
        Preconditions.checkState(!Thread.holdsLock(userAccounts.cacheLock), "Method should not be called with cacheLock");
        if (accountType != null) {
            synchronized (userAccounts.cacheLock) {
                accounts = userAccounts.accountCache.get(accountType);
            }
            if (accounts == null) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            return filterAccounts(userAccounts, (Account[]) Arrays.copyOf(accounts, accounts.length), callingUid, callingPackage, includeManagedNotVisible);
        }
        int totalLength = 0;
        synchronized (userAccounts.cacheLock) {
            for (Account[] accounts2 : userAccounts.accountCache.values()) {
                totalLength += accounts2.length;
            }
            if (totalLength == 0) {
                return EMPTY_ACCOUNT_ARRAY;
            }
            Account[] accountsArray = new Account[totalLength];
            int totalLength2 = 0;
            for (Account[] accountsOfType : userAccounts.accountCache.values()) {
                System.arraycopy(accountsOfType, 0, accountsArray, totalLength2, accountsOfType.length);
                totalLength2 += accountsOfType.length;
            }
            return filterAccounts(userAccounts, accountsArray, callingUid, callingPackage, includeManagedNotVisible);
        }
    }

    protected void writeUserDataIntoCacheLocked(UserAccounts accounts, Account account, String key, String value) {
        Map<String, String> userDataForAccount = (Map) accounts.userDataCache.get(account);
        if (userDataForAccount == null) {
            userDataForAccount = accounts.accountsDb.findUserExtrasForAccount(account);
            accounts.userDataCache.put(account, userDataForAccount);
        }
        if (value == null) {
            userDataForAccount.remove(key);
        } else {
            userDataForAccount.put(key, value);
        }
    }

    protected TokenCache.Value readCachedTokenInternal(UserAccounts accounts, Account account, String tokenType, String callingPackage, byte[] pkgSigDigest) {
        TokenCache.Value value;
        synchronized (accounts.cacheLock) {
            value = accounts.accountTokenCaches.get(account, tokenType, callingPackage, pkgSigDigest);
        }
        return value;
    }

    protected void writeAuthTokenIntoCacheLocked(UserAccounts accounts, Account account, String key, String value) {
        Map<String, String> authTokensForAccount = (Map) accounts.authTokenCache.get(account);
        if (authTokensForAccount == null) {
            authTokensForAccount = accounts.accountsDb.findAuthTokensByAccount(account);
            accounts.authTokenCache.put(account, authTokensForAccount);
        }
        if (value == null) {
            authTokensForAccount.remove(key);
        } else {
            authTokensForAccount.put(key, value);
        }
    }

    protected String readAuthTokenInternal(UserAccounts accounts, Account account, String authTokenType) {
        String str;
        synchronized (accounts.cacheLock) {
            Map<String, String> authTokensForAccount = (Map) accounts.authTokenCache.get(account);
            if (authTokensForAccount != null) {
                return authTokensForAccount.get(authTokenType);
            }
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    Map<String, String> authTokensForAccount2 = (Map) accounts.authTokenCache.get(account);
                    if (authTokensForAccount2 == null) {
                        authTokensForAccount2 = accounts.accountsDb.findAuthTokensByAccount(account);
                        accounts.authTokenCache.put(account, authTokensForAccount2);
                    }
                    str = authTokensForAccount2.get(authTokenType);
                }
            }
            return str;
        }
    }

    private String readUserDataInternal(UserAccounts accounts, Account account, String key) {
        Map<String, String> userDataForAccount;
        synchronized (accounts.cacheLock) {
            userDataForAccount = (Map) accounts.userDataCache.get(account);
        }
        if (userDataForAccount == null) {
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    userDataForAccount = (Map) accounts.userDataCache.get(account);
                    if (userDataForAccount == null) {
                        userDataForAccount = accounts.accountsDb.findUserExtrasForAccount(account);
                        accounts.userDataCache.put(account, userDataForAccount);
                    }
                }
            }
        }
        return userDataForAccount.get(key);
    }

    private Context getContextForUser(UserHandle user) {
        try {
            Context context = this.mContext;
            return context.createPackageContextAsUser(context.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            return this.mContext;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendResponse(IAccountManagerResponse response, Bundle result) {
        try {
            response.onResult(result);
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendErrorResponse(IAccountManagerResponse response, int errorCode, String errorMessage) {
        try {
            response.onError(errorCode, errorMessage);
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "failure while notifying response", e);
            }
        }
    }

    /* loaded from: classes.dex */
    private final class AccountManagerInternalImpl extends AccountManagerInternal {
        private AccountManagerBackupHelper mBackupHelper;
        private final Object mLock;

        private AccountManagerInternalImpl() {
            this.mLock = new Object();
        }

        public void requestAccountAccess(Account account, String packageName, int userId, RemoteCallback callback) {
            UserAccounts userAccounts;
            if (account == null) {
                Slog.w(AccountManagerService.TAG, "account cannot be null");
            } else if (packageName == null) {
                Slog.w(AccountManagerService.TAG, "packageName cannot be null");
            } else if (userId < 0) {
                Slog.w(AccountManagerService.TAG, "user id must be concrete");
            } else if (callback == null) {
                Slog.w(AccountManagerService.TAG, "callback cannot be null");
            } else {
                AccountManagerService accountManagerService = AccountManagerService.this;
                int visibility = accountManagerService.resolveAccountVisibility(account, packageName, accountManagerService.getUserAccounts(userId)).intValue();
                if (visibility == 3) {
                    Slog.w(AccountManagerService.TAG, "requestAccountAccess: account is hidden");
                } else if (AccountManagerService.this.hasAccountAccess(account, packageName, new UserHandle(userId))) {
                    Bundle result = new Bundle();
                    result.putBoolean("booleanResult", true);
                    callback.sendResult(result);
                } else {
                    try {
                        long identityToken = Binder.clearCallingIdentity();
                        int uid = AccountManagerService.this.mPackageManager.getPackageUidAsUser(packageName, userId);
                        Binder.restoreCallingIdentity(identityToken);
                        Intent intent = AccountManagerService.this.newRequestAccountAccessIntent(account, packageName, uid, callback);
                        synchronized (AccountManagerService.this.mUsers) {
                            userAccounts = (UserAccounts) AccountManagerService.this.mUsers.get(userId);
                        }
                        SystemNotificationChannels.createAccountChannelForPackage(packageName, uid, AccountManagerService.this.mContext);
                        AccountManagerService.this.doNotification(userAccounts, account, null, intent, packageName, userId);
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.e(AccountManagerService.TAG, "Unknown package " + packageName);
                    }
                }
            }
        }

        public void addOnAppPermissionChangeListener(AccountManagerInternal.OnAppPermissionChangeListener listener) {
            AccountManagerService.this.mAppPermissionChangeListeners.add(listener);
        }

        public boolean hasAccountAccess(Account account, int uid) {
            return AccountManagerService.this.hasAccountAccess(account, (String) null, uid);
        }

        public byte[] backupAccountAccessPermissions(int userId) {
            byte[] backupAccountAccessPermissions;
            synchronized (this.mLock) {
                if (this.mBackupHelper == null) {
                    this.mBackupHelper = new AccountManagerBackupHelper(AccountManagerService.this, this);
                }
                backupAccountAccessPermissions = this.mBackupHelper.backupAccountAccessPermissions(userId);
            }
            return backupAccountAccessPermissions;
        }

        public void restoreAccountAccessPermissions(byte[] data, int userId) {
            synchronized (this.mLock) {
                if (this.mBackupHelper == null) {
                    this.mBackupHelper = new AccountManagerBackupHelper(AccountManagerService.this, this);
                }
                this.mBackupHelper.restoreAccountAccessPermissions(data, userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        private final Context mContext;

        public Injector(Context context) {
            this.mContext = context;
        }

        Looper getMessageHandlerLooper() {
            ServiceThread serviceThread = new ServiceThread(AccountManagerService.TAG, -2, true);
            serviceThread.start();
            return serviceThread.getLooper();
        }

        Context getContext() {
            return this.mContext;
        }

        void addLocalService(AccountManagerInternal service) {
            LocalServices.addService(AccountManagerInternal.class, service);
        }

        String getDeDatabaseName(int userId) {
            File databaseFile = new File(Environment.getDataSystemDeDirectory(userId), "accounts_de.db");
            return databaseFile.getPath();
        }

        String getCeDatabaseName(int userId) {
            File databaseFile = new File(Environment.getDataSystemCeDirectory(userId), "accounts_ce.db");
            return databaseFile.getPath();
        }

        String getPreNDatabaseName(int userId) {
            File systemDir = Environment.getDataSystemDirectory();
            File databaseFile = new File(Environment.getUserSystemDirectory(userId), AccountManagerService.PRE_N_DATABASE_NAME);
            if (userId == 0) {
                File oldFile = new File(systemDir, AccountManagerService.PRE_N_DATABASE_NAME);
                if (oldFile.exists() && !databaseFile.exists()) {
                    File userDir = Environment.getUserSystemDirectory(userId);
                    if (!userDir.exists() && !userDir.mkdirs()) {
                        throw new IllegalStateException("User dir cannot be created: " + userDir);
                    }
                    if (!oldFile.renameTo(databaseFile)) {
                        throw new IllegalStateException("User dir cannot be migrated: " + databaseFile);
                    }
                }
            }
            return databaseFile.getPath();
        }

        IAccountAuthenticatorCache getAccountAuthenticatorCache() {
            return new AccountAuthenticatorCache(this.mContext);
        }

        INotificationManager getNotificationManager() {
            return NotificationManager.getService();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class NotificationId {
        private final int mId;
        final String mTag;

        NotificationId(String tag, int type) {
            this.mTag = tag;
            this.mId = type;
        }
    }
}
