package com.android.server.locksettings;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.admin.DeviceStateCache;
import android.app.admin.PasswordMetrics;
import android.app.trust.IStrongAuthTracker;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.authsecret.V1_0.IAuthSecret;
import android.hardware.biometrics.BiometricManager;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IProgressListener;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.security.AndroidKeyStoreMaintenance;
import android.security.Authorization;
import android.security.KeyStore;
import android.security.keystore.KeyProtection;
import android.security.keystore.UserNotAuthenticatedException;
import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.RecoveryCertPath;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.security.keystore2.AndroidKeyStoreLoadStoreParameter;
import android.security.keystore2.AndroidKeyStoreProvider;
import android.service.gatekeeper.GateKeeperResponse;
import android.service.gatekeeper.IGateKeeperService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.IWeakEscrowTokenActivatedListener;
import com.android.internal.widget.IWeakEscrowTokenRemovedListener;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockSettingsInternal;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.RebootEscrowListener;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
import com.android.server.am.HostingRecord;
import com.android.server.job.controllers.JobStatus;
import com.android.server.locksettings.LockSettingsStorage;
import com.android.server.locksettings.RebootEscrowManager;
import com.android.server.locksettings.SyntheticPasswordManager;
import com.android.server.locksettings.recoverablekeystore.RecoverableKeyStoreManager;
import com.android.server.pm.UserManagerInternal;
import com.android.server.utils.Watchable;
import com.android.server.utils.Watcher;
import com.android.server.wm.WindowManagerInternal;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import libcore.util.HexEncoding;
/* loaded from: classes.dex */
public class LockSettingsService extends ILockSettings.Stub {
    private static final String AOD_FP_DISPLAY = "aod_fingerprint_display";
    private static final String AOD_IS_SHOW = "aod_is_show";
    private static final String BIOMETRIC_PERMISSION = "android.permission.MANAGE_BIOMETRIC";
    private static final boolean DEBUG = false;
    private static final int GK_PW_HANDLE_STORE_DURATION_MS = 600000;
    private static final String GSI_RUNNING_PROP = "ro.gsid.image_running";
    private static final String PERMISSION = "android.permission.ACCESS_KEYGUARD_SECURE_STORAGE";
    private static final String PREV_SYNTHETIC_PASSWORD_HANDLE_KEY = "prev-sp-handle";
    private static final int PROFILE_KEY_IV_SIZE = 12;
    private static final String SYNTHETIC_PASSWORD_UPDATE_TIME_KEY = "sp-handle-ts";
    private static final String TAG = "LockSettingsService";
    private static final String USER_SERIAL_NUMBER_KEY = "serial-number";
    private int FLAG_CLEAR_LOCK_WITHOUT_PASSWORD_CONFIRM;
    private final IActivityManager mActivityManager;
    protected IAuthSecret mAuthSecretService;
    private final BiometricDeferredQueue mBiometricDeferredQueue;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final DeviceProvisionedObserver mDeviceProvisionedObserver;
    private HashMap<Integer, byte[]> mDiskCacheKeyMap;
    protected IGateKeeperService mGateKeeperService;
    private final LongSparseArray<byte[]> mGatekeeperPasswords;
    protected final Handler mHandler;
    protected boolean mHasSecureLockScreen;
    private final Injector mInjector;
    private final KeyStore mJavaKeyStore;
    private final android.security.KeyStore mKeyStore;
    private ManagedProfilePasswordCache mManagedProfilePasswordCache;
    private final NotificationManager mNotificationManager;
    private final Random mRandom;
    private final RebootEscrowManager mRebootEscrowManager;
    private final RecoverableKeyStoreManager mRecoverableKeyStoreManager;
    private final Object mSeparateChallengeLock;
    private final SyntheticPasswordManager mSpManager;
    protected final LockSettingsStorage mStorage;
    private final IStorageManager mStorageManager;
    private final Watcher mStorageWatcher;
    private final LockSettingsStrongAuth mStrongAuth;
    private final SynchronizedStrongAuthTracker mStrongAuthTracker;
    protected final UserManager mUserManager;
    private HashMap<UserHandle, UserManager> mUserManagerCache;
    final SparseArray<PasswordMetrics> mUserPasswordMetrics;
    private static final boolean OPTICAL_FINGERPRINT = "1".equals(SystemProperties.get("ro.optical_fingerprint_support", ""));
    private static final boolean MTK_AOD_SUPPORT = "1".equals(SystemProperties.get("ro.vendor.mtk_aod_support", ""));
    private static final int[] SYSTEM_CREDENTIAL_UIDS = {1016, 0, 1000};
    private static final String[] VALID_SETTINGS = {"lockscreen.lockedoutpermanently", "lockscreen.patterneverchosen", "lockscreen.password_type", "lockscreen.password_type_alternate", "lockscreen.password_salt", "lockscreen.disabled", "lockscreen.options", "lockscreen.biometric_weak_fallback", "lockscreen.biometricweakeverchosen", "lockscreen.power_button_instantly_locks", "lockscreen.passwordhistory", "lock_pattern_autolock", "lock_biometric_weak_flags", "lock_pattern_visible_pattern", "lock_pattern_tactile_feedback_enabled"};
    private static final String[] READ_CONTACTS_PROTECTED_SETTINGS = {"lock_screen_owner_info_enabled", "lock_screen_owner_info"};
    private static final String SEPARATE_PROFILE_CHALLENGE_KEY = "lockscreen.profilechallenge";
    private static final String[] READ_PASSWORD_PROTECTED_SETTINGS = {"lockscreen.password_salt", "lockscreen.passwordhistory", "lockscreen.password_type", SEPARATE_PROFILE_CHALLENGE_KEY};

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private LockSettingsService mLockSettingsService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.locksettings.LockSettingsService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.locksettings.LockSettingsService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            AndroidKeyStoreProvider.install();
            ?? lockSettingsService = new LockSettingsService(getContext());
            this.mLockSettingsService = lockSettingsService;
            publishBinderService("lock_settings", lockSettingsService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            super.onBootPhase(phase);
            if (phase == 550) {
                this.mLockSettingsService.migrateOldDataAfterSystemReady();
                this.mLockSettingsService.loadEscrowData();
            }
        }

