package com.android.server.notification;

import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.TypedXmlSerializer;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
/* loaded from: classes2.dex */
public class ConditionProviders extends ManagedServices {
    static final String TAG_ENABLED_DND_APPS = "dnd_apps";
    private Callback mCallback;
    private final ArrayList<ConditionRecord> mRecords;
    private final ArraySet<String> mSystemConditionProviderNames;
    private final ArraySet<SystemConditionProviderService> mSystemConditionProviders;

    /* loaded from: classes2.dex */
    public interface Callback {
        void onBootComplete();

        void onConditionChanged(Uri uri, Condition condition);

        void onServiceAdded(ComponentName componentName);

        void onUserSwitched();
    }

    public ConditionProviders(Context context, ManagedServices.UserProfiles userProfiles, IPackageManager pm) {
        super(context, new Object(), userProfiles, pm);
        this.mRecords = new ArrayList<>();
        this.mSystemConditionProviders = new ArraySet<>();
        this.mSystemConditionProviderNames = safeSet(PropConfig.getStringArray(this.mContext, "system.condition.providers", 17236137));
        this.mApprovalLevel = 0;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public boolean isSystemProviderEnabled(String path) {
        return this.mSystemConditionProviderNames.contains(path);
    }

    public void addSystemProvider(SystemConditionProviderService service) {
        this.mSystemConditionProviders.add(service);
        service.attachBase(this.mContext);
        registerSystemService(service.asInterface(), service.getComponent(), 0, 1000);
    }

    public Iterable<SystemConditionProviderService> getSystemProviders() {
        return this.mSystemConditionProviders;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.notification.ManagedServices
    public ArrayMap<Boolean, ArrayList<ComponentName>> resetComponents(String packageName, int userId) {
        resetPackage(packageName, userId);
        ArrayMap<Boolean, ArrayList<ComponentName>> changes = new ArrayMap<>();
        changes.put(true, new ArrayList<>(0));
        changes.put(false, new ArrayList<>(0));
        return changes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resetPackage(String packageName, int userId) {
        boolean isAllowed = super.isPackageOrComponentAllowed(packageName, userId);
        boolean isDefault = super.isDefaultComponentOrPackage(packageName);
        if (!isAllowed && isDefault) {
            setPackageOrComponentEnabled(packageName, userId, true, true);
        }
        if (isAllowed && !isDefault) {
            setPackageOrComponentEnabled(packageName, userId, true, false);
        }
        if (!isAllowed && isDefault) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.notification.ManagedServices
    void writeDefaults(TypedXmlSerializer out) throws IOException {
        synchronized (this.mDefaultsLock) {
            String defaults = String.join(":", this.mDefaultPackages);
            out.attribute((String) null, "defaults", defaults);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.notification.ManagedServices
    public ManagedServices.Config getConfig() {
        ManagedServices.Config c = new ManagedServices.Config();
        c.caption = "condition provider";
        c.serviceInterface = "android.service.notification.ConditionProviderService";
        c.secureSettingName = null;
        c.xmlTag = TAG_ENABLED_DND_APPS;
        c.secondarySettingName = "enabled_notification_listeners";
        c.bindPermission = "android.permission.BIND_CONDITION_PROVIDER_SERVICE";
        c.settingsAction = "android.settings.ACTION_CONDITION_PROVIDER_SETTINGS";
        c.clientLabel = 17039877;
        return c;
    }

    @Override // com.android.server.notification.ManagedServices
    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        super.dump(pw, filter);
        synchronized (this.mMutex) {
            pw.print("    mRecords(");
            pw.print(this.mRecords.size());
            pw.println("):");
            for (int i = 0; i < this.mRecords.size(); i++) {
                ConditionRecord r = this.mRecords.get(i);
                if (filter == null || filter.matches(r.component)) {
                    pw.print("      ");
                    pw.println(r);
                    String countdownDesc = CountdownConditionProvider.tryParseDescription(r.id);
                    if (countdownDesc != null) {
                        pw.print("        (");
                        pw.print(countdownDesc);
                        pw.println(")");
                    }
                }
            }
        }
        pw.print("    mSystemConditionProviders: ");
        pw.println(this.mSystemConditionProviderNames);
        for (int i2 = 0; i2 < this.mSystemConditionProviders.size(); i2++) {
            this.mSystemConditionProviders.valueAt(i2).dump(pw, filter);
        }
    }

    @Override // com.android.server.notification.ManagedServices
    protected IInterface asInterface(IBinder binder) {
        return IConditionProvider.Stub.asInterface(binder);
    }

    @Override // com.android.server.notification.ManagedServices
    protected boolean checkType(IInterface service) {
        return service instanceof IConditionProvider;
    }

    @Override // com.android.server.notification.ManagedServices
    public void onBootPhaseAppsCanStart() {
        super.onBootPhaseAppsCanStart();
        for (int i = 0; i < this.mSystemConditionProviders.size(); i++) {
            this.mSystemConditionProviders.valueAt(i).onBootComplete();
        }
        Callback callback = this.mCallback;
        if (callback != null) {
            callback.onBootComplete();
        }
    }

    @Override // com.android.server.notification.ManagedServices
    public void onUserSwitched(int user) {
        super.onUserSwitched(user);
        Callback callback = this.mCallback;
        if (callback != null) {
            callback.onUserSwitched();
        }
    }

    @Override // com.android.server.notification.ManagedServices
    protected void onServiceAdded(ManagedServices.ManagedServiceInfo info) {
        IConditionProvider provider = provider(info);
        try {
            provider.onConnected();
        } catch (RemoteException e) {
            Slog.e(this.TAG, "can't connect to service " + info, e);
        }
        Callback callback = this.mCallback;
        if (callback != null) {
            callback.onServiceAdded(info.component);
        }
    }

    @Override // com.android.server.notification.ManagedServices
    protected void ensureFilters(ServiceInfo si, int userId) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.notification.ManagedServices
    public void loadDefaultsFromConfig() {
        String defaultDndAccess = this.mContext.getResources().getString(17039927);
        if (defaultDndAccess != null) {
            String[] dnds = defaultDndAccess.split(":");
            for (int i = 0; i < dnds.length; i++) {
                if (!TextUtils.isEmpty(dnds[i])) {
                    addDefaultComponentOrPackage(dnds[i]);
                }
            }
        }
    }

    @Override // com.android.server.notification.ManagedServices
    protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo removed) {
        if (removed == null) {
            return;
        }
        for (int i = this.mRecords.size() - 1; i >= 0; i--) {
            ConditionRecord r = this.mRecords.get(i);
            if (r.component.equals(removed.component)) {
                this.mRecords.remove(i);
            }
        }
    }

    @Override // com.android.server.notification.ManagedServices
    public void onPackagesChanged(boolean removingPackage, String[] pkgList, int[] uid) {
        if (removingPackage) {
            INotificationManager inm = NotificationManager.getService();
            if (pkgList != null && pkgList.length > 0) {
                for (String pkgName : pkgList) {
                    try {
                        inm.removeAutomaticZenRules(pkgName);
                        inm.setNotificationPolicyAccessGranted(pkgName, false);
                    } catch (Exception e) {
                        Slog.e(this.TAG, "Failed to clean up rules for " + pkgName, e);
                    }
                }
            }
        }
        super.onPackagesChanged(removingPackage, pkgList, uid);
    }

    @Override // com.android.server.notification.ManagedServices
    protected boolean isValidEntry(String packageOrComponent, int userId) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.notification.ManagedServices
    public String getRequiredPermission() {
        return null;
    }

    public ManagedServices.ManagedServiceInfo checkServiceToken(IConditionProvider provider) {
        ManagedServices.ManagedServiceInfo checkServiceTokenLocked;
        synchronized (this.mMutex) {
            checkServiceTokenLocked = checkServiceTokenLocked(provider);
        }
        return checkServiceTokenLocked;
    }

    private Condition[] removeDuplicateConditions(String pkg, Condition[] conditions) {
        if (conditions == null || conditions.length == 0) {
            return null;
        }
        int N = conditions.length;
        ArrayMap<Uri, Condition> valid = new ArrayMap<>(N);
        for (int i = 0; i < N; i++) {
            Uri id = conditions[i].id;
            if (valid.containsKey(id)) {
                Slog.w(this.TAG, "Ignoring condition from " + pkg + " for duplicate id: " + id);
            } else {
                valid.put(id, conditions[i]);
            }
        }
        int i2 = valid.size();
        if (i2 == 0) {
            return null;
        }
        if (valid.size() == N) {
            return conditions;
        }
        Condition[] rt = new Condition[valid.size()];
        for (int i3 = 0; i3 < rt.length; i3++) {
            rt[i3] = valid.valueAt(i3);
        }
        return rt;
    }

    private ConditionRecord getRecordLocked(Uri id, ComponentName component, boolean create) {
        if (id == null || component == null) {
            return null;
        }
        int N = this.mRecords.size();
        for (int i = 0; i < N; i++) {
            ConditionRecord r = this.mRecords.get(i);
            if (r.id.equals(id) && r.component.equals(component)) {
                return r;
            }
        }
        if (!create) {
            return null;
        }
        ConditionRecord r2 = new ConditionRecord(id, component);
        this.mRecords.add(r2);
        return r2;
    }

    public void notifyConditions(String pkg, ManagedServices.ManagedServiceInfo info, Condition[] conditions) {
        synchronized (this.mMutex) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "notifyConditions pkg=" + pkg + " info=" + info + " conditions=" + (conditions == null ? null : Arrays.asList(conditions)));
            }
            Condition[] conditions2 = removeDuplicateConditions(pkg, conditions);
            if (conditions2 != null && conditions2.length != 0) {
                for (Condition c : conditions2) {
                    ConditionRecord r = getRecordLocked(c.id, info.component, true);
                    r.info = info;
                    r.condition = c;
                }
                for (Condition c2 : conditions2) {
                    Callback callback = this.mCallback;
                    if (callback != null) {
                        callback.onConditionChanged(c2.id, c2);
                    }
                }
            }
        }
    }

    public IConditionProvider findConditionProvider(ComponentName component) {
        if (component == null) {
            return null;
        }
        for (ManagedServices.ManagedServiceInfo service : getServices()) {
            if (component.equals(service.component)) {
                return provider(service);
            }
        }
        return null;
    }

    public Condition findCondition(ComponentName component, Uri conditionId) {
        Condition condition;
        if (component == null || conditionId == null) {
            return null;
        }
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, false);
            condition = r != null ? r.condition : null;
        }
        return condition;
    }

    public void ensureRecordExists(ComponentName component, Uri conditionId, IConditionProvider provider) {
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, true);
            if (r.info == null) {
                r.info = checkServiceTokenLocked(provider);
            }
        }
    }

    public boolean subscribeIfNecessary(ComponentName component, Uri conditionId) {
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, false);
            if (r == null) {
                Slog.w(this.TAG, "Unable to subscribe to " + component + " " + conditionId);
                return false;
            } else if (r.subscribed) {
                return true;
            } else {
                subscribeLocked(r);
                return r.subscribed;
            }
        }
    }

    public void unsubscribeIfNecessary(ComponentName component, Uri conditionId) {
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, false);
            if (r == null) {
                Slog.w(this.TAG, "Unable to unsubscribe to " + component + " " + conditionId);
            } else if (r.subscribed) {
                unsubscribeLocked(r);
            }
        }
    }

    private void subscribeLocked(ConditionRecord r) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "subscribeLocked " + r);
        }
        IConditionProvider provider = provider(r);
        RemoteException re = null;
        if (provider != null) {
            try {
                Slog.d(this.TAG, "Subscribing to " + r.id + " with " + r.component);
                provider.onSubscribe(r.id);
                r.subscribed = true;
            } catch (RemoteException e) {
                Slog.w(this.TAG, "Error subscribing to " + r, e);
                re = e;
            }
        }
        ZenLog.traceSubscribe(r != null ? r.id : null, provider, re);
    }

    @SafeVarargs
    private static <T> ArraySet<T> safeSet(T... items) {
        ArraySet<T> rt = new ArraySet<>();
        if (items == null || items.length == 0) {
            return rt;
        }
        for (T item : items) {
            if (item != null) {
                rt.add(item);
            }
        }
        return rt;
    }

    private void unsubscribeLocked(ConditionRecord r) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "unsubscribeLocked " + r);
        }
        IConditionProvider provider = provider(r);
        RemoteException re = null;
        if (provider != null) {
            try {
                provider.onUnsubscribe(r.id);
            } catch (RemoteException e) {
                Slog.w(this.TAG, "Error unsubscribing to " + r, e);
                re = e;
            }
            r.subscribed = false;
        }
        ZenLog.traceUnsubscribe(r != null ? r.id : null, provider, re);
    }

    private static IConditionProvider provider(ConditionRecord r) {
        if (r == null) {
            return null;
        }
        return provider(r.info);
    }

    private static IConditionProvider provider(ManagedServices.ManagedServiceInfo info) {
        if (info == null) {
            return null;
        }
        return info.service;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ConditionRecord {
        public final ComponentName component;
        public Condition condition;
        public final Uri id;
        public ManagedServices.ManagedServiceInfo info;
        public boolean subscribed;

        private ConditionRecord(Uri id, ComponentName component) {
            this.id = id;
            this.component = component;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ConditionRecord[id=").append(this.id).append(",component=").append(this.component).append(",subscribed=").append(this.subscribed);
            return sb.append(']').toString();
        }
    }
}
