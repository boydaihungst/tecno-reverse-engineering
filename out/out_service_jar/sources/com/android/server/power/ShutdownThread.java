package com.android.server.power;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.ProgressDialog;
import android.app.admin.SecurityLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManagerInternal;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.FileUtils;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.TimingsTraceLog;
import com.android.server.LocalServices;
import com.android.server.NVUtils;
import com.android.server.RescueParty;
import com.android.server.am.HostingRecord;
import com.android.server.job.controllers.JobStatus;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.mediatek.server.MtkSystemServiceFactory;
import com.transsion.hubcore.server.power.ITranShutdownThread;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import com.transsion.hubcore.sru.ITranSruManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
/* loaded from: classes2.dex */
public class ShutdownThread extends Thread {
    private static final int ACTION_DONE_POLL_WAIT_MS = 500;
    private static final int ACTIVITY_MANAGER_STOP_PERCENT = 4;
    private static final int BROADCAST_STOP_PERCENT = 2;
    private static final String CHECK_POINTS_FILE_BASENAME = "/data/system/shutdown-checkpoints/checkpoints";
    private static final int MAX_BROADCAST_TIME = 10000;
    private static final int MAX_CHECK_POINTS_DUMP_WAIT_TIME = 10000;
    private static final int MAX_RADIO_WAIT_TIME = 12000;
    private static final int MAX_UNCRYPT_WAIT_TIME = 900000;
    private static final String METRICS_FILE_BASENAME = "/data/system/shutdown-metrics";
    private static final int MOUNT_SERVICE_STOP_PERCENT = 20;
    private static final int PACKAGE_MANAGER_STOP_PERCENT = 6;
    private static final int RADIOS_STATE_POLL_SLEEP_MS = 100;
    private static final int RADIO_STOP_PERCENT = 18;
    public static final String REBOOT_SAFEMODE_PROPERTY = "persist.sys.safemode";
    public static final String RO_SAFEMODE_PROPERTY = "ro.sys.safemode";
    public static final String SHUTDOWN_ACTION_PROPERTY = "sys.shutdown.requested";
    private static final int SHUTDOWN_VIBRATE_MS = 500;
    private static final String TAG = "ShutdownThread";
    protected static String mReason;
    protected static boolean mReboot;
    protected static boolean mRebootHasProgressBar;
    protected static boolean mRebootSafeMode;
    private static AlertDialog sConfirmDialog;
    private boolean mActionDone;
    private final Object mActionDoneSync = new Object();
    protected Context mContext;
    private PowerManager.WakeLock mCpuWakeLock;
    protected Handler mHandler;
    protected PowerManager mPowerManager;
    private ProgressDialog mProgressDialog;
    protected PowerManager.WakeLock mScreenWakeLock;
    private static final Object sIsStartedGuard = new Object();
    private static boolean sIsStarted = false;
    protected static final ShutdownThread sInstance = MtkSystemServiceFactory.getInstance().makeMtkShutdownThread();
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private static final ArrayMap<String, Long> TRON_METRICS = new ArrayMap<>();
    private static String METRIC_SYSTEM_SERVER = "shutdown_system_server";
    private static String METRIC_SEND_BROADCAST = "shutdown_send_shutdown_broadcast";
    private static String METRIC_AM = "shutdown_activity_manager";
    private static String METRIC_PM = "shutdown_package_manager";
    private static String METRIC_RADIOS = "shutdown_radios";
    private static String METRIC_RADIO = "shutdown_radio";
    private static String METRIC_SHUTDOWN_TIME_START = "begin_shutdown";

    /* renamed from: -$$Nest$smnewTimingsLog  reason: not valid java name */
    static /* bridge */ /* synthetic */ TimingsTraceLog m6184$$Nest$smnewTimingsLog() {
        return newTimingsLog();
    }

    public static void shutdown(Context context, String reason, boolean confirm) {
        if (ITranWindowManagerService.Instance().getOneHandCurrentState() != 0) {
            ITranWindowManagerService.Instance().exitOneHandMode();
        }
        mReboot = false;
        mRebootSafeMode = false;
        mReason = reason;
        Log.d(TAG, "shutdown reason ==" + reason, new Throwable());
        shutdownInner(context, confirm);
    }

