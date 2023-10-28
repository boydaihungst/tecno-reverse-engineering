package com.android.server.companion;

import android.app.ActivityManagerInternal;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.DeviceNotAssociatedException;
import android.companion.IAssociationRequestCallback;
import android.companion.ICompanionDeviceManager;
import android.companion.IOnAssociationsChangedListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.net.MacAddress;
import android.net.NetworkPolicyManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerWhitelistManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.ExceptionUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.app.IAppOpsService;
import com.android.internal.content.PackageMonitor;
import com.android.internal.notification.NotificationAccessConfirmationActivityContract;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.companion.AssociationStore;
import com.android.server.companion.CompanionApplicationController;
import com.android.server.companion.presence.CompanionDevicePresenceMonitor;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.UserManagerInternal;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class CompanionDeviceManagerService extends SystemService {
    private static final int ASSOCIATIONS_IDS_PER_USER_RANGE = 100000;
    private static final long ASSOCIATION_REMOVAL_TIME_WINDOW_DEFAULT = TimeUnit.DAYS.toMillis(90);
    static final boolean DEBUG = false;
    private static final long PAIR_WITHOUT_PROMPT_WINDOW_MS = 600000;
    private static final String PREF_FILE_NAME = "companion_device_preferences.xml";
    private static final String PREF_KEY_AUTO_REVOKE_GRANTS_DONE = "auto_revoke_grants_done";
    private static final String SYS_PROP_DEBUG_REMOVAL_TIME_WINDOW = "debug.cdm.cdmservice.removal_time_window";
    static final String TAG = "CompanionDeviceManagerService";
    private final ActivityManagerInternal mAmInternal;
    private final IAppOpsService mAppOpsManager;
    private final CompanionApplicationController.Callback mApplicationControllerCallback;
    private AssociationRequestsProcessor mAssociationRequestsProcessor;
    private final AssociationStoreImpl mAssociationStore;
    private final AssociationStore.OnChangeListener mAssociationStoreChangeListener;
    private final ActivityTaskManagerInternal mAtmInternal;
    private CompanionApplicationController mCompanionAppController;
    private final CompanionDevicePresenceMonitor.Callback mDevicePresenceCallback;
    private CompanionDevicePresenceMonitor mDevicePresenceMonitor;
    private final RemoteCallbackList<IOnAssociationsChangedListener> mListeners;
    final PackageManagerInternal mPackageManagerInternal;
    private final PackageMonitor mPackageMonitor;
    private PersistentDataStore mPersistentStore;
    private final PowerWhitelistManager mPowerWhitelistManager;
    private final SparseArray<Map<String, Set<Integer>>> mPreviouslyUsedIds;
    private final UserManager mUserManager;
    private final PersistUserStateHandler mUserPersistenceHandler;

    public CompanionDeviceManagerService(Context context) {
        super(context);
        this.mPreviouslyUsedIds = new SparseArray<>();
        this.mListeners = new RemoteCallbackList<>();
        this.mAssociationStoreChangeListener = new AssociationStore.OnChangeListener() { // from class: com.android.server.companion.CompanionDeviceManagerService.1
            @Override // com.android.server.companion.AssociationStore.OnChangeListener
            public void onAssociationChanged(int changeType, AssociationInfo association) {
                CompanionDeviceManagerService.this.onAssociationChangedInternal(changeType, association);
            }
        };
        this.mDevicePresenceCallback = new CompanionDevicePresenceMonitor.Callback() { // from class: com.android.server.companion.CompanionDeviceManagerService.2
            @Override // com.android.server.companion.presence.CompanionDevicePresenceMonitor.Callback
            public void onDeviceAppeared(int associationId) {
                CompanionDeviceManagerService.this.onDeviceAppearedInternal(associationId);
            }

            @Override // com.android.server.companion.presence.CompanionDevicePresenceMonitor.Callback
            public void onDeviceDisappeared(int associationId) {
                CompanionDeviceManagerService.this.onDeviceDisappearedInternal(associationId);
            }
        };
        this.mApplicationControllerCallback = new CompanionApplicationController.Callback() { // from class: com.android.server.companion.CompanionDeviceManagerService.3
            @Override // com.android.server.companion.CompanionApplicationController.Callback
            public boolean onCompanionApplicationBindingDied(int userId, String packageName) {
                return CompanionDeviceManagerService.this.onCompanionApplicationBindingDiedInternal(userId, packageName);
            }

            @Override // com.android.server.companion.CompanionApplicationController.Callback
            public void onRebindCompanionApplicationTimeout(int userId, String packageName) {
                CompanionDeviceManagerService.this.onRebindCompanionApplicationTimeoutInternal(userId, packageName);
            }
        };
        this.mPackageMonitor = new PackageMonitor() { // from class: com.android.server.companion.CompanionDeviceManagerService.4
            public void onPackageRemoved(String packageName, int uid) {
                CompanionDeviceManagerService.this.onPackageRemoveOrDataClearedInternal(getChangingUserId(), packageName);
            }

            public void onPackageDataCleared(String packageName, int uid) {
                CompanionDeviceManagerService.this.onPackageRemoveOrDataClearedInternal(getChangingUserId(), packageName);
            }

            public void onPackageModified(String packageName) {
                CompanionDeviceManagerService.this.onPackageModifiedInternal(getChangingUserId(), packageName);
            }
        };
        this.mPowerWhitelistManager = (PowerWhitelistManager) context.getSystemService(PowerWhitelistManager.class);
        this.mAppOpsManager = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        this.mAtmInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mUserPersistenceHandler = new PersistUserStateHandler();
        this.mAssociationStore = new AssociationStoreImpl();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        this.mPersistentStore = new PersistentDataStore();
        loadAssociationsFromDisk();
        this.mAssociationStore.registerListener(this.mAssociationStoreChangeListener);
        this.mDevicePresenceMonitor = new CompanionDevicePresenceMonitor(this.mAssociationStore, this.mDevicePresenceCallback);
        this.mAssociationRequestsProcessor = new AssociationRequestsProcessor(this, this.mAssociationStore);
        Context context = getContext();
        this.mCompanionAppController = new CompanionApplicationController(context, this.mApplicationControllerCallback);
        publishBinderService("companiondevice", new CompanionDeviceManagerImpl());
        LocalServices.addService(CompanionDeviceManagerServiceInternal.class, new LocalService());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadAssociationsFromDisk() {
        Set<AssociationInfo> allAssociations = new ArraySet<>();
        synchronized (this.mPreviouslyUsedIds) {
            this.mPersistentStore.readStateForUsers(this.mUserManager.getAliveUsers(), allAssociations, this.mPreviouslyUsedIds);
        }
        this.mAssociationStore.setAssociations(allAssociations);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        Context context = getContext();
        if (phase == 500) {
            this.mPackageMonitor.register(context, FgThread.get().getLooper(), UserHandle.ALL, true);
            this.mDevicePresenceMonitor.init(context);
        } else if (phase == 1000) {
            InactiveAssociationsRemovalService.schedule(getContext());
        }
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(SystemService.TargetUser user) {
        int userId = user.getUserIdentifier();
        List<AssociationInfo> associations = this.mAssociationStore.getAssociationsForUser(userId);
        if (associations.isEmpty()) {
            return;
        }
        updateAtm(userId, associations);
        BackgroundThread.getHandler().sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.companion.CompanionDeviceManagerService$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((CompanionDeviceManagerService) obj).maybeGrantAutoRevokeExemptions();
            }
        }, this), TimeUnit.MINUTES.toMillis(10L));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AssociationInfo getAssociationWithCallerChecks(int userId, String packageName, String macAddress) {
        AssociationInfo association = this.mAssociationStore.getAssociationsForPackageWithAddress(userId, packageName, macAddress);
        return PermissionsUtils.sanitizeWithCallerChecks(getContext(), association);
    }

    AssociationInfo getAssociationWithCallerChecks(int associationId) {
        AssociationInfo association = this.mAssociationStore.getAssociationById(associationId);
        return PermissionsUtils.sanitizeWithCallerChecks(getContext(), association);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDeviceAppearedInternal(int associationId) {
        AssociationInfo association = this.mAssociationStore.getAssociationById(associationId);
        if (association.shouldBindWhenPresent()) {
            int userId = association.getUserId();
            String packageName = association.getPackageName();
            boolean bindImportant = association.isSelfManaged();
            if (!this.mCompanionAppController.isCompanionApplicationBound(userId, packageName)) {
                this.mCompanionAppController.bindCompanionApplication(userId, packageName, bindImportant);
            }
            this.mCompanionAppController.notifyCompanionApplicationDeviceAppeared(association);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDeviceDisappearedInternal(int associationId) {
        AssociationInfo association = this.mAssociationStore.getAssociationById(associationId);
        int userId = association.getUserId();
        String packageName = association.getPackageName();
        if (!this.mCompanionAppController.isCompanionApplicationBound(userId, packageName)) {
            return;
        }
        if (association.shouldBindWhenPresent()) {
            this.mCompanionAppController.notifyCompanionApplicationDeviceDisappeared(association);
        }
        if (shouldBindPackage(userId, packageName)) {
            return;
        }
        this.mCompanionAppController.unbindCompanionApplication(userId, packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean onCompanionApplicationBindingDiedInternal(int userId, String packageName) {
        for (AssociationInfo ai : this.mAssociationStore.getAssociationsForPackage(userId, packageName)) {
            int associationId = ai.getId();
            if (ai.isSelfManaged() && this.mDevicePresenceMonitor.isDevicePresent(associationId)) {
                this.mDevicePresenceMonitor.onSelfManagedDeviceReporterBinderDied(associationId);
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRebindCompanionApplicationTimeoutInternal(int userId, String packageName) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldBindPackage(int userId, String packageName) {
        List<AssociationInfo> packageAssociations = this.mAssociationStore.getAssociationsForPackage(userId, packageName);
        for (AssociationInfo association : packageAssociations) {
            if (association.shouldBindWhenPresent() && this.mDevicePresenceMonitor.isDevicePresent(association.getId())) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAssociationChangedInternal(int changeType, AssociationInfo association) {
        int id = association.getId();
        int userId = association.getUserId();
        String packageName = association.getPackageName();
        if (changeType == 1) {
            markIdAsPreviouslyUsedForPackage(id, userId, packageName);
        }
        List<AssociationInfo> updatedAssociations = this.mAssociationStore.getAssociationsForUser(userId);
        this.mUserPersistenceHandler.postPersistUserState(userId);
        if (changeType != 3) {
            notifyListeners(userId, updatedAssociations);
        }
        updateAtm(userId, updatedAssociations);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void persistStateForUser(int userId) {
        List<AssociationInfo> updatedAssociations = this.mAssociationStore.getAssociationsForUser(userId);
        Map<String, Set<Integer>> usedIdsForUser = getPreviouslyUsedIdsForUser(userId);
        this.mPersistentStore.persistStateForUser(userId, updatedAssociations, usedIdsForUser);
    }

    private void notifyListeners(final int userId, final List<AssociationInfo> associations) {
        this.mListeners.broadcast(new BiConsumer() { // from class: com.android.server.companion.CompanionDeviceManagerService$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                CompanionDeviceManagerService.lambda$notifyListeners$0(userId, associations, (IOnAssociationsChangedListener) obj, obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyListeners$0(int userId, List associations, IOnAssociationsChangedListener listener, Object callbackUserId) {
        if (((Integer) callbackUserId).intValue() == userId) {
            try {
                listener.onAssociationsChanged(associations);
            } catch (RemoteException e) {
            }
        }
    }

    private void markIdAsPreviouslyUsedForPackage(int associationId, int userId, String packageName) {
        synchronized (this.mPreviouslyUsedIds) {
            Map<String, Set<Integer>> usedIdsForUser = this.mPreviouslyUsedIds.get(userId);
            if (usedIdsForUser == null) {
                usedIdsForUser = new HashMap();
                this.mPreviouslyUsedIds.put(userId, usedIdsForUser);
            }
            Set<Integer> usedIdsForPackage = usedIdsForUser.computeIfAbsent(packageName, new Function() { // from class: com.android.server.companion.CompanionDeviceManagerService$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return CompanionDeviceManagerService.lambda$markIdAsPreviouslyUsedForPackage$1((String) obj);
                }
            });
            usedIdsForPackage.add(Integer.valueOf(associationId));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Set lambda$markIdAsPreviouslyUsedForPackage$1(String it) {
        return new HashSet();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageRemoveOrDataClearedInternal(int userId, String packageName) {
        List<AssociationInfo> associationsForPackage = this.mAssociationStore.getAssociationsForPackage(userId, packageName);
        for (AssociationInfo association : associationsForPackage) {
            this.mAssociationStore.removeAssociation(association.getId());
        }
        this.mCompanionAppController.onPackagesChanged(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageModifiedInternal(int userId, String packageName) {
        List<AssociationInfo> associationsForPackage = this.mAssociationStore.getAssociationsForPackage(userId, packageName);
        for (AssociationInfo association : associationsForPackage) {
            updateSpecialAccessPermissionForAssociatedPackage(association);
        }
        this.mCompanionAppController.onPackagesChanged(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeInactiveSelfManagedAssociations() {
        long currentTime = System.currentTimeMillis();
        long removalWindow = SystemProperties.getLong(SYS_PROP_DEBUG_REMOVAL_TIME_WINDOW, -1L);
        if (removalWindow <= 0) {
            removalWindow = ASSOCIATION_REMOVAL_TIME_WINDOW_DEFAULT;
        }
        for (AssociationInfo ai : this.mAssociationStore.getAssociations()) {
            if (ai.isSelfManaged()) {
                boolean isInactive = currentTime - ai.getLastTimeConnectedMs().longValue() >= removalWindow;
                if (isInactive) {
                    Slog.i(TAG, "Removing inactive self-managed association: " + ai.getId());
                    disassociateInternal(ai.getId());
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class CompanionDeviceManagerImpl extends ICompanionDeviceManager.Stub {
        CompanionDeviceManagerImpl() {
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (Throwable e) {
                Slog.e(CompanionDeviceManagerService.TAG, "Error during IPC", e);
                throw ExceptionUtils.propagate(e, RemoteException.class);
            }
        }

        public void associate(AssociationRequest request, IAssociationRequestCallback callback, String packageName, int userId) throws RemoteException {
            Slog.i(CompanionDeviceManagerService.TAG, "associate() request=" + request + ", package=u" + userId + SliceClientPermissions.SliceAuthority.DELIMITER + packageName);
            PermissionsUtils.enforceCallerCanManageAssociationsForPackage(CompanionDeviceManagerService.this.getContext(), userId, packageName, "create associations");
            CompanionDeviceManagerService.this.mAssociationRequestsProcessor.processNewAssociationRequest(request, packageName, userId, callback);
        }

        public List<AssociationInfo> getAssociations(String packageName, int userId) {
            PermissionsUtils.enforceCallerCanManageAssociationsForPackage(CompanionDeviceManagerService.this.getContext(), userId, packageName, "get associations");
            if (!PermissionsUtils.checkCallerCanManageCompanionDevice(CompanionDeviceManagerService.this.getContext())) {
                PackageUtils.enforceUsesCompanionDeviceFeature(CompanionDeviceManagerService.this.getContext(), userId, packageName);
            }
            return CompanionDeviceManagerService.this.mAssociationStore.getAssociationsForPackage(userId, packageName);
        }

        public List<AssociationInfo> getAllAssociationsForUser(int userId) throws RemoteException {
            PermissionsUtils.enforceCallerIsSystemOrCanInteractWithUserId(CompanionDeviceManagerService.this.getContext(), userId);
            PermissionsUtils.enforceCallerCanManageCompanionDevice(CompanionDeviceManagerService.this.getContext(), "getAllAssociationsForUser");
            return CompanionDeviceManagerService.this.mAssociationStore.getAssociationsForUser(userId);
        }

        public void addOnAssociationsChangedListener(IOnAssociationsChangedListener listener, int userId) {
            PermissionsUtils.enforceCallerIsSystemOrCanInteractWithUserId(CompanionDeviceManagerService.this.getContext(), userId);
            PermissionsUtils.enforceCallerCanManageCompanionDevice(CompanionDeviceManagerService.this.getContext(), "addOnAssociationsChangedListener");
            CompanionDeviceManagerService.this.mListeners.register(listener, Integer.valueOf(userId));
        }

        public void removeOnAssociationsChangedListener(IOnAssociationsChangedListener listener, int userId) {
            PermissionsUtils.enforceCallerIsSystemOrCanInteractWithUserId(CompanionDeviceManagerService.this.getContext(), userId);
            PermissionsUtils.enforceCallerCanManageCompanionDevice(CompanionDeviceManagerService.this.getContext(), "removeOnAssociationsChangedListener");
            CompanionDeviceManagerService.this.mListeners.unregister(listener);
        }

        public void legacyDisassociate(String deviceMacAddress, String packageName, int userId) {
            Objects.requireNonNull(deviceMacAddress);
            Objects.requireNonNull(packageName);
            AssociationInfo association = CompanionDeviceManagerService.this.getAssociationWithCallerChecks(userId, packageName, deviceMacAddress);
            if (association == null) {
                throw new IllegalArgumentException("Association does not exist or the caller does not have permissions to manage it (ie. it belongs to a different package or a different user).");
            }
            CompanionDeviceManagerService.this.disassociateInternal(association.getId());
        }

        public void disassociate(int associationId) {
            AssociationInfo association = CompanionDeviceManagerService.this.getAssociationWithCallerChecks(associationId);
            if (association == null) {
                throw new IllegalArgumentException("Association with ID " + associationId + " does not exist or belongs to a different package or belongs to a different user");
            }
            CompanionDeviceManagerService.this.disassociateInternal(associationId);
        }

        public PendingIntent requestNotificationAccess(ComponentName component, int userId) throws RemoteException {
            String callingPackage = component.getPackageName();
            checkCanCallNotificationApi(callingPackage);
            long identity = Binder.clearCallingIdentity();
            try {
                return PendingIntent.getActivityAsUser(CompanionDeviceManagerService.this.getContext(), 0, NotificationAccessConfirmationActivityContract.launcherIntent(CompanionDeviceManagerService.this.getContext(), userId, component), 1409286144, null, new UserHandle(userId));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Deprecated
        public boolean hasNotificationAccess(ComponentName component) throws RemoteException {
            checkCanCallNotificationApi(component.getPackageName());
            NotificationManager nm = (NotificationManager) CompanionDeviceManagerService.this.getContext().getSystemService(NotificationManager.class);
            return nm.isNotificationListenerAccessGranted(component);
        }

        public boolean isDeviceAssociatedForWifiConnection(String packageName, final String macAddress, int userId) {
            CompanionDeviceManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_COMPANION_DEVICES", "isDeviceAssociated");
            boolean bypassMacPermission = CompanionDeviceManagerService.this.getContext().getPackageManager().checkPermission("android.permission.COMPANION_APPROVE_WIFI_CONNECTIONS", packageName) == 0;
            if (bypassMacPermission) {
                return true;
            }
            return CollectionUtils.any(CompanionDeviceManagerService.this.mAssociationStore.getAssociationsForPackage(userId, packageName), new Predicate() { // from class: com.android.server.companion.CompanionDeviceManagerService$CompanionDeviceManagerImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean isLinkedTo;
                    isLinkedTo = ((AssociationInfo) obj).isLinkedTo(macAddress);
                    return isLinkedTo;
                }
            });
        }

        public void registerDevicePresenceListenerService(String deviceAddress, String callingPackage, int userId) throws RemoteException {
            registerDevicePresenceListenerActive(callingPackage, deviceAddress, true);
        }

        public void unregisterDevicePresenceListenerService(String deviceAddress, String callingPackage, int userId) throws RemoteException {
            registerDevicePresenceListenerActive(callingPackage, deviceAddress, false);
        }

        public void dispatchMessage(int messageId, int associationId, byte[] message) throws RemoteException {
        }

        public void notifyDeviceAppeared(int associationId) {
            AssociationInfo association = CompanionDeviceManagerService.this.getAssociationWithCallerChecks(associationId);
            if (association == null) {
                throw new IllegalArgumentException("Association with ID " + associationId + " does not exist or belongs to a different package or belongs to a different user");
            }
            if (!association.isSelfManaged()) {
                throw new IllegalArgumentException("Association with ID " + associationId + " is not self-managed. notifyDeviceAppeared(int) can only be called for self-managed associations.");
            }
            CompanionDeviceManagerService.this.mAssociationStore.updateAssociation(AssociationInfo.builder(association).setLastTimeConnected(System.currentTimeMillis()).build());
            CompanionDeviceManagerService.this.mDevicePresenceMonitor.onSelfManagedDeviceConnected(associationId);
        }

        public void notifyDeviceDisappeared(int associationId) {
            AssociationInfo association = CompanionDeviceManagerService.this.getAssociationWithCallerChecks(associationId);
            if (association == null) {
                throw new IllegalArgumentException("Association with ID " + associationId + " does not exist or belongs to a different package or belongs to a different user");
            }
            if (!association.isSelfManaged()) {
                throw new IllegalArgumentException("Association with ID " + associationId + " is not self-managed. notifyDeviceAppeared(int) can only be called for self-managed associations.");
            }
            CompanionDeviceManagerService.this.mDevicePresenceMonitor.onSelfManagedDeviceDisconnected(associationId);
        }

        private void registerDevicePresenceListenerActive(String packageName, String deviceAddress, boolean active) throws RemoteException {
            CompanionDeviceManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE", "[un]registerDevicePresenceListenerService");
            int userId = UserHandle.getCallingUserId();
            PermissionsUtils.enforceCallerIsSystemOr(userId, packageName);
            AssociationInfo association = CompanionDeviceManagerService.this.mAssociationStore.getAssociationsForPackageWithAddress(userId, packageName, deviceAddress);
            if (association == null) {
                throw new RemoteException(new DeviceNotAssociatedException("App " + packageName + " is not associated with device " + deviceAddress + " for user " + userId));
            }
            if (active == association.isNotifyOnDeviceNearby()) {
                return;
            }
            AssociationInfo association2 = AssociationInfo.builder(association).setNotifyOnDeviceNearby(active).build();
            CompanionDeviceManagerService.this.mAssociationStore.updateAssociation(association2);
            if (active && CompanionDeviceManagerService.this.mDevicePresenceMonitor.isDevicePresent(association2.getId())) {
                CompanionDeviceManagerService.this.onDeviceAppearedInternal(association2.getId());
            }
            if (!active && !CompanionDeviceManagerService.this.shouldBindPackage(userId, packageName)) {
                CompanionDeviceManagerService.this.mCompanionAppController.unbindCompanionApplication(userId, packageName);
            }
        }

        public void createAssociation(String packageName, String macAddress, int userId, byte[] certificate) {
            if (!CompanionDeviceManagerService.this.getContext().getPackageManager().hasSigningCertificate(packageName, certificate, 1)) {
                Slog.e(CompanionDeviceManagerService.TAG, "Given certificate doesn't match the package certificate.");
                return;
            }
            CompanionDeviceManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ASSOCIATE_COMPANION_DEVICES", "createAssociation");
            CompanionDeviceManagerService.this.legacyCreateAssociation(userId, macAddress, packageName, null);
        }

        private void checkCanCallNotificationApi(String callingPackage) {
            int userId = UserHandle.getCallingUserId();
            PermissionsUtils.enforceCallerIsSystemOr(userId, callingPackage);
            if (getCallingUid() == 1000) {
                return;
            }
            PackageUtils.enforceUsesCompanionDeviceFeature(CompanionDeviceManagerService.this.getContext(), userId, callingPackage);
            Preconditions.checkState(!ArrayUtils.isEmpty(CompanionDeviceManagerService.this.mAssociationStore.getAssociationsForPackage(userId, callingPackage)), "App must have an association before calling this API");
        }

        public boolean canPairWithoutPrompt(String packageName, String macAddress, int userId) {
            AssociationInfo association = CompanionDeviceManagerService.this.mAssociationStore.getAssociationsForPackageWithAddress(userId, packageName, macAddress);
            return association != null && System.currentTimeMillis() - association.getTimeApprovedMs() < 600000;
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.companion.CompanionDeviceManagerService$CompanionDeviceManagerImpl */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            PermissionsUtils.enforceCallerCanManageCompanionDevice(CompanionDeviceManagerService.this.getContext(), "onShellCommand");
            CompanionDeviceManagerService companionDeviceManagerService = CompanionDeviceManagerService.this;
            CompanionDeviceShellCommand cmd = new CompanionDeviceShellCommand(companionDeviceManagerService, companionDeviceManagerService.mAssociationStore, CompanionDeviceManagerService.this.mDevicePresenceMonitor);
            cmd.exec(this, in, out, err, args, callback, resultReceiver);
        }

        public void dump(FileDescriptor fd, PrintWriter out, String[] args) {
            if (!DumpUtils.checkDumpAndUsageStatsPermission(CompanionDeviceManagerService.this.getContext(), CompanionDeviceManagerService.TAG, out)) {
                return;
            }
            CompanionDeviceManagerService.this.mAssociationStore.dump(out);
            CompanionDeviceManagerService.this.mDevicePresenceMonitor.dump(out);
            CompanionDeviceManagerService.this.mCompanionAppController.dump(out);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public void legacyCreateAssociation(int userId, String deviceMacAddress, String packageName, String deviceProfile) {
        MacAddress macAddress = MacAddress.fromString(deviceMacAddress);
        createAssociation(userId, packageName, macAddress, null, deviceProfile, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AssociationInfo createAssociation(int userId, String packageName, MacAddress macAddress, CharSequence displayName, String deviceProfile, boolean selfManaged) {
        int id = getNewAssociationIdForPackage(userId, packageName);
        long timestamp = System.currentTimeMillis();
        AssociationInfo association = new AssociationInfo(id, userId, packageName, macAddress, displayName, deviceProfile, selfManaged, false, timestamp, (long) JobStatus.NO_LATEST_RUNTIME);
        Slog.i(TAG, "New CDM association created=" + association);
        this.mAssociationStore.addAssociation(association);
        if (deviceProfile != null) {
            RolesUtils.addRoleHolderForAssociation(getContext(), association);
        }
        updateSpecialAccessPermissionForAssociatedPackage(association);
        MetricUtils.logCreateAssociation(deviceProfile);
        return association;
    }

    private Map<String, Set<Integer>> getPreviouslyUsedIdsForUser(int userId) {
        Map<String, Set<Integer>> previouslyUsedIdsForUserLocked;
        synchronized (this.mPreviouslyUsedIds) {
            previouslyUsedIdsForUserLocked = getPreviouslyUsedIdsForUserLocked(userId);
        }
        return previouslyUsedIdsForUserLocked;
    }

    private Map<String, Set<Integer>> getPreviouslyUsedIdsForUserLocked(int userId) {
        Map<String, Set<Integer>> usedIdsForUser = this.mPreviouslyUsedIds.get(userId);
        if (usedIdsForUser == null) {
            return Collections.emptyMap();
        }
        return deepUnmodifiableCopy(usedIdsForUser);
    }

    private Set<Integer> getPreviouslyUsedIdsForPackageLocked(int userId, String packageName) {
        Map<String, Set<Integer>> usedIdsForUser = getPreviouslyUsedIdsForUserLocked(userId);
        Set<Integer> usedIdsForPackage = usedIdsForUser.get(packageName);
        if (usedIdsForPackage == null) {
            return Collections.emptySet();
        }
        return usedIdsForPackage;
    }

    private int getNewAssociationIdForPackage(int userId, String packageName) {
        int id;
        synchronized (this.mPreviouslyUsedIds) {
            SparseBooleanArray usedIds = new SparseBooleanArray();
            for (AssociationInfo it : this.mAssociationStore.getAssociations()) {
                usedIds.put(it.getId(), true);
            }
            Set<Integer> previouslyUsedIds = getPreviouslyUsedIdsForPackageLocked(userId, packageName);
            id = getFirstAssociationIdForUser(userId);
            int lastAvailableIdForUser = getLastAssociationIdForUser(userId);
            while (true) {
                if (!usedIds.get(id) && !previouslyUsedIds.contains(Integer.valueOf(id))) {
                }
                id++;
                if (id > lastAvailableIdForUser) {
                    throw new RuntimeException("Cannot create a new Association ID for " + packageName + " for user " + userId);
                }
            }
        }
        return id;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void disassociateInternal(int associationId) {
        final AssociationInfo association = this.mAssociationStore.getAssociationById(associationId);
        int userId = association.getUserId();
        String packageName = association.getPackageName();
        final String deviceProfile = association.getDeviceProfile();
        boolean wasPresent = this.mDevicePresenceMonitor.isDevicePresent(associationId);
        this.mAssociationStore.removeAssociation(associationId);
        MetricUtils.logRemoveAssociation(deviceProfile);
        List<AssociationInfo> otherAssociations = this.mAssociationStore.getAssociationsForPackage(userId, packageName);
        if (deviceProfile != null) {
            boolean shouldKeepTheRole = CollectionUtils.any(otherAssociations, new Predicate() { // from class: com.android.server.companion.CompanionDeviceManagerService$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = deviceProfile.equals(((AssociationInfo) obj).getDeviceProfile());
                    return equals;
                }
            });
            if (!shouldKeepTheRole) {
                Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.companion.CompanionDeviceManagerService$$ExternalSyntheticLambda4
                    public final void runOrThrow() {
                        CompanionDeviceManagerService.this.m2697xcc613034(association);
                    }
                });
            }
        }
        if (!wasPresent || !association.isNotifyOnDeviceNearby()) {
            return;
        }
        boolean shouldStayBound = CollectionUtils.any(otherAssociations, new Predicate() { // from class: com.android.server.companion.CompanionDeviceManagerService$$ExternalSyntheticLambda5
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return CompanionDeviceManagerService.this.m2698xd264fb93((AssociationInfo) obj);
            }
        });
        if (shouldStayBound) {
            return;
        }
        this.mCompanionAppController.unbindCompanionApplication(userId, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$disassociateInternal$3$com-android-server-companion-CompanionDeviceManagerService  reason: not valid java name */
    public /* synthetic */ void m2697xcc613034(AssociationInfo association) throws Exception {
        RolesUtils.removeRoleHolderForAssociation(getContext(), association);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$disassociateInternal$4$com-android-server-companion-CompanionDeviceManagerService  reason: not valid java name */
    public /* synthetic */ boolean m2698xd264fb93(AssociationInfo it) {
        return it.isNotifyOnDeviceNearby() && this.mDevicePresenceMonitor.isDevicePresent(it.getId());
    }

    private void updateSpecialAccessPermissionForAssociatedPackage(AssociationInfo association) {
        final PackageInfo packageInfo = PackageUtils.getPackageInfo(getContext(), association.getUserId(), association.getPackageName());
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.companion.CompanionDeviceManagerService$$ExternalSyntheticLambda1
            public final void runOrThrow() {
                CompanionDeviceManagerService.this.m2699xe8574fff(packageInfo);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: updateSpecialAccessPermissionAsSystem */
    public void m2699xe8574fff(PackageInfo packageInfo) {
        if (containsEither(packageInfo.requestedPermissions, "android.permission.RUN_IN_BACKGROUND", "android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND")) {
            this.mPowerWhitelistManager.addToWhitelist(packageInfo.packageName);
        } else {
            try {
                this.mPowerWhitelistManager.removeFromWhitelist(packageInfo.packageName);
            } catch (UnsupportedOperationException e) {
                Slog.w(TAG, packageInfo.packageName + " can't be removed from power save whitelist. It might due to the package is whitelisted by the system.");
            }
        }
        NetworkPolicyManager networkPolicyManager = NetworkPolicyManager.from(getContext());
        if (containsEither(packageInfo.requestedPermissions, "android.permission.USE_DATA_IN_BACKGROUND", "android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND")) {
            networkPolicyManager.addUidPolicy(packageInfo.applicationInfo.uid, 4);
        } else {
            networkPolicyManager.removeUidPolicy(packageInfo.applicationInfo.uid, 4);
        }
        exemptFromAutoRevoke(packageInfo.packageName, packageInfo.applicationInfo.uid);
    }

    private void exemptFromAutoRevoke(String packageName, int uid) {
        try {
            this.mAppOpsManager.setMode(97, uid, packageName, 1);
        } catch (RemoteException e) {
            Slog.w(TAG, "Error while granting auto revoke exemption for " + packageName, e);
        }
    }

    private void updateAtm(int userId, List<AssociationInfo> associations) {
        Set<Integer> companionAppUids = new ArraySet<>();
        for (AssociationInfo association : associations) {
            int uid = this.mPackageManagerInternal.getPackageUid(association.getPackageName(), 0L, userId);
            if (uid >= 0) {
                companionAppUids.add(Integer.valueOf(uid));
            }
        }
        ActivityTaskManagerInternal activityTaskManagerInternal = this.mAtmInternal;
        if (activityTaskManagerInternal != null) {
            activityTaskManagerInternal.setCompanionAppUids(userId, companionAppUids);
        }
        ActivityManagerInternal activityManagerInternal = this.mAmInternal;
        if (activityManagerInternal != null) {
            activityManagerInternal.setCompanionAppUids(userId, new ArraySet(companionAppUids));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Type inference failed for: r6v0 */
    /* JADX WARN: Type inference failed for: r6v1, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r6v2 */
    public void maybeGrantAutoRevokeExemptions() {
        Slog.d(TAG, "maybeGrantAutoRevokeExemptions()");
        PackageManager pm = getContext().getPackageManager();
        int[] userIds = ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserIds();
        int length = userIds.length;
        ?? r6 = 0;
        int i = 0;
        while (i < length) {
            int userId = userIds[i];
            SharedPreferences pref = getContext().getSharedPreferences(new File(Environment.getUserSystemDirectory(userId), PREF_FILE_NAME), (int) r6);
            if (!pref.getBoolean(PREF_KEY_AUTO_REVOKE_GRANTS_DONE, r6)) {
                try {
                    List<AssociationInfo> associations = this.mAssociationStore.getAssociationsForUser(userId);
                    for (AssociationInfo a : associations) {
                        try {
                            int uid = pm.getPackageUidAsUser(a.getPackageName(), userId);
                            exemptFromAutoRevoke(a.getPackageName(), uid);
                        } catch (PackageManager.NameNotFoundException e) {
                            Slog.w(TAG, "Unknown companion package: " + a.getPackageName(), e);
                        }
                    }
                } finally {
                    pref.edit().putBoolean(PREF_KEY_AUTO_REVOKE_GRANTS_DONE, true).apply();
                }
            }
            i++;
            r6 = 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getFirstAssociationIdForUser(int userId) {
        return (ASSOCIATIONS_IDS_PER_USER_RANGE * userId) + 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getLastAssociationIdForUser(int userId) {
        return (userId + 1) * ASSOCIATIONS_IDS_PER_USER_RANGE;
    }

    private static Map<String, Set<Integer>> deepUnmodifiableCopy(Map<String, Set<Integer>> orig) {
        Map<String, Set<Integer>> copy = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> entry : orig.entrySet()) {
            Set<Integer> valueCopy = new HashSet<>(entry.getValue());
            copy.put(entry.getKey(), Collections.unmodifiableSet(valueCopy));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static <T> boolean containsEither(T[] array, T a, T b) {
        return ArrayUtils.contains(array, a) || ArrayUtils.contains(array, b);
    }

    /* loaded from: classes.dex */
    private class LocalService implements CompanionDeviceManagerServiceInternal {
        private LocalService() {
        }

        @Override // com.android.server.companion.CompanionDeviceManagerServiceInternal
        public void removeInactiveSelfManagedAssociations() {
            CompanionDeviceManagerService.this.removeInactiveSelfManagedAssociations();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void persistState() {
        this.mUserPersistenceHandler.clearMessages();
        for (UserInfo user : this.mUserManager.getAliveUsers()) {
            persistStateForUser(user.id);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PersistUserStateHandler extends Handler {
        PersistUserStateHandler() {
            super(BackgroundThread.get().getLooper());
        }

        synchronized void postPersistUserState(int userId) {
            if (!hasMessages(userId)) {
                sendMessage(obtainMessage(userId));
            }
        }

        synchronized void clearMessages() {
            removeCallbacksAndMessages(null);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int userId = msg.what;
            CompanionDeviceManagerService.this.persistStateForUser(userId);
        }
    }
}
