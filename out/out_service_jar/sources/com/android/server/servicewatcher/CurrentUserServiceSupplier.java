package com.android.server.servicewatcher;

import android.app.ActivityManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.servicewatcher.CurrentUserServiceSupplier;
import com.android.server.servicewatcher.ServiceWatcher;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class CurrentUserServiceSupplier extends BroadcastReceiver implements ServiceWatcher.ServiceSupplier<BoundServiceInfo> {
    private static final String EXTRA_SERVICE_IS_MULTIUSER = "serviceIsMultiuser";
    private static final String EXTRA_SERVICE_VERSION = "serviceVersion";
    private static final String TAG = "CurrentUserServiceSupplier";
    private static final Comparator<BoundServiceInfo> sBoundServiceInfoComparator = new Comparator() { // from class: com.android.server.servicewatcher.CurrentUserServiceSupplier$$ExternalSyntheticLambda0
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return CurrentUserServiceSupplier.lambda$static$0((CurrentUserServiceSupplier.BoundServiceInfo) obj, (CurrentUserServiceSupplier.BoundServiceInfo) obj2);
        }
    };
    private final ActivityManagerInternal mActivityManager = (ActivityManagerInternal) Objects.requireNonNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
    private final String mCallerPermission;
    private final Context mContext;
    private final Intent mIntent;
    private volatile ServiceWatcher.ServiceChangedListener mListener;
    private final boolean mMatchSystemAppsOnly;
    private final String mServicePermission;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$static$0(BoundServiceInfo o1, BoundServiceInfo o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        int ret = Integer.compare(o1.getVersion(), o2.getVersion());
        if (ret == 0) {
            if (o1.getUserId() != 0 && o2.getUserId() == 0) {
                return -1;
            }
            if (o1.getUserId() == 0 && o2.getUserId() != 0) {
                return 1;
            }
            return ret;
        }
        return ret;
    }

    /* loaded from: classes2.dex */
    public static class BoundServiceInfo extends ServiceWatcher.BoundServiceInfo {
        private final Bundle mMetadata;
        private final int mVersion;

        private static int parseUid(ResolveInfo resolveInfo) {
            int uid = resolveInfo.serviceInfo.applicationInfo.uid;
            Bundle metadata = resolveInfo.serviceInfo.metaData;
            if (metadata != null && metadata.getBoolean(CurrentUserServiceSupplier.EXTRA_SERVICE_IS_MULTIUSER, false)) {
                return UserHandle.getUid(0, UserHandle.getAppId(uid));
            }
            return uid;
        }

        private static int parseVersion(ResolveInfo resolveInfo) {
            if (resolveInfo.serviceInfo.metaData != null) {
                int version = resolveInfo.serviceInfo.metaData.getInt(CurrentUserServiceSupplier.EXTRA_SERVICE_VERSION, Integer.MIN_VALUE);
                return version;
            }
            return Integer.MIN_VALUE;
        }

        protected BoundServiceInfo(String action, ResolveInfo resolveInfo) {
            this(action, parseUid(resolveInfo), resolveInfo.serviceInfo.getComponentName(), parseVersion(resolveInfo), resolveInfo.serviceInfo.metaData);
        }

        protected BoundServiceInfo(String action, int uid, ComponentName componentName, int version, Bundle metadata) {
            super(action, uid, componentName);
            this.mVersion = version;
            this.mMetadata = metadata;
        }

        public int getVersion() {
            return this.mVersion;
        }

        public Bundle getMetadata() {
            return this.mMetadata;
        }

        @Override // com.android.server.servicewatcher.ServiceWatcher.BoundServiceInfo
        public String toString() {
            return super.toString() + "@" + this.mVersion;
        }
    }

    public static CurrentUserServiceSupplier createFromConfig(Context context, String action, int enableOverlayResId, int nonOverlayPackageResId) {
        String explicitPackage = retrieveExplicitPackage(context, enableOverlayResId, nonOverlayPackageResId);
        return create(context, action, explicitPackage, null, null);
    }

    public static CurrentUserServiceSupplier create(Context context, String action, String explicitPackage, String callerPermission, String servicePermission) {
        return new CurrentUserServiceSupplier(context, action, explicitPackage, callerPermission, servicePermission, true);
    }

    public static CurrentUserServiceSupplier createUnsafeForTestsOnly(Context context, String action, String explicitPackage, String callerPermission, String servicePermission) {
        return new CurrentUserServiceSupplier(context, action, explicitPackage, callerPermission, servicePermission, false);
    }

    private static String retrieveExplicitPackage(Context context, int enableOverlayResId, int nonOverlayPackageResId) {
        Resources resources = context.getResources();
        boolean enableOverlay = resources.getBoolean(enableOverlayResId);
        if (!enableOverlay) {
            return resources.getString(nonOverlayPackageResId);
        }
        return null;
    }

    private CurrentUserServiceSupplier(Context context, String action, String explicitPackage, String callerPermission, String servicePermission, boolean matchSystemAppsOnly) {
        this.mContext = context;
        Intent intent = new Intent(action);
        this.mIntent = intent;
        if (explicitPackage != null) {
            intent.setPackage(explicitPackage);
        }
        this.mCallerPermission = callerPermission;
        this.mServicePermission = servicePermission;
        this.mMatchSystemAppsOnly = matchSystemAppsOnly;
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceSupplier
    public boolean hasMatchingService() {
        int intentQueryFlags = this.mMatchSystemAppsOnly ? 786432 | 1048576 : 786432;
        List<ResolveInfo> resolveInfos = this.mContext.getPackageManager().queryIntentServicesAsUser(this.mIntent, intentQueryFlags, 0);
        return !resolveInfos.isEmpty();
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceSupplier
    public void register(ServiceWatcher.ServiceChangedListener listener) {
        Preconditions.checkState(this.mListener == null);
        this.mListener = listener;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this, UserHandle.ALL, intentFilter, null, FgThread.getHandler());
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceSupplier
    public void unregister() {
        Preconditions.checkArgument(this.mListener != null);
        this.mListener = null;
        this.mContext.unregisterReceiver(this);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceSupplier
    public BoundServiceInfo getServiceInfo() {
        BoundServiceInfo bestServiceInfo = null;
        int intentQueryFlags = this.mMatchSystemAppsOnly ? 268435584 | 1048576 : 268435584;
        int currentUserId = this.mActivityManager.getCurrentUserId();
        List<ResolveInfo> resolveInfos = this.mContext.getPackageManager().queryIntentServicesAsUser(this.mIntent, intentQueryFlags, currentUserId);
        for (ResolveInfo resolveInfo : resolveInfos) {
            ServiceInfo service = (ServiceInfo) Objects.requireNonNull(resolveInfo.serviceInfo);
            String str = this.mCallerPermission;
            if (str != null && !str.equals(service.permission)) {
                Log.d(TAG, service.getComponentName().flattenToShortString() + " disqualified due to not requiring " + this.mCallerPermission);
            } else {
                BoundServiceInfo serviceInfo = new BoundServiceInfo(this.mIntent.getAction(), resolveInfo);
                String str2 = this.mServicePermission;
                if (str2 != null && PermissionManager.checkPackageNamePermission(str2, service.packageName, serviceInfo.getUserId()) != 0) {
                    Log.d(TAG, serviceInfo.getComponentName().flattenToShortString() + " disqualified due to not holding " + this.mCallerPermission);
                } else if (sBoundServiceInfoComparator.compare(serviceInfo, bestServiceInfo) > 0) {
                    bestServiceInfo = serviceInfo;
                }
            }
        }
        return bestServiceInfo;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        int userId;
        ServiceWatcher.ServiceChangedListener listener;
        String action = intent.getAction();
        if (action == null || (userId = intent.getIntExtra("android.intent.extra.user_handle", -10000)) == -10000 || (listener = this.mListener) == null) {
            return;
        }
        char c = 65535;
        switch (action.hashCode()) {
            case 833559602:
                if (action.equals("android.intent.action.USER_UNLOCKED")) {
                    c = 1;
                    break;
                }
                break;
            case 959232034:
                if (action.equals("android.intent.action.USER_SWITCHED")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                listener.onServiceChanged();
                return;
            case 1:
                if (userId == this.mActivityManager.getCurrentUserId()) {
                    listener.onServiceChanged();
                    return;
                }
                return;
            default:
                return;
        }
    }
}
