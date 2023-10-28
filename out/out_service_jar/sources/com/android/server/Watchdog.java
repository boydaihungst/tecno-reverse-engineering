package com.android.server;

import android.app.ActivityManager;
import android.app.IActivityController;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.health.V2_0.IHealth;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.hidl.manager.V1_0.IServiceManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IPowerManager;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceDebugInfo;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.sysprop.WatchdogProperties;
import android.util.Dumpable;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.MemInfoReader;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.TraceErrorLogger;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.criticalevents.CriticalEventLog;
import com.android.server.wm.SurfaceAnimationThread;
import com.mediatek.aee.ExceptionLog;
import com.transsion.tne.TNEService;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class Watchdog implements Dumpable {
    private static final int COMPLETED = 0;
    private static final boolean DB = false;
    public static final boolean DEBUG = false;
    private static final long DEFAULT_TIMEOUT = 60000;
    private static final int OVERDUE = 3;
    private static final String PROP_FATAL_LOOP_COUNT = "framework_watchdog.fatal_count";
    private static final String PROP_FATAL_LOOP_WINDOWS_SECS = "framework_watchdog.fatal_window.second";
    static final String TAG = "Watchdog";
    private static final String TIMEOUT_HISTORY_FILE = "/data/system/watchdog-timeout-history.txt";
    private static final int WAITED_HALF = 2;
    private static final int WAITING = 1;
    private static Watchdog sWatchdog;
    ExceptionLog exceptionHang;
    private ActivityManagerService mActivity;
    private boolean mAllowRestart;
    private HandlerThread mBinderMonitorThread;
    private IActivityController mController;
    private final ArrayList<HandlerCheckerAndTimeout> mHandlerCheckers;
    private final List<Integer> mInterestingJavaPids;
    private final HandlerChecker mMonitorChecker;
    private boolean mSfHang;
    private final Thread mThread;
    private final TraceErrorLogger mTraceErrorLogger;
    private volatile long mWatchdogTimeoutMillis;
    static boolean DEBUG_CONTROLLER = SystemProperties.getBoolean("persist.sys.watchdog.control", false);
    public static final String[] NATIVE_STACKS_OF_INTEREST = {"/system/bin/audioserver", "/system/bin/cameraserver", "/system/bin/drmserver", "/system/bin/keystore2", "/system/bin/mediadrmserver", "/system/bin/mediaserver", "/system/bin/netd", "/system/bin/sdcard", "/system/bin/surfaceflinger", "/system/bin/vold", "media.extractor", "media.metrics", "media.codec", "media.swcodec", "media.transcoding", "com.android.bluetooth", "/apex/com.android.os.statsd/bin/statsd"};
    public static final List<String> HAL_INTERFACES_OF_INTEREST = Arrays.asList("android.hardware.audio@4.0::IDevicesFactory", "android.hardware.audio@5.0::IDevicesFactory", "android.hardware.audio@6.0::IDevicesFactory", "android.hardware.audio@7.0::IDevicesFactory", IBiometricsFace.kInterfaceName, IBiometricsFingerprint.kInterfaceName, "android.hardware.bluetooth@1.0::IBluetoothHci", "android.hardware.camera.provider@2.4::ICameraProvider", "android.hardware.gnss@1.0::IGnss", "android.hardware.graphics.allocator@2.0::IAllocator", "android.hardware.graphics.composer@2.1::IComposer", IHealth.kInterfaceName, "android.hardware.light@2.0::ILight", "android.hardware.media.c2@1.0::IComponentStore", "android.hardware.media.omx@1.0::IOmx", "android.hardware.media.omx@1.0::IOmxStore", "android.hardware.neuralnetworks@1.0::IDevice", "android.hardware.power@1.0::IPower", "android.hardware.power.stats@1.0::IPowerStats", "android.hardware.sensors@1.0::ISensors", "android.hardware.sensors@2.0::ISensors", "android.hardware.sensors@2.1::ISensors", "android.hardware.vibrator@1.0::IVibrator", "android.hardware.vr@1.0::IVr", "android.system.suspend@1.0::ISystemSuspend");
    public static final String[] AIDL_INTERFACE_PREFIXES_OF_INTEREST = {"android.hardware.biometrics.face.IFace/", "android.hardware.biometrics.fingerprint.IFingerprint/", "android.hardware.light.ILights/", "android.hardware.power.IPower/", "android.hardware.power.stats.IPowerStats/", "android.hardware.vibrator.IVibrator/", "android.hardware.vibrator.IVibratorManager/"};
    private final int TIME_SF_WAIT = 20000;
    public long mLastDumpHeapTime = 0;
    public final long DUMP_HEAP_TIME_INTERVAL = 1800000;
    private final float TOTAL_MEM_LIMIT_LEVEL = 0.15f;
    private final float NATIVE_MEM_LIMIT_LEVEL = 0.1f;
    public boolean mIsGraphicsGc = false;
    public final long GRAPHICS_MAX_SIZE = 1048576;
    private final Object mLock = new Object();

    /* loaded from: classes.dex */
    public interface Monitor {
        void monitor();
    }

    private long getSfStatus() {
        ExceptionLog exceptionLog = this.exceptionHang;
        if (exceptionLog != null) {
            return exceptionLog.SFMatterJava(0L, 0L);
        }
        return 0L;
    }

    private static int getSfReboot() {
        return SystemProperties.getInt("service.sf.reboot", 0);
    }

    private static void setSfReboot() {
        int oldTime = SystemProperties.getInt("service.sf.reboot", 0) + 1;
        if (oldTime > 9) {
            oldTime = 9;
        }
        SystemProperties.set("service.sf.reboot", String.valueOf(oldTime));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class HandlerCheckerAndTimeout {
        private final Optional<Long> mCustomTimeoutMillis;
        private final HandlerChecker mHandler;

        private HandlerCheckerAndTimeout(HandlerChecker checker, Optional<Long> timeoutMillis) {
            this.mHandler = checker;
            this.mCustomTimeoutMillis = timeoutMillis;
        }

        HandlerChecker checker() {
            return this.mHandler;
        }

        Optional<Long> customTimeoutMillis() {
            return this.mCustomTimeoutMillis;
        }

        static HandlerCheckerAndTimeout withDefaultTimeout(HandlerChecker checker) {
            return new HandlerCheckerAndTimeout(checker, Optional.empty());
        }

        static HandlerCheckerAndTimeout withCustomTimeout(HandlerChecker checker, long timeoutMillis) {
            return new HandlerCheckerAndTimeout(checker, Optional.of(Long.valueOf(timeoutMillis)));
        }
    }

    /* loaded from: classes.dex */
    public final class HandlerChecker implements Runnable {
        private Monitor mCurrentMonitor;
        private final Handler mHandler;
        private final String mName;
        private int mPauseCount;
        private long mStartTime;
        private long mWaitMax;
        private final ArrayList<Monitor> mMonitors = new ArrayList<>();
        private final ArrayList<Monitor> mMonitorQueue = new ArrayList<>();
        private boolean mCompleted = true;

        HandlerChecker(Handler handler, String name) {
            this.mHandler = handler;
            this.mName = name;
        }

        void addMonitorLocked(Monitor monitor) {
            this.mMonitorQueue.add(monitor);
        }

        public void scheduleCheckLocked(long handlerCheckerTimeoutMillis) {
            this.mWaitMax = handlerCheckerTimeoutMillis;
            if (this.mCompleted) {
                this.mMonitors.addAll(this.mMonitorQueue);
                this.mMonitorQueue.clear();
            }
            if ((this.mMonitors.size() == 0 && this.mHandler.getLooper().getQueue().isPolling()) || this.mPauseCount > 0) {
                this.mCompleted = true;
            } else if (!this.mCompleted) {
            } else {
                this.mCompleted = false;
                this.mCurrentMonitor = null;
                this.mStartTime = SystemClock.uptimeMillis();
                this.mHandler.postAtFrontOfQueue(this);
            }
        }

        public int getCompletionStateLocked() {
            if (this.mCompleted) {
                return 0;
            }
            long latency = SystemClock.uptimeMillis() - this.mStartTime;
            long j = this.mWaitMax;
            if (latency < j / 2) {
                return 1;
            }
            if (latency < j) {
                return 2;
            }
            return 3;
        }

        public Thread getThread() {
            return this.mHandler.getLooper().getThread();
        }

        public String getName() {
            return this.mName;
        }

        String describeBlockedStateLocked() {
            if (this.mCurrentMonitor == null) {
                return "Blocked in handler on " + this.mName + " (" + getThread().getName() + ")";
            }
            return "Blocked in monitor " + this.mCurrentMonitor.getClass().getName() + " on " + this.mName + " (" + getThread().getName() + ")";
        }

        @Override // java.lang.Runnable
        public void run() {
            Monitor monitor;
            int size = this.mMonitors.size();
            for (int i = 0; i < size; i++) {
                synchronized (Watchdog.this.mLock) {
                    monitor = this.mMonitors.get(i);
                    this.mCurrentMonitor = monitor;
                }
                monitor.monitor();
            }
            synchronized (Watchdog.this.mLock) {
                this.mCompleted = true;
                this.mCurrentMonitor = null;
            }
        }

        public void pauseLocked(String reason) {
            this.mPauseCount++;
            this.mCompleted = true;
            Slog.i(Watchdog.TAG, "Pausing HandlerChecker: " + this.mName + " for reason: " + reason + ". Pause count: " + this.mPauseCount);
        }

        public void resumeLocked(String reason) {
            int i = this.mPauseCount;
            if (i > 0) {
                this.mPauseCount = i - 1;
                Slog.i(Watchdog.TAG, "Resuming HandlerChecker: " + this.mName + " for reason: " + reason + ". Pause count: " + this.mPauseCount);
                return;
            }
            Slog.wtf(Watchdog.TAG, "Already resumed HandlerChecker: " + this.mName);
        }
    }

    /* loaded from: classes.dex */
    final class RebootRequestReceiver extends BroadcastReceiver {
        RebootRequestReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context c, Intent intent) {
            if (intent.getIntExtra("nowait", 0) != 0) {
                Watchdog.this.rebootSystem("Received ACTION_REBOOT broadcast");
            } else {
                Slog.w(Watchdog.TAG, "Unsupported ACTION_REBOOT broadcast: " + intent);
            }
        }
    }

    /* loaded from: classes.dex */
    private static final class BinderThreadMonitor implements Monitor {
        private BinderThreadMonitor() {
        }

        @Override // com.android.server.Watchdog.Monitor
        public void monitor() {
            Binder.blockUntilThreadAvailable();
        }
    }

    public static Watchdog getInstance() {
        if (sWatchdog == null) {
            sWatchdog = new Watchdog();
        }
        return sWatchdog;
    }

    public void dumpHprofWhenOOM() {
        String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(System.currentTimeMillis()));
        String path = String.format("/data/hprof/SYSTEM_OOM_%d_%s.hprof", Integer.valueOf(Process.myPid()), date);
        Slog.d(TAG, "dump hprof when system_server OOM begin in dumpHprofWhenOOM.");
        try {
            Debug.dumpHprofData(path);
        } catch (IOException e) {
            Slog.w(TAG, "dump hprof when system_server OOM fail in dumpHprofWhenOOM.");
        } catch (RuntimeException e2) {
            Slog.w(TAG, "Failed to dump hprof:" + e2);
        }
        Slog.d(TAG, "abort system_server in dumpHprofWhenOOM to get coredump.");
        Process.sendSignal(Process.myPid(), 6);
    }

    private Watchdog() {
        ArrayList<HandlerCheckerAndTimeout> arrayList = new ArrayList<>();
        this.mHandlerCheckers = arrayList;
        this.mAllowRestart = true;
        this.mSfHang = false;
        this.mWatchdogTimeoutMillis = 60000L;
        ArrayList arrayList2 = new ArrayList();
        this.mInterestingJavaPids = arrayList2;
        this.mThread = new Thread(new Runnable() { // from class: com.android.server.Watchdog$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                Watchdog.this.run();
            }
        }, "watchdog");
        HandlerChecker handlerChecker = new HandlerChecker(FgThread.getHandler(), "foreground thread");
        this.mMonitorChecker = handlerChecker;
        arrayList.add(HandlerCheckerAndTimeout.withDefaultTimeout(handlerChecker));
        arrayList.add(HandlerCheckerAndTimeout.withDefaultTimeout(new HandlerChecker(new Handler(Looper.getMainLooper()), "main thread")));
        arrayList.add(HandlerCheckerAndTimeout.withDefaultTimeout(new HandlerChecker(UiThread.getHandler(), "ui thread")));
        arrayList.add(HandlerCheckerAndTimeout.withDefaultTimeout(new HandlerChecker(IoThread.getHandler(), "i/o thread")));
        arrayList.add(HandlerCheckerAndTimeout.withDefaultTimeout(new HandlerChecker(DisplayThread.getHandler(), "display thread")));
        arrayList.add(HandlerCheckerAndTimeout.withDefaultTimeout(new HandlerChecker(AnimationThread.getHandler(), "animation thread")));
        arrayList.add(HandlerCheckerAndTimeout.withDefaultTimeout(new HandlerChecker(SurfaceAnimationThread.getHandler(), "surface animation thread")));
        HandlerThread handlerThread = new HandlerThread("binder monitor");
        this.mBinderMonitorThread = handlerThread;
        handlerThread.start();
        HandlerChecker binderChecker = new HandlerChecker(this.mBinderMonitorThread.getThreadHandler(), "binder monitor thread");
        binderChecker.addMonitorLocked(new BinderThreadMonitor());
        arrayList.add(HandlerCheckerAndTimeout.withDefaultTimeout(binderChecker));
        arrayList2.add(Integer.valueOf(Process.myPid()));
        this.exceptionHang = ExceptionLog.getInstance();
        this.mTraceErrorLogger = new TraceErrorLogger();
    }

    public void start() {
        this.mThread.start();
    }

    public void init(Context context, ActivityManagerService activity) {
        this.mActivity = activity;
        context.registerReceiver(new RebootRequestReceiver(), new IntentFilter("android.intent.action.REBOOT"), "android.permission.REBOOT", null);
        ExceptionLog exceptionLog = this.exceptionHang;
        if (exceptionLog != null) {
            exceptionLog.WDTMatterJava(0L);
        }
    }

    /* loaded from: classes.dex */
    private static class SettingsObserver extends ContentObserver {
        private final Context mContext;
        private final Uri mUri;
        private final Watchdog mWatchdog;

        SettingsObserver(Context context, Watchdog watchdog) {
            super(BackgroundThread.getHandler());
            this.mUri = Settings.Global.getUriFor("system_server_watchdog_timeout_ms");
            this.mContext = context;
            this.mWatchdog = watchdog;
            onChange();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mUri.equals(uri)) {
                onChange();
            }
        }

        public void onChange() {
            try {
                this.mWatchdog.updateWatchdogTimeout(Settings.Global.getLong(this.mContext.getContentResolver(), "system_server_watchdog_timeout_ms", 60000L));
            } catch (RuntimeException e) {
                Slog.e(Watchdog.TAG, "Exception while reading settings " + e.getMessage(), e);
            }
        }
    }

    public void registerSettingsObserver(Context context) {
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("system_server_watchdog_timeout_ms"), false, new SettingsObserver(context, this), 0);
    }

    void updateWatchdogTimeout(long timeoutMillis) {
        if (timeoutMillis <= 30000) {
            timeoutMillis = 30001;
        }
        this.mWatchdogTimeoutMillis = timeoutMillis;
        Slog.i(TAG, "Watchdog timeout updated to " + this.mWatchdogTimeoutMillis + " millis");
    }

    private static boolean isInterestingJavaProcess(String processName) {
        return processName.equals(StorageManagerService.sMediaStoreAuthorityProcessName) || processName.equals("com.android.phone");
    }

    public void processStarted(String processName, int pid) {
        if (isInterestingJavaProcess(processName)) {
            Slog.i(TAG, "Interesting Java process " + processName + " started. Pid " + pid);
            synchronized (this.mLock) {
                this.mInterestingJavaPids.add(Integer.valueOf(pid));
            }
        }
    }

    public void processDied(String processName, int pid) {
        if (isInterestingJavaProcess(processName)) {
            Slog.i(TAG, "Interesting Java process " + processName + " died. Pid " + pid);
            synchronized (this.mLock) {
                this.mInterestingJavaPids.remove(Integer.valueOf(pid));
            }
        }
    }

    public void setActivityController(IActivityController controller) {
        synchronized (this.mLock) {
            this.mController = controller;
        }
    }

    public void setAllowRestart(boolean allowRestart) {
        synchronized (this.mLock) {
            this.mAllowRestart = allowRestart;
        }
    }

    public void addMonitor(Monitor monitor) {
        synchronized (this.mLock) {
            this.mMonitorChecker.addMonitorLocked(monitor);
        }
    }

    public void addThread(Handler thread) {
        synchronized (this.mLock) {
            String name = thread.getLooper().getThread().getName();
            this.mHandlerCheckers.add(HandlerCheckerAndTimeout.withDefaultTimeout(new HandlerChecker(thread, name)));
        }
    }

    public void addThread(Handler thread, long timeoutMillis) {
        synchronized (this.mLock) {
            String name = thread.getLooper().getThread().getName();
            this.mHandlerCheckers.add(HandlerCheckerAndTimeout.withCustomTimeout(new HandlerChecker(thread, name), timeoutMillis));
        }
    }

    public void pauseWatchingCurrentThread(String reason) {
        synchronized (this.mLock) {
            Iterator<HandlerCheckerAndTimeout> it = this.mHandlerCheckers.iterator();
            while (it.hasNext()) {
                HandlerCheckerAndTimeout hc = it.next();
                HandlerChecker checker = hc.checker();
                if (Thread.currentThread().equals(checker.getThread())) {
                    checker.pauseLocked(reason);
                }
            }
        }
    }

    public void resumeWatchingCurrentThread(String reason) {
        synchronized (this.mLock) {
            Iterator<HandlerCheckerAndTimeout> it = this.mHandlerCheckers.iterator();
            while (it.hasNext()) {
                HandlerCheckerAndTimeout hc = it.next();
                HandlerChecker checker = hc.checker();
                if (Thread.currentThread().equals(checker.getThread())) {
                    checker.resumeLocked(reason);
                }
            }
        }
    }

    void rebootSystem(String reason) {
        Slog.i(TAG, "Rebooting system because: " + reason);
        IPowerManager pms = ServiceManager.getService("power");
        try {
            pms.reboot(false, reason, false);
        } catch (RemoteException e) {
        }
    }

    private int evaluateCheckerCompletionLocked() {
        int state = 0;
        for (int i = 0; i < this.mHandlerCheckers.size(); i++) {
            HandlerChecker hc = this.mHandlerCheckers.get(i).checker();
            state = Math.max(state, hc.getCompletionStateLocked());
        }
        return state;
    }

    private ArrayList<HandlerChecker> getCheckersWithStateLocked(int completionState) {
        ArrayList<HandlerChecker> checkers = new ArrayList<>();
        for (int i = 0; i < this.mHandlerCheckers.size(); i++) {
            HandlerChecker hc = this.mHandlerCheckers.get(i).checker();
            if (hc.getCompletionStateLocked() == completionState) {
                checkers.add(hc);
            }
        }
        return checkers;
    }

    private String describeCheckersLocked(List<HandlerChecker> checkers) {
        StringBuilder builder = new StringBuilder(128);
        for (int i = 0; i < checkers.size(); i++) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(checkers.get(i).describeBlockedStateLocked());
        }
        return builder.toString();
    }

    private static void addInterestingHidlPids(HashSet<Integer> pids) {
        try {
            IServiceManager serviceManager = IServiceManager.getService();
            ArrayList<IServiceManager.InstanceDebugInfo> dump = serviceManager.debugDump();
            Iterator<IServiceManager.InstanceDebugInfo> it = dump.iterator();
            while (it.hasNext()) {
                IServiceManager.InstanceDebugInfo info = it.next();
                if (info.pid != -1 && HAL_INTERFACES_OF_INTEREST.contains(info.interfaceName)) {
                    pids.add(Integer.valueOf(info.pid));
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
    }

    private long getTotalMemorySize() {
        MemInfoReader reader = new MemInfoReader();
        reader.readMemInfo();
        long totalMemorySize = reader.getTotalSize();
        long totalSwapMemorySize = reader.getSwapTotalSizeKb() * GadgetFunction.NCM;
        if (totalSwapMemorySize == 0) {
            return -1L;
        }
        Slog.d(TAG, "totalMemorySize = " + totalMemorySize);
        Slog.d(TAG, "totalSwapMemorySize = " + totalSwapMemorySize);
        return totalMemorySize + totalSwapMemorySize;
    }

    private static void addInterestingAidlPids(HashSet<Integer> pids) {
        String[] strArr;
        ServiceDebugInfo[] infos = ServiceManager.getServiceDebugInfo();
        if (infos == null) {
            return;
        }
        for (ServiceDebugInfo info : infos) {
            for (String prefix : AIDL_INTERFACE_PREFIXES_OF_INTEREST) {
                if (info.name.startsWith(prefix)) {
                    pids.add(Integer.valueOf(info.debugPid));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ArrayList<Integer> getInterestingNativePids() {
        HashSet<Integer> pids = new HashSet<>();
        addInterestingAidlPids(pids);
        addInterestingHidlPids(pids);
        int[] nativePids = Process.getPidsForCommands(NATIVE_STACKS_OF_INTEREST);
        if (nativePids != null) {
            for (int i : nativePids) {
                pids.add(Integer.valueOf(i));
            }
        }
        return new ArrayList<>(pids);
    }

    /* JADX DEBUG: Different variable names in phi insn: [debuggerWasConnected, memoryInfo], use first */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [987=9] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:268:0x0461 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:89:0x0294 A[Catch: all -> 0x0305, TryCatch #17 {all -> 0x0305, blocks: (B:83:0x0260, B:85:0x0264, B:89:0x0294, B:91:0x02ac, B:95:0x02bb, B:97:0x02bf, B:105:0x02e8, B:107:0x02fa, B:112:0x0312, B:115:0x0318, B:117:0x031d, B:121:0x0323, B:126:0x032b, B:148:0x0387, B:86:0x028b), top: B:298:0x0260 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void run() {
        Object obj;
        List<HandlerChecker> blockedCheckers;
        String subject;
        boolean allowRestart;
        long sfHangTime;
        List<HandlerChecker> blockedCheckers2;
        String subject2;
        boolean allowRestart2;
        int debuggerWasConnected;
        ArrayList<Integer> pids;
        int i;
        IActivityController controller;
        Debug.MemoryInfo memoryInfo;
        int waitState;
        String subject3;
        boolean waitedHalf;
        int debuggerWasConnected2 = null;
        boolean waitedHalf2 = false;
        while (true) {
            List<HandlerChecker> blockedCheckers3 = Collections.emptyList();
            String subject4 = "";
            boolean allowRestart3 = true;
            boolean doWaitedHalfDump = false;
            long watchdogTimeoutMillis = this.mWatchdogTimeoutMillis;
            long checkIntervalMillis = watchdogTimeoutMillis / 2;
            this.mSfHang = false;
            ExceptionLog exceptionLog = this.exceptionHang;
            if (exceptionLog != null) {
                exceptionLog.WDTMatterJava(300L);
            }
            if ("true".equals(SystemProperties.get("persist.sys.test.hang", "false")) && "true".equals(SystemProperties.get("persist.sys.usic.support", "false"))) {
                SystemProperties.set("persist.sys.test.hang", "false");
                try {
                    Thread.sleep(500000L);
                } catch (Exception e) {
                }
            }
            if ("true".equals(SystemProperties.get("persist.sys.test.je", "false")) && "true".equals(SystemProperties.get("persist.sys.usic.support", "false"))) {
                SystemProperties.set("persist.sys.test.je", "false");
                String a = null;
                a.contains("");
            }
            this.mIsGraphicsGc = false;
            Object obj2 = this.mLock;
            synchronized (obj2) {
                int i2 = 0;
                while (i2 < this.mHandlerCheckers.size()) {
                    try {
                        try {
                            HandlerCheckerAndTimeout hc = this.mHandlerCheckers.get(i2);
                            blockedCheckers = blockedCheckers3;
                            try {
                                subject = subject4;
                                try {
                                    allowRestart = allowRestart3;
                                    try {
                                        obj = obj2;
                                        try {
                                            hc.checker().scheduleCheckLocked(hc.customTimeoutMillis().orElse(Long.valueOf(Build.HW_TIMEOUT_MULTIPLIER * watchdogTimeoutMillis)).longValue());
                                            i2++;
                                            blockedCheckers3 = blockedCheckers;
                                            subject4 = subject;
                                            allowRestart3 = allowRestart;
                                            obj2 = obj;
                                        } catch (Throwable th) {
                                            th = th;
                                            while (true) {
                                                try {
                                                    break;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                }
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        obj = obj2;
                                        while (true) {
                                            break;
                                            break;
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    obj = obj2;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                obj = obj2;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            obj = obj2;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        obj = obj2;
                    }
                }
                blockedCheckers = blockedCheckers3;
                subject = subject4;
                allowRestart = allowRestart3;
                obj = obj2;
                int debuggerWasConnected3 = 0 > 0 ? 0 - 1 : 0;
                try {
                    long start = SystemClock.uptimeMillis();
                    for (long timeout = checkIntervalMillis; timeout > 0; timeout = checkIntervalMillis - (SystemClock.uptimeMillis() - start)) {
                        if (Debug.isDebuggerConnected()) {
                            debuggerWasConnected3 = 2;
                        }
                        try {
                            this.mLock.wait(timeout);
                        } catch (InterruptedException e2) {
                            Log.wtf(TAG, e2);
                        }
                        if (Debug.isDebuggerConnected()) {
                            debuggerWasConnected3 = 2;
                        }
                    }
                    try {
                        sfHangTime = getSfStatus();
                    } catch (Throwable th8) {
                        th = th8;
                    }
                    try {
                        if (sfHangTime > 40000) {
                            Slog.v(TAG, "**SF hang Time **" + sfHangTime);
                            this.mSfHang = true;
                            List<HandlerChecker> blockedCheckers4 = getCheckersWithStateLocked(2);
                            try {
                                ArrayList<Integer> pids2 = new ArrayList<>(this.mInterestingJavaPids);
                                blockedCheckers2 = blockedCheckers4;
                                subject2 = "";
                                allowRestart2 = allowRestart;
                                debuggerWasConnected = debuggerWasConnected3;
                                pids = pids2;
                            } catch (Throwable th9) {
                                th = th9;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        } else {
                            if ("1".equals(SystemProperties.get("persist.sys.oomhprof.support", "0"))) {
                                try {
                                    long currentTime = System.currentTimeMillis();
                                    if (this.mLastDumpHeapTime + 1800000 < currentTime) {
                                        this.mLastDumpHeapTime = System.currentTimeMillis();
                                        if (debuggerWasConnected2 == null) {
                                            debuggerWasConnected2 = new Debug.MemoryInfo();
                                        }
                                        Debug.getMemoryInfo(debuggerWasConnected2);
                                        Slog.d(TAG, "system server total memory = " + debuggerWasConnected2.getSummaryTotalPss() + "K,dalvikPss = " + debuggerWasConnected2.dalvikPss + ",dalvikSwappedOutPss = " + debuggerWasConnected2.dalvikSwappedOutPss + ",nativePss = " + debuggerWasConnected2.nativePss + ",nativeSwappedOutPss = " + debuggerWasConnected2.nativeSwappedOutPss + ",SummaryCode = " + debuggerWasConnected2.getSummaryCode() + ",SummaryGraphics = " + debuggerWasConnected2.getSummaryGraphics() + ",SummarySystem = " + debuggerWasConnected2.getSummarySystem());
                                        long totalMemorySize = getTotalMemorySize();
                                        Slog.d(TAG, "totalMemorySize = " + totalMemorySize);
                                        debuggerWasConnected = debuggerWasConnected3;
                                        if (debuggerWasConnected2.getSummaryGraphics() >= 1048576) {
                                            try {
                                                if (!this.mIsGraphicsGc) {
                                                    System.gc();
                                                    System.runFinalization();
                                                    System.gc();
                                                    this.mIsGraphicsGc = true;
                                                    Slog.d(TAG, "System has gc the Graphics end;  mIsGraphicsGc =  " + this.mIsGraphicsGc);
                                                    if (totalMemorySize > 0) {
                                                        float mOOMLimitSize = (((float) totalMemorySize) * 0.15f) / 1024.0f;
                                                        float nativeLimitSize = (((float) totalMemorySize) * 0.1f) / 1024.0f;
                                                        if (debuggerWasConnected2.getSummaryTotalPss() <= mOOMLimitSize) {
                                                            if (debuggerWasConnected2.nativePss + debuggerWasConnected2.nativeSwappedOutPss > nativeLimitSize) {
                                                            }
                                                        }
                                                        if (!this.mIsGraphicsGc) {
                                                            dumpHprofWhenOOM();
                                                        }
                                                    }
                                                }
                                            } catch (Throwable th10) {
                                                th = th10;
                                                while (true) {
                                                    break;
                                                    break;
                                                }
                                                throw th;
                                            }
                                        }
                                        this.mIsGraphicsGc = false;
                                        if (totalMemorySize > 0) {
                                        }
                                    } else {
                                        debuggerWasConnected = debuggerWasConnected3;
                                    }
                                } catch (Throwable th11) {
                                    th = th11;
                                }
                            } else {
                                debuggerWasConnected = debuggerWasConnected3;
                            }
                            try {
                                int waitState2 = evaluateCheckerCompletionLocked();
                                if ("true".equals(SystemProperties.get("persist.sys.test.swt", "false")) && "true".equals(SystemProperties.get("persist.sys.usic.support", "false"))) {
                                    SystemProperties.set("persist.sys.test.swt", "false");
                                    waitState = 3;
                                } else {
                                    waitState = waitState2;
                                }
                                if (waitState == 0) {
                                    ExceptionLog exceptionLog2 = this.exceptionHang;
                                    if (exceptionLog2 != null && waitedHalf2) {
                                        exceptionLog2.switchFtrace(4);
                                    }
                                    waitedHalf2 = false;
                                } else if (waitState == 1) {
                                } else if (waitState != 2) {
                                    List<HandlerChecker> blockedCheckers5 = getCheckersWithStateLocked(3);
                                    String subject5 = describeCheckersLocked(blockedCheckers5);
                                    boolean allowRestart4 = this.mAllowRestart;
                                    try {
                                        try {
                                            blockedCheckers2 = blockedCheckers5;
                                            subject2 = subject5;
                                            allowRestart2 = allowRestart4;
                                            waitedHalf2 = waitedHalf2;
                                            pids = new ArrayList<>(this.mInterestingJavaPids);
                                        } catch (Throwable th12) {
                                            th = th12;
                                            while (true) {
                                                break;
                                                break;
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th13) {
                                        th = th13;
                                    }
                                } else if (waitedHalf2) {
                                } else {
                                    Slog.i(TAG, "WAITED_HALF");
                                    List<HandlerChecker> blockedCheckers6 = getCheckersWithStateLocked(2);
                                    try {
                                        subject3 = describeCheckersLocked(blockedCheckers6);
                                        try {
                                            pids = new ArrayList<>(this.mInterestingJavaPids);
                                            doWaitedHalfDump = true;
                                        } catch (Throwable th14) {
                                            th = th14;
                                        }
                                    } catch (Throwable th15) {
                                        th = th15;
                                    }
                                    try {
                                        if (ActivityManager.getService() != null) {
                                            ActivityManager.getService().startTNE("0x007a0000", 2217L, Process.myPid(), "");
                                        }
                                        waitedHalf = true;
                                    } catch (RemoteException e3) {
                                        waitedHalf = true;
                                        try {
                                            Log.e(TAG, "Can't call IActivityManager");
                                        } catch (Throwable th16) {
                                            th = th16;
                                            while (true) {
                                                break;
                                                break;
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th17) {
                                        th = th17;
                                        while (true) {
                                            break;
                                            break;
                                        }
                                        throw th;
                                    }
                                    blockedCheckers2 = blockedCheckers6;
                                    subject2 = subject3;
                                    allowRestart2 = allowRestart;
                                    waitedHalf2 = waitedHalf;
                                }
                            } catch (Throwable th18) {
                                th = th18;
                            }
                        }
                        logWatchog(doWaitedHalfDump, subject2, pids);
                        if (doWaitedHalfDump) {
                            continue;
                        } else {
                            if ("1".equals(SystemProperties.get("persist.sys.adb.support", "0"))) {
                                try {
                                    TNEService tnev = new TNEService();
                                    int type = 2;
                                    int time = 3000;
                                    try {
                                        int sysPid = SystemProperties.getInt("sys.transsion.lowmem", 0);
                                        if (sysPid > 0) {
                                            try {
                                                SystemProperties.set("sys.transsion.lowmem", "0");
                                                type = 256;
                                                time = 5000;
                                            } catch (Exception e4) {
                                                e = e4;
                                                Slog.e(TAG, "start tne error " + e);
                                                synchronized (this.mLock) {
                                                }
                                            }
                                        }
                                        try {
                                            tnev.startTNE("0x007a0001", type, sysPid, "");
                                            SystemClock.sleep(time);
                                        } catch (Exception e5) {
                                            e = e5;
                                            Slog.e(TAG, "start tne error " + e);
                                            synchronized (this.mLock) {
                                            }
                                        }
                                    } catch (Exception e6) {
                                        e = e6;
                                    }
                                } catch (Exception e7) {
                                    e = e7;
                                }
                            } else {
                                try {
                                    TNEService tnev2 = new TNEService();
                                    tnev2.startTNE("0xffffff01", 1962214L, Process.myPid(), "");
                                    SystemClock.sleep(BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
                                } catch (Exception e8) {
                                    Slog.e(TAG, "start tne error " + e8);
                                }
                            }
                            synchronized (this.mLock) {
                                try {
                                    controller = this.mController;
                                } finally {
                                    th = th;
                                    i = debuggerWasConnected2;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th19) {
                                            th = th19;
                                        }
                                    }
                                }
                            }
                            if (!this.mSfHang && controller != null) {
                                Slog.i(TAG, "Reporting stuck state to activity controller");
                                try {
                                    Binder.setDumpDisabled("Service dumps disabled due to hung system process.");
                                    if (DEBUG_CONTROLLER) {
                                        int res = controller.systemNotResponding(subject2);
                                        if (res >= 0) {
                                            Slog.i(TAG, "Activity controller requested to coninue to wait");
                                            waitedHalf2 = false;
                                        }
                                    }
                                    Slog.i(TAG, "Activity controller requested to reboot");
                                } catch (RemoteException e9) {
                                }
                            }
                            debuggerWasConnected = Debug.isDebuggerConnected() ? 2 : debuggerWasConnected2;
                            if (debuggerWasConnected >= 2) {
                                Slog.w(TAG, "Debugger connected: Watchdog is *not* killing the system process");
                                memoryInfo = debuggerWasConnected2;
                            } else if (debuggerWasConnected > 0) {
                                Slog.w(TAG, "Debugger was connected: Watchdog is *not* killing the system process");
                                memoryInfo = debuggerWasConnected2;
                            } else if (allowRestart2) {
                                Slog.w(TAG, "*** WATCHDOG KILLING SYSTEM PROCESS: " + subject2);
                                WatchdogDiagnostics.diagnoseCheckers(blockedCheckers2);
                                Slog.w(TAG, "*** GOODBYE!");
                                if (!Build.IS_USER && isCrashLoopFound() && !WatchdogProperties.should_ignore_fatal_count().orElse(false).booleanValue()) {
                                    breakCrashLoop();
                                }
                                memoryInfo = debuggerWasConnected2;
                                this.exceptionHang.WDTMatterJava(330L);
                                if (this.mSfHang) {
                                    Slog.w(TAG, "SF hang!");
                                    if (getSfReboot() > 3) {
                                        Slog.w(TAG, "SF hang reboot time larger than 3 time, reboot device!");
                                        rebootSystem("Maybe SF driver hang, reboot device.");
                                    } else {
                                        setSfReboot();
                                    }
                                    Slog.v(TAG, "killing surfaceflinger for surfaceflinger hang");
                                    String[] sf = {"/system/bin/surfaceflinger"};
                                    int[] pid_sf = Process.getPidsForCommands(sf);
                                    if (pid_sf[0] > 0) {
                                        Process.killProcess(pid_sf[0]);
                                    }
                                    Slog.v(TAG, "kill surfaceflinger end");
                                } else {
                                    Process.killProcess(Process.myPid());
                                }
                                System.exit(10);
                            } else {
                                Slog.w(TAG, "Restart not allowed: Watchdog is *not* killing the system process");
                                memoryInfo = debuggerWasConnected2;
                            }
                            waitedHalf2 = false;
                            debuggerWasConnected2 = memoryInfo;
                        }
                    } catch (Throwable th20) {
                        th = th20;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                } catch (Throwable th21) {
                    th = th21;
                }
            }
        }
    }

    private void logWatchog(boolean halfWatchdog, String subject, ArrayList<Integer> pids) {
        String dropboxTag;
        String criticalEvents = CriticalEventLog.getInstance().logLinesForSystemServerTraceFile();
        final UUID errorId = this.mTraceErrorLogger.generateErrorId();
        if (this.mTraceErrorLogger.isAddErrorIdEnabled()) {
            this.mTraceErrorLogger.addErrorIdToTrace("system_server", errorId);
            this.mTraceErrorLogger.addSubjectToTrace(subject, errorId);
        }
        if (halfWatchdog) {
            ExceptionLog exceptionLog = this.exceptionHang;
            if (exceptionLog != null) {
                exceptionLog.WDTMatterJava(360L);
                this.exceptionHang.switchFtrace(3);
            }
            CriticalEventLog.getInstance().logHalfWatchdog(subject);
            dropboxTag = "pre_watchdog";
        } else {
            CriticalEventLog.getInstance().logWatchdog(subject, errorId);
            Slog.e(TAG, "**SWT happen **" + subject);
            ExceptionLog exceptionLog2 = this.exceptionHang;
            if (exceptionLog2 != null) {
                exceptionLog2.switchFtrace(2);
            }
            String sfLog = (this.mSfHang && subject.isEmpty()) ? "surfaceflinger hang." : "";
            EventLog.writeEvent((int) EventLogTags.WATCHDOG, sfLog.isEmpty() ? subject : sfLog);
            ExceptionLog exceptionLog3 = this.exceptionHang;
            if (exceptionLog3 != null) {
                exceptionLog3.WDTMatterJava(420L);
            }
            FrameworkStatsLog.write(185, subject);
            dropboxTag = "watchdog";
        }
        long anrTime = SystemClock.uptimeMillis();
        final StringBuilder report = new StringBuilder();
        report.append(MemoryPressureUtil.currentPsiState());
        ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(false);
        StringWriter tracesFileException = new StringWriter();
        final File stack = ActivityManagerService.dumpStackTraces(pids, processCpuTracker, new SparseArray(), getInterestingNativePids(), tracesFileException, subject, criticalEvents);
        SystemClock.sleep(5000L);
        processCpuTracker.update();
        report.append(processCpuTracker.printCurrentState(anrTime));
        report.append(tracesFileException.getBuffer());
        if (!halfWatchdog) {
            doSysRq('w');
            doSysRq('l');
        }
        final String str = dropboxTag;
        Thread dropboxThread = new Thread("watchdogWriteToDropbox") { // from class: com.android.server.Watchdog.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                if (Watchdog.this.mActivity != null) {
                    Watchdog.this.mActivity.addErrorToDropBox(str, null, "system_server", null, null, null, null, report.toString(), stack, null, null, null, errorId);
                }
            }
        };
        dropboxThread.start();
        try {
            dropboxThread.join(4000L);
        } catch (InterruptedException e) {
        }
    }

    private void doSysRq(char c) {
        try {
            FileWriter sysrq_trigger = new FileWriter("/proc/sysrq-trigger");
            sysrq_trigger.write(c);
            sysrq_trigger.close();
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write to /proc/sysrq-trigger", e);
        }
    }

    private void resetTimeoutHistory() {
        writeTimeoutHistory(new ArrayList());
    }

    private void writeTimeoutHistory(Iterable<String> crashHistory) {
        String data = String.join(",", crashHistory);
        try {
            FileWriter writer = new FileWriter(TIMEOUT_HISTORY_FILE);
            writer.write(SystemProperties.get("ro.boottime.zygote"));
            writer.write(":");
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write file /data/system/watchdog-timeout-history.txt", e);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1223=4] */
    private String[] readTimeoutHistory() {
        String[] emptyStringArray = new String[0];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(TIMEOUT_HISTORY_FILE));
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                return emptyStringArray;
            }
            String[] data = line.trim().split(":");
            String boottime = data.length >= 1 ? data[0] : "";
            String history = data.length >= 2 ? data[1] : "";
            if (!SystemProperties.get("ro.boottime.zygote").equals(boottime) || history.isEmpty()) {
                reader.close();
                return emptyStringArray;
            }
            String[] split = history.split(",");
            reader.close();
            return split;
        } catch (FileNotFoundException e) {
            return emptyStringArray;
        } catch (IOException e2) {
            Slog.e(TAG, "Failed to read file /data/system/watchdog-timeout-history.txt", e2);
            return emptyStringArray;
        }
    }

    private boolean hasActiveUsbConnection() {
        try {
            String state = FileUtils.readTextFile(new File("/sys/class/android_usb/android0/state"), 128, null).trim();
            if ("CONFIGURED".equals(state)) {
                return true;
            }
            return false;
        } catch (IOException e) {
            Slog.w(TAG, "Failed to determine if device was on USB", e);
            return false;
        }
    }

    private boolean isCrashLoopFound() {
        int fatalCount = WatchdogProperties.fatal_count().orElse(0).intValue();
        long fatalWindowMs = TimeUnit.SECONDS.toMillis(WatchdogProperties.fatal_window_seconds().orElse(0).intValue());
        if (fatalCount == 0 || fatalWindowMs == 0) {
            if (fatalCount != fatalWindowMs) {
                Slog.w(TAG, String.format("sysprops '%s' and '%s' should be set or unset together", PROP_FATAL_LOOP_COUNT, PROP_FATAL_LOOP_WINDOWS_SECS));
            }
            return false;
        }
        long nowMs = SystemClock.elapsedRealtime();
        String[] rawCrashHistory = readTimeoutHistory();
        ArrayList<String> crashHistory = new ArrayList<>(Arrays.asList((String[]) Arrays.copyOfRange(rawCrashHistory, Math.max(0, (rawCrashHistory.length - fatalCount) - 1), rawCrashHistory.length)));
        crashHistory.add(String.valueOf(nowMs));
        writeTimeoutHistory(crashHistory);
        if (hasActiveUsbConnection()) {
            return false;
        }
        try {
            long firstCrashMs = Long.parseLong(crashHistory.get(0));
            return crashHistory.size() >= fatalCount && nowMs - firstCrashMs < fatalWindowMs;
        } catch (NumberFormatException t) {
            Slog.w(TAG, "Failed to parseLong " + crashHistory.get(0), t);
            resetTimeoutHistory();
            return false;
        }
    }

    private void breakCrashLoop() {
        try {
            FileWriter kmsg = new FileWriter("/dev/kmsg_debug", true);
            kmsg.append((CharSequence) "Fatal reset to escape the system_server crashing loop\n");
            kmsg.close();
        } catch (IOException e) {
            Slog.w(TAG, "Failed to append to kmsg", e);
        }
        doSysRq('c');
    }

    public void dump(PrintWriter pw, String[] args) {
        pw.print("WatchdogTimeoutMillis=");
        pw.println(this.mWatchdogTimeoutMillis);
    }
}
