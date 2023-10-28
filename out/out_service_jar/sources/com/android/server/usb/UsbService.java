package com.android.server.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.IUsbOperationInternal;
import android.hardware.usb.ParcelableUsbPort;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.FgThread;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.storage.DiskStatsFileLogger;
import com.android.server.usb.UsbService;
import dalvik.annotation.optimization.NeverCompile;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
/* loaded from: classes2.dex */
public class UsbService extends IUsbManager.Stub {
    private static final String TAG = "UsbService";
    private final UsbAlsaManager mAlsaManager;
    private final Context mContext;
    private int mCurrentUserId;
    private UsbDeviceManager mDeviceManager;
    private DeviceNameChangedObserver mDeviceNameChangedObserver;
    private UsbHostManager mHostManager;
    private final UsbPermissionManager mPermissionManager;
    private UsbPortManager mPortManager;
    private ContentResolver mResolver;
    private final UsbSettingsManager mSettingsManager;
    private final UserManager mUserManager;
    private final String USB_NODE_PATH = "/config/usb_gadget/g1/strings/0x409/product";
    private final Object mLock = new Object();

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        private final CompletableFuture<Void> mOnActivityManagerPhaseFinished;
        private final CompletableFuture<Void> mOnStartFinished;
        private UsbService mUsbService;