    private static void shutdownInner(final Context context, boolean confirm) {
        int resourceId;
        int i;
        context.assertRuntimeOverlayThemable();
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Request to shutdown already running, returning.");
                return;
            }
            ShutdownCheckPoints.recordCheckPoint(null);
            int longPressBehavior = context.getResources().getInteger(17694858);
            if (mRebootSafeMode) {
                resourceId = 17041361;
            } else if (longPressBehavior == 2) {
                resourceId = 17041515;
            } else {
                resourceId = 17041514;
            }
            Log.d(TAG, "Notifying thread to start shutdown longPressBehavior=" + longPressBehavior);
            if (confirm) {
                CloseDialogReceiver closer = new CloseDialogReceiver(context);
                AlertDialog alertDialog = sConfirmDialog;
                if (alertDialog != null) {
                    alertDialog.dismiss();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                if (mRebootSafeMode) {
                    i = 17041362;
                } else {
                    i = 17041343;
                }
                AlertDialog create = builder.setTitle(i).setMessage(resourceId).setPositiveButton(17039379, new DialogInterface.OnClickListener() { // from class: com.android.server.power.ShutdownThread.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        ShutdownThread.beginShutdownSequence(context);
                    }
                }).setNegativeButton(17039369, (DialogInterface.OnClickListener) null).create();
                sConfirmDialog = create;
                closer.dialog = create;
                sConfirmDialog.setOnDismissListener(closer);
                sConfirmDialog.getWindow().setType(2009);
                sConfirmDialog.show();
                return;
            }
            beginShutdownSequence(context);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class CloseDialogReceiver extends BroadcastReceiver implements DialogInterface.OnDismissListener {
        public Dialog dialog;
        private Context mContext;

        CloseDialogReceiver(Context context) {
            this.mContext = context;
            IntentFilter filter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            context.registerReceiver(this, filter, 2);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            this.dialog.cancel();
        }

        @Override // android.content.DialogInterface.OnDismissListener
        public void onDismiss(DialogInterface unused) {
            this.mContext.unregisterReceiver(this);
        }
    }

    public static void reboot(Context context, String reason, boolean confirm) {
        if (ITranWindowManagerService.Instance().getOneHandCurrentState() != 0) {
            ITranWindowManagerService.Instance().exitOneHandMode();
        }
        mReboot = true;
        mRebootSafeMode = false;
        mRebootHasProgressBar = false;
        mReason = reason;
        if (Build.IS_DEBUG_ENABLE) {
            Log.d(TAG, "reboot reason ==" + reason, new Throwable());
        }
        shutdownInner(context, confirm);
    }

    public static void rebootSafeMode(Context context, boolean confirm) {
        UserManager um = (UserManager) context.getSystemService("user");
        if (um.hasUserRestriction("no_safe_boot")) {
            return;
        }
        if (ITranWindowManagerService.Instance().getOneHandCurrentState() != 0) {
            ITranWindowManagerService.Instance().exitOneHandMode();
        }
        mReboot = true;
        mRebootSafeMode = true;
        mRebootHasProgressBar = false;
        mReason = null;
        shutdownInner(context, confirm);
    }

    private static ProgressDialog showShutdownDialog(Context context) {
        ProgressDialog pd = new ProgressDialog(context);
        String str = mReason;
        if (str != null && str.startsWith("recovery-update")) {
            mRebootHasProgressBar = RecoverySystem.UNCRYPT_PACKAGE_FILE.exists() && !RecoverySystem.BLOCK_MAP_FILE.exists();
            pd.setTitle(context.getText(17041368));
            if (mRebootHasProgressBar) {
                pd.setMax(100);
                pd.setProgress(0);
                pd.setIndeterminate(false);
                pd.setProgressNumberFormat(null);
                pd.setProgressStyle(1);
                pd.setMessage(context.getText(17041366));
            } else if (showSysuiReboot()) {
                return null;
            } else {
                pd.setIndeterminate(true);
                pd.setMessage(context.getText(17041367));
            }
        } else {
            String str2 = mReason;
            if (str2 != null && str2.equals("recovery")) {
                if (RescueParty.isAttemptingFactoryReset()) {
                    pd.setTitle(context.getText(17041343));
                    pd.setMessage(context.getText(17041516));
                    pd.setIndeterminate(true);
                } else if (showSysuiReboot()) {
                    return null;
                } else {
                    pd.setTitle(context.getText(17041364));
                    pd.setMessage(context.getText(17041363));
                    pd.setIndeterminate(true);
                }
            } else if (showSysuiReboot()) {
                return null;
            } else {
                pd.setTitle(context.getText(17041343));
                pd.setMessage(context.getText(17041516));
                pd.setIndeterminate(true);
            }
        }
        pd.setCancelable(false);
        pd.getWindow().setType(2009);
        if (sInstance.mIsShowShutdownDialog(context)) {
            pd.show();
        }
        return pd;
    }

