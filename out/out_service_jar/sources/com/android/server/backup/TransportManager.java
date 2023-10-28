package com.android.server.backup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.backup.transport.BackupTransportClient;
import com.android.server.backup.transport.OnTransportRegisteredListener;
import com.android.server.backup.transport.TransportConnection;
import com.android.server.backup.transport.TransportConnectionManager;
import com.android.server.backup.transport.TransportNotAvailableException;
import com.android.server.backup.transport.TransportNotRegisteredException;
import com.android.server.backup.transport.TransportStats;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class TransportManager {
    private static final boolean MORE_DEBUG = false;
    public static final String SERVICE_ACTION_TRANSPORT_HOST = "android.backup.TRANSPORT_HOST";
    private static final String TAG = "BackupTransportManager";
    private volatile String mCurrentTransportName;
    private OnTransportRegisteredListener mOnTransportRegisteredListener;
    private final PackageManager mPackageManager;
    private final Map<ComponentName, TransportDescription> mRegisteredTransportsDescriptionMap;
    private final TransportConnectionManager mTransportConnectionManager;
    private final Object mTransportLock;
    private final Intent mTransportServiceIntent;
    private final TransportStats mTransportStats;
    private final Set<ComponentName> mTransportWhitelist;
    private final int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$new$0(String c, String n) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TransportManager(int userId, Context context, Set<ComponentName> whitelist, String selectedTransport) {
        this.mTransportServiceIntent = new Intent(SERVICE_ACTION_TRANSPORT_HOST);
        this.mOnTransportRegisteredListener = new OnTransportRegisteredListener() { // from class: com.android.server.backup.TransportManager$$ExternalSyntheticLambda1
            @Override // com.android.server.backup.transport.OnTransportRegisteredListener
            public final void onTransportRegistered(String str, String str2) {
                TransportManager.lambda$new$0(str, str2);
            }
        };
        this.mTransportLock = new Object();
        this.mRegisteredTransportsDescriptionMap = new ArrayMap();
        this.mUserId = userId;
        this.mPackageManager = context.getPackageManager();
        this.mTransportWhitelist = (Set) Preconditions.checkNotNull(whitelist);
        this.mCurrentTransportName = selectedTransport;
        TransportStats transportStats = new TransportStats();
        this.mTransportStats = transportStats;
        this.mTransportConnectionManager = new TransportConnectionManager(userId, context, transportStats);
    }

    TransportManager(int userId, Context context, Set<ComponentName> whitelist, String selectedTransport, TransportConnectionManager transportConnectionManager) {
        this.mTransportServiceIntent = new Intent(SERVICE_ACTION_TRANSPORT_HOST);
        this.mOnTransportRegisteredListener = new OnTransportRegisteredListener() { // from class: com.android.server.backup.TransportManager$$ExternalSyntheticLambda1
            @Override // com.android.server.backup.transport.OnTransportRegisteredListener
            public final void onTransportRegistered(String str, String str2) {
                TransportManager.lambda$new$0(str, str2);
            }
        };
        this.mTransportLock = new Object();
        this.mRegisteredTransportsDescriptionMap = new ArrayMap();
        this.mUserId = userId;
        this.mPackageManager = context.getPackageManager();
        this.mTransportWhitelist = (Set) Preconditions.checkNotNull(whitelist);
        this.mCurrentTransportName = selectedTransport;
        this.mTransportStats = new TransportStats();
        this.mTransportConnectionManager = transportConnectionManager;
    }

    public void setOnTransportRegisteredListener(OnTransportRegisteredListener listener) {
        this.mOnTransportRegisteredListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onPackageAdded$1(ComponentName transportComponent) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageAdded(String packageName) {
        registerTransportsFromPackage(packageName, new Predicate() { // from class: com.android.server.backup.TransportManager$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TransportManager.lambda$onPackageAdded$1((ComponentName) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageRemoved(String packageName) {
        synchronized (this.mTransportLock) {
            this.mRegisteredTransportsDescriptionMap.keySet().removeIf(fromPackageFilter(packageName));
        }
    }

    void onPackageEnabled(String packageName) {
        onPackageAdded(packageName);
    }

    void onPackageDisabled(String packageName) {
        onPackageRemoved(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageChanged(String packageName, String... components) {
        if (components.length == 1 && components[0].equals(packageName)) {
            try {
                int enabled = this.mPackageManager.getApplicationEnabledSetting(packageName);
                switch (enabled) {
                    case 1:
                        onPackageEnabled(packageName);
                        return;
                    case 2:
                        onPackageDisabled(packageName);
                        return;
                    default:
                        Slog.w(TAG, "Package " + packageName + " enabled setting: " + enabled);
                        return;
                }
            } catch (IllegalArgumentException e) {
                return;
            }
        }
        final Set<ComponentName> transportComponents = new ArraySet<>(components.length);
        for (String componentName : components) {
            transportComponents.add(new ComponentName(packageName, componentName));
        }
        if (transportComponents.isEmpty()) {
            return;
        }
        synchronized (this.mTransportLock) {
            Set<ComponentName> keySet = this.mRegisteredTransportsDescriptionMap.keySet();
            Objects.requireNonNull(transportComponents);
            keySet.removeIf(new Predicate() { // from class: com.android.server.backup.TransportManager$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return transportComponents.contains((ComponentName) obj);
                }
            });
        }
        Objects.requireNonNull(transportComponents);
        registerTransportsFromPackage(packageName, new Predicate() { // from class: com.android.server.backup.TransportManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return transportComponents.contains((ComponentName) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName[] getRegisteredTransportComponents() {
        ComponentName[] componentNameArr;
        synchronized (this.mTransportLock) {
            componentNameArr = (ComponentName[]) this.mRegisteredTransportsDescriptionMap.keySet().toArray(new ComponentName[this.mRegisteredTransportsDescriptionMap.size()]);
        }
        return componentNameArr;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getRegisteredTransportNames() {
        String[] transportNames;
        synchronized (this.mTransportLock) {
            transportNames = new String[this.mRegisteredTransportsDescriptionMap.size()];
            int i = 0;
            for (TransportDescription description : this.mRegisteredTransportsDescriptionMap.values()) {
                transportNames[i] = description.name;
                i++;
            }
        }
        return transportNames;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<ComponentName> getTransportWhitelist() {
        return this.mTransportWhitelist;
    }

    public String getCurrentTransportName() {
        return this.mCurrentTransportName;
    }

    public ComponentName getCurrentTransportComponent() throws TransportNotRegisteredException {
        synchronized (this.mTransportLock) {
            if (this.mCurrentTransportName == null) {
                return null;
            }
            return getRegisteredTransportComponentOrThrowLocked(this.mCurrentTransportName);
        }
    }

    public String getTransportName(ComponentName transportComponent) throws TransportNotRegisteredException {
        String str;
        synchronized (this.mTransportLock) {
            str = getRegisteredTransportDescriptionOrThrowLocked(transportComponent).name;
        }
        return str;
    }

    public String getTransportDirName(ComponentName transportComponent) throws TransportNotRegisteredException {
        String str;
        synchronized (this.mTransportLock) {
            str = getRegisteredTransportDescriptionOrThrowLocked(transportComponent).transportDirName;
        }
        return str;
    }

    public String getTransportDirName(String transportName) throws TransportNotRegisteredException {
        String str;
        synchronized (this.mTransportLock) {
            str = getRegisteredTransportDescriptionOrThrowLocked(transportName).transportDirName;
        }
        return str;
    }

    public Intent getTransportConfigurationIntent(String transportName) throws TransportNotRegisteredException {
        Intent intent;
        synchronized (this.mTransportLock) {
            intent = getRegisteredTransportDescriptionOrThrowLocked(transportName).configurationIntent;
        }
        return intent;
    }

    public String getTransportCurrentDestinationString(String transportName) throws TransportNotRegisteredException {
        String str;
        synchronized (this.mTransportLock) {
            str = getRegisteredTransportDescriptionOrThrowLocked(transportName).currentDestinationString;
        }
        return str;
    }

    public Intent getTransportDataManagementIntent(String transportName) throws TransportNotRegisteredException {
        Intent intent;
        synchronized (this.mTransportLock) {
            intent = getRegisteredTransportDescriptionOrThrowLocked(transportName).dataManagementIntent;
        }
        return intent;
    }

    public CharSequence getTransportDataManagementLabel(String transportName) throws TransportNotRegisteredException {
        CharSequence charSequence;
        synchronized (this.mTransportLock) {
            charSequence = getRegisteredTransportDescriptionOrThrowLocked(transportName).dataManagementLabel;
        }
        return charSequence;
    }

    public boolean isTransportRegistered(String transportName) {
        boolean z;
        synchronized (this.mTransportLock) {
            z = getRegisteredTransportEntryLocked(transportName) != null;
        }
        return z;
    }

    public void forEachRegisteredTransport(Consumer<String> transportConsumer) {
        synchronized (this.mTransportLock) {
            for (TransportDescription transportDescription : this.mRegisteredTransportsDescriptionMap.values()) {
                transportConsumer.accept(transportDescription.name);
            }
        }
    }

    public void updateTransportAttributes(ComponentName transportComponent, String name, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, CharSequence dataManagementLabel) {
        synchronized (this.mTransportLock) {
            TransportDescription description = this.mRegisteredTransportsDescriptionMap.get(transportComponent);
            if (description == null) {
                Slog.e(TAG, "Transport " + name + " not registered tried to change description");
                return;
            }
            description.name = name;
            description.configurationIntent = configurationIntent;
            description.currentDestinationString = currentDestinationString;
            description.dataManagementIntent = dataManagementIntent;
            description.dataManagementLabel = dataManagementLabel;
            Slog.d(TAG, "Transport " + name + " updated its attributes");
        }
    }

    private ComponentName getRegisteredTransportComponentOrThrowLocked(String transportName) throws TransportNotRegisteredException {
        ComponentName transportComponent = getRegisteredTransportComponentLocked(transportName);
        if (transportComponent == null) {
            throw new TransportNotRegisteredException(transportName);
        }
        return transportComponent;
    }

    private TransportDescription getRegisteredTransportDescriptionOrThrowLocked(ComponentName transportComponent) throws TransportNotRegisteredException {
        TransportDescription description = this.mRegisteredTransportsDescriptionMap.get(transportComponent);
        if (description == null) {
            throw new TransportNotRegisteredException(transportComponent);
        }
        return description;
    }

    private TransportDescription getRegisteredTransportDescriptionOrThrowLocked(String transportName) throws TransportNotRegisteredException {
        TransportDescription description = getRegisteredTransportDescriptionLocked(transportName);
        if (description == null) {
            throw new TransportNotRegisteredException(transportName);
        }
        return description;
    }

    private ComponentName getRegisteredTransportComponentLocked(String transportName) {
        Map.Entry<ComponentName, TransportDescription> entry = getRegisteredTransportEntryLocked(transportName);
        if (entry == null) {
            return null;
        }
        return entry.getKey();
    }

    private TransportDescription getRegisteredTransportDescriptionLocked(String transportName) {
        Map.Entry<ComponentName, TransportDescription> entry = getRegisteredTransportEntryLocked(transportName);
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }

    private Map.Entry<ComponentName, TransportDescription> getRegisteredTransportEntryLocked(String transportName) {
        for (Map.Entry<ComponentName, TransportDescription> entry : this.mRegisteredTransportsDescriptionMap.entrySet()) {
            TransportDescription description = entry.getValue();
            if (transportName.equals(description.name)) {
                return entry;
            }
        }
        return null;
    }

    public TransportConnection getTransportClient(String transportName, String caller) {
        try {
            return getTransportClientOrThrow(transportName, caller);
        } catch (TransportNotRegisteredException e) {
            Slog.w(TAG, "Transport " + transportName + " not registered");
            return null;
        }
    }

    public TransportConnection getTransportClientOrThrow(String transportName, String caller) throws TransportNotRegisteredException {
        TransportConnection transportClient;
        synchronized (this.mTransportLock) {
            ComponentName component = getRegisteredTransportComponentLocked(transportName);
            if (component == null) {
                throw new TransportNotRegisteredException(transportName);
            }
            transportClient = this.mTransportConnectionManager.getTransportClient(component, caller);
        }
        return transportClient;
    }

    public TransportConnection getCurrentTransportClient(String caller) {
        TransportConnection transportClient;
        if (this.mCurrentTransportName == null) {
            throw new IllegalStateException("No transport selected");
        }
        synchronized (this.mTransportLock) {
            transportClient = getTransportClient(this.mCurrentTransportName, caller);
        }
        return transportClient;
    }

    public TransportConnection getCurrentTransportClientOrThrow(String caller) throws TransportNotRegisteredException {
        TransportConnection transportClientOrThrow;
        if (this.mCurrentTransportName == null) {
            throw new IllegalStateException("No transport selected");
        }
        synchronized (this.mTransportLock) {
            transportClientOrThrow = getTransportClientOrThrow(this.mCurrentTransportName, caller);
        }
        return transportClientOrThrow;
    }

    public void disposeOfTransportClient(TransportConnection transportConnection, String caller) {
        this.mTransportConnectionManager.disposeOfTransportClient(transportConnection, caller);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public String selectTransport(String transportName) {
        String prevTransport;
        synchronized (this.mTransportLock) {
            prevTransport = this.mCurrentTransportName;
            this.mCurrentTransportName = transportName;
        }
        return prevTransport;
    }

    public int registerAndSelectTransport(ComponentName transportComponent) {
        synchronized (this.mTransportLock) {
            try {
                try {
                    selectTransport(getTransportName(transportComponent));
                } catch (TransportNotRegisteredException e) {
                    int result = registerTransport(transportComponent);
                    if (result != 0) {
                        return result;
                    }
                    synchronized (this.mTransportLock) {
                        try {
                            try {
                                selectTransport(getTransportName(transportComponent));
                                return 0;
                            } finally {
                            }
                        } catch (TransportNotRegisteredException e2) {
                            Slog.wtf(TAG, "Transport got unregistered");
                            return -1;
                        }
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerTransports$2(ComponentName transportComponent) {
        return true;
    }

    public void registerTransports() {
        registerTransportsForIntent(this.mTransportServiceIntent, new Predicate() { // from class: com.android.server.backup.TransportManager$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TransportManager.lambda$registerTransports$2((ComponentName) obj);
            }
        });
    }

    private void registerTransportsFromPackage(String packageName, Predicate<ComponentName> transportComponentFilter) {
        try {
            this.mPackageManager.getPackageInfoAsUser(packageName, 0, this.mUserId);
            registerTransportsForIntent(new Intent(this.mTransportServiceIntent).setPackage(packageName), transportComponentFilter.and(fromPackageFilter(packageName)));
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Trying to register transports from package not found " + packageName);
        }
    }

    private void registerTransportsForIntent(Intent intent, Predicate<ComponentName> transportComponentFilter) {
        List<ResolveInfo> hosts = this.mPackageManager.queryIntentServicesAsUser(intent, 0, this.mUserId);
        if (hosts == null) {
            return;
        }
        for (ResolveInfo host : hosts) {
            ComponentName transportComponent = host.serviceInfo.getComponentName();
            if (transportComponentFilter.test(transportComponent) && isTransportTrusted(transportComponent)) {
                registerTransport(transportComponent);
            }
        }
    }

    private boolean isTransportTrusted(ComponentName transport) {
        if (!this.mTransportWhitelist.contains(transport)) {
            Slog.w(TAG, "BackupTransport " + transport.flattenToShortString() + " not whitelisted.");
            return false;
        }
        try {
            PackageInfo packInfo = this.mPackageManager.getPackageInfoAsUser(transport.getPackageName(), 0, this.mUserId);
            if ((packInfo.applicationInfo.privateFlags & 8) == 0) {
                Slog.w(TAG, "Transport package " + transport.getPackageName() + " not privileged");
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Package not found.", e);
            return false;
        }
    }

    private int registerTransport(ComponentName transportComponent) {
        int result;
        checkCanUseTransport();
        if (!isTransportTrusted(transportComponent)) {
            return -2;
        }
        String transportString = transportComponent.flattenToShortString();
        Bundle extras = new Bundle();
        extras.putBoolean("android.app.backup.extra.TRANSPORT_REGISTRATION", true);
        TransportConnection transportConnection = this.mTransportConnectionManager.getTransportClient(transportComponent, extras, "TransportManager.registerTransport()");
        try {
            BackupTransportClient transport = transportConnection.connectOrThrow("TransportManager.registerTransport()");
            try {
                String transportName = transport.name();
                String transportDirName = transport.transportDirName();
                registerTransport(transportComponent, transport);
                Slog.d(TAG, "Transport " + transportString + " registered");
                this.mOnTransportRegisteredListener.onTransportRegistered(transportName, transportDirName);
                result = 0;
            } catch (RemoteException e) {
                Slog.e(TAG, "Transport " + transportString + " died while registering");
                result = -1;
            }
            this.mTransportConnectionManager.disposeOfTransportClient(transportConnection, "TransportManager.registerTransport()");
            return result;
        } catch (TransportNotAvailableException e2) {
            Slog.e(TAG, "Couldn't connect to transport " + transportString + " for registration");
            this.mTransportConnectionManager.disposeOfTransportClient(transportConnection, "TransportManager.registerTransport()");
            return -1;
        }
    }

    private void registerTransport(ComponentName transportComponent, BackupTransportClient transport) throws RemoteException {
        checkCanUseTransport();
        TransportDescription description = new TransportDescription(transport.name(), transport.transportDirName(), transport.configurationIntent(), transport.currentDestinationString(), transport.dataManagementIntent(), transport.dataManagementIntentLabel());
        synchronized (this.mTransportLock) {
            this.mRegisteredTransportsDescriptionMap.put(transportComponent, description);
        }
    }

    private void checkCanUseTransport() {
        Preconditions.checkState(!Thread.holdsLock(this.mTransportLock), "Can't call transport with transport lock held");
    }

    public void dumpTransportClients(PrintWriter pw) {
        this.mTransportConnectionManager.dump(pw);
    }

    public void dumpTransportStats(PrintWriter pw) {
        this.mTransportStats.dump(pw);
    }

    private static Predicate<ComponentName> fromPackageFilter(final String packageName) {
        return new Predicate() { // from class: com.android.server.backup.TransportManager$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean equals;
                equals = packageName.equals(((ComponentName) obj).getPackageName());
                return equals;
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class TransportDescription {
        private Intent configurationIntent;
        private String currentDestinationString;
        private Intent dataManagementIntent;
        private CharSequence dataManagementLabel;
        private String name;
        private final String transportDirName;

        private TransportDescription(String name, String transportDirName, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, CharSequence dataManagementLabel) {
            this.name = name;
            this.transportDirName = transportDirName;
            this.configurationIntent = configurationIntent;
            this.currentDestinationString = currentDestinationString;
            this.dataManagementIntent = dataManagementIntent;
            this.dataManagementLabel = dataManagementLabel;
        }
    }
}