        public Lifecycle(Context context) {
            super(context);
            this.mOnStartFinished = new CompletableFuture<>();
            this.mOnActivityManagerPhaseFinished = new CompletableFuture<>();
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.usb.UsbService$Lifecycle$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    UsbService.Lifecycle.this.m7401lambda$onStart$0$comandroidserverusbUsbService$Lifecycle();
                }
            }, "UsbService$Lifecycle#onStart");
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.usb.UsbService$Lifecycle */
        /* JADX INFO: Access modifiers changed from: package-private */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.usb.UsbService, android.os.IBinder] */
        /* renamed from: lambda$onStart$0$com-android-server-usb-UsbService$Lifecycle  reason: not valid java name */
        public /* synthetic */ void m7401lambda$onStart$0$comandroidserverusbUsbService$Lifecycle() {
            ?? usbService = new UsbService(getContext());
            this.mUsbService = usbService;
            publishBinderService("usb", usbService);
            this.mOnStartFinished.complete(null);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 550) {
                SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.usb.UsbService$Lifecycle$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        UsbService.Lifecycle.this.m7400lambda$onBootPhase$1$comandroidserverusbUsbService$Lifecycle();
                    }
                }, "UsbService$Lifecycle#onBootPhase");
            } else if (phase == 1000) {
                this.mOnActivityManagerPhaseFinished.join();
                this.mUsbService.bootCompleted();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBootPhase$1$com-android-server-usb-UsbService$Lifecycle  reason: not valid java name */
        public /* synthetic */ void m7400lambda$onBootPhase$1$comandroidserverusbUsbService$Lifecycle() {
            this.mOnStartFinished.join();
            this.mUsbService.systemReady();
            this.mOnActivityManagerPhaseFinished.complete(null);
        }

        @Override // com.android.server.SystemService
        public void onUserSwitching(SystemService.TargetUser from, final SystemService.TargetUser to) {
            FgThread.getHandler().postAtFrontOfQueue(new Runnable() { // from class: com.android.server.usb.UsbService$Lifecycle$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UsbService.Lifecycle.this.m7402xd19a2631(to);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onUserSwitching$2$com-android-server-usb-UsbService$Lifecycle  reason: not valid java name */
        public /* synthetic */ void m7402xd19a2631(SystemService.TargetUser to) {
            this.mUsbService.onSwitchUser(to.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser userInfo) {
            this.mUsbService.onStopUser(userInfo.getUserHandle());
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser userInfo) {
            this.mUsbService.onUnlockUser(userInfo.getUserIdentifier());
        }
    }

    /* loaded from: classes2.dex */
    private class DeviceNameChangedObserver extends ContentObserver {
        public DeviceNameChangedObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            Slog.d(UsbService.TAG, "DeviceNameChangedObserver.onChange called");
            UsbService usbService = UsbService.this;
            usbService.setUsbDeviceName("/config/usb_gadget/g1/strings/0x409/product", Settings.Global.getString(usbService.mResolver, "device_name"));
            if (UsbService.this.mDeviceNameChangedObserver != null) {
                Slog.d(UsbService.TAG, "UsbService unregister DeviceNameChangedObserver");
                UsbService.this.mResolver.unregisterContentObserver(UsbService.this.mDeviceNameChangedObserver);
                UsbService.this.mDeviceNameChangedObserver = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbUserSettingsManager getSettingsForUser(int userId) {
        return this.mSettingsManager.getSettingsForUser(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbUserPermissionManager getPermissionsForUser(int userId) {
        return this.mPermissionManager.getPermissionsForUser(userId);
    }

    public UsbService(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        UsbSettingsManager usbSettingsManager = new UsbSettingsManager(context, this);
        this.mSettingsManager = usbSettingsManager;
        UsbPermissionManager usbPermissionManager = new UsbPermissionManager(context, this);
        this.mPermissionManager = usbPermissionManager;
        UsbAlsaManager usbAlsaManager = new UsbAlsaManager(context);
        this.mAlsaManager = usbAlsaManager;
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature("android.hardware.usb.host")) {
            this.mHostManager = new UsbHostManager(context, usbAlsaManager, usbPermissionManager);
        }
        if (new File("/sys/class/android_usb").exists()) {
            this.mDeviceManager = new UsbDeviceManager(context, usbAlsaManager, usbSettingsManager, usbPermissionManager);
        }
        if (this.mHostManager != null || this.mDeviceManager != null) {
            this.mPortManager = new UsbPortManager(context);
        }
        setUsbDeviceName("/config/usb_gadget/g1/strings/0x409/product", SystemProperties.get("persist.sys.tran.device.name"));
        onSwitchUser(0);
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.android.server.usb.UsbService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action) && UsbService.this.mDeviceManager != null) {
                    UsbService.this.mDeviceManager.updateUserRestrictions();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.setPriority(1000);
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        context.registerReceiverAsUser(receiver, UserHandle.ALL, filter, null, null);
        ContentResolver contentResolver = context.getContentResolver();
        this.mResolver = contentResolver;
        if ("0".equals(Settings.Global.getString(contentResolver, "device_provisioned")) || "none".equals(SystemProperties.get("persist.sys.tran.device.name", "none"))) {
            Slog.d(TAG, "UsbService register DeviceNameChangedObserver");
            this.mDeviceNameChangedObserver = new DeviceNameChangedObserver(null);
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("device_name"), false, this.mDeviceNameChangedObserver);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:7:0x0015 -> B:22:0x0025). Please submit an issue!!! */
    public void setUsbDeviceName(String nodePath, String deviceName) {
        BufferedWriter writer = null;
        try {
            try {
                try {
                    writer = new BufferedWriter(new FileWriter(nodePath));
                    writer.write(deviceName);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e2) {
                e2.printStackTrace();
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSwitchUser(int newUserId) {
        synchronized (this.mLock) {
            this.mCurrentUserId = newUserId;
            UsbProfileGroupSettingsManager settings = this.mSettingsManager.getSettingsForProfileGroup(UserHandle.of(newUserId));
            UsbHostManager usbHostManager = this.mHostManager;
            if (usbHostManager != null) {
                usbHostManager.setCurrentUserSettings(settings);
            }
            UsbDeviceManager usbDeviceManager = this.mDeviceManager;
            if (usbDeviceManager != null) {
                usbDeviceManager.setCurrentUser(newUserId, settings);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStopUser(UserHandle stoppedUser) {
        this.mSettingsManager.remove(stoppedUser);
    }

    public void systemReady() {
        this.mAlsaManager.systemReady();
        UsbDeviceManager usbDeviceManager = this.mDeviceManager;
        if (usbDeviceManager != null) {
            usbDeviceManager.systemReady();
        }
        UsbHostManager usbHostManager = this.mHostManager;
        if (usbHostManager != null) {
            usbHostManager.systemReady();
        }
        UsbPortManager usbPortManager = this.mPortManager;
        if (usbPortManager != null) {
            usbPortManager.systemReady();
        }
    }

    public void bootCompleted() {
        UsbDeviceManager usbDeviceManager = this.mDeviceManager;
        if (usbDeviceManager != null) {
            usbDeviceManager.bootCompleted();
        }
    }

    public void onUnlockUser(int user) {
        UsbDeviceManager usbDeviceManager = this.mDeviceManager;
        if (usbDeviceManager != null) {
            usbDeviceManager.onUnlockUser(user);
        }
    }

    public void getDeviceList(Bundle devices) {
        UsbHostManager usbHostManager = this.mHostManager;
        if (usbHostManager != null) {
            usbHostManager.getDeviceList(devices);
        }
    }

    public ParcelFileDescriptor openDevice(String deviceName, String packageName) {
        ParcelFileDescriptor fd = null;
        if (this.mHostManager != null && deviceName != null) {
            int uid = Binder.getCallingUid();
            int pid = Binder.getCallingPid();
            int user = UserHandle.getUserId(uid);
            long ident = clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    if (this.mUserManager.isSameProfileGroup(user, this.mCurrentUserId)) {
                        fd = this.mHostManager.openDevice(deviceName, getPermissionsForUser(user), packageName, pid, uid);
                    } else {
                        Slog.w(TAG, "Cannot open " + deviceName + " for user " + user + " as user is not active.");
                    }
                }
            } finally {
                restoreCallingIdentity(ident);
            }
        }
        return fd;
    }

    public UsbAccessory getCurrentAccessory() {
        UsbDeviceManager usbDeviceManager = this.mDeviceManager;
        if (usbDeviceManager != null) {
            return usbDeviceManager.getCurrentAccessory();
        }
        return null;
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory accessory) {
        if (this.mDeviceManager != null) {
            int uid = Binder.getCallingUid();
            int pid = Binder.getCallingPid();
            int user = UserHandle.getUserId(uid);
            long ident = clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    if (this.mUserManager.isSameProfileGroup(user, this.mCurrentUserId)) {
                        return this.mDeviceManager.openAccessory(accessory, getPermissionsForUser(user), pid, uid);
                    }
                    Slog.w(TAG, "Cannot open " + accessory + " for user " + user + " as user is not active.");
                    return null;
                }
            } finally {
                restoreCallingIdentity(ident);
            }
        }
        return null;
    }

    public ParcelFileDescriptor getControlFd(long function) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_MTP", null);
        return this.mDeviceManager.getControlFd(function);
    }

    public void setDevicePackage(UsbDevice device, String packageName, int userId) {
        Objects.requireNonNull(device);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle user = UserHandle.of(userId);
        long token = Binder.clearCallingIdentity();
        try {
            this.mSettingsManager.getSettingsForProfileGroup(user).setDevicePackage(device, packageName, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void setAccessoryPackage(UsbAccessory accessory, String packageName, int userId) {
        Objects.requireNonNull(accessory);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle user = UserHandle.of(userId);
        long token = Binder.clearCallingIdentity();
        try {
            this.mSettingsManager.getSettingsForProfileGroup(user).setAccessoryPackage(accessory, packageName, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void addDevicePackagesToPreferenceDenied(UsbDevice device, String[] packageNames, UserHandle user) {
        Objects.requireNonNull(device);
        String[] packageNames2 = (String[]) Preconditions.checkArrayElementsNotNull(packageNames, DiskStatsFileLogger.PACKAGE_NAMES_KEY);
        Objects.requireNonNull(user);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long token = Binder.clearCallingIdentity();
        try {
            this.mSettingsManager.getSettingsForProfileGroup(user).addDevicePackagesToDenied(device, packageNames2, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void addAccessoryPackagesToPreferenceDenied(UsbAccessory accessory, String[] packageNames, UserHandle user) {
        Objects.requireNonNull(accessory);
        String[] packageNames2 = (String[]) Preconditions.checkArrayElementsNotNull(packageNames, DiskStatsFileLogger.PACKAGE_NAMES_KEY);
        Objects.requireNonNull(user);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long token = Binder.clearCallingIdentity();
        try {
            this.mSettingsManager.getSettingsForProfileGroup(user).addAccessoryPackagesToDenied(accessory, packageNames2, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void removeDevicePackagesFromPreferenceDenied(UsbDevice device, String[] packageNames, UserHandle user) {
        Objects.requireNonNull(device);
        String[] packageNames2 = (String[]) Preconditions.checkArrayElementsNotNull(packageNames, DiskStatsFileLogger.PACKAGE_NAMES_KEY);
        Objects.requireNonNull(user);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long token = Binder.clearCallingIdentity();
        try {
            this.mSettingsManager.getSettingsForProfileGroup(user).removeDevicePackagesFromDenied(device, packageNames2, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void removeAccessoryPackagesFromPreferenceDenied(UsbAccessory accessory, String[] packageNames, UserHandle user) {
        Objects.requireNonNull(accessory);
        String[] packageNames2 = (String[]) Preconditions.checkArrayElementsNotNull(packageNames, DiskStatsFileLogger.PACKAGE_NAMES_KEY);
        Objects.requireNonNull(user);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long token = Binder.clearCallingIdentity();
        try {
            this.mSettingsManager.getSettingsForProfileGroup(user).removeAccessoryPackagesFromDenied(accessory, packageNames2, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void setDevicePersistentPermission(UsbDevice device, int uid, UserHandle user, boolean shouldBeGranted) {
        Objects.requireNonNull(device);
        Objects.requireNonNull(user);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long token = Binder.clearCallingIdentity();
        try {
            this.mPermissionManager.getPermissionsForUser(user).setDevicePersistentPermission(device, uid, shouldBeGranted);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void setAccessoryPersistentPermission(UsbAccessory accessory, int uid, UserHandle user, boolean shouldBeGranted) {
        Objects.requireNonNull(accessory);
        Objects.requireNonNull(user);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long token = Binder.clearCallingIdentity();
        try {
            this.mPermissionManager.getPermissionsForUser(user).setAccessoryPersistentPermission(accessory, uid, shouldBeGranted);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean hasDevicePermission(UsbDevice device, String packageName) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        int userId = UserHandle.getUserId(uid);
        long token = Binder.clearCallingIdentity();
        try {
            return getPermissionsForUser(userId).hasPermission(device, packageName, pid, uid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean hasAccessoryPermission(UsbAccessory accessory) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        int userId = UserHandle.getUserId(uid);
        long token = Binder.clearCallingIdentity();
        try {
            return getPermissionsForUser(userId).hasPermission(accessory, pid, uid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void requestDevicePermission(UsbDevice device, String packageName, PendingIntent pi) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        int userId = UserHandle.getUserId(uid);
        long token = Binder.clearCallingIdentity();
        try {
            getPermissionsForUser(userId).requestPermission(device, packageName, pi, pid, uid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void requestAccessoryPermission(UsbAccessory accessory, String packageName, PendingIntent pi) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        int userId = UserHandle.getUserId(uid);
        long token = Binder.clearCallingIdentity();
        try {
            getPermissionsForUser(userId).requestPermission(accessory, packageName, pi, pid, uid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void grantDevicePermission(UsbDevice device, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        int userId = UserHandle.getUserId(uid);
        long token = Binder.clearCallingIdentity();
        try {
            getPermissionsForUser(userId).grantDevicePermission(device, uid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void grantAccessoryPermission(UsbAccessory accessory, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        int userId = UserHandle.getUserId(uid);
        long token = Binder.clearCallingIdentity();
        try {
            getPermissionsForUser(userId).grantAccessoryPermission(accessory, uid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean hasDefaults(String packageName, int userId) {
        String packageName2 = (String) Preconditions.checkStringNotEmpty(packageName);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle user = UserHandle.of(userId);
        long token = Binder.clearCallingIdentity();
        try {
            return this.mSettingsManager.getSettingsForProfileGroup(user).hasDefaults(packageName2, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void clearDefaults(String packageName, int userId) {
        String packageName2 = (String) Preconditions.checkStringNotEmpty(packageName);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        UserHandle user = UserHandle.of(userId);
        long token = Binder.clearCallingIdentity();
        try {
            this.mSettingsManager.getSettingsForProfileGroup(user).clearDefaults(packageName2, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void setCurrentFunctions(long functions) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkArgument(UsbManager.areSettableFunctions(functions));
        Preconditions.checkState(this.mDeviceManager != null);
        this.mDeviceManager.setCurrentFunctions(functions);
    }

    public void setCurrentFunction(String functions, boolean usbDataUnlocked) {
        setCurrentFunctions(UsbManager.usbFunctionsFromString(functions));
    }

    public boolean isFunctionEnabled(String function) {
        return (getCurrentFunctions() & UsbManager.usbFunctionsFromString(function)) != 0;
    }

    public long getCurrentFunctions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkState(this.mDeviceManager != null);
        return this.mDeviceManager.getCurrentFunctions();
    }

    public void setScreenUnlockedFunctions(long functions) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkArgument(UsbManager.areSettableFunctions(functions));
        Preconditions.checkState(this.mDeviceManager != null);
        this.mDeviceManager.setScreenUnlockedFunctions(functions);
    }

    public long getScreenUnlockedFunctions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkState(this.mDeviceManager != null);
        return this.mDeviceManager.getScreenUnlockedFunctions();
    }

    public int getCurrentUsbSpeed() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkNotNull(this.mDeviceManager, "DeviceManager must not be null");
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mDeviceManager.getCurrentUsbSpeed();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getGadgetHalVersion() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkNotNull(this.mDeviceManager, "DeviceManager must not be null");
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mDeviceManager.getGadgetHalVersion();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void resetUsbGadget() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        Preconditions.checkNotNull(this.mDeviceManager, "DeviceManager must not be null");
        long ident = Binder.clearCallingIdentity();
        try {
            this.mDeviceManager.resetUsbGadget();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void resetUsbPort(String portId, int operationId, IUsbOperationInternal callback) {
        Objects.requireNonNull(portId, "resetUsbPort: portId must not be null. opId:" + operationId);
        Objects.requireNonNull(callback, "resetUsbPort: callback must not be null. opId:" + operationId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            UsbPortManager usbPortManager = this.mPortManager;
            if (usbPortManager != null) {
                usbPortManager.resetUsbPort(portId, operationId, callback, null);
            } else {
                try {
                    callback.onOperationComplete(1);
                } catch (RemoteException e) {
                    Slog.e(TAG, "resetUsbPort: Failed to call onOperationComplete", e);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public List<ParcelableUsbPort> getPorts() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            UsbPortManager usbPortManager = this.mPortManager;
            if (usbPortManager == null) {
                return null;
            }
            UsbPort[] ports = usbPortManager.getPorts();
            ArrayList<ParcelableUsbPort> parcelablePorts = new ArrayList<>();
            for (UsbPort usbPort : ports) {
                parcelablePorts.add(ParcelableUsbPort.of(usbPort));
            }
            return parcelablePorts;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public UsbPortStatus getPortStatus(String portId) {
        Objects.requireNonNull(portId, "portId must not be null");
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            UsbPortManager usbPortManager = this.mPortManager;
            return usbPortManager != null ? usbPortManager.getPortStatus(portId) : null;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setPortRoles(String portId, int powerRole, int dataRole) {
        Objects.requireNonNull(portId, "portId must not be null");
        UsbPort.checkRoles(powerRole, dataRole);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            UsbPortManager usbPortManager = this.mPortManager;
            if (usbPortManager != null) {
                usbPortManager.setPortRoles(portId, powerRole, dataRole, null);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void enableLimitPowerTransfer(String portId, boolean limit, int operationId, IUsbOperationInternal callback) {
        Objects.requireNonNull(portId, "portId must not be null. opID:" + operationId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            UsbPortManager usbPortManager = this.mPortManager;
            if (usbPortManager != null) {
                usbPortManager.enableLimitPowerTransfer(portId, limit, operationId, callback, null);
            } else {
                try {
                    callback.onOperationComplete(1);
                } catch (RemoteException e) {
                    Slog.e(TAG, "enableLimitPowerTransfer: Failed to call onOperationComplete", e);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void enableContaminantDetection(String portId, boolean enable) {
        Objects.requireNonNull(portId, "portId must not be null");
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            UsbPortManager usbPortManager = this.mPortManager;
            if (usbPortManager != null) {
                usbPortManager.enableContaminantDetection(portId, enable, null);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getUsbHalVersion() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            UsbPortManager usbPortManager = this.mPortManager;
            if (usbPortManager != null) {
                return usbPortManager.getUsbHalVersion();
            }
            return -1;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean enableUsbData(String portId, boolean enable, int operationId, IUsbOperationInternal callback) {
        boolean wait;
        Objects.requireNonNull(portId, "enableUsbData: portId must not be null. opId:" + operationId);
        Objects.requireNonNull(callback, "enableUsbData: callback must not be null. opId:" + operationId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            UsbPortManager usbPortManager = this.mPortManager;
            if (usbPortManager != null) {
                wait = usbPortManager.enableUsbData(portId, enable, operationId, callback, null);
            } else {
                wait = false;
                try {
                    callback.onOperationComplete(1);
                } catch (RemoteException e) {
                    Slog.e(TAG, "enableUsbData: Failed to call onOperationComplete", e);
                }
            }
            return wait;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void enableUsbDataWhileDocked(String portId, int operationId, IUsbOperationInternal callback) {
        Objects.requireNonNull(portId, "enableUsbDataWhileDocked: portId must not be null. opId:" + operationId);
        Objects.requireNonNull(callback, "enableUsbDataWhileDocked: callback must not be null. opId:" + operationId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        long ident = Binder.clearCallingIdentity();
        try {
            UsbPortManager usbPortManager = this.mPortManager;
            if (usbPortManager != null) {
                usbPortManager.enableUsbDataWhileDocked(portId, operationId, callback, null);
            } else {
                try {
                    callback.onOperationComplete(1);
                } catch (RemoteException e) {
                    Slog.e(TAG, "enableUsbData: Failed to call onOperationComplete", e);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setUsbDeviceConnectionHandler(ComponentName usbDeviceConnectionHandler) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        synchronized (this.mLock) {
            if (this.mCurrentUserId == UserHandle.getCallingUserId()) {
                UsbHostManager usbHostManager = this.mHostManager;
                if (usbHostManager != null) {
                    usbHostManager.setUsbDeviceConnectionHandler(usbDeviceConnectionHandler);
                }
            } else {
                throw new IllegalArgumentException("Only the current user can register a usb connection handler");
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1201=8] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @NeverCompile
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        DualDumpOutputStream dump;
        String str;
        boolean z;
        int mode;
        boolean z2;
        int powerRole;
        boolean z3;
        int dataRole;
        char c;
        int supportedModes;
        char c2;
        int powerRole2;
        char c3;
        int dataRole2;
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            return;
        }
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        long ident = Binder.clearCallingIdentity();
        try {
            ArraySet<String> argsSet = new ArraySet<>();
            Collections.addAll(argsSet, args);
            boolean dumpAsProto = argsSet.contains("--proto");
            try {
                if (args != null && args.length != 0 && !args[0].equals("-a")) {
                    if (!dumpAsProto) {
                        if ("set-port-roles".equals(args[0]) && args.length == 4) {
                            String portId = args[1];
                            String str2 = args[2];
                            switch (str2.hashCode()) {
                                case -896505829:
                                    if (str2.equals("source")) {
                                        c2 = 0;
                                        break;
                                    }
                                    c2 = 65535;
                                    break;
                                case -440560135:
                                    if (str2.equals("no-power")) {
                                        c2 = 2;
                                        break;
                                    }
                                    c2 = 65535;
                                    break;
                                case 3530387:
                                    if (str2.equals("sink")) {
                                        c2 = 1;
                                        break;
                                    }
                                    c2 = 65535;
                                    break;
                                default:
                                    c2 = 65535;
                                    break;
                            }
                            switch (c2) {
                                case 0:
                                    powerRole2 = 1;
                                    break;
                                case 1:
                                    powerRole2 = 2;
                                    break;
                                case 2:
                                    powerRole2 = 0;
                                    break;
                                default:
                                    pw.println("Invalid power role: " + args[2]);
                                    Binder.restoreCallingIdentity(ident);
                                    return;
                            }
                            String str3 = args[3];
                            switch (str3.hashCode()) {
                                case -1335157162:
                                    if (str3.equals("device")) {
                                        c3 = 1;
                                        break;
                                    }
                                    c3 = 65535;
                                    break;
                                case 3208616:
                                    if (str3.equals("host")) {
                                        c3 = 0;
                                        break;
                                    }
                                    c3 = 65535;
                                    break;
                                case 2063627318:
                                    if (str3.equals("no-data")) {
                                        c3 = 2;
                                        break;
                                    }
                                    c3 = 65535;
                                    break;
                                default:
                                    c3 = 65535;
                                    break;
                            }
                            switch (c3) {
                                case 0:
                                    dataRole2 = 1;
                                    break;
                                case 1:
                                    dataRole2 = 2;
                                    break;
                                case 2:
                                    dataRole2 = 0;
                                    break;
                                default:
                                    pw.println("Invalid data role: " + args[3]);
                                    Binder.restoreCallingIdentity(ident);
                                    return;
                            }
                            UsbPortManager usbPortManager = this.mPortManager;
                            if (usbPortManager != null) {
                                usbPortManager.setPortRoles(portId, powerRole2, dataRole2, pw);
                                pw.println();
                                this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), "", 0L);
                            }
                        } else {
                            if ("add-port".equals(args[0])) {
                                str = "  dumpsys usb reset";
                                if (args.length == 3) {
                                    String portId2 = args[1];
                                    String str4 = args[2];
                                    switch (str4.hashCode()) {
                                        case 99374:
                                            if (str4.equals("dfp")) {
                                                c = 1;
                                                break;
                                            }
                                            c = 65535;
                                            break;
                                        case 115711:
                                            if (str4.equals("ufp")) {
                                                c = 0;
                                                break;
                                            }
                                            c = 65535;
                                            break;
                                        case 3094652:
                                            if (str4.equals("dual")) {
                                                c = 2;
                                                break;
                                            }
                                            c = 65535;
                                            break;
                                        case 3387192:
                                            if (str4.equals("none")) {
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
                                            supportedModes = 1;
                                            break;
                                        case 1:
                                            supportedModes = 2;
                                            break;
                                        case 2:
                                            supportedModes = 3;
                                            break;
                                        case 3:
                                            supportedModes = 0;
                                            break;
                                        default:
                                            pw.println("Invalid mode: " + args[2]);
                                            Binder.restoreCallingIdentity(ident);
                                            return;
                                    }
                                    UsbPortManager usbPortManager2 = this.mPortManager;
                                    if (usbPortManager2 != null) {
                                        usbPortManager2.addSimulatedPort(portId2, supportedModes, pw);
                                        pw.println();
                                        this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), "", 0L);
                                    }
                                }
                            } else {
                                str = "  dumpsys usb reset";
                            }
                            if ("connect-port".equals(args[0]) && args.length == 5) {
                                String portId3 = args[1];
                                boolean canChangeMode = args[2].endsWith("?");
                                String removeLastChar = canChangeMode ? removeLastChar(args[2]) : args[2];
                                switch (removeLastChar.hashCode()) {
                                    case 99374:
                                        if (removeLastChar.equals("dfp")) {
                                            z = true;
                                            break;
                                        }
                                        z = true;
                                        break;
                                    case 115711:
                                        if (removeLastChar.equals("ufp")) {
                                            z = false;
                                            break;
                                        }
                                        z = true;
                                        break;
                                    default:
                                        z = true;
                                        break;
                                }
                                switch (z) {
                                    case false:
                                        mode = 1;
                                        break;
                                    case true:
                                        mode = 2;
                                        break;
                                    default:
                                        pw.println("Invalid mode: " + args[2]);
                                        Binder.restoreCallingIdentity(ident);
                                        return;
                                }
                                boolean canChangePowerRole = args[3].endsWith("?");
                                String removeLastChar2 = canChangePowerRole ? removeLastChar(args[3]) : args[3];
                                switch (removeLastChar2.hashCode()) {
                                    case -896505829:
                                        if (removeLastChar2.equals("source")) {
                                            z2 = false;
                                            break;
                                        }
                                        z2 = true;
                                        break;
                                    case 3530387:
                                        if (removeLastChar2.equals("sink")) {
                                            z2 = true;
                                            break;
                                        }
                                        z2 = true;
                                        break;
                                    default:
                                        z2 = true;
                                        break;
                                }
                                switch (z2) {
                                    case false:
                                        powerRole = 1;
                                        break;
                                    case true:
                                        powerRole = 2;
                                        break;
                                    default:
                                        pw.println("Invalid power role: " + args[3]);
                                        Binder.restoreCallingIdentity(ident);
                                        return;
                                }
                                boolean canChangeDataRole = args[4].endsWith("?");
                                String removeLastChar3 = canChangeDataRole ? removeLastChar(args[4]) : args[4];
                                switch (removeLastChar3.hashCode()) {
                                    case -1335157162:
                                        if (removeLastChar3.equals("device")) {
                                            z3 = true;
                                            break;
                                        }
                                        z3 = true;
                                        break;
                                    case 3208616:
                                        if (removeLastChar3.equals("host")) {
                                            z3 = false;
                                            break;
                                        }
                                        z3 = true;
                                        break;
                                    default:
                                        z3 = true;
                                        break;
                                }
                                switch (z3) {
                                    case false:
                                        dataRole = 1;
                                        break;
                                    case true:
                                        dataRole = 2;
                                        break;
                                    default:
                                        pw.println("Invalid data role: " + args[4]);
                                        Binder.restoreCallingIdentity(ident);
                                        return;
                                }
                                UsbPortManager usbPortManager3 = this.mPortManager;
                                if (usbPortManager3 != null) {
                                    usbPortManager3.connectSimulatedPort(portId3, mode, canChangeMode, powerRole, canChangePowerRole, dataRole, canChangeDataRole, pw);
                                    pw.println();
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), "", 0L);
                                }
                            } else if ("disconnect-port".equals(args[0]) && args.length == 2) {
                                String portId4 = args[1];
                                UsbPortManager usbPortManager4 = this.mPortManager;
                                if (usbPortManager4 != null) {
                                    usbPortManager4.disconnectSimulatedPort(portId4, pw);
                                    pw.println();
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), "", 0L);
                                }
                            } else if ("remove-port".equals(args[0]) && args.length == 2) {
                                String portId5 = args[1];
                                UsbPortManager usbPortManager5 = this.mPortManager;
                                if (usbPortManager5 != null) {
                                    usbPortManager5.removeSimulatedPort(portId5, pw);
                                    pw.println();
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), "", 0L);
                                }
                            } else if ("reset".equals(args[0]) && args.length == 1) {
                                UsbPortManager usbPortManager6 = this.mPortManager;
                                if (usbPortManager6 != null) {
                                    usbPortManager6.resetSimulation(pw);
                                    pw.println();
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), "", 0L);
                                }
                            } else if ("set-contaminant-status".equals(args[0]) && args.length == 3) {
                                String portId6 = args[1];
                                Boolean wet = Boolean.valueOf(Boolean.parseBoolean(args[2]));
                                UsbPortManager usbPortManager7 = this.mPortManager;
                                if (usbPortManager7 != null) {
                                    usbPortManager7.simulateContaminantStatus(portId6, wet.booleanValue(), pw);
                                    pw.println();
                                    this.mPortManager.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), "", 0L);
                                }
                            } else if ("ports".equals(args[0]) && args.length == 1) {
                                UsbPortManager usbPortManager8 = this.mPortManager;
                                if (usbPortManager8 != null) {
                                    usbPortManager8.dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), "", 0L);
                                }
                            } else if ("dump-descriptors".equals(args[0])) {
                                this.mHostManager.dumpDescriptors(pw, args);
                            } else {
                                pw.println("Dump current USB state or issue command:");
                                pw.println("  ports");
                                pw.println("  set-port-roles <id> <source|sink|no-power> <host|device|no-data>");
                                pw.println("  add-port <id> <ufp|dfp|dual|none>");
                                pw.println("  connect-port <id> <ufp|dfp><?> <source|sink><?> <host|device><?>");
                                pw.println("    (add ? suffix if mode, power role, or data role can be changed)");
                                pw.println("  disconnect-port <id>");
                                pw.println("  remove-port <id>");
                                pw.println("  reset");
                                pw.println();
                                pw.println("Example USB type C port role switch:");
                                pw.println("  dumpsys usb set-port-roles \"default\" source device");
                                pw.println();
                                pw.println("Example USB type C port simulation with full capabilities:");
                                pw.println("  dumpsys usb add-port \"matrix\" dual");
                                pw.println("  dumpsys usb connect-port \"matrix\" ufp? sink? device?");
                                pw.println("  dumpsys usb ports");
                                pw.println("  dumpsys usb disconnect-port \"matrix\"");
                                pw.println("  dumpsys usb remove-port \"matrix\"");
                                String str5 = str;
                                pw.println(str5);
                                pw.println();
                                pw.println("Example USB type C port where only power role can be changed:");
                                pw.println("  dumpsys usb add-port \"matrix\" dual");
                                pw.println("  dumpsys usb connect-port \"matrix\" dfp source? host");
                                pw.println(str5);
                                pw.println();
                                pw.println("Example USB OTG port where id pin determines function:");
                                pw.println("  dumpsys usb add-port \"matrix\" dual");
                                pw.println("  dumpsys usb connect-port \"matrix\" dfp source host");
                                pw.println(str5);
                                pw.println();
                                pw.println("Example USB device-only port:");
                                pw.println("  dumpsys usb add-port \"matrix\" ufp");
                                pw.println("  dumpsys usb connect-port \"matrix\" ufp sink device");
                                pw.println(str5);
                                pw.println();
                                pw.println("Example simulate contaminant status:");
                                pw.println("  dumpsys usb add-port \"matrix\" ufp");
                                pw.println("  dumpsys usb set-contaminant-status \"matrix\" true");
                                pw.println("  dumpsys usb set-contaminant-status \"matrix\" false");
                                pw.println();
                                pw.println("Example USB device descriptors:");
                                pw.println("  dumpsys usb dump-descriptors -dump-short");
                                pw.println("  dumpsys usb dump-descriptors -dump-tree");
                                pw.println("  dumpsys usb dump-descriptors -dump-list");
                                pw.println("  dumpsys usb dump-descriptors -dump-raw");
                            }
                        }
                        Binder.restoreCallingIdentity(ident);
                    }
                }
                if (dumpAsProto) {
                    dump = new DualDumpOutputStream(new ProtoOutputStream(fd));
                } else {
                    pw.println("USB MANAGER STATE (dumpsys usb):");
                    dump = new DualDumpOutputStream(new IndentingPrintWriter(pw, "  "));
                }
                UsbDeviceManager usbDeviceManager = this.mDeviceManager;
                if (usbDeviceManager != null) {
                    usbDeviceManager.dump(dump, "device_manager", 1146756268033L);
                }
                UsbHostManager usbHostManager = this.mHostManager;
                if (usbHostManager != null) {
                    usbHostManager.dump(dump, "host_manager", 1146756268034L);
                }
                UsbPortManager usbPortManager9 = this.mPortManager;
                if (usbPortManager9 != null) {
                    usbPortManager9.dump(dump, "port_manager", 1146756268035L);
                }
                this.mAlsaManager.dump(dump, "alsa_manager", 1146756268036L);
                this.mSettingsManager.dump(dump, "settings_manager", 1146756268037L);
                this.mPermissionManager.dump(dump, "permissions_manager", 1146756268038L);
                dump.flush();
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                th = th;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static String removeLastChar(String value) {
        return value.substring(0, value.length() - 1);
    }
}
