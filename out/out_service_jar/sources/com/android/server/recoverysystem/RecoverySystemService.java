package com.android.server.recoverysystem;

import android.apex.CompressedApexInfo;
import android.apex.CompressedApexInfoList;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.boot.V1_2.IBootControl;
import android.net.INetd;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Binder;
import android.os.Environment;
import android.os.IHwInterface;
import android.os.IRecoverySystem;
import android.os.IRecoverySystemProgressListener;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.ota.nano.OtaPackageMetadata;
import android.provider.DeviceConfig;
import android.sysprop.ApexProperties;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.FastImmutableArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.widget.LockSettingsInternal;
import com.android.internal.widget.RebootEscrowListener;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.ApexManager;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class RecoverySystemService extends IRecoverySystem.Stub implements RebootEscrowListener {
    static final String AB_UPDATE = "ro.build.ab_update";
    private static final long APEX_INFO_SIZE_LIMIT = 2457600;
    private static final boolean DEBUG = false;
    static final String INIT_SERVICE_CLEAR_BCB = "init.svc.clear-bcb";
    static final String INIT_SERVICE_SETUP_BCB = "init.svc.setup-bcb";
    static final String INIT_SERVICE_UNCRYPT = "init.svc.uncrypt";
    static final String LSKF_CAPTURED_COUNT_PREF = "lskf_captured_count";
    static final String LSKF_CAPTURED_TIMESTAMP_PREF = "lskf_captured_timestamp";
    static final String REQUEST_LSKF_COUNT_PREF_SUFFIX = "_request_lskf_count";
    static final String REQUEST_LSKF_TIMESTAMP_PREF_SUFFIX = "_request_lskf_timestamp";
    private static final int ROR_NEED_PREPARATION = 0;
    private static final int ROR_NOT_REQUESTED = 0;
    private static final int ROR_REQUESTED_NEED_CLEAR = 1;
    private static final int ROR_REQUESTED_SKIP_CLEAR = 2;
    private static final int ROR_SKIP_PREPARATION_AND_NOTIFY = 1;
    private static final int ROR_SKIP_PREPARATION_NOT_NOTIFY = 2;
    private static final int SOCKET_CONNECTION_MAX_RETRY = 30;
    private static final String TAG = "RecoverySystemService";
    private static final String UNCRYPT_SOCKET = "uncrypt";
    private final ArrayMap<String, IntentSender> mCallerPendingRequest;
    private final ArraySet<String> mCallerPreparedForReboot;
    private final Context mContext;
    private final Injector mInjector;
    private static final Object sRequestLock = new Object();
    static final FastImmutableArraySet<Integer> FATAL_ARM_ESCROW_ERRORS = new FastImmutableArraySet<>(new Integer[]{2, 3, 4, 5, 6});

    /* loaded from: classes2.dex */
    private @interface ResumeOnRebootActionsOnClear {
    }

    /* loaded from: classes2.dex */
    private @interface ResumeOnRebootActionsOnRequest {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class RebootPreparationError {
        final int mProviderErrorCode;
        final int mRebootErrorCode;

        RebootPreparationError(int rebootErrorCode, int providerErrorCode) {
            this.mRebootErrorCode = rebootErrorCode;
            this.mProviderErrorCode = providerErrorCode;
        }

        int getErrorCodeForMetrics() {
            return this.mRebootErrorCode + this.mProviderErrorCode;
        }
    }

    /* loaded from: classes2.dex */
    public static class PreferencesManager {
        private static final String METRICS_DIR = "recovery_system";
        private static final String METRICS_PREFS_FILE = "RecoverySystemMetricsPrefs.xml";
        private final File mMetricsPrefsFile;
        protected final SharedPreferences mSharedPreferences;

        PreferencesManager(Context context) {
            File prefsDir = new File(Environment.getDataSystemCeDirectory(0), METRICS_DIR);
            File file = new File(prefsDir, METRICS_PREFS_FILE);
            this.mMetricsPrefsFile = file;
            this.mSharedPreferences = context.getSharedPreferences(file, 0);
        }

        public long getLong(String key, long defaultValue) {
            return this.mSharedPreferences.getLong(key, defaultValue);
        }

        public int getInt(String key, int defaultValue) {
            return this.mSharedPreferences.getInt(key, defaultValue);
        }

        public void putLong(String key, long value) {
            this.mSharedPreferences.edit().putLong(key, value).commit();
        }

        public void putInt(String key, int value) {
            this.mSharedPreferences.edit().putInt(key, value).commit();
        }

        public synchronized void incrementIntKey(String key, int defaultInitialValue) {
            int oldValue = getInt(key, defaultInitialValue);
            putInt(key, oldValue + 1);
        }

        public void deletePrefsFile() {
            if (!this.mMetricsPrefsFile.delete()) {
                Slog.w(RecoverySystemService.TAG, "Failed to delete metrics prefs");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Injector {
        protected final Context mContext;
        protected final PreferencesManager mPrefs;

        Injector(Context context) {
            this.mContext = context;
            this.mPrefs = new PreferencesManager(context);
        }

        public Context getContext() {
            return this.mContext;
        }

        public LockSettingsInternal getLockSettingsService() {
            return (LockSettingsInternal) LocalServices.getService(LockSettingsInternal.class);
        }

        public PowerManager getPowerManager() {
            return (PowerManager) this.mContext.getSystemService("power");
        }

        public String systemPropertiesGet(String key) {
            return SystemProperties.get(key);
        }

        public void systemPropertiesSet(String key, String value) {
            SystemProperties.set(key, value);
        }

        public boolean uncryptPackageFileDelete() {
            return RecoverySystem.UNCRYPT_PACKAGE_FILE.delete();
        }

        public String getUncryptPackageFileName() {
            return RecoverySystem.UNCRYPT_PACKAGE_FILE.getName();
        }

        public FileWriter getUncryptPackageFileWriter() throws IOException {
            return new FileWriter(RecoverySystem.UNCRYPT_PACKAGE_FILE);
        }

        public UncryptSocket connectService() {
            UncryptSocket socket = new UncryptSocket();
            if (!socket.connectService()) {
                socket.close();
                return null;
            }
            return socket;
        }

        public IBootControl getBootControl() throws RemoteException {
            android.hardware.boot.V1_0.IBootControl bootControlV10 = android.hardware.boot.V1_0.IBootControl.getService(true);
            if (bootControlV10 == null) {
                throw new RemoteException("Failed to get boot control HAL V1_0.");
            }
            IBootControl bootControlV12 = IBootControl.castFrom((IHwInterface) bootControlV10);
            if (bootControlV12 == null) {
                Slog.w(RecoverySystemService.TAG, "Device doesn't implement boot control HAL V1_2.");
                return null;
            }
            return bootControlV12;
        }

        public void threadSleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }

        public int getUidFromPackageName(String packageName) {
            try {
                return this.mContext.getPackageManager().getPackageUidAsUser(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Slog.w(RecoverySystemService.TAG, "Failed to find uid for " + packageName);
                return -1;
            }
        }

        public PreferencesManager getMetricsPrefs() {
            return this.mPrefs;
        }

        public long getCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        public void reportRebootEscrowPreparationMetrics(int uid, int requestResult, int requestedClientCount) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.REBOOT_ESCROW_PREPARATION_REPORTED, uid, requestResult, requestedClientCount);
        }

        public void reportRebootEscrowLskfCapturedMetrics(int uid, int requestedClientCount, int requestedToLskfCapturedDurationInSeconds) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.REBOOT_ESCROW_LSKF_CAPTURE_REPORTED, uid, requestedClientCount, requestedToLskfCapturedDurationInSeconds);
        }

        public void reportRebootEscrowRebootMetrics(int errorCode, int uid, int preparedClientCount, int requestCount, boolean slotSwitch, boolean serverBased, int lskfCapturedToRebootDurationInSeconds, int lskfCapturedCounts) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.REBOOT_ESCROW_REBOOT_REPORTED, errorCode, uid, preparedClientCount, requestCount, slotSwitch, serverBased, lskfCapturedToRebootDurationInSeconds, lskfCapturedCounts);
        }
    }

    /* loaded from: classes2.dex */
    public static final class Lifecycle extends SystemService {
        private RecoverySystemService mRecoverySystemService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 500) {
                this.mRecoverySystemService.onSystemServicesReady();
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.recoverysystem.RecoverySystemService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.recoverysystem.RecoverySystemService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? recoverySystemService = new RecoverySystemService(getContext());
            this.mRecoverySystemService = recoverySystemService;
            publishBinderService("recovery", recoverySystemService);
        }
    }

    private RecoverySystemService(Context context) {
        this(new Injector(context));
    }

    RecoverySystemService(Injector injector) {
        this.mCallerPendingRequest = new ArrayMap<>();
        this.mCallerPreparedForReboot = new ArraySet<>();
        this.mInjector = injector;
        this.mContext = injector.getContext();
    }

    void onSystemServicesReady() {
        LockSettingsInternal lockSettings = this.mInjector.getLockSettingsService();
        if (lockSettings == null) {
            Slog.e(TAG, "Failed to get lock settings service, skipping set RebootEscrowListener");
        } else {
            lockSettings.setRebootEscrowListener(this);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [488=5] */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00af, code lost:
        android.util.Slog.e(com.android.server.recoverysystem.RecoverySystemService.TAG, "uncrypt failed with status: " + r4);
        r1.sendAck();
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00cc, code lost:
        r1.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x00d0, code lost:
        return false;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean uncrypt(String filename, IRecoverySystemProgressListener listener) {
        synchronized (sRequestLock) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            if (!checkAndWaitForUncryptService()) {
                Slog.e(TAG, "uncrypt service is unavailable.");
                return false;
            }
            this.mInjector.uncryptPackageFileDelete();
            try {
                FileWriter uncryptFile = this.mInjector.getUncryptPackageFileWriter();
                try {
                    uncryptFile.write(filename + "\n");
                    if (uncryptFile != null) {
                        uncryptFile.close();
                    }
                    this.mInjector.systemPropertiesSet("ctl.start", UNCRYPT_SOCKET);
                    UncryptSocket socket = this.mInjector.connectService();
                    if (socket == null) {
                        Slog.e(TAG, "Failed to connect to uncrypt socket");
                        return false;
                    }
                    int lastStatus = Integer.MIN_VALUE;
                    while (true) {
                        try {
                            int status = socket.getPercentageUncrypted();
                            if (status != lastStatus || lastStatus == Integer.MIN_VALUE) {
                                lastStatus = status;
                                if (status < 0 || status > 100) {
                                    break;
                                }
                                Slog.i(TAG, "uncrypt read status: " + status);
                                if (listener != null) {
                                    try {
                                        listener.onProgress(status);
                                    } catch (RemoteException e) {
                                        Slog.w(TAG, "RemoteException when posting progress");
                                    }
                                }
                                if (status == 100) {
                                    Slog.i(TAG, "uncrypt successfully finished.");
                                    socket.sendAck();
                                    socket.close();
                                    return true;
                                }
                            }
                        } catch (IOException e2) {
                            Slog.e(TAG, "IOException when reading status: ", e2);
                            socket.close();
                            return false;
                        }
                    }
                } catch (Throwable th) {
                    if (uncryptFile != null) {
                        try {
                            uncryptFile.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (IOException e3) {
                Slog.e(TAG, "IOException when writing \"" + this.mInjector.getUncryptPackageFileName() + "\":", e3);
                return false;
            }
        }
    }

    public boolean clearBcb() {
        boolean z;
        synchronized (sRequestLock) {
            z = setupOrClearBcb(false, null);
        }
        return z;
    }

    public boolean setupBcb(String command) {
        boolean z;
        synchronized (sRequestLock) {
            z = setupOrClearBcb(true, command);
        }
        return z;
    }

    public void rebootRecoveryWithCommand(String command) {
        synchronized (sRequestLock) {
            if (setupOrClearBcb(true, command)) {
                PowerManager pm = this.mInjector.getPowerManager();
                pm.reboot("recovery");
            }
        }
    }

    private void enforcePermissionForResumeOnReboot() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.RECOVERY") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.REBOOT") != 0) {
            throw new SecurityException("Caller must have android.permission.RECOVERY or android.permission.REBOOT for resume on reboot.");
        }
    }

    private void reportMetricsOnRequestLskf(String packageName, int requestResult) {
        int pendingRequestCount;
        int uid = this.mInjector.getUidFromPackageName(packageName);
        synchronized (this) {
            pendingRequestCount = this.mCallerPendingRequest.size();
        }
        PreferencesManager prefs = this.mInjector.getMetricsPrefs();
        prefs.putLong(packageName + REQUEST_LSKF_TIMESTAMP_PREF_SUFFIX, this.mInjector.getCurrentTimeMillis());
        prefs.incrementIntKey(packageName + REQUEST_LSKF_COUNT_PREF_SUFFIX, 0);
        this.mInjector.reportRebootEscrowPreparationMetrics(uid, requestResult, pendingRequestCount);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [587=4] */
    public boolean requestLskf(String packageName, IntentSender intentSender) {
        enforcePermissionForResumeOnReboot();
        if (packageName == null) {
            Slog.w(TAG, "Missing packageName when requesting lskf.");
            return false;
        }
        int action = updateRoRPreparationStateOnNewRequest(packageName, intentSender);
        reportMetricsOnRequestLskf(packageName, action);
        switch (action) {
            case 0:
                long origId = Binder.clearCallingIdentity();
                try {
                    LockSettingsInternal lockSettings = this.mInjector.getLockSettingsService();
                    if (lockSettings == null) {
                        Slog.e(TAG, "Failed to get lock settings service, skipping prepareRebootEscrow");
                        return false;
                    } else if (lockSettings.prepareRebootEscrow()) {
                        return true;
                    } else {
                        clearRoRPreparationState();
                        return false;
                    }
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            case 1:
                sendPreparedForRebootIntentIfNeeded(intentSender);
                return true;
            case 2:
                return true;
            default:
                throw new IllegalStateException("Unsupported action type on new request " + action);
        }
    }

    private synchronized int updateRoRPreparationStateOnNewRequest(String packageName, IntentSender intentSender) {
        if (!this.mCallerPreparedForReboot.isEmpty()) {
            if (this.mCallerPreparedForReboot.contains(packageName)) {
                Slog.i(TAG, "RoR already has prepared for " + packageName);
            }
            this.mCallerPreparedForReboot.add(packageName);
            return 1;
        }
        boolean needPreparation = this.mCallerPendingRequest.isEmpty();
        if (this.mCallerPendingRequest.containsKey(packageName)) {
            Slog.i(TAG, "Duplicate RoR preparation request for " + packageName);
        }
        this.mCallerPendingRequest.put(packageName, intentSender);
        return needPreparation ? 0 : 2;
    }

    private void reportMetricsOnPreparedForReboot() {
        List<String> preparedClients;
        long currentTimestamp = this.mInjector.getCurrentTimeMillis();
        synchronized (this) {
            preparedClients = new ArrayList<>(this.mCallerPreparedForReboot);
        }
        PreferencesManager prefs = this.mInjector.getMetricsPrefs();
        prefs.putLong(LSKF_CAPTURED_TIMESTAMP_PREF, currentTimestamp);
        prefs.incrementIntKey(LSKF_CAPTURED_COUNT_PREF, 0);
        for (String packageName : preparedClients) {
            int uid = this.mInjector.getUidFromPackageName(packageName);
            int durationSeconds = -1;
            long requestLskfTimestamp = prefs.getLong(packageName + REQUEST_LSKF_TIMESTAMP_PREF_SUFFIX, -1L);
            if (requestLskfTimestamp != -1 && currentTimestamp > requestLskfTimestamp) {
                durationSeconds = ((int) (currentTimestamp - requestLskfTimestamp)) / 1000;
            }
            Slog.i(TAG, String.format("Reporting lskf captured, lskf capture takes %d seconds for package %s", Integer.valueOf(durationSeconds), packageName));
            this.mInjector.reportRebootEscrowLskfCapturedMetrics(uid, preparedClients.size(), durationSeconds);
        }
    }

    public void onPreparedForReboot(boolean ready) {
        if (!ready) {
            return;
        }
        updateRoRPreparationStateOnPreparedForReboot();
        reportMetricsOnPreparedForReboot();
    }

    private synchronized void updateRoRPreparationStateOnPreparedForReboot() {
        if (!this.mCallerPreparedForReboot.isEmpty()) {
            Slog.w(TAG, "onPreparedForReboot called when some clients have prepared.");
        }
        if (this.mCallerPendingRequest.isEmpty()) {
            Slog.w(TAG, "onPreparedForReboot called but no client has requested.");
        }
        for (int i = 0; i < this.mCallerPendingRequest.size(); i++) {
            sendPreparedForRebootIntentIfNeeded(this.mCallerPendingRequest.valueAt(i));
            this.mCallerPreparedForReboot.add(this.mCallerPendingRequest.keyAt(i));
        }
        this.mCallerPendingRequest.clear();
    }

    private void sendPreparedForRebootIntentIfNeeded(IntentSender intentSender) {
        if (intentSender != null) {
            try {
                intentSender.sendIntent(null, 0, null, null, null);
            } catch (IntentSender.SendIntentException e) {
                Slog.w(TAG, "Could not send intent for prepared reboot: " + e.getMessage());
            }
        }
    }

    public boolean clearLskf(String packageName) {
        enforcePermissionForResumeOnReboot();
        if (packageName == null) {
            Slog.w(TAG, "Missing packageName when clearing lskf.");
            return false;
        }
        int action = updateRoRPreparationStateOnClear(packageName);
        switch (action) {
            case 0:
                Slog.w(TAG, "RoR clear called before preparation for caller " + packageName);
                return true;
            case 1:
                long origId = Binder.clearCallingIdentity();
                try {
                    LockSettingsInternal lockSettings = this.mInjector.getLockSettingsService();
                    if (lockSettings == null) {
                        Slog.e(TAG, "Failed to get lock settings service, skipping clearRebootEscrow");
                        return false;
                    }
                    return lockSettings.clearRebootEscrow();
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            case 2:
                return true;
            default:
                throw new IllegalStateException("Unsupported action type on clear " + action);
        }
    }

    private synchronized int updateRoRPreparationStateOnClear(String packageName) {
        boolean z = false;
        if (!this.mCallerPreparedForReboot.contains(packageName) && !this.mCallerPendingRequest.containsKey(packageName)) {
            Slog.w(TAG, packageName + " hasn't prepared for resume on reboot");
            return 0;
        }
        this.mCallerPendingRequest.remove(packageName);
        this.mCallerPreparedForReboot.remove(packageName);
        if (this.mCallerPendingRequest.isEmpty() && this.mCallerPreparedForReboot.isEmpty()) {
            z = true;
        }
        boolean needClear = z;
        return needClear ? 1 : 2;
    }

    private boolean isAbDevice() {
        return "true".equalsIgnoreCase(this.mInjector.systemPropertiesGet(AB_UPDATE));
    }

    private boolean verifySlotForNextBoot(boolean slotSwitch) {
        if (!isAbDevice()) {
            Slog.w(TAG, "Device isn't a/b, skipping slot verification.");
            return true;
        }
        try {
            IBootControl bootControl = this.mInjector.getBootControl();
            if (bootControl == null) {
                Slog.w(TAG, "Cannot get the boot control HAL, skipping slot verification.");
                return true;
            }
            try {
                int current_slot = bootControl.getCurrentSlot();
                if (current_slot != 0 && current_slot != 1) {
                    throw new IllegalStateException("Current boot slot should be 0 or 1, got " + current_slot);
                }
                int next_active_slot = bootControl.getActiveBootSlot();
                int expected_active_slot = current_slot;
                if (slotSwitch) {
                    expected_active_slot = current_slot == 0 ? 1 : 0;
                }
                if (next_active_slot != expected_active_slot) {
                    Slog.w(TAG, "The next active boot slot doesn't match the expected value, expected " + expected_active_slot + ", got " + next_active_slot);
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to query the active slots", e);
                return false;
            }
        } catch (RemoteException e2) {
            Slog.w(TAG, "Failed to get the boot control HAL " + e2);
            return false;
        }
    }

    private RebootPreparationError armRebootEscrow(String packageName, boolean slotSwitch) {
        if (packageName == null) {
            Slog.w(TAG, "Missing packageName when rebooting with lskf.");
            return new RebootPreparationError(2000, 0);
        } else if (!isLskfCaptured(packageName)) {
            return new RebootPreparationError(3000, 0);
        } else {
            if (!verifySlotForNextBoot(slotSwitch)) {
                return new RebootPreparationError(4000, 0);
            }
            long origId = Binder.clearCallingIdentity();
            try {
                LockSettingsInternal lockSettings = this.mInjector.getLockSettingsService();
                if (lockSettings == null) {
                    Slog.e(TAG, "Failed to get lock settings service, skipping armRebootEscrow");
                    return new RebootPreparationError(5000, 3);
                }
                int providerErrorCode = lockSettings.armRebootEscrow();
                if (providerErrorCode != 0) {
                    Slog.w(TAG, "Failure to escrow key for reboot, providerErrorCode: " + providerErrorCode);
                    return new RebootPreparationError(5000, providerErrorCode);
                }
                return new RebootPreparationError(0, 0);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    private boolean useServerBasedRoR() {
        long origId = Binder.clearCallingIdentity();
        try {
            return DeviceConfig.getBoolean("ota", "server_based_ror_enabled", false);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void reportMetricsOnRebootWithLskf(String packageName, boolean slotSwitch, RebootPreparationError escrowError) {
        int preparedClientCount;
        int uid = this.mInjector.getUidFromPackageName(packageName);
        boolean serverBased = useServerBasedRoR();
        synchronized (this) {
            preparedClientCount = this.mCallerPreparedForReboot.size();
        }
        long currentTimestamp = this.mInjector.getCurrentTimeMillis();
        int durationSeconds = -1;
        PreferencesManager prefs = this.mInjector.getMetricsPrefs();
        long lskfCapturedTimestamp = prefs.getLong(LSKF_CAPTURED_TIMESTAMP_PREF, -1L);
        if (lskfCapturedTimestamp != -1 && currentTimestamp > lskfCapturedTimestamp) {
            durationSeconds = ((int) (currentTimestamp - lskfCapturedTimestamp)) / 1000;
        }
        int requestCount = prefs.getInt(packageName + REQUEST_LSKF_COUNT_PREF_SUFFIX, -1);
        int lskfCapturedCount = prefs.getInt(LSKF_CAPTURED_COUNT_PREF, -1);
        Slog.i(TAG, String.format("Reporting reboot with lskf, package name %s, client count %d, request count %d, lskf captured count %d, duration since lskf captured %d seconds.", packageName, Integer.valueOf(preparedClientCount), Integer.valueOf(requestCount), Integer.valueOf(lskfCapturedCount), Integer.valueOf(durationSeconds)));
        this.mInjector.reportRebootEscrowRebootMetrics(escrowError.getErrorCodeForMetrics(), uid, preparedClientCount, requestCount, slotSwitch, serverBased, durationSeconds, lskfCapturedCount);
    }

    private synchronized void clearRoRPreparationState() {
        this.mCallerPendingRequest.clear();
        this.mCallerPreparedForReboot.clear();
    }

    private void clearRoRPreparationStateOnRebootFailure(RebootPreparationError escrowError) {
        if (!FATAL_ARM_ESCROW_ERRORS.contains(Integer.valueOf(escrowError.mProviderErrorCode))) {
            return;
        }
        Slog.w(TAG, "Clearing resume on reboot states for all clients on arm escrow error: " + escrowError.mProviderErrorCode);
        clearRoRPreparationState();
    }

    private int rebootWithLskfImpl(String packageName, String reason, boolean slotSwitch) {
        RebootPreparationError escrowError = armRebootEscrow(packageName, slotSwitch);
        reportMetricsOnRebootWithLskf(packageName, slotSwitch, escrowError);
        clearRoRPreparationStateOnRebootFailure(escrowError);
        int errorCode = escrowError.mRebootErrorCode;
        if (errorCode != 0) {
            return errorCode;
        }
        this.mInjector.getMetricsPrefs().deletePrefsFile();
        PowerManager pm = this.mInjector.getPowerManager();
        pm.reboot(reason);
        return 1000;
    }

    public int rebootWithLskfAssumeSlotSwitch(String packageName, String reason) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
        return rebootWithLskfImpl(packageName, reason, true);
    }

    public int rebootWithLskf(String packageName, String reason, boolean slotSwitch) {
        enforcePermissionForResumeOnReboot();
        return rebootWithLskfImpl(packageName, reason, slotSwitch);
    }

    public static boolean isUpdatableApexSupported() {
        return ((Boolean) ApexProperties.updatable().orElse(false)).booleanValue();
    }

    private static CompressedApexInfoList getCompressedApexInfoList(String packageFile) throws IOException {
        ZipFile zipFile = new ZipFile(packageFile);
        try {
            ZipEntry entry = zipFile.getEntry("apex_info.pb");
            if (entry != null) {
                if (entry.getSize() >= APEX_INFO_SIZE_LIMIT) {
                    throw new IllegalArgumentException("apex_info.pb has size " + entry.getSize() + " which is larger than the permitted limit" + APEX_INFO_SIZE_LIMIT);
                }
                if (entry.getSize() != 0) {
                    Log.i(TAG, "Allocating " + entry.getSize() + " bytes of memory to store OTA Metadata");
                    byte[] data = new byte[(int) entry.getSize()];
                    InputStream is = zipFile.getInputStream(entry);
                    int bytesRead = is.read(data);
                    String msg = "Read " + bytesRead + " when expecting " + data.length;
                    Log.e(TAG, msg);
                    if (bytesRead != data.length) {
                        throw new IOException(msg);
                    }
                    if (is != null) {
                        is.close();
                    }
                    OtaPackageMetadata.ApexMetadata metadata = OtaPackageMetadata.ApexMetadata.parseFrom(data);
                    CompressedApexInfoList apexInfoList = new CompressedApexInfoList();
                    apexInfoList.apexInfos = (CompressedApexInfo[]) Arrays.stream(metadata.apexInfo).filter(new Predicate() { // from class: com.android.server.recoverysystem.RecoverySystemService$$ExternalSyntheticLambda0
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            boolean z;
                            z = ((OtaPackageMetadata.ApexInfo) obj).isCompressed;
                            return z;
                        }
                    }).map(new Function() { // from class: com.android.server.recoverysystem.RecoverySystemService$$ExternalSyntheticLambda1
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            return RecoverySystemService.lambda$getCompressedApexInfoList$1((OtaPackageMetadata.ApexInfo) obj);
                        }
                    }).toArray(new IntFunction() { // from class: com.android.server.recoverysystem.RecoverySystemService$$ExternalSyntheticLambda2
                        @Override // java.util.function.IntFunction
                        public final Object apply(int i) {
                            return RecoverySystemService.lambda$getCompressedApexInfoList$2(i);
                        }
                    });
                    zipFile.close();
                    return apexInfoList;
                }
                CompressedApexInfoList infoList = new CompressedApexInfoList();
                infoList.apexInfos = new CompressedApexInfo[0];
                zipFile.close();
                return infoList;
            }
            zipFile.close();
            return null;
        } catch (Throwable th) {
            try {
                zipFile.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ CompressedApexInfo lambda$getCompressedApexInfoList$1(OtaPackageMetadata.ApexInfo apex) {
        CompressedApexInfo info = new CompressedApexInfo();
        info.moduleName = apex.packageName;
        info.decompressedSize = apex.decompressedSize;
        info.versionCode = apex.version;
        return info;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ CompressedApexInfo[] lambda$getCompressedApexInfoList$2(int x$0) {
        return new CompressedApexInfo[x$0];
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [987=4] */
    public boolean allocateSpaceForUpdate(String packageFile) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
        if (!isUpdatableApexSupported()) {
            Log.i(TAG, "Updatable Apex not supported, allocateSpaceForUpdate does nothing.");
            return true;
        }
        long token = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    CompressedApexInfoList apexInfoList = getCompressedApexInfoList(packageFile);
                    if (apexInfoList == null) {
                        Log.i(TAG, "apex_info.pb not present in OTA package. Assuming device doesn't support compressedAPEX, continueing without allocating space.");
                        return true;
                    }
                    ApexManager apexManager = ApexManager.getInstance();
                    apexManager.reserveSpaceForCompressedApex(apexInfoList);
                    return true;
                } catch (RemoteException e) {
                    e.rethrowAsRuntimeException();
                    Binder.restoreCallingIdentity(token);
                    return false;
                }
            } catch (IOException | UnsupportedOperationException e2) {
                Slog.e(TAG, "Failed to reserve space for compressed apex: ", e2);
                Binder.restoreCallingIdentity(token);
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean isLskfCaptured(String packageName) {
        boolean captured;
        enforcePermissionForResumeOnReboot();
        synchronized (this) {
            captured = this.mCallerPreparedForReboot.contains(packageName);
        }
        if (!captured) {
            Slog.i(TAG, "Reboot requested before prepare completed for caller " + packageName);
            return false;
        }
        return true;
    }

    private boolean checkAndWaitForUncryptService() {
        int retry = 0;
        while (true) {
            boolean busy = false;
            if (retry >= 30) {
                return false;
            }
            String uncryptService = this.mInjector.systemPropertiesGet(INIT_SERVICE_UNCRYPT);
            String setupBcbService = this.mInjector.systemPropertiesGet(INIT_SERVICE_SETUP_BCB);
            String clearBcbService = this.mInjector.systemPropertiesGet(INIT_SERVICE_CLEAR_BCB);
            if (INetd.IF_FLAG_RUNNING.equals(uncryptService) || INetd.IF_FLAG_RUNNING.equals(setupBcbService) || INetd.IF_FLAG_RUNNING.equals(clearBcbService)) {
                busy = true;
            }
            if (!busy) {
                return true;
            }
            try {
                this.mInjector.threadSleep(1000L);
            } catch (InterruptedException e) {
                Slog.w(TAG, "Interrupted:", e);
            }
            retry++;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1089=5] */
    private boolean setupOrClearBcb(boolean isSetup, String command) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
        boolean available = checkAndWaitForUncryptService();
        if (!available) {
            Slog.e(TAG, "uncrypt service is unavailable.");
            return false;
        }
        if (isSetup) {
            this.mInjector.systemPropertiesSet("ctl.start", "setup-bcb");
        } else {
            this.mInjector.systemPropertiesSet("ctl.start", "clear-bcb");
        }
        UncryptSocket socket = this.mInjector.connectService();
        if (socket == null) {
            Slog.e(TAG, "Failed to connect to uncrypt socket");
            return false;
        }
        try {
            if (isSetup) {
                socket.sendCommand(command);
            }
            int status = socket.getPercentageUncrypted();
            socket.sendAck();
            if (status != 100) {
                Slog.e(TAG, "uncrypt failed with status: " + status);
                return false;
            }
            Slog.i(TAG, "uncrypt " + (isSetup ? "setup" : "clear") + " bcb successfully finished.");
            socket.close();
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "IOException when communicating with uncrypt:", e);
            return false;
        } finally {
            socket.close();
        }
    }

    /* loaded from: classes2.dex */
    public static class UncryptSocket {
        private DataInputStream mInputStream;
        private LocalSocket mLocalSocket;
        private DataOutputStream mOutputStream;

        public boolean connectService() {
            this.mLocalSocket = new LocalSocket();
            boolean done = false;
            int retry = 0;
            while (true) {
                if (retry >= 30) {
                    break;
                }
                try {
                    this.mLocalSocket.connect(new LocalSocketAddress(RecoverySystemService.UNCRYPT_SOCKET, LocalSocketAddress.Namespace.RESERVED));
                    done = true;
                    break;
                } catch (IOException e) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e2) {
                        Slog.w(RecoverySystemService.TAG, "Interrupted:", e2);
                    }
                    retry++;
                }
            }
            if (!done) {
                Slog.e(RecoverySystemService.TAG, "Timed out connecting to uncrypt socket");
                close();
                return false;
            }
            try {
                this.mInputStream = new DataInputStream(this.mLocalSocket.getInputStream());
                this.mOutputStream = new DataOutputStream(this.mLocalSocket.getOutputStream());
                return true;
            } catch (IOException e3) {
                close();
                return false;
            }
        }

        public void sendCommand(String command) throws IOException {
            byte[] cmdUtf8 = command.getBytes(StandardCharsets.UTF_8);
            this.mOutputStream.writeInt(cmdUtf8.length);
            this.mOutputStream.write(cmdUtf8, 0, cmdUtf8.length);
        }

        public int getPercentageUncrypted() throws IOException {
            return this.mInputStream.readInt();
        }

        public void sendAck() throws IOException {
            this.mOutputStream.writeInt(0);
        }

        public void close() {
            IoUtils.closeQuietly(this.mInputStream);
            IoUtils.closeQuietly(this.mOutputStream);
            IoUtils.closeQuietly(this.mLocalSocket);
        }
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

    /* JADX DEBUG: Multi-variable search result rejected for r12v0, resolved type: com.android.server.recoverysystem.RecoverySystemService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        enforceShell();
        long origId = Binder.clearCallingIdentity();
        try {
        } catch (Throwable th) {
            th = th;
        }
        try {
            new RecoverySystemShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
            Binder.restoreCallingIdentity(origId);
        } catch (Throwable th2) {
            th = th2;
            Binder.restoreCallingIdentity(origId);
            throw th;
        }
    }
}
