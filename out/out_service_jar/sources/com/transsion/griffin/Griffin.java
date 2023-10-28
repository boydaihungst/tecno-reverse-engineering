package com.transsion.griffin;

import android.content.Context;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Singleton;
import android.util.Slog;
import com.transsion.griffin.FeatureSwitch;
import com.transsion.griffin.lib.monitor.Hooker;
import com.transsion.griffin.lib.pm.ProcessManager;
import com.transsion.griffin.lib.provider.ConfigProvider;
import com.transsion.griffin.lib.provider.StateProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
/* loaded from: classes2.dex */
public final class Griffin {
    public static boolean APM_SUPPORT = false;
    public static final int APP_TYPE_3RD = 4;
    public static final int APP_TYPE_GMS = 2;
    public static final int APP_TYPE_ISOLATED = 8;
    public static final int APP_TYPE_SYS = 1;
    public static final int BIND_SERVICE = 2;
    public static final int CURT_APP = 3;
    public static final int CURT_HOME = 1;
    public static final int CURT_PERMISSION = 4;
    public static final int CURT_RECENT = 2;
    public static boolean DEBUG = SystemProperties.getBoolean("ro.griffin.debug_sys", false);
    public static final String FEARURE_CORE = "griffin_core";
    public static final String FEATURE_POLICY = "griffin_policy";
    public static final String GMS = "gms";
    public static boolean GRIFFIN_KEEPALIVE_SUPPORT = false;
    public static final int INVAILD = 0;
    public static final int KILLED_BY_GRIFFIN = 1;
    public static final int KILLED_BY_SYS = 3;
    public static final int KILLED_BY_USER = 2;
    public static final int RESTART_SERVICE = 3;
    public static final int START_SERVICE = 1;
    public static boolean SUPPORT = false;
    public static final String SYS = "sys";
    private static final String TAG = "Griffin_old";
    public static final String THIRD = "3rd";
    public static final int UID_ACTIVE = 2;
    public static final int UID_IDLE = 3;
    public static final int UID_RUNNING = 1;
    public static final int UID_STOPPED = 4;
    private static final Singleton<Griffin> gDefault;
    private ConfigProvider mConfigProvider;
    private Context mContext;
    private Hooker mHooker;
    private boolean mInitialized;
    private ProcessManager mProcessManager;
    private StateProvider mStateProvider;
    private boolean mSwitching;
    private boolean mWorking;

    static {
        boolean z = false;
        SUPPORT = 1 == SystemProperties.getInt("ro.griffin.support", 0);
        APM_SUPPORT = SystemProperties.getBoolean("ro.product.apm", false);
        if (1 == SystemProperties.getInt("ro.product.keepalive_support", 0) && SUPPORT) {
            z = true;
        }
        GRIFFIN_KEEPALIVE_SUPPORT = z;
        gDefault = new Singleton<Griffin>() { // from class: com.transsion.griffin.Griffin.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX INFO: Access modifiers changed from: protected */
            /* renamed from: create */
            public Griffin m8559create() {
                return new Griffin();
            }
        };
    }

    private Griffin() {
        this.mInitialized = false;
        this.mSwitching = false;
        this.mWorking = false;
    }

    public static ConfigProvider getConfigProvider() {
        if (get().mInitialized) {
            return get().mConfigProvider;
        }
        Slog.w(TAG, "Not initialized when getConfigProvider()");
        return new ConfigProvider();
    }

    public static StateProvider getStateProvider() {
        if (get().mInitialized) {
            return get().mStateProvider;
        }
        Slog.w(TAG, "Not initialized when getStateProvider()");
        return new StateProvider();
    }

    public static Hooker getHooker() {
        if (get().mInitialized) {
            return get().mHooker;
        }
        Slog.w(TAG, "Not initialized when getHooker()");
        return new Hooker();
    }

    public static ProcessManager getPM() {
        if (get().mInitialized) {
            return get().mProcessManager;
        }
        Slog.w(TAG, "Not initialized when getPM()");
        return new ProcessManager();
    }

    public static Griffin get() {
        Slog.d("yvonne", "Griffin.get");
        try {
            throw new NullPointerException("used old Griffin");
        } catch (Exception e) {
            saveExc(e);
            e.printStackTrace();
            return (Griffin) gDefault.get();
        }
    }

