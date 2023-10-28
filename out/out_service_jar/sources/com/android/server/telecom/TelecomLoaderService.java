package com.android.server.telecom;

import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.telecom.DefaultDialerManager;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.util.IntArray;
import android.util.Slog;
import com.android.internal.telecom.ITelecomLoader;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.telephony.SmsApplication;
import com.android.server.DeviceIdleInternal;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.permission.LegacyPermissionManagerInternal;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class TelecomLoaderService extends SystemService {
    private static final String SERVICE_ACTION = "com.android.ITelecomService";
    private static final ComponentName SERVICE_COMPONENT = new ComponentName("com.android.server.telecom", "com.android.server.telecom.components.TelecomService");
    private static final String TAG = "TelecomLoaderService";
    private final Context mContext;
    private IntArray mDefaultSimCallManagerRequests;
    private final Object mLock;
    private TelecomServiceConnection mServiceConnection;
    private InternalServiceRepository mServiceRepo;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class TelecomServiceConnection implements ServiceConnection {
        private TelecomServiceConnection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                ITelecomLoader telecomLoader = ITelecomLoader.Stub.asInterface(service);
                ITelecomService telecomService = telecomLoader.createTelecomService(TelecomLoaderService.this.mServiceRepo);
                SmsApplication.getDefaultMmsApplication(TelecomLoaderService.this.mContext, false);
                ServiceManager.addService("telecom", telecomService.asBinder());
                synchronized (TelecomLoaderService.this.mLock) {
                    LegacyPermissionManagerInternal permissionManager = (LegacyPermissionManagerInternal) LocalServices.getService(LegacyPermissionManagerInternal.class);
                    if (TelecomLoaderService.this.mDefaultSimCallManagerRequests != null && TelecomLoaderService.this.mDefaultSimCallManagerRequests != null) {
                        TelecomManager telecomManager = (TelecomManager) TelecomLoaderService.this.mContext.getSystemService("telecom");
                        PhoneAccountHandle phoneAccount = telecomManager.getSimCallManager();
                        if (phoneAccount != null) {
                            int requestCount = TelecomLoaderService.this.mDefaultSimCallManagerRequests.size();
                            String packageName = phoneAccount.getComponentName().getPackageName();
                            for (int i = requestCount - 1; i >= 0; i--) {
                                int userId = TelecomLoaderService.this.mDefaultSimCallManagerRequests.get(i);
                                TelecomLoaderService.this.mDefaultSimCallManagerRequests.remove(i);
                                permissionManager.grantDefaultPermissionsToDefaultSimCallManager(packageName, userId);
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                Slog.w(TelecomLoaderService.TAG, "Failed linking to death.");
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            TelecomLoaderService.this.connectToTelecom();
        }
    }

    public TelecomLoaderService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mContext = context;
        registerDefaultAppProviders();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 550) {
            registerDefaultAppNotifier();
            registerCarrierConfigChangedReceiver();
            setupServiceRepository();
            connectToTelecom();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void connectToTelecom() {
        synchronized (this.mLock) {
            TelecomServiceConnection telecomServiceConnection = this.mServiceConnection;
            if (telecomServiceConnection != null) {
                this.mContext.unbindService(telecomServiceConnection);
                this.mServiceConnection = null;
            }
            TelecomServiceConnection serviceConnection = new TelecomServiceConnection();
            Intent intent = new Intent(SERVICE_ACTION);
            intent.setComponent(SERVICE_COMPONENT);
            if (this.mContext.bindServiceAsUser(intent, serviceConnection, 67108929, UserHandle.SYSTEM)) {
                this.mServiceConnection = serviceConnection;
            }
        }
    }

    private void setupServiceRepository() {
        DeviceIdleInternal deviceIdleInternal = (DeviceIdleInternal) getLocalService(DeviceIdleInternal.class);
        this.mServiceRepo = new InternalServiceRepository(deviceIdleInternal);
    }

    private void registerDefaultAppProviders() {
        LegacyPermissionManagerInternal permissionManager = (LegacyPermissionManagerInternal) LocalServices.getService(LegacyPermissionManagerInternal.class);
        permissionManager.setSmsAppPackagesProvider(new LegacyPermissionManagerInternal.PackagesProvider() { // from class: com.android.server.telecom.TelecomLoaderService$$ExternalSyntheticLambda0
            @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal.PackagesProvider
            public final String[] getPackages(int i) {
                return TelecomLoaderService.this.m6787xaf8fa5cb(i);
            }
        });
        permissionManager.setDialerAppPackagesProvider(new LegacyPermissionManagerInternal.PackagesProvider() { // from class: com.android.server.telecom.TelecomLoaderService$$ExternalSyntheticLambda1
            @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal.PackagesProvider
            public final String[] getPackages(int i) {
                return TelecomLoaderService.this.m6788xaf193fcc(i);
            }
        });
        permissionManager.setSimCallManagerPackagesProvider(new LegacyPermissionManagerInternal.PackagesProvider() { // from class: com.android.server.telecom.TelecomLoaderService$$ExternalSyntheticLambda2
            @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal.PackagesProvider
            public final String[] getPackages(int i) {
                return TelecomLoaderService.this.m6789xaea2d9cd(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerDefaultAppProviders$0$com-android-server-telecom-TelecomLoaderService  reason: not valid java name */
    public /* synthetic */ String[] m6787xaf8fa5cb(int userId) {
        synchronized (this.mLock) {
            if (this.mServiceConnection == null) {
                return null;
            }
            ComponentName smsComponent = SmsApplication.getDefaultSmsApplication(this.mContext, true);
            if (smsComponent != null) {
                return new String[]{smsComponent.getPackageName()};
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerDefaultAppProviders$1$com-android-server-telecom-TelecomLoaderService  reason: not valid java name */
    public /* synthetic */ String[] m6788xaf193fcc(int userId) {
        synchronized (this.mLock) {
            if (this.mServiceConnection == null) {
                return null;
            }
            String packageName = DefaultDialerManager.getDefaultDialerApplication(this.mContext);
            if (packageName != null) {
                return new String[]{packageName};
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerDefaultAppProviders$2$com-android-server-telecom-TelecomLoaderService  reason: not valid java name */
    public /* synthetic */ String[] m6789xaea2d9cd(int userId) {
        synchronized (this.mLock) {
            if (this.mServiceConnection == null) {
                if (this.mDefaultSimCallManagerRequests == null) {
                    this.mDefaultSimCallManagerRequests = new IntArray();
                }
                this.mDefaultSimCallManagerRequests.add(userId);
                return null;
            }
            SubscriptionManager subscriptionManager = (SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class);
            if (subscriptionManager == null) {
                return null;
            }
            TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
            List<String> packages = new ArrayList<>();
            int[] subIds = subscriptionManager.getActiveSubscriptionIdList();
            for (int subId : subIds) {
                PhoneAccountHandle phoneAccount = telecomManager.getSimCallManagerForSubscription(subId);
                if (phoneAccount != null) {
                    packages.add(phoneAccount.getComponentName().getPackageName());
                }
            }
            return (String[]) packages.toArray(new String[0]);
        }
    }

    private void registerDefaultAppNotifier() {
        RoleManager roleManager = (RoleManager) this.mContext.getSystemService(RoleManager.class);
        roleManager.addOnRoleHoldersChangedListenerAsUser(this.mContext.getMainExecutor(), new OnRoleHoldersChangedListener() { // from class: com.android.server.telecom.TelecomLoaderService$$ExternalSyntheticLambda3
            public final void onRoleHoldersChanged(String str, UserHandle userHandle) {
                TelecomLoaderService.this.m6786x74fcb252(str, userHandle);
            }
        }, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerDefaultAppNotifier$3$com-android-server-telecom-TelecomLoaderService  reason: not valid java name */
    public /* synthetic */ void m6786x74fcb252(String roleName, UserHandle user) {
        updateSimCallManagerPermissions(user.getIdentifier());
    }

    private void registerCarrierConfigChangedReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.android.server.telecom.TelecomLoaderService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int[] userIds;
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    for (int userId : UserManagerService.getInstance().getUserIds()) {
                        TelecomLoaderService.this.updateSimCallManagerPermissions(userId);
                    }
                }
            }
        };
        this.mContext.registerReceiverAsUser(receiver, UserHandle.ALL, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"), null, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSimCallManagerPermissions(int userId) {
        LegacyPermissionManagerInternal permissionManager = (LegacyPermissionManagerInternal) LocalServices.getService(LegacyPermissionManagerInternal.class);
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        PhoneAccountHandle phoneAccount = telecomManager.getSimCallManager(userId);
        if (phoneAccount != null) {
            Slog.i(TAG, "updating sim call manager permissions for userId:" + userId);
            String packageName = phoneAccount.getComponentName().getPackageName();
            permissionManager.grantDefaultPermissionsToDefaultSimCallManager(packageName, userId);
        }
    }
}