        @Override // com.android.server.SystemService
        public void onUserStarting(SystemService.TargetUser user) {
            this.mLockSettingsService.onStartUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            this.mLockSettingsService.onUnlockUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStopped(SystemService.TargetUser user) {
            this.mLockSettingsService.onCleanupUser(user.getUserIdentifier());
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static class SynchronizedStrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
        public SynchronizedStrongAuthTracker(Context context) {
            super(context);
        }

        protected void handleStrongAuthRequiredChanged(int strongAuthFlags, int userId) {
            synchronized (this) {
                super.handleStrongAuthRequiredChanged(strongAuthFlags, userId);
            }
        }

        public int getStrongAuthForUser(int userId) {
            int strongAuthForUser;
            synchronized (this) {
                strongAuthForUser = super.getStrongAuthForUser(userId);
            }
            return strongAuthForUser;
        }

        void register(LockSettingsStrongAuth strongAuth) {
            strongAuth.registerStrongAuthTracker(getStub());
        }
    }

    private LockscreenCredential generateRandomProfilePassword() {
        byte[] bArr = new byte[0];
        try {
            byte[] randomLockSeed = SecureRandom.getInstance("SHA1PRNG").generateSeed(40);
            char[] newPasswordChars = HexEncoding.encode(randomLockSeed);
            byte[] newPassword = new byte[newPasswordChars.length];
            for (int i = 0; i < newPasswordChars.length; i++) {
                newPassword[i] = (byte) newPasswordChars[i];
            }
            LockscreenCredential credential = LockscreenCredential.createManagedPassword(newPassword);
            Arrays.fill(newPasswordChars, (char) 0);
            Arrays.fill(newPassword, (byte) 0);
            Arrays.fill(randomLockSeed, (byte) 0);
            return credential;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Fail to generate profile password", e);
        }
    }

    public void tieProfileLockIfNecessary(int profileUserId, LockscreenCredential profileUserPassword) {
        if (!isCredentialSharableWithParent(profileUserId) || getSeparateProfileChallengeEnabledInternal(profileUserId) || this.mStorage.hasChildProfileLock(profileUserId)) {
            return;
        }
        int parentId = this.mUserManager.getProfileParent(profileUserId).id;
        if (!isUserSecure(parentId) && !profileUserPassword.isNone()) {
            setLockCredentialInternal(LockscreenCredential.createNone(), profileUserPassword, profileUserId, true);
            return;
        }
        try {
            if (getGateKeeperService().getSecureUserId(parentId) == 0) {
                return;
            }
            LockscreenCredential unifiedProfilePassword = generateRandomProfilePassword();
            try {
                setLockCredentialInternal(unifiedProfilePassword, profileUserPassword, profileUserId, true);
                tieProfileLockToParent(profileUserId, unifiedProfilePassword);
                this.mManagedProfilePasswordCache.storePassword(profileUserId, unifiedProfilePassword);
                if (unifiedProfilePassword != null) {
                    unifiedProfilePassword.close();
                }
            } catch (Throwable th) {
                if (unifiedProfilePassword != null) {
                    try {
                        unifiedProfilePassword.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to talk to GateKeeper service", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        protected Context mContext;

        public Injector(Context context) {
            this.mContext = context;
        }

        public Context getContext() {
            return this.mContext;
        }

        public ServiceThread getServiceThread() {
            ServiceThread handlerThread = new ServiceThread(LockSettingsService.TAG, 10, true);
            handlerThread.start();
            return handlerThread;
        }

        public Handler getHandler(ServiceThread handlerThread) {
            return new Handler(handlerThread.getLooper());
        }

        public LockSettingsStorage getStorage() {
            final LockSettingsStorage storage = new LockSettingsStorage(this.mContext);
            storage.setDatabaseOnCreateCallback(new LockSettingsStorage.Callback() { // from class: com.android.server.locksettings.LockSettingsService.Injector.1
                @Override // com.android.server.locksettings.LockSettingsStorage.Callback
                public void initialize(SQLiteDatabase db) {
                    boolean lockScreenDisable = SystemProperties.getBoolean("ro.lockscreen.disable.default", false);
                    if (lockScreenDisable) {
                        storage.writeKeyValue(db, "lockscreen.disabled", "1", 0);
                    }
                }
            });
            return storage;
        }

        public LockSettingsStrongAuth getStrongAuth() {
            return new LockSettingsStrongAuth(this.mContext);
        }

        public SynchronizedStrongAuthTracker getStrongAuthTracker() {
            return new SynchronizedStrongAuthTracker(this.mContext);
        }

        public IActivityManager getActivityManager() {
            return ActivityManager.getService();
        }

        public NotificationManager getNotificationManager() {
            return (NotificationManager) this.mContext.getSystemService("notification");
        }

        public UserManager getUserManager() {
            return (UserManager) this.mContext.getSystemService("user");
        }

        public UserManagerInternal getUserManagerInternal() {
            return (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }

        public DevicePolicyManager getDevicePolicyManager() {
            return (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        }

        public DeviceStateCache getDeviceStateCache() {
            return DeviceStateCache.getInstance();
        }

        public android.security.KeyStore getKeyStore() {
            return android.security.KeyStore.getInstance();
        }

        public RecoverableKeyStoreManager getRecoverableKeyStoreManager() {
            return RecoverableKeyStoreManager.getInstance(this.mContext);
        }

        public IStorageManager getStorageManager() {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                return IStorageManager.Stub.asInterface(service);
            }
            return null;
        }

        public SyntheticPasswordManager getSyntheticPasswordManager(LockSettingsStorage storage) {
            return new SyntheticPasswordManager(getContext(), storage, getUserManager(), new PasswordSlotManager());
        }

        public RebootEscrowManager getRebootEscrowManager(RebootEscrowManager.Callbacks callbacks, LockSettingsStorage storage) {
            return new RebootEscrowManager(this.mContext, callbacks, storage);
        }

        public boolean hasEnrolledBiometrics(int userId) {
            BiometricManager bm = (BiometricManager) this.mContext.getSystemService(BiometricManager.class);
            return bm.hasEnrolledBiometrics(userId);
        }

        public int binderGetCallingUid() {
            return Binder.getCallingUid();
        }

        public boolean isGsiRunning() {
            return SystemProperties.getInt(LockSettingsService.GSI_RUNNING_PROP, 0) > 0;
        }

        public FingerprintManager getFingerprintManager() {
            if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
                return (FingerprintManager) this.mContext.getSystemService("fingerprint");
            }
            return null;
        }

        public FaceManager getFaceManager() {
            if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.biometrics.face")) {
                return (FaceManager) this.mContext.getSystemService("face");
            }
            return null;
        }

        public int settingsGlobalGetInt(ContentResolver contentResolver, String keyName, int defaultValue) {
            return Settings.Global.getInt(contentResolver, keyName, defaultValue);
        }

        public int settingsSecureGetInt(ContentResolver contentResolver, String keyName, int defaultValue, int userId) {
            return Settings.Secure.getIntForUser(contentResolver, keyName, defaultValue, userId);
        }

        public KeyStore getJavaKeyStore() {
            try {
                KeyStore ks = KeyStore.getInstance(SyntheticPasswordCrypto.androidKeystoreProviderName());
                ks.load(new AndroidKeyStoreLoadStoreParameter(SyntheticPasswordCrypto.keyNamespace()));
                return ks;
            } catch (Exception e) {
                throw new IllegalStateException("Cannot load keystore", e);
            }
        }

        public ManagedProfilePasswordCache getManagedProfilePasswordCache(KeyStore ks) {
            return new ManagedProfilePasswordCache(ks, getUserManager());
        }
    }

    /* loaded from: classes.dex */
    private class StorageWatcher extends Watcher {
        private StorageWatcher() {
        }

        @Override // com.android.server.utils.Watcher
        public void onChange(Watchable what) {
            LockSettingsService.this.onChange();
        }
    }

    public LockSettingsService(Context context) {
        this(new Injector(context));
    }

    protected LockSettingsService(Injector injector) {
        this.mSeparateChallengeLock = new Object();
        this.mDeviceProvisionedObserver = new DeviceProvisionedObserver();
        this.mUserPasswordMetrics = new SparseArray<>();
        this.FLAG_CLEAR_LOCK_WITHOUT_PASSWORD_CONFIRM = 4;
        this.mDiskCacheKeyMap = new HashMap<>();
        this.mUserManagerCache = new HashMap<>();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.locksettings.LockSettingsService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int userHandle;
                if ("android.intent.action.USER_ADDED".equals(intent.getAction())) {
                    AndroidKeyStoreMaintenance.onUserAdded(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_STARTING".equals(intent.getAction())) {
                    LockSettingsService.this.mStorage.prefetchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_REMOVED".equals(intent.getAction()) && (userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0)) > 0) {
                    LockSettingsService.this.removeUser(userHandle, false);
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        this.mInjector = injector;
        Context context = injector.getContext();
        this.mContext = context;
        this.mKeyStore = injector.getKeyStore();
        KeyStore javaKeyStore = injector.getJavaKeyStore();
        this.mJavaKeyStore = javaKeyStore;
        this.mRecoverableKeyStoreManager = injector.getRecoverableKeyStoreManager();
        Handler handler = injector.getHandler(injector.getServiceThread());
        this.mHandler = handler;
        LockSettingsStrongAuth strongAuth = injector.getStrongAuth();
        this.mStrongAuth = strongAuth;
        this.mActivityManager = injector.getActivityManager();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_STARTING");
        filter.addAction("android.intent.action.USER_REMOVED");
        injector.getContext().registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, filter, null, null);
        LockSettingsStorage storage = injector.getStorage();
        this.mStorage = storage;
        this.mNotificationManager = injector.getNotificationManager();
        this.mUserManager = injector.getUserManager();
        this.mStorageManager = injector.getStorageManager();
        SynchronizedStrongAuthTracker strongAuthTracker = injector.getStrongAuthTracker();
        this.mStrongAuthTracker = strongAuthTracker;
        strongAuthTracker.register(strongAuth);
        this.mGatekeeperPasswords = new LongSparseArray<>();
        this.mRandom = new SecureRandom();
        SyntheticPasswordManager syntheticPasswordManager = injector.getSyntheticPasswordManager(storage);
        this.mSpManager = syntheticPasswordManager;
        this.mManagedProfilePasswordCache = injector.getManagedProfilePasswordCache(javaKeyStore);
        this.mBiometricDeferredQueue = new BiometricDeferredQueue(context, syntheticPasswordManager, handler);
        this.mRebootEscrowManager = injector.getRebootEscrowManager(new RebootEscrowCallbacks(), storage);
        LocalServices.addService(LockSettingsInternal.class, new LocalService());
        StorageWatcher storageWatcher = new StorageWatcher();
        this.mStorageWatcher = storageWatcher;
        storage.registerObserver(storageWatcher);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onChange() {
        LockPatternUtils.invalidateCredentialTypeCache();
    }

    private void maybeShowEncryptionNotificationForUser(int userId, String reason) {
        UserInfo parent;
        UserInfo user = this.mUserManager.getUserInfo(userId);
        if (!user.isManagedProfile() || isUserKeyUnlocked(userId)) {
            return;
        }
        UserHandle userHandle = user.getUserHandle();
        boolean isSecure = isUserSecure(userId);
        if (isSecure && !this.mUserManager.isUserUnlockingOrUnlocked(userHandle) && (parent = this.mUserManager.getProfileParent(userId)) != null && this.mUserManager.isUserUnlockingOrUnlocked(parent.getUserHandle()) && !this.mUserManager.isQuietModeEnabled(userHandle)) {
            showEncryptionNotificationForProfile(userHandle, reason);
        }
    }

    private void showEncryptionNotificationForProfile(UserHandle user, String reason) {
        this.mContext.getResources();
        CharSequence title = getEncryptionNotificationTitle();
        CharSequence message = getEncryptionNotificationMessage();
        CharSequence detail = getEncryptionNotificationDetail();
        KeyguardManager km = (KeyguardManager) this.mContext.getSystemService("keyguard");
        Intent unlockIntent = km.createConfirmDeviceCredentialIntent(null, null, user.getIdentifier());
        if (unlockIntent != null && StorageManager.isFileEncryptedNativeOrEmulated()) {
            unlockIntent.setFlags(276824064);
            PendingIntent intent = PendingIntent.getActivity(this.mContext, 0, unlockIntent, AudioFormat.E_AC3);
            Slog.d(TAG, String.format("showing encryption notification, user: %d; reason: %s", Integer.valueOf(user.getIdentifier()), reason));
            showEncryptionNotification(user, title, message, detail, intent);
        }
    }

    private String getEncryptionNotificationTitle() {
        return this.mInjector.getDevicePolicyManager().getResources().getString("Core.PROFILE_ENCRYPTED_TITLE", new Supplier() { // from class: com.android.server.locksettings.LockSettingsService$$ExternalSyntheticLambda4
            @Override // java.util.function.Supplier
            public final Object get() {
                return LockSettingsService.this.m4568xb0e89141();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getEncryptionNotificationTitle$0$com-android-server-locksettings-LockSettingsService  reason: not valid java name */
    public /* synthetic */ String m4568xb0e89141() {
        return this.mContext.getString(17041354);
    }

    private String getEncryptionNotificationDetail() {
        return this.mInjector.getDevicePolicyManager().getResources().getString("Core.PROFILE_ENCRYPTED_DETAIL", new Supplier() { // from class: com.android.server.locksettings.LockSettingsService$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                return LockSettingsService.this.m4566x5e5450e9();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getEncryptionNotificationDetail$1$com-android-server-locksettings-LockSettingsService  reason: not valid java name */
    public /* synthetic */ String m4566x5e5450e9() {
        return this.mContext.getString(17041352);
    }

    private String getEncryptionNotificationMessage() {
        return this.mInjector.getDevicePolicyManager().getResources().getString("Core.PROFILE_ENCRYPTED_MESSAGE", new Supplier() { // from class: com.android.server.locksettings.LockSettingsService$$ExternalSyntheticLambda6
            @Override // java.util.function.Supplier
            public final Object get() {
                return LockSettingsService.this.m4567x1f30f672();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getEncryptionNotificationMessage$2$com-android-server-locksettings-LockSettingsService  reason: not valid java name */
    public /* synthetic */ String m4567x1f30f672() {
        return this.mContext.getString(17041353);
    }

    private void showEncryptionNotification(UserHandle user, CharSequence title, CharSequence message, CharSequence detail, PendingIntent intent) {
        Notification notification = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17302908).setWhen(0L).setOngoing(true).setTicker(title).setColor(this.mContext.getColor(17170460)).setContentTitle(title).setContentText(message).setSubText(detail).setVisibility(1).setContentIntent(intent).build();
        this.mNotificationManager.notifyAsUser(null, 9, notification, user);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideEncryptionNotification(UserHandle userHandle) {
        Slog.d(TAG, "hide encryption notification, user: " + userHandle.getIdentifier());
        this.mNotificationManager.cancelAsUser(null, 9, userHandle);
    }

    public void onCleanupUser(int userId) {
        hideEncryptionNotification(new UserHandle(userId));
        int strongAuthRequired = LockPatternUtils.StrongAuthTracker.getDefaultFlags(this.mContext);
        requireStrongAuth(strongAuthRequired, userId);
        synchronized (this) {
            this.mUserPasswordMetrics.remove(userId);
        }
    }

    public void onStartUser(int userId) {
        maybeShowEncryptionNotificationForUser(userId, "user started");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupDataForReusedUserIdIfNecessary(int userId) {
        int serialNumber;
        int storedSerialNumber;
        if (userId != 0 && (storedSerialNumber = this.mStorage.getInt(USER_SERIAL_NUMBER_KEY, -1, userId)) != (serialNumber = this.mUserManager.getUserSerialNumber(userId))) {
            if (storedSerialNumber != -1) {
                removeUser(userId, true);
            }
            this.mStorage.setInt(USER_SERIAL_NUMBER_KEY, serialNumber, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ensureProfileKeystoreUnlocked(int userId) {
        android.security.KeyStore ks = android.security.KeyStore.getInstance();
        if (ks.state(userId) == KeyStore.State.LOCKED && isCredentialSharableWithParent(userId) && hasUnifiedChallenge(userId)) {
            Slog.i(TAG, "Profile got unlocked, will unlock its keystore");
            unlockChildProfile(userId, true);
        }
    }

    public void onUnlockUser(final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.LockSettingsService.1
            @Override // java.lang.Runnable
            public void run() {
                LockSettingsService.this.cleanupDataForReusedUserIdIfNecessary(userId);
                LockSettingsService.this.ensureProfileKeystoreUnlocked(userId);
                LockSettingsService.this.hideEncryptionNotification(new UserHandle(userId));
                if (LockSettingsService.this.isCredentialSharableWithParent(userId)) {
                    LockSettingsService.this.tieProfileLockIfNecessary(userId, LockscreenCredential.createNone());
                }
                if (LockSettingsService.this.mUserManager.getUserInfo(userId).isPrimary() && !LockSettingsService.this.isUserSecure(userId)) {
                    LockSettingsService.this.tryDeriveAuthTokenForUnsecuredPrimaryUser(userId);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryDeriveAuthTokenForUnsecuredPrimaryUser(int userId) {
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                long handle = getSyntheticPasswordHandleLocked(userId);
                SyntheticPasswordManager.AuthenticationResult result = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), handle, LockscreenCredential.createNone(), userId, null);
                if (result.authToken != null) {
                    Slog.i(TAG, "Retrieved auth token for user " + userId);
                    onAuthTokenKnownForUser(userId, result.authToken);
                } else {
                    Slog.e(TAG, "Auth token not available for user " + userId);
                }
            }
        }
    }

    public void systemReady() {
        if (this.mContext.checkCallingOrSelfPermission(PERMISSION) != 0) {
            EventLog.writeEvent(1397638484, "28251513", Integer.valueOf(getCallingUid()), "");
        }
        checkWritePermission(0);
        this.mHasSecureLockScreen = this.mContext.getPackageManager().hasSystemFeature("android.software.secure_lock_screen");
        migrateOldData();
        getGateKeeperService();
        this.mSpManager.initWeaverService();
        getAuthSecretHal();
        this.mDeviceProvisionedObserver.onSystemReady();
        this.mStorage.prefetchUser(0);
        this.mBiometricDeferredQueue.systemReady(this.mInjector.getFingerprintManager(), this.mInjector.getFaceManager());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadEscrowData() {
        this.mRebootEscrowManager.loadRebootEscrowDataIfAvailable(this.mHandler);
    }

    private void getAuthSecretHal() {
        try {
            this.mAuthSecretService = IAuthSecret.getService(true);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to get AuthSecret HAL", e);
        } catch (NoSuchElementException e2) {
            Slog.i(TAG, "Device doesn't implement AuthSecret HAL");
        }
    }

    private void migrateOldData() {
        boolean success;
        String[] strArr;
        if (getString("migrated", null, 0) == null) {
            ContentResolver cr = this.mContext.getContentResolver();
            for (String validSetting : VALID_SETTINGS) {
                String value = Settings.Secure.getStringForUser(cr, validSetting, cr.getUserId());
                if (value != null) {
                    setString(validSetting, value, 0);
                }
            }
            setString("migrated", "true", 0);
            Slog.i(TAG, "Migrated lock settings to new location");
        }
        if (getString("migrated_user_specific", null, 0) == null) {
            ContentResolver cr2 = this.mContext.getContentResolver();
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int user = 0; user < users.size(); user++) {
                int userId = users.get(user).id;
                String ownerInfo = Settings.Secure.getStringForUser(cr2, "lock_screen_owner_info", userId);
                if (!TextUtils.isEmpty(ownerInfo)) {
                    setString("lock_screen_owner_info", ownerInfo, userId);
                    Settings.Secure.putStringForUser(cr2, "lock_screen_owner_info", "", userId);
                }
                try {
                    int ivalue = Settings.Secure.getIntForUser(cr2, "lock_screen_owner_info_enabled", userId);
                    boolean enabled = ivalue != 0;
                    setLong("lock_screen_owner_info_enabled", enabled ? 1L : 0L, userId);
                } catch (Settings.SettingNotFoundException e) {
                    if (!TextUtils.isEmpty(ownerInfo)) {
                        setLong("lock_screen_owner_info_enabled", 1L, userId);
                    }
                }
                Settings.Secure.putIntForUser(cr2, "lock_screen_owner_info_enabled", 0, userId);
            }
            setString("migrated_user_specific", "true", 0);
            Slog.i(TAG, "Migrated per-user lock settings to new location");
        }
        if (getString("migrated_biometric_weak", null, 0) == null) {
            List<UserInfo> users2 = this.mUserManager.getUsers();
            for (int i = 0; i < users2.size(); i++) {
                int userId2 = users2.get(i).id;
                long type = getLong("lockscreen.password_type", 0L, userId2);
                long alternateType = getLong("lockscreen.password_type_alternate", 0L, userId2);
                if (type == 32768) {
                    setLong("lockscreen.password_type", alternateType, userId2);
                }
                setLong("lockscreen.password_type_alternate", 0L, userId2);
            }
            setString("migrated_biometric_weak", "true", 0);
            Slog.i(TAG, "Migrated biometric weak to use the fallback instead");
        }
        if (getString("migrated_lockscreen_disabled", null, 0) == null) {
            List<UserInfo> users3 = this.mUserManager.getUsers();
            int userCount = users3.size();
            int switchableUsers = 0;
            for (int i2 = 0; i2 < userCount; i2++) {
                if (users3.get(i2).supportsSwitchTo()) {
                    switchableUsers++;
                }
            }
            if (switchableUsers > 1) {
                for (int i3 = 0; i3 < userCount; i3++) {
                    int id = users3.get(i3).id;
                    if (getBoolean("lockscreen.disabled", false, id)) {
                        setBoolean("lockscreen.disabled", false, id);
                    }
                }
            }
            setString("migrated_lockscreen_disabled", "true", 0);
            Slog.i(TAG, "Migrated lockscreen disabled flag");
        }
        boolean isWatch = this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        if (isWatch && getString("migrated_wear_lockscreen_disabled", null, 0) == null) {
            List<UserInfo> users4 = this.mUserManager.getUsers();
            int userCount2 = users4.size();
            for (int i4 = 0; i4 < userCount2; i4++) {
                setBoolean("lockscreen.disabled", false, users4.get(i4).id);
            }
            setString("migrated_wear_lockscreen_disabled", "true", 0);
            Slog.i(TAG, "Migrated lockscreen_disabled for Wear devices");
        }
        if (getString("migrated_keystore_namespace", null, 0) == null) {
            synchronized (this.mSpManager) {
                success = true & this.mSpManager.migrateKeyNamespace();
            }
            if (migrateProfileLockKeys() & success) {
                setString("migrated_keystore_namespace", "true", 0);
                Slog.i(TAG, "Migrated keys to LSS namespace");
                return;
            }
            Slog.w(TAG, "Failed to migrate keys to LSS namespace");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void migrateOldDataAfterSystemReady() {
        if (LockPatternUtils.frpCredentialEnabled(this.mContext) && !getBoolean("migrated_frp", false, 0)) {
            migrateFrpCredential();
            setBoolean("migrated_frp", true, 0);
            Slog.i(TAG, "Migrated migrated_frp.");
        }
    }

    private void migrateFrpCredential() {
        if (this.mStorage.readPersistentDataBlock() != LockSettingsStorage.PersistentData.NONE) {
            return;
        }
        for (UserInfo userInfo : this.mUserManager.getUsers()) {
            if (LockPatternUtils.userOwnsFrpCredential(this.mContext, userInfo) && isUserSecure(userInfo.id)) {
                synchronized (this.mSpManager) {
                    if (isSyntheticPasswordBasedCredentialLocked(userInfo.id)) {
                        int actualQuality = (int) getLong("lockscreen.password_type", 0L, userInfo.id);
                        this.mSpManager.migrateFrpPasswordLocked(getSyntheticPasswordHandleLocked(userInfo.id), userInfo, redactActualQualityToMostLenientEquivalentQuality(actualQuality));
                    }
                }
                return;
            }
        }
    }

    private boolean migrateProfileLockKeys() {
        boolean success = true;
        List<UserInfo> users = this.mUserManager.getUsers();
        int userCount = users.size();
        for (int i = 0; i < userCount; i++) {
            UserInfo user = users.get(i);
            if (isCredentialSharableWithParent(user.id) && !getSeparateProfileChallengeEnabledInternal(user.id)) {
                success = success & SyntheticPasswordCrypto.migrateLockSettingsKey("profile_key_name_encrypt_" + user.id) & SyntheticPasswordCrypto.migrateLockSettingsKey("profile_key_name_decrypt_" + user.id);
            }
        }
        return success;
    }

    private int redactActualQualityToMostLenientEquivalentQuality(int quality) {
        switch (quality) {
            case 131072:
            case 196608:
                return 131072;
            case 262144:
            case 327680:
            case 393216:
                return 262144;
            default:
                return quality;
        }
    }

    private void enforceFrpResolved() {
        ContentResolver cr = this.mContext.getContentResolver();
        boolean inSetupWizard = this.mInjector.settingsSecureGetInt(cr, "user_setup_complete", 0, 0) == 0;
        boolean secureFrp = this.mInjector.settingsSecureGetInt(cr, "secure_frp_mode", 0, 0) == 1;
        if (inSetupWizard && secureFrp) {
            throw new SecurityException("Cannot change credential in SUW while factory reset protection is not resolved yet");
        }
    }

    private final void checkWritePermission(int userId) {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsWrite");
    }

    private final void checkPasswordReadPermission() {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsRead");
    }

    private final void checkPasswordHavePermission(int userId) {
        if (this.mContext.checkCallingOrSelfPermission(PERMISSION) != 0) {
            EventLog.writeEvent(1397638484, "28251513", Integer.valueOf(getCallingUid()), "");
        }
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsHave");
    }

    private final void checkReadPermission(String requestedKey, int userId) {
        int callingUid = Binder.getCallingUid();
        int i = 0;
        while (true) {
            String[] strArr = READ_CONTACTS_PROTECTED_SETTINGS;
            if (i < strArr.length) {
                String key = strArr[i];
                if (!key.equals(requestedKey) || this.mContext.checkCallingOrSelfPermission("android.permission.READ_CONTACTS") == 0) {
                    i++;
                } else {
                    throw new SecurityException("uid=" + callingUid + " needs permission android.permission.READ_CONTACTS to read " + requestedKey + " for user " + userId);
                }
            } else {
                int i2 = 0;
                while (true) {
                    String[] strArr2 = READ_PASSWORD_PROTECTED_SETTINGS;
                    if (i2 < strArr2.length) {
                        String key2 = strArr2[i2];
                        if (!key2.equals(requestedKey) || this.mContext.checkCallingOrSelfPermission(PERMISSION) == 0) {
                            i2++;
                        } else {
                            throw new SecurityException("uid=" + callingUid + " needs permission " + PERMISSION + " to read " + requestedKey + " for user " + userId);
                        }
                    } else {
                        return;
                    }
                }
            }
        }
    }

    private final void checkBiometricPermission() {
        this.mContext.enforceCallingOrSelfPermission(BIOMETRIC_PERMISSION, "LockSettingsBiometric");
    }

    private boolean hasPermission(String permission) {
        return this.mContext.checkCallingOrSelfPermission(permission) == 0;
    }

    private void checkManageWeakEscrowTokenMethodUsage() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_WEAK_ESCROW_TOKEN", "Requires MANAGE_WEAK_ESCROW_TOKEN permission.");
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
            throw new IllegalArgumentException("Weak escrow token are only for automotive devices.");
        }
    }

    public boolean hasSecureLockScreen() {
        return this.mHasSecureLockScreen;
    }

    public boolean getSeparateProfileChallengeEnabled(int userId) {
        checkReadPermission(SEPARATE_PROFILE_CHALLENGE_KEY, userId);
        return getSeparateProfileChallengeEnabledInternal(userId);
    }

    private boolean getSeparateProfileChallengeEnabledInternal(int userId) {
        boolean z;
        synchronized (this.mSeparateChallengeLock) {
            z = this.mStorage.getBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, false, userId);
        }
        return z;
    }

    public void setSeparateProfileChallengeEnabled(int userId, boolean enabled, LockscreenCredential profileUserPassword) {
        checkWritePermission(userId);
        if (!this.mHasSecureLockScreen && profileUserPassword != null && profileUserPassword.getType() != -1) {
            throw new UnsupportedOperationException("This operation requires secure lock screen feature.");
        }
        synchronized (this.mSeparateChallengeLock) {
            setSeparateProfileChallengeEnabledLocked(userId, enabled, profileUserPassword != null ? profileUserPassword : LockscreenCredential.createNone());
        }
        notifySeparateProfileChallengeChanged(userId);
    }

    private void setSeparateProfileChallengeEnabledLocked(int userId, boolean enabled, LockscreenCredential profileUserPassword) {
        boolean old = getBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, false, userId);
        setBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, enabled, userId);
        try {
            if (enabled) {
                this.mStorage.removeChildProfileLock(userId);
                removeKeystoreProfileKey(userId);
                return;
            }
            tieProfileLockIfNecessary(userId, profileUserPassword);
        } catch (IllegalStateException e) {
            setBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, old, userId);
            throw e;
        }
    }

    private void notifySeparateProfileChallengeChanged(final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.LockSettingsService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LockSettingsService.lambda$notifySeparateProfileChallengeChanged$3(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifySeparateProfileChallengeChanged$3(int userId) {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (dpmi != null) {
            dpmi.reportSeparateProfileChallengeChanged(userId);
        }
    }

    public void setBoolean(String key, boolean value, int userId) {
        checkWritePermission(userId);
        this.mStorage.setBoolean(key, value, userId);
    }

    public void setLong(String key, long value, int userId) {
        checkWritePermission(userId);
        this.mStorage.setLong(key, value, userId);
    }

    public void setString(String key, String value, int userId) {
        checkWritePermission(userId);
        this.mStorage.setString(key, value, userId);
    }

    public boolean getBoolean(String key, boolean defaultValue, int userId) {
        checkReadPermission(key, userId);
        if ("lock_pattern_autolock".equals(key)) {
            return getCredentialTypeInternal(userId) == 1;
        }
        return this.mStorage.getBoolean(key, defaultValue, userId);
    }

    public long getLong(String key, long defaultValue, int userId) {
        checkReadPermission(key, userId);
        return this.mStorage.getLong(key, defaultValue, userId);
    }

    public String getString(String key, String defaultValue, int userId) {
        checkReadPermission(key, userId);
        return this.mStorage.getString(key, defaultValue, userId);
    }

    private void setKeyguardStoredQuality(int quality, int userId) {
        this.mStorage.setLong("lockscreen.password_type", quality, userId);
    }

    private int getKeyguardStoredQuality(int userId) {
        return (int) this.mStorage.getLong("lockscreen.password_type", 0L, userId);
    }

    public int getCredentialType(int userId) {
        checkPasswordHavePermission(userId);
        return getCredentialTypeInternal(userId);
    }

    public int getCredentialTypeInternal(int userId) {
        if (userId == -9999) {
            return getFrpCredentialType();
        }
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                long handle = getSyntheticPasswordHandleLocked(userId);
                int rawType = this.mSpManager.getCredentialType(handle, userId);
                if (rawType != 2) {
                    return rawType;
                }
                return pinOrPasswordQualityToCredentialType(getKeyguardStoredQuality(userId));
            }
            int savedQuality = getKeyguardStoredQuality(userId);
            if (savedQuality == 65536 && this.mStorage.hasPattern(userId)) {
                return 1;
            }
            if (savedQuality >= 131072 && this.mStorage.hasPassword(userId)) {
                return pinOrPasswordQualityToCredentialType(savedQuality);
            }
            return -1;
        }
    }

    private int getFrpCredentialType() {
        LockSettingsStorage.PersistentData data = this.mStorage.readPersistentDataBlock();
        if (data.type != 1 && data.type != 2) {
            return -1;
        }
        int credentialType = SyntheticPasswordManager.getFrpCredentialType(data.payload);
        if (credentialType != 2) {
            return credentialType;
        }
        return pinOrPasswordQualityToCredentialType(data.qualityForUi);
    }

    private static int pinOrPasswordQualityToCredentialType(int quality) {
        if (LockPatternUtils.isQualityAlphabeticPassword(quality)) {
            return 4;
        }
        if (LockPatternUtils.isQualityNumericPin(quality)) {
            return 3;
        }
        throw new IllegalArgumentException("Quality is neither Pin nor password: " + quality);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isUserSecure(int userId) {
        return getCredentialTypeInternal(userId) != -1;
    }

    void setKeystorePassword(byte[] password, int userHandle) {
        AndroidKeyStoreMaintenance.onUserPasswordChanged(userHandle, password);
    }

    private void unlockKeystore(byte[] password, int userHandle) {
        Authorization.onLockScreenEvent(false, userHandle, password, (long[]) null);
    }

    protected LockscreenCredential getDecryptedPasswordForTiedProfile(int userId) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, CertificateException, IOException {
        byte[] storedData = this.mStorage.readChildProfileLock(userId);
        if (storedData == null) {
            throw new FileNotFoundException("Child profile lock file not found");
        }
        byte[] iv = Arrays.copyOfRange(storedData, 0, 12);
        byte[] encryptedPassword = Arrays.copyOfRange(storedData, 12, storedData.length);
        SecretKey decryptionKey = (SecretKey) this.mJavaKeyStore.getKey("profile_key_name_decrypt_" + userId, null);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, decryptionKey, new GCMParameterSpec(128, iv));
        byte[] decryptionResult = cipher.doFinal(encryptedPassword);
        LockscreenCredential credential = LockscreenCredential.createManagedPassword(decryptionResult);
        Arrays.fill(decryptionResult, (byte) 0);
        this.mManagedProfilePasswordCache.storePassword(userId, credential);
        return credential;
    }

    private void unlockChildProfile(int profileHandle, boolean ignoreUserNotAuthenticated) {
        try {
            doVerifyCredential(getDecryptedPasswordForTiedProfile(profileHandle), profileHandle, null, 0);
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            if (e instanceof FileNotFoundException) {
                Slog.i(TAG, "Child profile key not found");
            } else if (ignoreUserNotAuthenticated && (e instanceof UserNotAuthenticatedException)) {
                Slog.i(TAG, "Parent keystore seems locked, ignoring");
            } else {
                Slog.e(TAG, "Failed to decrypt child profile key", e);
            }
        }
    }

    private void unlockUser(int userId, byte[] secret) {
        Slog.i(TAG, "Unlocking user " + userId + " with secret only, length " + (secret != null ? secret.length : 0));
        boolean alreadyUnlocked = this.mUserManager.isUserUnlockingOrUnlocked(userId);
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            this.mActivityManager.unlockUser(userId, (byte[]) null, secret, new IProgressListener.Stub() { // from class: com.android.server.locksettings.LockSettingsService.3
                public void onStarted(int id, Bundle extras) throws RemoteException {
                    Slog.d(LockSettingsService.TAG, "unlockUser started");
                }

                public void onProgress(int id, int progress, Bundle extras) throws RemoteException {
                    Slog.d(LockSettingsService.TAG, "unlockUser progress " + progress);
                }

                public void onFinished(int id, Bundle extras) throws RemoteException {
                    Slog.d(LockSettingsService.TAG, "unlockUser finished");
                    latch.countDown();
                }
            });
            try {
                latch.await(15L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (isCredentialSharableWithParent(userId)) {
                if (!hasUnifiedChallenge(userId)) {
                    this.mBiometricDeferredQueue.processPendingLockoutResets();
                    return;
                }
                return;
            }
            for (UserInfo profile : this.mUserManager.getProfiles(userId)) {
                if (profile.id != userId && isCredentialSharableWithParent(profile.id)) {
                    if (hasUnifiedChallenge(profile.id)) {
                        if (this.mUserManager.isUserRunning(profile.id)) {
                            unlockChildProfile(profile.id, false);
                        } else {
                            try {
                                getDecryptedPasswordForTiedProfile(profile.id);
                            } catch (IOException | GeneralSecurityException e2) {
                                Slog.d(TAG, "Cache work profile password failed", e2);
                            }
                        }
                    }
                    if (alreadyUnlocked) {
                        continue;
                    } else {
                        long ident = clearCallingIdentity();
                        try {
                            maybeShowEncryptionNotificationForUser(profile.id, "parent unlocked");
                        } finally {
                            restoreCallingIdentity(ident);
                        }
                    }
                }
            }
            this.mBiometricDeferredQueue.processPendingLockoutResets();
        } catch (RemoteException e3) {
            throw e3.rethrowAsRuntimeException();
        }
    }

    private boolean hasUnifiedChallenge(int userId) {
        return !getSeparateProfileChallengeEnabledInternal(userId) && this.mStorage.hasChildProfileLock(userId);
    }

    private Map<Integer, LockscreenCredential> getDecryptedPasswordsForAllTiedProfiles(int userId) {
        if (isCredentialSharableWithParent(userId)) {
            return null;
        }
        Map<Integer, LockscreenCredential> result = new ArrayMap<>();
        List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
        int size = profiles.size();
        for (int i = 0; i < size; i++) {
            UserInfo profile = profiles.get(i);
            if (isCredentialSharableWithParent(profile.id)) {
                int profileUserId = profile.id;
                if (!getSeparateProfileChallengeEnabledInternal(profileUserId)) {
                    try {
                        result.put(Integer.valueOf(profileUserId), getDecryptedPasswordForTiedProfile(profileUserId));
                    } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
                        Slog.e(TAG, "getDecryptedPasswordsForAllTiedProfiles failed for user " + profileUserId, e);
                    }
                }
            }
        }
        return result;
    }

    private void synchronizeUnifiedWorkChallengeForProfiles(int userId, Map<Integer, LockscreenCredential> profilePasswordMap) {
        if (isCredentialSharableWithParent(userId)) {
            return;
        }
        boolean isSecure = isUserSecure(userId);
        List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
        int size = profiles.size();
        for (int i = 0; i < size; i++) {
            UserInfo profile = profiles.get(i);
            int profileUserId = profile.id;
            if (isCredentialSharableWithParent(profileUserId) && !getSeparateProfileChallengeEnabledInternal(profileUserId)) {
                if (isSecure) {
                    tieProfileLockIfNecessary(profileUserId, LockscreenCredential.createNone());
                } else if (profilePasswordMap != null && profilePasswordMap.containsKey(Integer.valueOf(profileUserId))) {
                    setLockCredentialInternal(LockscreenCredential.createNone(), profilePasswordMap.get(Integer.valueOf(profileUserId)), profileUserId, true);
                    this.mStorage.removeChildProfileLock(profileUserId);
                    removeKeystoreProfileKey(profileUserId);
                } else {
                    Slog.wtf(TAG, "Attempt to clear tied challenge, but no password supplied.");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isProfileWithUnifiedLock(int userId) {
        return isCredentialSharableWithParent(userId) && !getSeparateProfileChallengeEnabledInternal(userId);
    }

    private boolean isProfileWithSeparatedLock(int userId) {
        return isCredentialSharableWithParent(userId) && getSeparateProfileChallengeEnabledInternal(userId);
    }

    private void sendCredentialsOnUnlockIfRequired(LockscreenCredential credential, int userId) {
        if (userId == -9999 || isProfileWithUnifiedLock(userId)) {
            return;
        }
        byte[] secret = credential.isNone() ? null : credential.getCredential();
        for (Integer num : getProfilesWithSameLockScreen(userId)) {
            int profileId = num.intValue();
            this.mRecoverableKeyStoreManager.lockScreenSecretAvailable(credential.getType(), secret, profileId);
        }
    }

    private void sendCredentialsOnChangeIfRequired(LockscreenCredential credential, int userId, boolean isLockTiedToParent) {
        if (isLockTiedToParent) {
            return;
        }
        byte[] secret = credential.isNone() ? null : credential.getCredential();
        for (Integer num : getProfilesWithSameLockScreen(userId)) {
            int profileId = num.intValue();
            this.mRecoverableKeyStoreManager.lockScreenSecretChanged(credential.getType(), secret, profileId);
        }
    }

    private Set<Integer> getProfilesWithSameLockScreen(int userId) {
        Set<Integer> profiles = new ArraySet<>();
        for (UserInfo profile : this.mUserManager.getProfiles(userId)) {
            if (profile.id == userId || (profile.profileGroupId == userId && isProfileWithUnifiedLock(profile.id))) {
                profiles.add(Integer.valueOf(profile.id));
            }
        }
        return profiles;
    }

    public boolean setLockCredential(LockscreenCredential credential, LockscreenCredential savedCredential, int userId) {
        if (!this.mHasSecureLockScreen && credential != null && credential.getType() != -1) {
            throw new UnsupportedOperationException("This operation requires secure lock screen feature");
        }
        if (!hasPermission(PERMISSION) && !hasPermission("android.permission.SET_AND_VERIFY_LOCKSCREEN_CREDENTIALS") && (!hasPermission("android.permission.SET_INITIAL_LOCK") || !savedCredential.isNone())) {
            throw new SecurityException("setLockCredential requires SET_AND_VERIFY_LOCKSCREEN_CREDENTIALS or android.permission.ACCESS_KEYGUARD_SECURE_STORAGE");
        }
        long identity = Binder.clearCallingIdentity();
        try {
            enforceFrpResolved();
            if (!savedCredential.isNone() && isProfileWithUnifiedLock(userId)) {
                verifyCredential(savedCredential, this.mUserManager.getProfileParent(userId).id, 0);
                savedCredential.zeroize();
                savedCredential = LockscreenCredential.createNone();
            }
            synchronized (this.mSeparateChallengeLock) {
                if (!setLockCredentialInternal(credential, savedCredential, userId, false)) {
                    scheduleGc();
                    return false;
                }
                setSeparateProfileChallengeEnabledLocked(userId, true, null);
                notifyPasswordChanged(credential, userId);
                if (isCredentialSharableWithParent(userId)) {
                    setDeviceUnlockedForUser(userId);
                }
                notifySeparateProfileChallengeChanged(userId);
                onPostPasswordChanged(credential, userId);
                scheduleGc();
                return true;
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean setLockCredentialInternal(LockscreenCredential credential, LockscreenCredential savedCredential, int userId, boolean isLockTiedToParent) {
        LockscreenCredential savedCredential2;
        boolean spBasedSetLockCredentialInternalLocked;
        Objects.requireNonNull(credential);
        Objects.requireNonNull(savedCredential);
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                return spBasedSetLockCredentialInternalLocked(credential, savedCredential, userId, isLockTiedToParent);
            } else if (credential.isNone()) {
                clearUserKeyProtection(userId, null);
                gateKeeperClearSecureUserId(userId);
                this.mStorage.writeCredentialHash(LockSettingsStorage.CredentialHash.createEmptyHash(), userId);
                setKeyguardStoredQuality(0, userId);
                setKeystorePassword(null, userId);
                fixateNewestUserKeyAuth(userId);
                synchronizeUnifiedWorkChallengeForProfiles(userId, null);
                setUserPasswordMetrics(LockscreenCredential.createNone(), userId);
                sendCredentialsOnChangeIfRequired(credential, userId, isLockTiedToParent);
                return true;
            } else {
                LockSettingsStorage.CredentialHash currentHandle = this.mStorage.readCredentialHash(userId);
                if (isProfileWithUnifiedLock(userId)) {
                    if (savedCredential.isNone()) {
                        try {
                            savedCredential2 = getDecryptedPasswordForTiedProfile(userId);
                        } catch (FileNotFoundException e) {
                            Slog.i(TAG, "Child profile key not found");
                        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
                            Slog.e(TAG, "Failed to decrypt child profile key", e2);
                        }
                    }
                    savedCredential2 = savedCredential;
                } else {
                    if (currentHandle.hash == null) {
                        if (!savedCredential.isNone()) {
                            Slog.w(TAG, "Saved credential provided, but none stored");
                        }
                        savedCredential.close();
                        savedCredential2 = LockscreenCredential.createNone();
                    }
                    savedCredential2 = savedCredential;
                }
                synchronized (this.mSpManager) {
                    initializeSyntheticPasswordLocked(currentHandle.hash, savedCredential2, userId);
                    spBasedSetLockCredentialInternalLocked = spBasedSetLockCredentialInternalLocked(credential, savedCredential2, userId, isLockTiedToParent);
                }
                return spBasedSetLockCredentialInternalLocked;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPostPasswordChanged(LockscreenCredential newCredential, int userHandle) {
        if (userHandle == 0 && isDeviceEncryptionEnabled() && shouldEncryptWithCredentials() && newCredential.isNone()) {
            setCredentialRequiredToDecrypt(false);
        }
        if (newCredential.isPattern()) {
            setBoolean("lockscreen.patterneverchosen", true, userHandle);
        }
        updatePasswordHistory(newCredential, userHandle);
        ((TrustManager) this.mContext.getSystemService(TrustManager.class)).reportEnabledTrustAgentsChanged(userHandle);
    }

    private void updatePasswordHistory(LockscreenCredential password, int userHandle) {
        String passwordHistory;
        if (password.isNone() || password.isPattern()) {
            return;
        }
        String passwordHistory2 = getString("lockscreen.passwordhistory", null, userHandle);
        if (passwordHistory2 == null) {
            passwordHistory2 = "";
        }
        int passwordHistoryLength = getRequestedPasswordHistoryLength(userHandle);
        if (passwordHistoryLength == 0) {
            passwordHistory = "";
        } else {
            byte[] hashFactor = getHashFactor(password, userHandle);
            byte[] salt = getSalt(userHandle).getBytes();
            String hash = password.passwordToHistoryHash(salt, hashFactor);
            if (hash == null) {
                Slog.e(TAG, "Compute new style password hash failed, fallback to legacy style");
                hash = password.legacyPasswordToHash(salt);
            }
            if (TextUtils.isEmpty(passwordHistory2)) {
                passwordHistory = hash;
            } else {
                String[] history = passwordHistory2.split(",");
                StringJoiner joiner = new StringJoiner(",");
                joiner.add(hash);
                for (int i = 0; i < passwordHistoryLength - 1 && i < history.length; i++) {
                    joiner.add(history[i]);
                }
                passwordHistory = joiner.toString();
            }
        }
        setString("lockscreen.passwordhistory", passwordHistory, userHandle);
    }

    private String getSalt(int userId) {
        long salt = getLong("lockscreen.password_salt", 0L, userId);
        if (salt == 0) {
            try {
                salt = SecureRandom.getInstance("SHA1PRNG").nextLong();
                setLong("lockscreen.password_salt", salt, userId);
                Slog.v(TAG, "Initialized lock password salt for user: " + userId);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Couldn't get SecureRandom number", e);
            }
        }
        return Long.toHexString(salt);
    }

    private int getRequestedPasswordHistoryLength(int userId) {
        return this.mInjector.getDevicePolicyManager().getPasswordHistoryLength(null, userId);
    }

    private static boolean isDeviceEncryptionEnabled() {
        return StorageManager.isEncrypted();
    }

    private boolean shouldEncryptWithCredentials() {
        return isCredentialRequiredToDecrypt() && !isDoNotAskCredentialsOnBootSet();
    }

    private boolean isDoNotAskCredentialsOnBootSet() {
        return this.mInjector.getDevicePolicyManager().getDoNotAskCredentialsOnBoot();
    }

    private boolean isCredentialRequiredToDecrypt() {
        int value = Settings.Global.getInt(this.mContext.getContentResolver(), "require_password_to_decrypt", -1);
        return value != 0;
    }

    private UserManager getUserManagerFromCache(int userId) {
        UserHandle userHandle = UserHandle.of(userId);
        if (this.mUserManagerCache.containsKey(userHandle)) {
            return this.mUserManagerCache.get(userHandle);
        }
        try {
            Context userContext = this.mContext.createPackageContextAsUser(HostingRecord.HOSTING_TYPE_SYSTEM, 0, userHandle);
            UserManager userManager = (UserManager) userContext.getSystemService(UserManager.class);
            this.mUserManagerCache.put(userHandle, userManager);
            return userManager;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Failed to create context for user " + userHandle, e);
        }
    }

    protected boolean isCredentialSharableWithParent(int userId) {
        return getUserManagerFromCache(userId).isCredentialSharableWithParent();
    }

    private VerifyCredentialResponse convertResponse(GateKeeperResponse gateKeeperResponse) {
        return VerifyCredentialResponse.fromGateKeeperResponse(gateKeeperResponse);
    }

    private void setCredentialRequiredToDecrypt(boolean required) {
        if (isDeviceEncryptionEnabled()) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "require_password_to_decrypt", required ? 1 : 0);
        }
    }

    public boolean registerWeakEscrowTokenRemovedListener(IWeakEscrowTokenRemovedListener listener) {
        checkManageWeakEscrowTokenMethodUsage();
        long token = Binder.clearCallingIdentity();
        try {
            return this.mSpManager.registerWeakEscrowTokenRemovedListener(listener);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean unregisterWeakEscrowTokenRemovedListener(IWeakEscrowTokenRemovedListener listener) {
        checkManageWeakEscrowTokenMethodUsage();
        long token = Binder.clearCallingIdentity();
        try {
            return this.mSpManager.unregisterWeakEscrowTokenRemovedListener(listener);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public long addWeakEscrowToken(byte[] token, int userId, final IWeakEscrowTokenActivatedListener listener) {
        checkManageWeakEscrowTokenMethodUsage();
        Objects.requireNonNull(listener, "Listener can not be null.");
        LockPatternUtils.EscrowTokenStateChangeCallback internalListener = new LockPatternUtils.EscrowTokenStateChangeCallback() { // from class: com.android.server.locksettings.LockSettingsService$$ExternalSyntheticLambda2
            public final void onEscrowTokenActivated(long j, int i) {
                LockSettingsService.lambda$addWeakEscrowToken$4(listener, j, i);
            }
        };
        long restoreToken = Binder.clearCallingIdentity();
        try {
            return addEscrowToken(token, 1, userId, internalListener);
        } finally {
            Binder.restoreCallingIdentity(restoreToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addWeakEscrowToken$4(IWeakEscrowTokenActivatedListener listener, long handle, int userId1) {
        try {
            listener.onWeakEscrowTokenActivated(handle, userId1);
        } catch (RemoteException e) {
            Slog.e(TAG, "Exception while notifying weak escrow token has been activated", e);
        }
    }

    public boolean removeWeakEscrowToken(long handle, int userId) {
        checkManageWeakEscrowTokenMethodUsage();
        long token = Binder.clearCallingIdentity();
        try {
            return removeEscrowToken(handle, userId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean isWeakEscrowTokenActive(long handle, int userId) {
        checkManageWeakEscrowTokenMethodUsage();
        long token = Binder.clearCallingIdentity();
        try {
            return isEscrowTokenActive(handle, userId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2026=4] */
    public boolean isWeakEscrowTokenValid(long handle, byte[] token, int userId) {
        checkManageWeakEscrowTokenMethodUsage();
        long restoreToken = Binder.clearCallingIdentity();
        try {
            synchronized (this.mSpManager) {
                if (!this.mSpManager.hasEscrowData(userId)) {
                    Slog.w(TAG, "Escrow token is disabled on the current user");
                    return false;
                }
                SyntheticPasswordManager.AuthenticationResult authResult = this.mSpManager.unwrapWeakTokenBasedSyntheticPassword(getGateKeeperService(), handle, token, userId);
                if (authResult.authToken == null) {
                    Slog.w(TAG, "Invalid escrow token supplied");
                    return false;
                }
                return true;
            }
        } finally {
            Binder.restoreCallingIdentity(restoreToken);
        }
    }

    protected void tieProfileLockToParent(int userId, LockscreenCredential password) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            this.mJavaKeyStore.setEntry("profile_key_name_encrypt_" + userId, new KeyStore.SecretKeyEntry(secretKey), new KeyProtection.Builder(1).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
            this.mJavaKeyStore.setEntry("profile_key_name_decrypt_" + userId, new KeyStore.SecretKeyEntry(secretKey), new KeyProtection.Builder(2).setBlockModes("GCM").setEncryptionPaddings("NoPadding").setUserAuthenticationRequired(true).setUserAuthenticationValidityDurationSeconds(30).build());
            SecretKey keyStoreEncryptionKey = (SecretKey) this.mJavaKeyStore.getKey("profile_key_name_encrypt_" + userId, null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, keyStoreEncryptionKey);
            byte[] encryptionResult = cipher.doFinal(password.getCredential());
            byte[] iv = cipher.getIV();
            this.mJavaKeyStore.deleteEntry("profile_key_name_encrypt_" + userId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                if (iv.length != 12) {
                    throw new IllegalArgumentException("Invalid iv length: " + iv.length);
                }
                outputStream.write(iv);
                outputStream.write(encryptionResult);
                this.mStorage.writeChildProfileLock(userId, outputStream.toByteArray());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to concatenate byte arrays", e);
            }
        } catch (InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
            throw new IllegalStateException("Failed to encrypt key", e2);
        }
    }

    private void setUserKeyProtection(int userId, byte[] key) {
        addUserKeyAuth(userId, key);
    }

    private void clearUserKeyProtection(int userId, byte[] secret) {
        UserInfo userInfo = this.mUserManager.getUserInfo(userId);
        long callingId = Binder.clearCallingIdentity();
        try {
            try {
                this.mStorageManager.clearUserKeyAuth(userId, userInfo.serialNumber, secret);
            } catch (RemoteException e) {
                throw new IllegalStateException("clearUserKeyAuth failed user=" + userId);
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private static byte[] secretFromCredential(LockscreenCredential credential) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] personalization = "Android FBE credential hash".getBytes();
            digest.update(Arrays.copyOf(personalization, 128));
            digest.update(credential.getCredential());
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("NoSuchAlgorithmException for SHA-512");
        }
    }

    private boolean isUserKeyUnlocked(int userId) {
        try {
            return this.mStorageManager.isUserKeyUnlocked(userId);
        } catch (RemoteException e) {
            Slog.e(TAG, "failed to check user key locked state", e);
            return false;
        }
    }

    private void unlockUserKey(int userId, byte[] secret) {
        UserInfo userInfo = this.mUserManager.getUserInfo(userId);
        try {
            this.mStorageManager.unlockUserKey(userId, userInfo.serialNumber, secret);
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to unlock user key " + userId, e);
        }
    }

    private void addUserKeyAuth(int userId, byte[] secret) {
        UserInfo userInfo = this.mUserManager.getUserInfo(userId);
        long callingId = Binder.clearCallingIdentity();
        try {
            try {
                this.mStorageManager.addUserKeyAuth(userId, userInfo.serialNumber, secret);
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed to add new key to vold " + userId, e);
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private void fixateNewestUserKeyAuth(int userId) {
        long callingId = Binder.clearCallingIdentity();
        try {
            try {
                this.mStorageManager.fixateNewestUserKeyAuth(userId);
            } catch (RemoteException e) {
                Slog.w(TAG, "fixateNewestUserKeyAuth failed", e);
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2202=4] */
    public void resetKeyStore(int userId) {
        checkWritePermission(userId);
        List<Integer> profileUserIds = new ArrayList<>();
        List<LockscreenCredential> profileUserDecryptedPasswords = new ArrayList<>();
        List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
        for (UserInfo pi : profiles) {
            if (isCredentialSharableWithParent(pi.id) && !getSeparateProfileChallengeEnabledInternal(pi.id) && this.mStorage.hasChildProfileLock(pi.id)) {
                try {
                    profileUserDecryptedPasswords.add(getDecryptedPasswordForTiedProfile(pi.id));
                    profileUserIds.add(Integer.valueOf(pi.id));
                } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
                    Slog.e(TAG, "Failed to decrypt child profile key", e);
                }
            }
        }
        try {
            int[] profileIdsWithDisabled = this.mUserManager.getProfileIdsWithDisabled(userId);
            int length = profileIdsWithDisabled.length;
            for (int i = 0; i < length; i++) {
                int profileId = profileIdsWithDisabled[i];
                int[] iArr = SYSTEM_CREDENTIAL_UIDS;
                int length2 = iArr.length;
                int i2 = 0;
                while (i2 < length2) {
                    int uid = iArr[i2];
                    AndroidKeyStoreMaintenance.clearNamespace(0, UserHandle.getUid(profileId, uid));
                    i2++;
                    length = length;
                }
            }
            if (this.mUserManager.getUserInfo(userId).isPrimary()) {
                AndroidKeyStoreMaintenance.clearNamespace(2, 102L);
            }
            for (int i3 = 0; i3 < profileUserIds.size(); i3++) {
                int piUserId = profileUserIds.get(i3).intValue();
                LockscreenCredential piUserDecryptedPassword = profileUserDecryptedPasswords.get(i3);
                if (piUserId != -1 && piUserDecryptedPassword != null) {
                    tieProfileLockToParent(piUserId, piUserDecryptedPassword);
                }
                if (piUserDecryptedPassword != null) {
                    piUserDecryptedPassword.zeroize();
                }
            }
        } catch (Throwable th) {
            for (int i4 = 0; i4 < profileUserIds.size(); i4++) {
                int piUserId2 = profileUserIds.get(i4).intValue();
                LockscreenCredential piUserDecryptedPassword2 = profileUserDecryptedPasswords.get(i4);
                if (piUserId2 != -1 && piUserDecryptedPassword2 != null) {
                    tieProfileLockToParent(piUserId2, piUserDecryptedPassword2);
                }
                if (piUserDecryptedPassword2 != null) {
                    piUserDecryptedPassword2.zeroize();
                }
            }
            throw th;
        }
    }

    public VerifyCredentialResponse checkCredential(LockscreenCredential credential, int userId, ICheckCredentialProgressCallback progressCallback) {
        checkPasswordReadPermission();
        try {
            return doVerifyCredential(credential, userId, progressCallback, 0);
        } finally {
            scheduleGc();
        }
    }

    public VerifyCredentialResponse verifyCredential(LockscreenCredential credential, int userId, int flags) {
        if (!hasPermission(PERMISSION) && !hasPermission("android.permission.SET_AND_VERIFY_LOCKSCREEN_CREDENTIALS")) {
            throw new SecurityException("verifyCredential requires SET_AND_VERIFY_LOCKSCREEN_CREDENTIALS or android.permission.ACCESS_KEYGUARD_SECURE_STORAGE");
        }
        long identity = Binder.clearCallingIdentity();
        try {
            return doVerifyCredential(credential, userId, null, flags);
        } finally {
            Binder.restoreCallingIdentity(identity);
            scheduleGc();
        }
    }

    public VerifyCredentialResponse verifyGatekeeperPasswordHandle(long gatekeeperPasswordHandle, long challenge, int userId) {
        byte[] gatekeeperPassword;
        VerifyCredentialResponse response;
        checkPasswordReadPermission();
        synchronized (this.mGatekeeperPasswords) {
            gatekeeperPassword = this.mGatekeeperPasswords.get(gatekeeperPasswordHandle);
        }
        synchronized (this.mSpManager) {
            if (gatekeeperPassword == null) {
                Slog.d(TAG, "No gatekeeper password for handle");
                response = VerifyCredentialResponse.ERROR;
            } else {
                response = this.mSpManager.verifyChallengeInternal(getGateKeeperService(), gatekeeperPassword, challenge, userId);
            }
        }
        return response;
    }

    public void removeGatekeeperPasswordHandle(long gatekeeperPasswordHandle) {
        checkPasswordReadPermission();
        synchronized (this.mGatekeeperPasswords) {
            this.mGatekeeperPasswords.remove(gatekeeperPasswordHandle);
        }
    }

    private VerifyCredentialResponse doVerifyCredential(LockscreenCredential credential, int userId, ICheckCredentialProgressCallback progressCallback, int flags) {
        if (credential == null || credential.isNone()) {
            throw new IllegalArgumentException("Credential can't be null or empty");
        }
        if (userId == -9999 && this.mInjector.settingsGlobalGetInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
            Slog.e(TAG, "FRP credential can only be verified prior to provisioning.");
            return VerifyCredentialResponse.ERROR;
        }
        VerifyCredentialResponse response = spBasedDoVerifyCredential(credential, userId, progressCallback, flags);
        if (response != null) {
            if (response.getResponseCode() == 0) {
                sendCredentialsOnUnlockIfRequired(credential, userId);
            }
            return response;
        } else if (userId == -9999) {
            Slog.wtf(TAG, "Unexpected FRP credential type, should be SP based.");
            return VerifyCredentialResponse.ERROR;
        } else {
            LockSettingsStorage.CredentialHash storedHash = this.mStorage.readCredentialHash(userId);
            if (!credential.checkAgainstStoredType(storedHash.type)) {
                Slog.wtf(TAG, "doVerifyCredential type mismatch with stored credential?? stored: " + storedHash.type + " passed in: " + credential.getType());
                return VerifyCredentialResponse.ERROR;
            }
            VerifyCredentialResponse response2 = verifyCredential(userId, storedHash, credential, progressCallback);
            if (response2.getResponseCode() == 0) {
                this.mStrongAuth.reportSuccessfulStrongAuthUnlock(userId);
            }
            return response2;
        }
    }

    public VerifyCredentialResponse verifyTiedProfileChallenge(LockscreenCredential credential, int userId, int flags) {
        checkPasswordReadPermission();
        if (!isProfileWithUnifiedLock(userId)) {
            throw new IllegalArgumentException("User id must be managed/clone profile with unified lock");
        }
        int parentProfileId = this.mUserManager.getProfileParent(userId).id;
        VerifyCredentialResponse parentResponse = doVerifyCredential(credential, parentProfileId, null, flags);
        if (parentResponse.getResponseCode() == 0) {
            try {
                try {
                    return doVerifyCredential(getDecryptedPasswordForTiedProfile(userId), userId, null, flags);
                } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
                    Slog.e(TAG, "Failed to decrypt child profile key", e);
                    throw new IllegalStateException("Unable to get tied profile token");
                }
            } finally {
                scheduleGc();
            }
        }
        return parentResponse;
    }

    private VerifyCredentialResponse verifyCredential(int userId, LockSettingsStorage.CredentialHash storedHash, LockscreenCredential credential, ICheckCredentialProgressCallback progressCallback) {
        GateKeeperResponse gateKeeperResponse;
        if ((storedHash == null || storedHash.hash.length == 0) && credential.isNone()) {
            return VerifyCredentialResponse.OK;
        }
        if (storedHash == null || storedHash.hash.length == 0 || credential.isNone()) {
            return VerifyCredentialResponse.ERROR;
        }
        StrictMode.noteDiskRead();
        try {
            gateKeeperResponse = getGateKeeperService().verifyChallenge(userId, 0L, storedHash.hash, credential.getCredential());
        } catch (RemoteException e) {
            Slog.e(TAG, "gatekeeper verify failed", e);
            gateKeeperResponse = GateKeeperResponse.ERROR;
        }
        VerifyCredentialResponse response = convertResponse(gateKeeperResponse);
        boolean shouldReEnroll = gateKeeperResponse.getShouldReEnroll();
        if (response.getResponseCode() == 0) {
            if (progressCallback != null) {
                try {
                    progressCallback.onCredentialVerified();
                } catch (RemoteException e2) {
                    Slog.w(TAG, "progressCallback throws exception", e2);
                }
            }
            setUserPasswordMetrics(credential, userId);
            unlockKeystore(credential.getCredential(), userId);
            Slog.i(TAG, "Unlocking user " + userId);
            unlockUser(userId, secretFromCredential(credential));
            if (isProfileWithSeparatedLock(userId)) {
                setDeviceUnlockedForUser(userId);
            }
            if (shouldReEnroll) {
                setLockCredentialInternal(credential, credential, userId, false);
            } else {
                synchronized (this.mSpManager) {
                    if (shouldMigrateToSyntheticPasswordLocked(userId)) {
                        SyntheticPasswordManager.AuthenticationToken auth = initializeSyntheticPasswordLocked(storedHash.hash, credential, userId);
                        activateEscrowTokens(auth, userId);
                    }
                }
            }
            sendCredentialsOnUnlockIfRequired(credential, userId);
        } else if (response.getResponseCode() == 1 && response.getTimeout() > 0) {
            requireStrongAuth(8, userId);
        }
        return response;
    }

    private void setUserPasswordMetrics(LockscreenCredential password, int userHandle) {
        synchronized (this) {
            this.mUserPasswordMetrics.put(userHandle, PasswordMetrics.computeForCredential(password));
        }
    }

    PasswordMetrics getUserPasswordMetrics(int userHandle) {
        PasswordMetrics passwordMetrics;
        if (!isUserSecure(userHandle)) {
            return new PasswordMetrics(-1);
        }
        synchronized (this) {
            passwordMetrics = this.mUserPasswordMetrics.get(userHandle);
        }
        return passwordMetrics;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PasswordMetrics loadPasswordMetrics(SyntheticPasswordManager.AuthenticationToken auth, int userHandle) {
        PasswordMetrics passwordMetrics;
        synchronized (this.mSpManager) {
            passwordMetrics = this.mSpManager.getPasswordMetrics(auth, getSyntheticPasswordHandleLocked(userHandle), userHandle);
        }
        return passwordMetrics;
    }

    private void notifyPasswordChanged(final LockscreenCredential newCredential, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.LockSettingsService$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                LockSettingsService.this.m4569xb2612d08(newCredential, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyPasswordChanged$5$com-android-server-locksettings-LockSettingsService  reason: not valid java name */
    public /* synthetic */ void m4569xb2612d08(LockscreenCredential newCredential, int userId) {
        this.mInjector.getDevicePolicyManager().reportPasswordChanged(PasswordMetrics.computeForCredential(newCredential), userId);
        ((WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class)).reportPasswordChanged(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeUser(int userId, boolean unknownUser) {
        Slog.i(TAG, "RemoveUser: " + userId);
        removeBiometricsForUser(userId);
        this.mSpManager.removeUser(getGateKeeperService(), userId);
        this.mStrongAuth.removeUser(userId);
        AndroidKeyStoreMaintenance.onUserRemoved(userId);
        this.mManagedProfilePasswordCache.removePassword(userId);
        gateKeeperClearSecureUserId(userId);
        if (unknownUser || isCredentialSharableWithParent(userId)) {
            removeKeystoreProfileKey(userId);
        }
        this.mStorage.removeUser(userId);
    }

    private void removeKeystoreProfileKey(int targetUserId) {
        Slog.i(TAG, "Remove keystore profile key for user: " + targetUserId);
        try {
            this.mJavaKeyStore.deleteEntry("profile_key_name_encrypt_" + targetUserId);
            this.mJavaKeyStore.deleteEntry("profile_key_name_decrypt_" + targetUserId);
        } catch (KeyStoreException e) {
            Slog.e(TAG, "Unable to remove keystore profile key for user:" + targetUserId, e);
        }
    }

    public void registerStrongAuthTracker(IStrongAuthTracker tracker) {
        checkPasswordReadPermission();
        this.mStrongAuth.registerStrongAuthTracker(tracker);
    }

    public void unregisterStrongAuthTracker(IStrongAuthTracker tracker) {
        checkPasswordReadPermission();
        this.mStrongAuth.unregisterStrongAuthTracker(tracker);
    }

    public void requireStrongAuth(int strongAuthReason, int userId) {
        checkWritePermission(userId);
        this.mStrongAuth.requireStrongAuth(strongAuthReason, userId);
    }

    public void reportSuccessfulBiometricUnlock(boolean isStrongBiometric, int userId) {
        checkBiometricPermission();
        this.mStrongAuth.reportSuccessfulBiometricUnlock(isStrongBiometric, userId);
    }

    public void scheduleNonStrongBiometricIdleTimeout(int userId) {
        checkBiometricPermission();
        this.mStrongAuth.scheduleNonStrongBiometricIdleTimeout(userId);
    }

    public void userPresent(int userId) {
        checkWritePermission(userId);
        this.mStrongAuth.reportUnlock(userId);
    }

    public int getStrongAuthForUser(int userId) {
        checkPasswordReadPermission();
        return this.mStrongAuthTracker.getStrongAuthForUser(userId);
    }

    private boolean isCallerShell() {
        int callingUid = Binder.getCallingUid();
        return callingUid == 2000 || callingUid == 0;
    }

    private void enforceShell() {
        if (!isCallerShell()) {
            throw new SecurityException("Caller must be shell");
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r14v0, resolved type: com.android.server.locksettings.LockSettingsService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        enforceShell();
        int origPid = Binder.getCallingPid();
        int origUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        Slog.e(TAG, "Caller pid " + origPid + " Caller uid " + origUid);
        try {
            LockSettingsShellCommand command = new LockSettingsShellCommand(new LockPatternUtils(this.mContext), this.mContext, origPid, origUid);
            command.exec(this, in, out, err, args, callback, resultReceiver);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void initRecoveryServiceWithSigFile(String rootCertificateAlias, byte[] recoveryServiceCertFile, byte[] recoveryServiceSigFile) throws RemoteException {
        this.mRecoverableKeyStoreManager.initRecoveryServiceWithSigFile(rootCertificateAlias, recoveryServiceCertFile, recoveryServiceSigFile);
    }

    public KeyChainSnapshot getKeyChainSnapshot() throws RemoteException {
        return this.mRecoverableKeyStoreManager.getKeyChainSnapshot();
    }

    public void setSnapshotCreatedPendingIntent(PendingIntent intent) throws RemoteException {
        this.mRecoverableKeyStoreManager.setSnapshotCreatedPendingIntent(intent);
    }

    public void setServerParams(byte[] serverParams) throws RemoteException {
        this.mRecoverableKeyStoreManager.setServerParams(serverParams);
    }

    public void setRecoveryStatus(String alias, int status) throws RemoteException {
        this.mRecoverableKeyStoreManager.setRecoveryStatus(alias, status);
    }

    public Map getRecoveryStatus() throws RemoteException {
        return this.mRecoverableKeyStoreManager.getRecoveryStatus();
    }

    public void setRecoverySecretTypes(int[] secretTypes) throws RemoteException {
        this.mRecoverableKeyStoreManager.setRecoverySecretTypes(secretTypes);
    }

    public int[] getRecoverySecretTypes() throws RemoteException {
        return this.mRecoverableKeyStoreManager.getRecoverySecretTypes();
    }

    public byte[] startRecoverySessionWithCertPath(String sessionId, String rootCertificateAlias, RecoveryCertPath verifierCertPath, byte[] vaultParams, byte[] vaultChallenge, List<KeyChainProtectionParams> secrets) throws RemoteException {
        return this.mRecoverableKeyStoreManager.startRecoverySessionWithCertPath(sessionId, rootCertificateAlias, verifierCertPath, vaultParams, vaultChallenge, secrets);
    }

    public Map<String, String> recoverKeyChainSnapshot(String sessionId, byte[] recoveryKeyBlob, List<WrappedApplicationKey> applicationKeys) throws RemoteException {
        return this.mRecoverableKeyStoreManager.recoverKeyChainSnapshot(sessionId, recoveryKeyBlob, applicationKeys);
    }

    public void closeSession(String sessionId) throws RemoteException {
        this.mRecoverableKeyStoreManager.closeSession(sessionId);
    }

    public void removeKey(String alias) throws RemoteException {
        this.mRecoverableKeyStoreManager.removeKey(alias);
    }

    public String generateKey(String alias) throws RemoteException {
        return this.mRecoverableKeyStoreManager.generateKey(alias);
    }

    public String generateKeyWithMetadata(String alias, byte[] metadata) throws RemoteException {
        return this.mRecoverableKeyStoreManager.generateKeyWithMetadata(alias, metadata);
    }

    public String importKey(String alias, byte[] keyBytes) throws RemoteException {
        return this.mRecoverableKeyStoreManager.importKey(alias, keyBytes);
    }

    public String importKeyWithMetadata(String alias, byte[] keyBytes, byte[] metadata) throws RemoteException {
        return this.mRecoverableKeyStoreManager.importKeyWithMetadata(alias, keyBytes, metadata);
    }

    public String getKey(String alias) throws RemoteException {
        return this.mRecoverableKeyStoreManager.getKey(alias);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GateKeeperDiedRecipient implements IBinder.DeathRecipient {
        private GateKeeperDiedRecipient() {
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            LockSettingsService.this.mGateKeeperService.asBinder().unlinkToDeath(this, 0);
            LockSettingsService.this.mGateKeeperService = null;
        }
    }

    protected synchronized IGateKeeperService getGateKeeperService() {
        IGateKeeperService iGateKeeperService = this.mGateKeeperService;
        if (iGateKeeperService != null) {
            return iGateKeeperService;
        }
        IBinder service = ServiceManager.getService("android.service.gatekeeper.IGateKeeperService");
        if (service != null) {
            try {
                service.linkToDeath(new GateKeeperDiedRecipient(), 0);
            } catch (RemoteException e) {
                Slog.w(TAG, " Unable to register death recipient", e);
            }
            IGateKeeperService asInterface = IGateKeeperService.Stub.asInterface(service);
            this.mGateKeeperService = asInterface;
            return asInterface;
        }
        Slog.e(TAG, "Unable to acquire GateKeeperService");
        return null;
    }

    private void gateKeeperClearSecureUserId(int userId) {
        try {
            getGateKeeperService().clearSecureUserId(userId);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to clear SID", e);
        }
    }

    private void onAuthTokenKnownForUser(int userId, SyntheticPasswordManager.AuthenticationToken auth) {
        if (this.mInjector.isGsiRunning()) {
            Slog.w(TAG, "Running in GSI; skipping calls to AuthSecret and RebootEscrow");
            return;
        }
        this.mRebootEscrowManager.callToRebootEscrowIfNeeded(userId, auth.getVersion(), auth.getSyntheticPassword());
        callToAuthSecretIfNeeded(userId, auth);
    }

    private void callToAuthSecretIfNeeded(int userId, SyntheticPasswordManager.AuthenticationToken auth) {
        if (this.mAuthSecretService != null && this.mUserManager.getUserInfo(userId).isPrimary()) {
            try {
                byte[] rawSecret = auth.deriveVendorAuthSecret();
                ArrayList<Byte> secret = new ArrayList<>(rawSecret.length);
                for (byte b : rawSecret) {
                    secret.add(Byte.valueOf(b));
                }
                this.mAuthSecretService.primaryUserCredential(secret);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to pass primary user secret to AuthSecret HAL", e);
            }
        }
    }

    protected SyntheticPasswordManager.AuthenticationToken initializeSyntheticPasswordLocked(byte[] credentialHash, LockscreenCredential credential, int userId) {
        Slog.i(TAG, "Initialize SyntheticPassword for user: " + userId);
        Preconditions.checkState(getSyntheticPasswordHandleLocked(userId) == 0, "Cannot reinitialize SP");
        SyntheticPasswordManager.AuthenticationToken auth = this.mSpManager.newSyntheticPasswordAndSid(getGateKeeperService(), credentialHash, credential, userId);
        if (auth == null) {
            Slog.wtf(TAG, "initializeSyntheticPasswordLocked returns null auth token");
            return null;
        }
        long handle = this.mSpManager.createPasswordBasedSyntheticPassword(getGateKeeperService(), credential, auth, userId);
        if (!credential.isNone()) {
            if (credentialHash == null) {
                this.mSpManager.newSidForUser(getGateKeeperService(), auth, userId);
            }
            this.mSpManager.verifyChallenge(getGateKeeperService(), auth, 0L, userId);
            setUserKeyProtection(userId, auth.deriveDiskEncryptionKey());
            this.mDiskCacheKeyMap.put(Integer.valueOf(userId), auth.deriveDiskEncryptionKey());
            setKeystorePassword(auth.deriveKeyStorePassword(), userId);
        } else {
            clearUserKeyProtection(userId, null);
            setKeystorePassword(null, userId);
            gateKeeperClearSecureUserId(userId);
        }
        fixateNewestUserKeyAuth(userId);
        setSyntheticPasswordHandleLocked(handle, userId);
        onAuthTokenKnownForUser(userId, auth);
        return auth;
    }

    long getSyntheticPasswordHandleLocked(int userId) {
        return getLong("sp-handle", 0L, userId);
    }

    private void setSyntheticPasswordHandleLocked(long handle, int userId) {
        long oldHandle = getSyntheticPasswordHandleLocked(userId);
        setLong("sp-handle", handle, userId);
        setLong(PREV_SYNTHETIC_PASSWORD_HANDLE_KEY, oldHandle, userId);
        setLong(SYNTHETIC_PASSWORD_UPDATE_TIME_KEY, System.currentTimeMillis(), userId);
    }

    boolean isSyntheticPasswordBasedCredential(int userId) {
        boolean isSyntheticPasswordBasedCredentialLocked;
        synchronized (this.mSpManager) {
            isSyntheticPasswordBasedCredentialLocked = isSyntheticPasswordBasedCredentialLocked(userId);
        }
        return isSyntheticPasswordBasedCredentialLocked;
    }

    private boolean isSyntheticPasswordBasedCredentialLocked(int userId) {
        if (userId == -9999) {
            int type = this.mStorage.readPersistentDataBlock().type;
            return type == 1 || type == 2;
        }
        long handle = getSyntheticPasswordHandleLocked(userId);
        return handle != 0;
    }

    protected boolean shouldMigrateToSyntheticPasswordLocked(int userId) {
        return getSyntheticPasswordHandleLocked(userId) == 0;
    }

    private VerifyCredentialResponse spBasedDoVerifyCredential(LockscreenCredential userCredential, int userId, ICheckCredentialProgressCallback progressCallback, int flags) {
        Object obj;
        boolean hasEnrolledBiometrics = this.mInjector.hasEnrolledBiometrics(userId);
        Object obj2 = " hasEnrolledBiometrics=";
        Slog.d(TAG, "spBasedDoVerifyCredential: user=" + userId + " hasEnrolledBiometrics=" + hasEnrolledBiometrics);
        boolean requestGkPw = (flags & 1) != 0;
        Object obj3 = this.mSpManager;
        synchronized (obj3) {
            try {
                try {
                    if (!isSyntheticPasswordBasedCredentialLocked(userId)) {
                        return null;
                    }
                    if (userId != -9999) {
                        long handle = getSyntheticPasswordHandleLocked(userId);
                        SyntheticPasswordManager.AuthenticationResult authResult = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), handle, userCredential, userId, progressCallback);
                        if (authResult != null && authResult.authToken != null) {
                            this.mDiskCacheKeyMap.put(Integer.valueOf(userId), authResult.authToken.deriveDiskEncryptionKey());
                        }
                        VerifyCredentialResponse response = authResult.gkResponse;
                        if (response.getResponseCode() != 0) {
                            obj = obj3;
                        } else {
                            this.mBiometricDeferredQueue.addPendingLockoutResetForUser(userId, authResult.authToken.deriveGkPassword());
                            obj = obj3;
                            response = this.mSpManager.verifyChallenge(getGateKeeperService(), authResult.authToken, 0L, userId);
                            if (response.getResponseCode() != 0) {
                                Slog.wtf(TAG, "verifyChallenge with SP failed.");
                                VerifyCredentialResponse verifyCredentialResponse = VerifyCredentialResponse.ERROR;
                                return verifyCredentialResponse;
                            }
                        }
                        if (response.getResponseCode() == 0) {
                            onCredentialVerified(authResult.authToken, PasswordMetrics.computeForCredential(userCredential), userId);
                        } else if (response.getResponseCode() == 1 && response.getTimeout() > 0) {
                            requireStrongAuth(8, userId);
                        }
                        if (response.isMatched() && requestGkPw) {
                            long handle2 = storeGatekeeperPasswordTemporarily(authResult.authToken.deriveGkPassword());
                            return new VerifyCredentialResponse.Builder().setGatekeeperPasswordHandle(handle2).build();
                        }
                        return response;
                    }
                    try {
                        return this.mSpManager.verifyFrpCredential(getGateKeeperService(), userCredential, progressCallback);
                    } catch (Throwable th) {
                        th = th;
                        obj2 = obj3;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private long storeGatekeeperPasswordTemporarily(byte[] gatekeeperPassword) {
        long handle = 0;
        synchronized (this.mGatekeeperPasswords) {
            while (true) {
                if (handle != 0) {
                    if (this.mGatekeeperPasswords.get(handle) == null) {
                        this.mGatekeeperPasswords.put(handle, gatekeeperPassword);
                    }
                }
                handle = this.mRandom.nextLong();
            }
        }
        final long finalHandle = handle;
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.locksettings.LockSettingsService$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                LockSettingsService.this.m4571x695b5128(finalHandle);
            }
        }, 600000L);
        return handle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$storeGatekeeperPasswordTemporarily$6$com-android-server-locksettings-LockSettingsService  reason: not valid java name */
    public /* synthetic */ void m4571x695b5128(long finalHandle) {
        synchronized (this.mGatekeeperPasswords) {
            Slog.d(TAG, "Removing handle: " + finalHandle);
            this.mGatekeeperPasswords.remove(finalHandle);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCredentialVerified(SyntheticPasswordManager.AuthenticationToken authToken, PasswordMetrics metrics, int userId) {
        if (metrics != null) {
            synchronized (this) {
                this.mUserPasswordMetrics.put(userId, metrics);
            }
        } else {
            Slog.wtf(TAG, "Null metrics after credential verification");
        }
        unlockKeystore(authToken.deriveKeyStorePassword(), userId);
        byte[] secret = authToken.deriveDiskEncryptionKey();
        unlockUser(userId, secret);
        Arrays.fill(secret, (byte) 0);
        activateEscrowTokens(authToken, userId);
        if (isProfileWithSeparatedLock(userId)) {
            setDeviceUnlockedForUser(userId);
        }
        this.mStrongAuth.reportSuccessfulStrongAuthUnlock(userId);
        onAuthTokenKnownForUser(userId, authToken);
    }

    private void setDeviceUnlockedForUser(int userId) {
        TrustManager trustManager = (TrustManager) this.mContext.getSystemService(TrustManager.class);
        trustManager.setDeviceLockedForUser(userId, false);
    }

    private long setLockCredentialWithAuthTokenLocked(LockscreenCredential credential, SyntheticPasswordManager.AuthenticationToken auth, int userId) {
        Map<Integer, LockscreenCredential> profilePasswords;
        int savedCredentialType = getCredentialTypeInternal(userId);
        long newHandle = this.mSpManager.createPasswordBasedSyntheticPassword(getGateKeeperService(), credential, auth, userId);
        if (!credential.isNone()) {
            profilePasswords = null;
            if (this.mSpManager.hasSidForUser(userId)) {
                this.mSpManager.verifyChallenge(getGateKeeperService(), auth, 0L, userId);
            } else {
                this.mSpManager.newSidForUser(getGateKeeperService(), auth, userId);
                this.mSpManager.verifyChallenge(getGateKeeperService(), auth, 0L, userId);
                setUserKeyProtection(userId, auth.deriveDiskEncryptionKey());
                fixateNewestUserKeyAuth(userId);
                setKeystorePassword(auth.deriveKeyStorePassword(), userId);
            }
        } else {
            profilePasswords = getDecryptedPasswordsForAllTiedProfiles(userId);
            this.mSpManager.clearSidForUser(userId);
            gateKeeperClearSecureUserId(userId);
            unlockUserKey(userId, auth.deriveDiskEncryptionKey());
            clearUserKeyProtection(userId, auth.deriveDiskEncryptionKey());
            fixateNewestUserKeyAuth(userId);
            unlockKeystore(auth.deriveKeyStorePassword(), userId);
            setKeystorePassword(null, userId);
            removeBiometricsForUser(userId);
        }
        setSyntheticPasswordHandleLocked(newHandle, userId);
        synchronizeUnifiedWorkChallengeForProfiles(userId, profilePasswords);
        setUserPasswordMetrics(credential, userId);
        this.mManagedProfilePasswordCache.removePassword(userId);
        if (savedCredentialType != -1) {
            this.mSpManager.destroyAllWeakTokenBasedSyntheticPasswords(userId);
        }
        if (profilePasswords != null) {
            for (Map.Entry<Integer, LockscreenCredential> entry : profilePasswords.entrySet()) {
                entry.getValue().zeroize();
            }
        }
        return newHandle;
    }

    private void removeBiometricsForUser(int userId) {
        removeAllFingerprintForUser(userId);
        removeAllFaceForUser(userId);
    }

    private void removeAllFingerprintForUser(int userId) {
        FingerprintManager mFingerprintManager = this.mInjector.getFingerprintManager();
        if (mFingerprintManager != null && mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints(userId)) {
            CountDownLatch latch = new CountDownLatch(1);
            if (OPTICAL_FINGERPRINT && MTK_AOD_SUPPORT) {
                Settings.Secure.putInt(this.mContext.getContentResolver(), AOD_FP_DISPLAY, 0);
                if (Settings.Secure.getInt(this.mContext.getContentResolver(), AOD_IS_SHOW, 0) == 0 && Settings.System.getInt(this.mContext.getContentResolver(), "edge_notify_flash_switch", 0) == 0) {
                    Settings.Secure.putInt(this.mContext.getContentResolver(), "doze_enabled", 0);
                }
            }
            mFingerprintManager.removeAll(userId, fingerprintManagerRemovalCallback(latch), this.mHandler);
            try {
                latch.await(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Slog.e(TAG, "Latch interrupted when removing fingerprint", e);
            }
        }
    }

    private void removeAllFaceForUser(int userId) {
        FaceManager mFaceManager = this.mInjector.getFaceManager();
        if (mFaceManager != null && mFaceManager.isHardwareDetected() && mFaceManager.hasEnrolledTemplates(userId)) {
            CountDownLatch latch = new CountDownLatch(1);
            mFaceManager.removeAll(userId, faceManagerRemovalCallback(latch));
            try {
                latch.await(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Slog.e(TAG, "Latch interrupted when removing face", e);
            }
        }
    }

    private FingerprintManager.RemovalCallback fingerprintManagerRemovalCallback(final CountDownLatch latch) {
        return new FingerprintManager.RemovalCallback() { // from class: com.android.server.locksettings.LockSettingsService.4
            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence err) {
                Slog.e(LockSettingsService.TAG, "Unable to remove fingerprint, error: " + ((Object) err));
                latch.countDown();
            }

            public void onRemovalSucceeded(Fingerprint fp, int remaining) {
                Slog.d(LockSettingsService.TAG, "onRemovalSucceeded remaining=" + remaining);
                if (remaining == 0) {
                    latch.countDown();
                }
            }
        };
    }

    private FaceManager.RemovalCallback faceManagerRemovalCallback(final CountDownLatch latch) {
        return new FaceManager.RemovalCallback() { // from class: com.android.server.locksettings.LockSettingsService.5
            public void onRemovalError(Face face, int errMsgId, CharSequence err) {
                Slog.e(LockSettingsService.TAG, "Unable to remove face, error: " + ((Object) err));
                latch.countDown();
            }

            public void onRemovalSucceeded(Face face, int remaining) {
                if (remaining == 0) {
                    latch.countDown();
                }
            }
        };
    }

    /* JADX WARN: Removed duplicated region for block: B:15:0x0042  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0068  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean spBasedSetLockCredentialInternalLocked(LockscreenCredential credential, LockscreenCredential savedCredential, int userId, boolean isLockTiedToParent) {
        LockscreenCredential savedCredential2;
        SyntheticPasswordManager.AuthenticationToken auth;
        if (savedCredential.isNone() && isProfileWithUnifiedLock(userId)) {
            try {
                savedCredential2 = getDecryptedPasswordForTiedProfile(userId);
            } catch (FileNotFoundException e) {
                Slog.i(TAG, "Child profile key not found");
            } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
                Slog.e(TAG, "Failed to decrypt child profile key", e2);
            }
            long handle = getSyntheticPasswordHandleLocked(userId);
            SyntheticPasswordManager.AuthenticationResult authResult = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), handle, savedCredential2, userId, null);
            VerifyCredentialResponse response = authResult.gkResponse;
            auth = authResult.authToken;
            if (auth != null) {
                if (response != null && response.getResponseCode() != -1) {
                    if (response.getResponseCode() == 1) {
                        Slog.w(TAG, "Failed to enroll: rate limit exceeded.");
                        return false;
                    }
                    throw new IllegalStateException("password change failed");
                }
                Slog.w(TAG, "Failed to enroll: incorrect credential.");
                return false;
            }
            this.mDiskCacheKeyMap.put(Integer.valueOf(userId), auth.deriveDiskEncryptionKey());
            onAuthTokenKnownForUser(userId, auth);
            setLockCredentialWithAuthTokenLocked(credential, auth, userId);
            this.mSpManager.destroyPasswordBasedSyntheticPassword(handle, userId);
            sendCredentialsOnChangeIfRequired(credential, userId, isLockTiedToParent);
            return true;
        }
        savedCredential2 = savedCredential;
        long handle2 = getSyntheticPasswordHandleLocked(userId);
        SyntheticPasswordManager.AuthenticationResult authResult2 = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), handle2, savedCredential2, userId, null);
        VerifyCredentialResponse response2 = authResult2.gkResponse;
        auth = authResult2.authToken;
        if (auth != null) {
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3282=5] */
    public byte[] getHashFactor(LockscreenCredential currentCredential, int userId) {
        checkPasswordReadPermission();
        try {
            if (isProfileWithUnifiedLock(userId)) {
                try {
                    currentCredential = getDecryptedPasswordForTiedProfile(userId);
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to get work profile credential", e);
                    return null;
                }
            }
            synchronized (this.mSpManager) {
                if (!isSyntheticPasswordBasedCredentialLocked(userId)) {
                    Slog.w(TAG, "Synthetic password not enabled");
                    return null;
                }
                long handle = getSyntheticPasswordHandleLocked(userId);
                SyntheticPasswordManager.AuthenticationResult auth = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), handle, currentCredential, userId, null);
                if (auth.authToken == null) {
                    Slog.w(TAG, "Current credential is incorrect");
                    return null;
                }
                return auth.authToken.derivePasswordHashFactor();
            }
        } finally {
            scheduleGc();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long addEscrowToken(byte[] token, int type, int userId, LockPatternUtils.EscrowTokenStateChangeCallback callback) {
        long handle;
        synchronized (this.mSpManager) {
            SyntheticPasswordManager.AuthenticationToken auth = null;
            if (!isUserSecure(userId)) {
                if (shouldMigrateToSyntheticPasswordLocked(userId)) {
                    auth = initializeSyntheticPasswordLocked(null, LockscreenCredential.createNone(), userId);
                } else {
                    long pwdHandle = getSyntheticPasswordHandleLocked(userId);
                    auth = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), pwdHandle, LockscreenCredential.createNone(), userId, null).authToken;
                }
            }
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                disableEscrowTokenOnNonManagedDevicesIfNeeded(userId);
                if (!this.mSpManager.hasEscrowData(userId)) {
                    throw new SecurityException("Escrow token is disabled on the current user");
                }
            }
            handle = this.mSpManager.createTokenBasedSyntheticPassword(token, type, userId, callback);
            if (auth != null) {
                this.mSpManager.activateTokenBasedSyntheticPassword(handle, auth, userId);
            }
        }
        return handle;
    }

    private void activateEscrowTokens(SyntheticPasswordManager.AuthenticationToken auth, int userId) {
        synchronized (this.mSpManager) {
            disableEscrowTokenOnNonManagedDevicesIfNeeded(userId);
            for (Long l : this.mSpManager.getPendingTokensForUser(userId)) {
                long handle = l.longValue();
                Slog.i(TAG, String.format("activateEscrowTokens: %x %d ", Long.valueOf(handle), Integer.valueOf(userId)));
                this.mSpManager.activateTokenBasedSyntheticPassword(handle, auth, userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isEscrowTokenActive(long handle, int userId) {
        boolean existsHandle;
        synchronized (this.mSpManager) {
            existsHandle = this.mSpManager.existsHandle(handle, userId);
        }
        return existsHandle;
    }

    public boolean hasPendingEscrowToken(int userId) {
        boolean z;
        checkPasswordReadPermission();
        synchronized (this.mSpManager) {
            z = !this.mSpManager.getPendingTokensForUser(userId).isEmpty();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean removeEscrowToken(long handle, int userId) {
        synchronized (this.mSpManager) {
            if (handle == getSyntheticPasswordHandleLocked(userId)) {
                Slog.w(TAG, "Cannot remove password handle");
                return false;
            } else if (this.mSpManager.removePendingToken(handle, userId)) {
                return true;
            } else {
                if (this.mSpManager.existsHandle(handle, userId)) {
                    this.mSpManager.destroyTokenBasedSyntheticPassword(handle, userId);
                    return true;
                }
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setLockCredentialWithToken(LockscreenCredential credential, long tokenHandle, byte[] token, final int userId) {
        synchronized (this.mSpManager) {
            if (!this.mSpManager.hasEscrowData(userId)) {
                throw new SecurityException("Escrow token is disabled on the current user");
            }
            if (!isEscrowTokenActive(tokenHandle, userId)) {
                Slog.e(TAG, "Unknown or unactivated token: " + Long.toHexString(tokenHandle));
                return false;
            }
            boolean result = setLockCredentialWithTokenInternalLocked(credential, tokenHandle, token, userId);
            if (result) {
                synchronized (this.mSeparateChallengeLock) {
                    setSeparateProfileChallengeEnabledLocked(userId, true, null);
                }
                if (credential.isNone()) {
                    this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.LockSettingsService$$ExternalSyntheticLambda3
                        @Override // java.lang.Runnable
                        public final void run() {
                            LockSettingsService.this.m4570x245032a3(userId);
                        }
                    });
                }
                notifyPasswordChanged(credential, userId);
                notifySeparateProfileChallengeChanged(userId);
            }
            return result;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setLockCredentialWithToken$7$com-android-server-locksettings-LockSettingsService  reason: not valid java name */
    public /* synthetic */ void m4570x245032a3(int userId) {
        unlockUser(userId, null);
    }

    private boolean setLockCredentialWithTokenInternalLocked(LockscreenCredential credential, long tokenHandle, byte[] token, int userId) {
        SyntheticPasswordManager.AuthenticationResult result = this.mSpManager.unwrapTokenBasedSyntheticPassword(getGateKeeperService(), tokenHandle, token, userId);
        if (result.authToken == null) {
            Slog.w(TAG, "Invalid escrow token supplied");
            return false;
        } else if (result.gkResponse.getResponseCode() != 0) {
            Slog.e(TAG, "Obsolete token: synthetic password derived but it fails GK verification.");
            return false;
        } else {
            onAuthTokenKnownForUser(userId, result.authToken);
            long oldHandle = getSyntheticPasswordHandleLocked(userId);
            setLockCredentialWithAuthTokenLocked(credential, result.authToken, userId);
            this.mSpManager.destroyPasswordBasedSyntheticPassword(oldHandle, userId);
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean unlockUserWithToken(long tokenHandle, byte[] token, int userId) {
        synchronized (this.mSpManager) {
            if (!this.mSpManager.hasEscrowData(userId)) {
                Slog.w(TAG, "Escrow token is disabled on the current user");
                return false;
            }
            SyntheticPasswordManager.AuthenticationResult authResult = this.mSpManager.unwrapTokenBasedSyntheticPassword(getGateKeeperService(), tokenHandle, token, userId);
            if (authResult.authToken == null) {
                Slog.w(TAG, "Invalid escrow token supplied");
                return false;
            }
            onCredentialVerified(authResult.authToken, loadPasswordMetrics(authResult.authToken, userId), userId);
            return true;
        }
    }

    public boolean tryUnlockWithCachedUnifiedChallenge(int userId) {
        LockscreenCredential cred = this.mManagedProfilePasswordCache.retrievePassword(userId);
        if (cred != null) {
            try {
                boolean z = doVerifyCredential(cred, userId, null, 0).getResponseCode() == 0;
                if (cred != null) {
                    cred.close();
                }
                return z;
            } catch (Throwable th) {
                if (cred != null) {
                    try {
                        cred.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        if (cred != null) {
            cred.close();
        }
        return false;
    }

    public void removeCachedUnifiedChallenge(int userId) {
        this.mManagedProfilePasswordCache.removePassword(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String timestampToString(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    private static String credentialTypeToString(int credentialType) {
        switch (credentialType) {
            case -1:
                return "None";
            case 0:
            case 2:
            default:
                return "Unknown " + credentialType;
            case 1:
                return "Pattern";
            case 3:
                return "Pin";
            case 4:
                return "Password";
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
            pw.println("Current lock settings service state:");
            pw.println();
            pw.println("User State:");
            pw.increaseIndent();
            List<UserInfo> users = this.mUserManager.getUsers();
            for (int user = 0; user < users.size(); user++) {
                int userId = users.get(user).id;
                pw.println("User " + userId);
                pw.increaseIndent();
                synchronized (this.mSpManager) {
                    pw.println(String.format("SP Handle: %x", Long.valueOf(getSyntheticPasswordHandleLocked(userId))));
                    pw.println(String.format("Last changed: %s (%x)", timestampToString(getLong(SYNTHETIC_PASSWORD_UPDATE_TIME_KEY, 0L, userId)), Long.valueOf(getLong(PREV_SYNTHETIC_PASSWORD_HANDLE_KEY, 0L, userId))));
                }
                try {
                    pw.println(String.format("SID: %x", Long.valueOf(getGateKeeperService().getSecureUserId(userId))));
                } catch (RemoteException e) {
                }
                pw.println("Quality: " + getKeyguardStoredQuality(userId));
                pw.println("CredentialType: " + credentialTypeToString(getCredentialTypeInternal(userId)));
                pw.println("SeparateChallenge: " + getSeparateProfileChallengeEnabledInternal(userId));
                Object[] objArr = new Object[1];
                objArr[0] = getUserPasswordMetrics(userId) != null ? "known" : UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
                pw.println(String.format("Metrics: %s", objArr));
                pw.decreaseIndent();
            }
            pw.println();
            pw.decreaseIndent();
            pw.println("Keys in namespace:");
            pw.increaseIndent();
            dumpKeystoreKeys(pw);
            pw.println();
            pw.decreaseIndent();
            pw.println("Storage:");
            pw.increaseIndent();
            this.mStorage.dump(pw);
            pw.println();
            pw.decreaseIndent();
            pw.println("StrongAuth:");
            pw.increaseIndent();
            this.mStrongAuth.dump(pw);
            pw.println();
            pw.decreaseIndent();
            pw.println("RebootEscrow:");
            pw.increaseIndent();
            this.mRebootEscrowManager.dump(pw);
            pw.println();
            pw.decreaseIndent();
            pw.println("PasswordHandleCount: " + this.mGatekeeperPasswords.size());
        }
    }

    private void dumpKeystoreKeys(IndentingPrintWriter pw) {
        try {
            Enumeration<String> aliases = this.mJavaKeyStore.aliases();
            while (aliases.hasMoreElements()) {
                pw.println(aliases.nextElement());
            }
        } catch (KeyStoreException e) {
            pw.println("Unable to get keys: " + e.toString());
            Slog.d(TAG, "Dump error", e);
        }
    }

    private void disableEscrowTokenOnNonManagedDevicesIfNeeded(int userId) {
        UserManagerInternal userManagerInternal = this.mInjector.getUserManagerInternal();
        if (userManagerInternal.isUserManaged(userId)) {
            Slog.i(TAG, "Managed profile can have escrow token");
        } else if (userManagerInternal.isDeviceManaged()) {
            Slog.i(TAG, "Corp-owned device can have escrow token");
        } else if (!this.mInjector.getDeviceStateCache().isDeviceProvisioned()) {
            Slog.i(TAG, "Postpone disabling escrow tokens until device is provisioned");
        } else if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
        } else {
            Slog.i(TAG, "Disabling escrow token on user " + userId);
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                this.mSpManager.destroyEscrowData(userId);
            }
        }
    }

    private void scheduleGc() {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.locksettings.LockSettingsService$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                LockSettingsService.lambda$scheduleGc$8();
            }
        }, 2000L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$scheduleGc$8() {
        System.gc();
        System.runFinalization();
        System.gc();
    }

    /* loaded from: classes.dex */
    private class DeviceProvisionedObserver extends ContentObserver {
        private final Uri mDeviceProvisionedUri;
        private boolean mRegistered;

        public DeviceProvisionedObserver() {
            super(null);
            this.mDeviceProvisionedUri = Settings.Global.getUriFor("device_provisioned");
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mDeviceProvisionedUri.equals(uri)) {
                updateRegistration();
                if (isProvisioned()) {
                    Slog.i(LockSettingsService.TAG, "Reporting device setup complete to IGateKeeperService");
                    reportDeviceSetupComplete();
                    clearFrpCredentialIfOwnerNotSecure();
                }
            }
        }

        public void onSystemReady() {
            if (LockPatternUtils.frpCredentialEnabled(LockSettingsService.this.mContext)) {
                updateRegistration();
            } else if (!isProvisioned()) {
                Slog.i(LockSettingsService.TAG, "FRP credential disabled, reporting device setup complete to Gatekeeper immediately");
                reportDeviceSetupComplete();
            }
        }

        private void reportDeviceSetupComplete() {
            try {
                LockSettingsService.this.getGateKeeperService().reportDeviceSetupComplete();
            } catch (RemoteException e) {
                Slog.e(LockSettingsService.TAG, "Failure reporting to IGateKeeperService", e);
            }
        }

        private void clearFrpCredentialIfOwnerNotSecure() {
            List<UserInfo> users = LockSettingsService.this.mUserManager.getUsers();
            for (UserInfo user : users) {
                if (LockPatternUtils.userOwnsFrpCredential(LockSettingsService.this.mContext, user)) {
                    if (!LockSettingsService.this.isUserSecure(user.id)) {
                        LockSettingsService.this.mStorage.writePersistentDataBlock(0, user.id, 0, null);
                        return;
                    }
                    return;
                }
            }
        }

        private void updateRegistration() {
            boolean register = !isProvisioned();
            if (register == this.mRegistered) {
                return;
            }
            if (register) {
                LockSettingsService.this.mContext.getContentResolver().registerContentObserver(this.mDeviceProvisionedUri, false, this);
            } else {
                LockSettingsService.this.mContext.getContentResolver().unregisterContentObserver(this);
            }
            this.mRegistered = register;
        }

        private boolean isProvisioned() {
            return Settings.Global.getInt(LockSettingsService.this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends LockSettingsInternal {
        private LocalService() {
        }

        public long addEscrowToken(byte[] token, int userId, LockPatternUtils.EscrowTokenStateChangeCallback callback) {
            return LockSettingsService.this.addEscrowToken(token, 0, userId, callback);
        }

        public boolean removeEscrowToken(long handle, int userId) {
            return LockSettingsService.this.removeEscrowToken(handle, userId);
        }

        public boolean isEscrowTokenActive(long handle, int userId) {
            return LockSettingsService.this.isEscrowTokenActive(handle, userId);
        }

        public boolean setLockCredentialWithToken(LockscreenCredential credential, long tokenHandle, byte[] token, int userId) {
            if (!LockSettingsService.this.mHasSecureLockScreen && credential != null && credential.getType() != -1) {
                throw new UnsupportedOperationException("This operation requires secure lock screen feature.");
            }
            if (!LockSettingsService.this.setLockCredentialWithToken(credential, tokenHandle, token, userId)) {
                return false;
            }
            LockSettingsService.this.onPostPasswordChanged(credential, userId);
            return true;
        }

        public boolean unlockUserWithToken(long tokenHandle, byte[] token, int userId) {
            return LockSettingsService.this.unlockUserWithToken(tokenHandle, token, userId);
        }

        public PasswordMetrics getUserPasswordMetrics(int userHandle) {
            long identity = Binder.clearCallingIdentity();
            try {
                if (LockSettingsService.this.isProfileWithUnifiedLock(userHandle)) {
                    Slog.w(LockSettingsService.TAG, "Querying password metrics for unified challenge profile: " + userHandle);
                }
                Binder.restoreCallingIdentity(identity);
                return LockSettingsService.this.getUserPasswordMetrics(userHandle);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }

        public boolean prepareRebootEscrow() {
            if (!LockSettingsService.this.mRebootEscrowManager.prepareRebootEscrow()) {
                return false;
            }
            LockSettingsService.this.mStrongAuth.requireStrongAuth(64, -1);
            return true;
        }

        public void setRebootEscrowListener(RebootEscrowListener listener) {
            LockSettingsService.this.mRebootEscrowManager.setRebootEscrowListener(listener);
        }

        public boolean clearRebootEscrow() {
            if (!LockSettingsService.this.mRebootEscrowManager.clearRebootEscrow()) {
                return false;
            }
            LockSettingsService.this.mStrongAuth.noLongerRequireStrongAuth(64, -1);
            return true;
        }

        public int armRebootEscrow() {
            return LockSettingsService.this.mRebootEscrowManager.armRebootEscrowIfNeeded();
        }

        public void refreshStrongAuthTimeout(int userId) {
            LockSettingsService.this.mStrongAuth.refreshStrongAuthTimeout(userId);
        }
    }

    /* loaded from: classes.dex */
    private class RebootEscrowCallbacks implements RebootEscrowManager.Callbacks {
        private RebootEscrowCallbacks() {
        }

        @Override // com.android.server.locksettings.RebootEscrowManager.Callbacks
        public boolean isUserSecure(int userId) {
            return LockSettingsService.this.isUserSecure(userId);
        }

        @Override // com.android.server.locksettings.RebootEscrowManager.Callbacks
        public void onRebootEscrowRestored(byte spVersion, byte[] syntheticPassword, int userId) {
            SyntheticPasswordManager.AuthenticationToken authToken = new SyntheticPasswordManager.AuthenticationToken(spVersion);
            authToken.recreateDirectly(syntheticPassword);
            synchronized (LockSettingsService.this.mSpManager) {
                LockSettingsService.this.mSpManager.verifyChallenge(LockSettingsService.this.getGateKeeperService(), authToken, 0L, userId);
            }
            LockSettingsService lockSettingsService = LockSettingsService.this;
            lockSettingsService.onCredentialVerified(authToken, lockSettingsService.loadPasswordMetrics(authToken, userId), userId);
        }
    }

    public void setLockNoneCredential(LockscreenCredential credential, int userId, int flag) {
        LockscreenCredential savedCredential = LockscreenCredential.createNone();
        if ((this.FLAG_CLEAR_LOCK_WITHOUT_PASSWORD_CONFIRM & flag) != 0) {
            if (!this.mHasSecureLockScreen && credential != null && credential.getType() != -1) {
                throw new UnsupportedOperationException("This operation requires secure lock screen feature");
            }
            if (!hasPermission(PERMISSION) && !hasPermission("android.permission.SET_AND_VERIFY_LOCKSCREEN_CREDENTIALS")) {
                throw new SecurityException("setLockCredential requires SET_AND_VERIFY_LOCKSCREEN_CREDENTIALS or android.permission.ACCESS_KEYGUARD_SECURE_STORAGE");
            }
            long identity = Binder.clearCallingIdentity();
            try {
                enforceFrpResolved();
                if (!savedCredential.isNone() && isProfileWithUnifiedLock(userId)) {
                    verifyCredential(savedCredential, this.mUserManager.getProfileParent(userId).id, 0);
                    savedCredential.zeroize();
                    savedCredential = LockscreenCredential.createNone();
                }
                synchronized (this.mSeparateChallengeLock) {
                    setLockNoneCredentialInternal(credential, savedCredential, userId, false);
                    setSeparateProfileChallengeEnabledLocked(userId, true, null);
                    notifyPasswordChanged(credential, userId);
                }
                if (this.mUserManager.getUserInfo(userId).isManagedProfile()) {
                    setDeviceUnlockedForUser(userId);
                }
                notifySeparateProfileChallengeChanged(userId);
                onPostPasswordChanged(credential, userId);
                scheduleGc();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        if (savedCredential != null) {
            savedCredential.close();
        }
    }

    private boolean setLockNoneCredentialInternal(LockscreenCredential credential, LockscreenCredential savedCredential, int userId, boolean isLockTiedToParent) {
        LockscreenCredential savedCredential2;
        boolean spBasedSetLockNoneCredentialInternalLocked;
        Objects.requireNonNull(credential);
        Objects.requireNonNull(savedCredential);
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(userId)) {
                return spBasedSetLockNoneCredentialInternalLocked(credential, savedCredential, userId, isLockTiedToParent);
            } else if (credential.isNone()) {
                clearUserKeyProtection(userId, null);
                gateKeeperClearSecureUserId(userId);
                this.mStorage.writeCredentialHash(LockSettingsStorage.CredentialHash.createEmptyHash(), userId);
                setKeyguardStoredQuality(0, userId);
                setKeystorePassword(null, userId);
                fixateNewestUserKeyAuth(userId);
                synchronizeUnifiedWorkChallengeForProfiles(userId, null);
                setUserPasswordMetrics(LockscreenCredential.createNone(), userId);
                sendCredentialsOnChangeIfRequired(credential, userId, isLockTiedToParent);
                return true;
            } else {
                LockSettingsStorage.CredentialHash currentHandle = this.mStorage.readCredentialHash(userId);
                if (isProfileWithUnifiedLock(userId)) {
                    if (savedCredential.isNone()) {
                        try {
                            savedCredential2 = getDecryptedPasswordForTiedProfile(userId);
                        } catch (FileNotFoundException e) {
                            Slog.i(TAG, "Child profile key not found");
                        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
                            Slog.e(TAG, "Failed to decrypt child profile key", e2);
                        }
                    }
                    savedCredential2 = savedCredential;
                } else {
                    if (currentHandle.hash == null) {
                        if (!savedCredential.isNone()) {
                            Slog.w(TAG, "Saved credential provided, but none stored");
                        }
                        savedCredential.close();
                        savedCredential2 = LockscreenCredential.createNone();
                    }
                    savedCredential2 = savedCredential;
                }
                synchronized (this.mSpManager) {
                    initializeSyntheticPasswordLocked(currentHandle.hash, savedCredential2, userId);
                    spBasedSetLockNoneCredentialInternalLocked = spBasedSetLockNoneCredentialInternalLocked(credential, savedCredential2, userId, isLockTiedToParent);
                }
                return spBasedSetLockNoneCredentialInternalLocked;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:15:0x0044  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x0087  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean spBasedSetLockNoneCredentialInternalLocked(LockscreenCredential credential, LockscreenCredential savedCredential, int userId, boolean isLockTiedToParent) {
        LockscreenCredential savedCredential2;
        SyntheticPasswordManager.AuthenticationToken auth;
        long handle;
        if (savedCredential.isNone() && isProfileWithUnifiedLock(userId)) {
            try {
                savedCredential2 = getDecryptedPasswordForTiedProfile(userId);
            } catch (FileNotFoundException e) {
                Slog.i(TAG, "Child profile key not found");
            } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
                Slog.e(TAG, "Failed to decrypt child profile key", e2);
            }
            long handle2 = getSyntheticPasswordHandleLocked(userId);
            SyntheticPasswordManager.AuthenticationResult authResult = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), handle2, savedCredential2, userId, null);
            VerifyCredentialResponse response = authResult.gkResponse;
            auth = authResult.authToken;
            if (auth == null) {
                onAuthTokenKnownForUser(userId, auth);
                setLockCredentialWithAuthTokenLocked(credential, auth, userId);
                this.mSpManager.destroyPasswordBasedSyntheticPassword(handle2, userId);
                sendCredentialsOnChangeIfRequired(credential, userId, isLockTiedToParent);
                return true;
            }
            if (response == null) {
                handle = handle2;
            } else if (response.getResponseCode() != -1) {
                if (response.getResponseCode() == 1) {
                    Slog.w(TAG, "Failed to enroll,ready to setLockNoneCredentialNewPassword.");
                    setLockNoneCredentialNewPassword(credential, userId, handle2, isLockTiedToParent);
                    return false;
                }
                throw new IllegalStateException("password change failed");
            } else {
                handle = handle2;
            }
            Slog.w(TAG, "Failed to enroll, ready to setLockNoneCredentialNewPassword. ");
            setLockNoneCredentialNewPassword(credential, userId, handle, isLockTiedToParent);
            return false;
        }
        savedCredential2 = savedCredential;
        long handle22 = getSyntheticPasswordHandleLocked(userId);
        SyntheticPasswordManager.AuthenticationResult authResult2 = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), handle22, savedCredential2, userId, null);
        VerifyCredentialResponse response2 = authResult2.gkResponse;
        auth = authResult2.authToken;
        if (auth == null) {
        }
    }

    private void setLockNoneCredentialNewPassword(LockscreenCredential credential, int userId, long handle, boolean isLockTiedToParent) {
        initializeSyntheticNonePasswordLocked(null, credential, userId);
        synchronizeUnifiedWorkChallengeForProfiles(userId, null);
        this.mSpManager.destroyPasswordBasedSyntheticPassword(handle, userId);
        setUserPasswordMetrics(credential, userId);
        sendCredentialsOnChangeIfRequired(credential, userId, isLockTiedToParent);
    }

    protected SyntheticPasswordManager.AuthenticationToken initializeSyntheticNonePasswordLocked(byte[] credentialHash, LockscreenCredential credential, int userId) {
        Slog.i(TAG, "Initialize SyntheticPassword for user: " + userId);
        SyntheticPasswordManager.AuthenticationToken auth = this.mSpManager.newSyntheticPasswordAndSid(getGateKeeperService(), credentialHash, credential, userId);
        onAuthTokenKnownForUser(userId, auth);
        if (auth == null) {
            Slog.wtf(TAG, "initializeSyntheticPasswordLocked returns null auth token");
            return null;
        }
        long handle = this.mSpManager.createPasswordBasedSyntheticPassword(getGateKeeperService(), credential, auth, userId);
        if (credential.isNone()) {
            this.mSpManager.clearSidForUser(userId);
            gateKeeperClearSecureUserId(userId);
            unlockUserKey(userId, this.mDiskCacheKeyMap.get(Integer.valueOf(userId)));
            clearUserKeyProtection(userId, this.mDiskCacheKeyMap.get(Integer.valueOf(userId)));
            unlockKeystore(auth.deriveKeyStorePassword(), userId);
            setKeystorePassword(null, userId);
            removeBiometricsForUser(userId);
            gateKeeperClearSecureUserId(userId);
        }
        fixateNewestUserKeyAuth(userId);
        setSyntheticPasswordHandleLocked(handle, userId);
        return auth;
    }
}
