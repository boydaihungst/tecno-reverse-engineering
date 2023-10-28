package com.android.server.connectivity;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkIdentity;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.NetworkTemplate;
import android.net.TelephonyNetworkSpecifier;
import android.net.Uri;
import android.os.BestClock;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DebugUtils;
import android.util.Log;
import android.util.Range;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.connectivity.MultipathPolicyTracker;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.NetworkPolicyManagerInternal;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class MultipathPolicyTracker {
    private static final boolean DBG = false;
    private static final long MIN_THRESHOLD_BYTES = 2097152;
    private static final int OPQUOTA_USER_SETTING_DIVIDER = 20;
    private static String TAG = MultipathPolicyTracker.class.getSimpleName();
    private ConnectivityManager mCM;
    private final Clock mClock;
    private final ConfigChangeReceiver mConfigChangeReceiver;
    private final Context mContext;
    private final Dependencies mDeps;
    private final Handler mHandler;
    private ConnectivityManager.NetworkCallback mMobileNetworkCallback;
    private final ConcurrentHashMap<Network, MultipathTracker> mMultipathTrackers;
    private NetworkPolicyManager mNPM;
    private NetworkPolicyManager.Listener mPolicyListener;
    private final ContentResolver mResolver;
    final ContentObserver mSettingsObserver;
    private NetworkStatsManager mStatsManager;
    private final Context mUserAllContext;

    /* loaded from: classes.dex */
    public static class Dependencies {
        public Clock getClock() {
            return new BestClock(ZoneOffset.UTC, new Clock[]{SystemClock.currentNetworkTimeClock(), Clock.systemUTC()});
        }
    }

    public MultipathPolicyTracker(Context ctx, Handler handler) {
        this(ctx, handler, new Dependencies());
    }

    public MultipathPolicyTracker(Context ctx, Handler handler, Dependencies deps) {
        this.mMultipathTrackers = new ConcurrentHashMap<>();
        this.mContext = ctx;
        this.mUserAllContext = ctx.createContextAsUser(UserHandle.ALL, 0);
        this.mHandler = handler;
        this.mClock = deps.getClock();
        this.mDeps = deps;
        this.mResolver = ctx.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(handler);
        this.mConfigChangeReceiver = new ConfigChangeReceiver();
    }

    public void start() {
        this.mCM = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        this.mNPM = (NetworkPolicyManager) this.mContext.getSystemService(NetworkPolicyManager.class);
        this.mStatsManager = (NetworkStatsManager) this.mContext.getSystemService(NetworkStatsManager.class);
        registerTrackMobileCallback();
        registerNetworkPolicyListener();
        Uri defaultSettingUri = Settings.Global.getUriFor("network_default_daily_multipath_quota_bytes");
        this.mResolver.registerContentObserver(defaultSettingUri, false, this.mSettingsObserver);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mUserAllContext.registerReceiver(this.mConfigChangeReceiver, intentFilter, null, this.mHandler);
    }

    public void shutdown() {
        maybeUnregisterTrackMobileCallback();
        unregisterNetworkPolicyListener();
        for (MultipathTracker t : this.mMultipathTrackers.values()) {
            t.shutdown();
        }
        this.mMultipathTrackers.clear();
        this.mResolver.unregisterContentObserver(this.mSettingsObserver);
        this.mUserAllContext.unregisterReceiver(this.mConfigChangeReceiver);
    }

    public Integer getMultipathPreference(Network network) {
        MultipathTracker t;
        if (network == null || (t = this.mMultipathTrackers.get(network)) == null) {
            return null;
        }
        return Integer.valueOf(t.getMultipathPreference());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MultipathTracker {
        private volatile long mMultipathBudget;
        private NetworkCapabilities mNetworkCapabilities;
        private final NetworkTemplate mNetworkTemplate;
        private long mQuota;
        private final NetworkStatsManager mStatsManager;
        private final int mSubId;
        private final NetworkStatsManager.UsageCallback mUsageCallback;
        private boolean mUsageCallbackRegistered = false;
        final Network network;
        final String subscriberId;

        public MultipathTracker(final Network network, NetworkCapabilities nc) {
            this.network = network;
            this.mNetworkCapabilities = new NetworkCapabilities(nc);
            NetworkSpecifier specifier = nc.getNetworkSpecifier();
            if (specifier instanceof TelephonyNetworkSpecifier) {
                int subscriptionId = ((TelephonyNetworkSpecifier) specifier).getSubscriptionId();
                this.mSubId = subscriptionId;
                TelephonyManager tele = (TelephonyManager) MultipathPolicyTracker.this.mContext.getSystemService(TelephonyManager.class);
                if (tele == null) {
                    throw new IllegalStateException(String.format("Missing TelephonyManager", new Object[0]));
                }
                TelephonyManager tele2 = tele.createForSubscriptionId(subscriptionId);
                if (tele2 == null) {
                    throw new IllegalStateException(String.format("Can't get TelephonyManager for subId %d", Integer.valueOf(subscriptionId)));
                }
                String subscriberId = tele2.getSubscriberId();
                this.subscriberId = subscriberId;
                if (subscriberId == null) {
                    throw new IllegalStateException("Null subscriber Id for subId " + subscriptionId);
                }
                this.mNetworkTemplate = new NetworkTemplate.Builder(1).setSubscriberIds(Set.of(subscriberId)).setMeteredness(1).setDefaultNetworkStatus(0).build();
                this.mUsageCallback = new NetworkStatsManager.UsageCallback() { // from class: com.android.server.connectivity.MultipathPolicyTracker.MultipathTracker.1
                    @Override // android.app.usage.NetworkStatsManager.UsageCallback
                    public void onThresholdReached(int networkType, String subscriberId2) {
                        MultipathTracker.this.updateMultipathBudget();
                    }
                };
                NetworkStatsManager networkStatsManager = (NetworkStatsManager) MultipathPolicyTracker.this.mContext.getSystemService(NetworkStatsManager.class);
                this.mStatsManager = networkStatsManager;
                networkStatsManager.setPollOnOpen(false);
                updateMultipathBudget();
                return;
            }
            throw new IllegalStateException(String.format("Can't get subId from mobile network %s (%s)", network, nc));
        }

        public void setNetworkCapabilities(NetworkCapabilities nc) {
            this.mNetworkCapabilities = new NetworkCapabilities(nc);
        }

        private long getDailyNonDefaultDataUsage() {
            ZonedDateTime end = ZonedDateTime.ofInstant(MultipathPolicyTracker.this.mClock.instant(), ZoneId.systemDefault());
            ZonedDateTime start = end.truncatedTo(ChronoUnit.DAYS);
            long bytes = getNetworkTotalBytes(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
            return bytes;
        }

        private long getNetworkTotalBytes(long start, long end) {
            try {
                NetworkStats.Bucket ret = this.mStatsManager.querySummaryForDevice(this.mNetworkTemplate, start, end);
                return ret.getRxBytes() + ret.getTxBytes();
            } catch (RuntimeException e) {
                Log.w(MultipathPolicyTracker.TAG, "Failed to get data usage: " + e);
                return -1L;
            }
        }

        private NetworkIdentity getTemplateMatchingNetworkIdentity(NetworkCapabilities nc) {
            return new NetworkIdentity.Builder().setType(0).setSubscriberId(this.subscriberId).setRoaming(!nc.hasCapability(18)).setMetered(!nc.hasCapability(11)).setSubId(this.mSubId).build();
        }

        private long getRemainingDailyBudget(long limitBytes, Range<ZonedDateTime> cycle) {
            long start = cycle.getLower().toInstant().toEpochMilli();
            long end = cycle.getUpper().toInstant().toEpochMilli();
            long totalBytes = getNetworkTotalBytes(start, end);
            long remainingBytes = totalBytes != -1 ? Math.max(0L, limitBytes - totalBytes) : 0L;
            long remainingDays = (((end - MultipathPolicyTracker.this.mClock.millis()) - 1) / TimeUnit.DAYS.toMillis(1L)) + 1;
            return remainingBytes / Math.max(1L, remainingDays);
        }

        private long getUserPolicyOpportunisticQuotaBytes() {
            long policyBytes;
            long minQuota = JobStatus.NO_LATEST_RUNTIME;
            NetworkIdentity identity = getTemplateMatchingNetworkIdentity(this.mNetworkCapabilities);
            NetworkPolicy[] policies = MultipathPolicyTracker.this.mNPM.getNetworkPolicies();
            for (NetworkPolicy policy : policies) {
                if (policy.hasCycle() && policy.template.matches(identity)) {
                    long cycleStart = ((ZonedDateTime) ((Range) policy.cycleIterator().next()).getLower()).toInstant().toEpochMilli();
                    long activeWarning = MultipathPolicyTracker.getActiveWarning(policy, cycleStart);
                    if (activeWarning == -1) {
                        policyBytes = MultipathPolicyTracker.getActiveLimit(policy, cycleStart);
                    } else {
                        policyBytes = activeWarning;
                    }
                    if (policyBytes != -1 && policyBytes != -1) {
                        long policyBudget = getRemainingDailyBudget(policyBytes, (Range) policy.cycleIterator().next());
                        minQuota = Math.min(minQuota, policyBudget);
                    }
                }
            }
            if (minQuota == JobStatus.NO_LATEST_RUNTIME) {
                return -1L;
            }
            return minQuota / 20;
        }

        void updateMultipathBudget() {
            long quota = ((NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class)).getSubscriptionOpportunisticQuota(this.network, 2);
            if (quota == -1) {
                quota = getUserPolicyOpportunisticQuotaBytes();
            }
            if (quota == -1) {
                quota = MultipathPolicyTracker.this.getDefaultDailyMultipathQuotaBytes();
            }
            if (haveMultipathBudget() && quota == this.mQuota) {
                return;
            }
            this.mQuota = quota;
            long usage = getDailyNonDefaultDataUsage();
            long budget = usage != -1 ? Math.max(0L, quota - usage) : 0L;
            if (budget > MultipathPolicyTracker.MIN_THRESHOLD_BYTES) {
                setMultipathBudget(budget);
            } else {
                clearMultipathBudget();
            }
        }

        public int getMultipathPreference() {
            if (haveMultipathBudget()) {
                return 3;
            }
            return 0;
        }

        public long getQuota() {
            return this.mQuota;
        }

        public long getMultipathBudget() {
            return this.mMultipathBudget;
        }

        private boolean haveMultipathBudget() {
            return this.mMultipathBudget > 0;
        }

        private void setMultipathBudget(long budget) {
            maybeUnregisterUsageCallback();
            this.mStatsManager.registerUsageCallback(this.mNetworkTemplate, budget, new Executor() { // from class: com.android.server.connectivity.MultipathPolicyTracker$MultipathTracker$$ExternalSyntheticLambda0
                @Override // java.util.concurrent.Executor
                public final void execute(Runnable runnable) {
                    MultipathPolicyTracker.MultipathTracker.this.m2773x53b8f526(runnable);
                }
            }, this.mUsageCallback);
            this.mUsageCallbackRegistered = true;
            this.mMultipathBudget = budget;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setMultipathBudget$0$com-android-server-connectivity-MultipathPolicyTracker$MultipathTracker  reason: not valid java name */
        public /* synthetic */ void m2773x53b8f526(Runnable command) {
            MultipathPolicyTracker.this.mHandler.post(command);
        }

        private void maybeUnregisterUsageCallback() {
            if (this.mUsageCallbackRegistered) {
                this.mStatsManager.unregisterUsageCallback(this.mUsageCallback);
                this.mUsageCallbackRegistered = false;
            }
        }

        private void clearMultipathBudget() {
            maybeUnregisterUsageCallback();
            this.mMultipathBudget = 0L;
        }

        void shutdown() {
            clearMultipathBudget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long getActiveWarning(NetworkPolicy policy, long cycleStart) {
        if (policy.lastWarningSnooze < cycleStart) {
            return policy.warningBytes;
        }
        return -1L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long getActiveLimit(NetworkPolicy policy, long cycleStart) {
        if (policy.lastLimitSnooze < cycleStart) {
            return policy.limitBytes;
        }
        return -1L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getDefaultDailyMultipathQuotaBytes() {
        String setting = Settings.Global.getString(this.mContext.getContentResolver(), "network_default_daily_multipath_quota_bytes");
        if (setting != null) {
            try {
                return Long.parseLong(setting);
            } catch (NumberFormatException e) {
            }
        }
        return this.mContext.getResources().getInteger(17694887);
    }

    private void registerTrackMobileCallback() {
        NetworkRequest request = new NetworkRequest.Builder().addCapability(12).addTransportType(0).build();
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() { // from class: com.android.server.connectivity.MultipathPolicyTracker.1
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                MultipathTracker existing = (MultipathTracker) MultipathPolicyTracker.this.mMultipathTrackers.get(network);
                if (existing != null) {
                    existing.setNetworkCapabilities(nc);
                    existing.updateMultipathBudget();
                    return;
                }
                try {
                    MultipathPolicyTracker.this.mMultipathTrackers.put(network, new MultipathTracker(network, nc));
                } catch (IllegalStateException e) {
                    Log.e(MultipathPolicyTracker.TAG, "Can't track mobile network " + network + ": " + e.getMessage());
                }
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                MultipathTracker existing = (MultipathTracker) MultipathPolicyTracker.this.mMultipathTrackers.get(network);
                if (existing != null) {
                    existing.shutdown();
                    MultipathPolicyTracker.this.mMultipathTrackers.remove(network);
                }
            }
        };
        this.mMobileNetworkCallback = networkCallback;
        this.mCM.registerNetworkCallback(request, networkCallback, this.mHandler);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAllMultipathBudgets() {
        for (MultipathTracker t : this.mMultipathTrackers.values()) {
            t.updateMultipathBudget();
        }
    }

    private void maybeUnregisterTrackMobileCallback() {
        ConnectivityManager.NetworkCallback networkCallback = this.mMobileNetworkCallback;
        if (networkCallback != null) {
            this.mCM.unregisterNetworkCallback(networkCallback);
        }
        this.mMobileNetworkCallback = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.connectivity.MultipathPolicyTracker$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends NetworkPolicyManager.Listener {
        AnonymousClass2() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onMeteredIfacesChanged$0$com-android-server-connectivity-MultipathPolicyTracker$2  reason: not valid java name */
        public /* synthetic */ void m2772x108bec09() {
            MultipathPolicyTracker.this.updateAllMultipathBudgets();
        }

        public void onMeteredIfacesChanged(String[] meteredIfaces) {
            MultipathPolicyTracker.this.mHandler.post(new Runnable() { // from class: com.android.server.connectivity.MultipathPolicyTracker$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MultipathPolicyTracker.AnonymousClass2.this.m2772x108bec09();
                }
            });
        }
    }

    private void registerNetworkPolicyListener() {
        AnonymousClass2 anonymousClass2 = new AnonymousClass2();
        this.mPolicyListener = anonymousClass2;
        this.mNPM.registerListener(anonymousClass2);
    }

    private void unregisterNetworkPolicyListener() {
        this.mNPM.unregisterListener(this.mPolicyListener);
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            Log.wtf(MultipathPolicyTracker.TAG, "Should never be reached.");
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (!Settings.Global.getUriFor("network_default_daily_multipath_quota_bytes").equals(uri)) {
                Log.wtf(MultipathPolicyTracker.TAG, "Unexpected settings observation: " + uri);
            }
            MultipathPolicyTracker.this.updateAllMultipathBudgets();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ConfigChangeReceiver extends BroadcastReceiver {
        private ConfigChangeReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MultipathPolicyTracker.this.updateAllMultipathBudgets();
        }
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("MultipathPolicyTracker:");
        pw.increaseIndent();
        for (MultipathTracker t : this.mMultipathTrackers.values()) {
            pw.println(String.format("Network %s: quota %d, budget %d. Preference: %s", t.network, Long.valueOf(t.getQuota()), Long.valueOf(t.getMultipathBudget()), DebugUtils.flagsToString(ConnectivityManager.class, "MULTIPATH_PREFERENCE_", t.getMultipathPreference())));
        }
        pw.decreaseIndent();
    }
}
