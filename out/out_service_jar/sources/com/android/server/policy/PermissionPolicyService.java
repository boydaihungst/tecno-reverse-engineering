package com.android.server.policy;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.AppOpsManagerInternal;
import android.app.KeyguardManager;
import android.app.TaskInfo;
import android.app.compat.CompatChanges;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.permission.LegacyPermissionManager;
import android.permission.PermissionControllerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.LongSparseLongArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.R;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.policy.AttributeCache;
import com.android.internal.util.IntPair;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.notification.NotificationManagerInternal;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.policy.PermissionPolicyInternal;
import com.android.server.policy.PermissionPolicyService;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.wm.ActivityInterceptorCallback;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public final class PermissionPolicyService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = PermissionPolicyService.class.getSimpleName();
    private static final long NOTIFICATION_PERM_CHANGE_ID = 194833441;
    private static final String SYSTEM_PKG = "android";
    private static final long USER_SENSITIVE_UPDATE_DELAY_MS = 60000;
    private List<String> mAppOpPermissions;
    private IAppOpsCallback mAppOpsCallback;
    private boolean mBootCompleted;
    private Context mContext;
    private final Handler mHandler;
    private final ArraySet<Pair<String, Integer>> mIsPackageSyncsScheduled;
    private final SparseBooleanArray mIsStarted;
    private final SparseBooleanArray mIsUidSyncScheduled;
    private final KeyguardManager mKeyguardManager;
    private final Object mLock;
    private NotificationManagerInternal mNotificationManager;
    private PermissionPolicyInternal.OnInitializedCallback mOnInitializedCallback;
    private final PackageManager mPackageManager;
    private PackageManagerInternal mPackageManagerInternal;
    private PermissionManagerServiceInternal mPermissionManagerInternal;
    private final ArrayList<PhoneCarrierPrivilegesCallback> mPhoneCarrierPrivilegesCallbacks;
    private final BroadcastReceiver mSimConfigBroadcastReceiver;
    private TelephonyManager mTelephonyManager;

    public PermissionPolicyService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mBootCompleted = false;
        this.mIsStarted = new SparseBooleanArray();
        this.mIsPackageSyncsScheduled = new ArraySet<>();
        this.mIsUidSyncScheduled = new SparseBooleanArray();
        this.mPhoneCarrierPrivilegesCallbacks = new ArrayList<>();
        this.mSimConfigBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.PermissionPolicyService.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (!"android.telephony.action.MULTI_SIM_CONFIG_CHANGED".equals(intent.getAction())) {
                    return;
                }
                PermissionPolicyService.this.unregisterCarrierPrivilegesCallback();
                PermissionPolicyService.this.registerCarrierPrivilegesCallbacks();
            }
        };
        this.mContext = context;
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mPackageManager = context.getPackageManager();
        this.mKeyguardManager = (KeyguardManager) context.getSystemService(KeyguardManager.class);
        LocalServices.addService(PermissionPolicyInternal.class, new Internal());
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // com.android.server.SystemService
    public void onStart() {
        char c;
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mPermissionManagerInternal = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
        IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        this.mPackageManagerInternal.getPackageList(new PackageManagerInternal.PackageListObserver() { // from class: com.android.server.policy.PermissionPolicyService.1
            @Override // android.content.pm.PackageManagerInternal.PackageListObserver
            public void onPackageAdded(String packageName, int uid) {
                int userId = UserHandle.getUserId(uid);
                if (PermissionPolicyService.this.isStarted(userId)) {
                    PermissionPolicyService.this.synchronizePackagePermissionsAndAppOpsForUser(packageName, userId);
                }
            }

            @Override // android.content.pm.PackageManagerInternal.PackageListObserver
            public void onPackageChanged(String packageName, int uid) {
                int userId = UserHandle.getUserId(uid);
                if (PermissionPolicyService.this.isStarted(userId)) {
                    PermissionPolicyService.this.synchronizePackagePermissionsAndAppOpsForUser(packageName, userId);
                    PermissionPolicyService.this.resetAppOpPermissionsIfNotRequestedForUid(uid);
                }
            }

            @Override // android.content.pm.PackageManagerInternal.PackageListObserver
            public void onPackageRemoved(String packageName, int uid) {
                int userId = UserHandle.getUserId(uid);
                if (PermissionPolicyService.this.isStarted(userId)) {
                    PermissionPolicyService.this.resetAppOpPermissionsIfNotRequestedForUid(uid);
                }
            }
        });
        this.mPermissionManagerInternal.addOnRuntimePermissionStateChangedListener(new PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener() { // from class: com.android.server.policy.PermissionPolicyService$$ExternalSyntheticLambda4
            @Override // com.android.server.pm.permission.PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener
            public final void onRuntimePermissionStateChanged(String str, int i) {
                PermissionPolicyService.this.synchronizePackagePermissionsAndAppOpsAsyncForUser(str, i);
            }
        });
        this.mAppOpsCallback = new IAppOpsCallback.Stub() { // from class: com.android.server.policy.PermissionPolicyService.2
            public void opChanged(int op, int uid, String packageName) {
                PermissionPolicyService.this.synchronizePackagePermissionsAndAppOpsAsyncForUser(packageName, UserHandle.getUserId(uid));
                PermissionPolicyService.this.resetAppOpPermissionsIfNotRequestedForUidAsync(uid);
            }
        };
        ArrayList<PermissionInfo> dangerousPerms = this.mPermissionManagerInternal.getAllPermissionsWithProtection(1);
        try {
            int numDangerousPerms = dangerousPerms.size();
            for (int i = 0; i < numDangerousPerms; i++) {
                PermissionInfo perm = dangerousPerms.get(i);
                if (perm.isRuntime()) {
                    appOpsService.startWatchingMode(getSwitchOp(perm.name), (String) null, this.mAppOpsCallback);
                }
                if (perm.isSoftRestricted()) {
                    SoftRestrictedPermissionPolicy policy = SoftRestrictedPermissionPolicy.forPermission(null, null, null, null, perm.name);
                    int extraAppOp = policy.getExtraAppOpCode();
                    if (extraAppOp != -1) {
                        appOpsService.startWatchingMode(extraAppOp, (String) null, this.mAppOpsCallback);
                    }
                }
            }
        } catch (RemoteException e) {
            Slog.wtf(LOG_TAG, "Cannot set up app-ops listener");
        }
        List<PermissionInfo> appOpPermissionInfos = this.mPermissionManagerInternal.getAllPermissionsWithProtectionFlags(64);
        this.mAppOpPermissions = new ArrayList();
        int appOpPermissionInfosSize = appOpPermissionInfos.size();
        for (int i2 = 0; i2 < appOpPermissionInfosSize; i2++) {
            PermissionInfo appOpPermissionInfo = appOpPermissionInfos.get(i2);
            String str = appOpPermissionInfo.name;
            switch (str.hashCode()) {
                case 309844284:
                    if (str.equals("android.permission.MANAGE_IPSEC_TUNNELS")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1353874541:
                    if (str.equals("android.permission.ACCESS_NOTIFICATIONS")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1777263169:
                    if (str.equals("android.permission.REQUEST_INSTALL_PACKAGES")) {
                        c = 2;
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
                case 2:
                    break;
                default:
                    int appOpCode = AppOpsManager.permissionToOpCode(appOpPermissionInfo.name);
                    if (appOpCode != -1) {
                        this.mAppOpPermissions.add(appOpPermissionInfo.name);
                        try {
                            appOpsService.startWatchingMode(appOpCode, (String) null, this.mAppOpsCallback);
                            break;
                        } catch (RemoteException e2) {
                            Slog.wtf(LOG_TAG, "Cannot set up app-ops listener", e2);
                            break;
                        }
                    } else {
                        break;
                    }
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme("package");
        getContext().registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.policy.PermissionPolicyService.3
            final List<Integer> mUserSetupUids = new ArrayList(200);
            final Map<UserHandle, PermissionControllerManager> mPermControllerManagers = new HashMap();

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean hasSetupRun = true;
                try {
                    ContentResolver cr = PermissionPolicyService.this.getContext().getContentResolver();
                    hasSetupRun = Settings.Secure.getIntForUser(cr, "user_setup_complete", cr.getUserId()) != 0;
                } catch (Settings.SettingNotFoundException e3) {
                }
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (PermissionPolicyService.this.mPackageManagerInternal.getPackage(uid) == null) {
                    return;
                }
                if (hasSetupRun) {
                    if (!this.mUserSetupUids.isEmpty()) {
                        synchronized (this.mUserSetupUids) {
                            for (int i3 = this.mUserSetupUids.size() - 1; i3 >= 0; i3--) {
                                updateUid(this.mUserSetupUids.get(i3).intValue());
                            }
                            this.mUserSetupUids.clear();
                        }
                    }
                    updateUid(uid);
                    return;
                }
                synchronized (this.mUserSetupUids) {
                    if (!this.mUserSetupUids.contains(Integer.valueOf(uid))) {
                        this.mUserSetupUids.add(Integer.valueOf(uid));
                    }
                }
            }

            private void updateUid(int uid) {
                UserHandle user = UserHandle.getUserHandleForUid(uid);
                PermissionControllerManager manager = this.mPermControllerManagers.get(user);
                if (manager == null) {
                    manager = new PermissionControllerManager(PermissionPolicyService.getUserContext(PermissionPolicyService.this.getContext(), user), FgThread.getHandler());
                    this.mPermControllerManagers.put(user, manager);
                }
                manager.updateUserSensitiveForApp(uid);
            }
        }, UserHandle.ALL, intentFilter, null, null);
        final PermissionControllerManager manager = new PermissionControllerManager(getUserContext(getContext(), Process.myUserHandle()), FgThread.getHandler());
        Handler handler = FgThread.getHandler();
        Objects.requireNonNull(manager);
        handler.postDelayed(new Runnable() { // from class: com.android.server.policy.PermissionPolicyService$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                manager.updateUserSensitive();
            }
        }, 60000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getSwitchOp(String permission) {
        int op = AppOpsManager.permissionToOpCode(permission);
        if (op == -1) {
            return -1;
        }
        return AppOpsManager.opToSwitch(op);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void synchronizePackagePermissionsAndAppOpsAsyncForUser(String packageName, int changedUserId) {
        if (isStarted(changedUserId)) {
            synchronized (this.mLock) {
                if (this.mIsPackageSyncsScheduled.add(new Pair<>(packageName, Integer.valueOf(changedUserId)))) {
                    FgThread.getHandler().sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.policy.PermissionPolicyService$$ExternalSyntheticLambda2
                        public final void accept(Object obj, Object obj2, Object obj3) {
                            ((PermissionPolicyService) obj).synchronizePackagePermissionsAndAppOpsForUser((String) obj2, ((Integer) obj3).intValue());
                        }
                    }, this, packageName, Integer.valueOf(changedUserId)));
                }
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        int[] userIds;
        if (phase == 520) {
            registerCarrierPrivilegesCallbacks();
            IntentFilter filter = new IntentFilter("android.telephony.action.MULTI_SIM_CONFIG_CHANGED");
            this.mContext.registerReceiver(this.mSimConfigBroadcastReceiver, filter);
        }
        if (phase == 550) {
            UserManagerInternal um = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            for (int userId : um.getUserIds()) {
                if (um.isUserRunning(userId)) {
                    onStartUser(userId);
                }
            }
        }
        if (phase == 550) {
            ((Internal) LocalServices.getService(PermissionPolicyInternal.class)).onActivityManagerReady();
        }
        if (phase == 1000) {
            synchronized (this.mLock) {
                this.mBootCompleted = true;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initTelephonyManagerIfNeeded() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = TelephonyManager.from(this.mContext);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerCarrierPrivilegesCallbacks() {
        initTelephonyManagerIfNeeded();
        TelephonyManager telephonyManager = this.mTelephonyManager;
        if (telephonyManager == null) {
            return;
        }
        int numPhones = telephonyManager.getActiveModemCount();
        for (int i = 0; i < numPhones; i++) {
            PhoneCarrierPrivilegesCallback callback = new PhoneCarrierPrivilegesCallback(i);
            this.mPhoneCarrierPrivilegesCallbacks.add(callback);
            this.mTelephonyManager.registerCarrierPrivilegesCallback(i, this.mContext.getMainExecutor(), callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterCarrierPrivilegesCallback() {
        initTelephonyManagerIfNeeded();
        if (this.mTelephonyManager == null) {
            return;
        }
        for (int i = 0; i < this.mPhoneCarrierPrivilegesCallbacks.size(); i++) {
            PhoneCarrierPrivilegesCallback callback = this.mPhoneCarrierPrivilegesCallbacks.get(i);
            if (callback != null) {
                this.mTelephonyManager.unregisterCarrierPrivilegesCallback(callback);
            }
        }
        this.mPhoneCarrierPrivilegesCallbacks.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class PhoneCarrierPrivilegesCallback implements TelephonyManager.CarrierPrivilegesCallback {
        private int mPhoneId;

        PhoneCarrierPrivilegesCallback(int phoneId) {
            this.mPhoneId = phoneId;
        }

        public void onCarrierPrivilegesChanged(Set<String> privilegedPackageNames, Set<Integer> privilegedUids) {
            PermissionPolicyService.this.initTelephonyManagerIfNeeded();
            if (PermissionPolicyService.this.mTelephonyManager == null) {
                Log.e(PermissionPolicyService.LOG_TAG, "Cannot grant default permissions to Carrier Service app. TelephonyManager is null");
                return;
            }
            String servicePkg = PermissionPolicyService.this.mTelephonyManager.getCarrierServicePackageNameForLogicalSlot(this.mPhoneId);
            if (servicePkg == null) {
                return;
            }
            int[] users = ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserIds();
            LegacyPermissionManager legacyPermManager = (LegacyPermissionManager) PermissionPolicyService.this.mContext.getSystemService(LegacyPermissionManager.class);
            for (int i = 0; i < users.length; i++) {
                try {
                    PermissionPolicyService.this.mPackageManager.getPackageInfoAsUser(servicePkg, 0, users[i]);
                    legacyPermManager.grantDefaultPermissionsToCarrierServiceApp(servicePkg, users[i]);
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isStarted(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsStarted.get(userId);
        }
        return z;
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        onStartUser(user.getUserIdentifier());
    }

    private void onStartUser(int userId) {
        PermissionPolicyInternal.OnInitializedCallback callback;
        if (isStarted(userId)) {
            return;
        }
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("Permission_grant_default_permissions-" + userId);
        grantOrUpgradeDefaultRuntimePermissionsIfNeeded(userId);
        t.traceEnd();
        synchronized (this.mLock) {
            this.mIsStarted.put(userId, true);
            callback = this.mOnInitializedCallback;
        }
        t.traceBegin("Permission_synchronize_permissions-" + userId);
        synchronizePermissionsAndAppOpsForUser(userId);
        t.traceEnd();
        if (callback != null) {
            t.traceBegin("Permission_onInitialized-" + userId);
            callback.onInitialized(userId);
            t.traceEnd();
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            this.mIsStarted.delete(user.getUserIdentifier());
        }
    }

    private void grantOrUpgradeDefaultRuntimePermissionsIfNeeded(final int userId) {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        PermissionManagerServiceInternal permissionManagerServiceInternal = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
        if (packageManagerInternal.isPermissionUpgradeNeeded(userId)) {
            final AndroidFuture<Boolean> future = new AndroidFuture<>();
            PermissionControllerManager permissionControllerManager = new PermissionControllerManager(getUserContext(getContext(), UserHandle.of(userId)), FgThread.getHandler());
            permissionControllerManager.grantOrUpgradeDefaultRuntimePermissions(FgThread.getExecutor(), new Consumer() { // from class: com.android.server.policy.PermissionPolicyService$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PermissionPolicyService.lambda$grantOrUpgradeDefaultRuntimePermissionsIfNeeded$0(future, userId, (Boolean) obj);
                }
            });
            try {
                try {
                    t.traceBegin("Permission_callback_waiting-" + userId);
                    future.get();
                    t.traceEnd();
                    permissionControllerManager.updateUserSensitive();
                    packageManagerInternal.updateRuntimePermissionsFingerprint(userId);
                } catch (InterruptedException | ExecutionException e) {
                    throw new IllegalStateException(e);
                }
            } catch (Throwable th) {
                t.traceEnd();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$grantOrUpgradeDefaultRuntimePermissionsIfNeeded$0(AndroidFuture future, int userId, Boolean successful) {
        if (successful.booleanValue()) {
            future.complete((Object) null);
            return;
        }
        String message = "Error granting/upgrading runtime permissions for user " + userId;
        Slog.wtf(LOG_TAG, message);
        future.completeExceptionally(new IllegalStateException(message));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Context getUserContext(Context context, UserHandle user) {
        if (context.getUser().equals(user)) {
            return context;
        }
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(LOG_TAG, "Cannot create context for user " + user, e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void synchronizePackagePermissionsAndAppOpsForUser(String packageName, int userId) {
        synchronized (this.mLock) {
            this.mIsPackageSyncsScheduled.remove(new Pair(packageName, Integer.valueOf(userId)));
        }
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        PackageInfo pkg = packageManagerInternal.getPackageInfo(packageName, 0L, 1000, userId);
        if (pkg == null) {
            return;
        }
        PermissionToOpSynchroniser synchroniser = new PermissionToOpSynchroniser(getUserContext(getContext(), UserHandle.of(userId)));
        synchroniser.addPackage(pkg.packageName);
        String[] sharedPkgNames = packageManagerInternal.getSharedUserPackagesForPackage(pkg.packageName, userId);
        for (String sharedPkgName : sharedPkgNames) {
            AndroidPackage sharedPkg = packageManagerInternal.getPackage(sharedPkgName);
            if (sharedPkg != null) {
                synchroniser.addPackage(sharedPkg.getPackageName());
            }
        }
        synchroniser.syncPackages();
    }

    private void synchronizePermissionsAndAppOpsForUser(int userId) {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        final PermissionToOpSynchroniser synchronizer = new PermissionToOpSynchroniser(getUserContext(getContext(), UserHandle.of(userId)));
        t.traceBegin("Permission_synchronize_addPackages-" + userId);
        packageManagerInternal.forEachPackage(new Consumer() { // from class: com.android.server.policy.PermissionPolicyService$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PermissionPolicyService.PermissionToOpSynchroniser.this.addPackage(((AndroidPackage) obj).getPackageName());
            }
        });
        t.traceEnd();
        t.traceBegin("Permission_syncPackages-" + userId);
        synchronizer.syncPackages();
        t.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetAppOpPermissionsIfNotRequestedForUidAsync(int uid) {
        if (isStarted(UserHandle.getUserId(uid))) {
            synchronized (this.mLock) {
                if (!this.mIsUidSyncScheduled.get(uid)) {
                    this.mIsUidSyncScheduled.put(uid, true);
                    FgThread.getHandler().sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.policy.PermissionPolicyService$$ExternalSyntheticLambda0
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ((PermissionPolicyService) obj).resetAppOpPermissionsIfNotRequestedForUid(((Integer) obj2).intValue());
                        }
                    }, this, Integer.valueOf(uid)));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetAppOpPermissionsIfNotRequestedForUid(int uid) {
        AppOpsManager appOpsManager;
        AppOpsManager appOpsManager2;
        int i;
        int i2;
        int defaultAppOpMode;
        int appOpCode;
        String appOpPermission;
        int i3;
        synchronized (this.mLock) {
            this.mIsUidSyncScheduled.delete(uid);
        }
        Context context = getContext();
        PackageManager userPackageManager = getUserContext(context, UserHandle.getUserHandleForUid(uid)).getPackageManager();
        String[] packageNames = userPackageManager.getPackagesForUid(uid);
        if (packageNames != null && packageNames.length != 0) {
            ArraySet<String> requestedPermissions = new ArraySet<>();
            for (String packageName : packageNames) {
                try {
                    PackageInfo packageInfo = userPackageManager.getPackageInfo(packageName, 4096);
                    if (packageInfo != null && packageInfo.requestedPermissions != null) {
                        Collections.addAll(requestedPermissions, packageInfo.requestedPermissions);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            AppOpsManager appOpsManager3 = (AppOpsManager) context.getSystemService(AppOpsManager.class);
            AppOpsManagerInternal appOpsManagerInternal = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
            int appOpPermissionsSize = this.mAppOpPermissions.size();
            int i4 = 0;
            while (i4 < appOpPermissionsSize) {
                String appOpPermission2 = this.mAppOpPermissions.get(i4);
                if (requestedPermissions.contains(appOpPermission2)) {
                    appOpsManager = appOpsManager3;
                } else {
                    int appOpCode2 = AppOpsManager.permissionToOpCode(appOpPermission2);
                    int defaultAppOpMode2 = AppOpsManager.opToDefaultMode(appOpCode2);
                    int length = packageNames.length;
                    int i5 = 0;
                    while (i5 < length) {
                        String packageName2 = packageNames[i5];
                        Context context2 = context;
                        int appOpMode = appOpsManager3.unsafeCheckOpRawNoThrow(appOpCode2, uid, packageName2);
                        if (appOpMode == defaultAppOpMode2) {
                            appOpsManager2 = appOpsManager3;
                            i = i5;
                            i2 = length;
                            defaultAppOpMode = defaultAppOpMode2;
                            appOpCode = appOpCode2;
                            appOpPermission = appOpPermission2;
                            i3 = i4;
                        } else {
                            appOpsManager2 = appOpsManager3;
                            appOpsManagerInternal.setUidModeFromPermissionPolicy(appOpCode2, uid, defaultAppOpMode2, this.mAppOpsCallback);
                            i = i5;
                            i2 = length;
                            defaultAppOpMode = defaultAppOpMode2;
                            appOpCode = appOpCode2;
                            appOpPermission = appOpPermission2;
                            i3 = i4;
                            appOpsManagerInternal.setModeFromPermissionPolicy(appOpCode2, uid, packageName2, defaultAppOpMode, this.mAppOpsCallback);
                        }
                        i5 = i + 1;
                        context = context2;
                        appOpsManager3 = appOpsManager2;
                        length = i2;
                        defaultAppOpMode2 = defaultAppOpMode;
                        appOpCode2 = appOpCode;
                        appOpPermission2 = appOpPermission;
                        i4 = i3;
                    }
                    appOpsManager = appOpsManager3;
                }
                i4++;
                context = context;
                appOpsManager3 = appOpsManager;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PermissionToOpSynchroniser {
        private final AppOpsManager mAppOpsManager;
        private final Context mContext;
        private final PackageManager mPackageManager;
        private final ArrayList<OpToChange> mOpsToAllow = new ArrayList<>();
        private final ArrayList<OpToChange> mOpsToIgnore = new ArrayList<>();
        private final ArrayList<OpToChange> mOpsToIgnoreIfNotAllowed = new ArrayList<>();
        private final ArrayList<OpToChange> mOpsToForeground = new ArrayList<>();
        private final AppOpsManagerInternal mAppOpsManagerInternal = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
        private final ArrayMap<String, PermissionInfo> mRuntimeAndTheirBgPermissionInfos = new ArrayMap<>();

        PermissionToOpSynchroniser(Context context) {
            this.mContext = context;
            this.mPackageManager = context.getPackageManager();
            this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
            PermissionManagerServiceInternal permissionManagerInternal = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
            List<PermissionInfo> permissionInfos = permissionManagerInternal.getAllPermissionsWithProtection(1);
            int permissionInfosSize = permissionInfos.size();
            for (int i = 0; i < permissionInfosSize; i++) {
                PermissionInfo permissionInfo = permissionInfos.get(i);
                this.mRuntimeAndTheirBgPermissionInfos.put(permissionInfo.name, permissionInfo);
                if (permissionInfo.backgroundPermission != null) {
                    String backgroundNonRuntimePermission = permissionInfo.backgroundPermission;
                    int j = 0;
                    while (true) {
                        if (j >= permissionInfosSize) {
                            break;
                        }
                        PermissionInfo bgPermissionCandidate = permissionInfos.get(j);
                        if (!permissionInfo.backgroundPermission.equals(bgPermissionCandidate.name)) {
                            j++;
                        } else {
                            backgroundNonRuntimePermission = null;
                            break;
                        }
                    }
                    if (backgroundNonRuntimePermission != null) {
                        try {
                            PermissionInfo backgroundPermissionInfo = this.mPackageManager.getPermissionInfo(backgroundNonRuntimePermission, 0);
                            this.mRuntimeAndTheirBgPermissionInfos.put(backgroundPermissionInfo.name, backgroundPermissionInfo);
                        } catch (PackageManager.NameNotFoundException e) {
                            Slog.w(PermissionPolicyService.LOG_TAG, "Unknown background permission: " + backgroundNonRuntimePermission);
                        }
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void syncPackages() {
            LongSparseLongArray alreadySetAppOps = new LongSparseLongArray();
            int allowCount = this.mOpsToAllow.size();
            for (int i = 0; i < allowCount; i++) {
                OpToChange op = this.mOpsToAllow.get(i);
                setUidModeAllowed(op.code, op.uid, op.packageName);
                alreadySetAppOps.put(IntPair.of(op.uid, op.code), 1L);
            }
            int foregroundCount = this.mOpsToForeground.size();
            for (int i2 = 0; i2 < foregroundCount; i2++) {
                OpToChange op2 = this.mOpsToForeground.get(i2);
                if (alreadySetAppOps.indexOfKey(IntPair.of(op2.uid, op2.code)) < 0) {
                    setUidModeForeground(op2.code, op2.uid, op2.packageName);
                    alreadySetAppOps.put(IntPair.of(op2.uid, op2.code), 1L);
                }
            }
            int ignoreCount = this.mOpsToIgnore.size();
            for (int i3 = 0; i3 < ignoreCount; i3++) {
                OpToChange op3 = this.mOpsToIgnore.get(i3);
                if (alreadySetAppOps.indexOfKey(IntPair.of(op3.uid, op3.code)) < 0) {
                    setUidModeIgnored(op3.code, op3.uid, op3.packageName);
                    alreadySetAppOps.put(IntPair.of(op3.uid, op3.code), 1L);
                }
            }
            int ignoreIfNotAllowedCount = this.mOpsToIgnoreIfNotAllowed.size();
            for (int i4 = 0; i4 < ignoreIfNotAllowedCount; i4++) {
                OpToChange op4 = this.mOpsToIgnoreIfNotAllowed.get(i4);
                if (alreadySetAppOps.indexOfKey(IntPair.of(op4.uid, op4.code)) < 0) {
                    boolean wasSet = setUidModeIgnoredIfNotAllowed(op4.code, op4.uid, op4.packageName);
                    if (wasSet) {
                        alreadySetAppOps.put(IntPair.of(op4.uid, op4.code), 1L);
                    }
                }
            }
        }

        private void addAppOps(PackageInfo packageInfo, AndroidPackage pkg, String permissionName) {
            PermissionInfo permissionInfo = this.mRuntimeAndTheirBgPermissionInfos.get(permissionName);
            Integer ret = ITranPackageManagerService.Instance().checkUidPermission(permissionName, pkg.getUid(), pkg);
            if (ret != null && PermissionPolicyService.getSwitchOp(permissionName) != -1) {
                setModeAllowed(PermissionPolicyService.getSwitchOp(permissionName), pkg.getUid(), 0, packageInfo.packageName);
            }
            if (permissionInfo == null) {
                return;
            }
            addPermissionAppOp(packageInfo, pkg, permissionInfo);
            addExtraAppOp(packageInfo, pkg, permissionInfo);
        }

        private void addPermissionAppOp(PackageInfo packageInfo, AndroidPackage pkg, PermissionInfo permissionInfo) {
            int appOpCode;
            int appOpMode;
            if (!permissionInfo.isRuntime()) {
                return;
            }
            String permissionName = permissionInfo.name;
            String packageName = packageInfo.packageName;
            UserHandle.getUserHandleForUid(packageInfo.applicationInfo.uid);
            int permissionFlags = this.mPackageManager.getPermissionFlags(permissionName, packageName, this.mContext.getUser());
            boolean shouldGrantBackgroundAppOp = true;
            boolean isReviewRequired = (permissionFlags & 64) != 0;
            if (isReviewRequired || (appOpCode = PermissionPolicyService.getSwitchOp(permissionName)) == -1) {
                return;
            }
            boolean shouldGrantAppOp = shouldGrantAppOp(packageInfo, pkg, permissionInfo);
            if (shouldGrantAppOp) {
                if (permissionInfo.backgroundPermission != null) {
                    PermissionInfo backgroundPermissionInfo = this.mRuntimeAndTheirBgPermissionInfos.get(permissionInfo.backgroundPermission);
                    if (backgroundPermissionInfo == null || !shouldGrantAppOp(packageInfo, pkg, backgroundPermissionInfo)) {
                        shouldGrantBackgroundAppOp = false;
                    }
                    appOpMode = shouldGrantBackgroundAppOp ? 0 : 4;
                } else {
                    appOpMode = 0;
                }
            } else {
                appOpMode = 1;
            }
            int uid = packageInfo.applicationInfo.uid;
            OpToChange opToChange = new OpToChange(uid, packageName, appOpCode);
            switch (appOpMode) {
                case 0:
                    this.mOpsToAllow.add(opToChange);
                    return;
                case 1:
                    this.mOpsToIgnore.add(opToChange);
                    return;
                case 2:
                case 3:
                default:
                    return;
                case 4:
                    this.mOpsToForeground.add(opToChange);
                    return;
            }
        }

        private boolean shouldGrantAppOp(PackageInfo packageInfo, AndroidPackage pkg, PermissionInfo permissionInfo) {
            String permissionName = permissionInfo.name;
            String packageName = packageInfo.packageName;
            boolean isGranted = this.mPackageManager.checkPermission(permissionName, packageName) == 0;
            if (isGranted) {
                int permissionFlags = this.mPackageManager.getPermissionFlags(permissionName, packageName, this.mContext.getUser());
                boolean isRevokedCompat = (permissionFlags & 8) == 8;
                if (isRevokedCompat) {
                    return false;
                }
                if (permissionInfo.isHardRestricted()) {
                    boolean shouldApplyRestriction = (permissionFlags & 16384) == 16384;
                    return !shouldApplyRestriction;
                } else if (permissionInfo.isSoftRestricted()) {
                    SoftRestrictedPermissionPolicy policy = SoftRestrictedPermissionPolicy.forPermission(this.mContext, packageInfo.applicationInfo, pkg, this.mContext.getUser(), permissionName);
                    return policy.mayGrantPermission();
                } else {
                    return true;
                }
            }
            return false;
        }

        private void addExtraAppOp(PackageInfo packageInfo, AndroidPackage pkg, PermissionInfo permissionInfo) {
            if (!permissionInfo.isSoftRestricted()) {
                return;
            }
            String permissionName = permissionInfo.name;
            SoftRestrictedPermissionPolicy policy = SoftRestrictedPermissionPolicy.forPermission(this.mContext, packageInfo.applicationInfo, pkg, this.mContext.getUser(), permissionName);
            int extraOpCode = policy.getExtraAppOpCode();
            if (extraOpCode == -1) {
                return;
            }
            int uid = packageInfo.applicationInfo.uid;
            String packageName = packageInfo.packageName;
            OpToChange extraOpToChange = new OpToChange(uid, packageName, extraOpCode);
            if (policy.mayAllowExtraAppOp()) {
                this.mOpsToAllow.add(extraOpToChange);
            } else if (policy.mayDenyExtraAppOpIfGranted()) {
                this.mOpsToIgnore.add(extraOpToChange);
            } else {
                this.mOpsToIgnoreIfNotAllowed.add(extraOpToChange);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void addPackage(String pkgName) {
            int uid;
            String[] strArr;
            PackageManagerInternal pmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            try {
                PackageInfo pkgInfo = this.mPackageManager.getPackageInfo(pkgName, 4096);
                AndroidPackage pkg = pmInternal.getPackage(pkgName);
                if (pkgInfo == null || pkg == null || pkgInfo.applicationInfo == null || pkgInfo.requestedPermissions == null || (uid = pkgInfo.applicationInfo.uid) == 0 || uid == 1000) {
                    return;
                }
                for (String permission : pkgInfo.requestedPermissions) {
                    addAppOps(pkgInfo, pkg, permission);
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        private void setUidModeAllowed(int opCode, int uid, String packageName) {
            setUidMode(opCode, uid, 0, packageName);
        }

        private void setUidModeForeground(int opCode, int uid, String packageName) {
            setUidMode(opCode, uid, 4, packageName);
        }

        private void setUidModeIgnored(int opCode, int uid, String packageName) {
            setUidMode(opCode, uid, 1, packageName);
        }

        private boolean setUidModeIgnoredIfNotAllowed(int opCode, int uid, String packageName) {
            int currentMode = this.mAppOpsManager.unsafeCheckOpRaw(AppOpsManager.opToPublicName(opCode), uid, packageName);
            if (currentMode != 0) {
                if (currentMode != 1) {
                    this.mAppOpsManagerInternal.setUidModeFromPermissionPolicy(opCode, uid, 1, PermissionPolicyService.this.mAppOpsCallback);
                }
                return true;
            }
            return false;
        }

        private void setUidMode(int opCode, int uid, int mode, String packageName) {
            int oldMode = this.mAppOpsManager.unsafeCheckOpRaw(AppOpsManager.opToPublicName(opCode), uid, packageName);
            if (oldMode != mode) {
                this.mAppOpsManagerInternal.setUidModeFromPermissionPolicy(opCode, uid, mode, PermissionPolicyService.this.mAppOpsCallback);
                int newMode = this.mAppOpsManager.unsafeCheckOpRaw(AppOpsManager.opToPublicName(opCode), uid, packageName);
                if (newMode != mode) {
                    this.mAppOpsManagerInternal.setModeFromPermissionPolicy(opCode, uid, packageName, AppOpsManager.opToDefaultMode(opCode), PermissionPolicyService.this.mAppOpsCallback);
                }
            }
        }

        private void setModeAllowed(int opCode, int uid, int mode, String packageName) {
            int oldMode = this.mAppOpsManager.unsafeCheckOpRaw(AppOpsManager.opToPublicName(opCode), uid, packageName);
            if (oldMode != mode) {
                this.mAppOpsManagerInternal.setModeFromPermissionPolicy(opCode, uid, packageName, mode, (IAppOpsCallback) null);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class OpToChange {
            final int code;
            final String packageName;
            final int uid;

            OpToChange(int uid, String packageName, int code) {
                this.uid = uid;
                this.packageName = packageName;
                this.code = code;
            }
        }
    }

    /* loaded from: classes2.dex */
    private class Internal extends PermissionPolicyInternal {
        private final ActivityInterceptorCallback mActivityInterceptorCallback;

        private Internal() {
            this.mActivityInterceptorCallback = new AnonymousClass1();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: com.android.server.policy.PermissionPolicyService$Internal$1  reason: invalid class name */
        /* loaded from: classes2.dex */
        public class AnonymousClass1 extends ActivityInterceptorCallback {
            AnonymousClass1() {
            }

            @Override // com.android.server.wm.ActivityInterceptorCallback
            public ActivityInterceptorCallback.ActivityInterceptResult intercept(ActivityInterceptorCallback.ActivityInterceptorInfo info) {
                return null;
            }

            @Override // com.android.server.wm.ActivityInterceptorCallback
            public void onActivityLaunched(final TaskInfo taskInfo, final ActivityInfo activityInfo, final ActivityInterceptorCallback.ActivityInterceptorInfo info) {
                super.onActivityLaunched(taskInfo, activityInfo, info);
                if (!Internal.this.shouldShowNotificationDialogOrClearFlags(taskInfo, activityInfo.packageName, info.callingPackage, info.intent, info.checkedOptions, activityInfo.name, true) || Internal.this.isNoDisplayActivity(activityInfo)) {
                    return;
                }
                UserHandle user = UserHandle.of(taskInfo.userId);
                if (!CompatChanges.isChangeEnabled((long) PermissionPolicyService.NOTIFICATION_PERM_CHANGE_ID, activityInfo.packageName, user)) {
                    PermissionPolicyService.this.mHandler.post(new Runnable() { // from class: com.android.server.policy.PermissionPolicyService$Internal$1$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            PermissionPolicyService.Internal.AnonymousClass1.this.m5930x85d253f0(activityInfo, taskInfo, info);
                        }
                    });
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$onActivityLaunched$0$com-android-server-policy-PermissionPolicyService$Internal$1  reason: not valid java name */
            public /* synthetic */ void m5930x85d253f0(ActivityInfo activityInfo, TaskInfo taskInfo, ActivityInterceptorCallback.ActivityInterceptorInfo info) {
                Internal.this.showNotificationPromptIfNeeded(activityInfo.packageName, taskInfo.userId, taskInfo.taskId, info);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onActivityManagerReady() {
            ActivityTaskManagerInternal atm = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
            atm.registerActivityStartInterceptor(1, this.mActivityInterceptorCallback);
        }

        @Override // com.android.server.policy.PermissionPolicyInternal
        public boolean checkStartActivity(Intent intent, int callingUid, String callingPackage) {
            if (callingPackage != null && isActionRemovedForCallingPackage(intent, callingUid, callingPackage)) {
                Slog.w(PermissionPolicyService.LOG_TAG, "Action Removed: starting " + intent.toString() + " from " + callingPackage + " (uid=" + callingUid + ")");
                return false;
            } else if ("android.content.pm.action.REQUEST_PERMISSIONS_FOR_OTHER".equals(intent.getAction())) {
                if (callingUid != 1000 || !"android".equals(callingPackage)) {
                    return false;
                }
                return true;
            } else {
                return true;
            }
        }

        @Override // com.android.server.policy.PermissionPolicyInternal
        public void showNotificationPromptIfNeeded(String packageName, int userId, int taskId) {
            showNotificationPromptIfNeeded(packageName, userId, taskId, null);
        }

        void showNotificationPromptIfNeeded(String packageName, int userId, int taskId, ActivityInterceptorCallback.ActivityInterceptorInfo info) {
            UserHandle user = UserHandle.of(userId);
            if (packageName == null || taskId == -1 || !shouldForceShowNotificationPermissionRequest(packageName, user)) {
                return;
            }
            launchNotificationPermissionRequestDialog(packageName, user, taskId, info);
        }

        @Override // com.android.server.policy.PermissionPolicyInternal
        public boolean isIntentToPermissionDialog(Intent intent) {
            return Objects.equals(intent.getPackage(), PermissionPolicyService.this.mPackageManager.getPermissionControllerPackageName()) && (Objects.equals(intent.getAction(), "android.content.pm.action.REQUEST_PERMISSIONS_FOR_OTHER") || Objects.equals(intent.getAction(), "android.content.pm.action.REQUEST_PERMISSIONS"));
        }

        @Override // com.android.server.policy.PermissionPolicyInternal
        public boolean shouldShowNotificationDialogForTask(TaskInfo taskInfo, String currPkg, String callingPkg, Intent intent, String activityName) {
            return shouldShowNotificationDialogOrClearFlags(taskInfo, currPkg, callingPkg, intent, null, activityName, false);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isNoDisplayActivity(ActivityInfo aInfo) {
            AttributeCache.Entry ent;
            int themeResource = aInfo.getThemeResource();
            if (themeResource != 0 && (ent = AttributeCache.instance().get(aInfo.packageName, themeResource, R.styleable.Window, 0)) != null) {
                boolean noDisplay = ent.array.getBoolean(10, false);
                return noDisplay;
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean shouldShowNotificationDialogOrClearFlags(TaskInfo taskInfo, String currPkg, String callingPkg, Intent intent, ActivityOptions options, String topActivityName, boolean startedActivity) {
            if (intent == null || currPkg == null || taskInfo == null || topActivityName == null || ((!taskInfo.isFocused || !taskInfo.isVisible || !taskInfo.isRunning) && !startedActivity)) {
                return false;
            }
            if (!isLauncherIntent(intent) && ((options == null || !options.isEligibleForLegacyPermissionPrompt()) && !isTaskStartedFromLauncher(currPkg, taskInfo))) {
                if (!isTaskPotentialTrampoline(topActivityName, currPkg, callingPkg, taskInfo, intent)) {
                    return false;
                }
                if (startedActivity && !pkgHasRunningLauncherTask(currPkg, taskInfo)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isTaskPotentialTrampoline(String activityName, String currPkg, String callingPkg, TaskInfo taskInfo, Intent intent) {
            return currPkg.equals(callingPkg) && taskInfo.baseIntent.filterEquals(intent) && taskInfo.numActivities == 1 && activityName.equals(taskInfo.topActivityInfo.name);
        }

        private boolean pkgHasRunningLauncherTask(String currPkg, TaskInfo taskInfo) {
            ActivityTaskManagerInternal m = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
            try {
                List<ActivityManager.AppTask> tasks = m.getAppTasks(currPkg, PermissionPolicyService.this.mPackageManager.getPackageUid(currPkg, 0));
                for (int i = 0; i < tasks.size(); i++) {
                    TaskInfo other = tasks.get(i).getTaskInfo();
                    if (other.taskId != taskInfo.taskId && other.isFocused && other.isRunning && isTaskStartedFromLauncher(currPkg, other)) {
                        return true;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            return false;
        }

        private boolean isLauncherIntent(Intent intent) {
            return "android.intent.action.MAIN".equals(intent.getAction()) && intent.getCategories() != null && (intent.getCategories().contains("android.intent.category.LAUNCHER") || intent.getCategories().contains("android.intent.category.LEANBACK_LAUNCHER") || intent.getCategories().contains("android.intent.category.CAR_LAUNCHER"));
        }

        private boolean isTaskStartedFromLauncher(String currPkg, TaskInfo taskInfo) {
            return currPkg.equals(taskInfo.baseActivity.getPackageName()) && isLauncherIntent(taskInfo.baseIntent);
        }

        private void launchNotificationPermissionRequestDialog(String pkgName, UserHandle user, int taskId, ActivityInterceptorCallback.ActivityInterceptorInfo info) {
            ActivityOptions options;
            Intent grantPermission = PermissionPolicyService.this.mPackageManager.buildRequestPermissionsIntent(new String[]{"android.permission.POST_NOTIFICATIONS"});
            grantPermission.addFlags(268697600);
            grantPermission.setAction("android.content.pm.action.REQUEST_PERMISSIONS_FOR_OTHER");
            grantPermission.putExtra("android.intent.extra.PACKAGE_NAME", pkgName);
            boolean remoteAnimation = (info == null || info.checkedOptions == null || info.checkedOptions.getAnimationType() != 13 || info.clearOptionsAnimation == null) ? false : true;
            if (remoteAnimation) {
                options = ActivityOptions.makeRemoteAnimation(info.checkedOptions.getRemoteAnimationAdapter(), info.checkedOptions.getRemoteTransition());
            } else {
                options = new ActivityOptions(new Bundle());
            }
            options.setTaskOverlay(true, false);
            options.setLaunchTaskId(taskId);
            if (remoteAnimation) {
                info.clearOptionsAnimation.run();
            }
            try {
                PermissionPolicyService.this.mContext.startActivityAsUser(grantPermission, options.toBundle(), user);
            } catch (Exception e) {
                Log.e(PermissionPolicyService.LOG_TAG, "couldn't start grant permission dialogfor other package " + pkgName, e);
            }
        }

        @Override // com.android.server.policy.PermissionPolicyInternal
        public boolean isInitialized(int userId) {
            return PermissionPolicyService.this.isStarted(userId);
        }

        @Override // com.android.server.policy.PermissionPolicyInternal
        public void setOnInitializedCallback(PermissionPolicyInternal.OnInitializedCallback callback) {
            synchronized (PermissionPolicyService.this.mLock) {
                PermissionPolicyService.this.mOnInitializedCallback = callback;
            }
        }

        private boolean isActionRemovedForCallingPackage(Intent intent, int callingUid, String callingPackage) {
            String action = intent.getAction();
            if (action == null) {
                return false;
            }
            char c = 65535;
            switch (action.hashCode()) {
                case -1673968409:
                    if (action.equals("android.provider.Telephony.ACTION_CHANGE_DEFAULT")) {
                        c = 1;
                        break;
                    }
                    break;
                case 579418056:
                    if (action.equals("android.telecom.action.CHANGE_DEFAULT_DIALER")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                    try {
                        ApplicationInfo applicationInfo = PermissionPolicyService.this.getContext().getPackageManager().getApplicationInfoAsUser(callingPackage, 0, UserHandle.getUserId(callingUid));
                        if (applicationInfo.targetSdkVersion >= 29) {
                            return true;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.i(PermissionPolicyService.LOG_TAG, "Cannot find application info for " + callingPackage);
                    }
                    intent.putExtra("android.intent.extra.CALLING_PACKAGE", callingPackage);
                    return false;
                default:
                    return false;
            }
        }

        private boolean shouldForceShowNotificationPermissionRequest(String pkgName, UserHandle user) {
            boolean hasCreatedNotificationChannels;
            boolean granted;
            boolean explicitlySet;
            AndroidPackage pkg = PermissionPolicyService.this.mPackageManagerInternal.getPackage(pkgName);
            if (pkg == null || pkg.getPackageName() == null || Objects.equals(pkgName, PermissionPolicyService.this.mPackageManager.getPermissionControllerPackageName()) || pkg.getTargetSdkVersion() < 23) {
                if (pkg == null) {
                    Slog.w(PermissionPolicyService.LOG_TAG, "Cannot check for Notification prompt, no package for " + pkgName);
                }
                return false;
            }
            synchronized (PermissionPolicyService.this.mLock) {
                if (!PermissionPolicyService.this.mBootCompleted) {
                    return false;
                }
                if (!pkg.getRequestedPermissions().contains("android.permission.POST_NOTIFICATIONS") || CompatChanges.isChangeEnabled((long) PermissionPolicyService.NOTIFICATION_PERM_CHANGE_ID, pkgName, user) || PermissionPolicyService.this.mKeyguardManager.isKeyguardLocked()) {
                    return false;
                }
                int uid = user.getUid(pkg.getUid());
                if (PermissionPolicyService.this.mNotificationManager == null) {
                    PermissionPolicyService.this.mNotificationManager = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
                }
                if (PermissionPolicyService.this.mNotificationManager.getNumNotificationChannelsForPackage(pkgName, uid, true) <= 0) {
                    hasCreatedNotificationChannels = false;
                } else {
                    hasCreatedNotificationChannels = true;
                }
                if (PermissionPolicyService.this.mPermissionManagerInternal.checkUidPermission(uid, "android.permission.POST_NOTIFICATIONS") != 0) {
                    granted = false;
                } else {
                    granted = true;
                }
                int flags = PermissionPolicyService.this.mPackageManager.getPermissionFlags("android.permission.POST_NOTIFICATIONS", pkgName, user);
                if ((32823 & flags) == 0) {
                    explicitlySet = false;
                } else {
                    explicitlySet = true;
                }
                if (granted || !hasCreatedNotificationChannels || explicitlySet) {
                    return false;
                }
                return true;
            }
        }
    }
}
