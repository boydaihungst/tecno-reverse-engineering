package com.android.server.connectivity;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.ConnectivityManager;
import android.net.DnsResolver;
import android.net.INetd;
import android.net.INetworkManagementEventObserver;
import android.net.Ikev2VpnProfile;
import android.net.InetAddresses;
import android.net.IpPrefix;
import android.net.IpSecManager;
import android.net.IpSecTransform;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkProvider;
import android.net.NetworkRequest;
import android.net.NetworkScore;
import android.net.RouteInfo;
import android.net.UidRangeParcel;
import android.net.UnderlyingNetworkInfo;
import android.net.VpnProfileState;
import android.net.VpnTransportInfo;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.ChildSessionConfiguration;
import android.net.ipsec.ike.ChildSessionParams;
import android.net.ipsec.ike.IkeSession;
import android.net.ipsec.ike.IkeSessionCallback;
import android.net.ipsec.ike.IkeSessionParams;
import android.net.ipsec.ike.IkeTunnelConnectionParams;
import android.net.ipsec.ike.exceptions.IkeNetworkLostException;
import android.net.ipsec.ike.exceptions.IkeNonProtocolException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.IkeTimeoutException;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemService;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore2;
import android.security.KeyStoreException;
import android.system.keystore2.KeyDescriptor;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Range;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.modules.utils.build.SdkLevel;
import com.android.net.module.util.NetdUtils;
import com.android.net.module.util.NetworkStackConstants;
import com.android.server.DeviceIdleInternal;
import com.android.server.LocalServices;
import com.android.server.connectivity.Vpn;
import com.android.server.connectivity.VpnIkev2Utils;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.vcn.util.PersistableBundleUtils;
import com.transsion.griffin.Griffin;
import com.transsion.hubcore.server.connectivity.ITranVpn;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public class Vpn {
    private static final String ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String LOCKDOWN_ALLOWLIST_SETTING_NAME = "always_on_vpn_lockdown_whitelist";
    private static final boolean LOGD = true;
    static final int MAX_VPN_PROFILE_SIZE_BYTES = 131072;
    private static final String NETWORKTYPE = "VPN";
    private static final String TAG = "Vpn";
    static final String VPN_APP_EXCLUDED = "VPN_APP_EXCLUDED_";
    private static final int VPN_DEFAULT_SCORE = 101;
    private static final long VPN_LAUNCH_IDLE_ALLOWLIST_DURATION_MS = 60000;
    private static final long VPN_MANAGER_EVENT_ALLOWLIST_DURATION_MS = 30000;
    private static final String VPN_PROVIDER_NAME_BASE = "VpnNetworkProvider:";
    protected boolean mAlwaysOn;
    private final AppOpsManager mAppOpsManager;
    private final Set<UidRangeParcel> mBlockedUidsAsToldToConnectivity;
    protected VpnConfig mConfig;
    private Connection mConnection;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    final Dependencies mDeps;
    private volatile boolean mEnableTeardown;
    private final Ikev2SessionCreator mIkev2SessionCreator;
    protected String mInterface;
    private boolean mIsPackageTargetingAtLeastQ;
    private int mLegacyState;
    protected boolean mLockdown;
    private List<String> mLockdownAllowlist;
    private final Looper mLooper;
    private final INetd mNetd;
    protected NetworkAgent mNetworkAgent;
    protected NetworkCapabilities mNetworkCapabilities;
    private final NetworkInfo mNetworkInfo;
    private final NetworkProvider mNetworkProvider;
    private final INetworkManagementService mNms;
    private INetworkManagementEventObserver mObserver;
    private int mOwnerUID;
    protected String mPackage;
    private PendingIntent mStatusIntent;
    private final SystemServices mSystemServices;
    private final int mUserId;
    private final Context mUserIdContext;
    private final UserManager mUserManager;
    private final VpnProfileStore mVpnProfileStore;
    protected VpnRunner mVpnRunner;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface IkeV2VpnRunnerCallback {
        void onChildOpened(Network network, ChildSessionConfiguration childSessionConfiguration);

        void onChildTransformCreated(Network network, IpSecTransform ipSecTransform, int i);

        void onDefaultNetworkCapabilitiesChanged(NetworkCapabilities networkCapabilities);

        void onDefaultNetworkChanged(Network network);

        void onDefaultNetworkLinkPropertiesChanged(LinkProperties linkProperties);

        void onSessionLost(Network network, Exception exc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface RetryScheduler {
        void checkInterruptAndDelay(boolean z) throws InterruptedException;
    }

    private native boolean jniAddAddress(String str, String str2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public native int jniCheck(String str);

    /* JADX INFO: Access modifiers changed from: private */
    public native int jniCreate(int i);

    private native boolean jniDelAddress(String str, String str2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public native String jniGetName(int i);

    private native void jniReset(String str);

    /* JADX INFO: Access modifiers changed from: private */
    public native int jniSetAddresses(String str, String str2);

    VpnProfileStore getVpnProfileStore() {
        return this.mVpnProfileStore;
    }

    /* loaded from: classes.dex */
    public static class Dependencies {
        public boolean isCallerSystem() {
            return Binder.getCallingUid() == 1000;
        }

        public void startService(String serviceName) {
            SystemService.start(serviceName);
        }

        public void stopService(String serviceName) {
            SystemService.stop(serviceName);
        }

        public boolean isServiceRunning(String serviceName) {
            return SystemService.isRunning(serviceName);
        }

        public boolean isServiceStopped(String serviceName) {
            return SystemService.isStopped(serviceName);
        }

        public File getStateFile() {
            return new File("/data/misc/vpn/state");
        }

        public DeviceIdleInternal getDeviceIdleInternal() {
            return (DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class);
        }

        public PendingIntent getIntentForStatusPanel(Context context) {
            return VpnConfig.getIntentForStatusPanel(context);
        }

        public void sendArgumentsToDaemon(String daemon, LocalSocket socket, String[] arguments, RetryScheduler retryScheduler) throws IOException, InterruptedException {
            LocalSocketAddress address = new LocalSocketAddress(daemon, LocalSocketAddress.Namespace.RESERVED);
            while (true) {
                try {
                    socket.connect(address);
                    break;
                } catch (Exception e) {
                    retryScheduler.checkInterruptAndDelay(true);
                }
            }
            socket.setSoTimeout(500);
            OutputStream out = socket.getOutputStream();
            for (String argument : arguments) {
                byte[] bytes = argument.getBytes(StandardCharsets.UTF_8);
                if (bytes.length >= 65535) {
                    throw new IllegalArgumentException("Argument is too large");
                }
                out.write(bytes.length >> 8);
                out.write(bytes.length);
                out.write(bytes);
                retryScheduler.checkInterruptAndDelay(false);
            }
            out.write(255);
            out.write(255);
            InputStream in = socket.getInputStream();
            while (in.read() != -1) {
                retryScheduler.checkInterruptAndDelay(true);
            }
        }

        public InetAddress resolve(final String endpoint) throws ExecutionException, InterruptedException {
            try {
                return InetAddresses.parseNumericAddress(endpoint);
            } catch (IllegalArgumentException e) {
                CancellationSignal cancellationSignal = new CancellationSignal();
                try {
                    DnsResolver resolver = DnsResolver.getInstance();
                    final CompletableFuture<InetAddress> result = new CompletableFuture<>();
                    DnsResolver.Callback<List<InetAddress>> cb = new DnsResolver.Callback<List<InetAddress>>() { // from class: com.android.server.connectivity.Vpn.Dependencies.1
                        /* JADX DEBUG: Method merged with bridge method */
                        @Override // android.net.DnsResolver.Callback
                        public void onAnswer(List<InetAddress> answer, int rcode) {
                            if (answer.size() > 0) {
                                result.complete(answer.get(0));
                            } else {
                                result.completeExceptionally(new UnknownHostException(endpoint));
                            }
                        }

                        @Override // android.net.DnsResolver.Callback
                        public void onError(DnsResolver.DnsException error) {
                            Log.e(Vpn.TAG, "Async dns resolver error : " + error);
                            result.completeExceptionally(new UnknownHostException(endpoint));
                        }
                    };
                    resolver.query(null, endpoint, 0, new Executor() { // from class: com.android.server.connectivity.Vpn$Dependencies$$ExternalSyntheticLambda0
                        @Override // java.util.concurrent.Executor
                        public final void execute(Runnable runnable) {
                            runnable.run();
                        }
                    }, cancellationSignal, cb);
                    return result.get();
                } catch (InterruptedException e2) {
                    Log.e(Vpn.TAG, "Legacy VPN was interrupted while resolving the endpoint", e2);
                    cancellationSignal.cancel();
                    throw e2;
                } catch (ExecutionException e3) {
                    Log.e(Vpn.TAG, "Cannot resolve VPN endpoint : " + endpoint + ".", e3);
                    throw e3;
                }
            }
        }

        public boolean isInterfacePresent(Vpn vpn, String iface) {
            return vpn.jniCheck(iface) != 0;
        }

        public ParcelFileDescriptor adoptFd(Vpn vpn, int mtu) {
            return ParcelFileDescriptor.adoptFd(jniCreate(vpn, mtu));
        }

        public int jniCreate(Vpn vpn, int mtu) {
            return vpn.jniCreate(mtu);
        }

        public String jniGetName(Vpn vpn, int fd) {
            return vpn.jniGetName(fd);
        }

        public int jniSetAddresses(Vpn vpn, String interfaze, String addresses) {
            return vpn.jniSetAddresses(interfaze, addresses);
        }

        public void setBlocking(FileDescriptor fd, boolean blocking) {
            try {
                IoUtils.setBlocking(fd, blocking);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot set tunnel's fd as blocking=" + blocking, e);
            }
        }
    }

    public Vpn(Looper looper, Context context, INetworkManagementService netService, INetd netd, int userId, VpnProfileStore vpnProfileStore) {
        this(looper, context, new Dependencies(), netService, netd, userId, vpnProfileStore, new SystemServices(context), new Ikev2SessionCreator());
    }

    public Vpn(Looper looper, Context context, Dependencies deps, INetworkManagementService netService, INetd netd, int userId, VpnProfileStore vpnProfileStore) {
        this(looper, context, deps, netService, netd, userId, vpnProfileStore, new SystemServices(context), new Ikev2SessionCreator());
    }

    protected Vpn(Looper looper, Context context, Dependencies deps, INetworkManagementService netService, INetd netd, int userId, VpnProfileStore vpnProfileStore, SystemServices systemServices, Ikev2SessionCreator ikev2SessionCreator) {
        this.mEnableTeardown = true;
        this.mAlwaysOn = false;
        this.mLockdown = false;
        this.mLockdownAllowlist = Collections.emptyList();
        this.mBlockedUidsAsToldToConnectivity = new ArraySet();
        this.mObserver = new BaseNetworkObserver() { // from class: com.android.server.connectivity.Vpn.2
            public void interfaceStatusChanged(String interfaze, boolean up) {
                synchronized (Vpn.this) {
                    if (!up) {
                        if (Vpn.this.mVpnRunner != null && (Vpn.this.mVpnRunner instanceof LegacyVpnRunner)) {
                            ((LegacyVpnRunner) Vpn.this.mVpnRunner).exitIfOuterInterfaceIs(interfaze);
                        }
                    }
                }
            }

            public void interfaceRemoved(String interfaze) {
                synchronized (Vpn.this) {
                    if (interfaze.equals(Vpn.this.mInterface) && Vpn.this.jniCheck(interfaze) == 0) {
                        if (Vpn.this.mConnection != null) {
                            Vpn.this.mAppOpsManager.finishOp("android:establish_vpn_service", Vpn.this.mOwnerUID, Vpn.this.mPackage, null);
                            Vpn.this.mContext.unbindService(Vpn.this.mConnection);
                            Vpn.this.cleanupVpnStateLocked();
                        } else if (Vpn.this.mVpnRunner != null) {
                            if (!"[Legacy VPN]".equals(Vpn.this.mPackage)) {
                                Vpn.this.mAppOpsManager.finishOp("android:establish_vpn_manager", Vpn.this.mOwnerUID, Vpn.this.mPackage, null);
                            }
                            Vpn.this.mVpnRunner.exit();
                        }
                    }
                }
            }
        };
        this.mVpnProfileStore = vpnProfileStore;
        this.mContext = context;
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mUserIdContext = context.createContextAsUser(UserHandle.of(userId), 0);
        this.mDeps = deps;
        this.mNms = netService;
        this.mNetd = netd;
        this.mUserId = userId;
        this.mLooper = looper;
        this.mSystemServices = systemServices;
        this.mIkev2SessionCreator = ikev2SessionCreator;
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mPackage = "[Legacy VPN]";
        this.mOwnerUID = getAppUid("[Legacy VPN]", userId);
        this.mIsPackageTargetingAtLeastQ = doesPackageTargetAtLeastQ(this.mPackage);
        try {
            netService.registerObserver(this.mObserver);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Problem registering observer", e);
        }
        NetworkProvider networkProvider = new NetworkProvider(context, looper, VPN_PROVIDER_NAME_BASE + this.mUserId);
        this.mNetworkProvider = networkProvider;
        this.mConnectivityManager.registerNetworkProvider(networkProvider);
        this.mLegacyState = 0;
        this.mNetworkInfo = new NetworkInfo(17, 0, NETWORKTYPE, "");
        this.mNetworkCapabilities = new NetworkCapabilities.Builder().addTransportType(4).removeCapability(15).addCapability(28).setTransportInfo(new VpnTransportInfo(-1, (String) null)).build();
        loadAlwaysOnPackage();
    }

    public void setEnableTeardown(boolean enableTeardown) {
        this.mEnableTeardown = enableTeardown;
    }

    public boolean getEnableTeardown() {
        return this.mEnableTeardown;
    }

    protected void updateState(NetworkInfo.DetailedState detailedState, String reason) {
        Log.d(TAG, "setting state=" + detailedState + ", reason=" + reason);
        this.mLegacyState = LegacyVpnInfo.stateFromNetworkInfo(detailedState);
        this.mNetworkInfo.setDetailedState(detailedState, reason, null);
        switch (AnonymousClass3.$SwitchMap$android$net$NetworkInfo$DetailedState[detailedState.ordinal()]) {
            case 1:
                NetworkAgent networkAgent = this.mNetworkAgent;
                if (networkAgent != null) {
                    networkAgent.markConnected();
                    break;
                }
                break;
            case 2:
            case 3:
                NetworkAgent networkAgent2 = this.mNetworkAgent;
                if (networkAgent2 != null) {
                    networkAgent2.unregister();
                    this.mNetworkAgent = null;
                    break;
                }
                break;
            case 4:
                if (this.mNetworkAgent != null) {
                    throw new IllegalStateException("VPN can only go to CONNECTING state when the agent is null.");
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal state argument " + detailedState);
        }
        updateAlwaysOnNotification(detailedState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.connectivity.Vpn$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$android$net$NetworkInfo$DetailedState;

        static {
            int[] iArr = new int[NetworkInfo.DetailedState.values().length];
            $SwitchMap$android$net$NetworkInfo$DetailedState = iArr;
            try {
                iArr[NetworkInfo.DetailedState.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.DISCONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.FAILED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.CONNECTING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private void resetNetworkCapabilities() {
        this.mNetworkCapabilities = new NetworkCapabilities.Builder(this.mNetworkCapabilities).setUids((Set) null).setTransportInfo(new VpnTransportInfo(-1, (String) null)).build();
    }

    public synchronized void setLockdown(boolean lockdown) {
        enforceControlPermissionOrInternalCaller();
        setVpnForcedLocked(lockdown);
        this.mLockdown = lockdown;
        if (this.mAlwaysOn) {
            saveAlwaysOnPackage();
        }
    }

    public synchronized String getPackage() {
        return this.mPackage;
    }

    public synchronized boolean getLockdown() {
        return this.mLockdown;
    }

    public synchronized boolean getAlwaysOn() {
        return this.mAlwaysOn;
    }

    public boolean isAlwaysOnPackageSupported(String packageName) {
        enforceSettingsPermission();
        if (packageName == null) {
            return false;
        }
        long oldId = Binder.clearCallingIdentity();
        try {
            if (getVpnProfilePrivileged(packageName) != null) {
                return true;
            }
            Binder.restoreCallingIdentity(oldId);
            PackageManager pm = this.mContext.getPackageManager();
            ApplicationInfo appInfo = null;
            try {
                appInfo = pm.getApplicationInfoAsUser(packageName, 0, this.mUserId);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Can't find \"" + packageName + "\" when checking always-on support");
            }
            if (appInfo == null || appInfo.targetSdkVersion < 24) {
                return false;
            }
            Intent intent = new Intent("android.net.VpnService");
            intent.setPackage(packageName);
            List<ResolveInfo> services = pm.queryIntentServicesAsUser(intent, 128, this.mUserId);
            if (services == null || services.size() == 0) {
                return false;
            }
            for (ResolveInfo rInfo : services) {
                Bundle metaData = rInfo.serviceInfo.metaData;
                if (metaData != null && !metaData.getBoolean("android.net.VpnService.SUPPORTS_ALWAYS_ON", true)) {
                    return false;
                }
            }
            return true;
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean sendEventToVpnManagerApp(String category, int errorClass, int errorCode, String packageName, String sessionKey, VpnProfileState profileState, Network underlyingNetwork, NetworkCapabilities nc, LinkProperties lp) {
        Intent intent = new Intent("android.net.action.VPN_MANAGER_EVENT");
        intent.setPackage(packageName);
        intent.addCategory(category);
        intent.putExtra("android.net.extra.VPN_PROFILE_STATE", (Parcelable) profileState);
        intent.putExtra("android.net.extra.SESSION_KEY", sessionKey);
        intent.putExtra("android.net.extra.UNDERLYING_NETWORK", underlyingNetwork);
        intent.putExtra("android.net.extra.UNDERLYING_NETWORK_CAPABILITIES", nc);
        intent.putExtra("android.net.extra.UNDERLYING_LINK_PROPERTIES", lp);
        intent.putExtra("android.net.extra.TIMESTAMP_MILLIS", System.currentTimeMillis());
        if (!"android.net.category.EVENT_DEACTIVATED_BY_USER".equals(category) || !"android.net.category.EVENT_ALWAYS_ON_STATE_CHANGED".equals(category)) {
            intent.putExtra("android.net.extra.ERROR_CLASS", errorClass);
            intent.putExtra("android.net.extra.ERROR_CODE", errorCode);
        }
        DeviceIdleInternal idleController = this.mDeps.getDeviceIdleInternal();
        idleController.addPowerSaveTempWhitelistApp(Process.myUid(), packageName, 30000L, this.mUserId, false, 309, "VpnManager event");
        try {
            return this.mUserIdContext.startService(intent) != null;
        } catch (RuntimeException e) {
            Log.e(TAG, "Service of VpnManager app " + intent + " failed to start", e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isVpnApp(String packageName) {
        return (packageName == null || "[Legacy VPN]".equals(packageName)) ? false : true;
    }

    /* JADX WARN: Removed duplicated region for block: B:25:0x0042 A[DONT_GENERATE] */
    /* JADX WARN: Removed duplicated region for block: B:27:0x0044 A[Catch: all -> 0x0086, TRY_ENTER, TRY_LEAVE, TryCatch #0 {, blocks: (B:4:0x0003, B:8:0x0015, B:10:0x001c, B:12:0x0020, B:18:0x002d, B:23:0x003a, B:27:0x0044, B:34:0x0058, B:36:0x0063, B:35:0x005e, B:38:0x006e), top: B:44:0x0003 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized boolean setAlwaysOnPackage(String packageName, boolean lockdown, List<String> lockdownAllowlist) {
        boolean z;
        enforceControlPermissionOrInternalCaller();
        String oldPackage = this.mPackage;
        boolean isPackageChanged = !Objects.equals(packageName, oldPackage);
        if (isVpnApp(oldPackage) && this.mAlwaysOn) {
            if (lockdown != this.mLockdown || isPackageChanged) {
                z = true;
                boolean shouldNotifyOldPkg = z;
                boolean shouldNotifyNewPkg = !isVpnApp(packageName) && isPackageChanged;
                if (setAlwaysOnPackageInternal(packageName, lockdown, lockdownAllowlist)) {
                    return false;
                }
                saveAlwaysOnPackage();
                if (SdkLevel.isAtLeastT()) {
                    if (shouldNotifyOldPkg) {
                        sendEventToVpnManagerApp("android.net.category.EVENT_ALWAYS_ON_STATE_CHANGED", -1, -1, oldPackage, null, isPackageChanged ? makeDisconnectedVpnProfileState() : makeVpnProfileStateLocked(), null, null, null);
                    }
                    if (shouldNotifyNewPkg) {
                        sendEventToVpnManagerApp("android.net.category.EVENT_ALWAYS_ON_STATE_CHANGED", -1, -1, packageName, getSessionKeyLocked(), makeVpnProfileStateLocked(), null, null, null);
                    }
                    return true;
                }
                return true;
            }
        }
        z = false;
        boolean shouldNotifyOldPkg2 = z;
        boolean shouldNotifyNewPkg2 = !isVpnApp(packageName) && isPackageChanged;
        if (setAlwaysOnPackageInternal(packageName, lockdown, lockdownAllowlist)) {
        }
    }

    private boolean setAlwaysOnPackageInternal(String packageName, boolean lockdown, List<String> lockdownAllowlist) {
        List<String> emptyList;
        boolean z = false;
        if ("[Legacy VPN]".equals(packageName)) {
            Log.w(TAG, "Not setting legacy VPN \"" + packageName + "\" as always-on.");
            return false;
        }
        if (lockdownAllowlist != null) {
            for (String pkg : lockdownAllowlist) {
                if (pkg.contains(",")) {
                    Log.w(TAG, "Not setting always-on vpn, invalid allowed package: " + pkg);
                    return false;
                }
            }
        }
        if (packageName != null) {
            long oldId = Binder.clearCallingIdentity();
            try {
                VpnProfile profile = getVpnProfilePrivileged(packageName);
                int grantType = profile == null ? 1 : 2;
                if (!setPackageAuthorization(packageName, grantType)) {
                    return false;
                }
                this.mAlwaysOn = true;
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        } else {
            packageName = "[Legacy VPN]";
            this.mAlwaysOn = false;
        }
        if (this.mAlwaysOn && lockdown) {
            z = true;
        }
        this.mLockdown = z;
        if (z && lockdownAllowlist != null) {
            emptyList = Collections.unmodifiableList(new ArrayList(lockdownAllowlist));
        } else {
            emptyList = Collections.emptyList();
        }
        this.mLockdownAllowlist = emptyList;
        if (isCurrentPreparedPackage(packageName)) {
            updateAlwaysOnNotification(this.mNetworkInfo.getDetailedState());
            setVpnForcedLocked(this.mLockdown);
        } else {
            prepareInternal(packageName);
        }
        return true;
    }

    private static boolean isNullOrLegacyVpn(String packageName) {
        return packageName == null || "[Legacy VPN]".equals(packageName);
    }

    public synchronized String getAlwaysOnPackage() {
        enforceControlPermissionOrInternalCaller();
        return this.mAlwaysOn ? this.mPackage : null;
    }

    public synchronized List<String> getLockdownAllowlist() {
        return this.mLockdown ? this.mLockdownAllowlist : null;
    }

    private void saveAlwaysOnPackage() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mSystemServices.settingsSecurePutStringForUser("always_on_vpn_app", getAlwaysOnPackage(), this.mUserId);
            this.mSystemServices.settingsSecurePutIntForUser("always_on_vpn_lockdown", (this.mAlwaysOn && this.mLockdown) ? 1 : 0, this.mUserId);
            this.mSystemServices.settingsSecurePutStringForUser(LOCKDOWN_ALLOWLIST_SETTING_NAME, String.join(",", this.mLockdownAllowlist), this.mUserId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void loadAlwaysOnPackage() {
        long token = Binder.clearCallingIdentity();
        try {
            String alwaysOnPackage = this.mSystemServices.settingsSecureGetStringForUser("always_on_vpn_app", this.mUserId);
            boolean alwaysOnLockdown = this.mSystemServices.settingsSecureGetIntForUser("always_on_vpn_lockdown", 0, this.mUserId) != 0;
            String allowlistString = this.mSystemServices.settingsSecureGetStringForUser(LOCKDOWN_ALLOWLIST_SETTING_NAME, this.mUserId);
            List<String> allowedPackages = TextUtils.isEmpty(allowlistString) ? Collections.emptyList() : Arrays.asList(allowlistString.split(","));
            setAlwaysOnPackageInternal(alwaysOnPackage, alwaysOnLockdown, allowedPackages);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1003=6] */
    public boolean startAlwaysOnVpn() {
        synchronized (this) {
            String alwaysOnPackage = getAlwaysOnPackage();
            if (alwaysOnPackage == null) {
                return true;
            }
            if (!isAlwaysOnPackageSupported(alwaysOnPackage)) {
                setAlwaysOnPackage(null, false, null);
                return false;
            } else if (getNetworkInfo().isConnected()) {
                return true;
            } else {
                long oldId = Binder.clearCallingIdentity();
                try {
                    VpnProfile profile = getVpnProfilePrivileged(alwaysOnPackage);
                    if (profile != null) {
                        startVpnProfilePrivileged(profile, alwaysOnPackage);
                        return true;
                    }
                    DeviceIdleInternal idleController = this.mDeps.getDeviceIdleInternal();
                    idleController.addPowerSaveTempWhitelistApp(Process.myUid(), alwaysOnPackage, 60000L, this.mUserId, false, 309, "vpn");
                    Intent serviceIntent = new Intent("android.net.VpnService");
                    serviceIntent.setPackage(alwaysOnPackage);
                    try {
                        return this.mUserIdContext.startService(serviceIntent) != null;
                    } catch (RuntimeException e) {
                        Log.e(TAG, "VpnService " + serviceIntent + " failed to start", e);
                        return false;
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "Error starting always-on VPN", e2);
                    return false;
                } finally {
                    Binder.restoreCallingIdentity(oldId);
                }
            }
        }
    }

    public synchronized boolean prepare(String oldPackage, String newPackage, int vpnType) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.CONTROL_VPN") != 0) {
            if (oldPackage != null) {
                verifyCallingUidAndPackage(oldPackage);
            }
            if (newPackage != null) {
                verifyCallingUidAndPackage(newPackage);
            }
        }
        if (oldPackage != null) {
            if (this.mAlwaysOn && !isCurrentPreparedPackage(oldPackage)) {
                return false;
            }
            if (!isCurrentPreparedPackage(oldPackage)) {
                if (oldPackage.equals("[Legacy VPN]") || !isVpnPreConsented(this.mContext, oldPackage, vpnType)) {
                    return false;
                }
                prepareInternal(oldPackage);
                return true;
            } else if (!oldPackage.equals("[Legacy VPN]") && !isVpnPreConsented(this.mContext, oldPackage, vpnType)) {
                prepareInternal("[Legacy VPN]");
                return false;
            }
        }
        if (newPackage != null && (newPackage.equals("[Legacy VPN]") || !isCurrentPreparedPackage(newPackage))) {
            enforceControlPermission();
            if (!this.mAlwaysOn || isCurrentPreparedPackage(newPackage)) {
                prepareInternal(newPackage);
                return true;
            }
            return false;
        }
        return true;
    }

    private boolean isCurrentPreparedPackage(String packageName) {
        return getAppUid(packageName, this.mUserId) == this.mOwnerUID && this.mPackage.equals(packageName);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1179=4] */
    private void prepareInternal(String newPackage) {
        long token;
        VpnConfig vpnConfig;
        long token2 = Binder.clearCallingIdentity();
        try {
            if (this.mInterface != null) {
                try {
                    this.mStatusIntent = null;
                    agentDisconnect();
                    jniReset(this.mInterface);
                    this.mInterface = null;
                    resetNetworkCapabilities();
                } catch (Throwable th) {
                    th = th;
                    token = token2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            }
            Connection connection = this.mConnection;
            if (connection != null) {
                try {
                    connection.mService.transact(AudioFormat.SUB_MASK, Parcel.obtain(), null, 1);
                } catch (Exception e) {
                }
                this.mAppOpsManager.finishOp("android:establish_vpn_service", this.mOwnerUID, this.mPackage, null);
                this.mContext.unbindService(this.mConnection);
                cleanupVpnStateLocked();
                token = token2;
                vpnConfig = null;
            } else if (this.mVpnRunner != null) {
                if ("[Legacy VPN]".equals(this.mPackage)) {
                    token = token2;
                    vpnConfig = null;
                } else {
                    this.mAppOpsManager.finishOp("android:establish_vpn_manager", this.mOwnerUID, this.mPackage, null);
                    if (SdkLevel.isAtLeastT()) {
                        token = token2;
                        vpnConfig = null;
                        try {
                            sendEventToVpnManagerApp("android.net.category.EVENT_DEACTIVATED_BY_USER", -1, -1, this.mPackage, getSessionKeyLocked(), makeVpnProfileStateLocked(), null, null, null);
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(token);
                            throw th;
                        }
                    } else {
                        token = token2;
                        vpnConfig = null;
                    }
                }
                this.mVpnRunner.exit();
            } else {
                token = token2;
                vpnConfig = null;
            }
            try {
                this.mNms.denyProtect(this.mOwnerUID);
            } catch (Exception e2) {
                Log.wtf(TAG, "Failed to disallow UID " + this.mOwnerUID + " to call protect() " + e2);
            }
            Log.i(TAG, "Switched from " + this.mPackage + " to " + newPackage);
            this.mPackage = newPackage;
            this.mOwnerUID = getAppUid(newPackage, this.mUserId);
            this.mIsPackageTargetingAtLeastQ = doesPackageTargetAtLeastQ(newPackage);
            try {
                this.mNms.allowProtect(this.mOwnerUID);
            } catch (Exception e3) {
                Log.wtf(TAG, "Failed to allow UID " + this.mOwnerUID + " to call protect() " + e3);
            }
            this.mConfig = vpnConfig;
            updateState(NetworkInfo.DetailedState.DISCONNECTED, "prepare");
            setVpnForcedLocked(this.mLockdown);
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th3) {
            th = th3;
            token = token2;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1231=5] */
    public boolean setPackageAuthorization(String packageName, int vpnType) {
        String[] toChange;
        enforceControlPermissionOrInternalCaller();
        int uid = getAppUid(packageName, this.mUserId);
        if (uid == -1 || "[Legacy VPN]".equals(packageName)) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            switch (vpnType) {
                case -1:
                    toChange = new String[]{"android:activate_vpn", "android:activate_platform_vpn"};
                    break;
                case 0:
                default:
                    Log.wtf(TAG, "Unrecognized VPN type while granting authorization");
                    return false;
                case 1:
                    toChange = new String[]{"android:activate_vpn"};
                    break;
                case 2:
                    toChange = new String[]{"android:activate_platform_vpn"};
                    break;
                case 3:
                    return false;
            }
            int length = toChange.length;
            int i = 0;
            while (true) {
                int i2 = 1;
                if (i >= length) {
                    return true;
                }
                String appOpStr = toChange[i];
                AppOpsManager appOpsManager = this.mAppOpsManager;
                if (vpnType != -1) {
                    i2 = 0;
                }
                appOpsManager.setMode(appOpStr, uid, packageName, i2);
                i++;
            }
        } catch (Exception e) {
            Log.wtf(TAG, "Failed to set app ops for package " + packageName + ", uid " + uid, e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static boolean isVpnPreConsented(Context context, String packageName, int vpnType) {
        switch (vpnType) {
            case 1:
                return isVpnServicePreConsented(context, packageName);
            case 2:
                return isVpnProfilePreConsented(context, packageName);
            case 3:
                return "[Legacy VPN]".equals(packageName);
            default:
                return false;
        }
    }

    private static boolean doesPackageHaveAppop(Context context, String packageName, String appOpStr) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService("appops");
        return appOps.noteOpNoThrow(appOpStr, Binder.getCallingUid(), packageName, null, null) == 0;
    }

    private static boolean isVpnServicePreConsented(Context context, String packageName) {
        return doesPackageHaveAppop(context, packageName, "android:activate_vpn");
    }

    private static boolean isVpnProfilePreConsented(Context context, String packageName) {
        return doesPackageHaveAppop(context, packageName, "android:activate_platform_vpn") || isVpnServicePreConsented(context, packageName);
    }

    private int getAppUid(String app, int userId) {
        if ("[Legacy VPN]".equals(app)) {
            return Process.myUid();
        }
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

    private boolean doesPackageTargetAtLeastQ(String packageName) {
        if ("[Legacy VPN]".equals(packageName)) {
            return true;
        }
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfoAsUser(packageName, 0, this.mUserId);
            return appInfo.targetSdkVersion >= 29;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Can't find \"" + packageName + "\"");
            return false;
        }
    }

    public NetworkInfo getNetworkInfo() {
        return this.mNetworkInfo;
    }

    public synchronized Network getNetwork() {
        NetworkAgent agent = this.mNetworkAgent;
        if (agent == null) {
            return null;
        }
        Network network = agent.getNetwork();
        if (network == null) {
            return null;
        }
        return network;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LinkProperties makeLinkProperties() {
        boolean allowIPv4 = this.mConfig.allowIPv4;
        boolean allowIPv6 = this.mConfig.allowIPv6;
        LinkProperties lp = new LinkProperties();
        lp.setInterfaceName(this.mInterface);
        if (this.mConfig.addresses != null) {
            for (LinkAddress address : this.mConfig.addresses) {
                lp.addLinkAddress(address);
                allowIPv4 |= address.getAddress() instanceof Inet4Address;
                allowIPv6 |= address.getAddress() instanceof Inet6Address;
            }
        }
        if (this.mConfig.routes != null) {
            for (RouteInfo route : this.mConfig.routes) {
                lp.addRoute(route);
                InetAddress address2 = route.getDestination().getAddress();
                if (route.getType() == 1) {
                    allowIPv4 |= address2 instanceof Inet4Address;
                    allowIPv6 |= address2 instanceof Inet6Address;
                }
            }
        }
        if (this.mConfig.dnsServers != null) {
            for (String dnsServer : this.mConfig.dnsServers) {
                InetAddress address3 = InetAddresses.parseNumericAddress(dnsServer);
                lp.addDnsServer(address3);
                allowIPv4 |= address3 instanceof Inet4Address;
                allowIPv6 |= address3 instanceof Inet6Address;
            }
        }
        lp.setHttpProxy(this.mConfig.proxyInfo);
        if (!allowIPv4) {
            lp.addRoute(new RouteInfo(new IpPrefix(NetworkStackConstants.IPV4_ADDR_ANY, 0), null, null, 7));
        }
        if (!allowIPv6) {
            lp.addRoute(new RouteInfo(new IpPrefix(NetworkStackConstants.IPV6_ADDR_ANY, 0), null, null, 7));
        }
        StringBuilder buffer = new StringBuilder();
        if (this.mConfig.searchDomains != null) {
            for (String domain : this.mConfig.searchDomains) {
                buffer.append(domain).append(' ');
            }
        }
        lp.setDomains(buffer.toString().trim());
        if (this.mConfig.mtu > 0) {
            lp.setMtu(this.mConfig.mtu);
        }
        return lp;
    }

    private boolean updateLinkPropertiesInPlaceIfPossible(NetworkAgent agent, VpnConfig oldConfig) {
        if (oldConfig.allowBypass != this.mConfig.allowBypass) {
            Log.i(TAG, "Handover not possible due to changes to allowBypass");
            return false;
        } else if (!Objects.equals(oldConfig.allowedApplications, this.mConfig.allowedApplications) || !Objects.equals(oldConfig.disallowedApplications, this.mConfig.disallowedApplications)) {
            Log.i(TAG, "Handover not possible due to changes to allowed/denied apps");
            return false;
        } else {
            agent.sendLinkProperties(makeLinkProperties());
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void agentConnect() {
        LinkProperties lp = makeLinkProperties();
        NetworkCapabilities.Builder capsBuilder = new NetworkCapabilities.Builder(this.mNetworkCapabilities);
        capsBuilder.addCapability(12);
        this.mLegacyState = 2;
        updateState(NetworkInfo.DetailedState.CONNECTING, "agentConnect");
        NetworkAgentConfig networkAgentConfig = new NetworkAgentConfig.Builder().setLegacyType(17).setLegacyTypeName(NETWORKTYPE).setBypassableVpn(this.mConfig.allowBypass && !this.mLockdown).setVpnRequiresValidation(this.mConfig.requiresInternetValidation).setLocalRoutesExcludedForVpn(this.mConfig.excludeLocalRoutes).build();
        capsBuilder.setOwnerUid(this.mOwnerUID);
        capsBuilder.setAdministratorUids(new int[]{this.mOwnerUID});
        capsBuilder.setUids(createUserAndRestrictedProfilesRanges(this.mUserId, this.mConfig.allowedApplications, this.mConfig.disallowedApplications));
        capsBuilder.setTransportInfo(new VpnTransportInfo(getActiveVpnType(), this.mConfig.session));
        if (this.mIsPackageTargetingAtLeastQ && this.mConfig.isMetered) {
            capsBuilder.removeCapability(11);
        } else {
            capsBuilder.addCapability(11);
        }
        capsBuilder.setUnderlyingNetworks(this.mConfig.underlyingNetworks != null ? Arrays.asList(this.mConfig.underlyingNetworks) : null);
        this.mNetworkCapabilities = capsBuilder.build();
        this.mNetworkCapabilities = ITranVpn.Instance().agentConnect(this.mNetworkCapabilities, this.mLockdown);
        this.mNetworkAgent = new NetworkAgent(this.mContext, this.mLooper, NETWORKTYPE, this.mNetworkCapabilities, lp, new NetworkScore.Builder().setLegacyInt(101).build(), networkAgentConfig, this.mNetworkProvider) { // from class: com.android.server.connectivity.Vpn.1
            public void onNetworkUnwanted() {
            }
        };
        long token = Binder.clearCallingIdentity();
        try {
            try {
                this.mNetworkAgent.register();
                Binder.restoreCallingIdentity(token);
                updateState(NetworkInfo.DetailedState.CONNECTED, "agentConnect");
            } catch (Exception e) {
                this.mNetworkAgent = null;
                throw e;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private boolean canHaveRestrictedProfile(int userId) {
        long token = Binder.clearCallingIdentity();
        try {
            Context userContext = this.mContext.createContextAsUser(UserHandle.of(userId), 0);
            return ((UserManager) userContext.getSystemService(UserManager.class)).canHaveRestrictedProfile();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void agentDisconnect(NetworkAgent networkAgent) {
        if (networkAgent != null) {
            networkAgent.unregister();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void agentDisconnect() {
        updateState(NetworkInfo.DetailedState.DISCONNECTED, "agentDisconnect");
        ITranVpn.Instance().hookVpnChanged(null);
    }

    public synchronized ParcelFileDescriptor establish(VpnConfig config) {
        if (Binder.getCallingUid() != this.mOwnerUID) {
            return null;
        }
        if (isVpnServicePreConsented(this.mContext, this.mPackage)) {
            Intent intent = new Intent("android.net.VpnService");
            intent.setClassName(this.mPackage, config.user);
            long token = Binder.clearCallingIdentity();
            enforceNotRestrictedUser();
            PackageManager packageManager = this.mUserIdContext.getPackageManager();
            if (packageManager == null) {
                throw new IllegalStateException("Cannot get PackageManager.");
            }
            ResolveInfo info = packageManager.resolveService(intent, 0);
            if (info == null) {
                throw new SecurityException("Cannot find " + config.user);
            }
            if (!"android.permission.BIND_VPN_SERVICE".equals(info.serviceInfo.permission)) {
                throw new SecurityException(config.user + " does not require android.permission.BIND_VPN_SERVICE");
            }
            Binder.restoreCallingIdentity(token);
            VpnConfig oldConfig = this.mConfig;
            String oldInterface = this.mInterface;
            Connection oldConnection = this.mConnection;
            NetworkAgent oldNetworkAgent = this.mNetworkAgent;
            Set<Range<Integer>> oldUsers = this.mNetworkCapabilities.getUids();
            ParcelFileDescriptor tun = this.mDeps.adoptFd(this, config.mtu);
            try {
                String interfaze = this.mDeps.jniGetName(this, tun.getFd());
                StringBuilder builder = new StringBuilder();
                for (LinkAddress address : config.addresses) {
                    builder.append(" ");
                    builder.append(address);
                }
                if (this.mDeps.jniSetAddresses(this, interfaze, builder.toString()) >= 1) {
                    Connection connection = new Connection();
                    if (!this.mContext.bindServiceAsUser(intent, connection, AudioFormat.AAC_MAIN, new UserHandle(this.mUserId))) {
                        throw new IllegalStateException("Cannot bind " + config.user);
                    }
                    this.mConnection = connection;
                    this.mInterface = interfaze;
                    config.user = this.mPackage;
                    config.interfaze = this.mInterface;
                    config.startTime = SystemClock.elapsedRealtime();
                    this.mConfig = config;
                    if (oldConfig != null && updateLinkPropertiesInPlaceIfPossible(this.mNetworkAgent, oldConfig)) {
                        if (!Arrays.equals(oldConfig.underlyingNetworks, config.underlyingNetworks)) {
                            setUnderlyingNetworks(config.underlyingNetworks);
                        }
                    } else {
                        this.mNetworkAgent = null;
                        updateState(NetworkInfo.DetailedState.CONNECTING, "establish");
                        agentConnect();
                        agentDisconnect(oldNetworkAgent);
                    }
                    if (oldConnection != null) {
                        this.mContext.unbindService(oldConnection);
                    }
                    if (oldInterface != null && !oldInterface.equals(interfaze)) {
                        jniReset(oldInterface);
                    }
                    this.mDeps.setBlocking(tun.getFileDescriptor(), config.blocking);
                    if (oldNetworkAgent != this.mNetworkAgent) {
                        this.mAppOpsManager.startOp("android:establish_vpn_service", this.mOwnerUID, this.mPackage, null, null);
                    }
                    Log.i(TAG, "Established by " + config.user + " on " + this.mInterface);
                    ITranVpn.Instance().hookVpnChangedIfNeed(this.mConfig);
                    return tun;
                }
                throw new IllegalArgumentException("At least one address must be specified");
            } catch (RuntimeException e) {
                IoUtils.closeQuietly(tun);
                if (oldNetworkAgent != this.mNetworkAgent) {
                    agentDisconnect();
                }
                this.mConfig = oldConfig;
                this.mConnection = oldConnection;
                this.mNetworkCapabilities = new NetworkCapabilities.Builder(this.mNetworkCapabilities).setUids(oldUsers).build();
                this.mNetworkAgent = oldNetworkAgent;
                this.mInterface = oldInterface;
                throw e;
            }
        }
        return null;
    }

    private boolean isRunningLocked() {
        return (this.mNetworkAgent == null || this.mInterface == null) ? false : true;
    }

    protected boolean isCallerEstablishedOwnerLocked() {
        return isRunningLocked() && Binder.getCallingUid() == this.mOwnerUID;
    }

    private SortedSet<Integer> getAppsUids(List<String> packageNames, int userId) {
        SortedSet<Integer> uids = new TreeSet<>();
        for (String app : packageNames) {
            int uid = getAppUid(app, userId);
            if (uid != -1) {
                uids.add(Integer.valueOf(uid));
            }
            if (Process.isApplicationUid(uid) && SdkLevel.isAtLeastT()) {
                uids.add(Integer.valueOf(Process.toSdkSandboxUid(uid)));
            }
        }
        return uids;
    }

    Set<Range<Integer>> createUserAndRestrictedProfilesRanges(int userId, List<String> allowedApplications, List<String> disallowedApplications) {
        Set<Range<Integer>> ranges = new ArraySet<>();
        addUserToRanges(ranges, userId, allowedApplications, disallowedApplications);
        if (canHaveRestrictedProfile(userId)) {
            long token = Binder.clearCallingIdentity();
            try {
                List<UserInfo> users = this.mUserManager.getAliveUsers();
                Binder.restoreCallingIdentity(token);
                for (UserInfo user : users) {
                    if ((user.isRestricted() && user.restrictedProfileParentId == userId) || (user.isDualProfile() && user.profileGroupId == userId)) {
                        addUserToRanges(ranges, user.id, allowedApplications, disallowedApplications);
                    }
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }
        return ranges;
    }

    void addUserToRanges(Set<Range<Integer>> ranges, int userId, List<String> allowedApplications, List<String> disallowedApplications) {
        if (allowedApplications != null) {
            int start = -1;
            int stop = -1;
            for (Integer num : getAppsUids(allowedApplications, userId)) {
                int uid = num.intValue();
                if (start == -1) {
                    start = uid;
                } else if (uid != stop + 1) {
                    ranges.add(new Range<>(Integer.valueOf(start), Integer.valueOf(stop)));
                    start = uid;
                }
                stop = uid;
            }
            if (start != -1) {
                ranges.add(new Range<>(Integer.valueOf(start), Integer.valueOf(stop)));
            }
        } else if (disallowedApplications != null) {
            Range<Integer> userRange = createUidRangeForUser(userId);
            int start2 = userRange.getLower().intValue();
            for (Integer num2 : getAppsUids(disallowedApplications, userId)) {
                int uid2 = num2.intValue();
                if (uid2 == start2) {
                    start2++;
                } else {
                    ranges.add(new Range<>(Integer.valueOf(start2), Integer.valueOf(uid2 - 1)));
                    start2 = uid2 + 1;
                }
            }
            if (start2 <= userRange.getUpper().intValue()) {
                ranges.add(new Range<>(Integer.valueOf(start2), userRange.getUpper()));
            }
        } else {
            ranges.add(createUidRangeForUser(userId));
        }
    }

    private static List<Range<Integer>> uidRangesForUser(int userId, Set<Range<Integer>> existingRanges) {
        Range<Integer> userRange = createUidRangeForUser(userId);
        List<Range<Integer>> ranges = new ArrayList<>();
        for (Range<Integer> range : existingRanges) {
            if (userRange.contains(range)) {
                ranges.add(range);
            }
        }
        return ranges;
    }

    public void onUserAdded(int userId) {
        UserInfo user = this.mUserManager.getUserInfo(userId);
        if ((user.isRestricted() && user.restrictedProfileParentId == this.mUserId) || (user.isDualProfile() && user.profileGroupId == this.mUserId)) {
            synchronized (this) {
                Set<Range<Integer>> existingRanges = this.mNetworkCapabilities.getUids();
                if (existingRanges != null) {
                    try {
                        addUserToRanges(existingRanges, userId, this.mConfig.allowedApplications, this.mConfig.disallowedApplications);
                        this.mNetworkCapabilities = new NetworkCapabilities.Builder(this.mNetworkCapabilities).setUids(existingRanges).build();
                    } catch (Exception e) {
                        Log.wtf(TAG, "Failed to add restricted user to owner", e);
                    }
                    NetworkAgent networkAgent = this.mNetworkAgent;
                    if (networkAgent != null) {
                        networkAgent.sendNetworkCapabilities(this.mNetworkCapabilities);
                    }
                }
                setVpnForcedLocked(this.mLockdown);
            }
        }
    }

    public void onUserRemoved(int userId) {
        UserInfo user = this.mUserManager.getUserInfo(userId);
        if ((user.isRestricted() && user.restrictedProfileParentId == this.mUserId) || (user.isDualProfile() && user.profileGroupId == this.mUserId)) {
            synchronized (this) {
                Set<Range<Integer>> existingRanges = this.mNetworkCapabilities.getUids();
                if (existingRanges != null) {
                    try {
                        List<Range<Integer>> removedRanges = uidRangesForUser(userId, existingRanges);
                        existingRanges.removeAll(removedRanges);
                        this.mNetworkCapabilities = new NetworkCapabilities.Builder(this.mNetworkCapabilities).setUids(existingRanges).build();
                    } catch (Exception e) {
                        Log.wtf(TAG, "Failed to remove restricted user to owner", e);
                    }
                    NetworkAgent networkAgent = this.mNetworkAgent;
                    if (networkAgent != null) {
                        networkAgent.sendNetworkCapabilities(this.mNetworkCapabilities);
                    }
                }
                setVpnForcedLocked(this.mLockdown);
            }
        }
    }

    public synchronized void onUserStopped() {
        setVpnForcedLocked(false);
        this.mAlwaysOn = false;
        agentDisconnect();
        this.mConnectivityManager.unregisterNetworkProvider(this.mNetworkProvider);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.transsion.hubcore.server.connectivity.ITranVpn */
    /* JADX WARN: Multi-variable type inference failed */
    private void setVpnForcedLocked(boolean enforce) {
        List<String> exemptedPackages;
        Set<UidRangeParcel> rangesToAdd;
        if (isNullOrLegacyVpn(this.mPackage)) {
            exemptedPackages = null;
        } else {
            exemptedPackages = new ArrayList<>(this.mLockdownAllowlist);
            exemptedPackages.add(this.mPackage);
        }
        Set<UidRangeParcel> rangesToRemove = new ArraySet<>(this.mBlockedUidsAsToldToConnectivity);
        if (enforce) {
            Set<Range<Integer>> restrictedProfilesRanges = createUserAndRestrictedProfilesRanges(this.mUserId, null, exemptedPackages);
            Set<UidRangeParcel> rangesThatShouldBeBlocked = new ArraySet<>();
            for (Range<Integer> range : restrictedProfilesRanges) {
                if (range.getLower().intValue() == 0 && range.getUpper().intValue() != 0) {
                    rangesThatShouldBeBlocked.add(new UidRangeParcel(1, range.getUpper().intValue()));
                } else if (range.getLower().intValue() != 0) {
                    rangesThatShouldBeBlocked.add(new UidRangeParcel(range.getLower().intValue(), range.getUpper().intValue()));
                }
            }
            rangesToRemove.removeAll(rangesThatShouldBeBlocked);
            rangesThatShouldBeBlocked.removeAll(this.mBlockedUidsAsToldToConnectivity);
            rangesToAdd = rangesThatShouldBeBlocked;
        } else {
            Set<UidRangeParcel> rangesToAdd2 = Collections.emptySet();
            rangesToAdd = rangesToAdd2;
        }
        this.mNetworkCapabilities = ITranVpn.Instance().setVpnForcedLocked(enforce, rangesToAdd, rangesToRemove, this.mBlockedUidsAsToldToConnectivity, this.mNetworkCapabilities);
        setAllowOnlyVpnForUids(false, rangesToRemove);
        setAllowOnlyVpnForUids(true, rangesToAdd);
    }

    private boolean setAllowOnlyVpnForUids(boolean enforce, Collection<UidRangeParcel> ranges) {
        if (ranges.size() == 0) {
            return true;
        }
        ArrayList<Range<Integer>> integerRanges = new ArrayList<>(ranges.size());
        for (UidRangeParcel uidRange : ranges) {
            integerRanges.add(new Range<>(Integer.valueOf(uidRange.start), Integer.valueOf(uidRange.stop)));
        }
        try {
            this.mConnectivityManager.setRequireVpnForUids(enforce, integerRanges);
            if (enforce) {
                this.mBlockedUidsAsToldToConnectivity.addAll(ranges);
            } else {
                this.mBlockedUidsAsToldToConnectivity.removeAll(ranges);
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Updating blocked=" + enforce + " for UIDs " + Arrays.toString(ranges.toArray()) + " failed", e);
            return false;
        }
    }

    public synchronized VpnConfig getVpnConfig() {
        enforceControlPermission();
        return this.mConfig;
    }

    @Deprecated
    public synchronized void interfaceStatusChanged(String iface, boolean up) {
        try {
            try {
                this.mObserver.interfaceStatusChanged(iface, up);
            } catch (RemoteException e) {
            }
        } catch (RemoteException e2) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupVpnStateLocked() {
        this.mStatusIntent = null;
        resetNetworkCapabilities();
        this.mConfig = null;
        this.mInterface = null;
        this.mVpnRunner = null;
        this.mConnection = null;
        agentDisconnect();
    }

    private void enforceControlPermission() {
        this.mContext.enforceCallingPermission("android.permission.CONTROL_VPN", "Unauthorized Caller");
    }

    private void enforceControlPermissionOrInternalCaller() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_VPN", "Unauthorized Caller");
    }

    private void enforceSettingsPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", "Unauthorized Caller");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class Connection implements ServiceConnection {
        private IBinder mService;

        private Connection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.mService = service;
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            this.mService = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareStatusIntent() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mStatusIntent = this.mDeps.getIntentForStatusPanel(this.mContext);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public synchronized boolean addAddress(String address, int prefixLength) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        boolean success = jniAddAddress(this.mInterface, address, prefixLength);
        this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        return success;
    }

    public synchronized boolean removeAddress(String address, int prefixLength) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        boolean success = jniDelAddress(this.mInterface, address, prefixLength);
        this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        return success;
    }

    public synchronized boolean setUnderlyingNetworks(Network[] networks) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        List list = null;
        this.mConfig.underlyingNetworks = networks != null ? (Network[]) Arrays.copyOf(networks, networks.length) : null;
        NetworkAgent networkAgent = this.mNetworkAgent;
        if (this.mConfig.underlyingNetworks != null) {
            list = Arrays.asList(this.mConfig.underlyingNetworks);
        }
        networkAgent.setUnderlyingNetworks(list);
        return true;
    }

    public synchronized UnderlyingNetworkInfo getUnderlyingNetworkInfo() {
        if (!isRunningLocked()) {
            return null;
        }
        return new UnderlyingNetworkInfo(this.mOwnerUID, this.mInterface, new ArrayList());
    }

    public synchronized boolean appliesToUid(int uid) {
        if (isRunningLocked()) {
            Set<Range<Integer>> uids = this.mNetworkCapabilities.getUids();
            if (uids == null) {
                return true;
            }
            for (Range<Integer> range : uids) {
                if (range.contains((Range<Integer>) Integer.valueOf(uid))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public synchronized int getActiveVpnType() {
        if (this.mNetworkInfo.isConnectedOrConnecting()) {
            if (this.mVpnRunner == null) {
                return 1;
            }
            return isIkev2VpnRunner() ? 2 : 3;
        }
        return -1;
    }

    private void updateAlwaysOnNotification(NetworkInfo.DetailedState networkState) {
        boolean visible = this.mAlwaysOn && networkState != NetworkInfo.DetailedState.CONNECTED;
        UserHandle user = UserHandle.of(this.mUserId);
        long token = Binder.clearCallingIdentity();
        try {
            NotificationManager notificationManager = (NotificationManager) this.mUserIdContext.getSystemService(NotificationManager.class);
            if (!visible) {
                notificationManager.cancel(TAG, 17);
                return;
            }
            Intent intent = new Intent();
            intent.setComponent(ComponentName.unflattenFromString(this.mContext.getString(17039911)));
            intent.putExtra("lockdown", this.mLockdown);
            intent.addFlags(268435456);
            PendingIntent configIntent = this.mSystemServices.pendingIntentGetActivityAsUser(intent, AudioFormat.DTS_HD, user);
            Notification.Builder builder = new Notification.Builder(this.mContext, NETWORKTYPE).setSmallIcon(17303870).setContentTitle(this.mContext.getString(17041714)).setContentText(this.mContext.getString(17041711)).setContentIntent(configIntent).setCategory(Griffin.SYS).setVisibility(1).setOngoing(true).setColor(this.mContext.getColor(17170460));
            notificationManager.notify(TAG, 17, builder.build());
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* loaded from: classes.dex */
    public static class SystemServices {
        private final Context mContext;

        public SystemServices(Context context) {
            this.mContext = context;
        }

        public PendingIntent pendingIntentGetActivityAsUser(Intent intent, int flags, UserHandle user) {
            return PendingIntent.getActivity(this.mContext.createContextAsUser(user, 0), 0, intent, flags);
        }

        public void settingsSecurePutStringForUser(String key, String value, int userId) {
            Settings.Secure.putString(getContentResolverAsUser(userId), key, value);
        }

        public void settingsSecurePutIntForUser(String key, int value, int userId) {
            Settings.Secure.putInt(getContentResolverAsUser(userId), key, value);
        }

        public String settingsSecureGetStringForUser(String key, int userId) {
            return Settings.Secure.getString(getContentResolverAsUser(userId), key);
        }

        public int settingsSecureGetIntForUser(String key, int def, int userId) {
            return Settings.Secure.getInt(getContentResolverAsUser(userId), key, def);
        }

        private ContentResolver getContentResolverAsUser(int userId) {
            return this.mContext.createContextAsUser(UserHandle.of(userId), 0).getContentResolver();
        }
    }

    private static RouteInfo findIPv4DefaultRoute(LinkProperties prop) {
        for (RouteInfo route : prop.getAllRoutes()) {
            if (route.isDefaultRoute() && (route.getGateway() instanceof Inet4Address)) {
                return route;
            }
        }
        throw new IllegalStateException("Unable to find IPv4 default gateway");
    }

    private void enforceNotRestrictedUser() {
        long token = Binder.clearCallingIdentity();
        try {
            UserInfo user = this.mUserManager.getUserInfo(this.mUserId);
            if (user.isRestricted()) {
                throw new SecurityException("Restricted users cannot configure VPNs");
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void startLegacyVpn(VpnProfile profile, Network underlying, LinkProperties egress) {
        enforceControlPermission();
        long token = Binder.clearCallingIdentity();
        try {
            startLegacyVpnPrivileged(profile, underlying, egress);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private String makeKeystoreEngineGrantString(String alias) {
        if (alias == null) {
            return null;
        }
        KeyStore2 keystore2 = KeyStore2.getInstance();
        KeyDescriptor key = new KeyDescriptor();
        key.domain = 0;
        key.nspace = -1L;
        key.alias = alias;
        key.blob = null;
        try {
            return KeyStore2.makeKeystoreEngineGrantString(keystore2.grant(key, 1016, 260).nspace);
        } catch (KeyStoreException e) {
            Log.e(TAG, "Failed to get grant for keystore key.", e);
            throw new IllegalStateException("Failed to get grant for keystore key.", e);
        }
    }

    private String getCaCertificateFromKeystoreAsPem(KeyStore keystore, String alias) throws java.security.KeyStoreException, IOException, CertificateEncodingException {
        if (keystore.isCertificateEntry(alias)) {
            Certificate cert = keystore.getCertificate(alias);
            if (cert == null) {
                return null;
            }
            return new String(Credentials.convertToPem(new Certificate[]{cert}), StandardCharsets.UTF_8);
        }
        Certificate[] certs = keystore.getCertificateChain(alias);
        if (certs == null || certs.length <= 1) {
            return null;
        }
        return new String(Credentials.convertToPem((Certificate[]) Arrays.copyOfRange(certs, 1, certs.length)), StandardCharsets.UTF_8);
    }

    /* JADX WARN: Removed duplicated region for block: B:46:0x01ce  */
    /* JADX WARN: Removed duplicated region for block: B:47:0x0235  */
    /* JADX WARN: Removed duplicated region for block: B:56:0x02d3  */
    /* JADX WARN: Removed duplicated region for block: B:59:0x02e7  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x02fd  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void startLegacyVpnPrivileged(VpnProfile profile, Network underlying, LinkProperties egress) {
        String str;
        UserInfo user = this.mUserManager.getUserInfo(this.mUserId);
        if (user.isRestricted() || this.mUserManager.hasUserRestriction("no_config_vpn", new UserHandle(this.mUserId))) {
            throw new SecurityException("Restricted users cannot establish VPNs");
        }
        RouteInfo ipv4DefaultRoute = findIPv4DefaultRoute(egress);
        String gateway = ipv4DefaultRoute.getGateway().getHostAddress();
        String iface = ipv4DefaultRoute.getInterface();
        String privateKey = "";
        String userCert = "";
        String caCert = "";
        String serverCert = "";
        try {
            KeyStore keystore = KeyStore.getInstance("AndroidKeyStore");
            keystore.load(null);
            if (!profile.ipsecUserCert.isEmpty()) {
                privateKey = profile.ipsecUserCert;
                Certificate cert = keystore.getCertificate(profile.ipsecUserCert);
                if (cert == null) {
                    str = null;
                } else {
                    str = new String(Credentials.convertToPem(new Certificate[]{cert}), StandardCharsets.UTF_8);
                }
                userCert = str;
            }
            if (!profile.ipsecCaCert.isEmpty()) {
                caCert = getCaCertificateFromKeystoreAsPem(keystore, profile.ipsecCaCert);
            }
            if (!profile.ipsecServerCert.isEmpty()) {
                Certificate cert2 = keystore.getCertificate(profile.ipsecServerCert);
                serverCert = cert2 == null ? null : new String(Credentials.convertToPem(new Certificate[]{cert2}), StandardCharsets.UTF_8);
            }
            if (userCert == null || caCert == null || serverCert == null) {
                throw new IllegalStateException("Cannot load credentials");
            }
            String[] racoon = null;
            switch (profile.type) {
                case 1:
                    racoon = new String[]{iface, profile.server, "udppsk", profile.ipsecIdentifier, profile.ipsecSecret, "1701"};
                    String[] mtpd = null;
                    switch (profile.type) {
                        case 0:
                            String[] strArr = new String[20];
                            strArr[0] = iface;
                            strArr[1] = "pptp";
                            strArr[2] = profile.server;
                            strArr[3] = "1723";
                            strArr[4] = "name";
                            strArr[5] = profile.username;
                            strArr[6] = "password";
                            strArr[7] = profile.password;
                            strArr[8] = "linkname";
                            strArr[9] = "vpn";
                            strArr[10] = "refuse-eap";
                            strArr[11] = "nodefaultroute";
                            strArr[12] = "usepeerdns";
                            strArr[13] = "idle";
                            strArr[14] = "1800";
                            strArr[15] = "mtu";
                            strArr[16] = "1270";
                            strArr[17] = "mru";
                            strArr[18] = "1270";
                            strArr[19] = profile.mppe ? "+mppe" : "nomppe";
                            mtpd = strArr;
                            if (profile.mppe) {
                                mtpd = (String[]) Arrays.copyOf(mtpd, mtpd.length + 1);
                                mtpd[mtpd.length - 1] = "-pap";
                                break;
                            }
                            break;
                        case 1:
                        case 2:
                            mtpd = new String[]{iface, "l2tp", profile.server, "1701", profile.l2tpSecret, "name", profile.username, "password", profile.password, "linkname", "vpn", "refuse-eap", "nodefaultroute", "usepeerdns", "idle", "1800", "mtu", "1270", "mru", "1270"};
                            break;
                    }
                    VpnConfig config = new VpnConfig();
                    config.legacy = true;
                    config.user = profile.key;
                    config.interfaze = iface;
                    config.session = profile.name;
                    config.isMetered = false;
                    config.proxyInfo = profile.proxy;
                    if (underlying != null) {
                        config.underlyingNetworks = new Network[]{underlying};
                    }
                    config.addLegacyRoutes(profile.routes);
                    if (!profile.dnsServers.isEmpty()) {
                        config.dnsServers = Arrays.asList(profile.dnsServers.split(" +"));
                    }
                    if (!profile.searchDomains.isEmpty()) {
                        config.searchDomains = Arrays.asList(profile.searchDomains.split(" +"));
                    }
                    startLegacyVpn(config, racoon, mtpd, profile);
                    return;
                case 2:
                    racoon = new String[]{iface, profile.server, "udprsa", makeKeystoreEngineGrantString(privateKey), userCert, caCert, serverCert, "1701"};
                    String[] mtpd2 = null;
                    switch (profile.type) {
                    }
                    VpnConfig config2 = new VpnConfig();
                    config2.legacy = true;
                    config2.user = profile.key;
                    config2.interfaze = iface;
                    config2.session = profile.name;
                    config2.isMetered = false;
                    config2.proxyInfo = profile.proxy;
                    if (underlying != null) {
                    }
                    config2.addLegacyRoutes(profile.routes);
                    if (!profile.dnsServers.isEmpty()) {
                    }
                    if (!profile.searchDomains.isEmpty()) {
                    }
                    startLegacyVpn(config2, racoon, mtpd2, profile);
                    return;
                case 3:
                    racoon = new String[]{iface, profile.server, "xauthpsk", profile.ipsecIdentifier, profile.ipsecSecret, profile.username, profile.password, "", gateway};
                    String[] mtpd22 = null;
                    switch (profile.type) {
                    }
                    VpnConfig config22 = new VpnConfig();
                    config22.legacy = true;
                    config22.user = profile.key;
                    config22.interfaze = iface;
                    config22.session = profile.name;
                    config22.isMetered = false;
                    config22.proxyInfo = profile.proxy;
                    if (underlying != null) {
                    }
                    config22.addLegacyRoutes(profile.routes);
                    if (!profile.dnsServers.isEmpty()) {
                    }
                    if (!profile.searchDomains.isEmpty()) {
                    }
                    startLegacyVpn(config22, racoon, mtpd22, profile);
                    return;
                case 4:
                    racoon = new String[]{iface, profile.server, "xauthrsa", makeKeystoreEngineGrantString(privateKey), userCert, caCert, serverCert, profile.username, profile.password, "", gateway};
                    String[] mtpd222 = null;
                    switch (profile.type) {
                    }
                    VpnConfig config222 = new VpnConfig();
                    config222.legacy = true;
                    config222.user = profile.key;
                    config222.interfaze = iface;
                    config222.session = profile.name;
                    config222.isMetered = false;
                    config222.proxyInfo = profile.proxy;
                    if (underlying != null) {
                    }
                    config222.addLegacyRoutes(profile.routes);
                    if (!profile.dnsServers.isEmpty()) {
                    }
                    if (!profile.searchDomains.isEmpty()) {
                    }
                    startLegacyVpn(config222, racoon, mtpd222, profile);
                    return;
                case 5:
                    racoon = new String[]{iface, profile.server, "hybridrsa", caCert, serverCert, profile.username, profile.password, "", gateway};
                    String[] mtpd2222 = null;
                    switch (profile.type) {
                    }
                    VpnConfig config2222 = new VpnConfig();
                    config2222.legacy = true;
                    config2222.user = profile.key;
                    config2222.interfaze = iface;
                    config2222.session = profile.name;
                    config2222.isMetered = false;
                    config2222.proxyInfo = profile.proxy;
                    if (underlying != null) {
                    }
                    config2222.addLegacyRoutes(profile.routes);
                    if (!profile.dnsServers.isEmpty()) {
                    }
                    if (!profile.searchDomains.isEmpty()) {
                    }
                    startLegacyVpn(config2222, racoon, mtpd2222, profile);
                    return;
                case 6:
                    profile.ipsecCaCert = caCert;
                    profile.setAllowedAlgorithms(Ikev2VpnProfile.DEFAULT_ALGORITHMS);
                    startVpnProfilePrivileged(profile, "[Legacy VPN]");
                    return;
                case 7:
                    profile.ipsecSecret = Ikev2VpnProfile.encodeForIpsecSecret(profile.ipsecSecret.getBytes());
                    profile.setAllowedAlgorithms(Ikev2VpnProfile.DEFAULT_ALGORITHMS);
                    startVpnProfilePrivileged(profile, "[Legacy VPN]");
                    return;
                case 8:
                    profile.ipsecSecret = "KEYSTORE_ALIAS:" + privateKey;
                    profile.ipsecUserCert = userCert;
                    profile.ipsecCaCert = caCert;
                    profile.setAllowedAlgorithms(Ikev2VpnProfile.DEFAULT_ALGORITHMS);
                    startVpnProfilePrivileged(profile, "[Legacy VPN]");
                    return;
                case 9:
                    startVpnProfilePrivileged(profile, "[Legacy VPN]");
                    return;
                default:
                    String[] mtpd22222 = null;
                    switch (profile.type) {
                    }
                    VpnConfig config22222 = new VpnConfig();
                    config22222.legacy = true;
                    config22222.user = profile.key;
                    config22222.interfaze = iface;
                    config22222.session = profile.name;
                    config22222.isMetered = false;
                    config22222.proxyInfo = profile.proxy;
                    if (underlying != null) {
                    }
                    config22222.addLegacyRoutes(profile.routes);
                    if (!profile.dnsServers.isEmpty()) {
                    }
                    if (!profile.searchDomains.isEmpty()) {
                    }
                    startLegacyVpn(config22222, racoon, mtpd22222, profile);
                    return;
            }
        } catch (IOException | java.security.KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalStateException("Failed to load credentials from AndroidKeyStore", e);
        }
    }

    private synchronized void startLegacyVpn(VpnConfig config, String[] racoon, String[] mtpd, VpnProfile profile) {
        stopVpnRunnerPrivileged();
        prepareInternal("[Legacy VPN]");
        updateState(NetworkInfo.DetailedState.CONNECTING, "startLegacyVpn");
        this.mVpnRunner = new LegacyVpnRunner(config, racoon, mtpd, profile);
        startLegacyVpnRunner();
    }

    protected void startLegacyVpnRunner() {
        this.mVpnRunner.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSettingsVpnLocked() {
        return this.mVpnRunner != null && "[Legacy VPN]".equals(this.mPackage);
    }

    public synchronized void stopVpnRunnerPrivileged() {
        if (isSettingsVpnLocked()) {
            VpnRunner vpnRunner = this.mVpnRunner;
            boolean isLegacyVpn = vpnRunner instanceof LegacyVpnRunner;
            vpnRunner.exit();
            this.mVpnRunner = null;
            if (isLegacyVpn) {
                synchronized ("LegacyVpnRunner") {
                }
            }
        }
    }

    public synchronized LegacyVpnInfo getLegacyVpnInfo() {
        enforceControlPermission();
        return getLegacyVpnInfoPrivileged();
    }

    private synchronized LegacyVpnInfo getLegacyVpnInfoPrivileged() {
        if (isSettingsVpnLocked()) {
            LegacyVpnInfo info = new LegacyVpnInfo();
            info.key = this.mConfig.user;
            info.state = this.mLegacyState;
            if (this.mNetworkInfo.isConnected()) {
                info.intent = this.mStatusIntent;
            }
            return info;
        }
        return null;
    }

    public synchronized VpnConfig getLegacyVpnConfig() {
        if (isSettingsVpnLocked()) {
            return this.mConfig;
        }
        return null;
    }

    protected synchronized NetworkCapabilities getRedactedNetworkCapabilitiesOfUnderlyingNetwork(NetworkCapabilities nc) {
        if (nc == null) {
            return null;
        }
        return this.mConnectivityManager.getRedactedNetworkCapabilitiesForPackage(nc, this.mOwnerUID, this.mPackage);
    }

    protected synchronized LinkProperties getRedactedLinkPropertiesOfUnderlyingNetwork(LinkProperties lp) {
        if (lp == null) {
            return null;
        }
        return this.mConnectivityManager.getRedactedLinkPropertiesForPackage(lp, this.mOwnerUID, this.mPackage);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public abstract class VpnRunner extends Thread {
        protected abstract void exitVpnRunner();

        @Override // java.lang.Thread, java.lang.Runnable
        public abstract void run();

        protected VpnRunner(String name) {
            super(name);
        }

        protected final void exit() {
            synchronized (Vpn.this) {
                exitVpnRunner();
                Vpn.this.cleanupVpnStateLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class IkeV2VpnRunner extends VpnRunner implements IkeV2VpnRunnerCallback {
        private static final String TAG = "IkeV2VpnRunner";
        private Network mActiveNetwork;
        private final ExecutorService mExecutor;
        private final IpSecManager mIpSecManager;
        private boolean mIsRunning;
        private final ConnectivityManager.NetworkCallback mNetworkCallback;
        private final Ikev2VpnProfile mProfile;
        private IkeSession mSession;
        private final String mSessionKey;
        private IpSecManager.IpSecTunnelInterface mTunnelIface;
        private LinkProperties mUnderlyingLinkProperties;
        private NetworkCapabilities mUnderlyingNetworkCapabilities;

        IkeV2VpnRunner(Ikev2VpnProfile profile) {
            super(TAG);
            ExecutorService newSingleThreadExecutor = Executors.newSingleThreadExecutor();
            this.mExecutor = newSingleThreadExecutor;
            this.mIsRunning = true;
            this.mProfile = profile;
            this.mIpSecManager = (IpSecManager) Vpn.this.mContext.getSystemService(INetd.IPSEC_INTERFACE_PREFIX);
            this.mNetworkCallback = new VpnIkev2Utils.Ikev2VpnNetworkCallback(TAG, this, newSingleThreadExecutor);
            this.mSessionKey = UUID.randomUUID().toString();
        }

        @Override // com.android.server.connectivity.Vpn.VpnRunner, java.lang.Thread, java.lang.Runnable
        public void run() {
            NetworkRequest req;
            if (this.mProfile.isRestrictedToTestNetworks()) {
                req = new NetworkRequest.Builder().clearCapabilities().addTransportType(7).addCapability(15).build();
            } else {
                req = new NetworkRequest.Builder().addCapability(12).build();
            }
            Vpn.this.mConnectivityManager.requestNetwork(req, this.mNetworkCallback);
        }

        private boolean isActiveNetwork(Network network) {
            return Objects.equals(this.mActiveNetwork, network) && this.mIsRunning;
        }

        @Override // com.android.server.connectivity.Vpn.IkeV2VpnRunnerCallback
        public void onChildOpened(Network network, ChildSessionConfiguration childConfig) {
            if (!isActiveNetwork(network)) {
                Log.d(TAG, "onOpened called for obsolete network " + network);
                return;
            }
            try {
                String interfaceName = this.mTunnelIface.getInterfaceName();
                int maxMtu = this.mProfile.getMaxMtu();
                List<LinkAddress> internalAddresses = childConfig.getInternalAddresses();
                ArrayList arrayList = new ArrayList();
                Collection<RouteInfo> newRoutes = VpnIkev2Utils.getRoutesFromTrafficSelectors(childConfig.getOutboundTrafficSelectors());
                for (LinkAddress address : internalAddresses) {
                    this.mTunnelIface.addAddress(address.getAddress(), address.getPrefixLength());
                }
                for (InetAddress addr : childConfig.getInternalDnsServers()) {
                    arrayList.add(addr.getHostAddress());
                }
                synchronized (Vpn.this) {
                    Vpn.this.mInterface = interfaceName;
                    Vpn.this.mConfig.mtu = maxMtu;
                    Vpn.this.mConfig.interfaze = Vpn.this.mInterface;
                    Vpn.this.mConfig.addresses.clear();
                    Vpn.this.mConfig.addresses.addAll(internalAddresses);
                    Vpn.this.mConfig.routes.clear();
                    Vpn.this.mConfig.routes.addAll(newRoutes);
                    if (Vpn.this.mConfig.dnsServers == null) {
                        Vpn.this.mConfig.dnsServers = new ArrayList();
                    }
                    Vpn.this.mConfig.dnsServers.clear();
                    Vpn.this.mConfig.dnsServers.addAll(arrayList);
                    Vpn.this.mConfig.underlyingNetworks = new Network[]{network};
                    VpnConfig vpnConfig = Vpn.this.mConfig;
                    Vpn vpn = Vpn.this;
                    vpnConfig.disallowedApplications = vpn.getAppExclusionList(vpn.mPackage);
                    NetworkAgent networkAgent = Vpn.this.mNetworkAgent;
                    if (networkAgent == null) {
                        if (Vpn.this.isSettingsVpnLocked()) {
                            Vpn.this.prepareStatusIntent();
                        }
                        Vpn.this.agentConnect();
                        return;
                    }
                    networkAgent.setUnderlyingNetworks(Collections.singletonList(network));
                    LinkProperties lp = Vpn.this.makeLinkProperties();
                    networkAgent.sendLinkProperties(lp);
                }
            } catch (Exception e) {
                Log.d(TAG, "Error in ChildOpened for network " + network, e);
                onSessionLost(network, e);
            }
        }

        @Override // com.android.server.connectivity.Vpn.IkeV2VpnRunnerCallback
        public void onChildTransformCreated(Network network, IpSecTransform transform, int direction) {
            if (!isActiveNetwork(network)) {
                Log.d(TAG, "ChildTransformCreated for obsolete network " + network);
                return;
            }
            try {
                this.mIpSecManager.applyTunnelModeTransform(this.mTunnelIface, direction, transform);
            } catch (IOException e) {
                Log.d(TAG, "Transform application failed for network " + network, e);
                onSessionLost(network, e);
            }
        }

        @Override // com.android.server.connectivity.Vpn.IkeV2VpnRunnerCallback
        public void onDefaultNetworkChanged(Network network) {
            IkeSessionParams ikeSessionParams;
            ChildSessionParams childSessionParams;
            Log.d(TAG, "Starting IKEv2/IPsec session on new network: " + network);
            try {
                if (!this.mIsRunning) {
                    Log.d(TAG, "onDefaultNetworkChanged after exit");
                    return;
                }
                Vpn.this.mInterface = null;
                resetIkeState();
                this.mActiveNetwork = network;
                IkeTunnelConnectionParams ikeTunConnParams = this.mProfile.getIkeTunnelConnectionParams();
                if (ikeTunConnParams != null) {
                    IkeSessionParams.Builder builder = new IkeSessionParams.Builder(ikeTunConnParams.getIkeSessionParams()).setNetwork(network);
                    ikeSessionParams = builder.build();
                    childSessionParams = ikeTunConnParams.getTunnelModeChildSessionParams();
                } else {
                    ikeSessionParams = VpnIkev2Utils.buildIkeSessionParams(Vpn.this.mContext, this.mProfile, network);
                    childSessionParams = VpnIkev2Utils.buildChildSessionParams(this.mProfile.getAllowedAlgorithms());
                }
                InetAddress address = InetAddress.getLocalHost();
                this.mTunnelIface = this.mIpSecManager.createIpSecTunnelInterface(address, address, network);
                NetdUtils.setInterfaceUp(Vpn.this.mNetd, this.mTunnelIface.getInterfaceName());
                this.mSession = Vpn.this.mIkev2SessionCreator.createIkeSession(Vpn.this.mContext, ikeSessionParams, childSessionParams, this.mExecutor, new VpnIkev2Utils.IkeSessionCallbackImpl(TAG, this, network), new VpnIkev2Utils.ChildSessionCallbackImpl(TAG, this, network));
                Log.d(TAG, "Ike Session started for network " + network);
            } catch (Exception e) {
                Log.i(TAG, "Setup failed for network " + network + ". Aborting", e);
                onSessionLost(network, e);
            }
        }

        @Override // com.android.server.connectivity.Vpn.IkeV2VpnRunnerCallback
        public void onDefaultNetworkCapabilitiesChanged(NetworkCapabilities nc) {
            this.mUnderlyingNetworkCapabilities = nc;
        }

        @Override // com.android.server.connectivity.Vpn.IkeV2VpnRunnerCallback
        public void onDefaultNetworkLinkPropertiesChanged(LinkProperties lp) {
            this.mUnderlyingLinkProperties = lp;
        }

        private void markFailedAndDisconnect(Exception exception) {
            synchronized (Vpn.this) {
                Vpn.this.updateState(NetworkInfo.DetailedState.FAILED, exception.getMessage());
            }
            exitVpnRunner();
        }

        @Override // com.android.server.connectivity.Vpn.IkeV2VpnRunnerCallback
        public void onSessionLost(Network network, Exception exception) {
            if (!isActiveNetwork(network)) {
                Log.d(TAG, "onSessionLost() called for obsolete network " + network);
                return;
            }
            synchronized (Vpn.this) {
                if (exception instanceof IkeProtocolException) {
                    IkeProtocolException ikeException = (IkeProtocolException) exception;
                    switch (ikeException.getErrorType()) {
                        case 14:
                        case 17:
                        case 24:
                        case 34:
                        case 37:
                        case 38:
                            if (SdkLevel.isAtLeastT()) {
                                Vpn vpn = Vpn.this;
                                if (vpn.isVpnApp(vpn.mPackage)) {
                                    Vpn.this.sendEventToVpnManagerApp("android.net.category.EVENT_IKE_ERROR", 1, ikeException.getErrorType(), Vpn.this.getPackage(), this.mSessionKey, Vpn.this.makeVpnProfileStateLocked(), this.mActiveNetwork, Vpn.this.getRedactedNetworkCapabilitiesOfUnderlyingNetwork(this.mUnderlyingNetworkCapabilities), Vpn.this.getRedactedLinkPropertiesOfUnderlyingNetwork(this.mUnderlyingLinkProperties));
                                }
                            }
                            markFailedAndDisconnect(exception);
                            return;
                        default:
                            if (SdkLevel.isAtLeastT()) {
                                Vpn vpn2 = Vpn.this;
                                if (vpn2.isVpnApp(vpn2.mPackage)) {
                                    Vpn.this.sendEventToVpnManagerApp("android.net.category.EVENT_IKE_ERROR", 2, ikeException.getErrorType(), Vpn.this.getPackage(), this.mSessionKey, Vpn.this.makeVpnProfileStateLocked(), this.mActiveNetwork, Vpn.this.getRedactedNetworkCapabilitiesOfUnderlyingNetwork(this.mUnderlyingNetworkCapabilities), Vpn.this.getRedactedLinkPropertiesOfUnderlyingNetwork(this.mUnderlyingLinkProperties));
                                }
                            }
                            break;
                    }
                } else if (exception instanceof IllegalArgumentException) {
                    markFailedAndDisconnect(exception);
                    return;
                } else if (exception instanceof IkeNetworkLostException) {
                    if (SdkLevel.isAtLeastT()) {
                        Vpn vpn3 = Vpn.this;
                        if (vpn3.isVpnApp(vpn3.mPackage)) {
                            Vpn vpn4 = Vpn.this;
                            vpn4.sendEventToVpnManagerApp("android.net.category.EVENT_NETWORK_ERROR", 2, 2, vpn4.getPackage(), this.mSessionKey, Vpn.this.makeVpnProfileStateLocked(), this.mActiveNetwork, Vpn.this.getRedactedNetworkCapabilitiesOfUnderlyingNetwork(this.mUnderlyingNetworkCapabilities), Vpn.this.getRedactedLinkPropertiesOfUnderlyingNetwork(this.mUnderlyingLinkProperties));
                        }
                    }
                } else if (exception instanceof IkeNonProtocolException) {
                    if (exception.getCause() instanceof UnknownHostException) {
                        if (SdkLevel.isAtLeastT()) {
                            Vpn vpn5 = Vpn.this;
                            if (vpn5.isVpnApp(vpn5.mPackage)) {
                                Vpn vpn6 = Vpn.this;
                                vpn6.sendEventToVpnManagerApp("android.net.category.EVENT_NETWORK_ERROR", 2, 0, vpn6.getPackage(), this.mSessionKey, Vpn.this.makeVpnProfileStateLocked(), this.mActiveNetwork, Vpn.this.getRedactedNetworkCapabilitiesOfUnderlyingNetwork(this.mUnderlyingNetworkCapabilities), Vpn.this.getRedactedLinkPropertiesOfUnderlyingNetwork(this.mUnderlyingLinkProperties));
                            }
                        }
                    } else if (exception.getCause() instanceof IkeTimeoutException) {
                        if (SdkLevel.isAtLeastT()) {
                            Vpn vpn7 = Vpn.this;
                            if (vpn7.isVpnApp(vpn7.mPackage)) {
                                Vpn vpn8 = Vpn.this;
                                vpn8.sendEventToVpnManagerApp("android.net.category.EVENT_NETWORK_ERROR", 2, 1, vpn8.getPackage(), this.mSessionKey, Vpn.this.makeVpnProfileStateLocked(), this.mActiveNetwork, Vpn.this.getRedactedNetworkCapabilitiesOfUnderlyingNetwork(this.mUnderlyingNetworkCapabilities), Vpn.this.getRedactedLinkPropertiesOfUnderlyingNetwork(this.mUnderlyingLinkProperties));
                            }
                        }
                    } else if ((exception.getCause() instanceof IOException) && SdkLevel.isAtLeastT()) {
                        Vpn vpn9 = Vpn.this;
                        if (vpn9.isVpnApp(vpn9.mPackage)) {
                            Vpn vpn10 = Vpn.this;
                            vpn10.sendEventToVpnManagerApp("android.net.category.EVENT_NETWORK_ERROR", 2, 3, vpn10.getPackage(), this.mSessionKey, Vpn.this.makeVpnProfileStateLocked(), this.mActiveNetwork, Vpn.this.getRedactedNetworkCapabilitiesOfUnderlyingNetwork(this.mUnderlyingNetworkCapabilities), Vpn.this.getRedactedLinkPropertiesOfUnderlyingNetwork(this.mUnderlyingLinkProperties));
                        }
                    }
                } else if (exception != null) {
                    Log.wtf(TAG, "onSessionLost: exception = " + exception);
                }
                this.mActiveNetwork = null;
                this.mUnderlyingNetworkCapabilities = null;
                this.mUnderlyingLinkProperties = null;
                Log.d(TAG, "Resetting state for network: " + network);
                synchronized (Vpn.this) {
                    Vpn.this.mInterface = null;
                    if (Vpn.this.mConfig != null) {
                        Vpn.this.mConfig.interfaze = null;
                        if (Vpn.this.mConfig.routes != null) {
                            List<RouteInfo> oldRoutes = new ArrayList<>(Vpn.this.mConfig.routes);
                            Vpn.this.mConfig.routes.clear();
                            for (RouteInfo route : oldRoutes) {
                                Vpn.this.mConfig.routes.add(new RouteInfo(route.getDestination(), null, null, 7));
                            }
                            if (Vpn.this.mNetworkAgent != null) {
                                Vpn.this.mNetworkAgent.sendLinkProperties(Vpn.this.makeLinkProperties());
                            }
                        }
                    }
                }
                resetIkeState();
            }
        }

        private void resetIkeState() {
            IpSecManager.IpSecTunnelInterface ipSecTunnelInterface = this.mTunnelIface;
            if (ipSecTunnelInterface != null) {
                ipSecTunnelInterface.close();
                this.mTunnelIface = null;
            }
            IkeSession ikeSession = this.mSession;
            if (ikeSession != null) {
                ikeSession.kill();
                this.mSession = null;
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: disconnectVpnRunner */
        public void m2815xc26b73da() {
            this.mActiveNetwork = null;
            this.mUnderlyingNetworkCapabilities = null;
            this.mUnderlyingLinkProperties = null;
            this.mIsRunning = false;
            resetIkeState();
            Vpn.this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
            this.mExecutor.shutdown();
        }

        @Override // com.android.server.connectivity.Vpn.VpnRunner
        public void exitVpnRunner() {
            try {
                this.mExecutor.execute(new Runnable() { // from class: com.android.server.connectivity.Vpn$IkeV2VpnRunner$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        Vpn.IkeV2VpnRunner.this.m2815xc26b73da();
                    }
                });
            } catch (RejectedExecutionException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LegacyVpnRunner extends VpnRunner {
        private static final String TAG = "LegacyVpnRunner";
        private final String[][] mArguments;
        private long mBringupStartTime;
        private final BroadcastReceiver mBroadcastReceiver;
        private final String[] mDaemons;
        private final AtomicInteger mOuterConnection;
        private final String mOuterInterface;
        private final VpnProfile mProfile;
        private final LocalSocket[] mSockets;

        LegacyVpnRunner(VpnConfig config, String[] racoon, String[] mtpd, VpnProfile profile) {
            super(TAG);
            NetworkInfo netInfo;
            this.mOuterConnection = new AtomicInteger(-1);
            this.mBringupStartTime = -1L;
            this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.connectivity.Vpn.LegacyVpnRunner.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo info;
                    if (Vpn.this.mEnableTeardown && intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE") && intent.getIntExtra("networkType", -1) == LegacyVpnRunner.this.mOuterConnection.get() && (info = (NetworkInfo) intent.getExtra("networkInfo")) != null && !info.isConnectedOrConnecting()) {
                        try {
                            Vpn.this.mObserver.interfaceStatusChanged(LegacyVpnRunner.this.mOuterInterface, false);
                        } catch (RemoteException e) {
                        }
                    }
                }
            };
            if (racoon == null && mtpd == null) {
                throw new IllegalArgumentException("Arguments to racoon and mtpd must not both be null");
            }
            Vpn.this.mConfig = config;
            String[] strArr = {"racoon", "mtpd"};
            this.mDaemons = strArr;
            int i = 0;
            this.mArguments = new String[][]{racoon, mtpd};
            this.mSockets = new LocalSocket[strArr.length];
            String str = Vpn.this.mConfig.interfaze;
            this.mOuterInterface = str;
            this.mProfile = profile;
            if (!TextUtils.isEmpty(str)) {
                Network[] allNetworks = Vpn.this.mConnectivityManager.getAllNetworks();
                int length = allNetworks.length;
                while (true) {
                    if (i < length) {
                        Network network = allNetworks[i];
                        LinkProperties lp = Vpn.this.mConnectivityManager.getLinkProperties(network);
                        if (lp == null || !lp.getAllInterfaceNames().contains(this.mOuterInterface) || (netInfo = Vpn.this.mConnectivityManager.getNetworkInfo(network)) == null) {
                            i++;
                        } else {
                            this.mOuterConnection.set(netInfo.getType());
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            Vpn.this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        }

        public void exitIfOuterInterfaceIs(String interfaze) {
            if (interfaze.equals(this.mOuterInterface)) {
                Log.i(TAG, "Legacy VPN is going down with " + interfaze);
                exitVpnRunner();
            }
        }

        @Override // com.android.server.connectivity.Vpn.VpnRunner
        public void exitVpnRunner() {
            interrupt();
            Vpn.this.agentDisconnect();
            try {
                Vpn.this.mContext.unregisterReceiver(this.mBroadcastReceiver);
            } catch (IllegalArgumentException e) {
            }
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET, ARRAY_LENGTH, MOVE]}, finally: {[IGET, ARRAY_LENGTH, MOVE, ARITH, AGET, INVOKE, AGET, IGET, IGET, INVOKE, ARITH, GOTO, IF, IGET, ARRAY_LENGTH, AGET, IGET, IGET, INVOKE, ARITH, GOTO, IF, IGET, ARRAY_LENGTH, MOVE_EXCEPTION, INVOKE, AGET, IGET, IGET, INVOKE, ARITH, GOTO, IF, IGET, ARRAY_LENGTH, MOVE_EXCEPTION, IF] complete} */
        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3302=6, 3311=6] */
        @Override // com.android.server.connectivity.Vpn.VpnRunner, java.lang.Thread, java.lang.Runnable
        public void run() {
            LocalSocket[] localSocketArr;
            LocalSocket[] localSocketArr2;
            LocalSocket[] localSocketArr3;
            Log.v(TAG, "Waiting");
            synchronized (TAG) {
                Log.v(TAG, "Executing");
                int i = 0;
                try {
                    bringup();
                    waitForDaemonsToStop();
                    interrupted();
                    for (LocalSocket socket : this.mSockets) {
                        IoUtils.closeQuietly(socket);
                    }
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                    }
                    String[] strArr = this.mDaemons;
                    int length = strArr.length;
                    while (i < length) {
                        String daemon = strArr[i];
                        Vpn.this.mDeps.stopService(daemon);
                        i++;
                    }
                } catch (InterruptedException e2) {
                    for (LocalSocket socket2 : this.mSockets) {
                        IoUtils.closeQuietly(socket2);
                    }
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e3) {
                    }
                    String[] strArr2 = this.mDaemons;
                    int length2 = strArr2.length;
                    while (i < length2) {
                        String daemon2 = strArr2[i];
                        Vpn.this.mDeps.stopService(daemon2);
                        i++;
                    }
                } catch (Throwable th) {
                    for (LocalSocket socket3 : this.mSockets) {
                        IoUtils.closeQuietly(socket3);
                    }
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e4) {
                    }
                    String[] strArr3 = this.mDaemons;
                    int length3 = strArr3.length;
                    while (i < length3) {
                        String daemon3 = strArr3[i];
                        Vpn.this.mDeps.stopService(daemon3);
                        i++;
                    }
                    throw th;
                }
                Vpn.this.agentDisconnect();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void checkInterruptAndDelay(boolean sleepLonger) throws InterruptedException {
            long now = SystemClock.elapsedRealtime();
            if (now - this.mBringupStartTime <= 60000) {
                Thread.sleep(sleepLonger ? 200L : 1L);
            } else {
                Vpn.this.updateState(NetworkInfo.DetailedState.FAILED, "checkpoint");
                throw new IllegalStateException("VPN bringup took too long");
            }
        }

        private void checkAndFixupArguments(InetAddress endpointAddress) {
            String endpointAddressString = endpointAddress.getHostAddress();
            if (!"racoon".equals(this.mDaemons[0]) || !"mtpd".equals(this.mDaemons[1])) {
                throw new IllegalStateException("Unexpected daemons order");
            }
            if (this.mArguments[0] != null) {
                if (!this.mProfile.server.equals(this.mArguments[0][1])) {
                    throw new IllegalStateException("Invalid server argument for racoon");
                }
                this.mArguments[0][1] = endpointAddressString;
            }
            if (this.mArguments[1] != null) {
                if (!this.mProfile.server.equals(this.mArguments[1][2])) {
                    throw new IllegalStateException("Invalid server argument for mtpd");
                }
                this.mArguments[1][2] = endpointAddressString;
            }
        }

        /* JADX WARN: Code restructure failed: missing block: B:37:0x00cf, code lost:
            checkInterruptAndDelay(true);
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private void bringup() {
            String[] strArr;
            try {
                InetAddress endpointAddress = Vpn.this.mDeps.resolve(this.mProfile.server);
                checkAndFixupArguments(endpointAddress);
                this.mBringupStartTime = SystemClock.elapsedRealtime();
                for (String daemon : this.mDaemons) {
                    while (!Vpn.this.mDeps.isServiceStopped(daemon)) {
                        checkInterruptAndDelay(true);
                    }
                }
                File state = Vpn.this.mDeps.getStateFile();
                state.delete();
                if (state.exists()) {
                    throw new IllegalStateException("Cannot delete the state");
                }
                new File("/data/misc/vpn/abort").delete();
                Vpn.this.updateState(NetworkInfo.DetailedState.CONNECTING, "execute");
                int i = 0;
                while (true) {
                    String[] strArr2 = this.mDaemons;
                    if (i >= strArr2.length) {
                        break;
                    }
                    String[] arguments = this.mArguments[i];
                    if (arguments != null) {
                        String daemon2 = strArr2[i];
                        Vpn.this.mDeps.startService(daemon2);
                        while (!Vpn.this.mDeps.isServiceRunning(daemon2)) {
                            checkInterruptAndDelay(true);
                        }
                        this.mSockets[i] = new LocalSocket();
                        Vpn.this.mDeps.sendArgumentsToDaemon(daemon2, this.mSockets[i], arguments, new RetryScheduler() { // from class: com.android.server.connectivity.Vpn$LegacyVpnRunner$$ExternalSyntheticLambda0
                            @Override // com.android.server.connectivity.Vpn.RetryScheduler
                            public final void checkInterruptAndDelay(boolean z) {
                                Vpn.LegacyVpnRunner.this.checkInterruptAndDelay(z);
                            }
                        });
                    }
                    i++;
                }
                while (!state.exists()) {
                    int i2 = 0;
                    while (true) {
                        String[] strArr3 = this.mDaemons;
                        if (i2 < strArr3.length) {
                            String daemon3 = strArr3[i2];
                            if (this.mArguments[i2] != null && !Vpn.this.mDeps.isServiceRunning(daemon3)) {
                                throw new IllegalStateException(daemon3 + " is dead");
                            }
                            i2++;
                        }
                    }
                }
                String[] parameters = FileUtils.readTextFile(state, 0, null).split("\n", -1);
                if (parameters.length != 7) {
                    throw new IllegalStateException("Cannot parse the state: '" + String.join("', '", parameters) + "'");
                }
                Vpn.this.mConfig.interfaze = parameters[0].trim();
                Vpn.this.mConfig.addLegacyAddresses(parameters[1]);
                if (Vpn.this.mConfig.routes == null || Vpn.this.mConfig.routes.isEmpty()) {
                    Vpn.this.mConfig.addLegacyRoutes(parameters[2]);
                }
                if (Vpn.this.mConfig.dnsServers == null || Vpn.this.mConfig.dnsServers.size() == 0) {
                    String dnsServers = parameters[3].trim();
                    if (!dnsServers.isEmpty()) {
                        Vpn.this.mConfig.dnsServers = Arrays.asList(dnsServers.split(" "));
                    }
                }
                if (Vpn.this.mConfig.searchDomains == null || Vpn.this.mConfig.searchDomains.size() == 0) {
                    String searchDomains = parameters[4].trim();
                    if (!searchDomains.isEmpty()) {
                        Vpn.this.mConfig.searchDomains = Arrays.asList(searchDomains.split(" "));
                    }
                }
                if (endpointAddress instanceof Inet4Address) {
                    Vpn.this.mConfig.routes.add(new RouteInfo(new IpPrefix(endpointAddress, 32), null, null, 9));
                } else if (endpointAddress instanceof Inet6Address) {
                    Vpn.this.mConfig.routes.add(new RouteInfo(new IpPrefix(endpointAddress, 128), null, null, 9));
                } else {
                    Log.e(TAG, "Unknown IP address family for VPN endpoint: " + endpointAddress);
                }
                synchronized (Vpn.this) {
                    Vpn.this.mConfig.startTime = SystemClock.elapsedRealtime();
                    checkInterruptAndDelay(false);
                    Dependencies dependencies = Vpn.this.mDeps;
                    Vpn vpn = Vpn.this;
                    if (!dependencies.isInterfacePresent(vpn, vpn.mConfig.interfaze)) {
                        throw new IllegalStateException(Vpn.this.mConfig.interfaze + " is gone");
                    }
                    Vpn vpn2 = Vpn.this;
                    vpn2.mInterface = vpn2.mConfig.interfaze;
                    Vpn.this.prepareStatusIntent();
                    Vpn.this.agentConnect();
                    Log.i(TAG, "Connected!");
                }
            } catch (Exception e) {
                Log.i(TAG, "Aborting", e);
                Vpn.this.updateState(NetworkInfo.DetailedState.FAILED, e.getMessage());
                exitVpnRunner();
            }
        }

        private void waitForDaemonsToStop() throws InterruptedException {
            if (!Vpn.this.mNetworkInfo.isConnected()) {
                return;
            }
            while (true) {
                Thread.sleep(2000L);
                for (int i = 0; i < this.mDaemons.length; i++) {
                    if (this.mArguments[i] != null && Vpn.this.mDeps.isServiceStopped(this.mDaemons[i])) {
                        return;
                    }
                }
            }
        }
    }

    private void verifyCallingUidAndPackage(String packageName) {
        int callingUid = Binder.getCallingUid();
        if (getAppUid(packageName, this.mUserId) != callingUid) {
            throw new SecurityException(packageName + " does not belong to uid " + callingUid);
        }
    }

    String getProfileNameForPackage(String packageName) {
        return "PLATFORM_VPN_" + this.mUserId + "_" + packageName;
    }

    void validateRequiredFeatures(VpnProfile profile) {
        switch (profile.type) {
            case 6:
            case 7:
            case 8:
            case 9:
                if (!this.mContext.getPackageManager().hasSystemFeature("android.software.ipsec_tunnels")) {
                    throw new UnsupportedOperationException("Ikev2VpnProfile(s) requires PackageManager.FEATURE_IPSEC_TUNNELS");
                }
                return;
            default:
                return;
        }
    }

    public synchronized boolean provisionVpnProfile(String packageName, VpnProfile profile) {
        Objects.requireNonNull(packageName, "No package name provided");
        Objects.requireNonNull(profile, "No profile provided");
        verifyCallingUidAndPackage(packageName);
        enforceNotRestrictedUser();
        validateRequiredFeatures(profile);
        if (profile.isRestrictedToTestNetworks) {
            this.mContext.enforceCallingPermission("android.permission.MANAGE_TEST_NETWORKS", "Test-mode profiles require the MANAGE_TEST_NETWORKS permission");
        }
        byte[] encodedProfile = profile.encode();
        if (encodedProfile.length > 131072) {
            throw new IllegalArgumentException("Profile too big");
        }
        long token = Binder.clearCallingIdentity();
        getVpnProfileStore().put(getProfileNameForPackage(packageName), encodedProfile);
        Binder.restoreCallingIdentity(token);
        return isVpnProfilePreConsented(this.mContext, packageName);
    }

    private boolean isCurrentIkev2VpnLocked(String packageName) {
        return isCurrentPreparedPackage(packageName) && isIkev2VpnRunner();
    }

    public synchronized void deleteVpnProfile(String packageName) {
        Objects.requireNonNull(packageName, "No package name provided");
        verifyCallingUidAndPackage(packageName);
        enforceNotRestrictedUser();
        long token = Binder.clearCallingIdentity();
        try {
            if (isCurrentIkev2VpnLocked(packageName)) {
                try {
                    if (this.mAlwaysOn) {
                        setAlwaysOnPackage(null, false, null);
                    } else {
                        prepareInternal("[Legacy VPN]");
                    }
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            }
            getVpnProfileStore().remove(getProfileNameForPackage(packageName));
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th2) {
            th = th2;
        }
    }

    VpnProfile getVpnProfilePrivileged(String packageName) {
        if (!this.mDeps.isCallerSystem()) {
            Log.wtf(TAG, "getVpnProfilePrivileged called as non-System UID ");
            return null;
        }
        byte[] encoded = getVpnProfileStore().get(getProfileNameForPackage(packageName));
        if (encoded == null) {
            return null;
        }
        return VpnProfile.decode("", encoded);
    }

    private boolean isIkev2VpnRunner() {
        return this.mVpnRunner instanceof IkeV2VpnRunner;
    }

    private String getSessionKeyLocked() {
        if (isIkev2VpnRunner()) {
            return ((IkeV2VpnRunner) this.mVpnRunner).mSessionKey;
        }
        return null;
    }

    public synchronized String startVpnProfile(String packageName) {
        String sessionKeyLocked;
        Objects.requireNonNull(packageName, "No package name provided");
        enforceNotRestrictedUser();
        if (!prepare(packageName, null, 2)) {
            throw new SecurityException("User consent not granted for package " + packageName);
        }
        long token = Binder.clearCallingIdentity();
        try {
            VpnProfile profile = getVpnProfilePrivileged(packageName);
            if (profile == null) {
                throw new IllegalArgumentException("No profile found for " + packageName);
            }
            startVpnProfilePrivileged(profile, packageName);
            if (!isIkev2VpnRunner()) {
                throw new IllegalStateException("mVpnRunner shouldn't be null and should also be an instance of Ikev2VpnRunner");
            }
            try {
                sessionKeyLocked = getSessionKeyLocked();
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                th = th;
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        return sessionKeyLocked;
    }

    private synchronized void startVpnProfilePrivileged(VpnProfile profile, String packageName) {
        prepareInternal(packageName);
        updateState(NetworkInfo.DetailedState.CONNECTING, "startPlatformVpn");
        try {
            this.mConfig = new VpnConfig();
            if ("[Legacy VPN]".equals(packageName)) {
                this.mConfig.legacy = true;
                this.mConfig.session = profile.name;
                this.mConfig.user = profile.key;
                this.mConfig.isMetered = true;
            } else {
                this.mConfig.user = packageName;
                this.mConfig.isMetered = profile.isMetered;
            }
            this.mConfig.startTime = SystemClock.elapsedRealtime();
            this.mConfig.proxyInfo = profile.proxy;
            this.mConfig.requiresInternetValidation = profile.requiresInternetValidation;
            this.mConfig.excludeLocalRoutes = profile.excludeLocalRoutes;
            switch (profile.type) {
                case 6:
                case 7:
                case 8:
                case 9:
                    IkeV2VpnRunner ikeV2VpnRunner = new IkeV2VpnRunner(Ikev2VpnProfile.fromVpnProfile(profile));
                    this.mVpnRunner = ikeV2VpnRunner;
                    ikeV2VpnRunner.start();
                    break;
                default:
                    updateState(NetworkInfo.DetailedState.FAILED, "Invalid platform VPN type");
                    Log.d(TAG, "Unknown VPN profile type: " + profile.type);
                    break;
            }
            if (!"[Legacy VPN]".equals(packageName)) {
                this.mAppOpsManager.startOp("android:establish_vpn_manager", this.mOwnerUID, this.mPackage, null, null);
            }
        } catch (GeneralSecurityException e) {
            this.mConfig = null;
            updateState(NetworkInfo.DetailedState.FAILED, "VPN startup failed");
            throw new IllegalArgumentException("VPN startup failed", e);
        }
    }

    public synchronized void stopVpnProfile(String packageName) {
        Objects.requireNonNull(packageName, "No package name provided");
        enforceNotRestrictedUser();
        if (isCurrentIkev2VpnLocked(packageName)) {
            prepareInternal("[Legacy VPN]");
        }
    }

    private boolean storeAppExclusionList(String packageName, List<String> excludedApps) {
        try {
            PersistableBundle bundle = PersistableBundleUtils.fromList(excludedApps, PersistableBundleUtils.STRING_SERIALIZER);
            byte[] data = PersistableBundleUtils.toDiskStableBytes(bundle);
            long oldId = Binder.clearCallingIdentity();
            try {
                getVpnProfileStore().put(getVpnAppExcludedForPackage(packageName), data);
                Binder.restoreCallingIdentity(oldId);
                return true;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(oldId);
                throw th;
            }
        } catch (IOException e) {
            Log.e(TAG, "problem writing into stream", e);
            return false;
        }
    }

    String getVpnAppExcludedForPackage(String packageName) {
        return VPN_APP_EXCLUDED + this.mUserId + "_" + packageName;
    }

    public synchronized boolean setAppExclusionList(String packageName, List<String> excludedApps) {
        enforceNotRestrictedUser();
        if (storeAppExclusionList(packageName, excludedApps)) {
            if (this.mNetworkAgent != null && isIkev2VpnRunner()) {
                this.mConfig.disallowedApplications = List.copyOf(excludedApps);
                NetworkCapabilities build = new NetworkCapabilities.Builder(this.mNetworkCapabilities).setUids(createUserAndRestrictedProfilesRanges(this.mUserId, null, excludedApps)).build();
                this.mNetworkCapabilities = build;
                this.mNetworkAgent.sendNetworkCapabilities(build);
            }
            return true;
        }
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3844=5] */
    public synchronized List<String> getAppExclusionList(String packageName) {
        enforceNotRestrictedUser();
        long oldId = Binder.clearCallingIdentity();
        try {
            byte[] bytes = getVpnProfileStore().get(getVpnAppExcludedForPackage(packageName));
            if (bytes != null && bytes.length != 0) {
                PersistableBundle bundle = PersistableBundleUtils.fromDiskStableBytes(bytes);
                List<String> list = PersistableBundleUtils.toList(bundle, PersistableBundleUtils.STRING_DESERIALIZER);
                Binder.restoreCallingIdentity(oldId);
                return list;
            }
            ArrayList arrayList = new ArrayList();
            Binder.restoreCallingIdentity(oldId);
            return arrayList;
        } catch (IOException e) {
            Log.e(TAG, "problem reading from stream", e);
            Binder.restoreCallingIdentity(oldId);
            return new ArrayList();
        }
    }

    private int getStateFromLegacyState(int legacyState) {
        switch (legacyState) {
            case 0:
                return 0;
            case 1:
            case 4:
            default:
                Log.wtf(TAG, "Unhandled state " + legacyState + ", treat it as STATE_DISCONNECTED");
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 5:
                return 3;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public VpnProfileState makeVpnProfileStateLocked() {
        return new VpnProfileState(getStateFromLegacyState(this.mLegacyState), isIkev2VpnRunner() ? getSessionKeyLocked() : null, this.mAlwaysOn, this.mLockdown);
    }

    private VpnProfileState makeDisconnectedVpnProfileState() {
        return new VpnProfileState(0, (String) null, false, false);
    }

    public synchronized VpnProfileState getProvisionedVpnProfileState(String packageName) {
        Objects.requireNonNull(packageName, "No package name provided");
        enforceNotRestrictedUser();
        return isCurrentIkev2VpnLocked(packageName) ? makeVpnProfileStateLocked() : null;
    }

    /* loaded from: classes.dex */
    public static class Ikev2SessionCreator {
        public IkeSession createIkeSession(Context context, IkeSessionParams ikeSessionParams, ChildSessionParams firstChildSessionParams, Executor userCbExecutor, IkeSessionCallback ikeSessionCallback, ChildSessionCallback firstChildSessionCallback) {
            return new IkeSession(context, ikeSessionParams, firstChildSessionParams, userCbExecutor, ikeSessionCallback, firstChildSessionCallback);
        }
    }

    static Range<Integer> createUidRangeForUser(int userId) {
        return new Range<>(Integer.valueOf(userId * 100000), Integer.valueOf(((userId + 1) * 100000) - 1));
    }
}