    private static boolean showSysuiReboot() {
        if (sInstance.mIsShowShutdownSysui()) {
            Log.d(TAG, "Attempting to use SysUI shutdown UI");
            try {
                StatusBarManagerInternal service = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                if (service.showShutdownUi(mReboot, mReason)) {
                    Log.d(TAG, "SysUI handling shutdown UI");
                    return true;
                }
            } catch (Exception e) {
            }
            Log.d(TAG, "SysUI is unavailable");
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void beginShutdownSequence(Context context) {
        ITranSruManager.Instance().sendShutdownOrBootEvent(2, mReason);
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Shutdown sequence already running, returning.");
                return;
            }
            sIsStarted = true;
            ShutdownThread shutdownThread = sInstance;
            shutdownThread.mProgressDialog = showShutdownDialog(context);
            shutdownThread.mContext = context;
            PowerManager powerManager = (PowerManager) context.getSystemService("power");
            shutdownThread.mPowerManager = powerManager;
            shutdownThread.mCpuWakeLock = null;
            try {
                PowerManager.WakeLock newWakeLock = powerManager.newWakeLock(1, "ShutdownThread-cpu");
                shutdownThread.mCpuWakeLock = newWakeLock;
                newWakeLock.setReferenceCounted(false);
                shutdownThread.mCpuWakeLock.acquire();
            } catch (SecurityException e) {
                Log.w(TAG, "No permission to acquire wake lock", e);
                sInstance.mCpuWakeLock = null;
            }
            ShutdownThread shutdownThread2 = sInstance;
            shutdownThread2.mScreenWakeLock = null;
            if (shutdownThread2.mPowerManager.isScreenOn()) {
                try {
                    PowerManager.WakeLock newWakeLock2 = shutdownThread2.mPowerManager.newWakeLock(26, "ShutdownThread-screen");
                    shutdownThread2.mScreenWakeLock = newWakeLock2;
                    newWakeLock2.setReferenceCounted(false);
                    shutdownThread2.mScreenWakeLock.acquire();
                } catch (SecurityException e2) {
                    Log.w(TAG, "No permission to acquire wake lock", e2);
                    sInstance.mScreenWakeLock = null;
                }
            }
            if (SecurityLog.isLoggingEnabled()) {
                SecurityLog.writeEvent(210010, new Object[0]);
            }
            ShutdownThread shutdownThread3 = sInstance;
            shutdownThread3.mHandler = new Handler() { // from class: com.android.server.power.ShutdownThread.2
            };
            if (shutdownThread3.mStartShutdownSeq(context)) {
                shutdownThread3.start();
            }
        }
    }

