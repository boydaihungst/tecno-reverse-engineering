package android.net;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.util.SharedLog;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.util.ArraySet;
import android.util.Slog;
import java.io.File;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class ConnectivityModuleConnector {
    private static final String CONFIG_ALWAYS_RATELIMIT_NETWORKSTACK_CRASH = "always_ratelimit_networkstack_crash";
    private static final String CONFIG_MIN_CRASH_INTERVAL_MS = "min_crash_interval";
    private static final String CONFIG_MIN_UPTIME_BEFORE_CRASH_MS = "min_uptime_before_crash";
    private static final long DEFAULT_MIN_CRASH_INTERVAL_MS = 21600000;
    private static final long DEFAULT_MIN_UPTIME_BEFORE_CRASH_MS = 1800000;
    private static final String IN_PROCESS_SUFFIX = ".InProcess";
    private static final String PREFS_FILE = "ConnectivityModuleConnector.xml";
    private static final String PREF_KEY_LAST_CRASH_TIME = "lastcrash_time";
    private static final String TAG = ConnectivityModuleConnector.class.getSimpleName();
    private static ConnectivityModuleConnector sInstance;
    private Context mContext;
    private final Dependencies mDeps;
    private final ArraySet<ConnectivityModuleHealthListener> mHealthListeners;
    private final SharedLog mLog;

    /* loaded from: classes.dex */
    public interface ConnectivityModuleHealthListener {
        void onNetworkStackFailure(String str);
    }

    /* loaded from: classes.dex */
    protected interface Dependencies {
        Intent getModuleServiceIntent(PackageManager packageManager, String str, String str2, boolean z);
    }

    /* loaded from: classes.dex */
    public interface ModuleServiceCallback {
        void onModuleServiceConnected(IBinder iBinder);
    }

    private ConnectivityModuleConnector() {
        this(new DependenciesImpl());
    }

    ConnectivityModuleConnector(Dependencies deps) {
        this.mLog = new SharedLog(TAG);
        this.mHealthListeners = new ArraySet<>();
        this.mDeps = deps;
    }

    public static synchronized ConnectivityModuleConnector getInstance() {
        ConnectivityModuleConnector connectivityModuleConnector;
        synchronized (ConnectivityModuleConnector.class) {
            if (sInstance == null) {
                sInstance = new ConnectivityModuleConnector();
            }
            connectivityModuleConnector = sInstance;
        }
        return connectivityModuleConnector;
    }

    public void init(Context context) {
        log("Network stack init");
        this.mContext = context;
    }

    /* loaded from: classes.dex */
    private static class DependenciesImpl implements Dependencies {
        private DependenciesImpl() {
        }

        @Override // android.net.ConnectivityModuleConnector.Dependencies
        public Intent getModuleServiceIntent(PackageManager pm, String serviceIntentBaseAction, String servicePermissionName, boolean inSystemProcess) {
            String str;
            if (inSystemProcess) {
                str = serviceIntentBaseAction + ConnectivityModuleConnector.IN_PROCESS_SUFFIX;
            } else {
                str = serviceIntentBaseAction;
            }
            Intent intent = new Intent(str);
            ComponentName comp = intent.resolveSystemService(pm, 0);
            if (comp == null) {
                return null;
            }
            intent.setComponent(comp);
            try {
                int uid = pm.getPackageUidAsUser(comp.getPackageName(), 0);
                int expectedUid = inSystemProcess ? 1000 : 1073;
                if (uid != expectedUid) {
                    throw new SecurityException("Invalid network stack UID: " + uid);
                }
                if (!inSystemProcess) {
                    ConnectivityModuleConnector.checkModuleServicePermission(pm, comp, servicePermissionName);
                }
                return intent;
            } catch (PackageManager.NameNotFoundException e) {
                throw new SecurityException("Could not check network stack UID; package not found.", e);
            }
        }
    }

    public void registerHealthListener(ConnectivityModuleHealthListener listener) {
        synchronized (this.mHealthListeners) {
            this.mHealthListeners.add(listener);
        }
    }

    public void startModuleService(String serviceIntentBaseAction, String servicePermissionName, ModuleServiceCallback callback) {
        log("Starting networking module " + serviceIntentBaseAction);
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = this.mDeps.getModuleServiceIntent(pm, serviceIntentBaseAction, servicePermissionName, true);
        if (intent == null) {
            intent = this.mDeps.getModuleServiceIntent(pm, serviceIntentBaseAction, servicePermissionName, false);
            log("Starting networking module in network_stack process");
        } else {
            log("Starting networking module in system_server process");
        }
        if (intent == null) {
            maybeCrashWithTerribleFailure("Could not resolve the networking module", null);
            return;
        }
        String packageName = intent.getComponent().getPackageName();
        if (!this.mContext.bindServiceAsUser(intent, new ModuleServiceConnection(packageName, callback), 65, UserHandle.SYSTEM)) {
            maybeCrashWithTerribleFailure("Could not bind to networking module in-process, or in app with " + intent, packageName);
        } else {
            log("Networking module service start requested");
        }
    }

    /* loaded from: classes.dex */
    private class ModuleServiceConnection implements ServiceConnection {
        private final ModuleServiceCallback mModuleServiceCallback;
        private final String mPackageName;

        private ModuleServiceConnection(String packageName, ModuleServiceCallback moduleCallback) {
            this.mPackageName = packageName;
            this.mModuleServiceCallback = moduleCallback;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            ConnectivityModuleConnector.this.logi("Networking module service connected");
            this.mModuleServiceCallback.onModuleServiceConnected(service);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            ConnectivityModuleConnector.this.maybeCrashWithTerribleFailure("Lost network stack. This is not the root cause of any issue, it is a side effect of a crash that happened earlier. Earlier logs should point to the actual issue.", this.mPackageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void checkModuleServicePermission(PackageManager pm, ComponentName comp, String servicePermissionName) {
        int hasPermission = pm.checkPermission(servicePermissionName, comp.getPackageName());
        if (hasPermission != 0) {
            throw new SecurityException("Networking module does not have permission " + servicePermissionName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x007c, code lost:
        r4 = r23.mHealthListeners;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x0080, code lost:
        monitor-enter(r4);
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x0081, code lost:
        r0 = new android.util.ArraySet<>(r23.mHealthListeners);
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x0088, code lost:
        monitor-exit(r4);
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x0089, code lost:
        r4 = r0.iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x0091, code lost:
        if (r4.hasNext() == false) goto L39;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x0093, code lost:
        r5 = r4.next();
        r5.onNetworkStackFailure(r25);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized void maybeCrashWithTerribleFailure(String message, String packageName) {
        logWtf(message, null);
        long uptime = SystemClock.elapsedRealtime();
        long now = System.currentTimeMillis();
        long minCrashIntervalMs = DeviceConfig.getLong("connectivity", CONFIG_MIN_CRASH_INTERVAL_MS, (long) DEFAULT_MIN_CRASH_INTERVAL_MS);
        long minUptimeBeforeCrash = DeviceConfig.getLong("connectivity", CONFIG_MIN_UPTIME_BEFORE_CRASH_MS, 1800000L);
        boolean haveKnownRecentCrash = false;
        boolean alwaysRatelimit = DeviceConfig.getBoolean("connectivity", CONFIG_ALWAYS_RATELIMIT_NETWORKSTACK_CRASH, false);
        SharedPreferences prefs = getSharedPreferences();
        long lastCrashTime = tryGetLastCrashTime(prefs);
        boolean alwaysCrash = Build.IS_DEBUGGABLE && !alwaysRatelimit;
        boolean justBooted = uptime < minUptimeBeforeCrash;
        boolean haveLastCrashTime = lastCrashTime != 0 && lastCrashTime < now;
        if (haveLastCrashTime && now < lastCrashTime + minCrashIntervalMs) {
            haveKnownRecentCrash = true;
        }
        if (!alwaysCrash) {
            if (!justBooted && !haveKnownRecentCrash) {
            }
        }
        tryWriteLastCrashTime(prefs, now);
        throw new IllegalStateException(message);
    }

    private SharedPreferences getSharedPreferences() {
        try {
            File prefsFile = new File(Environment.getDataSystemDeDirectory(0), PREFS_FILE);
            return this.mContext.createDeviceProtectedStorageContext().getSharedPreferences(prefsFile, 0);
        } catch (Throwable e) {
            logWtf("Error loading shared preferences", e);
            return null;
        }
    }

    private long tryGetLastCrashTime(SharedPreferences prefs) {
        if (prefs == null) {
            return 0L;
        }
        try {
            return prefs.getLong(PREF_KEY_LAST_CRASH_TIME, 0L);
        } catch (Throwable e) {
            logWtf("Error getting last crash time", e);
            return 0L;
        }
    }

    private void tryWriteLastCrashTime(SharedPreferences prefs, long value) {
        if (prefs == null) {
            return;
        }
        try {
            prefs.edit().putLong(PREF_KEY_LAST_CRASH_TIME, value).commit();
        } catch (Throwable e) {
            logWtf("Error writing last crash time", e);
        }
    }

    private void log(String message) {
        Slog.d(TAG, message);
        synchronized (this.mLog) {
            this.mLog.log(message);
        }
    }

    private void logWtf(String message, Throwable e) {
        Slog.wtf(TAG, message, e);
        synchronized (this.mLog) {
            this.mLog.e(message);
        }
    }

    private void loge(String message, Throwable e) {
        Slog.e(TAG, message, e);
        synchronized (this.mLog) {
            this.mLog.e(message);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logi(String message) {
        Slog.i(TAG, message);
        synchronized (this.mLog) {
            this.mLog.i(message);
        }
    }

    public void dump(PrintWriter pw) {
        this.mLog.dump(null, pw, null);
    }
}
