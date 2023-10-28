package com.android.server.locksettings;

import android.content.Context;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.widget.RebootEscrowListener;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.SecretKey;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class RebootEscrowManager {
    private static final int BOOT_COUNT_TOLERANCE = 5;
    private static final int DEFAULT_LOAD_ESCROW_DATA_RETRY_COUNT = 3;
    private static final int DEFAULT_LOAD_ESCROW_DATA_RETRY_INTERVAL_SECONDS = 30;
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT_MILLIS = 180000;
    static final int ERROR_KEYSTORE_FAILURE = 7;
    static final int ERROR_LOAD_ESCROW_KEY = 3;
    static final int ERROR_NONE = 0;
    static final int ERROR_NO_NETWORK = 8;
    static final int ERROR_NO_PROVIDER = 2;
    static final int ERROR_PROVIDER_MISMATCH = 6;
    static final int ERROR_RETRY_COUNT_EXHAUSTED = 4;
    static final int ERROR_UNKNOWN = 1;
    static final int ERROR_UNLOCK_ALL_USERS = 5;
    static final String OTHER_VBMETA_DIGEST_PROP_NAME = "ota.other.vbmeta_digest";
    public static final String REBOOT_ESCROW_ARMED_KEY = "reboot_escrow_armed_count";
    static final String REBOOT_ESCROW_KEY_ARMED_TIMESTAMP = "reboot_escrow_key_stored_timestamp";
    static final String REBOOT_ESCROW_KEY_OTHER_VBMETA_DIGEST = "reboot_escrow_key_other_vbmeta_digest";
    static final String REBOOT_ESCROW_KEY_PROVIDER = "reboot_escrow_key_provider";
    static final String REBOOT_ESCROW_KEY_VBMETA_DIGEST = "reboot_escrow_key_vbmeta_digest";
    private static final String TAG = "RebootEscrowManager";
    static final String VBMETA_DIGEST_PROP_NAME = "ro.boot.vbmeta.digest";
    private final Callbacks mCallbacks;
    private final RebootEscrowEventLog mEventLog;
    private final Injector mInjector;
    private final Object mKeyGenerationLock;
    private final RebootEscrowKeyStoreManager mKeyStoreManager;
    private int mLoadEscrowDataErrorCode;
    private RebootEscrowKey mPendingRebootEscrowKey;
    private RebootEscrowListener mRebootEscrowListener;
    private boolean mRebootEscrowReady;
    private boolean mRebootEscrowWanted;
    private final LockSettingsStorage mStorage;
    private final UserManager mUserManager;
    PowerManager.WakeLock mWakeLock;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Callbacks {
        boolean isUserSecure(int i);

        void onRebootEscrowRestored(byte b, byte[] bArr, int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface RebootEscrowErrorCode {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        protected Context mContext;
        private final RebootEscrowKeyStoreManager mKeyStoreManager = new RebootEscrowKeyStoreManager();
        private RebootEscrowProviderInterface mRebootEscrowProvider;
        private final LockSettingsStorage mStorage;

        Injector(Context context, LockSettingsStorage storage) {
            this.mContext = context;
            this.mStorage = storage;
        }

        private RebootEscrowProviderInterface createRebootEscrowProvider() {
            RebootEscrowProviderInterface rebootEscrowProvider;
            if (serverBasedResumeOnReboot()) {
                Slog.i(RebootEscrowManager.TAG, "Using server based resume on reboot");
                rebootEscrowProvider = new RebootEscrowProviderServerBasedImpl(this.mContext, this.mStorage);
            } else {
                Slog.i(RebootEscrowManager.TAG, "Using HAL based resume on reboot");
                rebootEscrowProvider = new RebootEscrowProviderHalImpl();
            }
            if (rebootEscrowProvider.hasRebootEscrowSupport()) {
                return rebootEscrowProvider;
            }
            return null;
        }

        void post(Handler handler, Runnable runnable) {
            handler.post(runnable);
        }

        void postDelayed(Handler handler, Runnable runnable, long delayMillis) {
            handler.postDelayed(runnable, delayMillis);
        }

        public boolean serverBasedResumeOnReboot() {
            if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.reboot_escrow")) {
                return true;
            }
            return DeviceConfig.getBoolean("ota", "server_based_ror_enabled", false);
        }

        public boolean isNetworkConnected() {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
            if (connectivityManager == null) {
                return false;
            }
            Network activeNetwork = connectivityManager.getActiveNetwork();
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return networkCapabilities != null && networkCapabilities.hasCapability(12) && networkCapabilities.hasCapability(16);
        }

        public Context getContext() {
            return this.mContext;
        }

        public UserManager getUserManager() {
            return (UserManager) this.mContext.getSystemService("user");
        }

        public RebootEscrowKeyStoreManager getKeyStoreManager() {
            return this.mKeyStoreManager;
        }

        public RebootEscrowProviderInterface createRebootEscrowProviderIfNeeded() {
            if (this.mRebootEscrowProvider == null) {
                this.mRebootEscrowProvider = createRebootEscrowProvider();
            }
            return this.mRebootEscrowProvider;
        }

        PowerManager.WakeLock getWakeLock() {
            PowerManager pm = (PowerManager) this.mContext.getSystemService(PowerManager.class);
            return pm.newWakeLock(1, RebootEscrowManager.TAG);
        }

        public RebootEscrowProviderInterface getRebootEscrowProvider() {
            return this.mRebootEscrowProvider;
        }

        public void clearRebootEscrowProvider() {
            this.mRebootEscrowProvider = null;
        }

        public int getBootCount() {
            return Settings.Global.getInt(this.mContext.getContentResolver(), "boot_count", 0);
        }

        public long getCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        public int getLoadEscrowDataRetryLimit() {
            return DeviceConfig.getInt("ota", "load_escrow_data_retry_count", 3);
        }

        public int getLoadEscrowDataRetryIntervalSeconds() {
            return DeviceConfig.getInt("ota", "load_escrow_data_retry_interval_seconds", 30);
        }

        public void reportMetric(boolean success, int errorCode, int serviceType, int attemptCount, int escrowDurationInSeconds, int vbmetaDigestStatus, int durationSinceBootCompleteInSeconds) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.REBOOT_ESCROW_RECOVERY_REPORTED, success, errorCode, serviceType, attemptCount, escrowDurationInSeconds, vbmetaDigestStatus, durationSinceBootCompleteInSeconds);
        }

        public RebootEscrowEventLog getEventLog() {
            return new RebootEscrowEventLog();
        }

        public String getVbmetaDigest(boolean other) {
            return other ? SystemProperties.get(RebootEscrowManager.OTHER_VBMETA_DIGEST_PROP_NAME) : SystemProperties.get(RebootEscrowManager.VBMETA_DIGEST_PROP_NAME);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RebootEscrowManager(Context context, Callbacks callbacks, LockSettingsStorage storage) {
        this(new Injector(context, storage), callbacks, storage);
    }

    RebootEscrowManager(Injector injector, Callbacks callbacks, LockSettingsStorage storage) {
        this.mLoadEscrowDataErrorCode = 0;
        this.mKeyGenerationLock = new Object();
        this.mInjector = injector;
        this.mCallbacks = callbacks;
        this.mStorage = storage;
        this.mUserManager = injector.getUserManager();
        this.mEventLog = injector.getEventLog();
        this.mKeyStoreManager = injector.getKeyStoreManager();
    }

    private void onGetRebootEscrowKeyFailed(List<UserInfo> users, int attemptCount) {
        Slog.w(TAG, "Had reboot escrow data for users, but no key; removing escrow storage.");
        for (UserInfo user : users) {
            this.mStorage.removeRebootEscrow(user.id);
        }
        onEscrowRestoreComplete(false, attemptCount);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadRebootEscrowDataIfAvailable(final Handler retryHandler) {
        final List<UserInfo> users = this.mUserManager.getUsers();
        final List<UserInfo> rebootEscrowUsers = new ArrayList<>();
        for (UserInfo user : users) {
            if (this.mCallbacks.isUserSecure(user.id) && this.mStorage.hasRebootEscrow(user.id)) {
                rebootEscrowUsers.add(user);
            }
        }
        if (rebootEscrowUsers.isEmpty()) {
            Slog.i(TAG, "No reboot escrow data found for users, skipping loading escrow data");
            clearMetricsStorage();
            return;
        }
        PowerManager.WakeLock wakeLock = this.mInjector.getWakeLock();
        this.mWakeLock = wakeLock;
        if (wakeLock != null) {
            wakeLock.setReferenceCounted(false);
            this.mWakeLock.acquire(180000L);
        }
        this.mInjector.post(retryHandler, new Runnable() { // from class: com.android.server.locksettings.RebootEscrowManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RebootEscrowManager.this.m4584x991c3881(retryHandler, users, rebootEscrowUsers);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$loadRebootEscrowDataIfAvailable$0$com-android-server-locksettings-RebootEscrowManager  reason: not valid java name */
    public /* synthetic */ void m4584x991c3881(Handler retryHandler, List users, List rebootEscrowUsers) {
        m4585x77b81bfc(retryHandler, 0, users, rebootEscrowUsers);
    }

    void scheduleLoadRebootEscrowDataOrFail(final Handler retryHandler, final int attemptNumber, final List<UserInfo> users, final List<UserInfo> rebootEscrowUsers) {
        Objects.requireNonNull(retryHandler);
        int retryLimit = this.mInjector.getLoadEscrowDataRetryLimit();
        int retryIntervalInSeconds = this.mInjector.getLoadEscrowDataRetryIntervalSeconds();
        if (attemptNumber < retryLimit) {
            Slog.i(TAG, "Scheduling loadRebootEscrowData retry number: " + attemptNumber);
            this.mInjector.postDelayed(retryHandler, new Runnable() { // from class: com.android.server.locksettings.RebootEscrowManager$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    RebootEscrowManager.this.m4585x77b81bfc(retryHandler, attemptNumber, users, rebootEscrowUsers);
                }
            }, retryIntervalInSeconds * 1000);
            return;
        }
        Slog.w(TAG, "Failed to load reboot escrow data after " + attemptNumber + " attempts");
        if (this.mInjector.serverBasedResumeOnReboot() && !this.mInjector.isNetworkConnected()) {
            this.mLoadEscrowDataErrorCode = 8;
        } else {
            this.mLoadEscrowDataErrorCode = 4;
        }
        onGetRebootEscrowKeyFailed(users, attemptNumber);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: loadRebootEscrowDataWithRetry */
    public void m4585x77b81bfc(Handler retryHandler, int attemptNumber, List<UserInfo> users, List<UserInfo> rebootEscrowUsers) {
        SecretKey kk = this.mKeyStoreManager.getKeyStoreEncryptionKey();
        if (kk == null) {
            Slog.i(TAG, "Failed to load the key for resume on reboot from key store.");
        }
        try {
            RebootEscrowKey escrowKey = getAndClearRebootEscrowKey(kk);
            int providerType = 1;
            if (escrowKey != null) {
                this.mEventLog.addEntry(1);
                boolean allUsersUnlocked = true;
                for (UserInfo user : rebootEscrowUsers) {
                    allUsersUnlocked &= restoreRebootEscrowForUser(user.id, escrowKey, kk);
                }
                if (!allUsersUnlocked && this.mLoadEscrowDataErrorCode == 0) {
                    this.mLoadEscrowDataErrorCode = 5;
                }
                onEscrowRestoreComplete(allUsersUnlocked, attemptNumber + 1);
                return;
            }
            if (this.mLoadEscrowDataErrorCode == 0) {
                if (!this.mInjector.serverBasedResumeOnReboot()) {
                    providerType = 0;
                }
                if (providerType != this.mStorage.getInt(REBOOT_ESCROW_KEY_PROVIDER, -1, 0)) {
                    this.mLoadEscrowDataErrorCode = 6;
                } else {
                    this.mLoadEscrowDataErrorCode = 3;
                }
            }
            int providerType2 = attemptNumber + 1;
            onGetRebootEscrowKeyFailed(users, providerType2);
        } catch (IOException e) {
            Slog.i(TAG, "Failed to load escrow key, scheduling retry.", e);
            scheduleLoadRebootEscrowDataOrFail(retryHandler, attemptNumber + 1, users, rebootEscrowUsers);
        }
    }

    private void clearMetricsStorage() {
        this.mStorage.removeKey(REBOOT_ESCROW_ARMED_KEY, 0);
        this.mStorage.removeKey(REBOOT_ESCROW_KEY_ARMED_TIMESTAMP, 0);
        this.mStorage.removeKey(REBOOT_ESCROW_KEY_VBMETA_DIGEST, 0);
        this.mStorage.removeKey(REBOOT_ESCROW_KEY_OTHER_VBMETA_DIGEST, 0);
        this.mStorage.removeKey(REBOOT_ESCROW_KEY_PROVIDER, 0);
    }

    private int getVbmetaDigestStatusOnRestoreComplete() {
        String currentVbmetaDigest = this.mInjector.getVbmetaDigest(false);
        String vbmetaDigestStored = this.mStorage.getString(REBOOT_ESCROW_KEY_VBMETA_DIGEST, "", 0);
        String vbmetaDigestOtherStored = this.mStorage.getString(REBOOT_ESCROW_KEY_OTHER_VBMETA_DIGEST, "", 0);
        if (vbmetaDigestOtherStored.isEmpty()) {
            return currentVbmetaDigest.equals(vbmetaDigestStored) ? 0 : 2;
        } else if (currentVbmetaDigest.equals(vbmetaDigestOtherStored)) {
            return 0;
        } else {
            return currentVbmetaDigest.equals(vbmetaDigestStored) ? 1 : 2;
        }
    }

    private void reportMetricOnRestoreComplete(boolean success, int attemptCount) {
        int i;
        int escrowDurationInSeconds;
        if (this.mInjector.serverBasedResumeOnReboot()) {
            i = 2;
        } else {
            i = 1;
        }
        int serviceType = i;
        long armedTimestamp = this.mStorage.getLong(REBOOT_ESCROW_KEY_ARMED_TIMESTAMP, -1L, 0);
        long currentTimeStamp = this.mInjector.getCurrentTimeMillis();
        if (armedTimestamp != -1 && currentTimeStamp > armedTimestamp) {
            int escrowDurationInSeconds2 = ((int) (currentTimeStamp - armedTimestamp)) / 1000;
            escrowDurationInSeconds = escrowDurationInSeconds2;
        } else {
            escrowDurationInSeconds = -1;
        }
        int vbmetaDigestStatus = getVbmetaDigestStatusOnRestoreComplete();
        if (!success && this.mLoadEscrowDataErrorCode == 0) {
            this.mLoadEscrowDataErrorCode = 1;
        }
        Slog.i(TAG, "Reporting RoR recovery metrics, success: " + success + ", service type: " + serviceType + ", error code: " + this.mLoadEscrowDataErrorCode);
        this.mInjector.reportMetric(success, this.mLoadEscrowDataErrorCode, serviceType, attemptCount, escrowDurationInSeconds, vbmetaDigestStatus, -1);
        this.mLoadEscrowDataErrorCode = 0;
    }

    private void onEscrowRestoreComplete(boolean success, int attemptCount) {
        int previousBootCount = this.mStorage.getInt(REBOOT_ESCROW_ARMED_KEY, -1, 0);
        int bootCountDelta = this.mInjector.getBootCount() - previousBootCount;
        if (success || (previousBootCount != -1 && bootCountDelta <= 5)) {
            reportMetricOnRestoreComplete(success, attemptCount);
        }
        this.mKeyStoreManager.clearKeyStoreEncryptionKey();
        this.mInjector.clearRebootEscrowProvider();
        clearMetricsStorage();
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock != null) {
            wakeLock.release();
        }
    }

    private RebootEscrowKey getAndClearRebootEscrowKey(SecretKey kk) throws IOException {
        RebootEscrowProviderInterface rebootEscrowProvider = this.mInjector.createRebootEscrowProviderIfNeeded();
        if (rebootEscrowProvider == null) {
            Slog.w(TAG, "Had reboot escrow data for users, but RebootEscrowProvider is unavailable");
            this.mLoadEscrowDataErrorCode = 2;
            return null;
        } else if (rebootEscrowProvider.getType() == 1 && kk == null) {
            this.mLoadEscrowDataErrorCode = 7;
            return null;
        } else {
            RebootEscrowKey key = rebootEscrowProvider.getAndClearRebootEscrowKey(kk);
            if (key != null) {
                this.mEventLog.addEntry(4);
            }
            return key;
        }
    }

    private boolean restoreRebootEscrowForUser(int userId, RebootEscrowKey ks, SecretKey kk) {
        if (this.mStorage.hasRebootEscrow(userId)) {
            try {
                byte[] blob = this.mStorage.readRebootEscrow(userId);
                this.mStorage.removeRebootEscrow(userId);
                RebootEscrowData escrowData = RebootEscrowData.fromEncryptedData(ks, blob, kk);
                this.mCallbacks.onRebootEscrowRestored(escrowData.getSpVersion(), escrowData.getSyntheticPassword(), userId);
                Slog.i(TAG, "Restored reboot escrow data for user " + userId);
                this.mEventLog.addEntry(7, userId);
                return true;
            } catch (IOException e) {
                Slog.w(TAG, "Could not load reboot escrow data for user " + userId, e);
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void callToRebootEscrowIfNeeded(int userId, byte spVersion, byte[] syntheticPassword) {
        if (!this.mRebootEscrowWanted) {
            return;
        }
        if (this.mInjector.createRebootEscrowProviderIfNeeded() == null) {
            Slog.w(TAG, "Not storing escrow data, RebootEscrowProvider is unavailable");
            return;
        }
        RebootEscrowKey escrowKey = generateEscrowKeyIfNeeded();
        if (escrowKey == null) {
            Slog.e(TAG, "Could not generate escrow key");
            return;
        }
        SecretKey kk = this.mKeyStoreManager.generateKeyStoreEncryptionKeyIfNeeded();
        if (kk == null) {
            Slog.e(TAG, "Failed to generate encryption key from keystore.");
            return;
        }
        try {
            RebootEscrowData escrowData = RebootEscrowData.fromSyntheticPassword(escrowKey, spVersion, syntheticPassword, kk);
            this.mStorage.writeRebootEscrow(userId, escrowData.getBlob());
            this.mEventLog.addEntry(6, userId);
            setRebootEscrowReady(true);
        } catch (IOException e) {
            setRebootEscrowReady(false);
            Slog.w(TAG, "Could not escrow reboot data", e);
        }
    }

    private RebootEscrowKey generateEscrowKeyIfNeeded() {
        synchronized (this.mKeyGenerationLock) {
            RebootEscrowKey rebootEscrowKey = this.mPendingRebootEscrowKey;
            if (rebootEscrowKey != null) {
                return rebootEscrowKey;
            }
            try {
                RebootEscrowKey key = RebootEscrowKey.generate();
                this.mPendingRebootEscrowKey = key;
                return key;
            } catch (IOException e) {
                Slog.w(TAG, "Could not generate reboot escrow key");
                return null;
            }
        }
    }

    private void clearRebootEscrowIfNeeded() {
        this.mRebootEscrowWanted = false;
        setRebootEscrowReady(false);
        RebootEscrowProviderInterface rebootEscrowProvider = this.mInjector.createRebootEscrowProviderIfNeeded();
        if (rebootEscrowProvider == null) {
            Slog.w(TAG, "RebootEscrowProvider is unavailable for clear request");
        } else {
            rebootEscrowProvider.clearRebootEscrowKey();
        }
        this.mInjector.clearRebootEscrowProvider();
        clearMetricsStorage();
        List<UserInfo> users = this.mUserManager.getUsers();
        for (UserInfo user : users) {
            this.mStorage.removeRebootEscrow(user.id);
        }
        this.mEventLog.addEntry(3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int armRebootEscrowIfNeeded() {
        int expectedProviderType;
        RebootEscrowKey escrowKey;
        if (this.mRebootEscrowReady) {
            RebootEscrowProviderInterface rebootEscrowProvider = this.mInjector.getRebootEscrowProvider();
            if (rebootEscrowProvider == null) {
                Slog.w(TAG, "Not storing escrow key, RebootEscrowProvider is unavailable");
                clearRebootEscrowIfNeeded();
                return 3;
            }
            if (this.mInjector.serverBasedResumeOnReboot()) {
                expectedProviderType = 1;
            } else {
                expectedProviderType = 0;
            }
            int actualProviderType = rebootEscrowProvider.getType();
            if (expectedProviderType != actualProviderType) {
                Slog.w(TAG, "Expect reboot escrow provider " + expectedProviderType + ", but the RoR is prepared with " + actualProviderType + ". Please prepare the RoR again.");
                clearRebootEscrowIfNeeded();
                return 4;
            }
            synchronized (this.mKeyGenerationLock) {
                escrowKey = this.mPendingRebootEscrowKey;
            }
            if (escrowKey == null) {
                Slog.e(TAG, "Escrow key is null, but escrow was marked as ready");
                clearRebootEscrowIfNeeded();
                return 5;
            }
            SecretKey kk = this.mKeyStoreManager.getKeyStoreEncryptionKey();
            if (kk == null) {
                Slog.e(TAG, "Failed to get encryption key from keystore.");
                clearRebootEscrowIfNeeded();
                return 6;
            }
            boolean armedRebootEscrow = rebootEscrowProvider.storeRebootEscrowKey(escrowKey, kk);
            if (!armedRebootEscrow) {
                return 7;
            }
            this.mStorage.setInt(REBOOT_ESCROW_ARMED_KEY, this.mInjector.getBootCount(), 0);
            this.mStorage.setLong(REBOOT_ESCROW_KEY_ARMED_TIMESTAMP, this.mInjector.getCurrentTimeMillis(), 0);
            this.mStorage.setString(REBOOT_ESCROW_KEY_VBMETA_DIGEST, this.mInjector.getVbmetaDigest(false), 0);
            this.mStorage.setString(REBOOT_ESCROW_KEY_OTHER_VBMETA_DIGEST, this.mInjector.getVbmetaDigest(true), 0);
            this.mStorage.setInt(REBOOT_ESCROW_KEY_PROVIDER, actualProviderType, 0);
            this.mEventLog.addEntry(2);
            return 0;
        }
        return 2;
    }

    private void setRebootEscrowReady(boolean ready) {
        if (this.mRebootEscrowReady != ready) {
            this.mRebootEscrowListener.onPreparedForReboot(ready);
        }
        this.mRebootEscrowReady = ready;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean prepareRebootEscrow() {
        clearRebootEscrowIfNeeded();
        if (this.mInjector.createRebootEscrowProviderIfNeeded() == null) {
            Slog.w(TAG, "No reboot escrow provider, skipping resume on reboot preparation.");
            return false;
        }
        this.mRebootEscrowWanted = true;
        this.mEventLog.addEntry(5);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean clearRebootEscrow() {
        clearRebootEscrowIfNeeded();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRebootEscrowListener(RebootEscrowListener listener) {
        this.mRebootEscrowListener = listener;
    }

    /* loaded from: classes.dex */
    public static class RebootEscrowEvent {
        static final int CLEARED_LSKF_REQUEST = 3;
        static final int FOUND_ESCROW_DATA = 1;
        static final int REQUESTED_LSKF = 5;
        static final int RETRIEVED_LSKF_FOR_USER = 7;
        static final int RETRIEVED_STORED_KEK = 4;
        static final int SET_ARMED_STATUS = 2;
        static final int STORED_LSKF_FOR_USER = 6;
        final int mEventId;
        final long mTimestamp;
        final Integer mUserId;
        final long mWallTime;

        RebootEscrowEvent(int eventId) {
            this(eventId, null);
        }

        RebootEscrowEvent(int eventId, Integer userId) {
            this.mEventId = eventId;
            this.mUserId = userId;
            this.mTimestamp = SystemClock.uptimeMillis();
            this.mWallTime = System.currentTimeMillis();
        }

        String getEventDescription() {
            switch (this.mEventId) {
                case 1:
                    return "Found escrow data";
                case 2:
                    return "Set armed status";
                case 3:
                    return "Cleared request for LSKF";
                case 4:
                    return "Retrieved stored KEK";
                case 5:
                    return "Requested LSKF";
                case 6:
                    return "Stored LSKF for user";
                case 7:
                    return "Retrieved LSKF for user";
                default:
                    return "Unknown event ID " + this.mEventId;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class RebootEscrowEventLog {
        private RebootEscrowEvent[] mEntries = new RebootEscrowEvent[16];
        private int mNextIndex = 0;

        void addEntry(int eventId) {
            addEntryInternal(new RebootEscrowEvent(eventId));
        }

        void addEntry(int eventId, int userId) {
            addEntryInternal(new RebootEscrowEvent(eventId, Integer.valueOf(userId)));
        }

        private void addEntryInternal(RebootEscrowEvent event) {
            int index = this.mNextIndex;
            RebootEscrowEvent[] rebootEscrowEventArr = this.mEntries;
            rebootEscrowEventArr[index] = event;
            this.mNextIndex = (this.mNextIndex + 1) % rebootEscrowEventArr.length;
        }

        void dump(IndentingPrintWriter pw) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            int i = 0;
            while (true) {
                RebootEscrowEvent[] rebootEscrowEventArr = this.mEntries;
                if (i < rebootEscrowEventArr.length) {
                    RebootEscrowEvent event = rebootEscrowEventArr[(this.mNextIndex + i) % rebootEscrowEventArr.length];
                    if (event != null) {
                        pw.print("Event #");
                        pw.println(i);
                        pw.println(" time=" + sdf.format(new Date(event.mWallTime)) + " (timestamp=" + event.mTimestamp + ")");
                        pw.print(" event=");
                        pw.println(event.getEventDescription());
                        if (event.mUserId != null) {
                            pw.print(" user=");
                            pw.println(event.mUserId);
                        }
                    }
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        boolean keySet;
        pw.print("mRebootEscrowWanted=");
        pw.println(this.mRebootEscrowWanted);
        pw.print("mRebootEscrowReady=");
        pw.println(this.mRebootEscrowReady);
        pw.print("mRebootEscrowListener=");
        pw.println(this.mRebootEscrowListener);
        synchronized (this.mKeyGenerationLock) {
            keySet = this.mPendingRebootEscrowKey != null;
        }
        pw.print("mPendingRebootEscrowKey is ");
        pw.println(keySet ? "set" : "not set");
        RebootEscrowProviderInterface provider = this.mInjector.getRebootEscrowProvider();
        String providerType = provider == null ? "null" : String.valueOf(provider.getType());
        pw.print("RebootEscrowProvider type is " + providerType);
        pw.println();
        pw.println("Event log:");
        pw.increaseIndent();
        this.mEventLog.dump(pw);
        pw.println();
        pw.decreaseIndent();
    }
}