    private static void saveExc(final Throwable e) {
        new Thread(new Runnable() { // from class: com.transsion.griffin.Griffin.2
            /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [193=4, 190=4] */
            @Override // java.lang.Runnable
            public void run() {
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    try {
                        try {
                            File file = new File("/data/performance/griffin/thub_error.txt");
                            if (!file.exists()) {
                                if (!file.getParentFile().exists()) {
                                    file.getParentFile().mkdirs();
                                }
                                file.createNewFile();
                            }
                            FileInputStream fis2 = new FileInputStream("/data/performance/griffin/thub_error.txt");
                            if (fis2.available() > 1410065408) {
                                try {
                                    fis2.close();
                                    if (0 != 0) {
                                        fos.close();
                                        return;
                                    }
                                    return;
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                    return;
                                }
                            }
                            fis2.close();
                            fis = null;
                            String excpInfo = Griffin.getExcpInfo(e);
                            fos = new FileOutputStream("/data/performance/griffin/thub_error.txt", true);
                            byte[] bytes = excpInfo.getBytes();
                            fos.write(bytes);
                            fos.flush();
                            if (0 != 0) {
                                fis.close();
                            }
                            fos.close();
                        } catch (Exception e3) {
                            e3.printStackTrace();
                            if (fis != null) {
                                fis.close();
                            }
                            if (fos != null) {
                                fos.close();
                            }
                        }
                    } catch (Exception e4) {
                        e4.printStackTrace();
                    }
                } catch (Throwable th) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Exception e5) {
                            e5.printStackTrace();
                            throw th;
                        }
                    }
                    if (fos != null) {
                        fos.close();
                    }
                    throw th;
                }
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getExcpInfo(Throwable e) {
        if (e == null) {
            return " \r\n";
        }
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        StringBuffer buffer = stringWriter.getBuffer();
        printWriter.close();
        return buffer.toString();
    }

    public Griffin initialize(Context context) {
        if (!this.mInitialized) {
            Slog.d(TAG, "initialize");
            this.mContext = context;
            loadFeature();
            this.mInitialized = true;
        } else {
            Slog.d(TAG, "already initialized");
        }
        return this;
    }

    public void supportGriffin(PrintWriter pw, boolean support) {
        if (!support) {
            pw.println("Close ProcessManager feature!");
            SUPPORT = false;
            this.mInitialized = false;
            stopWork();
            return;
        }
        pw.println("Need reboot device to support ProcessManager feature!");
    }

    public void restart() {
        if (this.mSwitching) {
            Slog.d(TAG, "already restarting");
            return;
        }
        long start = SystemClock.elapsedRealtime();
        this.mSwitching = true;
        stopWork();
        loadFeature();
        startWork();
        this.mSwitching = false;
        Slog.d(TAG, "Griffin restart coast " + (SystemClock.elapsedRealtime() - start) + "ms");
    }

    public void stopWork() {
        if (!this.mWorking) {
            Slog.d(TAG, "already stop work");
            return;
        }
        Slog.d(TAG, "stopWork begin");
        Hooker hooker = this.mHooker;
        if (hooker != null) {
            hooker.stopWork();
            this.mHooker = null;
        }
        ProcessManager processManager = this.mProcessManager;
        if (processManager != null) {
            processManager.stopWork();
            this.mProcessManager = null;
        }
        StateProvider stateProvider = this.mStateProvider;
        if (stateProvider != null) {
            stateProvider.stopWork();
            this.mStateProvider = null;
        }
        ConfigProvider configProvider = this.mConfigProvider;
        if (configProvider != null) {
            configProvider.stopWork();
            this.mConfigProvider = null;
        }
        this.mWorking = false;
        Slog.d(TAG, "stopWork finish");
    }

    public void startWork() {
        if (this.mWorking) {
            Slog.d(TAG, "already start work");
            return;
        }
        Slog.d(TAG, "startWork begin");
        ConfigProvider configProvider = this.mConfigProvider;
        if (configProvider != null) {
            configProvider.startWork();
        }
        StateProvider stateProvider = this.mStateProvider;
        if (stateProvider != null) {
            stateProvider.startWork();
        }
        Hooker hooker = this.mHooker;
        if (hooker != null) {
            hooker.startWork();
        }
        ProcessManager processManager = this.mProcessManager;
        if (processManager != null) {
            processManager.startWork();
        }
        this.mWorking = true;
        Slog.d(TAG, "startWork finish");
    }

    public void afterSystemReady() {
        Slog.d(TAG, "afterSystemReady");
        ConfigProvider configProvider = this.mConfigProvider;
        if (configProvider != null) {
            configProvider.afterSystemReady();
        }
        Hooker hooker = this.mHooker;
        if (hooker != null) {
            hooker.afterSystemReady();
        }
        ProcessManager processManager = this.mProcessManager;
        if (processManager != null) {
            processManager.afterSystemReady();
        }
    }

