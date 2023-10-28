package com.android.server.devicepolicy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.IStopUserCallback;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyCache;
import android.app.admin.DevicePolicyDrawableResource;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.admin.DevicePolicyManagerLiteInternal;
import android.app.admin.DevicePolicySafetyChecker;
import android.app.admin.DevicePolicyStringResource;
import android.app.admin.DeviceStateCache;
import android.app.admin.FactoryResetProtectionPolicy;
import android.app.admin.FullyManagedDeviceProvisioningParams;
import android.app.admin.ManagedProfileProvisioningParams;
import android.app.admin.NetworkEvent;
import android.app.admin.ParcelableGranteeMap;
import android.app.admin.ParcelableResource;
import android.app.admin.PasswordMetrics;
import android.app.admin.PasswordPolicy;
import android.app.admin.PreferentialNetworkServiceConfig;
import android.app.admin.SecurityLog;
import android.app.admin.StartInstallingUpdateCallback;
import android.app.admin.SystemUpdateInfo;
import android.app.admin.SystemUpdatePolicy;
import android.app.admin.UnsafeStateException;
import android.app.admin.WifiSsidPolicy;
import android.app.backup.IBackupManager;
import android.app.compat.CompatChanges;
import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.app.trust.TrustManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.PermissionChecker;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.CrossProfileApps;
import android.content.pm.CrossProfileAppsInternal;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.pm.StringParceledListSlice;
import android.content.pm.SuspendDialogInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.usb.UsbManager;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.IAudioService;
import android.net.ConnectivityManager;
import android.net.ConnectivitySettingsManager;
import android.net.IIpConnectivityMetrics;
import android.net.ProfileNetworkPreference;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.VpnManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.permission.AdminPermissionControlParams;
import android.permission.IPermissionManager;
import android.permission.PermissionControllerManager;
import android.provider.ContactsContract;
import android.provider.ContactsInternal;
import android.provider.Settings;
import android.provider.Telephony;
import android.security.AppUriAuthenticationPolicy;
import android.security.IKeyChainAliasCallback;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.ParcelableKeyGenParameterSpec;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.DebugUtils;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.view.IWindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.IAccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.app.LocalePicker;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.net.NetworkUtilsInternal;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BackgroundThread;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.Preconditions;
import com.android.internal.util.StatLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockSettingsInternal;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.PasswordValidationError;
import com.android.net.module.util.ProxyUtils;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.PersistentDataBlockManagerInternal;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
import com.android.server.am.HostingRecord;
import com.android.server.devicepolicy.ActiveAdmin;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.devicepolicy.TransferOwnershipMetadataManager;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.RestrictionsSet;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.UserRestrictionsUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.storage.DeviceStorageMonitorInternal;
import com.android.server.uri.NeededUriGrants;
import com.android.server.uri.UriGrantsManagerInternal;
import com.android.server.utils.Slogf;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.transsion.hubcore.server.devicepolicy.ITranDevicePolicyManagerService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class DevicePolicyManagerService extends BaseIDevicePolicyManager {
    private static final String AB_DEVICE_KEY = "ro.build.ab_update";
    private static final String ACTION_EXPIRED_PASSWORD_NOTIFICATION = "com.android.server.ACTION_EXPIRED_PASSWORD_NOTIFICATION";
    static final String ACTION_PROFILE_OFF_DEADLINE = "com.android.server.ACTION_PROFILE_OFF_DEADLINE";
    static final String ACTION_TURN_PROFILE_ON_NOTIFICATION = "com.android.server.ACTION_TURN_PROFILE_ON_NOTIFICATION";
    private static final long ADMIN_APP_PASSWORD_COMPLEXITY = 123562444;
    private static final String ALLOW_USER_PROVISIONING_KEY = "ro.config.allowuserprovisioning";
    private static final String ATTRIBUTION_TAG = "DevicePolicyManagerService";
    private static final String CALLED_FROM_PARENT = "calledFromParent";
    private static final int COPY_ACCOUNT_EXCEPTION = 4;
    private static final int COPY_ACCOUNT_FAILED = 2;
    private static final int COPY_ACCOUNT_SUCCEEDED = 1;
    private static final int COPY_ACCOUNT_TIMED_OUT = 3;
    private static final String CREDENTIAL_MANAGEMENT_APP = "credentialManagementApp";
    private static final String CREDENTIAL_MANAGEMENT_APP_INVALID_ALIAS_MSG = "The alias provided must be contained in the aliases specified in the credential management app's authentication policy";
    private static final Set<Integer> DA_DISALLOWED_POLICIES;
    private static final String[] DELEGATIONS;
    private static final int DEVICE_ADMIN_DEACTIVATE_TIMEOUT = 10000;
    private static final List<String> DEVICE_OWNER_OR_MANAGED_PROFILE_OWNER_DELEGATIONS;
    private static final List<String> DEVICE_OWNER_OR_ORGANIZATION_OWNED_MANAGED_PROFILE_OWNER_DELEGATIONS;
    static final String DEVICE_POLICIES_XML = "device_policies.xml";
    static final int DPMS_VERSION = 3;
    private static final boolean ENABLE_LOCK_GUARD = true;
    private static final List<String> EXCLUSIVE_DELEGATIONS;
    private static final long EXPIRATION_GRACE_PERIOD_MS;
    private static final Set<String> GLOBAL_SETTINGS_ALLOWLIST;
    private static final Set<String> GLOBAL_SETTINGS_DEPRECATED;
    protected static final String LOG_TAG = "DevicePolicyManager";
    private static final String LOG_TAG_DEVICE_OWNER = "device-owner";
    private static final String LOG_TAG_PROFILE_OWNER = "profile-owner";
    private static final long MANAGED_PROFILE_MAXIMUM_TIME_OFF_THRESHOLD;
    private static final long MANAGED_PROFILE_OFF_WARNING_PERIOD;
    private static final int MAX_LONG_SUPPORT_MESSAGE_LENGTH = 20000;
    private static final int MAX_ORG_NAME_LENGTH = 200;
    private static final int MAX_PACKAGE_NAME_LENGTH = 223;
    private static final int MAX_POLICY_STRING_LENGTH = 65535;
    private static final int MAX_SHORT_SUPPORT_MESSAGE_LENGTH = 200;
    private static final long MINIMUM_STRONG_AUTH_TIMEOUT_MS;
    private static final long MS_PER_DAY;
    private static final String NOT_CALLED_FROM_PARENT = "notCalledFromParent";
    private static final String NOT_CREDENTIAL_MANAGEMENT_APP = "notCredentialManagementApp";
    private static final String NOT_SYSTEM_CALLER_MSG = "Only the system can %s";
    private static final String NULL_STRING_ARRAY = "nullStringArray";
    static final String POLICIES_VERSION_XML = "device_policies_version";
    private static final long PREVENT_SETTING_PASSWORD_QUALITY_ON_PARENT = 165573442;
    private static final int PROFILE_KEYGUARD_FEATURES = 440;
    private static final int PROFILE_KEYGUARD_FEATURES_PROFILE_ONLY = 8;
    private static final int PROFILE_OFF_NOTIFICATION_NONE = 0;
    private static final int PROFILE_OFF_NOTIFICATION_SUSPENDED = 2;
    private static final int PROFILE_OFF_NOTIFICATION_WARNING = 1;
    private static final String PROPERTY_ORGANIZATION_OWNED = "ro.organization_owned";
    private static final int REQUEST_EXPIRE_PASSWORD = 5571;
    private static final int REQUEST_PROFILE_OFF_DEADLINE = 5572;
    private static final Set<String> SECURE_SETTINGS_ALLOWLIST;
    private static final Set<String> SECURE_SETTINGS_DEVICEOWNER_ALLOWLIST;
    private static final int STATUS_BAR_DISABLE2_MASK = 1;
    private static final int STATUS_BAR_DISABLE_MASK = 34013184;
    private static final Set<String> SYSTEM_SETTINGS_ALLOWLIST;
    private static final String TAG_TRANSFER_OWNERSHIP_BUNDLE = "transfer-ownership-bundle";
    private static final String TRANSFER_OWNERSHIP_PARAMETERS_XML = "transfer-ownership-parameters.xml";
    private static final int UNATTENDED_MANAGED_KIOSK_MS = 30000;
    private static final long USE_SET_LOCATION_ENABLED = 117835097;
    static final boolean VERBOSE_LOG = false;
    final Handler mBackgroundHandler;
    private final RemoteBugreportManager mBugreportCollectionManager;
    private final CertificateMonitor mCertificateMonitor;
    private DevicePolicyConstants mConstants;
    private final DevicePolicyConstantsObserver mConstantsObserver;
    final Context mContext;
    private final DeviceAdminServiceController mDeviceAdminServiceController;
    private final DeviceManagementResourcesProvider mDeviceManagementResourcesProvider;
    private final DevicePolicyManagementRoleObserver mDevicePolicyManagementRoleObserver;
    private final Object mESIDInitilizationLock;
    private EnterpriseSpecificIdCalculator mEsidCalculator;
    final Handler mHandler;
    final boolean mHasFeature;
    final boolean mHasTelephonyFeature;
    final IPackageManager mIPackageManager;
    final IPermissionManager mIPermissionManager;
    final Injector mInjector;
    private final boolean mIsAutomotive;
    final boolean mIsWatch;
    final LocalService mLocalService;
    private final Object mLockDoNoUseDirectly;
    private final LockPatternUtils mLockPatternUtils;
    private final LockSettingsInternal mLockSettingsInternal;
    private int mLogoutUserId;
    private NetworkLogger mNetworkLogger;
    private int mNetworkLoggingNotificationUserId;
    private final OverlayPackagesProvider mOverlayPackagesProvider;
    final Owners mOwners;
    private final Set<Pair<String, Integer>> mPackagesToRemove;
    final PolicyPathProvider mPathProvider;
    private final ArrayList<Object> mPendingUserCreatedCallbackTokens;
    private final DevicePolicyCacheImpl mPolicyCache;
    final BroadcastReceiver mReceiver;
    private DevicePolicySafetyChecker mSafetyChecker;
    private final SecurityLogMonitor mSecurityLogMonitor;
    private final SetupContentObserver mSetupContentObserver;
    private final StatLogger mStatLogger;
    private final DeviceStateCacheImpl mStateCache;
    final TelephonyManager mTelephonyManager;
    private final Binder mToken;
    final TransferOwnershipMetadataManager mTransferOwnershipMetadataManager;
    final UsageStatsManagerInternal mUsageStatsManagerInternal;
    final SparseArray<DevicePolicyData> mUserData;
    final UserManager mUserManager;
    final UserManagerInternal mUserManagerInternal;

    /* loaded from: classes.dex */
    private @interface CopyAccountStatus {
    }

    /* loaded from: classes.dex */
    interface Stats {
        public static final int COUNT = 1;
        public static final int LOCK_GUARD_GUARD = 0;
    }

    static {
        long millis = TimeUnit.DAYS.toMillis(1L);
        MS_PER_DAY = millis;
        EXPIRATION_GRACE_PERIOD_MS = 5 * millis;
        MANAGED_PROFILE_MAXIMUM_TIME_OFF_THRESHOLD = 3 * millis;
        MANAGED_PROFILE_OFF_WARNING_PERIOD = millis * 1;
        DELEGATIONS = new String[]{"delegation-cert-install", "delegation-app-restrictions", "delegation-block-uninstall", "delegation-enable-system-app", "delegation-keep-uninstalled-packages", "delegation-package-access", "delegation-permission-grant", "delegation-install-existing-package", "delegation-keep-uninstalled-packages", "delegation-network-logging", "delegation-security-logging", "delegation-cert-selection"};
        DEVICE_OWNER_OR_MANAGED_PROFILE_OWNER_DELEGATIONS = Arrays.asList("delegation-network-logging");
        DEVICE_OWNER_OR_ORGANIZATION_OWNED_MANAGED_PROFILE_OWNER_DELEGATIONS = Arrays.asList("delegation-security-logging");
        EXCLUSIVE_DELEGATIONS = Arrays.asList("delegation-network-logging", "delegation-security-logging", "delegation-cert-selection");
        ArraySet arraySet = new ArraySet();
        SECURE_SETTINGS_ALLOWLIST = arraySet;
        arraySet.add("default_input_method");
        arraySet.add("skip_first_use_hints");
        arraySet.add("install_non_market_apps");
        ArraySet arraySet2 = new ArraySet();
        SECURE_SETTINGS_DEVICEOWNER_ALLOWLIST = arraySet2;
        arraySet2.addAll((Collection) arraySet);
        arraySet2.add("location_mode");
        ArraySet arraySet3 = new ArraySet();
        GLOBAL_SETTINGS_ALLOWLIST = arraySet3;
        arraySet3.add("adb_enabled");
        arraySet3.add("adb_wifi_enabled");
        arraySet3.add("auto_time");
        arraySet3.add("auto_time_zone");
        arraySet3.add("data_roaming");
        arraySet3.add("usb_mass_storage_enabled");
        arraySet3.add("wifi_sleep_policy");
        arraySet3.add("stay_on_while_plugged_in");
        arraySet3.add("wifi_device_owner_configs_lockdown");
        arraySet3.add("private_dns_mode");
        arraySet3.add("private_dns_specifier");
        ArraySet arraySet4 = new ArraySet();
        GLOBAL_SETTINGS_DEPRECATED = arraySet4;
        arraySet4.add("bluetooth_on");
        arraySet4.add("development_settings_enabled");
        arraySet4.add("mode_ringer");
        arraySet4.add("network_preference");
        arraySet4.add("wifi_on");
        ArraySet arraySet5 = new ArraySet();
        SYSTEM_SETTINGS_ALLOWLIST = arraySet5;
        arraySet5.add("screen_brightness");
        arraySet5.add("screen_brightness_float");
        arraySet5.add("screen_brightness_mode");
        arraySet5.add("screen_off_timeout");
        ArraySet arraySet6 = new ArraySet();
        DA_DISALLOWED_POLICIES = arraySet6;
        arraySet6.add(8);
        arraySet6.add(9);
        arraySet6.add(6);
        arraySet6.add(0);
        MINIMUM_STRONG_AUTH_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1L);
    }

    final Object getLockObject() {
        long start = this.mStatLogger.getTime();
        LockGuard.guard(8);
        this.mStatLogger.logDurationStat(0, start);
        return this.mLockDoNoUseDirectly;
    }

    final void ensureLocked() {
        if (Thread.holdsLock(this.mLockDoNoUseDirectly)) {
            return;
        }
        Slogf.wtfStack(LOG_TAG, "Not holding DPMS lock.");
    }

    private void wtfIfInLock() {
        if (Thread.holdsLock(this.mLockDoNoUseDirectly)) {
            Slogf.wtfStack(LOG_TAG, "Shouldn't be called with DPMS lock held");
        }
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private BaseIDevicePolicyManager mService;

        public Lifecycle(Context context) {
            super(context);
            String dpmsClassName = context.getResources().getString(17039956);
            dpmsClassName = TextUtils.isEmpty(dpmsClassName) ? DevicePolicyManagerService.class.getName() : dpmsClassName;
            try {
                Class<?> serviceClass = Class.forName(dpmsClassName);
                Constructor<?> constructor = serviceClass.getConstructor(Context.class);
                this.mService = (BaseIDevicePolicyManager) constructor.newInstance(context);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to instantiate DevicePolicyManagerService with class name: " + dpmsClassName, e);
            }
        }

        public void setDevicePolicySafetyChecker(DevicePolicySafetyChecker safetyChecker) {
            this.mService.setDevicePolicySafetyChecker(safetyChecker);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            publishBinderService("device_policy", this.mService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            this.mService.systemReady(phase);
        }

        @Override // com.android.server.SystemService
        public void onUserStarting(SystemService.TargetUser user) {
            if (user.isPreCreated()) {
                return;
            }
            this.mService.handleStartUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            if (user.isPreCreated()) {
                return;
            }
            this.mService.handleUnlockUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser user) {
            if (user.isPreCreated()) {
                return;
            }
            this.mService.handleStopUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocked(SystemService.TargetUser user) {
            if (user.isPreCreated()) {
                return;
            }
            this.mService.handleOnUserUnlocked(user.getUserIdentifier());
        }
    }

    /* loaded from: classes.dex */
    protected static class RestrictionsListener implements UserManagerInternal.UserRestrictionsListener {
        private final Context mContext;
        private final DevicePolicyManagerService mDpms;
        private final UserManagerInternal mUserManagerInternal;

        public RestrictionsListener(Context context, UserManagerInternal userManagerInternal, DevicePolicyManagerService dpms) {
            this.mContext = context;
            this.mUserManagerInternal = userManagerInternal;
            this.mDpms = dpms;
        }

        @Override // com.android.server.pm.UserManagerInternal.UserRestrictionsListener
        public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            resetCrossProfileIntentFiltersIfNeeded(userId, newRestrictions, prevRestrictions);
            resetUserVpnIfNeeded(userId, newRestrictions, prevRestrictions);
        }

        private void resetUserVpnIfNeeded(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            boolean newlyEnforced = !prevRestrictions.getBoolean("no_config_vpn") && newRestrictions.getBoolean("no_config_vpn");
            if (newlyEnforced) {
                this.mDpms.clearUserConfiguredVpns(userId);
            }
        }

        private void resetCrossProfileIntentFiltersIfNeeded(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            int parentId;
            if (!UserRestrictionsUtils.restrictionsChanged(prevRestrictions, newRestrictions, "no_sharing_into_profile") || (parentId = this.mUserManagerInternal.getProfileParentId(userId)) == userId) {
                return;
            }
            Slogf.i(DevicePolicyManagerService.LOG_TAG, "Resetting cross-profile intent filters on restriction change");
            this.mDpms.resetDefaultCrossProfileIntentFilters(parentId);
            this.mContext.sendBroadcastAsUser(new Intent("android.app.action.DATA_SHARING_RESTRICTION_APPLIED"), UserHandle.of(userId));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearUserConfiguredVpns(int userId) {
        synchronized (getLockObject()) {
            ActiveAdmin owner = getDeviceOrProfileOwnerAdminLocked(userId);
            if (owner == null) {
                Slogf.wtf(LOG_TAG, "Admin not found");
                return;
            }
            String adminConfiguredVpnPkg = owner.mAlwaysOnVpnPackage;
            if (adminConfiguredVpnPkg == null) {
                this.mInjector.getVpnManager().setAlwaysOnVpnPackageForUser(userId, null, false, null);
            }
            List<AppOpsManager.PackageOps> allVpnOps = this.mInjector.getAppOpsManager().getPackagesForOps(new int[]{47});
            if (allVpnOps == null) {
                return;
            }
            for (AppOpsManager.PackageOps pkgOps : allVpnOps) {
                if (UserHandle.getUserId(pkgOps.getUid()) == userId && !pkgOps.getPackageName().equals(adminConfiguredVpnPkg)) {
                    if (pkgOps.getOps().size() == 1) {
                        int mode = ((AppOpsManager.OpEntry) pkgOps.getOps().get(0)).getMode();
                        if (mode == 0) {
                            Slogf.i(LOG_TAG, String.format("Revoking VPN authorization for package %s uid %d", pkgOps.getPackageName(), Integer.valueOf(pkgOps.getUid())));
                            this.mInjector.getAppOpsManager().setMode(47, pkgOps.getUid(), pkgOps.getPackageName(), 3);
                        }
                    } else {
                        Slogf.wtf(LOG_TAG, "Unexpected number of ops returned");
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class UserLifecycleListener implements UserManagerInternal.UserLifecycleListener {
        private UserLifecycleListener() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onUserCreated$0$com-android-server-devicepolicy-DevicePolicyManagerService$UserLifecycleListener  reason: not valid java name */
        public /* synthetic */ void m3133xdbce907b(UserInfo user, Object token) {
            DevicePolicyManagerService.this.handleNewUserCreated(user, token);
        }

        @Override // com.android.server.pm.UserManagerInternal.UserLifecycleListener
        public void onUserCreated(final UserInfo user, final Object token) {
            DevicePolicyManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$UserLifecycleListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DevicePolicyManagerService.UserLifecycleListener.this.m3133xdbce907b(user, token);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePackagesChanged(String packageName, int userHandle) {
        boolean removedAdmin = false;
        DevicePolicyData policy = m3013x8b75536b(userHandle);
        synchronized (getLockObject()) {
            for (int i = policy.mAdminList.size() - 1; i >= 0; i--) {
                ActiveAdmin aa = policy.mAdminList.get(i);
                try {
                    String adminPackage = aa.info.getPackageName();
                    if ((packageName == null || packageName.equals(adminPackage)) && (this.mIPackageManager.getPackageInfo(adminPackage, 0L, userHandle) == null || this.mIPackageManager.getReceiverInfo(aa.info.getComponent(), 786432L, userHandle) == null)) {
                        removedAdmin = true;
                        policy.mAdminList.remove(i);
                        policy.mAdminMap.remove(aa.info.getComponent());
                        pushActiveAdminPackagesLocked(userHandle);
                        pushMeteredDisabledPackages(userHandle);
                    }
                } catch (RemoteException e) {
                }
            }
            if (removedAdmin) {
                policy.validatePasswordOwner();
            }
            boolean removedDelegate = false;
            for (int i2 = policy.mDelegationMap.size() - 1; i2 >= 0; i2--) {
                String delegatePackage = policy.mDelegationMap.keyAt(i2);
                if (isRemovedPackage(packageName, delegatePackage, userHandle)) {
                    policy.mDelegationMap.removeAt(i2);
                    removedDelegate = true;
                }
            }
            ComponentName owner = getOwnerComponent(userHandle);
            if (packageName != null && owner != null && owner.getPackageName().equals(packageName)) {
                startOwnerService(userHandle, "package-broadcast");
            }
            if (removedAdmin || removedDelegate) {
                saveSettingsLocked(policy.mUserId);
            }
        }
        if (removedAdmin) {
            pushUserRestrictions(userHandle);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeCredentialManagementApp(final String packageName) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda109
            @Override // java.lang.Runnable
            public final void run() {
                DevicePolicyManagerService.this.m3057xaac20c47(packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeCredentialManagementApp$0$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3057xaac20c47(String packageName) {
        try {
            KeyChain.KeyChainConnection connection = this.mInjector.keyChainBind();
            IKeyChainService service = connection.getService();
            if (service.hasCredentialManagementApp() && packageName.equals(service.getCredentialManagementAppPackageName())) {
                service.removeCredentialManagementApp();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (RemoteException | AssertionError | IllegalStateException | InterruptedException e) {
            Slogf.e(LOG_TAG, "Unable to remove the credential management app", e);
        }
    }

    private boolean isRemovedPackage(String changedPackage, String targetPackage, int userHandle) {
        if (targetPackage != null) {
            if (changedPackage != null) {
                try {
                    if (!changedPackage.equals(targetPackage)) {
                        return false;
                    }
                } catch (RemoteException e) {
                    return false;
                }
            }
            return this.mIPackageManager.getPackageInfo(targetPackage, 0L, userHandle) == null;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNewPackageInstalled(String packageName, int userHandle) {
        if (!m3013x8b75536b(userHandle).mAppsSuspended) {
            return;
        }
        String[] packagesToSuspend = {packageName};
        if (this.mInjector.getPackageManager(userHandle).getUnsuspendablePackages(packagesToSuspend).length != 0) {
            Slogf.i(LOG_TAG, "Newly installed package is unsuspendable: " + packageName);
            return;
        }
        try {
            this.mIPackageManager.setPackagesSuspendedAsUser(packagesToSuspend, true, (PersistableBundle) null, (PersistableBundle) null, (SuspendDialogInfo) null, PackageManagerService.PLATFORM_PACKAGE_NAME, userHandle);
        } catch (RemoteException e) {
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void setDevicePolicySafetyChecker(DevicePolicySafetyChecker safetyChecker) {
        CallerIdentity callerIdentity = getCallerIdentity();
        Preconditions.checkCallAuthorization(this.mIsAutomotive || isAdb(callerIdentity), "can only set DevicePolicySafetyChecker on automotive builds or from ADB (but caller is %s)", new Object[]{callerIdentity});
        setDevicePolicySafetyCheckerUnchecked(safetyChecker);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDevicePolicySafetyCheckerUnchecked(DevicePolicySafetyChecker safetyChecker) {
        Slogf.i(LOG_TAG, "Setting DevicePolicySafetyChecker as %s", safetyChecker);
        this.mSafetyChecker = safetyChecker;
        this.mInjector.setDevicePolicySafetyChecker(safetyChecker);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DevicePolicySafetyChecker getDevicePolicySafetyChecker() {
        return this.mSafetyChecker;
    }

    private void checkCanExecuteOrThrowUnsafe(int operation) {
        int reason = getUnsafeOperationReason(operation);
        if (reason == -1) {
            return;
        }
        DevicePolicySafetyChecker devicePolicySafetyChecker = this.mSafetyChecker;
        if (devicePolicySafetyChecker == null) {
            throw new UnsafeStateException(operation, reason);
        }
        throw devicePolicySafetyChecker.newUnsafeStateException(operation, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUnsafeOperationReason(int operation) {
        DevicePolicySafetyChecker devicePolicySafetyChecker = this.mSafetyChecker;
        if (devicePolicySafetyChecker == null) {
            return -1;
        }
        return devicePolicySafetyChecker.getUnsafeOperationReason(operation);
    }

    public void setNextOperationSafety(int operation, int reason) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS"));
        Slogf.i(LOG_TAG, "setNextOperationSafety(%s, %s)", DevicePolicyManager.operationToString(operation), DevicePolicyManager.operationSafetyReasonToString(reason));
        this.mSafetyChecker = new OneTimeSafetyChecker(this, operation, reason);
    }

    public boolean isSafeOperation(int reason) {
        DevicePolicySafetyChecker devicePolicySafetyChecker = this.mSafetyChecker;
        if (devicePolicySafetyChecker == null) {
            return true;
        }
        return devicePolicySafetyChecker.isSafeOperation(reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<OwnerShellData> listAllOwners() {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS"));
        return (List) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda88
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3041x6b0c6a26();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$listAllOwners$1$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ List m3041x6b0c6a26() throws Exception {
        SparseArray<DevicePolicyData> userData;
        List<OwnerShellData> owners = this.mOwners.listAllOwners();
        synchronized (getLockObject()) {
            for (int i = 0; i < owners.size(); i++) {
                OwnerShellData owner = owners.get(i);
                owner.isAffiliated = isUserAffiliatedWithDeviceLocked(owner.userId);
            }
            userData = this.mUserData;
        }
        for (int i2 = 0; i2 < userData.size(); i2++) {
            DevicePolicyData policyData = this.mUserData.valueAt(i2);
            int userId = userData.keyAt(i2);
            int parentUserId = this.mUserManagerInternal.getProfileParentId(userId);
            boolean isProfile = parentUserId != userId;
            if (isProfile) {
                for (int j = 0; j < policyData.mAdminList.size(); j++) {
                    ActiveAdmin admin = policyData.mAdminList.get(j);
                    owners.add(OwnerShellData.forManagedProfileOwner(userId, parentUserId, admin.info.getComponent()));
                }
            }
        }
        return owners;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        public final Context mContext;
        private DevicePolicySafetyChecker mSafetyChecker;

        Injector(Context context) {
            this.mContext = context;
        }

        public boolean hasFeature() {
            return getPackageManager().hasSystemFeature("android.software.device_admin");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Context createContextAsUser(UserHandle user) throws PackageManager.NameNotFoundException {
            String packageName = this.mContext.getPackageName();
            return this.mContext.createPackageContextAsUser(packageName, 0, user);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Resources getResources() {
            return this.mContext.getResources();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public UserManager getUserManager() {
            return UserManager.get(this.mContext);
        }

        UserManagerInternal getUserManagerInternal() {
            return (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }

        PackageManagerInternal getPackageManagerInternal() {
            return (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }

        ActivityTaskManagerInternal getActivityTaskManagerInternal() {
            return (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        }

        PermissionControllerManager getPermissionControllerManager(UserHandle user) {
            if (user.equals(this.mContext.getUser())) {
                return (PermissionControllerManager) this.mContext.getSystemService(PermissionControllerManager.class);
            }
            try {
                Context context = this.mContext;
                return (PermissionControllerManager) context.createPackageContextAsUser(context.getPackageName(), 0, user).getSystemService(PermissionControllerManager.class);
            } catch (PackageManager.NameNotFoundException notPossible) {
                throw new IllegalStateException(notPossible);
            }
        }

        UsageStatsManagerInternal getUsageStatsManagerInternal() {
            return (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        }

        NetworkPolicyManagerInternal getNetworkPolicyManagerInternal() {
            return (NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public NotificationManager getNotificationManager() {
            return (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public IIpConnectivityMetrics getIIpConnectivityMetrics() {
            return IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("connmetrics"));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public PackageManager getPackageManager() {
            return this.mContext.getPackageManager();
        }

        PackageManager getPackageManager(int userId) {
            return this.mContext.createContextAsUser(UserHandle.of(userId), 0).getPackageManager();
        }

        PowerManagerInternal getPowerManagerInternal() {
            return (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        }

        TelephonyManager getTelephonyManager() {
            return (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        }

        TrustManager getTrustManager() {
            return (TrustManager) this.mContext.getSystemService("trust");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public AlarmManager getAlarmManager() {
            return (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        }

        ConnectivityManager getConnectivityManager() {
            return (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        }

        VpnManager getVpnManager() {
            return (VpnManager) this.mContext.getSystemService(VpnManager.class);
        }

        LocationManager getLocationManager() {
            return (LocationManager) this.mContext.getSystemService(LocationManager.class);
        }

        IWindowManager getIWindowManager() {
            return IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public IActivityManager getIActivityManager() {
            return ActivityManager.getService();
        }

        IActivityTaskManager getIActivityTaskManager() {
            return ActivityTaskManager.getService();
        }

        ActivityManagerInternal getActivityManagerInternal() {
            return (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        IPermissionManager getIPermissionManager() {
            return AppGlobals.getPermissionManager();
        }

        IBackupManager getIBackupManager() {
            return IBackupManager.Stub.asInterface(ServiceManager.getService(HostingRecord.HOSTING_TYPE_BACKUP));
        }

        IAudioService getIAudioService() {
            return IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        }

        PersistentDataBlockManagerInternal getPersistentDataBlockManagerInternal() {
            return (PersistentDataBlockManagerInternal) LocalServices.getService(PersistentDataBlockManagerInternal.class);
        }

        AppOpsManager getAppOpsManager() {
            return (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        }

        LockSettingsInternal getLockSettingsInternal() {
            return (LockSettingsInternal) LocalServices.getService(LockSettingsInternal.class);
        }

        CrossProfileApps getCrossProfileApps() {
            return (CrossProfileApps) this.mContext.getSystemService(CrossProfileApps.class);
        }

        boolean hasUserSetupCompleted(DevicePolicyData userData) {
            return userData.mUserSetupComplete;
        }

        boolean isBuildDebuggable() {
            return Build.IS_DEBUGGABLE;
        }

        LockPatternUtils newLockPatternUtils() {
            return new LockPatternUtils(this.mContext);
        }

        EnterpriseSpecificIdCalculator newEnterpriseSpecificIdCalculator() {
            return new EnterpriseSpecificIdCalculator(this.mContext);
        }

        boolean storageManagerIsFileBasedEncryptionEnabled() {
            return StorageManager.isFileEncryptedNativeOnly();
        }

        Looper getMyLooper() {
            return Looper.myLooper();
        }

        WifiManager getWifiManager() {
            return (WifiManager) this.mContext.getSystemService(WifiManager.class);
        }

        UsbManager getUsbManager() {
            return (UsbManager) this.mContext.getSystemService(UsbManager.class);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public long binderClearCallingIdentity() {
            return Binder.clearCallingIdentity();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void binderRestoreCallingIdentity(long token) {
            Binder.restoreCallingIdentity(token);
        }

        int binderGetCallingUid() {
            return Binder.getCallingUid();
        }

        int binderGetCallingPid() {
            return Binder.getCallingPid();
        }

        UserHandle binderGetCallingUserHandle() {
            return Binder.getCallingUserHandle();
        }

        boolean binderIsCallingUidMyUid() {
            return Binder.getCallingUid() == Process.myUid();
        }

        void binderWithCleanCallingIdentity(FunctionalUtils.ThrowingRunnable action) {
            Binder.withCleanCallingIdentity(action);
        }

        final <T> T binderWithCleanCallingIdentity(FunctionalUtils.ThrowingSupplier<T> action) {
            return (T) Binder.withCleanCallingIdentity(action);
        }

        final int userHandleGetCallingUserId() {
            return UserHandle.getUserId(binderGetCallingUid());
        }

        void powerManagerGoToSleep(long time, int reason, int flags) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).goToSleep(time, reason, flags);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void powerManagerReboot(String reason) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).reboot(reason);
        }

        boolean recoverySystemRebootWipeUserData(boolean shutdown, String reason, boolean force, boolean wipeEuicc, boolean wipeExtRequested, boolean wipeResetProtectionData) throws IOException {
            return FactoryResetter.newBuilder(this.mContext).setSafetyChecker(this.mSafetyChecker).setReason(reason).setShutdown(shutdown).setForce(force).setWipeEuicc(wipeEuicc).setWipeAdoptableStorage(wipeExtRequested).setWipeFactoryResetProtection(wipeResetProtectionData).build().factoryReset();
        }

        boolean systemPropertiesGetBoolean(String key, boolean def) {
            return SystemProperties.getBoolean(key, def);
        }

        long systemPropertiesGetLong(String key, long def) {
            return SystemProperties.getLong(key, def);
        }

        String systemPropertiesGet(String key, String def) {
            return SystemProperties.get(key, def);
        }

        String systemPropertiesGet(String key) {
            return SystemProperties.get(key);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void systemPropertiesSet(String key, String value) {
            SystemProperties.set(key, value);
        }

        boolean userManagerIsHeadlessSystemUserMode() {
            return UserManager.isHeadlessSystemUserMode();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public PendingIntent pendingIntentGetActivityAsUser(Context context, int requestCode, Intent intent, int flags, Bundle options, UserHandle user) {
            return PendingIntent.getActivityAsUser(context, requestCode, intent, flags, options, user);
        }

        PendingIntent pendingIntentGetBroadcast(Context context, int requestCode, Intent intent, int flags) {
            return PendingIntent.getBroadcast(context, requestCode, intent, flags);
        }

        void registerContentObserver(Uri uri, boolean notifyForDescendents, ContentObserver observer, int userHandle) {
            this.mContext.getContentResolver().registerContentObserver(uri, notifyForDescendents, observer, userHandle);
        }

        int settingsSecureGetIntForUser(String name, int def, int userHandle) {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), name, def, userHandle);
        }

        String settingsSecureGetStringForUser(String name, int userHandle) {
            return Settings.Secure.getStringForUser(this.mContext.getContentResolver(), name, userHandle);
        }

        void settingsSecurePutIntForUser(String name, int value, int userHandle) {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), name, value, userHandle);
        }

        void settingsSecurePutStringForUser(String name, String value, int userHandle) {
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), name, value, userHandle);
        }

        void settingsGlobalPutStringForUser(String name, String value, int userHandle) {
            Settings.Global.putStringForUser(this.mContext.getContentResolver(), name, value, userHandle);
        }

        void settingsSecurePutInt(String name, int value) {
            Settings.Secure.putInt(this.mContext.getContentResolver(), name, value);
        }

        int settingsGlobalGetInt(String name, int def) {
            return Settings.Global.getInt(this.mContext.getContentResolver(), name, def);
        }

        String settingsGlobalGetString(String name) {
            return Settings.Global.getString(this.mContext.getContentResolver(), name);
        }

        void settingsGlobalPutInt(String name, int value) {
            Settings.Global.putInt(this.mContext.getContentResolver(), name, value);
        }

        void settingsSecurePutString(String name, String value) {
            Settings.Secure.putString(this.mContext.getContentResolver(), name, value);
        }

        void settingsGlobalPutString(String name, String value) {
            Settings.Global.putString(this.mContext.getContentResolver(), name, value);
        }

        void settingsSystemPutStringForUser(String name, String value, int userId) {
            Settings.System.putStringForUser(this.mContext.getContentResolver(), name, value, userId);
        }

        void securityLogSetLoggingEnabledProperty(boolean enabled) {
            SecurityLog.setLoggingEnabledProperty(enabled);
        }

        boolean securityLogGetLoggingEnabledProperty() {
            return SecurityLog.getLoggingEnabledProperty();
        }

        boolean securityLogIsLoggingEnabled() {
            return SecurityLog.isLoggingEnabled();
        }

        KeyChain.KeyChainConnection keyChainBind() throws InterruptedException {
            return KeyChain.bind(this.mContext);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public KeyChain.KeyChainConnection keyChainBindAsUser(UserHandle user) throws InterruptedException {
            return KeyChain.bindAsUser(this.mContext, user);
        }

        void postOnSystemServerInitThreadPool(Runnable runnable) {
            SystemServerInitThreadPool.submit(runnable, DevicePolicyManagerService.LOG_TAG);
        }

        public TransferOwnershipMetadataManager newTransferOwnershipMetadataManager() {
            return new TransferOwnershipMetadataManager();
        }

        public void runCryptoSelfTest() {
            CryptoTestHelper.runAndLogSelfTest();
        }

        public String[] getPersonalAppsForSuspension(int userId) {
            return PersonalAppsSuspensionHelper.forUser(this.mContext, userId).getPersonalAppsForSuspension();
        }

        public long systemCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        public boolean isChangeEnabled(long changeId, String packageName, int userId) {
            return CompatChanges.isChangeEnabled(changeId, packageName, UserHandle.of(userId));
        }

        void setDevicePolicySafetyChecker(DevicePolicySafetyChecker safetyChecker) {
            this.mSafetyChecker = safetyChecker;
        }

        DeviceManagementResourcesProvider getDeviceManagementResourcesProvider() {
            return new DeviceManagementResourcesProvider();
        }
    }

    public DevicePolicyManagerService(Context context) {
        this(new Injector(context.createAttributionContext(ATTRIBUTION_TAG)), new PolicyPathProvider() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService.2
        });
    }

    DevicePolicyManagerService(Injector injector, PolicyPathProvider pathProvider) {
        this.mPolicyCache = new DevicePolicyCacheImpl();
        this.mStateCache = new DeviceStateCacheImpl();
        this.mESIDInitilizationLock = new Object();
        this.mPackagesToRemove = new ArraySet();
        this.mToken = new Binder();
        this.mLogoutUserId = -10000;
        this.mNetworkLoggingNotificationUserId = -10000;
        this.mStatLogger = new StatLogger(new String[]{"LockGuard.guard()"});
        this.mLockDoNoUseDirectly = LockGuard.installNewLock(8, true);
        this.mPendingUserCreatedCallbackTokens = new ArrayList<>();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                final int userHandle = intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId());
                if ("android.intent.action.USER_STARTED".equals(action) && userHandle == 0) {
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        if (DevicePolicyManagerService.this.isNetworkLoggingEnabledInternalLocked()) {
                            DevicePolicyManagerService.this.setNetworkLoggingActiveInternal(true);
                        }
                    }
                }
                if ("android.intent.action.BOOT_COMPLETED".equals(action) && userHandle == DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserId()) {
                    DevicePolicyManagerService.this.mBugreportCollectionManager.checkForPendingBugreportAfterBoot();
                }
                if ("android.intent.action.BOOT_COMPLETED".equals(action) || DevicePolicyManagerService.ACTION_EXPIRED_PASSWORD_NOTIFICATION.equals(action)) {
                    DevicePolicyManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            DevicePolicyManagerService.this.handlePasswordExpirationNotification(userHandle);
                        }
                    });
                }
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_ADDED", userHandle);
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        DevicePolicyManagerService.this.maybePauseDeviceWideLoggingLocked();
                    }
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_REMOVED", userHandle);
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        boolean isRemovedUserAffiliated = DevicePolicyManagerService.this.isUserAffiliatedWithDeviceLocked(userHandle);
                        DevicePolicyManagerService.this.removeUserData(userHandle);
                        if (!isRemovedUserAffiliated) {
                            DevicePolicyManagerService.this.discardDeviceWideLogsLocked();
                            DevicePolicyManagerService.this.maybeResumeDeviceWideLoggingLocked();
                        }
                    }
                } else if ("android.intent.action.USER_STARTED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_STARTED", userHandle);
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        DevicePolicyManagerService.this.maybeSendAdminEnabledBroadcastLocked(userHandle);
                        DevicePolicyManagerService.this.mUserData.remove(userHandle);
                    }
                    DevicePolicyManagerService.this.handlePackagesChanged(null, userHandle);
                    DevicePolicyManagerService.this.updatePersonalAppsSuspensionOnUserStart(userHandle);
                } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_STOPPED", userHandle);
                    if (DevicePolicyManagerService.this.isManagedProfile(userHandle)) {
                        Slogf.d(DevicePolicyManagerService.LOG_TAG, "Managed profile was stopped");
                        DevicePolicyManagerService.this.updatePersonalAppsSuspension(userHandle, false);
                    }
                } else if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_SWITCHED", userHandle);
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        DevicePolicyManagerService.this.maybeSendAdminEnabledBroadcastLocked(userHandle);
                    }
                    if (DevicePolicyManagerService.this.isManagedProfile(userHandle)) {
                        Slogf.d(DevicePolicyManagerService.LOG_TAG, "Managed profile became unlocked");
                        boolean suspended = DevicePolicyManagerService.this.updatePersonalAppsSuspension(userHandle, true);
                        DevicePolicyManagerService.this.triggerPolicyComplianceCheckIfNeeded(userHandle, suspended);
                    }
                } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                    DevicePolicyManagerService.this.handlePackagesChanged(null, userHandle);
                } else if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                    DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), userHandle);
                } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), userHandle);
                    } else {
                        DevicePolicyManagerService.this.handleNewPackageInstalled(intent.getData().getSchemeSpecificPart(), userHandle);
                    }
                } else if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), userHandle);
                    DevicePolicyManagerService.this.removeCredentialManagementApp(intent.getData().getSchemeSpecificPart());
                } else if ("android.intent.action.MANAGED_PROFILE_ADDED".equals(action)) {
                    DevicePolicyManagerService.this.clearWipeProfileNotification();
                } else if ("android.intent.action.DATE_CHANGED".equals(action) || "android.intent.action.TIME_SET".equals(action)) {
                    DevicePolicyManagerService.this.updateSystemUpdateFreezePeriodsRecord(true);
                    int userId = DevicePolicyManagerService.this.getManagedUserId(0);
                    if (userId >= 0) {
                        DevicePolicyManagerService devicePolicyManagerService = DevicePolicyManagerService.this;
                        devicePolicyManagerService.updatePersonalAppsSuspension(userId, devicePolicyManagerService.mUserManager.isUserUnlocked(userId));
                    }
                } else if (DevicePolicyManagerService.ACTION_PROFILE_OFF_DEADLINE.equals(action)) {
                    Slogf.i(DevicePolicyManagerService.LOG_TAG, "Profile off deadline alarm was triggered");
                    int userId2 = DevicePolicyManagerService.this.getManagedUserId(0);
                    if (userId2 >= 0) {
                        DevicePolicyManagerService devicePolicyManagerService2 = DevicePolicyManagerService.this;
                        devicePolicyManagerService2.updatePersonalAppsSuspension(userId2, devicePolicyManagerService2.mUserManager.isUserUnlocked(userId2));
                        return;
                    }
                    Slogf.wtf(DevicePolicyManagerService.LOG_TAG, "Got deadline alarm for nonexistent profile");
                } else if (DevicePolicyManagerService.ACTION_TURN_PROFILE_ON_NOTIFICATION.equals(action)) {
                    Slogf.i(DevicePolicyManagerService.LOG_TAG, "requesting to turn on the profile: " + userHandle);
                    DevicePolicyManagerService.this.mUserManager.requestQuietModeEnabled(false, UserHandle.of(userHandle));
                }
            }

            private void sendDeviceOwnerUserCommand(String action, int userHandle) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    ActiveAdmin deviceOwner = DevicePolicyManagerService.this.getDeviceOwnerAdminLocked();
                    if (deviceOwner != null) {
                        Bundle extras = new Bundle();
                        extras.putParcelable("android.intent.extra.USER", UserHandle.of(userHandle));
                        DevicePolicyManagerService.this.sendAdminCommandLocked(deviceOwner, action, extras, null, true);
                    }
                }
            }
        };
        this.mReceiver = broadcastReceiver;
        DevicePolicyManager.disableLocalCaches();
        this.mInjector = injector;
        this.mPathProvider = pathProvider;
        Context context = (Context) Objects.requireNonNull(injector.mContext);
        this.mContext = context;
        Handler handler = new Handler((Looper) Objects.requireNonNull(injector.getMyLooper()));
        this.mHandler = handler;
        DevicePolicyConstantsObserver devicePolicyConstantsObserver = new DevicePolicyConstantsObserver(handler);
        this.mConstantsObserver = devicePolicyConstantsObserver;
        devicePolicyConstantsObserver.register();
        this.mConstants = loadConstants();
        this.mUserManager = (UserManager) Objects.requireNonNull(injector.getUserManager());
        UserManagerInternal userManagerInternal = (UserManagerInternal) Objects.requireNonNull(injector.getUserManagerInternal());
        this.mUserManagerInternal = userManagerInternal;
        this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) Objects.requireNonNull(injector.getUsageStatsManagerInternal());
        this.mIPackageManager = (IPackageManager) Objects.requireNonNull(injector.getIPackageManager());
        this.mIPermissionManager = (IPermissionManager) Objects.requireNonNull(injector.getIPermissionManager());
        this.mTelephonyManager = (TelephonyManager) Objects.requireNonNull(injector.getTelephonyManager());
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        this.mLockPatternUtils = injector.newLockPatternUtils();
        this.mLockSettingsInternal = injector.getLockSettingsInternal();
        this.mSecurityLogMonitor = new SecurityLogMonitor(this);
        boolean hasFeature = injector.hasFeature();
        this.mHasFeature = hasFeature;
        this.mIsWatch = injector.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        this.mHasTelephonyFeature = injector.getPackageManager().hasSystemFeature("android.hardware.telephony");
        this.mIsAutomotive = injector.getPackageManager().hasSystemFeature("android.hardware.type.automotive");
        Handler handler2 = BackgroundThread.getHandler();
        this.mBackgroundHandler = handler2;
        this.mCertificateMonitor = new CertificateMonitor(this, injector, handler2);
        this.mDeviceAdminServiceController = new DeviceAdminServiceController(this, this.mConstants);
        this.mOverlayPackagesProvider = new OverlayPackagesProvider(context);
        this.mTransferOwnershipMetadataManager = injector.newTransferOwnershipMetadataManager();
        this.mBugreportCollectionManager = new RemoteBugreportManager(this, injector);
        DeviceManagementResourcesProvider deviceManagementResourcesProvider = injector.getDeviceManagementResourcesProvider();
        this.mDeviceManagementResourcesProvider = deviceManagementResourcesProvider;
        DevicePolicyManagementRoleObserver devicePolicyManagementRoleObserver = new DevicePolicyManagementRoleObserver(context);
        this.mDevicePolicyManagementRoleObserver = devicePolicyManagementRoleObserver;
        devicePolicyManagementRoleObserver.register();
        LocalServices.addService(DevicePolicyManagerLiteInternal.class, localService);
        if (hasFeature) {
            performPolicyVersionUpgrade();
        }
        this.mUserData = new SparseArray<>();
        this.mOwners = makeOwners(injector, pathProvider);
        if (!hasFeature) {
            this.mSetupContentObserver = null;
            return;
        }
        loadOwners();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.addAction(ACTION_EXPIRED_PASSWORD_NOTIFICATION);
        filter.addAction(ACTION_TURN_PROFILE_ON_NOTIFICATION);
        filter.addAction(ACTION_PROFILE_OFF_DEADLINE);
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_STARTED");
        filter.addAction("android.intent.action.USER_STOPPED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_UNLOCKED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        filter.setPriority(1000);
        context.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, filter, null, handler);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.PACKAGE_CHANGED");
        filter2.addAction("android.intent.action.PACKAGE_REMOVED");
        filter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        filter2.addAction("android.intent.action.PACKAGE_ADDED");
        filter2.addDataScheme("package");
        context.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, filter2, null, handler);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        filter3.addAction("android.intent.action.TIME_SET");
        filter3.addAction("android.intent.action.DATE_CHANGED");
        context.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, filter3, null, handler);
        LocalServices.addService(DevicePolicyManagerInternal.class, localService);
        this.mSetupContentObserver = new SetupContentObserver(handler);
        userManagerInternal.addUserRestrictionsListener(new RestrictionsListener(context, userManagerInternal, this));
        userManagerInternal.addUserLifecycleListener(new UserLifecycleListener());
        deviceManagementResourcesProvider.load();
        invalidateBinderCaches();
    }

    private Owners makeOwners(Injector injector, PolicyPathProvider pathProvider) {
        return new Owners(injector.getUserManager(), injector.getUserManagerInternal(), injector.getPackageManagerInternal(), injector.getActivityTaskManagerInternal(), injector.getActivityManagerInternal(), pathProvider);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void invalidateBinderCaches() {
        DevicePolicyManager.invalidateBinderCaches();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: getUserData */
    public DevicePolicyData m3013x8b75536b(int userHandle) {
        DevicePolicyData policy;
        synchronized (getLockObject()) {
            policy = this.mUserData.get(userHandle);
            if (policy == null) {
                policy = new DevicePolicyData(userHandle);
                this.mUserData.append(userHandle, policy);
                loadSettingsLocked(policy, userHandle);
                if (userHandle == 0) {
                    this.mStateCache.setDeviceProvisioned(policy.mUserSetupComplete);
                }
            }
        }
        return policy;
    }

    DevicePolicyData getUserDataUnchecked(final int userHandle) {
        return (DevicePolicyData) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda72
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3013x8b75536b(userHandle);
            }
        });
    }

    void removeUserData(int userHandle) {
        synchronized (getLockObject()) {
            if (userHandle == 0) {
                Slogf.w(LOG_TAG, "Tried to remove device policy file for user 0! Ignoring.");
                return;
            }
            updatePasswordQualityCacheForUserGroup(userHandle);
            this.mPolicyCache.onUserRemoved(userHandle);
            if (isManagedProfile(userHandle)) {
                clearManagedProfileApnUnchecked();
            }
            boolean isOrgOwned = this.mOwners.isProfileOwnerOfOrganizationOwnedDevice(userHandle);
            this.mOwners.removeProfileOwner(userHandle);
            this.mOwners.writeProfileOwner(userHandle);
            pushScreenCapturePolicy(userHandle);
            DevicePolicyData policy = this.mUserData.get(userHandle);
            if (policy != null) {
                this.mUserData.remove(userHandle);
            }
            File policyFile = new File(this.mPathProvider.getUserSystemDirectory(userHandle), DEVICE_POLICIES_XML);
            policyFile.delete();
            Slogf.i(LOG_TAG, "Removed device policy file " + policyFile.getAbsolutePath());
            if (isOrgOwned) {
                UserInfo primaryUser = this.mUserManager.getPrimaryUser();
                if (primaryUser != null) {
                    clearOrgOwnedProfileOwnerDeviceWidePolicies(primaryUser.id);
                } else {
                    Slogf.wtf(LOG_TAG, "Was unable to get primary user.");
                }
            }
        }
    }

    void loadOwners() {
        synchronized (getLockObject()) {
            this.mOwners.load();
            setDeviceOwnershipSystemPropertyLocked();
            if (this.mOwners.hasDeviceOwner()) {
                Owners owners = this.mOwners;
                setGlobalSettingDeviceOwnerType(owners.getDeviceOwnerType(owners.getDeviceOwnerPackageName()));
            }
        }
    }

    private CallerIdentity getCallerIdentity() {
        return getCallerIdentity(null, null);
    }

    private CallerIdentity getCallerIdentity(String callerPackage) {
        return getCallerIdentity(null, callerPackage);
    }

    CallerIdentity getCallerIdentity(ComponentName adminComponent) {
        return getCallerIdentity(adminComponent, null);
    }

    CallerIdentity getCallerIdentity(ComponentName adminComponent, String callerPackage) {
        int callerUid = this.mInjector.binderGetCallingUid();
        if (callerPackage != null && !isCallingFromPackage(callerPackage, callerUid)) {
            throw new SecurityException(String.format("Caller with uid %d is not %s", Integer.valueOf(callerUid), callerPackage));
        }
        if (adminComponent != null) {
            DevicePolicyData policy = m3013x8b75536b(UserHandle.getUserId(callerUid));
            ActiveAdmin admin = policy.mAdminMap.get(adminComponent);
            if (admin == null || admin.getUid() != callerUid) {
                throw new SecurityException(String.format("Admin %s does not exist or is not owned by uid %d", adminComponent, Integer.valueOf(callerUid)));
            }
            if (callerPackage != null) {
                Preconditions.checkArgument(callerPackage.equals(adminComponent.getPackageName()));
            } else {
                callerPackage = adminComponent.getPackageName();
            }
        }
        return new CallerIdentity(callerUid, callerPackage, adminComponent);
    }

    private void migrateToProfileOnOrganizationOwnedDeviceIfCompLocked() {
        int doUserId = this.mOwners.getDeviceOwnerUserId();
        if (doUserId == -10000) {
            return;
        }
        List<UserInfo> profiles = this.mUserManager.getProfiles(doUserId);
        if (profiles.size() != 2) {
            if (profiles.size() != 1) {
                Slogf.wtf(LOG_TAG, "Found " + profiles.size() + " profiles, skipping migration");
                return;
            }
            return;
        }
        int poUserId = getManagedUserId(doUserId);
        if (poUserId < 0) {
            Slogf.wtf(LOG_TAG, "Found DO and a profile, but it is not managed, skipping migration");
            return;
        }
        ActiveAdmin doAdmin = getDeviceOwnerAdminLocked();
        ActiveAdmin poAdmin = getProfileOwnerAdminLocked(poUserId);
        if (doAdmin == null || poAdmin == null) {
            Slogf.wtf(LOG_TAG, "Failed to get either PO or DO admin, aborting migration.");
            return;
        }
        ComponentName doAdminComponent = this.mOwners.getDeviceOwnerComponent();
        ComponentName poAdminComponent = this.mOwners.getProfileOwnerComponent(poUserId);
        if (doAdminComponent == null || poAdminComponent == null) {
            Slogf.wtf(LOG_TAG, "Cannot find PO or DO component name, aborting migration.");
        } else if (!doAdminComponent.getPackageName().equals(poAdminComponent.getPackageName())) {
            Slogf.e(LOG_TAG, "DO and PO are different packages, aborting migration.");
        } else {
            Slogf.i(LOG_TAG, "Migrating COMP to PO on a corp owned device; primary user: %d; profile: %d", Integer.valueOf(doUserId), Integer.valueOf(poUserId));
            Slogf.i(LOG_TAG, "Giving the PO additional power...");
            setProfileOwnerOnOrganizationOwnedDeviceUncheckedLocked(poAdminComponent, poUserId, true);
            Slogf.i(LOG_TAG, "Migrating DO policies to PO...");
            moveDoPoliciesToProfileParentAdminLocked(doAdmin, poAdmin.getParentActiveAdmin());
            migratePersonalAppSuspensionLocked(doUserId, poUserId, poAdmin);
            saveSettingsLocked(poUserId);
            Slogf.i(LOG_TAG, "Clearing the DO...");
            ComponentName doAdminReceiver = doAdmin.info.getComponent();
            clearDeviceOwnerLocked(doAdmin, doUserId);
            Slogf.i(LOG_TAG, "Removing admin artifacts...");
            removeAdminArtifacts(doAdminReceiver, doUserId);
            Slogf.i(LOG_TAG, "Uninstalling the DO...");
            uninstallOrDisablePackage(doAdminComponent.getPackageName(), doUserId);
            Slogf.i(LOG_TAG, "Migration complete.");
            DevicePolicyEventLogger.createEvent(137).setAdmin(poAdminComponent).write();
        }
    }

    private void migratePersonalAppSuspensionLocked(int doUserId, int poUserId, ActiveAdmin poAdmin) {
        PackageManagerInternal pmi = this.mInjector.getPackageManagerInternal();
        if (!pmi.isSuspendingAnyPackages(PackageManagerService.PLATFORM_PACKAGE_NAME, doUserId)) {
            Slogf.i(LOG_TAG, "DO is not suspending any apps.");
        } else if (getTargetSdk(poAdmin.info.getPackageName(), poUserId) >= 30) {
            Slogf.i(LOG_TAG, "PO is targeting R+, keeping personal apps suspended.");
            m3013x8b75536b(doUserId).mAppsSuspended = true;
            poAdmin.mSuspendPersonalApps = true;
        } else {
            Slogf.i(LOG_TAG, "PO isn't targeting R+, unsuspending personal apps.");
            pmi.unsuspendForSuspendingPackage(PackageManagerService.PLATFORM_PACKAGE_NAME, doUserId);
        }
    }

    private void uninstallOrDisablePackage(final String packageName, final int userId) {
        try {
            ApplicationInfo appInfo = this.mIPackageManager.getApplicationInfo(packageName, 786432L, userId);
            if (appInfo == null) {
                Slogf.wtf(LOG_TAG, "Failed to get package info for " + packageName);
            } else if ((appInfo.flags & 1) != 0) {
                Slogf.i(LOG_TAG, "Package %s is pre-installed, marking disabled until used", packageName);
                this.mContext.getPackageManager().setApplicationEnabledSetting(packageName, 4, 0);
            } else {
                IIntentSender.Stub mLocalSender = new IIntentSender.Stub() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService.3
                    public void send(int code, Intent intent, String resolvedType, IBinder allowlistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                        int status = intent.getIntExtra("android.content.pm.extra.STATUS", 1);
                        if (status == 0) {
                            Slogf.i(DevicePolicyManagerService.LOG_TAG, "Package %s uninstalled for user %d", packageName, Integer.valueOf(userId));
                        } else {
                            Slogf.e(DevicePolicyManagerService.LOG_TAG, "Failed to uninstall %s; status: %d", packageName, Integer.valueOf(status));
                        }
                    }
                };
                PackageInstaller pi = this.mInjector.getPackageManager(userId).getPackageInstaller();
                pi.uninstall(packageName, 0, new IntentSender(mLocalSender));
            }
        } catch (RemoteException e) {
        }
    }

    private void moveDoPoliciesToProfileParentAdminLocked(ActiveAdmin doAdmin, ActiveAdmin parentAdmin) {
        if (parentAdmin.mPasswordPolicy.quality == 0) {
            parentAdmin.mPasswordPolicy = doAdmin.mPasswordPolicy;
        }
        if (parentAdmin.passwordHistoryLength == 0) {
            parentAdmin.passwordHistoryLength = doAdmin.passwordHistoryLength;
        }
        if (parentAdmin.passwordExpirationTimeout == 0) {
            parentAdmin.passwordExpirationTimeout = doAdmin.passwordExpirationTimeout;
        }
        if (parentAdmin.maximumFailedPasswordsForWipe == 0) {
            parentAdmin.maximumFailedPasswordsForWipe = doAdmin.maximumFailedPasswordsForWipe;
        }
        if (parentAdmin.maximumTimeToUnlock == 0) {
            parentAdmin.maximumTimeToUnlock = doAdmin.maximumTimeToUnlock;
        }
        if (parentAdmin.strongAuthUnlockTimeout == 259200000) {
            parentAdmin.strongAuthUnlockTimeout = doAdmin.strongAuthUnlockTimeout;
        }
        parentAdmin.disabledKeyguardFeatures |= doAdmin.disabledKeyguardFeatures & 438;
        parentAdmin.trustAgentInfos.putAll((ArrayMap<? extends String, ? extends ActiveAdmin.TrustAgentInfo>) doAdmin.trustAgentInfos);
        parentAdmin.disableCamera = doAdmin.disableCamera;
        parentAdmin.disableScreenCapture = doAdmin.disableScreenCapture;
        parentAdmin.accountTypesWithManagementDisabled.addAll(doAdmin.accountTypesWithManagementDisabled);
        moveDoUserRestrictionsToCopeParent(doAdmin, parentAdmin);
        if (doAdmin.requireAutoTime) {
            parentAdmin.ensureUserRestrictions().putBoolean("no_config_date_time", true);
        }
    }

    private void moveDoUserRestrictionsToCopeParent(ActiveAdmin doAdmin, ActiveAdmin parentAdmin) {
        if (doAdmin.userRestrictions == null) {
            return;
        }
        for (String restriction : doAdmin.userRestrictions.keySet()) {
            if (UserRestrictionsUtils.canProfileOwnerOfOrganizationOwnedDeviceChange(restriction)) {
                parentAdmin.ensureUserRestrictions().putBoolean(restriction, doAdmin.userRestrictions.getBoolean(restriction));
            }
        }
    }

    private void applyProfileRestrictionsIfDeviceOwnerLocked() {
        int doUserId = this.mOwners.getDeviceOwnerUserId();
        if (doUserId == -10000) {
            return;
        }
        UserHandle doUserHandle = UserHandle.of(doUserId);
        if (!this.mUserManager.hasUserRestriction("no_add_clone_profile", doUserHandle)) {
            this.mUserManager.setUserRestriction("no_add_clone_profile", true, doUserHandle);
        }
        if (!this.mUserManager.hasUserRestriction("no_add_managed_profile", doUserHandle)) {
            this.mUserManager.setUserRestriction("no_add_managed_profile", true, doUserHandle);
        }
    }

    private void maybeSetDefaultProfileOwnerUserRestrictions() {
        synchronized (getLockObject()) {
            for (Integer num : this.mOwners.getProfileOwnerKeys()) {
                int userId = num.intValue();
                ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userId);
                if (profileOwner != null && this.mUserManager.isManagedProfile(userId)) {
                    maybeSetDefaultRestrictionsForAdminLocked(userId, profileOwner, UserRestrictionsUtils.getDefaultEnabledForManagedProfiles());
                    ensureUnknownSourcesRestrictionForProfileOwnerLocked(userId, profileOwner, false);
                }
            }
        }
    }

    private void ensureUnknownSourcesRestrictionForProfileOwnerLocked(int userId, ActiveAdmin profileOwner, boolean newOwner) {
        if (newOwner || this.mInjector.settingsSecureGetIntForUser("unknown_sources_default_reversed", 0, userId) != 0) {
            profileOwner.ensureUserRestrictions().putBoolean("no_install_unknown_sources", true);
            saveUserRestrictionsLocked(userId);
            this.mInjector.settingsSecurePutIntForUser("unknown_sources_default_reversed", 0, userId);
        }
    }

    private void maybeSetDefaultRestrictionsForAdminLocked(int userId, ActiveAdmin admin, Set<String> defaultRestrictions) {
        if (defaultRestrictions.equals(admin.defaultEnabledRestrictionsAlreadySet)) {
            return;
        }
        Slogf.i(LOG_TAG, "New user restrictions need to be set by default for user " + userId);
        Set<String> restrictionsToSet = new ArraySet<>(defaultRestrictions);
        restrictionsToSet.removeAll(admin.defaultEnabledRestrictionsAlreadySet);
        if (!restrictionsToSet.isEmpty()) {
            for (String restriction : restrictionsToSet) {
                admin.ensureUserRestrictions().putBoolean(restriction, true);
            }
            admin.defaultEnabledRestrictionsAlreadySet.addAll(restrictionsToSet);
            Slogf.i(LOG_TAG, "Enabled the following restrictions by default: " + restrictionsToSet);
            saveUserRestrictionsLocked(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDeviceOwnershipSystemPropertyLocked() {
        boolean z = false;
        boolean deviceProvisioned = this.mInjector.settingsGlobalGetInt("device_provisioned", 0) != 0;
        boolean hasDeviceOwner = this.mOwners.hasDeviceOwner();
        boolean hasOrgOwnedProfile = isOrganizationOwnedDeviceWithManagedProfile();
        if (!hasDeviceOwner && !hasOrgOwnedProfile && !deviceProvisioned) {
            return;
        }
        if (hasDeviceOwner || hasOrgOwnedProfile) {
            z = true;
        }
        String value = Boolean.toString(z);
        String currentVal = this.mInjector.systemPropertiesGet(PROPERTY_ORGANIZATION_OWNED, null);
        if (TextUtils.isEmpty(currentVal)) {
            Slogf.i(LOG_TAG, "Set ro.organization_owned property to " + value);
            this.mInjector.systemPropertiesSet(PROPERTY_ORGANIZATION_OWNED, value);
        } else if (!value.equals(currentVal)) {
            Slogf.w(LOG_TAG, "Cannot change existing ro.organization_owned to " + value);
        }
    }

    private void maybeStartSecurityLogMonitorOnActivityManagerReady() {
        synchronized (getLockObject()) {
            if (this.mInjector.securityLogIsLoggingEnabled()) {
                this.mSecurityLogMonitor.start(getSecurityLoggingEnabledUser());
                this.mInjector.runCryptoSelfTest();
                maybePauseDeviceWideLoggingLocked();
            }
        }
    }

    private void fixupAutoTimeRestrictionDuringOrganizationOwnedDeviceMigration() {
        ActiveAdmin parent;
        for (UserInfo ui : this.mUserManager.getUsers()) {
            int userId = ui.id;
            if (isProfileOwnerOfOrganizationOwnedDevice(userId) && (parent = getProfileOwnerAdminLocked(userId).parentAdmin) != null && parent.requireAutoTime) {
                parent.requireAutoTime = false;
                saveSettingsLocked(userId);
                this.mUserManagerInternal.setDevicePolicyUserRestrictions(0, new Bundle(), new RestrictionsSet(), false);
                parent.ensureUserRestrictions().putBoolean("no_config_date_time", true);
                pushUserRestrictions(userId);
            }
        }
    }

    private void setExpirationAlarmCheckLocked(final Context context, final int userHandle, final boolean parent) {
        long alarmTime;
        long expiration = getPasswordExpirationLocked(null, userHandle, parent);
        long now = System.currentTimeMillis();
        long timeToExpire = expiration - now;
        if (expiration == 0) {
            alarmTime = 0;
        } else if (timeToExpire <= 0) {
            alarmTime = MS_PER_DAY + now;
        } else {
            long alarmInterval = timeToExpire % MS_PER_DAY;
            if (alarmInterval == 0) {
                alarmInterval = MS_PER_DAY;
            }
            alarmTime = now + alarmInterval;
        }
        final long j = alarmTime;
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda16
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3083x6312a575(parent, userHandle, context, j);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setExpirationAlarmCheckLocked$3$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3083x6312a575(boolean parent, int userHandle, Context context, long alarmTime) throws Exception {
        int affectedUserHandle = parent ? getProfileParentId(userHandle) : userHandle;
        AlarmManager am = this.mInjector.getAlarmManager();
        PendingIntent pi = PendingIntent.getBroadcastAsUser(context, REQUEST_EXPIRE_PASSWORD, new Intent(ACTION_EXPIRED_PASSWORD_NOTIFICATION), 1275068416, UserHandle.of(affectedUserHandle));
        am.cancel(pi);
        if (alarmTime != 0) {
            am.set(1, alarmTime, pi);
        }
    }

    ActiveAdmin getActiveAdminUncheckedLocked(ComponentName who, int userHandle) {
        ensureLocked();
        ActiveAdmin admin = m3013x8b75536b(userHandle).mAdminMap.get(who);
        if (admin != null && who.getPackageName().equals(admin.info.getActivityInfo().packageName) && who.getClassName().equals(admin.info.getActivityInfo().name)) {
            return admin;
        }
        return null;
    }

    ActiveAdmin getActiveAdminUncheckedLocked(ComponentName who, int userHandle, boolean parent) {
        ensureLocked();
        if (parent) {
            Preconditions.checkCallAuthorization(isManagedProfile(userHandle), "You can not call APIs on the parent profile outside a managed profile, userId = %d", new Object[]{Integer.valueOf(userHandle)});
        }
        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
        if (admin != null && parent) {
            return admin.getParentActiveAdmin();
        }
        return admin;
    }

    ActiveAdmin getActiveAdminForCallerLocked(ComponentName who, int reqPolicy) throws SecurityException {
        return getActiveAdminOrCheckPermissionForCallerLocked(who, reqPolicy, null);
    }

    ActiveAdmin getDeviceOwnerLocked(CallerIdentity caller) {
        ensureLocked();
        ComponentName doComponent = this.mOwners.getDeviceOwnerComponent();
        Preconditions.checkState(doComponent != null, "No device owner for user %d", new Object[]{Integer.valueOf(caller.getUid())});
        ActiveAdmin doAdmin = m3013x8b75536b(caller.getUserId()).mAdminMap.get(doComponent);
        Preconditions.checkState(doAdmin != null, "Device owner %s for user %d not found", new Object[]{doComponent, Integer.valueOf(caller.getUid())});
        Preconditions.checkCallAuthorization(doAdmin.getUid() == caller.getUid(), "Admin %s is not owned by uid %d, but uid %d", new Object[]{doComponent, Integer.valueOf(caller.getUid()), Integer.valueOf(doAdmin.getUid())});
        Preconditions.checkCallAuthorization(!caller.hasAdminComponent() || doAdmin.info.getComponent().equals(caller.getComponentName()), "Caller component %s is not device owner", new Object[]{caller.getComponentName()});
        return doAdmin;
    }

    ActiveAdmin getProfileOwnerLocked(CallerIdentity caller) {
        ensureLocked();
        ComponentName poAdminComponent = this.mOwners.getProfileOwnerComponent(caller.getUserId());
        Preconditions.checkState(poAdminComponent != null, "No profile owner for user %d", new Object[]{Integer.valueOf(caller.getUid())});
        ActiveAdmin poAdmin = m3013x8b75536b(caller.getUserId()).mAdminMap.get(poAdminComponent);
        Preconditions.checkState(poAdmin != null, "No device profile owner for caller %d", new Object[]{Integer.valueOf(caller.getUid())});
        Preconditions.checkCallAuthorization(poAdmin.getUid() == caller.getUid(), "Admin %s is not owned by uid %d", new Object[]{poAdminComponent, Integer.valueOf(caller.getUid())});
        Preconditions.checkCallAuthorization(!caller.hasAdminComponent() || poAdmin.info.getComponent().equals(caller.getComponentName()), "Caller component %s is not profile owner", new Object[]{caller.getComponentName()});
        return poAdmin;
    }

    ActiveAdmin getOrganizationOwnedProfileOwnerLocked(CallerIdentity caller) {
        ActiveAdmin profileOwner = getProfileOwnerLocked(caller);
        Preconditions.checkCallAuthorization(this.mOwners.isProfileOwnerOfOrganizationOwnedDevice(caller.getUserId()), "Admin %s is not of an org-owned device", new Object[]{profileOwner.info.getComponent()});
        return profileOwner;
    }

    ActiveAdmin getProfileOwnerOrDeviceOwnerLocked(CallerIdentity caller) {
        ensureLocked();
        ComponentName poAdminComponent = this.mOwners.getProfileOwnerComponent(caller.getUserId());
        ComponentName doAdminComponent = this.mOwners.getDeviceOwnerComponent();
        if (poAdminComponent == null && doAdminComponent == null) {
            throw new IllegalStateException(String.format("No profile or device owner for user %d", Integer.valueOf(caller.getUid())));
        }
        if (poAdminComponent != null) {
            return getProfileOwnerLocked(caller);
        }
        return getDeviceOwnerLocked(caller);
    }

    ActiveAdmin getParentOfAdminIfRequired(ActiveAdmin admin, boolean parent) {
        Objects.requireNonNull(admin);
        return parent ? admin.getParentActiveAdmin() : admin;
    }

    ActiveAdmin getActiveAdminOrCheckPermissionForCallerLocked(ComponentName who, int reqPolicy, String permission) throws SecurityException {
        ensureLocked();
        CallerIdentity caller = getCallerIdentity();
        ActiveAdmin result = getActiveAdminWithPolicyForUidLocked(who, reqPolicy, caller.getUid());
        if (result != null) {
            return result;
        }
        if (permission != null && hasCallingPermission(permission)) {
            return null;
        }
        if (who != null) {
            DevicePolicyData policy = m3013x8b75536b(caller.getUserId());
            ActiveAdmin admin = policy.mAdminMap.get(who);
            boolean isDeviceOwner = isDeviceOwner(admin.info.getComponent(), caller.getUserId());
            boolean isProfileOwner = isProfileOwner(admin.info.getComponent(), caller.getUserId());
            if (DA_DISALLOWED_POLICIES.contains(Integer.valueOf(reqPolicy)) && !isDeviceOwner && !isProfileOwner) {
                throw new SecurityException("Admin " + admin.info.getComponent() + " is not a device owner or profile owner, so may not use policy: " + admin.info.getTagForPolicy(reqPolicy));
            }
            throw new SecurityException("Admin " + admin.info.getComponent() + " did not specify uses-policy for: " + admin.info.getTagForPolicy(reqPolicy));
        }
        throw new SecurityException("No active admin owned by uid " + caller.getUid() + " for policy #" + reqPolicy + (permission == null ? "" : ", which doesn't have " + permission));
    }

    ActiveAdmin getActiveAdminForCallerLocked(ComponentName who, int reqPolicy, boolean parent) throws SecurityException {
        return getActiveAdminOrCheckPermissionForCallerLocked(who, reqPolicy, parent, null);
    }

    ActiveAdmin getActiveAdminOrCheckPermissionForCallerLocked(ComponentName who, int reqPolicy, boolean parent, String permission) throws SecurityException {
        ensureLocked();
        if (parent) {
            Preconditions.checkCallingUser(isManagedProfile(getCallerIdentity().getUserId()));
        }
        ActiveAdmin admin = getActiveAdminOrCheckPermissionForCallerLocked(who, reqPolicy, permission);
        return parent ? admin.getParentActiveAdmin() : admin;
    }

    private ActiveAdmin getActiveAdminForUidLocked(ComponentName who, int uid) {
        ensureLocked();
        int userId = UserHandle.getUserId(uid);
        DevicePolicyData policy = m3013x8b75536b(userId);
        ActiveAdmin admin = policy.mAdminMap.get(who);
        if (admin == null) {
            throw new SecurityException("No active admin " + who + " for UID " + uid);
        }
        if (admin.getUid() != uid) {
            throw new SecurityException("Admin " + who + " is not owned by uid " + uid);
        }
        return admin;
    }

    private ActiveAdmin getActiveAdminWithPolicyForUidLocked(ComponentName who, int reqPolicy, int uid) {
        ensureLocked();
        int userId = UserHandle.getUserId(uid);
        DevicePolicyData policy = m3013x8b75536b(userId);
        if (who != null) {
            ActiveAdmin admin = policy.mAdminMap.get(who);
            if (admin == null) {
                throw new SecurityException("No active admin " + who);
            }
            if (admin.getUid() != uid) {
                throw new SecurityException("Admin " + who + " is not owned by uid " + uid);
            }
            if (isActiveAdminWithPolicyForUserLocked(admin, reqPolicy, userId)) {
                return admin;
            }
            return null;
        }
        Iterator<ActiveAdmin> it = policy.mAdminList.iterator();
        while (it.hasNext()) {
            ActiveAdmin admin2 = it.next();
            if (admin2.getUid() == uid && isActiveAdminWithPolicyForUserLocked(admin2, reqPolicy, userId)) {
                return admin2;
            }
        }
        return null;
    }

    boolean isActiveAdminWithPolicyForUserLocked(ActiveAdmin admin, int reqPolicy, int userId) {
        ensureLocked();
        boolean ownsDevice = isDeviceOwner(admin.info.getComponent(), userId);
        boolean ownsProfile = isProfileOwner(admin.info.getComponent(), userId);
        boolean allowedToUsePolicy = ownsDevice || ownsProfile || !DA_DISALLOWED_POLICIES.contains(Integer.valueOf(reqPolicy)) || getTargetSdk(admin.info.getPackageName(), userId) < 29;
        return allowedToUsePolicy && admin.info.usesPolicy(reqPolicy);
    }

    void sendAdminCommandLocked(ActiveAdmin admin, String action) {
        sendAdminCommandLocked(admin, action, null);
    }

    void sendAdminCommandLocked(ActiveAdmin admin, String action, BroadcastReceiver result) {
        sendAdminCommandLocked(admin, action, (Bundle) null, result);
    }

    void sendAdminCommandLocked(ActiveAdmin admin, String action, Bundle adminExtras, BroadcastReceiver result) {
        sendAdminCommandLocked(admin, action, adminExtras, result, false);
    }

    boolean sendAdminCommandLocked(ActiveAdmin admin, String action, Bundle adminExtras, BroadcastReceiver result, boolean inForeground) {
        Intent intent = new Intent(action);
        intent.setComponent(admin.info.getComponent());
        if (UserManager.isDeviceInDemoMode(this.mContext)) {
            intent.addFlags(268435456);
        }
        if (action.equals("android.app.action.ACTION_PASSWORD_EXPIRING")) {
            intent.putExtra("expiration", admin.passwordExpirationDate);
        }
        if (inForeground) {
            intent.addFlags(268435456);
        }
        if (adminExtras != null) {
            intent.putExtras(adminExtras);
        }
        if (this.mInjector.getPackageManager().queryBroadcastReceiversAsUser(intent, 268435456, admin.getUserHandle()).isEmpty()) {
            return false;
        }
        BroadcastOptions options = BroadcastOptions.makeBasic();
        options.setBackgroundActivityStartsAllowed(true);
        if (result == null) {
            this.mContext.sendBroadcastAsUser(intent, admin.getUserHandle(), null, options.toBundle());
            return true;
        }
        this.mContext.sendOrderedBroadcastAsUser(intent, admin.getUserHandle(), null, -1, options.toBundle(), result, this.mHandler, -1, null, null);
        return true;
    }

    void sendAdminCommandLocked(String action, int reqPolicy, int userHandle, Bundle adminExtras) {
        DevicePolicyData policy = m3013x8b75536b(userHandle);
        int count = policy.mAdminList.size();
        for (int i = 0; i < count; i++) {
            ActiveAdmin admin = policy.mAdminList.get(i);
            if (admin.info.usesPolicy(reqPolicy)) {
                sendAdminCommandLocked(admin, action, adminExtras, (BroadcastReceiver) null);
            }
        }
    }

    private void sendAdminCommandToSelfAndProfilesLocked(String action, int reqPolicy, int userHandle, Bundle adminExtras) {
        int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(userHandle);
        for (int profileId : profileIds) {
            sendAdminCommandLocked(action, reqPolicy, profileId, adminExtras);
        }
    }

    private void sendAdminCommandForLockscreenPoliciesLocked(String action, int reqPolicy, int userHandle) {
        Bundle extras = new Bundle();
        extras.putParcelable("android.intent.extra.USER", UserHandle.of(userHandle));
        if (isSeparateProfileChallengeEnabled(userHandle)) {
            sendAdminCommandLocked(action, reqPolicy, userHandle, extras);
        } else {
            sendAdminCommandToSelfAndProfilesLocked(action, reqPolicy, userHandle, extras);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: removeActiveAdminLocked */
    public void m3056x58c4fc7d(final ComponentName adminReceiver, final int userHandle) {
        ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
        DevicePolicyData policy = m3013x8b75536b(userHandle);
        if (admin != null && !policy.mRemovingAdmins.contains(adminReceiver)) {
            policy.mRemovingAdmins.add(adminReceiver);
            sendAdminCommandLocked(admin, "android.app.action.DEVICE_ADMIN_DISABLED", new BroadcastReceiver() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService.4
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    DevicePolicyManagerService.this.removeAdminArtifacts(adminReceiver, userHandle);
                    DevicePolicyManagerService.this.removePackageIfRequired(adminReceiver.getPackageName(), userHandle);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DeviceAdminInfo findAdmin(final ComponentName adminName, final int userHandle, boolean throwForMissingPermission) {
        ActivityInfo ai = (ActivityInfo) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda164
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m2989x74cbccb0(adminName, userHandle);
            }
        });
        if (ai == null) {
            throw new IllegalArgumentException("Unknown admin: " + adminName);
        }
        if (!"android.permission.BIND_DEVICE_ADMIN".equals(ai.permission)) {
            String message = "DeviceAdminReceiver " + adminName + " must be protected with android.permission.BIND_DEVICE_ADMIN";
            Slogf.w(LOG_TAG, message);
            if (throwForMissingPermission && ai.applicationInfo.targetSdkVersion > 23) {
                throw new IllegalArgumentException(message);
            }
        }
        try {
            return new DeviceAdminInfo(this.mContext, ai);
        } catch (IOException | XmlPullParserException e) {
            Slogf.w(LOG_TAG, "Bad device admin requested for user=" + userHandle + ": " + adminName, e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$findAdmin$4$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ ActivityInfo m2989x74cbccb0(ComponentName adminName, int userHandle) throws Exception {
        try {
            return this.mIPackageManager.getReceiverInfo(adminName, 819328L, userHandle);
        } catch (RemoteException e) {
            return null;
        }
    }

    private File getPolicyFileDirectory(int userId) {
        if (userId == 0) {
            return this.mPathProvider.getDataSystemDirectory();
        }
        return this.mPathProvider.getUserSystemDirectory(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public JournaledFile makeJournaledFile(int userId, String fileName) {
        String base = new File(getPolicyFileDirectory(userId), fileName).getAbsolutePath();
        return new JournaledFile(new File(base), new File(base + ".tmp"));
    }

    private JournaledFile makeJournaledFile(int userId) {
        return makeJournaledFile(userId, DEVICE_POLICIES_XML);
    }

    private void saveSettingsForUsersLocked(Set<Integer> affectedUserIds) {
        for (Integer num : affectedUserIds) {
            int userId = num.intValue();
            saveSettingsLocked(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveSettingsLocked(int userHandle) {
        if (DevicePolicyData.store(m3013x8b75536b(userHandle), makeJournaledFile(userHandle), !this.mInjector.storageManagerIsFileBasedEncryptionEnabled())) {
            sendChangedNotification(userHandle);
        }
        invalidateBinderCaches();
    }

    private void sendChangedNotification(final int userHandle) {
        final Intent intent = new Intent("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        intent.setFlags(1073741824);
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda150
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3065x292aa4ee(intent, userHandle);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendChangedNotification$5$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3065x292aa4ee(Intent intent, int userHandle) throws Exception {
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(userHandle));
    }

    private void loadSettingsLocked(DevicePolicyData policy, final int userHandle) {
        DevicePolicyData.load(policy, !this.mInjector.storageManagerIsFileBasedEncryptionEnabled(), makeJournaledFile(userHandle), new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda102
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DevicePolicyManagerService.this.m3044x62bf57b7(userHandle, (ComponentName) obj);
            }
        }, getOwnerComponent(userHandle));
        policy.validatePasswordOwner();
        updateMaximumTimeToLockLocked(userHandle);
        updateLockTaskPackagesLocked(policy.mLockTaskPackages, userHandle);
        updateLockTaskFeaturesLocked(policy.mLockTaskFeatures, userHandle);
        if (policy.mStatusBarDisabled) {
            setStatusBarDisabledInternal(policy.mStatusBarDisabled, userHandle);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$loadSettingsLocked$6$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ DeviceAdminInfo m3044x62bf57b7(int userHandle, ComponentName component) {
        return findAdmin(component, userHandle, false);
    }

    private void updateLockTaskPackagesLocked(List<String> packages, int userId) {
        String[] packagesArray = null;
        if (!packages.isEmpty()) {
            List<String> exemptApps = listPolicyExemptAppsUnchecked();
            if (!exemptApps.isEmpty()) {
                HashSet<String> updatedPackages = new HashSet<>(packages);
                updatedPackages.addAll(exemptApps);
                packagesArray = (String[]) updatedPackages.toArray(new String[updatedPackages.size()]);
            }
        }
        if (packagesArray == null) {
            packagesArray = (String[]) packages.toArray(new String[packages.size()]);
        }
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.getIActivityManager().updateLockTaskPackages(userId, packagesArray);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
            throw th;
        }
        this.mInjector.binderRestoreCallingIdentity(ident);
    }

    private void updateLockTaskFeaturesLocked(int flags, int userId) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.getIActivityTaskManager().updateLockTaskFeatures(userId, flags);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
            throw th;
        }
        this.mInjector.binderRestoreCallingIdentity(ident);
    }

    static void validateQualityConstant(int quality) {
        switch (quality) {
            case 0:
            case 32768:
            case 65536:
            case 131072:
            case 196608:
            case 262144:
            case 327680:
            case 393216:
            case 524288:
                return;
            default:
                throw new IllegalArgumentException("Invalid quality constant: 0x" + Integer.toHexString(quality));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void systemReady(int phase) {
        if (!this.mHasFeature) {
            return;
        }
        switch (phase) {
            case SystemService.PHASE_LOCK_SETTINGS_READY /* 480 */:
                onLockSettingsReady();
                loadAdminDataAsync();
                this.mOwners.systemReady();
                return;
            case SystemService.PHASE_ACTIVITY_MANAGER_READY /* 550 */:
                synchronized (getLockObject()) {
                    migrateToProfileOnOrganizationOwnedDeviceIfCompLocked();
                    applyProfileRestrictionsIfDeviceOwnerLocked();
                }
                maybeStartSecurityLogMonitorOnActivityManagerReady();
                return;
            case 1000:
                factoryResetIfDelayedEarlier();
                ensureDeviceOwnerUserStarted();
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePersonalAppsSuspensionOnUserStart(int userHandle) {
        int profileUserHandle = getManagedUserId(userHandle);
        if (profileUserHandle >= 0) {
            updatePersonalAppsSuspension(profileUserHandle, false);
        } else {
            suspendPersonalAppsInternal(userHandle, false);
        }
    }

    private void onLockSettingsReady() {
        List<String> packageList;
        synchronized (getLockObject()) {
            fixupAutoTimeRestrictionDuringOrganizationOwnedDeviceMigration();
        }
        m3013x8b75536b(0);
        cleanUpOldUsers();
        maybeSetDefaultProfileOwnerUserRestrictions();
        handleStartUser(0);
        maybeLogStart();
        this.mSetupContentObserver.register();
        updateUserSetupCompleteAndPaired();
        synchronized (getLockObject()) {
            packageList = getKeepUninstalledPackagesLocked();
        }
        if (packageList != null) {
            this.mInjector.getPackageManagerInternal().setKeepUninstalledPackages(packageList);
        }
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            if (deviceOwner != null) {
                this.mUserManagerInternal.setForceEphemeralUsers(deviceOwner.forceEphemeralUsers);
                ActivityManagerInternal activityManagerInternal = this.mInjector.getActivityManagerInternal();
                activityManagerInternal.setSwitchingFromSystemUserMessage(deviceOwner.startUserSessionMessage);
                activityManagerInternal.setSwitchingToSystemUserMessage(deviceOwner.endUserSessionMessage);
            }
            revertTransferOwnershipIfNecessaryLocked();
        }
        updateUsbDataSignal();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DpmsUpgradeDataProvider implements PolicyUpgraderDataProvider {
        private DpmsUpgradeDataProvider() {
        }

        @Override // com.android.server.devicepolicy.PolicyUpgraderDataProvider
        public boolean storageManagerIsFileBasedEncryptionEnabled() {
            return DevicePolicyManagerService.this.mInjector.storageManagerIsFileBasedEncryptionEnabled();
        }

        @Override // com.android.server.devicepolicy.PolicyUpgraderDataProvider
        public JournaledFile makeDevicePoliciesJournaledFile(int userId) {
            return DevicePolicyManagerService.this.makeJournaledFile(userId, DevicePolicyManagerService.DEVICE_POLICIES_XML);
        }

        @Override // com.android.server.devicepolicy.PolicyUpgraderDataProvider
        public JournaledFile makePoliciesVersionJournaledFile(int userId) {
            return DevicePolicyManagerService.this.makeJournaledFile(userId, DevicePolicyManagerService.POLICIES_VERSION_XML);
        }

        @Override // com.android.server.devicepolicy.PolicyUpgraderDataProvider
        public Function<ComponentName, DeviceAdminInfo> getAdminInfoSupplier(final int userId) {
            return new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$DpmsUpgradeDataProvider$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return DevicePolicyManagerService.DpmsUpgradeDataProvider.this.m3129x6085211e(userId, (ComponentName) obj);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getAdminInfoSupplier$0$com-android-server-devicepolicy-DevicePolicyManagerService$DpmsUpgradeDataProvider  reason: not valid java name */
        public /* synthetic */ DeviceAdminInfo m3129x6085211e(int userId, ComponentName component) {
            return DevicePolicyManagerService.this.findAdmin(component, userId, false);
        }

        @Override // com.android.server.devicepolicy.PolicyUpgraderDataProvider
        public int[] getUsersForUpgrade() {
            List<UserInfo> allUsers = DevicePolicyManagerService.this.mUserManager.getUsers();
            return allUsers.stream().mapToInt(new ToIntFunction() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$DpmsUpgradeDataProvider$$ExternalSyntheticLambda1
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    int i;
                    i = ((UserInfo) obj).id;
                    return i;
                }
            }).toArray();
        }
    }

    private void performPolicyVersionUpgrade() {
        PolicyVersionUpgrader upgrader = new PolicyVersionUpgrader(new DpmsUpgradeDataProvider(), this.mPathProvider);
        upgrader.upgradePolicy(3);
    }

    private void revertTransferOwnershipIfNecessaryLocked() {
        if (!this.mTransferOwnershipMetadataManager.metadataFileExists()) {
            return;
        }
        Slogf.e(LOG_TAG, "Owner transfer metadata file exists! Reverting transfer.");
        TransferOwnershipMetadataManager.Metadata metadata = this.mTransferOwnershipMetadataManager.loadMetadataFile();
        if (metadata.adminType.equals(LOG_TAG_PROFILE_OWNER)) {
            transferProfileOwnershipLocked(metadata.targetComponent, metadata.sourceComponent, metadata.userId);
            deleteTransferOwnershipMetadataFileLocked();
            deleteTransferOwnershipBundleLocked(metadata.userId);
        } else if (metadata.adminType.equals(LOG_TAG_DEVICE_OWNER)) {
            transferDeviceOwnershipLocked(metadata.targetComponent, metadata.sourceComponent, metadata.userId);
            deleteTransferOwnershipMetadataFileLocked();
            deleteTransferOwnershipBundleLocked(metadata.userId);
        }
        updateSystemUpdateFreezePeriodsRecord(true);
        pushUserControlDisabledPackagesLocked(metadata.userId);
    }

    private void maybeLogStart() {
        if (!SecurityLog.isLoggingEnabled()) {
            return;
        }
        String verifiedBootState = this.mInjector.systemPropertiesGet("ro.boot.verifiedbootstate");
        String verityMode = this.mInjector.systemPropertiesGet("ro.boot.veritymode");
        SecurityLog.writeEvent(210009, new Object[]{verifiedBootState, verityMode});
    }

    private void ensureDeviceOwnerUserStarted() {
        synchronized (getLockObject()) {
            if (this.mOwners.hasDeviceOwner()) {
                int userId = this.mOwners.getDeviceOwnerUserId();
                if (userId != 0) {
                    try {
                        this.mInjector.getIActivityManager().startUserInBackground(userId);
                    } catch (RemoteException e) {
                        Slogf.w(LOG_TAG, "Exception starting user", e);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void handleStartUser(int userId) {
        List<PreferentialNetworkServiceConfig> of;
        List<PreferentialNetworkServiceConfig> preferentialNetworkServiceConfigs;
        synchronized (getLockObject()) {
            pushScreenCapturePolicy(userId);
            pushUserControlDisabledPackagesLocked(userId);
        }
        pushUserRestrictions(userId);
        updatePasswordQualityCacheForUserGroup(userId == 0 ? -1 : userId);
        updatePermissionPolicyCache(userId);
        updateAdminCanGrantSensorsPermissionCache(userId);
        synchronized (getLockObject()) {
            ActiveAdmin owner = getDeviceOrProfileOwnerAdminLocked(userId);
            if (owner != null) {
                of = owner.mPreferentialNetworkServiceConfigs;
            } else {
                of = List.of(PreferentialNetworkServiceConfig.DEFAULT);
            }
            preferentialNetworkServiceConfigs = of;
        }
        updateNetworkPreferenceForUser(userId, preferentialNetworkServiceConfigs);
        startOwnerService(userId, "start-user");
    }

    void pushUserControlDisabledPackagesLocked(int userId) {
        ActiveAdmin owner;
        final int targetUserId;
        if (getDeviceOwnerUserIdUncheckedLocked() == userId) {
            owner = getDeviceOwnerAdminLocked();
            targetUserId = -1;
        } else {
            owner = getProfileOwnerAdminLocked(userId);
            targetUserId = userId;
        }
        final List<String> protectedPackages = (owner == null || owner.protectedPackages == null) ? Collections.emptyList() : owner.protectedPackages;
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda20
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3052xb82da0d8(targetUserId, protectedPackages);
            }
        });
        this.mUsageStatsManagerInternal.setAdminProtectedPackages(new ArraySet(protectedPackages), targetUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pushUserControlDisabledPackagesLocked$7$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3052xb82da0d8(int targetUserId, List protectedPackages) throws Exception {
        this.mInjector.getPackageManagerInternal().setOwnerProtectedPackages(targetUserId, protectedPackages);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void handleUnlockUser(int userId) {
        startOwnerService(userId, "unlock-user");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void handleOnUserUnlocked(int userId) {
        showNewUserDisclaimerIfNecessary(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void handleStopUser(int userId) {
        updateNetworkPreferenceForUser(userId, List.of(PreferentialNetworkServiceConfig.DEFAULT));
        stopOwnerService(userId, "stop-user");
    }

    private void startOwnerService(int userId, String actionForLog) {
        ComponentName owner = getOwnerComponent(userId);
        if (owner != null) {
            this.mDeviceAdminServiceController.startServiceForOwner(owner.getPackageName(), userId, actionForLog);
            invalidateBinderCaches();
        }
    }

    private void stopOwnerService(int userId, String actionForLog) {
        this.mDeviceAdminServiceController.stopServiceForOwner(userId, actionForLog);
    }

    private void cleanUpOldUsers() {
        Collection<? extends Integer> usersWithProfileOwners;
        ArraySet arraySet;
        synchronized (getLockObject()) {
            usersWithProfileOwners = this.mOwners.getProfileOwnerKeys();
            arraySet = new ArraySet();
            for (int i = 0; i < this.mUserData.size(); i++) {
                arraySet.add(Integer.valueOf(this.mUserData.keyAt(i)));
            }
        }
        List<UserInfo> allUsers = this.mUserManager.getUsers();
        Set<Integer> deletedUsers = new ArraySet<>();
        deletedUsers.addAll(usersWithProfileOwners);
        deletedUsers.addAll(arraySet);
        for (UserInfo userInfo : allUsers) {
            deletedUsers.remove(Integer.valueOf(userInfo.id));
        }
        for (Integer userId : deletedUsers) {
            removeUserData(userId.intValue());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePasswordExpirationNotification(int userHandle) {
        Bundle adminExtras = new Bundle();
        adminExtras.putParcelable("android.intent.extra.USER", UserHandle.of(userHandle));
        synchronized (getLockObject()) {
            long now = System.currentTimeMillis();
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin = admins.get(i);
                if (admin.info.usesPolicy(6) && admin.passwordExpirationTimeout > 0 && now >= admin.passwordExpirationDate - EXPIRATION_GRACE_PERIOD_MS && admin.passwordExpirationDate > 0) {
                    sendAdminCommandLocked(admin, "android.app.action.ACTION_PASSWORD_EXPIRING", adminExtras, (BroadcastReceiver) null);
                }
            }
            setExpirationAlarmCheckLocked(this.mContext, userHandle, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onInstalledCertificatesChanged(UserHandle userHandle, Collection<String> installedCertificates) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()));
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(userHandle.getIdentifier());
            boolean changed = false | policy.mAcceptedCaCertificates.retainAll(installedCertificates);
            if (changed | policy.mOwnerInstalledCaCerts.retainAll(installedCertificates)) {
                saveSettingsLocked(userHandle.getIdentifier());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Set<String> getAcceptedCaCertificates(UserHandle userHandle) {
        ArraySet<String> arraySet;
        if (!this.mHasFeature) {
            return Collections.emptySet();
        }
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(userHandle.getIdentifier());
            arraySet = policy.mAcceptedCaCertificates;
        }
        return arraySet;
    }

    public void setActiveAdmin(final ComponentName adminReceiver, final boolean refreshing, final int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS"));
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        final DevicePolicyData policy = m3013x8b75536b(userHandle);
        final DeviceAdminInfo info = findAdmin(adminReceiver, userHandle, true);
        synchronized (getLockObject()) {
            checkActiveAdminPrecondition(adminReceiver, info, policy);
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda85
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3067xf81c79e1(adminReceiver, userHandle, refreshing, info, policy);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setActiveAdmin$8$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3067xf81c79e1(ComponentName adminReceiver, int userHandle, boolean refreshing, DeviceAdminInfo info, DevicePolicyData policy) throws Exception {
        ActiveAdmin existingAdmin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
        if (!refreshing && existingAdmin != null) {
            throw new IllegalArgumentException("Admin is already added");
        }
        ActiveAdmin newAdmin = new ActiveAdmin(info, false);
        newAdmin.testOnlyAdmin = existingAdmin != null ? existingAdmin.testOnlyAdmin : isPackageTestOnly(adminReceiver.getPackageName(), userHandle);
        policy.mAdminMap.put(adminReceiver, newAdmin);
        int replaceIndex = -1;
        int N = policy.mAdminList.size();
        int i = 0;
        while (true) {
            if (i >= N) {
                break;
            }
            ActiveAdmin oldAdmin = policy.mAdminList.get(i);
            if (!oldAdmin.info.getComponent().equals(adminReceiver)) {
                i++;
            } else {
                replaceIndex = i;
                break;
            }
        }
        if (replaceIndex == -1) {
            policy.mAdminList.add(newAdmin);
            enableIfNecessary(info.getPackageName(), userHandle);
            this.mUsageStatsManagerInternal.onActiveAdminAdded(adminReceiver.getPackageName(), userHandle);
        } else {
            policy.mAdminList.set(replaceIndex, newAdmin);
        }
        saveSettingsLocked(userHandle);
        sendAdminCommandLocked(newAdmin, "android.app.action.DEVICE_ADMIN_ENABLED", (Bundle) null, (BroadcastReceiver) null);
    }

    private void loadAdminDataAsync() {
        this.mInjector.postOnSystemServerInitThreadPool(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                DevicePolicyManagerService.this.m3043x70888c3e();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$loadAdminDataAsync$9$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3043x70888c3e() {
        pushActiveAdminPackages();
        this.mUsageStatsManagerInternal.onAdminDataAvailable();
        pushAllMeteredRestrictedPackages();
        this.mInjector.getNetworkPolicyManagerInternal().onAdminDataAvailable();
    }

    private void pushActiveAdminPackages() {
        synchronized (getLockObject()) {
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int i = users.size() - 1; i >= 0; i--) {
                int userId = users.get(i).id;
                this.mUsageStatsManagerInternal.setActiveAdminApps(getActiveAdminPackagesLocked(userId), userId);
            }
        }
    }

    private void pushAllMeteredRestrictedPackages() {
        synchronized (getLockObject()) {
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int i = users.size() - 1; i >= 0; i--) {
                int userId = users.get(i).id;
                this.mInjector.getNetworkPolicyManagerInternal().setMeteredRestrictedPackagesAsync(getMeteredDisabledPackages(userId), userId);
            }
        }
    }

    private void pushActiveAdminPackagesLocked(int userId) {
        this.mUsageStatsManagerInternal.setActiveAdminApps(getActiveAdminPackagesLocked(userId), userId);
    }

    private Set<String> getActiveAdminPackagesLocked(int userId) {
        DevicePolicyData policy = m3013x8b75536b(userId);
        Set<String> adminPkgs = null;
        for (int i = policy.mAdminList.size() - 1; i >= 0; i--) {
            String pkgName = policy.mAdminList.get(i).info.getPackageName();
            if (adminPkgs == null) {
                adminPkgs = new ArraySet<>();
            }
            adminPkgs.add(pkgName);
        }
        return adminPkgs;
    }

    private void transferActiveAdminUncheckedLocked(ComponentName incomingReceiver, ComponentName outgoingReceiver, int userHandle) {
        DevicePolicyData policy = m3013x8b75536b(userHandle);
        if (!policy.mAdminMap.containsKey(outgoingReceiver) && policy.mAdminMap.containsKey(incomingReceiver)) {
            return;
        }
        DeviceAdminInfo incomingDeviceInfo = findAdmin(incomingReceiver, userHandle, true);
        ActiveAdmin adminToTransfer = policy.mAdminMap.get(outgoingReceiver);
        int oldAdminUid = adminToTransfer.getUid();
        adminToTransfer.transfer(incomingDeviceInfo);
        policy.mAdminMap.remove(outgoingReceiver);
        policy.mAdminMap.put(incomingReceiver, adminToTransfer);
        if (policy.mPasswordOwner == oldAdminUid) {
            policy.mPasswordOwner = adminToTransfer.getUid();
        }
        saveSettingsLocked(userHandle);
        sendAdminCommandLocked(adminToTransfer, "android.app.action.DEVICE_ADMIN_ENABLED", (Bundle) null, (BroadcastReceiver) null);
    }

    private void checkActiveAdminPrecondition(ComponentName adminReceiver, DeviceAdminInfo info, DevicePolicyData policy) {
        if (info == null) {
            throw new IllegalArgumentException("Bad admin: " + adminReceiver);
        }
        if (!info.getActivityInfo().applicationInfo.isInternal()) {
            throw new IllegalArgumentException("Only apps in internal storage can be active admin: " + adminReceiver);
        }
        if (info.getActivityInfo().applicationInfo.isInstantApp()) {
            throw new IllegalArgumentException("Instant apps cannot be device admins: " + adminReceiver);
        }
        if (policy.mRemovingAdmins.contains(adminReceiver)) {
            throw new IllegalArgumentException("Trying to set an admin which is being removed");
        }
    }

    private void checkAllUsersAreAffiliatedWithDevice() {
        Preconditions.checkCallAuthorization(areAllUsersAffiliatedWithDeviceLocked(), "operation not allowed when device has unaffiliated users");
    }

    public boolean isAdminActive(ComponentName adminReceiver, int userHandle) {
        boolean z;
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            synchronized (getLockObject()) {
                z = getActiveAdminUncheckedLocked(adminReceiver, userHandle) != null;
            }
            return z;
        }
        return false;
    }

    public boolean isRemovingAdmin(ComponentName adminReceiver, int userHandle) {
        boolean contains;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        synchronized (getLockObject()) {
            DevicePolicyData policyData = m3013x8b75536b(userHandle);
            contains = policyData.mRemovingAdmins.contains(adminReceiver);
        }
        return contains;
    }

    public boolean hasGrantedPolicy(ComponentName adminReceiver, int policyId, int userHandle) {
        boolean usesPolicy;
        boolean z = false;
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            Preconditions.checkCallAuthorization((isCallingFromPackage(adminReceiver.getPackageName(), caller.getUid()) || isSystemUid(caller)) ? true : true);
            synchronized (getLockObject()) {
                ActiveAdmin administrator = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
                if (administrator == null) {
                    throw new SecurityException("No active admin " + adminReceiver);
                }
                usesPolicy = administrator.info.usesPolicy(policyId);
            }
            return usesPolicy;
        }
        return false;
    }

    public List<ComponentName> getActiveAdmins(int userHandle) {
        if (!this.mHasFeature) {
            return Collections.EMPTY_LIST;
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(userHandle);
            int N = policy.mAdminList.size();
            if (N <= 0) {
                return null;
            }
            ArrayList<ComponentName> res = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                res.add(policy.mAdminList.get(i).info.getComponent());
            }
            return res;
        }
    }

    public boolean packageHasActiveAdmins(String packageName, int userHandle) {
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            synchronized (getLockObject()) {
                DevicePolicyData policy = m3013x8b75536b(userHandle);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    if (policy.mAdminList.get(i).info.getPackageName().equals(packageName)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    public void forceRemoveActiveAdmin(final ComponentName adminReceiver, final int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(adminReceiver, "ComponentName is null");
        Preconditions.checkCallAuthorization(isAdb(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"), "Caller must be shell or hold MANAGE_PROFILE_AND_DEVICE_OWNERS to call forceRemoveActiveAdmin");
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda131
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m2991xa747de4d(adminReceiver, userHandle);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$forceRemoveActiveAdmin$10$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m2991xa747de4d(ComponentName adminReceiver, int userHandle) throws Exception {
        boolean isOrgOwnedProfile = false;
        synchronized (getLockObject()) {
            if (!isAdminTestOnlyLocked(adminReceiver, userHandle)) {
                throw new SecurityException("Attempt to remove non-test admin " + adminReceiver + " " + userHandle);
            }
            if (isDeviceOwner(adminReceiver, userHandle)) {
                clearDeviceOwnerLocked(getDeviceOwnerAdminLocked(), userHandle);
            }
            if (isProfileOwner(adminReceiver, userHandle)) {
                isOrgOwnedProfile = isProfileOwnerOfOrganizationOwnedDevice(userHandle);
                ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle, false);
                clearProfileOwnerLocked(admin, userHandle);
            }
        }
        removeAdminArtifacts(adminReceiver, userHandle);
        if (isOrgOwnedProfile) {
            UserHandle parentUser = UserHandle.of(getProfileParentId(userHandle));
            m3125x2b6f2b2a(parentUser);
            clearOrgOwnedProfileOwnerDeviceWidePolicies(parentUser.getIdentifier());
        }
        Slogf.i(LOG_TAG, "Admin " + adminReceiver + " removed from user " + userHandle);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: clearOrgOwnedProfileOwnerUserRestrictions */
    public void m3125x2b6f2b2a(UserHandle parentUserHandle) {
        this.mUserManager.setUserRestriction("no_remove_managed_profile", false, parentUserHandle);
        this.mUserManager.setUserRestriction("no_add_user", false, parentUserHandle);
    }

    private void clearDeviceOwnerUserRestriction(UserHandle userHandle) {
        if (this.mUserManager.hasUserRestriction("no_add_user", userHandle)) {
            this.mUserManager.setUserRestriction("no_add_user", false, userHandle);
        }
        if (this.mUserManager.hasUserRestriction("no_add_managed_profile", userHandle)) {
            this.mUserManager.setUserRestriction("no_add_managed_profile", false, userHandle);
        }
        if (this.mUserManager.hasUserRestriction("no_add_clone_profile", userHandle)) {
            this.mUserManager.setUserRestriction("no_add_clone_profile", false, userHandle);
        }
    }

    private boolean isPackageTestOnly(String packageName, int userHandle) {
        try {
            ApplicationInfo ai = this.mInjector.getIPackageManager().getApplicationInfo(packageName, 786432L, userHandle);
            if (ai != null) {
                return (ai.flags & 256) != 0;
            }
            throw new IllegalStateException("Couldn't find package: " + packageName + " on user " + userHandle);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isAdminTestOnlyLocked(ComponentName who, int userHandle) {
        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
        return admin != null && admin.testOnlyAdmin;
    }

    public void removeActiveAdmin(final ComponentName adminReceiver, final int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = hasCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS") ? getCallerIdentity() : getCallerIdentity(adminReceiver);
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        checkCanExecuteOrThrowUnsafe(27);
        enforceUserUnlocked(userHandle);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
            if (admin == null) {
                return;
            }
            if (!isDeviceOwner(adminReceiver, userHandle) && !isProfileOwner(adminReceiver, userHandle)) {
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda10
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3056x58c4fc7d(adminReceiver, userHandle);
                    }
                });
                return;
            }
            Slogf.e(LOG_TAG, "Device/profile owner cannot be removed: component=" + adminReceiver);
        }
    }

    private boolean canSetPasswordQualityOnParent(String packageName, CallerIdentity caller) {
        return !this.mInjector.isChangeEnabled(PREVENT_SETTING_PASSWORD_QUALITY_ON_PARENT, packageName, caller.getUserId()) || isProfileOwnerOfOrganizationOwnedDevice(caller);
    }

    private boolean isPasswordLimitingAdminTargetingP(CallerIdentity caller) {
        boolean z;
        if (caller.hasAdminComponent()) {
            synchronized (getLockObject()) {
                z = getActiveAdminWithPolicyForUidLocked(caller.getComponentName(), 0, caller.getUid()) != null;
            }
            return z;
        }
        return false;
    }

    private boolean notSupportedOnAutomotive(String method) {
        if (this.mIsAutomotive) {
            Slogf.i(LOG_TAG, "%s is not supported on automotive builds", method);
            return true;
        }
        return false;
    }

    public void setPasswordQuality(final ComponentName who, final int quality, final boolean parent) {
        if (this.mHasFeature && !notSupportedOnAutomotive("setPasswordQuality")) {
            Objects.requireNonNull(who, "ComponentName is null");
            validateQualityConstant(quality);
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller) || isSystemUid(caller) || isPasswordLimitingAdminTargetingP(caller));
            if (parent) {
                Preconditions.checkCallAuthorization(canSetPasswordQualityOnParent(who.getPackageName(), caller), "Profile Owner may not apply password quality requirements device-wide");
            }
            final int userId = caller.getUserId();
            synchronized (getLockObject()) {
                try {
                    final ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
                    if (parent) {
                        ActiveAdmin primaryAdmin = getActiveAdminForCallerLocked(who, 0, false);
                        boolean hasComplexitySet = primaryAdmin.mPasswordComplexity != 0;
                        Preconditions.checkState(!hasComplexitySet, "Cannot set password quality when complexity is set on the primary admin. Set the primary admin's complexity to NONE first.");
                    }
                    this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda34
                        public final void runOrThrow() {
                            DevicePolicyManagerService.this.m3096x1ce6ed93(ap, quality, userId, parent, who);
                        }
                    });
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
            DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(1).setAdmin(who).setInt(quality);
            String[] strArr = new String[1];
            strArr[0] = parent ? CALLED_FROM_PARENT : NOT_CALLED_FROM_PARENT;
            devicePolicyEventLogger.setStrings(strArr).write();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setPasswordQuality$12$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3096x1ce6ed93(ActiveAdmin ap, int quality, int userId, boolean parent, ComponentName who) throws Exception {
        PasswordPolicy passwordPolicy = ap.mPasswordPolicy;
        if (passwordPolicy.quality != quality) {
            passwordPolicy.quality = quality;
            ap.mPasswordComplexity = 0;
            resetInactivePasswordRequirementsIfRPlus(userId, ap);
            updatePasswordValidityCheckpointLocked(userId, parent);
            updatePasswordQualityCacheForUserGroup(userId);
            saveSettingsLocked(userId);
        }
        logPasswordQualitySetIfSecurityLogEnabled(who, userId, parent, passwordPolicy);
    }

    private boolean passwordQualityInvocationOrderCheckEnabled(String packageName, int userId) {
        return this.mInjector.isChangeEnabled(ADMIN_APP_PASSWORD_COMPLEXITY, packageName, userId);
    }

    private void resetInactivePasswordRequirementsIfRPlus(int userId, ActiveAdmin admin) {
        if (passwordQualityInvocationOrderCheckEnabled(admin.info.getPackageName(), userId)) {
            PasswordPolicy policy = admin.mPasswordPolicy;
            if (policy.quality < 131072) {
                policy.length = 0;
            }
            if (policy.quality < 393216) {
                policy.letters = 1;
                policy.upperCase = 0;
                policy.lowerCase = 0;
                policy.numeric = 1;
                policy.symbols = 1;
                policy.nonLetter = 0;
            }
        }
    }

    private Set<Integer> updatePasswordValidityCheckpointLocked(int userHandle, boolean parent) {
        ArraySet<Integer> affectedUserIds = new ArraySet<>();
        int credentialOwner = getCredentialOwner(userHandle, parent);
        DevicePolicyData policy = m3013x8b75536b(credentialOwner);
        PasswordMetrics metrics = this.mLockSettingsInternal.getUserPasswordMetrics(credentialOwner);
        if (metrics != null) {
            int userToCheck = getProfileParentUserIfRequested(userHandle, parent);
            boolean newCheckpoint = isPasswordSufficientForUserWithoutCheckpointLocked(metrics, userToCheck);
            if (newCheckpoint != policy.mPasswordValidAtLastCheckpoint) {
                policy.mPasswordValidAtLastCheckpoint = newCheckpoint;
                affectedUserIds.add(Integer.valueOf(credentialOwner));
            }
        }
        return affectedUserIds;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePasswordQualityCacheForUserGroup(int userId) {
        List<UserInfo> users;
        if (userId == -1) {
            users = this.mUserManager.getUsers();
        } else {
            users = this.mUserManager.getProfiles(userId);
        }
        for (UserInfo userInfo : users) {
            int currentUserId = userInfo.id;
            this.mPolicyCache.setPasswordQuality(currentUserId, getPasswordQuality(null, currentUserId, false));
        }
    }

    public int getPasswordQuality(ComponentName who, int userHandle, boolean parent) {
        boolean z = false;
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            Preconditions.checkCallAuthorization((who == null || isCallingFromPackage(who.getPackageName(), caller.getUid()) || canQueryAdminPolicy(caller)) ? true : true);
            synchronized (getLockObject()) {
                int mode = 0;
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    return admin != null ? admin.mPasswordPolicy.quality : 0;
                }
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(getProfileParentUserIfRequested(userHandle, parent));
                int N = admins.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = admins.get(i);
                    if (mode < admin2.mPasswordPolicy.quality) {
                        mode = admin2.mPasswordPolicy.quality;
                    }
                }
                return mode;
            }
        }
        return 0;
    }

    private List<ActiveAdmin> getActiveAdminsForLockscreenPoliciesLocked(int userHandle) {
        if (isSeparateProfileChallengeEnabled(userHandle)) {
            return getUserDataUnchecked(userHandle).mAdminList;
        }
        return getActiveAdminsForUserAndItsManagedProfilesLocked(getProfileParentId(userHandle), new Predicate() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda147
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DevicePolicyManagerService.this.m2992x2785b22a((UserInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getActiveAdminsForLockscreenPoliciesLocked$13$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ boolean m2992x2785b22a(UserInfo user) {
        return !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(user.id);
    }

    private List<ActiveAdmin> getActiveAdminsForAffectedUserLocked(int userHandle) {
        if (isManagedProfile(userHandle)) {
            return getUserDataUnchecked(userHandle).mAdminList;
        }
        return getActiveAdminsForUserAndItsManagedProfilesLocked(userHandle, new Predicate() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda35
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DevicePolicyManagerService.lambda$getActiveAdminsForAffectedUserLocked$14((UserInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getActiveAdminsForAffectedUserLocked$14(UserInfo user) {
        return false;
    }

    private List<ActiveAdmin> getActiveAdminsForUserAndItsManagedProfilesLocked(final int userHandle, final Predicate<UserInfo> shouldIncludeProfileAdmins) {
        final ArrayList<ActiveAdmin> admins = new ArrayList<>();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda66
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m2993x5cf30922(userHandle, admins, shouldIncludeProfileAdmins);
            }
        });
        return admins;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getActiveAdminsForUserAndItsManagedProfilesLocked$15$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m2993x5cf30922(int userHandle, ArrayList admins, Predicate shouldIncludeProfileAdmins) throws Exception {
        for (UserInfo userInfo : this.mUserManager.getProfiles(userHandle)) {
            DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
            if (userInfo.id == userHandle) {
                admins.addAll(policy.mAdminList);
            } else if (userInfo.isManagedProfile()) {
                for (int i = 0; i < policy.mAdminList.size(); i++) {
                    ActiveAdmin admin = policy.mAdminList.get(i);
                    if (admin.hasParentActiveAdmin()) {
                        admins.add(admin.getParentActiveAdmin());
                    }
                    if (shouldIncludeProfileAdmins.test(userInfo)) {
                        admins.add(admin);
                    }
                }
            } else if (!userInfo.isDualProfile()) {
                Slogf.w(LOG_TAG, "Unknown user type: " + userInfo);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSeparateProfileChallengeEnabled(final int userHandle) {
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda62
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3039xd41093cd(userHandle);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isSeparateProfileChallengeEnabled$16$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3039xd41093cd(int userHandle) throws Exception {
        return Boolean.valueOf(this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userHandle));
    }

    public void setPasswordMinimumLength(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature || notSupportedOnAutomotive("setPasswordMinimumLength")) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            ensureMinimumQuality(userId, ap, 131072, "setPasswordMinimumLength");
            PasswordPolicy passwordPolicy = ap.mPasswordPolicy;
            if (passwordPolicy.length != length) {
                passwordPolicy.length = length;
                updatePasswordValidityCheckpointLocked(userId, parent);
                saveSettingsLocked(userId);
            }
            logPasswordQualitySetIfSecurityLogEnabled(who, userId, parent, passwordPolicy);
        }
        DevicePolicyEventLogger.createEvent(2).setAdmin(who).setInt(length).write();
    }

    private void ensureMinimumQuality(final int userId, final ActiveAdmin admin, final int minimumQuality, final String operation) {
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda110
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m2988xe3541e3b(admin, minimumQuality, userId, operation);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$ensureMinimumQuality$17$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m2988xe3541e3b(ActiveAdmin admin, int minimumQuality, int userId, String operation) throws Exception {
        if (admin.mPasswordPolicy.quality < minimumQuality && passwordQualityInvocationOrderCheckEnabled(admin.info.getPackageName(), userId)) {
            throw new IllegalStateException(String.format("password quality should be at least %d for %s", Integer.valueOf(minimumQuality), operation));
        }
    }

    public int getPasswordMinimumLength(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda89
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Integer valueOf;
                valueOf = Integer.valueOf(((ActiveAdmin) obj).mPasswordPolicy.length);
                return valueOf;
            }
        }, 131072);
    }

    public void setPasswordHistoryLength(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature || !this.mLockPatternUtils.hasSecureLockScreen()) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.passwordHistoryLength != length) {
                ap.passwordHistoryLength = length;
                updatePasswordValidityCheckpointLocked(userId, parent);
                saveSettingsLocked(userId);
            }
        }
        if (SecurityLog.isLoggingEnabled()) {
            int affectedUserId = parent ? getProfileParentId(userId) : userId;
            SecurityLog.writeEvent(210018, new Object[]{who.getPackageName(), Integer.valueOf(userId), Integer.valueOf(affectedUserId), Integer.valueOf(length)});
        }
    }

    public int getPasswordHistoryLength(ComponentName who, int userHandle, boolean parent) {
        if (!this.mLockPatternUtils.hasSecureLockScreen()) {
            return 0;
        }
        return getStrictestPasswordRequirement(who, userHandle, parent, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda137
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Integer valueOf;
                valueOf = Integer.valueOf(((ActiveAdmin) obj).passwordHistoryLength);
                return valueOf;
            }
        }, 0);
    }

    public void setPasswordExpirationTimeout(ComponentName who, long timeout, boolean parent) {
        if (!this.mHasFeature || !this.mLockPatternUtils.hasSecureLockScreen()) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        Preconditions.checkArgumentNonnegative(timeout, "Timeout must be >= 0 ms");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 6, parent);
            long expiration = timeout > 0 ? System.currentTimeMillis() + timeout : 0L;
            ap.passwordExpirationDate = expiration;
            ap.passwordExpirationTimeout = timeout;
            if (timeout > 0) {
                Slogf.w(LOG_TAG, "setPasswordExpiration(): password will expire on " + DateFormat.getDateTimeInstance(2, 2).format(new Date(expiration)));
            }
            saveSettingsLocked(userHandle);
            setExpirationAlarmCheckLocked(this.mContext, userHandle, parent);
        }
        if (SecurityLog.isLoggingEnabled()) {
            int affectedUserId = parent ? getProfileParentId(userHandle) : userHandle;
            SecurityLog.writeEvent(210016, new Object[]{who.getPackageName(), Integer.valueOf(userHandle), Integer.valueOf(affectedUserId), Long.valueOf(timeout)});
        }
    }

    public long getPasswordExpirationTimeout(ComponentName who, int userHandle, boolean parent) {
        if (this.mHasFeature && this.mLockPatternUtils.hasSecureLockScreen()) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            synchronized (getLockObject()) {
                long timeout = 0;
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    return admin != null ? admin.passwordExpirationTimeout : 0L;
                }
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(getProfileParentUserIfRequested(userHandle, parent));
                int N = admins.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = admins.get(i);
                    if (timeout == 0 || (admin2.passwordExpirationTimeout != 0 && timeout > admin2.passwordExpirationTimeout)) {
                        timeout = admin2.passwordExpirationTimeout;
                    }
                }
                return timeout;
            }
        }
        return 0L;
    }

    public boolean addCrossProfileWidgetProvider(ComponentName admin, String packageName) {
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isProfileOwner(caller));
        List<String> changedProviders = null;
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getProfileOwnerLocked(caller);
            if (activeAdmin.crossProfileWidgetProviders == null) {
                activeAdmin.crossProfileWidgetProviders = new ArrayList();
            }
            List<String> providers = activeAdmin.crossProfileWidgetProviders;
            if (!providers.contains(packageName)) {
                providers.add(packageName);
                changedProviders = new ArrayList<>(providers);
                saveSettingsLocked(caller.getUserId());
            }
        }
        DevicePolicyEventLogger.createEvent(49).setAdmin(admin).write();
        if (changedProviders != null) {
            this.mLocalService.notifyCrossProfileProvidersChanged(caller.getUserId(), changedProviders);
            return true;
        }
        return false;
    }

    public boolean removeCrossProfileWidgetProvider(ComponentName admin, String packageName) {
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isProfileOwner(caller));
        List<String> changedProviders = null;
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getProfileOwnerLocked(caller);
            if (activeAdmin.crossProfileWidgetProviders != null && !activeAdmin.crossProfileWidgetProviders.isEmpty()) {
                List<String> providers = activeAdmin.crossProfileWidgetProviders;
                if (providers.remove(packageName)) {
                    changedProviders = new ArrayList<>(providers);
                    saveSettingsLocked(caller.getUserId());
                }
                DevicePolicyEventLogger.createEvent(117).setAdmin(admin).write();
                if (changedProviders != null) {
                    this.mLocalService.notifyCrossProfileProvidersChanged(caller.getUserId(), changedProviders);
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    public List<String> getCrossProfileWidgetProviders(ComponentName admin) {
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isProfileOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getProfileOwnerLocked(caller);
            if (activeAdmin.crossProfileWidgetProviders != null && !activeAdmin.crossProfileWidgetProviders.isEmpty()) {
                if (this.mInjector.binderIsCallingUidMyUid()) {
                    return new ArrayList(activeAdmin.crossProfileWidgetProviders);
                }
                return activeAdmin.crossProfileWidgetProviders;
            }
            return null;
        }
    }

    private long getPasswordExpirationLocked(ComponentName who, int userHandle, boolean parent) {
        long timeout = 0;
        if (who != null) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
            if (admin != null) {
                return admin.passwordExpirationDate;
            }
            return 0L;
        }
        List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(getProfileParentUserIfRequested(userHandle, parent));
        int N = admins.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin admin2 = admins.get(i);
            if (timeout == 0 || (admin2.passwordExpirationDate != 0 && timeout > admin2.passwordExpirationDate)) {
                timeout = admin2.passwordExpirationDate;
            }
        }
        return timeout;
    }

    public long getPasswordExpiration(ComponentName who, int userHandle, boolean parent) {
        long passwordExpirationLocked;
        if (!this.mHasFeature || !this.mLockPatternUtils.hasSecureLockScreen()) {
            return 0L;
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        synchronized (getLockObject()) {
            passwordExpirationLocked = getPasswordExpirationLocked(who, userHandle, parent);
        }
        return passwordExpirationLocked;
    }

    public void setPasswordMinimumUpperCase(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature || notSupportedOnAutomotive("setPasswordMinimumUpperCase")) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            ensureMinimumQuality(userId, ap, 393216, "setPasswordMinimumUpperCase");
            PasswordPolicy passwordPolicy = ap.mPasswordPolicy;
            if (passwordPolicy.upperCase != length) {
                passwordPolicy.upperCase = length;
                updatePasswordValidityCheckpointLocked(userId, parent);
                saveSettingsLocked(userId);
            }
            logPasswordQualitySetIfSecurityLogEnabled(who, userId, parent, passwordPolicy);
        }
        DevicePolicyEventLogger.createEvent(7).setAdmin(who).setInt(length).write();
    }

    public int getPasswordMinimumUpperCase(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda73
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Integer valueOf;
                valueOf = Integer.valueOf(((ActiveAdmin) obj).mPasswordPolicy.upperCase);
                return valueOf;
            }
        }, 393216);
    }

    public void setPasswordMinimumLowerCase(ComponentName who, int length, boolean parent) {
        if (notSupportedOnAutomotive("setPasswordMinimumLowerCase")) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            ensureMinimumQuality(userId, ap, 393216, "setPasswordMinimumLowerCase");
            PasswordPolicy passwordPolicy = ap.mPasswordPolicy;
            if (passwordPolicy.lowerCase != length) {
                passwordPolicy.lowerCase = length;
                updatePasswordValidityCheckpointLocked(userId, parent);
                saveSettingsLocked(userId);
            }
            logPasswordQualitySetIfSecurityLogEnabled(who, userId, parent, passwordPolicy);
        }
        DevicePolicyEventLogger.createEvent(6).setAdmin(who).setInt(length).write();
    }

    public int getPasswordMinimumLowerCase(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda11
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Integer valueOf;
                valueOf = Integer.valueOf(((ActiveAdmin) obj).mPasswordPolicy.lowerCase);
                return valueOf;
            }
        }, 393216);
    }

    public void setPasswordMinimumLetters(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature || notSupportedOnAutomotive("setPasswordMinimumLetters")) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            ensureMinimumQuality(userId, ap, 393216, "setPasswordMinimumLetters");
            PasswordPolicy passwordPolicy = ap.mPasswordPolicy;
            if (passwordPolicy.letters != length) {
                passwordPolicy.letters = length;
                updatePasswordValidityCheckpointLocked(userId, parent);
                saveSettingsLocked(userId);
            }
            logPasswordQualitySetIfSecurityLogEnabled(who, userId, parent, passwordPolicy);
        }
        DevicePolicyEventLogger.createEvent(5).setAdmin(who).setInt(length).write();
    }

    public int getPasswordMinimumLetters(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda81
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Integer valueOf;
                valueOf = Integer.valueOf(((ActiveAdmin) obj).mPasswordPolicy.letters);
                return valueOf;
            }
        }, 393216);
    }

    public void setPasswordMinimumNumeric(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature || notSupportedOnAutomotive("setPasswordMinimumNumeric")) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            ensureMinimumQuality(userId, ap, 393216, "setPasswordMinimumNumeric");
            PasswordPolicy passwordPolicy = ap.mPasswordPolicy;
            if (passwordPolicy.numeric != length) {
                passwordPolicy.numeric = length;
                updatePasswordValidityCheckpointLocked(userId, parent);
                saveSettingsLocked(userId);
            }
            logPasswordQualitySetIfSecurityLogEnabled(who, userId, parent, passwordPolicy);
        }
        DevicePolicyEventLogger.createEvent(3).setAdmin(who).setInt(length).write();
    }

    public int getPasswordMinimumNumeric(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda153
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Integer valueOf;
                valueOf = Integer.valueOf(((ActiveAdmin) obj).mPasswordPolicy.numeric);
                return valueOf;
            }
        }, 393216);
    }

    public void setPasswordMinimumSymbols(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature || notSupportedOnAutomotive("setPasswordMinimumSymbols")) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            ensureMinimumQuality(userId, ap, 393216, "setPasswordMinimumSymbols");
            PasswordPolicy passwordPolicy = ap.mPasswordPolicy;
            if (passwordPolicy.symbols != length) {
                ap.mPasswordPolicy.symbols = length;
                updatePasswordValidityCheckpointLocked(userId, parent);
                saveSettingsLocked(userId);
            }
            logPasswordQualitySetIfSecurityLogEnabled(who, userId, parent, passwordPolicy);
        }
        DevicePolicyEventLogger.createEvent(8).setAdmin(who).setInt(length).write();
    }

    public int getPasswordMinimumSymbols(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda158
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Integer valueOf;
                valueOf = Integer.valueOf(((ActiveAdmin) obj).mPasswordPolicy.symbols);
                return valueOf;
            }
        }, 393216);
    }

    public void setPasswordMinimumNonLetter(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature || notSupportedOnAutomotive("setPasswordMinimumNonLetter")) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            ensureMinimumQuality(userId, ap, 393216, "setPasswordMinimumNonLetter");
            PasswordPolicy passwordPolicy = ap.mPasswordPolicy;
            if (passwordPolicy.nonLetter != length) {
                ap.mPasswordPolicy.nonLetter = length;
                updatePasswordValidityCheckpointLocked(userId, parent);
                saveSettingsLocked(userId);
            }
            logPasswordQualitySetIfSecurityLogEnabled(who, userId, parent, passwordPolicy);
        }
        DevicePolicyEventLogger.createEvent(4).setAdmin(who).setInt(length).write();
    }

    public int getPasswordMinimumNonLetter(ComponentName who, int userHandle, boolean parent) {
        return getStrictestPasswordRequirement(who, userHandle, parent, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda167
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Integer valueOf;
                valueOf = Integer.valueOf(((ActiveAdmin) obj).mPasswordPolicy.nonLetter);
                return valueOf;
            }
        }, 393216);
    }

    private int getStrictestPasswordRequirement(ComponentName who, int userHandle, boolean parent, Function<ActiveAdmin, Integer> getter, int minimumPasswordQuality) {
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            synchronized (getLockObject()) {
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    return admin != null ? getter.apply(admin).intValue() : 0;
                }
                int maxValue = 0;
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(getProfileParentUserIfRequested(userHandle, parent));
                int N = admins.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = admins.get(i);
                    if (isLimitPasswordAllowed(admin2, minimumPasswordQuality)) {
                        Integer adminValue = getter.apply(admin2);
                        if (adminValue.intValue() > maxValue) {
                            maxValue = adminValue.intValue();
                        }
                    }
                }
                return maxValue;
            }
        }
        return 0;
    }

    public PasswordMetrics getPasswordMinimumMetrics(int userHandle, boolean deviceWideOnly) {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle) && (isSystemUid(caller) || hasCallingOrSelfPermission("android.permission.SET_INITIAL_LOCK")));
        return getPasswordMinimumMetricsUnchecked(userHandle, deviceWideOnly);
    }

    private PasswordMetrics getPasswordMinimumMetricsUnchecked(int userId) {
        return getPasswordMinimumMetricsUnchecked(userId, false);
    }

    private PasswordMetrics getPasswordMinimumMetricsUnchecked(int userId, boolean deviceWideOnly) {
        List<ActiveAdmin> admins;
        if (!this.mHasFeature) {
            new PasswordMetrics(-1);
        }
        Preconditions.checkArgumentNonnegative(userId, "Invalid userId");
        if (deviceWideOnly) {
            Preconditions.checkArgument(!isManagedProfile(userId));
        }
        ArrayList<PasswordMetrics> adminMetrics = new ArrayList<>();
        synchronized (getLockObject()) {
            if (deviceWideOnly) {
                admins = getActiveAdminsForUserAndItsManagedProfilesLocked(userId, new Predicate() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda4
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return DevicePolicyManagerService.lambda$getPasswordMinimumMetricsUnchecked$26((UserInfo) obj);
                    }
                });
            } else {
                admins = getActiveAdminsForLockscreenPoliciesLocked(userId);
            }
            for (ActiveAdmin admin : admins) {
                adminMetrics.add(admin.mPasswordPolicy.getMinMetrics());
            }
        }
        return PasswordMetrics.merge(adminMetrics);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getPasswordMinimumMetricsUnchecked$26(UserInfo user) {
        return false;
    }

    public boolean isActivePasswordSufficient(int userHandle, boolean parent) {
        boolean activePasswordSufficientForUserLocked;
        if (!this.mHasFeature) {
            return true;
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        enforceUserUnlocked(userHandle, parent);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(null, 0, parent);
            int credentialOwner = getCredentialOwner(userHandle, parent);
            DevicePolicyData policy = getUserDataUnchecked(credentialOwner);
            PasswordMetrics metrics = this.mLockSettingsInternal.getUserPasswordMetrics(credentialOwner);
            int userToCheck = getProfileParentUserIfRequested(userHandle, parent);
            activePasswordSufficientForUserLocked = isActivePasswordSufficientForUserLocked(policy.mPasswordValidAtLastCheckpoint, metrics, userToCheck);
        }
        return activePasswordSufficientForUserLocked;
    }

    public boolean isActivePasswordSufficientForDeviceRequirement() {
        boolean isSufficient;
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(isProfileOwner(caller));
            int profileUserId = caller.getUserId();
            Preconditions.checkCallingUser(isManagedProfile(profileUserId));
            int parentUser = getProfileParentId(profileUserId);
            enforceUserUnlocked(parentUser);
            synchronized (getLockObject()) {
                int complexity = getAggregatedPasswordComplexityLocked(parentUser, true);
                PasswordMetrics minMetrics = getPasswordMinimumMetricsUnchecked(parentUser, true);
                PasswordMetrics metrics = this.mLockSettingsInternal.getUserPasswordMetrics(parentUser);
                List<PasswordValidationError> passwordValidationErrors = PasswordMetrics.validatePasswordMetrics(minMetrics, complexity, metrics);
                isSufficient = passwordValidationErrors.isEmpty();
            }
            DevicePolicyEventLogger.createEvent(189).setStrings(new String[]{this.mOwners.getProfileOwnerComponent(caller.getUserId()).getPackageName()}).write();
            return isSufficient;
        }
        return true;
    }

    public boolean isUsingUnifiedPassword(ComponentName admin) {
        if (this.mHasFeature) {
            Objects.requireNonNull(admin, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(admin);
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
            Preconditions.checkCallingUser(isManagedProfile(caller.getUserId()));
            return true ^ isSeparateProfileChallengeEnabled(caller.getUserId());
        }
        return true;
    }

    public boolean isPasswordSufficientAfterProfileUnification(int userHandle, final int profileUser) {
        boolean isEmpty;
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            Preconditions.checkCallAuthorization(!isManagedProfile(userHandle), "You can not check password sufficiency for a managed profile, userId = %d", new Object[]{Integer.valueOf(userHandle)});
            enforceUserUnlocked(userHandle);
            synchronized (getLockObject()) {
                PasswordMetrics metrics = this.mLockSettingsInternal.getUserPasswordMetrics(userHandle);
                List<ActiveAdmin> admins = getActiveAdminsForUserAndItsManagedProfilesLocked(userHandle, new Predicate() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda157
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return DevicePolicyManagerService.this.m3035x15bc5bc8(profileUser, (UserInfo) obj);
                    }
                });
                ArrayList<PasswordMetrics> adminMetrics = new ArrayList<>(admins.size());
                int maxRequiredComplexity = 0;
                for (ActiveAdmin admin : admins) {
                    adminMetrics.add(admin.mPasswordPolicy.getMinMetrics());
                    maxRequiredComplexity = Math.max(maxRequiredComplexity, admin.mPasswordComplexity);
                }
                isEmpty = PasswordMetrics.validatePasswordMetrics(PasswordMetrics.merge(adminMetrics), maxRequiredComplexity, metrics).isEmpty();
            }
            return isEmpty;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isPasswordSufficientAfterProfileUnification$27$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ boolean m3035x15bc5bc8(int profileUser, UserInfo user) {
        return user.id == profileUser || !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(user.id);
    }

    private boolean isActivePasswordSufficientForUserLocked(boolean passwordValidAtLastCheckpoint, PasswordMetrics metrics, int userHandle) {
        if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled() && metrics == null) {
            return passwordValidAtLastCheckpoint;
        }
        if (metrics == null) {
            throw new IllegalStateException("isActivePasswordSufficient called on FBE-locked user");
        }
        return isPasswordSufficientForUserWithoutCheckpointLocked(metrics, userHandle);
    }

    private boolean isPasswordSufficientForUserWithoutCheckpointLocked(PasswordMetrics metrics, int userId) {
        int complexity = getAggregatedPasswordComplexityLocked(userId);
        PasswordMetrics minMetrics = getPasswordMinimumMetricsUnchecked(userId);
        List<PasswordValidationError> passwordValidationErrors = PasswordMetrics.validatePasswordMetrics(minMetrics, complexity, metrics);
        return passwordValidationErrors.isEmpty();
    }

    public int getPasswordComplexity(boolean parent) {
        CallerIdentity caller = getCallerIdentity();
        DevicePolicyEventLogger.createEvent(72).setStrings(parent ? CALLED_FROM_PARENT : NOT_CALLED_FROM_PARENT, this.mInjector.getPackageManager().getPackagesForUid(caller.getUid())).write();
        enforceUserUnlocked(caller.getUserId());
        boolean z = true;
        int i = 0;
        if (parent) {
            if (!isDefaultDeviceOwner(caller) && !isProfileOwner(caller) && !isSystemUid(caller)) {
                z = false;
            }
            Preconditions.checkCallAuthorization(z, "Only profile owner, device owner and system may call this method on parent.");
        } else {
            if (!hasCallingOrSelfPermission("android.permission.REQUEST_PASSWORD_COMPLEXITY") && !isDefaultDeviceOwner(caller) && !isProfileOwner(caller)) {
                z = false;
            }
            Preconditions.checkCallAuthorization(z, "Must have android.permission.REQUEST_PASSWORD_COMPLEXITY permission, or be a profile owner or device owner.");
        }
        synchronized (getLockObject()) {
            int credentialOwner = getCredentialOwner(caller.getUserId(), parent);
            PasswordMetrics metrics = this.mLockSettingsInternal.getUserPasswordMetrics(credentialOwner);
            if (metrics != null) {
                i = metrics.determineComplexity();
            }
        }
        return i;
    }

    public void setRequiredPasswordComplexity(final int passwordComplexity, final boolean calledOnParent) {
        if (!this.mHasFeature) {
            return;
        }
        Set<Integer> allowedModes = Set.of(0, 65536, 196608, 327680);
        Preconditions.checkArgument(allowedModes.contains(Integer.valueOf(passwordComplexity)), "Provided complexity is not one of the allowed values.");
        final CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        Preconditions.checkArgument(!calledOnParent || isProfileOwner(caller));
        synchronized (getLockObject()) {
            final ActiveAdmin admin = getParentOfAdminIfRequired(getProfileOwnerOrDeviceOwnerLocked(caller), calledOnParent);
            if (admin.mPasswordComplexity != passwordComplexity) {
                if (!calledOnParent) {
                    boolean hasQualityRequirementsOnParent = admin.hasParentActiveAdmin() && admin.getParentActiveAdmin().mPasswordPolicy.quality != 0;
                    Preconditions.checkState(hasQualityRequirementsOnParent ? false : true, "Password quality is set on the parent when attempting to set passwordcomplexity. Clear the quality by setting the password quality on the parent to PASSWORD_QUALITY_UNSPECIFIED first");
                }
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda63
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3103x6ec1b8c(admin, passwordComplexity, caller, calledOnParent);
                    }
                });
                DevicePolicyEventLogger.createEvent(177).setAdmin(admin.info.getPackageName()).setInt(passwordComplexity).setBoolean(calledOnParent).write();
            }
            logPasswordComplexityRequiredIfSecurityLogEnabled(admin.info.getComponent(), caller.getUserId(), calledOnParent, passwordComplexity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setRequiredPasswordComplexity$28$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3103x6ec1b8c(ActiveAdmin admin, int passwordComplexity, CallerIdentity caller, boolean calledOnParent) throws Exception {
        admin.mPasswordComplexity = passwordComplexity;
        admin.mPasswordPolicy = new PasswordPolicy();
        updatePasswordValidityCheckpointLocked(caller.getUserId(), calledOnParent);
        updatePasswordQualityCacheForUserGroup(caller.getUserId());
        saveSettingsLocked(caller.getUserId());
    }

    private void logPasswordComplexityRequiredIfSecurityLogEnabled(ComponentName who, int userId, boolean parent, int complexity) {
        if (SecurityLog.isLoggingEnabled()) {
            int affectedUserId = parent ? getProfileParentId(userId) : userId;
            SecurityLog.writeEvent(210035, new Object[]{who.getPackageName(), Integer.valueOf(userId), Integer.valueOf(affectedUserId), Integer.valueOf(complexity)});
        }
    }

    private int getAggregatedPasswordComplexityLocked(int userHandle) {
        return getAggregatedPasswordComplexityLocked(userHandle, false);
    }

    private int getAggregatedPasswordComplexityLocked(int userHandle, boolean deviceWideOnly) {
        List<ActiveAdmin> admins;
        ensureLocked();
        if (deviceWideOnly) {
            admins = getActiveAdminsForUserAndItsManagedProfilesLocked(userHandle, new Predicate() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda156
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return DevicePolicyManagerService.lambda$getAggregatedPasswordComplexityLocked$29((UserInfo) obj);
                }
            });
        } else {
            admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle);
        }
        int maxRequiredComplexity = 0;
        for (ActiveAdmin admin : admins) {
            maxRequiredComplexity = Math.max(maxRequiredComplexity, admin.mPasswordComplexity);
        }
        return maxRequiredComplexity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getAggregatedPasswordComplexityLocked$29(UserInfo user) {
        return false;
    }

    public int getRequiredPasswordComplexity(boolean calledOnParent) {
        int i;
        boolean z = false;
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
            if (!calledOnParent || isProfileOwner(caller)) {
                z = true;
            }
            Preconditions.checkArgument(z);
            synchronized (getLockObject()) {
                ActiveAdmin requiredAdmin = getParentOfAdminIfRequired(getDeviceOrProfileOwnerAdminLocked(caller.getUserId()), calledOnParent);
                i = requiredAdmin.mPasswordComplexity;
            }
            return i;
        }
        return 0;
    }

    public int getAggregatedPasswordComplexityForUser(int userId, boolean deviceWideOnly) {
        int aggregatedPasswordComplexityLocked;
        if (!this.mHasFeature) {
            return 0;
        }
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userId));
        synchronized (getLockObject()) {
            aggregatedPasswordComplexityLocked = getAggregatedPasswordComplexityLocked(userId, deviceWideOnly);
        }
        return aggregatedPasswordComplexityLocked;
    }

    public int getCurrentFailedPasswordAttempts(int userHandle, boolean parent) {
        int i;
        if (!this.mLockPatternUtils.hasSecureLockScreen()) {
            return 0;
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        synchronized (getLockObject()) {
            if (!isSystemUid(caller) && !hasCallingPermission("android.permission.ACCESS_KEYGUARD_SECURE_STORAGE")) {
                getActiveAdminForCallerLocked(null, 1, parent);
            }
            DevicePolicyData policy = getUserDataUnchecked(getCredentialOwner(userHandle, parent));
            i = policy.mFailedPasswordAttempts;
        }
        return i;
    }

    public void setMaximumFailedPasswordsForWipe(ComponentName who, int num, boolean parent) {
        if (!this.mHasFeature || !this.mLockPatternUtils.hasSecureLockScreen()) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(who, 4, parent);
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 1, parent);
            if (ap.maximumFailedPasswordsForWipe != num) {
                ap.maximumFailedPasswordsForWipe = num;
                saveSettingsLocked(userId);
            }
        }
        if (SecurityLog.isLoggingEnabled()) {
            int affectedUserId = parent ? getProfileParentId(userId) : userId;
            SecurityLog.writeEvent(210020, new Object[]{who.getPackageName(), Integer.valueOf(userId), Integer.valueOf(affectedUserId), Integer.valueOf(num)});
        }
    }

    public int getMaximumFailedPasswordsForWipe(ComponentName who, int userHandle, boolean parent) {
        ActiveAdmin admin;
        int i;
        if (this.mHasFeature && this.mLockPatternUtils.hasSecureLockScreen()) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            Preconditions.checkCallAuthorization(who == null || isCallingFromPackage(who.getPackageName(), caller.getUid()) || canQueryAdminPolicy(caller));
            synchronized (getLockObject()) {
                if (who != null) {
                    admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                } else {
                    admin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle, parent);
                }
                i = admin != null ? admin.maximumFailedPasswordsForWipe : 0;
            }
            return i;
        }
        return 0;
    }

    public int getProfileWithMinimumFailedPasswordsForWipe(int userHandle, boolean parent) {
        int userIdToWipeForFailedPasswords;
        if (this.mHasFeature && this.mLockPatternUtils.hasSecureLockScreen()) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            synchronized (getLockObject()) {
                ActiveAdmin admin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle, parent);
                userIdToWipeForFailedPasswords = admin != null ? getUserIdToWipeForFailedPasswords(admin) : -10000;
            }
            return userIdToWipeForFailedPasswords;
        }
        return -10000;
    }

    private ActiveAdmin getAdminWithMinimumFailedPasswordsForWipeLocked(int userHandle, boolean parent) {
        int count = 0;
        ActiveAdmin strictestAdmin = null;
        List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(getProfileParentUserIfRequested(userHandle, parent));
        int N = admins.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin admin = admins.get(i);
            if (admin.maximumFailedPasswordsForWipe != 0) {
                int userId = getUserIdToWipeForFailedPasswords(admin);
                if (count == 0 || count > admin.maximumFailedPasswordsForWipe || (count == admin.maximumFailedPasswordsForWipe && getUserInfo(userId).isPrimary())) {
                    count = admin.maximumFailedPasswordsForWipe;
                    strictestAdmin = admin;
                }
            }
        }
        return strictestAdmin;
    }

    private UserInfo getUserInfo(final int userId) {
        return (UserInfo) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda151
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3014x7ba30aaa(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getUserInfo$30$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ UserInfo m3014x7ba30aaa(int userId) throws Exception {
        return this.mUserManager.getUserInfo(userId);
    }

    private boolean setPasswordPrivileged(String password, int flags, CallerIdentity caller) {
        if (isLockScreenSecureUnchecked(caller.getUserId())) {
            throw new SecurityException("Cannot change current password");
        }
        return resetPasswordInternal(password, 0L, null, flags, caller);
    }

    public boolean resetPassword(String password, int flags) throws RemoteException {
        if (!this.mLockPatternUtils.hasSecureLockScreen()) {
            Slogf.w(LOG_TAG, "Cannot reset password when the device has no lock screen");
            return false;
        }
        if (password == null) {
            password = "";
        }
        CallerIdentity caller = getCallerIdentity();
        int userHandle = caller.getUserId();
        if (hasCallingPermission("android.permission.RESET_PASSWORD")) {
            boolean result = setPasswordPrivileged(password, flags, caller);
            if (result) {
                DevicePolicyEventLogger.createEvent(205).write();
            }
            return result;
        } else if (isDefaultDeviceOwner(caller) || isProfileOwner(caller)) {
            synchronized (getLockObject()) {
                if (getTargetSdk(getProfileOwnerOrDeviceOwnerLocked(caller).info.getPackageName(), userHandle) < 26) {
                    Slogf.e(LOG_TAG, "DPC can no longer call resetPassword()");
                } else {
                    throw new SecurityException("Device admin can no longer call resetPassword()");
                }
            }
            return false;
        } else {
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminForCallerLocked(null, 2, false);
                Preconditions.checkCallAuthorization(admin != null, "Unauthorized caller cannot call resetPassword.");
                if (getTargetSdk(admin.info.getPackageName(), userHandle) <= 23) {
                    Slogf.e(LOG_TAG, "Device admin can no longer call resetPassword()");
                } else {
                    throw new SecurityException("Device admin can no longer call resetPassword()");
                }
            }
            return false;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5115=5] */
    /* JADX WARN: Removed duplicated region for block: B:56:0x00d8  */
    /* JADX WARN: Removed duplicated region for block: B:59:0x00dc A[Catch: all -> 0x0103, TRY_ENTER, TryCatch #2 {all -> 0x0103, blocks: (B:50:0x00c7, B:59:0x00dc, B:61:0x00e5, B:62:0x00e9, B:66:0x00ee, B:68:0x00f2, B:69:0x00f7), top: B:89:0x00c7 }] */
    /* JADX WARN: Removed duplicated region for block: B:60:0x00e4  */
    /* JADX WARN: Removed duplicated region for block: B:63:0x00ea  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean resetPasswordInternal(String password, long tokenHandle, byte[] token, int flags, CallerIdentity caller) {
        LockscreenCredential newCredential;
        LockscreenCredential newCredential2;
        long ident;
        Throwable th;
        boolean requireEntry;
        int i;
        int callingUid = caller.getUid();
        int userHandle = UserHandle.getUserId(callingUid);
        boolean isPin = PasswordMetrics.isNumericOnly(password);
        synchronized (getLockObject()) {
            try {
                PasswordMetrics minMetrics = getPasswordMinimumMetricsUnchecked(userHandle);
                int complexity = getAggregatedPasswordComplexityLocked(userHandle);
                List<PasswordValidationError> validationErrors = password.isEmpty() ? PasswordMetrics.validatePasswordMetrics(minMetrics, complexity, new PasswordMetrics(-1)) : PasswordMetrics.validatePassword(minMetrics, complexity, isPin, password.getBytes());
                if (!validationErrors.isEmpty()) {
                    Slogf.w(LOG_TAG, "Failed to reset password due to constraint violation: %s", validationErrors.get(0));
                    return false;
                }
                DevicePolicyData policy = m3013x8b75536b(userHandle);
                if (policy.mPasswordOwner >= 0 && policy.mPasswordOwner != callingUid) {
                    Slogf.w(LOG_TAG, "resetPassword: already set by another uid and not entered by user");
                    return false;
                }
                boolean callerIsDeviceOwnerAdmin = isDefaultDeviceOwner(caller);
                boolean doNotAskCredentialsOnBoot = (flags & 2) != 0;
                if (callerIsDeviceOwnerAdmin && doNotAskCredentialsOnBoot) {
                    setDoNotAskCredentialsOnBoot();
                }
                long ident2 = this.mInjector.binderClearCallingIdentity();
                if (isPin) {
                    newCredential = LockscreenCredential.createPin(password);
                } else {
                    LockscreenCredential newCredential3 = LockscreenCredential.createPasswordOrNone(password);
                    newCredential = newCredential3;
                }
                try {
                    try {
                        if (tokenHandle == 0) {
                            newCredential2 = newCredential;
                            ident = ident2;
                        } else if (token == null) {
                            newCredential2 = newCredential;
                            ident = ident2;
                        } else {
                            try {
                                ident = ident2;
                                try {
                                    if (!this.mLockPatternUtils.setLockCredentialWithToken(newCredential, tokenHandle, token, userHandle)) {
                                        this.mInjector.binderRestoreCallingIdentity(ident);
                                        return false;
                                    }
                                    requireEntry = (flags & 1) != 0;
                                    if (requireEntry) {
                                        i = -1;
                                    } else {
                                        i = -1;
                                        this.mLockPatternUtils.requireStrongAuth(2, -1);
                                    }
                                    synchronized (getLockObject()) {
                                        if (requireEntry) {
                                            i = callingUid;
                                        }
                                        int newOwner = i;
                                        if (policy.mPasswordOwner != newOwner) {
                                            policy.mPasswordOwner = newOwner;
                                            saveSettingsLocked(userHandle);
                                        }
                                    }
                                    this.mInjector.binderRestoreCallingIdentity(ident);
                                    return true;
                                } catch (Throwable th2) {
                                    th = th2;
                                    this.mInjector.binderRestoreCallingIdentity(ident);
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                ident = ident2;
                            }
                        }
                        if (!this.mLockPatternUtils.setLockCredential(newCredential2, LockscreenCredential.createNone(), userHandle)) {
                            this.mInjector.binderRestoreCallingIdentity(ident);
                            return false;
                        }
                        requireEntry = (flags & 1) != 0;
                        if (requireEntry) {
                        }
                        synchronized (getLockObject()) {
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        this.mInjector.binderRestoreCallingIdentity(ident);
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    this.mInjector.binderRestoreCallingIdentity(ident);
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th7) {
                        th = th7;
                    }
                }
                throw th;
            }
        }
    }

    private boolean isLockScreenSecureUnchecked(final int userId) {
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda169
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3031x23769f0(userId);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isLockScreenSecureUnchecked$31$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3031x23769f0(int userId) throws Exception {
        return Boolean.valueOf(this.mLockPatternUtils.isSecure(userId));
    }

    private void setDoNotAskCredentialsOnBoot() {
        synchronized (getLockObject()) {
            DevicePolicyData policyData = m3013x8b75536b(0);
            if (!policyData.mDoNotAskCredentialsOnBoot) {
                policyData.mDoNotAskCredentialsOnBoot = true;
                saveSettingsLocked(0);
            }
        }
    }

    public boolean getDoNotAskCredentialsOnBoot() {
        boolean z;
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.QUERY_DO_NOT_ASK_CREDENTIALS_ON_BOOT"));
        synchronized (getLockObject()) {
            DevicePolicyData policyData = m3013x8b75536b(0);
            z = policyData.mDoNotAskCredentialsOnBoot;
        }
        return z;
    }

    public void setMaximumTimeToLock(ComponentName who, long timeMs, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 3, parent);
            if (ap.maximumTimeToUnlock != timeMs) {
                ap.maximumTimeToUnlock = timeMs;
                saveSettingsLocked(userHandle);
                updateMaximumTimeToLockLocked(userHandle);
            }
        }
        if (SecurityLog.isLoggingEnabled()) {
            int affectedUserId = parent ? getProfileParentId(userHandle) : userHandle;
            SecurityLog.writeEvent(210019, new Object[]{who.getPackageName(), Integer.valueOf(userHandle), Integer.valueOf(affectedUserId), Long.valueOf(timeMs)});
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMaximumTimeToLockLocked(final int userId) {
        if (isManagedProfile(userId)) {
            updateProfileLockTimeoutLocked(userId);
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda12
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3118xeed9a6ed(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateMaximumTimeToLockLocked$32$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3118xeed9a6ed(int userId) throws Exception {
        int parentId = getProfileParentId(userId);
        long timeMs = getMaximumTimeToLockPolicyFromAdmins(getActiveAdminsForLockscreenPoliciesLocked(parentId));
        DevicePolicyData policy = getUserDataUnchecked(parentId);
        if (policy.mLastMaximumTimeToLock == timeMs) {
            return;
        }
        policy.mLastMaximumTimeToLock = timeMs;
        if (policy.mLastMaximumTimeToLock != JobStatus.NO_LATEST_RUNTIME) {
            this.mInjector.settingsGlobalPutInt("stay_on_while_plugged_in", 0);
        }
        getPowerManagerInternal().setMaximumScreenOffTimeoutFromDeviceAdmin(0, timeMs);
    }

    private void updateProfileLockTimeoutLocked(final int userId) {
        long timeMs;
        if (isSeparateProfileChallengeEnabled(userId)) {
            timeMs = getMaximumTimeToLockPolicyFromAdmins(getActiveAdminsForLockscreenPoliciesLocked(userId));
        } else {
            timeMs = JobStatus.NO_LATEST_RUNTIME;
        }
        final DevicePolicyData policy = getUserDataUnchecked(userId);
        if (policy.mLastMaximumTimeToLock == timeMs) {
            return;
        }
        policy.mLastMaximumTimeToLock = timeMs;
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda28
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3121x499d2072(userId, policy);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateProfileLockTimeoutLocked$33$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3121x499d2072(int userId, DevicePolicyData policy) throws Exception {
        getPowerManagerInternal().setMaximumScreenOffTimeoutFromDeviceAdmin(userId, policy.mLastMaximumTimeToLock);
    }

    public long getMaximumTimeToLock(ComponentName who, int userHandle, boolean parent) {
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            Preconditions.checkCallAuthorization(who == null || isCallingFromPackage(who.getPackageName(), caller.getUid()) || canQueryAdminPolicy(caller));
            synchronized (getLockObject()) {
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    return admin != null ? admin.maximumTimeToUnlock : 0L;
                }
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(getProfileParentUserIfRequested(userHandle, parent));
                long timeMs = getMaximumTimeToLockPolicyFromAdmins(admins);
                if (timeMs != JobStatus.NO_LATEST_RUNTIME) {
                    r1 = timeMs;
                }
                return r1;
            }
        }
        return 0L;
    }

    private long getMaximumTimeToLockPolicyFromAdmins(List<ActiveAdmin> admins) {
        long time = JobStatus.NO_LATEST_RUNTIME;
        for (ActiveAdmin admin : admins) {
            if (admin.maximumTimeToUnlock > 0 && admin.maximumTimeToUnlock < time) {
                time = admin.maximumTimeToUnlock;
            }
        }
        return time;
    }

    public void setRequiredStrongAuthTimeout(ComponentName who, long timeoutMs, boolean parent) {
        long timeoutMs2;
        if (!this.mHasFeature || !this.mLockPatternUtils.hasSecureLockScreen()) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        boolean z = true;
        Preconditions.checkArgument(timeoutMs >= 0, "Timeout must not be a negative number.");
        CallerIdentity caller = getCallerIdentity(who);
        if (!isDefaultDeviceOwner(caller) && !isProfileOwner(caller)) {
            z = false;
        }
        Preconditions.checkCallAuthorization(z);
        long minimumStrongAuthTimeout = getMinimumStrongAuthTimeoutMs();
        if (timeoutMs != 0 && timeoutMs < minimumStrongAuthTimeout) {
            timeoutMs = minimumStrongAuthTimeout;
        }
        if (timeoutMs <= 259200000) {
            timeoutMs2 = timeoutMs;
        } else {
            timeoutMs2 = 259200000;
        }
        int userHandle = caller.getUserId();
        boolean changed = false;
        synchronized (getLockObject()) {
            ActiveAdmin ap = getParentOfAdminIfRequired(getProfileOwnerOrDeviceOwnerLocked(caller), parent);
            if (ap.strongAuthUnlockTimeout != timeoutMs2) {
                ap.strongAuthUnlockTimeout = timeoutMs2;
                saveSettingsLocked(userHandle);
                changed = true;
            }
        }
        if (changed) {
            this.mLockSettingsInternal.refreshStrongAuthTimeout(userHandle);
            if (isManagedProfile(userHandle) && !isSeparateProfileChallengeEnabled(userHandle)) {
                this.mLockSettingsInternal.refreshStrongAuthTimeout(getProfileParentId(userHandle));
            }
        }
    }

    public long getRequiredStrongAuthTimeout(ComponentName who, int userId, boolean parent) {
        if (!this.mHasFeature) {
            return 259200000L;
        }
        Preconditions.checkArgumentNonnegative(userId, "Invalid userId");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userId));
        if (this.mLockPatternUtils.hasSecureLockScreen()) {
            synchronized (getLockObject()) {
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userId, parent);
                    return admin != null ? admin.strongAuthUnlockTimeout : 0L;
                }
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(getProfileParentUserIfRequested(userId, parent));
                long strongAuthUnlockTimeout = 259200000;
                for (int i = 0; i < admins.size(); i++) {
                    long timeout = admins.get(i).strongAuthUnlockTimeout;
                    if (timeout != 0) {
                        strongAuthUnlockTimeout = Math.min(timeout, strongAuthUnlockTimeout);
                    }
                }
                return Math.max(strongAuthUnlockTimeout, getMinimumStrongAuthTimeoutMs());
            }
        }
        return 0L;
    }

    private long getMinimumStrongAuthTimeoutMs() {
        if (!this.mInjector.isBuildDebuggable()) {
            return MINIMUM_STRONG_AUTH_TIMEOUT_MS;
        }
        Injector injector = this.mInjector;
        long j = MINIMUM_STRONG_AUTH_TIMEOUT_MS;
        return Math.min(injector.systemPropertiesGetLong("persist.sys.min_str_auth_timeo", j), j);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5414=4] */
    public void lockNow(int flags, boolean parent) {
        ComponentName component;
        Injector injector;
        CallerIdentity caller = getCallerIdentity();
        int callingUserId = caller.getUserId();
        ComponentName adminComponent = null;
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminOrCheckPermissionForCallerLocked(null, 3, parent, "android.permission.LOCK_DEVICE");
            checkCanExecuteOrThrowUnsafe(1);
            long ident = this.mInjector.binderClearCallingIdentity();
            if (admin == null) {
                component = null;
            } else {
                try {
                    component = admin.info.getComponent();
                } catch (RemoteException e) {
                    injector = this.mInjector;
                    injector.binderRestoreCallingIdentity(ident);
                    DevicePolicyEventLogger.createEvent(10).setAdmin(adminComponent).setInt(flags).write();
                } catch (Throwable th) {
                    th = th;
                    this.mInjector.binderRestoreCallingIdentity(ident);
                    throw th;
                }
            }
            adminComponent = component;
            if (adminComponent != null && (flags & 1) != 0) {
                try {
                    Preconditions.checkCallingUser(isManagedProfile(callingUserId));
                    Preconditions.checkArgument(!parent, "Cannot set FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY for the parent");
                    if (!isProfileOwner(adminComponent, callingUserId)) {
                        throw new SecurityException("Only profile owner admins can set FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY");
                    }
                    if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
                        throw new UnsupportedOperationException("FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY only applies to FBE devices");
                    }
                    this.mUserManager.evictCredentialEncryptionKey(callingUserId);
                } catch (RemoteException e2) {
                    injector = this.mInjector;
                    injector.binderRestoreCallingIdentity(ident);
                    DevicePolicyEventLogger.createEvent(10).setAdmin(adminComponent).setInt(flags).write();
                } catch (Throwable th2) {
                    th = th2;
                    this.mInjector.binderRestoreCallingIdentity(ident);
                    throw th;
                }
            }
            int userToLock = (parent || !isSeparateProfileChallengeEnabled(callingUserId)) ? -1 : callingUserId;
            this.mLockPatternUtils.requireStrongAuth(2, userToLock);
            try {
                if (userToLock == -1) {
                    if (!this.mIsAutomotive) {
                        this.mInjector.powerManagerGoToSleep(SystemClock.uptimeMillis(), 1, 0);
                    }
                    this.mInjector.getIWindowManager().lockNow((Bundle) null);
                } else {
                    this.mInjector.getTrustManager().setDeviceLockedForUser(userToLock, true);
                }
                if (SecurityLog.isLoggingEnabled() && adminComponent != null) {
                    int affectedUserId = parent ? getProfileParentId(callingUserId) : callingUserId;
                    SecurityLog.writeEvent(210022, new Object[]{adminComponent.getPackageName(), Integer.valueOf(callingUserId), Integer.valueOf(affectedUserId)});
                }
                injector = this.mInjector;
            } catch (RemoteException e3) {
                injector = this.mInjector;
                injector.binderRestoreCallingIdentity(ident);
                DevicePolicyEventLogger.createEvent(10).setAdmin(adminComponent).setInt(flags).write();
            } catch (Throwable th3) {
                th = th3;
                this.mInjector.binderRestoreCallingIdentity(ident);
                throw th;
            }
            injector.binderRestoreCallingIdentity(ident);
        }
        DevicePolicyEventLogger.createEvent(10).setAdmin(adminComponent).setInt(flags).write();
    }

    public void enforceCanManageCaCerts(ComponentName who, String callerPackage) {
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization(canManageCaCerts(caller));
    }

    private boolean canManageCaCerts(CallerIdentity caller) {
        return (caller.hasAdminComponent() && (isDefaultDeviceOwner(caller) || isProfileOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-cert-install")) || hasCallingOrSelfPermission("android.permission.MANAGE_CA_CERTIFICATES");
    }

    public boolean approveCaCert(String alias, int userId, boolean approval) {
        Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()));
        synchronized (getLockObject()) {
            Set<String> certs = m3013x8b75536b(userId).mAcceptedCaCertificates;
            boolean changed = approval ? certs.add(alias) : certs.remove(alias);
            if (!changed) {
                return false;
            }
            saveSettingsLocked(userId);
            this.mCertificateMonitor.onCertificateApprovalsChanged(userId);
            return true;
        }
    }

    public boolean isCaCertApproved(String alias, int userId) {
        boolean contains;
        Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()));
        synchronized (getLockObject()) {
            contains = m3013x8b75536b(userId).mAcceptedCaCertificates.contains(alias);
        }
        return contains;
    }

    private Set<Integer> removeCaApprovalsIfNeeded(int userId) {
        ArraySet<Integer> affectedUserIds = new ArraySet<>();
        for (UserInfo userInfo : this.mUserManager.getProfiles(userId)) {
            boolean isSecure = this.mLockPatternUtils.isSecure(userInfo.id);
            if (userInfo.isManagedProfile()) {
                isSecure |= this.mLockPatternUtils.isSecure(getProfileParentId(userInfo.id));
            }
            if (!isSecure) {
                synchronized (getLockObject()) {
                    m3013x8b75536b(userInfo.id).mAcceptedCaCertificates.clear();
                    affectedUserIds.add(Integer.valueOf(userInfo.id));
                }
                this.mCertificateMonitor.onCertificateApprovalsChanged(userId);
            }
        }
        return affectedUserIds;
    }

    public boolean installCaCert(final ComponentName admin, String callerPackage, final byte[] certBuffer) {
        if (this.mHasFeature) {
            final CallerIdentity caller = getCallerIdentity(admin, callerPackage);
            Preconditions.checkCallAuthorization(canManageCaCerts(caller));
            checkCanExecuteOrThrowUnsafe(24);
            String alias = (String) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda74
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3019xc8bfff74(caller, certBuffer, admin);
                }
            });
            if (alias == null) {
                Slogf.w(LOG_TAG, "Problem installing cert");
                return false;
            }
            synchronized (getLockObject()) {
                m3013x8b75536b(caller.getUserId()).mOwnerInstalledCaCerts.add(alias);
                saveSettingsLocked(caller.getUserId());
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$installCaCert$34$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ String m3019xc8bfff74(CallerIdentity caller, byte[] certBuffer, ComponentName admin) throws Exception {
        String installedAlias = this.mCertificateMonitor.installCaCert(caller.getUserHandle(), certBuffer);
        DevicePolicyEventLogger.createEvent(21).setAdmin(caller.getPackageName()).setBoolean(admin == null).write();
        return installedAlias;
    }

    public void uninstallCaCerts(final ComponentName admin, String callerPackage, final String[] aliases) {
        if (!this.mHasFeature) {
            return;
        }
        final CallerIdentity caller = getCallerIdentity(admin, callerPackage);
        Preconditions.checkCallAuthorization(canManageCaCerts(caller));
        checkCanExecuteOrThrowUnsafe(40);
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda18
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3117x96607bdf(caller, aliases, admin);
            }
        });
        synchronized (getLockObject()) {
            if (m3013x8b75536b(caller.getUserId()).mOwnerInstalledCaCerts.removeAll(Arrays.asList(aliases))) {
                saveSettingsLocked(caller.getUserId());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$uninstallCaCerts$35$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3117x96607bdf(CallerIdentity caller, String[] aliases, ComponentName admin) throws Exception {
        this.mCertificateMonitor.uninstallCaCerts(caller.getUserHandle(), aliases);
        DevicePolicyEventLogger.createEvent(24).setAdmin(caller.getPackageName()).setBoolean(admin == null).write();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5582=6, 5588=5] */
    public boolean installKeyPair(ComponentName who, String callerPackage, byte[] privKey, byte[] cert, byte[] chain, String alias, boolean requestAccess, boolean isUserSelectable) {
        long id;
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        boolean isCallerDelegate = isCallerDelegate(caller, "delegation-cert-install");
        boolean isCredentialManagementApp = isCredentialManagementApp(caller);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && (isCallerDelegate || isCredentialManagementApp)));
        if (isCredentialManagementApp) {
            Preconditions.checkCallAuthorization(!isUserSelectable, "The credential management app is not allowed to install a user selectable key pair");
            Preconditions.checkCallAuthorization(isAliasInCredentialManagementAppPolicy(caller, alias), CREDENTIAL_MANAGEMENT_APP_INVALID_ALIAS_MSG);
        }
        checkCanExecuteOrThrowUnsafe(25);
        long id2 = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, caller.getUserHandle());
                try {
                    try {
                        try {
                            IKeyChainService keyChain = keyChainConnection.getService();
                            id = id2;
                            try {
                                if (!keyChain.installKeyPair(privKey, cert, chain, alias, -1)) {
                                    logInstallKeyPairFailure(caller, isCredentialManagementApp);
                                    keyChainConnection.close();
                                    this.mInjector.binderRestoreCallingIdentity(id);
                                    return false;
                                }
                                if (requestAccess) {
                                    keyChain.setGrant(caller.getUid(), alias, true);
                                }
                                keyChain.setUserSelectable(alias, isUserSelectable);
                                DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(20).setAdmin(caller.getPackageName()).setBoolean(isCallerDelegate);
                                String[] strArr = new String[1];
                                strArr[0] = isCredentialManagementApp ? CREDENTIAL_MANAGEMENT_APP : NOT_CREDENTIAL_MANAGEMENT_APP;
                                devicePolicyEventLogger.setStrings(strArr).write();
                                keyChainConnection.close();
                                this.mInjector.binderRestoreCallingIdentity(id);
                                return true;
                            } catch (RemoteException e) {
                                e = e;
                                Slogf.e(LOG_TAG, "Installing certificate", e);
                                keyChainConnection.close();
                                this.mInjector.binderRestoreCallingIdentity(id);
                                logInstallKeyPairFailure(caller, isCredentialManagementApp);
                                return false;
                            }
                        } catch (Throwable th) {
                            th = th;
                            keyChainConnection.close();
                            throw th;
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                        id = id2;
                    } catch (Throwable th2) {
                        th = th2;
                        keyChainConnection.close();
                        throw th;
                    }
                } catch (InterruptedException e3) {
                    e = e3;
                    Slogf.w(LOG_TAG, "Interrupted while installing certificate", e);
                    Thread.currentThread().interrupt();
                    this.mInjector.binderRestoreCallingIdentity(id);
                    logInstallKeyPairFailure(caller, isCredentialManagementApp);
                    return false;
                }
            } catch (Throwable th3) {
                th = th3;
                this.mInjector.binderRestoreCallingIdentity(id2);
                throw th;
            }
        } catch (InterruptedException e4) {
            e = e4;
            id = id2;
        } catch (Throwable th4) {
            th = th4;
            this.mInjector.binderRestoreCallingIdentity(id2);
            throw th;
        }
    }

    private void logInstallKeyPairFailure(CallerIdentity caller, boolean isCredentialManagementApp) {
        if (!isCredentialManagementApp) {
            return;
        }
        DevicePolicyEventLogger.createEvent(184).setStrings(new String[]{caller.getPackageName()}).write();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5637=4, 5643=4] */
    public boolean removeKeyPair(ComponentName who, String callerPackage, String alias) {
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        boolean isCallerDelegate = isCallerDelegate(caller, "delegation-cert-install");
        boolean isCredentialManagementApp = isCredentialManagementApp(caller);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && (isCallerDelegate || isCredentialManagementApp)));
        if (isCredentialManagementApp) {
            Preconditions.checkCallAuthorization(isAliasInCredentialManagementAppPolicy(caller, alias), CREDENTIAL_MANAGEMENT_APP_INVALID_ALIAS_MSG);
        }
        checkCanExecuteOrThrowUnsafe(28);
        long id = Binder.clearCallingIdentity();
        try {
            try {
                KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, caller.getUserHandle());
                try {
                    try {
                        IKeyChainService keyChain = keyChainConnection.getService();
                        DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(23).setAdmin(caller.getPackageName()).setBoolean(isCallerDelegate);
                        String[] strArr = new String[1];
                        strArr[0] = isCredentialManagementApp ? CREDENTIAL_MANAGEMENT_APP : NOT_CREDENTIAL_MANAGEMENT_APP;
                        devicePolicyEventLogger.setStrings(strArr).write();
                        return keyChain.removeKeyPair(alias);
                    } catch (RemoteException e) {
                        Slogf.e(LOG_TAG, "Removing keypair", e);
                        keyChainConnection.close();
                        return false;
                    }
                } finally {
                    keyChainConnection.close();
                }
            } catch (InterruptedException e2) {
                Slogf.w(LOG_TAG, "Interrupted while removing keypair", e2);
                Thread.currentThread().interrupt();
            }
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    public boolean hasKeyPair(String callerPackage, final String alias) {
        final CallerIdentity caller = getCallerIdentity(callerPackage);
        boolean isCredentialManagementApp = isCredentialManagementApp(caller);
        Preconditions.checkCallAuthorization(canInstallCertificates(caller) || isCredentialManagementApp);
        if (isCredentialManagementApp) {
            Preconditions.checkCallAuthorization(isAliasInCredentialManagementAppPolicy(caller, alias), CREDENTIAL_MANAGEMENT_APP_INVALID_ALIAS_MSG);
        }
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda105
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3017x6124890e(caller, alias);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hasKeyPair$36$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3017x6124890e(CallerIdentity caller, String alias) throws Exception {
        try {
            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, caller.getUserHandle());
            try {
                Boolean valueOf = Boolean.valueOf(keyChainConnection.getService().containsKeyPair(alias));
                if (keyChainConnection != null) {
                    keyChainConnection.close();
                }
                return valueOf;
            } catch (Throwable th) {
                if (keyChainConnection != null) {
                    try {
                        keyChainConnection.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (RemoteException e) {
            Slogf.e(LOG_TAG, "Querying keypair", e);
            return false;
        } catch (InterruptedException e2) {
            Slogf.w(LOG_TAG, "Interrupted while querying keypair", e2);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean canInstallCertificates(CallerIdentity caller) {
        return isProfileOwner(caller) || isDefaultDeviceOwner(caller) || isCallerDelegate(caller, "delegation-cert-install");
    }

    private boolean canChooseCertificates(CallerIdentity caller) {
        return isProfileOwner(caller) || isDefaultDeviceOwner(caller) || isCallerDelegate(caller, "delegation-cert-selection");
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public boolean setKeyGrantToWifiAuth(String callerPackage, String alias, boolean hasGrant) {
        Preconditions.checkStringNotEmpty(alias, "Alias to grant cannot be empty");
        CallerIdentity caller = getCallerIdentity(callerPackage);
        Preconditions.checkCallAuthorization(canChooseCertificates(caller));
        return setKeyChainGrantInternal(alias, hasGrant, 1010, caller.getUserHandle());
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public boolean isKeyPairGrantedToWifiAuth(String callerPackage, final String alias) {
        Preconditions.checkStringNotEmpty(alias, "Alias to check cannot be empty");
        final CallerIdentity caller = getCallerIdentity(callerPackage);
        Preconditions.checkCallAuthorization(canChooseCertificates(caller));
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda51
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3030xe9829352(caller, alias);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isKeyPairGrantedToWifiAuth$37$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3030xe9829352(CallerIdentity caller, String alias) throws Exception {
        try {
            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, caller.getUserHandle());
            new ArrayList();
            int[] granteeUids = keyChainConnection.getService().getGrants(alias);
            for (int uid : granteeUids) {
                if (uid == 1010) {
                    if (keyChainConnection != null) {
                        keyChainConnection.close();
                    }
                    return true;
                }
            }
            if (keyChainConnection != null) {
                keyChainConnection.close();
            }
            return false;
        } catch (RemoteException e) {
            Slogf.e(LOG_TAG, "Querying grant to wifi auth.", e);
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public boolean setKeyGrantForApp(ComponentName who, String callerPackage, String alias, String packageName, boolean hasGrant) {
        Preconditions.checkStringNotEmpty(alias, "Alias to grant cannot be empty");
        Preconditions.checkStringNotEmpty(packageName, "Package to grant to cannot be empty");
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-cert-selection")));
        try {
            ApplicationInfo ai = this.mInjector.getIPackageManager().getApplicationInfo(packageName, 0L, caller.getUserId());
            Preconditions.checkArgument(ai != null, "Provided package %s is not installed", new Object[]{packageName});
            int granteeUid = ai.uid;
            return setKeyChainGrantInternal(alias, hasGrant, granteeUid, caller.getUserHandle());
        } catch (RemoteException e) {
            throw new IllegalStateException("Failure getting grantee uid", e);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5761=5] */
    private boolean setKeyChainGrantInternal(String alias, boolean hasGrant, int granteeUid, UserHandle userHandle) {
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, userHandle);
                try {
                    IKeyChainService keyChain = keyChainConnection.getService();
                    boolean grant = keyChain.setGrant(granteeUid, alias, hasGrant);
                    if (keyChainConnection != null) {
                        keyChainConnection.close();
                    }
                    return grant;
                } catch (Throwable th) {
                    if (keyChainConnection != null) {
                        try {
                            keyChainConnection.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (RemoteException e) {
                Slogf.e(LOG_TAG, "Setting grant for package.", e);
                return false;
            }
        } catch (InterruptedException e2) {
            Slogf.w(LOG_TAG, "Interrupted while setting key grant", e2);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public ParcelableGranteeMap getKeyPairGrants(String callerPackage, final String alias) {
        final CallerIdentity caller = getCallerIdentity(callerPackage);
        Preconditions.checkCallAuthorization(canChooseCertificates(caller));
        final ArrayMap<Integer, Set<String>> result = new ArrayMap<>();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda76
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3002x49738947(caller, alias, result);
            }
        });
        return new ParcelableGranteeMap(result);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getKeyPairGrants$38$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3002x49738947(CallerIdentity caller, String alias, ArrayMap result) throws Exception {
        try {
            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, caller.getUserHandle());
            try {
                int[] granteeUids = keyChainConnection.getService().getGrants(alias);
                PackageManager pm = this.mInjector.getPackageManager(caller.getUserId());
                for (int uid : granteeUids) {
                    String[] packages = pm.getPackagesForUid(uid);
                    if (packages == null) {
                        Slogf.wtf(LOG_TAG, "No packages found for uid " + uid);
                    } else {
                        result.put(Integer.valueOf(uid), new ArraySet(packages));
                    }
                }
                if (keyChainConnection != null) {
                    keyChainConnection.close();
                }
            } catch (Throwable th) {
                if (keyChainConnection != null) {
                    try {
                        keyChainConnection.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (RemoteException e) {
            Slogf.e(LOG_TAG, "Querying keypair grants", e);
        } catch (InterruptedException e2) {
            Slogf.w(LOG_TAG, "Interrupted while querying keypair grants", e2);
            Thread.currentThread().interrupt();
        }
    }

    public void enforceCallerCanRequestDeviceIdAttestation(CallerIdentity caller) throws SecurityException {
        int callerUserId = caller.getUserId();
        if (hasProfileOwner(callerUserId)) {
            Preconditions.checkCallAuthorization(canInstallCertificates(caller));
            if (isProfileOwnerOfOrganizationOwnedDevice(callerUserId) || isUserAffiliatedWithDevice(callerUserId)) {
                return;
            }
            throw new SecurityException("Profile Owner is not allowed to access Device IDs.");
        }
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isCallerDelegate(caller, "delegation-cert-install"));
    }

    public static int[] translateIdAttestationFlags(int idAttestationFlags) {
        Map<Integer, Integer> idTypeToAttestationFlag = new HashMap<>();
        idTypeToAttestationFlag.put(2, 1);
        idTypeToAttestationFlag.put(4, 2);
        idTypeToAttestationFlag.put(8, 3);
        idTypeToAttestationFlag.put(16, 4);
        int numFlagsSet = Integer.bitCount(idAttestationFlags);
        if (numFlagsSet == 0) {
            return null;
        }
        if ((idAttestationFlags & 1) != 0) {
            numFlagsSet--;
            idAttestationFlags &= -2;
        }
        int[] attestationUtilsFlags = new int[numFlagsSet];
        int i = 0;
        for (Integer idType : idTypeToAttestationFlag.keySet()) {
            if ((idType.intValue() & idAttestationFlags) != 0) {
                attestationUtilsFlags[i] = idTypeToAttestationFlag.get(idType).intValue();
                i++;
            }
        }
        return attestationUtilsFlags;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5940=4, 6007=7] */
    /* JADX WARN: Removed duplicated region for block: B:146:0x020c A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean generateKeyPair(ComponentName who, String callerPackage, String algorithm, ParcelableKeyGenParameterSpec parcelableKeySpec, int idAttestationFlags, KeymasterCertificateChain attestationChain) {
        CertificateException certificateException;
        int[] attestationUtilsFlags = translateIdAttestationFlags(idAttestationFlags);
        boolean deviceIdAttestationRequired = attestationUtilsFlags != null;
        KeyGenParameterSpec keySpec = parcelableKeySpec.getSpec();
        String alias = keySpec.getKeystoreAlias();
        Preconditions.checkStringNotEmpty(alias, "Empty alias provided");
        Preconditions.checkArgument((deviceIdAttestationRequired && keySpec.getAttestationChallenge() == null) ? false : true, "Requested Device ID attestation but challenge is empty");
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        boolean isCallerDelegate = isCallerDelegate(caller, "delegation-cert-install");
        boolean isCredentialManagementApp = isCredentialManagementApp(caller);
        if (!deviceIdAttestationRequired || attestationUtilsFlags.length <= 0) {
            Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && (isCallerDelegate || isCredentialManagementApp)));
            if (isCredentialManagementApp) {
                Preconditions.checkCallAuthorization(isAliasInCredentialManagementAppPolicy(caller, alias), CREDENTIAL_MANAGEMENT_APP_INVALID_ALIAS_MSG);
            }
        } else {
            enforceCallerCanRequestDeviceIdAttestation(caller);
            enforceIndividualAttestationSupportedIfRequested(attestationUtilsFlags);
        }
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("Empty alias provided.");
        }
        if (keySpec.getUid() != -1) {
            Slogf.e(LOG_TAG, "Only the caller can be granted access to the generated keypair.");
            logGenerateKeyPairFailure(caller, isCredentialManagementApp);
            return false;
        }
        if (deviceIdAttestationRequired) {
            if (keySpec.getAttestationChallenge() == null) {
                throw new IllegalArgumentException("Requested Device ID attestation but challenge is empty.");
            }
            KeyGenParameterSpec.Builder specBuilder = new KeyGenParameterSpec.Builder(keySpec);
            specBuilder.setAttestationIds(attestationUtilsFlags);
            specBuilder.setDevicePropertiesAttestationIncluded(true);
            keySpec = specBuilder.build();
        }
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                try {
                    KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, caller.getUserHandle());
                    try {
                        IKeyChainService keyChain = keyChainConnection.getService();
                        int generationResult = keyChain.generateKeyPair(algorithm, new ParcelableKeyGenParameterSpec(keySpec));
                        try {
                            if (generationResult != 0) {
                                try {
                                    Slogf.e(LOG_TAG, "KeyChain failed to generate a keypair, error %d.", Integer.valueOf(generationResult));
                                    logGenerateKeyPairFailure(caller, isCredentialManagementApp);
                                    switch (generationResult) {
                                        case 3:
                                            throw new UnsupportedOperationException("Device does not support Device ID attestation.");
                                        case 6:
                                            throw new ServiceSpecificException(1, String.format("KeyChain error: %d", Integer.valueOf(generationResult)));
                                        default:
                                            if (keyChainConnection != null) {
                                                try {
                                                    keyChainConnection.close();
                                                } catch (RemoteException e) {
                                                    e = e;
                                                    Slogf.e(LOG_TAG, "KeyChain error while generating a keypair", e);
                                                    this.mInjector.binderRestoreCallingIdentity(id);
                                                    logGenerateKeyPairFailure(caller, isCredentialManagementApp);
                                                    return false;
                                                } catch (InterruptedException e2) {
                                                    e = e2;
                                                    Slogf.w(LOG_TAG, "Interrupted while generating keypair", e);
                                                    Thread.currentThread().interrupt();
                                                    this.mInjector.binderRestoreCallingIdentity(id);
                                                    logGenerateKeyPairFailure(caller, isCredentialManagementApp);
                                                    return false;
                                                } catch (Throwable th) {
                                                    th = th;
                                                    this.mInjector.binderRestoreCallingIdentity(id);
                                                    throw th;
                                                }
                                            }
                                            this.mInjector.binderRestoreCallingIdentity(id);
                                            return false;
                                    }
                                } catch (Throwable th2) {
                                    certificateException = th2;
                                    if (keyChainConnection != null) {
                                        try {
                                            keyChainConnection.close();
                                        } catch (Throwable th3) {
                                            certificateException.addSuppressed(th3);
                                        }
                                    }
                                    throw certificateException;
                                }
                            }
                            try {
                                keyChain.setGrant(caller.getUid(), alias, true);
                                try {
                                    List<byte[]> encodedCerts = new ArrayList<>();
                                    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                                    byte[] certChainBytes = keyChain.getCaCertificates(alias);
                                    encodedCerts.add(keyChain.getCertificate(alias));
                                    if (certChainBytes != null) {
                                        Iterator<? extends Certificate> it = certFactory.generateCertificates(new ByteArrayInputStream(certChainBytes)).iterator();
                                        while (it.hasNext()) {
                                            X509Certificate cert = (X509Certificate) it.next();
                                            encodedCerts.add(cert.getEncoded());
                                            certFactory = certFactory;
                                        }
                                    }
                                    try {
                                        try {
                                            attestationChain.shallowCopyFrom(new KeymasterCertificateChain(encodedCerts));
                                            DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(59).setAdmin(caller.getPackageName()).setBoolean(isCallerDelegate).setInt(idAttestationFlags);
                                            String[] strArr = new String[2];
                                            strArr[0] = algorithm;
                                            strArr[1] = isCredentialManagementApp ? CREDENTIAL_MANAGEMENT_APP : NOT_CREDENTIAL_MANAGEMENT_APP;
                                            devicePolicyEventLogger.setStrings(strArr).write();
                                            if (keyChainConnection != null) {
                                                keyChainConnection.close();
                                            }
                                            this.mInjector.binderRestoreCallingIdentity(id);
                                            return true;
                                        } catch (CertificateException e3) {
                                            e = e3;
                                            logGenerateKeyPairFailure(caller, isCredentialManagementApp);
                                            Slogf.e(LOG_TAG, "While retrieving certificate chain.", e);
                                            if (keyChainConnection != null) {
                                                keyChainConnection.close();
                                            }
                                            this.mInjector.binderRestoreCallingIdentity(id);
                                            return false;
                                        }
                                    } catch (Throwable th4) {
                                        e = th4;
                                        certificateException = e;
                                        if (keyChainConnection != null) {
                                        }
                                        throw certificateException;
                                    }
                                } catch (CertificateException e4) {
                                    e = e4;
                                }
                            } catch (Throwable th5) {
                                e = th5;
                            }
                        } catch (Throwable th6) {
                            e = th6;
                        }
                    } catch (Throwable th7) {
                        certificateException = th7;
                    }
                } catch (Throwable th8) {
                    th = th8;
                }
            } catch (RemoteException e5) {
                e = e5;
            } catch (InterruptedException e6) {
                e = e6;
            }
        } catch (RemoteException e7) {
            e = e7;
        } catch (InterruptedException e8) {
            e = e8;
        } catch (Throwable th9) {
            th = th9;
        }
    }

    private void logGenerateKeyPairFailure(CallerIdentity caller, boolean isCredentialManagementApp) {
        if (!isCredentialManagementApp) {
            return;
        }
        DevicePolicyEventLogger.createEvent(185).setStrings(new String[]{caller.getPackageName()}).write();
    }

    private void enforceIndividualAttestationSupportedIfRequested(int[] attestationUtilsFlags) {
        for (int attestationFlag : attestationUtilsFlags) {
            if (attestationFlag == 4 && !this.mInjector.getPackageManager().hasSystemFeature("android.hardware.device_unique_attestation")) {
                throw new UnsupportedOperationException("Device Individual attestation is not supported on this device.");
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [6066=5, 6072=6] */
    /* JADX WARN: Removed duplicated region for block: B:82:0x00bf A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean setKeyPairCertificate(ComponentName who, String callerPackage, String alias, byte[] cert, byte[] chain, boolean isUserSelectable) {
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        boolean isCallerDelegate = isCallerDelegate(caller, "delegation-cert-install");
        boolean isCredentialManagementApp = isCredentialManagementApp(caller);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && (isCallerDelegate || isCredentialManagementApp)));
        if (isCredentialManagementApp) {
            Preconditions.checkCallAuthorization(isAliasInCredentialManagementAppPolicy(caller, alias), CREDENTIAL_MANAGEMENT_APP_INVALID_ALIAS_MSG);
        }
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, caller.getUserHandle());
                try {
                    try {
                        IKeyChainService keyChain = keyChainConnection.getService();
                        try {
                            if (keyChain.setKeyPairCertificate(alias, cert, chain)) {
                                try {
                                    keyChain.setUserSelectable(alias, isUserSelectable);
                                    DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(60).setAdmin(caller.getPackageName()).setBoolean(isCallerDelegate);
                                    String[] strArr = new String[1];
                                    strArr[0] = isCredentialManagementApp ? CREDENTIAL_MANAGEMENT_APP : NOT_CREDENTIAL_MANAGEMENT_APP;
                                    devicePolicyEventLogger.setStrings(strArr).write();
                                    if (keyChainConnection != null) {
                                        keyChainConnection.close();
                                    }
                                    this.mInjector.binderRestoreCallingIdentity(id);
                                    return true;
                                } catch (Throwable th) {
                                    th = th;
                                    Throwable th2 = th;
                                    if (keyChainConnection != null) {
                                        try {
                                            keyChainConnection.close();
                                        } catch (Throwable th3) {
                                            th2.addSuppressed(th3);
                                        }
                                    }
                                    throw th2;
                                }
                            }
                            if (keyChainConnection != null) {
                                try {
                                    keyChainConnection.close();
                                } catch (RemoteException e) {
                                    e = e;
                                    Slogf.e(LOG_TAG, "Failed setting keypair certificate", e);
                                    this.mInjector.binderRestoreCallingIdentity(id);
                                    return false;
                                } catch (InterruptedException e2) {
                                    e = e2;
                                    Slogf.w(LOG_TAG, "Interrupted while setting keypair certificate", e);
                                    Thread.currentThread().interrupt();
                                    this.mInjector.binderRestoreCallingIdentity(id);
                                    return false;
                                } catch (Throwable th4) {
                                    th = th4;
                                    this.mInjector.binderRestoreCallingIdentity(id);
                                    throw th;
                                }
                            }
                            this.mInjector.binderRestoreCallingIdentity(id);
                            return false;
                        } catch (Throwable th5) {
                            th = th5;
                            Throwable th22 = th;
                            if (keyChainConnection != null) {
                            }
                            throw th22;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                    }
                } catch (RemoteException e3) {
                    e = e3;
                    Slogf.e(LOG_TAG, "Failed setting keypair certificate", e);
                    this.mInjector.binderRestoreCallingIdentity(id);
                    return false;
                } catch (InterruptedException e4) {
                    e = e4;
                    Slogf.w(LOG_TAG, "Interrupted while setting keypair certificate", e);
                    Thread.currentThread().interrupt();
                    this.mInjector.binderRestoreCallingIdentity(id);
                    return false;
                }
            } catch (Throwable th7) {
                th = th7;
                this.mInjector.binderRestoreCallingIdentity(id);
                throw th;
            }
        } catch (RemoteException e5) {
            e = e5;
        } catch (InterruptedException e6) {
            e = e6;
        } catch (Throwable th8) {
            th = th8;
        }
    }

    public void choosePrivateKeyAlias(int uid, Uri uri, String alias, final IBinder response) {
        ComponentName aliasChooser;
        boolean isDelegate;
        final CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isSystemUid(caller), String.format(NOT_SYSTEM_CALLER_MSG, "choose private key alias"));
        ComponentName aliasChooser2 = m3036x91e56c0c(caller.getUserId());
        if (aliasChooser2 == null && caller.getUserHandle().isSystem()) {
            synchronized (getLockObject()) {
                ActiveAdmin deviceOwnerAdmin = getDeviceOwnerAdminLocked();
                if (deviceOwnerAdmin != null) {
                    aliasChooser2 = deviceOwnerAdmin.info.getComponent();
                }
            }
            aliasChooser = aliasChooser2;
        } else {
            aliasChooser = aliasChooser2;
        }
        if (aliasChooser == null) {
            sendPrivateKeyAliasResponse(null, response);
            return;
        }
        final Intent intent = new Intent("android.app.action.CHOOSE_PRIVATE_KEY_ALIAS");
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_SENDER_UID", uid);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_URI", uri);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_ALIAS", alias);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_RESPONSE", response);
        intent.addFlags(268435456);
        ComponentName delegateReceiver = resolveDelegateReceiver("delegation-cert-selection", "android.app.action.CHOOSE_PRIVATE_KEY_ALIAS", caller.getUserId());
        if (delegateReceiver != null) {
            intent.setComponent(delegateReceiver);
            isDelegate = true;
        } else {
            intent.setComponent(aliasChooser);
            isDelegate = false;
        }
        final boolean z = isDelegate;
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda68
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m2982x8feca2ef(intent, caller, response, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$choosePrivateKeyAlias$39$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m2982x8feca2ef(Intent intent, CallerIdentity caller, final IBinder response, boolean isDelegate) throws Exception {
        this.mContext.sendOrderedBroadcastAsUser(intent, caller.getUserHandle(), null, new BroadcastReceiver() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent2) {
                String chosenAlias = getResultData();
                DevicePolicyManagerService.this.sendPrivateKeyAliasResponse(chosenAlias, response);
            }
        }, null, -1, null, null);
        DevicePolicyEventLogger.createEvent(22).setAdmin(intent.getComponent()).setBoolean(isDelegate).write();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendPrivateKeyAliasResponse(String alias, IBinder responseBinder) {
        IKeyChainAliasCallback keyChainAliasResponse = IKeyChainAliasCallback.Stub.asInterface(responseBinder);
        try {
            keyChainAliasResponse.alias(alias);
        } catch (Exception e) {
            Slogf.e(LOG_TAG, "error while responding to callback", e);
        }
    }

    private static boolean shouldCheckIfDelegatePackageIsInstalled(String delegatePackage, int targetSdk, List<String> scopes) {
        if (targetSdk >= 24) {
            return true;
        }
        return ((scopes.size() == 1 && scopes.get(0).equals("delegation-cert-install")) || scopes.isEmpty()) ? false : true;
    }

    public void setDelegatedScopes(ComponentName who, String delegatePackage, List<String> scopeList) throws SecurityException {
        Objects.requireNonNull(who, "ComponentName is null");
        Preconditions.checkStringNotEmpty(delegatePackage, "Delegate package is null or empty");
        Preconditions.checkCollectionElementsNotNull(scopeList, "Scopes");
        CallerIdentity caller = getCallerIdentity(who);
        ArrayList<String> scopes = new ArrayList<>(new ArraySet(scopeList));
        if (scopes.retainAll(Arrays.asList(DELEGATIONS))) {
            throw new IllegalArgumentException("Unexpected delegation scopes");
        }
        int userId = caller.getUserId();
        boolean z = false;
        if (!Collections.disjoint(scopes, DEVICE_OWNER_OR_MANAGED_PROFILE_OWNER_DELEGATIONS)) {
            if (isDefaultDeviceOwner(caller) || (isProfileOwner(caller) && isManagedProfile(caller.getUserId()))) {
                z = true;
            }
            Preconditions.checkCallAuthorization(z);
        } else if (!Collections.disjoint(scopes, DEVICE_OWNER_OR_ORGANIZATION_OWNED_MANAGED_PROFILE_OWNER_DELEGATIONS)) {
            Preconditions.checkCallAuthorization((isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller)) ? true : true);
        } else {
            Preconditions.checkCallAuthorization((isDefaultDeviceOwner(caller) || isProfileOwner(caller)) ? true : true);
        }
        synchronized (getLockObject()) {
            if (shouldCheckIfDelegatePackageIsInstalled(delegatePackage, getTargetSdk(who.getPackageName(), userId), scopes) && !isPackageInstalledForUser(delegatePackage, userId)) {
                throw new IllegalArgumentException("Package " + delegatePackage + " is not installed on the current user");
            }
            DevicePolicyData policy = m3013x8b75536b(userId);
            List<String> exclusiveScopes = null;
            if (!scopes.isEmpty()) {
                policy.mDelegationMap.put(delegatePackage, new ArrayList(scopes));
                exclusiveScopes = new ArrayList<>(scopes);
                exclusiveScopes.retainAll(EXCLUSIVE_DELEGATIONS);
            } else {
                policy.mDelegationMap.remove(delegatePackage);
            }
            sendDelegationChangedBroadcast(delegatePackage, scopes, userId);
            if (exclusiveScopes != null && !exclusiveScopes.isEmpty()) {
                for (int i = policy.mDelegationMap.size() - 1; i >= 0; i--) {
                    String currentPackage = policy.mDelegationMap.keyAt(i);
                    List<String> currentScopes = policy.mDelegationMap.valueAt(i);
                    if (!currentPackage.equals(delegatePackage) && currentScopes.removeAll(exclusiveScopes)) {
                        if (currentScopes.isEmpty()) {
                            policy.mDelegationMap.removeAt(i);
                        }
                        sendDelegationChangedBroadcast(currentPackage, new ArrayList<>(currentScopes), userId);
                    }
                }
            }
            saveSettingsLocked(userId);
        }
    }

    private void sendDelegationChangedBroadcast(String delegatePackage, ArrayList<String> scopes, int userId) {
        Intent intent = new Intent("android.app.action.APPLICATION_DELEGATION_SCOPES_CHANGED");
        intent.addFlags(1073741824);
        intent.setPackage(delegatePackage);
        intent.putStringArrayListExtra("android.app.extra.DELEGATION_SCOPES", scopes);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
    }

    public List<String> getDelegatedScopes(ComponentName who, String delegatePackage) throws SecurityException {
        List<String> list;
        Objects.requireNonNull(delegatePackage, "Delegate package is null");
        CallerIdentity caller = getCallerIdentity(who);
        boolean z = true;
        if (caller.hasAdminComponent()) {
            if (!isProfileOwner(caller) && !isDefaultDeviceOwner(caller)) {
                z = false;
            }
            Preconditions.checkCallAuthorization(z);
        } else {
            Preconditions.checkCallAuthorization(isPackage(caller, delegatePackage), String.format("Caller with uid %d is not %s", Integer.valueOf(caller.getUid()), delegatePackage));
        }
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(caller.getUserId());
            List<String> scopes = policy.mDelegationMap.get(delegatePackage);
            list = scopes == null ? Collections.EMPTY_LIST : scopes;
        }
        return list;
    }

    public List<String> getDelegatePackages(ComponentName who, String scope) throws SecurityException {
        List<String> delegatePackagesInternalLocked;
        Objects.requireNonNull(who, "ComponentName is null");
        Objects.requireNonNull(scope, "Scope is null");
        if (!Arrays.asList(DELEGATIONS).contains(scope)) {
            throw new IllegalArgumentException("Unexpected delegation scope: " + scope);
        }
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        synchronized (getLockObject()) {
            delegatePackagesInternalLocked = getDelegatePackagesInternalLocked(scope, caller.getUserId());
        }
        return delegatePackagesInternalLocked;
    }

    private List<String> getDelegatePackagesInternalLocked(String scope, int userId) {
        DevicePolicyData policy = m3013x8b75536b(userId);
        List<String> delegatePackagesWithScope = new ArrayList<>();
        for (int i = 0; i < policy.mDelegationMap.size(); i++) {
            if (policy.mDelegationMap.valueAt(i).contains(scope)) {
                delegatePackagesWithScope.add(policy.mDelegationMap.keyAt(i));
            }
        }
        return delegatePackagesWithScope;
    }

    private ComponentName resolveDelegateReceiver(String scope, String action, int userId) {
        List<String> delegates;
        synchronized (getLockObject()) {
            delegates = getDelegatePackagesInternalLocked(scope, userId);
        }
        if (delegates.size() == 0) {
            return null;
        }
        if (delegates.size() > 1) {
            Slogf.wtf(LOG_TAG, "More than one delegate holds " + scope);
            return null;
        }
        String pkg = delegates.get(0);
        Intent intent = new Intent(action);
        intent.setPackage(pkg);
        try {
            List<ResolveInfo> receivers = this.mIPackageManager.queryIntentReceivers(intent, (String) null, 0L, userId).getList();
            int count = receivers.size();
            if (count >= 1) {
                if (count > 1) {
                    Slogf.w(LOG_TAG, pkg + " defines more than one delegate receiver for " + action);
                }
                return receivers.get(0).activityInfo.getComponentName();
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    private boolean isCallerDelegate(String callerPackage, int callerUid, String scope) {
        Objects.requireNonNull(callerPackage, "callerPackage is null");
        if (!Arrays.asList(DELEGATIONS).contains(scope)) {
            throw new IllegalArgumentException("Unexpected delegation scope: " + scope);
        }
        int userId = UserHandle.getUserId(callerUid);
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(userId);
            List<String> scopes = policy.mDelegationMap.get(callerPackage);
            if (scopes != null && scopes.contains(scope)) {
                return isCallingFromPackage(callerPackage, callerUid);
            }
            return false;
        }
    }

    private boolean isCallerDelegate(CallerIdentity caller, String scope) {
        Objects.requireNonNull(caller.getPackageName(), "callerPackage is null");
        boolean z = true;
        Preconditions.checkArgument(Arrays.asList(DELEGATIONS).contains(scope), "Unexpected delegation scope: %s", new Object[]{scope});
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(caller.getUserId());
            List<String> scopes = policy.mDelegationMap.get(caller.getPackageName());
            if (scopes == null || !scopes.contains(scope)) {
                z = false;
            }
        }
        return z;
    }

    private void setDelegatedScopePreO(ComponentName who, String delegatePackage, String scope) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(caller.getUserId());
            if (delegatePackage != null) {
                List<String> scopes = policy.mDelegationMap.get(delegatePackage);
                if (scopes == null) {
                    scopes = new ArrayList();
                }
                if (!scopes.contains(scope)) {
                    scopes.add(scope);
                    setDelegatedScopes(who, delegatePackage, scopes);
                }
            }
            for (int i = 0; i < policy.mDelegationMap.size(); i++) {
                String currentPackage = policy.mDelegationMap.keyAt(i);
                List<String> currentScopes = policy.mDelegationMap.valueAt(i);
                if (!currentPackage.equals(delegatePackage) && currentScopes.contains(scope)) {
                    List<String> newScopes = new ArrayList<>(currentScopes);
                    newScopes.remove(scope);
                    setDelegatedScopes(who, currentPackage, newScopes);
                }
            }
        }
    }

    private boolean isCredentialManagementApp(final CallerIdentity caller) {
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda101
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3027xa3f92819(caller);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isCredentialManagementApp$40$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3027xa3f92819(CallerIdentity caller) throws Exception {
        AppOpsManager appOpsManager = this.mInjector.getAppOpsManager();
        if (appOpsManager == null) {
            return false;
        }
        return Boolean.valueOf(appOpsManager.noteOpNoThrow(104, caller.getUid(), caller.getPackageName(), (String) null, (String) null) == 0);
    }

    private boolean isAliasInCredentialManagementAppPolicy(final CallerIdentity caller, final String alias) {
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda59
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3021xc647e1a3(caller, alias);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isAliasInCredentialManagementAppPolicy$41$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3021xc647e1a3(CallerIdentity caller, String alias) throws Exception {
        try {
            KeyChain.KeyChainConnection connection = KeyChain.bindAsUser(this.mContext, caller.getUserHandle());
            AppUriAuthenticationPolicy policy = connection.getService().getCredentialManagementAppPolicy();
            Boolean valueOf = Boolean.valueOf((policy == null || policy.getAppAndUriMappings().isEmpty() || !containsAlias(policy, alias)) ? false : true);
            if (connection != null) {
                connection.close();
            }
            return valueOf;
        } catch (RemoteException | InterruptedException e) {
            return false;
        }
    }

    private static boolean containsAlias(AppUriAuthenticationPolicy policy, String alias) {
        for (Map.Entry<String, Map<Uri, String>> appsToUris : policy.getAppAndUriMappings().entrySet()) {
            for (Map.Entry<Uri, String> urisToAliases : appsToUris.getValue().entrySet()) {
                if (urisToAliases.getValue().equals(alias)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setCertInstallerPackage(ComponentName who, String installerPackage) throws SecurityException {
        setDelegatedScopePreO(who, installerPackage, "delegation-cert-install");
        DevicePolicyEventLogger.createEvent(25).setAdmin(who).setStrings(new String[]{installerPackage}).write();
    }

    public String getCertInstallerPackage(ComponentName who) throws SecurityException {
        List<String> delegatePackages = getDelegatePackages(who, "delegation-cert-install");
        if (delegatePackages.size() > 0) {
            return delegatePackages.get(0);
        }
        return null;
    }

    public boolean setAlwaysOnVpnPackage(ComponentName who, final String vpnPackage, final boolean lockdown, final List<String> lockdownAllowlist) throws SecurityException {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        checkCanExecuteOrThrowUnsafe(30);
        if (vpnPackage == null) {
            synchronized (getLockObject()) {
                String prevVpnPackage = getProfileOwnerOrDeviceOwnerLocked(caller).mAlwaysOnVpnPackage;
                if (TextUtils.isEmpty(prevVpnPackage)) {
                    return true;
                }
                revokeVpnAuthorizationForPackage(prevVpnPackage, caller.getUserId());
            }
        }
        final int userId = caller.getUserId();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda95
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3068xdcd861fc(vpnPackage, userId, lockdown, lockdownAllowlist);
            }
        });
        DevicePolicyEventLogger.createEvent(26).setAdmin(caller.getComponentName()).setStrings(new String[]{vpnPackage}).setBoolean(lockdown).setInt(lockdownAllowlist != null ? lockdownAllowlist.size() : 0).write();
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            if (!TextUtils.equals(vpnPackage, admin.mAlwaysOnVpnPackage) || lockdown != admin.mAlwaysOnVpnLockdown) {
                admin.mAlwaysOnVpnPackage = vpnPackage;
                admin.mAlwaysOnVpnLockdown = lockdown;
                saveSettingsLocked(userId);
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setAlwaysOnVpnPackage$42$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3068xdcd861fc(String vpnPackage, int userId, boolean lockdown, List lockdownAllowlist) throws Exception {
        if (vpnPackage != null && !isPackageInstalledForUser(vpnPackage, userId)) {
            Slogf.w(LOG_TAG, "Non-existent VPN package specified: " + vpnPackage);
            throw new ServiceSpecificException(1, vpnPackage);
        }
        if (vpnPackage != null && lockdown && lockdownAllowlist != null) {
            Iterator it = lockdownAllowlist.iterator();
            while (it.hasNext()) {
                String packageName = (String) it.next();
                if (!isPackageInstalledForUser(packageName, userId)) {
                    Slogf.w(LOG_TAG, "Non-existent package in VPN allowlist: " + packageName);
                    throw new ServiceSpecificException(1, packageName);
                }
            }
        }
        if (!this.mInjector.getVpnManager().setAlwaysOnVpnPackageForUser(userId, vpnPackage, lockdown, lockdownAllowlist)) {
            throw new UnsupportedOperationException();
        }
    }

    private void revokeVpnAuthorizationForPackage(final String vpnPackage, final int userId) {
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda31
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3064x3e28f03b(vpnPackage, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$revokeVpnAuthorizationForPackage$43$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3064x3e28f03b(String vpnPackage, int userId) throws Exception {
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(vpnPackage, 0L, userId);
            if (ai == null) {
                Slogf.w(LOG_TAG, "Non-existent VPN package: " + vpnPackage);
            } else {
                this.mInjector.getAppOpsManager().setMode(47, ai.uid, vpnPackage, 3);
            }
        } catch (RemoteException e) {
            Slogf.e(LOG_TAG, "Can't talk to package managed", e);
        }
    }

    public String getAlwaysOnVpnPackage(ComponentName admin) throws SecurityException {
        Objects.requireNonNull(admin, "ComponentName is null");
        final CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        return (String) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda23
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m2995xf11c3ae(caller);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getAlwaysOnVpnPackage$44$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ String m2995xf11c3ae(CallerIdentity caller) throws Exception {
        return this.mInjector.getVpnManager().getAlwaysOnVpnPackageForUser(caller.getUserId());
    }

    public String getAlwaysOnVpnPackageForUser(int userHandle) {
        String str;
        Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "call getAlwaysOnVpnPackageForUser"));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getDeviceOrProfileOwnerAdminLocked(userHandle);
            str = admin != null ? admin.mAlwaysOnVpnPackage : null;
        }
        return str;
    }

    public boolean isAlwaysOnVpnLockdownEnabled(ComponentName admin) throws SecurityException {
        final CallerIdentity caller;
        if (hasCallingPermission("android.permission.MAINLINE_NETWORK_STACK")) {
            caller = getCallerIdentity();
        } else {
            caller = getCallerIdentity(admin);
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        }
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda57
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3022x2666e9a9(caller);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isAlwaysOnVpnLockdownEnabled$45$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3022x2666e9a9(CallerIdentity caller) throws Exception {
        return Boolean.valueOf(this.mInjector.getVpnManager().isVpnLockdownEnabled(caller.getUserId()));
    }

    public boolean isAlwaysOnVpnLockdownEnabledForUser(int userHandle) {
        boolean z = true;
        Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "call isAlwaysOnVpnLockdownEnabledForUser"));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getDeviceOrProfileOwnerAdminLocked(userHandle);
            if (admin == null || !admin.mAlwaysOnVpnLockdown) {
                z = false;
            }
        }
        return z;
    }

    public List<String> getAlwaysOnVpnLockdownAllowlist(ComponentName admin) throws SecurityException {
        Objects.requireNonNull(admin, "ComponentName is null");
        final CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        return (List) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda124
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m2994xb8d4900(caller);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getAlwaysOnVpnLockdownAllowlist$46$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ List m2994xb8d4900(CallerIdentity caller) throws Exception {
        return this.mInjector.getVpnManager().getVpnLockdownAllowlist(caller.getUserId());
    }

    private void forceWipeDeviceNoLock(boolean wipeExtRequested, String reason, boolean wipeEuicc, boolean wipeResetProtectionData) {
        wtfIfInLock();
        try {
            try {
                boolean delayed = !this.mInjector.recoverySystemRebootWipeUserData(false, reason, true, wipeEuicc, wipeExtRequested, wipeResetProtectionData);
                if (delayed) {
                    Slogf.i(LOG_TAG, "Persisting factory reset request as it could be delayed by %s", this.mSafetyChecker);
                    synchronized (getLockObject()) {
                        DevicePolicyData policy = m3013x8b75536b(0);
                        policy.setDelayedFactoryReset(reason, wipeExtRequested, wipeEuicc, wipeResetProtectionData);
                        saveSettingsLocked(0);
                    }
                }
                if (1 == 0) {
                    SecurityLog.writeEvent(210023, new Object[0]);
                }
            } catch (IOException | SecurityException e) {
                Slogf.w(LOG_TAG, "Failed requesting data wipe", e);
                if (0 == 0) {
                    SecurityLog.writeEvent(210023, new Object[0]);
                }
            }
        } catch (Throwable th) {
            if (0 == 0) {
                SecurityLog.writeEvent(210023, new Object[0]);
            }
            throw th;
        }
    }

    private void factoryResetIfDelayedEarlier() {
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(0);
            if (policy.mFactoryResetFlags == 0) {
                return;
            }
            if (policy.mFactoryResetReason == null) {
                Slogf.e(LOG_TAG, "no persisted reason for factory resetting");
                policy.mFactoryResetReason = "requested before boot";
            }
            FactoryResetter factoryResetter = FactoryResetter.newBuilder(this.mContext).setReason(policy.mFactoryResetReason).setForce(true).setWipeEuicc((policy.mFactoryResetFlags & 4) != 0).setWipeAdoptableStorage((policy.mFactoryResetFlags & 2) != 0).setWipeFactoryResetProtection((policy.mFactoryResetFlags & 8) != 0).build();
            Slogf.i(LOG_TAG, "Factory resetting on boot using " + factoryResetter);
            try {
                if (!factoryResetter.factoryReset()) {
                    Slogf.wtf(LOG_TAG, "Factory reset using " + factoryResetter + " failed.");
                }
            } catch (IOException e) {
                Slogf.wtf(LOG_TAG, "Could not factory reset using " + factoryResetter, e);
            }
        }
    }

    private void forceWipeUser(int userId, String wipeReasonForUser, boolean wipeSilently) {
        try {
            if (getCurrentForegroundUserId() == userId) {
                this.mInjector.getIActivityManager().switchUser(0);
            }
            boolean success = this.mUserManagerInternal.removeUserEvenWhenDisallowed(userId);
            if (!success) {
                Slogf.w(LOG_TAG, "Couldn't remove user " + userId);
            } else if (isManagedProfile(userId) && !wipeSilently) {
                sendWipeProfileNotification(wipeReasonForUser);
            }
            if (!success) {
                SecurityLog.writeEvent(210023, new Object[0]);
            }
        } catch (RemoteException e) {
            if (0 == 0) {
                SecurityLog.writeEvent(210023, new Object[0]);
            }
        } catch (Throwable th) {
            if (0 == 0) {
                SecurityLog.writeEvent(210023, new Object[0]);
            }
            throw th;
        }
    }

    public void wipeDataWithReason(int flags, String wipeReasonForUser, boolean calledOnParentInstance) {
        ActiveAdmin admin;
        String wipeReasonForUser2;
        int userId;
        ComponentName adminComp;
        String adminName;
        if (!this.mHasFeature && !hasCallingOrSelfPermission("android.permission.MASTER_CLEAR")) {
            return;
        }
        CallerIdentity caller = getCallerIdentity();
        boolean calledByProfileOwnerOnOrgOwnedDevice = isProfileOwnerOfOrganizationOwnedDevice(caller.getUserId());
        if (calledOnParentInstance) {
            Preconditions.checkCallAuthorization(calledByProfileOwnerOnOrgOwnedDevice, "Wiping the entire device can only be done by a profile owner on organization-owned device.");
        }
        if ((flags & 2) != 0) {
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || calledByProfileOwnerOnOrgOwnedDevice || isFinancedDeviceOwner(caller), "Only device owners or profile owners of organization-owned device can set WIPE_RESET_PROTECTION_DATA");
        }
        synchronized (getLockObject()) {
            admin = getActiveAdminWithPolicyForUidLocked(null, 4, caller.getUid());
        }
        Preconditions.checkCallAuthorization(admin != null || hasCallingOrSelfPermission("android.permission.MASTER_CLEAR"), "No active admin for user %d and caller %d does not hold MASTER_CLEAR permission", new Object[]{Integer.valueOf(caller.getUserId()), Integer.valueOf(caller.getUid())});
        checkCanExecuteOrThrowUnsafe(8);
        if (!TextUtils.isEmpty(wipeReasonForUser)) {
            wipeReasonForUser2 = wipeReasonForUser;
        } else {
            wipeReasonForUser2 = getGenericWipeReason(calledByProfileOwnerOnOrgOwnedDevice, calledOnParentInstance);
        }
        int userId2 = admin != null ? admin.getUserHandle().getIdentifier() : caller.getUserId();
        Slogf.i(LOG_TAG, "wipeDataWithReason(%s): admin=%s, user=%d", wipeReasonForUser2, admin, Integer.valueOf(userId2));
        if (calledByProfileOwnerOnOrgOwnedDevice) {
            if (calledOnParentInstance) {
                userId2 = 0;
            } else {
                final UserHandle parentUser = UserHandle.of(getProfileParentId(userId2));
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda142
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3125x2b6f2b2a(parentUser);
                    }
                });
            }
        }
        DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(11).setInt(flags);
        String[] strArr = new String[1];
        strArr[0] = calledOnParentInstance ? CALLED_FROM_PARENT : NOT_CALLED_FROM_PARENT;
        DevicePolicyEventLogger event = devicePolicyEventLogger.setStrings(strArr);
        if (admin != null) {
            ComponentName adminComp2 = admin.info.getComponent();
            String adminName2 = adminComp2.flattenToShortString();
            event.setAdmin(adminComp2);
            userId = userId2;
            adminComp = adminComp2;
            adminName = adminName2;
        } else {
            String adminName3 = this.mInjector.getPackageManager().getPackagesForUid(caller.getUid())[0];
            Slogf.i(LOG_TAG, "Logging wipeData() event admin as " + adminName3);
            event.setAdmin(adminName3);
            if (!this.mInjector.userManagerIsHeadlessSystemUserMode()) {
                userId = userId2;
                adminComp = null;
                adminName = adminName3;
            } else {
                userId = 0;
                adminComp = null;
                adminName = adminName3;
            }
        }
        event.write();
        String internalReason = String.format("DevicePolicyManager.wipeDataWithReason() from %s, organization-owned? %s", adminName, Boolean.valueOf(calledByProfileOwnerOnOrgOwnedDevice));
        wipeDataNoLock(adminComp, flags, internalReason, wipeReasonForUser2, userId);
    }

    private String getGenericWipeReason(boolean calledByProfileOwnerOnOrgOwnedDevice, boolean calledOnParentInstance) {
        if (calledByProfileOwnerOnOrgOwnedDevice && !calledOnParentInstance) {
            return getUpdatableString("Core.WORK_PROFILE_DELETED_ORG_OWNED_MESSAGE", 17040156, new Object[0]);
        }
        return getUpdatableString("Core.WORK_PROFILE_DELETED_GENERIC_MESSAGE", 17041783, new Object[0]);
    }

    private void clearOrgOwnedProfileOwnerDeviceWidePolicies(int parentId) {
        boolean hasSystemUpdatePolicy;
        Slogf.i(LOG_TAG, "Cleaning up device-wide policies left over from org-owned profile...");
        this.mLockPatternUtils.setDeviceOwnerInfo((String) null);
        this.mInjector.settingsGlobalPutInt("wifi_device_owner_configs_lockdown", 0);
        if (this.mInjector.securityLogGetLoggingEnabledProperty()) {
            this.mSecurityLogMonitor.stop();
            this.mInjector.securityLogSetLoggingEnabledProperty(false);
        }
        setNetworkLoggingActiveInternal(false);
        synchronized (getLockObject()) {
            hasSystemUpdatePolicy = this.mOwners.getSystemUpdatePolicy() != null;
            if (hasSystemUpdatePolicy) {
                this.mOwners.clearSystemUpdatePolicy();
                this.mOwners.writeDeviceOwner();
            }
        }
        if (hasSystemUpdatePolicy) {
            this.mContext.sendBroadcastAsUser(new Intent("android.app.action.SYSTEM_UPDATE_POLICY_CHANGED"), UserHandle.SYSTEM);
        }
        suspendPersonalAppsInternal(parentId, false);
        int frpAgentUid = getFrpManagementAgentUid();
        if (frpAgentUid > 0) {
            m3084x10bf7da5(frpAgentUid);
        }
        this.mLockSettingsInternal.refreshStrongAuthTimeout(parentId);
        Slogf.i(LOG_TAG, "Cleaning up device-wide policies done.");
    }

    private void wipeDataNoLock(final ComponentName admin, final int flags, final String internalReason, final String wipeReasonForUser, final int userId) {
        wtfIfInLock();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda87
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3124x153299eb(userId, admin, flags, internalReason, wipeReasonForUser);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$wipeDataNoLock$48$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3124x153299eb(int userId, ComponentName admin, int flags, String internalReason, String wipeReasonForUser) throws Exception {
        String restriction;
        if (userId == 0) {
            restriction = "no_factory_reset";
        } else if (isManagedProfile(userId)) {
            restriction = "no_remove_managed_profile";
        } else {
            restriction = "no_remove_user";
        }
        if (isAdminAffectedByRestriction(admin, restriction, userId)) {
            throw new SecurityException("Cannot wipe data. " + restriction + " restriction is set for user " + userId);
        }
        if (userId == 0) {
            forceWipeDeviceNoLock((flags & 1) != 0, internalReason, (flags & 4) != 0, (flags & 2) != 0);
        } else {
            forceWipeUser(userId, wipeReasonForUser, (flags & 8) != 0);
        }
    }

    private void sendWipeProfileNotification(String wipeReasonForUser) {
        Notification notification = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17301642).setContentTitle(getWorkProfileDeletedTitle()).setContentText(wipeReasonForUser).setColor(this.mContext.getColor(17170460)).setStyle(new Notification.BigTextStyle().bigText(wipeReasonForUser)).build();
        this.mInjector.getNotificationManager().notify(1001, notification);
    }

    private String getWorkProfileDeletedTitle() {
        return getUpdatableString("Core.WORK_PROFILE_DELETED_TITLE", 17041782, new Object[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearWipeProfileNotification() {
        this.mInjector.getNotificationManager().cancel(1001);
    }

    public void setFactoryResetProtectionPolicy(ComponentName who, FactoryResetProtectionPolicy policy) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller));
        checkCanExecuteOrThrowUnsafe(32);
        final int frpManagementAgentUid = getFrpManagementAgentUidOrThrow();
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            admin.mFactoryResetProtectionPolicy = policy;
            saveSettingsLocked(caller.getUserId());
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda80
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3084x10bf7da5(frpManagementAgentUid);
            }
        });
        DevicePolicyEventLogger.createEvent(130).setAdmin(who).write();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: notifyResetProtectionPolicyChanged */
    public void m3084x10bf7da5(int frpManagementAgentUid) {
        Intent intent = new Intent("android.app.action.RESET_PROTECTION_POLICY_CHANGED").addFlags(AudioFormat.EVRCB);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.getUserHandleForUid(frpManagementAgentUid), "android.permission.MANAGE_FACTORY_RESET_PROTECTION");
    }

    public FactoryResetProtectionPolicy getFactoryResetProtectionPolicy(ComponentName who) {
        ActiveAdmin admin;
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity(who);
            int frpManagementAgentUid = getFrpManagementAgentUidOrThrow();
            synchronized (getLockObject()) {
                boolean z = false;
                if (who == null) {
                    if (frpManagementAgentUid == caller.getUid() || hasCallingPermission("android.permission.MASTER_CLEAR")) {
                        z = true;
                    }
                    Preconditions.checkCallAuthorization(z, "Must be called by the FRP management agent on device");
                    admin = getDeviceOwnerOrProfileOwnerOfOrganizationOwnedDeviceLocked(UserHandle.getUserId(frpManagementAgentUid));
                } else {
                    if (isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller)) {
                        z = true;
                    }
                    Preconditions.checkCallAuthorization(z);
                    admin = getProfileOwnerOrDeviceOwnerLocked(caller);
                }
            }
            if (admin != null) {
                return admin.mFactoryResetProtectionPolicy;
            }
            return null;
        }
        return null;
    }

    private int getFrpManagementAgentUid() {
        PersistentDataBlockManagerInternal pdb = this.mInjector.getPersistentDataBlockManagerInternal();
        if (pdb != null) {
            return pdb.getAllowedUid();
        }
        return -1;
    }

    private int getFrpManagementAgentUidOrThrow() {
        int uid = getFrpManagementAgentUid();
        if (uid == -1) {
            throw new UnsupportedOperationException("The persistent data block service is not supported on this device");
        }
        return uid;
    }

    public boolean isFactoryResetProtectionPolicySupported() {
        return getFrpManagementAgentUid() != -1;
    }

    public void sendLostModeLocationUpdate(final AndroidFuture<Boolean> future) {
        if (!this.mHasFeature) {
            future.complete(false);
            return;
        }
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.TRIGGER_LOST_MODE"));
        synchronized (getLockObject()) {
            final ActiveAdmin admin = getDeviceOwnerOrProfileOwnerOfOrganizationOwnedDeviceLocked(0);
            Preconditions.checkState(admin != null, "Lost mode location updates can only be sent on an organization-owned device.");
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda163
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3066xef110ac4(admin, future);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendLostModeLocationUpdate$50$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3066xef110ac4(ActiveAdmin admin, AndroidFuture future) throws Exception {
        String[] providers = {"fused", "network", "gps"};
        tryRetrieveAndSendLocationUpdate(admin, future, providers, 0);
    }

    private void tryRetrieveAndSendLocationUpdate(final ActiveAdmin admin, final AndroidFuture<Boolean> future, final String[] providers, final int index) {
        if (index == providers.length) {
            future.complete(false);
        } else if (this.mInjector.getLocationManager().isProviderEnabled(providers[index])) {
            this.mInjector.getLocationManager().getCurrentLocation(providers[index], null, this.mContext.getMainExecutor(), new Consumer() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda82
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DevicePolicyManagerService.this.m3116x7886bf3c(admin, future, providers, index, (Location) obj);
                }
            });
        } else {
            tryRetrieveAndSendLocationUpdate(admin, future, providers, index + 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$tryRetrieveAndSendLocationUpdate$51$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3116x7886bf3c(ActiveAdmin admin, AndroidFuture future, String[] providers, int index, Location location) {
        if (location != null) {
            this.mContext.sendBroadcastAsUser(newLostModeLocationUpdateIntent(admin, location), admin.getUserHandle());
            future.complete(true);
            return;
        }
        tryRetrieveAndSendLocationUpdate(admin, future, providers, index + 1);
    }

    private Intent newLostModeLocationUpdateIntent(ActiveAdmin admin, Location location) {
        Intent intent = new Intent("android.app.action.LOST_MODE_LOCATION_UPDATE");
        intent.putExtra("android.app.extra.LOST_MODE_LOCATION", location);
        intent.setPackage(admin.info.getPackageName());
        return intent;
    }

    public void getRemoveWarning(ComponentName comp, final RemoteCallback result, int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN"));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(comp, userHandle);
            if (admin == null) {
                result.sendResult((Bundle) null);
                return;
            }
            Intent intent = new Intent("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED");
            intent.setFlags(268435456);
            intent.setComponent(admin.info.getComponent());
            this.mContext.sendOrderedBroadcastAsUser(intent, new UserHandle(userHandle), null, new BroadcastReceiver() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService.6
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent2) {
                    result.sendResult(getResultExtras(false));
                }
            }, null, -1, null, null);
        }
    }

    public void reportPasswordChanged(PasswordMetrics metrics, int userId) {
        if (!this.mHasFeature || !this.mLockPatternUtils.hasSecureLockScreen()) {
            return;
        }
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isSystemUid(caller));
        if (!isSeparateProfileChallengeEnabled(userId)) {
            Preconditions.checkCallAuthorization(!isManagedProfile(userId), "You can not set the active password for a managed profile, userId = %d", new Object[]{Integer.valueOf(userId)});
        }
        DevicePolicyData policy = m3013x8b75536b(userId);
        ArraySet<Integer> affectedUserIds = new ArraySet<>();
        synchronized (getLockObject()) {
            policy.mFailedPasswordAttempts = 0;
            affectedUserIds.add(Integer.valueOf(userId));
            affectedUserIds.addAll(updatePasswordValidityCheckpointLocked(userId, false));
            affectedUserIds.addAll(updatePasswordExpirationsLocked(userId));
            setExpirationAlarmCheckLocked(this.mContext, userId, false);
            sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_CHANGED", 0, userId);
            affectedUserIds.addAll(removeCaApprovalsIfNeeded(userId));
            saveSettingsForUsersLocked(affectedUserIds);
        }
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210036, new Object[]{Integer.valueOf(metrics.determineComplexity()), Integer.valueOf(userId)});
        }
    }

    private Set<Integer> updatePasswordExpirationsLocked(int userHandle) {
        ArraySet<Integer> affectedUserIds = new ArraySet<>();
        List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle);
        for (int i = 0; i < admins.size(); i++) {
            ActiveAdmin admin = admins.get(i);
            if (admin.info.usesPolicy(6)) {
                affectedUserIds.add(Integer.valueOf(admin.getUserHandle().getIdentifier()));
                long timeout = admin.passwordExpirationTimeout;
                admin.passwordExpirationDate = timeout > 0 ? System.currentTimeMillis() + timeout : 0L;
            }
        }
        return affectedUserIds;
    }

    public void reportFailedPasswordAttempt(int userHandle) {
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN"));
        if (!isSeparateProfileChallengeEnabled(userHandle)) {
            Preconditions.checkCallAuthorization(!isManagedProfile(userHandle), "You can not report failed password attempt if separate profile challenge is not in place for a managed profile, userId = %d", new Object[]{Integer.valueOf(userHandle)});
        }
        boolean wipeData = false;
        ActiveAdmin strictestAdmin = null;
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                DevicePolicyData policy = m3013x8b75536b(userHandle);
                policy.mFailedPasswordAttempts++;
                saveSettingsLocked(userHandle);
                if (this.mHasFeature) {
                    strictestAdmin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle, false);
                    int max = strictestAdmin != null ? strictestAdmin.maximumFailedPasswordsForWipe : 0;
                    if (max > 0 && policy.mFailedPasswordAttempts >= max) {
                        wipeData = true;
                    }
                    sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_FAILED", 1, userHandle);
                }
            }
            if (wipeData && strictestAdmin != null) {
                int userId = getUserIdToWipeForFailedPasswords(strictestAdmin);
                Slogf.i(LOG_TAG, "Max failed password attempts policy reached for admin: " + strictestAdmin.info.getComponent().flattenToShortString() + ". Calling wipeData for user " + userId);
                try {
                    wipeDataNoLock(strictestAdmin.info.getComponent(), 0, "reportFailedPasswordAttempt()", getFailedPasswordAttemptWipeMessage(), userId);
                } catch (SecurityException e) {
                    Slogf.w(LOG_TAG, "Failed to wipe user " + userId + " after max failed password attempts reached.", e);
                }
            }
            if (this.mInjector.securityLogIsLoggingEnabled()) {
                SecurityLog.writeEvent(210007, new Object[]{0, 1});
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private String getFailedPasswordAttemptWipeMessage() {
        return getUpdatableString("Core.WORK_PROFILE_DELETED_FAILED_PASSWORD_ATTEMPTS_MESSAGE", 17041785, new Object[0]);
    }

    private int getUserIdToWipeForFailedPasswords(ActiveAdmin admin) {
        int userId = admin.getUserHandle().getIdentifier();
        ComponentName component = admin.info.getComponent();
        return isProfileOwnerOfOrganizationOwnedDevice(component, userId) ? getProfileParentId(userId) : userId;
    }

    public void reportSuccessfulPasswordAttempt(final int userHandle) {
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN"));
        synchronized (getLockObject()) {
            final DevicePolicyData policy = m3013x8b75536b(userHandle);
            if (policy.mFailedPasswordAttempts != 0 || policy.mPasswordOwner >= 0) {
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda94
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3060x8c86067f(policy, userHandle);
                    }
                });
            }
        }
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210007, new Object[]{1, 1});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportSuccessfulPasswordAttempt$52$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3060x8c86067f(DevicePolicyData policy, int userHandle) throws Exception {
        policy.mFailedPasswordAttempts = 0;
        policy.mPasswordOwner = -1;
        saveSettingsLocked(userHandle);
        if (this.mHasFeature) {
            sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_SUCCEEDED", 1, userHandle);
        }
    }

    public void reportFailedBiometricAttempt(int userHandle) {
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN"));
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210007, new Object[]{0, 0});
        }
    }

    public void reportSuccessfulBiometricAttempt(int userHandle) {
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN"));
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210007, new Object[]{1, 0});
        }
    }

    public void reportKeyguardDismissed(int userHandle) {
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN"));
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210006, new Object[0]);
        }
    }

    public void reportKeyguardSecured(int userHandle) {
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN"));
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210008, new Object[0]);
        }
    }

    public ComponentName setGlobalProxy(ComponentName who, String proxySpec, String exclusionList) {
        if (this.mHasFeature) {
            synchronized (getLockObject()) {
                Objects.requireNonNull(who, "ComponentName is null");
                final DevicePolicyData policy = m3013x8b75536b(0);
                ActiveAdmin admin = getActiveAdminForCallerLocked(who, 5);
                Set<ComponentName> compSet = policy.mAdminMap.keySet();
                for (ComponentName component : compSet) {
                    ActiveAdmin ap = policy.mAdminMap.get(component);
                    if (ap.specifiesGlobalProxy && !component.equals(who)) {
                        return component;
                    }
                }
                if (UserHandle.getCallingUserId() != 0) {
                    Slogf.w(LOG_TAG, "Only the owner is allowed to set the global proxy. User " + UserHandle.getCallingUserId() + " is not permitted.");
                    return null;
                }
                if (proxySpec == null) {
                    admin.specifiesGlobalProxy = false;
                    admin.globalProxySpec = null;
                    admin.globalProxyExclusionList = null;
                } else {
                    admin.specifiesGlobalProxy = true;
                    admin.globalProxySpec = proxySpec;
                    admin.globalProxyExclusionList = exclusionList;
                }
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda139
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3085xe47a50dd(policy);
                    }
                });
                return null;
            }
        }
        return null;
    }

    public ComponentName getGlobalProxyAdmin(int userHandle) {
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle) && canQueryAdminPolicy(caller));
            synchronized (getLockObject()) {
                DevicePolicyData policy = m3013x8b75536b(0);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin ap = policy.mAdminList.get(i);
                    if (ap.specifiesGlobalProxy) {
                        return ap.info.getComponent();
                    }
                }
                return null;
            }
        }
        return null;
    }

    public void setRecommendedGlobalProxy(ComponentName who, final ProxyInfo proxyInfo) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        checkAllUsersAreAffiliatedWithDevice();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda64
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3102x3682a4b(proxyInfo);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setRecommendedGlobalProxy$54$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3102x3682a4b(ProxyInfo proxyInfo) throws Exception {
        this.mInjector.getConnectivityManager().setGlobalProxy(proxyInfo);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: resetGlobalProxyLocked */
    public void m3085xe47a50dd(DevicePolicyData policy) {
        int N = policy.mAdminList.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin ap = policy.mAdminList.get(i);
            if (ap.specifiesGlobalProxy) {
                saveGlobalProxyLocked(ap.globalProxySpec, ap.globalProxyExclusionList);
                return;
            }
        }
        saveGlobalProxyLocked(null, null);
    }

    private void saveGlobalProxyLocked(String proxySpec, String exclusionList) {
        if (exclusionList == null) {
            exclusionList = "";
        }
        if (proxySpec == null) {
            proxySpec = "";
        }
        String[] data = proxySpec.trim().split(":");
        int proxyPort = 8080;
        if (data.length > 1) {
            try {
                proxyPort = Integer.parseInt(data[1]);
            } catch (NumberFormatException e) {
            }
        }
        String exclusionList2 = exclusionList.trim();
        ProxyInfo proxyProperties = ProxyInfo.buildDirectProxy(data[0], proxyPort, ProxyUtils.exclusionStringAsList(exclusionList2));
        if (proxyProperties.isValid()) {
            this.mInjector.settingsGlobalPutString("global_http_proxy_host", data[0]);
            this.mInjector.settingsGlobalPutInt("global_http_proxy_port", proxyPort);
            this.mInjector.settingsGlobalPutString("global_http_proxy_exclusion_list", exclusionList2);
            return;
        }
        Slogf.e(LOG_TAG, "Invalid proxy properties, ignoring: " + proxyProperties.toString());
    }

    public int setStorageEncryption(ComponentName who, boolean encrypt) {
        int i;
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            int userHandle = UserHandle.getCallingUserId();
            synchronized (getLockObject()) {
                if (userHandle != 0) {
                    Slogf.w(LOG_TAG, "Only owner/system user is allowed to set storage encryption. User " + UserHandle.getCallingUserId() + " is not permitted.");
                    return 0;
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 7);
                if (isEncryptionSupported()) {
                    if (ap.encryptionRequested != encrypt) {
                        ap.encryptionRequested = encrypt;
                        saveSettingsLocked(userHandle);
                    }
                    DevicePolicyData policy = m3013x8b75536b(0);
                    boolean newRequested = false;
                    int N = policy.mAdminList.size();
                    for (int i2 = 0; i2 < N; i2++) {
                        newRequested |= policy.mAdminList.get(i2).encryptionRequested;
                    }
                    setEncryptionRequested(newRequested);
                    if (newRequested) {
                        i = 3;
                    } else {
                        i = 1;
                    }
                    return i;
                }
                return 0;
            }
        }
        return 0;
    }

    public boolean getStorageEncryption(ComponentName who, int userHandle) {
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            synchronized (getLockObject()) {
                if (caller.hasAdminComponent()) {
                    ActiveAdmin ap = getActiveAdminUncheckedLocked(who, userHandle);
                    return ap != null ? ap.encryptionRequested : false;
                }
                DevicePolicyData policy = m3013x8b75536b(userHandle);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    if (policy.mAdminList.get(i).encryptionRequested) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    public int getStorageEncryptionStatus(String callerPackage, int userHandle) {
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity(callerPackage);
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(callerPackage, 0L, userHandle);
            boolean legacyApp = false;
            if (ai.targetSdkVersion <= 23) {
                legacyApp = true;
            }
            int rawStatus = getEncryptionStatus();
            if (rawStatus == 5 && legacyApp) {
                return 3;
            }
            return rawStatus;
        } catch (RemoteException e) {
            throw new SecurityException(e);
        }
    }

    private boolean isEncryptionSupported() {
        return getEncryptionStatus() != 0;
    }

    private int getEncryptionStatus() {
        if (this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
            return 5;
        }
        return 0;
    }

    private void setEncryptionRequested(boolean encrypt) {
    }

    public void setScreenCaptureDisabled(ComponentName who, boolean disabled, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        if (parent) {
            Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
        }
        synchronized (getLockObject()) {
            ActiveAdmin ap = getParentOfAdminIfRequired(getProfileOwnerOrDeviceOwnerLocked(caller), parent);
            if (ap.disableScreenCapture != disabled) {
                ap.disableScreenCapture = disabled;
                saveSettingsLocked(caller.getUserId());
                pushScreenCapturePolicy(caller.getUserId());
            }
        }
        DevicePolicyEventLogger.createEvent(29).setAdmin(caller.getComponentName()).setBoolean(disabled).write();
    }

    private void pushScreenCapturePolicy(int adminUserId) {
        ActiveAdmin admin = getDeviceOwnerOrProfileOwnerOfOrganizationOwnedDeviceParentLocked(0);
        if (admin != null && admin.disableScreenCapture) {
            setScreenCaptureDisabled(-1);
            return;
        }
        ActiveAdmin admin2 = getProfileOwnerAdminLocked(adminUserId);
        if (admin2 != null && admin2.disableScreenCapture) {
            setScreenCaptureDisabled(adminUserId);
        } else {
            setScreenCaptureDisabled(-10000);
        }
    }

    private void setScreenCaptureDisabled(int userHandle) {
        int current = this.mPolicyCache.getScreenCaptureDisallowedUser();
        if (userHandle == current) {
            return;
        }
        this.mPolicyCache.setScreenCaptureDisallowedUser(userHandle);
        updateScreenCaptureDisabled();
    }

    public boolean getScreenCaptureDisabled(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return false;
        }
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        if (parent) {
            Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(getCallerIdentity().getUserId()));
        }
        return !this.mPolicyCache.isScreenCaptureAllowed(userHandle);
    }

    private void updateScreenCaptureDisabled() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda149
            @Override // java.lang.Runnable
            public final void run() {
                DevicePolicyManagerService.this.m3122x7f36e205();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateScreenCaptureDisabled$55$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3122x7f36e205() {
        try {
            this.mInjector.getIWindowManager().refreshScreenCaptureDisabled();
        } catch (RemoteException e) {
            Slogf.w(LOG_TAG, "Unable to notify WindowManager.", e);
        }
    }

    public void setNearbyNotificationStreamingPolicy(int policy) {
        if (!this.mHasFeature) {
            return;
        }
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            if (admin.mNearbyNotificationStreamingPolicy != policy) {
                admin.mNearbyNotificationStreamingPolicy = policy;
                saveSettingsLocked(caller.getUserId());
            }
        }
    }

    public int getNearbyNotificationStreamingPolicy(int userId) {
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller) || hasCallingOrSelfPermission("android.permission.READ_NEARBY_STREAMING_POLICY"));
            synchronized (getLockObject()) {
                if (!this.mOwners.hasProfileOwner(userId) && !this.mOwners.hasDeviceOwner()) {
                    return 0;
                }
                ActiveAdmin admin = getDeviceOrProfileOwnerAdminLocked(userId);
                return admin.mNearbyNotificationStreamingPolicy;
            }
        }
        return 0;
    }

    public void setNearbyAppStreamingPolicy(int policy) {
        if (!this.mHasFeature) {
            return;
        }
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            if (admin.mNearbyAppStreamingPolicy != policy) {
                admin.mNearbyAppStreamingPolicy = policy;
                saveSettingsLocked(caller.getUserId());
            }
        }
    }

    public int getNearbyAppStreamingPolicy(int userId) {
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller) || hasCallingOrSelfPermission("android.permission.READ_NEARBY_STREAMING_POLICY"));
            synchronized (getLockObject()) {
                if (!this.mOwners.hasProfileOwner(userId) && !this.mOwners.hasDeviceOwner()) {
                    return 0;
                }
                ActiveAdmin admin = getDeviceOrProfileOwnerAdminLocked(userId);
                return admin.mNearbyAppStreamingPolicy;
            }
        }
        return 0;
    }

    public void setAutoTimeRequired(ComponentName who, boolean required) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        boolean requireAutoTimeChanged = false;
        synchronized (getLockObject()) {
            Preconditions.checkCallAuthorization(!isManagedProfile(caller.getUserId()), "Managed profile cannot set auto time required");
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            if (admin.requireAutoTime != required) {
                admin.requireAutoTime = required;
                saveSettingsLocked(caller.getUserId());
                requireAutoTimeChanged = true;
            }
        }
        if (requireAutoTimeChanged) {
            pushUserRestrictions(caller.getUserId());
        }
        if (required) {
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda1
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3073x86c72934();
                }
            });
        }
        DevicePolicyEventLogger.createEvent(36).setAdmin(who).setBoolean(required).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setAutoTimeRequired$56$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3073x86c72934() throws Exception {
        this.mInjector.settingsGlobalPutInt("auto_time", 1);
    }

    public boolean getAutoTimeRequired() {
        if (this.mHasFeature) {
            synchronized (getLockObject()) {
                ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
                if (deviceOwner != null && deviceOwner.requireAutoTime) {
                    return true;
                }
                for (Integer userId : this.mOwners.getProfileOwnerKeys()) {
                    ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userId.intValue());
                    if (profileOwner != null && profileOwner.requireAutoTime) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    public void setAutoTimeEnabled(ComponentName who, final boolean enabled) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwnerOnUser0(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller) || isDefaultDeviceOwner(caller));
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda129
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3072x5b2f4153(enabled);
            }
        });
        DevicePolicyEventLogger.createEvent((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME).setAdmin(caller.getComponentName()).setBoolean(enabled).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setAutoTimeEnabled$57$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3072x5b2f4153(boolean enabled) throws Exception {
        this.mInjector.settingsGlobalPutInt("auto_time", enabled ? 1 : 0);
    }

    public boolean getAutoTimeEnabled(ComponentName who) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(isProfileOwnerOnUser0(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller) || isDefaultDeviceOwner(caller));
            return this.mInjector.settingsGlobalGetInt("auto_time", 0) > 0;
        }
        return false;
    }

    public void setAutoTimeZoneEnabled(ComponentName who, final boolean enabled) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwnerOnUser0(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller) || isDefaultDeviceOwner(caller));
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda154
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3074x43d58ce6(enabled);
            }
        });
        DevicePolicyEventLogger.createEvent(128).setAdmin(caller.getComponentName()).setBoolean(enabled).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setAutoTimeZoneEnabled$58$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3074x43d58ce6(boolean enabled) throws Exception {
        this.mInjector.settingsGlobalPutInt("auto_time_zone", enabled ? 1 : 0);
    }

    public boolean getAutoTimeZoneEnabled(ComponentName who) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(isProfileOwnerOnUser0(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller) || isDefaultDeviceOwner(caller));
            return this.mInjector.settingsGlobalGetInt("auto_time_zone", 0) > 0;
        }
        return false;
    }

    public void setForceEphemeralUsers(ComponentName who, boolean forceEphemeralUsers) {
        throw new UnsupportedOperationException("This method was used by split system user only.");
    }

    public boolean getForceEphemeralUsers(ComponentName who) {
        throw new UnsupportedOperationException("This method was used by split system user only.");
    }

    public boolean requestBugreport(ComponentName who) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
            checkAllUsersAreAffiliatedWithDevice();
            checkCanExecuteOrThrowUnsafe(29);
            if (this.mBugreportCollectionManager.requestBugreport()) {
                DevicePolicyEventLogger.createEvent(53).setAdmin(who).write();
                long currentTime = System.currentTimeMillis();
                synchronized (getLockObject()) {
                    DevicePolicyData policyData = m3013x8b75536b(0);
                    if (currentTime > policyData.mLastBugReportRequestTime) {
                        policyData.mLastBugReportRequestTime = currentTime;
                        saveSettingsLocked(0);
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendDeviceOwnerCommand(String action, Bundle extras) {
        int deviceOwnerUserId;
        ComponentName receiverComponent;
        synchronized (getLockObject()) {
            deviceOwnerUserId = this.mOwners.getDeviceOwnerUserId();
            receiverComponent = this.mOwners.getDeviceOwnerComponent();
        }
        sendActiveAdminCommand(action, extras, deviceOwnerUserId, receiverComponent, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendDeviceOwnerOrProfileOwnerCommand(String action, Bundle extras, int userId) {
        boolean inForeground;
        ComponentName receiverComponent;
        if (userId == -1) {
            userId = 0;
        }
        boolean inForeground2 = false;
        ComponentName receiverComponent2 = null;
        if (action.equals("android.app.action.NETWORK_LOGS_AVAILABLE")) {
            inForeground2 = true;
            receiverComponent2 = resolveDelegateReceiver("delegation-network-logging", action, userId);
        }
        if (!action.equals("android.app.action.SECURITY_LOGS_AVAILABLE")) {
            inForeground = inForeground2;
        } else {
            receiverComponent2 = resolveDelegateReceiver("delegation-security-logging", action, userId);
            inForeground = true;
        }
        if (receiverComponent2 != null) {
            receiverComponent = receiverComponent2;
        } else {
            ComponentName receiverComponent3 = getOwnerComponent(userId);
            receiverComponent = receiverComponent3;
        }
        sendActiveAdminCommand(action, extras, userId, receiverComponent, inForeground);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendProfileOwnerCommand(String action, Bundle extras, int userId) {
        sendActiveAdminCommand(action, extras, userId, this.mOwners.getProfileOwnerComponent(userId), false);
    }

    private void sendActiveAdminCommand(String action, Bundle extras, int userId, ComponentName receiverComponent, boolean inForeground) {
        Intent intent = new Intent(action);
        intent.setComponent(receiverComponent);
        if (extras != null) {
            intent.putExtras(extras);
        }
        if (inForeground) {
            intent.addFlags(268435456);
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
    }

    private void sendOwnerChangedBroadcast(String broadcast, int userId) {
        Intent intent = new Intent(broadcast).addFlags(16777216);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendBugreportToDeviceOwner(Uri bugreportUri, String bugreportHash) {
        synchronized (getLockObject()) {
            Intent intent = new Intent("android.app.action.BUGREPORT_SHARE");
            intent.setComponent(this.mOwners.getDeviceOwnerComponent());
            intent.setDataAndType(bugreportUri, "application/vnd.android.bugreport");
            intent.putExtra("android.app.extra.BUGREPORT_HASH", bugreportHash);
            intent.setFlags(1);
            UriGrantsManagerInternal ugm = (UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class);
            NeededUriGrants needed = ugm.checkGrantUriPermissionFromIntent(intent, 2000, this.mOwners.getDeviceOwnerComponent().getPackageName(), this.mOwners.getDeviceOwnerUserId());
            ugm.grantUriPermissionUncheckedFromIntent(needed, null);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.of(this.mOwners.getDeviceOwnerUserId()));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceOwnerRemoteBugreportUriAndHash(String bugreportUri, String bugreportHash) {
        synchronized (getLockObject()) {
            this.mOwners.setDeviceOwnerRemoteBugreportUriAndHash(bugreportUri, bugreportHash);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Pair<String, String> getDeviceOwnerRemoteBugreportUriAndHash() {
        Pair<String, String> pair;
        synchronized (getLockObject()) {
            String uri = this.mOwners.getDeviceOwnerRemoteBugreportUri();
            pair = uri == null ? null : new Pair<>(uri, this.mOwners.getDeviceOwnerRemoteBugreportHash());
        }
        return pair;
    }

    public void setCameraDisabled(ComponentName who, boolean disabled, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        if (parent) {
            Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
        }
        checkCanExecuteOrThrowUnsafe(31);
        int userHandle = caller.getUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 8, parent);
            if (ap.disableCamera != disabled) {
                ap.disableCamera = disabled;
                saveSettingsLocked(userHandle);
            }
        }
        pushUserRestrictions(userHandle);
        int affectedUserId = parent ? getProfileParentId(userHandle) : userHandle;
        if (SecurityLog.isLoggingEnabled()) {
            SecurityLog.writeEvent(210034, new Object[]{who.getPackageName(), Integer.valueOf(userHandle), Integer.valueOf(affectedUserId), Integer.valueOf(disabled ? 1 : 0)});
        }
        DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(30).setAdmin(caller.getComponentName()).setBoolean(disabled);
        String[] strArr = new String[1];
        strArr[0] = parent ? CALLED_FROM_PARENT : NOT_CALLED_FROM_PARENT;
        devicePolicyEventLogger.setStrings(strArr).write();
    }

    public boolean getCameraDisabled(ComponentName who, int userHandle, boolean parent) {
        boolean z = false;
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            if (parent) {
                Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller.getUserId()));
            }
            synchronized (getLockObject()) {
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    if (admin != null && admin.disableCamera) {
                        z = true;
                    }
                    return z;
                }
                ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
                if (deviceOwner != null && deviceOwner.disableCamera) {
                    return true;
                }
                int affectedUserId = parent ? getProfileParentId(userHandle) : userHandle;
                List<ActiveAdmin> admins = getActiveAdminsForAffectedUserLocked(affectedUserId);
                for (ActiveAdmin admin2 : admins) {
                    if (admin2.disableCamera) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    public void setKeyguardDisabledFeatures(ComponentName who, int which, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        int userHandle = caller.getUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 9, parent);
            if (isManagedProfile(userHandle)) {
                if (parent) {
                    if (isProfileOwnerOfOrganizationOwnedDevice(caller)) {
                        which &= 438;
                    } else {
                        which &= FrameworkStatsLog.HOTWORD_DETECTION_SERVICE_RESTARTED;
                    }
                } else {
                    which &= PROFILE_KEYGUARD_FEATURES;
                }
            }
            if (ap.disabledKeyguardFeatures != which) {
                ap.disabledKeyguardFeatures = which;
                saveSettingsLocked(userHandle);
            }
        }
        if (SecurityLog.isLoggingEnabled()) {
            int affectedUserId = parent ? getProfileParentId(userHandle) : userHandle;
            SecurityLog.writeEvent(210021, new Object[]{who.getPackageName(), Integer.valueOf(userHandle), Integer.valueOf(affectedUserId), Integer.valueOf(which)});
        }
        DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(9).setAdmin(caller.getComponentName()).setInt(which);
        String[] strArr = new String[1];
        strArr[0] = parent ? CALLED_FROM_PARENT : NOT_CALLED_FROM_PARENT;
        devicePolicyEventLogger.setStrings(strArr).write();
    }

    public int getKeyguardDisabledFeatures(ComponentName who, int userHandle, boolean parent) {
        List<ActiveAdmin> admins;
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            Preconditions.checkCallAuthorization(who == null || isCallingFromPackage(who.getPackageName(), caller.getUid()) || isSystemUid(caller));
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                synchronized (getLockObject()) {
                    if (who != null) {
                        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                        return admin != null ? admin.disabledKeyguardFeatures : 0;
                    }
                    if (!parent && isManagedProfile(userHandle)) {
                        admins = getUserDataUnchecked(userHandle).mAdminList;
                    } else {
                        admins = getActiveAdminsForLockscreenPoliciesLocked(getProfileParentUserIfRequested(userHandle, parent));
                    }
                    int which = 0;
                    int N = admins.size();
                    for (int i = 0; i < N; i++) {
                        ActiveAdmin admin2 = admins.get(i);
                        int userId = admin2.getUserHandle().getIdentifier();
                        boolean isRequestedUser = !parent && userId == userHandle;
                        if (!isRequestedUser && isManagedProfile(userId)) {
                            which = (admin2.disabledKeyguardFeatures & 438) | which;
                        }
                        int which2 = admin2.disabledKeyguardFeatures;
                        which = which2 | which;
                    }
                    return which;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
        return 0;
    }

    public void setKeepUninstalledPackages(ComponentName who, String callerPackage, List<String> packageList) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(packageList, "packageList is null");
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && isDefaultDeviceOwner(caller)) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-keep-uninstalled-packages")));
        checkCanExecuteOrThrowUnsafe(17);
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            deviceOwner.keepUninstalledPackages = packageList;
            saveSettingsLocked(caller.getUserId());
            this.mInjector.getPackageManagerInternal().setKeepUninstalledPackages(packageList);
        }
        DevicePolicyEventLogger.createEvent(61).setAdmin(caller.getPackageName()).setBoolean(who == null).setStrings((String[]) packageList.toArray(new String[0])).write();
    }

    public List<String> getKeepUninstalledPackages(ComponentName who, String callerPackage) {
        List<String> keepUninstalledPackagesLocked;
        if (!this.mHasFeature) {
            return null;
        }
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && isDefaultDeviceOwner(caller)) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-keep-uninstalled-packages")));
        synchronized (getLockObject()) {
            keepUninstalledPackagesLocked = getKeepUninstalledPackagesLocked();
        }
        return keepUninstalledPackagesLocked;
    }

    private List<String> getKeepUninstalledPackagesLocked() {
        ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
        if (deviceOwner != null) {
            return deviceOwner.keepUninstalledPackages;
        }
        return null;
    }

    private void logMissingFeatureAction(String message) {
        Slogf.w(LOG_TAG, message + " because device does not have the android.software.device_admin feature.");
    }

    public boolean setDeviceOwner(ComponentName admin, String ownerName, final int userId, boolean setProfileOwnerOnCurrentUserIfNecessary) {
        int currentForegroundUser;
        if (!this.mHasFeature) {
            logMissingFeatureAction("Cannot set " + ComponentName.flattenToShortString(admin) + " as device owner for user " + userId);
            return false;
        }
        Preconditions.checkArgument(admin != null);
        CallerIdentity caller = getCallerIdentity();
        boolean hasIncompatibleAccountsOrNonAdb = hasIncompatibleAccountsOrNonAdbNoLock(caller, userId, admin);
        synchronized (getLockObject()) {
            enforceCanSetDeviceOwnerLocked(caller, admin, userId, hasIncompatibleAccountsOrNonAdb);
            Preconditions.checkArgument(isPackageInstalledForUser(admin.getPackageName(), userId), "Invalid component " + admin + " for device owner");
            ActiveAdmin activeAdmin = getActiveAdminUncheckedLocked(admin, userId);
            Preconditions.checkArgument((activeAdmin == null || m3013x8b75536b(userId).mRemovingAdmins.contains(admin)) ? false : true, "Not active admin: " + admin);
            toggleBackupServiceActive(0, false);
            if (isAdb(caller)) {
                MetricsLogger.action(this.mContext, 617, LOG_TAG_DEVICE_OWNER);
                DevicePolicyEventLogger.createEvent(82).setAdmin(admin).setStrings(new String[]{LOG_TAG_DEVICE_OWNER}).write();
            }
            this.mOwners.setDeviceOwner(admin, ownerName, userId);
            this.mOwners.writeDeviceOwner();
            setDeviceOwnershipSystemPropertyLocked();
            if (isAdb(caller)) {
                activeAdmin.mAdminCanGrantSensorsPermissions = true;
                this.mPolicyCache.setAdminCanGrantSensorsPermissions(userId, true);
                saveSettingsLocked(userId);
            }
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda120
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3078x62a4ff09(userId);
                }
            });
            this.mDeviceAdminServiceController.startServiceForOwner(admin.getPackageName(), userId, "set-device-owner");
            Slogf.i(LOG_TAG, "Device owner set: " + admin + " on user " + userId);
        }
        if (setProfileOwnerOnCurrentUserIfNecessary && this.mInjector.userManagerIsHeadlessSystemUserMode()) {
            synchronized (getLockObject()) {
                currentForegroundUser = getCurrentForegroundUserId();
            }
            Slogf.i(LOG_TAG, "setDeviceOwner(): setting " + admin + " as profile owner on user " + currentForegroundUser);
            manageUserUnchecked(admin, admin, currentForegroundUser, null, false);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDeviceOwner$59$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3078x62a4ff09(int userId) throws Exception {
        this.mUserManager.setUserRestriction("no_add_managed_profile", true, UserHandle.of(userId));
        this.mUserManager.setUserRestriction("no_add_clone_profile", true, UserHandle.of(userId));
        sendOwnerChangedBroadcast("android.app.action.DEVICE_OWNER_CHANGED", userId);
    }

    public boolean hasDeviceOwner() {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || canManageUsers(caller) || isFinancedDeviceOwner(caller) || hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        return this.mOwners.hasDeviceOwner();
    }

    boolean isDeviceOwner(ActiveAdmin admin) {
        return isDeviceOwner(admin.info.getComponent(), admin.getUserHandle().getIdentifier());
    }

    public boolean isDeviceOwner(ComponentName who, int userId) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == userId && this.mOwners.getDeviceOwnerComponent().equals(who);
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDefaultDeviceOwner(CallerIdentity caller) {
        boolean z;
        synchronized (getLockObject()) {
            z = isDeviceOwnerLocked(caller) && (getDeviceOwnerTypeLocked(this.mOwners.getDeviceOwnerPackageName()) == 0 || IDevicePolicyManagerServiceLice.Instance().isDefaultDeviceOwner(this.mOwners.getDeviceOwnerPackageName()));
        }
        return z;
    }

    private boolean isDeviceOwnerLocked(CallerIdentity caller) {
        if (!this.mOwners.hasDeviceOwner() || this.mOwners.getDeviceOwnerUserId() != caller.getUserId()) {
            return false;
        }
        if (caller.hasAdminComponent()) {
            return this.mOwners.getDeviceOwnerComponent().equals(caller.getComponentName());
        }
        return isUidDeviceOwnerLocked(caller.getUid());
    }

    private boolean isDeviceOwnerUserId(int userId) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.getDeviceOwnerComponent() != null && this.mOwners.getDeviceOwnerUserId() == userId;
        }
        return z;
    }

    private boolean isDeviceOwnerPackage(String packageName, int userId) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == userId && this.mOwners.getDeviceOwnerPackageName().equals(packageName);
        }
        return z;
    }

    private boolean isProfileOwnerPackage(String packageName, int userId) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.hasProfileOwner(userId) && this.mOwners.getProfileOwnerPackage(userId).equals(packageName);
        }
        return z;
    }

    public boolean isProfileOwner(ComponentName who, final int userId) {
        ComponentName profileOwner = (ComponentName) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda144
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3036x91e56c0c(userId);
            }
        });
        return who != null && who.equals(profileOwner);
    }

    public boolean isProfileOwner(final CallerIdentity caller) {
        synchronized (getLockObject()) {
            ComponentName profileOwner = (ComponentName) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda47
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3037x97e9376b(caller);
                }
            });
            if (profileOwner == null) {
                return false;
            }
            if (caller.hasAdminComponent()) {
                return profileOwner.equals(caller.getComponentName());
            }
            return isUidProfileOwnerLocked(caller.getUid());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isProfileOwner$61$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ ComponentName m3037x97e9376b(CallerIdentity caller) throws Exception {
        return m3036x91e56c0c(caller.getUserId());
    }

    private boolean isUidProfileOwnerLocked(int appUid) {
        ensureLocked();
        int userId = UserHandle.getUserId(appUid);
        ComponentName profileOwnerComponent = this.mOwners.getProfileOwnerComponent(userId);
        if (profileOwnerComponent == null) {
            return false;
        }
        Iterator<ActiveAdmin> it = m3013x8b75536b(userId).mAdminList.iterator();
        while (it.hasNext()) {
            ActiveAdmin admin = it.next();
            ComponentName currentAdminComponent = admin.info.getComponent();
            if (admin.getUid() == appUid && profileOwnerComponent.equals(currentAdminComponent)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasProfileOwner(int userId) {
        boolean hasProfileOwner;
        synchronized (getLockObject()) {
            hasProfileOwner = this.mOwners.hasProfileOwner(userId);
        }
        return hasProfileOwner;
    }

    private boolean isProfileOwnerOfOrganizationOwnedDevice(CallerIdentity caller) {
        return isProfileOwner(caller) && isProfileOwnerOfOrganizationOwnedDevice(caller.getUserId());
    }

    private boolean isProfileOwnerOfOrganizationOwnedDevice(int userId) {
        boolean isProfileOwnerOfOrganizationOwnedDevice;
        synchronized (getLockObject()) {
            isProfileOwnerOfOrganizationOwnedDevice = this.mOwners.isProfileOwnerOfOrganizationOwnedDevice(userId);
        }
        return isProfileOwnerOfOrganizationOwnedDevice;
    }

    private boolean isProfileOwnerOfOrganizationOwnedDevice(ComponentName who, int userId) {
        return isProfileOwner(who, userId) && isProfileOwnerOfOrganizationOwnedDevice(userId);
    }

    private boolean isProfileOwnerOnUser0(CallerIdentity caller) {
        return isProfileOwner(caller) && caller.getUserHandle().isSystem();
    }

    private boolean isPackage(CallerIdentity caller, String packageName) {
        return isCallingFromPackage(packageName, caller.getUid());
    }

    public ComponentName getDeviceOwnerComponent(boolean callingUserOnly) {
        if (this.mHasFeature) {
            if (!callingUserOnly) {
                Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
            }
            synchronized (getLockObject()) {
                if (this.mOwners.hasDeviceOwner()) {
                    if (!callingUserOnly || this.mInjector.userHandleGetCallingUserId() == this.mOwners.getDeviceOwnerUserId()) {
                        return this.mOwners.getDeviceOwnerComponent();
                    }
                    return null;
                }
                return null;
            }
        }
        return null;
    }

    private int getDeviceOwnerUserIdUncheckedLocked() {
        if (this.mOwners.hasDeviceOwner()) {
            return this.mOwners.getDeviceOwnerUserId();
        }
        return -10000;
    }

    public int getDeviceOwnerUserId() {
        int deviceOwnerUserIdUncheckedLocked;
        if (!this.mHasFeature) {
            return -10000;
        }
        Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()));
        synchronized (getLockObject()) {
            deviceOwnerUserIdUncheckedLocked = getDeviceOwnerUserIdUncheckedLocked();
        }
        return deviceOwnerUserIdUncheckedLocked;
    }

    public String getDeviceOwnerName() {
        if (this.mHasFeature) {
            Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
            synchronized (getLockObject()) {
                if (this.mOwners.hasDeviceOwner()) {
                    String deviceOwnerPackage = this.mOwners.getDeviceOwnerPackageName();
                    return getApplicationLabel(deviceOwnerPackage, 0);
                }
                return null;
            }
        }
        return null;
    }

    ActiveAdmin getDeviceOwnerAdminLocked() {
        ensureLocked();
        ComponentName component = this.mOwners.getDeviceOwnerComponent();
        if (component == null) {
            return null;
        }
        DevicePolicyData policy = m3013x8b75536b(this.mOwners.getDeviceOwnerUserId());
        int n = policy.mAdminList.size();
        for (int i = 0; i < n; i++) {
            ActiveAdmin admin = policy.mAdminList.get(i);
            if (component.equals(admin.info.getComponent())) {
                return admin;
            }
        }
        Slogf.wtf(LOG_TAG, "Active admin for device owner not found. component=" + component);
        return null;
    }

    ActiveAdmin getDeviceOwnerOrProfileOwnerOfOrganizationOwnedDeviceLocked(int userId) {
        ensureLocked();
        ActiveAdmin admin = getDeviceOwnerAdminLocked();
        if (admin == null) {
            return getProfileOwnerOfOrganizationOwnedDeviceLocked(userId);
        }
        return admin;
    }

    ActiveAdmin getDeviceOwnerOrProfileOwnerOfOrganizationOwnedDeviceParentLocked(int userId) {
        ensureLocked();
        ActiveAdmin admin = getDeviceOwnerAdminLocked();
        if (admin != null) {
            return admin;
        }
        ActiveAdmin admin2 = getProfileOwnerOfOrganizationOwnedDeviceLocked(userId);
        if (admin2 != null) {
            return admin2.getParentActiveAdmin();
        }
        return null;
    }

    public void clearDeviceOwner(String packageName) {
        Objects.requireNonNull(packageName, "packageName is null");
        boolean ignorePermCheck = ITranDevicePolicyManagerService.Instance().clearDeviceOwnerPermissionCheck(packageName, this.mInjector.binderGetCallingUid());
        CallerIdentity caller = ignorePermCheck ? getCallerIdentity() : getCallerIdentity(packageName);
        synchronized (getLockObject()) {
            final ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
            final int deviceOwnerUserId = this.mOwners.getDeviceOwnerUserId();
            if (!this.mOwners.hasDeviceOwner() || ((!ignorePermCheck && !deviceOwnerComponent.getPackageName().equals(packageName)) || deviceOwnerUserId != caller.getUserId())) {
                throw new SecurityException("clearDeviceOwner can only be called by the device owner");
            }
            enforceUserUnlocked(deviceOwnerUserId);
            DevicePolicyData policy = m3013x8b75536b(deviceOwnerUserId);
            if (policy.mPasswordTokenHandle != 0) {
                this.mLockPatternUtils.removeEscrowToken(policy.mPasswordTokenHandle, deviceOwnerUserId);
            }
            final ActiveAdmin admin = getDeviceOwnerAdminLocked();
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda141
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m2984xe7756b26(admin, deviceOwnerUserId, deviceOwnerComponent);
                }
            });
            Slogf.i(LOG_TAG, "Device owner removed: " + deviceOwnerComponent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearDeviceOwner$62$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m2984xe7756b26(ActiveAdmin admin, int deviceOwnerUserId, ComponentName deviceOwnerComponent) throws Exception {
        clearDeviceOwnerLocked(admin, deviceOwnerUserId);
        m3056x58c4fc7d(deviceOwnerComponent, deviceOwnerUserId);
        sendOwnerChangedBroadcast("android.app.action.DEVICE_OWNER_CHANGED", deviceOwnerUserId);
    }

    private void clearOverrideApnUnchecked() {
        if (!this.mHasTelephonyFeature) {
            return;
        }
        setOverrideApnsEnabledUnchecked(false);
        List<ApnSetting> apns = getOverrideApnsUnchecked();
        for (int i = 0; i < apns.size(); i++) {
            removeOverrideApnUnchecked(apns.get(i).getId());
        }
    }

    private void clearManagedProfileApnUnchecked() {
        if (!this.mHasTelephonyFeature) {
            return;
        }
        List<ApnSetting> apns = getOverrideApnsUnchecked();
        for (ApnSetting apn : apns) {
            if (apn.getApnTypeBitmask() == 16384) {
                removeOverrideApnUnchecked(apn.getId());
            }
        }
    }

    private void clearDeviceOwnerLocked(ActiveAdmin admin, int userId) {
        this.mDeviceAdminServiceController.stopServiceForOwner(userId, "clear-device-owner");
        if (admin != null) {
            admin.disableCamera = false;
            admin.userRestrictions = null;
            admin.defaultEnabledRestrictionsAlreadySet.clear();
            admin.forceEphemeralUsers = false;
            admin.isNetworkLoggingEnabled = false;
            admin.requireAutoTime = false;
            this.mUserManagerInternal.setForceEphemeralUsers(admin.forceEphemeralUsers);
        }
        DevicePolicyData policyData = m3013x8b75536b(userId);
        policyData.mCurrentInputMethodSet = false;
        saveSettingsLocked(userId);
        this.mPolicyCache.onUserRemoved(userId);
        DevicePolicyData systemPolicyData = m3013x8b75536b(0);
        systemPolicyData.mLastSecurityLogRetrievalTime = -1L;
        systemPolicyData.mLastBugReportRequestTime = -1L;
        systemPolicyData.mLastNetworkLogsRetrievalTime = -1L;
        saveSettingsLocked(0);
        clearUserPoliciesLocked(userId);
        clearOverrideApnUnchecked();
        clearApplicationRestrictions(userId);
        this.mInjector.getPackageManagerInternal().clearBlockUninstallForUser(userId);
        this.mOwners.clearDeviceOwner();
        this.mOwners.writeDeviceOwner();
        clearDeviceOwnerUserRestriction(UserHandle.of(userId));
        this.mInjector.securityLogSetLoggingEnabledProperty(false);
        this.mSecurityLogMonitor.stop();
        setNetworkLoggingActiveInternal(false);
        deleteTransferOwnershipBundleLocked(userId);
        toggleBackupServiceActive(0, true);
        pushUserControlDisabledPackagesLocked(userId);
        setGlobalSettingDeviceOwnerType(0);
    }

    private void clearApplicationRestrictions(final int userId) {
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda119
            @Override // java.lang.Runnable
            public final void run() {
                DevicePolicyManagerService.this.m2983x478c5a9f(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearApplicationRestrictions$63$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m2983x478c5a9f(int userId) {
        List<PackageInfo> installedPackageInfos = this.mInjector.getPackageManager(userId).getInstalledPackages(786432);
        UserHandle userHandle = UserHandle.of(userId);
        for (PackageInfo packageInfo : installedPackageInfos) {
            this.mInjector.getUserManager().setApplicationRestrictions(packageInfo.packageName, null, userHandle);
        }
    }

    public boolean setProfileOwner(ComponentName who, String ownerName, final int userHandle) {
        if (!this.mHasFeature) {
            logMissingFeatureAction("Cannot set " + ComponentName.flattenToShortString(who) + " as profile owner for user " + userHandle);
            return false;
        }
        Preconditions.checkArgument(who != null);
        CallerIdentity caller = getCallerIdentity();
        boolean hasIncompatibleAccountsOrNonAdb = hasIncompatibleAccountsOrNonAdbNoLock(caller, userHandle, who);
        synchronized (getLockObject()) {
            enforceCanSetProfileOwnerLocked(caller, who, userHandle, hasIncompatibleAccountsOrNonAdb);
            final ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
            Preconditions.checkArgument((!isPackageInstalledForUser(who.getPackageName(), userHandle) || admin == null || m3013x8b75536b(userHandle).mRemovingAdmins.contains(who)) ? false : true, "Not active admin: " + who);
            int parentUserId = getProfileParentId(userHandle);
            if (parentUserId != userHandle && this.mUserManager.hasUserRestriction("no_add_managed_profile", UserHandle.of(parentUserId))) {
                Slogf.i(LOG_TAG, "Cannot set profile owner because of restriction.");
                return false;
            }
            if (isAdb(caller)) {
                MetricsLogger.action(this.mContext, 617, LOG_TAG_PROFILE_OWNER);
                DevicePolicyEventLogger.createEvent(82).setAdmin(who).setStrings(new String[]{LOG_TAG_PROFILE_OWNER}).write();
            }
            toggleBackupServiceActive(userHandle, false);
            this.mOwners.setProfileOwner(who, ownerName, userHandle);
            this.mOwners.writeProfileOwner(userHandle);
            Slogf.i(LOG_TAG, "Profile owner set: " + who + " on user " + userHandle);
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda112
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3100x7e9e86e6(userHandle, admin);
                }
            });
            this.mDeviceAdminServiceController.startServiceForOwner(who.getPackageName(), userHandle, "set-profile-owner");
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setProfileOwner$64$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3100x7e9e86e6(int userHandle, ActiveAdmin admin) throws Exception {
        if (this.mUserManager.isManagedProfile(userHandle)) {
            maybeSetDefaultRestrictionsForAdminLocked(userHandle, admin, UserRestrictionsUtils.getDefaultEnabledForManagedProfiles());
            ensureUnknownSourcesRestrictionForProfileOwnerLocked(userHandle, admin, true);
        }
        sendOwnerChangedBroadcast("android.app.action.PROFILE_OWNER_CHANGED", userHandle);
    }

    private void toggleBackupServiceActive(int userId, boolean makeActive) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                if (this.mInjector.getIBackupManager() != null) {
                    this.mInjector.getIBackupManager().setBackupServiceActive(userId, makeActive);
                }
            } catch (RemoteException e) {
                Object[] objArr = new Object[1];
                objArr[0] = makeActive ? "activating" : "deactivating";
                throw new IllegalStateException(String.format("Failed %s backup service.", objArr), e);
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public void clearProfileOwner(final ComponentName who) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        final int userId = caller.getUserId();
        Preconditions.checkCallingUser(!isManagedProfile(userId));
        enforceUserUnlocked(userId);
        synchronized (getLockObject()) {
            final ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda56
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m2985x1ed85db0(admin, userId, who);
                }
            });
            Slogf.i(LOG_TAG, "Profile owner " + who + " removed from user " + userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearProfileOwner$65$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m2985x1ed85db0(ActiveAdmin admin, int userId, ComponentName who) throws Exception {
        clearProfileOwnerLocked(admin, userId);
        m3056x58c4fc7d(who, userId);
        sendOwnerChangedBroadcast("android.app.action.PROFILE_OWNER_CHANGED", userId);
    }

    public void clearProfileOwnerLocked(ActiveAdmin admin, int userId) {
        this.mDeviceAdminServiceController.stopServiceForOwner(userId, "clear-profile-owner");
        if (admin != null) {
            admin.disableCamera = false;
            admin.userRestrictions = null;
            admin.defaultEnabledRestrictionsAlreadySet.clear();
        }
        DevicePolicyData policyData = m3013x8b75536b(userId);
        policyData.mCurrentInputMethodSet = false;
        policyData.mOwnerInstalledCaCerts.clear();
        saveSettingsLocked(userId);
        clearUserPoliciesLocked(userId);
        clearApplicationRestrictions(userId);
        this.mOwners.removeProfileOwner(userId);
        this.mOwners.writeProfileOwner(userId);
        deleteTransferOwnershipBundleLocked(userId);
        toggleBackupServiceActive(userId, true);
        applyProfileRestrictionsIfDeviceOwnerLocked();
        setNetworkLoggingActiveInternal(false);
    }

    public void setDeviceOwnerLockScreenInfo(ComponentName who, final CharSequence info) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller));
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda79
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3079x29321f92(info);
            }
        });
        DevicePolicyEventLogger.createEvent(42).setAdmin(caller.getComponentName()).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDeviceOwnerLockScreenInfo$66$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3079x29321f92(CharSequence info) throws Exception {
        this.mLockPatternUtils.setDeviceOwnerInfo(info != null ? info.toString() : null);
    }

    public CharSequence getDeviceOwnerLockScreenInfo() {
        return this.mLockPatternUtils.getDeviceOwnerInfo();
    }

    private void clearUserPoliciesLocked(int userId) {
        DevicePolicyData policy = m3013x8b75536b(userId);
        policy.mPermissionPolicy = 0;
        policy.mDelegationMap.clear();
        policy.mStatusBarDisabled = false;
        policy.mSecondaryLockscreenEnabled = false;
        policy.mUserProvisioningState = 0;
        policy.mAffiliationIds.clear();
        policy.mLockTaskPackages.clear();
        updateLockTaskPackagesLocked(policy.mLockTaskPackages, userId);
        policy.mLockTaskFeatures = 0;
        saveSettingsLocked(userId);
        try {
            this.mIPermissionManager.updatePermissionFlagsForAllApps(4, 0, userId);
            pushUserRestrictions(userId);
        } catch (RemoteException e) {
        }
    }

    public boolean hasUserSetupCompleted() {
        return hasUserSetupCompleted(this.mInjector.userHandleGetCallingUserId());
    }

    private boolean hasUserSetupCompleted(int userHandle) {
        if (!this.mHasFeature) {
            return true;
        }
        return this.mInjector.hasUserSetupCompleted(m3013x8b75536b(userHandle));
    }

    private boolean hasPaired(int userHandle) {
        if (!this.mHasFeature) {
            return true;
        }
        return m3013x8b75536b(userHandle).mPaired;
    }

    public int getUserProvisioningState() {
        boolean z = false;
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization((canManageUsers(caller) || hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS")) ? true : true);
            return getUserProvisioningState(caller.getUserId());
        }
        return 0;
    }

    private int getUserProvisioningState(int userHandle) {
        return m3013x8b75536b(userHandle).mUserProvisioningState;
    }

    public void setUserProvisioningState(int newState, int userId) {
        boolean hasProfileOwner;
        int managedUserId;
        if (!this.mHasFeature) {
            logMissingFeatureAction("Cannot set provisioning state " + newState + " for user " + userId);
            return;
        }
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        CallerIdentity caller = getCallerIdentity();
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            int deviceOwnerUserId = this.mOwners.getDeviceOwnerUserId();
            if (userId != deviceOwnerUserId && !(hasProfileOwner = this.mOwners.hasProfileOwner(userId)) && (managedUserId = getManagedUserId(userId)) == -1 && newState != 0) {
                Slogf.w(LOG_TAG, "setUserProvisioningState(newState=%d, userId=%d) failed: deviceOwnerId=%d, hasProfileOwner=%b, managedUserId=%d, err=%s", Integer.valueOf(newState), Integer.valueOf(userId), Integer.valueOf(deviceOwnerUserId), Boolean.valueOf(hasProfileOwner), Integer.valueOf(managedUserId), "Not allowed to change provisioning state unless a device or profile owner is set.");
                throw new IllegalStateException("Not allowed to change provisioning state unless a device or profile owner is set.");
            }
            synchronized (getLockObject()) {
                boolean transitionCheckNeeded = true;
                if (isAdb(caller)) {
                    if (getUserProvisioningState(userId) != 0 || newState != 3) {
                        throw new IllegalStateException("Not allowed to change provisioning state unless current provisioning state is unmanaged, and new stateis finalized.");
                    }
                    transitionCheckNeeded = false;
                }
                DevicePolicyData policyData = m3013x8b75536b(userId);
                if (transitionCheckNeeded) {
                    checkUserProvisioningStateTransition(policyData.mUserProvisioningState, newState);
                }
                policyData.mUserProvisioningState = newState;
                saveSettingsLocked(userId);
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    private void checkUserProvisioningStateTransition(int currentState, int newState) {
        switch (currentState) {
            case 0:
                if (newState != 0) {
                    return;
                }
                break;
            case 1:
            case 2:
                if (newState == 3) {
                    return;
                }
                break;
            case 4:
                if (newState == 5) {
                    return;
                }
                break;
            case 5:
                if (newState == 0) {
                    return;
                }
                break;
        }
        throw new IllegalStateException("Cannot move to user provisioning state [" + newState + "] from state [" + currentState + "]");
    }

    public void setDualProfileEnabled(ComponentName who, final int userId) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        synchronized (getLockObject()) {
            Preconditions.checkArgument(isDualProfile(userId));
            UserInfo dualProfile = getUserInfo(userId);
            if (dualProfile.isEnabled()) {
                Slogf.e(LOG_TAG, "setDualProfileEnabled is called when the profile is already enabled");
            } else {
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda166
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3082x1f06662d(userId);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDualProfileEnabled$67$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3082x1f06662d(int userId) throws Exception {
        ComponentName settingsComponentName = new ComponentName("com.android.settings", "com.android.settings.Settings");
        changeComponentEnabledSetting(settingsComponentName, userId, 2);
        this.mUserManager.setUserEnabled(userId);
        UserInfo parent = this.mUserManager.getProfileParent(userId);
        Intent intent = new Intent("android.intent.action.MANAGED_PROFILE_ADDED");
        intent.putExtra("android.intent.extra.USER", new UserHandle(userId));
        UserHandle parentHandle = new UserHandle(parent.id);
        this.mLocalService.broadcastIntentToManifestReceivers(intent, parentHandle, true);
        intent.addFlags(1342177280);
        this.mContext.sendBroadcastAsUser(intent, parentHandle);
    }

    public void setProfileEnabled(ComponentName who) {
        if (!this.mHasFeature) {
            logMissingFeatureAction("Cannot enable profile for " + ComponentName.flattenToShortString(who));
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        final int userId = caller.getUserId();
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        Preconditions.checkCallingUser(isManagedProfile(userId));
        synchronized (getLockObject()) {
            UserInfo managedProfile = getUserInfo(userId);
            if (managedProfile.isEnabled()) {
                Slogf.e(LOG_TAG, "setProfileEnabled is called when the profile is already enabled");
            } else {
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda108
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3098x6aa93970(userId);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setProfileEnabled$68$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3098x6aa93970(int userId) throws Exception {
        this.mUserManager.setUserEnabled(userId);
        UserInfo parent = this.mUserManager.getProfileParent(userId);
        Intent intent = new Intent("android.intent.action.MANAGED_PROFILE_ADDED");
        intent.putExtra("android.intent.extra.USER", new UserHandle(userId));
        UserHandle parentHandle = new UserHandle(parent.id);
        this.mLocalService.broadcastIntentToManifestReceivers(intent, parentHandle, true);
        intent.addFlags(1342177280);
        this.mContext.sendBroadcastAsUser(intent, parentHandle);
    }

    public void setProfileName(ComponentName who, final String profileName) {
        Objects.requireNonNull(who, "ComponentName is null");
        final CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda43
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3099xa0960321(caller, profileName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setProfileName$69$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3099xa0960321(CallerIdentity caller, String profileName) throws Exception {
        this.mUserManager.setUserName(caller.getUserId(), profileName);
        DevicePolicyEventLogger.createEvent(40).setAdmin(caller.getComponentName()).write();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: getProfileOwnerAsUser */
    public ComponentName m3036x91e56c0c(int userId) {
        ComponentName profileOwnerComponent;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkArgumentNonnegative(userId, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasCrossUsersPermission(caller, userId) || hasFullCrossUsersPermission(caller, userId));
        synchronized (getLockObject()) {
            profileOwnerComponent = this.mOwners.getProfileOwnerComponent(userId);
        }
        return profileOwnerComponent;
    }

    ActiveAdmin getProfileOwnerAdminLocked(int userHandle) {
        ComponentName profileOwner = this.mOwners.getProfileOwnerComponent(userHandle);
        if (profileOwner == null) {
            return null;
        }
        DevicePolicyData policy = m3013x8b75536b(userHandle);
        int n = policy.mAdminList.size();
        for (int i = 0; i < n; i++) {
            ActiveAdmin admin = policy.mAdminList.get(i);
            if (profileOwner.equals(admin.info.getComponent())) {
                return admin;
            }
        }
        return null;
    }

    private ActiveAdmin getDeviceOrProfileOwnerAdminLocked(int userHandle) {
        ActiveAdmin admin = getProfileOwnerAdminLocked(userHandle);
        if (admin == null && getDeviceOwnerUserIdUncheckedLocked() == userHandle) {
            return getDeviceOwnerAdminLocked();
        }
        return admin;
    }

    ActiveAdmin getProfileOwnerOfOrganizationOwnedDeviceLocked(final int userHandle) {
        return (ActiveAdmin) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda93
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3007x4628514(userHandle);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getProfileOwnerOfOrganizationOwnedDeviceLocked$70$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ ActiveAdmin m3007x4628514(int userHandle) throws Exception {
        for (UserInfo userInfo : this.mUserManager.getProfiles(userHandle)) {
            if (userInfo.isManagedProfile() && m3036x91e56c0c(userInfo.id) != null && isProfileOwnerOfOrganizationOwnedDevice(userInfo.id)) {
                ComponentName who = m3036x91e56c0c(userInfo.id);
                return getActiveAdminUncheckedLocked(who, userInfo.id);
            }
        }
        return null;
    }

    public ComponentName getProfileOwnerOrDeviceOwnerSupervisionComponent(UserHandle userHandle) {
        if (this.mHasFeature) {
            synchronized (getLockObject()) {
                ComponentName doComponent = this.mOwners.getDeviceOwnerComponent();
                ComponentName poComponent = this.mOwners.getProfileOwnerComponent(userHandle.getIdentifier());
                if (this.mConstants.USE_TEST_ADMIN_AS_SUPERVISION_COMPONENT) {
                    if (isAdminTestOnlyLocked(doComponent, userHandle.getIdentifier())) {
                        return doComponent;
                    }
                    if (isAdminTestOnlyLocked(poComponent, userHandle.getIdentifier())) {
                        return poComponent;
                    }
                }
                if (isSupervisionComponentLocked(poComponent)) {
                    return poComponent;
                }
                if (isSupervisionComponentLocked(doComponent)) {
                    return doComponent;
                }
                return null;
            }
        }
        return null;
    }

    public boolean isSupervisionComponent(ComponentName who) {
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            if (this.mConstants.USE_TEST_ADMIN_AS_SUPERVISION_COMPONENT) {
                CallerIdentity caller = getCallerIdentity();
                if (isAdminTestOnlyLocked(who, caller.getUserId())) {
                    return true;
                }
            }
            return isSupervisionComponentLocked(who);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSupervisionComponentLocked(ComponentName who) {
        if (who == null) {
            return false;
        }
        String configComponent = this.mContext.getResources().getString(17039944);
        if (configComponent != null) {
            ComponentName componentName = ComponentName.unflattenFromString(configComponent);
            if (who.equals(componentName)) {
                return true;
            }
        }
        String configPackage = this.mContext.getResources().getString(17039420);
        return who.getPackageName().equals(configPackage);
    }

    public String getProfileOwnerName(int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        return getProfileOwnerNameUnchecked(userHandle);
    }

    private String getProfileOwnerNameUnchecked(int userHandle) {
        ComponentName profileOwner = m3036x91e56c0c(userHandle);
        if (profileOwner == null) {
            return null;
        }
        return getApplicationLabel(profileOwner.getPackageName(), userHandle);
    }

    private int getOrganizationOwnedProfileUserId() {
        UserInfo[] userInfos;
        for (UserInfo ui : this.mUserManagerInternal.getUserInfos()) {
            if (ui.isManagedProfile() && isProfileOwnerOfOrganizationOwnedDevice(ui.id)) {
                return ui.id;
            }
        }
        return -10000;
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public boolean isOrganizationOwnedDeviceWithManagedProfile() {
        return this.mHasFeature && getOrganizationOwnedProfileUserId() != -10000;
    }

    public boolean checkDeviceIdentifierAccess(String packageName, int pid, int uid) {
        CallerIdentity caller = getCallerIdentity();
        ensureCallerIdentityMatchesIfNotSystem(packageName, pid, uid, caller);
        if (doesPackageMatchUid(packageName, uid) && hasPermission("android.permission.READ_PHONE_STATE", pid, uid)) {
            ComponentName deviceOwner = getDeviceOwnerComponent(true);
            if (deviceOwner != null && (deviceOwner.getPackageName().equals(packageName) || isCallerDelegate(packageName, uid, "delegation-cert-install"))) {
                return true;
            }
            int userId = UserHandle.getUserId(uid);
            ComponentName profileOwner = m3036x91e56c0c(userId);
            boolean isCallerProfileOwnerOrDelegate = profileOwner != null && (profileOwner.getPackageName().equals(packageName) || isCallerDelegate(packageName, uid, "delegation-cert-install"));
            return isCallerProfileOwnerOrDelegate && (isProfileOwnerOfOrganizationOwnedDevice(userId) || isUserAffiliatedWithDevice(userId));
        }
        return false;
    }

    private boolean doesPackageMatchUid(String packageName, int uid) {
        int userId = UserHandle.getUserId(uid);
        try {
            ApplicationInfo appInfo = this.mIPackageManager.getApplicationInfo(packageName, 0L, userId);
            if (appInfo == null) {
                Slogf.w(LOG_TAG, "appInfo could not be found for package %s", packageName);
                return false;
            } else if (uid == appInfo.uid) {
                return true;
            } else {
                String message = String.format("Package %s (uid=%d) does not match provided uid %d", packageName, Integer.valueOf(appInfo.uid), Integer.valueOf(uid));
                Slogf.w(LOG_TAG, message);
                throw new SecurityException(message);
            }
        } catch (RemoteException e) {
            Slogf.e(LOG_TAG, e, "Exception caught obtaining appInfo for package %s", packageName);
            return false;
        }
    }

    private void ensureCallerIdentityMatchesIfNotSystem(String packageName, int pid, int uid, CallerIdentity caller) {
        int callingUid = caller.getUid();
        int callingPid = this.mInjector.binderGetCallingPid();
        if (UserHandle.getAppId(callingUid) >= 10000) {
            if (callingUid != uid || callingPid != pid) {
                String message = String.format("Calling uid %d, pid %d cannot check device identifier access for package %s (uid=%d, pid=%d)", Integer.valueOf(callingUid), Integer.valueOf(callingPid), packageName, Integer.valueOf(uid), Integer.valueOf(pid));
                Slogf.w(LOG_TAG, message);
                throw new SecurityException(message);
            }
        }
    }

    private String getApplicationLabel(final String packageName, final int userId) {
        return (String) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda132
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m2997x56a5aa38(userId, packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getApplicationLabel$71$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ String m2997x56a5aa38(int userId, String packageName) throws Exception {
        try {
            UserHandle userHandle = UserHandle.of(userId);
            Context userContext = this.mContext.createPackageContextAsUser(packageName, 0, userHandle);
            ApplicationInfo appInfo = userContext.getApplicationInfo();
            CharSequence result = null;
            if (appInfo != null) {
                result = appInfo.loadUnsafeLabel(userContext.getPackageManager());
            }
            if (result != null) {
                return result.toString();
            }
            return null;
        } catch (PackageManager.NameNotFoundException nnfe) {
            Slogf.w(LOG_TAG, nnfe, "%s is not installed for user %d", packageName, Integer.valueOf(userId));
            return null;
        }
    }

    private void enforceCanSetProfileOwnerLocked(CallerIdentity caller, ComponentName owner, int userHandle, boolean hasIncompatibleAccountsOrNonAdb) {
        UserInfo info = getUserInfo(userHandle);
        if (info == null) {
            throw new IllegalArgumentException("Attempted to set profile owner for invalid userId: " + userHandle);
        }
        if (info.isGuest()) {
            throw new IllegalStateException("Cannot set a profile owner on a guest");
        }
        if (this.mOwners.hasProfileOwner(userHandle)) {
            throw new IllegalStateException("Trying to set the profile owner, but profile owner is already set.");
        }
        if (this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == userHandle) {
            throw new IllegalStateException("Trying to set the profile owner, but the user already has a device owner.");
        }
        if (isAdb(caller)) {
            if ((this.mIsWatch || hasUserSetupCompleted(userHandle)) && hasIncompatibleAccountsOrNonAdb) {
                throw new IllegalStateException("Not allowed to set the profile owner because there are already some accounts on the profile");
            }
            return;
        }
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        if (this.mIsWatch || hasUserSetupCompleted(userHandle)) {
            Preconditions.checkState(isSystemUid(caller), "Cannot set the profile owner on a user which is already set-up");
            if (!this.mIsWatch && !isSupervisionComponentLocked(owner)) {
                throw new IllegalStateException("Unable to set non-default profile owner post-setup " + owner);
            }
        }
    }

    private void enforceCanSetDeviceOwnerLocked(CallerIdentity caller, ComponentName owner, int deviceOwnerUserId, boolean hasIncompatibleAccountsOrNonAdb) {
        ITranDevicePolicyManagerService.Instance().checkSetDeviceOwnerByAdb(isAdb(caller), this.mContext);
        if (!isAdb(caller)) {
            Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        }
        int code = checkDeviceOwnerProvisioningPreConditionLocked(owner, deviceOwnerUserId, caller.getUserId(), isAdb(caller), hasIncompatibleAccountsOrNonAdb);
        if (code != 0) {
            if (code == 4 && ITranDevicePolicyManagerService.Instance().checkSetDeviceOwnerAfterUserSetup(this.mContext, this.mInjector.userManagerIsHeadlessSystemUserMode(), hasUserSetupCompleted(0), owner.getPackageName(), this.mIsWatch, this.mUserManager.getUserCount())) {
                return;
            }
            throw new IllegalStateException(computeProvisioningErrorString(code, deviceOwnerUserId));
        }
        IDevicePolicyManagerServiceLice.Instance().checkSetDeviceOwnerWhenCodeOk(owner.getPackageName(), this.mContext);
    }

    private static String computeProvisioningErrorString(int code, int userId) {
        switch (code) {
            case 0:
                return "OK";
            case 1:
                return "Trying to set the device owner, but device owner is already set.";
            case 2:
                return "Trying to set the device owner, but the user already has a profile owner.";
            case 3:
                return "User " + userId + " not running.";
            case 4:
                return "Cannot set the device owner if the device is already set-up.";
            case 5:
                return "Not allowed to set the device owner because there are already several users on the device.";
            case 6:
                return "Not allowed to set the device owner because there are already some accounts on the device.";
            case 7:
                return "User " + userId + " is not system user.";
            case 8:
                return "Not allowed to set the device owner because this device has already paired.";
            default:
                return "Unexpected @ProvisioningPreCondition: " + code;
        }
    }

    private void enforceUserUnlocked(int userId) {
        Preconditions.checkState(this.mUserManager.isUserUnlocked(userId), "User must be running and unlocked");
    }

    private void enforceUserUnlocked(int userId, boolean parent) {
        if (parent) {
            enforceUserUnlocked(getProfileParentId(userId));
        } else {
            enforceUserUnlocked(userId);
        }
    }

    private boolean canManageUsers(CallerIdentity caller) {
        return hasCallingOrSelfPermission("android.permission.MANAGE_USERS");
    }

    private boolean canQueryAdminPolicy(CallerIdentity caller) {
        return hasCallingOrSelfPermission("android.permission.QUERY_ADMIN_POLICY");
    }

    private boolean hasPermission(String permission, int pid, int uid) {
        return this.mContext.checkPermission(permission, pid, uid) == 0;
    }

    private boolean hasCallingPermission(String permission) {
        return this.mContext.checkCallingPermission(permission) == 0;
    }

    private boolean hasCallingOrSelfPermission(String permission) {
        return this.mContext.checkCallingOrSelfPermission(permission) == 0;
    }

    private boolean hasPermissionForPreflight(CallerIdentity caller, String permission) {
        int callingPid = this.mInjector.binderGetCallingPid();
        String packageName = this.mContext.getPackageName();
        return PermissionChecker.checkPermissionForPreflight(this.mContext, permission, callingPid, caller.getUid(), packageName) == 0;
    }

    private boolean hasFullCrossUsersPermission(CallerIdentity caller, int userHandle) {
        return userHandle == caller.getUserId() || isSystemUid(caller) || isRootUid(caller) || hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL");
    }

    private boolean hasCrossUsersPermission(CallerIdentity caller, int userHandle) {
        return userHandle == caller.getUserId() || isSystemUid(caller) || isRootUid(caller) || hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS");
    }

    private boolean canUserUseLockTaskLocked(int userId) {
        if (isUserAffiliatedWithDeviceLocked(userId)) {
            return true;
        }
        if (this.mOwners.hasDeviceOwner()) {
            return false;
        }
        ComponentName profileOwner = m3036x91e56c0c(userId);
        return (profileOwner == null || isManagedProfile(userId)) ? false : true;
    }

    private void enforceCanCallLockTaskLocked(CallerIdentity caller) {
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller) || isFinancedDeviceOwner(caller));
        int userId = caller.getUserId();
        if (!canUserUseLockTaskLocked(userId)) {
            throw new SecurityException("User " + userId + " is not allowed to use lock task");
        }
    }

    private boolean isSystemUid(CallerIdentity caller) {
        return UserHandle.isSameApp(caller.getUid(), 1000);
    }

    private boolean isRootUid(CallerIdentity caller) {
        return UserHandle.isSameApp(caller.getUid(), 0);
    }

    private boolean isShellUid(CallerIdentity caller) {
        return UserHandle.isSameApp(caller.getUid(), 2000);
    }

    private int getCurrentForegroundUserId() {
        try {
            UserInfo currentUser = this.mInjector.getIActivityManager().getCurrentUser();
            if (currentUser == null) {
                Slogf.wtf(LOG_TAG, "getCurrentForegroundUserId(): mInjector.getIActivityManager().getCurrentUser() returned null, please ignore when running unit tests");
                return ActivityManager.getCurrentUser();
            }
            return currentUser.id;
        } catch (RemoteException e) {
            Slogf.wtf(LOG_TAG, "cannot get current user");
            return -10000;
        }
    }

    public List<UserHandle> listForegroundAffiliatedUsers() {
        checkIsDeviceOwner(getCallerIdentity());
        return (List) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda39
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3042x25aab12d();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$listForegroundAffiliatedUsers$72$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ List m3042x25aab12d() throws Exception {
        boolean isAffiliated;
        int userId = getCurrentForegroundUserId();
        synchronized (getLockObject()) {
            isAffiliated = isUserAffiliatedWithDeviceLocked(userId);
        }
        if (isAffiliated) {
            List<UserHandle> users = new ArrayList<>(1);
            users.add(UserHandle.of(userId));
            return users;
        }
        return Collections.emptyList();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getProfileParentId(final int userHandle) {
        return ((Integer) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda123
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3008x986eaa4e(userHandle);
            }
        })).intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getProfileParentId$73$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Integer m3008x986eaa4e(int userHandle) throws Exception {
        UserInfo parentUser = this.mUserManager.getProfileParent(userHandle);
        return Integer.valueOf(parentUser != null ? parentUser.id : userHandle);
    }

    private int getProfileParentUserIfRequested(int userHandle, boolean parent) {
        if (parent) {
            return getProfileParentId(userHandle);
        }
        return userHandle;
    }

    private int getCredentialOwner(final int userHandle, final boolean parent) {
        return ((Integer) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda19
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3000xa0953d9b(userHandle, parent);
            }
        })).intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCredentialOwner$74$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Integer m3000xa0953d9b(int userHandle, boolean parent) throws Exception {
        UserInfo parentProfile;
        int effectiveUserHandle = userHandle;
        if (parent && (parentProfile = this.mUserManager.getProfileParent(userHandle)) != null) {
            effectiveUserHandle = parentProfile.id;
        }
        return Integer.valueOf(this.mUserManager.getCredentialOwnerProfile(effectiveUserHandle));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isManagedProfile(int userHandle) {
        UserInfo user = getUserInfo(userHandle);
        return user != null && user.isManagedProfile();
    }

    private boolean isDualProfile(int userHandle) {
        UserInfo user = getUserInfo(userHandle);
        return user != null && user.isDualProfile();
    }

    private void changeComponentEnabledSetting(ComponentName packageName, int userId, int enabledSetting) {
        try {
            this.mIPackageManager.setComponentEnabledSetting(packageName, enabledSetting, 1, userId);
        } catch (RemoteException e) {
        }
    }

    private void enableIfNecessary(String packageName, int userId) {
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(packageName, 32768L, userId);
            if (ai.enabledSetting == 4) {
                this.mIPackageManager.setApplicationEnabledSetting(packageName, 0, 1, userId, LOG_TAG);
            }
        } catch (RemoteException e) {
        }
    }

    private void dumpPerUserData(IndentingPrintWriter pw) {
        int userCount = this.mUserData.size();
        for (int i = 0; i < userCount; i++) {
            int userId = this.mUserData.keyAt(i);
            DevicePolicyData policy = m3013x8b75536b(userId);
            policy.dump(pw);
            pw.println();
            if (userId == 0) {
                pw.increaseIndent();
                PersonalAppsSuspensionHelper.forUser(this.mContext, userId).dump(pw);
                pw.decreaseIndent();
                pw.println();
            } else {
                Slogf.d(LOG_TAG, "skipping PersonalAppsSuspensionHelper.dump() for user " + userId);
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, printWriter)) {
            final IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
            try {
                pw.println("Current Device Policy Manager state:");
                pw.increaseIndent();
                dumpImmutableState(pw);
                synchronized (getLockObject()) {
                    this.mOwners.dump(pw);
                    pw.println();
                    this.mDeviceAdminServiceController.dump(pw);
                    pw.println();
                    dumpPerUserData(pw);
                    pw.println();
                    this.mConstants.dump(pw);
                    pw.println();
                    this.mStatLogger.dump(pw);
                    pw.println();
                    pw.println("Encryption Status: " + getEncryptionStatusName(getEncryptionStatus()));
                    pw.println("Logout user: " + getLogoutUserIdUnchecked());
                    pw.println();
                    if (this.mPendingUserCreatedCallbackTokens.isEmpty()) {
                        pw.println("no pending user created callback tokens");
                    } else {
                        int size = this.mPendingUserCreatedCallbackTokens.size();
                        Object[] objArr = new Object[2];
                        objArr[0] = Integer.valueOf(size);
                        objArr[1] = size == 1 ? "" : "s";
                        pw.printf("%d pending user created callback token%s\n", objArr);
                    }
                    pw.println();
                    this.mPolicyCache.dump(pw);
                    pw.println();
                    this.mStateCache.dump(pw);
                    pw.println();
                }
                this.mHandler.post(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda53
                    @Override // java.lang.Runnable
                    public final void run() {
                        DevicePolicyManagerService.this.m2987xd01b92c8(pw);
                    }
                });
                dumpResources(pw);
                pw.close();
            } catch (Throwable th) {
                try {
                    pw.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleDump */
    public void m2987xd01b92c8(IndentingPrintWriter pw) {
        if (this.mNetworkLoggingNotificationUserId != -10000) {
            pw.println("mNetworkLoggingNotificationUserId:  " + this.mNetworkLoggingNotificationUserId);
        }
    }

    private void dumpImmutableState(IndentingPrintWriter pw) {
        pw.println("Immutable state:");
        pw.increaseIndent();
        pw.printf("mHasFeature=%b\n", new Object[]{Boolean.valueOf(this.mHasFeature)});
        pw.printf("mIsWatch=%b\n", new Object[]{Boolean.valueOf(this.mIsWatch)});
        pw.printf("mIsAutomotive=%b\n", new Object[]{Boolean.valueOf(this.mIsAutomotive)});
        pw.printf("mHasTelephonyFeature=%b\n", new Object[]{Boolean.valueOf(this.mHasTelephonyFeature)});
        pw.printf("mSafetyChecker=%s\n", new Object[]{this.mSafetyChecker});
        pw.decreaseIndent();
    }

    private void dumpResources(IndentingPrintWriter pw) {
        this.mOverlayPackagesProvider.dump(pw);
        pw.println();
        pw.println("Other overlayable app resources");
        pw.increaseIndent();
        dumpResources(pw, this.mContext, "cross_profile_apps", 17236156);
        dumpResources(pw, this.mContext, "vendor_cross_profile_apps", 17236199);
        dumpResources(pw, this.mContext, "config_packagesExemptFromSuspension", 17236107);
        dumpResources(pw, this.mContext, "policy_exempt_apps", 17236178);
        dumpResources(pw, this.mContext, "vendor_policy_exempt_apps", 17236203);
        pw.decreaseIndent();
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void dumpResources(IndentingPrintWriter pw, Context context, String resName, int resId) {
        dumpApps(pw, resName, context.getResources().getStringArray(resId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void dumpApps(IndentingPrintWriter pw, String name, String[] apps) {
        dumpApps(pw, name, Arrays.asList(apps));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void dumpApps(IndentingPrintWriter pw, String name, List apps) {
        if (apps == null || apps.isEmpty()) {
            pw.printf("%s: empty\n", new Object[]{name});
            return;
        }
        int size = apps.size();
        Object[] objArr = new Object[3];
        objArr[0] = name;
        objArr[1] = Integer.valueOf(size);
        objArr[2] = size == 1 ? "" : "s";
        pw.printf("%s: %d app%s\n", objArr);
        pw.increaseIndent();
        for (int i = 0; i < size; i++) {
            pw.printf("%d: %s\n", new Object[]{Integer.valueOf(i), apps.get(i)});
        }
        pw.decreaseIndent();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.devicepolicy.DevicePolicyManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new DevicePolicyManagerServiceShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    private String getEncryptionStatusName(int encryptionStatus) {
        switch (encryptionStatus) {
            case 0:
                return "unsupported";
            case 1:
                return "inactive";
            case 2:
                return "activating";
            case 3:
                return "block";
            case 4:
                return "block default key";
            case 5:
                return "per-user";
            default:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    public void addPersistentPreferredActivity(ComponentName who, IntentFilter filter, ComponentName activity) {
        Injector injector;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller) || isFinancedDeviceOwner(caller));
        int userHandle = caller.getUserId();
        synchronized (getLockObject()) {
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.addPersistentPreferredActivity(filter, activity, userHandle);
                this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                injector = this.mInjector;
            } catch (RemoteException e) {
                injector = this.mInjector;
            }
            injector.binderRestoreCallingIdentity(id);
        }
        String activityPackage = activity != null ? activity.getPackageName() : null;
        DevicePolicyEventLogger.createEvent(52).setAdmin(who).setStrings(activityPackage, getIntentFilterActions(filter)).write();
    }

    public void clearPackagePersistentPreferredActivities(ComponentName who, String packageName) {
        Injector injector;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller) || isFinancedDeviceOwner(caller));
        int userHandle = caller.getUserId();
        synchronized (getLockObject()) {
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.clearPackagePersistentPreferredActivities(packageName, userHandle);
                this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                injector = this.mInjector;
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(id);
                throw th;
            }
            injector.binderRestoreCallingIdentity(id);
        }
    }

    public void setDefaultSmsApplication(ComponentName admin, final String packageName, boolean parent) {
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || (parent && isProfileOwnerOfOrganizationOwnedDevice(caller)));
        if (parent) {
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda125
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3076xe7da23c9(packageName);
                }
            });
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda126
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3077xedddef28(packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDefaultSmsApplication$76$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3076xe7da23c9(String packageName) throws Exception {
        m3069xe7b1a5eb(packageName, getProfileParentId(this.mInjector.userHandleGetCallingUserId()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDefaultSmsApplication$77$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3077xedddef28(String packageName) throws Exception {
        SmsApplication.setDefaultApplication(packageName, this.mContext);
    }

    public boolean setApplicationRestrictionsManagingPackage(ComponentName admin, String packageName) {
        try {
            setDelegatedScopePreO(admin, packageName, "delegation-app-restrictions");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getApplicationRestrictionsManagingPackage(ComponentName admin) {
        List<String> delegatePackages = getDelegatePackages(admin, "delegation-app-restrictions");
        if (delegatePackages.size() > 0) {
            return delegatePackages.get(0);
        }
        return null;
    }

    public boolean isCallerApplicationRestrictionsManagingPackage(String callerPackage) {
        return isCallerDelegate(callerPackage, getCallerIdentity().getUid(), "delegation-app-restrictions");
    }

    public void setApplicationRestrictions(final ComponentName who, String callerPackage, final String packageName, final Bundle settings) {
        final CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-app-restrictions")));
        checkCanExecuteOrThrowUnsafe(16);
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda33
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3071x3eafc046(packageName, settings, caller, who);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setApplicationRestrictions$78$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3071x3eafc046(String packageName, Bundle settings, CallerIdentity caller, ComponentName who) throws Exception {
        this.mUserManager.setApplicationRestrictions(packageName, settings, caller.getUserHandle());
        DevicePolicyEventLogger.createEvent(62).setAdmin(caller.getPackageName()).setBoolean(who == null).setStrings(new String[]{packageName}).write();
    }

    public void setTrustAgentConfiguration(ComponentName admin, ComponentName agent, PersistableBundle args, boolean parent) {
        if (!this.mHasFeature || !this.mLockPatternUtils.hasSecureLockScreen()) {
            return;
        }
        Objects.requireNonNull(admin, "admin is null");
        Objects.requireNonNull(agent, "agent is null");
        enforceMaxPackageNameLength(agent.getPackageName());
        String agentAsString = agent.flattenToString();
        enforceMaxStringLength(agentAsString, "agent name");
        if (args != null) {
            enforceMaxStringLength(args, "args");
        }
        int userHandle = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(admin, 9, parent);
            checkCanExecuteOrThrowUnsafe(21);
            ap.trustAgentInfos.put(agent.flattenToString(), new ActiveAdmin.TrustAgentInfo(args));
            saveSettingsLocked(userHandle);
        }
    }

    public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent, int userHandle, boolean parent) {
        String componentName;
        if (this.mHasFeature && this.mLockPatternUtils.hasSecureLockScreen()) {
            Objects.requireNonNull(agent, "agent null");
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity(admin);
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            synchronized (getLockObject()) {
                String componentName2 = agent.flattenToString();
                if (admin != null) {
                    ActiveAdmin ap = getActiveAdminUncheckedLocked(admin, userHandle, parent);
                    if (ap == null) {
                        return null;
                    }
                    ActiveAdmin.TrustAgentInfo trustAgentInfo = ap.trustAgentInfos.get(componentName2);
                    if (trustAgentInfo != null && trustAgentInfo.options != null) {
                        List<PersistableBundle> result = new ArrayList<>();
                        result.add(trustAgentInfo.options);
                        return result;
                    }
                    return null;
                }
                List<PersistableBundle> result2 = null;
                List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(getProfileParentUserIfRequested(userHandle, parent));
                boolean allAdminsHaveOptions = true;
                int N = admins.size();
                int i = 0;
                while (true) {
                    if (i >= N) {
                        break;
                    }
                    ActiveAdmin active = admins.get(i);
                    boolean disablesTrust = (active.disabledKeyguardFeatures & 16) != 0;
                    ActiveAdmin.TrustAgentInfo info = active.trustAgentInfos.get(componentName2);
                    if (info != null) {
                        componentName = componentName2;
                        if (info.options != null && !info.options.isEmpty()) {
                            if (disablesTrust) {
                                if (result2 == null) {
                                    result2 = new ArrayList<>();
                                }
                                result2.add(info.options);
                            } else {
                                Slogf.w(LOG_TAG, "Ignoring admin %s because it has trust options but doesn't declare KEYGUARD_DISABLE_TRUST_AGENTS", active.info);
                            }
                            i++;
                            componentName2 = componentName;
                        }
                    } else {
                        componentName = componentName2;
                    }
                    if (!disablesTrust) {
                        i++;
                        componentName2 = componentName;
                    } else {
                        allAdminsHaveOptions = false;
                        break;
                    }
                }
                return allAdminsHaveOptions ? result2 : null;
            }
        }
        return null;
    }

    public void setRestrictionsProvider(ComponentName who, ComponentName permissionProvider) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(39);
        synchronized (getLockObject()) {
            int userHandle = caller.getUserId();
            DevicePolicyData userData = m3013x8b75536b(userHandle);
            userData.mRestrictionsProvider = permissionProvider;
            saveSettingsLocked(userHandle);
        }
    }

    public ComponentName getRestrictionsProvider(int userHandle) {
        ComponentName componentName;
        Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "query the permission provider"));
        synchronized (getLockObject()) {
            DevicePolicyData userData = m3013x8b75536b(userHandle);
            componentName = userData != null ? userData.mRestrictionsProvider : null;
        }
        return componentName;
    }

    public void addCrossProfileIntentFilter(ComponentName who, IntentFilter filter, int flags) {
        Injector injector;
        UserInfo parent;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        int callingUserId = caller.getUserId();
        synchronized (getLockObject()) {
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                parent = this.mUserManager.getProfileParent(callingUserId);
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(id);
                throw th;
            }
            if (parent == null) {
                Slogf.e(LOG_TAG, "Cannot call addCrossProfileIntentFilter if there is no parent");
                this.mInjector.binderRestoreCallingIdentity(id);
                return;
            }
            if ((flags & 1) != 0) {
                this.mIPackageManager.addCrossProfileIntentFilter(filter, who.getPackageName(), callingUserId, parent.id, 0);
            }
            if ((flags & 2) != 0) {
                this.mIPackageManager.addCrossProfileIntentFilter(filter, who.getPackageName(), parent.id, callingUserId, 0);
            }
            injector = this.mInjector;
            injector.binderRestoreCallingIdentity(id);
            DevicePolicyEventLogger.createEvent(48).setAdmin(who).setStrings(getIntentFilterActions(filter)).setInt(flags).write();
        }
    }

    private static String[] getIntentFilterActions(IntentFilter filter) {
        if (filter == null) {
            return null;
        }
        int actionsCount = filter.countActions();
        String[] actions = new String[actionsCount];
        for (int i = 0; i < actionsCount; i++) {
            actions[i] = filter.getAction(i);
        }
        return actions;
    }

    public void clearCrossProfileIntentFilters(ComponentName who) {
        Injector injector;
        UserInfo parent;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        int callingUserId = caller.getUserId();
        synchronized (getLockObject()) {
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                parent = this.mUserManager.getProfileParent(callingUserId);
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(id);
                throw th;
            }
            if (parent == null) {
                Slogf.e(LOG_TAG, "Cannot call clearCrossProfileIntentFilter if there is no parent");
                this.mInjector.binderRestoreCallingIdentity(id);
                return;
            }
            this.mIPackageManager.clearCrossProfileIntentFilters(callingUserId, who.getPackageName());
            this.mIPackageManager.clearCrossProfileIntentFilters(parent.id, who.getPackageName());
            injector = this.mInjector;
            injector.binderRestoreCallingIdentity(id);
        }
    }

    private boolean checkPackagesInPermittedListOrSystem(List<String> enabledPackages, List<String> permittedList, int userIdToCheck) {
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            UserInfo user = getUserInfo(userIdToCheck);
            if (user.isManagedProfile()) {
                userIdToCheck = user.profileGroupId;
            }
            Iterator<String> it = enabledPackages.iterator();
            while (true) {
                if (!it.hasNext()) {
                    return true;
                }
                String enabledPackage = it.next();
                boolean systemService = false;
                try {
                    ApplicationInfo applicationInfo = this.mIPackageManager.getApplicationInfo(enabledPackage, 8192L, userIdToCheck);
                    systemService = (applicationInfo.flags & 1) != 0;
                } catch (RemoteException e) {
                    Slogf.i(LOG_TAG, "Can't talk to package managed", e);
                }
                if (!systemService && !permittedList.contains(enabledPackage)) {
                    return false;
                }
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    private <T> T withAccessibilityManager(int userId, Function<AccessibilityManager, T> function) {
        IBinder iBinder = ServiceManager.getService("accessibility");
        IAccessibilityManager service = iBinder == null ? null : IAccessibilityManager.Stub.asInterface(iBinder);
        AccessibilityManager am = new AccessibilityManager(this.mContext, service, userId);
        try {
            return function.apply(am);
        } finally {
            am.removeClient();
        }
    }

    public boolean setPermittedAccessibilityServices(ComponentName who, List<String> packageList) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            if (packageList != null) {
                for (String pkg : packageList) {
                    enforceMaxPackageNameLength(pkg);
                }
                int userId = caller.getUserId();
                long id = this.mInjector.binderClearCallingIdentity();
                try {
                    UserInfo user = getUserInfo(userId);
                    if (user.isManagedProfile()) {
                        userId = user.profileGroupId;
                    }
                    List<AccessibilityServiceInfo> enabledServices = (List) withAccessibilityManager(userId, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda5
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            List enabledAccessibilityServiceList;
                            enabledAccessibilityServiceList = ((AccessibilityManager) obj).getEnabledAccessibilityServiceList(-1);
                            return enabledAccessibilityServiceList;
                        }
                    });
                    if (enabledServices != null) {
                        List<String> enabledPackages = new ArrayList<>();
                        for (AccessibilityServiceInfo service : enabledServices) {
                            enabledPackages.add(service.getResolveInfo().serviceInfo.packageName);
                        }
                        if (!checkPackagesInPermittedListOrSystem(enabledPackages, packageList, userId)) {
                            Slogf.e(LOG_TAG, "Cannot set permitted accessibility services, because it contains already enabled accesibility services.");
                            return false;
                        }
                    }
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            }
            synchronized (getLockObject()) {
                ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
                admin.permittedAccessiblityServices = packageList;
                saveSettingsLocked(UserHandle.getCallingUserId());
            }
            String[] packageArray = packageList != null ? (String[]) packageList.toArray(new String[0]) : null;
            DevicePolicyEventLogger.createEvent(28).setAdmin(who).setStrings(packageArray).write();
            return true;
        }
        return false;
    }

    public List<String> getPermittedAccessibilityServices(ComponentName who) {
        List<String> list;
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            list = admin.permittedAccessiblityServices;
        }
        return list;
    }

    public List<String> getPermittedAccessibilityServicesForUser(int userId) {
        List<String> result;
        if (!this.mHasFeature) {
            return null;
        }
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(canManageUsers(caller) || canQueryAdminPolicy(caller));
        synchronized (getLockObject()) {
            result = null;
            int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(userId);
            for (int profileId : profileIds) {
                DevicePolicyData policy = getUserDataUnchecked(profileId);
                int N = policy.mAdminList.size();
                for (int j = 0; j < N; j++) {
                    ActiveAdmin admin = policy.mAdminList.get(j);
                    List<String> fromAdmin = admin.permittedAccessiblityServices;
                    if (fromAdmin != null) {
                        if (result == null) {
                            result = new ArrayList<>(fromAdmin);
                        } else {
                            result.retainAll(fromAdmin);
                        }
                    }
                }
            }
            if (result != null) {
                long id = this.mInjector.binderClearCallingIdentity();
                UserInfo user = getUserInfo(userId);
                if (user.isManagedProfile()) {
                    userId = user.profileGroupId;
                }
                List<AccessibilityServiceInfo> installedServices = (List) withAccessibilityManager(userId, new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda69
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return ((AccessibilityManager) obj).getInstalledAccessibilityServiceList();
                    }
                });
                if (installedServices != null) {
                    for (AccessibilityServiceInfo service : installedServices) {
                        ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
                        ApplicationInfo applicationInfo = serviceInfo.applicationInfo;
                        if ((applicationInfo.flags & 1) != 0) {
                            result.add(serviceInfo.packageName);
                        }
                    }
                }
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        return result;
    }

    public boolean isAccessibilityServicePermittedByAdmin(ComponentName who, String packageName, int userHandle) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            Preconditions.checkStringNotEmpty(packageName, "packageName is null");
            Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "query if an accessibility service is disabled by admin"));
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin == null) {
                    return false;
                }
                if (admin.permittedAccessiblityServices == null) {
                    return true;
                }
                return checkPackagesInPermittedListOrSystem(Collections.singletonList(packageName), admin.permittedAccessiblityServices, userHandle);
            }
        }
        return true;
    }

    public boolean setPermittedInputMethods(ComponentName who, List<String> packageList, boolean calledOnParentInstance) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            final int userId = getProfileParentUserIfRequested(caller.getUserId(), calledOnParentInstance);
            if (calledOnParentInstance) {
                Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
                Preconditions.checkArgument(packageList == null || packageList.isEmpty(), "Permitted input methods must allow all input methods or only system input methods when called on the parent instance of an organization-owned device");
            } else {
                Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
            }
            if (packageList != null) {
                for (String pkg : packageList) {
                    enforceMaxPackageNameLength(pkg);
                }
                List<InputMethodInfo> enabledImes = (List) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda140
                    public final Object getOrThrow() {
                        List enabledInputMethodListAsUser;
                        enabledInputMethodListAsUser = InputMethodManagerInternal.get().getEnabledInputMethodListAsUser(userId);
                        return enabledInputMethodListAsUser;
                    }
                });
                if (enabledImes != null) {
                    List<String> enabledPackages = new ArrayList<>();
                    for (InputMethodInfo ime : enabledImes) {
                        enabledPackages.add(ime.getPackageName());
                    }
                    if (!checkPackagesInPermittedListOrSystem(enabledPackages, packageList, userId)) {
                        Slogf.e(LOG_TAG, "Cannot set permitted input methods, because the list of permitted input methods excludes an already-enabled input method.");
                        return false;
                    }
                }
            }
            synchronized (getLockObject()) {
                ActiveAdmin admin = getParentOfAdminIfRequired(getProfileOwnerOrDeviceOwnerLocked(caller), calledOnParentInstance);
                admin.permittedInputMethods = packageList;
                saveSettingsLocked(caller.getUserId());
            }
            DevicePolicyEventLogger.createEvent(27).setAdmin(who).setStrings(getStringArrayForLogging(packageList, calledOnParentInstance)).write();
            return true;
        }
        return false;
    }

    private String[] getStringArrayForLogging(List list, boolean calledOnParentInstance) {
        List<String> stringList = new ArrayList<>();
        stringList.add(calledOnParentInstance ? CALLED_FROM_PARENT : NOT_CALLED_FROM_PARENT);
        if (list == null) {
            stringList.add(NULL_STRING_ARRAY);
        } else {
            stringList.addAll(list);
        }
        return (String[]) stringList.toArray(new String[0]);
    }

    public List<String> getPermittedInputMethods(ComponentName who, boolean calledOnParentInstance) {
        List<String> list;
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        if (calledOnParentInstance) {
            Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
        } else {
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        }
        synchronized (getLockObject()) {
            ActiveAdmin admin = getParentOfAdminIfRequired(getProfileOwnerOrDeviceOwnerLocked(caller), calledOnParentInstance);
            list = admin.permittedInputMethods;
        }
        return list;
    }

    public List<String> getPermittedInputMethodsAsUser(int userId) {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userId));
        Preconditions.checkCallAuthorization(canManageUsers(caller) || canQueryAdminPolicy(caller));
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            return getPermittedInputMethodsUnchecked(userId);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private List<String> getPermittedInputMethodsUnchecked(int userId) {
        List<String> result;
        List<InputMethodInfo> imes;
        synchronized (getLockObject()) {
            result = null;
            List<ActiveAdmin> admins = getActiveAdminsForAffectedUserLocked(userId);
            for (ActiveAdmin admin : admins) {
                List<String> fromAdmin = admin.permittedInputMethods;
                if (fromAdmin != null) {
                    if (result == null) {
                        result = new ArrayList<>(fromAdmin);
                    } else {
                        result.retainAll(fromAdmin);
                    }
                }
            }
            if (result != null && (imes = InputMethodManagerInternal.get().getInputMethodListAsUser(userId)) != null) {
                for (InputMethodInfo ime : imes) {
                    ServiceInfo serviceInfo = ime.getServiceInfo();
                    ApplicationInfo applicationInfo = serviceInfo.applicationInfo;
                    if ((applicationInfo.flags & 1) != 0) {
                        result.add(serviceInfo.packageName);
                    }
                }
            }
        }
        return result;
    }

    public boolean isInputMethodPermittedByAdmin(ComponentName who, String packageName, int userHandle, boolean calledOnParentInstance) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            Preconditions.checkStringNotEmpty(packageName, "packageName is null");
            Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "query if an input method is disabled by admin"));
            synchronized (getLockObject()) {
                ActiveAdmin admin = getParentOfAdminIfRequired(getActiveAdminUncheckedLocked(who, userHandle), calledOnParentInstance);
                if (admin == null) {
                    return false;
                }
                if (admin.permittedInputMethods == null) {
                    return true;
                }
                return checkPackagesInPermittedListOrSystem(Collections.singletonList(packageName), admin.permittedInputMethods, userHandle);
            }
        }
        return true;
    }

    public boolean setPermittedCrossProfileNotificationListeners(ComponentName who, List<String> packageList) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            if (isManagedProfile(caller.getUserId())) {
                synchronized (getLockObject()) {
                    ActiveAdmin admin = getProfileOwnerLocked(caller);
                    admin.permittedNotificationListeners = packageList;
                    saveSettingsLocked(caller.getUserId());
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public List<String> getPermittedCrossProfileNotificationListeners(ComponentName who) {
        List<String> list;
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            list = admin.permittedNotificationListeners;
        }
        return list;
    }

    public boolean isNotificationListenerServicePermitted(String packageName, int userId) {
        if (this.mHasFeature) {
            Preconditions.checkStringNotEmpty(packageName, "packageName is null or empty");
            Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "query if a notification listener service is permitted"));
            synchronized (getLockObject()) {
                ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userId);
                if (profileOwner != null && profileOwner.permittedNotificationListeners != null) {
                    return checkPackagesInPermittedListOrSystem(Collections.singletonList(packageName), profileOwner.permittedNotificationListeners, userId);
                }
                return true;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeSendAdminEnabledBroadcastLocked(int userHandle) {
        DevicePolicyData policyData = m3013x8b75536b(userHandle);
        if (policyData.mAdminBroadcastPending) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userHandle);
            boolean clearInitBundle = true;
            if (admin != null) {
                PersistableBundle initBundle = policyData.mInitBundle;
                clearInitBundle = sendAdminCommandLocked(admin, "android.app.action.DEVICE_ADMIN_ENABLED", initBundle == null ? null : new Bundle(initBundle), null, true);
            }
            if (clearInitBundle) {
                policyData.mInitBundle = null;
                policyData.mAdminBroadcastPending = false;
                saveSettingsLocked(userHandle);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [10869=5] */
    /* JADX WARN: Not initialized variable reg: 25, insn: 0x01d9: MOVE  (r1 I:??[OBJECT, ARRAY]) = (r25 I:??[OBJECT, ARRAY] A[D('user' android.os.UserHandle)]), block:B:111:0x01d9 */
    public UserHandle createAndManageUser(ComponentName admin, String name, ComponentName profileOwner, PersistableBundle adminExtras, int flags) {
        int i;
        Objects.requireNonNull(admin, "admin is null");
        Objects.requireNonNull(profileOwner, "profileOwner is null");
        if (!admin.getPackageName().equals(profileOwner.getPackageName())) {
            throw new IllegalArgumentException("profileOwner " + profileOwner + " and admin " + admin + " are not in the same package");
        }
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(caller.getUserHandle().isSystem(), "createAndManageUser was called from non-system user");
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(5);
        boolean ephemeral = (flags & 2) != 0;
        boolean demo = (flags & 4) != 0 && UserManager.isDeviceInDemoMode(this.mContext);
        boolean leaveAllSystemAppsEnabled = (flags & 16) != 0;
        synchronized (getLockObject()) {
            try {
                try {
                    long id = this.mInjector.binderClearCallingIdentity();
                    try {
                        int targetSdkVersion = this.mInjector.getPackageManagerInternal().getUidTargetSdkVersion(caller.getUid());
                        DeviceStorageMonitorInternal deviceStorageMonitorInternal = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
                        try {
                            try {
                                if (deviceStorageMonitorInternal.isMemoryLow()) {
                                    if (targetSdkVersion < 28) {
                                        return null;
                                    }
                                    throw new ServiceSpecificException(5, "low device storage");
                                }
                                String userType = demo ? "android.os.usertype.full.DEMO" : "android.os.usertype.full.SECONDARY";
                                int userInfoFlags = ephemeral ? 256 : 0;
                                if (!this.mUserManager.canAddMoreUsers(userType)) {
                                    if (targetSdkVersion < 28) {
                                        return null;
                                    }
                                    throw new ServiceSpecificException(6, "user limit reached");
                                }
                                String[] disallowedPackages = !leaveAllSystemAppsEnabled ? (String[]) this.mOverlayPackagesProvider.getNonRequiredApps(admin, UserHandle.myUserId(), "android.app.action.PROVISION_MANAGED_USER").toArray(new String[0]) : null;
                                Object token = new Object();
                                UserHandle user = null;
                                try {
                                    try {
                                        Slogf.d(LOG_TAG, "Adding new pending token: " + token);
                                        this.mPendingUserCreatedCallbackTokens.add(token);
                                        try {
                                            UserInfo userInfo = this.mUserManagerInternal.createUserEvenWhenDisallowed(name, userType, userInfoFlags, disallowedPackages, token);
                                            UserHandle user2 = userInfo != null ? userInfo.getUserHandle() : null;
                                            user = user2;
                                        } catch (UserManager.CheckedUserOperationException e) {
                                            Slogf.e(LOG_TAG, "Couldn't createUserEvenWhenDisallowed", (Throwable) e);
                                        }
                                        if (user == null) {
                                            if (targetSdkVersion < 28) {
                                                return null;
                                            }
                                            throw new ServiceSpecificException(1, "failed to create user");
                                        }
                                        int userHandle = user.getIdentifier();
                                        id = this.mInjector.binderClearCallingIdentity();
                                        try {
                                            maybeInstallDevicePolicyManagementRoleHolderInUser(userHandle);
                                            i = 28;
                                            try {
                                                manageUserUnchecked(admin, profileOwner, userHandle, adminExtras, true);
                                                if ((flags & 1) != 0) {
                                                    try {
                                                        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, userHandle);
                                                    } catch (Throwable th) {
                                                        re = th;
                                                        id = id;
                                                        try {
                                                            this.mUserManager.removeUser(userHandle);
                                                            if (targetSdkVersion < i) {
                                                                return null;
                                                            }
                                                            throw new ServiceSpecificException(1, re.getMessage());
                                                        } finally {
                                                            this.mInjector.binderRestoreCallingIdentity(id);
                                                        }
                                                    }
                                                }
                                                sendProvisioningCompletedBroadcast(userHandle, "android.app.action.PROVISION_MANAGED_USER", leaveAllSystemAppsEnabled);
                                                return user;
                                            } catch (Throwable th2) {
                                                re = th2;
                                                id = id;
                                            }
                                        } catch (Throwable th3) {
                                            re = th3;
                                            id = id;
                                            i = 28;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                        throw th;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th9) {
                        th = th9;
                    }
                } catch (Throwable th10) {
                    th = th10;
                }
            } catch (Throwable th11) {
                th = th11;
            }
        }
    }

    private void sendProvisioningCompletedBroadcast(int user, String action, boolean leaveAllSystemAppsEnabled) {
        Intent intent = new Intent("android.app.action.PROVISIONING_COMPLETED").putExtra("android.intent.extra.user_handle", user).putExtra("android.intent.extra.USER", UserHandle.of(user)).putExtra("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", leaveAllSystemAppsEnabled).putExtra("android.app.extra.PROVISIONING_ACTION", action).setPackage(getManagedProvisioningPackage(this.mContext)).addFlags(268435456);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

    private void manageUserUnchecked(ComponentName admin, ComponentName profileOwner, final int userId, PersistableBundle adminExtras, boolean showDisclaimer) {
        String str;
        synchronized (getLockObject()) {
        }
        final String adminPkg = admin.getPackageName();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda145
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3046x951eb2bd(adminPkg, userId);
            }
        });
        setActiveAdmin(profileOwner, true, userId);
        String ownerName = getProfileOwnerNameUnchecked(Process.myUserHandle().getIdentifier());
        setProfileOwner(profileOwner, ownerName, userId);
        synchronized (getLockObject()) {
            DevicePolicyData policyData = m3013x8b75536b(userId);
            policyData.mInitBundle = adminExtras;
            policyData.mAdminBroadcastPending = true;
            if (showDisclaimer) {
                str = "needed";
            } else {
                str = "not_needed";
            }
            policyData.mNewUserDisclaimer = str;
            saveSettingsLocked(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$manageUserUnchecked$81$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3046x951eb2bd(String adminPkg, int userId) throws Exception {
        try {
            if (!this.mIPackageManager.isPackageAvailable(adminPkg, userId)) {
                this.mIPackageManager.installExistingPackageAsUser(adminPkg, userId, 4194304, 1, (List) null);
            }
        } catch (RemoteException e) {
            Slogf.wtf(LOG_TAG, e, "Failed to install admin package %s for user %d", adminPkg, Integer.valueOf(userId));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNewUserCreated(UserInfo user, Object token) {
        int userId = user.id;
        if (token != null) {
            synchronized (getLockObject()) {
                if (this.mPendingUserCreatedCallbackTokens.contains(token)) {
                    Slogf.d(LOG_TAG, "handleNewUserCreated(): ignoring for user " + userId + " due to token " + token);
                    this.mPendingUserCreatedCallbackTokens.remove(token);
                    return;
                }
            }
        }
        if (!this.mOwners.hasDeviceOwner() || !user.isFull() || user.isManagedProfile() || user.isGuest()) {
            return;
        }
        if (this.mInjector.userManagerIsHeadlessSystemUserMode()) {
            ComponentName admin = this.mOwners.getDeviceOwnerComponent();
            Slogf.i(LOG_TAG, "Automatically setting profile owner (" + admin + ") on new user " + userId);
            manageUserUnchecked(admin, admin, userId, null, true);
            return;
        }
        Slogf.i(LOG_TAG, "User %d added on DO mode; setting ShowNewUserDisclaimer", Integer.valueOf(userId));
        setShowNewUserDisclaimer(userId, "needed");
    }

    public void acknowledgeNewUserDisclaimer(int userId) {
        CallerIdentity callerIdentity = getCallerIdentity();
        Preconditions.checkCallAuthorization(canManageUsers(callerIdentity) || hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS"));
        setShowNewUserDisclaimer(userId, "acked");
    }

    private void setShowNewUserDisclaimer(int userId, String value) {
        Slogf.i(LOG_TAG, "Setting new user disclaimer for user " + userId + " as " + value);
        synchronized (getLockObject()) {
            DevicePolicyData policyData = m3013x8b75536b(userId);
            policyData.mNewUserDisclaimer = value;
            saveSettingsLocked(userId);
        }
    }

    private void showNewUserDisclaimerIfNecessary(int userId) {
        boolean mustShow;
        synchronized (getLockObject()) {
            DevicePolicyData policyData = m3013x8b75536b(userId);
            mustShow = "needed".equals(policyData.mNewUserDisclaimer);
        }
        if (mustShow) {
            Intent intent = new Intent("android.app.action.SHOW_NEW_USER_DISCLAIMER");
            Slogf.i(LOG_TAG, "Dispatching ACTION_SHOW_NEW_USER_DISCLAIMER intent");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
        }
    }

    public boolean isNewUserDisclaimerAcknowledged(int userId) {
        boolean isNewUserDisclaimerAcknowledged;
        CallerIdentity callerIdentity = getCallerIdentity();
        Preconditions.checkCallAuthorization(canManageUsers(callerIdentity) || hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS"));
        synchronized (getLockObject()) {
            DevicePolicyData policyData = m3013x8b75536b(userId);
            isNewUserDisclaimerAcknowledged = policyData.isNewUserDisclaimerAcknowledged();
        }
        return isNewUserDisclaimerAcknowledged;
    }

    public boolean removeUser(final ComponentName who, final UserHandle userHandle) {
        Objects.requireNonNull(who, "ComponentName is null");
        Objects.requireNonNull(userHandle, "UserHandle is null");
        final CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(6);
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda122
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3059xf44166a7(userHandle, who, caller);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeUser$82$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3059xf44166a7(UserHandle userHandle, ComponentName who, CallerIdentity caller) throws Exception {
        String restriction;
        if (isManagedProfile(userHandle.getIdentifier())) {
            restriction = "no_remove_managed_profile";
        } else {
            restriction = "no_remove_user";
        }
        if (isAdminAffectedByRestriction(who, restriction, caller.getUserId())) {
            Slogf.w(LOG_TAG, "The device owner cannot remove a user because %s is enabled, and was not set by the device owner", restriction);
            return false;
        }
        return Boolean.valueOf(this.mUserManagerInternal.removeUserEvenWhenDisallowed(userHandle.getIdentifier()));
    }

    private boolean isAdminAffectedByRestriction(ComponentName admin, String userRestriction, int userId) {
        switch (this.mUserManager.getUserRestrictionSource(userRestriction, UserHandle.of(userId))) {
            case 0:
                return false;
            case 1:
            case 3:
            default:
                return true;
            case 2:
                return !isDeviceOwner(admin, userId);
            case 4:
                return !isProfileOwner(admin, userId);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11122=4] */
    public boolean switchUser(ComponentName who, UserHandle userHandle) {
        boolean switched;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(2);
        int logoutUserId = getLogoutUserIdUnchecked();
        synchronized (getLockObject()) {
            long id = this.mInjector.binderClearCallingIdentity();
            int userId = 0;
            if (userHandle != null) {
                try {
                    userId = userHandle.getIdentifier();
                } catch (RemoteException e) {
                    Slogf.e(LOG_TAG, "Couldn't switch user", e);
                    this.mInjector.binderRestoreCallingIdentity(id);
                    if (0 == 0) {
                        setLogoutUserIdLocked(logoutUserId);
                    }
                    return false;
                }
            }
            Slogf.i(LOG_TAG, "Switching to user %d (logout user is %d)", Integer.valueOf(userId), Integer.valueOf(logoutUserId));
            setLogoutUserIdLocked(-2);
            switched = this.mInjector.getIActivityManager().switchUser(userId);
            if (switched) {
                Slogf.d(LOG_TAG, "Switched");
            } else {
                Slogf.w(LOG_TAG, "Failed to switch to user %d", Integer.valueOf(userId));
            }
            this.mInjector.binderRestoreCallingIdentity(id);
            if (!switched) {
                setLogoutUserIdLocked(logoutUserId);
            }
        }
        return switched;
    }

    public int getLogoutUserId() {
        Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS"));
        return getLogoutUserIdUnchecked();
    }

    private int getLogoutUserIdUnchecked() {
        int i;
        synchronized (getLockObject()) {
            i = this.mLogoutUserId;
        }
        return i;
    }

    private void clearLogoutUser() {
        synchronized (getLockObject()) {
            setLogoutUserIdLocked(-10000);
        }
    }

    private void setLogoutUserIdLocked(int userId) {
        if (userId == -2) {
            userId = getCurrentForegroundUserId();
        }
        Slogf.d(LOG_TAG, "setLogoutUserId(): %d -> %d", Integer.valueOf(this.mLogoutUserId), Integer.valueOf(userId));
        this.mLogoutUserId = userId;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11192=5] */
    public int startUserInBackground(ComponentName who, UserHandle userHandle) {
        Objects.requireNonNull(who, "ComponentName is null");
        Objects.requireNonNull(userHandle, "UserHandle is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(3);
        int userId = userHandle.getIdentifier();
        if (isManagedProfile(userId)) {
            Slogf.w(LOG_TAG, "Managed profile cannot be started in background");
            return 2;
        }
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            if (!this.mInjector.getActivityManagerInternal().canStartMoreUsers()) {
                Slogf.w(LOG_TAG, "Cannot start user %d, too many users in background", Integer.valueOf(userId));
                return 3;
            }
            Slogf.i(LOG_TAG, "Starting user %d in background", Integer.valueOf(userId));
            if (this.mInjector.getIActivityManager().startUserInBackground(userId)) {
                return 0;
            }
            Slogf.w(LOG_TAG, "failed to start user %d in background", Integer.valueOf(userId));
            return 1;
        } catch (RemoteException e) {
            return 1;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public int stopUser(ComponentName who, UserHandle userHandle) {
        Objects.requireNonNull(who, "ComponentName is null");
        Objects.requireNonNull(userHandle, "UserHandle is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(4);
        int userId = userHandle.getIdentifier();
        if (isManagedProfile(userId)) {
            Slogf.w(LOG_TAG, "Managed profile cannot be stopped");
            return 2;
        }
        return stopUserUnchecked(userId);
    }

    public int logoutUser(ComponentName who) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(9);
        int callingUserId = caller.getUserId();
        synchronized (getLockObject()) {
            if (!isUserAffiliatedWithDeviceLocked(callingUserId)) {
                throw new SecurityException("Admin " + who + " is neither the device owner or affiliated user's profile owner.");
            }
        }
        if (isManagedProfile(callingUserId)) {
            Slogf.w(LOG_TAG, "Managed profile cannot be logout");
            return 2;
        } else if (callingUserId != ((Integer) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda168
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3045xbadff58c();
            }
        })).intValue()) {
            Slogf.d(LOG_TAG, "logoutUser(): user %d is in background, just stopping, not switching", Integer.valueOf(callingUserId));
            return stopUserUnchecked(callingUserId);
        } else {
            return logoutUserUnchecked(callingUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$logoutUser$83$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Integer m3045xbadff58c() throws Exception {
        return Integer.valueOf(getCurrentForegroundUserId());
    }

    public int logoutUserInternal() {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(canManageUsers(caller) || hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS"));
        int currentUserId = getCurrentForegroundUserId();
        int result = logoutUserUnchecked(currentUserId);
        return result;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11283=4] */
    private int logoutUserUnchecked(int userIdToStop) {
        int logoutUserId = getLogoutUserIdUnchecked();
        if (logoutUserId == -10000) {
            Slogf.w(LOG_TAG, "logoutUser(): could not determine which user to switch to");
            return 1;
        }
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            Slogf.i(LOG_TAG, "logoutUser(): switching to user %d", Integer.valueOf(logoutUserId));
            if (!this.mInjector.getIActivityManager().switchUser(logoutUserId)) {
                Slogf.w(LOG_TAG, "Failed to switch to user %d", Integer.valueOf(logoutUserId));
                return 1;
            }
            clearLogoutUser();
            this.mInjector.binderRestoreCallingIdentity(id);
            return stopUserUnchecked(userIdToStop);
        } catch (RemoteException e) {
            return 1;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11305=5] */
    private int stopUserUnchecked(int userId) {
        Slogf.i(LOG_TAG, "Stopping user %d", Integer.valueOf(userId));
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            switch (this.mInjector.getIActivityManager().stopUser(userId, true, (IStopUserCallback) null)) {
                case -2:
                    return 4;
                case -1:
                default:
                    return 1;
                case 0:
                    return 0;
            }
        } catch (RemoteException e) {
            return 1;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public List<UserHandle> getSecondaryUsers(ComponentName who) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        return (List) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda58
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3009x5cfde486();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getSecondaryUsers$84$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ List m3009x5cfde486() throws Exception {
        List<UserInfo> userInfos = this.mInjector.getUserManager().getAliveUsers();
        List<UserHandle> userHandles = new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            UserHandle userHandle = userInfo.getUserHandle();
            if (!userHandle.isSystem() && !isManagedProfile(userHandle.getIdentifier())) {
                userHandles.add(userInfo.getUserHandle());
            }
        }
        return userHandles;
    }

    public boolean isEphemeralUser(ComponentName who) {
        Objects.requireNonNull(who, "ComponentName is null");
        final CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda159
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3029x5abe69b9(caller);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isEphemeralUser$85$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3029x5abe69b9(CallerIdentity caller) throws Exception {
        return Boolean.valueOf(this.mInjector.getUserManager().isUserEphemeral(caller.getUserId()));
    }

    public Bundle getApplicationRestrictions(ComponentName who, String callerPackage, final String packageName) {
        final CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-app-restrictions")));
        return (Bundle) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda138
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m2998x50d8b795(packageName, caller);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getApplicationRestrictions$86$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Bundle m2998x50d8b795(String packageName, CallerIdentity caller) throws Exception {
        Bundle bundle = this.mUserManager.getApplicationRestrictions(packageName, caller.getUserHandle());
        return bundle != null ? bundle : Bundle.EMPTY;
    }

    private String[] populateNonExemptAndExemptFromPolicyApps(String[] packageNames, Set<String> outputExemptApps) {
        Preconditions.checkArgument(outputExemptApps.isEmpty(), "outputExemptApps is not empty");
        List<String> exemptAppsList = listPolicyExemptAppsUnchecked();
        if (exemptAppsList.isEmpty()) {
            return packageNames;
        }
        Set<String> exemptApps = new HashSet<>(exemptAppsList);
        List<String> nonExemptApps = new ArrayList<>(packageNames.length);
        for (String app : packageNames) {
            if (exemptApps.contains(app)) {
                outputExemptApps.add(app);
            } else {
                nonExemptApps.add(app);
            }
        }
        int i = nonExemptApps.size();
        String[] result = new String[i];
        nonExemptApps.toArray(result);
        return result;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11415=4] */
    /* JADX WARN: Removed duplicated region for block: B:36:0x008d  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x008f  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x009d  */
    /* JADX WARN: Removed duplicated region for block: B:42:0x00af  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public String[] setPackagesSuspended(ComponentName who, String callerPackage, String[] packageNames, boolean suspended) {
        long id;
        Injector injector;
        Objects.requireNonNull(packageNames, "array of packages cannot be null");
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-package-access")));
        checkCanExecuteOrThrowUnsafe(20);
        Set<String> exemptApps = new HashSet<>();
        String[] packageNames2 = populateNonExemptAndExemptFromPolicyApps(packageNames, exemptApps);
        String[] nonSuspendedPackages = null;
        synchronized (getLockObject()) {
            long id2 = this.mInjector.binderClearCallingIdentity();
            try {
                id = id2;
                try {
                    try {
                        nonSuspendedPackages = this.mIPackageManager.setPackagesSuspendedAsUser(packageNames2, suspended, (PersistableBundle) null, (PersistableBundle) null, (SuspendDialogInfo) null, PackageManagerService.PLATFORM_PACKAGE_NAME, caller.getUserId());
                        injector = this.mInjector;
                    } catch (RemoteException e) {
                        re = e;
                        Slogf.e(LOG_TAG, "Failed talking to the package manager", re);
                        injector = this.mInjector;
                        injector.binderRestoreCallingIdentity(id);
                        DevicePolicyEventLogger.createEvent(68).setAdmin(caller.getPackageName()).setBoolean(who != null).setStrings(packageNames2).write();
                        if (nonSuspendedPackages != null) {
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    this.mInjector.binderRestoreCallingIdentity(id);
                    throw th;
                }
            } catch (RemoteException e2) {
                re = e2;
                id = id2;
            } catch (Throwable th2) {
                th = th2;
                id = id2;
                this.mInjector.binderRestoreCallingIdentity(id);
                throw th;
            }
            injector.binderRestoreCallingIdentity(id);
        }
        DevicePolicyEventLogger.createEvent(68).setAdmin(caller.getPackageName()).setBoolean(who != null).setStrings(packageNames2).write();
        if (nonSuspendedPackages != null) {
            Slogf.w(LOG_TAG, "PM failed to suspend packages (%s)", Arrays.toString(packageNames2));
            return packageNames2;
        } else if (exemptApps.isEmpty()) {
            return nonSuspendedPackages;
        } else {
            String[] result = buildNonSuspendedPackagesUnionArray(nonSuspendedPackages, exemptApps);
            return result;
        }
    }

    private String[] buildNonSuspendedPackagesUnionArray(String[] nonSuspendedPackages, Set<String> exemptApps) {
        String[] result = new String[nonSuspendedPackages.length + exemptApps.size()];
        int index = 0;
        int length = nonSuspendedPackages.length;
        int i = 0;
        while (i < length) {
            String app = nonSuspendedPackages[i];
            result[index] = app;
            i++;
            index++;
        }
        for (String app2 : exemptApps) {
            result[index] = app2;
            index++;
        }
        return result;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11470=4] */
    public boolean isPackageSuspended(ComponentName who, String callerPackage, String packageName) {
        boolean isPackageSuspendedForUser;
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-package-access")));
        synchronized (getLockObject()) {
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                isPackageSuspendedForUser = this.mIPackageManager.isPackageSuspendedForUser(packageName, caller.getUserId());
                this.mInjector.binderRestoreCallingIdentity(id);
            } catch (RemoteException re) {
                Slogf.e(LOG_TAG, "Failed talking to the package manager", re);
                this.mInjector.binderRestoreCallingIdentity(id);
                return false;
            }
        }
        return isPackageSuspendedForUser;
    }

    public List<String> listPolicyExemptApps() {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS") || isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        return listPolicyExemptAppsUnchecked();
    }

    private List<String> listPolicyExemptAppsUnchecked() {
        String[] core = this.mContext.getResources().getStringArray(17236178);
        String[] vendor2 = this.mContext.getResources().getStringArray(17236203);
        int size = core.length + vendor2.length;
        Set<String> apps = new ArraySet<>(size);
        for (String app : core) {
            apps.add(app);
        }
        for (String app2 : vendor2) {
            apps.add(app2);
        }
        return new ArrayList(apps);
    }

    public void setUserRestriction(ComponentName who, String key, boolean enabledFromThisOwner, boolean parent) {
        int eventId;
        int eventTag;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        if (!UserRestrictionsUtils.isValidRestriction(key)) {
            return;
        }
        int userHandle = caller.getUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getParentOfAdminIfRequired(getProfileOwnerOrDeviceOwnerLocked(caller), parent);
            if (isDefaultDeviceOwner(caller)) {
                if (!UserRestrictionsUtils.canDeviceOwnerChange(key)) {
                    throw new SecurityException("Device owner cannot set user restriction " + key);
                }
                Preconditions.checkArgument(!parent, "Cannot use the parent instance in Device Owner mode");
            } else if (isFinancedDeviceOwner(caller)) {
                if (!UserRestrictionsUtils.canFinancedDeviceOwnerChange(key)) {
                    throw new SecurityException("Cannot set user restriction " + key + " when managing a financed device");
                }
                Preconditions.checkArgument(!parent, "Cannot use the parent instance in Financed Device Owner mode");
            } else {
                boolean profileOwnerCanChangeOnItself = !parent && UserRestrictionsUtils.canProfileOwnerChange(key, userHandle);
                boolean orgOwnedProfileOwnerCanChangesGlobally = parent && isProfileOwnerOfOrganizationOwnedDevice(caller) && UserRestrictionsUtils.canProfileOwnerOfOrganizationOwnedDeviceChange(key);
                if (!profileOwnerCanChangeOnItself && !orgOwnedProfileOwnerCanChangesGlobally) {
                    throw new SecurityException("Profile owner cannot set user restriction " + key);
                }
            }
            checkCanExecuteOrThrowUnsafe(10);
            Bundle restrictions = activeAdmin.ensureUserRestrictions();
            if (enabledFromThisOwner) {
                restrictions.putBoolean(key, true);
            } else {
                restrictions.remove(key);
            }
            saveUserRestrictionsLocked(userHandle);
        }
        if (enabledFromThisOwner) {
            eventId = 12;
        } else {
            eventId = 13;
        }
        DevicePolicyEventLogger admin = DevicePolicyEventLogger.createEvent(eventId).setAdmin(caller.getComponentName());
        String[] strArr = new String[2];
        strArr[0] = key;
        strArr[1] = parent ? CALLED_FROM_PARENT : NOT_CALLED_FROM_PARENT;
        admin.setStrings(strArr).write();
        if (SecurityLog.isLoggingEnabled()) {
            if (enabledFromThisOwner) {
                eventTag = 210027;
            } else {
                eventTag = 210028;
            }
            SecurityLog.writeEvent(eventTag, new Object[]{who.getPackageName(), Integer.valueOf(userHandle), key});
        }
    }

    private void saveUserRestrictionsLocked(int userId) {
        saveSettingsLocked(userId);
        pushUserRestrictions(userId);
        sendChangedNotification(userId);
    }

    private void pushUserRestrictions(int originatingUserId) {
        Bundle global;
        RestrictionsSet local = new RestrictionsSet();
        synchronized (getLockObject()) {
            boolean isDeviceOwner = this.mOwners.isDeviceOwnerUserId(originatingUserId);
            if (isDeviceOwner) {
                ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
                if (deviceOwner == null) {
                    return;
                }
                global = deviceOwner.getGlobalUserRestrictions(0);
                local.updateRestrictions(originatingUserId, deviceOwner.getLocalUserRestrictions(0));
            } else {
                ActiveAdmin profileOwner = getProfileOwnerAdminLocked(originatingUserId);
                if (profileOwner == null) {
                    return;
                }
                global = profileOwner.getGlobalUserRestrictions(1);
                local.updateRestrictions(originatingUserId, profileOwner.getLocalUserRestrictions(1));
                if (isProfileOwnerOfOrganizationOwnedDevice(profileOwner.getUserHandle().getIdentifier())) {
                    UserRestrictionsUtils.merge(global, profileOwner.getParentActiveAdmin().getGlobalUserRestrictions(2));
                    local.updateRestrictions(getProfileParentId(profileOwner.getUserHandle().getIdentifier()), profileOwner.getParentActiveAdmin().getLocalUserRestrictions(2));
                }
            }
            this.mUserManagerInternal.setDevicePolicyUserRestrictions(originatingUserId, global, local, isDeviceOwner);
        }
    }

    public Bundle getUserRestrictions(ComponentName who, boolean parent) {
        Bundle bundle;
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isFinancedDeviceOwner(caller) || isProfileOwner(caller) || (parent && isProfileOwnerOfOrganizationOwnedDevice(caller)));
        synchronized (getLockObject()) {
            ActiveAdmin activeAdmin = getParentOfAdminIfRequired(getProfileOwnerOrDeviceOwnerLocked(caller), parent);
            bundle = activeAdmin.userRestrictions;
        }
        return bundle;
    }

    public boolean setApplicationHidden(ComponentName who, String callerPackage, final String packageName, final boolean hidden, boolean parent) {
        boolean result;
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-package-access")));
        List<String> exemptApps = listPolicyExemptAppsUnchecked();
        if (exemptApps.contains(packageName)) {
            Slogf.d(LOG_TAG, "setApplicationHidden(): ignoring %s as it's on policy-exempt list", packageName);
            return false;
        }
        final int userId = caller.getUserId();
        if (parent) {
            userId = getProfileParentId(userId);
        }
        synchronized (getLockObject()) {
            if (parent) {
                Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller.getUserId()) && isManagedProfile(caller.getUserId()));
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda127
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3069xe7b1a5eb(packageName, userId);
                    }
                });
            }
            checkCanExecuteOrThrowUnsafe(15);
            result = ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda128
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3070xedb5714a(packageName, hidden, userId);
                }
            })).booleanValue();
        }
        DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(63).setAdmin(caller.getPackageName()).setBoolean(who == null);
        String[] strArr = new String[3];
        strArr[0] = packageName;
        strArr[1] = hidden ? "hidden" : "not_hidden";
        strArr[2] = parent ? CALLED_FROM_PARENT : NOT_CALLED_FROM_PARENT;
        devicePolicyEventLogger.setStrings(strArr).write();
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setApplicationHidden$88$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3070xedb5714a(String packageName, boolean hidden, int userId) throws Exception {
        return Boolean.valueOf(this.mIPackageManager.setApplicationHiddenSettingAsUser(packageName, hidden, userId));
    }

    public boolean isApplicationHidden(ComponentName who, String callerPackage, final String packageName, boolean parent) {
        boolean booleanValue;
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        boolean z = true;
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-package-access")));
        final int userId = caller.getUserId();
        if (parent) {
            userId = getProfileParentId(userId);
        }
        synchronized (getLockObject()) {
            if (parent) {
                if (!isProfileOwnerOfOrganizationOwnedDevice(caller.getUserId()) || !isManagedProfile(caller.getUserId())) {
                    z = false;
                }
                Preconditions.checkCallAuthorization(z);
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda135
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3023x39cf7f47(packageName, userId);
                    }
                });
            }
            booleanValue = ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda136
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3024xbe22f971(packageName, userId);
                }
            })).booleanValue();
        }
        return booleanValue;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isApplicationHidden$90$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3024xbe22f971(String packageName, int userId) throws Exception {
        return Boolean.valueOf(this.mIPackageManager.getApplicationHiddenSettingAsUser(packageName, userId));
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: enforcePackageIsSystemPackage */
    public void m3069xe7b1a5eb(String packageName, int userId) throws RemoteException {
        boolean isSystem;
        try {
            isSystem = isSystemApp(this.mIPackageManager, packageName, userId);
        } catch (IllegalArgumentException e) {
            isSystem = false;
        }
        if (!isSystem) {
            throw new IllegalArgumentException("The provided package is not a system package");
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11771=5] */
    /* JADX WARN: Can't wrap try/catch for region: R(14:10|11|12|(3:45|46|(10:48|15|16|17|18|19|(1:21)|23|24|25))|14|15|16|17|18|19|(0)|23|24|25) */
    /* JADX WARN: Can't wrap try/catch for region: R(7:(3:45|46|(10:48|15|16|17|18|19|(1:21)|23|24|25))|18|19|(0)|23|24|25) */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x0085, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x0087, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x0088, code lost:
        r10 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x008a, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x008b, code lost:
        r10 = r2;
     */
    /* JADX WARN: Removed duplicated region for block: B:32:0x0073 A[Catch: RemoteException -> 0x0085, all -> 0x00cd, TRY_LEAVE, TryCatch #5 {RemoteException -> 0x0085, blocks: (B:30:0x006e, B:32:0x0073), top: B:58:0x006e }] */
    /* JADX WARN: Removed duplicated region for block: B:47:0x00b8  */
    /* JADX WARN: Removed duplicated region for block: B:48:0x00ba  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void enableSystemApp(ComponentName who, String callerPackage, String packageName) {
        long id;
        boolean z;
        Injector injector;
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-enable-system-app")));
        synchronized (getLockObject()) {
            boolean isDemo = isCurrentUserDemo();
            int userId = caller.getUserId();
            long id2 = this.mInjector.binderClearCallingIdentity();
            try {
                if (!isDemo) {
                    try {
                    } catch (RemoteException e) {
                        re = e;
                        id = id2;
                        Slogf.wtf(LOG_TAG, "Failed to install " + packageName, re);
                        injector = this.mInjector;
                        injector.binderRestoreCallingIdentity(id);
                        DevicePolicyEventLogger.createEvent(64).setAdmin(caller.getPackageName()).setBoolean(who == null).setStrings(new String[]{packageName}).write();
                    } catch (Throwable th) {
                        th = th;
                        id = id2;
                        this.mInjector.binderRestoreCallingIdentity(id);
                        throw th;
                    }
                    if (!isSystemApp(this.mIPackageManager, packageName, getProfileParentId(userId))) {
                        z = false;
                        Preconditions.checkArgument(z, "Only system apps can be enabled this way");
                        id = id2;
                        this.mIPackageManager.installExistingPackageAsUser(packageName, userId, 4194304, 1, (List) null);
                        if (isDemo) {
                            this.mIPackageManager.setApplicationEnabledSetting(packageName, 1, 1, userId, LOG_TAG);
                        }
                        injector = this.mInjector;
                        injector.binderRestoreCallingIdentity(id);
                    }
                }
                this.mIPackageManager.installExistingPackageAsUser(packageName, userId, 4194304, 1, (List) null);
                if (isDemo) {
                }
                injector = this.mInjector;
                injector.binderRestoreCallingIdentity(id);
            } catch (Throwable th2) {
                th = th2;
                this.mInjector.binderRestoreCallingIdentity(id);
                throw th;
            }
            z = true;
            Preconditions.checkArgument(z, "Only system apps can be enabled this way");
            id = id2;
        }
        DevicePolicyEventLogger.createEvent(64).setAdmin(caller.getPackageName()).setBoolean(who == null).setStrings(new String[]{packageName}).write();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11827=4] */
    public int enableSystemAppWithIntent(ComponentName who, String callerPackage, Intent intent) {
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-enable-system-app")));
        int numberOfAppsInstalled = 0;
        synchronized (getLockObject()) {
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                int parentUserId = getProfileParentId(caller.getUserId());
                List<ResolveInfo> activitiesToEnable = this.mIPackageManager.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432L, parentUserId).getList();
                if (activitiesToEnable != null) {
                    for (ResolveInfo info : activitiesToEnable) {
                        if (info.activityInfo != null) {
                            String packageName = info.activityInfo.packageName;
                            if (isSystemApp(this.mIPackageManager, packageName, parentUserId)) {
                                numberOfAppsInstalled++;
                                this.mIPackageManager.installExistingPackageAsUser(packageName, caller.getUserId(), 4194304, 1, (List) null);
                            } else {
                                Slogf.d(LOG_TAG, "Not enabling " + packageName + " since is not a system app");
                            }
                        }
                    }
                }
                this.mInjector.binderRestoreCallingIdentity(id);
            } catch (RemoteException e) {
                Slogf.wtf(LOG_TAG, "Failed to resolve intent for: " + intent);
                this.mInjector.binderRestoreCallingIdentity(id);
                return 0;
            }
        }
        DevicePolicyEventLogger.createEvent(65).setAdmin(caller.getPackageName()).setBoolean(who == null).setStrings(new String[]{intent.getAction()}).write();
        return numberOfAppsInstalled;
    }

    private boolean isSystemApp(IPackageManager pm, String packageName, int userId) throws RemoteException {
        ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 8192L, userId);
        if (appInfo != null) {
            return (appInfo.flags & 1) != 0;
        }
        throw new IllegalArgumentException("The application " + packageName + " is not present on this device");
    }

    public boolean installExistingPackage(ComponentName who, String callerPackage, String packageName) {
        boolean result;
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-install-existing-package")));
        synchronized (getLockObject()) {
            Preconditions.checkCallAuthorization(isUserAffiliatedWithDeviceLocked(caller.getUserId()), "Admin %s is neither the device owner or affiliated user's profile owner.", new Object[]{who});
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                result = this.mIPackageManager.installExistingPackageAsUser(packageName, caller.getUserId(), 4194304, 1, (List) null) == 1;
            } catch (RemoteException e) {
                return false;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        if (result) {
            DevicePolicyEventLogger.createEvent(66).setAdmin(caller.getPackageName()).setBoolean(who == null).setStrings(new String[]{packageName}).write();
        }
        return result;
    }

    public void setAccountManagementDisabled(ComponentName who, String accountType, boolean disabled, boolean parent) {
        ActiveAdmin ap;
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        enforceMaxStringLength(accountType, "account type");
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            if (parent) {
                ap = getParentOfAdminIfRequired(getOrganizationOwnedProfileOwnerLocked(caller), parent);
            } else {
                ActiveAdmin ap2 = getProfileOwnerOrDeviceOwnerLocked(caller);
                ap = getParentOfAdminIfRequired(ap2, parent);
            }
            if (disabled) {
                ap.accountTypesWithManagementDisabled.add(accountType);
            } else {
                ap.accountTypesWithManagementDisabled.remove(accountType);
            }
            saveSettingsLocked(UserHandle.getCallingUserId());
        }
    }

    public String[] getAccountTypesWithManagementDisabled() {
        return getAccountTypesWithManagementDisabledAsUser(UserHandle.getCallingUserId(), false);
    }

    public String[] getAccountTypesWithManagementDisabledAsUser(int userId, boolean parent) {
        String[] strArr;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkArgumentNonnegative(userId, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userId));
        synchronized (getLockObject()) {
            ArraySet<String> resultSet = new ArraySet<>();
            if (!parent) {
                DevicePolicyData policy = m3013x8b75536b(userId);
                Iterator<ActiveAdmin> it = policy.mAdminList.iterator();
                while (it.hasNext()) {
                    ActiveAdmin admin = it.next();
                    resultSet.addAll(admin.accountTypesWithManagementDisabled);
                }
            }
            ActiveAdmin orgOwnedAdmin = getProfileOwnerOfOrganizationOwnedDeviceLocked(userId);
            boolean shouldGetParentAccounts = orgOwnedAdmin != null && (parent || UserHandle.getUserId(orgOwnedAdmin.getUid()) != userId);
            if (shouldGetParentAccounts) {
                resultSet.addAll(orgOwnedAdmin.getParentActiveAdmin().accountTypesWithManagementDisabled);
            }
            strArr = (String[]) resultSet.toArray(new String[resultSet.size()]);
        }
        return strArr;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [11986=4] */
    public void setUninstallBlocked(ComponentName who, String callerPackage, String packageName, boolean uninstallBlocked) {
        Injector injector;
        CallerIdentity caller = getCallerIdentity(who, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller) || isFinancedDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-block-uninstall")));
        int userId = caller.getUserId();
        synchronized (getLockObject()) {
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.setBlockUninstallForUser(packageName, uninstallBlocked, userId);
                injector = this.mInjector;
            } catch (RemoteException re) {
                Slogf.e(LOG_TAG, "Failed to setBlockUninstallForUser", re);
                injector = this.mInjector;
            }
            injector.binderRestoreCallingIdentity(id);
        }
        if (uninstallBlocked) {
            PackageManagerInternal pmi = this.mInjector.getPackageManagerInternal();
            pmi.removeNonSystemPackageSuspensions(packageName, userId);
            pmi.removeDistractingPackageRestrictions(packageName, userId);
            pmi.flushPackageRestrictions(userId);
        }
        DevicePolicyEventLogger.createEvent(67).setAdmin(caller.getPackageName()).setBoolean(who == null).setStrings(new String[]{packageName}).write();
    }

    public boolean isUninstallBlocked(ComponentName who, String packageName) {
        boolean blockUninstallForUser;
        boolean z;
        int userId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            if (who != null) {
                try {
                    CallerIdentity caller = getCallerIdentity(who);
                    if (!isProfileOwner(caller) && !isDefaultDeviceOwner(caller) && !isFinancedDeviceOwner(caller)) {
                        z = false;
                        Preconditions.checkCallAuthorization(z);
                    }
                    z = true;
                    Preconditions.checkCallAuthorization(z);
                } catch (Throwable th) {
                    throw th;
                }
            }
            try {
                blockUninstallForUser = this.mIPackageManager.getBlockUninstallForUser(packageName, userId);
            } catch (RemoteException re) {
                Slogf.e(LOG_TAG, "Failed to getBlockUninstallForUser", re);
                return false;
            }
        }
        return blockUninstallForUser;
    }

    public void setCrossProfileCallerIdDisabled(ComponentName who, boolean disabled) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            if (admin.disableCallerId != disabled) {
                admin.disableCallerId = disabled;
                saveSettingsLocked(caller.getUserId());
            }
        }
        DevicePolicyEventLogger.createEvent(46).setAdmin(who).setBoolean(disabled).write();
    }

    public boolean getCrossProfileCallerIdDisabled(ComponentName who) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            z = admin.disableCallerId;
        }
        return z;
    }

    public boolean getCrossProfileCallerIdDisabledForUser(int userId) {
        boolean z;
        Preconditions.checkArgumentNonnegative(userId, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasCrossUsersPermission(caller, userId));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            z = admin != null ? admin.disableCallerId : false;
        }
        return z;
    }

    public void setCrossProfileContactsSearchDisabled(ComponentName who, boolean disabled) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            if (admin.disableContactsSearch != disabled) {
                admin.disableContactsSearch = disabled;
                saveSettingsLocked(caller.getUserId());
            }
        }
        DevicePolicyEventLogger.createEvent(45).setAdmin(who).setBoolean(disabled).write();
    }

    public boolean getCrossProfileContactsSearchDisabled(ComponentName who) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            z = admin.disableContactsSearch;
        }
        return z;
    }

    public boolean getCrossProfileContactsSearchDisabledForUser(int userId) {
        boolean z;
        Preconditions.checkArgumentNonnegative(userId, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasCrossUsersPermission(caller, userId));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            z = admin != null ? admin.disableContactsSearch : false;
        }
        return z;
    }

    public void startManagedQuickContact(String actualLookupKey, long actualContactId, boolean isContactIdIgnored, long actualDirectoryId, Intent originalIntent) {
        final Intent intent = ContactsContract.QuickContact.rebuildManagedQuickContactsIntent(actualLookupKey, actualContactId, isContactIdIgnored, actualDirectoryId, originalIntent);
        final int callingUserId = UserHandle.getCallingUserId();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda115
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3113xbb39bdca(callingUserId, intent);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startManagedQuickContact$91$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3113xbb39bdca(int callingUserId, Intent intent) throws Exception {
        synchronized (getLockObject()) {
            int managedUserId = getManagedUserId(callingUserId);
            if (managedUserId < 0) {
                return;
            }
            if (isCrossProfileQuickContactDisabled(managedUserId)) {
                return;
            }
            ContactsInternal.startQuickContactWithErrorToastForUser(this.mContext, intent, new UserHandle(managedUserId));
        }
    }

    private boolean isCrossProfileQuickContactDisabled(int userId) {
        return getCrossProfileCallerIdDisabledForUser(userId) && getCrossProfileContactsSearchDisabledForUser(userId);
    }

    public int getManagedUserId(int callingUserId) {
        for (UserInfo ui : this.mUserManager.getProfiles(callingUserId)) {
            if (ui.id != callingUserId && ui.isManagedProfile()) {
                return ui.id;
            }
        }
        return -1;
    }

    public void setBluetoothContactSharingDisabled(ComponentName who, boolean disabled) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            if (admin.disableBluetoothContactSharing != disabled) {
                admin.disableBluetoothContactSharing = disabled;
                saveSettingsLocked(caller.getUserId());
            }
        }
        DevicePolicyEventLogger.createEvent(47).setAdmin(who).setBoolean(disabled).write();
    }

    public boolean getBluetoothContactSharingDisabled(ComponentName who) {
        boolean z;
        boolean z2 = false;
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization((isDefaultDeviceOwner(caller) || isProfileOwner(caller)) ? true : true);
            synchronized (getLockObject()) {
                ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
                z = admin.disableBluetoothContactSharing;
            }
            return z;
        }
        return false;
    }

    public boolean getBluetoothContactSharingDisabledForUser(int userId) {
        boolean z;
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            z = admin != null ? admin.disableBluetoothContactSharing : false;
        }
        return z;
    }

    public void setSecondaryLockscreenEnabled(ComponentName who, boolean enabled) {
        boolean z;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        Preconditions.checkCallAuthorization(!isManagedProfile(caller.getUserId()), "User %d is not allowed to call setSecondaryLockscreenEnabled", new Object[]{Integer.valueOf(caller.getUserId())});
        synchronized (getLockObject()) {
            if (!isAdminTestOnlyLocked(who, caller.getUserId()) && !isSupervisionComponentLocked(caller.getComponentName())) {
                z = false;
                Preconditions.checkCallAuthorization(z, "Admin %s is not the default supervision component", new Object[]{caller.getComponentName()});
                DevicePolicyData policy = m3013x8b75536b(caller.getUserId());
                policy.mSecondaryLockscreenEnabled = enabled;
                saveSettingsLocked(caller.getUserId());
            }
            z = true;
            Preconditions.checkCallAuthorization(z, "Admin %s is not the default supervision component", new Object[]{caller.getComponentName()});
            DevicePolicyData policy2 = m3013x8b75536b(caller.getUserId());
            policy2.mSecondaryLockscreenEnabled = enabled;
            saveSettingsLocked(caller.getUserId());
        }
    }

    public boolean isSecondaryLockscreenEnabled(UserHandle userHandle) {
        boolean z;
        synchronized (getLockObject()) {
            z = m3013x8b75536b(userHandle.getIdentifier()).mSecondaryLockscreenEnabled;
        }
        return z;
    }

    private boolean isManagedProfileOwner(CallerIdentity caller) {
        return isProfileOwner(caller) && isManagedProfile(caller.getUserId());
    }

    public void setPreferentialNetworkServiceConfigs(List<PreferentialNetworkServiceConfig> preferentialNetworkServiceConfigs) {
        if (!this.mHasFeature) {
            return;
        }
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization((isProfileOwner(caller) && isManagedProfile(caller.getUserId())) || isDefaultDeviceOwner(caller), "Caller is not managed profile owner or device owner; only managed profile owner or device owner may control the preferential network service");
        synchronized (getLockObject()) {
            ActiveAdmin requiredAdmin = getDeviceOrProfileOwnerAdminLocked(caller.getUserId());
            if (!requiredAdmin.mPreferentialNetworkServiceConfigs.equals(preferentialNetworkServiceConfigs)) {
                requiredAdmin.mPreferentialNetworkServiceConfigs = new ArrayList(preferentialNetworkServiceConfigs);
                saveSettingsLocked(caller.getUserId());
            }
        }
        updateNetworkPreferenceForUser(caller.getUserId(), preferentialNetworkServiceConfigs);
        DevicePolicyEventLogger.createEvent((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_PREFERENTIAL_NETWORK_SERVICE_ENABLED).setBoolean(preferentialNetworkServiceConfigs.stream().anyMatch(new Predicate() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda46
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isEnabled;
                isEnabled = ((PreferentialNetworkServiceConfig) obj).isEnabled();
                return isEnabled;
            }
        })).write();
    }

    public List<PreferentialNetworkServiceConfig> getPreferentialNetworkServiceConfigs() {
        List<PreferentialNetworkServiceConfig> list;
        if (!this.mHasFeature) {
            return List.of(PreferentialNetworkServiceConfig.DEFAULT);
        }
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization((isProfileOwner(caller) && isManagedProfile(caller.getUserId())) || isDefaultDeviceOwner(caller), "Caller is not managed profile owner or device owner; only managed profile owner or device owner may retrieve the preferential network service configurations");
        synchronized (getLockObject()) {
            ActiveAdmin requiredAdmin = getDeviceOrProfileOwnerAdminLocked(caller.getUserId());
            list = requiredAdmin.mPreferentialNetworkServiceConfigs;
        }
        return list;
    }

    public void setLockTaskPackages(ComponentName who, String[] packages) throws SecurityException {
        Objects.requireNonNull(who, "ComponentName is null");
        Objects.requireNonNull(packages, "packages is null");
        for (String pkg : packages) {
            enforceMaxPackageNameLength(pkg);
        }
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(caller);
            checkCanExecuteOrThrowUnsafe(19);
            int userHandle = caller.getUserId();
            setLockTaskPackagesLocked(userHandle, new ArrayList(Arrays.asList(packages)));
        }
    }

    private void setLockTaskPackagesLocked(int userHandle, List<String> packages) {
        DevicePolicyData policy = m3013x8b75536b(userHandle);
        policy.mLockTaskPackages = packages;
        saveSettingsLocked(userHandle);
        updateLockTaskPackagesLocked(packages, userHandle);
    }

    public String[] getLockTaskPackages(ComponentName who) {
        String[] strArr;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        int userHandle = caller.getUserId();
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(caller);
            List<String> packages = m3013x8b75536b(userHandle).mLockTaskPackages;
            strArr = (String[]) packages.toArray(new String[packages.size()]);
        }
        return strArr;
    }

    public boolean isLockTaskPermitted(String pkg) {
        boolean contains;
        if (listPolicyExemptAppsUnchecked().contains(pkg)) {
            return true;
        }
        int userId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            contains = m3013x8b75536b(userId).mLockTaskPackages.contains(pkg);
        }
        return contains;
    }

    public void setLockTaskFeatures(ComponentName who, int flags) {
        Objects.requireNonNull(who, "ComponentName is null");
        boolean z = true;
        boolean hasHome = (flags & 4) != 0;
        boolean hasOverview = (flags & 8) != 0;
        Preconditions.checkArgument(hasHome || !hasOverview, "Cannot use LOCK_TASK_FEATURE_OVERVIEW without LOCK_TASK_FEATURE_HOME");
        boolean hasNotification = (flags & 2) != 0;
        if (!hasHome && hasNotification) {
            z = false;
        }
        Preconditions.checkArgument(z, "Cannot use LOCK_TASK_FEATURE_NOTIFICATIONS without LOCK_TASK_FEATURE_HOME");
        CallerIdentity caller = getCallerIdentity(who);
        int userHandle = caller.getUserId();
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(caller);
            enforceCanSetLockTaskFeaturesOnFinancedDevice(caller, flags);
            checkCanExecuteOrThrowUnsafe(18);
            setLockTaskFeaturesLocked(userHandle, flags);
        }
    }

    private void setLockTaskFeaturesLocked(int userHandle, int flags) {
        DevicePolicyData policy = m3013x8b75536b(userHandle);
        policy.mLockTaskFeatures = flags;
        saveSettingsLocked(userHandle);
        updateLockTaskFeaturesLocked(flags, userHandle);
    }

    public int getLockTaskFeatures(ComponentName who) {
        int i;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        int userHandle = caller.getUserId();
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(caller);
            i = m3013x8b75536b(userHandle).mLockTaskFeatures;
        }
        return i;
    }

    private void maybeClearLockTaskPolicyLocked() {
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda15
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3047xc88bc2c9();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$maybeClearLockTaskPolicyLocked$93$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3047xc88bc2c9() throws Exception {
        List<UserInfo> userInfos = this.mUserManager.getAliveUsers();
        for (int i = userInfos.size() - 1; i >= 0; i--) {
            int userId = userInfos.get(i).id;
            if (!canUserUseLockTaskLocked(userId)) {
                List<String> lockTaskPackages = m3013x8b75536b(userId).mLockTaskPackages;
                if (!lockTaskPackages.isEmpty()) {
                    Slogf.d(LOG_TAG, "User id " + userId + " not affiliated. Clearing lock task packages");
                    setLockTaskPackagesLocked(userId, Collections.emptyList());
                }
                int lockTaskFeatures = m3013x8b75536b(userId).mLockTaskFeatures;
                if (lockTaskFeatures != 0) {
                    Slogf.d(LOG_TAG, "User id " + userId + " not affiliated. Clearing lock task features");
                    setLockTaskFeaturesLocked(userId, 0);
                }
            }
        }
    }

    private void enforceCanSetLockTaskFeaturesOnFinancedDevice(CallerIdentity caller, int flags) {
        if (!isFinancedDeviceOwner(caller)) {
            return;
        }
        if (flags == 0 || ((~55) & flags) != 0) {
            throw new SecurityException("Permitted lock task features when managing a financed device: LOCK_TASK_FEATURE_SYSTEM_INFO, LOCK_TASK_FEATURE_KEYGUARD, LOCK_TASK_FEATURE_HOME, LOCK_TASK_FEATURE_GLOBAL_ACTIONS, or LOCK_TASK_FEATURE_NOTIFICATIONS");
        }
    }

    public void notifyLockTaskModeChanged(boolean isEnabled, String pkg, int userHandle) {
        Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "call notifyLockTaskModeChanged"));
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(userHandle);
            if (policy.mStatusBarDisabled) {
                setStatusBarDisabledInternal(!isEnabled, userHandle);
            }
            Bundle adminExtras = new Bundle();
            adminExtras.putString("android.app.extra.LOCK_TASK_PACKAGE", pkg);
            Iterator<ActiveAdmin> it = policy.mAdminList.iterator();
            while (it.hasNext()) {
                ActiveAdmin admin = it.next();
                boolean ownsDevice = isDeviceOwner(admin.info.getComponent(), userHandle);
                boolean ownsProfile = isProfileOwner(admin.info.getComponent(), userHandle);
                if (ownsDevice || ownsProfile) {
                    if (isEnabled) {
                        sendAdminCommandLocked(admin, "android.app.action.LOCK_TASK_ENTERING", adminExtras, (BroadcastReceiver) null);
                    } else {
                        sendAdminCommandLocked(admin, "android.app.action.LOCK_TASK_EXITING");
                    }
                    DevicePolicyEventLogger.createEvent(51).setAdmin(admin.info.getPackageName()).setBoolean(isEnabled).setStrings(new String[]{pkg}).write();
                }
            }
        }
    }

    public void setGlobalSetting(ComponentName who, final String setting, final String value) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        DevicePolicyEventLogger.createEvent(111).setAdmin(who).setStrings(new String[]{setting, value}).write();
        synchronized (getLockObject()) {
            if (GLOBAL_SETTINGS_DEPRECATED.contains(setting)) {
                Slogf.i(LOG_TAG, "Global setting no longer supported: %s", setting);
                return;
            }
            if (!GLOBAL_SETTINGS_ALLOWLIST.contains(setting) && !UserManager.isDeviceInDemoMode(this.mContext)) {
                throw new SecurityException(String.format("Permission denial: device owners cannot update %1$s", setting));
            }
            if ("stay_on_while_plugged_in".equals(setting)) {
                long timeMs = getMaximumTimeToLock(who, this.mInjector.userHandleGetCallingUserId(), false);
                if (timeMs > 0 && timeMs < JobStatus.NO_LATEST_RUNTIME) {
                    return;
                }
            }
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda61
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3086x834e8ca2(setting, value);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setGlobalSetting$94$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3086x834e8ca2(String setting, String value) throws Exception {
        this.mInjector.settingsGlobalPutString(setting, value);
    }

    public void setSystemSetting(ComponentName who, final String setting, final String value) {
        Objects.requireNonNull(who, "ComponentName is null");
        Preconditions.checkStringNotEmpty(setting, "String setting is null or empty");
        final CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(11);
        synchronized (getLockObject()) {
            if (!SYSTEM_SETTINGS_ALLOWLIST.contains(setting)) {
                throw new SecurityException(String.format("Permission denial: device owners cannot update %1$s", setting));
            }
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda30
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3107x583880f5(setting, value, caller);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setSystemSetting$95$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3107x583880f5(String setting, String value, CallerIdentity caller) throws Exception {
        this.mInjector.settingsSystemPutStringForUser(setting, value, caller.getUserId());
    }

    public void setConfiguredNetworksLockdownState(ComponentName who, final boolean lockdown) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller));
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda8
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3075xf8f6ff14(lockdown);
            }
        });
        DevicePolicyEventLogger.createEvent(132).setAdmin(caller.getComponentName()).setBoolean(lockdown).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setConfiguredNetworksLockdownState$96$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3075xf8f6ff14(boolean lockdown) throws Exception {
        this.mInjector.settingsGlobalPutInt("wifi_device_owner_configs_lockdown", lockdown ? 1 : 0);
    }

    public boolean hasLockdownAdminConfiguredNetworks(ComponentName who) {
        boolean z = false;
        if (this.mHasFeature) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization((isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller)) ? true : true);
            return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda38
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3018x9647d69f();
                }
            })).booleanValue();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hasLockdownAdminConfiguredNetworks$97$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3018x9647d69f() throws Exception {
        return Boolean.valueOf(this.mInjector.settingsGlobalGetInt("wifi_device_owner_configs_lockdown", 0) > 0);
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void setLocationEnabled(ComponentName who, final boolean locationEnabled) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        final UserHandle userHandle = caller.getUserHandle();
        if (this.mIsAutomotive && !locationEnabled) {
            Slogf.i(LOG_TAG, "setLocationEnabled(%s, %b): ignoring for user %s on automotive build", who.flattenToShortString(), Boolean.valueOf(locationEnabled), userHandle);
            return;
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda103
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3088x8e5723bd(userHandle, locationEnabled);
            }
        });
        DevicePolicyEventLogger admin = DevicePolicyEventLogger.createEvent(14).setAdmin(who);
        String[] strArr = new String[2];
        strArr[0] = "location_mode";
        strArr[1] = Integer.toString(locationEnabled ? 3 : 0);
        admin.setStrings(strArr).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setLocationEnabled$98$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3088x8e5723bd(UserHandle userHandle, boolean locationEnabled) throws Exception {
        boolean wasLocationEnabled = this.mInjector.getLocationManager().isLocationEnabledForUser(userHandle);
        Slogf.v(LOG_TAG, "calling locationMgr.setLocationEnabledForUser(%b, %s) when it was %b", Boolean.valueOf(locationEnabled), userHandle, Boolean.valueOf(wasLocationEnabled));
        this.mInjector.getLocationManager().setLocationEnabledForUser(locationEnabled, userHandle);
        if (locationEnabled && !wasLocationEnabled) {
            showLocationSettingsEnabledNotification(userHandle);
        }
    }

    private void showLocationSettingsEnabledNotification(UserHandle user) {
        Intent intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS").addFlags(268435456);
        ActivityInfo targetInfo = intent.resolveActivityInfo(this.mInjector.getPackageManager(user.getIdentifier()), 1048576);
        if (targetInfo != null) {
            intent.setComponent(targetInfo.getComponentName());
        } else {
            Slogf.wtf(LOG_TAG, "Failed to resolve intent for location settings");
        }
        PendingIntent locationSettingsIntent = this.mInjector.pendingIntentGetActivityAsUser(this.mContext, 0, intent, AudioFormat.DTS_HD, null, user);
        Notification notification = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17302497).setContentTitle(getLocationChangedTitle()).setContentText(getLocationChangedText()).setColor(this.mContext.getColor(17170460)).setShowWhen(true).setContentIntent(locationSettingsIntent).setAutoCancel(true).build();
        this.mInjector.getNotificationManager().notify(59, notification);
    }

    private String getLocationChangedTitle() {
        return getUpdatableString("Core.LOCATION_CHANGED_TITLE", 17040597, new Object[0]);
    }

    private String getLocationChangedText() {
        return getUpdatableString("Core.LOCATION_CHANGED_MESSAGE", 17040596, new Object[0]);
    }

    public boolean setTime(ComponentName who, final long millis) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller));
        if (this.mInjector.settingsGlobalGetInt("auto_time", 0) == 1) {
            return false;
        }
        DevicePolicyEventLogger.createEvent(133).setAdmin(caller.getComponentName()).write();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda104
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3109xecd3eb47(millis);
            }
        });
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setTime$99$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3109xecd3eb47(long millis) throws Exception {
        this.mInjector.getAlarmManager().setTime(millis);
    }

    public boolean setTimeZone(ComponentName who, final String timeZone) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller));
        if (this.mInjector.settingsGlobalGetInt("auto_time_zone", 0) == 1) {
            return false;
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda0
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3110x1ba764ae(timeZone);
            }
        });
        DevicePolicyEventLogger.createEvent(134).setAdmin(caller.getComponentName()).write();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setTimeZone$100$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3110x1ba764ae(String timeZone) throws Exception {
        this.mInjector.getAlarmManager().setTimeZone(timeZone);
    }

    public void setSecureSetting(ComponentName who, final String setting, final String value) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        final int callingUserId = caller.getUserId();
        synchronized (getLockObject()) {
            if (isDeviceOwner(who, callingUserId)) {
                if (!SECURE_SETTINGS_DEVICEOWNER_ALLOWLIST.contains(setting) && !isCurrentUserDemo()) {
                    throw new SecurityException(String.format("Permission denial: Device owners cannot update %1$s", setting));
                }
            } else if (!SECURE_SETTINGS_ALLOWLIST.contains(setting) && !isCurrentUserDemo()) {
                throw new SecurityException(String.format("Permission denial: Profile owners cannot update %1$s", setting));
            }
            if (setting.equals("location_mode") && isSetSecureSettingLocationModeCheckEnabled(who.getPackageName(), callingUserId)) {
                throw new UnsupportedOperationException("location_mode is deprecated. Please use setLocationEnabled() instead.");
            }
            if (setting.equals("install_non_market_apps")) {
                if (getTargetSdk(who.getPackageName(), callingUserId) >= 26) {
                    throw new UnsupportedOperationException("install_non_market_apps is deprecated. Please use one of the user restrictions no_install_unknown_sources or no_install_unknown_sources_globally instead.");
                }
                if (!this.mUserManager.isManagedProfile(callingUserId)) {
                    Slogf.e(LOG_TAG, "Ignoring setSecureSetting request for " + setting + ". User restriction no_install_unknown_sources or no_install_unknown_sources_globally should be used instead.");
                } else {
                    try {
                        setUserRestriction(who, "no_install_unknown_sources", Integer.parseInt(value) == 0, false);
                        DevicePolicyEventLogger.createEvent(14).setAdmin(who).setStrings(new String[]{setting, value}).write();
                    } catch (NumberFormatException e) {
                        Slogf.e(LOG_TAG, "Invalid value: " + value + " for setting " + setting);
                    }
                }
                return;
            }
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda91
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3105x1a431db7(setting, callingUserId, value);
                }
            });
            DevicePolicyEventLogger.createEvent(14).setAdmin(who).setStrings(new String[]{setting, value}).write();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setSecureSetting$101$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3105x1a431db7(String setting, int callingUserId, String value) throws Exception {
        if ("default_input_method".equals(setting)) {
            String currentValue = this.mInjector.settingsSecureGetStringForUser("default_input_method", callingUserId);
            if (!TextUtils.equals(currentValue, value)) {
                this.mSetupContentObserver.addPendingChangeByOwnerLocked(callingUserId);
            }
            m3013x8b75536b(callingUserId).mCurrentInputMethodSet = true;
            saveSettingsLocked(callingUserId);
        }
        this.mInjector.settingsSecurePutStringForUser(setting, value, callingUserId);
        if (setting.equals("location_mode") && Integer.parseInt(value) != 0) {
            showLocationSettingsEnabledNotification(UserHandle.of(callingUserId));
        }
    }

    private boolean isSetSecureSettingLocationModeCheckEnabled(String packageName, int userId) {
        return this.mInjector.isChangeEnabled(USE_SET_LOCATION_ENABLED, packageName, userId);
    }

    public void setMasterVolumeMuted(ComponentName who, boolean on) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(35);
        synchronized (getLockObject()) {
            setUserRestriction(who, "disallow_unmute_device", on, false);
            DevicePolicyEventLogger.createEvent(35).setAdmin(who).setBoolean(on).write();
        }
    }

    public boolean isMasterVolumeMuted(ComponentName who) {
        boolean isMasterMute;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        synchronized (getLockObject()) {
            AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
            isMasterMute = audioManager.isMasterMute();
        }
        return isMasterMute;
    }

    public void setUserIcon(ComponentName who, final Bitmap icon) {
        Objects.requireNonNull(who, "ComponentName is null");
        final CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        synchronized (getLockObject()) {
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda96
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3111xd7bccb41(caller, icon);
                }
            });
        }
        DevicePolicyEventLogger.createEvent(41).setAdmin(who).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setUserIcon$102$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3111xd7bccb41(CallerIdentity caller, Bitmap icon) throws Exception {
        this.mUserManagerInternal.setUserIcon(caller.getUserId(), icon);
    }

    public boolean setKeyguardDisabled(ComponentName who, boolean disabled) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        int userId = caller.getUserId();
        synchronized (getLockObject()) {
            Preconditions.checkCallAuthorization(isUserAffiliatedWithDeviceLocked(userId), String.format("Admin %s is neither the device owner or affiliated user's profile owner.", who));
        }
        if (isManagedProfile(userId)) {
            throw new SecurityException("Managed profile cannot disable keyguard");
        }
        checkCanExecuteOrThrowUnsafe(12);
        long ident = this.mInjector.binderClearCallingIdentity();
        if (disabled) {
            try {
                if (this.mLockPatternUtils.isSecure(userId)) {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                    return false;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
                throw th;
            }
        }
        this.mLockPatternUtils.setLockScreenDisabled(disabled, userId);
        if (disabled) {
            this.mInjector.getIWindowManager().dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
        }
        DevicePolicyEventLogger.createEvent(37).setAdmin(who).setBoolean(disabled).write();
        this.mInjector.binderRestoreCallingIdentity(ident);
        return true;
    }

    public boolean setStatusBarDisabled(ComponentName who, boolean disabled) {
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        int userId = caller.getUserId();
        synchronized (getLockObject()) {
            Preconditions.checkCallAuthorization(isUserAffiliatedWithDeviceLocked(userId), "Admin " + who + " is neither the device owner or affiliated user's profile owner.");
            if (isManagedProfile(userId)) {
                throw new SecurityException("Managed profile cannot disable status bar");
            }
            checkCanExecuteOrThrowUnsafe(13);
            DevicePolicyData policy = m3013x8b75536b(userId);
            if (policy.mStatusBarDisabled != disabled) {
                boolean isLockTaskMode = false;
                try {
                    isLockTaskMode = this.mInjector.getIActivityTaskManager().getLockTaskModeState() != 0;
                } catch (RemoteException e) {
                    Slogf.e(LOG_TAG, "Failed to get LockTask mode");
                }
                if (!isLockTaskMode && !setStatusBarDisabledInternal(disabled, userId)) {
                    return false;
                }
                policy.mStatusBarDisabled = disabled;
                saveSettingsLocked(userId);
            }
            DevicePolicyEventLogger.createEvent(38).setAdmin(who).setBoolean(disabled).write();
            return true;
        }
    }

    private boolean setStatusBarDisabledInternal(boolean disabled, int userId) {
        int flags2;
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                IStatusBarService statusBarService = IStatusBarService.Stub.asInterface(ServiceManager.checkService("statusbar"));
                if (statusBarService != null) {
                    int flags1 = disabled ? STATUS_BAR_DISABLE_MASK : 0;
                    if (!disabled) {
                        flags2 = 0;
                    } else {
                        flags2 = 1;
                    }
                    statusBarService.disableForUser(flags1, this.mToken, this.mContext.getPackageName(), userId);
                    statusBarService.disable2ForUser(flags2, this.mToken, this.mContext.getPackageName(), userId);
                    return true;
                }
            } catch (RemoteException e) {
                Slogf.e(LOG_TAG, "Failed to disable the status bar", e);
            }
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    void updateUserSetupCompleteAndPaired() {
        List<UserInfo> users = this.mUserManager.getAliveUsers();
        int N = users.size();
        for (int i = 0; i < N; i++) {
            int userHandle = users.get(i).id;
            if (this.mInjector.settingsSecureGetIntForUser("user_setup_complete", 0, userHandle) != 0) {
                DevicePolicyData policy = m3013x8b75536b(userHandle);
                if (!policy.mUserSetupComplete) {
                    policy.mUserSetupComplete = true;
                    if (userHandle == 0) {
                        this.mStateCache.setDeviceProvisioned(true);
                    }
                    synchronized (getLockObject()) {
                        saveSettingsLocked(userHandle);
                    }
                }
            }
            if (this.mIsWatch && this.mInjector.settingsSecureGetIntForUser("device_paired", 0, userHandle) != 0) {
                DevicePolicyData policy2 = m3013x8b75536b(userHandle);
                if (policy2.mPaired) {
                    continue;
                } else {
                    policy2.mPaired = true;
                    synchronized (getLockObject()) {
                        saveSettingsLocked(userHandle);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SetupContentObserver extends ContentObserver {
        private final Uri mDefaultImeChanged;
        private final Uri mDeviceProvisioned;
        private final Uri mPaired;
        private Set<Integer> mUserIdsWithPendingChangesByOwner;
        private final Uri mUserSetupComplete;

        public SetupContentObserver(Handler handler) {
            super(handler);
            this.mUserSetupComplete = Settings.Secure.getUriFor("user_setup_complete");
            this.mDeviceProvisioned = Settings.Global.getUriFor("device_provisioned");
            this.mPaired = Settings.Secure.getUriFor("device_paired");
            this.mDefaultImeChanged = Settings.Secure.getUriFor("default_input_method");
            this.mUserIdsWithPendingChangesByOwner = new ArraySet();
        }

        void register() {
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mUserSetupComplete, false, this, -1);
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mDeviceProvisioned, false, this, -1);
            if (DevicePolicyManagerService.this.mIsWatch) {
                DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mPaired, false, this, -1);
            }
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mDefaultImeChanged, false, this, -1);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addPendingChangeByOwnerLocked(int userId) {
            this.mUserIdsWithPendingChangesByOwner.add(Integer.valueOf(userId));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mUserSetupComplete.equals(uri) || (DevicePolicyManagerService.this.mIsWatch && this.mPaired.equals(uri))) {
                DevicePolicyManagerService.this.updateUserSetupCompleteAndPaired();
            } else if (this.mDeviceProvisioned.equals(uri)) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    DevicePolicyManagerService.this.setDeviceOwnershipSystemPropertyLocked();
                }
            } else if (this.mDefaultImeChanged.equals(uri)) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    if (this.mUserIdsWithPendingChangesByOwner.contains(Integer.valueOf(userId))) {
                        this.mUserIdsWithPendingChangesByOwner.remove(Integer.valueOf(userId));
                    } else {
                        DevicePolicyManagerService.this.m3013x8b75536b(userId).mCurrentInputMethodSet = false;
                        DevicePolicyManagerService.this.saveSettingsLocked(userId);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DevicePolicyConstantsObserver extends ContentObserver {
        final Uri mConstantsUri;

        DevicePolicyConstantsObserver(Handler handler) {
            super(handler);
            this.mConstantsUri = Settings.Global.getUriFor("device_policy_constants");
        }

        void register() {
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mConstantsUri, false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            DevicePolicyManagerService devicePolicyManagerService = DevicePolicyManagerService.this;
            devicePolicyManagerService.mConstants = devicePolicyManagerService.loadConstants();
            DevicePolicyManagerService.invalidateBinderCaches();
            DevicePolicyManagerService.this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$DevicePolicyConstantsObserver$$ExternalSyntheticLambda0
                public final void runOrThrow() {
                    DevicePolicyManagerService.DevicePolicyConstantsObserver.this.m3128xbabc8b5a();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onChange$0$com-android-server-devicepolicy-DevicePolicyManagerService$DevicePolicyConstantsObserver  reason: not valid java name */
        public /* synthetic */ void m3128xbabc8b5a() throws Exception {
            Intent intent = new Intent("android.app.action.DEVICE_POLICY_CONSTANTS_CHANGED");
            intent.setFlags(1073741824);
            List<UserInfo> users = DevicePolicyManagerService.this.mUserManager.getAliveUsers();
            for (int i = 0; i < users.size(); i++) {
                DevicePolicyManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.of(users.get(i).id));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class LocalService extends DevicePolicyManagerInternal implements DevicePolicyManagerLiteInternal {
        private List<DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener> mWidgetProviderListeners;

        LocalService() {
        }

        public List<String> getCrossProfileWidgetProviders(int profileId) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                if (DevicePolicyManagerService.this.mOwners == null) {
                    return Collections.emptyList();
                }
                ComponentName ownerComponent = DevicePolicyManagerService.this.mOwners.getProfileOwnerComponent(profileId);
                if (ownerComponent == null) {
                    return Collections.emptyList();
                }
                DevicePolicyData policy = DevicePolicyManagerService.this.getUserDataUnchecked(profileId);
                ActiveAdmin admin = policy.mAdminMap.get(ownerComponent);
                if (admin != null && admin.crossProfileWidgetProviders != null && !admin.crossProfileWidgetProviders.isEmpty()) {
                    return admin.crossProfileWidgetProviders;
                }
                return Collections.emptyList();
            }
        }

        public void addOnCrossProfileWidgetProvidersChangeListener(DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener listener) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                if (this.mWidgetProviderListeners == null) {
                    this.mWidgetProviderListeners = new ArrayList();
                }
                if (!this.mWidgetProviderListeners.contains(listener)) {
                    this.mWidgetProviderListeners.add(listener);
                }
            }
        }

        public ComponentName getProfileOwnerOrDeviceOwnerSupervisionComponent(UserHandle userHandle) {
            return DevicePolicyManagerService.this.getProfileOwnerOrDeviceOwnerSupervisionComponent(userHandle);
        }

        public boolean isActiveDeviceOwner(int uid) {
            return DevicePolicyManagerService.this.isDefaultDeviceOwner(new CallerIdentity(uid, null, null));
        }

        public boolean isActiveProfileOwner(int uid) {
            return DevicePolicyManagerService.this.isProfileOwner(new CallerIdentity(uid, null, null));
        }

        public boolean isActiveSupervisionApp(int uid) {
            if (DevicePolicyManagerService.this.isProfileOwner(new CallerIdentity(uid, null, null))) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    ActiveAdmin admin = DevicePolicyManagerService.this.getProfileOwnerAdminLocked(UserHandle.getUserId(uid));
                    if (admin == null) {
                        return false;
                    }
                    return DevicePolicyManagerService.this.isSupervisionComponentLocked(admin.info.getComponent());
                }
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyCrossProfileProvidersChanged(int userId, List<String> packages) {
            List<DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener> listeners;
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                listeners = new ArrayList<>(this.mWidgetProviderListeners);
            }
            int listenerCount = listeners.size();
            for (int i = 0; i < listenerCount; i++) {
                DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener listener = listeners.get(i);
                listener.onCrossProfileWidgetProvidersChanged(userId, packages);
            }
        }

        public Intent createShowAdminSupportIntent(int userId, boolean useDefaultIfNoAdmin) {
            if (DevicePolicyManagerService.this.getEnforcingAdminAndUserDetailsInternal(userId, null) != null || useDefaultIfNoAdmin) {
                return DevicePolicyManagerService.this.createShowAdminSupportIntent(userId);
            }
            return null;
        }

        public Intent createUserRestrictionSupportIntent(int userId, String userRestriction) {
            if (DevicePolicyManagerService.this.getEnforcingAdminAndUserDetailsInternal(userId, userRestriction) == null) {
                return null;
            }
            Intent intent = DevicePolicyManagerService.this.createShowAdminSupportIntent(userId);
            intent.putExtra("android.app.extra.RESTRICTION", userRestriction);
            return intent;
        }

        public boolean isUserAffiliatedWithDevice(int userId) {
            return DevicePolicyManagerService.this.isUserAffiliatedWithDeviceLocked(userId);
        }

        public boolean canSilentlyInstallPackage(String callerPackage, int callerUid) {
            if (callerPackage == null) {
                return false;
            }
            CallerIdentity caller = new CallerIdentity(callerUid, null, null);
            if (!isUserAffiliatedWithDevice(UserHandle.getUserId(callerUid)) || (!isActiveProfileOwner(callerUid) && !DevicePolicyManagerService.this.isDefaultDeviceOwner(caller) && !DevicePolicyManagerService.this.isFinancedDeviceOwner(caller))) {
                return false;
            }
            return true;
        }

        public void reportSeparateProfileChallengeChanged(final int userId) {
            DevicePolicyManagerService.this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$LocalService$$ExternalSyntheticLambda1
                public final void runOrThrow() {
                    DevicePolicyManagerService.LocalService.this.m3131x4b59a2a7(userId);
                }
            });
            DevicePolicyEventLogger.createEvent(110).setBoolean(DevicePolicyManagerService.this.isSeparateProfileChallengeEnabled(userId)).write();
            DevicePolicyManagerService.invalidateBinderCaches();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$reportSeparateProfileChallengeChanged$0$com-android-server-devicepolicy-DevicePolicyManagerService$LocalService  reason: not valid java name */
        public /* synthetic */ void m3131x4b59a2a7(int userId) throws Exception {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                DevicePolicyManagerService.this.updateMaximumTimeToLockLocked(userId);
                DevicePolicyManagerService.this.updatePasswordQualityCacheForUserGroup(userId);
            }
        }

        public CharSequence getPrintingDisabledReasonForUser(int userId) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                if (!DevicePolicyManagerService.this.mUserManager.hasUserRestriction("no_printing", UserHandle.of(userId))) {
                    Slogf.e(DevicePolicyManagerService.LOG_TAG, "printing is enabled for user %d", Integer.valueOf(userId));
                    return null;
                }
                String ownerPackage = DevicePolicyManagerService.this.mOwners.getProfileOwnerPackage(userId);
                if (ownerPackage == null) {
                    ownerPackage = DevicePolicyManagerService.this.mOwners.getDeviceOwnerPackageName();
                }
                final String packageName = ownerPackage;
                final PackageManager pm = DevicePolicyManagerService.this.mInjector.getPackageManager();
                PackageInfo packageInfo = (PackageInfo) DevicePolicyManagerService.this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$LocalService$$ExternalSyntheticLambda0
                    public final Object getOrThrow() {
                        return DevicePolicyManagerService.LocalService.lambda$getPrintingDisabledReasonForUser$1(pm, packageName);
                    }
                });
                if (packageInfo == null) {
                    Slogf.e(DevicePolicyManagerService.LOG_TAG, "packageInfo is inexplicably null");
                    return null;
                }
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                if (appInfo == null) {
                    Slogf.e(DevicePolicyManagerService.LOG_TAG, "appInfo is inexplicably null");
                    return null;
                }
                CharSequence appLabel = pm.getApplicationLabel(appInfo);
                if (appLabel == null) {
                    Slogf.e(DevicePolicyManagerService.LOG_TAG, "appLabel is inexplicably null");
                    return null;
                }
                return DevicePolicyManagerService.this.getUpdatableString("Core.PRINTING_DISABLED_NAMED_ADMIN", 17041350, appLabel);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ PackageInfo lambda$getPrintingDisabledReasonForUser$1(PackageManager pm, String packageName) throws Exception {
            try {
                return pm.getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Slogf.e(DevicePolicyManagerService.LOG_TAG, "getPackageInfo error", e);
                return null;
            }
        }

        protected DevicePolicyCache getDevicePolicyCache() {
            return DevicePolicyManagerService.this.mPolicyCache;
        }

        protected DeviceStateCache getDeviceStateCache() {
            return DevicePolicyManagerService.this.mStateCache;
        }

        public List<String> getAllCrossProfilePackages() {
            return DevicePolicyManagerService.this.getAllCrossProfilePackages();
        }

        public List<String> getDefaultCrossProfilePackages() {
            return DevicePolicyManagerService.this.getDefaultCrossProfilePackages();
        }

        public void broadcastIntentToManifestReceivers(Intent intent, UserHandle parentHandle, boolean requiresPermission) {
            Objects.requireNonNull(intent);
            Objects.requireNonNull(parentHandle);
            Slogf.i(DevicePolicyManagerService.LOG_TAG, "Sending %s broadcast to manifest receivers.", intent.getAction());
            broadcastIntentToCrossProfileManifestReceivers(intent, parentHandle, requiresPermission);
            broadcastIntentToDevicePolicyManagerRoleHolder(intent, parentHandle);
        }

        private void broadcastIntentToCrossProfileManifestReceivers(Intent intent, UserHandle userHandle, boolean requiresPermission) {
            int userId = userHandle.getIdentifier();
            try {
                List<ResolveInfo> receivers = DevicePolicyManagerService.this.mIPackageManager.queryIntentReceivers(intent, (String) null, (long) GadgetFunction.NCM, userId).getList();
                for (ResolveInfo receiver : receivers) {
                    String packageName = receiver.getComponentInfo().packageName;
                    if (checkCrossProfilePackagePermissions(packageName, userId, requiresPermission) || checkModifyQuietModePermission(packageName, userId)) {
                        Slogf.i(DevicePolicyManagerService.LOG_TAG, "Sending %s broadcast to %s.", intent.getAction(), packageName);
                        Intent packageIntent = new Intent(intent).setComponent(receiver.getComponentInfo().getComponentName()).addFlags(16777216);
                        DevicePolicyManagerService.this.mContext.sendBroadcastAsUser(packageIntent, userHandle);
                    }
                }
            } catch (RemoteException ex) {
                Slogf.w(DevicePolicyManagerService.LOG_TAG, "Cannot get list of broadcast receivers for %s because: %s.", intent.getAction(), ex);
            }
        }

        private void broadcastIntentToDevicePolicyManagerRoleHolder(Intent intent, UserHandle userHandle) {
            int userId = userHandle.getIdentifier();
            DevicePolicyManagerService devicePolicyManagerService = DevicePolicyManagerService.this;
            String packageName = devicePolicyManagerService.getDevicePolicyManagementRoleHolderPackageName(devicePolicyManagerService.mContext);
            if (packageName == null) {
                return;
            }
            try {
                Intent packageIntent = new Intent(intent).setPackage(packageName);
                List<ResolveInfo> receivers = DevicePolicyManagerService.this.mIPackageManager.queryIntentReceivers(packageIntent, (String) null, (long) GadgetFunction.NCM, userId).getList();
                if (receivers.isEmpty()) {
                    return;
                }
                for (ResolveInfo receiver : receivers) {
                    Intent componentIntent = new Intent(packageIntent).setComponent(receiver.getComponentInfo().getComponentName()).addFlags(16777216);
                    DevicePolicyManagerService.this.mContext.sendBroadcastAsUser(componentIntent, userHandle);
                }
            } catch (RemoteException ex) {
                Slogf.w(DevicePolicyManagerService.LOG_TAG, "Cannot get list of broadcast receivers for %s because: %s.", intent.getAction(), ex);
            }
        }

        private boolean checkModifyQuietModePermission(String packageName, int userId) {
            try {
                int uid = ((ApplicationInfo) Objects.requireNonNull(DevicePolicyManagerService.this.mInjector.getPackageManager().getApplicationInfoAsUser((String) Objects.requireNonNull(packageName), 0, userId))).uid;
                return ActivityManager.checkComponentPermission("android.permission.MODIFY_QUIET_MODE", uid, -1, true) == 0;
            } catch (PackageManager.NameNotFoundException e) {
                Slogf.w(DevicePolicyManagerService.LOG_TAG, "Cannot find the package %s to check for permissions.", packageName);
                return false;
            }
        }

        private boolean checkCrossProfilePackagePermissions(String packageName, int userId, boolean requiresPermission) {
            PackageManagerInternal pmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            AndroidPackage androidPackage = pmInternal.getPackage(packageName);
            if (androidPackage == null || !androidPackage.isCrossProfile()) {
                return false;
            }
            if (!requiresPermission) {
                return true;
            }
            if (!isPackageEnabled(packageName, userId)) {
                return false;
            }
            try {
                CrossProfileAppsInternal crossProfileAppsService = (CrossProfileAppsInternal) LocalServices.getService(CrossProfileAppsInternal.class);
                return crossProfileAppsService.verifyPackageHasInteractAcrossProfilePermission(packageName, userId);
            } catch (PackageManager.NameNotFoundException e) {
                Slogf.w(DevicePolicyManagerService.LOG_TAG, "Cannot find the package %s to check for permissions.", packageName);
                return false;
            }
        }

        private boolean isPackageEnabled(String packageName, int userId) {
            boolean z;
            int callingUid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PackageInfo info = DevicePolicyManagerService.this.mInjector.getPackageManagerInternal().getPackageInfo(packageName, 786432L, callingUid, userId);
                if (info != null) {
                    if (info.applicationInfo.enabled) {
                        z = true;
                        return z;
                    }
                }
                z = false;
                return z;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public ComponentName getProfileOwnerAsUser(int userId) {
            return DevicePolicyManagerService.this.m3036x91e56c0c(userId);
        }

        public int getDeviceOwnerUserId() {
            return DevicePolicyManagerService.this.getDeviceOwnerUserId();
        }

        public boolean isDeviceOrProfileOwnerInCallingUser(String packageName) {
            return isDeviceOwnerInCallingUser(packageName) || isProfileOwnerInCallingUser(packageName);
        }

        private boolean isDeviceOwnerInCallingUser(String packageName) {
            ComponentName deviceOwnerInCallingUser = DevicePolicyManagerService.this.getDeviceOwnerComponent(true);
            return deviceOwnerInCallingUser != null && packageName.equals(deviceOwnerInCallingUser.getPackageName());
        }

        private boolean isProfileOwnerInCallingUser(String packageName) {
            ComponentName profileOwnerInCallingUser = getProfileOwnerAsUser(UserHandle.getCallingUserId());
            return profileOwnerInCallingUser != null && packageName.equals(profileOwnerInCallingUser.getPackageName());
        }

        public boolean supportsResetOp(int op) {
            return op == 93 && LocalServices.getService(CrossProfileAppsInternal.class) != null;
        }

        public void resetOp(int op, String packageName, int userId) {
            if (op != 93) {
                throw new IllegalArgumentException("Unsupported op for DPM reset: " + op);
            }
            ((CrossProfileAppsInternal) LocalServices.getService(CrossProfileAppsInternal.class)).setInteractAcrossProfilesAppOp(packageName, findInteractAcrossProfilesResetMode(packageName), userId);
        }

        public void notifyUnsafeOperationStateChanged(DevicePolicySafetyChecker checker, int reason, boolean isSafe) {
            Preconditions.checkArgument(DevicePolicyManagerService.this.mSafetyChecker == checker, "invalid checker: should be %s, was %s", new Object[]{DevicePolicyManagerService.this.mSafetyChecker, checker});
            Bundle extras = new Bundle();
            extras.putInt("android.app.extra.OPERATION_SAFETY_REASON", reason);
            extras.putBoolean("android.app.extra.OPERATION_SAFETY_STATE", isSafe);
            if (DevicePolicyManagerService.this.mOwners.hasDeviceOwner()) {
                DevicePolicyManagerService.this.sendDeviceOwnerCommand("android.app.action.OPERATION_SAFETY_STATE_CHANGED", extras);
            }
            for (Integer num : DevicePolicyManagerService.this.mOwners.getProfileOwnerKeys()) {
                int profileOwnerId = num.intValue();
                DevicePolicyManagerService.this.sendProfileOwnerCommand("android.app.action.OPERATION_SAFETY_STATE_CHANGED", extras, profileOwnerId);
            }
        }

        private int findInteractAcrossProfilesResetMode(String packageName) {
            if (getDefaultCrossProfilePackages().contains(packageName)) {
                return 0;
            }
            return AppOpsManager.opToDefaultMode(93);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Intent createShowAdminSupportIntent(int userId) {
        Intent intent = new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS");
        intent.putExtra("android.intent.extra.USER_ID", userId);
        intent.setFlags(268435456);
        return intent;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [13644=8] */
    /* JADX INFO: Access modifiers changed from: private */
    public Bundle getEnforcingAdminAndUserDetailsInternal(int userId, String restriction) {
        ActiveAdmin admin;
        if (restriction == null || "policy_suspend_packages".equals(restriction)) {
            ComponentName profileOwner = this.mOwners.getProfileOwnerComponent(userId);
            if (profileOwner != null) {
                Bundle result = new Bundle();
                result.putInt("android.intent.extra.USER_ID", userId);
                result.putParcelable("android.app.extra.DEVICE_ADMIN", profileOwner);
                return result;
            }
            Pair<Integer, ComponentName> deviceOwner = this.mOwners.getDeviceOwnerUserIdAndComponent();
            if (deviceOwner != null && ((Integer) deviceOwner.first).intValue() == userId) {
                Bundle result2 = new Bundle();
                result2.putInt("android.intent.extra.USER_ID", userId);
                result2.putParcelable("android.app.extra.DEVICE_ADMIN", (Parcelable) deviceOwner.second);
                return result2;
            }
        } else if ("policy_disable_camera".equals(restriction) || "policy_disable_screen_capture".equals(restriction)) {
            synchronized (getLockObject()) {
                DevicePolicyData policy = m3013x8b75536b(userId);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if ((admin2.disableCamera && "policy_disable_camera".equals(restriction)) || (admin2.disableScreenCapture && "policy_disable_screen_capture".equals(restriction))) {
                        Bundle result3 = new Bundle();
                        result3.putInt("android.intent.extra.USER_ID", userId);
                        result3.putParcelable("android.app.extra.DEVICE_ADMIN", admin2.info.getComponent());
                        return result3;
                    }
                }
                if (0 == 0 && "policy_disable_camera".equals(restriction) && (admin = getDeviceOwnerAdminLocked()) != null && admin.disableCamera) {
                    Bundle result4 = new Bundle();
                    result4.putInt("android.intent.extra.USER_ID", this.mOwners.getDeviceOwnerUserId());
                    result4.putParcelable("android.app.extra.DEVICE_ADMIN", admin.info.getComponent());
                    return result4;
                }
            }
        } else {
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                List<UserManager.EnforcingUser> sources = this.mUserManager.getUserRestrictionSources(restriction, UserHandle.of(userId));
                if (sources == null) {
                    return null;
                }
                int sizeBefore = sources.size();
                if (sizeBefore > 1) {
                    Slogf.d(LOG_TAG, "getEnforcingAdminAndUserDetailsInternal(%d, %s): %d sources found, excluding those set by UserManager", Integer.valueOf(userId), restriction, Integer.valueOf(sizeBefore));
                    sources = getDevicePolicySources(sources);
                }
                if (sources.isEmpty()) {
                    return null;
                }
                if (sources.size() > 1) {
                    Slogf.w(LOG_TAG, "getEnforcingAdminAndUserDetailsInternal(%d, %s): multiple sources for restriction %s on user %d", restriction, Integer.valueOf(userId));
                    Bundle result5 = new Bundle();
                    result5.putInt("android.intent.extra.USER_ID", userId);
                    return result5;
                }
                UserManager.EnforcingUser enforcingUser = sources.get(0);
                int sourceType = enforcingUser.getUserRestrictionSource();
                int enforcingUserId = enforcingUser.getUserHandle().getIdentifier();
                if (sourceType == 4) {
                    ComponentName profileOwner2 = this.mOwners.getProfileOwnerComponent(enforcingUserId);
                    if (profileOwner2 != null) {
                        Bundle result6 = new Bundle();
                        result6.putInt("android.intent.extra.USER_ID", enforcingUserId);
                        result6.putParcelable("android.app.extra.DEVICE_ADMIN", profileOwner2);
                        return result6;
                    }
                } else if (sourceType == 2) {
                    Pair<Integer, ComponentName> deviceOwner2 = this.mOwners.getDeviceOwnerUserIdAndComponent();
                    if (deviceOwner2 != null) {
                        Bundle result7 = new Bundle();
                        result7.putInt("android.intent.extra.USER_ID", ((Integer) deviceOwner2.first).intValue());
                        result7.putParcelable("android.app.extra.DEVICE_ADMIN", (Parcelable) deviceOwner2.second);
                        return result7;
                    }
                } else if (sourceType == 1) {
                    return null;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
        return null;
    }

    private List<UserManager.EnforcingUser> getDevicePolicySources(List<UserManager.EnforcingUser> sources) {
        int sizeBefore = sources.size();
        List<UserManager.EnforcingUser> realSources = new ArrayList<>(sizeBefore);
        for (int i = 0; i < sizeBefore; i++) {
            UserManager.EnforcingUser source = sources.get(i);
            int type = source.getUserRestrictionSource();
            if (type != 4 && type != 2) {
                Slogf.d(LOG_TAG, "excluding source of type %s at index %d", userRestrictionSourceToString(type), Integer.valueOf(i));
            } else {
                realSources.add(source);
            }
        }
        return realSources;
    }

    private static String userRestrictionSourceToString(int source) {
        return DebugUtils.flagsToString(UserManager.class, "RESTRICTION_", source);
    }

    public Bundle getEnforcingAdminAndUserDetails(int userId, String restriction) {
        Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()));
        return getEnforcingAdminAndUserDetailsInternal(userId, restriction);
    }

    public Intent createAdminSupportIntent(String restriction) {
        Objects.requireNonNull(restriction);
        CallerIdentity caller = getCallerIdentity();
        int userId = caller.getUserId();
        if (getEnforcingAdminAndUserDetailsInternal(userId, restriction) == null) {
            return null;
        }
        Intent intent = createShowAdminSupportIntent(userId);
        intent.putExtra("android.app.extra.RESTRICTION", restriction);
        return intent;
    }

    private static boolean isLimitPasswordAllowed(ActiveAdmin admin, int minPasswordQuality) {
        if (admin.mPasswordPolicy.quality < minPasswordQuality) {
            return false;
        }
        return admin.info.usesPolicy(0);
    }

    /* JADX WARN: Removed duplicated region for block: B:15:0x0041 A[Catch: all -> 0x0077, TryCatch #0 {, blocks: (B:6:0x0026, B:8:0x002d, B:13:0x0037, B:15:0x0041, B:17:0x004f, B:18:0x0054, B:16:0x0047), top: B:27:0x0026 }] */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0047 A[Catch: all -> 0x0077, TryCatch #0 {, blocks: (B:6:0x0026, B:8:0x002d, B:13:0x0037, B:15:0x0041, B:17:0x004f, B:18:0x0054, B:16:0x0047), top: B:27:0x0026 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setSystemUpdatePolicy(ComponentName who, SystemUpdatePolicy policy) {
        boolean z;
        if (policy != null) {
            policy.validateType();
            policy.validateFreezePeriods();
            Pair<LocalDate, LocalDate> record = this.mOwners.getSystemUpdateFreezePeriodRecord();
            policy.validateAgainstPreviousFreezePeriod((LocalDate) record.first, (LocalDate) record.second, LocalDate.now());
        }
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            if (!isProfileOwnerOfOrganizationOwnedDevice(caller) && !isDefaultDeviceOwner(caller)) {
                z = false;
                Preconditions.checkCallAuthorization(z);
                checkCanExecuteOrThrowUnsafe(14);
                if (policy != null) {
                    this.mOwners.clearSystemUpdatePolicy();
                } else {
                    this.mOwners.setSystemUpdatePolicy(policy);
                    updateSystemUpdateFreezePeriodsRecord(false);
                }
                this.mOwners.writeDeviceOwner();
            }
            z = true;
            Preconditions.checkCallAuthorization(z);
            checkCanExecuteOrThrowUnsafe(14);
            if (policy != null) {
            }
            this.mOwners.writeDeviceOwner();
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda106
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3108xac5acfda();
            }
        });
        DevicePolicyEventLogger.createEvent(50).setAdmin(who).setInt(policy != null ? policy.getPolicyType() : 0).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setSystemUpdatePolicy$103$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3108xac5acfda() throws Exception {
        this.mContext.sendBroadcastAsUser(new Intent("android.app.action.SYSTEM_UPDATE_POLICY_CHANGED"), UserHandle.SYSTEM);
    }

    public SystemUpdatePolicy getSystemUpdatePolicy() {
        synchronized (getLockObject()) {
            SystemUpdatePolicy policy = this.mOwners.getSystemUpdatePolicy();
            if (policy == null || policy.isValid()) {
                return policy;
            }
            Slogf.w(LOG_TAG, "Stored system update policy is invalid, return null instead.");
            return null;
        }
    }

    private static boolean withinRange(Pair<LocalDate, LocalDate> range, LocalDate date) {
        return (date.isBefore((ChronoLocalDate) range.first) || date.isAfter((ChronoLocalDate) range.second)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSystemUpdateFreezePeriodsRecord(boolean saveIfChanged) {
        boolean changed;
        Slogf.d(LOG_TAG, "updateSystemUpdateFreezePeriodsRecord");
        synchronized (getLockObject()) {
            SystemUpdatePolicy policy = this.mOwners.getSystemUpdatePolicy();
            if (policy == null) {
                return;
            }
            LocalDate now = LocalDate.now();
            Pair<LocalDate, LocalDate> currentPeriod = policy.getCurrentFreezePeriod(now);
            if (currentPeriod == null) {
                return;
            }
            Pair<LocalDate, LocalDate> record = this.mOwners.getSystemUpdateFreezePeriodRecord();
            LocalDate start = (LocalDate) record.first;
            LocalDate end = (LocalDate) record.second;
            if (end != null && start != null) {
                if (now.equals(end.plusDays(1L))) {
                    changed = this.mOwners.setSystemUpdateFreezePeriodRecord(start, now);
                } else if (now.isAfter(end.plusDays(1L))) {
                    if (withinRange(currentPeriod, start) && withinRange(currentPeriod, end)) {
                        changed = this.mOwners.setSystemUpdateFreezePeriodRecord(start, now);
                    } else {
                        changed = this.mOwners.setSystemUpdateFreezePeriodRecord(now, now);
                    }
                } else {
                    boolean changed2 = now.isBefore(start);
                    if (changed2) {
                        changed = this.mOwners.setSystemUpdateFreezePeriodRecord(now, now);
                    } else {
                        changed = false;
                    }
                }
                if (changed && saveIfChanged) {
                    this.mOwners.writeDeviceOwner();
                }
            }
            changed = this.mOwners.setSystemUpdateFreezePeriodRecord(now, now);
            if (changed) {
                this.mOwners.writeDeviceOwner();
            }
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void clearSystemUpdatePolicyFreezePeriodRecord() {
        Preconditions.checkCallAuthorization(isAdb(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.CLEAR_FREEZE_PERIOD"), "Caller must be shell, or hold CLEAR_FREEZE_PERIOD permission to call clearSystemUpdatePolicyFreezePeriodRecord");
        synchronized (getLockObject()) {
            Slogf.i(LOG_TAG, "Clear freeze period record: " + this.mOwners.getSystemUpdateFreezePeriodRecordAsString());
            if (this.mOwners.setSystemUpdateFreezePeriodRecord(null, null)) {
                this.mOwners.writeDeviceOwner();
            }
        }
    }

    private boolean isUidDeviceOwnerLocked(int appUid) {
        ensureLocked();
        String deviceOwnerPackageName = this.mOwners.getDeviceOwnerComponent().getPackageName();
        try {
            String[] pkgs = this.mInjector.getIPackageManager().getPackagesForUid(appUid);
            if (pkgs == null) {
                return false;
            }
            for (String pkg : pkgs) {
                if (deviceOwnerPackageName.equals(pkg)) {
                    return true;
                }
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public void notifyPendingSystemUpdate(SystemUpdateInfo info) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.NOTIFY_PENDING_SYSTEM_UPDATE"), "Only the system update service can broadcast update information");
        if (UserHandle.getCallingUserId() != 0) {
            Slogf.w(LOG_TAG, "Only the system update service in the system user can broadcast update information.");
        } else if (!this.mOwners.saveSystemUpdateInfo(info)) {
        } else {
            final Intent intent = new Intent("android.app.action.NOTIFY_PENDING_SYSTEM_UPDATE").putExtra("android.app.extra.SYSTEM_UPDATE_RECEIVED_TIME", info == null ? -1L : info.getReceivedTime());
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda22
                public final void runOrThrow() {
                    DevicePolicyManagerService.this.m3050x2cce2f9f(intent);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyPendingSystemUpdate$104$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3050x2cce2f9f(Intent intent) throws Exception {
        synchronized (getLockObject()) {
            if (this.mOwners.hasDeviceOwner()) {
                UserHandle deviceOwnerUser = UserHandle.of(this.mOwners.getDeviceOwnerUserId());
                intent.setComponent(this.mOwners.getDeviceOwnerComponent());
                this.mContext.sendBroadcastAsUser(intent, deviceOwnerUser);
            }
        }
        try {
            int[] runningUserIds = this.mInjector.getIActivityManager().getRunningUserIds();
            for (int userId : runningUserIds) {
                synchronized (getLockObject()) {
                    ComponentName profileOwnerPackage = this.mOwners.getProfileOwnerComponent(userId);
                    if (profileOwnerPackage != null) {
                        intent.setComponent(profileOwnerPackage);
                        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
                    }
                }
            }
        } catch (RemoteException e) {
            Slogf.e(LOG_TAG, "Could not retrieve the list of running users", e);
        }
    }

    public SystemUpdateInfo getPendingSystemUpdate(ComponentName admin) {
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        return this.mOwners.getSystemUpdateInfo();
    }

    public void setPermissionPolicy(ComponentName admin, String callerPackage, int policy) {
        CallerIdentity caller = getCallerIdentity(admin, callerPackage);
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-permission-grant")));
        checkCanExecuteOrThrowUnsafe(38);
        int forUser = caller.getUserId();
        synchronized (getLockObject()) {
            DevicePolicyData userPolicy = m3013x8b75536b(forUser);
            if (userPolicy.mPermissionPolicy != policy) {
                userPolicy.mPermissionPolicy = policy;
                this.mPolicyCache.setPermissionPolicy(forUser, policy);
                saveSettingsLocked(forUser);
            }
        }
        DevicePolicyEventLogger.createEvent(18).setAdmin(caller.getPackageName()).setInt(policy).setBoolean(admin == null).write();
    }

    private void updatePermissionPolicyCache(int userId) {
        synchronized (getLockObject()) {
            DevicePolicyData userPolicy = m3013x8b75536b(userId);
            this.mPolicyCache.setPermissionPolicy(userId, userPolicy.mPermissionPolicy);
        }
    }

    public int getPermissionPolicy(ComponentName admin) throws RemoteException {
        int userId = UserHandle.getCallingUserId();
        return this.mPolicyCache.getPermissionPolicy(userId);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [14040=7] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:70:0x0122 */
    /* JADX DEBUG: Multi-variable search result rejected for r13v3, resolved type: long */
    /* JADX DEBUG: Multi-variable search result rejected for r20v0, resolved type: com.android.server.devicepolicy.DevicePolicyManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r13v0, types: [com.android.server.devicepolicy.CallerIdentity] */
    /* JADX WARN: Type inference failed for: r13v1 */
    /* JADX WARN: Type inference failed for: r13v6 */
    public void setPermissionGrantState(final ComponentName admin, String callerPackage, String packageName, final String permission, final int grantState, final RemoteCallback callback) throws RemoteException {
        Object obj;
        Object obj2;
        long ident;
        Injector injector;
        final boolean isPostQAdmin;
        Objects.requireNonNull(callback);
        final long ident2 = getCallerIdentity(admin, callerPackage);
        Preconditions.checkCallAuthorization((ident2.hasAdminComponent() && (isProfileOwner(ident2) || isDefaultDeviceOwner(ident2) || isFinancedDeviceOwner(ident2))) || (ident2.hasPackage() && isCallerDelegate(ident2, "delegation-permission-grant")));
        checkCanExecuteOrThrowUnsafe(37);
        Object lockObject = getLockObject();
        synchronized (lockObject) {
            try {
                try {
                    if (isFinancedDeviceOwner(ident2)) {
                        try {
                            enforcePermissionGrantStateOnFinancedDevice(packageName, permission);
                        } catch (Throwable th) {
                            th = th;
                            obj = lockObject;
                            throw th;
                        }
                    }
                    long ident3 = this.mInjector.binderClearCallingIdentity();
                    try {
                        try {
                            isPostQAdmin = getTargetSdk(ident2.getPackageName(), ident2.getUserId()) >= 29;
                            if (!isPostQAdmin) {
                                try {
                                    if (getTargetSdk(packageName, ident2.getUserId()) < 23) {
                                        callback.sendResult((Bundle) null);
                                        this.mInjector.binderRestoreCallingIdentity(ident3);
                                        return;
                                    }
                                } catch (SecurityException e) {
                                    e = e;
                                    obj2 = lockObject;
                                    ident = ident3;
                                    Slogf.e(LOG_TAG, "Could not set permission grant state", e);
                                    callback.sendResult((Bundle) null);
                                    injector = this.mInjector;
                                    injector.binderRestoreCallingIdentity(ident);
                                } catch (Throwable th2) {
                                    th = th2;
                                    ident2 = ident3;
                                    this.mInjector.binderRestoreCallingIdentity(ident2);
                                    throw th;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    } catch (SecurityException e2) {
                        e = e2;
                        obj2 = lockObject;
                        ident = ident3;
                    } catch (Throwable th4) {
                        th = th4;
                        ident2 = ident3;
                    }
                    if (!isRuntimePermission(permission)) {
                        callback.sendResult((Bundle) null);
                        this.mInjector.binderRestoreCallingIdentity(ident3);
                        return;
                    }
                    if (grantState == 1 || grantState == 2 || grantState == 0) {
                        AdminPermissionControlParams permissionParams = new AdminPermissionControlParams(packageName, permission, grantState, canAdminGrantSensorsPermissionsForUser(ident2.getUserId()));
                        PermissionControllerManager permissionControllerManager = this.mInjector.getPermissionControllerManager(ident2.getUserHandle());
                        String packageName2 = ident2.getPackageName();
                        obj2 = lockObject;
                        ident = ident3;
                        try {
                            permissionControllerManager.setRuntimePermissionGrantStateByDeviceAdmin(packageName2, permissionParams, this.mContext.getMainExecutor(), new Consumer() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda36
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj3) {
                                    DevicePolicyManagerService.lambda$setPermissionGrantState$105(isPostQAdmin, callback, ident2, permission, grantState, admin, (Boolean) obj3);
                                }
                            });
                        } catch (SecurityException e3) {
                            e = e3;
                            Slogf.e(LOG_TAG, "Could not set permission grant state", e);
                            callback.sendResult((Bundle) null);
                            injector = this.mInjector;
                            injector.binderRestoreCallingIdentity(ident);
                        }
                    } else {
                        obj2 = lockObject;
                        ident = ident3;
                    }
                    injector = this.mInjector;
                    injector.binderRestoreCallingIdentity(ident);
                } catch (Throwable th5) {
                    th = th5;
                }
            } catch (Throwable th6) {
                th = th6;
                obj = lockObject;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setPermissionGrantState$105(boolean isPostQAdmin, RemoteCallback callback, CallerIdentity caller, String permission, int grantState, ComponentName admin, Boolean permissionWasSet) {
        if (isPostQAdmin && !permissionWasSet.booleanValue()) {
            callback.sendResult((Bundle) null);
            return;
        }
        DevicePolicyEventLogger.createEvent(19).setAdmin(caller.getPackageName()).setStrings(new String[]{permission}).setInt(grantState).setBoolean(admin == null).write();
        callback.sendResult(Bundle.EMPTY);
    }

    private void enforcePermissionGrantStateOnFinancedDevice(String packageName, String permission) {
        if (!"android.permission.READ_PHONE_STATE".equals(permission)) {
            throw new SecurityException(permission + " cannot be used when managing a financed device for permission grant state");
        }
        if (!this.mOwners.getDeviceOwnerPackageName().equals(packageName)) {
            throw new SecurityException("Device owner package is the only package that can be used for permission grant state when managing a financed device");
        }
    }

    public int getPermissionGrantState(ComponentName admin, String callerPackage, final String packageName, final String permission) throws RemoteException {
        int intValue;
        final CallerIdentity caller = getCallerIdentity(admin, callerPackage);
        Preconditions.checkCallAuthorization(isSystemUid(caller) || (caller.hasAdminComponent() && (isProfileOwner(caller) || isDefaultDeviceOwner(caller) || isFinancedDeviceOwner(caller))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-permission-grant")));
        synchronized (getLockObject()) {
            if (isFinancedDeviceOwner(caller)) {
                enforcePermissionGrantStateOnFinancedDevice(packageName, permission);
            }
            intValue = ((Integer) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda71
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3005x991423a9(caller, permission, packageName);
                }
            })).intValue();
        }
        return intValue;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getPermissionGrantState$106$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Integer m3005x991423a9(CallerIdentity caller, String permission, String packageName) throws Exception {
        int granted;
        if (getTargetSdk(caller.getPackageName(), caller.getUserId()) < 29) {
            granted = this.mIPackageManager.checkPermission(permission, packageName, caller.getUserId());
        } else {
            try {
                int uid = this.mInjector.getPackageManager().getPackageUidAsUser(packageName, caller.getUserId());
                if (PermissionChecker.checkPermissionForPreflight(this.mContext, permission, -1, uid, packageName) != 0) {
                    granted = -1;
                } else {
                    granted = 0;
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new RemoteException("Cannot check if " + permission + "is a runtime permission", e, false, true);
            }
        }
        int permFlags = this.mInjector.getPackageManager().getPermissionFlags(permission, packageName, caller.getUserHandle());
        if ((permFlags & 4) != 4) {
            return 0;
        }
        return Integer.valueOf(granted != 0 ? 2 : 1);
    }

    boolean isPackageInstalledForUser(final String packageName, final int userHandle) {
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda70
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3034xb4089d8c(packageName, userHandle);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isPackageInstalledForUser$107$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3034xb4089d8c(String packageName, int userHandle) throws Exception {
        try {
            PackageInfo pi = this.mInjector.getIPackageManager().getPackageInfo(packageName, 0L, userHandle);
            return Boolean.valueOf((pi == null || pi.applicationInfo.flags == 0) ? false : true);
        } catch (RemoteException re) {
            throw new RuntimeException("Package manager has died", re);
        }
    }

    private boolean isRuntimePermission(String permissionName) {
        try {
            PackageManager packageManager = this.mInjector.getPackageManager();
            PermissionInfo permissionInfo = packageManager.getPermissionInfo(permissionName, 0);
            return (permissionInfo.protectionLevel & 15) == 1;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public boolean isProvisioningAllowed(String action, String packageName) {
        Objects.requireNonNull(packageName);
        CallerIdentity caller = getCallerIdentity();
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            List<String> callerUidPackageNames = Arrays.asList(this.mInjector.getPackageManager().getPackagesForUid(caller.getUid()));
            Preconditions.checkArgument(callerUidPackageNames.contains(packageName), "Caller uid doesn't match the one for the provided package.");
            this.mInjector.binderRestoreCallingIdentity(ident);
            return checkProvisioningPreconditionSkipPermission(action, packageName) == 0;
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
            throw th;
        }
    }

    public int checkProvisioningPrecondition(String action, String packageName) {
        Objects.requireNonNull(packageName, "packageName is null");
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        return checkProvisioningPreconditionSkipPermission(action, packageName);
    }

    private int checkProvisioningPreconditionSkipPermission(String action, String packageName) {
        if (!this.mHasFeature) {
            logMissingFeatureAction("Cannot check provisioning for action " + action);
            return 13;
        } else if (!isProvisioningAllowed()) {
            return 15;
        } else {
            int code = checkProvisioningPreConditionSkipPermissionNoLog(action, packageName);
            if (code != 0) {
                Slogf.d(LOG_TAG, "checkProvisioningPreCondition(" + action + ", " + packageName + ") failed: " + computeProvisioningErrorString(code, this.mInjector.userHandleGetCallingUserId()));
            }
            return code;
        }
    }

    private boolean isProvisioningAllowed() {
        boolean isDeveloperMode = isDeveloperMode(this.mContext);
        boolean isProvisioningAllowedForNormalUsers = SystemProperties.getBoolean(ALLOW_USER_PROVISIONING_KEY, true);
        return isDeveloperMode || isProvisioningAllowedForNormalUsers;
    }

    private static boolean isDeveloperMode(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "adb_enabled", 0) > 0;
    }

    private int checkProvisioningPreConditionSkipPermissionNoLog(String action, String packageName) {
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        if (action != null) {
            char c = 65535;
            switch (action.hashCode()) {
                case -920528692:
                    if (action.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                        c = 1;
                        break;
                    }
                    break;
                case -340845101:
                    if (action.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                        c = 0;
                        break;
                    }
                    break;
                case 1340354933:
                    if (action.equals("android.app.action.PROVISION_FINANCED_DEVICE")) {
                        c = 2;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    return checkManagedProfileProvisioningPreCondition(packageName, callingUserId);
                case 1:
                case 2:
                    return checkDeviceOwnerProvisioningPreCondition(callingUserId);
            }
        }
        throw new IllegalArgumentException("Unknown provisioning action " + action);
    }

    private int checkDeviceOwnerProvisioningPreConditionLocked(ComponentName owner, int deviceOwnerUserId, int callingUserId, boolean isAdb, boolean hasIncompatibleAccountsOrNonAdb) {
        if (this.mOwners.hasDeviceOwner()) {
            return 1;
        }
        if (this.mOwners.hasProfileOwner(deviceOwnerUserId)) {
            return 2;
        }
        boolean isHeadlessSystemUserMode = this.mInjector.userManagerIsHeadlessSystemUserMode();
        if (!isHeadlessSystemUserMode && !this.mUserManager.isUserRunning(new UserHandle(deviceOwnerUserId))) {
            return 3;
        }
        if (this.mIsWatch && hasPaired(0)) {
            return 8;
        }
        if (isHeadlessSystemUserMode && deviceOwnerUserId != 0) {
            Slogf.e(LOG_TAG, "In headless system user mode, device owner can only be set on headless system user.");
            return 7;
        } else if (isAdb) {
            if (this.mIsWatch || hasUserSetupCompleted(0)) {
                int maxNumberOfExistingUsers = isHeadlessSystemUserMode ? 2 : 1;
                if (this.mUserManager.getUserCount() > maxNumberOfExistingUsers) {
                    return 5;
                }
                int currentForegroundUser = getCurrentForegroundUserId();
                if (callingUserId != currentForegroundUser && this.mInjector.userManagerIsHeadlessSystemUserMode() && currentForegroundUser == 0) {
                    Slogf.wtf(LOG_TAG, "In headless system user mode, current user cannot be system user when setting device owner");
                    return 10;
                } else if (hasIncompatibleAccountsOrNonAdb) {
                    return 6;
                }
            }
            return 0;
        } else if (deviceOwnerUserId != 0) {
            return 7;
        } else {
            return hasUserSetupCompleted(0) ? 4 : 0;
        }
    }

    private int checkDeviceOwnerProvisioningPreCondition(int callingUserId) {
        int deviceOwnerUserId;
        int checkDeviceOwnerProvisioningPreConditionLocked;
        synchronized (getLockObject()) {
            if (this.mInjector.userManagerIsHeadlessSystemUserMode()) {
                deviceOwnerUserId = 0;
            } else {
                deviceOwnerUserId = callingUserId;
            }
            Slogf.i(LOG_TAG, "Calling user %d, device owner will be set on user %d", Integer.valueOf(callingUserId), Integer.valueOf(deviceOwnerUserId));
            checkDeviceOwnerProvisioningPreConditionLocked = checkDeviceOwnerProvisioningPreConditionLocked(null, deviceOwnerUserId, callingUserId, false, true);
        }
        return checkDeviceOwnerProvisioningPreConditionLocked;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [14343=5] */
    private int checkManagedProfileProvisioningPreCondition(String packageName, int callingUserId) {
        boolean hasDeviceOwner;
        if (hasFeatureManagedUsers()) {
            if (m3036x91e56c0c(callingUserId) != null) {
                return 2;
            }
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                UserHandle callingUserHandle = UserHandle.of(callingUserId);
                synchronized (getLockObject()) {
                    hasDeviceOwner = getDeviceOwnerAdminLocked() != null;
                }
                boolean addingProfileRestricted = this.mUserManager.hasUserRestriction("no_add_managed_profile", callingUserHandle);
                if (this.mUserManager.getUserInfo(callingUserId).isProfile()) {
                    Slogf.i(LOG_TAG, "Calling user %d is a profile, cannot add another.", Integer.valueOf(callingUserId));
                    return 11;
                }
                if (hasDeviceOwner && !addingProfileRestricted) {
                    Slogf.wtf(LOG_TAG, "Has a device owner but no restriction on adding a profile.");
                }
                if (addingProfileRestricted) {
                    Slogf.i(LOG_TAG, "Adding a profile is restricted: User %s Has device owner? %b", callingUserHandle, Boolean.valueOf(hasDeviceOwner));
                    return 11;
                } else if (this.mUserManager.canAddMoreManagedProfiles(callingUserId, false)) {
                    return 0;
                } else {
                    Slogf.i(LOG_TAG, "A work profile already exists.");
                    return 11;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
        return 9;
    }

    private void checkIsDeviceOwner(CallerIdentity caller) {
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller), caller.getUid() + " is not device owner");
    }

    private ComponentName getOwnerComponent(int userId) {
        synchronized (getLockObject()) {
            if (this.mOwners.getDeviceOwnerUserId() == userId) {
                return this.mOwners.getDeviceOwnerComponent();
            } else if (this.mOwners.hasProfileOwner(userId)) {
                return this.mOwners.getProfileOwnerComponent(userId);
            } else {
                return null;
            }
        }
    }

    private boolean hasFeatureManagedUsers() {
        try {
            return this.mIPackageManager.hasSystemFeature("android.software.managed_users", 0);
        } catch (RemoteException e) {
            return false;
        }
    }

    public String getWifiMacAddress(ComponentName admin) {
        Objects.requireNonNull(admin, "ComponentName is null");
        final CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller));
        return (String) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda111
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3015x18e09451(caller);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getWifiMacAddress$108$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ String m3015x18e09451(CallerIdentity caller) throws Exception {
        String[] macAddresses = this.mInjector.getWifiManager().getFactoryMacAddresses();
        if (macAddresses == null) {
            return null;
        }
        DevicePolicyEventLogger.createEvent(54).setAdmin(caller.getComponentName()).write();
        if (macAddresses.length > 0) {
            return macAddresses[0];
        }
        return null;
    }

    private int getTargetSdk(String packageName, int userId) {
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(packageName, 0L, userId);
            if (ai == null) {
                return 0;
            }
            return ai.targetSdkVersion;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public boolean isManagedProfile(ComponentName admin) {
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        return isManagedProfile(caller.getUserId());
    }

    public void reboot(final ComponentName admin) {
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(7);
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda83
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3054x23a760a1(admin);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reboot$109$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3054x23a760a1(ComponentName admin) throws Exception {
        if (this.mTelephonyManager.getCallState() != 0) {
            throw new IllegalStateException("Cannot be called with ongoing call on the device");
        }
        DevicePolicyEventLogger.createEvent(34).setAdmin(admin).write();
        this.mInjector.powerManagerReboot("deviceowner");
    }

    public void setShortSupportMessage(ComponentName who, CharSequence message) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CharSequence message2 = truncateIfLonger(message, 200);
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminForUidLocked(who, caller.getUid());
            if (!TextUtils.equals(admin.shortSupportMessage, message2)) {
                admin.shortSupportMessage = message2;
                saveSettingsLocked(caller.getUserId());
            }
        }
        DevicePolicyEventLogger.createEvent(43).setAdmin(who).write();
    }

    public CharSequence getShortSupportMessage(ComponentName who) {
        CharSequence charSequence;
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminForUidLocked(who, caller.getUid());
            charSequence = admin.shortSupportMessage;
        }
        return charSequence;
    }

    public void setLongSupportMessage(ComponentName who, CharSequence message) {
        if (!this.mHasFeature) {
            return;
        }
        CharSequence message2 = truncateIfLonger(message, MAX_LONG_SUPPORT_MESSAGE_LENGTH);
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminForUidLocked(who, caller.getUid());
            if (!TextUtils.equals(admin.longSupportMessage, message2)) {
                admin.longSupportMessage = message2;
                saveSettingsLocked(caller.getUserId());
            }
        }
        DevicePolicyEventLogger.createEvent(44).setAdmin(who).write();
    }

    public CharSequence getLongSupportMessage(ComponentName who) {
        CharSequence charSequence;
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminForUidLocked(who, caller.getUid());
            charSequence = admin.longSupportMessage;
        }
        return charSequence;
    }

    public CharSequence getShortSupportMessageForUser(ComponentName who, int userHandle) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "query support message for user"));
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin != null) {
                    return admin.shortSupportMessage;
                }
                return null;
            }
        }
        return null;
    }

    public CharSequence getLongSupportMessageForUser(ComponentName who, int userHandle) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "query support message for user"));
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                if (admin != null) {
                    return admin.longSupportMessage;
                }
                return null;
            }
        }
        return null;
    }

    public void setOrganizationColor(ComponentName who, int color) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallingUser(isManagedProfile(caller.getUserId()));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            admin.organizationColor = color;
            saveSettingsLocked(caller.getUserId());
        }
        DevicePolicyEventLogger.createEvent(39).setAdmin(caller.getComponentName()).write();
    }

    public void setOrganizationColorForUser(int color, int userId) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkArgumentNonnegative(userId, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userId));
        Preconditions.checkCallAuthorization(canManageUsers(caller));
        Preconditions.checkCallAuthorization(isManagedProfile(userId), "You can not set organization color outside a managed profile, userId = %d", new Object[]{Integer.valueOf(userId)});
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            admin.organizationColor = color;
            saveSettingsLocked(userId);
        }
    }

    public int getOrganizationColor(ComponentName who) {
        int i;
        if (!this.mHasFeature) {
            return ActiveAdmin.DEF_ORGANIZATION_COLOR;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallingUser(isManagedProfile(caller.getUserId()));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            i = admin.organizationColor;
        }
        return i;
    }

    public int getOrganizationColorForUser(int userHandle) {
        int i;
        if (!this.mHasFeature) {
            return ActiveAdmin.DEF_ORGANIZATION_COLOR;
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
        Preconditions.checkCallAuthorization(isManagedProfile(userHandle), "You can not get organization color outside a managed profile, userId = %d", new Object[]{Integer.valueOf(userHandle)});
        synchronized (getLockObject()) {
            ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userHandle);
            if (profileOwner != null) {
                i = profileOwner.organizationColor;
            } else {
                i = ActiveAdmin.DEF_ORGANIZATION_COLOR;
            }
        }
        return i;
    }

    public void setOrganizationName(ComponentName who, CharSequence text) {
        String str;
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        CharSequence text2 = truncateIfLonger(text, 200);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            if (!TextUtils.equals(admin.organizationName, text2)) {
                if (text2 != null && text2.length() != 0) {
                    str = text2.toString();
                    admin.organizationName = str;
                    saveSettingsLocked(caller.getUserId());
                }
                str = null;
                admin.organizationName = str;
                saveSettingsLocked(caller.getUserId());
            }
        }
    }

    public CharSequence getOrganizationName(ComponentName who) {
        String str;
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallingUser(isManagedProfile(caller.getUserId()));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            str = admin.organizationName;
        }
        return str;
    }

    public CharSequence getDeviceOwnerOrganizationName() {
        String str = null;
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || canManageUsers(caller) || isFinancedDeviceOwner(caller));
            synchronized (getLockObject()) {
                ActiveAdmin deviceOwnerAdmin = getDeviceOwnerAdminLocked();
                if (deviceOwnerAdmin != null) {
                    str = deviceOwnerAdmin.organizationName;
                }
            }
            return str;
        }
        return null;
    }

    public CharSequence getOrganizationNameForUser(int userHandle) {
        String str;
        if (this.mHasFeature) {
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            Preconditions.checkCallAuthorization(hasFullCrossUsersPermission(caller, userHandle));
            Preconditions.checkCallAuthorization(canManageUsers(caller));
            Preconditions.checkCallAuthorization(isManagedProfile(userHandle), "You can not get organization name outside a managed profile, userId = %d", new Object[]{Integer.valueOf(userHandle)});
            synchronized (getLockObject()) {
                ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userHandle);
                str = profileOwner != null ? profileOwner.organizationName : null;
            }
            return str;
        }
        return null;
    }

    public List<String> setMeteredDataDisabledPackages(ComponentName who, final List<String> packageNames) {
        List<String> list;
        Objects.requireNonNull(who);
        Objects.requireNonNull(packageNames);
        final CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller), "Admin %s does not own the profile", new Object[]{caller.getComponentName()});
        if (!this.mHasFeature) {
            return packageNames;
        }
        synchronized (getLockObject()) {
            final ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            list = (List) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda78
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3090xf0beb57(caller, packageNames, admin);
                }
            });
        }
        return list;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setMeteredDataDisabledPackages$110$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ List m3090xf0beb57(CallerIdentity caller, List packageNames, ActiveAdmin admin) throws Exception {
        List<String> excludedPkgs = removeInvalidPkgsForMeteredDataRestriction(caller.getUserId(), packageNames);
        admin.meteredDisabledPackages = packageNames;
        pushMeteredDisabledPackages(caller.getUserId());
        saveSettingsLocked(caller.getUserId());
        return excludedPkgs;
    }

    private List<String> removeInvalidPkgsForMeteredDataRestriction(int userId, List<String> pkgNames) {
        Set<String> activeAdmins = getActiveAdminPackagesLocked(userId);
        List<String> excludedPkgs = new ArrayList<>();
        for (int i = pkgNames.size() - 1; i >= 0; i--) {
            String pkgName = pkgNames.get(i);
            if (activeAdmins.contains(pkgName)) {
                excludedPkgs.add(pkgName);
            } else {
                try {
                    if (!this.mInjector.getIPackageManager().isPackageAvailable(pkgName, userId)) {
                        excludedPkgs.add(pkgName);
                    }
                } catch (RemoteException e) {
                }
            }
        }
        pkgNames.removeAll(excludedPkgs);
        return excludedPkgs;
    }

    public List<String> getMeteredDataDisabledPackages(ComponentName who) {
        List<String> arrayList;
        Objects.requireNonNull(who);
        if (!this.mHasFeature) {
            return new ArrayList();
        }
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller), "Admin %s does not own the profile", new Object[]{caller.getComponentName()});
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            arrayList = admin.meteredDisabledPackages == null ? new ArrayList<>() : admin.meteredDisabledPackages;
        }
        return arrayList;
    }

    public boolean isMeteredDataDisabledPackageForUser(ComponentName who, String packageName, int userId) {
        Objects.requireNonNull(who);
        if (this.mHasFeature) {
            Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "query restricted pkgs for a specific user"));
            synchronized (getLockObject()) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userId);
                if (admin == null || admin.meteredDisabledPackages == null) {
                    return false;
                }
                return admin.meteredDisabledPackages.contains(packageName);
            }
        }
        return false;
    }

    public void setProfileOwnerOnOrganizationOwnedDevice(ComponentName who, int userId, boolean isProfileOwnerOnOrganizationOwnedDevice) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who);
        CallerIdentity caller = getCallerIdentity();
        if (!isAdb(caller) && !hasCallingPermission("android.permission.MARK_DEVICE_ORGANIZATION_OWNED") && !hasCallingPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS")) {
            throw new SecurityException("Only the system can mark a profile owner of organization-owned device.");
        }
        synchronized (getLockObject()) {
            if (!isProfileOwnerOnOrganizationOwnedDevice) {
                if (!isAdminTestOnlyLocked(who, userId)) {
                    throw new SecurityException("Only a test admin can be unmarked as a profile owner of organization-owned device.");
                }
            }
        }
        if (isAdb(caller)) {
            if (hasIncompatibleAccountsOrNonAdbNoLock(caller, userId, who)) {
                throw new SecurityException("Can only be called from ADB if the device has no accounts.");
            }
        } else if (hasUserSetupCompleted(0)) {
            throw new IllegalStateException("Cannot mark profile owner as managing an organization-owned device after set-up");
        }
        synchronized (getLockObject()) {
            setProfileOwnerOnOrganizationOwnedDeviceUncheckedLocked(who, userId, isProfileOwnerOnOrganizationOwnedDevice);
        }
    }

    private void setProfileOwnerOnOrganizationOwnedDeviceUncheckedLocked(ComponentName who, final int userId, final boolean isProfileOwnerOnOrganizationOwnedDevice) {
        if (!isProfileOwner(who, userId)) {
            throw new IllegalArgumentException(String.format("Component %s is not a Profile Owner of user %d", who.flattenToString(), Integer.valueOf(userId)));
        }
        Object[] objArr = new Object[3];
        objArr[0] = isProfileOwnerOnOrganizationOwnedDevice ? "Marking" : "Unmarking";
        objArr[1] = who.flattenToString();
        objArr[2] = Integer.valueOf(userId);
        Slogf.i(LOG_TAG, "%s %s as profile owner on organization-owned device for user %d", objArr);
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda113
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3101x7707b46e(userId, isProfileOwnerOnOrganizationOwnedDevice);
            }
        });
        this.mOwners.setProfileOwnerOfOrganizationOwnedDevice(userId, isProfileOwnerOnOrganizationOwnedDevice);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setProfileOwnerOnOrganizationOwnedDeviceUncheckedLocked$111$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3101x7707b46e(int userId, boolean isProfileOwnerOnOrganizationOwnedDevice) throws Exception {
        UserHandle parentUser = this.mUserManager.getProfileParent(UserHandle.of(userId));
        if (parentUser == null) {
            throw new IllegalStateException(String.format("User %d is not a profile", Integer.valueOf(userId)));
        }
        if (!parentUser.isSystem()) {
            throw new IllegalStateException(String.format("Only the profile owner of a managed profile on the primary user can be granted access to device identifiers, not on user %d", Integer.valueOf(parentUser.getIdentifier())));
        }
        this.mUserManager.setUserRestriction("no_remove_managed_profile", isProfileOwnerOnOrganizationOwnedDevice, parentUser);
        this.mUserManager.setUserRestriction("no_add_user", isProfileOwnerOnOrganizationOwnedDevice, parentUser);
    }

    private void pushMeteredDisabledPackages(int userId) {
        wtfIfInLock();
        this.mInjector.getNetworkPolicyManagerInternal().setMeteredRestrictedPackages(getMeteredDisabledPackages(userId), userId);
    }

    private Set<String> getMeteredDisabledPackages(int userId) {
        Set<String> restrictedPkgs;
        synchronized (getLockObject()) {
            restrictedPkgs = new ArraySet<>();
            ActiveAdmin admin = getDeviceOrProfileOwnerAdminLocked(userId);
            if (admin != null && admin.meteredDisabledPackages != null) {
                restrictedPkgs.addAll(admin.meteredDisabledPackages);
            }
        }
        return restrictedPkgs;
    }

    public void setAffiliationIds(ComponentName admin, List<String> ids) {
        boolean z;
        if (!this.mHasFeature) {
            return;
        }
        if (ids == null) {
            throw new IllegalArgumentException("ids must not be null");
        }
        Iterator<String> it = ids.iterator();
        while (true) {
            z = true;
            if (!it.hasNext()) {
                break;
            }
            String id = it.next();
            Preconditions.checkArgument(true ^ TextUtils.isEmpty(id), "ids must not have empty string");
            enforceMaxStringLength(id, "affiliation id");
        }
        Set<String> affiliationIds = new ArraySet<>(ids);
        CallerIdentity caller = getCallerIdentity(admin);
        if (!isProfileOwner(caller) && !isDefaultDeviceOwner(caller)) {
            z = false;
        }
        Preconditions.checkCallAuthorization(z);
        int callingUserId = caller.getUserId();
        synchronized (getLockObject()) {
            m3013x8b75536b(callingUserId).mAffiliationIds = affiliationIds;
            saveSettingsLocked(callingUserId);
            if (callingUserId != 0 && isDeviceOwner(admin, callingUserId)) {
                m3013x8b75536b(0).mAffiliationIds = affiliationIds;
                saveSettingsLocked(0);
            }
            maybePauseDeviceWideLoggingLocked();
            maybeResumeDeviceWideLoggingLocked();
            maybeClearLockTaskPolicyLocked();
            updateAdminCanGrantSensorsPermissionCache(callingUserId);
        }
    }

    public List<String> getAffiliationIds(ComponentName admin) {
        ArrayList arrayList;
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        Objects.requireNonNull(admin);
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        synchronized (getLockObject()) {
            arrayList = new ArrayList(m3013x8b75536b(caller.getUserId()).mAffiliationIds);
        }
        return arrayList;
    }

    public boolean isCallingUserAffiliated() {
        boolean isUserAffiliatedWithDeviceLocked;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            isUserAffiliatedWithDeviceLocked = isUserAffiliatedWithDeviceLocked(this.mInjector.userHandleGetCallingUserId());
        }
        return isUserAffiliatedWithDeviceLocked;
    }

    public boolean isAffiliatedUser(int userId) {
        if (!this.mHasFeature) {
            return false;
        }
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasCrossUsersPermission(caller, userId));
        return isUserAffiliatedWithDevice(userId);
    }

    private boolean isUserAffiliatedWithDevice(int userId) {
        boolean isUserAffiliatedWithDeviceLocked;
        synchronized (getLockObject()) {
            isUserAffiliatedWithDeviceLocked = isUserAffiliatedWithDeviceLocked(userId);
        }
        return isUserAffiliatedWithDeviceLocked;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isUserAffiliatedWithDeviceLocked(int userId) {
        if (this.mOwners.hasDeviceOwner()) {
            if (userId == 0 || userId == this.mOwners.getDeviceOwnerUserId()) {
                return true;
            }
            ComponentName profileOwner = m3036x91e56c0c(userId);
            if (profileOwner == null) {
                return false;
            }
            Set<String> userAffiliationIds = m3013x8b75536b(userId).mAffiliationIds;
            Set<String> deviceAffiliationIds = m3013x8b75536b(0).mAffiliationIds;
            for (String id : userAffiliationIds) {
                if (deviceAffiliationIds.contains(id)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean areAllUsersAffiliatedWithDeviceLocked() {
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda3
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m2979xbaa17530();
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$areAllUsersAffiliatedWithDeviceLocked$112$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m2979xbaa17530() throws Exception {
        List<UserInfo> userInfos = this.mUserManager.getAliveUsers();
        for (int i = 0; i < userInfos.size(); i++) {
            int userId = userInfos.get(i).id;
            if (!isUserAffiliatedWithDeviceLocked(userId)) {
                Slogf.d(LOG_TAG, "User id " + userId + " not affiliated.");
                return false;
            }
        }
        return true;
    }

    private int getSecurityLoggingEnabledUser() {
        synchronized (getLockObject()) {
            if (this.mOwners.hasDeviceOwner()) {
                return -1;
            }
            return getOrganizationOwnedProfileUserId();
        }
    }

    public void setSecurityLoggingEnabled(ComponentName admin, String packageName, boolean enabled) {
        boolean z;
        if (!this.mHasFeature) {
            return;
        }
        CallerIdentity caller = getCallerIdentity(admin, packageName);
        synchronized (getLockObject()) {
            if (admin != null) {
                if (!isProfileOwnerOfOrganizationOwnedDevice(caller) && !isDefaultDeviceOwner(caller)) {
                    z = false;
                    Preconditions.checkCallAuthorization(z);
                }
                z = true;
                Preconditions.checkCallAuthorization(z);
            } else {
                Preconditions.checkCallAuthorization(isCallerDelegate(caller, "delegation-security-logging"));
            }
            if (enabled == this.mInjector.securityLogGetLoggingEnabledProperty()) {
                return;
            }
            this.mInjector.securityLogSetLoggingEnabledProperty(enabled);
            if (enabled) {
                this.mSecurityLogMonitor.start(getSecurityLoggingEnabledUser());
                maybePauseDeviceWideLoggingLocked();
            } else {
                this.mSecurityLogMonitor.stop();
            }
            DevicePolicyEventLogger.createEvent(15).setAdmin(admin).setBoolean(enabled).write();
        }
    }

    public boolean isSecurityLoggingEnabled(ComponentName admin, String packageName) {
        boolean securityLogGetLoggingEnabledProperty;
        boolean z = false;
        if (this.mHasFeature) {
            synchronized (getLockObject()) {
                if (!isSystemUid(getCallerIdentity())) {
                    CallerIdentity caller = getCallerIdentity(admin, packageName);
                    if (admin != null) {
                        Preconditions.checkCallAuthorization((isProfileOwnerOfOrganizationOwnedDevice(caller) || isDefaultDeviceOwner(caller)) ? true : true);
                    } else {
                        Preconditions.checkCallAuthorization(isCallerDelegate(caller, "delegation-security-logging"));
                    }
                }
                securityLogGetLoggingEnabledProperty = this.mInjector.securityLogGetLoggingEnabledProperty();
            }
            return securityLogGetLoggingEnabledProperty;
        }
        return false;
    }

    private void recordSecurityLogRetrievalTime() {
        synchronized (getLockObject()) {
            long currentTime = System.currentTimeMillis();
            DevicePolicyData policyData = m3013x8b75536b(0);
            if (currentTime > policyData.mLastSecurityLogRetrievalTime) {
                policyData.mLastSecurityLogRetrievalTime = currentTime;
                saveSettingsLocked(0);
            }
        }
    }

    public ParceledListSlice<SecurityLog.SecurityEvent> retrievePreRebootSecurityLogs(ComponentName admin, String packageName) {
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity(admin, packageName);
            boolean z = false;
            if (admin != null) {
                Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller) || isDefaultDeviceOwner(caller));
            } else {
                Preconditions.checkCallAuthorization(isCallerDelegate(caller, "delegation-security-logging"));
            }
            if (isOrganizationOwnedDeviceWithManagedProfile() || areAllUsersAffiliatedWithDeviceLocked()) {
                z = true;
            }
            Preconditions.checkCallAuthorization(z);
            DevicePolicyEventLogger.createEvent(17).setAdmin(caller.getComponentName()).write();
            if (this.mContext.getResources().getBoolean(17891773) && this.mInjector.securityLogGetLoggingEnabledProperty()) {
                recordSecurityLogRetrievalTime();
                ArrayList<SecurityLog.SecurityEvent> output = new ArrayList<>();
                try {
                    SecurityLog.readPreviousEvents(output);
                    int enabledUser = getSecurityLoggingEnabledUser();
                    if (enabledUser != -1) {
                        SecurityLog.redactEvents(output, enabledUser);
                    }
                    return new ParceledListSlice<>(output);
                } catch (IOException e) {
                    Slogf.w(LOG_TAG, "Fail to read previous events", e);
                    return new ParceledListSlice<>(Collections.emptyList());
                }
            }
            return null;
        }
        return null;
    }

    public ParceledListSlice<SecurityLog.SecurityEvent> retrieveSecurityLogs(ComponentName admin, String packageName) {
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity(admin, packageName);
            boolean z = false;
            if (admin != null) {
                Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller) || isDefaultDeviceOwner(caller));
            } else {
                Preconditions.checkCallAuthorization(isCallerDelegate(caller, "delegation-security-logging"));
            }
            if (isOrganizationOwnedDeviceWithManagedProfile() || areAllUsersAffiliatedWithDeviceLocked()) {
                z = true;
            }
            Preconditions.checkCallAuthorization(z);
            if (this.mInjector.securityLogGetLoggingEnabledProperty()) {
                recordSecurityLogRetrievalTime();
                List<SecurityLog.SecurityEvent> logs = this.mSecurityLogMonitor.retrieveLogs();
                DevicePolicyEventLogger.createEvent(16).setAdmin(caller.getComponentName()).write();
                if (logs != null) {
                    return new ParceledListSlice<>(logs);
                }
                return null;
            }
            return null;
        }
        return null;
    }

    public long forceSecurityLogs() {
        Preconditions.checkCallAuthorization(isAdb(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.FORCE_DEVICE_POLICY_MANAGER_LOGS"), "Caller must be shell or hold FORCE_DEVICE_POLICY_MANAGER_LOGS to call forceSecurityLogs");
        if (!this.mInjector.securityLogGetLoggingEnabledProperty()) {
            throw new IllegalStateException("logging is not available");
        }
        return this.mSecurityLogMonitor.forceLogs();
    }

    public boolean isUninstallInQueue(String packageName) {
        boolean contains;
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS"));
        Pair<String, Integer> packageUserPair = new Pair<>(packageName, Integer.valueOf(caller.getUserId()));
        synchronized (getLockObject()) {
            contains = this.mPackagesToRemove.contains(packageUserPair);
        }
        return contains;
    }

    public void uninstallPackageWithActiveAdmins(final String packageName) {
        Preconditions.checkArgument(!TextUtils.isEmpty(packageName));
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS"));
        final int userId = caller.getUserId();
        enforceUserUnlocked(userId);
        ComponentName profileOwner = m3036x91e56c0c(userId);
        if (profileOwner != null && packageName.equals(profileOwner.getPackageName())) {
            throw new IllegalArgumentException("Cannot uninstall a package with a profile owner");
        }
        ComponentName deviceOwner = getDeviceOwnerComponent(false);
        if (getDeviceOwnerUserId() == userId && deviceOwner != null && packageName.equals(deviceOwner.getPackageName())) {
            throw new IllegalArgumentException("Cannot uninstall a package with a device owner");
        }
        Pair<String, Integer> packageUserPair = new Pair<>(packageName, Integer.valueOf(userId));
        synchronized (getLockObject()) {
            this.mPackagesToRemove.add(packageUserPair);
        }
        List<ComponentName> allActiveAdmins = getActiveAdmins(userId);
        final List<ComponentName> packageActiveAdmins = new ArrayList<>();
        if (allActiveAdmins != null) {
            for (ComponentName activeAdmin : allActiveAdmins) {
                if (packageName.equals(activeAdmin.getPackageName())) {
                    packageActiveAdmins.add(activeAdmin);
                    removeActiveAdmin(activeAdmin, userId);
                }
            }
        }
        if (packageActiveAdmins.size() == 0) {
            startUninstallIntent(packageName, userId);
        } else {
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService.7
                @Override // java.lang.Runnable
                public void run() {
                    for (ComponentName activeAdmin2 : packageActiveAdmins) {
                        DevicePolicyManagerService.this.removeAdminArtifacts(activeAdmin2, userId);
                    }
                    DevicePolicyManagerService.this.startUninstallIntent(packageName, userId);
                }
            }, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    public boolean isDeviceProvisioned() {
        boolean z;
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(canManageUsers(caller));
        synchronized (getLockObject()) {
            z = getUserDataUnchecked(0).mUserSetupComplete;
        }
        return z;
    }

    private boolean isCurrentUserDemo() {
        if (UserManager.isDeviceInDemoMode(this.mContext)) {
            final int userId = this.mInjector.userHandleGetCallingUserId();
            return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda27
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3028xb3a2d58a(userId);
                }
            })).booleanValue();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isCurrentUserDemo$113$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3028xb3a2d58a(int userId) throws Exception {
        return Boolean.valueOf(this.mUserManager.getUserInfo(userId).isDemo());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removePackageIfRequired(String packageName, int userId) {
        if (!packageHasActiveAdmins(packageName, userId)) {
            startUninstallIntent(packageName, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startUninstallIntent(String packageName, int userId) {
        Pair<String, Integer> packageUserPair = new Pair<>(packageName, Integer.valueOf(userId));
        synchronized (getLockObject()) {
            if (this.mPackagesToRemove.contains(packageUserPair)) {
                this.mPackagesToRemove.remove(packageUserPair);
                if (!isPackageInstalledForUser(packageName, userId)) {
                    return;
                }
                try {
                    this.mInjector.getIActivityManager().forceStopPackage(packageName, userId);
                } catch (RemoteException e) {
                    Slogf.e(LOG_TAG, "Failure talking to ActivityManager while force stopping package");
                }
                Uri packageURI = Uri.parse("package:" + packageName);
                Intent uninstallIntent = new Intent("android.intent.action.UNINSTALL_PACKAGE", packageURI);
                uninstallIntent.setFlags(268435456);
                this.mContext.startActivityAsUser(uninstallIntent, UserHandle.of(userId));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeAdminArtifacts(ComponentName adminReceiver, int userHandle) {
        synchronized (getLockObject()) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
            if (admin == null) {
                return;
            }
            DevicePolicyData policy = m3013x8b75536b(userHandle);
            boolean doProxyCleanup = admin.info.usesPolicy(5);
            policy.mAdminList.remove(admin);
            policy.mAdminMap.remove(adminReceiver);
            policy.validatePasswordOwner();
            if (doProxyCleanup) {
                m3085xe47a50dd(policy);
            }
            pushActiveAdminPackagesLocked(userHandle);
            saveSettingsLocked(userHandle);
            updateMaximumTimeToLockLocked(userHandle);
            policy.mRemovingAdmins.remove(adminReceiver);
            pushScreenCapturePolicy(userHandle);
            Slogf.i(LOG_TAG, "Device admin " + adminReceiver + " removed from user " + userHandle);
            pushMeteredDisabledPackages(userHandle);
            pushUserRestrictions(userHandle);
        }
    }

    public void setDeviceProvisioningConfigApplied() {
        Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()));
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(0);
            policy.mDeviceProvisioningConfigApplied = true;
            saveSettingsLocked(0);
        }
    }

    public boolean isDeviceProvisioningConfigApplied() {
        boolean z;
        Preconditions.checkCallAuthorization(canManageUsers(getCallerIdentity()));
        synchronized (getLockObject()) {
            DevicePolicyData policy = m3013x8b75536b(0);
            z = policy.mDeviceProvisioningConfigApplied;
        }
        return z;
    }

    public void forceUpdateUserSetupComplete(int userId) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        boolean isUserCompleted = this.mInjector.settingsSecureGetIntForUser("user_setup_complete", 0, userId) != 0;
        DevicePolicyData policy = m3013x8b75536b(userId);
        policy.mUserSetupComplete = isUserCompleted;
        this.mStateCache.setDeviceProvisioned(isUserCompleted);
        synchronized (getLockObject()) {
            saveSettingsLocked(userId);
        }
    }

    public void setBackupServiceEnabled(ComponentName admin, boolean enabled) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller) || isFinancedDeviceOwner(caller));
        toggleBackupServiceActive(caller.getUserId(), enabled);
    }

    public boolean isBackupServiceEnabled(ComponentName admin) {
        boolean z = true;
        if (this.mHasFeature) {
            Objects.requireNonNull(admin, "ComponentName is null");
            final CallerIdentity caller = getCallerIdentity(admin);
            if (!isDefaultDeviceOwner(caller) && !isProfileOwner(caller) && !isFinancedDeviceOwner(caller)) {
                z = false;
            }
            Preconditions.checkCallAuthorization(z);
            return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda75
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3025x458c0614(caller);
                }
            })).booleanValue();
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isBackupServiceEnabled$114$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3025x458c0614(CallerIdentity caller) throws Exception {
        Boolean valueOf;
        synchronized (getLockObject()) {
            try {
                try {
                    IBackupManager ibm = this.mInjector.getIBackupManager();
                    valueOf = Boolean.valueOf(ibm != null && ibm.isBackupServiceActive(caller.getUserId()));
                } catch (RemoteException e) {
                    throw new IllegalStateException("Failed requesting backup service state.", e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return valueOf;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [15479=5] */
    public boolean bindDeviceAdminServiceAsUser(ComponentName admin, IApplicationThread caller, IBinder activtiyToken, Intent serviceIntent, IServiceConnection connection, int flags, int targetUserId) {
        String targetPackage;
        long callingIdentity;
        if (!this.mHasFeature) {
            return false;
        }
        Objects.requireNonNull(admin);
        Objects.requireNonNull(caller);
        Objects.requireNonNull(serviceIntent);
        Preconditions.checkArgument((serviceIntent.getComponent() == null && serviceIntent.getPackage() == null) ? false : true, "Service intent must be explicit (with a package name or component): " + serviceIntent);
        Objects.requireNonNull(connection);
        Preconditions.checkArgument(this.mInjector.userHandleGetCallingUserId() != targetUserId, "target user id must be different from the calling user id");
        if (!getBindDeviceAdminTargetUsers(admin).contains(UserHandle.of(targetUserId))) {
            throw new SecurityException("Not allowed to bind to target user id");
        }
        synchronized (getLockObject()) {
            targetPackage = getOwnerPackageNameForUserLocked(targetUserId);
        }
        long callingIdentity2 = this.mInjector.binderClearCallingIdentity();
        try {
            Intent sanitizedIntent = createCrossUserServiceIntent(serviceIntent, targetPackage, targetUserId);
            if (sanitizedIntent == null) {
                this.mInjector.binderRestoreCallingIdentity(callingIdentity2);
                return false;
            }
            callingIdentity = callingIdentity2;
            try {
                boolean z = this.mInjector.getIActivityManager().bindService(caller, activtiyToken, serviceIntent, serviceIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), connection, flags, this.mContext.getOpPackageName(), targetUserId) != 0;
                this.mInjector.binderRestoreCallingIdentity(callingIdentity);
                return z;
            } catch (RemoteException e) {
                this.mInjector.binderRestoreCallingIdentity(callingIdentity);
                return false;
            } catch (Throwable th) {
                th = th;
                this.mInjector.binderRestoreCallingIdentity(callingIdentity);
                throw th;
            }
        } catch (RemoteException e2) {
            callingIdentity = callingIdentity2;
        } catch (Throwable th2) {
            th = th2;
            callingIdentity = callingIdentity2;
        }
    }

    public List<UserHandle> getBindDeviceAdminTargetUsers(final ComponentName admin) {
        List<UserHandle> list;
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        Objects.requireNonNull(admin);
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        synchronized (getLockObject()) {
            final int callingUserId = caller.getUserId();
            list = (List) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda162
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m2999xc8c89574(admin, callingUserId);
                }
            });
        }
        return list;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getBindDeviceAdminTargetUsers$115$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ ArrayList m2999xc8c89574(ComponentName admin, int callingUserId) throws Exception {
        ArrayList<UserHandle> targetUsers = new ArrayList<>();
        if (!isDeviceOwner(admin, callingUserId)) {
            if (canUserBindToDeviceOwnerLocked(callingUserId)) {
                targetUsers.add(UserHandle.of(this.mOwners.getDeviceOwnerUserId()));
            }
        } else {
            List<UserInfo> userInfos = this.mUserManager.getAliveUsers();
            for (int i = 0; i < userInfos.size(); i++) {
                int userId = userInfos.get(i).id;
                if (userId != callingUserId && canUserBindToDeviceOwnerLocked(userId)) {
                    targetUsers.add(UserHandle.of(userId));
                }
            }
        }
        return targetUsers;
    }

    private boolean canUserBindToDeviceOwnerLocked(int userId) {
        if (this.mOwners.hasDeviceOwner() && userId != this.mOwners.getDeviceOwnerUserId() && this.mOwners.hasProfileOwner(userId) && TextUtils.equals(this.mOwners.getDeviceOwnerPackageName(), this.mOwners.getProfileOwnerPackage(userId))) {
            return isUserAffiliatedWithDeviceLocked(userId);
        }
        return false;
    }

    private boolean hasIncompatibleAccountsOrNonAdbNoLock(CallerIdentity caller, final int userId, final ComponentName owner) {
        if (!isAdb(caller)) {
            return true;
        }
        wtfIfInLock();
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda41
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3016xc73f8dca(userId, owner);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hasIncompatibleAccountsOrNonAdbNoLock$116$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3016xc73f8dca(int userId, ComponentName owner) throws Exception {
        AccountManager am = AccountManager.get(this.mContext);
        Account[] accounts = am.getAccountsAsUser(userId);
        if (accounts.length == 0) {
            return false;
        }
        synchronized (getLockObject()) {
            if (owner != null) {
                if (isAdminTestOnlyLocked(owner, userId)) {
                    String[] feature_allow = {"android.account.DEVICE_OR_PROFILE_OWNER_ALLOWED"};
                    String[] feature_disallow = {"android.account.DEVICE_OR_PROFILE_OWNER_DISALLOWED"};
                    boolean compatible = true;
                    int length = accounts.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        Account account = accounts[i];
                        if (hasAccountFeatures(am, account, feature_disallow)) {
                            Slogf.e(LOG_TAG, "%s has %s", account, feature_disallow[0]);
                            compatible = false;
                            break;
                        } else if (hasAccountFeatures(am, account, feature_allow)) {
                            i++;
                        } else {
                            Slogf.e(LOG_TAG, "%s doesn't have %s", account, feature_allow[0]);
                            compatible = false;
                            break;
                        }
                    }
                    if (compatible) {
                        Slogf.w(LOG_TAG, "All accounts are compatible");
                    } else {
                        Slogf.e(LOG_TAG, "Found incompatible accounts");
                    }
                    return Boolean.valueOf(!compatible);
                }
            }
            Slogf.w(LOG_TAG, "Non test-only owner can't be installed with existing accounts.");
            return true;
        }
    }

    private boolean hasAccountFeatures(AccountManager am, Account account, String[] features) {
        try {
            return am.hasFeatures(account, features, null, null).getResult().booleanValue();
        } catch (Exception e) {
            Slogf.w(LOG_TAG, "Failed to get account feature", e);
            return false;
        }
    }

    private boolean isAdb(CallerIdentity caller) {
        return isShellUid(caller) || isRootUid(caller);
    }

    public void setNetworkLoggingEnabled(ComponentName admin, String packageName, boolean enabled) {
        if (!this.mHasFeature) {
            return;
        }
        CallerIdentity caller = getCallerIdentity(admin, packageName);
        boolean isManagedProfileOwner = isProfileOwner(caller) && isManagedProfile(caller.getUserId());
        Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isDefaultDeviceOwner(caller) || isManagedProfileOwner)) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-network-logging")));
        synchronized (getLockObject()) {
            if (enabled == isNetworkLoggingEnabledInternalLocked()) {
                return;
            }
            ActiveAdmin activeAdmin = getDeviceOrProfileOwnerAdminLocked(caller.getUserId());
            activeAdmin.isNetworkLoggingEnabled = enabled;
            if (!enabled) {
                activeAdmin.numNetworkLoggingNotifications = 0;
                activeAdmin.lastNetworkLoggingNotificationTimeMs = 0L;
            }
            saveSettingsLocked(caller.getUserId());
            setNetworkLoggingActiveInternal(enabled);
            DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(119).setAdmin(caller.getPackageName()).setBoolean(admin == null).setInt(enabled ? 1 : 0);
            String[] strArr = new String[1];
            strArr[0] = isManagedProfileOwner ? LOG_TAG_PROFILE_OWNER : LOG_TAG_DEVICE_OWNER;
            devicePolicyEventLogger.setStrings(strArr).write();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setNetworkLoggingActiveInternal(final boolean active) {
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda2
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3093xf621312b(active);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setNetworkLoggingActiveInternal$119$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3093xf621312b(boolean active) throws Exception {
        boolean shouldSendNotification = false;
        synchronized (getLockObject()) {
            if (active) {
                if (this.mNetworkLogger == null) {
                    int affectedUserId = getNetworkLoggingAffectedUser();
                    this.mNetworkLogger = new NetworkLogger(this, this.mInjector.getPackageManagerInternal(), affectedUserId == 0 ? -1 : affectedUserId);
                }
                if (!this.mNetworkLogger.startNetworkLogging()) {
                    this.mNetworkLogger = null;
                    Slogf.wtf(LOG_TAG, "Network logging could not be started due to the logging service not being available yet.");
                }
                maybePauseDeviceWideLoggingLocked();
                shouldSendNotification = shouldSendNetworkLoggingNotificationLocked();
            } else {
                NetworkLogger networkLogger = this.mNetworkLogger;
                if (networkLogger != null && !networkLogger.stopNetworkLogging()) {
                    Slogf.wtf(LOG_TAG, "Network logging could not be stopped due to the logging service not being available yet.");
                }
                this.mNetworkLogger = null;
            }
        }
        if (active) {
            if (shouldSendNotification) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda116
                    @Override // java.lang.Runnable
                    public final void run() {
                        DevicePolicyManagerService.this.m3091xea199a6d();
                    }
                });
                return;
            }
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda117
            @Override // java.lang.Runnable
            public final void run() {
                DevicePolicyManagerService.this.m3092xf01d65cc();
            }
        });
    }

    private int getNetworkLoggingAffectedUser() {
        synchronized (getLockObject()) {
            if (this.mOwners.hasDeviceOwner()) {
                return this.mOwners.getDeviceOwnerUserId();
            }
            return ((Integer) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda134
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3003x5d88d165();
                }
            })).intValue();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getNetworkLoggingAffectedUser$120$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Integer m3003x5d88d165() throws Exception {
        return Integer.valueOf(getManagedUserId(0));
    }

    private ActiveAdmin getNetworkLoggingControllingAdminLocked() {
        int affectedUserId = getNetworkLoggingAffectedUser();
        if (affectedUserId < 0) {
            return null;
        }
        return getDeviceOrProfileOwnerAdminLocked(affectedUserId);
    }

    public long forceNetworkLogs() {
        Preconditions.checkCallAuthorization(isAdb(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.FORCE_DEVICE_POLICY_MANAGER_LOGS"), "Caller must be shell or hold FORCE_DEVICE_POLICY_MANAGER_LOGS to call forceNetworkLogs");
        synchronized (getLockObject()) {
            if (!isNetworkLoggingEnabledInternalLocked()) {
                throw new IllegalStateException("logging is not available");
            }
            if (this.mNetworkLogger != null) {
                return ((Long) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda155
                    public final Object getOrThrow() {
                        return DevicePolicyManagerService.this.m2990x9aa5cbde();
                    }
                })).longValue();
            }
            return 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$forceNetworkLogs$121$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Long m2990x9aa5cbde() throws Exception {
        return Long.valueOf(this.mNetworkLogger.forceBatchFinalization());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybePauseDeviceWideLoggingLocked() {
        if (!areAllUsersAffiliatedWithDeviceLocked()) {
            if (this.mOwners.hasDeviceOwner()) {
                Slogf.i(LOG_TAG, "There are unaffiliated users, network logging will be paused if enabled.");
                NetworkLogger networkLogger = this.mNetworkLogger;
                if (networkLogger != null) {
                    networkLogger.pause();
                }
            }
            if (!isOrganizationOwnedDeviceWithManagedProfile()) {
                Slogf.i(LOG_TAG, "Not org-owned managed profile device, security logging will be paused if enabled.");
                this.mSecurityLogMonitor.pause();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeResumeDeviceWideLoggingLocked() {
        final boolean allUsersAffiliated = areAllUsersAffiliatedWithDeviceLocked();
        final boolean orgOwnedProfileDevice = isOrganizationOwnedDeviceWithManagedProfile();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda107
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3048x87e63964(allUsersAffiliated, orgOwnedProfileDevice);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$maybeResumeDeviceWideLoggingLocked$122$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3048x87e63964(boolean allUsersAffiliated, boolean orgOwnedProfileDevice) throws Exception {
        NetworkLogger networkLogger;
        if (allUsersAffiliated || orgOwnedProfileDevice) {
            this.mSecurityLogMonitor.resume();
        }
        if ((allUsersAffiliated || !this.mOwners.hasDeviceOwner()) && (networkLogger = this.mNetworkLogger) != null) {
            networkLogger.resume();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void discardDeviceWideLogsLocked() {
        this.mSecurityLogMonitor.discardLogs();
        NetworkLogger networkLogger = this.mNetworkLogger;
        if (networkLogger != null) {
            networkLogger.discardLogs();
        }
    }

    public boolean isNetworkLoggingEnabled(ComponentName admin, String packageName) {
        boolean isNetworkLoggingEnabledInternalLocked;
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity(admin, packageName);
            Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isDefaultDeviceOwner(caller) || (isProfileOwner(caller) && isManagedProfile(caller.getUserId())))) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-network-logging")) || hasCallingOrSelfPermission("android.permission.MANAGE_USERS"));
            synchronized (getLockObject()) {
                isNetworkLoggingEnabledInternalLocked = isNetworkLoggingEnabledInternalLocked();
            }
            return isNetworkLoggingEnabledInternalLocked;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isNetworkLoggingEnabledInternalLocked() {
        ActiveAdmin activeAdmin = getNetworkLoggingControllingAdminLocked();
        return activeAdmin != null && activeAdmin.isNetworkLoggingEnabled;
    }

    public List<NetworkEvent> retrieveNetworkLogs(ComponentName admin, String packageName, long batchToken) {
        if (this.mHasFeature) {
            CallerIdentity caller = getCallerIdentity(admin, packageName);
            boolean isManagedProfileOwner = isProfileOwner(caller) && isManagedProfile(caller.getUserId());
            Preconditions.checkCallAuthorization((caller.hasAdminComponent() && (isDefaultDeviceOwner(caller) || isManagedProfileOwner)) || (caller.hasPackage() && isCallerDelegate(caller, "delegation-network-logging")));
            if (this.mOwners.hasDeviceOwner()) {
                checkAllUsersAreAffiliatedWithDevice();
            }
            synchronized (getLockObject()) {
                if (this.mNetworkLogger != null && isNetworkLoggingEnabledInternalLocked()) {
                    DevicePolicyEventLogger devicePolicyEventLogger = DevicePolicyEventLogger.createEvent(120).setAdmin(caller.getPackageName()).setBoolean(admin == null);
                    String[] strArr = new String[1];
                    strArr[0] = isManagedProfileOwner ? LOG_TAG_PROFILE_OWNER : LOG_TAG_DEVICE_OWNER;
                    devicePolicyEventLogger.setStrings(strArr).write();
                    long currentTime = System.currentTimeMillis();
                    DevicePolicyData policyData = m3013x8b75536b(caller.getUserId());
                    if (currentTime > policyData.mLastNetworkLogsRetrievalTime) {
                        policyData.mLastNetworkLogsRetrievalTime = currentTime;
                        saveSettingsLocked(caller.getUserId());
                    }
                    return this.mNetworkLogger.retrieveLogs(batchToken);
                }
                return null;
            }
        }
        return null;
    }

    private boolean shouldSendNetworkLoggingNotificationLocked() {
        ensureLocked();
        ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
        if (deviceOwner == null || !deviceOwner.isNetworkLoggingEnabled || deviceOwner.numNetworkLoggingNotifications >= 2) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - deviceOwner.lastNetworkLoggingNotificationTimeMs < MS_PER_DAY) {
            return false;
        }
        deviceOwner.numNetworkLoggingNotifications++;
        if (deviceOwner.numNetworkLoggingNotifications >= 2) {
            deviceOwner.lastNetworkLoggingNotificationTimeMs = 0L;
        } else {
            deviceOwner.lastNetworkLoggingNotificationTimeMs = now;
        }
        saveSettingsLocked(deviceOwner.getUserHandle().getIdentifier());
        return true;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleSendNetworkLoggingNotification */
    public void m3091xea199a6d() {
        PackageManagerInternal pm = this.mInjector.getPackageManagerInternal();
        Intent intent = new Intent("android.app.action.SHOW_DEVICE_MONITORING_DIALOG");
        intent.setPackage(pm.getSystemUiServiceComponent().getPackageName());
        this.mNetworkLoggingNotificationUserId = getCurrentForegroundUserId();
        PendingIntent pendingIntent = PendingIntent.getBroadcastAsUser(this.mContext, 0, intent, 67108864, UserHandle.CURRENT);
        String title = getNetworkLoggingTitle();
        String text = getNetworkLoggingText();
        Notification notification = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17302497).setContentTitle(title).setContentText(text).setTicker(title).setShowWhen(true).setContentIntent(pendingIntent).setStyle(new Notification.BigTextStyle().bigText(text)).build();
        Slogf.i(LOG_TAG, "Sending network logging notification to user %d", Integer.valueOf(this.mNetworkLoggingNotificationUserId));
        this.mInjector.getNotificationManager().notifyAsUser(null, 1002, notification, UserHandle.of(this.mNetworkLoggingNotificationUserId));
    }

    private String getNetworkLoggingTitle() {
        return getUpdatableString("Core.NETWORK_LOGGING_TITLE", 17040851, new Object[0]);
    }

    private String getNetworkLoggingText() {
        return getUpdatableString("Core.NETWORK_LOGGING_MESSAGE", 17040850, new Object[0]);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleCancelNetworkLoggingNotification */
    public void m3092xf01d65cc() {
        int i = this.mNetworkLoggingNotificationUserId;
        if (i == -10000) {
            Slogf.d(LOG_TAG, "Not cancelling network logging notification for USER_NULL");
            return;
        }
        Slogf.i(LOG_TAG, "Cancelling network logging notification for user %d", Integer.valueOf(i));
        this.mInjector.getNotificationManager().cancelAsUser(null, 1002, UserHandle.of(this.mNetworkLoggingNotificationUserId));
        this.mNetworkLoggingNotificationUserId = -10000;
    }

    private String getOwnerPackageNameForUserLocked(int userId) {
        if (this.mOwners.getDeviceOwnerUserId() == userId) {
            return this.mOwners.getDeviceOwnerPackageName();
        }
        return this.mOwners.getProfileOwnerPackage(userId);
    }

    private Intent createCrossUserServiceIntent(Intent rawIntent, String expectedPackageName, int targetUserId) throws RemoteException, SecurityException {
        ResolveInfo info = this.mIPackageManager.resolveService(rawIntent, rawIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 0L, targetUserId);
        if (info == null || info.serviceInfo == null) {
            Slogf.e(LOG_TAG, "Fail to look up the service: %s or user %d is not running", rawIntent, Integer.valueOf(targetUserId));
            return null;
        } else if (!expectedPackageName.equals(info.serviceInfo.packageName)) {
            throw new SecurityException("Only allow to bind service in " + expectedPackageName);
        } else {
            if (info.serviceInfo.exported && !"android.permission.BIND_DEVICE_ADMIN".equals(info.serviceInfo.permission)) {
                throw new SecurityException("Service must be protected by BIND_DEVICE_ADMIN permission");
            }
            rawIntent.setComponent(info.serviceInfo.getComponentName());
            return rawIntent;
        }
    }

    public long getLastSecurityLogRetrievalTime() {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || canManageUsers(caller));
        return m3013x8b75536b(0).mLastSecurityLogRetrievalTime;
    }

    public long getLastBugReportRequestTime() {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || canManageUsers(caller));
        return m3013x8b75536b(0).mLastBugReportRequestTime;
    }

    public long getLastNetworkLogRetrievalTime() {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || (isProfileOwner(caller) && isManagedProfile(caller.getUserId())) || canManageUsers(caller));
        int affectedUserId = getNetworkLoggingAffectedUser();
        if (affectedUserId >= 0) {
            return m3013x8b75536b(affectedUserId).mLastNetworkLogsRetrievalTime;
        }
        return -1L;
    }

    public boolean setResetPasswordToken(ComponentName admin, final byte[] token) {
        boolean booleanValue;
        boolean z = false;
        if (this.mHasFeature && this.mLockPatternUtils.hasSecureLockScreen()) {
            if (token == null || token.length < 32) {
                throw new IllegalArgumentException("token must be at least 32-byte long");
            }
            CallerIdentity caller = getCallerIdentity(admin);
            Preconditions.checkCallAuthorization((isProfileOwner(caller) || isDefaultDeviceOwner(caller)) ? true : true);
            synchronized (getLockObject()) {
                final int userHandle = caller.getUserId();
                final DevicePolicyData policy = m3013x8b75536b(userHandle);
                booleanValue = ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda99
                    public final Object getOrThrow() {
                        return DevicePolicyManagerService.this.m3104xfd108c57(policy, userHandle, token);
                    }
                })).booleanValue();
            }
            return booleanValue;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setResetPasswordToken$123$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3104xfd108c57(DevicePolicyData policy, int userHandle, byte[] token) throws Exception {
        if (policy.mPasswordTokenHandle != 0) {
            this.mLockPatternUtils.removeEscrowToken(policy.mPasswordTokenHandle, userHandle);
        }
        policy.mPasswordTokenHandle = this.mLockPatternUtils.addEscrowToken(token, userHandle, (LockPatternUtils.EscrowTokenStateChangeCallback) null);
        saveSettingsLocked(userHandle);
        return Boolean.valueOf(policy.mPasswordTokenHandle != 0);
    }

    public boolean clearResetPasswordToken(ComponentName admin) {
        if (this.mHasFeature && this.mLockPatternUtils.hasSecureLockScreen()) {
            CallerIdentity caller = getCallerIdentity(admin);
            Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
            synchronized (getLockObject()) {
                final int userHandle = caller.getUserId();
                final DevicePolicyData policy = m3013x8b75536b(userHandle);
                if (policy.mPasswordTokenHandle != 0) {
                    return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda148
                        public final Object getOrThrow() {
                            return DevicePolicyManagerService.this.m2986xb829adeb(policy, userHandle);
                        }
                    })).booleanValue();
                }
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearResetPasswordToken$124$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m2986xb829adeb(DevicePolicyData policy, int userHandle) throws Exception {
        boolean result = this.mLockPatternUtils.removeEscrowToken(policy.mPasswordTokenHandle, userHandle);
        policy.mPasswordTokenHandle = 0L;
        saveSettingsLocked(userHandle);
        return Boolean.valueOf(result);
    }

    public boolean isResetPasswordTokenActive(ComponentName admin) {
        boolean isResetPasswordTokenActiveForUserLocked;
        boolean z = false;
        if (this.mHasFeature && this.mLockPatternUtils.hasSecureLockScreen()) {
            CallerIdentity caller = getCallerIdentity(admin);
            Preconditions.checkCallAuthorization((isProfileOwner(caller) || isDefaultDeviceOwner(caller)) ? true : true);
            synchronized (getLockObject()) {
                isResetPasswordTokenActiveForUserLocked = isResetPasswordTokenActiveForUserLocked(caller.getUserId());
            }
            return isResetPasswordTokenActiveForUserLocked;
        }
        return false;
    }

    private boolean isResetPasswordTokenActiveForUserLocked(final int userHandle) {
        final DevicePolicyData policy = m3013x8b75536b(userHandle);
        if (policy.mPasswordTokenHandle != 0) {
            return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda118
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3038x3ce366c7(policy, userHandle);
                }
            })).booleanValue();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isResetPasswordTokenActiveForUserLocked$125$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3038x3ce366c7(DevicePolicyData policy, int userHandle) throws Exception {
        return Boolean.valueOf(this.mLockPatternUtils.isEscrowTokenActive(policy.mPasswordTokenHandle, userHandle));
    }

    public boolean resetPasswordWithToken(ComponentName admin, String passwordOrNull, byte[] token, int flags) {
        if (this.mHasFeature && this.mLockPatternUtils.hasSecureLockScreen()) {
            Objects.requireNonNull(token);
            CallerIdentity caller = getCallerIdentity(admin);
            Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
            synchronized (getLockObject()) {
                DevicePolicyData policy = m3013x8b75536b(caller.getUserId());
                if (policy.mPasswordTokenHandle != 0) {
                    String password = passwordOrNull != null ? passwordOrNull : "";
                    boolean result = resetPasswordInternal(password, policy.mPasswordTokenHandle, token, flags, caller);
                    if (result) {
                        DevicePolicyEventLogger.createEvent(206).setAdmin(caller.getComponentName()).write();
                    }
                    return result;
                }
                Slogf.w(LOG_TAG, "No saved token handle");
                return false;
            }
        }
        return false;
    }

    public boolean isCurrentInputMethodSetByOwner() {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller) || isSystemUid(caller), "Only profile owner, device owner and system may call this method.");
        return m3013x8b75536b(caller.getUserId()).mCurrentInputMethodSet;
    }

    public StringParceledListSlice getOwnerInstalledCaCerts(UserHandle user) {
        StringParceledListSlice stringParceledListSlice;
        int userId = user.getIdentifier();
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(userId == caller.getUserId() || isProfileOwner(caller) || isDefaultDeviceOwner(caller) || hasFullCrossUsersPermission(caller, userId));
        synchronized (getLockObject()) {
            stringParceledListSlice = new StringParceledListSlice(new ArrayList(m3013x8b75536b(userId).mOwnerInstalledCaCerts));
        }
        return stringParceledListSlice;
    }

    public void clearApplicationUserData(ComponentName admin, String packageName, IPackageDataObserver callback) {
        Objects.requireNonNull(admin, "ComponentName is null");
        Objects.requireNonNull(packageName, "packageName is null");
        Objects.requireNonNull(callback, "callback is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller));
        checkCanExecuteOrThrowUnsafe(23);
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                ActivityManager.getService().clearApplicationUserData(packageName, false, callback, caller.getUserId());
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        } catch (RemoteException e) {
        } catch (SecurityException se) {
            Slogf.w(LOG_TAG, "Not allowed to clear application user data for package " + packageName, se);
            try {
                callback.onRemoveCompleted(packageName, false);
            } catch (RemoteException e2) {
            }
        }
    }

    public void setLogoutEnabled(ComponentName admin, boolean enabled) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(34);
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            if (deviceOwner.isLogoutEnabled == enabled) {
                return;
            }
            deviceOwner.isLogoutEnabled = enabled;
            saveSettingsLocked(caller.getUserId());
        }
    }

    public boolean isLogoutEnabled() {
        boolean z = false;
        if (this.mHasFeature) {
            synchronized (getLockObject()) {
                ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
                if (deviceOwner != null && deviceOwner.isLogoutEnabled) {
                    z = true;
                }
            }
            return z;
        }
        return false;
    }

    public List<String> getDisallowedSystemApps(ComponentName admin, int userId, String provisioningAction) throws RemoteException {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        return new ArrayList(this.mOverlayPackagesProvider.getNonRequiredApps(admin, userId, provisioningAction));
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:40:0x00d9
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [16277=6] */
    public void transferOwnership(android.content.ComponentName r21, android.content.ComponentName r22, android.os.PersistableBundle r23) {
        /*
            r20 = this;
            r7 = r20
            r8 = r21
            r9 = r22
            r1 = r23
            boolean r0 = r7.mHasFeature
            if (r0 != 0) goto Ld
            return
        Ld:
            java.lang.String r0 = "ComponentName is null"
            java.util.Objects.requireNonNull(r8, r0)
            java.lang.String r0 = "Target cannot be null."
            java.util.Objects.requireNonNull(r9, r0)
            boolean r0 = r21.equals(r22)
            r10 = 1
            r0 = r0 ^ r10
            java.lang.String r2 = "Provided administrator and target are the same object."
            com.android.internal.util.Preconditions.checkArgument(r0, r2)
            java.lang.String r0 = r21.getPackageName()
            java.lang.String r2 = r22.getPackageName()
            boolean r0 = r0.equals(r2)
            r0 = r0 ^ r10
            java.lang.String r2 = "Provided administrator and target have the same package name."
            com.android.internal.util.Preconditions.checkArgument(r0, r2)
            if (r1 == 0) goto L3b
            java.lang.String r0 = "bundle"
            enforceMaxStringLength(r1, r0)
        L3b:
            com.android.server.devicepolicy.CallerIdentity r11 = r20.getCallerIdentity(r21)
            boolean r0 = r7.isDefaultDeviceOwner(r11)
            r12 = 0
            if (r0 != 0) goto L50
            boolean r0 = r7.isProfileOwner(r11)
            if (r0 == 0) goto L4e
            goto L50
        L4e:
            r0 = r12
            goto L51
        L50:
            r0 = r10
        L51:
            com.android.internal.util.Preconditions.checkCallAuthorization(r0)
            int r13 = r11.getUserId()
            com.android.server.devicepolicy.DevicePolicyData r14 = r7.m3013x8b75536b(r13)
            android.app.admin.DeviceAdminInfo r15 = r7.findAdmin(r9, r13, r10)
            r7.checkActiveAdminPrecondition(r9, r15, r14)
            boolean r0 = r15.supportsTransferOwnership()
            java.lang.String r2 = "Provided target does not support ownership transfer."
            com.android.internal.util.Preconditions.checkArgument(r0, r2)
            com.android.server.devicepolicy.DevicePolicyManagerService$Injector r0 = r7.mInjector
            long r5 = r0.binderClearCallingIdentity()
            r2 = 0
            java.lang.Object r16 = r20.getLockObject()     // Catch: java.lang.Throwable -> L16a
            monitor-enter(r16)     // Catch: java.lang.Throwable -> L16a
            if (r1 != 0) goto L86
            android.os.PersistableBundle r0 = new android.os.PersistableBundle     // Catch: java.lang.Throwable -> L81
            r0.<init>()     // Catch: java.lang.Throwable -> L81
            r3 = r0
            goto L87
        L81:
            r0 = move-exception
            r10 = r1
            r3 = r5
            goto L163
        L86:
            r3 = r1
        L87:
            boolean r0 = r7.isProfileOwner(r11)     // Catch: java.lang.Throwable -> L160
            if (r0 == 0) goto Led
            java.lang.String r0 = "profile-owner"
            r17 = r0
            java.lang.String r0 = "profile-owner"
            r1 = r20
            r2 = r21
            r23 = r3
            r3 = r22
            r4 = r23
            r18 = r5
            r5 = r13
            r6 = r0
            r1.prepareTransfer(r2, r3, r4, r5, r6)     // Catch: java.lang.Throwable -> Lcf
            r7.transferProfileOwnershipLocked(r8, r9, r13)     // Catch: java.lang.Throwable -> Lcf
            java.lang.String r0 = "android.app.action.TRANSFER_OWNERSHIP_COMPLETE"
            r6 = r23
            android.os.Bundle r1 = r7.getTransferOwnershipAdminExtras(r6)     // Catch: java.lang.Throwable -> Lc7
            r7.sendProfileOwnerCommand(r0, r1, r13)     // Catch: java.lang.Throwable -> Lc7
            java.lang.String r0 = "android.app.action.PROFILE_OWNER_CHANGED"
            r7.postTransfer(r0, r13)     // Catch: java.lang.Throwable -> Lc7
            boolean r0 = r7.isUserAffiliatedWithDeviceLocked(r13)     // Catch: java.lang.Throwable -> Lc7
            if (r0 == 0) goto Lc2
            r7.notifyAffiliatedProfileTransferOwnershipComplete(r13)     // Catch: java.lang.Throwable -> Lc7
        Lc2:
            r10 = r6
            r2 = r17
            goto L130
        Lc7:
            r0 = move-exception
            r10 = r6
            r2 = r17
            r3 = r18
            goto L163
        Lcf:
            r0 = move-exception
            r6 = r23
            r10 = r6
            r2 = r17
            r3 = r18
            goto L163
        Ld9:
            r0 = move-exception
            r18 = r5
            r6 = r3
            r10 = r6
            r2 = r17
            r3 = r18
            goto L163
        Le4:
            r0 = move-exception
            r18 = r5
            r6 = r3
            r10 = r6
            r3 = r18
            goto L163
        Led:
            r18 = r5
            r6 = r3
            boolean r0 = r7.isDefaultDeviceOwner(r11)     // Catch: java.lang.Throwable -> L15b
            if (r0 == 0) goto L12f
            java.lang.String r0 = "device-owner"
            r17 = r0
            java.lang.String r0 = "device-owner"
            r1 = r20
            r2 = r21
            r3 = r22
            r4 = r6
            r5 = r13
            r10 = r6
            r6 = r0
            r1.prepareTransfer(r2, r3, r4, r5, r6)     // Catch: java.lang.Throwable -> L11d
            r7.transferDeviceOwnershipLocked(r8, r9, r13)     // Catch: java.lang.Throwable -> L11d
            java.lang.String r0 = "android.app.action.TRANSFER_OWNERSHIP_COMPLETE"
            android.os.Bundle r1 = r7.getTransferOwnershipAdminExtras(r10)     // Catch: java.lang.Throwable -> L11d
            r7.sendDeviceOwnerCommand(r0, r1)     // Catch: java.lang.Throwable -> L11d
            java.lang.String r0 = "android.app.action.DEVICE_OWNER_CHANGED"
            r7.postTransfer(r0, r13)     // Catch: java.lang.Throwable -> L11d
            r2 = r17
            goto L130
        L11d:
            r0 = move-exception
            r2 = r17
            r3 = r18
            goto L163
        L123:
            r0 = move-exception
            r10 = r6
            r2 = r17
            r3 = r18
            goto L163
        L12a:
            r0 = move-exception
            r10 = r6
            r3 = r18
            goto L163
        L12f:
            r10 = r6
        L130:
            monitor-exit(r16)     // Catch: java.lang.Throwable -> L157
            com.android.server.devicepolicy.DevicePolicyManagerService$Injector r0 = r7.mInjector
            r3 = r18
            r0.binderRestoreCallingIdentity(r3)
            r0 = 58
            android.app.admin.DevicePolicyEventLogger r0 = android.app.admin.DevicePolicyEventLogger.createEvent(r0)
            android.app.admin.DevicePolicyEventLogger r0 = r0.setAdmin(r8)
            r1 = 2
            java.lang.String[] r1 = new java.lang.String[r1]
            java.lang.String r5 = r22.getPackageName()
            r1[r12] = r5
            r5 = 1
            r1[r5] = r2
            android.app.admin.DevicePolicyEventLogger r0 = r0.setStrings(r1)
            r0.write()
            return
        L157:
            r0 = move-exception
            r3 = r18
            goto L163
        L15b:
            r0 = move-exception
            r10 = r6
            r3 = r18
            goto L163
        L160:
            r0 = move-exception
            r10 = r3
            r3 = r5
        L163:
            monitor-exit(r16)     // Catch: java.lang.Throwable -> L168
            throw r0     // Catch: java.lang.Throwable -> L165
        L165:
            r0 = move-exception
            r1 = r10
            goto L16c
        L168:
            r0 = move-exception
            goto L163
        L16a:
            r0 = move-exception
            r3 = r5
        L16c:
            com.android.server.devicepolicy.DevicePolicyManagerService$Injector r5 = r7.mInjector
            r5.binderRestoreCallingIdentity(r3)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.DevicePolicyManagerService.transferOwnership(android.content.ComponentName, android.content.ComponentName, android.os.PersistableBundle):void");
    }

    private void prepareTransfer(ComponentName admin, ComponentName target, PersistableBundle bundle, int callingUserId, String adminType) {
        saveTransferOwnershipBundleLocked(bundle, callingUserId);
        this.mTransferOwnershipMetadataManager.saveMetadataFile(new TransferOwnershipMetadataManager.Metadata(admin, target, callingUserId, adminType));
    }

    private void postTransfer(String broadcast, int callingUserId) {
        deleteTransferOwnershipMetadataFileLocked();
        sendOwnerChangedBroadcast(broadcast, callingUserId);
    }

    private void notifyAffiliatedProfileTransferOwnershipComplete(int callingUserId) {
        Bundle extras = new Bundle();
        extras.putParcelable("android.intent.extra.USER", UserHandle.of(callingUserId));
        sendDeviceOwnerCommand("android.app.action.AFFILIATED_PROFILE_TRANSFER_OWNERSHIP_COMPLETE", extras);
    }

    private void transferProfileOwnershipLocked(ComponentName admin, ComponentName target, int profileOwnerUserId) {
        transferActiveAdminUncheckedLocked(target, admin, profileOwnerUserId);
        this.mOwners.transferProfileOwner(target, profileOwnerUserId);
        Slogf.i(LOG_TAG, "Profile owner set: " + target + " on user " + profileOwnerUserId);
        this.mOwners.writeProfileOwner(profileOwnerUserId);
        this.mDeviceAdminServiceController.startServiceForOwner(target.getPackageName(), profileOwnerUserId, "transfer-profile-owner");
    }

    private void transferDeviceOwnershipLocked(ComponentName admin, ComponentName target, int userId) {
        transferActiveAdminUncheckedLocked(target, admin, userId);
        this.mOwners.transferDeviceOwnership(target);
        Slogf.i(LOG_TAG, "Device owner set: " + target + " on user " + userId);
        this.mOwners.writeDeviceOwner();
        this.mDeviceAdminServiceController.startServiceForOwner(target.getPackageName(), userId, "transfer-device-owner");
    }

    private Bundle getTransferOwnershipAdminExtras(PersistableBundle bundle) {
        Bundle extras = new Bundle();
        if (bundle != null) {
            extras.putParcelable("android.app.extra.TRANSFER_OWNERSHIP_ADMIN_EXTRAS_BUNDLE", bundle);
        }
        return extras;
    }

    public void setStartUserSessionMessage(ComponentName admin, CharSequence startUserSessionMessage) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        String startUserSessionMessageString = startUserSessionMessage != null ? startUserSessionMessage.toString() : null;
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            if (TextUtils.equals(deviceOwner.startUserSessionMessage, startUserSessionMessage)) {
                return;
            }
            deviceOwner.startUserSessionMessage = startUserSessionMessageString;
            saveSettingsLocked(caller.getUserId());
            this.mInjector.getActivityManagerInternal().setSwitchingFromSystemUserMessage(startUserSessionMessageString);
        }
    }

    public void setEndUserSessionMessage(ComponentName admin, CharSequence endUserSessionMessage) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        String endUserSessionMessageString = endUserSessionMessage != null ? endUserSessionMessage.toString() : null;
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            if (TextUtils.equals(deviceOwner.endUserSessionMessage, endUserSessionMessage)) {
                return;
            }
            deviceOwner.endUserSessionMessage = endUserSessionMessageString;
            saveSettingsLocked(caller.getUserId());
            this.mInjector.getActivityManagerInternal().setSwitchingToSystemUserMessage(endUserSessionMessageString);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    public String getStartUserSessionMessage(ComponentName admin) {
        String str;
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            str = deviceOwner.startUserSessionMessage;
        }
        return str;
    }

    /* JADX DEBUG: Method merged with bridge method */
    public String getEndUserSessionMessage(ComponentName admin) {
        String str;
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            str = deviceOwner.endUserSessionMessage;
        }
        return str;
    }

    private void deleteTransferOwnershipMetadataFileLocked() {
        this.mTransferOwnershipMetadataManager.deleteMetadataFile();
    }

    public PersistableBundle getTransferOwnershipBundle() {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isProfileOwner(caller) || isDefaultDeviceOwner(caller));
        synchronized (getLockObject()) {
            int callingUserId = caller.getUserId();
            File bundleFile = new File(this.mPathProvider.getUserSystemDirectory(callingUserId), TRANSFER_OWNERSHIP_PARAMETERS_XML);
            if (bundleFile.exists()) {
                try {
                    FileInputStream stream = new FileInputStream(bundleFile);
                    try {
                        TypedXmlPullParser parser = Xml.resolvePullParser(stream);
                        parser.next();
                        PersistableBundle restoreFromXml = PersistableBundle.restoreFromXml(parser);
                        stream.close();
                        return restoreFromXml;
                    } catch (Throwable th) {
                        try {
                            stream.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                } catch (IOException | IllegalArgumentException | XmlPullParserException e) {
                    Slogf.e(LOG_TAG, "Caught exception while trying to load the owner transfer parameters from file " + bundleFile, e);
                    return null;
                }
            }
            return null;
        }
    }

    public int addOverrideApn(ComponentName who, final ApnSetting apnSetting) {
        if (this.mHasFeature && this.mHasTelephonyFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            Objects.requireNonNull(apnSetting, "ApnSetting is null in addOverrideApn");
            CallerIdentity caller = getCallerIdentity(who);
            if (apnSetting.getApnTypeBitmask() == 16384) {
                Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isManagedProfileOwner(caller));
            } else {
                Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
            }
            final TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
            if (tm != null) {
                return ((Integer) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda60
                    public final Object getOrThrow() {
                        return DevicePolicyManagerService.this.m2978x10a4ef39(tm, apnSetting);
                    }
                })).intValue();
            }
            Slogf.w(LOG_TAG, "TelephonyManager is null when trying to add override apn");
            return -1;
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addOverrideApn$126$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Integer m2978x10a4ef39(TelephonyManager tm, ApnSetting apnSetting) throws Exception {
        return Integer.valueOf(tm.addDevicePolicyOverrideApn(this.mContext, apnSetting));
    }

    public boolean updateOverrideApn(ComponentName who, final int apnId, final ApnSetting apnSetting) {
        if (this.mHasFeature && this.mHasTelephonyFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            Objects.requireNonNull(apnSetting, "ApnSetting is null in updateOverrideApn");
            CallerIdentity caller = getCallerIdentity(who);
            ApnSetting apn = getApnSetting(apnId);
            if (apn != null && apn.getApnTypeBitmask() == 16384 && apnSetting.getApnTypeBitmask() == 16384) {
                Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isManagedProfileOwner(caller));
            } else {
                Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
            }
            if (apnId < 0) {
                return false;
            }
            final TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
            if (tm != null) {
                return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda65
                    public final Object getOrThrow() {
                        return DevicePolicyManagerService.this.m3120xc0335e1a(tm, apnId, apnSetting);
                    }
                })).booleanValue();
            }
            Slogf.w(LOG_TAG, "TelephonyManager is null when trying to modify override apn");
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateOverrideApn$127$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3120xc0335e1a(TelephonyManager tm, int apnId, ApnSetting apnSetting) throws Exception {
        return Boolean.valueOf(tm.modifyDevicePolicyOverrideApn(this.mContext, apnId, apnSetting));
    }

    public boolean removeOverrideApn(ComponentName who, int apnId) {
        boolean z = false;
        if (this.mHasFeature && this.mHasTelephonyFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            ApnSetting apn = getApnSetting(apnId);
            if (apn != null && apn.getApnTypeBitmask() == 16384) {
                Preconditions.checkCallAuthorization((isDefaultDeviceOwner(caller) || isManagedProfileOwner(caller)) ? true : true);
            } else {
                Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
            }
            return removeOverrideApnUnchecked(apnId);
        }
        return false;
    }

    private boolean removeOverrideApnUnchecked(final int apnId) {
        if (apnId < 0) {
            return false;
        }
        int numDeleted = ((Integer) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda77
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3058x7748b0a(apnId);
            }
        })).intValue();
        return numDeleted > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeOverrideApnUnchecked$128$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Integer m3058x7748b0a(int apnId) throws Exception {
        return Integer.valueOf(this.mContext.getContentResolver().delete(Uri.withAppendedPath(Telephony.Carriers.DPC_URI, Integer.toString(apnId)), null, null));
    }

    private ApnSetting getApnSetting(final int apnId) {
        if (apnId < 0) {
            return null;
        }
        ApnSetting apnSetting = null;
        Cursor cursor = (Cursor) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda21
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m2996xb13fe6bb(apnId);
            }
        });
        if (cursor != null) {
            while (cursor.moveToNext() && (apnSetting = ApnSetting.makeApnSetting(cursor)) == null) {
            }
            cursor.close();
        }
        return apnSetting;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getApnSetting$129$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Cursor m2996xb13fe6bb(int apnId) throws Exception {
        return this.mContext.getContentResolver().query(Uri.withAppendedPath(Telephony.Carriers.DPC_URI, Integer.toString(apnId)), null, null, null, "name ASC");
    }

    public List<ApnSetting> getOverrideApns(ComponentName who) {
        if (!this.mHasFeature || !this.mHasTelephonyFeature) {
            return Collections.emptyList();
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isManagedProfileOwner(caller));
        List<ApnSetting> apnSettings = getOverrideApnsUnchecked();
        if (isProfileOwner(caller)) {
            List<ApnSetting> apnSettingList = new ArrayList<>();
            for (ApnSetting apnSetting : apnSettings) {
                if (apnSetting.getApnTypeBitmask() == 16384) {
                    apnSettingList.add(apnSetting);
                }
            }
            return apnSettingList;
        }
        return apnSettings;
    }

    private List<ApnSetting> getOverrideApnsUnchecked() {
        final TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        if (tm != null) {
            return (List) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda130
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3004x2f7f8d5a(tm);
                }
            });
        }
        Slogf.w(LOG_TAG, "TelephonyManager is null when trying to get override apns");
        return Collections.emptyList();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getOverrideApnsUnchecked$130$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ List m3004x2f7f8d5a(TelephonyManager tm) throws Exception {
        return tm.getDevicePolicyOverrideApns(this.mContext);
    }

    public void setOverrideApnsEnabled(ComponentName who, boolean enabled) {
        if (!this.mHasFeature || !this.mHasTelephonyFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(36);
        setOverrideApnsEnabledUnchecked(enabled);
    }

    private void setOverrideApnsEnabledUnchecked(boolean enabled) {
        final ContentValues value = new ContentValues();
        value.put("enforced", Boolean.valueOf(enabled));
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda14
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3095xc876febc(value);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setOverrideApnsEnabledUnchecked$131$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Integer m3095xc876febc(ContentValues value) throws Exception {
        return Integer.valueOf(this.mContext.getContentResolver().update(Telephony.Carriers.ENFORCE_MANAGED_URI, value, null, null));
    }

    public boolean isOverrideApnEnabled(ComponentName who) {
        if (this.mHasFeature && this.mHasTelephonyFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
            Cursor enforceCursor = (Cursor) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda98
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3032xc08512b8();
                }
            });
            try {
                if (enforceCursor == null) {
                    return false;
                }
                try {
                    if (enforceCursor.moveToFirst()) {
                        return enforceCursor.getInt(enforceCursor.getColumnIndex("enforced")) == 1;
                    }
                } catch (IllegalArgumentException e) {
                    Slogf.e(LOG_TAG, "Cursor returned from ENFORCE_MANAGED_URI doesn't contain correct info.", e);
                }
                return false;
            } finally {
                enforceCursor.close();
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isOverrideApnEnabled$132$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Cursor m3032xc08512b8() throws Exception {
        return this.mContext.getContentResolver().query(Telephony.Carriers.ENFORCE_MANAGED_URI, null, null, null, null);
    }

    void saveTransferOwnershipBundleLocked(PersistableBundle bundle, int userId) {
        File parametersFile = new File(this.mPathProvider.getUserSystemDirectory(userId), TRANSFER_OWNERSHIP_PARAMETERS_XML);
        AtomicFile atomicFile = new AtomicFile(parametersFile);
        FileOutputStream stream = null;
        try {
            stream = atomicFile.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(stream);
            serializer.startDocument((String) null, true);
            serializer.startTag((String) null, TAG_TRANSFER_OWNERSHIP_BUNDLE);
            bundle.saveToXml(serializer);
            serializer.endTag((String) null, TAG_TRANSFER_OWNERSHIP_BUNDLE);
            serializer.endDocument();
            atomicFile.finishWrite(stream);
        } catch (IOException | XmlPullParserException e) {
            Slogf.e(LOG_TAG, "Caught exception while trying to save the owner transfer parameters to file " + parametersFile, e);
            parametersFile.delete();
            atomicFile.failWrite(stream);
        }
    }

    void deleteTransferOwnershipBundleLocked(int userId) {
        File parametersFile = new File(this.mPathProvider.getUserSystemDirectory(userId), TRANSFER_OWNERSHIP_PARAMETERS_XML);
        parametersFile.delete();
    }

    private void logPasswordQualitySetIfSecurityLogEnabled(ComponentName who, int userId, boolean parent, PasswordPolicy passwordPolicy) {
        if (SecurityLog.isLoggingEnabled()) {
            int affectedUserId = parent ? getProfileParentId(userId) : userId;
            SecurityLog.writeEvent(210017, new Object[]{who.getPackageName(), Integer.valueOf(userId), Integer.valueOf(affectedUserId), Integer.valueOf(passwordPolicy.length), Integer.valueOf(passwordPolicy.quality), Integer.valueOf(passwordPolicy.letters), Integer.valueOf(passwordPolicy.nonLetter), Integer.valueOf(passwordPolicy.numeric), Integer.valueOf(passwordPolicy.upperCase), Integer.valueOf(passwordPolicy.lowerCase), Integer.valueOf(passwordPolicy.symbols)});
        }
    }

    private static String getManagedProvisioningPackage(Context context) {
        return context.getResources().getString(17039995);
    }

    private void putPrivateDnsSettings(final int mode, final String host) {
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda9
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3053x92c60eb1(mode, host);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$putPrivateDnsSettings$133$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3053x92c60eb1(int mode, String host) throws Exception {
        ConnectivitySettingsManager.setPrivateDnsMode(this.mContext, mode);
        ConnectivitySettingsManager.setPrivateDnsHostname(this.mContext, host);
    }

    public int setGlobalPrivateDns(ComponentName who, int mode, String privateDnsHost) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
            checkAllUsersAreAffiliatedWithDevice();
            checkCanExecuteOrThrowUnsafe(33);
            switch (mode) {
                case 2:
                    if (!TextUtils.isEmpty(privateDnsHost)) {
                        throw new IllegalArgumentException("Host provided for opportunistic mode, but is not needed.");
                    }
                    putPrivateDnsSettings(2, null);
                    return 0;
                case 3:
                    if (TextUtils.isEmpty(privateDnsHost) || !NetworkUtilsInternal.isWeaklyValidatedHostname(privateDnsHost)) {
                        throw new IllegalArgumentException(String.format("Provided hostname %s is not valid", privateDnsHost));
                    }
                    putPrivateDnsSettings(3, privateDnsHost);
                    return 0;
                default:
                    throw new IllegalArgumentException(String.format("Provided mode, %d, is not a valid mode.", Integer.valueOf(mode)));
            }
        }
        return 2;
    }

    public int getGlobalPrivateDnsMode(ComponentName who) {
        if (this.mHasFeature) {
            Objects.requireNonNull(who, "ComponentName is null");
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
            int currentMode = ConnectivitySettingsManager.getPrivateDnsMode(this.mContext);
            switch (currentMode) {
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                default:
                    return 0;
            }
        }
        return 0;
    }

    public String getGlobalPrivateDnsHost(ComponentName who) {
        if (!this.mHasFeature) {
            return null;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller));
        return this.mInjector.settingsGlobalGetString("private_dns_specifier");
    }

    public void installUpdateFromFile(ComponentName admin, final ParcelFileDescriptor updateFileDescriptor, final StartInstallingUpdateCallback callback) {
        Objects.requireNonNull(admin, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(admin);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller));
        checkCanExecuteOrThrowUnsafe(26);
        DevicePolicyEventLogger.createEvent(73).setAdmin(caller.getComponentName()).setBoolean(isDeviceAB()).write();
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda50
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3020xe9f3497e(updateFileDescriptor, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$installUpdateFromFile$134$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3020xe9f3497e(ParcelFileDescriptor updateFileDescriptor, StartInstallingUpdateCallback callback) throws Exception {
        UpdateInstaller updateInstaller;
        if (isDeviceAB()) {
            updateInstaller = new AbUpdateInstaller(this.mContext, updateFileDescriptor, callback, this.mInjector, this.mConstants);
        } else {
            updateInstaller = new NonAbUpdateInstaller(this.mContext, updateFileDescriptor, callback, this.mInjector, this.mConstants);
        }
        updateInstaller.startInstallUpdate();
    }

    private boolean isDeviceAB() {
        return "true".equalsIgnoreCase(SystemProperties.get(AB_DEVICE_KEY, ""));
    }

    public void setCrossProfileCalendarPackages(ComponentName who, List<String> packageNames) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            admin.mCrossProfileCalendarPackages = packageNames;
            saveSettingsLocked(caller.getUserId());
        }
        DevicePolicyEventLogger.createEvent(70).setAdmin(who).setStrings(packageNames == null ? null : (String[]) packageNames.toArray(new String[packageNames.size()])).write();
    }

    public List<String> getCrossProfileCalendarPackages(ComponentName who) {
        List<String> list;
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            list = admin.mCrossProfileCalendarPackages;
        }
        return list;
    }

    public boolean isPackageAllowedToAccessCalendarForUser(final String packageName, final int userHandle) {
        if (this.mHasFeature) {
            Preconditions.checkStringNotEmpty(packageName, "Package name is null or empty");
            Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
            CallerIdentity caller = getCallerIdentity();
            int packageUid = ((Integer) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda146
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3033xabb2ccbc(packageName, userHandle);
                }
            })).intValue();
            if (caller.getUid() != packageUid) {
                Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") || hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL"));
            }
            synchronized (getLockObject()) {
                if (this.mInjector.settingsSecureGetIntForUser("cross_profile_calendar_enabled", 0, userHandle) == 0) {
                    return false;
                }
                ActiveAdmin admin = getProfileOwnerAdminLocked(userHandle);
                if (admin != null) {
                    if (admin.mCrossProfileCalendarPackages == null) {
                        return true;
                    }
                    return admin.mCrossProfileCalendarPackages.contains(packageName);
                }
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isPackageAllowedToAccessCalendarForUser$135$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Integer m3033xabb2ccbc(String packageName, int userHandle) throws Exception {
        try {
            return Integer.valueOf(this.mInjector.getPackageManager().getPackageUidAsUser(packageName, userHandle));
        } catch (PackageManager.NameNotFoundException e) {
            Slogf.w(LOG_TAG, e, "Couldn't find package %s in user %d", packageName, Integer.valueOf(userHandle));
            return -1;
        }
    }

    public List<String> getCrossProfileCalendarPackagesForUser(int userHandle) {
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        Preconditions.checkArgumentNonnegative(userHandle, "Invalid userId");
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") || hasCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL"));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userHandle);
            if (admin != null) {
                return admin.mCrossProfileCalendarPackages;
            }
            return Collections.emptyList();
        }
    }

    public void setCrossProfilePackages(ComponentName who, final List<String> packageNames) {
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(who, "ComponentName is null");
        Objects.requireNonNull(packageNames, "Package names is null");
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            final List<String> previousCrossProfilePackages = admin.mCrossProfilePackages;
            if (packageNames.equals(previousCrossProfilePackages)) {
                return;
            }
            admin.mCrossProfilePackages = packageNames;
            saveSettingsLocked(caller.getUserId());
            logSetCrossProfilePackages(who, packageNames);
            final CrossProfileApps crossProfileApps = (CrossProfileApps) this.mContext.getSystemService(CrossProfileApps.class);
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda52
                public final void runOrThrow() {
                    crossProfileApps.resetInteractAcrossProfilesAppOps(previousCrossProfilePackages, new HashSet(packageNames));
                }
            });
        }
    }

    private void logSetCrossProfilePackages(ComponentName who, List<String> packageNames) {
        DevicePolicyEventLogger.createEvent(138).setAdmin(who).setStrings((String[]) packageNames.toArray(new String[packageNames.size()])).write();
    }

    public List<String> getCrossProfilePackages(ComponentName who) {
        List<String> list;
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            list = admin.mCrossProfilePackages;
        }
        return list;
    }

    public List<String> getAllCrossProfilePackages() {
        List<String> packages;
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isSystemUid(caller) || isRootUid(caller) || hasCallingPermission("android.permission.INTERACT_ACROSS_USERS") || hasCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") || hasPermissionForPreflight(caller, "android.permission.INTERACT_ACROSS_PROFILES"));
        synchronized (getLockObject()) {
            List<ActiveAdmin> admins = getProfileOwnerAdminsForCurrentProfileGroup();
            packages = getCrossProfilePackagesForAdmins(admins);
            packages.addAll(getDefaultCrossProfilePackages());
        }
        return packages;
    }

    private List<String> getCrossProfilePackagesForAdmins(List<ActiveAdmin> admins) {
        List<String> packages = new ArrayList<>();
        for (int i = 0; i < admins.size(); i++) {
            packages.addAll(admins.get(i).mCrossProfilePackages);
        }
        return packages;
    }

    public List<String> getDefaultCrossProfilePackages() {
        Set<String> crossProfilePackages = new HashSet<>();
        Collections.addAll(crossProfilePackages, this.mContext.getResources().getStringArray(17236156));
        Collections.addAll(crossProfilePackages, this.mContext.getResources().getStringArray(17236199));
        String defaultComponent = Settings.Secure.getString(this.mContext.getContentResolver(), "default_input_method");
        if (defaultComponent != null && !"com.google.android.inputmethod.latin".equals(ComponentName.unflattenFromString(defaultComponent).getPackageName())) {
            crossProfilePackages.remove("com.google.android.inputmethod.latin");
        }
        return new ArrayList(crossProfilePackages);
    }

    private List<ActiveAdmin> getProfileOwnerAdminsForCurrentProfileGroup() {
        List<ActiveAdmin> admins;
        ActiveAdmin admin;
        synchronized (getLockObject()) {
            admins = new ArrayList<>();
            int[] users = this.mUserManager.getProfileIdsWithDisabled(this.mInjector.userHandleGetCallingUserId());
            for (int i = 0; i < users.length; i++) {
                ComponentName componentName = m3036x91e56c0c(users[i]);
                if (componentName != null && (admin = getActiveAdminUncheckedLocked(componentName, users[i])) != null) {
                    admins.add(admin);
                }
            }
        }
        return admins;
    }

    public boolean isManagedKiosk() {
        boolean z = false;
        if (this.mHasFeature) {
            Preconditions.checkCallAuthorization((canManageUsers(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS")) ? true : true);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    return isManagedKioskInternal();
                } catch (RemoteException e) {
                    throw new IllegalStateException(e);
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        return false;
    }

    private boolean isUnattendedManagedKioskUnchecked() {
        try {
            if (isManagedKioskInternal()) {
                if (getPowerManagerInternal().wasDeviceIdleFor(30000L)) {
                    return true;
                }
            }
            return false;
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isUnattendedManagedKiosk() {
        boolean z = false;
        if (this.mHasFeature) {
            Preconditions.checkCallAuthorization((canManageUsers(getCallerIdentity()) || hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS")) ? true : true);
            return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda37
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3040x6cbae65();
                }
            })).booleanValue();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isUnattendedManagedKiosk$137$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3040x6cbae65() throws Exception {
        return Boolean.valueOf(isUnattendedManagedKioskUnchecked());
    }

    private boolean isManagedKioskInternal() throws RemoteException {
        return (!this.mOwners.hasDeviceOwner() || this.mInjector.getIActivityManager().getLockTaskModeState() != 1 || isLockTaskFeatureEnabled(1) || deviceHasKeyguard() || inEphemeralUserSession()) ? false : true;
    }

    private boolean isLockTaskFeatureEnabled(int lockTaskFeature) throws RemoteException {
        int lockTaskFeatures = m3013x8b75536b(getCurrentForegroundUserId()).mLockTaskFeatures;
        return (lockTaskFeatures & lockTaskFeature) == lockTaskFeature;
    }

    private boolean deviceHasKeyguard() {
        for (UserInfo userInfo : this.mUserManager.getUsers()) {
            if (this.mLockPatternUtils.isSecure(userInfo.id)) {
                return true;
            }
        }
        return false;
    }

    private boolean inEphemeralUserSession() {
        for (UserInfo userInfo : this.mUserManager.getUsers()) {
            if (this.mInjector.getUserManager().isUserEphemeral(userInfo.id)) {
                return true;
            }
        }
        return false;
    }

    private PowerManagerInternal getPowerManagerInternal() {
        return this.mInjector.getPowerManagerInternal();
    }

    public boolean startViewCalendarEventInManagedProfile(final String packageName, final long eventId, final long start, final long end, final boolean allDay, final int flags) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkStringNotEmpty(packageName, "Package name is empty");
        final CallerIdentity caller = getCallerIdentity();
        if (!isCallingFromPackage(packageName, caller.getUid())) {
            throw new SecurityException("Input package name doesn't align with actual calling package.");
        }
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda49
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3114x36b2a7c6(caller, packageName, eventId, start, end, allDay, flags);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startViewCalendarEventInManagedProfile$138$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3114x36b2a7c6(CallerIdentity caller, String packageName, long eventId, long start, long end, boolean allDay, int flags) throws Exception {
        int workProfileUserId = getManagedUserId(caller.getUserId());
        if (workProfileUserId < 0) {
            return false;
        }
        if (!isPackageAllowedToAccessCalendarForUser(packageName, workProfileUserId)) {
            Slogf.d(LOG_TAG, "Package %s is not allowed to access cross-profile calendar APIs", packageName);
            return false;
        }
        Intent intent = new Intent("android.provider.calendar.action.VIEW_MANAGED_PROFILE_CALENDAR_EVENT");
        intent.setPackage(packageName);
        intent.putExtra("id", eventId);
        intent.putExtra("beginTime", start);
        intent.putExtra("endTime", end);
        intent.putExtra("allDay", allDay);
        intent.setFlags(flags);
        try {
            this.mContext.startActivityAsUser(intent, UserHandle.of(workProfileUserId));
            return true;
        } catch (ActivityNotFoundException e) {
            Slogf.e(LOG_TAG, "View event activity not found", e);
            return false;
        }
    }

    private boolean isCallingFromPackage(final String packageName, final int callingUid) {
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda86
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3026xd76b6447(packageName, callingUid);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isCallingFromPackage$139$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3026xd76b6447(String packageName, int callingUid) throws Exception {
        try {
            int packageUid = this.mInjector.getPackageManager().getPackageUidAsUser(packageName, UserHandle.getUserId(callingUid));
            return Boolean.valueOf(packageUid == callingUid);
        } catch (PackageManager.NameNotFoundException e) {
            Slogf.d(LOG_TAG, "Calling package not found", e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DevicePolicyConstants loadConstants() {
        return DevicePolicyConstants.loadFromString(this.mInjector.settingsGlobalGetString("device_policy_constants"));
    }

    public void setUserControlDisabledPackages(ComponentName who, List<String> packages) {
        Objects.requireNonNull(who, "ComponentName is null");
        Objects.requireNonNull(packages, "packages is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller) || isFinancedDeviceOwner(caller));
        checkCanExecuteOrThrowUnsafe(22);
        synchronized (getLockObject()) {
            ActiveAdmin owner = getDeviceOrProfileOwnerAdminLocked(caller.getUserId());
            if (!Objects.equals(owner.protectedPackages, packages)) {
                owner.protectedPackages = packages.isEmpty() ? null : packages;
                saveSettingsLocked(caller.getUserId());
                pushUserControlDisabledPackagesLocked(caller.getUserId());
            }
        }
        DevicePolicyEventLogger.createEvent(129).setAdmin(who).setStrings((String[]) packages.toArray(new String[packages.size()])).write();
    }

    public List<String> getUserControlDisabledPackages(ComponentName who) {
        List<String> emptyList;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller) || isFinancedDeviceOwner(caller));
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwner = getDeviceOrProfileOwnerAdminLocked(caller.getUserId());
            emptyList = deviceOwner.protectedPackages != null ? deviceOwner.protectedPackages : Collections.emptyList();
        }
        return emptyList;
    }

    public void setCommonCriteriaModeEnabled(ComponentName who, boolean enabled) {
        Objects.requireNonNull(who, "Admin component name must be provided");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller), "Common Criteria mode can only be controlled by a device owner or a profile owner on an organization-owned device.");
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            admin.mCommonCriteriaMode = enabled;
            saveSettingsLocked(caller.getUserId());
        }
        DevicePolicyEventLogger.createEvent(131).setAdmin(who).setBoolean(enabled).write();
    }

    public boolean isCommonCriteriaModeEnabled(ComponentName who) {
        boolean z;
        boolean z2;
        if (who != null) {
            CallerIdentity caller = getCallerIdentity(who);
            Preconditions.checkCallAuthorization((isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller)) ? true : true, "Common Criteria mode can only be controlled by a device owner or a profile owner on an organization-owned device.");
            synchronized (getLockObject()) {
                z2 = getProfileOwnerOrDeviceOwnerLocked(caller).mCommonCriteriaMode;
            }
            return z2;
        }
        synchronized (getLockObject()) {
            ActiveAdmin admin = getDeviceOwnerOrProfileOwnerOfOrganizationOwnedDeviceLocked(0);
            z = admin != null ? admin.mCommonCriteriaMode : false;
        }
        return z;
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public int getPersonalAppsSuspendedReasons(ComponentName who) {
        int result;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            long deadline = admin.mProfileOffDeadline;
            result = makeSuspensionReasons(admin.mSuspendPersonalApps, deadline != 0 && this.mInjector.systemCurrentTimeMillis() > deadline);
            Slogf.d(LOG_TAG, "getPersonalAppsSuspendedReasons user: %d; result: %d", Integer.valueOf(this.mInjector.userHandleGetCallingUserId()), Integer.valueOf(result));
        }
        return result;
    }

    private int makeSuspensionReasons(boolean explicit, boolean timeout) {
        int result = 0;
        if (explicit) {
            result = 0 | 1;
        }
        if (timeout) {
            return result | 2;
        }
        return result;
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void setPersonalAppsSuspended(ComponentName who, boolean suspended) {
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
        Preconditions.checkState(canHandleCheckPolicyComplianceIntent(caller));
        final int callingUserId = caller.getUserId();
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            boolean shouldSaveSettings = false;
            if (admin.mSuspendPersonalApps != suspended) {
                admin.mSuspendPersonalApps = suspended;
                shouldSaveSettings = true;
            }
            if (admin.mProfileOffDeadline != 0) {
                admin.mProfileOffDeadline = 0L;
                shouldSaveSettings = true;
            }
            if (shouldSaveSettings) {
                saveSettingsLocked(callingUserId);
            }
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda114
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3097xce33e70c(callingUserId);
            }
        });
        DevicePolicyEventLogger.createEvent(135).setAdmin(caller.getComponentName()).setBoolean(suspended).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setPersonalAppsSuspended$140$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3097xce33e70c(int callingUserId) throws Exception {
        return Boolean.valueOf(updatePersonalAppsSuspension(callingUserId, this.mUserManager.isUserUnlocked(callingUserId)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void triggerPolicyComplianceCheckIfNeeded(int profileUserId, boolean suspended) {
        synchronized (getLockObject()) {
            ActiveAdmin profileOwner = getProfileOwnerAdminLocked(profileUserId);
            if (profileOwner == null) {
                Slogf.wtf(LOG_TAG, "Profile owner not found for compliance check");
                return;
            }
            if (suspended) {
                Intent intent = new Intent("android.app.action.CHECK_POLICY_COMPLIANCE");
                intent.setPackage(profileOwner.info.getPackageName());
                this.mContext.startActivityAsUser(intent, UserHandle.of(profileUserId));
            } else if (profileOwner.mProfileOffDeadline > 0) {
                sendAdminCommandLocked(profileOwner, "android.app.action.COMPLIANCE_ACKNOWLEDGEMENT_REQUIRED", null, null, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updatePersonalAppsSuspension(int profileUserId, boolean unlocked) {
        boolean shouldSuspend;
        synchronized (getLockObject()) {
            ActiveAdmin profileOwner = getProfileOwnerAdminLocked(profileUserId);
            if (profileOwner != null) {
                int notificationState = updateProfileOffDeadlineLocked(profileUserId, profileOwner, unlocked);
                boolean suspendedExplicitly = profileOwner.mSuspendPersonalApps;
                boolean z = true;
                boolean suspendedByTimeout = profileOwner.mProfileOffDeadline == -1;
                Slogf.d(LOG_TAG, "Personal apps suspended explicitly: %b, by timeout: %b, notification: %d", Boolean.valueOf(suspendedExplicitly), Boolean.valueOf(suspendedByTimeout), Integer.valueOf(notificationState));
                updateProfileOffDeadlineNotificationLocked(profileUserId, profileOwner, notificationState);
                if (!suspendedExplicitly && !suspendedByTimeout) {
                    z = false;
                }
                shouldSuspend = z;
            } else {
                shouldSuspend = false;
            }
        }
        int parentUserId = getProfileParentId(profileUserId);
        suspendPersonalAppsInternal(parentUserId, shouldSuspend);
        return shouldSuspend;
    }

    private int updateProfileOffDeadlineLocked(int profileUserId, ActiveAdmin profileOwner, boolean unlocked) {
        long alarmTime;
        int notificationState;
        long now = this.mInjector.systemCurrentTimeMillis();
        if (profileOwner.mProfileOffDeadline != 0 && now > profileOwner.mProfileOffDeadline) {
            Slogf.i(LOG_TAG, "Profile off deadline has been reached, unlocked: " + unlocked);
            if (profileOwner.mProfileOffDeadline != -1) {
                profileOwner.mProfileOffDeadline = -1L;
                saveSettingsLocked(profileUserId);
            }
            return unlocked ? 0 : 2;
        }
        boolean shouldSaveSettings = false;
        if (profileOwner.mSuspendPersonalApps) {
            if (profileOwner.mProfileOffDeadline != 0) {
                profileOwner.mProfileOffDeadline = 0L;
                shouldSaveSettings = true;
            }
        } else if (profileOwner.mProfileOffDeadline != 0 && profileOwner.mProfileMaximumTimeOffMillis == 0) {
            Slogf.i(LOG_TAG, "Profile off deadline is reset to zero");
            profileOwner.mProfileOffDeadline = 0L;
            shouldSaveSettings = true;
        } else if (profileOwner.mProfileOffDeadline == 0 && profileOwner.mProfileMaximumTimeOffMillis != 0 && !unlocked) {
            Slogf.i(LOG_TAG, "Profile off deadline is set.");
            profileOwner.mProfileOffDeadline = profileOwner.mProfileMaximumTimeOffMillis + now;
            shouldSaveSettings = true;
        }
        if (shouldSaveSettings) {
            saveSettingsLocked(profileUserId);
        }
        if (unlocked || profileOwner.mProfileOffDeadline == 0) {
            alarmTime = 0;
            notificationState = 0;
        } else {
            long j = MANAGED_PROFILE_OFF_WARNING_PERIOD;
            if (profileOwner.mProfileOffDeadline - now < j) {
                alarmTime = profileOwner.mProfileOffDeadline;
                notificationState = 1;
            } else {
                long alarmTime2 = profileOwner.mProfileOffDeadline;
                alarmTime = alarmTime2 - j;
                notificationState = 0;
            }
        }
        AlarmManager am = this.mInjector.getAlarmManager();
        Intent intent = new Intent(ACTION_PROFILE_OFF_DEADLINE);
        intent.setPackage(this.mContext.getPackageName());
        PendingIntent pi = this.mInjector.pendingIntentGetBroadcast(this.mContext, REQUEST_PROFILE_OFF_DEADLINE, intent, 1275068416);
        if (alarmTime == 0) {
            Slogf.i(LOG_TAG, "Profile off deadline alarm is removed.");
            am.cancel(pi);
        } else {
            Slogf.i(LOG_TAG, "Profile off deadline alarm is set.");
            am.set(1, alarmTime, pi);
        }
        return notificationState;
    }

    private void suspendPersonalAppsInternal(int userId, boolean suspended) {
        if (m3013x8b75536b(userId).mAppsSuspended == suspended) {
            return;
        }
        Object[] objArr = new Object[2];
        objArr[0] = suspended ? "Suspending" : "Unsuspending";
        objArr[1] = Integer.valueOf(userId);
        Slogf.i(LOG_TAG, "%s personal apps for user %d", objArr);
        if (suspended) {
            suspendPersonalAppsInPackageManager(userId);
        } else {
            this.mInjector.getPackageManagerInternal().unsuspendForSuspendingPackage(PackageManagerService.PLATFORM_PACKAGE_NAME, userId);
        }
        synchronized (getLockObject()) {
            m3013x8b75536b(userId).mAppsSuspended = suspended;
            saveSettingsLocked(userId);
        }
    }

    private void suspendPersonalAppsInPackageManager(final int userId) {
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda6
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3115x59ef5572(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suspendPersonalAppsInPackageManager$141$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3115x59ef5572(int userId) throws Exception {
        try {
            String[] appsToSuspend = this.mInjector.getPersonalAppsForSuspension(userId);
            String[] failedApps = this.mIPackageManager.setPackagesSuspendedAsUser(appsToSuspend, true, (PersistableBundle) null, (PersistableBundle) null, (SuspendDialogInfo) null, PackageManagerService.PLATFORM_PACKAGE_NAME, userId);
            if (!ArrayUtils.isEmpty(failedApps)) {
                Slogf.wtf(LOG_TAG, "Failed to suspend apps: " + String.join(",", failedApps));
            }
        } catch (RemoteException re) {
            Slogf.e(LOG_TAG, "Failed talking to the package manager", re);
        }
    }

    private void updateProfileOffDeadlineNotificationLocked(int profileUserId, ActiveAdmin profileOwner, int notificationState) {
        String text;
        boolean ongoing;
        if (notificationState == 0) {
            this.mInjector.getNotificationManager().cancel(1003);
            return;
        }
        Intent intent = new Intent(ACTION_TURN_PROFILE_ON_NOTIFICATION);
        intent.setPackage(this.mContext.getPackageName());
        intent.putExtra("android.intent.extra.user_handle", profileUserId);
        PendingIntent pendingIntent = this.mInjector.pendingIntentGetBroadcast(this.mContext, 0, intent, AudioFormat.DTS_HD);
        Notification.Action turnProfileOnButton = new Notification.Action.Builder((Icon) null, getPersonalAppSuspensionButtonText(), pendingIntent).build();
        if (notificationState == 1) {
            long j = profileOwner.mProfileMaximumTimeOffMillis;
            long j2 = MS_PER_DAY;
            int maxDays = (int) ((j + (j2 / 2)) / j2);
            String date = DateUtils.formatDateTime(this.mContext, profileOwner.mProfileOffDeadline, 16);
            String time = DateUtils.formatDateTime(this.mContext, profileOwner.mProfileOffDeadline, 1);
            text = getPersonalAppSuspensionSoonText(date, time, maxDays);
            ongoing = false;
        } else {
            text = getPersonalAppSuspensionText();
            ongoing = true;
        }
        int color = this.mContext.getColor(17171012);
        Bundle extras = new Bundle();
        extras.putString("android.substName", getWorkProfileContentDescription());
        Notification notification = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17302427).setOngoing(ongoing).setAutoCancel(false).setContentTitle(getPersonalAppSuspensionTitle()).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setColor(color).addAction(turnProfileOnButton).addExtras(extras).build();
        this.mInjector.getNotificationManager().notify(1003, notification);
    }

    private String getPersonalAppSuspensionButtonText() {
        return getUpdatableString("Core.PERSONAL_APP_SUSPENSION_TURN_ON_PROFILE", 17041286, new Object[0]);
    }

    private String getPersonalAppSuspensionTitle() {
        return getUpdatableString("Core.PERSONAL_APP_SUSPENSION_TITLE", 17041289, new Object[0]);
    }

    private String getPersonalAppSuspensionText() {
        return getUpdatableString("Core.PERSONAL_APP_SUSPENSION_MESSAGE", 17041288, new Object[0]);
    }

    private String getPersonalAppSuspensionSoonText(String date, String time, int maxDays) {
        return getUpdatableString("Core.PERSONAL_APP_SUSPENSION_SOON_MESSAGE", 17041287, date, time, Integer.valueOf(maxDays));
    }

    private String getWorkProfileContentDescription() {
        return getUpdatableString("Core.NOTIFICATION_WORK_PROFILE_CONTENT_DESCRIPTION", 17040931, new Object[0]);
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void setManagedProfileMaximumTimeOff(ComponentName who, long timeoutMillis) {
        Objects.requireNonNull(who, "ComponentName is null");
        Preconditions.checkArgumentNonnegative(timeoutMillis, "Timeout must be non-negative.");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
        Preconditions.checkState(canHandleCheckPolicyComplianceIntent(caller));
        final int userId = caller.getUserId();
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            if (timeoutMillis > 0) {
                long j = MANAGED_PROFILE_MAXIMUM_TIME_OFF_THRESHOLD;
                if (timeoutMillis < j && !isAdminTestOnlyLocked(who, userId)) {
                    timeoutMillis = j;
                }
            }
            if (admin.mProfileMaximumTimeOffMillis == timeoutMillis) {
                return;
            }
            admin.mProfileMaximumTimeOffMillis = timeoutMillis;
            saveSettingsLocked(userId);
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda152
                public final Object getOrThrow() {
                    return DevicePolicyManagerService.this.m3089x856aaedd(userId);
                }
            });
            DevicePolicyEventLogger.createEvent((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF).setAdmin(caller.getComponentName()).setTimePeriod(timeoutMillis).write();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setManagedProfileMaximumTimeOff$142$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3089x856aaedd(int userId) throws Exception {
        return Boolean.valueOf(updatePersonalAppsSuspension(userId, this.mUserManager.isUserUnlocked()));
    }

    private boolean canHandleCheckPolicyComplianceIntent(final CallerIdentity caller) {
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda92
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m2980x53c1a277(caller);
            }
        });
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$canHandleCheckPolicyComplianceIntent$143$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m2980x53c1a277(CallerIdentity caller) throws Exception {
        Intent intent = new Intent("android.app.action.CHECK_POLICY_COMPLIANCE");
        intent.setPackage(caller.getPackageName());
        List<ResolveInfo> handlers = this.mInjector.getPackageManager().queryIntentActivitiesAsUser(intent, 0, caller.getUserId());
        return Boolean.valueOf(!handlers.isEmpty());
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public long getManagedProfileMaximumTimeOff(ComponentName who) {
        long j;
        Objects.requireNonNull(who, "ComponentName is null");
        CallerIdentity caller = getCallerIdentity(who);
        Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            j = admin.mProfileMaximumTimeOffMillis;
        }
        return j;
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void acknowledgeDeviceCompliant() {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
        enforceUserUnlocked(caller.getUserId());
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            if (admin.mProfileOffDeadline > 0) {
                admin.mProfileOffDeadline = 0L;
                saveSettingsLocked(caller.getUserId());
            }
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public boolean isComplianceAcknowledgementRequired() {
        boolean z;
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isProfileOwnerOfOrganizationOwnedDevice(caller));
        enforceUserUnlocked(caller.getUserId());
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerLocked(caller);
            z = admin.mProfileOffDeadline != 0;
        }
        return z;
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public boolean canProfileOwnerResetPasswordWhenLocked(int userId) {
        Preconditions.checkCallAuthorization(isSystemUid(getCallerIdentity()), String.format(NOT_SYSTEM_CALLER_MSG, "call canProfileOwnerResetPasswordWhenLocked"));
        synchronized (getLockObject()) {
            ActiveAdmin poAdmin = getProfileOwnerAdminLocked(userId);
            if (poAdmin != null && getEncryptionStatus() == 5 && isResetPasswordTokenActiveForUserLocked(userId)) {
                try {
                    ApplicationInfo poAppInfo = this.mIPackageManager.getApplicationInfo(poAdmin.info.getPackageName(), 0L, userId);
                    if (poAppInfo == null) {
                        Slogf.wtf(LOG_TAG, "Cannot find AppInfo for profile owner");
                        return false;
                    } else if (poAppInfo.isEncryptionAware()) {
                        Slogf.d(LOG_TAG, "PO should be able to reset password from direct boot");
                        return true;
                    } else {
                        return false;
                    }
                } catch (RemoteException e) {
                    Slogf.e(LOG_TAG, "Failed to query PO app info", e);
                    return false;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public String getEnrollmentSpecificId(String callerPackage) {
        String str;
        if (!this.mHasFeature) {
            return "";
        }
        CallerIdentity caller = getCallerIdentity(callerPackage);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller) || isCallerDelegate(caller, "delegation-cert-install"));
        synchronized (getLockObject()) {
            ActiveAdmin requiredAdmin = getDeviceOrProfileOwnerAdminLocked(caller.getUserId());
            String esid = requiredAdmin.mEnrollmentSpecificId;
            str = esid != null ? esid : "";
        }
        return str;
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void setOrganizationIdForUser(String callerPackage, String organizationId, int userId) {
        String ownerPackage;
        if (!this.mHasFeature) {
            return;
        }
        Objects.requireNonNull(callerPackage);
        CallerIdentity caller = getCallerIdentity(callerPackage);
        boolean z = false;
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwner(caller), "Only a Device Owner or Profile Owner may set the Enterprise ID.");
        Preconditions.checkArgument(!TextUtils.isEmpty(organizationId), "Enterprise ID may not be empty.");
        Slogf.i(LOG_TAG, "Setting Enterprise ID to %s for user %d", organizationId, Integer.valueOf(userId));
        synchronized (this.mESIDInitilizationLock) {
            if (this.mEsidCalculator == null) {
                this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda32
                    public final void runOrThrow() {
                        DevicePolicyManagerService.this.m3094xff49ab6b();
                    }
                });
            }
        }
        synchronized (getLockObject()) {
            ActiveAdmin owner = getDeviceOrProfileOwnerAdminLocked(userId);
            Preconditions.checkCallAuthorization(owner != null && owner.getUserHandle().getIdentifier() == userId, String.format("The Profile Owner or Device Owner may only set the Enterprise ID on its own user, called on user %d but owner user is %d", Integer.valueOf(userId), Integer.valueOf(owner.getUserHandle().getIdentifier())));
            ownerPackage = owner.info.getPackageName();
            if (TextUtils.isEmpty(owner.mOrganizationId) || owner.mOrganizationId.equals(organizationId)) {
                z = true;
            }
            Preconditions.checkState(z, "The organization ID has been previously set to a different value and cannot be changed");
            String dpcPackage = owner.info.getPackageName();
            String esid = this.mEsidCalculator.calculateEnterpriseId(dpcPackage, organizationId);
            owner.mOrganizationId = organizationId;
            owner.mEnrollmentSpecificId = esid;
            saveSettingsLocked(userId);
        }
        DevicePolicyEventLogger.createEvent((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_ORGANIZATION_ID).setAdmin(ownerPackage).setBoolean(isManagedProfile(userId)).write();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setOrganizationIdForUser$144$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3094xff49ab6b() throws Exception {
        this.mEsidCalculator = this.mInjector.newEnterpriseSpecificIdCalculator();
    }

    public void clearOrganizationIdForUser(int userHandle) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        synchronized (getLockObject()) {
            ActiveAdmin owner = getDeviceOrProfileOwnerAdminLocked(userHandle);
            owner.mOrganizationId = null;
            owner.mEnrollmentSpecificId = null;
            saveSettingsLocked(userHandle);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [17869=4] */
    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public UserHandle createAndProvisionManagedProfile(ManagedProfileProvisioningParams provisioningParams, String callerPackage) {
        Objects.requireNonNull(provisioningParams, "provisioningParams is null");
        Objects.requireNonNull(callerPackage, "callerPackage is null");
        ComponentName admin = provisioningParams.getProfileAdminComponentName();
        Objects.requireNonNull(admin, "admin is null");
        CallerIdentity caller = getCallerIdentity(callerPackage);
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        provisioningParams.logParams(callerPackage);
        UserInfo userInfo = null;
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                int result = checkProvisioningPreconditionSkipPermission("android.app.action.PROVISION_MANAGED_PROFILE", admin.getPackageName());
                if (result == 0) {
                    long startTime = SystemClock.elapsedRealtime();
                    onCreateAndProvisionManagedProfileStarted(provisioningParams);
                    Set<String> nonRequiredApps = provisioningParams.isLeaveAllSystemAppsEnabled() ? Collections.emptySet() : this.mOverlayPackagesProvider.getNonRequiredApps(admin, caller.getUserId(), "android.app.action.PROVISION_MANAGED_PROFILE");
                    if (nonRequiredApps.isEmpty()) {
                        Slogf.i(LOG_TAG, "No disallowed packages for the managed profile.");
                    } else {
                        for (Iterator<String> it = nonRequiredApps.iterator(); it.hasNext(); it = it) {
                            String packageName = it.next();
                            Slogf.i(LOG_TAG, "Disallowed package [" + packageName + "]");
                        }
                    }
                    UserInfo userInfo2 = this.mUserManager.createProfileForUserEvenWhenDisallowed(provisioningParams.getProfileName(), "android.os.usertype.profile.MANAGED", 64, caller.getUserId(), (String[]) nonRequiredApps.toArray(new String[nonRequiredApps.size()]));
                    try {
                        if (userInfo2 != null) {
                            resetInteractAcrossProfilesAppOps();
                            logEventDuration(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PLATFORM_PROVISIONING_CREATE_PROFILE_MS, startTime, callerPackage);
                            maybeInstallDevicePolicyManagementRoleHolderInUser(userInfo2.id);
                            installExistingAdminPackage(userInfo2.id, admin.getPackageName());
                            if (enableAdminAndSetProfileOwner(userInfo2.id, caller.getUserId(), admin, provisioningParams.getOwnerName())) {
                                setUserSetupComplete(userInfo2.id);
                                startUser(userInfo2.id, callerPackage);
                                maybeMigrateAccount(userInfo2.id, caller.getUserId(), provisioningParams.getAccountToMigrate(), provisioningParams.isKeepingAccountOnMigration(), callerPackage);
                                if (provisioningParams.isOrganizationOwnedProvisioning()) {
                                    synchronized (getLockObject()) {
                                        setProfileOwnerOnOrganizationOwnedDeviceUncheckedLocked(admin, userInfo2.id, true);
                                    }
                                }
                                onCreateAndProvisionManagedProfileCompleted(provisioningParams);
                                sendProvisioningCompletedBroadcast(userInfo2.id, "android.app.action.PROVISION_MANAGED_PROFILE", provisioningParams.isLeaveAllSystemAppsEnabled());
                                UserHandle userHandle = userInfo2.getUserHandle();
                                Binder.restoreCallingIdentity(identity);
                                return userHandle;
                            }
                            throw new ServiceSpecificException(4, "Error setting profile owner.");
                        }
                        throw new ServiceSpecificException(2, "Error creating profile, createProfileForUserEvenWhenDisallowed returned null.");
                    } catch (Exception e) {
                        e = e;
                        userInfo = userInfo2;
                        DevicePolicyEventLogger.createEvent(194).setStrings(new String[]{callerPackage}).write();
                        if (userInfo != null) {
                            this.mUserManager.removeUserEvenWhenDisallowed(userInfo.id);
                        }
                        throw e;
                    } catch (Throwable th) {
                        e = th;
                        Binder.restoreCallingIdentity(identity);
                        throw e;
                    }
                }
                throw new ServiceSpecificException(1, "Provisioning preconditions failed with result: " + result);
            } catch (Throwable th2) {
                e = th2;
            }
        } catch (Exception e2) {
            e = e2;
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void finalizeWorkProfileProvisioning(UserHandle managedProfileUser, Account migratedAccount) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        if (!isManagedProfile(managedProfileUser.getIdentifier())) {
            throw new IllegalStateException("Given user is not a managed profile");
        }
        ComponentName profileOwnerComponent = this.mOwners.getProfileOwnerComponent(managedProfileUser.getIdentifier());
        if (profileOwnerComponent == null) {
            throw new IllegalStateException("There is no profile owner on the given profile");
        }
        Intent primaryProfileSuccessIntent = new Intent("android.app.action.MANAGED_PROFILE_PROVISIONED");
        primaryProfileSuccessIntent.setPackage(profileOwnerComponent.getPackageName());
        primaryProfileSuccessIntent.addFlags(268435488);
        primaryProfileSuccessIntent.putExtra("android.intent.extra.USER", managedProfileUser);
        if (migratedAccount != null) {
            primaryProfileSuccessIntent.putExtra("android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE", migratedAccount);
        }
        this.mContext.sendBroadcastAsUser(primaryProfileSuccessIntent, UserHandle.of(getProfileParentId(managedProfileUser.getIdentifier())));
    }

    private void onCreateAndProvisionManagedProfileStarted(ManagedProfileProvisioningParams provisioningParams) {
    }

    private void onCreateAndProvisionManagedProfileCompleted(ManagedProfileProvisioningParams provisioningParams) {
    }

    private void maybeInstallDevicePolicyManagementRoleHolderInUser(int targetUserId) {
        String devicePolicyManagerRoleHolderPackageName = getDevicePolicyManagementRoleHolderPackageName(this.mContext);
        if (devicePolicyManagerRoleHolderPackageName == null) {
            Slogf.d(LOG_TAG, "No device policy management role holder specified.");
            return;
        }
        try {
            if (this.mIPackageManager.isPackageAvailable(devicePolicyManagerRoleHolderPackageName, targetUserId)) {
                Slogf.d(LOG_TAG, "The device policy management role holder " + devicePolicyManagerRoleHolderPackageName + " is already installed in user " + targetUserId);
                return;
            }
            Slogf.d(LOG_TAG, "Installing the device policy management role holder " + devicePolicyManagerRoleHolderPackageName + " in user " + targetUserId);
            this.mIPackageManager.installExistingPackageAsUser(devicePolicyManagerRoleHolderPackageName, targetUserId, 4194304, 1, (List) null);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getDevicePolicyManagementRoleHolderPackageName(Context context) {
        RoleManager roleManager = (RoleManager) context.getSystemService(RoleManager.class);
        List<String> roleHolders = roleManager.getRoleHolders("android.app.role.DEVICE_POLICY_MANAGEMENT");
        if (roleHolders.isEmpty()) {
            return null;
        }
        return roleHolders.get(0);
    }

    private void resetInteractAcrossProfilesAppOps() {
        this.mInjector.getCrossProfileApps().clearInteractAcrossProfilesAppOps();
        pregrantDefaultInteractAcrossProfilesAppOps();
    }

    private void pregrantDefaultInteractAcrossProfilesAppOps() {
        String op = AppOpsManager.permissionToOp("android.permission.INTERACT_ACROSS_PROFILES");
        for (String packageName : getConfigurableDefaultCrossProfilePackages()) {
            if (!appOpIsChangedFromDefault(op, packageName)) {
                this.mInjector.getCrossProfileApps().setInteractAcrossProfilesAppOp(packageName, 0);
            }
        }
    }

    private Set<String> getConfigurableDefaultCrossProfilePackages() {
        List<String> defaultPackages = getDefaultCrossProfilePackages();
        Stream<String> stream = defaultPackages.stream();
        final CrossProfileApps crossProfileApps = this.mInjector.getCrossProfileApps();
        Objects.requireNonNull(crossProfileApps);
        return (Set) stream.filter(new Predicate() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda42
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return crossProfileApps.canConfigureInteractAcrossProfiles((String) obj);
            }
        }).collect(Collectors.toSet());
    }

    private boolean appOpIsChangedFromDefault(String op, String packageName) {
        try {
            int uid = this.mContext.getPackageManager().getPackageUid(packageName, 0);
            return this.mInjector.getAppOpsManager().unsafeCheckOpNoThrow(op, uid, packageName) != 3;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void installExistingAdminPackage(int userId, String packageName) {
        try {
            int status = this.mContext.getPackageManager().installExistingPackageAsUser(packageName, userId);
            if (status != 1) {
                throw new ServiceSpecificException(3, String.format("Failed to install existing package %s for user %d with result code %d", packageName, Integer.valueOf(userId), Integer.valueOf(status)));
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new ServiceSpecificException(3, String.format("Failed to install existing package %s for user %d: %s", packageName, Integer.valueOf(userId), e.getMessage()));
        }
    }

    private boolean enableAdminAndSetProfileOwner(int userId, int callingUserId, ComponentName adminComponent, String ownerName) {
        enableAndSetActiveAdmin(userId, callingUserId, adminComponent);
        return setProfileOwner(adminComponent, ownerName, userId);
    }

    private void enableAndSetActiveAdmin(int userId, int callingUserId, ComponentName adminComponent) {
        String adminPackage = adminComponent.getPackageName();
        enablePackage(adminPackage, callingUserId);
        setActiveAdmin(adminComponent, true, userId);
    }

    private void enablePackage(String packageName, int userId) {
        try {
            int enabledSetting = this.mIPackageManager.getApplicationEnabledSetting(packageName, userId);
            if (enabledSetting != 0 && enabledSetting != 1) {
                this.mIPackageManager.setApplicationEnabledSetting(packageName, 0, 1, userId, this.mContext.getOpPackageName());
            }
        } catch (RemoteException e) {
        }
    }

    private void setUserSetupComplete(int userId) {
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, userId);
    }

    private void startUser(int userId, String callerPackage) throws IllegalStateException {
        long startTime = SystemClock.elapsedRealtime();
        UserUnlockedBlockingReceiver unlockedReceiver = new UserUnlockedBlockingReceiver(userId);
        this.mContext.registerReceiverAsUser(unlockedReceiver, new UserHandle(userId), new IntentFilter("android.intent.action.USER_UNLOCKED"), null, null);
        try {
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mContext.unregisterReceiver(unlockedReceiver);
            throw th;
        }
        if (!this.mInjector.getIActivityManager().startUserInBackground(userId)) {
            throw new ServiceSpecificException(5, String.format("Unable to start user %d in background", Integer.valueOf(userId)));
        }
        if (!unlockedReceiver.waitForUserUnlocked()) {
            throw new ServiceSpecificException(5, String.format("Timeout whilst waiting for unlock of user %d.", Integer.valueOf(userId)));
        }
        logEventDuration(192, startTime, callerPackage);
        this.mContext.unregisterReceiver(unlockedReceiver);
    }

    private void maybeMigrateAccount(int targetUserId, int sourceUserId, Account accountToMigrate, boolean keepAccountMigrated, String callerPackage) {
        UserHandle sourceUser = UserHandle.of(sourceUserId);
        UserHandle targetUser = UserHandle.of(targetUserId);
        if (accountToMigrate == null) {
            Slogf.d(LOG_TAG, "No account to migrate.");
        } else if (sourceUser.equals(targetUser)) {
            Slogf.w(LOG_TAG, "sourceUser and targetUser are the same, won't migrate account.");
        } else {
            copyAccount(targetUser, sourceUser, accountToMigrate, callerPackage);
            if (!keepAccountMigrated) {
                removeAccount(accountToMigrate);
            }
        }
    }

    private void copyAccount(UserHandle targetUser, UserHandle sourceUser, Account accountToMigrate, String callerPackage) {
        long startTime = SystemClock.elapsedRealtime();
        try {
            AccountManager accountManager = (AccountManager) this.mContext.getSystemService(AccountManager.class);
            boolean copySucceeded = ((Boolean) accountManager.copyAccountToUser(accountToMigrate, sourceUser, targetUser, null, null).getResult(180L, TimeUnit.SECONDS)).booleanValue();
            if (copySucceeded) {
                logCopyAccountStatus(1, callerPackage);
                logEventDuration(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PLATFORM_PROVISIONING_COPY_ACCOUNT_MS, startTime, callerPackage);
            } else {
                logCopyAccountStatus(2, callerPackage);
                Slogf.e(LOG_TAG, "Failed to copy account to " + targetUser);
            }
        } catch (AuthenticatorException | IOException e) {
            logCopyAccountStatus(4, callerPackage);
            Slogf.e(LOG_TAG, "Exception copying account to " + targetUser, e);
        } catch (OperationCanceledException e2) {
            logCopyAccountStatus(3, callerPackage);
            Slogf.e(LOG_TAG, "Exception copying account to " + targetUser, e2);
        }
    }

    private static void logCopyAccountStatus(int status, String callerPackage) {
        DevicePolicyEventLogger.createEvent(193).setInt(status).setStrings(new String[]{callerPackage}).write();
    }

    private void removeAccount(Account account) {
        AccountManager accountManager = (AccountManager) this.mContext.getSystemService(AccountManager.class);
        AccountManagerFuture<Bundle> bundle = accountManager.removeAccount(account, null, null, null);
        try {
            Bundle result = bundle.getResult();
            if (result.getBoolean("booleanResult", false)) {
                Slogf.i(LOG_TAG, "Account removed from the primary user.");
            } else {
                final Intent removeIntent = (Intent) result.getParcelable("intent");
                removeIntent.addFlags(268435456);
                if (removeIntent != null) {
                    Slogf.i(LOG_TAG, "Starting activity to remove account");
                    new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda26
                        @Override // java.lang.Runnable
                        public final void run() {
                            DevicePolicyManagerService.this.m3055xd6070c1f(removeIntent);
                        }
                    });
                } else {
                    Slogf.e(LOG_TAG, "Could not remove account from the primary user.");
                }
            }
        } catch (AuthenticatorException | OperationCanceledException | IOException e) {
            Slogf.e(LOG_TAG, "Exception removing account from the primary user.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeAccount$145$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3055xd6070c1f(Intent removeIntent) {
        this.mContext.startActivity(removeIntent);
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void provisionFullyManagedDevice(FullyManagedDeviceProvisioningParams provisioningParams, String callerPackage) {
        Objects.requireNonNull(provisioningParams, "provisioningParams is null.");
        Objects.requireNonNull(callerPackage, "callerPackage is null.");
        ComponentName deviceAdmin = provisioningParams.getDeviceAdminComponentName();
        Objects.requireNonNull(deviceAdmin, "admin is null.");
        Objects.requireNonNull(provisioningParams.getOwnerName(), "owner name is null.");
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS") || (hasCallingOrSelfPermission("android.permission.PROVISION_DEMO_DEVICE") && provisioningParams.isDemoDevice()));
        provisioningParams.logParams(callerPackage);
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                int result = checkProvisioningPreconditionSkipPermission("android.app.action.PROVISION_MANAGED_DEVICE", deviceAdmin.getPackageName());
                if (result != 0) {
                    throw new ServiceSpecificException(1, "Provisioning preconditions failed with result: " + result);
                }
                onProvisionFullyManagedDeviceStarted(provisioningParams);
                setTimeAndTimezone(provisioningParams.getTimeZone(), provisioningParams.getLocalTime());
                setLocale(provisioningParams.getLocale());
                int deviceOwnerUserId = this.mInjector.userManagerIsHeadlessSystemUserMode() ? 0 : caller.getUserId();
                if (!removeNonRequiredAppsForManagedDevice(deviceOwnerUserId, provisioningParams.isLeaveAllSystemAppsEnabled(), deviceAdmin)) {
                    throw new ServiceSpecificException(6, "PackageManager failed to remove non required apps.");
                }
                if (!setActiveAdminAndDeviceOwner(deviceOwnerUserId, deviceAdmin, provisioningParams.getOwnerName())) {
                    throw new ServiceSpecificException(7, "Failed to set device owner.");
                }
                disallowAddUser();
                setAdminCanGrantSensorsPermissionForUserUnchecked(deviceOwnerUserId, provisioningParams.canDeviceOwnerGrantSensorsPermissions());
                setDemoDeviceStateUnchecked(deviceOwnerUserId, provisioningParams.isDemoDevice());
                onProvisionFullyManagedDeviceCompleted(provisioningParams);
                sendProvisioningCompletedBroadcast(deviceOwnerUserId, "android.app.action.PROVISION_MANAGED_DEVICE", provisioningParams.isLeaveAllSystemAppsEnabled());
            } catch (Exception e) {
                DevicePolicyEventLogger.createEvent(194).setStrings(new String[]{callerPackage}).write();
                throw e;
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void onProvisionFullyManagedDeviceStarted(FullyManagedDeviceProvisioningParams provisioningParams) {
    }

    private void onProvisionFullyManagedDeviceCompleted(FullyManagedDeviceProvisioningParams provisioningParams) {
    }

    private void setTimeAndTimezone(String timeZone, long localTime) {
        try {
            AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
            if (timeZone != null) {
                alarmManager.setTimeZone(timeZone);
            }
            if (localTime > 0) {
                alarmManager.setTime(localTime);
            }
        } catch (Exception e) {
            Slogf.e(LOG_TAG, "Alarm manager failed to set the system time/timezone.", e);
        }
    }

    private void setLocale(Locale locale) {
        if (locale == null || locale.equals(Locale.getDefault())) {
            return;
        }
        try {
            LocalePicker.updateLocale(locale);
        } catch (Exception e) {
            Slogf.e(LOG_TAG, "Failed to set the system locale.", e);
        }
    }

    private boolean removeNonRequiredAppsForManagedDevice(int userId, boolean leaveAllSystemAppsEnabled, ComponentName admin) {
        Set<String> packagesToDelete;
        if (leaveAllSystemAppsEnabled) {
            packagesToDelete = Collections.emptySet();
        } else {
            packagesToDelete = this.mOverlayPackagesProvider.getNonRequiredApps(admin, userId, "android.app.action.PROVISION_MANAGED_DEVICE");
        }
        removeNonInstalledPackages(packagesToDelete, userId);
        if (packagesToDelete.isEmpty()) {
            Slogf.i(LOG_TAG, "No packages to delete on user " + userId);
            return true;
        }
        IPackageDeleteObserver nonRequiredPackageDeleteObserver = new NonRequiredPackageDeleteObserver(packagesToDelete.size());
        for (String packageName : packagesToDelete) {
            Slogf.i(LOG_TAG, "Deleting package [" + packageName + "] as user " + userId);
            this.mContext.getPackageManager().deletePackageAsUser(packageName, nonRequiredPackageDeleteObserver, 4, userId);
        }
        Slogf.i(LOG_TAG, "Waiting for non required apps to be deleted");
        return nonRequiredPackageDeleteObserver.awaitPackagesDeletion();
    }

    private void removeNonInstalledPackages(Set<String> packages, int userId) {
        Set<String> toBeRemoved = new HashSet<>();
        for (String packageName : packages) {
            if (!isPackageInstalledForUser(packageName, userId)) {
                toBeRemoved.add(packageName);
            }
        }
        packages.removeAll(toBeRemoved);
    }

    private void disallowAddUser() {
        if (this.mInjector.userManagerIsHeadlessSystemUserMode()) {
            Slogf.i(LOG_TAG, "Not setting DISALLOW_ADD_USER on headless system user mode.");
            return;
        }
        for (UserInfo userInfo : this.mUserManager.getUsers()) {
            UserHandle userHandle = userInfo.getUserHandle();
            if (!this.mUserManager.hasUserRestriction("no_add_user", userHandle)) {
                this.mUserManager.setUserRestriction("no_add_user", true, userHandle);
            }
        }
    }

    private boolean setActiveAdminAndDeviceOwner(int userId, ComponentName adminComponent, String name) {
        enableAndSetActiveAdmin(userId, userId, adminComponent);
        if (getDeviceOwnerComponent(true) != null) {
            return true;
        }
        return setDeviceOwner(adminComponent, name, userId, true);
    }

    private static void logEventDuration(int eventId, long startTime, String callerPackage) {
        long duration = SystemClock.elapsedRealtime() - startTime;
        DevicePolicyEventLogger.createEvent(eventId).setTimePeriod(duration).setStrings(new String[]{callerPackage}).write();
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void resetDefaultCrossProfileIntentFilters(final int userId) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda25
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3061x105ab103(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$resetDefaultCrossProfileIntentFilters$146$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3061x105ab103(int userId) throws Exception {
        try {
            List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
            int numOfProfiles = profiles.size();
            if (numOfProfiles <= 1) {
                return;
            }
            String managedProvisioningPackageName = getManagedProvisioningPackage(this.mContext);
            this.mIPackageManager.clearCrossProfileIntentFilters(userId, this.mContext.getOpPackageName());
            this.mIPackageManager.clearCrossProfileIntentFilters(userId, managedProvisioningPackageName);
            for (int i = 0; i < numOfProfiles; i++) {
                UserInfo profile = profiles.get(i);
                this.mIPackageManager.clearCrossProfileIntentFilters(profile.id, this.mContext.getOpPackageName());
                this.mIPackageManager.clearCrossProfileIntentFilters(profile.id, managedProvisioningPackageName);
                this.mUserManagerInternal.setDefaultCrossProfileIntentFilters(userId, profile.id);
            }
        } catch (RemoteException e) {
        }
    }

    private void setAdminCanGrantSensorsPermissionForUserUnchecked(int userId, boolean canGrant) {
        boolean z = false;
        Slogf.d(LOG_TAG, "setAdminCanGrantSensorsPermissionForUserUnchecked(%d, %b)", Integer.valueOf(userId), Boolean.valueOf(canGrant));
        synchronized (getLockObject()) {
            ActiveAdmin owner = getDeviceOrProfileOwnerAdminLocked(userId);
            if (isDeviceOwner(owner) && owner.getUserHandle().getIdentifier() == userId) {
                z = true;
            }
            Preconditions.checkState(z, "May only be set on a the user of a device owner.");
            owner.mAdminCanGrantSensorsPermissions = canGrant;
            this.mPolicyCache.setAdminCanGrantSensorsPermissions(userId, canGrant);
            saveSettingsLocked(userId);
        }
    }

    private void setDemoDeviceStateUnchecked(int userId, boolean isDemoDevice) {
        Slogf.d(LOG_TAG, "setDemoDeviceStateUnchecked(%d, %b)", Integer.valueOf(userId), Boolean.valueOf(isDemoDevice));
        if (!isDemoDevice) {
            return;
        }
        synchronized (getLockObject()) {
            this.mInjector.settingsGlobalPutStringForUser("device_demo_mode", Integer.toString(1), userId);
        }
        setUserProvisioningState(3, userId);
    }

    private void updateAdminCanGrantSensorsPermissionCache(int userId) {
        ActiveAdmin owner;
        synchronized (getLockObject()) {
            if (isUserAffiliatedWithDeviceLocked(userId)) {
                owner = getDeviceOwnerAdminLocked();
            } else {
                owner = getDeviceOrProfileOwnerAdminLocked(userId);
            }
            boolean canGrant = owner != null ? owner.mAdminCanGrantSensorsPermissions : false;
            this.mPolicyCache.setAdminCanGrantSensorsPermissions(userId, canGrant);
        }
    }

    private void updateNetworkPreferenceForUser(final int userId, List<PreferentialNetworkServiceConfig> preferentialNetworkServiceConfigs) {
        if (!isManagedProfile(userId) && !isDeviceOwnerUserId(userId)) {
            return;
        }
        final List<ProfileNetworkPreference> preferences = new ArrayList<>();
        for (PreferentialNetworkServiceConfig preferentialNetworkServiceConfig : preferentialNetworkServiceConfigs) {
            ProfileNetworkPreference.Builder preferenceBuilder = new ProfileNetworkPreference.Builder();
            if (preferentialNetworkServiceConfig.isEnabled()) {
                if (preferentialNetworkServiceConfig.isFallbackToDefaultConnectionAllowed()) {
                    preferenceBuilder.setPreference(1);
                } else {
                    preferenceBuilder.setPreference(2);
                }
                preferenceBuilder.setIncludedUids(preferentialNetworkServiceConfig.getIncludedUids());
                preferenceBuilder.setExcludedUids(preferentialNetworkServiceConfig.getExcludedUids());
                preferenceBuilder.setPreferenceEnterpriseId(preferentialNetworkServiceConfig.getNetworkId());
            } else {
                preferenceBuilder.setPreference(0);
            }
            preferences.add(preferenceBuilder.build());
        }
        Slogf.d(LOG_TAG, "updateNetworkPreferenceForUser to " + preferences);
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda67
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3119x9717fc78(userId, preferences);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateNetworkPreferenceForUser$147$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3119x9717fc78(int userId, List preferences) throws Exception {
        this.mInjector.getConnectivityManager().setProfileNetworkPreferences(UserHandle.of(userId), preferences, null, null);
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public boolean canAdminGrantSensorsPermissionsForUser(int userId) {
        if (!this.mHasFeature) {
            return false;
        }
        return this.mPolicyCache.canAdminGrantSensorsPermissionsForUser(userId);
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void setDeviceOwnerType(ComponentName admin, int deviceOwnerType) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        synchronized (getLockObject()) {
            setDeviceOwnerTypeLocked(admin, deviceOwnerType);
        }
    }

    private void setDeviceOwnerTypeLocked(ComponentName admin, int deviceOwnerType) {
        String packageName = admin.getPackageName();
        verifyDeviceOwnerTypePreconditionsLocked(admin);
        boolean isAdminTestOnly = isAdminTestOnlyLocked(admin, this.mOwners.getDeviceOwnerUserId());
        Preconditions.checkState(isAdminTestOnly || !this.mOwners.isDeviceOwnerTypeSetForDeviceOwner(packageName), "Test only admins can only set the device owner type more than once");
        this.mOwners.setDeviceOwnerType(packageName, deviceOwnerType, isAdminTestOnly);
        setGlobalSettingDeviceOwnerType(deviceOwnerType);
    }

    private void setGlobalSettingDeviceOwnerType(final int deviceOwnerType) {
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda13
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3087x23bdb239(deviceOwnerType);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setGlobalSettingDeviceOwnerType$148$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3087x23bdb239(int deviceOwnerType) throws Exception {
        this.mInjector.settingsGlobalPutInt("device_owner_type", deviceOwnerType);
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public int getDeviceOwnerType(ComponentName admin) {
        int deviceOwnerTypeLocked;
        synchronized (getLockObject()) {
            verifyDeviceOwnerTypePreconditionsLocked(admin);
            deviceOwnerTypeLocked = getDeviceOwnerTypeLocked(admin.getPackageName());
        }
        return deviceOwnerTypeLocked;
    }

    private int getDeviceOwnerTypeLocked(String packageName) {
        return this.mOwners.getDeviceOwnerType(packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isFinancedDeviceOwner(CallerIdentity caller) {
        boolean z;
        synchronized (getLockObject()) {
            z = true;
            if (!isDeviceOwnerLocked(caller) || getDeviceOwnerTypeLocked(this.mOwners.getDeviceOwnerPackageName()) != 1) {
                z = false;
            }
        }
        return z;
    }

    private void verifyDeviceOwnerTypePreconditionsLocked(ComponentName admin) {
        Preconditions.checkState(this.mOwners.hasDeviceOwner(), "there is no device owner");
        Preconditions.checkState(this.mOwners.getDeviceOwnerComponent().equals(admin), "admin is not the device owner");
    }

    public void setUsbDataSignalingEnabled(String packageName, boolean enabled) {
        Objects.requireNonNull(packageName, "Admin package name must be provided");
        CallerIdentity caller = getCallerIdentity(packageName);
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller), "USB data signaling can only be controlled by a device owner or a profile owner on an organization-owned device.");
        Preconditions.checkState(canUsbDataSignalingBeDisabled(), "USB data signaling cannot be disabled.");
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            if (admin.mUsbDataSignalingEnabled != enabled) {
                admin.mUsbDataSignalingEnabled = enabled;
                saveSettingsLocked(caller.getUserId());
                updateUsbDataSignal();
            }
        }
        DevicePolicyEventLogger.createEvent((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_USB_DATA_SIGNALING).setAdmin(packageName).setBoolean(enabled).write();
    }

    private void updateUsbDataSignal() {
        final boolean usbEnabled;
        if (!canUsbDataSignalingBeDisabled()) {
            return;
        }
        synchronized (getLockObject()) {
            usbEnabled = isUsbDataSignalingEnabledInternalLocked();
        }
        if (!((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda40
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3123xa68a5717(usbEnabled);
            }
        })).booleanValue()) {
            Slogf.w(LOG_TAG, "Failed to set usb data signaling state");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateUsbDataSignal$149$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3123xa68a5717(boolean usbEnabled) throws Exception {
        return Boolean.valueOf(this.mInjector.getUsbManager().enableUsbDataSignal(usbEnabled));
    }

    public boolean isUsbDataSignalingEnabled(String packageName) {
        CallerIdentity caller = getCallerIdentity(packageName);
        synchronized (getLockObject()) {
            if (!isDefaultDeviceOwner(caller) && !isProfileOwnerOfOrganizationOwnedDevice(caller)) {
                return isUsbDataSignalingEnabledInternalLocked();
            }
            return getProfileOwnerOrDeviceOwnerLocked(caller).mUsbDataSignalingEnabled;
        }
    }

    public boolean isUsbDataSignalingEnabledForUser(int userId) {
        boolean isUsbDataSignalingEnabledInternalLocked;
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isSystemUid(caller));
        synchronized (getLockObject()) {
            isUsbDataSignalingEnabledInternalLocked = isUsbDataSignalingEnabledInternalLocked();
        }
        return isUsbDataSignalingEnabledInternalLocked;
    }

    private boolean isUsbDataSignalingEnabledInternalLocked() {
        ActiveAdmin admin = getDeviceOwnerOrProfileOwnerOfOrganizationOwnedDeviceLocked(0);
        return admin == null || admin.mUsbDataSignalingEnabled;
    }

    public boolean canUsbDataSignalingBeDisabled() {
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda48
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m2981x1e4adb13();
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$canUsbDataSignalingBeDisabled$150$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m2981x1e4adb13() throws Exception {
        return Boolean.valueOf(this.mInjector.getUsbManager() != null && this.mInjector.getUsbManager().getUsbHalVersion() >= 13);
    }

    private void notifyMinimumRequiredWifiSecurityLevelChanged(final int level) {
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda121
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3049xc666d424(level);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyMinimumRequiredWifiSecurityLevelChanged$151$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3049xc666d424(int level) throws Exception {
        this.mInjector.getWifiManager().notifyMinimumRequiredWifiSecurityLevelChanged(level);
    }

    private void notifyWifiSsidPolicyChanged(final WifiSsidPolicy policy) {
        if (policy == null) {
            return;
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda54
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3051xc75fd5c3(policy);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyWifiSsidPolicyChanged$152$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3051xc75fd5c3(WifiSsidPolicy policy) throws Exception {
        this.mInjector.getWifiManager().notifyWifiSsidPolicyChanged(policy);
    }

    public void setMinimumRequiredWifiSecurityLevel(int level) {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller), "Wi-Fi minimum security level can only be controlled by a device owner or a profile owner on an organization-owned device.");
        boolean valueChanged = false;
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            if (admin.mWifiMinimumSecurityLevel != level) {
                admin.mWifiMinimumSecurityLevel = level;
                saveSettingsLocked(caller.getUserId());
                valueChanged = true;
            }
        }
        if (valueChanged) {
            notifyMinimumRequiredWifiSecurityLevelChanged(level);
        }
    }

    public int getMinimumRequiredWifiSecurityLevel() {
        int i;
        synchronized (getLockObject()) {
            i = 0;
            ActiveAdmin admin = getDeviceOwnerOrProfileOwnerOfOrganizationOwnedDeviceLocked(0);
            if (admin != null) {
                i = admin.mWifiMinimumSecurityLevel;
            }
        }
        return i;
    }

    public WifiSsidPolicy getWifiSsidPolicy() {
        WifiSsidPolicy wifiSsidPolicy;
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller) || canQueryAdminPolicy(caller), "SSID policy can only be retrieved by a device owner or a profile owner on an organization-owned device or an app with the QUERY_ADMIN_POLICY permission.");
        synchronized (getLockObject()) {
            ActiveAdmin admin = getDeviceOwnerOrProfileOwnerOfOrganizationOwnedDeviceLocked(0);
            wifiSsidPolicy = admin != null ? admin.mWifiSsidPolicy : null;
        }
        return wifiSsidPolicy;
    }

    public void setWifiSsidPolicy(WifiSsidPolicy policy) {
        CallerIdentity caller = getCallerIdentity();
        Preconditions.checkCallAuthorization(isDefaultDeviceOwner(caller) || isProfileOwnerOfOrganizationOwnedDevice(caller), "SSID denylist can only be controlled by a device owner or a profile owner on an organization-owned device.");
        boolean changed = false;
        synchronized (getLockObject()) {
            ActiveAdmin admin = getProfileOwnerOrDeviceOwnerLocked(caller);
            if (!Objects.equals(policy, admin.mWifiSsidPolicy)) {
                admin.mWifiSsidPolicy = policy;
                changed = true;
            }
            if (changed) {
                saveSettingsLocked(caller.getUserId());
            }
        }
        if (changed) {
            notifyWifiSsidPolicyChanged(policy);
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void setDrawables(final List<DevicePolicyDrawableResource> drawables) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.UPDATE_DEVICE_MANAGEMENT_RESOURCES"));
        Objects.requireNonNull(drawables, "drawables must be provided.");
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda143
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3081xcb43489d(drawables);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDrawables$154$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3081xcb43489d(List drawables) throws Exception {
        if (this.mDeviceManagementResourcesProvider.updateDrawables(drawables)) {
            sendDrawableUpdatedBroadcast((List) drawables.stream().map(new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda90
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    String drawableId;
                    drawableId = ((DevicePolicyDrawableResource) obj).getDrawableId();
                    return drawableId;
                }
            }).collect(Collectors.toList()));
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void resetDrawables(final List<String> drawableIds) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.UPDATE_DEVICE_MANAGEMENT_RESOURCES"));
        Objects.requireNonNull(drawableIds, "drawableIds must be provided.");
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda100
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3062x76a83329(drawableIds);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$resetDrawables$155$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3062x76a83329(List drawableIds) throws Exception {
        if (this.mDeviceManagementResourcesProvider.removeDrawables(drawableIds)) {
            sendDrawableUpdatedBroadcast(drawableIds);
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public ParcelableResource getDrawable(final String drawableId, final String drawableStyle, final String drawableSource) {
        return (ParcelableResource) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda161
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3001x26315cd4(drawableId, drawableStyle, drawableSource);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getDrawable$156$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ ParcelableResource m3001x26315cd4(String drawableId, String drawableStyle, String drawableSource) throws Exception {
        return this.mDeviceManagementResourcesProvider.getDrawable(drawableId, drawableStyle, drawableSource);
    }

    private void sendDrawableUpdatedBroadcast(List<String> drawableIds) {
        sendResourceUpdatedBroadcast(1, drawableIds);
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void setStrings(final List<DevicePolicyStringResource> strings) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.UPDATE_DEVICE_MANAGEMENT_RESOURCES"));
        Objects.requireNonNull(strings, "strings must be provided.");
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda17
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3106xfc4442cc(strings);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setStrings$158$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3106xfc4442cc(List strings) throws Exception {
        if (this.mDeviceManagementResourcesProvider.updateStrings(strings)) {
            sendStringsUpdatedBroadcast((List) strings.stream().map(new Function() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda24
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    String stringId;
                    stringId = ((DevicePolicyStringResource) obj).getStringId();
                    return stringId;
                }
            }).collect(Collectors.toList()));
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public void resetStrings(final List<String> stringIds) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.UPDATE_DEVICE_MANAGEMENT_RESOURCES"));
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda133
            public final void runOrThrow() {
                DevicePolicyManagerService.this.m3063x65d11498(stringIds);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$resetStrings$159$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m3063x65d11498(List stringIds) throws Exception {
        if (this.mDeviceManagementResourcesProvider.removeStrings(stringIds)) {
            sendStringsUpdatedBroadcast(stringIds);
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public ParcelableResource getString(final String stringId) {
        return (ParcelableResource) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda165
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3010x873bd08(stringId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getString$160$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ ParcelableResource m3010x873bd08(String stringId) throws Exception {
        return this.mDeviceManagementResourcesProvider.getString(stringId);
    }

    private void sendStringsUpdatedBroadcast(List<String> stringIds) {
        sendResourceUpdatedBroadcast(2, stringIds);
    }

    private void sendResourceUpdatedBroadcast(int resourceType, List<String> resourceIds) {
        Intent intent = new Intent("android.app.action.DEVICE_POLICY_RESOURCE_UPDATED");
        intent.putExtra("android.app.extra.RESOURCE_IDS", (String[]) resourceIds.toArray(new IntFunction() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda160
            @Override // java.util.function.IntFunction
            public final Object apply(int i) {
                return DevicePolicyManagerService.lambda$sendResourceUpdatedBroadcast$161(i);
            }
        }));
        intent.putExtra("android.app.extra.RESOURCE_TYPE", resourceType);
        intent.setFlags(268435456);
        intent.setFlags(1073741824);
        List<UserInfo> users = this.mUserManager.getAliveUsers();
        for (int i = 0; i < users.size(); i++) {
            UserHandle user = users.get(i).getUserHandle();
            this.mContext.sendBroadcastAsUser(intent, user);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$sendResourceUpdatedBroadcast$161(int x$0) {
        return new String[x$0];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getUpdatableString(String updatableStringId, final int defaultStringId, final Object... formatArgs) {
        ParcelableResource resource = this.mDeviceManagementResourcesProvider.getString(updatableStringId);
        if (resource == null) {
            return ParcelableResource.loadDefaultString(new Supplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda44
                @Override // java.util.function.Supplier
                public final Object get() {
                    return DevicePolicyManagerService.this.m3011x3d51b5c2(defaultStringId, formatArgs);
                }
            });
        }
        return resource.getString(this.mContext, new Supplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda45
            @Override // java.util.function.Supplier
            public final Object get() {
                return DevicePolicyManagerService.this.m3012x43558121(defaultStringId, formatArgs);
            }
        }, formatArgs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getUpdatableString$162$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ String m3011x3d51b5c2(int defaultStringId, Object[] formatArgs) {
        return this.mContext.getString(defaultStringId, formatArgs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getUpdatableString$163$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ String m3012x43558121(int defaultStringId, Object[] formatArgs) {
        return this.mContext.getString(defaultStringId, formatArgs);
    }

    public boolean isDpcDownloaded() {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        final ContentResolver cr = this.mContext.getContentResolver();
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda84
            public final Object getOrThrow() {
                Boolean valueOf;
                ContentResolver contentResolver = cr;
                valueOf = Boolean.valueOf(Settings.Secure.getIntForUser(cr, "managed_provisioning_dpc_downloaded", 0, cr.getUserId()) == 1);
                return valueOf;
            }
        })).booleanValue();
    }

    public void setDpcDownloaded(boolean downloaded) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        Injector injector = this.mInjector;
        final int i = downloaded ? 1 : 0;
        injector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda97
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3080x34b69494(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDpcDownloaded$165$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3080x34b69494(int setTo) throws Exception {
        return Boolean.valueOf(Settings.Secure.putInt(this.mContext.getContentResolver(), "managed_provisioning_dpc_downloaded", setTo));
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public boolean shouldAllowBypassingDevicePolicyManagementRoleQualification() {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_ROLE_HOLDERS"));
        return ((Boolean) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda55
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3112x42b659bf();
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$shouldAllowBypassingDevicePolicyManagementRoleQualification$166$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m3112x42b659bf() throws Exception {
        if (m3013x8b75536b(0).mBypassDevicePolicyManagementRoleQualifications) {
            return true;
        }
        return Boolean.valueOf(shouldAllowBypassingDevicePolicyManagementRoleQualificationInternal());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldAllowBypassingDevicePolicyManagementRoleQualificationInternal() {
        if (this.mUserManager.getUserCount() > 1) {
            return false;
        }
        AccountManager am = AccountManager.get(this.mContext);
        Account[] accounts = am.getAccounts();
        return accounts.length == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setBypassDevicePolicyManagementRoleQualificationStateInternal(String currentRoleHolder, boolean allowBypass) {
        boolean stateChanged = false;
        DevicePolicyData policy = m3013x8b75536b(0);
        if (policy.mBypassDevicePolicyManagementRoleQualifications != allowBypass) {
            policy.mBypassDevicePolicyManagementRoleQualifications = allowBypass;
            stateChanged = true;
        }
        if (!Objects.equals(currentRoleHolder, policy.mCurrentRoleHolder)) {
            policy.mCurrentRoleHolder = currentRoleHolder;
            stateChanged = true;
        }
        if (stateChanged) {
            synchronized (getLockObject()) {
                saveSettingsLocked(0);
            }
        }
    }

    /* loaded from: classes.dex */
    private final class DevicePolicyManagementRoleObserver implements OnRoleHoldersChangedListener {
        private final Context mContext;
        private final Executor mExecutor;
        private RoleManager mRm;

        DevicePolicyManagementRoleObserver(Context context) {
            this.mContext = context;
            this.mExecutor = context.getMainExecutor();
            this.mRm = (RoleManager) context.getSystemService(RoleManager.class);
        }

        public void register() {
            this.mRm.addOnRoleHoldersChangedListenerAsUser(this.mExecutor, this, UserHandle.SYSTEM);
        }

        public void onRoleHoldersChanged(String roleName, UserHandle user) {
            if (!"android.app.role.DEVICE_POLICY_MANAGEMENT".equals(roleName)) {
                return;
            }
            String newRoleHolder = getRoleHolder();
            if (isDefaultRoleHolder(newRoleHolder)) {
                Slogf.i(DevicePolicyManagerService.LOG_TAG, "onRoleHoldersChanged: Default role holder is set, returning early");
            } else if (newRoleHolder == null) {
                Slogf.i(DevicePolicyManagerService.LOG_TAG, "onRoleHoldersChanged: New role holder is null, returning early");
            } else if (DevicePolicyManagerService.this.shouldAllowBypassingDevicePolicyManagementRoleQualificationInternal()) {
                Slogf.w(DevicePolicyManagerService.LOG_TAG, "onRoleHoldersChanged: Updating current role holder to " + newRoleHolder);
                DevicePolicyManagerService.this.setBypassDevicePolicyManagementRoleQualificationStateInternal(newRoleHolder, true);
            } else {
                DevicePolicyData policy = DevicePolicyManagerService.this.m3013x8b75536b(0);
                if (!newRoleHolder.equals(policy.mCurrentRoleHolder)) {
                    Slogf.w(DevicePolicyManagerService.LOG_TAG, "onRoleHoldersChanged: You can't set a different role holder, role is getting revoked from " + newRoleHolder);
                    DevicePolicyManagerService.this.setBypassDevicePolicyManagementRoleQualificationStateInternal(null, false);
                    this.mRm.removeRoleHolderAsUser("android.app.role.DEVICE_POLICY_MANAGEMENT", newRoleHolder, 0, user, this.mExecutor, new Consumer() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$DevicePolicyManagementRoleObserver$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            DevicePolicyManagerService.DevicePolicyManagementRoleObserver.lambda$onRoleHoldersChanged$0((Boolean) obj);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onRoleHoldersChanged$0(Boolean successful) {
        }

        private String getRoleHolder() {
            return DevicePolicyManagerService.this.getDevicePolicyManagementRoleHolderPackageName(this.mContext);
        }

        private boolean isDefaultRoleHolder(String packageName) {
            String defaultRoleHolder = getDefaultRoleHolderPackageName();
            if (packageName == null || defaultRoleHolder == null || !defaultRoleHolder.equals(packageName)) {
                return false;
            }
            return hasSigningCertificate(packageName, getDefaultRoleHolderPackageSignature());
        }

        private boolean hasSigningCertificate(String packageName, String certificateString) {
            if (packageName == null || certificateString == null) {
                return false;
            }
            try {
                byte[] certificate = new Signature(certificateString).toByteArray();
                PackageManager pm = DevicePolicyManagerService.this.mInjector.getPackageManager();
                return pm.hasSigningCertificate(packageName, certificate, 1);
            } catch (IllegalArgumentException e) {
                Slogf.w(DevicePolicyManagerService.LOG_TAG, "Cannot parse signing certificate: " + certificateString, e);
                return false;
            }
        }

        private String getDefaultRoleHolderPackageName() {
            String[] info = getDefaultRoleHolderPackageNameAndSignature();
            if (info == null) {
                return null;
            }
            return info[0];
        }

        private String getDefaultRoleHolderPackageSignature() {
            String[] info = getDefaultRoleHolderPackageNameAndSignature();
            if (info == null || info.length < 2) {
                return null;
            }
            return info[1];
        }

        private String[] getDefaultRoleHolderPackageNameAndSignature() {
            String packageNameAndSignature = this.mContext.getString(17039421);
            if (TextUtils.isEmpty(packageNameAndSignature)) {
                return null;
            }
            return packageNameAndSignature.contains(":") ? packageNameAndSignature.split(":") : new String[]{packageNameAndSignature};
        }
    }

    @Override // com.android.server.devicepolicy.BaseIDevicePolicyManager
    public List<UserHandle> getPolicyManagedProfiles(UserHandle user) {
        Preconditions.checkCallAuthorization(hasCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS"));
        final int userId = user.getIdentifier();
        return (List) this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.DevicePolicyManagerService$$ExternalSyntheticLambda29
            public final Object getOrThrow() {
                return DevicePolicyManagerService.this.m3006x4fb8feed(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getPolicyManagedProfiles$167$com-android-server-devicepolicy-DevicePolicyManagerService  reason: not valid java name */
    public /* synthetic */ List m3006x4fb8feed(int userId) throws Exception {
        List<UserInfo> userProfiles = this.mUserManager.getProfiles(userId);
        List<UserHandle> result = new ArrayList<>();
        for (int i = 0; i < userProfiles.size(); i++) {
            UserInfo userInfo = userProfiles.get(i);
            if (userInfo.isManagedProfile() && hasProfileOwner(userInfo.id)) {
                result.add(new UserHandle(userInfo.id));
            }
        }
        return result;
    }

    private static CharSequence truncateIfLonger(CharSequence input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.subSequence(0, maxLength);
    }

    private static void enforceMaxStringLength(String str, String argName) {
        Preconditions.checkArgument(str.length() <= 65535, argName + " loo long");
    }

    private static void enforceMaxPackageNameLength(String pkg) {
        Preconditions.checkArgument(pkg.length() <= 223, "Package name too long");
    }

    private static void enforceMaxStringLength(PersistableBundle bundle, String argName) {
        String[] strArr;
        Queue<PersistableBundle> queue = new ArrayDeque<>();
        queue.add(bundle);
        while (!queue.isEmpty()) {
            PersistableBundle current = queue.remove();
            for (String key : current.keySet()) {
                enforceMaxStringLength(key, "key in " + argName);
                Object value = current.get(key);
                if (value instanceof String) {
                    enforceMaxStringLength((String) value, "string value in " + argName);
                } else if (value instanceof String[]) {
                    for (String str : (String[]) value) {
                        enforceMaxStringLength(str, "string value in " + argName);
                    }
                } else if (value instanceof PersistableBundle) {
                    queue.add((PersistableBundle) value);
                }
            }
        }
    }
}
