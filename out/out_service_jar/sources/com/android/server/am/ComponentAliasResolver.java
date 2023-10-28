package com.android.server.am;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.compat.CompatChange;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class ComponentAliasResolver {
    private static final String ALIAS_FILTER_ACTION = "com.android.intent.action.EXPERIMENTAL_IS_ALIAS";
    private static final String ALIAS_FILTER_ACTION_ALT = "android.intent.action.EXPERIMENTAL_IS_ALIAS";
    private static final boolean DEBUG = true;
    private static final String META_DATA_ALIAS_TARGET = "alias_target";
    private static final String OPT_IN_PROPERTY = "com.android.EXPERIMENTAL_COMPONENT_ALIAS_OPT_IN";
    private static final int PACKAGE_QUERY_FLAGS = 4989056;
    private static final String TAG = "ComponentAliasResolver";
    public static final long USE_EXPERIMENTAL_COMPONENT_ALIAS = 196254758;
    private final ActivityManagerService mAm;
    private final Context mContext;
    private boolean mEnabled;
    private boolean mEnabledByDeviceConfig;
    private String mOverrideString;
    private PlatformCompat mPlatformCompat;
    private final Object mLock = new Object();
    private final ArrayMap<ComponentName, ComponentName> mFromTo = new ArrayMap<>();
    final PackageMonitor mPackageMonitor = new PackageMonitor() { // from class: com.android.server.am.ComponentAliasResolver.1
        public void onPackageModified(String packageName) {
            ComponentAliasResolver.this.refresh();
        }

        public void onPackageAdded(String packageName, int uid) {
            ComponentAliasResolver.this.refresh();
        }

        public void onPackageRemoved(String packageName, int uid) {
            ComponentAliasResolver.this.refresh();
        }
    };
    private final CompatChange.ChangeListener mCompatChangeListener = new CompatChange.ChangeListener() { // from class: com.android.server.am.ComponentAliasResolver$$ExternalSyntheticLambda3
        @Override // com.android.server.compat.CompatChange.ChangeListener
        public final void onCompatChange(String str) {
            ComponentAliasResolver.this.m1415lambda$new$0$comandroidserveramComponentAliasResolver(str);
        }
    };

    public ComponentAliasResolver(ActivityManagerService service) {
        this.mAm = service;
        this.mContext = service.mContext;
    }

    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mEnabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-am-ComponentAliasResolver  reason: not valid java name */
    public /* synthetic */ void m1415lambda$new$0$comandroidserveramComponentAliasResolver(String packageName) {
        Slog.d(TAG, "USE_EXPERIMENTAL_COMPONENT_ALIAS changed.");
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.am.ComponentAliasResolver$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                ComponentAliasResolver.this.refresh();
            }
        });
    }

    public void onSystemReady(boolean enabledByDeviceConfig, String overrides) {
        synchronized (this.mLock) {
            PlatformCompat platformCompat = (PlatformCompat) ServiceManager.getService("platform_compat");
            this.mPlatformCompat = platformCompat;
            platformCompat.registerListener(USE_EXPERIMENTAL_COMPONENT_ALIAS, this.mCompatChangeListener);
        }
        Slog.d(TAG, "Compat listener set.");
        update(enabledByDeviceConfig, overrides);
    }

    public void update(boolean enabledByDeviceConfig, String overrides) {
        synchronized (this.mLock) {
            if (this.mPlatformCompat == null) {
                return;
            }
            boolean z = false;
            if (Build.isDebuggable() && (enabledByDeviceConfig || this.mPlatformCompat.isChangeEnabledByPackageName(USE_EXPERIMENTAL_COMPONENT_ALIAS, PackageManagerService.PLATFORM_PACKAGE_NAME, 0))) {
                z = true;
            }
            final boolean enabled = z;
            if (enabled != this.mEnabled) {
                Slog.i(TAG, (enabled ? "Enabling" : "Disabling") + " component aliases...");
                FgThread.getHandler().post(new Runnable() { // from class: com.android.server.am.ComponentAliasResolver$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ComponentAliasResolver.this.m1416lambda$update$1$comandroidserveramComponentAliasResolver(enabled);
                    }
                });
            }
            this.mEnabled = enabled;
            this.mEnabledByDeviceConfig = enabledByDeviceConfig;
            this.mOverrideString = overrides;
            if (enabled) {
                refreshLocked();
            } else {
                this.mFromTo.clear();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$update$1$com-android-server-am-ComponentAliasResolver  reason: not valid java name */
    public /* synthetic */ void m1416lambda$update$1$comandroidserveramComponentAliasResolver(boolean enabled) {
        if (enabled) {
            this.mPackageMonitor.register(this.mAm.mContext, UserHandle.ALL, false, BackgroundThread.getHandler());
        } else {
            this.mPackageMonitor.unregister();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refresh() {
        synchronized (this.mLock) {
            update(this.mEnabledByDeviceConfig, this.mOverrideString);
        }
    }

    private void refreshLocked() {
        Slog.d(TAG, "Refreshing aliases...");
        this.mFromTo.clear();
        loadFromMetadataLocked();
        loadOverridesLocked();
    }

    private void loadFromMetadataLocked() {
        Slog.d(TAG, "Scanning service aliases...");
        loadFromMetadataLockedInner(new Intent(ALIAS_FILTER_ACTION_ALT));
        loadFromMetadataLockedInner(new Intent(ALIAS_FILTER_ACTION));
    }

    private void loadFromMetadataLockedInner(Intent i) {
        List<ResolveInfo> services = this.mContext.getPackageManager().queryIntentServicesAsUser(i, PACKAGE_QUERY_FLAGS, 0);
        extractAliasesLocked(services);
        Slog.d(TAG, "Scanning receiver aliases...");
        List<ResolveInfo> receivers = this.mContext.getPackageManager().queryBroadcastReceiversAsUser(i, PACKAGE_QUERY_FLAGS, 0);
        extractAliasesLocked(receivers);
    }

    private boolean isEnabledForPackageLocked(String packageName) {
        boolean enabled = false;
        try {
            PackageManager.Property p = this.mContext.getPackageManager().getProperty(OPT_IN_PROPERTY, packageName);
            enabled = p.getBoolean();
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (!enabled) {
            Slog.w(TAG, "USE_EXPERIMENTAL_COMPONENT_ALIAS not enabled for " + packageName);
        }
        return enabled;
    }

    private static boolean validateAlias(ComponentName from, ComponentName to) {
        String fromPackage = from.getPackageName();
        String toPackage = to.getPackageName();
        if (Objects.equals(fromPackage, toPackage) || toPackage.startsWith(fromPackage + ".")) {
            return true;
        }
        Slog.w(TAG, "Invalid alias: " + from.flattenToShortString() + " -> " + to.flattenToShortString());
        return false;
    }

    private void validateAndAddAliasLocked(ComponentName from, ComponentName to) {
        Slog.d(TAG, "" + from.flattenToShortString() + " -> " + to.flattenToShortString());
        if (!validateAlias(from, to) || !isEnabledForPackageLocked(from.getPackageName()) || !isEnabledForPackageLocked(to.getPackageName())) {
            return;
        }
        this.mFromTo.put(from, to);
    }

    private void extractAliasesLocked(List<ResolveInfo> components) {
        for (ResolveInfo ri : components) {
            ComponentInfo ci = ri.getComponentInfo();
            ComponentName from = ci.getComponentName();
            ComponentName to = unflatten(ci.metaData.getString(META_DATA_ALIAS_TARGET));
            if (to != null) {
                validateAndAddAliasLocked(from, to);
            }
        }
    }

    private void loadOverridesLocked() {
        String[] split;
        ComponentName from;
        Slog.d(TAG, "Loading aliases overrides ...");
        for (String line : this.mOverrideString.split("\\,+")) {
            String[] fields = line.split("\\:+", 2);
            if (!TextUtils.isEmpty(fields[0]) && (from = unflatten(fields[0])) != null) {
                if (fields.length == 1) {
                    Slog.d(TAG, "" + from.flattenToShortString() + " [removed]");
                    this.mFromTo.remove(from);
                } else {
                    ComponentName to = unflatten(fields[1]);
                    if (to != null) {
                        validateAndAddAliasLocked(from, to);
                    }
                }
            }
        }
    }

    private static ComponentName unflatten(String name) {
        ComponentName cn = ComponentName.unflattenFromString(name);
        if (cn != null) {
            return cn;
        }
        Slog.e(TAG, "Invalid component name detected: " + name);
        return null;
    }

    public void dump(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("ACTIVITY MANAGER COMPONENT-ALIAS (dumpsys activity component-alias)");
            pw.print("  Enabled: ");
            pw.println(this.mEnabled);
            pw.println("  Aliases:");
            for (int i = 0; i < this.mFromTo.size(); i++) {
                ComponentName from = this.mFromTo.keyAt(i);
                ComponentName to = this.mFromTo.valueAt(i);
                pw.print("    ");
                pw.print(from.flattenToShortString());
                pw.print(" -> ");
                pw.print(to.flattenToShortString());
                pw.println();
            }
            pw.println();
        }
    }

    /* loaded from: classes.dex */
    public static class Resolution<T> {
        public final T resolved;
        public final T source;

        public Resolution(T source, T resolved) {
            this.source = source;
            this.resolved = resolved;
        }

        public boolean isAlias() {
            return this.resolved != null;
        }

        public T getAlias() {
            if (isAlias()) {
                return this.source;
            }
            return null;
        }

        public T getTarget() {
            if (isAlias()) {
                return this.resolved;
            }
            return null;
        }
    }

    public Resolution<ComponentName> resolveComponentAlias(Supplier<ComponentName> aliasSupplier) {
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (!this.mEnabled) {
                    return new Resolution<>(null, null);
                }
                ComponentName alias = aliasSupplier.get();
                ComponentName target = this.mFromTo.get(alias);
                if (target != null) {
                    Exception stacktrace = null;
                    if (Log.isLoggable(TAG, 2)) {
                        stacktrace = new RuntimeException("STACKTRACE");
                    }
                    Slog.d(TAG, "Alias resolved: " + alias.flattenToShortString() + " -> " + target.flattenToShortString(), stacktrace);
                }
                return new Resolution<>(alias, target);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public Resolution<ComponentName> resolveService(final Intent service, final String resolvedType, final int packageFlags, final int userId, final int callingUid) {
        Resolution<ComponentName> result = resolveComponentAlias(new Supplier() { // from class: com.android.server.am.ComponentAliasResolver$$ExternalSyntheticLambda2
            @Override // java.util.function.Supplier
            public final Object get() {
                return ComponentAliasResolver.lambda$resolveService$2(service, resolvedType, packageFlags, userId, callingUid);
            }
        });
        if (result != null && result.isAlias()) {
            service.setOriginalIntent(new Intent(service));
            service.setPackage(null);
            service.setComponent(result.getTarget());
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ComponentName lambda$resolveService$2(Intent service, String resolvedType, int packageFlags, int userId, int callingUid) {
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        ResolveInfo rInfo = pmi.resolveService(service, resolvedType, packageFlags, userId, callingUid);
        ServiceInfo sInfo = rInfo != null ? rInfo.serviceInfo : null;
        if (sInfo == null) {
            return null;
        }
        return new ComponentName(sInfo.applicationInfo.packageName, sInfo.name);
    }

    public Resolution<ResolveInfo> resolveReceiver(Intent intent, final ResolveInfo receiver, String resolvedType, int packageFlags, int userId, int callingUid, boolean forSend) {
        Resolution<ComponentName> resolution = resolveComponentAlias(new Supplier() { // from class: com.android.server.am.ComponentAliasResolver$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                ComponentName componentName;
                componentName = receiver.activityInfo.getComponentName();
                return componentName;
            }
        });
        ComponentName target = resolution.getTarget();
        if (target == null) {
            return new Resolution<>(receiver, null);
        }
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        Intent i = new Intent(intent);
        i.setPackage(null);
        i.setComponent(resolution.getTarget());
        List<ResolveInfo> resolved = pmi.queryIntentReceivers(i, resolvedType, packageFlags, callingUid, userId, forSend);
        if (resolved != null && resolved.size() != 0) {
            return new Resolution<>(receiver, resolved.get(0));
        }
        Slog.w(TAG, "Alias target " + target.flattenToShortString() + " not found");
        return null;
    }
}
