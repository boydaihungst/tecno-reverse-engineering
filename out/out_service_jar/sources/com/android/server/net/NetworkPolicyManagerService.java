package com.android.server.net;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.ConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.INetworkPolicyListener;
import android.net.INetworkPolicyManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkIdentity;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkRequest;
import android.net.NetworkStack;
import android.net.NetworkStateSnapshot;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BestClock;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.MessageQueue;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.PowerWhitelistManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.DataUnit;
import android.util.IntArray;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.RecurrenceRule;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.SparseSetArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.StatLogger;
import com.android.internal.util.XmlUtils;
import com.android.net.module.util.NetworkIdentityUtils;
import com.android.net.module.util.PermissionUtils;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.am.HostingRecord;
import com.android.server.connectivity.MultipathPolicyTracker;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.usage.AppStandbyInternal;
import com.transsion.hubcore.server.net.ITranNetworkPolicyManagerService;
import dalvik.annotation.optimization.NeverCompile;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class NetworkPolicyManagerService extends INetworkPolicyManager.Stub {
    private static final String ACTION_SNOOZE_RAPID = "com.android.server.net.action.SNOOZE_RAPID";
    private static final String ACTION_SNOOZE_WARNING = "com.android.server.net.action.SNOOZE_WARNING";
    private static final String ATTR_APP_ID = "appId";
    @Deprecated
    private static final String ATTR_CYCLE_DAY = "cycleDay";
    private static final String ATTR_CYCLE_END = "cycleEnd";
    private static final String ATTR_CYCLE_PERIOD = "cyclePeriod";
    private static final String ATTR_CYCLE_START = "cycleStart";
    @Deprecated
    private static final String ATTR_CYCLE_TIMEZONE = "cycleTimezone";
    private static final String ATTR_INFERRED = "inferred";
    private static final String ATTR_LAST_LIMIT_SNOOZE = "lastLimitSnooze";
    private static final String ATTR_LAST_SNOOZE = "lastSnooze";
    private static final String ATTR_LAST_WARNING_SNOOZE = "lastWarningSnooze";
    private static final String ATTR_LIMIT_BEHAVIOR = "limitBehavior";
    private static final String ATTR_LIMIT_BYTES = "limitBytes";
    private static final String ATTR_METERED = "metered";
    private static final String ATTR_NETWORK_ID = "networkId";
    private static final String ATTR_NETWORK_TEMPLATE = "networkTemplate";
    private static final String ATTR_NETWORK_TYPES = "networkTypes";
    private static final String ATTR_OWNER_PACKAGE = "ownerPackage";
    private static final String ATTR_POLICY = "policy";
    private static final String ATTR_RESTRICT_BACKGROUND = "restrictBackground";
    private static final String ATTR_SUBSCRIBER_ID = "subscriberId";
    private static final String ATTR_SUBSCRIBER_ID_MATCH_RULE = "subscriberIdMatchRule";
    private static final String ATTR_SUB_ID = "subId";
    private static final String ATTR_SUMMARY = "summary";
    private static final String ATTR_TEMPLATE_METERED = "templateMetered";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_UID = "uid";
    private static final String ATTR_USAGE_BYTES = "usageBytes";
    private static final String ATTR_USAGE_TIME = "usageTime";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_WARNING_BYTES = "warningBytes";
    private static final String ATTR_XML_UTILS_NAME = "name";
    private static final int CHAIN_TOGGLE_DISABLE = 2;
    private static final int CHAIN_TOGGLE_ENABLE = 1;
    private static final int CHAIN_TOGGLE_NONE = 0;
    private static final int MSG_ADVISE_PERSIST_THRESHOLD = 7;
    private static final int MSG_CLEAR_SUBSCRIPTION_PLANS = 22;
    private static final int MSG_LIMIT_REACHED = 5;
    private static final int MSG_METERED_IFACES_CHANGED = 2;
    private static final int MSG_METERED_RESTRICTED_PACKAGES_CHANGED = 17;
    private static final int MSG_POLICIES_CHANGED = 13;
    private static final int MSG_REMOVE_INTERFACE_QUOTAS = 11;
    private static final int MSG_RESET_FIREWALL_RULES_BY_UID = 15;
    private static final int MSG_RESTRICT_BACKGROUND_CHANGED = 6;
    private static final int MSG_RULES_CHANGED = 1;
    private static final int MSG_SET_NETWORK_TEMPLATE_ENABLED = 18;
    private static final int MSG_STATS_PROVIDER_WARNING_OR_LIMIT_REACHED = 20;
    private static final int MSG_SUBSCRIPTION_OVERRIDE = 16;
    private static final int MSG_SUBSCRIPTION_PLANS_CHANGED = 19;
    private static final int MSG_UIDS_BLOCKED_REASONS_CHANGED = 23;
    private static final int MSG_UID_BLOCKED_REASON_CHANGED = 21;
    private static final int MSG_UPDATE_INTERFACE_QUOTAS = 10;
    public static final int OPPORTUNISTIC_QUOTA_UNKNOWN = -1;
    private static final String PROP_SUB_PLAN_OWNER = "persist.sys.sub_plan_owner";
    private static final float QUOTA_FRAC_JOBS_DEFAULT = 0.5f;
    private static final float QUOTA_FRAC_MULTIPATH_DEFAULT = 0.5f;
    private static final float QUOTA_LIMITED_DEFAULT = 0.1f;
    static final String TAG = "NetworkPolicy";
    private static final String TAG_APP_POLICY = "app-policy";
    private static final String TAG_NETWORK_POLICY = "network-policy";
    private static final String TAG_POLICY_LIST = "policy-list";
    private static final String TAG_RESTRICT_BACKGROUND = "restrict-background";
    private static final String TAG_REVOKED_RESTRICT_BACKGROUND = "revoked-restrict-background";
    private static final String TAG_UID_POLICY = "uid-policy";
    private static final String TAG_WHITELIST = "whitelist";
    private static final String TAG_XML_UTILS_INT_ARRAY = "int-array";
    public static final int TYPE_LIMIT = 35;
    public static final int TYPE_LIMIT_SNOOZED = 36;
    public static final int TYPE_RAPID = 45;
    public static final int TYPE_WARNING = 34;
    private static final int UID_MSG_GONE = 101;
    private static final int UID_MSG_STATE_CHANGED = 100;
    private static final int VERSION_ADDED_CYCLE = 11;
    private static final int VERSION_ADDED_INFERRED = 7;
    private static final int VERSION_ADDED_METERED = 4;
    private static final int VERSION_ADDED_NETWORK_ID = 9;
    private static final int VERSION_ADDED_NETWORK_TYPES = 12;
    private static final int VERSION_ADDED_RESTRICT_BACKGROUND = 3;
    private static final int VERSION_ADDED_SNOOZE = 2;
    private static final int VERSION_ADDED_TIMEZONE = 6;
    private static final int VERSION_INIT = 1;
    private static final int VERSION_LATEST = 14;
    private static final int VERSION_REMOVED_SUBSCRIPTION_PLANS = 14;
    private static final int VERSION_SPLIT_SNOOZE = 5;
    private static final int VERSION_SUPPORTED_CARRIER_USAGE = 13;
    private static final int VERSION_SWITCH_APP_ID = 8;
    private static final int VERSION_SWITCH_UID = 10;
    private static final long WAIT_FOR_ADMIN_DATA_TIMEOUT_MS = 10000;
    private final ArraySet<NotificationId> mActiveNotifs;
    private final IActivityManager mActivityManager;
    private ActivityManagerInternal mActivityManagerInternal;
    private final CountDownLatch mAdminDataAvailableLatch;
    private final INetworkManagementEventObserver mAlertObserver;
    private final SparseBooleanArray mAppIdleTempWhitelistAppIds;
    private final AppOpsManager mAppOps;
    private AppStandbyInternal mAppStandby;
    private final CarrierConfigManager mCarrierConfigManager;
    private BroadcastReceiver mCarrierConfigReceiver;
    private final Clock mClock;
    private ConnectivityManager mConnManager;
    private BroadcastReceiver mConnReceiver;
    private final Context mContext;
    private final SparseBooleanArray mDefaultRestrictBackgroundAllowlistUids;
    private final Dependencies mDeps;
    volatile boolean mDeviceIdleMode;
    final SparseBooleanArray mFirewallChainStates;
    final Handler mHandler;
    private final Handler.Callback mHandlerCallback;
    private final IPackageManager mIPm;
    private final SparseBooleanArray mInternetPermissionMap;
    private final RemoteCallbackList<INetworkPolicyListener> mListeners;
    private boolean mLoadedRestrictBackground;
    private final NetworkPolicyLogger mLogger;
    volatile boolean mLowPowerStandbyActive;
    private final SparseBooleanArray mLowPowerStandbyAllowlistUids;
    private List<String[]> mMergedSubscriberIds;
    private ArraySet<String> mMeteredIfaces;
    final Object mMeteredIfacesLock;
    private final SparseArray<Set<Integer>> mMeteredRestrictedUids;
    private final MultipathPolicyTracker mMultipathPolicyTracker;
    private final SparseIntArray mNetIdToSubId;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;
    private final INetworkManagementService mNetworkManager;
    private volatile boolean mNetworkManagerReady;
    private final SparseBooleanArray mNetworkMetered;
    final Object mNetworkPoliciesSecondLock;
    final ArrayMap<NetworkTemplate, NetworkPolicy> mNetworkPolicy;
    private final SparseBooleanArray mNetworkRoaming;
    private NetworkStatsManager mNetworkStats;
    private SparseSetArray<String> mNetworkToIfaces;
    private final ArraySet<NetworkTemplate> mOverLimitNotified;
    private final BroadcastReceiver mPackageReceiver;
    private final AtomicFile mPolicyFile;
    private PowerManagerInternal mPowerManagerInternal;
    private final SparseBooleanArray mPowerSaveTempWhitelistAppIds;
    private final SparseBooleanArray mPowerSaveWhitelistAppIds;
    private final SparseBooleanArray mPowerSaveWhitelistExceptIdleAppIds;
    private final BroadcastReceiver mPowerSaveWhitelistReceiver;
    private PowerWhitelistManager mPowerWhitelistManager;
    volatile boolean mRestrictBackground;
    private final SparseBooleanArray mRestrictBackgroundAllowlistRevokedUids;
    private boolean mRestrictBackgroundBeforeBsm;
    volatile boolean mRestrictBackgroundChangedInBsm;
    private boolean mRestrictBackgroundLowPowerMode;
    volatile boolean mRestrictPower;
    private RestrictedModeObserver mRestrictedModeObserver;
    volatile boolean mRestrictedNetworkingMode;
    int mSetSubscriptionPlansIdCounter;
    final SparseIntArray mSetSubscriptionPlansIds;
    private BroadcastReceiver mSimStateChangedReceiver;
    private final BroadcastReceiver mSnoozeReceiver;
    public final StatLogger mStatLogger;
    private final StatsCallback mStatsCallback;
    private final SparseArray<PersistableBundle> mSubIdToCarrierConfig;
    private final SparseArray<String> mSubIdToSubscriberId;
    final SparseLongArray mSubscriptionOpportunisticQuota;
    final SparseArray<SubscriptionPlan[]> mSubscriptionPlans;
    final SparseArray<String> mSubscriptionPlansOwner;
    private final boolean mSuppressDefaultPolicy;
    volatile boolean mSystemReady;
    private final SparseArray<UidBlockedState> mTmpUidBlockedState;
    private final SparseArray<UidBlockedState> mUidBlockedState;
    final Handler mUidEventHandler;
    private final Handler.Callback mUidEventHandlerCallback;
    private final ServiceThread mUidEventThread;
    final SparseIntArray mUidFirewallDozableRules;
    final SparseIntArray mUidFirewallLowPowerStandbyModeRules;
    final SparseIntArray mUidFirewallPowerSaveRules;
    final SparseIntArray mUidFirewallRestrictedModeRules;
    final SparseIntArray mUidFirewallStandbyRules;
    private final IUidObserver mUidObserver;
    final SparseIntArray mUidPolicy;
    private final BroadcastReceiver mUidRemovedReceiver;
    final Object mUidRulesFirstLock;
    private final SparseArray<NetworkPolicyManager.UidState> mUidState;
    private final SparseArray<UidStateCallbackInfo> mUidStateCallbackInfos;
    private UsageStatsManagerInternal mUsageStats;
    private final UserManager mUserManager;
    private final BroadcastReceiver mUserReceiver;
    private final BroadcastReceiver mWifiReceiver;
    private static final boolean LOGD = NetworkPolicyLogger.LOGD;
    private static final boolean LOGV = NetworkPolicyLogger.LOGV;
    private static final long QUOTA_UNLIMITED_DEFAULT = DataUnit.MEBIBYTES.toBytes(20);

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ChainToggleType {
    }

    /* loaded from: classes2.dex */
    interface Stats {
        public static final int COUNT = 2;
        public static final int IS_UID_NETWORKING_BLOCKED = 1;
        public static final int UPDATE_NETWORK_ENABLED = 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class RestrictedModeObserver extends ContentObserver {
        private final Context mContext;
        private final RestrictedModeListener mListener;

        /* loaded from: classes2.dex */
        public interface RestrictedModeListener {
            void onChange(boolean z);
        }

        RestrictedModeObserver(Context ctx, RestrictedModeListener listener) {
            super(null);
            this.mContext = ctx;
            this.mListener = listener;
            ctx.getContentResolver().registerContentObserver(Settings.Global.getUriFor("restricted_networking_mode"), false, this);
        }

        public boolean isRestrictedModeEnabled() {
            return Settings.Global.getInt(this.mContext.getContentResolver(), "restricted_networking_mode", 0) != 0;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            this.mListener.onChange(isRestrictedModeEnabled());
        }
    }

    public NetworkPolicyManagerService(Context context, IActivityManager activityManager, INetworkManagementService networkManagement) {
        this(context, activityManager, networkManagement, AppGlobals.getPackageManager(), getDefaultClock(), getDefaultSystemDir(), false, new Dependencies(context));
    }

    private static File getDefaultSystemDir() {
        return new File(Environment.getDataDirectory(), HostingRecord.HOSTING_TYPE_SYSTEM);
    }

    private static Clock getDefaultClock() {
        return new BestClock(ZoneOffset.UTC, new Clock[]{SystemClock.currentNetworkTimeClock(), Clock.systemUTC()});
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Dependencies {
        final Context mContext;
        final NetworkStatsManager mNetworkStatsManager;

        Dependencies(Context context) {
            this.mContext = context;
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(NetworkStatsManager.class);
            this.mNetworkStatsManager = networkStatsManager;
            networkStatsManager.setPollOnOpen(false);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [786=4] */
        long getNetworkTotalBytes(NetworkTemplate template, long start, long end) {
            Trace.traceBegin(2097152L, "getNetworkTotalBytes");
            try {
                NetworkStats.Bucket ret = this.mNetworkStatsManager.querySummaryForDevice(template, start, end);
                return ret.getRxBytes() + ret.getTxBytes();
            } catch (RuntimeException e) {
                Slog.w(NetworkPolicyManagerService.TAG, "Failed to read network stats: " + e);
                return 0L;
            } finally {
                Trace.traceEnd(2097152L);
            }
        }

        List<NetworkStats.Bucket> getNetworkUidBytes(NetworkTemplate template, long start, long end) {
            Trace.traceBegin(2097152L, "getNetworkUidBytes");
            List<NetworkStats.Bucket> buckets = new ArrayList<>();
            try {
                try {
                    NetworkStats stats = this.mNetworkStatsManager.querySummary(template, start, end);
                    while (stats.hasNextBucket()) {
                        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
                        stats.getNextBucket(bucket);
                        buckets.add(bucket);
                    }
                } catch (RuntimeException e) {
                    Slog.w(NetworkPolicyManagerService.TAG, "Failed to read network stats: " + e);
                }
                return buckets;
            } finally {
                Trace.traceEnd(2097152L);
            }
        }
    }

    public NetworkPolicyManagerService(Context context, IActivityManager activityManager, INetworkManagementService networkManagement, IPackageManager pm, Clock clock, File systemDir, boolean suppressDefaultPolicy, Dependencies deps) {
        this.mUidRulesFirstLock = new Object();
        this.mNetworkPoliciesSecondLock = new Object();
        this.mAdminDataAvailableLatch = new CountDownLatch(1);
        this.mNetworkPolicy = new ArrayMap<>();
        this.mSubscriptionPlans = new SparseArray<>();
        this.mSubscriptionPlansOwner = new SparseArray<>();
        this.mSetSubscriptionPlansIds = new SparseIntArray();
        this.mSetSubscriptionPlansIdCounter = 0;
        this.mSubscriptionOpportunisticQuota = new SparseLongArray();
        this.mUidPolicy = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mUidFirewallRestrictedModeRules = new SparseIntArray();
        this.mUidFirewallLowPowerStandbyModeRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mPowerSaveWhitelistExceptIdleAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistAppIds = new SparseBooleanArray();
        this.mPowerSaveTempWhitelistAppIds = new SparseBooleanArray();
        this.mLowPowerStandbyAllowlistUids = new SparseBooleanArray();
        this.mAppIdleTempWhitelistAppIds = new SparseBooleanArray();
        this.mDefaultRestrictBackgroundAllowlistUids = new SparseBooleanArray();
        this.mRestrictBackgroundAllowlistRevokedUids = new SparseBooleanArray();
        this.mMeteredIfacesLock = new Object();
        this.mMeteredIfaces = new ArraySet<>();
        this.mOverLimitNotified = new ArraySet<>();
        this.mActiveNotifs = new ArraySet<>();
        this.mUidState = new SparseArray<>();
        this.mUidBlockedState = new SparseArray<>();
        this.mTmpUidBlockedState = new SparseArray<>();
        this.mNetworkMetered = new SparseBooleanArray();
        this.mNetworkRoaming = new SparseBooleanArray();
        this.mNetworkToIfaces = new SparseSetArray<>();
        this.mNetIdToSubId = new SparseIntArray();
        this.mSubIdToSubscriberId = new SparseArray<>();
        this.mMergedSubscriberIds = new ArrayList();
        this.mSubIdToCarrierConfig = new SparseArray<>();
        this.mMeteredRestrictedUids = new SparseArray<>();
        this.mListeners = new RemoteCallbackList<>();
        this.mLogger = new NetworkPolicyLogger();
        this.mInternetPermissionMap = new SparseBooleanArray();
        this.mUidStateCallbackInfos = new SparseArray<>();
        this.mStatLogger = new StatLogger(new String[]{"updateNetworkEnabledNL()", "isUidNetworkingBlocked()"});
        this.mUidObserver = new IUidObserver.Stub() { // from class: com.android.server.net.NetworkPolicyManagerService.4
            public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
                synchronized (NetworkPolicyManagerService.this.mUidStateCallbackInfos) {
                    UidStateCallbackInfo callbackInfo = (UidStateCallbackInfo) NetworkPolicyManagerService.this.mUidStateCallbackInfos.get(uid);
                    if (callbackInfo == null) {
                        callbackInfo = new UidStateCallbackInfo();
                        NetworkPolicyManagerService.this.mUidStateCallbackInfos.put(uid, callbackInfo);
                    }
                    if (callbackInfo.procStateSeq == -1 || procStateSeq > callbackInfo.procStateSeq) {
                        callbackInfo.update(uid, procState, procStateSeq, capability);
                    }
                    if (!callbackInfo.isPending) {
                        NetworkPolicyManagerService.this.mUidEventHandler.obtainMessage(100, callbackInfo).sendToTarget();
                    }
                }
            }

            public void onUidGone(int uid, boolean disabled) {
                NetworkPolicyManagerService.this.mUidEventHandler.obtainMessage(101, uid, 0).sendToTarget();
            }

            public void onUidActive(int uid) {
            }

            public void onUidIdle(int uid, boolean disabled) {
            }

            public void onUidCachedChanged(int uid, boolean cached) {
            }

            public void onUidProcAdjChanged(int uid) {
            }
        };
        this.mPowerSaveWhitelistReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkPolicyManagerService.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    NetworkPolicyManagerService.this.updatePowerSaveWhitelistUL();
                    NetworkPolicyManagerService.this.updateRulesForRestrictPowerUL();
                    NetworkPolicyManagerService.this.updateRulesForAppIdleUL();
                }
            }
        };
        this.mPackageReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkPolicyManagerService.6
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (uid != -1 && "android.intent.action.PACKAGE_ADDED".equals(action)) {
                    if (NetworkPolicyManagerService.LOGV) {
                        Slog.v(NetworkPolicyManagerService.TAG, "ACTION_PACKAGE_ADDED for uid=" + uid);
                    }
                    synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                        NetworkPolicyManagerService.this.mInternetPermissionMap.delete(uid);
                        NetworkPolicyManagerService.this.updateRestrictionRulesForUidUL(uid);
                    }
                }
            }
        };
        this.mUidRemovedReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkPolicyManagerService.7
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (uid == -1) {
                    return;
                }
                if (NetworkPolicyManagerService.LOGV) {
                    Slog.v(NetworkPolicyManagerService.TAG, "ACTION_UID_REMOVED for uid=" + uid);
                }
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    NetworkPolicyManagerService.this.onUidDeletedUL(uid);
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        NetworkPolicyManagerService.this.writePolicyAL();
                    }
                }
            }
        };
        this.mUserReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkPolicyManagerService.8
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                char c = 65535;
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userId == -1) {
                    return;
                }
                switch (action.hashCode()) {
                    case -2061058799:
                        if (action.equals("android.intent.action.USER_REMOVED")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1121780209:
                        if (action.equals("android.intent.action.USER_ADDED")) {
                            c = 1;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                        synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                            NetworkPolicyManagerService.this.removeUserStateUL(userId, true, false);
                            NetworkPolicyManagerService.this.mMeteredRestrictedUids.remove(userId);
                            if (action == "android.intent.action.USER_ADDED") {
                                NetworkPolicyManagerService.this.addDefaultRestrictBackgroundAllowlistUidsUL(userId);
                            }
                            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                                NetworkPolicyManagerService.this.updateRulesForGlobalChangeAL(true);
                            }
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mStatsCallback = new StatsCallback();
        this.mSnoozeReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkPolicyManagerService.9
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                NetworkTemplate template = intent.getParcelableExtra("android.net.NETWORK_TEMPLATE");
                if (NetworkPolicyManagerService.ACTION_SNOOZE_WARNING.equals(intent.getAction())) {
                    NetworkPolicyManagerService.this.performSnooze(template, 34);
                } else if (NetworkPolicyManagerService.ACTION_SNOOZE_RAPID.equals(intent.getAction())) {
                    NetworkPolicyManagerService.this.performSnooze(template, 45);
                }
            }
        };
        this.mWifiReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkPolicyManagerService.10
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                NetworkPolicyManagerService.this.upgradeWifiMeteredOverride();
                NetworkPolicyManagerService.this.mContext.unregisterReceiver(this);
            }
        };
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.android.server.net.NetworkPolicyManagerService.11
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                    boolean newRoaming = true;
                    boolean newMetered = !networkCapabilities.hasCapability(11);
                    boolean meteredChanged = NetworkPolicyManagerService.updateCapabilityChange(NetworkPolicyManagerService.this.mNetworkMetered, newMetered, network);
                    if (networkCapabilities.hasCapability(18)) {
                        newRoaming = false;
                    }
                    boolean roamingChanged = NetworkPolicyManagerService.updateCapabilityChange(NetworkPolicyManagerService.this.mNetworkRoaming, newRoaming, network);
                    if (meteredChanged || roamingChanged) {
                        NetworkPolicyManagerService.this.mLogger.meterednessChanged(network.getNetId(), newMetered);
                        NetworkPolicyManagerService.this.updateNetworkRulesNL();
                    }
                }
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLinkPropertiesChanged(Network network, LinkProperties lp) {
                synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                    ArraySet<String> newIfaces = new ArraySet<>(lp.getAllInterfaceNames());
                    boolean ifacesChanged = NetworkPolicyManagerService.this.updateNetworkToIfacesNL(network.getNetId(), newIfaces);
                    if (ifacesChanged) {
                        NetworkPolicyManagerService.this.updateNetworkRulesNL();
                    }
                }
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                    NetworkPolicyManagerService.this.mNetworkToIfaces.remove(network.getNetId());
                }
            }
        };
        this.mAlertObserver = new BaseNetworkObserver() { // from class: com.android.server.net.NetworkPolicyManagerService.12
            public void limitReached(String limitName, String iface) {
                NetworkStack.checkNetworkStackPermission(NetworkPolicyManagerService.this.mContext);
                if (!"globalAlert".equals(limitName)) {
                    NetworkPolicyManagerService.this.mHandler.obtainMessage(5, iface).sendToTarget();
                }
            }
        };
        this.mConnReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkPolicyManagerService.13
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                NetworkPolicyManagerService.this.updateNetworksInternal();
            }
        };
        this.mCarrierConfigReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkPolicyManagerService.14
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (!intent.hasExtra("android.telephony.extra.SUBSCRIPTION_INDEX")) {
                    return;
                }
                int subId = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1);
                NetworkPolicyManagerService.this.updateSubscriptions();
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        String subscriberId = (String) NetworkPolicyManagerService.this.mSubIdToSubscriberId.get(subId, null);
                        if (subscriberId != null) {
                            NetworkPolicyManagerService.this.ensureActiveCarrierPolicyAL(subId, subscriberId);
                            NetworkPolicyManagerService.this.maybeUpdateCarrierPolicyCycleAL(subId, subscriberId);
                        } else {
                            Slog.wtf(NetworkPolicyManagerService.TAG, "Missing subscriberId for subId " + subId);
                        }
                        NetworkPolicyManagerService.this.handleNetworkPoliciesUpdateAL(true);
                    }
                }
            }
        };
        this.mSimStateChangedReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkPolicyManagerService.15
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String stateExtra = intent.getStringExtra("ss");
                if ("LOADED".equals(stateExtra)) {
                    Slog.i(NetworkPolicyManagerService.TAG, "INTENT_VALUE_ICC_LOADED");
                    NetworkPolicyManagerService.this.updateSubscriptions();
                }
            }
        };
        Handler.Callback callback = new Handler.Callback() { // from class: com.android.server.net.NetworkPolicyManagerService.16
            @Override // android.os.Handler.Callback
            public boolean handleMessage(Message msg) {
                boolean enabled;
                switch (msg.what) {
                    case 1:
                        int uid = msg.arg1;
                        int uidRules = msg.arg2;
                        if (NetworkPolicyManagerService.LOGV) {
                            Slog.v(NetworkPolicyManagerService.TAG, "Dispatching rules=" + NetworkPolicyManager.uidRulesToString(uidRules) + " for uid=" + uid);
                        }
                        int length = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i = 0; i < length; i++) {
                            INetworkPolicyListener listener = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i);
                            NetworkPolicyManagerService.this.dispatchUidRulesChanged(listener, uid, uidRules);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 2:
                        String[] meteredIfaces = (String[]) msg.obj;
                        int length2 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i2 = 0; i2 < length2; i2++) {
                            INetworkPolicyListener listener2 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i2);
                            NetworkPolicyManagerService.this.dispatchMeteredIfacesChanged(listener2, meteredIfaces);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 3:
                    case 4:
                    case 8:
                    case 9:
                    case 12:
                    case 14:
                    default:
                        return false;
                    case 5:
                        String iface = (String) msg.obj;
                        synchronized (NetworkPolicyManagerService.this.mMeteredIfacesLock) {
                            if (NetworkPolicyManagerService.this.mMeteredIfaces.contains(iface)) {
                                NetworkPolicyManagerService.this.mNetworkStats.forceUpdate();
                                synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                                    NetworkPolicyManagerService.this.updateNetworkRulesNL();
                                    NetworkPolicyManagerService.this.updateNetworkEnabledNL();
                                    NetworkPolicyManagerService.this.updateNotificationsNL();
                                }
                                return true;
                            }
                            return true;
                        }
                    case 6:
                        enabled = msg.arg1 != 0;
                        boolean restrictBackground = enabled;
                        int length3 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i3 = 0; i3 < length3; i3++) {
                            INetworkPolicyListener listener3 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i3);
                            NetworkPolicyManagerService.this.dispatchRestrictBackgroundChanged(listener3, restrictBackground);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        Intent intent = new Intent("android.net.conn.RESTRICT_BACKGROUND_CHANGED");
                        intent.setFlags(1073741824);
                        NetworkPolicyManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                        return true;
                    case 7:
                        long lowestRule = ((Long) msg.obj).longValue();
                        long persistThreshold = lowestRule / 1000;
                        NetworkPolicyManagerService.this.mNetworkStats.setDefaultGlobalAlert(persistThreshold);
                        return true;
                    case 10:
                        IfaceQuotas val = (IfaceQuotas) msg.obj;
                        NetworkPolicyManagerService.this.removeInterfaceLimit(val.iface);
                        NetworkPolicyManagerService.this.setInterfaceLimit(val.iface, val.limit);
                        NetworkPolicyManagerService.this.mNetworkStats.setStatsProviderWarningAndLimitAsync(val.iface, val.warning, val.limit);
                        return true;
                    case 11:
                        String iface2 = (String) msg.obj;
                        NetworkPolicyManagerService.this.removeInterfaceLimit(iface2);
                        NetworkPolicyManagerService.this.mNetworkStats.setStatsProviderWarningAndLimitAsync(iface2, -1L, -1L);
                        return true;
                    case 13:
                        int uid2 = msg.arg1;
                        int policy = msg.arg2;
                        Boolean notifyApp = (Boolean) msg.obj;
                        int length4 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i4 = 0; i4 < length4; i4++) {
                            INetworkPolicyListener listener4 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i4);
                            NetworkPolicyManagerService.this.dispatchUidPoliciesChanged(listener4, uid2, policy);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        if (notifyApp.booleanValue()) {
                            NetworkPolicyManagerService.this.broadcastRestrictBackgroundChanged(uid2, notifyApp);
                            return true;
                        }
                        return true;
                    case 15:
                        NetworkPolicyManagerService.this.resetUidFirewallRules(msg.arg1);
                        return true;
                    case 16:
                        SomeArgs args = (SomeArgs) msg.obj;
                        int subId = ((Integer) args.arg1).intValue();
                        int overrideMask = ((Integer) args.arg2).intValue();
                        int overrideValue = ((Integer) args.arg3).intValue();
                        int[] networkTypes = (int[]) args.arg4;
                        int length5 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i5 = 0; i5 < length5; i5++) {
                            INetworkPolicyListener listener5 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i5);
                            NetworkPolicyManagerService.this.dispatchSubscriptionOverride(listener5, subId, overrideMask, overrideValue, networkTypes);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 17:
                        int userId = msg.arg1;
                        Set<String> packageNames = (Set) msg.obj;
                        NetworkPolicyManagerService.this.setMeteredRestrictedPackagesInternal(packageNames, userId);
                        return true;
                    case 18:
                        NetworkTemplate template = (NetworkTemplate) msg.obj;
                        enabled = msg.arg1 != 0;
                        NetworkPolicyManagerService.this.setNetworkTemplateEnabledInner(template, enabled);
                        return true;
                    case 19:
                        SubscriptionPlan[] plans = (SubscriptionPlan[]) msg.obj;
                        int subId2 = msg.arg1;
                        int length6 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i6 = 0; i6 < length6; i6++) {
                            INetworkPolicyListener listener6 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i6);
                            NetworkPolicyManagerService.this.dispatchSubscriptionPlansChanged(listener6, subId2, plans);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 20:
                        NetworkPolicyManagerService.this.mNetworkStats.forceUpdate();
                        synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                            NetworkPolicyManagerService.this.updateNetworkRulesNL();
                            NetworkPolicyManagerService.this.updateNetworkEnabledNL();
                            NetworkPolicyManagerService.this.updateNotificationsNL();
                        }
                        return true;
                    case 21:
                        int uid3 = msg.arg1;
                        int newBlockedReasons = msg.arg2;
                        int oldBlockedReasons = ((Integer) msg.obj).intValue();
                        int length7 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i7 = 0; i7 < length7; i7++) {
                            INetworkPolicyListener listener7 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i7);
                            NetworkPolicyManagerService.this.dispatchBlockedReasonChanged(listener7, uid3, oldBlockedReasons, newBlockedReasons);
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 22:
                        synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                                int subId3 = msg.arg1;
                                if (msg.arg2 == NetworkPolicyManagerService.this.mSetSubscriptionPlansIds.get(subId3)) {
                                    if (NetworkPolicyManagerService.LOGD) {
                                        Slog.d(NetworkPolicyManagerService.TAG, "Clearing expired subscription plans.");
                                    }
                                    NetworkPolicyManagerService.this.setSubscriptionPlansInternal(subId3, new SubscriptionPlan[0], 0L, (String) msg.obj);
                                } else if (NetworkPolicyManagerService.LOGD) {
                                    Slog.d(NetworkPolicyManagerService.TAG, "Ignoring stale CLEAR_SUBSCRIPTION_PLANS.");
                                }
                            }
                        }
                        return true;
                    case 23:
                        SparseArray<SomeArgs> uidStateUpdates = (SparseArray) msg.obj;
                        int uidsSize = uidStateUpdates.size();
                        int listenersSize = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        for (int i8 = 0; i8 < listenersSize; i8++) {
                            INetworkPolicyListener listener8 = NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i8);
                            for (int uidIndex = 0; uidIndex < uidsSize; uidIndex++) {
                                int uid4 = uidStateUpdates.keyAt(uidIndex);
                                SomeArgs someArgs = uidStateUpdates.valueAt(uidIndex);
                                int oldBlockedReasons2 = someArgs.argi1;
                                int newBlockedReasons2 = someArgs.argi2;
                                int uidRules2 = someArgs.argi3;
                                NetworkPolicyManagerService.this.dispatchBlockedReasonChanged(listener8, uid4, oldBlockedReasons2, newBlockedReasons2);
                                if (NetworkPolicyManagerService.LOGV) {
                                    Slog.v(NetworkPolicyManagerService.TAG, "Dispatching rules=" + NetworkPolicyManager.uidRulesToString(uidRules2) + " for uid=" + uid4);
                                }
                                NetworkPolicyManagerService.this.dispatchUidRulesChanged(listener8, uid4, uidRules2);
                            }
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        for (int uidIndex2 = 0; uidIndex2 < uidsSize; uidIndex2++) {
                            uidStateUpdates.valueAt(uidIndex2).recycle();
                        }
                        return true;
                }
            }
        };
        this.mHandlerCallback = callback;
        Handler.Callback callback2 = new Handler.Callback() { // from class: com.android.server.net.NetworkPolicyManagerService.17
            @Override // android.os.Handler.Callback
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 100:
                        NetworkPolicyManagerService.this.handleUidChanged((UidStateCallbackInfo) msg.obj);
                        return true;
                    case 101:
                        int uid = msg.arg1;
                        NetworkPolicyManagerService.this.handleUidGone(uid);
                        return true;
                    default:
                        return false;
                }
            }
        };
        this.mUidEventHandlerCallback = callback2;
        Context context2 = (Context) Objects.requireNonNull(context, "missing context");
        this.mContext = context2;
        this.mActivityManager = (IActivityManager) Objects.requireNonNull(activityManager, "missing activityManager");
        this.mNetworkManager = (INetworkManagementService) Objects.requireNonNull(networkManagement, "missing networkManagement");
        this.mPowerWhitelistManager = (PowerWhitelistManager) context2.getSystemService(PowerWhitelistManager.class);
        this.mClock = (Clock) Objects.requireNonNull(clock, "missing Clock");
        this.mUserManager = (UserManager) context2.getSystemService("user");
        this.mCarrierConfigManager = (CarrierConfigManager) context2.getSystemService(CarrierConfigManager.class);
        this.mIPm = pm;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        Handler handler = new Handler(thread.getLooper(), callback);
        this.mHandler = handler;
        ServiceThread serviceThread = new ServiceThread("NetworkPolicy.uid", -2, false);
        this.mUidEventThread = serviceThread;
        serviceThread.start();
        this.mUidEventHandler = new Handler(serviceThread.getLooper(), callback2);
        this.mSuppressDefaultPolicy = suppressDefaultPolicy;
        this.mDeps = (Dependencies) Objects.requireNonNull(deps, "missing Dependencies");
        this.mPolicyFile = new AtomicFile(new File(systemDir, "netpolicy.xml"), "net-policy");
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mNetworkStats = (NetworkStatsManager) context.getSystemService(NetworkStatsManager.class);
        this.mMultipathPolicyTracker = new MultipathPolicyTracker(context2, handler);
        LocalServices.addService(NetworkPolicyManagerInternal.class, new NetworkPolicyManagerInternalImpl());
    }

    public void bindConnectivityManager() {
        this.mConnManager = (ConnectivityManager) Objects.requireNonNull((ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class), "missing ConnectivityManager");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePowerSaveWhitelistUL() {
        int[] whitelist = this.mPowerWhitelistManager.getWhitelistedAppIds(false);
        this.mPowerSaveWhitelistExceptIdleAppIds.clear();
        for (int uid : whitelist) {
            this.mPowerSaveWhitelistExceptIdleAppIds.put(uid, true);
        }
        int[] whitelist2 = this.mPowerWhitelistManager.getWhitelistedAppIds(true);
        this.mPowerSaveWhitelistAppIds.clear();
        for (int uid2 : whitelist2) {
            this.mPowerSaveWhitelistAppIds.put(uid2, true);
        }
    }

    boolean addDefaultRestrictBackgroundAllowlistUidsUL() {
        List<UserInfo> users = this.mUserManager.getUsers();
        int numberUsers = users.size();
        boolean changed = false;
        for (int i = 0; i < numberUsers; i++) {
            UserInfo user = users.get(i);
            changed = addDefaultRestrictBackgroundAllowlistUidsUL(user.id) || changed;
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean addDefaultRestrictBackgroundAllowlistUidsUL(int userId) {
        SystemConfig sysConfig = SystemConfig.getInstance();
        PackageManager pm = this.mContext.getPackageManager();
        ArraySet<String> allowDataUsage = sysConfig.getAllowInDataUsageSave();
        boolean changed = false;
        for (int i = 0; i < allowDataUsage.size(); i++) {
            String pkg = allowDataUsage.valueAt(i);
            boolean z = LOGD;
            if (z) {
                Slog.d(TAG, "checking restricted background exemption for package " + pkg + " and user " + userId);
            }
            try {
                ApplicationInfo app = pm.getApplicationInfoAsUser(pkg, 1048576, userId);
                if (!app.isPrivilegedApp()) {
                    Slog.e(TAG, "addDefaultRestrictBackgroundAllowlistUidsUL(): skipping non-privileged app  " + pkg);
                } else {
                    int uid = UserHandle.getUid(userId, app.uid);
                    this.mDefaultRestrictBackgroundAllowlistUids.append(uid, true);
                    if (z) {
                        Slog.d(TAG, "Adding uid " + uid + " (user " + userId + ") to default restricted background allowlist. Revoked status: " + this.mRestrictBackgroundAllowlistRevokedUids.get(uid));
                    }
                    if (!this.mRestrictBackgroundAllowlistRevokedUids.get(uid)) {
                        if (z) {
                            Slog.d(TAG, "adding default package " + pkg + " (uid " + uid + " for user " + userId + ") to restrict background allowlist");
                        }
                        setUidPolicyUncheckedUL(uid, 4, false);
                        changed = true;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                if (LOGD) {
                    Slog.d(TAG, "No ApplicationInfo for package " + pkg);
                }
            }
        }
        return changed;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: initService */
    public void m4967x5fd880a6(CountDownLatch initCompleteSignal) {
        Trace.traceBegin(2097152L, "systemReady");
        int oldPriority = Process.getThreadPriority(Process.myTid());
        try {
            Process.setThreadPriority(-2);
            if (isBandwidthControlEnabled()) {
                this.mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
                this.mAppStandby = (AppStandbyInternal) LocalServices.getService(AppStandbyInternal.class);
                this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
                synchronized (this.mUidRulesFirstLock) {
                    synchronized (this.mNetworkPoliciesSecondLock) {
                        updatePowerSaveWhitelistUL();
                        PowerManagerInternal powerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
                        this.mPowerManagerInternal = powerManagerInternal;
                        powerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() { // from class: com.android.server.net.NetworkPolicyManagerService.1
                            public int getServiceType() {
                                return 6;
                            }

                            public void onLowPowerModeChanged(PowerSaveState result) {
                                boolean enabled = result.batterySaverEnabled;
                                if (NetworkPolicyManagerService.LOGD) {
                                    Slog.d(NetworkPolicyManagerService.TAG, "onLowPowerModeChanged(" + enabled + ")");
                                }
                                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                                    if (NetworkPolicyManagerService.this.mRestrictPower != enabled) {
                                        NetworkPolicyManagerService.this.mRestrictPower = enabled;
                                        NetworkPolicyManagerService.this.updateRulesForRestrictPowerUL();
                                    }
                                }
                            }
                        });
                        this.mRestrictPower = this.mPowerManagerInternal.getLowPowerState(6).batterySaverEnabled;
                        RestrictedModeObserver restrictedModeObserver = new RestrictedModeObserver(this.mContext, new RestrictedModeObserver.RestrictedModeListener() { // from class: com.android.server.net.NetworkPolicyManagerService$$ExternalSyntheticLambda7
                            @Override // com.android.server.net.NetworkPolicyManagerService.RestrictedModeObserver.RestrictedModeListener
                            public final void onChange(boolean z) {
                                NetworkPolicyManagerService.this.m4966xb1776d68(z);
                            }
                        });
                        this.mRestrictedModeObserver = restrictedModeObserver;
                        this.mRestrictedNetworkingMode = restrictedModeObserver.isRestrictedModeEnabled();
                        this.mSystemReady = true;
                        waitForAdminData();
                        readPolicyAL();
                        this.mRestrictBackgroundBeforeBsm = this.mLoadedRestrictBackground;
                        boolean z = this.mPowerManagerInternal.getLowPowerState(10).batterySaverEnabled;
                        this.mRestrictBackgroundLowPowerMode = z;
                        if (z && !this.mLoadedRestrictBackground) {
                            this.mLoadedRestrictBackground = true;
                        }
                        this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() { // from class: com.android.server.net.NetworkPolicyManagerService.2
                            public int getServiceType() {
                                return 10;
                            }

                            public void onLowPowerModeChanged(PowerSaveState result) {
                                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                                    NetworkPolicyManagerService.this.updateRestrictBackgroundByLowPowerModeUL(result);
                                }
                            }
                        });
                        if (addDefaultRestrictBackgroundAllowlistUidsUL()) {
                            writePolicyAL();
                        }
                        setRestrictBackgroundUL(this.mLoadedRestrictBackground, "init_service");
                        updateRulesForGlobalChangeAL(false);
                        updateNotificationsNL();
                    }
                }
                try {
                    this.mActivityManagerInternal.registerNetworkPolicyUidObserver(this.mUidObserver, 35, 5, PackageManagerService.PLATFORM_PACKAGE_NAME);
                    this.mNetworkManager.registerObserver(this.mAlertObserver);
                } catch (RemoteException e) {
                }
                IntentFilter whitelistFilter = new IntentFilter("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
                this.mContext.registerReceiver(this.mPowerSaveWhitelistReceiver, whitelistFilter, null, this.mHandler);
                IntentFilter connFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
                this.mContext.registerReceiver(this.mConnReceiver, connFilter, "android.permission.NETWORK_STACK", this.mHandler);
                IntentFilter packageFilter = new IntentFilter();
                packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
                packageFilter.addDataScheme("package");
                this.mContext.registerReceiverForAllUsers(this.mPackageReceiver, packageFilter, null, this.mHandler);
                this.mContext.registerReceiverForAllUsers(this.mUidRemovedReceiver, new IntentFilter("android.intent.action.UID_REMOVED"), null, this.mHandler);
                IntentFilter userFilter = new IntentFilter();
                userFilter.addAction("android.intent.action.USER_ADDED");
                userFilter.addAction("android.intent.action.USER_REMOVED");
                this.mContext.registerReceiver(this.mUserReceiver, userFilter, null, this.mHandler);
                Executor executor = new HandlerExecutor(this.mHandler);
                this.mNetworkStats.registerUsageCallback(new NetworkTemplate.Builder(1).build(), 0L, executor, this.mStatsCallback);
                this.mNetworkStats.registerUsageCallback(new NetworkTemplate.Builder(4).build(), 0L, executor, this.mStatsCallback);
                this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter(ACTION_SNOOZE_WARNING), "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
                this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter(ACTION_SNOOZE_RAPID), "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
                IntentFilter wifiFilter = new IntentFilter("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
                this.mContext.registerReceiver(this.mWifiReceiver, wifiFilter, null, this.mHandler);
                IntentFilter carrierConfigFilter = new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED");
                this.mContext.registerReceiver(this.mCarrierConfigReceiver, carrierConfigFilter, null, this.mHandler);
                IntentFilter simStateChangedFilter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
                this.mContext.registerReceiver(this.mSimStateChangedReceiver, simStateChangedFilter, null, this.mHandler);
                this.mConnManager.registerNetworkCallback(new NetworkRequest.Builder().build(), this.mNetworkCallback);
                this.mAppStandby.addListener(new NetPolicyAppIdleStateChangeListener());
                synchronized (this.mUidRulesFirstLock) {
                    updateRulesForAppIdleParoleUL();
                }
                ((SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class)).addOnSubscriptionsChangedListener(new HandlerExecutor(this.mHandler), new SubscriptionManager.OnSubscriptionsChangedListener() { // from class: com.android.server.net.NetworkPolicyManagerService.3
                    @Override // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
                    public void onSubscriptionsChanged() {
                        NetworkPolicyManagerService.this.updateNetworksInternal();
                    }
                });
                ITranNetworkPolicyManagerService.Instance().onInitService(this.mContext);
                initCompleteSignal.countDown();
                return;
            }
            Slog.w(TAG, "bandwidth controls disabled, unable to enforce policy");
        } finally {
            Process.setThreadPriority(oldPriority);
            Trace.traceEnd(2097152L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initService$0$com-android-server-net-NetworkPolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m4966xb1776d68(boolean enabled) {
        synchronized (this.mUidRulesFirstLock) {
            this.mRestrictedNetworkingMode = enabled;
            updateRestrictedModeAllowlistUL();
        }
    }

    public CountDownLatch networkScoreAndNetworkManagementServiceReady() {
        this.mNetworkManagerReady = true;
        final CountDownLatch initCompleteSignal = new CountDownLatch(1);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.NetworkPolicyManagerService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                NetworkPolicyManagerService.this.m4967x5fd880a6(initCompleteSignal);
            }
        });
        return initCompleteSignal;
    }

    public void systemReady(CountDownLatch initCompleteSignal) {
        try {
            if (!initCompleteSignal.await(30L, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Service NetworkPolicy init timeout");
            }
            this.mMultipathPolicyTracker.start();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Service NetworkPolicy init interrupted", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class UidStateCallbackInfo {
        public int capability;
        public boolean isPending;
        public int procState;
        public long procStateSeq;
        public int uid;

        private UidStateCallbackInfo() {
            this.procState = 20;
            this.procStateSeq = -1L;
        }

        public void update(int uid, int procState, long procStateSeq, int capability) {
            this.uid = uid;
            this.procState = procState;
            this.procStateSeq = procStateSeq;
            this.capability = capability;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class StatsCallback extends NetworkStatsManager.UsageCallback {
        private boolean mIsAnyCallbackReceived;

        private StatsCallback() {
            this.mIsAnyCallbackReceived = false;
        }

        @Override // android.app.usage.NetworkStatsManager.UsageCallback
        public void onThresholdReached(int networkType, String subscriberId) {
            this.mIsAnyCallbackReceived = true;
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                NetworkPolicyManagerService.this.updateNetworkRulesNL();
                NetworkPolicyManagerService.this.updateNetworkEnabledNL();
                NetworkPolicyManagerService.this.updateNotificationsNL();
            }
        }

        public boolean isAnyCallbackReceived() {
            return this.mIsAnyCallbackReceived;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean updateCapabilityChange(SparseBooleanArray lastValues, boolean newValue, Network network) {
        boolean changed = false;
        boolean lastValue = lastValues.get(network.getNetId(), false);
        changed = (lastValue != newValue || lastValues.indexOfKey(network.getNetId()) < 0) ? true : true;
        if (changed) {
            lastValues.put(network.getNetId(), newValue);
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateNetworkToIfacesNL(int netId, ArraySet<String> newIfaces) {
        ArraySet<String> lastIfaces = this.mNetworkToIfaces.get(netId);
        boolean changed = true;
        if (lastIfaces != null && lastIfaces.equals(newIfaces)) {
            changed = false;
        }
        if (changed) {
            this.mNetworkToIfaces.remove(netId);
            Iterator<String> it = newIfaces.iterator();
            while (it.hasNext()) {
                String iface = it.next();
                this.mNetworkToIfaces.add(netId, iface);
            }
        }
        return changed;
    }

    void updateNotificationsNL() {
        long totalBytes;
        int i;
        ArraySet<NotificationId> beforeNotifs;
        boolean z;
        NetworkPolicyManagerService networkPolicyManagerService = this;
        if (LOGV) {
            Slog.v(TAG, "updateNotificationsNL()");
        }
        Trace.traceBegin(2097152L, "updateNotificationsNL");
        ArraySet<NotificationId> beforeNotifs2 = new ArraySet<>(networkPolicyManagerService.mActiveNotifs);
        networkPolicyManagerService.mActiveNotifs.clear();
        long now = networkPolicyManagerService.mClock.millis();
        boolean z2 = true;
        int i2 = networkPolicyManagerService.mNetworkPolicy.size() - 1;
        while (i2 >= 0) {
            NetworkPolicy policy = networkPolicyManagerService.mNetworkPolicy.valueAt(i2);
            int subId = networkPolicyManagerService.findRelevantSubIdNL(policy.template);
            if (subId == -1) {
                i = i2;
                beforeNotifs = beforeNotifs2;
                z = z2;
            } else if (policy.hasCycle()) {
                Pair<ZonedDateTime, ZonedDateTime> cycle = (Pair) NetworkPolicyManager.cycleIterator(policy).next();
                long cycleStart = ((ZonedDateTime) cycle.first).toInstant().toEpochMilli();
                long cycleEnd = ((ZonedDateTime) cycle.second).toInstant().toEpochMilli();
                long totalBytes2 = getTotalBytes(policy.template, cycleStart, cycleEnd);
                PersistableBundle config = networkPolicyManagerService.mSubIdToCarrierConfig.get(subId);
                if (!CarrierConfigManager.isConfigForIdentifiedCarrier(config)) {
                    if (LOGV) {
                        Slog.v(TAG, "isConfigForIdentifiedCarrier returned false");
                    }
                    i = i2;
                    beforeNotifs = beforeNotifs2;
                    z = z2;
                } else {
                    boolean notifyWarning = getBooleanDefeatingNullable(config, "data_warning_notification_bool", z2);
                    boolean notifyLimit = getBooleanDefeatingNullable(config, "data_limit_notification_bool", z2);
                    boolean notifyRapid = getBooleanDefeatingNullable(config, "data_rapid_notification_bool", z2);
                    if (!notifyWarning) {
                        totalBytes = totalBytes2;
                    } else if (!policy.isOverWarning(totalBytes2) || policy.isOverLimit(totalBytes2)) {
                        totalBytes = totalBytes2;
                    } else {
                        boolean snoozedThisCycle = policy.lastWarningSnooze >= cycleStart ? z2 : false;
                        if (snoozedThisCycle) {
                            totalBytes = totalBytes2;
                        } else {
                            totalBytes = totalBytes2;
                            enqueueNotification(policy, 34, totalBytes2, null);
                        }
                    }
                    if (notifyLimit) {
                        long totalBytes3 = totalBytes;
                        if (policy.isOverLimit(totalBytes3)) {
                            boolean snoozedThisCycle2 = policy.lastLimitSnooze >= cycleStart ? z2 : false;
                            if (snoozedThisCycle2) {
                                enqueueNotification(policy, 36, totalBytes3, null);
                            } else {
                                enqueueNotification(policy, 35, totalBytes3, null);
                                networkPolicyManagerService.notifyOverLimitNL(policy.template);
                            }
                        } else {
                            networkPolicyManagerService.notifyUnderLimitNL(policy.template);
                        }
                    }
                    if (!notifyRapid || policy.limitBytes == -1) {
                        i = i2;
                        beforeNotifs = beforeNotifs2;
                        z = z2;
                    } else {
                        long recentDuration = TimeUnit.DAYS.toMillis(4L);
                        long recentStart = now - recentDuration;
                        long recentBytes = getTotalBytes(policy.template, recentStart, now);
                        long cycleDuration = cycleEnd - cycleStart;
                        long projectedBytes = (recentBytes * cycleDuration) / recentDuration;
                        long alertBytes = (policy.limitBytes * 3) / 2;
                        if (LOGD) {
                            Slog.d(TAG, "Rapid usage considering recent " + recentBytes + " projected " + projectedBytes + " alert " + alertBytes);
                        }
                        long cycleDuration2 = policy.lastRapidSnooze;
                        boolean snoozedRecently = cycleDuration2 >= now - 86400000;
                        if (projectedBytes <= alertBytes || snoozedRecently) {
                            i = i2;
                            z = true;
                            beforeNotifs = beforeNotifs2;
                        } else {
                            i = i2;
                            z = true;
                            beforeNotifs = beforeNotifs2;
                            enqueueNotification(policy, 45, 0L, findRapidBlame(policy.template, recentStart, now));
                        }
                    }
                }
            } else {
                i = i2;
                beforeNotifs = beforeNotifs2;
                z = z2;
            }
            i2 = i - 1;
            beforeNotifs2 = beforeNotifs;
            z2 = z;
            networkPolicyManagerService = this;
        }
        ArraySet<NotificationId> beforeNotifs3 = beforeNotifs2;
        for (int i3 = beforeNotifs3.size() - 1; i3 >= 0; i3--) {
            NotificationId notificationId = beforeNotifs3.valueAt(i3);
            if (!this.mActiveNotifs.contains(notificationId)) {
                cancelNotification(notificationId);
            }
        }
        Trace.traceEnd(2097152L);
    }

    private ApplicationInfo findRapidBlame(NetworkTemplate template, long start, long end) {
        String[] packageNames;
        if (this.mStatsCallback.isAnyCallbackReceived()) {
            List<NetworkStats.Bucket> stats = this.mDeps.getNetworkUidBytes(template, start, end);
            long maxBytes = 0;
            long totalBytes = 0;
            int maxUid = 0;
            for (NetworkStats.Bucket entry : stats) {
                long bytes = entry.getRxBytes() + entry.getTxBytes();
                totalBytes += bytes;
                if (bytes > maxBytes) {
                    maxBytes = bytes;
                    maxUid = entry.getUid();
                }
            }
            if (maxBytes > 0 && maxBytes > totalBytes / 2 && (packageNames = this.mContext.getPackageManager().getPackagesForUid(maxUid)) != null && packageNames.length == 1) {
                try {
                    return this.mContext.getPackageManager().getApplicationInfo(packageNames[0], 4989440);
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            return null;
        }
        return null;
    }

    private int findRelevantSubIdNL(NetworkTemplate template) {
        for (int i = 0; i < this.mSubIdToSubscriberId.size(); i++) {
            int subId = this.mSubIdToSubscriberId.keyAt(i);
            String subscriberId = this.mSubIdToSubscriberId.valueAt(i);
            NetworkIdentity probeIdent = new NetworkIdentity.Builder().setType(0).setSubscriberId(subscriberId).setMetered(true).setDefaultNetwork(true).setSubId(subId).build();
            if (template.matches(probeIdent)) {
                return subId;
            }
        }
        return -1;
    }

    private void notifyOverLimitNL(NetworkTemplate template) {
        if (!this.mOverLimitNotified.contains(template)) {
            Context context = this.mContext;
            context.startActivity(buildNetworkOverLimitIntent(context.getResources(), template));
            this.mOverLimitNotified.add(template);
        }
    }

    private void notifyUnderLimitNL(NetworkTemplate template) {
        this.mOverLimitNotified.remove(template);
    }

    private void enqueueNotification(NetworkPolicy policy, int type, long totalBytes, ApplicationInfo rapidBlame) {
        CharSequence body;
        CharSequence title;
        CharSequence title2;
        NotificationId notificationId = new NotificationId(policy, type);
        Notification.Builder builder = new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_ALERTS);
        builder.setOnlyAlertOnce(true);
        builder.setWhen(0L);
        builder.setColor(this.mContext.getColor(17170460));
        Resources res = this.mContext.getResources();
        switch (type) {
            case 34:
                CharSequence title3 = res.getText(17040106);
                body = res.getString(17040105, Formatter.formatFileSize(this.mContext, totalBytes, 8));
                builder.setSmallIcon(17301624);
                Intent snoozeIntent = buildSnoozeWarningIntent(policy.template, this.mContext.getPackageName());
                builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, snoozeIntent, AudioFormat.DTS_HD));
                Intent viewIntent = buildViewDataUsageIntent(res, policy.template);
                if (UserManager.isHeadlessSystemUserMode()) {
                    builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, viewIntent, AudioFormat.DTS_HD, null, UserHandle.CURRENT));
                } else {
                    builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, viewIntent, AudioFormat.DTS_HD));
                }
                title = title3;
                break;
            case 35:
                switch (policy.template.getMatchRule()) {
                    case 1:
                    case 10:
                        CharSequence title4 = res.getText(17040099);
                        title = title4;
                        break;
                    case 4:
                        CharSequence title5 = res.getText(17040108);
                        title = title5;
                        break;
                    default:
                        return;
                }
                body = res.getText(17040096);
                builder.setOngoing(true);
                builder.setSmallIcon(17303604);
                Intent intent = buildNetworkOverLimitIntent(res, policy.template);
                if (UserManager.isHeadlessSystemUserMode()) {
                    builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, AudioFormat.DTS_HD, null, UserHandle.CURRENT));
                    break;
                } else {
                    builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, intent, AudioFormat.DTS_HD));
                    break;
                }
            case 36:
                switch (policy.template.getMatchRule()) {
                    case 1:
                    case 10:
                        title2 = res.getText(17040098);
                        break;
                    case 4:
                        title2 = res.getText(17040107);
                        break;
                    default:
                        return;
                }
                long overBytes = totalBytes - policy.limitBytes;
                body = res.getString(17040097, Formatter.formatFileSize(this.mContext, overBytes, 8));
                builder.setOngoing(true);
                builder.setSmallIcon(17301624);
                builder.setChannelId(SystemNotificationChannels.NETWORK_STATUS);
                Intent intent2 = buildViewDataUsageIntent(res, policy.template);
                if (UserManager.isHeadlessSystemUserMode()) {
                    builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent2, AudioFormat.DTS_HD, null, UserHandle.CURRENT));
                } else {
                    builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, intent2, AudioFormat.DTS_HD));
                }
                title = title2;
                break;
            case 45:
                title = res.getText(17040102);
                if (rapidBlame != null) {
                    body = res.getString(17040100, rapidBlame.loadLabel(this.mContext.getPackageManager()));
                } else {
                    body = res.getString(17040101);
                }
                builder.setSmallIcon(17301624);
                Intent snoozeIntent2 = buildSnoozeRapidIntent(policy.template, this.mContext.getPackageName());
                builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, snoozeIntent2, AudioFormat.DTS_HD));
                Intent viewIntent2 = buildViewDataUsageIntent(res, policy.template);
                if (UserManager.isHeadlessSystemUserMode()) {
                    builder.setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, viewIntent2, AudioFormat.DTS_HD, null, UserHandle.CURRENT));
                    break;
                } else {
                    builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, viewIntent2, AudioFormat.DTS_HD));
                    break;
                }
            default:
                return;
        }
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setStyle(new Notification.BigTextStyle().bigText(body));
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).notifyAsUser(notificationId.getTag(), notificationId.getId(), builder.build(), UserHandle.ALL);
        this.mActiveNotifs.add(notificationId);
    }

    private void cancelNotification(NotificationId notificationId) {
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).cancel(notificationId.getTag(), notificationId.getId());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNetworksInternal() {
        updateSubscriptions();
        synchronized (this.mUidRulesFirstLock) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                ensureActiveCarrierPolicyAL();
                normalizePoliciesNL();
                updateNetworkEnabledNL();
                updateNetworkRulesNL();
                updateNotificationsNL();
            }
        }
    }

    void updateNetworks() throws InterruptedException {
        updateNetworksInternal();
        final CountDownLatch latch = new CountDownLatch(1);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.NetworkPolicyManagerService$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                latch.countDown();
            }
        });
        latch.await(5L, TimeUnit.SECONDS);
    }

    Handler getHandlerForTesting() {
        return this.mHandler;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean maybeUpdateCarrierPolicyCycleAL(int subId, String subscriberId) {
        if (LOGV) {
            Slog.v(TAG, "maybeUpdateCarrierPolicyCycleAL()");
        }
        boolean policyUpdated = false;
        NetworkIdentity probeIdent = new NetworkIdentity.Builder().setType(0).setSubscriberId(subscriberId).setMetered(true).setDefaultNetwork(true).setSubId(subId).build();
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            NetworkTemplate template = this.mNetworkPolicy.keyAt(i);
            if (template.matches(probeIdent)) {
                NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
                policyUpdated |= updateDefaultCarrierPolicyAL(subId, policy);
            }
        }
        return policyUpdated;
    }

    int getCycleDayFromCarrierConfig(PersistableBundle config, int fallbackCycleDay) {
        if (config == null) {
            return fallbackCycleDay;
        }
        int cycleDay = config.getInt("monthly_data_cycle_day_int");
        if (cycleDay == -1) {
            return fallbackCycleDay;
        }
        Calendar cal = Calendar.getInstance();
        if (cycleDay < cal.getMinimum(5) || cycleDay > cal.getMaximum(5)) {
            Slog.e(TAG, "Invalid date in CarrierConfigManager.KEY_MONTHLY_DATA_CYCLE_DAY_INT: " + cycleDay);
            return fallbackCycleDay;
        }
        return cycleDay;
    }

    long getWarningBytesFromCarrierConfig(PersistableBundle config, long fallbackWarningBytes) {
        if (config == null) {
            return fallbackWarningBytes;
        }
        long warningBytes = config.getLong("data_warning_threshold_bytes_long");
        if (warningBytes == -2) {
            return -1L;
        }
        if (warningBytes == -1) {
            return getPlatformDefaultWarningBytes();
        }
        if (warningBytes < 0) {
            Slog.e(TAG, "Invalid value in CarrierConfigManager.KEY_DATA_WARNING_THRESHOLD_BYTES_LONG; expected a non-negative value but got: " + warningBytes);
            return fallbackWarningBytes;
        }
        return warningBytes;
    }

    long getLimitBytesFromCarrierConfig(PersistableBundle config, long fallbackLimitBytes) {
        if (config == null) {
            return fallbackLimitBytes;
        }
        long limitBytes = config.getLong("data_limit_threshold_bytes_long");
        if (limitBytes == -2) {
            return -1L;
        }
        if (limitBytes == -1) {
            return getPlatformDefaultLimitBytes();
        }
        if (limitBytes < 0) {
            Slog.e(TAG, "Invalid value in CarrierConfigManager.KEY_DATA_LIMIT_THRESHOLD_BYTES_LONG; expected a non-negative value but got: " + limitBytes);
            return fallbackLimitBytes;
        }
        return limitBytes;
    }

    void handleNetworkPoliciesUpdateAL(boolean shouldNormalizePolicies) {
        if (shouldNormalizePolicies) {
            normalizePoliciesNL();
        }
        updateNetworkEnabledNL();
        updateNetworkRulesNL();
        updateNotificationsNL();
        writePolicyAL();
    }

    void updateNetworkEnabledNL() {
        if (LOGV) {
            Slog.v(TAG, "updateNetworkEnabledNL()");
        }
        Trace.traceBegin(2097152L, "updateNetworkEnabledNL");
        long startTime = this.mStatLogger.getTime();
        int i = this.mNetworkPolicy.size() - 1;
        while (true) {
            if (i >= 0) {
                NetworkPolicy policy = this.mNetworkPolicy.valueAt(i);
                if (policy.limitBytes == -1 || !policy.hasCycle()) {
                    setNetworkTemplateEnabled(policy.template, true);
                } else {
                    Pair<ZonedDateTime, ZonedDateTime> cycle = (Pair) NetworkPolicyManager.cycleIterator(policy).next();
                    long start = ((ZonedDateTime) cycle.first).toInstant().toEpochMilli();
                    long end = ((ZonedDateTime) cycle.second).toInstant().toEpochMilli();
                    long totalBytes = getTotalBytes(policy.template, start, end);
                    boolean overLimitWithoutSnooze = policy.isOverLimit(totalBytes) && policy.lastLimitSnooze < start;
                    boolean networkEnabled = overLimitWithoutSnooze ? false : true;
                    setNetworkTemplateEnabled(policy.template, networkEnabled);
                }
                i--;
            } else {
                this.mStatLogger.logDurationStat(0, startTime);
                Trace.traceEnd(2097152L);
                return;
            }
        }
    }

    private void setNetworkTemplateEnabled(NetworkTemplate template, boolean enabled) {
        this.mHandler.obtainMessage(18, enabled ? 1 : 0, 0, template).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setNetworkTemplateEnabledInner(NetworkTemplate template, boolean enabled) {
        if (template.getMatchRule() == 1 || template.getMatchRule() == 10) {
            IntArray matchingSubIds = new IntArray();
            synchronized (this.mNetworkPoliciesSecondLock) {
                for (int i = 0; i < this.mSubIdToSubscriberId.size(); i++) {
                    int subId = this.mSubIdToSubscriberId.keyAt(i);
                    String subscriberId = this.mSubIdToSubscriberId.valueAt(i);
                    NetworkIdentity probeIdent = new NetworkIdentity.Builder().setType(0).setSubscriberId(subscriberId).setMetered(true).setDefaultNetwork(true).setSubId(subId).build();
                    if (template.matches(probeIdent)) {
                        matchingSubIds.add(subId);
                    }
                }
            }
            TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
            for (int i2 = 0; i2 < matchingSubIds.size(); i2++) {
                tm.createForSubscriptionId(matchingSubIds.get(i2)).setPolicyDataEnabled(enabled);
            }
        }
    }

    private static void collectIfaces(ArraySet<String> ifaces, NetworkStateSnapshot snapshot) {
        ifaces.addAll(snapshot.getLinkProperties().getAllInterfaceNames());
    }

    void updateSubscriptions() {
        Iterator<SubscriptionInfo> it;
        if (LOGV) {
            Slog.v(TAG, "updateSubscriptions()");
        }
        Trace.traceBegin(2097152L, "updateSubscriptions");
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        SubscriptionManager sm = (SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class);
        List<SubscriptionInfo> subList = CollectionUtils.emptyIfNull(sm.getActiveSubscriptionInfoList());
        List<String[]> mergedSubscriberIdsList = new ArrayList<>();
        SparseArray<String> subIdToSubscriberId = new SparseArray<>(subList.size());
        SparseArray<PersistableBundle> subIdToCarrierConfig = new SparseArray<>();
        Iterator<SubscriptionInfo> it2 = subList.iterator();
        while (it2.hasNext()) {
            SubscriptionInfo sub = it2.next();
            int subId = sub.getSubscriptionId();
            TelephonyManager tmSub = tm.createForSubscriptionId(subId);
            String subscriberId = tmSub.getSubscriberId();
            if (!TextUtils.isEmpty(subscriberId)) {
                subIdToSubscriberId.put(tmSub.getSubscriptionId(), subscriberId);
            } else {
                Slog.w(TAG, "Missing subscriberId for subId " + tmSub.getSubscriptionId());
            }
            String[] mergedSubscriberId = ArrayUtils.defeatNullable(tmSub.getMergedImsisFromGroup());
            mergedSubscriberIdsList.add(mergedSubscriberId);
            PersistableBundle config = this.mCarrierConfigManager.getConfigForSubId(subId);
            if (config != null) {
                subIdToCarrierConfig.put(subId, config);
                it = it2;
            } else {
                it = it2;
                Slog.e(TAG, "Missing CarrierConfig for subId " + subId);
            }
            it2 = it;
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            this.mSubIdToSubscriberId.clear();
            for (int i = 0; i < subIdToSubscriberId.size(); i++) {
                this.mSubIdToSubscriberId.put(subIdToSubscriberId.keyAt(i), subIdToSubscriberId.valueAt(i));
            }
            this.mMergedSubscriberIds = mergedSubscriberIdsList;
            this.mSubIdToCarrierConfig.clear();
            for (int i2 = 0; i2 < subIdToCarrierConfig.size(); i2++) {
                this.mSubIdToCarrierConfig.put(subIdToCarrierConfig.keyAt(i2), subIdToCarrierConfig.valueAt(i2));
            }
        }
        Trace.traceEnd(2097152L);
    }

    void updateNetworkRulesNL() {
        int i;
        String[] meteredIfaces;
        int subId;
        SubscriptionPlan plan;
        ContentResolver cr;
        int i2;
        int subId2;
        Instant now;
        Iterator<NetworkStateSnapshot> it;
        long quotaBytes;
        ArraySet<String> newMeteredIfaces;
        NetworkPolicy policy;
        ArraySet<String> newMeteredIfaces2;
        ArraySet<String> newMeteredIfaces3;
        if (LOGV) {
            Slog.v(TAG, "updateNetworkRulesNL()");
        }
        Trace.traceBegin(2097152L, "updateNetworkRulesNL");
        List<NetworkStateSnapshot> snapshots = this.mConnManager.getAllNetworkStateSnapshots();
        this.mNetIdToSubId.clear();
        ArrayMap<NetworkStateSnapshot, NetworkIdentity> identified = new ArrayMap<>();
        Iterator<NetworkStateSnapshot> it2 = snapshots.iterator();
        while (true) {
            i = 1;
            if (!it2.hasNext()) {
                break;
            }
            NetworkStateSnapshot snapshot = it2.next();
            this.mNetIdToSubId.put(snapshot.getNetwork().getNetId(), snapshot.getSubId());
            NetworkIdentity ident = new NetworkIdentity.Builder().setNetworkStateSnapshot(snapshot).setDefaultNetwork(true).build();
            identified.put(snapshot, ident);
        }
        ArraySet<String> newMeteredIfaces4 = new ArraySet<>();
        ArraySet<String> matchingIfaces = new ArraySet<>();
        long lowestRule = Long.MAX_VALUE;
        int i3 = this.mNetworkPolicy.size() - 1;
        while (true) {
            if (i3 < 0) {
                break;
            }
            NetworkPolicy policy2 = this.mNetworkPolicy.valueAt(i3);
            matchingIfaces.clear();
            for (int j = identified.size() - i; j >= 0; j--) {
                if (policy2.template.matches(identified.valueAt(j))) {
                    collectIfaces(matchingIfaces, identified.keyAt(j));
                }
            }
            if (LOGD) {
                Slog.d(TAG, "Applying " + policy2 + " to ifaces " + matchingIfaces);
            }
            int i4 = policy2.warningBytes != -1 ? i : 0;
            int i5 = policy2.limitBytes != -1 ? i : 0;
            long limitBytes = JobStatus.NO_LATEST_RUNTIME;
            long warningBytes = JobStatus.NO_LATEST_RUNTIME;
            if (i5 == 0 && i4 == 0) {
                policy = policy2;
                newMeteredIfaces2 = newMeteredIfaces4;
            } else if (!policy2.hasCycle()) {
                policy = policy2;
                newMeteredIfaces2 = newMeteredIfaces4;
            } else {
                Pair<ZonedDateTime, ZonedDateTime> cycle = (Pair) NetworkPolicyManager.cycleIterator(policy2).next();
                long start = ((ZonedDateTime) cycle.first).toInstant().toEpochMilli();
                long end = ((ZonedDateTime) cycle.second).toInstant().toEpochMilli();
                policy = policy2;
                newMeteredIfaces2 = newMeteredIfaces4;
                long totalBytes = getTotalBytes(policy2.template, start, end);
                if (i5 != 0 && policy.lastLimitSnooze < start) {
                    limitBytes = Math.max(1L, policy.limitBytes - totalBytes);
                }
                if (i4 != 0 && policy.lastWarningSnooze < start && !policy.isOverWarning(totalBytes)) {
                    warningBytes = Math.max(1L, policy.warningBytes - totalBytes);
                }
            }
            if (i4 == 0 && i5 == 0 && !policy.metered) {
                newMeteredIfaces3 = newMeteredIfaces2;
            } else {
                if (matchingIfaces.size() > 1) {
                    Slog.w(TAG, "shared quota unsupported; generating rule for each iface");
                }
                for (int j2 = matchingIfaces.size() - 1; j2 >= 0; j2--) {
                    String iface = matchingIfaces.valueAt(j2);
                    setInterfaceQuotasAsync(iface, warningBytes, limitBytes);
                    newMeteredIfaces2.add(iface);
                }
                newMeteredIfaces3 = newMeteredIfaces2;
            }
            if (i4 != 0 && policy.warningBytes < lowestRule) {
                lowestRule = policy.warningBytes;
            }
            if (i5 != 0 && policy.limitBytes < lowestRule) {
                lowestRule = policy.limitBytes;
            }
            i3--;
            newMeteredIfaces4 = newMeteredIfaces3;
            i = 1;
        }
        ArraySet<String> newMeteredIfaces5 = newMeteredIfaces4;
        for (NetworkStateSnapshot snapshot2 : snapshots) {
            if (!snapshot2.getNetworkCapabilities().hasCapability(11)) {
                matchingIfaces.clear();
                collectIfaces(matchingIfaces, snapshot2);
                int j3 = matchingIfaces.size() - 1;
                while (j3 >= 0) {
                    String iface2 = matchingIfaces.valueAt(j3);
                    if (newMeteredIfaces5.contains(iface2)) {
                        newMeteredIfaces = newMeteredIfaces5;
                    } else {
                        newMeteredIfaces = newMeteredIfaces5;
                        setInterfaceQuotasAsync(iface2, JobStatus.NO_LATEST_RUNTIME, JobStatus.NO_LATEST_RUNTIME);
                        newMeteredIfaces.add(iface2);
                    }
                    j3--;
                    newMeteredIfaces5 = newMeteredIfaces;
                }
            }
            newMeteredIfaces5 = newMeteredIfaces5;
        }
        ArraySet<String> newMeteredIfaces6 = newMeteredIfaces5;
        synchronized (this.mMeteredIfacesLock) {
            for (int i6 = this.mMeteredIfaces.size() - 1; i6 >= 0; i6--) {
                String iface3 = this.mMeteredIfaces.valueAt(i6);
                if (!newMeteredIfaces6.contains(iface3)) {
                    removeInterfaceQuotasAsync(iface3);
                }
            }
            this.mMeteredIfaces = newMeteredIfaces6;
        }
        ContentResolver cr2 = this.mContext.getContentResolver();
        int i7 = Settings.Global.getInt(cr2, "netpolicy_quota_enabled", 1) != 0 ? 1 : 0;
        long quotaUnlimited = Settings.Global.getLong(cr2, "netpolicy_quota_unlimited", QUOTA_UNLIMITED_DEFAULT);
        float quotaLimited = Settings.Global.getFloat(cr2, "netpolicy_quota_limited", QUOTA_LIMITED_DEFAULT);
        this.mSubscriptionOpportunisticQuota.clear();
        Iterator<NetworkStateSnapshot> it3 = snapshots.iterator();
        while (it3.hasNext()) {
            NetworkStateSnapshot snapshot3 = it3.next();
            if (i7 != 0 && snapshot3.getNetwork() != null && (subId = getSubIdLocked(snapshot3.getNetwork())) != -1 && (plan = getPrimarySubscriptionPlanLocked(subId)) != null) {
                long limitBytes2 = plan.getDataLimitBytes();
                if (!snapshot3.getNetworkCapabilities().hasCapability(18)) {
                    quotaBytes = 0;
                    it = it3;
                    subId2 = subId;
                    cr = cr2;
                    i2 = i7;
                } else if (limitBytes2 == -1) {
                    quotaBytes = -1;
                    it = it3;
                    subId2 = subId;
                    cr = cr2;
                    i2 = i7;
                } else if (limitBytes2 == JobStatus.NO_LATEST_RUNTIME) {
                    quotaBytes = quotaUnlimited;
                    it = it3;
                    subId2 = subId;
                    cr = cr2;
                    i2 = i7;
                } else {
                    Range<ZonedDateTime> cycle2 = plan.cycleIterator().next();
                    long start2 = cycle2.getLower().toInstant().toEpochMilli();
                    long end2 = cycle2.getUpper().toInstant().toEpochMilli();
                    Instant now2 = this.mClock.instant();
                    long startOfDay = ZonedDateTime.ofInstant(now2, cycle2.getLower().getZone()).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli();
                    String subscriberId = snapshot3.getSubscriberId();
                    long totalBytes2 = 0;
                    if (subscriberId == null) {
                        subId2 = subId;
                        now = now2;
                        cr = cr2;
                        i2 = i7;
                    } else {
                        cr = cr2;
                        i2 = i7;
                        subId2 = subId;
                        now = now2;
                        totalBytes2 = getTotalBytes(buildTemplateCarrierMetered(subscriberId), start2, startOfDay);
                    }
                    long remainingBytes = limitBytes2 - totalBytes2;
                    it = it3;
                    long remainingDays = (((end2 - now.toEpochMilli()) - 1) / TimeUnit.DAYS.toMillis(1L)) + 1;
                    quotaBytes = Math.max(0L, ((float) (remainingBytes / remainingDays)) * quotaLimited);
                }
                this.mSubscriptionOpportunisticQuota.put(subId2, quotaBytes);
                it3 = it;
                cr2 = cr;
                i7 = i2;
            }
        }
        synchronized (this.mMeteredIfacesLock) {
            ArraySet<String> arraySet = this.mMeteredIfaces;
            meteredIfaces = (String[]) arraySet.toArray(new String[arraySet.size()]);
        }
        this.mHandler.obtainMessage(2, meteredIfaces).sendToTarget();
        this.mHandler.obtainMessage(7, Long.valueOf(lowestRule)).sendToTarget();
        Trace.traceEnd(2097152L);
    }

    private void ensureActiveCarrierPolicyAL() {
        if (LOGV) {
            Slog.v(TAG, "ensureActiveCarrierPolicyAL()");
        }
        if (this.mSuppressDefaultPolicy) {
            return;
        }
        for (int i = 0; i < this.mSubIdToSubscriberId.size(); i++) {
            int subId = this.mSubIdToSubscriberId.keyAt(i);
            String subscriberId = this.mSubIdToSubscriberId.valueAt(i);
            ensureActiveCarrierPolicyAL(subId, subscriberId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean ensureActiveCarrierPolicyAL(int subId, String subscriberId) {
        NetworkIdentity probeIdent = new NetworkIdentity.Builder().setType(0).setSubscriberId(subscriberId).setMetered(true).setDefaultNetwork(true).setSubId(subId).build();
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            NetworkTemplate template = this.mNetworkPolicy.keyAt(i);
            if (template.matches(probeIdent)) {
                if (LOGD) {
                    Slog.d(TAG, "Found template " + template + " which matches subscriber " + NetworkIdentityUtils.scrubSubscriberId(subscriberId));
                }
                return false;
            }
        }
        Slog.i(TAG, "No policy for subscriber " + NetworkIdentityUtils.scrubSubscriberId(subscriberId) + "; generating default policy");
        NetworkPolicy policy = buildDefaultCarrierPolicy(subId, subscriberId);
        addNetworkPolicyAL(policy);
        return true;
    }

    private long getPlatformDefaultWarningBytes() {
        int dataWarningConfig = this.mContext.getResources().getInteger(17694890);
        if (dataWarningConfig == -1) {
            return -1L;
        }
        return DataUnit.MEBIBYTES.toBytes(dataWarningConfig);
    }

    private long getPlatformDefaultLimitBytes() {
        return -1L;
    }

    NetworkPolicy buildDefaultCarrierPolicy(int subId, String subscriberId) {
        NetworkTemplate template = buildTemplateCarrierMetered(subscriberId);
        RecurrenceRule cycleRule = NetworkPolicy.buildRule(ZonedDateTime.now().getDayOfMonth(), ZoneId.systemDefault());
        NetworkPolicy policy = new NetworkPolicy(template, cycleRule, getPlatformDefaultWarningBytes(), getPlatformDefaultLimitBytes(), -1L, -1L, true, true);
        synchronized (this.mUidRulesFirstLock) {
            try {
                try {
                    synchronized (this.mNetworkPoliciesSecondLock) {
                        updateDefaultCarrierPolicyAL(subId, policy);
                    }
                    return policy;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public static NetworkTemplate buildTemplateCarrierMetered(String subscriberId) {
        Objects.requireNonNull(subscriberId);
        return new NetworkTemplate.Builder(10).setSubscriberIds(Set.of(subscriberId)).setMeteredness(1).build();
    }

    private boolean updateDefaultCarrierPolicyAL(int subId, NetworkPolicy policy) {
        int currentCycleDay;
        if (!policy.inferred) {
            if (LOGD) {
                Slog.d(TAG, "Ignoring user-defined policy " + policy);
            }
            return false;
        }
        NetworkPolicy original = new NetworkPolicy(policy.template, policy.cycleRule, policy.warningBytes, policy.limitBytes, policy.lastWarningSnooze, policy.lastLimitSnooze, policy.metered, policy.inferred);
        SubscriptionPlan[] plans = this.mSubscriptionPlans.get(subId);
        if (!ArrayUtils.isEmpty(plans)) {
            SubscriptionPlan plan = plans[0];
            policy.cycleRule = plan.getCycleRule();
            long planLimitBytes = plan.getDataLimitBytes();
            if (planLimitBytes == -1) {
                policy.warningBytes = getPlatformDefaultWarningBytes();
                policy.limitBytes = getPlatformDefaultLimitBytes();
            } else if (planLimitBytes == JobStatus.NO_LATEST_RUNTIME) {
                policy.warningBytes = -1L;
                policy.limitBytes = -1L;
            } else {
                policy.warningBytes = (9 * planLimitBytes) / 10;
                switch (plan.getDataLimitBehavior()) {
                    case 0:
                    case 1:
                        policy.limitBytes = planLimitBytes;
                        break;
                    default:
                        policy.limitBytes = -1L;
                        break;
                }
            }
        } else {
            PersistableBundle config = this.mSubIdToCarrierConfig.get(subId);
            if (policy.cycleRule.isMonthly()) {
                currentCycleDay = policy.cycleRule.start.getDayOfMonth();
            } else {
                currentCycleDay = -1;
            }
            int cycleDay = getCycleDayFromCarrierConfig(config, currentCycleDay);
            policy.cycleRule = NetworkPolicy.buildRule(cycleDay, ZoneId.systemDefault());
            policy.warningBytes = getWarningBytesFromCarrierConfig(config, policy.warningBytes);
            policy.limitBytes = getLimitBytesFromCarrierConfig(config, policy.limitBytes);
        }
        if (policy.equals(original)) {
            return false;
        }
        Slog.d(TAG, "Updated " + original + " to " + policy);
        return true;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2716=5] */
    private void readPolicyAL() {
        FileInputStream fis;
        int version;
        int subscriberIdMatchRule;
        int subscriberIdMatchRule2;
        SparseBooleanArray restrictBackgroundAllowedUids;
        int type;
        RecurrenceRule cycleRule;
        boolean metered;
        if (LOGV) {
            Slog.v(TAG, "readPolicyAL()");
        }
        this.mNetworkPolicy.clear();
        this.mSubscriptionPlans.clear();
        this.mSubscriptionPlansOwner.clear();
        this.mUidPolicy.clear();
        FileInputStream fis2 = null;
        try {
            try {
                fis2 = this.mPolicyFile.openRead();
                try {
                    TypedXmlPullParser in = Xml.resolvePullParser(fis2);
                    SparseBooleanArray restrictBackgroundAllowedUids2 = new SparseBooleanArray();
                    int version2 = 1;
                    boolean insideAllowlist = false;
                    while (true) {
                        int type2 = in.next();
                        boolean z = false;
                        if (type2 == 1) {
                            fis = fis2;
                            int size = restrictBackgroundAllowedUids2.size();
                            for (int i = 0; i < size; i++) {
                                int uid = restrictBackgroundAllowedUids2.keyAt(i);
                                int policy = this.mUidPolicy.get(uid, 0);
                                if ((policy & 1) != 0) {
                                    Slog.w(TAG, "ignoring restrict-background-allowlist for " + uid + " because its policy is " + NetworkPolicyManager.uidPoliciesToString(policy));
                                } else if (UserHandle.isApp(uid)) {
                                    int newPolicy = policy | 4;
                                    if (LOGV) {
                                        Log.v(TAG, "new policy for " + uid + ": " + NetworkPolicyManager.uidPoliciesToString(newPolicy));
                                    }
                                    setUidPolicyUncheckedUL(uid, newPolicy, false);
                                } else {
                                    Slog.w(TAG, "unable to update policy on UID " + uid);
                                }
                            }
                            IoUtils.closeQuietly(fis);
                            return;
                        }
                        String tag = in.getName();
                        if (type2 != 2) {
                            fis = fis2;
                            version = version2;
                            if (type2 == 3 && TAG_WHITELIST.equals(tag)) {
                                insideAllowlist = false;
                                version2 = version;
                            }
                            version2 = version;
                        } else if (TAG_POLICY_LIST.equals(tag)) {
                            boolean z2 = this.mRestrictBackground;
                            version2 = XmlUtils.readIntAttribute(in, ATTR_VERSION);
                            if (version2 >= 3 && XmlUtils.readBooleanAttribute(in, ATTR_RESTRICT_BACKGROUND)) {
                                z = true;
                            }
                            this.mLoadedRestrictBackground = z;
                            fis = fis2;
                        } else {
                            if (TAG_NETWORK_POLICY.equals(tag)) {
                                int templateType = XmlUtils.readIntAttribute(in, ATTR_NETWORK_TEMPLATE);
                                String subscriberId = in.getAttributeValue((String) null, ATTR_SUBSCRIBER_ID);
                                String networkId = version2 >= 9 ? in.getAttributeValue((String) null, ATTR_NETWORK_ID) : null;
                                if (version2 >= 13) {
                                    subscriberIdMatchRule2 = XmlUtils.readIntAttribute(in, ATTR_SUBSCRIBER_ID_MATCH_RULE);
                                    subscriberIdMatchRule = XmlUtils.readIntAttribute(in, ATTR_TEMPLATE_METERED);
                                } else if (templateType == 1) {
                                    Log.d(TAG, "Update template match rule from mobile to carrier and force to metered");
                                    templateType = 10;
                                    subscriberIdMatchRule = 1;
                                    subscriberIdMatchRule2 = 0;
                                } else {
                                    subscriberIdMatchRule = -1;
                                    subscriberIdMatchRule2 = 0;
                                }
                                if (version2 >= 11) {
                                    String start = XmlUtils.readStringAttribute(in, ATTR_CYCLE_START);
                                    String end = XmlUtils.readStringAttribute(in, ATTR_CYCLE_END);
                                    fis = fis2;
                                    try {
                                        String period = XmlUtils.readStringAttribute(in, ATTR_CYCLE_PERIOD);
                                        type = type2;
                                        restrictBackgroundAllowedUids = restrictBackgroundAllowedUids2;
                                        cycleRule = new RecurrenceRule(RecurrenceRule.convertZonedDateTime(start), RecurrenceRule.convertZonedDateTime(end), RecurrenceRule.convertPeriod(period));
                                    } catch (FileNotFoundException e) {
                                        fis2 = fis;
                                        upgradeDefaultBackgroundDataUL();
                                        IoUtils.closeQuietly(fis2);
                                        return;
                                    } catch (Exception e2) {
                                        e = e2;
                                        fis2 = fis;
                                        Log.wtf(TAG, "problem reading network policy", e);
                                        IoUtils.closeQuietly(fis2);
                                        return;
                                    } catch (Throwable th) {
                                        th = th;
                                        fis2 = fis;
                                        IoUtils.closeQuietly(fis2);
                                        throw th;
                                    }
                                } else {
                                    fis = fis2;
                                    restrictBackgroundAllowedUids = restrictBackgroundAllowedUids2;
                                    type = type2;
                                    int cycleDay = XmlUtils.readIntAttribute(in, ATTR_CYCLE_DAY);
                                    String cycleTimezone = version2 >= 6 ? in.getAttributeValue((String) null, ATTR_CYCLE_TIMEZONE) : "UTC";
                                    cycleRule = NetworkPolicy.buildRule(cycleDay, ZoneId.of(cycleTimezone));
                                }
                                long warningBytes = XmlUtils.readLongAttribute(in, ATTR_WARNING_BYTES);
                                long limitBytes = XmlUtils.readLongAttribute(in, ATTR_LIMIT_BYTES);
                                long lastLimitSnooze = version2 >= 5 ? XmlUtils.readLongAttribute(in, ATTR_LAST_LIMIT_SNOOZE) : version2 >= 2 ? XmlUtils.readLongAttribute(in, ATTR_LAST_SNOOZE) : -1L;
                                if (version2 < 4) {
                                    switch (templateType) {
                                        case 1:
                                            metered = true;
                                            break;
                                        default:
                                            metered = false;
                                            break;
                                    }
                                } else {
                                    metered = XmlUtils.readBooleanAttribute(in, ATTR_METERED);
                                }
                                long lastWarningSnooze = version2 >= 5 ? XmlUtils.readLongAttribute(in, ATTR_LAST_WARNING_SNOOZE) : -1L;
                                boolean inferred = version2 >= 7 ? XmlUtils.readBooleanAttribute(in, ATTR_INFERRED) : false;
                                version = version2;
                                NetworkTemplate.Builder builder = new NetworkTemplate.Builder(templateType).setMeteredness(subscriberIdMatchRule);
                                if (subscriberIdMatchRule2 == 0) {
                                    ArraySet<String> ids = new ArraySet<>();
                                    ids.add(subscriberId);
                                    builder.setSubscriberIds(ids);
                                }
                                if (networkId != null) {
                                    builder.setWifiNetworkKeys(Set.of(networkId));
                                }
                                NetworkTemplate template = builder.build();
                                if (NetworkPolicy.isTemplatePersistable(template)) {
                                    this.mNetworkPolicy.put(template, new NetworkPolicy(template, cycleRule, warningBytes, limitBytes, lastWarningSnooze, lastLimitSnooze, metered, inferred));
                                }
                                restrictBackgroundAllowedUids2 = restrictBackgroundAllowedUids;
                            } else {
                                fis = fis2;
                                SparseBooleanArray restrictBackgroundAllowedUids3 = restrictBackgroundAllowedUids2;
                                version = version2;
                                if (TAG_UID_POLICY.equals(tag)) {
                                    int uid2 = XmlUtils.readIntAttribute(in, "uid");
                                    int policy2 = XmlUtils.readIntAttribute(in, ATTR_POLICY);
                                    if (UserHandle.isApp(uid2)) {
                                        setUidPolicyUncheckedUL(uid2, policy2, false);
                                    } else {
                                        Slog.w(TAG, "unable to apply policy to UID " + uid2 + "; ignoring");
                                    }
                                    restrictBackgroundAllowedUids2 = restrictBackgroundAllowedUids3;
                                } else if (TAG_APP_POLICY.equals(tag)) {
                                    int appId = XmlUtils.readIntAttribute(in, ATTR_APP_ID);
                                    int policy3 = XmlUtils.readIntAttribute(in, ATTR_POLICY);
                                    int uid3 = UserHandle.getUid(0, appId);
                                    if (UserHandle.isApp(uid3)) {
                                        setUidPolicyUncheckedUL(uid3, policy3, false);
                                    } else {
                                        Slog.w(TAG, "unable to apply policy to UID " + uid3 + "; ignoring");
                                    }
                                    restrictBackgroundAllowedUids2 = restrictBackgroundAllowedUids3;
                                } else if (TAG_WHITELIST.equals(tag)) {
                                    insideAllowlist = true;
                                    version2 = version;
                                    restrictBackgroundAllowedUids2 = restrictBackgroundAllowedUids3;
                                } else if (TAG_RESTRICT_BACKGROUND.equals(tag) && insideAllowlist) {
                                    restrictBackgroundAllowedUids2 = restrictBackgroundAllowedUids3;
                                    restrictBackgroundAllowedUids2.append(XmlUtils.readIntAttribute(in, "uid"), true);
                                } else {
                                    restrictBackgroundAllowedUids2 = restrictBackgroundAllowedUids3;
                                    if (TAG_REVOKED_RESTRICT_BACKGROUND.equals(tag) && insideAllowlist) {
                                        this.mRestrictBackgroundAllowlistRevokedUids.put(XmlUtils.readIntAttribute(in, "uid"), true);
                                    }
                                }
                            }
                            version2 = version;
                        }
                        fis2 = fis;
                    }
                } catch (FileNotFoundException e3) {
                } catch (Exception e4) {
                    e = e4;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (FileNotFoundException e5) {
        } catch (Exception e6) {
            e = e6;
        }
    }

    private void upgradeDefaultBackgroundDataUL() {
        this.mLoadedRestrictBackground = Settings.Global.getInt(this.mContext.getContentResolver(), "default_restrict_background_data", 0) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void upgradeWifiMeteredOverride() {
        int i;
        ArrayMap<String, Boolean> wifiNetworkKeys = new ArrayMap<>();
        synchronized (this.mNetworkPoliciesSecondLock) {
            int i2 = 0;
            while (i2 < this.mNetworkPolicy.size()) {
                NetworkPolicy policy = this.mNetworkPolicy.valueAt(i2);
                if (policy.template.getMatchRule() == 4 && !policy.inferred) {
                    this.mNetworkPolicy.removeAt(i2);
                    Set<String> keys = policy.template.getWifiNetworkKeys();
                    wifiNetworkKeys.put(keys.isEmpty() ? null : keys.iterator().next(), Boolean.valueOf(policy.metered));
                } else {
                    i2++;
                }
            }
        }
        if (wifiNetworkKeys.isEmpty()) {
            return;
        }
        WifiManager wm = (WifiManager) this.mContext.getSystemService(WifiManager.class);
        List<WifiConfiguration> configs = wm.getConfiguredNetworks();
        for (int i3 = 0; i3 < configs.size(); i3++) {
            WifiConfiguration config = configs.get(i3);
            Iterator it = config.getAllNetworkKeys().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                String key = (String) it.next();
                Boolean metered = wifiNetworkKeys.get(key);
                if (metered != null) {
                    Slog.d(TAG, "Found network " + key + "; upgrading metered hint");
                    if (metered.booleanValue()) {
                        i = 1;
                    } else {
                        i = 2;
                    }
                    config.meteredOverride = i;
                    wm.updateNetwork(config);
                }
            }
        }
        synchronized (this.mUidRulesFirstLock) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                writePolicyAL();
            }
        }
    }

    void writePolicyAL() {
        int subscriberIdMatchRule;
        if (LOGV) {
            Slog.v(TAG, "writePolicyAL()");
        }
        FileOutputStream fos = null;
        try {
            fos = this.mPolicyFile.startWrite();
            TypedXmlSerializer out = Xml.resolveSerializer(fos);
            int i = 1;
            out.startDocument((String) null, true);
            out.startTag((String) null, TAG_POLICY_LIST);
            XmlUtils.writeIntAttribute(out, ATTR_VERSION, 14);
            XmlUtils.writeBooleanAttribute(out, ATTR_RESTRICT_BACKGROUND, this.mRestrictBackground);
            int i2 = 0;
            while (i2 < this.mNetworkPolicy.size()) {
                NetworkPolicy policy = this.mNetworkPolicy.valueAt(i2);
                NetworkTemplate template = policy.template;
                if (NetworkPolicy.isTemplatePersistable(template)) {
                    out.startTag((String) null, TAG_NETWORK_POLICY);
                    XmlUtils.writeIntAttribute(out, ATTR_NETWORK_TEMPLATE, template.getMatchRule());
                    String subscriberId = template.getSubscriberIds().isEmpty() ? null : (String) template.getSubscriberIds().iterator().next();
                    if (subscriberId != null) {
                        out.attribute((String) null, ATTR_SUBSCRIBER_ID, subscriberId);
                    }
                    if (template.getSubscriberIds().isEmpty()) {
                        subscriberIdMatchRule = i;
                    } else {
                        subscriberIdMatchRule = 0;
                    }
                    XmlUtils.writeIntAttribute(out, ATTR_SUBSCRIBER_ID_MATCH_RULE, subscriberIdMatchRule);
                    if (!template.getWifiNetworkKeys().isEmpty()) {
                        out.attribute((String) null, ATTR_NETWORK_ID, (String) template.getWifiNetworkKeys().iterator().next());
                    }
                    XmlUtils.writeIntAttribute(out, ATTR_TEMPLATE_METERED, template.getMeteredness());
                    XmlUtils.writeStringAttribute(out, ATTR_CYCLE_START, RecurrenceRule.convertZonedDateTime(policy.cycleRule.start));
                    XmlUtils.writeStringAttribute(out, ATTR_CYCLE_END, RecurrenceRule.convertZonedDateTime(policy.cycleRule.end));
                    XmlUtils.writeStringAttribute(out, ATTR_CYCLE_PERIOD, RecurrenceRule.convertPeriod(policy.cycleRule.period));
                    XmlUtils.writeLongAttribute(out, ATTR_WARNING_BYTES, policy.warningBytes);
                    XmlUtils.writeLongAttribute(out, ATTR_LIMIT_BYTES, policy.limitBytes);
                    XmlUtils.writeLongAttribute(out, ATTR_LAST_WARNING_SNOOZE, policy.lastWarningSnooze);
                    XmlUtils.writeLongAttribute(out, ATTR_LAST_LIMIT_SNOOZE, policy.lastLimitSnooze);
                    XmlUtils.writeBooleanAttribute(out, ATTR_METERED, policy.metered);
                    XmlUtils.writeBooleanAttribute(out, ATTR_INFERRED, policy.inferred);
                    out.endTag((String) null, TAG_NETWORK_POLICY);
                }
                i2++;
                i = 1;
            }
            for (int i3 = 0; i3 < this.mUidPolicy.size(); i3++) {
                int uid = this.mUidPolicy.keyAt(i3);
                int policy2 = this.mUidPolicy.valueAt(i3);
                if (policy2 != 0) {
                    out.startTag((String) null, TAG_UID_POLICY);
                    XmlUtils.writeIntAttribute(out, "uid", uid);
                    XmlUtils.writeIntAttribute(out, ATTR_POLICY, policy2);
                    out.endTag((String) null, TAG_UID_POLICY);
                }
            }
            out.endTag((String) null, TAG_POLICY_LIST);
            out.startTag((String) null, TAG_WHITELIST);
            int size = this.mRestrictBackgroundAllowlistRevokedUids.size();
            for (int i4 = 0; i4 < size; i4++) {
                int uid2 = this.mRestrictBackgroundAllowlistRevokedUids.keyAt(i4);
                out.startTag((String) null, TAG_REVOKED_RESTRICT_BACKGROUND);
                XmlUtils.writeIntAttribute(out, "uid", uid2);
                out.endTag((String) null, TAG_REVOKED_RESTRICT_BACKGROUND);
            }
            out.endTag((String) null, TAG_WHITELIST);
            out.endDocument();
            this.mPolicyFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mPolicyFile.failWrite(fos);
            }
        }
    }

    public void setUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(uid)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + uid);
        }
        synchronized (this.mUidRulesFirstLock) {
            long token = Binder.clearCallingIdentity();
            int oldPolicy = this.mUidPolicy.get(uid, 0);
            if (oldPolicy != policy) {
                setUidPolicyUncheckedUL(uid, oldPolicy, policy, true);
                this.mLogger.uidPolicyChanged(uid, oldPolicy, policy);
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    public void addUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(uid)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + uid);
        }
        synchronized (this.mUidRulesFirstLock) {
            int oldPolicy = this.mUidPolicy.get(uid, 0);
            int policy2 = policy | oldPolicy;
            if (oldPolicy != policy2) {
                setUidPolicyUncheckedUL(uid, oldPolicy, policy2, true);
                this.mLogger.uidPolicyChanged(uid, oldPolicy, policy2);
            }
        }
    }

    public void removeUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (!UserHandle.isApp(uid)) {
            throw new IllegalArgumentException("cannot apply policy to UID " + uid);
        }
        synchronized (this.mUidRulesFirstLock) {
            int oldPolicy = this.mUidPolicy.get(uid, 0);
            int policy2 = oldPolicy & (~policy);
            if (oldPolicy != policy2) {
                setUidPolicyUncheckedUL(uid, oldPolicy, policy2, true);
                this.mLogger.uidPolicyChanged(uid, oldPolicy, policy2);
            }
        }
    }

    private void setUidPolicyUncheckedUL(int uid, int oldPolicy, int policy, boolean persist) {
        boolean wasDenied;
        boolean isDenied;
        boolean wasAllowed;
        boolean isAllowed;
        boolean wasBlocked;
        boolean isBlocked;
        boolean notifyApp = false;
        setUidPolicyUncheckedUL(uid, policy, false);
        if (!isUidValidForAllowlistRulesUL(uid)) {
            notifyApp = false;
        } else {
            if (oldPolicy != 1) {
                wasDenied = false;
            } else {
                wasDenied = true;
            }
            if (policy != 1) {
                isDenied = false;
            } else {
                isDenied = true;
            }
            if (oldPolicy != 4) {
                wasAllowed = false;
            } else {
                wasAllowed = true;
            }
            if (policy != 4) {
                isAllowed = false;
            } else {
                isAllowed = true;
            }
            if (!wasDenied && (!this.mRestrictBackground || wasAllowed)) {
                wasBlocked = false;
            } else {
                wasBlocked = true;
            }
            if (!isDenied && (!this.mRestrictBackground || isAllowed)) {
                isBlocked = false;
            } else {
                isBlocked = true;
            }
            if (wasAllowed && ((!isAllowed || isDenied) && this.mDefaultRestrictBackgroundAllowlistUids.get(uid) && !this.mRestrictBackgroundAllowlistRevokedUids.get(uid))) {
                if (LOGD) {
                    Slog.d(TAG, "Adding uid " + uid + " to revoked restrict background allowlist");
                }
                this.mRestrictBackgroundAllowlistRevokedUids.append(uid, true);
            }
            if (wasBlocked != isBlocked) {
                notifyApp = true;
            }
        }
        this.mHandler.obtainMessage(13, uid, policy, Boolean.valueOf(notifyApp)).sendToTarget();
        if (persist) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                writePolicyAL();
            }
        }
    }

    private void setUidPolicyUncheckedUL(int uid, int policy, boolean persist) {
        if (policy == 0) {
            this.mUidPolicy.delete(uid);
        } else {
            this.mUidPolicy.put(uid, policy);
        }
        m4969x3105fe71(uid);
        if (persist) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                writePolicyAL();
            }
        }
    }

    public int getUidPolicy(int uid) {
        int i;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesFirstLock) {
            i = this.mUidPolicy.get(uid, 0);
        }
        return i;
    }

    public int[] getUidsWithPolicy(int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        int[] uids = new int[0];
        synchronized (this.mUidRulesFirstLock) {
            for (int i = 0; i < this.mUidPolicy.size(); i++) {
                int uid = this.mUidPolicy.keyAt(i);
                int uidPolicy = this.mUidPolicy.valueAt(i);
                if ((policy == 0 && uidPolicy == 0) || (uidPolicy & policy) != 0) {
                    uids = ArrayUtils.appendInt(uids, uid);
                }
            }
        }
        return uids;
    }

    boolean removeUserStateUL(int userId, boolean writePolicy, boolean updateGlobalRules) {
        this.mLogger.removingUserState(userId);
        boolean changed = false;
        for (int i = this.mRestrictBackgroundAllowlistRevokedUids.size() - 1; i >= 0; i--) {
            if (UserHandle.getUserId(this.mRestrictBackgroundAllowlistRevokedUids.keyAt(i)) == userId) {
                this.mRestrictBackgroundAllowlistRevokedUids.removeAt(i);
                changed = true;
            }
        }
        int[] uids = new int[0];
        for (int i2 = 0; i2 < this.mUidPolicy.size(); i2++) {
            int uid = this.mUidPolicy.keyAt(i2);
            if (UserHandle.getUserId(uid) == userId) {
                uids = ArrayUtils.appendInt(uids, uid);
            }
        }
        int i3 = uids.length;
        if (i3 > 0) {
            for (int uid2 : uids) {
                this.mUidPolicy.delete(uid2);
            }
            changed = true;
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            if (updateGlobalRules) {
                try {
                    updateRulesForGlobalChangeAL(true);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (writePolicy && changed) {
                writePolicyAL();
            }
        }
        return changed;
    }

    private boolean checkAnyPermissionOf(String... permissions) {
        for (String permission : permissions) {
            if (this.mContext.checkCallingOrSelfPermission(permission) == 0) {
                return true;
            }
        }
        return false;
    }

    private void enforceAnyPermissionOf(String... permissions) {
        if (!checkAnyPermissionOf(permissions)) {
            throw new SecurityException("Requires one of the following permissions: " + String.join(", ", permissions) + ".");
        }
    }

    public void registerListener(INetworkPolicyListener listener) {
        Objects.requireNonNull(listener);
        enforceAnyPermissionOf("android.permission.CONNECTIVITY_INTERNAL", "android.permission.OBSERVE_NETWORK_POLICY");
        this.mListeners.register(listener);
    }

    public void unregisterListener(INetworkPolicyListener listener) {
        Objects.requireNonNull(listener);
        enforceAnyPermissionOf("android.permission.CONNECTIVITY_INTERNAL", "android.permission.OBSERVE_NETWORK_POLICY");
        this.mListeners.unregister(listener);
    }

    public void setNetworkPolicies(NetworkPolicy[] policies) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    normalizePoliciesNL(policies);
                    handleNetworkPoliciesUpdateAL(false);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void addNetworkPolicyAL(NetworkPolicy policy) {
        NetworkPolicy[] policies = getNetworkPolicies(this.mContext.getOpPackageName());
        setNetworkPolicies((NetworkPolicy[]) ArrayUtils.appendElement(NetworkPolicy.class, policies, policy));
    }

    public NetworkPolicy[] getNetworkPolicies(String callingPackage) {
        NetworkPolicy[] policies;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", TAG);
        } catch (SecurityException e) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", TAG);
            if (this.mAppOps.noteOp(51, Binder.getCallingUid(), callingPackage) != 0) {
                return new NetworkPolicy[0];
            }
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            int size = this.mNetworkPolicy.size();
            policies = new NetworkPolicy[size];
            for (int i = 0; i < size; i++) {
                policies[i] = this.mNetworkPolicy.valueAt(i);
            }
        }
        return policies;
    }

    private void normalizePoliciesNL() {
        normalizePoliciesNL(getNetworkPolicies(this.mContext.getOpPackageName()));
    }

    private void normalizePoliciesNL(NetworkPolicy[] policies) {
        this.mNetworkPolicy.clear();
        for (NetworkPolicy policy : policies) {
            if (policy != null) {
                policy.template = normalizeTemplate(policy.template, this.mMergedSubscriberIds);
                NetworkPolicy existing = this.mNetworkPolicy.get(policy.template);
                if (existing == null || existing.compareTo(policy) > 0) {
                    if (existing != null) {
                        Slog.d(TAG, "Normalization replaced " + existing + " with " + policy);
                    }
                    this.mNetworkPolicy.put(policy.template, policy);
                }
            }
        }
    }

    private static NetworkTemplate normalizeTemplate(NetworkTemplate template, List<String[]> mergedList) {
        if (template.getSubscriberIds().isEmpty()) {
            return template;
        }
        for (String[] merged : mergedList) {
            for (String subscriberId : template.getSubscriberIds()) {
                if (com.android.net.module.util.CollectionUtils.contains(merged, subscriberId)) {
                    return new NetworkTemplate.Builder(template.getMatchRule()).setWifiNetworkKeys(template.getWifiNetworkKeys()).setSubscriberIds(Set.of((Object[]) merged)).setMeteredness(template.getMeteredness()).build();
                }
            }
        }
        return template;
    }

    public void snoozeLimit(NetworkTemplate template) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long token = Binder.clearCallingIdentity();
        try {
            performSnooze(template, 35);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void performSnooze(NetworkTemplate template, int type) {
        long currentTime = this.mClock.millis();
        synchronized (this.mUidRulesFirstLock) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                NetworkPolicy policy = this.mNetworkPolicy.get(template);
                if (policy == null) {
                    throw new IllegalArgumentException("unable to find policy for " + template);
                }
                switch (type) {
                    case 34:
                        policy.lastWarningSnooze = currentTime;
                        break;
                    case 35:
                        policy.lastLimitSnooze = currentTime;
                        break;
                    case 45:
                        policy.lastRapidSnooze = currentTime;
                        break;
                    default:
                        throw new IllegalArgumentException("unexpected type");
                }
                handleNetworkPoliciesUpdateAL(true);
            }
        }
    }

    public void setRestrictBackground(boolean restrictBackground) {
        Trace.traceBegin(2097152L, "setRestrictBackground");
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            int callingUid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            synchronized (this.mUidRulesFirstLock) {
                setRestrictBackgroundUL(restrictBackground, "uid:" + callingUid);
            }
            Binder.restoreCallingIdentity(token);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3310=4] */
    private void setRestrictBackgroundUL(boolean restrictBackground, String reason) {
        Trace.traceBegin(2097152L, "setRestrictBackgroundUL");
        try {
            if (restrictBackground == this.mRestrictBackground) {
                Slog.w(TAG, "setRestrictBackgroundUL: already " + restrictBackground);
                return;
            }
            Slog.d(TAG, "setRestrictBackgroundUL(): " + restrictBackground + "; reason: " + reason);
            boolean oldRestrictBackground = this.mRestrictBackground;
            this.mRestrictBackground = restrictBackground;
            updateRulesForRestrictBackgroundUL();
            try {
                if (!this.mNetworkManager.setDataSaverModeEnabled(this.mRestrictBackground)) {
                    Slog.e(TAG, "Could not change Data Saver Mode on NMS to " + this.mRestrictBackground);
                    this.mRestrictBackground = oldRestrictBackground;
                    return;
                }
            } catch (RemoteException e) {
            }
            sendRestrictBackgroundChangedMsg();
            this.mLogger.restrictBackgroundChanged(oldRestrictBackground, this.mRestrictBackground);
            if (this.mRestrictBackgroundLowPowerMode) {
                this.mRestrictBackgroundChangedInBsm = true;
            }
            synchronized (this.mNetworkPoliciesSecondLock) {
                updateNotificationsNL();
                writePolicyAL();
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void sendRestrictBackgroundChangedMsg() {
        this.mHandler.removeMessages(6);
        this.mHandler.obtainMessage(6, this.mRestrictBackground ? 1 : 0, 0).sendToTarget();
    }

    public int getRestrictBackgroundByCaller() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
        return getRestrictBackgroundStatusInternal(Binder.getCallingUid());
    }

    public int getRestrictBackgroundStatus(int uid) {
        PermissionUtils.enforceNetworkStackPermission(this.mContext);
        return getRestrictBackgroundStatusInternal(uid);
    }

    private int getRestrictBackgroundStatusInternal(int uid) {
        synchronized (this.mUidRulesFirstLock) {
            long token = Binder.clearCallingIdentity();
            int policy = getUidPolicy(uid);
            Binder.restoreCallingIdentity(token);
            int i = 3;
            if (policy == 1) {
                return 3;
            }
            if (this.mRestrictBackground) {
                if ((this.mUidPolicy.get(uid) & 4) != 0) {
                    i = 2;
                }
                return i;
            }
            return 1;
        }
    }

    public boolean getRestrictBackground() {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesFirstLock) {
            z = this.mRestrictBackground;
        }
        return z;
    }

    public void setDeviceIdleMode(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        Trace.traceBegin(2097152L, "setDeviceIdleMode");
        try {
            synchronized (this.mUidRulesFirstLock) {
                if (this.mDeviceIdleMode == enabled) {
                    return;
                }
                this.mDeviceIdleMode = enabled;
                this.mLogger.deviceIdleModeEnabled(enabled);
                if (this.mSystemReady) {
                    handleDeviceIdleModeChangedUL(enabled);
                }
                if (enabled) {
                    EventLogTags.writeDeviceIdleOnPhase("net");
                } else {
                    EventLogTags.writeDeviceIdleOffPhase("net");
                }
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    public void setWifiMeteredOverride(String networkId, int meteredOverride) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long token = Binder.clearCallingIdentity();
        try {
            WifiManager wm = (WifiManager) this.mContext.getSystemService(WifiManager.class);
            List<WifiConfiguration> configs = wm.getConfiguredNetworks();
            for (WifiConfiguration config : configs) {
                if (Objects.equals(NetworkPolicyManager.resolveNetworkId(config), networkId)) {
                    config.meteredOverride = meteredOverride;
                    wm.updateNetwork(config);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void enforceSubscriptionPlanAccess(int subId, int callingUid, String callingPackage) {
        this.mAppOps.checkPackage(callingUid, callingPackage);
        long token = Binder.clearCallingIdentity();
        try {
            PersistableBundle config = this.mCarrierConfigManager.getConfigForSubId(subId);
            TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
            if (tm != null && tm.hasCarrierPrivileges(subId)) {
                return;
            }
            if (config != null) {
                String overridePackage = config.getString("config_plans_package_override_string", null);
                if (!TextUtils.isEmpty(overridePackage) && Objects.equals(overridePackage, callingPackage)) {
                    return;
                }
            }
            String defaultPackage = this.mCarrierConfigManager.getDefaultCarrierServicePackageName();
            if (TextUtils.isEmpty(defaultPackage) || !Objects.equals(defaultPackage, callingPackage)) {
                String testPackage = SystemProperties.get("persist.sys.sub_plan_owner." + subId, (String) null);
                if (TextUtils.isEmpty(testPackage) || !Objects.equals(testPackage, callingPackage)) {
                    String legacyTestPackage = SystemProperties.get("fw.sub_plan_owner." + subId, (String) null);
                    if (!TextUtils.isEmpty(legacyTestPackage) && Objects.equals(legacyTestPackage, callingPackage)) {
                        return;
                    }
                    this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_SUBSCRIPTION_PLANS", TAG);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void enforceSubscriptionPlanValidity(SubscriptionPlan[] plans) {
        if (plans.length == 0) {
            Log.d(TAG, "Received empty plans list. Clearing existing SubscriptionPlans.");
            return;
        }
        int[] allNetworkTypes = TelephonyManager.getAllNetworkTypes();
        ArraySet<Integer> allNetworksSet = new ArraySet<>();
        addAll(allNetworksSet, allNetworkTypes);
        ArraySet<Integer> applicableNetworkTypes = new ArraySet<>();
        boolean hasGeneralPlan = false;
        for (SubscriptionPlan subscriptionPlan : plans) {
            int[] planNetworkTypes = subscriptionPlan.getNetworkTypes();
            ArraySet<Integer> planNetworksSet = new ArraySet<>();
            for (int j = 0; j < planNetworkTypes.length; j++) {
                if (allNetworksSet.contains(Integer.valueOf(planNetworkTypes[j]))) {
                    if (!planNetworksSet.add(Integer.valueOf(planNetworkTypes[j]))) {
                        throw new IllegalArgumentException("Subscription plan contains duplicate network types.");
                    }
                } else {
                    throw new IllegalArgumentException("Invalid network type: " + planNetworkTypes[j]);
                }
            }
            int j2 = planNetworkTypes.length;
            if (j2 == allNetworkTypes.length) {
                hasGeneralPlan = true;
            } else if (!addAll(applicableNetworkTypes, planNetworkTypes)) {
                throw new IllegalArgumentException("Multiple subscription plans defined for a single network type.");
            }
        }
        if (!hasGeneralPlan) {
            throw new IllegalArgumentException("No generic subscription plan that applies to all network types.");
        }
    }

    private static boolean addAll(ArraySet<Integer> set, int... elements) {
        boolean result = true;
        for (int i : elements) {
            result &= set.add(Integer.valueOf(i));
        }
        return result;
    }

    public SubscriptionPlan getSubscriptionPlan(NetworkTemplate template) {
        SubscriptionPlan primarySubscriptionPlanLocked;
        enforceAnyPermissionOf("android.permission.MAINLINE_NETWORK_STACK");
        synchronized (this.mNetworkPoliciesSecondLock) {
            int subId = findRelevantSubIdNL(template);
            primarySubscriptionPlanLocked = getPrimarySubscriptionPlanLocked(subId);
        }
        return primarySubscriptionPlanLocked;
    }

    public void notifyStatsProviderWarningOrLimitReached() {
        enforceAnyPermissionOf("android.permission.MAINLINE_NETWORK_STACK");
        synchronized (this.mNetworkPoliciesSecondLock) {
            if (this.mSystemReady) {
                this.mHandler.obtainMessage(20).sendToTarget();
            }
        }
    }

    public SubscriptionPlan[] getSubscriptionPlans(int subId, String callingPackage) {
        enforceSubscriptionPlanAccess(subId, Binder.getCallingUid(), callingPackage);
        String fake = SystemProperties.get("fw.fake_plan");
        if (!TextUtils.isEmpty(fake)) {
            List<SubscriptionPlan> plans = new ArrayList<>();
            if ("month_hard".equals(fake)) {
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile").setDataLimit(DataUnit.GIBIBYTES.toBytes(5L), 1).setDataUsage(DataUnit.GIBIBYTES.toBytes(1L), ZonedDateTime.now().minusHours(36L).toInstant().toEpochMilli()).build());
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile Happy").setDataLimit(JobStatus.NO_LATEST_RUNTIME, 1).setDataUsage(DataUnit.GIBIBYTES.toBytes(5L), ZonedDateTime.now().minusHours(36L).toInstant().toEpochMilli()).build());
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, Charged after limit").setDataLimit(DataUnit.GIBIBYTES.toBytes(5L), 1).setDataUsage(DataUnit.GIBIBYTES.toBytes(5L), ZonedDateTime.now().minusHours(36L).toInstant().toEpochMilli()).build());
            } else if ("month_soft".equals(fake)) {
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile is the carriers name who this plan belongs to").setSummary("Crazy unlimited bandwidth plan with incredibly long title that should be cut off to prevent UI from looking terrible").setDataLimit(DataUnit.GIBIBYTES.toBytes(5L), 2).setDataUsage(DataUnit.GIBIBYTES.toBytes(1L), ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, Throttled after limit").setDataLimit(DataUnit.GIBIBYTES.toBytes(5L), 2).setDataUsage(DataUnit.GIBIBYTES.toBytes(5L), ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, No data connection after limit").setDataLimit(DataUnit.GIBIBYTES.toBytes(5L), 0).setDataUsage(DataUnit.GIBIBYTES.toBytes(5L), ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
            } else if ("month_over".equals(fake)) {
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile is the carriers name who this plan belongs to").setDataLimit(DataUnit.GIBIBYTES.toBytes(5L), 2).setDataUsage(DataUnit.GIBIBYTES.toBytes(6L), ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, Throttled after limit").setDataLimit(DataUnit.GIBIBYTES.toBytes(5L), 2).setDataUsage(DataUnit.GIBIBYTES.toBytes(5L), ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, No data connection after limit").setDataLimit(DataUnit.GIBIBYTES.toBytes(5L), 0).setDataUsage(DataUnit.GIBIBYTES.toBytes(5L), ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
            } else if ("month_none".equals(fake)) {
                plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile").build());
            } else if ("prepaid".equals(fake)) {
                plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(20L), ZonedDateTime.now().plusDays(10L)).setTitle("G-Mobile").setDataLimit(DataUnit.MEBIBYTES.toBytes(512L), 0).setDataUsage(DataUnit.MEBIBYTES.toBytes(100L), ZonedDateTime.now().minusHours(3L).toInstant().toEpochMilli()).build());
            } else if ("prepaid_crazy".equals(fake)) {
                plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(20L), ZonedDateTime.now().plusDays(10L)).setTitle("G-Mobile Anytime").setDataLimit(DataUnit.MEBIBYTES.toBytes(512L), 0).setDataUsage(DataUnit.MEBIBYTES.toBytes(100L), ZonedDateTime.now().minusHours(3L).toInstant().toEpochMilli()).build());
                plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(10L), ZonedDateTime.now().plusDays(20L)).setTitle("G-Mobile Nickel Nights").setSummary("5¢/GB between 1-5AM").setDataLimit(DataUnit.GIBIBYTES.toBytes(5L), 2).setDataUsage(DataUnit.MEBIBYTES.toBytes(15L), ZonedDateTime.now().minusHours(30L).toInstant().toEpochMilli()).build());
                plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(10L), ZonedDateTime.now().plusDays(20L)).setTitle("G-Mobile Bonus 3G").setSummary("Unlimited 3G data").setDataLimit(DataUnit.GIBIBYTES.toBytes(1L), 2).setDataUsage(DataUnit.MEBIBYTES.toBytes(300L), ZonedDateTime.now().minusHours(1L).toInstant().toEpochMilli()).build());
            } else if ("unlimited".equals(fake)) {
                plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(20L), ZonedDateTime.now().plusDays(10L)).setTitle("G-Mobile Awesome").setDataLimit(JobStatus.NO_LATEST_RUNTIME, 2).setDataUsage(DataUnit.MEBIBYTES.toBytes(50L), ZonedDateTime.now().minusHours(3L).toInstant().toEpochMilli()).build());
            }
            return (SubscriptionPlan[]) plans.toArray(new SubscriptionPlan[plans.size()]);
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            String ownerPackage = this.mSubscriptionPlansOwner.get(subId);
            if (!Objects.equals(ownerPackage, callingPackage) && UserHandle.getCallingAppId() != 1000 && UserHandle.getCallingAppId() != 1001) {
                Log.w(TAG, "Not returning plans because caller " + callingPackage + " doesn't match owner " + ownerPackage);
                return null;
            }
            return this.mSubscriptionPlans.get(subId);
        }
    }

    public void setSubscriptionPlans(int subId, SubscriptionPlan[] plans, long expirationDurationMillis, String callingPackage) {
        enforceSubscriptionPlanAccess(subId, Binder.getCallingUid(), callingPackage);
        enforceSubscriptionPlanValidity(plans);
        for (SubscriptionPlan plan : plans) {
            Objects.requireNonNull(plan);
        }
        long token = Binder.clearCallingIdentity();
        try {
            setSubscriptionPlansInternal(subId, plans, expirationDurationMillis, callingPackage);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSubscriptionPlansInternal(int subId, SubscriptionPlan[] plans, long expirationDurationMillis, String callingPackage) {
        synchronized (this.mUidRulesFirstLock) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                this.mSubscriptionPlans.put(subId, plans);
                this.mSubscriptionPlansOwner.put(subId, callingPackage);
                String subscriberId = this.mSubIdToSubscriberId.get(subId, null);
                if (subscriberId != null) {
                    ensureActiveCarrierPolicyAL(subId, subscriberId);
                    maybeUpdateCarrierPolicyCycleAL(subId, subscriberId);
                } else {
                    Slog.wtf(TAG, "Missing subscriberId for subId " + subId);
                }
                handleNetworkPoliciesUpdateAL(true);
                Intent intent = new Intent("android.telephony.action.SUBSCRIPTION_PLANS_CHANGED");
                intent.addFlags(1073741824);
                intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", subId);
                this.mContext.sendBroadcast(intent, "android.permission.MANAGE_SUBSCRIPTION_PLANS");
                Handler handler = this.mHandler;
                handler.sendMessage(handler.obtainMessage(19, subId, 0, plans));
                int setPlansId = this.mSetSubscriptionPlansIdCounter;
                this.mSetSubscriptionPlansIdCounter = setPlansId + 1;
                this.mSetSubscriptionPlansIds.put(subId, setPlansId);
                if (expirationDurationMillis > 0) {
                    Handler handler2 = this.mHandler;
                    handler2.sendMessageDelayed(handler2.obtainMessage(22, subId, setPlansId, callingPackage), expirationDurationMillis);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSubscriptionPlansOwner(int subId, String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", TAG);
        SystemProperties.set("persist.sys.sub_plan_owner." + subId, packageName);
    }

    public String getSubscriptionPlansOwner(int subId) {
        String str;
        if (UserHandle.getCallingAppId() != 1000) {
            throw new SecurityException();
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            str = this.mSubscriptionPlansOwner.get(subId);
        }
        return str;
    }

    public void setSubscriptionOverride(int subId, int overrideMask, int overrideValue, int[] networkTypes, long expirationDurationMillis, String callingPackage) {
        enforceSubscriptionPlanAccess(subId, Binder.getCallingUid(), callingPackage);
        ArraySet<Integer> allNetworksSet = new ArraySet<>();
        addAll(allNetworksSet, TelephonyManager.getAllNetworkTypes());
        IntArray applicableNetworks = new IntArray();
        for (int networkType : networkTypes) {
            if (allNetworksSet.contains(Integer.valueOf(networkType))) {
                applicableNetworks.add(networkType);
            } else {
                Log.d(TAG, "setSubscriptionOverride removing invalid network type: " + networkType);
            }
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            try {
                try {
                    SubscriptionPlan plan = getPrimarySubscriptionPlanLocked(subId);
                    if ((overrideMask == 1 || plan != null) && plan.getDataLimitBehavior() != -1) {
                        boolean overrideEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "netpolicy_override_enabled", 1) != 0;
                        if (overrideEnabled || overrideValue == 0) {
                            SomeArgs args = SomeArgs.obtain();
                            args.arg1 = Integer.valueOf(subId);
                            args.arg2 = Integer.valueOf(overrideMask);
                            args.arg3 = Integer.valueOf(overrideValue);
                            args.arg4 = applicableNetworks.toArray();
                            Handler handler = this.mHandler;
                            handler.sendMessage(handler.obtainMessage(16, args));
                            if (expirationDurationMillis > 0) {
                                args.arg3 = 0;
                                Handler handler2 = this.mHandler;
                                handler2.sendMessageDelayed(handler2.obtainMessage(16, args), expirationDurationMillis);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    throw new IllegalStateException("Must provide valid SubscriptionPlan to enable overriding");
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public int getMultipathPreference(Network network) {
        PermissionUtils.enforceNetworkStackPermission(this.mContext);
        Integer preference = this.mMultipathPolicyTracker.getMultipathPreference(network);
        if (preference != null) {
            return preference.intValue();
        }
        return 0;
    }

    @NeverCompile
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            IndentingPrintWriter fout = new IndentingPrintWriter(writer, "  ");
            ArraySet<String> argSet = new ArraySet<>(args.length);
            for (String arg : args) {
                argSet.add(arg);
            }
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    if (argSet.contains("--unsnooze")) {
                        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
                            this.mNetworkPolicy.valueAt(i).clearSnooze();
                        }
                        handleNetworkPoliciesUpdateAL(true);
                        fout.println("Cleared snooze timestamps");
                        return;
                    }
                    fout.print("System ready: ");
                    fout.println(this.mSystemReady);
                    fout.print("Restrict background: ");
                    fout.println(this.mRestrictBackground);
                    fout.print("Restrict power: ");
                    fout.println(this.mRestrictPower);
                    fout.print("Device idle: ");
                    fout.println(this.mDeviceIdleMode);
                    fout.print("Restricted networking mode: ");
                    fout.println(this.mRestrictedNetworkingMode);
                    fout.print("Low Power Standby mode: ");
                    fout.println(this.mLowPowerStandbyActive);
                    synchronized (this.mMeteredIfacesLock) {
                        fout.print("Metered ifaces: ");
                        fout.println(this.mMeteredIfaces);
                    }
                    fout.println();
                    fout.println("mRestrictBackgroundLowPowerMode: " + this.mRestrictBackgroundLowPowerMode);
                    fout.println("mRestrictBackgroundBeforeBsm: " + this.mRestrictBackgroundBeforeBsm);
                    fout.println("mLoadedRestrictBackground: " + this.mLoadedRestrictBackground);
                    fout.println("mRestrictBackgroundChangedInBsm: " + this.mRestrictBackgroundChangedInBsm);
                    fout.println();
                    fout.println("Network policies:");
                    fout.increaseIndent();
                    for (int i2 = 0; i2 < this.mNetworkPolicy.size(); i2++) {
                        fout.println(this.mNetworkPolicy.valueAt(i2).toString());
                    }
                    fout.decreaseIndent();
                    fout.println();
                    fout.println("Subscription plans:");
                    fout.increaseIndent();
                    for (int i3 = 0; i3 < this.mSubscriptionPlans.size(); i3++) {
                        int subId = this.mSubscriptionPlans.keyAt(i3);
                        fout.println("Subscriber ID " + subId + ":");
                        fout.increaseIndent();
                        SubscriptionPlan[] plans = this.mSubscriptionPlans.valueAt(i3);
                        if (!ArrayUtils.isEmpty(plans)) {
                            for (SubscriptionPlan plan : plans) {
                                fout.println(plan);
                            }
                        }
                        fout.decreaseIndent();
                    }
                    fout.decreaseIndent();
                    fout.println();
                    fout.println("Active subscriptions:");
                    fout.increaseIndent();
                    for (int i4 = 0; i4 < this.mSubIdToSubscriberId.size(); i4++) {
                        int subId2 = this.mSubIdToSubscriberId.keyAt(i4);
                        String subscriberId = this.mSubIdToSubscriberId.valueAt(i4);
                        fout.println(subId2 + "=" + NetworkIdentityUtils.scrubSubscriberId(subscriberId));
                    }
                    fout.decreaseIndent();
                    fout.println();
                    for (String[] mergedSubscribers : this.mMergedSubscriberIds) {
                        fout.println("Merged subscriptions: " + Arrays.toString(NetworkIdentityUtils.scrubSubscriberIds(mergedSubscribers)));
                    }
                    fout.println();
                    fout.println("Policy for UIDs:");
                    fout.increaseIndent();
                    int size = this.mUidPolicy.size();
                    for (int i5 = 0; i5 < size; i5++) {
                        int uid = this.mUidPolicy.keyAt(i5);
                        int policy = this.mUidPolicy.valueAt(i5);
                        fout.print("UID=");
                        fout.print(uid);
                        fout.print(" policy=");
                        fout.print(NetworkPolicyManager.uidPoliciesToString(policy));
                        fout.println();
                    }
                    fout.decreaseIndent();
                    int size2 = this.mPowerSaveWhitelistExceptIdleAppIds.size();
                    if (size2 > 0) {
                        fout.println("Power save whitelist (except idle) app ids:");
                        fout.increaseIndent();
                        for (int i6 = 0; i6 < size2; i6++) {
                            fout.print("UID=");
                            fout.print(this.mPowerSaveWhitelistExceptIdleAppIds.keyAt(i6));
                            fout.print(": ");
                            fout.print(this.mPowerSaveWhitelistExceptIdleAppIds.valueAt(i6));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    int size3 = this.mPowerSaveWhitelistAppIds.size();
                    if (size3 > 0) {
                        fout.println("Power save whitelist app ids:");
                        fout.increaseIndent();
                        for (int i7 = 0; i7 < size3; i7++) {
                            fout.print("UID=");
                            fout.print(this.mPowerSaveWhitelistAppIds.keyAt(i7));
                            fout.print(": ");
                            fout.print(this.mPowerSaveWhitelistAppIds.valueAt(i7));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    int size4 = this.mAppIdleTempWhitelistAppIds.size();
                    if (size4 > 0) {
                        fout.println("App idle whitelist app ids:");
                        fout.increaseIndent();
                        for (int i8 = 0; i8 < size4; i8++) {
                            fout.print("UID=");
                            fout.print(this.mAppIdleTempWhitelistAppIds.keyAt(i8));
                            fout.print(": ");
                            fout.print(this.mAppIdleTempWhitelistAppIds.valueAt(i8));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    int size5 = this.mDefaultRestrictBackgroundAllowlistUids.size();
                    if (size5 > 0) {
                        fout.println("Default restrict background allowlist uids:");
                        fout.increaseIndent();
                        for (int i9 = 0; i9 < size5; i9++) {
                            fout.print("UID=");
                            fout.print(this.mDefaultRestrictBackgroundAllowlistUids.keyAt(i9));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    int size6 = this.mRestrictBackgroundAllowlistRevokedUids.size();
                    if (size6 > 0) {
                        fout.println("Default restrict background allowlist uids revoked by users:");
                        fout.increaseIndent();
                        for (int i10 = 0; i10 < size6; i10++) {
                            fout.print("UID=");
                            fout.print(this.mRestrictBackgroundAllowlistRevokedUids.keyAt(i10));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    int size7 = this.mLowPowerStandbyAllowlistUids.size();
                    if (size7 > 0) {
                        fout.println("Low Power Standby allowlist uids:");
                        fout.increaseIndent();
                        for (int i11 = 0; i11 < size7; i11++) {
                            fout.print("UID=");
                            fout.print(this.mLowPowerStandbyAllowlistUids.keyAt(i11));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    SparseBooleanArray knownUids = new SparseBooleanArray();
                    collectKeys(this.mUidState, knownUids);
                    synchronized (this.mUidBlockedState) {
                        collectKeys(this.mUidBlockedState, knownUids);
                    }
                    fout.println("Status for all known UIDs:");
                    fout.increaseIndent();
                    int size8 = knownUids.size();
                    for (int i12 = 0; i12 < size8; i12++) {
                        int uid2 = knownUids.keyAt(i12);
                        fout.print("UID=");
                        fout.print(uid2);
                        NetworkPolicyManager.UidState uidState = this.mUidState.get(uid2);
                        if (uidState == null) {
                            fout.print(" state={null}");
                        } else {
                            fout.print(" state=");
                            fout.print(uidState.toString());
                        }
                        synchronized (this.mUidBlockedState) {
                            UidBlockedState uidBlockedState = this.mUidBlockedState.get(uid2);
                            if (uidBlockedState == null) {
                                fout.print(" blocked_state={null}");
                            } else {
                                fout.print(" blocked_state=");
                                fout.print(uidBlockedState);
                            }
                        }
                        fout.println();
                    }
                    fout.decreaseIndent();
                    fout.println("Admin restricted uids for metered data:");
                    fout.increaseIndent();
                    int size9 = this.mMeteredRestrictedUids.size();
                    for (int i13 = 0; i13 < size9; i13++) {
                        fout.print("u" + this.mMeteredRestrictedUids.keyAt(i13) + ": ");
                        fout.println(this.mMeteredRestrictedUids.valueAt(i13));
                    }
                    fout.decreaseIndent();
                    fout.println();
                    this.mStatLogger.dump(fout);
                    this.mLogger.dumpLogs(fout);
                    fout.println();
                    this.mMultipathPolicyTracker.dump(fout);
                }
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.android.server.net.NetworkPolicyManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public int handleShellCommand(ParcelFileDescriptor in, ParcelFileDescriptor out, ParcelFileDescriptor err, String[] args) {
        return new NetworkPolicyManagerShellCommand(this.mContext, this).exec(this, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDebugUid(int uid) {
        this.mLogger.setDebugUid(uid);
    }

    boolean isUidForeground(int uid) {
        boolean isProcStateAllowedWhileIdleOrPowerSaveMode;
        synchronized (this.mUidRulesFirstLock) {
            isProcStateAllowedWhileIdleOrPowerSaveMode = NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(this.mUidState.get(uid));
        }
        return isProcStateAllowedWhileIdleOrPowerSaveMode;
    }

    private boolean isUidForegroundOnRestrictBackgroundUL(int uid) {
        NetworkPolicyManager.UidState uidState = this.mUidState.get(uid);
        return ITranNetworkPolicyManagerService.Instance().isUidForegroundOnRestrictBackgroundUL(uidState, NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(uidState));
    }

    private boolean isUidForegroundOnRestrictPowerUL(int uid) {
        NetworkPolicyManager.UidState uidState = this.mUidState.get(uid);
        return ITranNetworkPolicyManagerService.Instance().isUidForegroundOnRestrictPowerUL(uidState, NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(uidState));
    }

    private boolean isUidTop(int uid) {
        NetworkPolicyManager.UidState uidState = this.mUidState.get(uid);
        return NetworkPolicyManager.isProcStateAllowedWhileInLowPowerStandby(uidState);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4177=4] */
    private boolean updateUidStateUL(int uid, int procState, long procStateSeq, int capability) {
        Trace.traceBegin(2097152L, "updateUidStateUL");
        try {
            NetworkPolicyManager.UidState oldUidState = this.mUidState.get(uid);
            if (oldUidState != null && procStateSeq < oldUidState.procStateSeq) {
                if (LOGV) {
                    Slog.v(TAG, "Ignoring older uid state updates; uid=" + uid + ",procState=" + ActivityManager.procStateToString(procState) + ",seq=" + procStateSeq + ",cap=" + capability + ",oldUidState=" + oldUidState);
                }
                Trace.traceEnd(2097152L);
                return false;
            }
            if (oldUidState != null && oldUidState.procState == procState) {
                try {
                    if (oldUidState.capability == capability) {
                        Trace.traceEnd(2097152L);
                        return false;
                    }
                } catch (Throwable th) {
                    th = th;
                    Trace.traceEnd(2097152L);
                    throw th;
                }
            }
            NetworkPolicyManager.UidState newUidState = new NetworkPolicyManager.UidState(uid, procState, procStateSeq, capability);
            this.mUidState.put(uid, newUidState);
            updateRestrictBackgroundRulesOnUidStatusChangedUL(uid, oldUidState, newUidState);
            boolean allowedWhileIdleOrPowerSaveModeChanged = NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(oldUidState) != NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(newUidState);
            if (allowedWhileIdleOrPowerSaveModeChanged) {
                updateRuleForAppIdleUL(uid, procState);
                if (this.mDeviceIdleMode) {
                    updateRuleForDeviceIdleUL(uid);
                }
                if (this.mRestrictPower) {
                    updateRuleForRestrictPowerUL(uid);
                }
                updateRulesForPowerRestrictionsUL(uid, procState);
            }
            if (this.mLowPowerStandbyActive) {
                boolean allowedInLpsChanged = NetworkPolicyManager.isProcStateAllowedWhileInLowPowerStandby(oldUidState) != NetworkPolicyManager.isProcStateAllowedWhileInLowPowerStandby(newUidState);
                if (allowedInLpsChanged) {
                    if (!allowedWhileIdleOrPowerSaveModeChanged) {
                        updateRulesForPowerRestrictionsUL(uid, procState);
                    }
                    updateRuleForLowPowerStandbyUL(uid);
                }
            }
            Trace.traceEnd(2097152L);
            return true;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private boolean removeUidStateUL(int uid) {
        int index = this.mUidState.indexOfKey(uid);
        if (index >= 0) {
            NetworkPolicyManager.UidState oldUidState = this.mUidState.valueAt(index);
            this.mUidState.removeAt(index);
            if (oldUidState != null) {
                updateRestrictBackgroundRulesOnUidStatusChangedUL(uid, oldUidState, null);
                if (this.mDeviceIdleMode) {
                    updateRuleForDeviceIdleUL(uid);
                }
                if (this.mRestrictPower) {
                    updateRuleForRestrictPowerUL(uid);
                }
                m4970x670de143(uid);
                if (this.mLowPowerStandbyActive) {
                    updateRuleForLowPowerStandbyUL(uid);
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    private void updateNetworkStats(int uid, boolean uidForeground) {
        if (Trace.isTagEnabled(2097152L)) {
            Trace.traceBegin(2097152L, "updateNetworkStats: " + uid + SliceClientPermissions.SliceAuthority.DELIMITER + (uidForeground ? "F" : "B"));
        }
        try {
            this.mNetworkStats.noteUidForeground(uid, uidForeground);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void updateRestrictBackgroundRulesOnUidStatusChangedUL(int uid, NetworkPolicyManager.UidState oldUidState, NetworkPolicyManager.UidState newUidState) {
        boolean oldForeground = NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(oldUidState);
        boolean newForeground = NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(newUidState);
        if (oldForeground != newForeground) {
            m4969x3105fe71(uid);
            ITranNetworkPolicyManagerService.Instance().updateRestrictBackgroundRulesOnUidStatusChangedUL(uid, oldUidState, newUidState);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRestrictedModeEnabled() {
        boolean z;
        synchronized (this.mUidRulesFirstLock) {
            z = this.mRestrictedNetworkingMode;
        }
        return z;
    }

    void updateRestrictedModeAllowlistUL() {
        this.mUidFirewallRestrictedModeRules.clear();
        forEachUid("updateRestrictedModeAllowlist", new IntConsumer() { // from class: com.android.server.net.NetworkPolicyManagerService$$ExternalSyntheticLambda1
            @Override // java.util.function.IntConsumer
            public final void accept(int i) {
                NetworkPolicyManagerService.this.m4968x1f7d5ed3(i);
            }
        });
        if (this.mRestrictedNetworkingMode) {
            setUidFirewallRulesUL(4, this.mUidFirewallRestrictedModeRules);
        }
        enableFirewallChainUL(4, this.mRestrictedNetworkingMode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateRestrictedModeAllowlistUL$3$com-android-server-net-NetworkPolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m4968x1f7d5ed3(int uid) {
        synchronized (this.mUidRulesFirstLock) {
            int effectiveBlockedReasons = updateBlockedReasonsForRestrictedModeUL(uid);
            int newFirewallRule = getRestrictedModeFirewallRule(effectiveBlockedReasons);
            if (newFirewallRule != 0) {
                this.mUidFirewallRestrictedModeRules.append(uid, newFirewallRule);
            }
        }
    }

    void updateRestrictedModeForUidUL(int uid) {
        int effectiveBlockedReasons = updateBlockedReasonsForRestrictedModeUL(uid);
        if (this.mRestrictedNetworkingMode) {
            setUidFirewallRuleUL(4, uid, getRestrictedModeFirewallRule(effectiveBlockedReasons));
        }
    }

    private int updateBlockedReasonsForRestrictedModeUL(int uid) {
        int oldEffectiveBlockedReasons;
        int newEffectiveBlockedReasons;
        int deriveUidRules;
        int uidRules;
        boolean hasRestrictedModeAccess = hasRestrictedModeAccess(uid);
        synchronized (this.mUidBlockedState) {
            UidBlockedState uidBlockedState = getOrCreateUidBlockedStateForUid(this.mUidBlockedState, uid);
            oldEffectiveBlockedReasons = uidBlockedState.effectiveBlockedReasons;
            if (this.mRestrictedNetworkingMode) {
                uidBlockedState.blockedReasons |= 8;
            } else {
                uidBlockedState.blockedReasons &= -9;
            }
            if (hasRestrictedModeAccess) {
                uidBlockedState.allowedReasons |= 16;
            } else {
                uidBlockedState.allowedReasons &= -17;
            }
            uidBlockedState.updateEffectiveBlockedReasons();
            newEffectiveBlockedReasons = uidBlockedState.effectiveBlockedReasons;
            if (oldEffectiveBlockedReasons == newEffectiveBlockedReasons) {
                deriveUidRules = 0;
            } else {
                deriveUidRules = uidBlockedState.deriveUidRules();
            }
            uidRules = deriveUidRules;
        }
        if (oldEffectiveBlockedReasons != newEffectiveBlockedReasons) {
            handleBlockedReasonsChanged(uid, newEffectiveBlockedReasons, oldEffectiveBlockedReasons);
            postUidRulesChangedMsg(uid, uidRules);
        }
        return newEffectiveBlockedReasons;
    }

    private static int getRestrictedModeFirewallRule(int effectiveBlockedReasons) {
        if ((effectiveBlockedReasons & 8) != 0) {
            return 0;
        }
        return 1;
    }

    private boolean hasRestrictedModeAccess(int uid) {
        try {
            if (this.mIPm.checkUidPermission("android.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS", uid) != 0 && this.mIPm.checkUidPermission("android.permission.NETWORK_STACK", uid) != 0) {
                if (this.mIPm.checkUidPermission("android.permission.MAINLINE_NETWORK_STACK", uid) != 0) {
                    return false;
                }
            }
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    void updateRulesForPowerSaveUL() {
        Trace.traceBegin(2097152L, "updateRulesForPowerSaveUL");
        try {
            updateRulesForWhitelistedPowerSaveUL(this.mRestrictPower, 3, this.mUidFirewallPowerSaveRules);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    void updateRuleForRestrictPowerUL(int uid) {
        updateRulesForWhitelistedPowerSaveUL(uid, this.mRestrictPower, 3);
    }

    void updateRulesForDeviceIdleUL() {
        Trace.traceBegin(2097152L, "updateRulesForDeviceIdleUL");
        try {
            updateRulesForWhitelistedPowerSaveUL(this.mDeviceIdleMode, 1, this.mUidFirewallDozableRules);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    void updateRuleForDeviceIdleUL(int uid) {
        updateRulesForWhitelistedPowerSaveUL(uid, this.mDeviceIdleMode, 1);
    }

    private void updateRulesForWhitelistedPowerSaveUL(boolean enabled, int chain, SparseIntArray rules) {
        if (enabled) {
            rules.clear();
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int ui = users.size() - 1; ui >= 0; ui--) {
                UserInfo user = users.get(ui);
                updateRulesForWhitelistedAppIds(rules, this.mPowerSaveTempWhitelistAppIds, user.id);
                updateRulesForWhitelistedAppIds(rules, this.mPowerSaveWhitelistAppIds, user.id);
                if (chain == 3) {
                    updateRulesForWhitelistedAppIds(rules, this.mPowerSaveWhitelistExceptIdleAppIds, user.id);
                }
            }
            for (int i = this.mUidState.size() - 1; i >= 0; i--) {
                if (NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(this.mUidState.valueAt(i))) {
                    rules.put(this.mUidState.keyAt(i), 1);
                }
            }
            setUidFirewallRulesUL(chain, rules, 1);
            return;
        }
        setUidFirewallRulesUL(chain, null, 2);
    }

    private void updateRulesForWhitelistedAppIds(SparseIntArray uidRules, SparseBooleanArray whitelistedAppIds, int userId) {
        for (int i = whitelistedAppIds.size() - 1; i >= 0; i--) {
            if (whitelistedAppIds.valueAt(i)) {
                int appId = whitelistedAppIds.keyAt(i);
                int uid = UserHandle.getUid(userId, appId);
                if (ITranNetworkPolicyManagerService.Instance().updateRulesForWhitelistedAppIds(uid)) {
                    uidRules.put(uid, 1);
                }
            }
        }
    }

    void updateRulesForLowPowerStandbyUL() {
        Trace.traceBegin(2097152L, "updateRulesForLowPowerStandbyUL");
        try {
            if (this.mLowPowerStandbyActive) {
                this.mUidFirewallLowPowerStandbyModeRules.clear();
                for (int i = this.mUidState.size() - 1; i >= 0; i--) {
                    int uid = this.mUidState.keyAt(i);
                    int effectiveBlockedReasons = getEffectiveBlockedReasons(uid);
                    if (hasInternetPermissionUL(uid) && (effectiveBlockedReasons & 32) == 0) {
                        this.mUidFirewallLowPowerStandbyModeRules.put(uid, 1);
                    }
                }
                setUidFirewallRulesUL(5, this.mUidFirewallLowPowerStandbyModeRules, 1);
            } else {
                setUidFirewallRulesUL(5, null, 2);
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    void updateRuleForLowPowerStandbyUL(int uid) {
        if (!hasInternetPermissionUL(uid)) {
            return;
        }
        int effectiveBlockedReasons = getEffectiveBlockedReasons(uid);
        if (this.mUidState.contains(uid) && (effectiveBlockedReasons & 32) == 0) {
            this.mUidFirewallLowPowerStandbyModeRules.put(uid, 1);
            setUidFirewallRuleUL(5, uid, 1);
            return;
        }
        this.mUidFirewallLowPowerStandbyModeRules.delete(uid);
        setUidFirewallRuleUL(5, uid, 0);
    }

    private boolean isWhitelistedFromPowerSaveUL(int uid, boolean deviceIdleMode) {
        int appId = UserHandle.getAppId(uid);
        boolean z = false;
        boolean isWhitelisted = this.mPowerSaveTempWhitelistAppIds.get(appId) || this.mPowerSaveWhitelistAppIds.get(appId);
        if (!deviceIdleMode) {
            if (isWhitelisted || isWhitelistedFromPowerSaveExceptIdleUL(uid)) {
                z = true;
            }
            isWhitelisted = z;
        }
        return ITranNetworkPolicyManagerService.Instance().isWhitelistedFromPowerSaveUL(uid, isWhitelisted);
    }

    private boolean isWhitelistedFromPowerSaveExceptIdleUL(int uid) {
        int appId = UserHandle.getAppId(uid);
        return this.mPowerSaveWhitelistExceptIdleAppIds.get(appId);
    }

    private boolean isAllowlistedFromLowPowerStandbyUL(int uid) {
        return this.mLowPowerStandbyAllowlistUids.get(uid);
    }

    private void updateRulesForWhitelistedPowerSaveUL(int uid, boolean enabled, int chain) {
        if (enabled) {
            boolean isWhitelisted = isWhitelistedFromPowerSaveUL(uid, chain == 1);
            if (isWhitelisted || isUidForegroundOnRestrictPowerUL(uid)) {
                setUidFirewallRuleUL(chain, uid, 1);
            } else {
                setUidFirewallRuleUL(chain, uid, 0);
            }
        }
    }

    void updateRulesForAppIdleUL() {
        Trace.traceBegin(2097152L, "updateRulesForAppIdleUL");
        try {
            SparseIntArray uidRules = this.mUidFirewallStandbyRules;
            uidRules.clear();
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int ui = users.size() - 1; ui >= 0; ui--) {
                UserInfo user = users.get(ui);
                int[] idleUids = this.mUsageStats.getIdleUidsForUser(user.id);
                for (int uid : idleUids) {
                    if (!this.mPowerSaveTempWhitelistAppIds.get(UserHandle.getAppId(uid), false) && hasInternetPermissionUL(uid) && !isUidForegroundOnRestrictPowerUL(uid)) {
                        uidRules.put(uid, 2);
                    }
                }
            }
            setUidFirewallRulesUL(2, uidRules, 0);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    void updateRuleForAppIdleUL(int uid, int uidProcessState) {
        if (isUidValidForDenylistRulesUL(uid)) {
            if (Trace.isTagEnabled(2097152L)) {
                Trace.traceBegin(2097152L, "updateRuleForAppIdleUL: " + uid);
            }
            try {
                int appId = UserHandle.getAppId(uid);
                if (!this.mPowerSaveTempWhitelistAppIds.get(appId) && isUidIdle(uid, uidProcessState) && !isUidForegroundOnRestrictPowerUL(uid)) {
                    setUidFirewallRuleUL(2, uid, 2);
                    if (LOGD) {
                        Log.d(TAG, "updateRuleForAppIdleUL DENY " + uid);
                    }
                } else {
                    setUidFirewallRuleUL(2, uid, 0);
                    if (LOGD) {
                        Log.d(TAG, "updateRuleForAppIdleUL " + uid + " to DEFAULT");
                    }
                }
            } finally {
                Trace.traceEnd(2097152L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRulesForAppIdleParoleUL() {
        if (ITranNetworkPolicyManagerService.Instance().updateRulesForAppIdleParoleUL()) {
            return;
        }
        boolean paroled = this.mAppStandby.isInParole();
        boolean enableChain = !paroled;
        int ruleCount = this.mUidFirewallStandbyRules.size();
        SparseIntArray blockedUids = new SparseIntArray();
        int i = 0;
        while (true) {
            boolean isUidIdle = true;
            if (i >= ruleCount) {
                break;
            }
            int uid = this.mUidFirewallStandbyRules.keyAt(i);
            if (isUidValidForDenylistRulesUL(uid)) {
                int blockedReasons = getBlockedReasons(uid);
                if (enableChain || (65535 & blockedReasons) != 0) {
                    if (paroled || !isUidIdle(uid)) {
                        isUidIdle = false;
                    }
                    if (!isUidIdle || this.mPowerSaveTempWhitelistAppIds.get(UserHandle.getAppId(uid)) || isUidForegroundOnRestrictPowerUL(uid)) {
                        this.mUidFirewallStandbyRules.put(uid, 0);
                    } else {
                        this.mUidFirewallStandbyRules.put(uid, 2);
                        blockedUids.put(uid, 2);
                    }
                    updateRulesForPowerRestrictionsUL(uid, isUidIdle);
                }
            }
            i++;
        }
        setUidFirewallRulesUL(2, blockedUids, enableChain ? 1 : 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRulesForGlobalChangeAL(boolean restrictedNetworksChanged) {
        if (Trace.isTagEnabled(2097152L)) {
            Trace.traceBegin(2097152L, "updateRulesForGlobalChangeAL: " + (restrictedNetworksChanged ? "R" : "-"));
        }
        try {
            updateRulesForAppIdleUL();
            updateRulesForRestrictPowerUL();
            updateRulesForRestrictBackgroundUL();
            updateRestrictedModeAllowlistUL();
            if (restrictedNetworksChanged) {
                normalizePoliciesNL();
                updateNetworkRulesNL();
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void handleDeviceIdleModeChangedUL(boolean enabled) {
        Trace.traceBegin(2097152L, "updateRulesForRestrictPowerUL");
        try {
            updateRulesForDeviceIdleUL();
            if (enabled) {
                forEachUid("updateRulesForRestrictPower", new IntConsumer() { // from class: com.android.server.net.NetworkPolicyManagerService$$ExternalSyntheticLambda6
                    @Override // java.util.function.IntConsumer
                    public final void accept(int i) {
                        NetworkPolicyManagerService.this.m4965xc6996253(i);
                    }
                });
            } else {
                handleDeviceIdleModeDisabledUL();
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleDeviceIdleModeChangedUL$4$com-android-server-net-NetworkPolicyManagerService  reason: not valid java name */
    public /* synthetic */ void m4965xc6996253(int uid) {
        synchronized (this.mUidRulesFirstLock) {
            m4970x670de143(uid);
        }
    }

    private void handleDeviceIdleModeDisabledUL() {
        Trace.traceBegin(2097152L, "handleDeviceIdleModeDisabledUL");
        try {
            SparseArray<SomeArgs> uidStateUpdates = new SparseArray<>();
            synchronized (this.mUidBlockedState) {
                int size = this.mUidBlockedState.size();
                for (int i = 0; i < size; i++) {
                    int uid = this.mUidBlockedState.keyAt(i);
                    UidBlockedState uidBlockedState = this.mUidBlockedState.valueAt(i);
                    if ((uidBlockedState.blockedReasons & 2) != 0) {
                        uidBlockedState.blockedReasons &= -3;
                        int oldEffectiveBlockedReasons = uidBlockedState.effectiveBlockedReasons;
                        uidBlockedState.updateEffectiveBlockedReasons();
                        if (LOGV) {
                            Log.v(TAG, "handleDeviceIdleModeDisabled(" + uid + "); newUidBlockedState=" + uidBlockedState + ", oldEffectiveBlockedReasons=" + oldEffectiveBlockedReasons);
                        }
                        if (oldEffectiveBlockedReasons != uidBlockedState.effectiveBlockedReasons) {
                            SomeArgs someArgs = SomeArgs.obtain();
                            someArgs.argi1 = oldEffectiveBlockedReasons;
                            someArgs.argi2 = uidBlockedState.effectiveBlockedReasons;
                            someArgs.argi3 = uidBlockedState.deriveUidRules();
                            uidStateUpdates.append(uid, someArgs);
                            this.mActivityManagerInternal.onUidBlockedReasonsChanged(uid, uidBlockedState.effectiveBlockedReasons);
                        }
                    }
                }
            }
            if (uidStateUpdates.size() != 0) {
                this.mHandler.obtainMessage(23, uidStateUpdates).sendToTarget();
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRulesForRestrictPowerUL() {
        Trace.traceBegin(2097152L, "updateRulesForRestrictPowerUL");
        try {
            updateRulesForDeviceIdleUL();
            updateRulesForPowerSaveUL();
            forEachUid("updateRulesForRestrictPower", new IntConsumer() { // from class: com.android.server.net.NetworkPolicyManagerService$$ExternalSyntheticLambda0
                @Override // java.util.function.IntConsumer
                public final void accept(int i) {
                    NetworkPolicyManagerService.this.m4970x670de143(i);
                }
            });
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void updateRulesForRestrictBackgroundUL() {
        Trace.traceBegin(2097152L, "updateRulesForRestrictBackgroundUL");
        try {
            forEachUid("updateRulesForRestrictBackground", new IntConsumer() { // from class: com.android.server.net.NetworkPolicyManagerService$$ExternalSyntheticLambda4
                @Override // java.util.function.IntConsumer
                public final void accept(int i) {
                    NetworkPolicyManagerService.this.m4969x3105fe71(i);
                }
            });
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forEachUid(String tag, final IntConsumer consumer) {
        if (Trace.isTagEnabled(2097152L)) {
            Trace.traceBegin(2097152L, "forEachUid-" + tag);
        }
        try {
            Trace.traceBegin(2097152L, "list-users");
            List<UserInfo> users = this.mUserManager.getUsers();
            Trace.traceEnd(2097152L);
            Trace.traceBegin(2097152L, "iterate-uids");
            PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            int usersSize = users.size();
            for (int i = 0; i < usersSize; i++) {
                final int userId = users.get(i).id;
                final SparseBooleanArray sharedAppIdsHandled = new SparseBooleanArray();
                packageManagerInternal.forEachInstalledPackage(new Consumer() { // from class: com.android.server.net.NetworkPolicyManagerService$$ExternalSyntheticLambda5
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        NetworkPolicyManagerService.lambda$forEachUid$7(sharedAppIdsHandled, userId, consumer, (AndroidPackage) obj);
                    }
                }, userId);
            }
            Trace.traceEnd(2097152L);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$forEachUid$7(SparseBooleanArray sharedAppIdsHandled, int userId, IntConsumer consumer, AndroidPackage androidPackage) {
        int appId = androidPackage.getUid();
        if (androidPackage.getSharedUserId() != null) {
            if (sharedAppIdsHandled.indexOfKey(appId) < 0) {
                sharedAppIdsHandled.put(appId, true);
            } else {
                return;
            }
        }
        int uid = UserHandle.getUid(userId, appId);
        consumer.accept(uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRulesForTempWhitelistChangeUL(int appId) {
        List<UserInfo> users = this.mUserManager.getUsers();
        int numUsers = users.size();
        for (int i = 0; i < numUsers; i++) {
            UserInfo user = users.get(i);
            int uid = UserHandle.getUid(user.id, appId);
            updateRuleForAppIdleUL(uid, -1);
            updateRuleForDeviceIdleUL(uid);
            updateRuleForRestrictPowerUL(uid);
            m4970x670de143(uid);
        }
    }

    private boolean isUidValidForDenylistRulesUL(int uid) {
        if (uid == 1013 || uid == 1019 || isUidValidForAllowlistRulesUL(uid)) {
            return true;
        }
        return false;
    }

    private boolean isUidValidForAllowlistRulesUL(int uid) {
        return UserHandle.isApp(uid) && hasInternetPermissionUL(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAppIdleWhitelist(int uid, boolean shouldWhitelist) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesFirstLock) {
            if (this.mAppIdleTempWhitelistAppIds.get(uid) == shouldWhitelist) {
                return;
            }
            long token = Binder.clearCallingIdentity();
            this.mLogger.appIdleWlChanged(uid, shouldWhitelist);
            if (shouldWhitelist) {
                this.mAppIdleTempWhitelistAppIds.put(uid, true);
            } else {
                this.mAppIdleTempWhitelistAppIds.delete(uid);
            }
            updateRuleForAppIdleUL(uid, -1);
            m4970x670de143(uid);
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] getAppIdleWhitelist() {
        int[] uids;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesFirstLock) {
            int len = this.mAppIdleTempWhitelistAppIds.size();
            uids = new int[len];
            for (int i = 0; i < len; i++) {
                uids[i] = this.mAppIdleTempWhitelistAppIds.keyAt(i);
            }
        }
        return uids;
    }

    boolean isUidIdle(int uid) {
        return isUidIdle(uid, -1);
    }

    private boolean isUidIdle(int uid, int uidProcessState) {
        synchronized (this.mUidRulesFirstLock) {
            if (uidProcessState != -1) {
                if (ActivityManager.isProcStateConsideredInteraction(uidProcessState)) {
                    return false;
                }
            }
            if (this.mAppIdleTempWhitelistAppIds.get(uid)) {
                return false;
            }
            String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
            int userId = UserHandle.getUserId(uid);
            if (packages != null) {
                for (String packageName : packages) {
                    if (!this.mUsageStats.isAppIdle(packageName, uid, userId)) {
                        return false;
                    }
                }
                return true;
            }
            return true;
        }
    }

    private boolean hasInternetPermissionUL(int uid) {
        try {
            if (this.mInternetPermissionMap.get(uid)) {
                return true;
            }
            boolean hasPermission = this.mIPm.checkUidPermission("android.permission.INTERNET", uid) == 0;
            this.mInternetPermissionMap.put(uid, hasPermission);
            return hasPermission;
        } catch (RemoteException e) {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUidDeletedUL(int uid) {
        synchronized (this.mUidBlockedState) {
            this.mUidBlockedState.delete(uid);
        }
        this.mUidState.delete(uid);
        this.mActivityManagerInternal.onUidBlockedReasonsChanged(uid, 0);
        this.mUidPolicy.delete(uid);
        this.mUidFirewallStandbyRules.delete(uid);
        this.mUidFirewallDozableRules.delete(uid);
        this.mUidFirewallPowerSaveRules.delete(uid);
        this.mPowerSaveWhitelistExceptIdleAppIds.delete(uid);
        this.mPowerSaveWhitelistAppIds.delete(uid);
        this.mPowerSaveTempWhitelistAppIds.delete(uid);
        this.mAppIdleTempWhitelistAppIds.delete(uid);
        this.mUidFirewallRestrictedModeRules.delete(uid);
        this.mUidFirewallLowPowerStandbyModeRules.delete(uid);
        synchronized (this.mUidStateCallbackInfos) {
            this.mUidStateCallbackInfos.remove(uid);
        }
        this.mHandler.obtainMessage(15, uid, 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRestrictionRulesForUidUL(int uid) {
        updateRuleForDeviceIdleUL(uid);
        updateRuleForAppIdleUL(uid, -1);
        updateRuleForRestrictPowerUL(uid);
        updateRestrictedModeForUidUL(uid);
        m4970x670de143(uid);
        m4969x3105fe71(uid);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: updateRulesForDataUsageRestrictionsUL */
    public void m4969x3105fe71(int uid) {
        if (Trace.isTagEnabled(2097152L)) {
            Trace.traceBegin(2097152L, "updateRulesForDataUsageRestrictionsUL: " + uid);
        }
        try {
            updateRulesForDataUsageRestrictionsULInner(uid);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void updateRulesForDataUsageRestrictionsULInner(int uid) {
        int i;
        int i2;
        int oldAllowedReasons;
        if (!isUidValidForAllowlistRulesUL(uid)) {
            if (LOGD) {
                Slog.d(TAG, "no need to update restrict data rules for uid " + uid);
                return;
            }
            return;
        }
        int uidPolicy = ITranNetworkPolicyManagerService.Instance().updateRulesForDataUsageRestrictionsULInnerUidPolicy(uid, this.mUidPolicy.get(uid, 0));
        boolean isForeground = isUidForegroundOnRestrictBackgroundUL(uid);
        boolean isRestrictedByAdmin = isRestrictedByAdminUL(uid);
        boolean isDenied = (uidPolicy & 1) != 0;
        boolean isAllowed = (uidPolicy & 4) != 0;
        int i3 = 262144;
        if (!isRestrictedByAdmin) {
            i = 0;
        } else {
            i = 262144;
        }
        int newBlockedReasons = 0 | i;
        int newBlockedReasons2 = newBlockedReasons | (this.mRestrictBackground ? 65536 : 0);
        int i4 = 131072;
        if (!isDenied) {
            i2 = 0;
        } else {
            i2 = 131072;
        }
        int newBlockedReasons3 = i2 | newBlockedReasons2;
        if (!isSystem(uid)) {
            i4 = 0;
        }
        int newAllowedReasons = 0 | i4;
        if (!isForeground) {
            i3 = 0;
        }
        int newAllowedReasons2 = newAllowedReasons | i3 | (isAllowed ? 65536 : 0);
        synchronized (this.mUidBlockedState) {
            try {
                try {
                    UidBlockedState uidBlockedState = getOrCreateUidBlockedStateForUid(this.mUidBlockedState, uid);
                    UidBlockedState previousUidBlockedState = getOrCreateUidBlockedStateForUid(this.mTmpUidBlockedState, uid);
                    previousUidBlockedState.copyFrom(uidBlockedState);
                    uidBlockedState.blockedReasons = (uidBlockedState.blockedReasons & GnssNative.GNSS_AIDING_TYPE_ALL) | newBlockedReasons3;
                    uidBlockedState.allowedReasons = (uidBlockedState.allowedReasons & GnssNative.GNSS_AIDING_TYPE_ALL) | newAllowedReasons2;
                    uidBlockedState.updateEffectiveBlockedReasons();
                    int oldEffectiveBlockedReasons = previousUidBlockedState.effectiveBlockedReasons;
                    int newEffectiveBlockedReasons = uidBlockedState.effectiveBlockedReasons;
                    int oldAllowedReasons2 = previousUidBlockedState.allowedReasons;
                    int uidRules = oldEffectiveBlockedReasons == newEffectiveBlockedReasons ? 0 : uidBlockedState.deriveUidRules();
                    if (LOGV) {
                        oldAllowedReasons = oldAllowedReasons2;
                        Log.v(TAG, "updateRuleForRestrictBackgroundUL(" + uid + "): isForeground=" + isForeground + ", isDenied=" + isDenied + ", isAllowed=" + isAllowed + ", isRestrictedByAdmin=" + isRestrictedByAdmin + ", oldBlockedState=" + previousUidBlockedState + ", newBlockedState=" + uidBlockedState + ", newBlockedMeteredReasons=" + NetworkPolicyManager.blockedReasonsToString(newBlockedReasons3) + ", newAllowedMeteredReasons=" + NetworkPolicyManager.allowedReasonsToString(newAllowedReasons2));
                    } else {
                        oldAllowedReasons = oldAllowedReasons2;
                    }
                    if (oldEffectiveBlockedReasons != newEffectiveBlockedReasons) {
                        handleBlockedReasonsChanged(uid, newEffectiveBlockedReasons, oldEffectiveBlockedReasons);
                        postUidRulesChangedMsg(uid, uidRules);
                    }
                    if ((oldEffectiveBlockedReasons & 393216) != 0 || (newEffectiveBlockedReasons & 393216) != 0) {
                        setMeteredNetworkDenylist(uid, (393216 & newEffectiveBlockedReasons) != 0);
                    }
                    if ((oldAllowedReasons & 327680) != 0 || (newAllowedReasons2 & 327680) != 0) {
                        setMeteredNetworkAllowlist(uid, (327680 & newAllowedReasons2) != 0);
                    }
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

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: updateRulesForPowerRestrictionsUL */
    public void m4970x670de143(int uid) {
        updateRulesForPowerRestrictionsUL(uid, -1);
    }

    private void updateRulesForPowerRestrictionsUL(int uid, int uidProcState) {
        updateRulesForPowerRestrictionsUL(uid, isUidIdle(uid, uidProcState));
    }

    private void updateRulesForPowerRestrictionsUL(int uid, boolean isUidIdle) {
        if (Trace.isTagEnabled(2097152L)) {
            Trace.traceBegin(2097152L, "updateRulesForPowerRestrictionsUL: " + uid + SliceClientPermissions.SliceAuthority.DELIMITER + (isUidIdle ? "I" : "-"));
        }
        try {
            updateRulesForPowerRestrictionsULInner(uid, isUidIdle);
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    private void updateRulesForPowerRestrictionsULInner(int uid, boolean isUidIdle) {
        int oldEffectiveBlockedReasons;
        int newEffectiveBlockedReasons;
        int uidRules;
        if (!isUidValidForDenylistRulesUL(uid)) {
            if (LOGD) {
                Slog.d(TAG, "no need to update restrict power rules for uid " + uid);
                return;
            }
            return;
        }
        boolean isForeground = isUidForegroundOnRestrictPowerUL(uid);
        boolean isTop = isUidTop(uid);
        boolean isWhitelisted = isWhitelistedFromPowerSaveUL(uid, this.mDeviceIdleMode);
        synchronized (this.mUidBlockedState) {
            UidBlockedState uidBlockedState = getOrCreateUidBlockedStateForUid(this.mUidBlockedState, uid);
            UidBlockedState previousUidBlockedState = getOrCreateUidBlockedStateForUid(this.mTmpUidBlockedState, uid);
            previousUidBlockedState.copyFrom(uidBlockedState);
            int i = 0;
            int newBlockedReasons = 0 | (this.mRestrictPower ? 1 : 0);
            int i2 = 2;
            int i3 = 32;
            int i4 = 8;
            int newBlockedReasons2 = newBlockedReasons | (this.mDeviceIdleMode ? 2 : 0) | (this.mLowPowerStandbyActive ? 32 : 0) | (isUidIdle ? 4 : 0) | (uidBlockedState.blockedReasons & 8);
            int newAllowedReasons = 0 | (isSystem(uid) ? 1 : 0);
            if (!isForeground) {
                i2 = 0;
            }
            int newAllowedReasons2 = newAllowedReasons | i2;
            if (!isTop) {
                i3 = 0;
            }
            int newAllowedReasons3 = newAllowedReasons2 | i3 | (isWhitelistedFromPowerSaveUL(uid, true) ? 4 : 0);
            if (!isWhitelistedFromPowerSaveExceptIdleUL(uid)) {
                i4 = 0;
            }
            int newAllowedReasons4 = newAllowedReasons3 | i4 | (uidBlockedState.allowedReasons & 16) | (isAllowlistedFromLowPowerStandbyUL(uid) ? 64 : 0);
            uidBlockedState.blockedReasons = (uidBlockedState.blockedReasons & (-65536)) | newBlockedReasons2;
            uidBlockedState.allowedReasons = (uidBlockedState.allowedReasons & (-65536)) | newAllowedReasons4;
            uidBlockedState.updateEffectiveBlockedReasons();
            if (LOGV) {
                Log.v(TAG, "updateRulesForPowerRestrictionsUL(" + uid + "), isIdle: " + isUidIdle + ", mRestrictPower: " + this.mRestrictPower + ", mDeviceIdleMode: " + this.mDeviceIdleMode + ", isForeground=" + isForeground + ", isTop=" + isTop + ", isWhitelisted=" + isWhitelisted + ", oldUidBlockedState=" + previousUidBlockedState + ", newUidBlockedState=" + uidBlockedState);
            }
            oldEffectiveBlockedReasons = previousUidBlockedState.effectiveBlockedReasons;
            newEffectiveBlockedReasons = uidBlockedState.effectiveBlockedReasons;
            if (oldEffectiveBlockedReasons != newEffectiveBlockedReasons) {
                i = uidBlockedState.deriveUidRules();
            }
            uidRules = i;
        }
        if (oldEffectiveBlockedReasons != newEffectiveBlockedReasons) {
            handleBlockedReasonsChanged(uid, newEffectiveBlockedReasons, oldEffectiveBlockedReasons);
            postUidRulesChangedMsg(uid, uidRules);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class NetPolicyAppIdleStateChangeListener extends AppStandbyInternal.AppIdleStateChangeListener {
        private NetPolicyAppIdleStateChangeListener() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
            try {
                int uid = NetworkPolicyManagerService.this.mContext.getPackageManager().getPackageUidAsUser(packageName, 8192, userId);
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    NetworkPolicyManagerService.this.mLogger.appIdleStateChanged(uid, idle);
                    NetworkPolicyManagerService.this.updateRuleForAppIdleUL(uid, -1);
                    NetworkPolicyManagerService.this.m4970x670de143(uid);
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        public void onParoleStateChanged(boolean isParoleOn) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                NetworkPolicyManagerService.this.mLogger.paroleStateChanged(isParoleOn);
                NetworkPolicyManagerService.this.updateRulesForAppIdleParoleUL();
            }
        }
    }

    private void handleBlockedReasonsChanged(int uid, int newEffectiveBlockedReasons, int oldEffectiveBlockedReasons) {
        this.mActivityManagerInternal.onUidBlockedReasonsChanged(uid, newEffectiveBlockedReasons);
        postBlockedReasonsChangedMsg(uid, newEffectiveBlockedReasons, oldEffectiveBlockedReasons);
    }

    private void postBlockedReasonsChangedMsg(int uid, int newEffectiveBlockedReasons, int oldEffectiveBlockedReasons) {
        this.mHandler.obtainMessage(21, uid, newEffectiveBlockedReasons, Integer.valueOf(oldEffectiveBlockedReasons)).sendToTarget();
    }

    private void postUidRulesChangedMsg(int uid, int uidRules) {
        this.mHandler.obtainMessage(1, uid, uidRules).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchUidRulesChanged(INetworkPolicyListener listener, int uid, int uidRules) {
        try {
            listener.onUidRulesChanged(uid, uidRules);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchMeteredIfacesChanged(INetworkPolicyListener listener, String[] meteredIfaces) {
        try {
            listener.onMeteredIfacesChanged(meteredIfaces);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchRestrictBackgroundChanged(INetworkPolicyListener listener, boolean restrictBackground) {
        try {
            listener.onRestrictBackgroundChanged(restrictBackground);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchUidPoliciesChanged(INetworkPolicyListener listener, int uid, int uidPolicies) {
        try {
            listener.onUidPoliciesChanged(uid, uidPolicies);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchSubscriptionOverride(INetworkPolicyListener listener, int subId, int overrideMask, int overrideValue, int[] networkTypes) {
        try {
            listener.onSubscriptionOverride(subId, overrideMask, overrideValue, networkTypes);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchSubscriptionPlansChanged(INetworkPolicyListener listener, int subId, SubscriptionPlan[] plans) {
        try {
            listener.onSubscriptionPlansChanged(subId, plans);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchBlockedReasonChanged(INetworkPolicyListener listener, int uid, int oldBlockedReasons, int newBlockedReasons) {
        try {
            listener.onBlockedReasonChanged(uid, oldBlockedReasons, newBlockedReasons);
        } catch (RemoteException e) {
        }
    }

    void handleUidChanged(UidStateCallbackInfo uidStateCallbackInfo) {
        int uid;
        int procState;
        long procStateSeq;
        int capability;
        boolean updated;
        Trace.traceBegin(2097152L, "onUidStateChanged");
        try {
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mUidStateCallbackInfos) {
                    uid = uidStateCallbackInfo.uid;
                    procState = uidStateCallbackInfo.procState;
                    procStateSeq = uidStateCallbackInfo.procStateSeq;
                    capability = uidStateCallbackInfo.capability;
                    uidStateCallbackInfo.isPending = false;
                }
                this.mLogger.uidStateChanged(uid, procState, procStateSeq, capability);
                updated = updateUidStateUL(uid, procState, procStateSeq, capability);
                this.mActivityManagerInternal.notifyNetworkPolicyRulesUpdated(uid, procStateSeq);
            }
            if (updated) {
                updateNetworkStats(uid, NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(procState));
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    void handleUidGone(int uid) {
        boolean updated;
        Trace.traceBegin(2097152L, "onUidGone");
        try {
            synchronized (this.mUidRulesFirstLock) {
                updated = removeUidStateUL(uid);
            }
            if (updated) {
                updateNetworkStats(uid, false);
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void broadcastRestrictBackgroundChanged(int uid, Boolean changed) {
        PackageManager pm = this.mContext.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        if (packages != null) {
            int userId = UserHandle.getUserId(uid);
            for (String packageName : packages) {
                Intent intent = new Intent("android.net.conn.RESTRICT_BACKGROUND_CHANGED");
                intent.setPackage(packageName);
                intent.setFlags(1073741824);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class IfaceQuotas {
        public final String iface;
        public final long limit;
        public final long warning;

        private IfaceQuotas(String iface, long warning, long limit) {
            this.iface = iface;
            this.warning = warning;
            this.limit = limit;
        }
    }

    private void setInterfaceQuotasAsync(String iface, long warningBytes, long limitBytes) {
        this.mHandler.obtainMessage(10, new IfaceQuotas(iface, warningBytes, limitBytes)).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setInterfaceLimit(String iface, long limitBytes) {
        try {
            this.mNetworkManager.setInterfaceQuota(iface, limitBytes);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting interface quota", e2);
        }
    }

    private void removeInterfaceQuotasAsync(String iface) {
        this.mHandler.obtainMessage(11, iface).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeInterfaceLimit(String iface) {
        try {
            this.mNetworkManager.removeInterfaceQuota(iface);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem removing interface quota", e2);
        }
    }

    private void setMeteredNetworkDenylist(int uid, boolean enable) {
        if (LOGV) {
            Slog.v(TAG, "setMeteredNetworkDenylist " + uid + ": " + enable);
        }
        try {
            this.mNetworkManager.setUidOnMeteredNetworkDenylist(uid, enable);
            this.mLogger.meteredDenylistChanged(uid, enable);
            if (Process.isApplicationUid(uid)) {
                int sdkSandboxUid = Process.toSdkSandboxUid(uid);
                this.mNetworkManager.setUidOnMeteredNetworkDenylist(sdkSandboxUid, enable);
                this.mLogger.meteredDenylistChanged(sdkSandboxUid, enable);
            }
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting denylist (" + enable + ") rules for " + uid, e2);
        }
    }

    private void setMeteredNetworkAllowlist(int uid, boolean enable) {
        if (LOGV) {
            Slog.v(TAG, "setMeteredNetworkAllowlist " + uid + ": " + enable);
        }
        try {
            this.mNetworkManager.setUidOnMeteredNetworkAllowlist(uid, enable);
            this.mLogger.meteredAllowlistChanged(uid, enable);
            if (Process.isApplicationUid(uid)) {
                int sdkSandboxUid = Process.toSdkSandboxUid(uid);
                this.mNetworkManager.setUidOnMeteredNetworkAllowlist(sdkSandboxUid, enable);
                this.mLogger.meteredAllowlistChanged(sdkSandboxUid, enable);
            }
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting allowlist (" + enable + ") rules for " + uid, e2);
        }
    }

    private void setUidFirewallRulesUL(int chain, SparseIntArray uidRules, int toggle) {
        if (uidRules != null) {
            setUidFirewallRulesUL(chain, uidRules);
        }
        if (toggle != 0) {
            enableFirewallChainUL(chain, toggle == 1);
        }
    }

    private void addSdkSandboxUidsIfNeeded(SparseIntArray uidRules) {
        int size = uidRules.size();
        SparseIntArray sdkSandboxUids = new SparseIntArray();
        for (int index = 0; index < size; index++) {
            int uid = uidRules.keyAt(index);
            int rule = uidRules.valueAt(index);
            if (Process.isApplicationUid(uid)) {
                sdkSandboxUids.put(Process.toSdkSandboxUid(uid), rule);
            }
        }
        for (int index2 = 0; index2 < sdkSandboxUids.size(); index2++) {
            int uid2 = sdkSandboxUids.keyAt(index2);
            int rule2 = sdkSandboxUids.valueAt(index2);
            uidRules.put(uid2, rule2);
        }
    }

    private void setUidFirewallRulesUL(int chain, SparseIntArray uidRules) {
        addSdkSandboxUidsIfNeeded(uidRules);
        try {
            int size = uidRules.size();
            int[] uids = new int[size];
            int[] rules = new int[size];
            for (int index = size - 1; index >= 0; index--) {
                uids[index] = uidRules.keyAt(index);
                rules[index] = uidRules.valueAt(index);
            }
            this.mNetworkManager.setFirewallUidRules(chain, uids, rules);
            this.mLogger.firewallRulesChanged(chain, uids, rules);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem setting firewall uid rules", e2);
        }
    }

    private void setUidFirewallRuleUL(int chain, int uid, int rule) {
        if (Trace.isTagEnabled(2097152L)) {
            Trace.traceBegin(2097152L, "setUidFirewallRuleUL: " + chain + SliceClientPermissions.SliceAuthority.DELIMITER + uid + SliceClientPermissions.SliceAuthority.DELIMITER + rule);
        }
        try {
            if (chain == 1) {
                this.mUidFirewallDozableRules.put(uid, rule);
            } else if (chain == 2) {
                this.mUidFirewallStandbyRules.put(uid, rule);
            } else if (chain == 3) {
                this.mUidFirewallPowerSaveRules.put(uid, rule);
            } else if (chain == 4) {
                this.mUidFirewallRestrictedModeRules.put(uid, rule);
            } else if (chain == 5) {
                this.mUidFirewallLowPowerStandbyModeRules.put(uid, rule);
            }
            try {
                this.mNetworkManager.setFirewallUidRule(chain, uid, rule);
                this.mLogger.uidFirewallRuleChanged(chain, uid, rule);
                if (Process.isApplicationUid(uid)) {
                    int sdkSandboxUid = Process.toSdkSandboxUid(uid);
                    this.mNetworkManager.setFirewallUidRule(chain, sdkSandboxUid, rule);
                    this.mLogger.uidFirewallRuleChanged(chain, sdkSandboxUid, rule);
                }
            } catch (RemoteException e) {
            } catch (IllegalStateException e2) {
                Log.wtf(TAG, "problem setting firewall uid rules", e2);
            }
            Trace.traceEnd(2097152L);
        } catch (Throwable th) {
            Trace.traceEnd(2097152L);
            throw th;
        }
    }

    private void enableFirewallChainUL(int chain, boolean enable) {
        if (this.mFirewallChainStates.indexOfKey(chain) >= 0 && this.mFirewallChainStates.get(chain) == enable) {
            return;
        }
        this.mFirewallChainStates.put(chain, enable);
        try {
            this.mNetworkManager.setFirewallChainEnabled(chain, enable);
            this.mLogger.firewallChainEnabled(chain, enable);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem enable firewall chain", e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetUidFirewallRules(int uid) {
        try {
            this.mNetworkManager.setFirewallUidRule(1, uid, 0);
            this.mNetworkManager.setFirewallUidRule(2, uid, 0);
            this.mNetworkManager.setFirewallUidRule(3, uid, 0);
            this.mNetworkManager.setFirewallUidRule(4, uid, 0);
            this.mNetworkManager.setFirewallUidRule(5, uid, 0);
            this.mNetworkManager.setUidOnMeteredNetworkAllowlist(uid, false);
            this.mLogger.meteredAllowlistChanged(uid, false);
            this.mNetworkManager.setUidOnMeteredNetworkDenylist(uid, false);
            this.mLogger.meteredDenylistChanged(uid, false);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Log.wtf(TAG, "problem resetting firewall uid rules for " + uid, e2);
        }
        if (Process.isApplicationUid(uid)) {
            resetUidFirewallRules(Process.toSdkSandboxUid(uid));
        }
    }

    @Deprecated
    private long getTotalBytes(NetworkTemplate template, long start, long end) {
        if (this.mStatsCallback.isAnyCallbackReceived()) {
            return this.mDeps.getNetworkTotalBytes(template, start, end);
        }
        return 0L;
    }

    private boolean isBandwidthControlEnabled() {
        long token = Binder.clearCallingIdentity();
        try {
            return this.mNetworkManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static Intent buildSnoozeWarningIntent(NetworkTemplate template, String targetPackage) {
        Intent intent = new Intent(ACTION_SNOOZE_WARNING);
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) template);
        intent.setPackage(targetPackage);
        return intent;
    }

    private static Intent buildSnoozeRapidIntent(NetworkTemplate template, String targetPackage) {
        Intent intent = new Intent(ACTION_SNOOZE_RAPID);
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) template);
        intent.setPackage(targetPackage);
        return intent;
    }

    private static Intent buildNetworkOverLimitIntent(Resources res, NetworkTemplate template) {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(res.getString(17040004)));
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", (Parcelable) template);
        return intent;
    }

    private static Intent buildViewDataUsageIntent(Resources res, NetworkTemplate template) {
        Intent intent = new Intent();
        intent.setAction("android.settings.MOBILE_DATA_USAGE");
        intent.addFlags(268435456);
        intent.putExtra("network_template", (Parcelable) template);
        intent.putExtra("android.provider.extra.SUB_ID", SubscriptionManager.getDefaultDataSubscriptionId());
        return intent;
    }

    void addIdleHandler(MessageQueue.IdleHandler handler) {
        this.mHandler.getLooper().getQueue().addIdleHandler(handler);
    }

    void updateRestrictBackgroundByLowPowerModeUL(PowerSaveState result) {
        boolean shouldInvokeRestrictBackground;
        if (this.mRestrictBackgroundLowPowerMode == result.batterySaverEnabled) {
            return;
        }
        this.mRestrictBackgroundLowPowerMode = result.batterySaverEnabled;
        boolean restrictBackground = this.mRestrictBackgroundLowPowerMode;
        boolean localRestrictBgChangedInBsm = this.mRestrictBackgroundChangedInBsm;
        if (this.mRestrictBackgroundLowPowerMode) {
            shouldInvokeRestrictBackground = !this.mRestrictBackground;
            this.mRestrictBackgroundBeforeBsm = this.mRestrictBackground;
            localRestrictBgChangedInBsm = false;
        } else {
            boolean shouldInvokeRestrictBackground2 = this.mRestrictBackgroundChangedInBsm;
            shouldInvokeRestrictBackground = !shouldInvokeRestrictBackground2;
            restrictBackground = this.mRestrictBackgroundBeforeBsm;
        }
        if (shouldInvokeRestrictBackground) {
            setRestrictBackgroundUL(restrictBackground, "low_power");
        }
        this.mRestrictBackgroundChangedInBsm = localRestrictBgChangedInBsm;
    }

    private static void collectKeys(SparseIntArray source, SparseBooleanArray target) {
        int size = source.size();
        for (int i = 0; i < size; i++) {
            target.put(source.keyAt(i), true);
        }
    }

    private static <T> void collectKeys(SparseArray<T> source, SparseBooleanArray target) {
        int size = source.size();
        for (int i = 0; i < size; i++) {
            target.put(source.keyAt(i), true);
        }
    }

    public void factoryReset(String subscriber) {
        NetworkTemplate templateCarrier;
        int[] uidsWithPolicy;
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", TAG);
        if (this.mUserManager.hasUserRestriction("no_network_reset")) {
            return;
        }
        NetworkPolicy[] policies = getNetworkPolicies(this.mContext.getOpPackageName());
        NetworkTemplate templateMobile = null;
        if (subscriber == null) {
            templateCarrier = null;
        } else {
            templateCarrier = buildTemplateCarrierMetered(subscriber);
        }
        if (subscriber != null) {
            templateMobile = new NetworkTemplate.Builder(1).setSubscriberIds(Set.of(subscriber)).setMeteredness(1).build();
        }
        for (NetworkPolicy policy : policies) {
            if (policy.template.equals(templateCarrier) || policy.template.equals(templateMobile)) {
                policy.limitBytes = -1L;
                policy.inferred = false;
                policy.clearSnooze();
            }
        }
        setNetworkPolicies(policies);
        setRestrictBackground(false);
        if (!this.mUserManager.hasUserRestriction("no_control_apps")) {
            for (int uid : getUidsWithPolicy(1)) {
                setUidPolicy(uid, 0);
            }
        }
    }

    public boolean isUidNetworkingBlocked(int uid, boolean isNetworkMetered) {
        int blockedReasons;
        long startTime = this.mStatLogger.getTime();
        this.mContext.enforceCallingOrSelfPermission("android.permission.OBSERVE_NETWORK_POLICY", TAG);
        synchronized (this.mUidBlockedState) {
            UidBlockedState uidBlockedState = this.mUidBlockedState.get(uid);
            blockedReasons = uidBlockedState == null ? 0 : uidBlockedState.effectiveBlockedReasons;
            if (!isNetworkMetered) {
                blockedReasons &= GnssNative.GNSS_AIDING_TYPE_ALL;
            }
            this.mLogger.networkBlocked(uid, uidBlockedState);
        }
        this.mStatLogger.logDurationStat(1, startTime);
        return blockedReasons != 0;
    }

    public boolean isUidRestrictedOnMeteredNetworks(int uid) {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.OBSERVE_NETWORK_POLICY", TAG);
        synchronized (this.mUidBlockedState) {
            UidBlockedState uidBlockedState = this.mUidBlockedState.get(uid);
            int blockedReasons = uidBlockedState == null ? 0 : uidBlockedState.effectiveBlockedReasons;
            z = (blockedReasons & (-65536)) != 0;
        }
        return z;
    }

    private static boolean isSystem(int uid) {
        return uid < 10000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class NetworkPolicyManagerInternalImpl extends NetworkPolicyManagerInternal {
        private NetworkPolicyManagerInternalImpl() {
        }

        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public void resetUserState(int userId) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                boolean z = true;
                boolean changed = NetworkPolicyManagerService.this.removeUserStateUL(userId, false, true);
                if (!NetworkPolicyManagerService.this.addDefaultRestrictBackgroundAllowlistUidsUL(userId) && !changed) {
                    z = false;
                }
                boolean changed2 = z;
                if (changed2) {
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        NetworkPolicyManagerService.this.writePolicyAL();
                    }
                }
            }
        }

        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public void onTempPowerSaveWhitelistChange(int appId, boolean added, int reasonCode, String reason) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                if (NetworkPolicyManagerService.this.mSystemReady) {
                    NetworkPolicyManagerService.this.mLogger.tempPowerSaveWlChanged(appId, added, reasonCode, reason);
                    if (added) {
                        NetworkPolicyManagerService.this.mPowerSaveTempWhitelistAppIds.put(appId, true);
                    } else {
                        NetworkPolicyManagerService.this.mPowerSaveTempWhitelistAppIds.delete(appId);
                    }
                    NetworkPolicyManagerService.this.updateRulesForTempWhitelistChangeUL(appId);
                }
            }
        }

        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public SubscriptionPlan getSubscriptionPlan(Network network) {
            SubscriptionPlan primarySubscriptionPlanLocked;
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                int subId = NetworkPolicyManagerService.this.getSubIdLocked(network);
                primarySubscriptionPlanLocked = NetworkPolicyManagerService.this.getPrimarySubscriptionPlanLocked(subId);
            }
            return primarySubscriptionPlanLocked;
        }

        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public long getSubscriptionOpportunisticQuota(Network network, int quotaType) {
            long quotaBytes;
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                quotaBytes = NetworkPolicyManagerService.this.mSubscriptionOpportunisticQuota.get(NetworkPolicyManagerService.this.getSubIdLocked(network), -1L);
            }
            if (quotaBytes == -1) {
                return -1L;
            }
            if (quotaType == 1) {
                return ((float) quotaBytes) * Settings.Global.getFloat(NetworkPolicyManagerService.this.mContext.getContentResolver(), "netpolicy_quota_frac_jobs", 0.5f);
            }
            if (quotaType == 2) {
                return ((float) quotaBytes) * Settings.Global.getFloat(NetworkPolicyManagerService.this.mContext.getContentResolver(), "netpolicy_quota_frac_multipath", 0.5f);
            }
            return -1L;
        }

        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public void onAdminDataAvailable() {
            NetworkPolicyManagerService.this.mAdminDataAvailableLatch.countDown();
        }

        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public void setAppIdleWhitelist(int uid, boolean shouldWhitelist) {
            NetworkPolicyManagerService.this.setAppIdleWhitelist(uid, shouldWhitelist);
        }

        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public void setMeteredRestrictedPackages(Set<String> packageNames, int userId) {
            NetworkPolicyManagerService.this.setMeteredRestrictedPackagesInternal(packageNames, userId);
        }

        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public void setMeteredRestrictedPackagesAsync(Set<String> packageNames, int userId) {
            NetworkPolicyManagerService.this.mHandler.obtainMessage(17, userId, 0, packageNames).sendToTarget();
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [6187=4] */
        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public void setLowPowerStandbyActive(boolean active) {
            Trace.traceBegin(2097152L, "setLowPowerStandbyActive");
            try {
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    if (NetworkPolicyManagerService.this.mLowPowerStandbyActive == active) {
                        return;
                    }
                    NetworkPolicyManagerService.this.mLowPowerStandbyActive = active;
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        if (NetworkPolicyManagerService.this.mSystemReady) {
                            NetworkPolicyManagerService.this.forEachUid("updateRulesForRestrictPower", new IntConsumer() { // from class: com.android.server.net.NetworkPolicyManagerService$NetworkPolicyManagerInternalImpl$$ExternalSyntheticLambda0
                                @Override // java.util.function.IntConsumer
                                public final void accept(int i) {
                                    NetworkPolicyManagerService.NetworkPolicyManagerInternalImpl.this.m4971xefc2ae1b(i);
                                }
                            });
                            NetworkPolicyManagerService.this.updateRulesForLowPowerStandbyUL();
                        }
                    }
                }
            } finally {
                Trace.traceEnd(2097152L);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setLowPowerStandbyActive$0$com-android-server-net-NetworkPolicyManagerService$NetworkPolicyManagerInternalImpl  reason: not valid java name */
        public /* synthetic */ void m4971xefc2ae1b(int uid) {
            NetworkPolicyManagerService.this.m4970x670de143(uid);
        }

        @Override // com.android.server.net.NetworkPolicyManagerInternal
        public void setLowPowerStandbyAllowlist(int[] uids) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                SparseBooleanArray changedUids = new SparseBooleanArray();
                for (int i = 0; i < NetworkPolicyManagerService.this.mLowPowerStandbyAllowlistUids.size(); i++) {
                    int oldUid = NetworkPolicyManagerService.this.mLowPowerStandbyAllowlistUids.keyAt(i);
                    if (!ArrayUtils.contains(uids, oldUid)) {
                        changedUids.put(oldUid, true);
                    }
                }
                for (int i2 = 0; i2 < changedUids.size(); i2++) {
                    int deletedUid = changedUids.keyAt(i2);
                    NetworkPolicyManagerService.this.mLowPowerStandbyAllowlistUids.delete(deletedUid);
                }
                for (int newUid : uids) {
                    if (NetworkPolicyManagerService.this.mLowPowerStandbyAllowlistUids.indexOfKey(newUid) < 0) {
                        changedUids.append(newUid, true);
                        NetworkPolicyManagerService.this.mLowPowerStandbyAllowlistUids.append(newUid, true);
                    }
                }
                if (NetworkPolicyManagerService.this.mLowPowerStandbyActive) {
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        if (NetworkPolicyManagerService.this.mSystemReady) {
                            for (int i3 = 0; i3 < changedUids.size(); i3++) {
                                int changedUid = changedUids.keyAt(i3);
                                NetworkPolicyManagerService.this.m4970x670de143(changedUid);
                                NetworkPolicyManagerService.this.updateRuleForLowPowerStandbyUL(changedUid);
                            }
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMeteredRestrictedPackagesInternal(Set<String> packageNames, int userId) {
        synchronized (this.mUidRulesFirstLock) {
            Set<Integer> newRestrictedUids = new ArraySet<>();
            for (String packageName : packageNames) {
                int uid = getUidForPackage(packageName, userId);
                if (uid >= 0) {
                    newRestrictedUids.add(Integer.valueOf(uid));
                }
            }
            Set<Integer> oldRestrictedUids = this.mMeteredRestrictedUids.get(userId);
            this.mMeteredRestrictedUids.put(userId, newRestrictedUids);
            handleRestrictedPackagesChangeUL(oldRestrictedUids, newRestrictedUids);
            this.mLogger.meteredRestrictedPkgsChanged(newRestrictedUids);
        }
    }

    private int getUidForPackage(String packageName, int userId) {
        try {
            return this.mContext.getPackageManager().getPackageUidAsUser(packageName, 4202496, userId);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getSubIdLocked(Network network) {
        return this.mNetIdToSubId.get(network.getNetId(), -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SubscriptionPlan getPrimarySubscriptionPlanLocked(int subId) {
        SubscriptionPlan[] plans = this.mSubscriptionPlans.get(subId);
        if (!ArrayUtils.isEmpty(plans)) {
            for (SubscriptionPlan plan : plans) {
                if (plan.getCycleRule().isRecurring()) {
                    return plan;
                }
                Range<ZonedDateTime> cycle = plan.cycleIterator().next();
                if (cycle.contains((Range<ZonedDateTime>) ZonedDateTime.now(this.mClock))) {
                    return plan;
                }
            }
            return null;
        }
        return null;
    }

    private void waitForAdminData() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.device_admin")) {
            ConcurrentUtils.waitForCountDownNoInterrupt(this.mAdminDataAvailableLatch, 10000L, "Wait for admin data");
        }
    }

    private void handleRestrictedPackagesChangeUL(Set<Integer> oldRestrictedUids, Set<Integer> newRestrictedUids) {
        if (!this.mNetworkManagerReady) {
            return;
        }
        if (oldRestrictedUids == null) {
            for (Integer num : newRestrictedUids) {
                m4969x3105fe71(num.intValue());
            }
            return;
        }
        for (Integer num2 : oldRestrictedUids) {
            int uid = num2.intValue();
            if (!newRestrictedUids.contains(Integer.valueOf(uid))) {
                m4969x3105fe71(uid);
            }
        }
        for (Integer num3 : newRestrictedUids) {
            int uid2 = num3.intValue();
            if (!oldRestrictedUids.contains(Integer.valueOf(uid2))) {
                m4969x3105fe71(uid2);
            }
        }
    }

    private boolean isRestrictedByAdminUL(int uid) {
        Set<Integer> restrictedUids = this.mMeteredRestrictedUids.get(UserHandle.getUserId(uid));
        boolean isRestricted = restrictedUids != null && restrictedUids.contains(Integer.valueOf(uid));
        return ITranNetworkPolicyManagerService.Instance().isRestrictedByAdminUL(uid, isRestricted);
    }

    private static boolean getBooleanDefeatingNullable(PersistableBundle bundle, String key, boolean defaultValue) {
        return bundle != null ? bundle.getBoolean(key, defaultValue) : defaultValue;
    }

    private static UidBlockedState getOrCreateUidBlockedStateForUid(SparseArray<UidBlockedState> uidBlockedStates, int uid) {
        UidBlockedState uidBlockedState = uidBlockedStates.get(uid);
        if (uidBlockedState == null) {
            UidBlockedState uidBlockedState2 = new UidBlockedState();
            uidBlockedStates.put(uid, uidBlockedState2);
            return uidBlockedState2;
        }
        return uidBlockedState;
    }

    private int getEffectiveBlockedReasons(int uid) {
        int i;
        synchronized (this.mUidBlockedState) {
            UidBlockedState uidBlockedState = this.mUidBlockedState.get(uid);
            if (uidBlockedState == null) {
                i = 0;
            } else {
                i = uidBlockedState.effectiveBlockedReasons;
            }
        }
        return i;
    }

    private int getBlockedReasons(int uid) {
        int i;
        synchronized (this.mUidBlockedState) {
            UidBlockedState uidBlockedState = this.mUidBlockedState.get(uid);
            if (uidBlockedState == null) {
                i = 0;
            } else {
                i = uidBlockedState.blockedReasons;
            }
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class UidBlockedState {
        public int allowedReasons;
        public int blockedReasons;
        public int effectiveBlockedReasons;
        private static final int[] BLOCKED_REASONS = {1, 2, 4, 8, 32, 65536, 131072, 262144};
        private static final int[] ALLOWED_REASONS = {1, 2, 32, 4, 8, 16, 64, 65536, 131072, 262144};

        private UidBlockedState(int blockedReasons, int allowedReasons, int effectiveBlockedReasons) {
            this.blockedReasons = blockedReasons;
            this.allowedReasons = allowedReasons;
            this.effectiveBlockedReasons = effectiveBlockedReasons;
        }

        UidBlockedState() {
            this(0, 0, 0);
        }

        void updateEffectiveBlockedReasons() {
            if (NetworkPolicyManagerService.LOGV && this.blockedReasons == 0) {
                Log.v(NetworkPolicyManagerService.TAG, "updateEffectiveBlockedReasons(): no blocked reasons");
            }
            this.effectiveBlockedReasons = getEffectiveBlockedReasons(this.blockedReasons, this.allowedReasons);
            if (NetworkPolicyManagerService.LOGV) {
                Log.v(NetworkPolicyManagerService.TAG, "updateEffectiveBlockedReasons(): blockedReasons=" + Integer.toBinaryString(this.blockedReasons) + ", effectiveReasons=" + Integer.toBinaryString(this.effectiveBlockedReasons));
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static int getEffectiveBlockedReasons(int blockedReasons, int allowedReasons) {
            int effectiveBlockedReasons = blockedReasons;
            if (blockedReasons == 0) {
                return effectiveBlockedReasons;
            }
            if ((allowedReasons & 1) != 0) {
                effectiveBlockedReasons &= -65536;
            }
            if ((131072 & allowedReasons) != 0) {
                effectiveBlockedReasons &= GnssNative.GNSS_AIDING_TYPE_ALL;
            }
            if ((allowedReasons & 2) != 0) {
                effectiveBlockedReasons = effectiveBlockedReasons & (-2) & (-3) & (-5);
            }
            if ((262144 & allowedReasons) != 0) {
                effectiveBlockedReasons = effectiveBlockedReasons & (-65537) & (-131073);
            }
            if ((allowedReasons & 32) != 0) {
                effectiveBlockedReasons &= -33;
            }
            if ((allowedReasons & 4) != 0) {
                effectiveBlockedReasons = effectiveBlockedReasons & (-2) & (-3) & (-5);
            }
            if ((allowedReasons & 8) != 0) {
                effectiveBlockedReasons = effectiveBlockedReasons & (-2) & (-5);
            }
            if ((allowedReasons & 16) != 0) {
                effectiveBlockedReasons &= -9;
            }
            if ((65536 & allowedReasons) != 0) {
                effectiveBlockedReasons &= -65537;
            }
            if ((allowedReasons & 64) != 0) {
                return effectiveBlockedReasons & (-33);
            }
            return effectiveBlockedReasons;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static int getAllowedReasonsForProcState(int procState) {
            if (procState > 5) {
                return 0;
            }
            if (procState <= 3) {
                return 262178;
            }
            return 262146;
        }

        public String toString() {
            return toString(this.blockedReasons, this.allowedReasons, this.effectiveBlockedReasons);
        }

        public static String toString(int blockedReasons, int allowedReasons, int effectiveBlockedReasons) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("blocked=").append(blockedReasonsToString(blockedReasons)).append(",");
            sb.append("allowed=").append(allowedReasonsToString(allowedReasons)).append(",");
            sb.append("effective=").append(blockedReasonsToString(effectiveBlockedReasons));
            sb.append("}");
            return sb.toString();
        }

        private static String blockedReasonToString(int blockedReason) {
            switch (blockedReason) {
                case 0:
                    return "NONE";
                case 1:
                    return "BATTERY_SAVER";
                case 2:
                    return "DOZE";
                case 4:
                    return "APP_STANDBY";
                case 8:
                    return "RESTRICTED_MODE";
                case 32:
                    return "LOW_POWER_STANDBY";
                case 65536:
                    return "DATA_SAVER";
                case 131072:
                    return "METERED_USER_RESTRICTED";
                case 262144:
                    return "METERED_ADMIN_DISABLED";
                default:
                    Slog.wtfStack(NetworkPolicyManagerService.TAG, "Unknown blockedReason: " + blockedReason);
                    return String.valueOf(blockedReason);
            }
        }

        private static String allowedReasonToString(int allowedReason) {
            switch (allowedReason) {
                case 0:
                    return "NONE";
                case 1:
                    return "SYSTEM";
                case 2:
                    return "FOREGROUND";
                case 4:
                    return "POWER_SAVE_ALLOWLIST";
                case 8:
                    return "POWER_SAVE_EXCEPT_IDLE_ALLOWLIST";
                case 16:
                    return "RESTRICTED_MODE_PERMISSIONS";
                case 32:
                    return "TOP";
                case 64:
                    return "LOW_POWER_STANDBY_ALLOWLIST";
                case 65536:
                    return "METERED_USER_EXEMPTED";
                case 131072:
                    return "METERED_SYSTEM";
                case 262144:
                    return "METERED_FOREGROUND";
                default:
                    Slog.wtfStack(NetworkPolicyManagerService.TAG, "Unknown allowedReason: " + allowedReason);
                    return String.valueOf(allowedReason);
            }
        }

        public static String blockedReasonsToString(int blockedReasons) {
            int i = 0;
            if (blockedReasons == 0) {
                return blockedReasonToString(0);
            }
            StringBuilder sb = new StringBuilder();
            int[] iArr = BLOCKED_REASONS;
            int length = iArr.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                int reason = iArr[i];
                if ((blockedReasons & reason) != 0) {
                    sb.append(sb.length() != 0 ? "|" : "");
                    sb.append(blockedReasonToString(reason));
                    blockedReasons &= ~reason;
                }
                i++;
            }
            if (blockedReasons != 0) {
                sb.append(sb.length() != 0 ? "|" : "");
                sb.append(String.valueOf(blockedReasons));
                Slog.wtfStack(NetworkPolicyManagerService.TAG, "Unknown blockedReasons: " + blockedReasons);
            }
            return sb.toString();
        }

        public static String allowedReasonsToString(int allowedReasons) {
            int i = 0;
            if (allowedReasons == 0) {
                return allowedReasonToString(0);
            }
            StringBuilder sb = new StringBuilder();
            int[] iArr = ALLOWED_REASONS;
            int length = iArr.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                int reason = iArr[i];
                if ((allowedReasons & reason) != 0) {
                    sb.append(sb.length() != 0 ? "|" : "");
                    sb.append(allowedReasonToString(reason));
                    allowedReasons &= ~reason;
                }
                i++;
            }
            if (allowedReasons != 0) {
                sb.append(sb.length() != 0 ? "|" : "");
                sb.append(String.valueOf(allowedReasons));
                Slog.wtfStack(NetworkPolicyManagerService.TAG, "Unknown allowedReasons: " + allowedReasons);
            }
            return sb.toString();
        }

        public void copyFrom(UidBlockedState uidBlockedState) {
            this.blockedReasons = uidBlockedState.blockedReasons;
            this.allowedReasons = uidBlockedState.allowedReasons;
            this.effectiveBlockedReasons = uidBlockedState.effectiveBlockedReasons;
        }

        public int deriveUidRules() {
            int uidRule = 0;
            int i = this.effectiveBlockedReasons;
            if ((i & 8) != 0) {
                uidRule = 0 | 1024;
            }
            if ((i & 39) == 0) {
                if ((this.blockedReasons & 39) != 0) {
                    uidRule |= 32;
                }
            } else {
                uidRule |= 64;
            }
            if ((i & 393216) != 0) {
                uidRule |= 4;
            } else {
                int i2 = this.blockedReasons;
                if ((131072 & i2) != 0 && (this.allowedReasons & 262144) != 0) {
                    uidRule |= 2;
                } else if ((i2 & 65536) != 0) {
                    int i3 = this.allowedReasons;
                    if ((65536 & i3) != 0) {
                        uidRule |= 32;
                    } else if ((i3 & 262144) != 0) {
                        uidRule |= 2;
                    }
                }
            }
            if (NetworkPolicyManagerService.LOGV) {
                Slog.v(NetworkPolicyManagerService.TAG, "uidBlockedState=" + this + " -> uidRule=" + NetworkPolicyManager.uidRulesToString(uidRule));
            }
            return uidRule;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class NotificationId {
        private final int mId;
        private final String mTag;

        NotificationId(NetworkPolicy policy, int type) {
            this.mTag = buildNotificationTag(policy, type);
            this.mId = type;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof NotificationId) {
                NotificationId that = (NotificationId) o;
                return Objects.equals(this.mTag, that.mTag);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.mTag);
        }

        private String buildNotificationTag(NetworkPolicy policy, int type) {
            return "NetworkPolicy:" + policy.template.hashCode() + ":" + type;
        }

        public String getTag() {
            return this.mTag;
        }

        public int getId() {
            return this.mId;
        }
    }
}