    void actionDone() {
        synchronized (this.mActionDoneSync) {
            this.mActionDone = true;
            this.mActionDoneSync.notifyAll();
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:37:0x0134, code lost:
        android.util.Log.w(com.android.server.power.ShutdownThread.TAG, "Shutdown broadcast timed out");
     */
    @Override // java.lang.Thread, java.lang.Runnable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void run() {
        long endTime;
        TimingsTraceLog shutdownTimingLog = newTimingsLog();
        shutdownTimingLog.traceBegin("SystemServerShutdown");
        metricShutdownStart();
        metricStarted(METRIC_SYSTEM_SERVER);
        Thread dumpCheckPointsThread = ShutdownCheckPoints.newDumpThread(new File(CHECK_POINTS_FILE_BASENAME));
        dumpCheckPointsThread.start();
        BroadcastReceiver br = new BroadcastReceiver() { // from class: com.android.server.power.ShutdownThread.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                ShutdownThread.this.actionDone();
            }
        };
        StringBuilder append = new StringBuilder().append(mReboot ? "1" : "0");
        String str = mReason;
        if (str == null) {
            str = "";
        }
        String reason = append.append(str).toString();
        SystemProperties.set(SHUTDOWN_ACTION_PROPERTY, reason);
        if (mRebootSafeMode) {
            SystemProperties.set(REBOOT_SAFEMODE_PROPERTY, "1");
        }
        shutdownTimingLog.traceBegin("DumpPreRebootInfo");
        try {
            Slog.i(TAG, "Logging pre-reboot information...");
            PreRebootLogger.log(this.mContext);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to log pre-reboot information", e);
        }
        shutdownTimingLog.traceEnd();
        metricStarted(METRIC_SEND_BROADCAST);
        shutdownTimingLog.traceBegin("SendShutdownBroadcast");
        Log.i(TAG, "Sending shutdown broadcast...");
        this.mActionDone = false;
        NVUtils mNVUtils = new NVUtils();
        if (mNVUtils.rlk_readData(SystemProperties.getInt("ro.proinfo.auto_shutdown", 830)) != 0) {
            mNVUtils.rlk_writeDate(SystemProperties.getInt("ro.proinfo.auto_shutdown", 830), 0);
        } else {
            if (NVUtils.isOOBE(this.mContext)) {
                mNVUtils.rlk_writeDate(817, 1);
                Log.d(TAG, "running : ------- Utils.rlk_writeDate(817, 1)");
            } else {
                if (1 == mNVUtils.rlk_readData(SystemProperties.getInt("ro.proinfo.skip_oobe", 819))) {
                    mNVUtils.rlk_writeDate(817, 3);
                } else {
                    mNVUtils.rlk_writeDate(817, 2);
                }
                Log.d(TAG, "running : ------- Utils.rlk_writeDate(817, 2/3) ");
            }
            Log.d(TAG, "running : ------- Utils.rlk_readData(817) : " + mNVUtils.rlk_readData(817));
        }
        Intent intent = new Intent("android.intent.action.ACTION_SHUTDOWN");
        intent.addFlags(1342177280);
        this.mContext.sendOrderedBroadcastAsUser(intent, UserHandle.ALL, null, br, this.mHandler, 0, null, null);
        long endTime2 = SystemClock.elapsedRealtime() + JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        synchronized (this.mActionDoneSync) {
            while (true) {
                try {
                } catch (Throwable th) {
                    th = th;
                }
                if (this.mActionDone) {
                    break;
                }
                long delay = endTime2 - SystemClock.elapsedRealtime();
                if (delay > 0) {
                    if (!mRebootHasProgressBar) {
                        endTime = endTime2;
                    } else {
                        endTime = endTime2;
                        long endTime3 = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY - delay;
                        int status = (int) (((endTime3 * 1.0d) * 2.0d) / 10000.0d);
                        try {
                            sInstance.setRebootProgress(status, null);
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    try {
                        this.mActionDoneSync.wait(Math.min(delay, 500L));
                    } catch (InterruptedException e2) {
                    }
                    endTime2 = endTime;
                } else {
                    try {
                        break;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                throw th;
            }
            if (mRebootHasProgressBar) {
                sInstance.setRebootProgress(2, null);
            }
            shutdownTimingLog.traceEnd();
            metricEnded(METRIC_SEND_BROADCAST);
            Log.i(TAG, "Shutting down activity manager...");
            shutdownTimingLog.traceBegin("ShutdownActivityManager");
            metricStarted(METRIC_AM);
            IActivityManager am = IActivityManager.Stub.asInterface(ServiceManager.checkService(HostingRecord.HOSTING_TYPE_ACTIVITY));
            if (am != null) {
                try {
                    am.shutdown(10000);
                } catch (RemoteException e3) {
                }
            }
            if (mRebootHasProgressBar) {
                sInstance.setRebootProgress(4, null);
            }
            shutdownTimingLog.traceEnd();
            metricEnded(METRIC_AM);
            Log.i(TAG, "Shutting down package manager...");
            shutdownTimingLog.traceBegin("ShutdownPackageManager");
            metricStarted(METRIC_PM);
            PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            if (pm != null) {
                pm.shutdown();
            }
            if (mRebootHasProgressBar) {
                sInstance.setRebootProgress(6, null);
            }
            shutdownTimingLog.traceEnd();
            metricEnded(METRIC_PM);
            shutdownTimingLog.traceBegin("ShutdownRadios");
            metricStarted(METRIC_RADIOS);
            shutdownRadios(MAX_RADIO_WAIT_TIME);
            if (mRebootHasProgressBar) {
                sInstance.setRebootProgress(18, null);
            }
            shutdownTimingLog.traceEnd();
            metricEnded(METRIC_RADIOS);
            if (mRebootHasProgressBar) {
                sInstance.setRebootProgress(20, null);
                uncrypt();
            }
            shutdownTimingLog.traceBegin("ShutdownCheckPointsDumpWait");
            try {
                dumpCheckPointsThread.join(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            } catch (InterruptedException e4) {
            }
            shutdownTimingLog.traceEnd();
            mShutdownSeqFinish(this.mContext);
            shutdownTimingLog.traceEnd();
            metricEnded(METRIC_SYSTEM_SERVER);
            saveMetrics(mReboot, mReason);
            rebootOrShutdown(this.mContext, mReboot, mReason);
        }
    }

    private static TimingsTraceLog newTimingsLog() {
        return new TimingsTraceLog("ShutdownTiming", 524288L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void metricStarted(String metricKey) {
        ArrayMap<String, Long> arrayMap = TRON_METRICS;
        synchronized (arrayMap) {
            arrayMap.put(metricKey, Long.valueOf(SystemClock.elapsedRealtime() * (-1)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void metricEnded(String metricKey) {
        ArrayMap<String, Long> arrayMap = TRON_METRICS;
        synchronized (arrayMap) {
            arrayMap.put(metricKey, Long.valueOf(SystemClock.elapsedRealtime() + arrayMap.get(metricKey).longValue()));
        }
    }

    private static void metricShutdownStart() {
        ArrayMap<String, Long> arrayMap = TRON_METRICS;
        synchronized (arrayMap) {
            arrayMap.put(METRIC_SHUTDOWN_TIME_START, Long.valueOf(System.currentTimeMillis()));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setRebootProgress(final int progress, final CharSequence message) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.power.ShutdownThread.4
            @Override // java.lang.Runnable
            public void run() {
                if (ShutdownThread.this.mProgressDialog != null) {
                    ShutdownThread.this.mProgressDialog.setProgress(progress);
                    if (message != null) {
                        ShutdownThread.this.mProgressDialog.setMessage(message);
                    }
                }
            }
        });
    }

    private void shutdownRadios(final int timeout) {
        final long endTime = SystemClock.elapsedRealtime() + timeout;
        final boolean[] done = new boolean[1];
        Thread t = new Thread() { // from class: com.android.server.power.ShutdownThread.5
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                TimingsTraceLog shutdownTimingsTraceLog = ShutdownThread.m6184$$Nest$smnewTimingsLog();
                TelephonyManager telephonyManager = (TelephonyManager) ShutdownThread.this.mContext.getSystemService(TelephonyManager.class);
                boolean radioOff = telephonyManager == null || !telephonyManager.isAnyRadioPoweredOn();
                if (!radioOff) {
                    Log.w(ShutdownThread.TAG, "Turning off cellular radios...");
                    ShutdownThread.metricStarted(ShutdownThread.METRIC_RADIO);
                    telephonyManager.shutdownAllRadios();
                }
                Log.i(ShutdownThread.TAG, "Waiting for Radio...");
                long delay = endTime - SystemClock.elapsedRealtime();
                while (delay > 0) {
                    if (ShutdownThread.mRebootHasProgressBar) {
                        int i = timeout;
                        int status = (int) ((((i - delay) * 1.0d) * 12.0d) / i);
                        ShutdownThread.sInstance.setRebootProgress(status + 6, null);
                    }
                    if (!radioOff && (!telephonyManager.isAnyRadioPoweredOn())) {
                        Log.i(ShutdownThread.TAG, "Radio turned off.");
                        ShutdownThread.metricEnded(ShutdownThread.METRIC_RADIO);
                        shutdownTimingsTraceLog.logDuration("ShutdownRadio", ((Long) ShutdownThread.TRON_METRICS.get(ShutdownThread.METRIC_RADIO)).longValue());
                    }
                    if (radioOff) {
                        Log.i(ShutdownThread.TAG, "Radio shutdown complete.");
                        done[0] = true;
                        return;
                    }
                    SystemClock.sleep(100L);
                    delay = endTime - SystemClock.elapsedRealtime();
                }
            }
        };
        t.start();
        try {
            t.join(timeout);
        } catch (InterruptedException e) {
        }
        if (!done[0]) {
            Log.w(TAG, "Timed out waiting for Radio shutdown.");
        }
    }

    public static void rebootOrShutdown(Context context, boolean reboot, String reason) {
        if (reboot) {
            Log.i(TAG, "Rebooting, reason: " + reason);
            if (Build.TRANCARE_SUPPORT) {
                ITranShutdownThread.Instance().recordShutdown(context, reboot, reason);
            }
            PowerManagerService.lowLevelReboot(reason);
            Log.e(TAG, "Reboot failed, will attempt shutdown instead");
            reason = null;
        } else if (context != null) {
            try {
                new SystemVibrator(context).vibrate(500L, VIBRATION_ATTRIBUTES);
            } catch (Exception e) {
                Log.w(TAG, "Failed to vibrate during shutdown.", e);
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e2) {
            }
        }
        sInstance.mLowLevelShutdownSeq(context);
        Log.i(TAG, "Performing low-level shutdown...");
        if (Build.TRANCARE_SUPPORT) {
            ITranShutdownThread.Instance().recordShutdown(context, reboot, reason);
        }
        PowerManagerService.lowLevelShutdown(reason);
    }

    private static void saveMetrics(boolean reboot, String reason) {
        StringBuilder metricValue = new StringBuilder();
        metricValue.append("reboot:");
        metricValue.append(reboot ? "y" : "n");
        metricValue.append(",").append("reason:").append(reason);
        int metricsSize = TRON_METRICS.size();
        for (int i = 0; i < metricsSize; i++) {
            ArrayMap<String, Long> arrayMap = TRON_METRICS;
            String name = arrayMap.keyAt(i);
            long value = arrayMap.valueAt(i).longValue();
            if (value < 0) {
                Log.e(TAG, "metricEnded wasn't called for " + name);
            } else {
                metricValue.append(',').append(name).append(':').append(value);
            }
        }
        File tmp = new File("/data/system/shutdown-metrics.tmp");
        boolean saved = false;
        try {
            FileOutputStream fos = new FileOutputStream(tmp);
            fos.write(metricValue.toString().getBytes(StandardCharsets.UTF_8));
            saved = true;
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Cannot save shutdown metrics", e);
        }
        if (saved) {
            tmp.renameTo(new File("/data/system/shutdown-metrics.txt"));
        }
    }

    private void uncrypt() {
        Log.i(TAG, "Calling uncrypt and monitoring the progress...");
        final RecoverySystem.ProgressListener progressListener = new RecoverySystem.ProgressListener() { // from class: com.android.server.power.ShutdownThread.6
            @Override // android.os.RecoverySystem.ProgressListener
            public void onProgress(int status) {
                if (status < 0 || status >= 100) {
                    if (status == 100) {
                        CharSequence msg = ShutdownThread.this.mContext.getText(17041367);
                        ShutdownThread.sInstance.setRebootProgress(status, msg);
                        return;
                    }
                    return;
                }
                CharSequence msg2 = ShutdownThread.this.mContext.getText(17041365);
                ShutdownThread.sInstance.setRebootProgress(((int) ((status * 80.0d) / 100.0d)) + 20, msg2);
            }
        };
        final boolean[] done = {false};
        Thread t = new Thread() { // from class: com.android.server.power.ShutdownThread.7
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                RecoverySystem recoverySystem = (RecoverySystem) ShutdownThread.this.mContext.getSystemService("recovery");
                try {
                    String filename = FileUtils.readTextFile(RecoverySystem.UNCRYPT_PACKAGE_FILE, 0, null);
                    RecoverySystem.processPackage(ShutdownThread.this.mContext, new File(filename), progressListener);
                } catch (IOException e) {
                    Log.e(ShutdownThread.TAG, "Error uncrypting file", e);
                }
                done[0] = true;
            }
        };
        t.start();
        try {
            t.join(900000L);
        } catch (InterruptedException e) {
        }
        if (!done[0]) {
            Log.w(TAG, "Timed out waiting for uncrypt.");
            String timeoutMessage = String.format("uncrypt_time: %d\nuncrypt_error: %d\n", 900, 100);
            try {
                FileUtils.stringToFile(RecoverySystem.UNCRYPT_STATUS_FILE, timeoutMessage);
            } catch (IOException e2) {
                Log.e(TAG, "Failed to write timeout message to uncrypt status", e2);
            }
        }
    }

    protected boolean mIsShowShutdownSysui() {
        return true;
    }

    protected boolean mIsShowShutdownDialog(Context c) {
        return true;
    }

    protected boolean mStartShutdownSeq(Context c) {
        return true;
    }

    protected void mShutdownSeqFinish(Context c) {
    }

    protected void mLowLevelShutdownSeq(Context c) {
    }
}
