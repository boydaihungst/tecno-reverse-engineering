package com.android.server.autofill;

import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.content.AutofillOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.service.autofill.FillEventHistory;
import android.service.autofill.UserData;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillManagerInternal;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManager;
import android.view.autofill.IAutoFillManagerClient;
import com.android.internal.infra.GlobalWhitelistState;
import com.android.internal.infra.WhitelistHelper;
import com.android.internal.os.IResultReceiver;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.SyncResultReceiver;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.autofill.ui.AutoFillUI;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
import com.android.server.infra.SecureSettingsServiceNameResolver;
import com.android.server.infra.ServiceNameResolver;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes.dex */
public final class AutofillManagerService extends AbstractMasterSystemService<AutofillManagerService, AutofillManagerServiceImpl> {
    private static final char COMPAT_PACKAGE_DELIMITER = ':';
    private static final char COMPAT_PACKAGE_URL_IDS_BLOCK_BEGIN = '[';
    private static final char COMPAT_PACKAGE_URL_IDS_BLOCK_END = ']';
    private static final char COMPAT_PACKAGE_URL_IDS_DELIMITER = ',';
    private static final int DEFAULT_AUGMENTED_AUTOFILL_REQUEST_TIMEOUT_MILLIS = 5000;
    static final String RECEIVER_BUNDLE_EXTRA_SESSIONS = "sessions";
    private static final String TAG = "AutofillManagerService";
    private static final Object sLock = AutofillManagerService.class;
    private static int sPartitionMaxCount = 10;
    private static int sVisibleDatasetsMaxCount = 0;
    private final ActivityManagerInternal mAm;
    final FrameworkResourcesServiceNameResolver mAugmentedAutofillResolver;
    final AugmentedAutofillState mAugmentedAutofillState;
    int mAugmentedServiceIdleUnbindTimeoutMs;
    int mAugmentedServiceRequestTimeoutMs;
    private final AutofillCompatState mAutofillCompatState;
    private final BroadcastReceiver mBroadcastReceiver;
    private final DisabledInfoCache mDisabledInfoCache;
    private final LocalService mLocalService;
    private final LocalLog mRequestsHistory;
    private int mSupportedSmartSuggestionModes;
    private final AutoFillUI mUi;
    private final LocalLog mUiLatencyHistory;
    private final LocalLog mWtfHistory;

