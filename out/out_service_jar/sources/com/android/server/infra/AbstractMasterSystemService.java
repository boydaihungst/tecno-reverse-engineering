package com.android.server.infra;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.infra.ServiceNameResolver;
import com.android.server.pm.UserManagerInternal;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public abstract class AbstractMasterSystemService<M extends AbstractMasterSystemService<M, S>, S extends AbstractPerUserSystemService<S, M>> extends SystemService {
    public static final int PACKAGE_RESTART_POLICY_NO_REFRESH = 16;
    public static final int PACKAGE_RESTART_POLICY_REFRESH_EAGER = 64;
    public static final int PACKAGE_RESTART_POLICY_REFRESH_LAZY = 32;
    public static final int PACKAGE_UPDATE_POLICY_NO_REFRESH = 1;
    public static final int PACKAGE_UPDATE_POLICY_REFRESH_EAGER = 4;
    public static final int PACKAGE_UPDATE_POLICY_REFRESH_LAZY = 2;
    public boolean debug;
    protected boolean mAllowInstantService;
    private final SparseBooleanArray mDisabledByUserRestriction;
    protected final Object mLock;
    protected final ServiceNameResolver mServiceNameResolver;
    private final int mServicePackagePolicyFlags;
    private final SparseArray<List<S>> mServicesCacheList;
    protected final String mTag;
    private UserManagerInternal mUm;
    private SparseArray<String> mUpdatingPackageNames;
    public boolean verbose;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface ServicePackagePolicyFlags {
    }

    /* loaded from: classes.dex */
    public interface Visitor<S> {
        void visit(S s);
    }

    protected abstract S newServiceLocked(int i, boolean z);

    /* JADX INFO: Access modifiers changed from: protected */
    public AbstractMasterSystemService(Context context, ServiceNameResolver serviceNameResolver, String disallowProperty) {
        this(context, serviceNameResolver, disallowProperty, 34);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public AbstractMasterSystemService(Context context, ServiceNameResolver serviceNameResolver, final String disallowProperty, int servicePackagePolicyFlags) {
        super(context);
        this.mTag = getClass().getSimpleName();
        this.mLock = new Object();
        this.verbose = false;
        this.debug = false;
        this.mServicesCacheList = new SparseArray<>();
        servicePackagePolicyFlags = (servicePackagePolicyFlags & 7) == 0 ? servicePackagePolicyFlags | 2 : servicePackagePolicyFlags;
        this.mServicePackagePolicyFlags = (servicePackagePolicyFlags & 112) == 0 ? servicePackagePolicyFlags | 32 : servicePackagePolicyFlags;
        this.mServiceNameResolver = serviceNameResolver;
        if (serviceNameResolver != null) {
            serviceNameResolver.setOnTemporaryServiceNameChangedCallback(new ServiceNameResolver.NameResolverListener() { // from class: com.android.server.infra.AbstractMasterSystemService$$ExternalSyntheticLambda0
                @Override // com.android.server.infra.ServiceNameResolver.NameResolverListener
                public final void onNameResolved(int i, String str, boolean z) {
                    AbstractMasterSystemService.this.onServiceNameChanged(i, str, z);
                }
            });
        }
        if (disallowProperty == null) {
            this.mDisabledByUserRestriction = null;
        } else {
            this.mDisabledByUserRestriction = new SparseBooleanArray();
            UserManagerInternal umi = getUserManagerInternal();
            List<UserInfo> users = getSupportedUsers();
            for (int i = 0; i < users.size(); i++) {
                int userId = users.get(i).id;
                boolean disabled = umi.getUserRestriction(userId, disallowProperty);
                if (disabled) {
                    Slog.i(this.mTag, "Disabling by restrictions user " + userId);
                    this.mDisabledByUserRestriction.put(userId, disabled);
                }
            }
            umi.addUserRestrictionsListener(new UserManagerInternal.UserRestrictionsListener() { // from class: com.android.server.infra.AbstractMasterSystemService$$ExternalSyntheticLambda1
                @Override // com.android.server.pm.UserManagerInternal.UserRestrictionsListener
                public final void onUserRestrictionsChanged(int i2, Bundle bundle, Bundle bundle2) {
                    AbstractMasterSystemService.this.m3903x63c93efa(disallowProperty, i2, bundle, bundle2);
                }
            });
        }
        startTrackingPackageChanges();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-infra-AbstractMasterSystemService  reason: not valid java name */
    public /* synthetic */ void m3903x63c93efa(String disallowProperty, int userId, Bundle newRestrictions, Bundle prevRestrictions) {
        boolean disabledNow = newRestrictions.getBoolean(disallowProperty, false);
        synchronized (this.mLock) {
            boolean disabledBefore = this.mDisabledByUserRestriction.get(userId);
            if (disabledBefore == disabledNow && this.debug) {
                Slog.d(this.mTag, "Restriction did not change for user " + userId);
                return;
            }
            Slog.i(this.mTag, "Updating for user " + userId + ": disabled=" + disabledNow);
            this.mDisabledByUserRestriction.put(userId, disabledNow);
            updateCachedServiceLocked(userId, disabledNow);
        }
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 600) {
            new SettingsObserver(BackgroundThread.getHandler());
        }
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            updateCachedServiceLocked(user.getUserIdentifier());
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStopped(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            removeCachedServiceListLocked(user.getUserIdentifier());
        }
    }

    public final boolean getAllowInstantService() {
        boolean z;
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            z = this.mAllowInstantService;
        }
        return z;
    }

    public final boolean isBindInstantServiceAllowed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mAllowInstantService;
        }
        return z;
    }

    public final void setAllowInstantService(boolean mode) {
        Slog.i(this.mTag, "setAllowInstantService(): " + mode);
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            this.mAllowInstantService = mode;
        }
    }

    public final void setTemporaryService(int userId, String componentName, int durationMs) {
        Slog.i(this.mTag, "setTemporaryService(" + userId + ") to " + componentName + " for " + durationMs + "ms");
        if (this.mServiceNameResolver == null) {
            return;
        }
        enforceCallingPermissionForManagement();
        Objects.requireNonNull(componentName);
        int maxDurationMs = getMaximumTemporaryServiceDurationMs();
        if (durationMs > maxDurationMs) {
            throw new IllegalArgumentException("Max duration is " + maxDurationMs + " (called with " + durationMs + ")");
        }
        synchronized (this.mLock) {
            S oldService = peekServiceForUserLocked(userId);
            if (oldService != null) {
                oldService.removeSelfFromCache();
            }
            this.mServiceNameResolver.setTemporaryService(userId, componentName, durationMs);
        }
    }

    public final void setTemporaryServices(int userId, String[] componentNames, int durationMs) {
        Slog.i(this.mTag, "setTemporaryService(" + userId + ") to " + Arrays.toString(componentNames) + " for " + durationMs + "ms");
        if (this.mServiceNameResolver == null) {
            return;
        }
        enforceCallingPermissionForManagement();
        Objects.requireNonNull(componentNames);
        int maxDurationMs = getMaximumTemporaryServiceDurationMs();
        if (durationMs > maxDurationMs) {
            throw new IllegalArgumentException("Max duration is " + maxDurationMs + " (called with " + durationMs + ")");
        }
        synchronized (this.mLock) {
            S oldService = peekServiceForUserLocked(userId);
            if (oldService != null) {
                oldService.removeSelfFromCache();
            }
            this.mServiceNameResolver.setTemporaryServices(userId, componentNames, durationMs);
        }
    }

    public final boolean setDefaultServiceEnabled(int userId, boolean enabled) {
        Slog.i(this.mTag, "setDefaultServiceEnabled() for userId " + userId + ": " + enabled);
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            ServiceNameResolver serviceNameResolver = this.mServiceNameResolver;
            if (serviceNameResolver == null) {
                return false;
            }
            boolean changed = serviceNameResolver.setDefaultServiceEnabled(userId, enabled);
            if (!changed) {
                if (this.verbose) {
                    Slog.v(this.mTag, "setDefaultServiceEnabled(" + userId + "): already " + enabled);
                }
                return false;
            }
            S oldService = peekServiceForUserLocked(userId);
            if (oldService != null) {
                oldService.removeSelfFromCache();
            }
            updateCachedServiceLocked(userId);
            return true;
        }
    }

    public final boolean isDefaultServiceEnabled(int userId) {
        boolean isDefaultServiceEnabled;
        enforceCallingPermissionForManagement();
        if (this.mServiceNameResolver == null) {
            return false;
        }
        synchronized (this.mLock) {
            isDefaultServiceEnabled = this.mServiceNameResolver.isDefaultServiceEnabled(userId);
        }
        return isDefaultServiceEnabled;
    }

    protected int getMaximumTemporaryServiceDurationMs() {
        throw new UnsupportedOperationException("Not implemented by " + getClass());
    }

    public final void resetTemporaryService(int userId) {
        Slog.i(this.mTag, "resetTemporaryService(): " + userId);
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            S service = getServiceForUserLocked(userId);
            if (service != null) {
                service.resetTemporaryServiceLocked();
            }
        }
    }

    protected void enforceCallingPermissionForManagement() {
        throw new UnsupportedOperationException("Not implemented by " + getClass());
    }

    protected List<S> newServiceListLocked(int resolvedUserId, boolean disabled, String[] serviceNames) {
        throw new UnsupportedOperationException("newServiceListLocked not implemented. ");
    }

    protected void registerForExtraSettingsChanges(ContentResolver resolver, ContentObserver observer) {
    }

    protected void onSettingsChanged(int userId, String property) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public S getServiceForUserLocked(int userId) {
        List<S> services = getServiceListForUserLocked(userId);
        if (services == null || services.size() == 0) {
            return null;
        }
        return services.get(0);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<S> getServiceListForUserLocked(int userId) {
        int resolvedUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, null, null);
        List<S> services = this.mServicesCacheList.get(resolvedUserId);
        if (services == null || services.size() == 0) {
            boolean disabled = isDisabledLocked(userId);
            ServiceNameResolver serviceNameResolver = this.mServiceNameResolver;
            if (serviceNameResolver != null && serviceNameResolver.isConfiguredInMultipleMode()) {
                services = newServiceListLocked(resolvedUserId, disabled, this.mServiceNameResolver.getServiceNameList(userId));
            } else {
                services = new ArrayList<>();
                services.add(newServiceLocked(resolvedUserId, disabled));
            }
            if (!disabled) {
                for (int i = 0; i < services.size(); i++) {
                    onServiceEnabledLocked(services.get(i), resolvedUserId);
                }
            }
            this.mServicesCacheList.put(userId, services);
        }
        return services;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public S peekServiceForUserLocked(int userId) {
        List<S> serviceList = peekServiceListForUserLocked(userId);
        if (serviceList == null || serviceList.size() == 0) {
            return null;
        }
        return serviceList.get(0);
    }

    protected List<S> peekServiceListForUserLocked(int userId) {
        int resolvedUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, null, null);
        return this.mServicesCacheList.get(resolvedUserId);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateCachedServiceLocked(int userId) {
        updateCachedServiceListLocked(userId, isDisabledLocked(userId));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isDisabledLocked(int userId) {
        SparseBooleanArray sparseBooleanArray = this.mDisabledByUserRestriction;
        return sparseBooleanArray != null && sparseBooleanArray.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public S updateCachedServiceLocked(int userId, boolean disabled) {
        S service = getServiceForUserLocked(userId);
        updateCachedServiceListLocked(userId, disabled);
        return service;
    }

    protected List<S> updateCachedServiceListLocked(int userId, boolean disabled) {
        List<S> services = getServiceListForUserLocked(userId);
        if (services == null) {
            return null;
        }
        for (int i = 0; i < services.size(); i++) {
            S service = services.get(i);
            if (service != null) {
                synchronized (service.mLock) {
                    service.updateLocked(disabled);
                    if (!service.isEnabledLocked()) {
                        removeCachedServiceListLocked(userId);
                    } else {
                        onServiceEnabledLocked(services.get(i), userId);
                    }
                }
            }
        }
        return services;
    }

    protected String getServiceSettingsProperty() {
        return null;
    }

    protected void onServiceEnabledLocked(S service, int userId) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final List<S> removeCachedServiceListLocked(int userId) {
        List<S> services = peekServiceListForUserLocked(userId);
        if (services != null) {
            this.mServicesCacheList.delete(userId);
            for (int i = 0; i < services.size(); i++) {
                onServiceRemoved(services.get(i), userId);
            }
        }
        return services;
    }

    protected void onServicePackageUpdatingLocked(int userId) {
        if (this.verbose) {
            Slog.v(this.mTag, "onServicePackageUpdatingLocked(" + userId + ")");
        }
    }

    protected void onServicePackageUpdatedLocked(int userId) {
        if (this.verbose) {
            Slog.v(this.mTag, "onServicePackageUpdated(" + userId + ")");
        }
    }

    protected void onServicePackageDataClearedLocked(int userId) {
        if (this.verbose) {
            Slog.v(this.mTag, "onServicePackageDataCleared(" + userId + ")");
        }
    }

    protected void onServicePackageRestartedLocked(int userId) {
        if (this.verbose) {
            Slog.v(this.mTag, "onServicePackageRestarted(" + userId + ")");
        }
    }

    protected void onServiceRemoved(S service, int userId) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onServiceNameChanged(int userId, String serviceName, boolean isTemporary) {
        synchronized (this.mLock) {
            updateCachedServiceListLocked(userId, isDisabledLocked(userId));
        }
    }

    protected void onServiceNameListChanged(int userId, String[] serviceNames, boolean isTemporary) {
        synchronized (this.mLock) {
            updateCachedServiceListLocked(userId, isDisabledLocked(userId));
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void visitServicesLocked(Visitor<S> visitor) {
        int size = this.mServicesCacheList.size();
        for (int i = 0; i < size; i++) {
            List<S> services = this.mServicesCacheList.valueAt(i);
            for (int j = 0; j < services.size(); j++) {
                visitor.visit(services.get(j));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void clearCacheLocked() {
        this.mServicesCacheList.clear();
    }

    protected UserManagerInternal getUserManagerInternal() {
        if (this.mUm == null) {
            if (this.verbose) {
                Slog.v(this.mTag, "lazy-loading UserManagerInternal");
            }
            this.mUm = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }
        return this.mUm;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<UserInfo> getSupportedUsers() {
        UserInfo[] allUsers = getUserManagerInternal().getUserInfos();
        int size = allUsers.length;
        List<UserInfo> supportedUsers = new ArrayList<>(size);
        for (UserInfo userInfo : allUsers) {
            if (isUserSupported(new SystemService.TargetUser(userInfo))) {
                supportedUsers.add(userInfo);
            }
        }
        return supportedUsers;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void assertCalledByPackageOwner(String packageName) {
        Objects.requireNonNull(packageName);
        int uid = Binder.getCallingUid();
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        if (packages != null) {
            for (String candidate : packages) {
                if (packageName.equals(candidate)) {
                    return;
                }
            }
        }
        throw new SecurityException("UID " + uid + " does not own " + packageName);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpLocked(String prefix, PrintWriter pw) {
        boolean realDebug = this.debug;
        boolean realVerbose = this.verbose;
        try {
            this.verbose = true;
            this.debug = true;
            int size = this.mServicesCacheList.size();
            pw.print(prefix);
            pw.print("Debug: ");
            pw.print(realDebug);
            pw.print(" Verbose: ");
            pw.println(realVerbose);
            pw.print("Package policy flags: ");
            pw.println(this.mServicePackagePolicyFlags);
            if (this.mUpdatingPackageNames != null) {
                pw.print("Packages being updated: ");
                pw.println(this.mUpdatingPackageNames);
            }
            dumpSupportedUsers(pw, prefix);
            if (this.mServiceNameResolver != null) {
                pw.print(prefix);
                pw.print("Name resolver: ");
                this.mServiceNameResolver.dumpShort(pw);
                pw.println();
                List<UserInfo> users = getSupportedUsers();
                for (int i = 0; i < users.size(); i++) {
                    int userId = users.get(i).id;
                    pw.print("    ");
                    pw.print(userId);
                    pw.print(": ");
                    this.mServiceNameResolver.dumpShort(pw, userId);
                    pw.println();
                }
            }
            pw.print(prefix);
            pw.print("Users disabled by restriction: ");
            pw.println(this.mDisabledByUserRestriction);
            pw.print(prefix);
            pw.print("Allow instant service: ");
            pw.println(this.mAllowInstantService);
            String settingsProperty = getServiceSettingsProperty();
            if (settingsProperty != null) {
                pw.print(prefix);
                pw.print("Settings property: ");
                pw.println(settingsProperty);
            }
            pw.print(prefix);
            pw.print("Cached services: ");
            if (size == 0) {
                pw.println("none");
            } else {
                pw.println(size);
                for (int i2 = 0; i2 < size; i2++) {
                    pw.print(prefix);
                    pw.print("Service at ");
                    pw.print(i2);
                    pw.println(": ");
                    List<S> services = this.mServicesCacheList.valueAt(i2);
                    for (int j = 0; j < services.size(); j++) {
                        S service = services.get(j);
                        synchronized (service.mLock) {
                            service.dumpLocked("    ", pw);
                        }
                    }
                    pw.println();
                }
            }
        } finally {
            this.debug = realDebug;
            this.verbose = realVerbose;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.infra.AbstractMasterSystemService$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends PackageMonitor {
        AnonymousClass1() {
        }

        public void onPackageUpdateStarted(String packageName, int uid) {
            if (AbstractMasterSystemService.this.verbose) {
                Slog.v(AbstractMasterSystemService.this.mTag, "onPackageUpdateStarted(): " + packageName);
            }
            String activePackageName = getActiveServicePackageNameLocked();
            if (packageName.equals(activePackageName)) {
                int userId = getChangingUserId();
                synchronized (AbstractMasterSystemService.this.mLock) {
                    if (AbstractMasterSystemService.this.mUpdatingPackageNames == null) {
                        AbstractMasterSystemService.this.mUpdatingPackageNames = new SparseArray(AbstractMasterSystemService.this.mServicesCacheList.size());
                    }
                    AbstractMasterSystemService.this.mUpdatingPackageNames.put(userId, packageName);
                    AbstractMasterSystemService.this.onServicePackageUpdatingLocked(userId);
                    if ((AbstractMasterSystemService.this.mServicePackagePolicyFlags & 1) != 0) {
                        if (AbstractMasterSystemService.this.debug) {
                            Slog.d(AbstractMasterSystemService.this.mTag, "Holding service for user " + userId + " while package " + activePackageName + " is being updated");
                        }
                    } else {
                        if (AbstractMasterSystemService.this.debug) {
                            Slog.d(AbstractMasterSystemService.this.mTag, "Removing service for user " + userId + " because package " + activePackageName + " is being updated");
                        }
                        AbstractMasterSystemService.this.removeCachedServiceListLocked(userId);
                        if ((AbstractMasterSystemService.this.mServicePackagePolicyFlags & 4) != 0) {
                            if (AbstractMasterSystemService.this.debug) {
                                Slog.d(AbstractMasterSystemService.this.mTag, "Eagerly recreating service for user " + userId);
                            }
                            AbstractMasterSystemService.this.getServiceForUserLocked(userId);
                        }
                    }
                }
            }
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
            if (AbstractMasterSystemService.this.verbose) {
                Slog.v(AbstractMasterSystemService.this.mTag, "onPackageUpdateFinished(): " + packageName);
            }
            int userId = getChangingUserId();
            synchronized (AbstractMasterSystemService.this.mLock) {
                String activePackageName = AbstractMasterSystemService.this.mUpdatingPackageNames == null ? null : (String) AbstractMasterSystemService.this.mUpdatingPackageNames.get(userId);
                if (packageName.equals(activePackageName)) {
                    if (AbstractMasterSystemService.this.mUpdatingPackageNames != null) {
                        AbstractMasterSystemService.this.mUpdatingPackageNames.remove(userId);
                        if (AbstractMasterSystemService.this.mUpdatingPackageNames.size() == 0) {
                            AbstractMasterSystemService.this.mUpdatingPackageNames = null;
                        }
                    }
                    AbstractMasterSystemService.this.onServicePackageUpdatedLocked(userId);
                } else {
                    handlePackageUpdateLocked(packageName);
                }
            }
        }

        public void onPackageRemoved(String packageName, int uid) {
            ComponentName componentName;
            synchronized (AbstractMasterSystemService.this.mLock) {
                int userId = getChangingUserId();
                AbstractPerUserSystemService peekServiceForUserLocked = AbstractMasterSystemService.this.peekServiceForUserLocked(userId);
                if (peekServiceForUserLocked != null && (componentName = peekServiceForUserLocked.getServiceComponentName()) != null && packageName.equals(componentName.getPackageName())) {
                    handleActiveServiceRemoved(userId);
                }
            }
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            synchronized (AbstractMasterSystemService.this.mLock) {
                String activePackageName = getActiveServicePackageNameLocked();
                for (String pkg : packages) {
                    if (pkg.equals(activePackageName)) {
                        if (!doit) {
                            return true;
                        }
                        String action = intent.getAction();
                        int userId = getChangingUserId();
                        if ("android.intent.action.PACKAGE_RESTARTED".equals(action)) {
                            handleActiveServiceRestartedLocked(activePackageName, userId);
                        } else {
                            AbstractMasterSystemService.this.removeCachedServiceListLocked(userId);
                        }
                    } else {
                        handlePackageUpdateLocked(pkg);
                    }
                }
                return false;
            }
        }

        public void onPackageDataCleared(String packageName, int uid) {
            ComponentName componentName;
            if (AbstractMasterSystemService.this.verbose) {
                Slog.v(AbstractMasterSystemService.this.mTag, "onPackageDataCleared(): " + packageName);
            }
            int userId = getChangingUserId();
            synchronized (AbstractMasterSystemService.this.mLock) {
                AbstractPerUserSystemService peekServiceForUserLocked = AbstractMasterSystemService.this.peekServiceForUserLocked(userId);
                if (peekServiceForUserLocked != null && (componentName = peekServiceForUserLocked.getServiceComponentName()) != null && packageName.equals(componentName.getPackageName())) {
                    AbstractMasterSystemService.this.onServicePackageDataClearedLocked(userId);
                }
            }
        }

        private void handleActiveServiceRemoved(int userId) {
            synchronized (AbstractMasterSystemService.this.mLock) {
                AbstractMasterSystemService.this.removeCachedServiceListLocked(userId);
            }
            String serviceSettingsProperty = AbstractMasterSystemService.this.getServiceSettingsProperty();
            if (serviceSettingsProperty != null) {
                Settings.Secure.putStringForUser(AbstractMasterSystemService.this.getContext().getContentResolver(), serviceSettingsProperty, null, userId);
            }
        }

        private void handleActiveServiceRestartedLocked(String activePackageName, int userId) {
            if ((AbstractMasterSystemService.this.mServicePackagePolicyFlags & 16) != 0) {
                if (AbstractMasterSystemService.this.debug) {
                    Slog.d(AbstractMasterSystemService.this.mTag, "Holding service for user " + userId + " while package " + activePackageName + " is being restarted");
                }
            } else {
                if (AbstractMasterSystemService.this.debug) {
                    Slog.d(AbstractMasterSystemService.this.mTag, "Removing service for user " + userId + " because package " + activePackageName + " is being restarted");
                }
                AbstractMasterSystemService.this.removeCachedServiceListLocked(userId);
                if ((AbstractMasterSystemService.this.mServicePackagePolicyFlags & 64) != 0) {
                    if (AbstractMasterSystemService.this.debug) {
                        Slog.d(AbstractMasterSystemService.this.mTag, "Eagerly recreating service for user " + userId);
                    }
                    AbstractMasterSystemService.this.updateCachedServiceLocked(userId);
                }
            }
            AbstractMasterSystemService.this.onServicePackageRestartedLocked(userId);
        }

        public void onPackageModified(String packageName) {
            synchronized (AbstractMasterSystemService.this.mLock) {
                if (AbstractMasterSystemService.this.verbose) {
                    Slog.v(AbstractMasterSystemService.this.mTag, "onPackageModified(): " + packageName);
                }
                if (AbstractMasterSystemService.this.mServiceNameResolver == null) {
                    return;
                }
                int userId = getChangingUserId();
                String[] serviceNames = AbstractMasterSystemService.this.mServiceNameResolver.getDefaultServiceNameList(userId);
                if (serviceNames != null) {
                    for (String str : serviceNames) {
                        peekAndUpdateCachedServiceLocked(packageName, userId, str);
                    }
                }
            }
        }

        private void peekAndUpdateCachedServiceLocked(String packageName, int userId, String serviceName) {
            ComponentName serviceComponentName;
            AbstractPerUserSystemService peekServiceForUserLocked;
            if (serviceName != null && (serviceComponentName = ComponentName.unflattenFromString(serviceName)) != null && serviceComponentName.getPackageName().equals(packageName) && (peekServiceForUserLocked = AbstractMasterSystemService.this.peekServiceForUserLocked(userId)) != null) {
                ComponentName componentName = peekServiceForUserLocked.getServiceComponentName();
                if (componentName == null) {
                    if (AbstractMasterSystemService.this.verbose) {
                        Slog.v(AbstractMasterSystemService.this.mTag, "update cached");
                    }
                    AbstractMasterSystemService.this.updateCachedServiceLocked(userId);
                }
            }
        }

        private String getActiveServicePackageNameLocked() {
            ComponentName serviceComponent;
            int userId = getChangingUserId();
            AbstractPerUserSystemService peekServiceForUserLocked = AbstractMasterSystemService.this.peekServiceForUserLocked(userId);
            if (peekServiceForUserLocked == null || (serviceComponent = peekServiceForUserLocked.getServiceComponentName()) == null) {
                return null;
            }
            return serviceComponent.getPackageName();
        }

        private void handlePackageUpdateLocked(final String packageName) {
            AbstractMasterSystemService.this.visitServicesLocked(new Visitor() { // from class: com.android.server.infra.AbstractMasterSystemService$1$$ExternalSyntheticLambda0
                @Override // com.android.server.infra.AbstractMasterSystemService.Visitor
                public final void visit(Object obj) {
                    ((AbstractPerUserSystemService) obj).handlePackageUpdateLocked(packageName);
                }
            });
        }
    }

    private void startTrackingPackageChanges() {
        PackageMonitor monitor = new AnonymousClass1();
        monitor.register(getContext(), (Looper) null, UserHandle.ALL, true);
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            ContentResolver resolver = AbstractMasterSystemService.this.getContext().getContentResolver();
            String serviceProperty = AbstractMasterSystemService.this.getServiceSettingsProperty();
            if (serviceProperty != null) {
                resolver.registerContentObserver(Settings.Secure.getUriFor(serviceProperty), false, this, -1);
            }
            resolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this, -1);
            AbstractMasterSystemService.this.registerForExtraSettingsChanges(resolver, this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (AbstractMasterSystemService.this.verbose) {
                Slog.v(AbstractMasterSystemService.this.mTag, "onChange(): uri=" + uri + ", userId=" + userId);
            }
            String property = uri.getLastPathSegment();
            if (property == null) {
                return;
            }
            if (property.equals(AbstractMasterSystemService.this.getServiceSettingsProperty()) || property.equals("user_setup_complete")) {
                synchronized (AbstractMasterSystemService.this.mLock) {
                    AbstractMasterSystemService.this.updateCachedServiceLocked(userId);
                }
                return;
            }
            AbstractMasterSystemService.this.onSettingsChanged(userId, property);
        }
    }
}