    public void onBootCompleted() {
        Slog.d(TAG, "onBootCompleted");
        ConfigProvider configProvider = this.mConfigProvider;
        if (configProvider != null) {
            configProvider.onBootCompleted();
        }
        Hooker hooker = this.mHooker;
        if (hooker != null) {
            hooker.onBootCompleted();
        }
        ProcessManager processManager = this.mProcessManager;
        if (processManager != null) {
            processManager.onBootCompleted();
        }
    }

    private void loadFeature() {
        loadGriffinCore();
        loadGriffinPolicy();
    }

    private void loadGriffinPolicy() {
        Slog.d(TAG, "loadGriffinPolicy start");
        if (FeatureSwitch.DEFAULT_GRIFFIN_PM_SUPPORT) {
            Slog.e(TAG, "loadGriffinPolicy failed");
            this.mProcessManager = new ProcessManager();
        } else {
            this.mProcessManager = new ProcessManager();
        }
        Slog.d(TAG, "loadGriffinPolicy end");
    }

    private void loadGriffinCore() {
        Slog.d(TAG, "initGriffinCore start");
        if (FeatureSwitch.DEFAULT_GRIFFIN_CORE_SUPPORT) {
            Slog.e(TAG, "initGriffinCore failed");
            this.mConfigProvider = new ConfigProvider();
            this.mStateProvider = new StateProvider();
            this.mHooker = new Hooker();
        } else {
            this.mConfigProvider = new ConfigProvider();
            this.mStateProvider = new StateProvider();
            this.mHooker = new Hooker();
        }
        Slog.d(TAG, "initGriffinCore end");
    }

    public void startFeature(String feature, String function, FeatureSwitch.SwitchListener listener) {
        if (TextUtils.isEmpty(feature) || TextUtils.isEmpty(function)) {
            return;
        }
        char c = 65535;
        switch (feature.hashCode()) {
            case -2097582437:
                if (feature.equals(FEARURE_CORE)) {
                    c = 0;
                    break;
                }
                break;
            case -1065052882:
                if (feature.equals(FEATURE_POLICY)) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case 1:
                return;
            default:
                Slog.w(TAG, "not found feature " + feature);
                return;
        }
    }

    public void stopFeature(String feature, String function, FeatureSwitch.SwitchListener listener) {
        if (TextUtils.isEmpty(feature) || TextUtils.isEmpty(function)) {
            return;
        }
        char c = 65535;
        switch (feature.hashCode()) {
            case -2097582437:
                if (feature.equals(FEARURE_CORE)) {
                    c = 0;
                    break;
                }
                break;
            case -1065052882:
                if (feature.equals(FEATURE_POLICY)) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case 1:
                return;
            default:
                Slog.w(TAG, "not found feature " + feature);
                return;
        }
    }

    public void doCommand(PrintWriter pw, String[] args) {
        if (!this.mWorking) {
            pw.println("Griffin working wrong!");
            return;
        }
        boolean result = false;
        Hooker hooker = this.mHooker;
        if (hooker != null) {
            result = hooker.doCommand(pw, args);
        }
        ProcessManager processManager = this.mProcessManager;
        if (processManager != null) {
            result = processManager.doCommand(pw, args) || result;
        }
        if (result) {
            pw.println("Griffin do command success!");
        } else {
            pw.println("Griffin do command failed!");
        }
    }

    public void doCommand(PrintWriter pw, String[] args, int opti) {
        if (!this.mWorking) {
            pw.println("Griffin working wrong!");
            return;
        }
        boolean z = false;
        if (opti < args.length && opti + 1 < args.length) {
            String key = args[opti];
            if ("support".equals(key)) {
                try {
                    int value = Integer.parseInt(args[opti + 1]);
                    boolean support = value != 0;
                    supportGriffin(pw, support);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        boolean result = false;
        Hooker hooker = this.mHooker;
        if (hooker != null) {
            result = hooker.doCommand(pw, args, opti);
        }
        ProcessManager processManager = this.mProcessManager;
        if (processManager != null) {
            if (processManager.doCommand(pw, args, opti) || result) {
                z = true;
            }
            result = z;
        }
        if (result) {
            pw.println("Griffin do command success!");
        } else {
            pw.println("Griffin do command failed!");
        }
    }

    public void getVersion(PrintWriter pw, String[] args, int opti) {
        if (!this.mWorking) {
            pw.println("Griffin working wrong!");
            return;
        }
        Hooker hooker = this.mHooker;
        if (hooker != null) {
            String core_version = hooker.getVersion();
            pw.println("Griffin-core version: " + core_version);
        }
        ProcessManager processManager = this.mProcessManager;
        if (processManager != null) {
            String policy_version = processManager.getVersion();
            pw.println("Griffin-policy version: " + policy_version);
        }
    }
}