    /* renamed from: com.android.server.autofill.AutofillManagerService$1  reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                if (Helper.sDebug) {
                    Slog.d(AutofillManagerService.TAG, "Close system dialogs");
                }
                synchronized (AutofillManagerService.this.mLock) {
                    AutofillManagerService.this.visitServicesLocked(new AbstractMasterSystemService.Visitor() { // from class: com.android.server.autofill.AutofillManagerService$1$$ExternalSyntheticLambda0
                        @Override // com.android.server.infra.AbstractMasterSystemService.Visitor
                        public final void visit(Object obj) {
                            ((AutofillManagerServiceImpl) obj).forceRemoveFinishedSessionsLocked();
                        }
                    });
                }
                AutofillManagerService.this.mUi.hideAll(null);
            }
        }
    }

    public AutofillManagerService(Context context) {
        super(context, new SecureSettingsServiceNameResolver(context, "autofill_service"), "no_autofill", 4);
        this.mRequestsHistory = new LocalLog(20);
        this.mUiLatencyHistory = new LocalLog(20);
        this.mWtfHistory = new LocalLog(50);
        this.mAutofillCompatState = new AutofillCompatState();
        this.mDisabledInfoCache = new DisabledInfoCache();
        this.mLocalService = new LocalService();
        AnonymousClass1 anonymousClass1 = new AnonymousClass1();
        this.mBroadcastReceiver = anonymousClass1;
        this.mAugmentedAutofillState = new AugmentedAutofillState();
        this.mUi = new AutoFillUI(ActivityThread.currentActivityThread().getSystemUiContext());
        this.mAm = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        DeviceConfig.addOnPropertiesChangedListener("autofill", ActivityThread.currentApplication().getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.autofill.AutofillManagerService$$ExternalSyntheticLambda2
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                AutofillManagerService.this.m1997lambda$new$0$comandroidserverautofillAutofillManagerService(properties);
            }
        });
        setLogLevelFromSettings();
        setMaxPartitionsFromSettings();
        setMaxVisibleDatasetsFromSettings();
        setDeviceConfigProperties();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        context.registerReceiver(anonymousClass1, filter, null, FgThread.getHandler(), 2);
        FrameworkResourcesServiceNameResolver frameworkResourcesServiceNameResolver = new FrameworkResourcesServiceNameResolver(getContext(), 17039921);
        this.mAugmentedAutofillResolver = frameworkResourcesServiceNameResolver;
        frameworkResourcesServiceNameResolver.setOnTemporaryServiceNameChangedCallback(new ServiceNameResolver.NameResolverListener() { // from class: com.android.server.autofill.AutofillManagerService$$ExternalSyntheticLambda3
            @Override // com.android.server.infra.ServiceNameResolver.NameResolverListener
            public final void onNameResolved(int i, String str, boolean z) {
                AutofillManagerService.this.m1998lambda$new$1$comandroidserverautofillAutofillManagerService(i, str, z);
            }
        });
        if (this.mSupportedSmartSuggestionModes != 0) {
            List<UserInfo> users = getSupportedUsers();
            for (int i = 0; i < users.size(); i++) {
                int userId = users.get(i).id;
                getServiceForUserLocked(userId);
                this.mAugmentedAutofillState.setServiceInfo(userId, this.mAugmentedAutofillResolver.getServiceName(userId), this.mAugmentedAutofillResolver.isTemporary(userId));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-autofill-AutofillManagerService  reason: not valid java name */
    public /* synthetic */ void m1997lambda$new$0$comandroidserverautofillAutofillManagerService(DeviceConfig.Properties properties) {
        onDeviceConfigChange(properties.getKeyset());
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected String getServiceSettingsProperty() {
        return "autofill_service";
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void registerForExtraSettingsChanges(ContentResolver resolver, ContentObserver observer) {
        resolver.registerContentObserver(Settings.Global.getUriFor("autofill_logging_level"), false, observer, -1);
        resolver.registerContentObserver(Settings.Global.getUriFor("autofill_max_partitions_size"), false, observer, -1);
        resolver.registerContentObserver(Settings.Global.getUriFor("autofill_max_visible_datasets"), false, observer, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("selected_input_method_subtype"), false, observer, -1);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onSettingsChanged(int userId, String property) {
        char c;
        switch (property.hashCode()) {
            case -1848997872:
                if (property.equals("autofill_max_visible_datasets")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1299292969:
                if (property.equals("autofill_logging_level")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1048937777:
                if (property.equals("autofill_max_partitions_size")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1194058837:
                if (property.equals("selected_input_method_subtype")) {
                    c = 3;
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
                setLogLevelFromSettings();
                return;
            case 1:
                setMaxPartitionsFromSettings();
                return;
            case 2:
                setMaxVisibleDatasetsFromSettings();
                return;
            case 3:
                handleInputMethodSwitch(userId);
                return;
            default:
                Slog.w(TAG, "Unexpected property (" + property + "); updating cache instead");
                synchronized (this.mLock) {
                    updateCachedServiceLocked(userId);
                }
                return;
        }
    }

    private void handleInputMethodSwitch(int userId) {
        synchronized (this.mLock) {
            AutofillManagerServiceImpl service = peekServiceForUserLocked(userId);
            if (service != null) {
                service.onSwitchInputMethod();
            }
        }
    }

    private void onDeviceConfigChange(Set<String> keys) {
        for (String key : keys) {
            char c = 65535;
            switch (key.hashCode()) {
                case -1546842390:
                    if (key.equals("augmented_service_idle_unbind_timeout")) {
                        c = 1;
                        break;
                    }
                    break;
                case -987506216:
                    if (key.equals("augmented_service_request_timeout")) {
                        c = 2;
                        break;
                    }
                    break;
                case 1168452547:
                    if (key.equals("compat_mode_allowed_packages")) {
                        c = 3;
                        break;
                    }
                    break;
                case 1709136986:
                    if (key.equals("smart_suggestion_supported_modes")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                case 2:
                    setDeviceConfigProperties();
                    break;
                case 3:
                    updateCachedServices();
                    break;
                default:
                    Slog.i(this.mTag, "Ignoring change on " + key);
                    break;
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onAugmentedServiceNameChanged */
    public void m1998lambda$new$1$comandroidserverautofillAutofillManagerService(int userId, String serviceName, boolean isTemporary) {
        this.mAugmentedAutofillState.setServiceInfo(userId, serviceName, isTemporary);
        synchronized (this.mLock) {
            AutofillManagerServiceImpl service = peekServiceForUserLocked(userId);
            if (service == null) {
                getServiceForUserLocked(userId);
            } else {
                service.updateRemoteAugmentedAutofillService();
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public AutofillManagerServiceImpl newServiceLocked(int resolvedUserId, boolean disabled) {
        return new AutofillManagerServiceImpl(this, this.mLock, this.mUiLatencyHistory, this.mWtfHistory, resolvedUserId, this.mUi, this.mAutofillCompatState, disabled, this.mDisabledInfoCache);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public void onServiceRemoved(AutofillManagerServiceImpl service, int userId) {
        service.destroyLocked();
        this.mDisabledInfoCache.remove(userId);
        this.mAutofillCompatState.removeCompatibilityModeRequests(userId);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public void onServiceEnabledLocked(AutofillManagerServiceImpl service, int userId) {
        addCompatibilityModeRequestsLocked(service, userId);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("autofill", new AutoFillManagerServiceStub());
        publishLocalService(AutofillManagerInternal.class, this.mLocalService);
    }

    @Override // com.android.server.SystemService
    public boolean isUserSupported(SystemService.TargetUser user) {
        return user.isFull() || user.isManagedProfile();
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        if (Helper.sDebug) {
            Slog.d(TAG, "Hiding UI when user switched");
        }
        this.mUi.hideAll(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSupportedSmartSuggestionModesLocked() {
        return this.mSupportedSmartSuggestionModes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logRequestLocked(String historyItem) {
        this.mRequestsHistory.log(historyItem);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInstantServiceAllowed() {
        return this.mAllowInstantService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllSessions(int userId, IResultReceiver receiver) {
        Slog.i(TAG, "removeAllSessions() for userId " + userId);
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            if (userId != -1) {
                AutofillManagerServiceImpl service = peekServiceForUserLocked(userId);
                if (service != null) {
                    service.forceRemoveAllSessionsLocked();
                }
            } else {
                visitServicesLocked(new AbstractMasterSystemService.Visitor() { // from class: com.android.server.autofill.AutofillManagerService$$ExternalSyntheticLambda0
                    @Override // com.android.server.infra.AbstractMasterSystemService.Visitor
                    public final void visit(Object obj) {
                        ((AutofillManagerServiceImpl) obj).forceRemoveAllSessionsLocked();
                    }
                });
            }
        }
        try {
            receiver.send(0, new Bundle());
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void listSessions(int userId, IResultReceiver receiver) {
        Slog.i(TAG, "listSessions() for userId " + userId);
        enforceCallingPermissionForManagement();
        Bundle resultData = new Bundle();
        final ArrayList<String> sessions = new ArrayList<>();
        synchronized (this.mLock) {
            if (userId != -1) {
                AutofillManagerServiceImpl service = peekServiceForUserLocked(userId);
                if (service != null) {
                    service.listSessionsLocked(sessions);
                }
            } else {
                visitServicesLocked(new AbstractMasterSystemService.Visitor() { // from class: com.android.server.autofill.AutofillManagerService$$ExternalSyntheticLambda1
                    @Override // com.android.server.infra.AbstractMasterSystemService.Visitor
                    public final void visit(Object obj) {
                        ((AutofillManagerServiceImpl) obj).listSessionsLocked(sessions);
                    }
                });
            }
        }
        resultData.putStringArrayList(RECEIVER_BUNDLE_EXTRA_SESSIONS, sessions);
        try {
            receiver.send(0, resultData);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        Slog.i(TAG, "reset()");
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            visitServicesLocked(new AbstractMasterSystemService.Visitor() { // from class: com.android.server.autofill.AutofillManagerService$$ExternalSyntheticLambda4
                @Override // com.android.server.infra.AbstractMasterSystemService.Visitor
                public final void visit(Object obj) {
                    ((AutofillManagerServiceImpl) obj).destroyLocked();
                }
            });
            clearCacheLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLogLevel(int level) {
        Slog.i(TAG, "setLogLevel(): " + level);
        enforceCallingPermissionForManagement();
        long token = Binder.clearCallingIdentity();
        try {
            Settings.Global.putInt(getContext().getContentResolver(), "autofill_logging_level", level);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void setLogLevelFromSettings() {
        int level = Settings.Global.getInt(getContext().getContentResolver(), "autofill_logging_level", AutofillManager.DEFAULT_LOGGING_LEVEL);
        boolean debug = false;
        boolean verbose = false;
        if (level != 0) {
            if (level == 4) {
                verbose = true;
                debug = true;
            } else if (level == 2) {
                debug = true;
            } else {
                Slog.w(TAG, "setLogLevelFromSettings(): invalid level: " + level);
            }
        }
        if (debug || Helper.sDebug) {
            Slog.d(TAG, "setLogLevelFromSettings(): level=" + level + ", debug=" + debug + ", verbose=" + verbose);
        }
        synchronized (this.mLock) {
            setLoggingLevelsLocked(debug, verbose);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLogLevel() {
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            if (Helper.sVerbose) {
                return 4;
            }
            return Helper.sDebug ? 2 : 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMaxPartitions() {
        int i;
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            i = sPartitionMaxCount;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMaxPartitions(int max) {
        Slog.i(TAG, "setMaxPartitions(): " + max);
        enforceCallingPermissionForManagement();
        long token = Binder.clearCallingIdentity();
        try {
            Settings.Global.putInt(getContext().getContentResolver(), "autofill_max_partitions_size", max);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void setMaxPartitionsFromSettings() {
        int max = Settings.Global.getInt(getContext().getContentResolver(), "autofill_max_partitions_size", 10);
        if (Helper.sDebug) {
            Slog.d(TAG, "setMaxPartitionsFromSettings(): " + max);
        }
        synchronized (sLock) {
            sPartitionMaxCount = max;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMaxVisibleDatasets() {
        int i;
        enforceCallingPermissionForManagement();
        synchronized (sLock) {
            i = sVisibleDatasetsMaxCount;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMaxVisibleDatasets(int max) {
        Slog.i(TAG, "setMaxVisibleDatasets(): " + max);
        enforceCallingPermissionForManagement();
        long token = Binder.clearCallingIdentity();
        try {
            Settings.Global.putInt(getContext().getContentResolver(), "autofill_max_visible_datasets", max);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void setMaxVisibleDatasetsFromSettings() {
        int max = Settings.Global.getInt(getContext().getContentResolver(), "autofill_max_visible_datasets", 0);
        if (Helper.sDebug) {
            Slog.d(TAG, "setMaxVisibleDatasetsFromSettings(): " + max);
        }
        synchronized (sLock) {
            sVisibleDatasetsMaxCount = max;
        }
    }

    private void setDeviceConfigProperties() {
        synchronized (this.mLock) {
            this.mAugmentedServiceIdleUnbindTimeoutMs = DeviceConfig.getInt("autofill", "augmented_service_idle_unbind_timeout", 0);
            this.mAugmentedServiceRequestTimeoutMs = DeviceConfig.getInt("autofill", "augmented_service_request_timeout", 5000);
            this.mSupportedSmartSuggestionModes = DeviceConfig.getInt("autofill", "smart_suggestion_supported_modes", 1);
            if (this.verbose) {
                Slog.v(this.mTag, "setDeviceConfigProperties(): augmentedIdleTimeout=" + this.mAugmentedServiceIdleUnbindTimeoutMs + ", augmentedRequestTimeout=" + this.mAugmentedServiceRequestTimeoutMs + ", smartSuggestionMode=" + AutofillManager.getSmartSuggestionModeToString(this.mSupportedSmartSuggestionModes));
            }
        }
    }

    private void updateCachedServices() {
        List<UserInfo> supportedUsers = getSupportedUsers();
        for (UserInfo userInfo : supportedUsers) {
            synchronized (this.mLock) {
                updateCachedServiceLocked(userInfo.id);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void calculateScore(String algorithmName, String value1, String value2, RemoteCallback callback) {
        enforceCallingPermissionForManagement();
        FieldClassificationStrategy strategy = new FieldClassificationStrategy(getContext(), -2);
        strategy.calculateScores(callback, Arrays.asList(AutofillValue.forText(value1)), new String[]{value2}, new String[]{null}, algorithmName, null, null, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Boolean getFullScreenMode() {
        enforceCallingPermissionForManagement();
        return Helper.sFullScreenMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFullScreenMode(Boolean mode) {
        enforceCallingPermissionForManagement();
        Helper.sFullScreenMode = mode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTemporaryAugmentedAutofillService(int userId, String serviceName, int durationMs) {
        Slog.i(this.mTag, "setTemporaryAugmentedAutofillService(" + userId + ") to " + serviceName + " for " + durationMs + "ms");
        enforceCallingPermissionForManagement();
        Objects.requireNonNull(serviceName);
        if (durationMs > 120000) {
            throw new IllegalArgumentException("Max duration is 120000 (called with " + durationMs + ")");
        }
        this.mAugmentedAutofillResolver.setTemporaryService(userId, serviceName, durationMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetTemporaryAugmentedAutofillService(int userId) {
        enforceCallingPermissionForManagement();
        this.mAugmentedAutofillResolver.resetTemporaryService(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDefaultAugmentedServiceEnabled(int userId) {
        enforceCallingPermissionForManagement();
        return this.mAugmentedAutofillResolver.isDefaultServiceEnabled(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setDefaultAugmentedServiceEnabled(int userId, boolean enabled) {
        Slog.i(this.mTag, "setDefaultAugmentedServiceEnabled() for userId " + userId + ": " + enabled);
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            AutofillManagerServiceImpl service = getServiceForUserLocked(userId);
            if (service != null) {
                boolean changed = this.mAugmentedAutofillResolver.setDefaultServiceEnabled(userId, enabled);
                if (changed) {
                    service.updateRemoteAugmentedAutofillService();
                    return true;
                } else if (this.debug) {
                    Slog.d(TAG, "setDefaultAugmentedServiceEnabled(): already " + enabled);
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean requestSavedPasswordCount(int userId, IResultReceiver receiver) {
        enforceCallingPermissionForManagement();
        synchronized (this.mLock) {
            AutofillManagerServiceImpl service = peekServiceForUserLocked(userId);
            if (service != null) {
                service.requestSavedPasswordCount(receiver);
                return true;
            }
            if (Helper.sVerbose) {
                Slog.v(TAG, "requestSavedPasswordCount(): no service for " + userId);
            }
            return false;
        }
    }

    private void setLoggingLevelsLocked(boolean debug, boolean verbose) {
        Helper.sDebug = debug;
        android.view.autofill.Helper.sDebug = debug;
        this.debug = debug;
        Helper.sVerbose = verbose;
        android.view.autofill.Helper.sVerbose = verbose;
        this.verbose = verbose;
    }

    private void addCompatibilityModeRequestsLocked(AutofillManagerServiceImpl service, int userId) {
        this.mAutofillCompatState.reset(userId);
        ArrayMap<String, Long> compatPackages = service.getCompatibilityPackagesLocked();
        if (compatPackages == null || compatPackages.isEmpty()) {
            return;
        }
        Map<String, String[]> allowedPackages = getAllowedCompatModePackages();
        int compatPackageCount = compatPackages.size();
        for (int i = 0; i < compatPackageCount; i++) {
            String packageName = compatPackages.keyAt(i);
            if (allowedPackages == null || !allowedPackages.containsKey(packageName)) {
                Slog.w(TAG, "Ignoring not allowed compat package " + packageName);
            } else {
                Long maxVersionCode = compatPackages.valueAt(i);
                if (maxVersionCode != null) {
                    this.mAutofillCompatState.addCompatibilityModeRequest(packageName, maxVersionCode.longValue(), allowedPackages.get(packageName), userId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getAllowedCompatModePackagesFromDeviceConfig() {
        String config = DeviceConfig.getString("autofill", "compat_mode_allowed_packages", (String) null);
        if (!TextUtils.isEmpty(config)) {
            return config;
        }
        return getAllowedCompatModePackagesFromSettings();
    }

    private String getAllowedCompatModePackagesFromSettings() {
        return Settings.Global.getString(getContext().getContentResolver(), "autofill_compat_mode_allowed_packages");
    }

    private Map<String, String[]> getAllowedCompatModePackages() {
        return getAllowedCompatModePackages(getAllowedCompatModePackagesFromDeviceConfig());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void send(IResultReceiver receiver, int value) {
        try {
            receiver.send(value, (Bundle) null);
        } catch (RemoteException e) {
            Slog.w(TAG, "Error async reporting result to client: " + e);
        }
    }

    private void send(IResultReceiver receiver, Bundle value) {
        try {
            receiver.send(0, value);
        } catch (RemoteException e) {
            Slog.w(TAG, "Error async reporting result to client: " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void send(IResultReceiver receiver, String value) {
        send(receiver, SyncResultReceiver.bundleFor(value));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void send(IResultReceiver receiver, String[] value) {
        send(receiver, SyncResultReceiver.bundleFor(value));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void send(IResultReceiver receiver, Parcelable value) {
        send(receiver, SyncResultReceiver.bundleFor(value));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void send(IResultReceiver receiver, boolean value) {
        send(receiver, value ? 1 : 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void send(IResultReceiver receiver, int value1, int value2) {
        try {
            receiver.send(value1, SyncResultReceiver.bundleFor(value2));
        } catch (RemoteException e) {
            Slog.w(TAG, "Error async reporting result to client: " + e);
        }
    }

    static Map<String, String[]> getAllowedCompatModePackages(String setting) {
        String packageName;
        List<String> urlBarIds;
        if (TextUtils.isEmpty(setting)) {
            return null;
        }
        ArrayMap<String, String[]> compatPackages = new ArrayMap<>();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(COMPAT_PACKAGE_DELIMITER);
        splitter.setString(setting);
        while (splitter.hasNext()) {
            String packageBlock = splitter.next();
            int urlBlockIndex = packageBlock.indexOf(91);
            if (urlBlockIndex == -1) {
                packageName = packageBlock;
                urlBarIds = null;
            } else if (packageBlock.charAt(packageBlock.length() - 1) != ']') {
                Slog.w(TAG, "Ignoring entry '" + packageBlock + "' on '" + setting + "'because it does not end on '" + COMPAT_PACKAGE_URL_IDS_BLOCK_END + "'");
            } else {
                packageName = packageBlock.substring(0, urlBlockIndex);
                List<String> urlBarIds2 = new ArrayList<>();
                String urlBarIdsBlock = packageBlock.substring(urlBlockIndex + 1, packageBlock.length() - 1);
                if (Helper.sVerbose) {
                    Slog.v(TAG, "pkg:" + packageName + ": block:" + packageBlock + ": urls:" + urlBarIds2 + ": block:" + urlBarIdsBlock + ":");
                }
                TextUtils.SimpleStringSplitter splitter2 = new TextUtils.SimpleStringSplitter(COMPAT_PACKAGE_URL_IDS_DELIMITER);
                splitter2.setString(urlBarIdsBlock);
                while (splitter2.hasNext()) {
                    String urlBarId = splitter2.next();
                    urlBarIds2.add(urlBarId);
                }
                urlBarIds = urlBarIds2;
            }
            if (urlBarIds == null) {
                compatPackages.put(packageName, null);
            } else {
                String[] urlBarIdsArray = new String[urlBarIds.size()];
                urlBarIds.toArray(urlBarIdsArray);
                compatPackages.put(packageName, urlBarIdsArray);
            }
        }
        return compatPackages;
    }

    public static int getPartitionMaxCount() {
        int i;
        synchronized (sLock) {
            i = sPartitionMaxCount;
        }
        return i;
    }

    public static int getVisibleDatasetsMaxCount() {
        int i;
        synchronized (sLock) {
            i = sVisibleDatasetsMaxCount;
        }
        return i;
    }

    /* loaded from: classes.dex */
    private final class LocalService extends AutofillManagerInternal {
        private LocalService() {
        }

        public void onBackKeyPressed() {
            if (Helper.sDebug) {
                Slog.d(AutofillManagerService.TAG, "onBackKeyPressed()");
            }
            AutofillManagerService.this.mUi.hideAll(null);
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.getServiceForUserLocked(UserHandle.getCallingUserId());
                service.onBackKeyPressed();
            }
        }

        public AutofillOptions getAutofillOptions(String packageName, long versionCode, int userId) {
            int loggingLevel;
            if (AutofillManagerService.this.verbose) {
                loggingLevel = 6;
            } else if (AutofillManagerService.this.debug) {
                loggingLevel = 2;
            } else {
                loggingLevel = 0;
            }
            boolean compatModeEnabled = AutofillManagerService.this.mAutofillCompatState.isCompatibilityModeRequested(packageName, versionCode, userId);
            AutofillOptions options = new AutofillOptions(loggingLevel, compatModeEnabled);
            AutofillManagerService.this.mAugmentedAutofillState.injectAugmentedAutofillInfo(options, userId, packageName);
            injectDisableAppInfo(options, userId, packageName);
            return options;
        }

        public boolean isAugmentedAutofillServiceForUser(int callingUid, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    return service.isAugmentedAutofillServiceForUserLocked(callingUid);
                }
                return false;
            }
        }

        private void injectDisableAppInfo(AutofillOptions options, int userId, String packageName) {
            options.appDisabledExpiration = AutofillManagerService.this.mDisabledInfoCache.getAppDisabledExpiration(userId, packageName);
            options.disabledActivities = AutofillManagerService.this.mDisabledInfoCache.getAppDisabledActivities(userId, packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class PackageCompatState {
        private final long maxVersionCode;
        private final String[] urlBarResourceIds;

        PackageCompatState(long maxVersionCode, String[] urlBarResourceIds) {
            this.maxVersionCode = maxVersionCode;
            this.urlBarResourceIds = urlBarResourceIds;
        }

        public String toString() {
            return "maxVersionCode=" + this.maxVersionCode + ", urlBarResourceIds=" + Arrays.toString(this.urlBarResourceIds);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class DisabledInfoCache {
        private final Object mLock = new Object();
        private final SparseArray<AutofillDisabledInfo> mCache = new SparseArray<>();

        DisabledInfoCache() {
        }

        void remove(int userId) {
            synchronized (this.mLock) {
                this.mCache.remove(userId);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void addDisabledAppLocked(int userId, String packageName, long expiration) {
            Objects.requireNonNull(packageName);
            synchronized (this.mLock) {
                AutofillDisabledInfo info = getOrCreateAutofillDisabledInfoByUserIdLocked(userId);
                info.putDisableAppsLocked(packageName, expiration);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void addDisabledActivityLocked(int userId, ComponentName componentName, long expiration) {
            Objects.requireNonNull(componentName);
            synchronized (this.mLock) {
                AutofillDisabledInfo info = getOrCreateAutofillDisabledInfoByUserIdLocked(userId);
                info.putDisableActivityLocked(componentName, expiration);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isAutofillDisabledLocked(int userId, ComponentName componentName) {
            boolean disabled;
            Objects.requireNonNull(componentName);
            synchronized (this.mLock) {
                AutofillDisabledInfo info = this.mCache.get(userId);
                disabled = info != null ? info.isAutofillDisabledLocked(componentName) : false;
            }
            return disabled;
        }

        long getAppDisabledExpiration(int userId, String packageName) {
            Long expiration;
            Objects.requireNonNull(packageName);
            synchronized (this.mLock) {
                AutofillDisabledInfo info = this.mCache.get(userId);
                expiration = Long.valueOf(info != null ? info.getAppDisabledExpirationLocked(packageName) : 0L);
            }
            return expiration.longValue();
        }

        ArrayMap<String, Long> getAppDisabledActivities(int userId, String packageName) {
            ArrayMap<String, Long> disabledList;
            Objects.requireNonNull(packageName);
            synchronized (this.mLock) {
                AutofillDisabledInfo info = this.mCache.get(userId);
                disabledList = info != null ? info.getAppDisabledActivitiesLocked(packageName) : null;
            }
            return disabledList;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void dump(int userId, String prefix, PrintWriter pw) {
            synchronized (this.mLock) {
                AutofillDisabledInfo info = this.mCache.get(userId);
                if (info != null) {
                    info.dumpLocked(prefix, pw);
                }
            }
        }

        private AutofillDisabledInfo getOrCreateAutofillDisabledInfoByUserIdLocked(int userId) {
            AutofillDisabledInfo info = this.mCache.get(userId);
            if (info == null) {
                AutofillDisabledInfo info2 = new AutofillDisabledInfo();
                this.mCache.put(userId, info2);
                return info2;
            }
            return info;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AutofillDisabledInfo {
        private ArrayMap<ComponentName, Long> mDisabledActivities;
        private ArrayMap<String, Long> mDisabledApps;

        private AutofillDisabledInfo() {
        }

        void putDisableAppsLocked(String packageName, long expiration) {
            if (this.mDisabledApps == null) {
                this.mDisabledApps = new ArrayMap<>(1);
            }
            this.mDisabledApps.put(packageName, Long.valueOf(expiration));
        }

        void putDisableActivityLocked(ComponentName componentName, long expiration) {
            if (this.mDisabledActivities == null) {
                this.mDisabledActivities = new ArrayMap<>(1);
            }
            this.mDisabledActivities.put(componentName, Long.valueOf(expiration));
        }

        long getAppDisabledExpirationLocked(String packageName) {
            Long expiration;
            ArrayMap<String, Long> arrayMap = this.mDisabledApps;
            if (arrayMap == null || (expiration = arrayMap.get(packageName)) == null) {
                return 0L;
            }
            return expiration.longValue();
        }

        ArrayMap<String, Long> getAppDisabledActivitiesLocked(String packageName) {
            ArrayMap<ComponentName, Long> arrayMap = this.mDisabledActivities;
            if (arrayMap != null) {
                int size = arrayMap.size();
                ArrayMap<String, Long> disabledList = null;
                for (int i = 0; i < size; i++) {
                    ComponentName component = this.mDisabledActivities.keyAt(i);
                    if (packageName.equals(component.getPackageName())) {
                        if (disabledList == null) {
                            disabledList = new ArrayMap<>();
                        }
                        long expiration = this.mDisabledActivities.valueAt(i).longValue();
                        disabledList.put(component.flattenToShortString(), Long.valueOf(expiration));
                    }
                }
                return disabledList;
            }
            return null;
        }

        boolean isAutofillDisabledLocked(ComponentName componentName) {
            Long expiration;
            long elapsedTime = 0;
            if (this.mDisabledActivities != null) {
                elapsedTime = SystemClock.elapsedRealtime();
                Long expiration2 = this.mDisabledActivities.get(componentName);
                if (expiration2 != null) {
                    if (expiration2.longValue() >= elapsedTime) {
                        return true;
                    }
                    if (Helper.sVerbose) {
                        Slog.v(AutofillManagerService.TAG, "Removing " + componentName.toShortString() + " from disabled list");
                    }
                    this.mDisabledActivities.remove(componentName);
                }
            }
            String packageName = componentName.getPackageName();
            ArrayMap<String, Long> arrayMap = this.mDisabledApps;
            if (arrayMap == null || (expiration = arrayMap.get(packageName)) == null) {
                return false;
            }
            if (elapsedTime == 0) {
                elapsedTime = SystemClock.elapsedRealtime();
            }
            if (expiration.longValue() >= elapsedTime) {
                return true;
            }
            if (Helper.sVerbose) {
                Slog.v(AutofillManagerService.TAG, "Removing " + packageName + " from disabled list");
            }
            this.mDisabledApps.remove(packageName);
            return false;
        }

        void dumpLocked(String prefix, PrintWriter pw) {
            String str;
            AutofillDisabledInfo autofillDisabledInfo = this;
            String str2 = prefix;
            pw.print(str2);
            pw.print("Disabled apps: ");
            ArrayMap<String, Long> arrayMap = autofillDisabledInfo.mDisabledApps;
            String str3 = ": ";
            if (arrayMap == null) {
                pw.println("N/A");
                str = ": ";
            } else {
                int size = arrayMap.size();
                pw.println(size);
                StringBuilder builder = new StringBuilder();
                long now = SystemClock.elapsedRealtime();
                int i = 0;
                while (i < size) {
                    String packageName = autofillDisabledInfo.mDisabledApps.keyAt(i);
                    long expiration = autofillDisabledInfo.mDisabledApps.valueAt(i).longValue();
                    builder.append(str2).append(str2).append(i).append(". ").append(packageName).append(str3);
                    TimeUtils.formatDuration(expiration - now, builder);
                    builder.append('\n');
                    i++;
                    str3 = str3;
                }
                str = str3;
                pw.println(builder);
            }
            pw.print(str2);
            pw.print("Disabled activities: ");
            ArrayMap<ComponentName, Long> arrayMap2 = autofillDisabledInfo.mDisabledActivities;
            if (arrayMap2 == null) {
                pw.println("N/A");
                return;
            }
            int size2 = arrayMap2.size();
            pw.println(size2);
            StringBuilder builder2 = new StringBuilder();
            long now2 = SystemClock.elapsedRealtime();
            int i2 = 0;
            while (i2 < size2) {
                ComponentName component = autofillDisabledInfo.mDisabledActivities.keyAt(i2);
                long expiration2 = autofillDisabledInfo.mDisabledActivities.valueAt(i2).longValue();
                builder2.append(str2).append(str2).append(i2).append(". ").append(component).append(str);
                TimeUtils.formatDuration(expiration2 - now2, builder2);
                builder2.append('\n');
                i2++;
                autofillDisabledInfo = this;
                str2 = prefix;
            }
            pw.println(builder2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AutofillCompatState {
        private final Object mLock = new Object();
        private SparseArray<ArrayMap<String, PackageCompatState>> mUserSpecs;

        AutofillCompatState() {
        }

        boolean isCompatibilityModeRequested(String packageName, long versionCode, int userId) {
            synchronized (this.mLock) {
                SparseArray<ArrayMap<String, PackageCompatState>> sparseArray = this.mUserSpecs;
                if (sparseArray == null) {
                    return false;
                }
                ArrayMap<String, PackageCompatState> userSpec = sparseArray.get(userId);
                if (userSpec == null) {
                    return false;
                }
                PackageCompatState metadata = userSpec.get(packageName);
                if (metadata == null) {
                    return false;
                }
                return versionCode <= metadata.maxVersionCode;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public String[] getUrlBarResourceIds(String packageName, int userId) {
            synchronized (this.mLock) {
                SparseArray<ArrayMap<String, PackageCompatState>> sparseArray = this.mUserSpecs;
                if (sparseArray == null) {
                    return null;
                }
                ArrayMap<String, PackageCompatState> userSpec = sparseArray.get(userId);
                if (userSpec == null) {
                    return null;
                }
                PackageCompatState metadata = userSpec.get(packageName);
                if (metadata == null) {
                    return null;
                }
                return metadata.urlBarResourceIds;
            }
        }

        void addCompatibilityModeRequest(String packageName, long versionCode, String[] urlBarResourceIds, int userId) {
            synchronized (this.mLock) {
                if (this.mUserSpecs == null) {
                    this.mUserSpecs = new SparseArray<>();
                }
                ArrayMap<String, PackageCompatState> userSpec = this.mUserSpecs.get(userId);
                if (userSpec == null) {
                    userSpec = new ArrayMap<>();
                    this.mUserSpecs.put(userId, userSpec);
                }
                userSpec.put(packageName, new PackageCompatState(versionCode, urlBarResourceIds));
            }
        }

        void removeCompatibilityModeRequests(int userId) {
            synchronized (this.mLock) {
                SparseArray<ArrayMap<String, PackageCompatState>> sparseArray = this.mUserSpecs;
                if (sparseArray != null) {
                    sparseArray.remove(userId);
                    if (this.mUserSpecs.size() <= 0) {
                        this.mUserSpecs = null;
                    }
                }
            }
        }

        void reset(int userId) {
            synchronized (this.mLock) {
                SparseArray<ArrayMap<String, PackageCompatState>> sparseArray = this.mUserSpecs;
                if (sparseArray != null) {
                    sparseArray.delete(userId);
                    int newSize = this.mUserSpecs.size();
                    if (newSize == 0) {
                        if (Helper.sVerbose) {
                            Slog.v(AutofillManagerService.TAG, "reseting mUserSpecs");
                        }
                        this.mUserSpecs = null;
                    } else if (Helper.sVerbose) {
                        Slog.v(AutofillManagerService.TAG, "mUserSpecs down to " + newSize);
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(String prefix, PrintWriter pw) {
            synchronized (this.mLock) {
                if (this.mUserSpecs == null) {
                    pw.println("N/A");
                    return;
                }
                pw.println();
                String prefix2 = prefix + "  ";
                for (int i = 0; i < this.mUserSpecs.size(); i++) {
                    int user = this.mUserSpecs.keyAt(i);
                    pw.print(prefix);
                    pw.print("User: ");
                    pw.println(user);
                    ArrayMap<String, PackageCompatState> perUser = this.mUserSpecs.valueAt(i);
                    for (int j = 0; j < perUser.size(); j++) {
                        String packageName = perUser.keyAt(j);
                        PackageCompatState state = perUser.valueAt(j);
                        pw.print(prefix2);
                        pw.print(packageName);
                        pw.print(": ");
                        pw.println(state);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AugmentedAutofillState extends GlobalWhitelistState {
        private final SparseArray<String> mServicePackages = new SparseArray<>();
        private final SparseBooleanArray mTemporaryServices = new SparseBooleanArray();

        AugmentedAutofillState() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setServiceInfo(int userId, String serviceName, boolean isTemporary) {
            synchronized (this.mGlobalWhitelistStateLock) {
                if (isTemporary) {
                    this.mTemporaryServices.put(userId, true);
                } else {
                    this.mTemporaryServices.delete(userId);
                }
                if (serviceName != null) {
                    ComponentName componentName = ComponentName.unflattenFromString(serviceName);
                    if (componentName == null) {
                        Slog.w(AutofillManagerService.TAG, "setServiceInfo(): invalid name: " + serviceName);
                        this.mServicePackages.remove(userId);
                    } else {
                        this.mServicePackages.put(userId, componentName.getPackageName());
                    }
                } else {
                    this.mServicePackages.remove(userId);
                }
            }
        }

        public void injectAugmentedAutofillInfo(AutofillOptions options, int userId, String packageName) {
            synchronized (this.mGlobalWhitelistStateLock) {
                if (this.mWhitelisterHelpers == null) {
                    return;
                }
                WhitelistHelper helper = (WhitelistHelper) this.mWhitelisterHelpers.get(userId);
                if (helper != null) {
                    options.augmentedAutofillEnabled = helper.isWhitelisted(packageName);
                    options.whitelistedActivitiesForAugmentedAutofill = helper.getWhitelistedComponents(packageName);
                }
            }
        }

        public boolean isWhitelisted(int userId, ComponentName componentName) {
            synchronized (this.mGlobalWhitelistStateLock) {
                if (super.isWhitelisted(userId, componentName)) {
                    if (Build.IS_USER && this.mTemporaryServices.get(userId)) {
                        String packageName = componentName.getPackageName();
                        if (!packageName.equals(this.mServicePackages.get(userId))) {
                            Slog.w(AutofillManagerService.TAG, "Ignoring package " + packageName + " for augmented autofill while using temporary service " + this.mServicePackages.get(userId));
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }

        public void dump(String prefix, PrintWriter pw) {
            super.dump(prefix, pw);
            synchronized (this.mGlobalWhitelistStateLock) {
                if (this.mServicePackages.size() > 0) {
                    pw.print(prefix);
                    pw.print("Service packages: ");
                    pw.println(this.mServicePackages);
                }
                if (this.mTemporaryServices.size() > 0) {
                    pw.print(prefix);
                    pw.print("Temp services: ");
                    pw.println(this.mTemporaryServices);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    final class AutoFillManagerServiceStub extends IAutoFillManager.Stub {
        AutoFillManagerServiceStub() {
        }

        public void addClient(IAutoFillManagerClient client, ComponentName componentName, int userId, IResultReceiver receiver) {
            int flags = 0;
            synchronized (AutofillManagerService.this.mLock) {
                int enabledFlags = ((AutofillManagerServiceImpl) AutofillManagerService.this.getServiceForUserLocked(userId)).addClientLocked(client, componentName);
                if (enabledFlags != 0) {
                    flags = 0 | enabledFlags;
                }
                if (Helper.sDebug) {
                    flags |= 2;
                }
                if (Helper.sVerbose) {
                    flags |= 4;
                }
            }
            AutofillManagerService.this.send(receiver, flags);
        }

        public void removeClient(IAutoFillManagerClient client, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.removeClientLocked(client);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "removeClient(): no service for " + userId);
                }
            }
        }

        public void setAuthenticationResult(Bundle data, int sessionId, int authenticationId, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.getServiceForUserLocked(userId);
                service.setAuthenticationResultLocked(data, sessionId, authenticationId, getCallingUid());
            }
        }

        public void setHasCallback(int sessionId, int userId, boolean hasIt) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.getServiceForUserLocked(userId);
                service.setHasCallback(sessionId, getCallingUid(), hasIt);
            }
        }

        public void startSession(IBinder activityToken, IBinder clientCallback, AutofillId autofillId, Rect bounds, AutofillValue value, int userId, boolean hasCallback, int flags, ComponentName clientActivity, boolean compatMode, IResultReceiver receiver) {
            Objects.requireNonNull(activityToken, "activityToken");
            Objects.requireNonNull(clientCallback, "clientCallback");
            Objects.requireNonNull(autofillId, "autofillId");
            Objects.requireNonNull(clientActivity, "clientActivity");
            String packageName = (String) Objects.requireNonNull(clientActivity.getPackageName());
            Preconditions.checkArgument(userId == UserHandle.getUserId(getCallingUid()), "userId");
            try {
                AutofillManagerService.this.getContext().getPackageManager().getPackageInfoAsUser(packageName, 0, userId);
                int taskId = AutofillManagerService.this.mAm.getTaskIdForActivity(activityToken, false);
                synchronized (AutofillManagerService.this.mLock) {
                    try {
                        try {
                            AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.getServiceForUserLocked(userId);
                            long result = service.startSessionLocked(activityToken, taskId, getCallingUid(), clientCallback, autofillId, bounds, value, hasCallback, clientActivity, compatMode, AutofillManagerService.this.mAllowInstantService, flags);
                            int sessionId = (int) result;
                            int resultFlags = (int) (result >> 32);
                            if (resultFlags != 0) {
                                AutofillManagerService.this.send(receiver, sessionId, resultFlags);
                            } else {
                                AutofillManagerService.this.send(receiver, sessionId);
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException(packageName + " is not a valid package", e);
            }
        }

        public void getFillEventHistory(IResultReceiver receiver) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            FillEventHistory fillEventHistory = null;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    fillEventHistory = service.getFillEventHistory(getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getFillEventHistory(): no service for " + userId);
                }
            }
            AutofillManagerService.this.send(receiver, fillEventHistory);
        }

        public void getUserData(IResultReceiver receiver) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            UserData userData = null;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    userData = service.getUserData(getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getUserData(): no service for " + userId);
                }
            }
            AutofillManagerService.this.send(receiver, userData);
        }

        public void getUserDataId(IResultReceiver receiver) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            UserData userData = null;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    userData = service.getUserData(getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getUserDataId(): no service for " + userId);
                }
            }
            String userDataId = userData == null ? null : userData.getId();
            AutofillManagerService.this.send(receiver, userDataId);
        }

        public void setUserData(UserData userData) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.setUserData(getCallingUid(), userData);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "setUserData(): no service for " + userId);
                }
            }
        }

        public void isFieldClassificationEnabled(IResultReceiver receiver) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            boolean enabled = false;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    enabled = service.isFieldClassificationEnabled(getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "isFieldClassificationEnabled(): no service for " + userId);
                }
            }
            AutofillManagerService.this.send(receiver, enabled);
        }

        public void getDefaultFieldClassificationAlgorithm(IResultReceiver receiver) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            String algorithm = null;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    algorithm = service.getDefaultFieldClassificationAlgorithm(getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getDefaultFcAlgorithm(): no service for " + userId);
                }
            }
            AutofillManagerService.this.send(receiver, algorithm);
        }

        public void setAugmentedAutofillWhitelist(List<String> packages, List<ComponentName> activities, IResultReceiver receiver) throws RemoteException {
            boolean ok;
            int userId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    ok = service.setAugmentedAutofillWhitelistLocked(packages, activities, getCallingUid());
                } else {
                    boolean ok2 = Helper.sVerbose;
                    if (ok2) {
                        Slog.v(AutofillManagerService.TAG, "setAugmentedAutofillWhitelist(): no service for " + userId);
                    }
                    ok = false;
                }
            }
            AutofillManagerService.this.send(receiver, ok ? 0 : -1);
        }

        public void getAvailableFieldClassificationAlgorithms(IResultReceiver receiver) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            String[] algorithms = null;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    algorithms = service.getAvailableFieldClassificationAlgorithms(getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getAvailableFcAlgorithms(): no service for " + userId);
                }
            }
            AutofillManagerService.this.send(receiver, algorithms);
        }

        public void getAutofillServiceComponentName(IResultReceiver receiver) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            ComponentName componentName = null;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    componentName = service.getServiceComponentName();
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getAutofillServiceComponentName(): no service for " + userId);
                }
            }
            AutofillManagerService.this.send(receiver, componentName);
        }

        public void restoreSession(int sessionId, IBinder activityToken, IBinder appCallback, IResultReceiver receiver) throws RemoteException {
            int userId = UserHandle.getCallingUserId();
            Objects.requireNonNull(activityToken, "activityToken");
            Objects.requireNonNull(appCallback, "appCallback");
            boolean restored = false;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    restored = service.restoreSession(sessionId, getCallingUid(), activityToken, appCallback);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "restoreSession(): no service for " + userId);
                }
            }
            AutofillManagerService.this.send(receiver, restored);
        }

        public void updateSession(int sessionId, AutofillId autoFillId, Rect bounds, AutofillValue value, int action, int flags, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.updateSessionLocked(sessionId, getCallingUid(), autoFillId, bounds, value, action, flags);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "updateSession(): no service for " + userId);
                }
            }
        }

        public void setAutofillFailure(int sessionId, List<AutofillId> ids, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.setAutofillFailureLocked(sessionId, getCallingUid(), ids);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "setAutofillFailure(): no service for " + userId);
                }
            }
        }

        public void finishSession(int sessionId, int userId, int commitReason) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.finishSessionLocked(sessionId, getCallingUid(), commitReason);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "finishSession(): no service for " + userId);
                }
            }
        }

        public void cancelSession(int sessionId, int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.cancelSessionLocked(sessionId, getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "cancelSession(): no service for " + userId);
                }
            }
        }

        public void disableOwnedAutofillServices(int userId) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(userId);
                if (service != null) {
                    service.disableOwnedAutofillServicesLocked(Binder.getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "cancelSession(): no service for " + userId);
                }
            }
        }

        public void isServiceSupported(int userId, IResultReceiver receiver) {
            boolean supported;
            synchronized (AutofillManagerService.this.mLock) {
                supported = !AutofillManagerService.this.isDisabledLocked(userId);
            }
            AutofillManagerService.this.send(receiver, supported);
        }

        public void isServiceEnabled(int userId, String packageName, IResultReceiver receiver) {
            boolean enabled;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.getServiceForUserLocked(userId);
                enabled = Objects.equals(packageName, service.getServicePackageName());
            }
            AutofillManagerService.this.send(receiver, enabled);
        }

        public void onPendingSaveUi(int operation, IBinder token) {
            Objects.requireNonNull(token, "token");
            Preconditions.checkArgument(operation == 1 || operation == 2, "invalid operation: %d", new Object[]{Integer.valueOf(operation)});
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl service = (AutofillManagerServiceImpl) AutofillManagerService.this.peekServiceForUserLocked(UserHandle.getCallingUserId());
                if (service != null) {
                    service.onPendingSaveUi(operation, token);
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(AutofillManagerService.this.getContext(), AutofillManagerService.TAG, pw)) {
                boolean showHistory = true;
                boolean uiOnly = false;
                if (args != null) {
                    for (String arg : args) {
                        char c = 65535;
                        switch (arg.hashCode()) {
                            case 900765093:
                                if (arg.equals("--ui-only")) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case 1098711592:
                                if (arg.equals("--no-history")) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case 1333069025:
                                if (arg.equals("--help")) {
                                    c = 2;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                showHistory = false;
                                break;
                            case 1:
                                uiOnly = true;
                                break;
                            case 2:
                                pw.println("Usage: dumpsys autofill [--ui-only|--no-history]");
                                return;
                            default:
                                Slog.w(AutofillManagerService.TAG, "Ignoring invalid dump arg: " + arg);
                                break;
                        }
                    }
                }
                if (uiOnly) {
                    AutofillManagerService.this.mUi.dump(pw);
                    return;
                }
                boolean realDebug = Helper.sDebug;
                boolean realVerbose = Helper.sVerbose;
                try {
                    Helper.sVerbose = true;
                    Helper.sDebug = true;
                    synchronized (AutofillManagerService.this.mLock) {
                        pw.print("sDebug: ");
                        pw.print(realDebug);
                        pw.print(" sVerbose: ");
                        pw.println(realVerbose);
                        AutofillManagerService.this.dumpLocked("", pw);
                        AutofillManagerService.this.mAugmentedAutofillResolver.dumpShort(pw);
                        pw.println();
                        pw.print("Max partitions per session: ");
                        pw.println(AutofillManagerService.sPartitionMaxCount);
                        pw.print("Max visible datasets: ");
                        pw.println(AutofillManagerService.sVisibleDatasetsMaxCount);
                        if (Helper.sFullScreenMode != null) {
                            pw.print("Overridden full-screen mode: ");
                            pw.println(Helper.sFullScreenMode);
                        }
                        pw.println("User data constraints: ");
                        UserData.dumpConstraints("  ", pw);
                        AutofillManagerService.this.mUi.dump(pw);
                        pw.print("Autofill Compat State: ");
                        AutofillManagerService.this.mAutofillCompatState.dump("  ", pw);
                        pw.print("from device config: ");
                        pw.println(AutofillManagerService.this.getAllowedCompatModePackagesFromDeviceConfig());
                        if (AutofillManagerService.this.mSupportedSmartSuggestionModes != 0) {
                            pw.print("Smart Suggestion modes: ");
                            pw.println(AutofillManager.getSmartSuggestionModeToString(AutofillManagerService.this.mSupportedSmartSuggestionModes));
                        }
                        pw.print("Augmented Service Idle Unbind Timeout: ");
                        pw.println(AutofillManagerService.this.mAugmentedServiceIdleUnbindTimeoutMs);
                        pw.print("Augmented Service Request Timeout: ");
                        pw.println(AutofillManagerService.this.mAugmentedServiceRequestTimeoutMs);
                        if (showHistory) {
                            pw.println();
                            pw.println("Requests history:");
                            pw.println();
                            AutofillManagerService.this.mRequestsHistory.reverseDump(fd, pw, args);
                            pw.println();
                            pw.println("UI latency history:");
                            pw.println();
                            AutofillManagerService.this.mUiLatencyHistory.reverseDump(fd, pw, args);
                            pw.println();
                            pw.println("WTF history:");
                            pw.println();
                            AutofillManagerService.this.mWtfHistory.reverseDump(fd, pw, args);
                        }
                        pw.println("Augmented Autofill State: ");
                        AutofillManagerService.this.mAugmentedAutofillState.dump("  ", pw);
                    }
                } finally {
                    Helper.sDebug = realDebug;
                    Helper.sVerbose = realVerbose;
                }
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.autofill.AutofillManagerService$AutoFillManagerServiceStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new AutofillManagerServiceShellCommand(AutofillManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }
}
