package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.INetd;
import android.net.IVpnManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.UnderlyingNetworkInfo;
import android.net.Uri;
import android.net.VpnProfileState;
import android.net.util.NetdService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.INetworkManagementService;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.net.module.util.PermissionUtils;
import com.android.server.connectivity.Vpn;
import com.android.server.connectivity.VpnProfileStore;
import com.android.server.net.LockdownVpnTracker;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes.dex */
public class VpnManagerService extends IVpnManager.Stub {
    private static final String TAG = VpnManagerService.class.getSimpleName();
    private final ConnectivityManager mCm;
    private final Context mContext;
    private final Dependencies mDeps;
    private final Handler mHandler;
    protected final HandlerThread mHandlerThread;
    private boolean mLockdownEnabled;
    private LockdownVpnTracker mLockdownTracker;
    private final INetworkManagementService mNMS;
    private final INetd mNetd;
    private final Context mUserAllContext;
    private final UserManager mUserManager;
    private final VpnProfileStore mVpnProfileStore;
    protected final SparseArray<Vpn> mVpns = new SparseArray<>();
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.VpnManagerService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VpnManagerService.this.ensureRunningOnHandlerThread();
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            int uid = intent.getIntExtra("android.intent.extra.UID", -1);
            Uri packageData = intent.getData();
            String packageName = packageData != null ? packageData.getSchemeSpecificPart() : null;
            if (LockdownVpnTracker.ACTION_LOCKDOWN_RESET.equals(action)) {
                VpnManagerService.this.onVpnLockdownReset();
            }
            if (userId == -10000) {
                return;
            }
            if ("android.intent.action.USER_STARTED".equals(action)) {
                VpnManagerService.this.onUserStarted(userId);
            } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                VpnManagerService.this.onUserStopped(userId);
            } else if ("android.intent.action.USER_ADDED".equals(action)) {
                VpnManagerService.this.onUserAdded(userId);
            } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                VpnManagerService.this.onUserRemoved(userId);
            } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                VpnManagerService.this.onUserUnlocked(userId);
            } else if ("android.intent.action.PACKAGE_REPLACED".equals(action)) {
                VpnManagerService.this.onPackageReplaced(packageName, uid);
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                boolean isReplacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                VpnManagerService.this.onPackageRemoved(packageName, uid, isReplacing);
            } else {
                Log.wtf(VpnManagerService.TAG, "received unexpected intent: " + action);
            }
        }
    };
    private BroadcastReceiver mUserPresentReceiver = new BroadcastReceiver() { // from class: com.android.server.VpnManagerService.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VpnManagerService.this.ensureRunningOnHandlerThread();
            VpnManagerService.this.updateLockdownVpn();
            context.unregisterReceiver(this);
        }
    };

    /* loaded from: classes.dex */
    public static class Dependencies {
        public int getCallingUid() {
            return Binder.getCallingUid();
        }

        public HandlerThread makeHandlerThread() {
            return new HandlerThread("VpnManagerService");
        }

        public VpnProfileStore getVpnProfileStore() {
            return new VpnProfileStore();
        }

        public INetd getNetd() {
            return NetdService.getInstance();
        }

        public INetworkManagementService getINetworkManagementService() {
            return INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        }
    }

    public VpnManagerService(Context context, Dependencies deps) {
        this.mContext = context;
        this.mDeps = deps;
        HandlerThread makeHandlerThread = deps.makeHandlerThread();
        this.mHandlerThread = makeHandlerThread;
        makeHandlerThread.start();
        this.mHandler = makeHandlerThread.getThreadHandler();
        this.mVpnProfileStore = deps.getVpnProfileStore();
        this.mUserAllContext = context.createContextAsUser(UserHandle.ALL, 0);
        this.mCm = (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
        this.mNMS = deps.getINetworkManagementService();
        this.mNetd = deps.getNetd();
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        registerReceivers();
        log("VpnManagerService starting up");
    }

    public static VpnManagerService create(Context context) {
        return new VpnManagerService(context, new Dependencies());
    }

    public void systemReady() {
        updateLockdownVpn();
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            pw.println("VPNs:");
            pw.increaseIndent();
            synchronized (this.mVpns) {
                for (int i = 0; i < this.mVpns.size(); i++) {
                    pw.println(this.mVpns.keyAt(i) + ": " + this.mVpns.valueAt(i).getPackage());
                }
                pw.decreaseIndent();
            }
        }
    }

    public boolean prepareVpn(String oldPackage, String newPackage, int userId) {
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            Vpn vpn = this.mVpns.get(userId);
            if (vpn != null) {
                return vpn.prepare(oldPackage, newPackage, 1);
            }
            return false;
        }
    }

    public void setVpnPackageAuthorization(String packageName, int userId, int vpnType) {
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn != null) {
                vpn.setPackageAuthorization(packageName, vpnType);
            }
        }
    }

    public ParcelFileDescriptor establishVpn(VpnConfig config) {
        ParcelFileDescriptor establish;
        int user = UserHandle.getUserId(this.mDeps.getCallingUid());
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            establish = this.mVpns.get(user).establish(config);
        }
        return establish;
    }

    public boolean addVpnAddress(String address, int prefixLength) {
        boolean addAddress;
        int user = UserHandle.getUserId(this.mDeps.getCallingUid());
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            addAddress = this.mVpns.get(user).addAddress(address, prefixLength);
        }
        return addAddress;
    }

    public boolean removeVpnAddress(String address, int prefixLength) {
        boolean removeAddress;
        int user = UserHandle.getUserId(this.mDeps.getCallingUid());
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            removeAddress = this.mVpns.get(user).removeAddress(address, prefixLength);
        }
        return removeAddress;
    }

    public boolean setUnderlyingNetworksForVpn(Network[] networks) {
        boolean success;
        int user = UserHandle.getUserId(this.mDeps.getCallingUid());
        synchronized (this.mVpns) {
            success = this.mVpns.get(user).setUnderlyingNetworks(networks);
        }
        return success;
    }

    public boolean provisionVpnProfile(VpnProfile profile, String packageName) {
        boolean provisionVpnProfile;
        int user = UserHandle.getUserId(this.mDeps.getCallingUid());
        synchronized (this.mVpns) {
            provisionVpnProfile = this.mVpns.get(user).provisionVpnProfile(packageName, profile);
        }
        return provisionVpnProfile;
    }

    public void deleteVpnProfile(String packageName) {
        int user = UserHandle.getUserId(this.mDeps.getCallingUid());
        synchronized (this.mVpns) {
            this.mVpns.get(user).deleteVpnProfile(packageName);
        }
    }

    private int getAppUid(String app, int userId) {
        PackageManager pm = this.mContext.getPackageManager();
        long token = Binder.clearCallingIdentity();
        try {
            return pm.getPackageUidAsUser(app, userId);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void verifyCallingUidAndPackage(String packageName, int callingUid) {
        int userId = UserHandle.getUserId(callingUid);
        if (getAppUid(packageName, userId) != callingUid) {
            throw new SecurityException(packageName + " does not belong to uid " + callingUid);
        }
    }

    public String startVpnProfile(String packageName) {
        String startVpnProfile;
        int callingUid = Binder.getCallingUid();
        verifyCallingUidAndPackage(packageName, callingUid);
        int user = UserHandle.getUserId(callingUid);
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            startVpnProfile = this.mVpns.get(user).startVpnProfile(packageName);
        }
        return startVpnProfile;
    }

    public void stopVpnProfile(String packageName) {
        int callingUid = Binder.getCallingUid();
        verifyCallingUidAndPackage(packageName, callingUid);
        int user = UserHandle.getUserId(callingUid);
        synchronized (this.mVpns) {
            this.mVpns.get(user).stopVpnProfile(packageName);
        }
    }

    public VpnProfileState getProvisionedVpnProfileState(String packageName) {
        VpnProfileState provisionedVpnProfileState;
        int callingUid = Binder.getCallingUid();
        verifyCallingUidAndPackage(packageName, callingUid);
        int user = UserHandle.getUserId(callingUid);
        synchronized (this.mVpns) {
            provisionedVpnProfileState = this.mVpns.get(user).getProvisionedVpnProfileState(packageName);
        }
        return provisionedVpnProfileState;
    }

    public void startLegacyVpn(VpnProfile profile) {
        if (Build.VERSION.DEVICE_INITIAL_SDK_INT >= 31 && VpnProfile.isLegacyType(profile.type)) {
            throw new UnsupportedOperationException("Legacy VPN is deprecated");
        }
        int user = UserHandle.getUserId(this.mDeps.getCallingUid());
        ConnectivityManager connectivityManager = this.mCm;
        LinkProperties egress = connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork());
        if (egress == null) {
            throw new IllegalStateException("Missing active network connection");
        }
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            this.mVpns.get(user).startLegacyVpn(profile, null, egress);
        }
    }

    public LegacyVpnInfo getLegacyVpnInfo(int userId) {
        LegacyVpnInfo legacyVpnInfo;
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            legacyVpnInfo = this.mVpns.get(userId).getLegacyVpnInfo();
        }
        return legacyVpnInfo;
    }

    public VpnConfig getVpnConfig(int userId) {
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn != null) {
                return vpn.getVpnConfig();
            }
            return null;
        }
    }

    private boolean isLockdownVpnEnabled() {
        return this.mVpnProfileStore.get("LOCKDOWN_VPN") != null;
    }

    public boolean updateLockdownVpn() {
        if (this.mDeps.getCallingUid() != 1000 && Binder.getCallingPid() != Process.myPid()) {
            logw("Lockdown VPN only available to system process or AID_SYSTEM");
            return false;
        }
        synchronized (this.mVpns) {
            boolean isLockdownVpnEnabled = isLockdownVpnEnabled();
            this.mLockdownEnabled = isLockdownVpnEnabled;
            if (!isLockdownVpnEnabled) {
                setLockdownTracker(null);
                return true;
            }
            byte[] profileTag = this.mVpnProfileStore.get("LOCKDOWN_VPN");
            if (profileTag == null) {
                loge("Lockdown VPN configured but cannot be read from keystore");
                return false;
            }
            String profileName = new String(profileTag);
            VpnProfile profile = VpnProfile.decode(profileName, this.mVpnProfileStore.get("VPN_" + profileName));
            if (profile == null) {
                loge("Lockdown VPN configured invalid profile " + profileName);
                setLockdownTracker(null);
                return true;
            }
            int user = UserHandle.getUserId(this.mDeps.getCallingUid());
            Vpn vpn = this.mVpns.get(user);
            if (vpn == null) {
                logw("VPN for user " + user + " not ready yet. Skipping lockdown");
                return false;
            }
            setLockdownTracker(new LockdownVpnTracker(this.mContext, this.mHandler, vpn, profile));
            return true;
        }
    }

    private void setLockdownTracker(LockdownVpnTracker tracker) {
        LockdownVpnTracker existing = this.mLockdownTracker;
        this.mLockdownTracker = null;
        if (existing != null) {
            existing.shutdown();
        }
        if (tracker != null) {
            this.mLockdownTracker = tracker;
            tracker.init();
        }
    }

    private void throwIfLockdownEnabled() {
        if (this.mLockdownEnabled) {
            throw new IllegalStateException("Unavailable in lockdown mode");
        }
    }

    private boolean startAlwaysOnVpn(int userId) {
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                Log.wtf(TAG, "User " + userId + " has no Vpn configuration");
                return false;
            }
            return vpn.startAlwaysOnVpn();
        }
    }

    public boolean isAlwaysOnVpnPackageSupported(int userId, String packageName) {
        enforceSettingsPermission();
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                logw("User " + userId + " has no Vpn configuration");
                return false;
            }
            return vpn.isAlwaysOnPackageSupported(packageName);
        }
    }

    public boolean setAlwaysOnVpnPackage(int userId, String packageName, boolean lockdown, List<String> lockdownAllowlist) {
        enforceControlAlwaysOnVpnPermission();
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            if (isLockdownVpnEnabled()) {
                return false;
            }
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                logw("User " + userId + " has no Vpn configuration");
                return false;
            } else if (vpn.setAlwaysOnPackage(packageName, lockdown, lockdownAllowlist)) {
                if (!startAlwaysOnVpn(userId)) {
                    vpn.setAlwaysOnPackage(null, false, null);
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public String getAlwaysOnVpnPackage(int userId) {
        enforceControlAlwaysOnVpnPermission();
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                logw("User " + userId + " has no Vpn configuration");
                return null;
            }
            return vpn.getAlwaysOnPackage();
        }
    }

    public boolean isVpnLockdownEnabled(int userId) {
        enforceControlAlwaysOnVpnPermission();
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                logw("User " + userId + " has no Vpn configuration");
                return false;
            }
            return vpn.getLockdown();
        }
    }

    public List<String> getVpnLockdownAllowlist(int userId) {
        enforceControlAlwaysOnVpnPermission();
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                logw("User " + userId + " has no Vpn configuration");
                return null;
            }
            return vpn.getLockdownAllowlist();
        }
    }

    private Vpn getVpnIfOwner() {
        return getVpnIfOwner(this.mDeps.getCallingUid());
    }

    private Vpn getVpnIfOwner(int uid) {
        UnderlyingNetworkInfo info;
        int user = UserHandle.getUserId(uid);
        Vpn vpn = this.mVpns.get(user);
        if (vpn == null || (info = vpn.getUnderlyingNetworkInfo()) == null || info.getOwnerUid() != uid) {
            return null;
        }
        return vpn;
    }

    private void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_STARTED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mUserAllContext.registerReceiver(this.mIntentReceiver, intentFilter, null, this.mHandler);
        this.mContext.createContextAsUser(UserHandle.SYSTEM, 0).registerReceiver(this.mUserPresentReceiver, new IntentFilter("android.intent.action.USER_PRESENT"), null, this.mHandler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme("package");
        this.mUserAllContext.registerReceiver(this.mIntentReceiver, intentFilter2, null, this.mHandler);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction(LockdownVpnTracker.ACTION_LOCKDOWN_RESET);
        this.mUserAllContext.registerReceiver(this.mIntentReceiver, intentFilter3, "android.permission.NETWORK_STACK", this.mHandler, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserStarted(int userId) {
        synchronized (this.mVpns) {
            Vpn userVpn = this.mVpns.get(userId);
            if (userVpn != null) {
                loge("Starting user already has a VPN");
                return;
            }
            Vpn userVpn2 = new Vpn(this.mHandler.getLooper(), this.mContext, this.mNMS, this.mNetd, userId, new VpnProfileStore());
            this.mVpns.put(userId, userVpn2);
            if (this.mUserManager.getUserInfo(userId).isPrimary() && isLockdownVpnEnabled()) {
                updateLockdownVpn();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserStopped(int userId) {
        synchronized (this.mVpns) {
            Vpn userVpn = this.mVpns.get(userId);
            if (userVpn == null) {
                loge("Stopped user has no VPN");
                return;
            }
            userVpn.onUserStopped();
            this.mVpns.delete(userId);
        }
    }

    public boolean isCallerCurrentAlwaysOnVpnApp() {
        boolean z;
        synchronized (this.mVpns) {
            Vpn vpn = getVpnIfOwner();
            z = vpn != null && vpn.getAlwaysOn();
        }
        return z;
    }

    public boolean isCallerCurrentAlwaysOnVpnLockdownApp() {
        boolean z;
        synchronized (this.mVpns) {
            Vpn vpn = getVpnIfOwner();
            z = vpn != null && vpn.getLockdown();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserAdded(int userId) {
        synchronized (this.mVpns) {
            int vpnsSize = this.mVpns.size();
            for (int i = 0; i < vpnsSize; i++) {
                Vpn vpn = this.mVpns.valueAt(i);
                vpn.onUserAdded(userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserRemoved(int userId) {
        synchronized (this.mVpns) {
            int vpnsSize = this.mVpns.size();
            for (int i = 0; i < vpnsSize; i++) {
                Vpn vpn = this.mVpns.valueAt(i);
                vpn.onUserRemoved(userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageReplaced(String packageName, int uid) {
        if (TextUtils.isEmpty(packageName) || uid < 0) {
            Log.wtf(TAG, "Invalid package in onPackageReplaced: " + packageName + " | " + uid);
            return;
        }
        int userId = UserHandle.getUserId(uid);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                return;
            }
            if (TextUtils.equals(vpn.getAlwaysOnPackage(), packageName)) {
                log("Restarting always-on VPN package " + packageName + " for user " + userId);
                vpn.startAlwaysOnVpn();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageRemoved(String packageName, int uid, boolean isReplacing) {
        if (TextUtils.isEmpty(packageName) || uid < 0) {
            Log.wtf(TAG, "Invalid package in onPackageRemoved: " + packageName + " | " + uid);
            return;
        }
        int userId = UserHandle.getUserId(uid);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                return;
            }
            if (TextUtils.equals(vpn.getAlwaysOnPackage(), packageName) && !isReplacing) {
                log("Removing always-on VPN package " + packageName + " for user " + userId);
                vpn.setAlwaysOnPackage(null, false, null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserUnlocked(int userId) {
        synchronized (this.mVpns) {
            if (this.mUserManager.getUserInfo(userId).isPrimary() && isLockdownVpnEnabled()) {
                updateLockdownVpn();
            } else {
                startAlwaysOnVpn(userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onVpnLockdownReset() {
        synchronized (this.mVpns) {
            LockdownVpnTracker lockdownVpnTracker = this.mLockdownTracker;
            if (lockdownVpnTracker != null) {
                lockdownVpnTracker.reset();
            }
        }
    }

    public boolean setAppExclusionList(int userId, String vpnPackage, List<String> excludedApps) {
        boolean appExclusionList;
        enforceSettingsPermission();
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn != null) {
                appExclusionList = vpn.setAppExclusionList(vpnPackage, excludedApps);
            } else {
                logw("User " + userId + " has no Vpn configuration");
                throw new IllegalStateException("VPN for user " + userId + " not ready yet. Skipping setting the list");
            }
        }
        return appExclusionList;
    }

    public List<String> getAppExclusionList(int userId, String vpnPackage) {
        enforceSettingsPermission();
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn != null) {
                return vpn.getAppExclusionList(vpnPackage);
            }
            logw("User " + userId + " has no Vpn configuration");
            return null;
        }
    }

    public void factoryReset() {
        enforceSettingsPermission();
        if (this.mUserManager.hasUserRestriction("no_network_reset") || this.mUserManager.hasUserRestriction("no_config_vpn")) {
            return;
        }
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mVpns) {
            String alwaysOnPackage = getAlwaysOnVpnPackage(userId);
            if (alwaysOnPackage != null) {
                setAlwaysOnVpnPackage(userId, null, false, null);
                setVpnPackageAuthorization(alwaysOnPackage, userId, -1);
            }
            if (this.mLockdownEnabled && userId == 0) {
                long ident = Binder.clearCallingIdentity();
                this.mVpnProfileStore.remove("LOCKDOWN_VPN");
                this.mLockdownEnabled = false;
                setLockdownTracker(null);
                Binder.restoreCallingIdentity(ident);
            }
            VpnConfig vpnConfig = getVpnConfig(userId);
            if (vpnConfig != null) {
                if (!vpnConfig.legacy) {
                    setVpnPackageAuthorization(vpnConfig.user, userId, -1);
                    prepareVpn(null, "[Legacy VPN]", userId);
                } else {
                    prepareVpn("[Legacy VPN]", "[Legacy VPN]", userId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ensureRunningOnHandlerThread() {
        if (this.mHandler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Not running on VpnManagerService thread: " + Thread.currentThread().getName());
        }
    }

    private void enforceControlAlwaysOnVpnPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_ALWAYS_ON_VPN", "VpnManagerService");
    }

    private void enforceCrossUserPermission(int userId) {
        if (userId == UserHandle.getCallingUserId()) {
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "VpnManagerService");
    }

    private void enforceSettingsPermission() {
        PermissionUtils.enforceAnyPermissionOf(this.mContext, new String[]{"android.permission.NETWORK_SETTINGS", "android.permission.MAINLINE_NETWORK_STACK"});
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }

    private static void logw(String s) {
        Log.w(TAG, s);
    }

    private static void loge(String s) {
        Log.e(TAG, s);
    }
}
