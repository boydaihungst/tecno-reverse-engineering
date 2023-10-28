package com.android.server;

import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.FileUtils;
import android.os.MessageQueue;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Downloads;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.XmlUtils;
import com.android.server.am.DropboxRateLimiter;
import com.android.server.am.HostingRecord;
import com.android.server.health.HealthServiceWrapperHidl;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.policy.PhoneWindowManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class BootReceiver extends BroadcastReceiver {
    private static final String ERROR_REPORT_TRACE_PIPE = "/sys/kernel/tracing/instances/bootreceiver/trace_pipe";
    private static final String FSCK_FS_MODIFIED = "FILE SYSTEM WAS MODIFIED";
    private static final String FSCK_PASS_PATTERN = "Pass ([1-9]E?):";
    private static final String FSCK_TREE_OPTIMIZATION_PATTERN = "Inode [0-9]+ extent tree.*could be shorter";
    private static final int FS_STAT_FS_FIXED = 1024;
    private static final String FS_STAT_PATTERN = "fs_stat,[^,]*/([^/,]+),(0x[0-9a-fA-F]+)";
    private static final int GMSCORE_LASTK_LOG_SIZE = 196608;
    private static final int LASTK_LOG_SIZE;
    private static final String LAST_HEADER_FILE = "last-header.txt";
    private static final String[] LAST_KMSG_FILES;
    private static final String LAST_SHUTDOWN_TIME_PATTERN = "powerctl_shutdown_time_ms:([0-9]+):([0-9]+)";
    private static final String LOG_FILES_FILE = "log-files.xml";
    private static final int LOG_SIZE;
    private static final int MAX_ERROR_REPORTS = 8;
    private static final String METRIC_SHUTDOWN_TIME_START = "begin_shutdown";
    private static final String METRIC_SYSTEM_SERVER = "shutdown_system_server";
    private static final String[] MOUNT_DURATION_PROPS_POSTFIX;
    private static final String OLD_UPDATER_CLASS = "com.google.android.systemupdater.SystemUpdateReceiver";
    private static final String OLD_UPDATER_PACKAGE = "com.google.android.systemupdater";
    private static final String SHUTDOWN_METRICS_FILE = "/data/system/shutdown-metrics.txt";
    private static final String SHUTDOWN_TRON_METRICS_PREFIX = "shutdown_";
    private static final String TAG = "BootReceiver";
    private static final String TAG_TOMBSTONE = "SYSTEM_TOMBSTONE";
    private static final String TAG_TOMBSTONE_PROTO = "SYSTEM_TOMBSTONE_PROTO";
    private static final String TAG_TRUNCATED = "[[TRUNCATED]]\n";
    private static final int UMOUNT_STATUS_NOT_AVAILABLE = 4;
    private static boolean isSystemServerNativeCrash;
    private static final File lastHeaderFile;
    private static final DropboxRateLimiter sDropboxRateLimiter;
    private static final AtomicFile sFile;
    private static int sSentReports;

    static {
        LOG_SIZE = SystemProperties.getInt("ro.debuggable", 0) == 1 ? 98304 : 65536;
        LASTK_LOG_SIZE = SystemProperties.getInt("ro.debuggable", 0) == 1 ? GMSCORE_LASTK_LOG_SIZE : 65536;
        sFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), LOG_FILES_FILE), "log-files");
        lastHeaderFile = new File(Environment.getDataSystemDirectory(), LAST_HEADER_FILE);
        MOUNT_DURATION_PROPS_POSTFIX = new String[]{"early", HealthServiceWrapperHidl.INSTANCE_VENDOR, "late"};
        LAST_KMSG_FILES = new String[]{"/sys/fs/pstore/console-ramoops", "/proc/last_kmsg"};
        isSystemServerNativeCrash = false;
        sSentReports = 0;
        sDropboxRateLimiter = new DropboxRateLimiter();
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.BootReceiver$1] */
    @Override // android.content.BroadcastReceiver
    public void onReceive(final Context context, Intent intent) {
        new Thread() { // from class: com.android.server.BootReceiver.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                try {
                    BootReceiver.this.logBootEvents(context);
                } catch (Exception e) {
                    Slog.e(BootReceiver.TAG, "Can't log boot events", e);
                }
                boolean onlyCore = false;
                try {
                    try {
                        onlyCore = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
                    } catch (RemoteException e2) {
                    }
                    if (!onlyCore) {
                        BootReceiver.this.removeOldUpdatePackages(context);
                    }
                } catch (Exception e3) {
                    Slog.e(BootReceiver.TAG, "Can't remove old update packages", e3);
                }
            }
        }.start();
        try {
            FileDescriptor tracefd = Os.open(ERROR_REPORT_TRACE_PIPE, OsConstants.O_RDONLY, FrameworkStatsLog.NON_A11Y_TOOL_SERVICE_WARNING_REPORT);
            MessageQueue.OnFileDescriptorEventListener traceCallback = new MessageQueue.OnFileDescriptorEventListener() { // from class: com.android.server.BootReceiver.2
                final int mBufferSize = 1024;
                byte[] mTraceBuffer = new byte[1024];

                @Override // android.os.MessageQueue.OnFileDescriptorEventListener
                public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                    try {
                        int nbytes = Os.read(fd, this.mTraceBuffer, 0, 1024);
                        if (nbytes > 0) {
                            String readStr = new String(this.mTraceBuffer);
                            if (readStr.indexOf("\n") != -1 && BootReceiver.sSentReports < 8) {
                                SystemProperties.set("dmesgd.start", "1");
                                BootReceiver.sSentReports++;
                            }
                        }
                        return 1;
                    } catch (Exception e) {
                        Slog.wtf(BootReceiver.TAG, "Error watching for trace events", e);
                        return 0;
                    }
                }
            };
            IoThread.get().getLooper().getQueue().addOnFileDescriptorEventListener(tracefd, 1, traceCallback);
        } catch (ErrnoException e) {
            Slog.wtf(TAG, "Could not open /sys/kernel/tracing/instances/bootreceiver/trace_pipe", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeOldUpdatePackages(Context context) {
        Downloads.removeAllDownloadsByPackage(context, OLD_UPDATER_PACKAGE, OLD_UPDATER_CLASS);
    }

    private static String getPreviousBootHeaders() {
        try {
            return FileUtils.readTextFile(lastHeaderFile, 0, null);
        } catch (IOException e) {
            return null;
        }
    }

    private static String getCurrentBootHeaders() throws IOException {
        return new StringBuilder(512).append("Build: ").append(Build.FINGERPRINT).append("\n").append("Hardware: ").append(Build.BOARD).append("\n").append("Revision: ").append(SystemProperties.get("ro.revision", "")).append("\n").append("Bootloader: ").append(Build.BOOTLOADER).append("\n").append("Radio: ").append(Build.getRadioVersion()).append("\n").append("Kernel: ").append(FileUtils.readTextFile(new File("/proc/version"), 1024, "...\n")).append("\n").toString();
    }

    private static String getBootHeadersToLogAndUpdate() throws IOException {
        String oldHeaders = getPreviousBootHeaders();
        String newHeaders = getCurrentBootHeaders();
        try {
            FileUtils.stringToFile(lastHeaderFile, newHeaders);
        } catch (IOException e) {
            Slog.e(TAG, "Error writing " + lastHeaderFile, e);
        }
        if (oldHeaders == null) {
            return "isPrevious: false\n" + newHeaders;
        }
        return "isPrevious: true\n" + oldHeaders;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logBootEvents(Context ctx) throws IOException {
        String lastKmsgFooter;
        DropBoxManager db = (DropBoxManager) ctx.getSystemService("dropbox");
        String headers = getBootHeadersToLogAndUpdate();
        String bootReason = SystemProperties.get("ro.boot.bootreason", (String) null);
        String recovery = RecoverySystem.handleAftermath(ctx);
        if (recovery != null && db != null) {
            db.addText("SYSTEM_RECOVERY_LOG", headers + recovery);
        }
        if (bootReason == null) {
            lastKmsgFooter = "";
        } else {
            String lastKmsgFooter2 = new StringBuilder(512).append("\n").append("Boot info:\n").append("Last boot reason: ").append(bootReason).append("\n").toString();
            lastKmsgFooter = lastKmsgFooter2;
        }
        HashMap<String, Long> timestamps = readTimestamps();
        if (SystemProperties.getLong("ro.runtime.firstboot", 0L) == 0) {
            String now = Long.toString(System.currentTimeMillis());
            SystemProperties.set("ro.runtime.firstboot", now);
            if (db != null) {
                db.addText("SYSTEM_BOOT", headers);
            }
            int i = LASTK_LOG_SIZE;
            String str = lastKmsgFooter;
            addLastkToDropBox(db, timestamps, headers, str, "/proc/last_kmsg", -i, "SYSTEM_LAST_KMSG");
            addLastkToDropBox(db, timestamps, headers, str, "/sys/fs/pstore/console-ramoops", -i, "SYSTEM_LAST_KMSG");
            addLastkToDropBox(db, timestamps, headers, str, "/sys/fs/pstore/console-ramoops-0", -i, "SYSTEM_LAST_KMSG");
            int i2 = LOG_SIZE;
            addFileToDropBox(db, timestamps, headers, "/cache/recovery/log", -i2, "SYSTEM_RECOVERY_LOG");
            addFileToDropBox(db, timestamps, headers, "/cache/recovery/last_kmsg", -i2, "SYSTEM_RECOVERY_KMSG");
            addAuditErrorsToDropBox(db, timestamps, headers, -i2, "SYSTEM_AUDIT");
        } else if (db != null) {
            db.addText("SYSTEM_RESTART", headers);
        }
        logFsShutdownTime();
        logFsMountTime();
        addFsckErrorsToDropBoxAndLogFsStat(db, timestamps, headers, -LOG_SIZE, "SYSTEM_FSCK");
        logSystemServerShutdownTimeMetrics();
        writeTimestamps(timestamps);
    }

    public static void resetDropboxRateLimiter() {
        sDropboxRateLimiter.reset();
    }

    public static void addTombstoneToDropBox(Context ctx, File tombstone, boolean proto, String processName) {
        DropBoxManager db = (DropBoxManager) ctx.getSystemService(DropBoxManager.class);
        if (db == null) {
            Slog.e(TAG, "Can't log tombstone: DropBoxManager not available");
            return;
        }
        DropboxRateLimiter.RateLimitResult rateLimitResult = sDropboxRateLimiter.shouldRateLimit(TAG_TOMBSTONE, processName);
        if (rateLimitResult.shouldRateLimit()) {
            return;
        }
        HashMap<String, Long> timestamps = readTimestamps();
        try {
            if (proto) {
                if (recordFileTimestamp(tombstone, timestamps)) {
                    db.addFile(TAG_TOMBSTONE_PROTO, tombstone, 0);
                }
            } else {
                String headers = getBootHeadersToLogAndUpdate() + rateLimitResult.createHeader();
                addFileToDropBox(db, timestamps, headers, tombstone.getPath(), LOG_SIZE, TAG_TOMBSTONE);
            }
        } catch (IOException e) {
            Slog.e(TAG, "Can't log tombstone", e);
        }
        writeTimestamps(timestamps);
    }

    private static void addLastkToDropBox(DropBoxManager db, HashMap<String, Long> timestamps, String headers, String footers, String filename, int maxSize, String tag) throws IOException {
        int extraSize = headers.length() + TAG_TRUNCATED.length() + footers.length();
        if (LASTK_LOG_SIZE + extraSize > GMSCORE_LASTK_LOG_SIZE) {
            if (GMSCORE_LASTK_LOG_SIZE > extraSize) {
                maxSize = -(GMSCORE_LASTK_LOG_SIZE - extraSize);
            } else {
                maxSize = 0;
            }
        }
        addFileWithFootersToDropBox(db, timestamps, headers, footers, filename, maxSize, tag);
    }

    private static void addFileToDropBox(DropBoxManager db, HashMap<String, Long> timestamps, String headers, String filename, int maxSize, String tag) throws IOException {
        addFileWithFootersToDropBox(db, timestamps, headers, "", filename, maxSize, tag);
    }

    private static void addFileWithFootersToDropBox(DropBoxManager db, HashMap<String, Long> timestamps, String headers, String footers, String filename, int maxSize, String tag) throws IOException {
        if (db == null || !db.isTagEnabled(tag)) {
            return;
        }
        File file = new File(filename);
        if (!recordFileTimestamp(file, timestamps)) {
            return;
        }
        String fileContents = FileUtils.readTextFile(file, maxSize, TAG_TRUNCATED);
        String text = headers + fileContents + footers;
        isSystemServerNativeCrash = false;
        if (tag.equals(TAG_TOMBSTONE) && fileContents.contains(">>> system_server <<<")) {
            isSystemServerNativeCrash = true;
            addTextToDropBox(db, "system_server_native_crash", text, filename, maxSize);
        }
        if (tag.equals(TAG_TOMBSTONE)) {
            FrameworkStatsLog.write(186);
        }
        addTextToDropBox(db, tag, text, filename, maxSize);
    }

    private static boolean recordFileTimestamp(File file, HashMap<String, Long> timestamps) {
        long fileTime = file.lastModified();
        if (fileTime <= 0) {
            return false;
        }
        String filename = file.getPath();
        if (timestamps.containsKey(filename) && timestamps.get(filename).longValue() == fileTime) {
            return false;
        }
        timestamps.put(filename, Long.valueOf(fileTime));
        return true;
    }

    private static void addTextToDropBox(DropBoxManager db, String tag, String text, String filename, int maxSize) {
        Slog.i(TAG, "Copying " + filename + " to DropBox (" + tag + ")");
        if ("1".equals(SystemProperties.get("persist.sys.exceptionreport.support", "0")) && tag.equals(TAG_TOMBSTONE) && !isSystemServerNativeCrash) {
            File file = new File(filename);
            if (file.isFile() && file.getName().startsWith("tombstone_")) {
                try {
                    IActivityManager.Stub.asInterface(ServiceManager.getService(HostingRecord.HOSTING_TYPE_ACTIVITY)).reportNe(file.getPath());
                } catch (RemoteException e) {
                    Slog.e(TAG, "reportNe is error", e);
                }
            }
        }
        db.addText(tag, text);
        EventLog.writeEvent(81002, filename, Integer.valueOf(maxSize), tag);
    }

    private static void addAuditErrorsToDropBox(DropBoxManager db, HashMap<String, Long> timestamps, String headers, int maxSize, String tag) throws IOException {
        String[] split;
        if (db != null && db.isTagEnabled(tag)) {
            Slog.i(TAG, "Copying audit failures to DropBox");
            File file = new File("/proc/last_kmsg");
            long fileTime = file.lastModified();
            if (fileTime <= 0) {
                file = new File("/sys/fs/pstore/console-ramoops");
                fileTime = file.lastModified();
                if (fileTime <= 0) {
                    file = new File("/sys/fs/pstore/console-ramoops-0");
                    fileTime = file.lastModified();
                }
            }
            if (fileTime <= 0) {
                return;
            }
            if (timestamps.containsKey(tag) && timestamps.get(tag).longValue() == fileTime) {
                return;
            }
            timestamps.put(tag, Long.valueOf(fileTime));
            String log = FileUtils.readTextFile(file, maxSize, TAG_TRUNCATED);
            StringBuilder sb = new StringBuilder();
            for (String line : log.split("\n")) {
                if (line.contains("audit")) {
                    sb.append(line + "\n");
                }
            }
            Slog.i(TAG, "Copied " + sb.toString().length() + " worth of audits to DropBox");
            db.addText(tag, headers + sb.toString());
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:12:0x002e A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:13:0x002f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static void addFsckErrorsToDropBoxAndLogFsStat(DropBoxManager db, HashMap<String, Long> timestamps, String headers, int maxSize, String tag) throws IOException {
        boolean uploadEnabled;
        long fileTime;
        int lastFsStatLineNumber;
        if (db != null && db.isTagEnabled(tag)) {
            uploadEnabled = true;
            Slog.i(TAG, "Checking for fsck errors");
            File file = new File("/dev/fscklogs/log");
            fileTime = file.lastModified();
            if (fileTime > 0) {
                return;
            }
            String log = FileUtils.readTextFile(file, maxSize, TAG_TRUNCATED);
            Pattern pattern = Pattern.compile(FS_STAT_PATTERN);
            String[] lines = log.split("\n");
            int lastFsStatLineNumber2 = 0;
            int length = lines.length;
            boolean uploadNeeded = false;
            int i = 0;
            int lineNumber = 0;
            while (i < length) {
                String line = lines[i];
                int i2 = length;
                if (line.contains(FSCK_FS_MODIFIED)) {
                    uploadNeeded = true;
                } else {
                    if (!line.contains("fs_stat")) {
                        lastFsStatLineNumber = lastFsStatLineNumber2;
                    } else {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            handleFsckFsStat(matcher, lines, lastFsStatLineNumber2, lineNumber);
                            lastFsStatLineNumber2 = lineNumber;
                        } else {
                            lastFsStatLineNumber = lastFsStatLineNumber2;
                            Slog.w(TAG, "cannot parse fs_stat:" + line);
                        }
                    }
                    lastFsStatLineNumber2 = lastFsStatLineNumber;
                }
                lineNumber++;
                i++;
                length = i2;
            }
            if (uploadEnabled && uploadNeeded) {
                addFileToDropBox(db, timestamps, headers, "/dev/fscklogs/log", maxSize, tag);
            }
            file.delete();
            return;
        }
        uploadEnabled = false;
        Slog.i(TAG, "Checking for fsck errors");
        File file2 = new File("/dev/fscklogs/log");
        fileTime = file2.lastModified();
        if (fileTime > 0) {
        }
    }

    private static void logFsMountTime() {
        String[] strArr;
        int eventType;
        for (String propPostfix : MOUNT_DURATION_PROPS_POSTFIX) {
            int duration = SystemProperties.getInt("ro.boottime.init.mount_all." + propPostfix, 0);
            if (duration != 0) {
                char c = 65535;
                switch (propPostfix.hashCode()) {
                    case 3314342:
                        if (propPostfix.equals("late")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 96278371:
                        if (propPostfix.equals("early")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1544803905:
                        if (propPostfix.equals(HealthServiceWrapperHidl.INSTANCE_VENDOR)) {
                            c = 1;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        eventType = 11;
                        FrameworkStatsLog.write(239, eventType, duration);
                        break;
                    case 1:
                        eventType = 10;
                        FrameworkStatsLog.write(239, eventType, duration);
                        break;
                    case 2:
                        eventType = 12;
                        FrameworkStatsLog.write(239, eventType, duration);
                        break;
                }
            }
        }
    }

    private static void logSystemServerShutdownTimeMetrics() {
        File metricsFile = new File(SHUTDOWN_METRICS_FILE);
        String metricsStr = null;
        if (metricsFile.exists()) {
            try {
                metricsStr = FileUtils.readTextFile(metricsFile, 0, null);
            } catch (IOException e) {
                Slog.e(TAG, "Problem reading " + metricsFile, e);
            }
        }
        if (!TextUtils.isEmpty(metricsStr)) {
            String reboot = null;
            String reason = null;
            String start_time = null;
            String duration = null;
            String[] array = metricsStr.split(",");
            for (String keyValueStr : array) {
                String[] keyValue = keyValueStr.split(":");
                if (keyValue.length != 2) {
                    Slog.e(TAG, "Wrong format of shutdown metrics - " + metricsStr);
                } else {
                    if (keyValue[0].startsWith(SHUTDOWN_TRON_METRICS_PREFIX)) {
                        logTronShutdownMetric(keyValue[0], keyValue[1]);
                        if (keyValue[0].equals(METRIC_SYSTEM_SERVER)) {
                            duration = keyValue[1];
                        }
                    }
                    if (keyValue[0].equals("reboot")) {
                        reboot = keyValue[1];
                    } else if (keyValue[0].equals(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY)) {
                        reason = keyValue[1];
                    } else if (keyValue[0].equals(METRIC_SHUTDOWN_TIME_START)) {
                        start_time = keyValue[1];
                    }
                }
            }
            logStatsdShutdownAtom(reboot, reason, start_time, duration);
        }
        metricsFile.delete();
    }

    private static void logTronShutdownMetric(String metricName, String valueStr) {
        try {
            int value = Integer.parseInt(valueStr);
            if (value >= 0) {
                MetricsLogger.histogram((Context) null, metricName, value);
            }
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Cannot parse metric " + metricName + " int value - " + valueStr);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x0043  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x0046  */
    /* JADX WARN: Removed duplicated region for block: B:21:0x006d  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0094  */
    /* JADX WARN: Removed duplicated region for block: B:31:0x004d A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:33:0x0074 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static void logStatsdShutdownAtom(String rebootStr, String reasonStr, String startStr, String durationStr) {
        boolean reboot;
        String reason = "<EMPTY>";
        long start = 0;
        long duration = 0;
        if (rebootStr != null) {
            if (!rebootStr.equals("y")) {
                if (!rebootStr.equals("n")) {
                    Slog.e(TAG, "Unexpected value for reboot : " + rebootStr);
                }
            } else {
                reboot = true;
                if (reasonStr != null) {
                    Slog.e(TAG, "No value received for shutdown reason");
                } else {
                    reason = reasonStr;
                }
                if (startStr != null) {
                    Slog.e(TAG, "No value received for shutdown start time");
                } else {
                    try {
                        start = Long.parseLong(startStr);
                    } catch (NumberFormatException e) {
                        Slog.e(TAG, "Cannot parse shutdown start time: " + startStr);
                    }
                }
                if (durationStr != null) {
                    Slog.e(TAG, "No value received for shutdown duration");
                } else {
                    try {
                        duration = Long.parseLong(durationStr);
                    } catch (NumberFormatException e2) {
                        Slog.e(TAG, "Cannot parse shutdown duration: " + startStr);
                    }
                }
                FrameworkStatsLog.write(56, reboot, reason, start, duration);
            }
        } else {
            Slog.e(TAG, "No value received for reboot");
        }
        reboot = false;
        if (reasonStr != null) {
        }
        if (startStr != null) {
        }
        if (durationStr != null) {
        }
        FrameworkStatsLog.write(56, reboot, reason, start, duration);
    }

    private static void logFsShutdownTime() {
        File f = null;
        String[] strArr = LAST_KMSG_FILES;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String fileName = strArr[i];
            File file = new File(fileName);
            if (!file.exists()) {
                i++;
            } else {
                f = file;
                break;
            }
        }
        if (f == null) {
            return;
        }
        try {
            String lines = FileUtils.readTextFile(f, -16384, null);
            Pattern pattern = Pattern.compile(LAST_SHUTDOWN_TIME_PATTERN, 8);
            Matcher matcher = pattern.matcher(lines);
            if (matcher.find()) {
                FrameworkStatsLog.write(239, 9, Integer.parseInt(matcher.group(1)));
                FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ERROR_CODE_REPORTED, 2, Integer.parseInt(matcher.group(2)));
                Slog.i(TAG, "boot_fs_shutdown," + matcher.group(1) + "," + matcher.group(2));
                return;
            }
            FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ERROR_CODE_REPORTED, 2, 4);
            Slog.w(TAG, "boot_fs_shutdown, string not found");
        } catch (IOException e) {
            Slog.w(TAG, "cannot read last msg", e);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:23:0x0093, code lost:
        r8 = true;
        r9 = r14;
        r6 = r16;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x0148, code lost:
        r6 = r16;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static int fixFsckFsStat(String partition, int statOrg, String[] lines, int startLineNumber, int endLineNumber) {
        boolean foundQuotaFix;
        Pattern passPattern;
        if ((statOrg & 1024) != 0) {
            Pattern passPattern2 = Pattern.compile(FSCK_PASS_PATTERN);
            Pattern treeOptPattern = Pattern.compile(FSCK_TREE_OPTIMIZATION_PATTERN);
            String currentPass = "";
            boolean foundTreeOptimization = false;
            boolean foundQuotaFix2 = false;
            boolean foundTimestampAdjustment = false;
            boolean foundOtherFix = false;
            String otherFixLine = null;
            int i = startLineNumber;
            while (true) {
                if (i >= endLineNumber) {
                    foundQuotaFix = foundQuotaFix2;
                    break;
                }
                String line = lines[i];
                if (line.contains(FSCK_FS_MODIFIED)) {
                    foundQuotaFix = foundQuotaFix2;
                    break;
                }
                boolean foundQuotaFix3 = foundQuotaFix2;
                if (line.startsWith("Pass ")) {
                    Matcher matcher = passPattern2.matcher(line);
                    if (matcher.find()) {
                        currentPass = matcher.group(1);
                    }
                    passPattern = passPattern2;
                    foundQuotaFix2 = foundQuotaFix3;
                } else if (line.startsWith("Inode ")) {
                    if (!treeOptPattern.matcher(line).find() || !currentPass.equals("1")) {
                        break;
                    }
                    foundTreeOptimization = true;
                    Slog.i(TAG, "fs_stat, partition:" + partition + " found tree optimization:" + line);
                    passPattern = passPattern2;
                    foundQuotaFix2 = foundQuotaFix3;
                } else {
                    passPattern = passPattern2;
                    if (line.startsWith("[QUOTA WARNING]") && currentPass.equals("5")) {
                        Slog.i(TAG, "fs_stat, partition:" + partition + " found quota warning:" + line);
                        foundQuotaFix2 = true;
                        if (!foundTreeOptimization) {
                            otherFixLine = line;
                            break;
                        }
                    } else {
                        if (!line.startsWith("Update quota info") || !currentPass.equals("5")) {
                            if (line.startsWith("Timestamp(s) on inode") && line.contains("beyond 2310-04-04 are likely pre-1970") && currentPass.equals("1")) {
                                Slog.i(TAG, "fs_stat, partition:" + partition + " found timestamp adjustment:" + line);
                                if (lines[i + 1].contains("Fix? yes")) {
                                    i++;
                                }
                                foundTimestampAdjustment = true;
                                foundQuotaFix2 = foundQuotaFix3;
                            } else {
                                String line2 = line.trim();
                                if (!line2.isEmpty() && !currentPass.isEmpty()) {
                                    foundOtherFix = true;
                                    otherFixLine = line2;
                                    foundQuotaFix2 = foundQuotaFix3;
                                    break;
                                }
                            }
                        }
                        foundQuotaFix2 = foundQuotaFix3;
                    }
                }
                i++;
                passPattern2 = passPattern;
            }
            if (foundOtherFix) {
                if (otherFixLine != null) {
                    Slog.i(TAG, "fs_stat, partition:" + partition + " fix:" + otherFixLine);
                    return statOrg;
                }
                return statOrg;
            } else if (foundQuotaFix2 && !foundTreeOptimization) {
                Slog.i(TAG, "fs_stat, got quota fix without tree optimization, partition:" + partition);
                return statOrg;
            } else if ((foundTreeOptimization && foundQuotaFix2) || foundTimestampAdjustment) {
                Slog.i(TAG, "fs_stat, partition:" + partition + " fix ignored");
                int stat = statOrg & (-1025);
                return stat;
            } else {
                return statOrg;
            }
        }
        return statOrg;
    }

    private static void handleFsckFsStat(Matcher match, String[] lines, int startLineNumber, int endLineNumber) {
        String partition = match.group(1);
        try {
            int stat = fixFsckFsStat(partition, Integer.decode(match.group(2)).intValue(), lines, startLineNumber, endLineNumber);
            if ("userdata".equals(partition) || "data".equals(partition)) {
                FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ERROR_CODE_REPORTED, 3, stat);
            }
            Slog.i(TAG, "fs_stat, partition:" + partition + " stat:0x" + Integer.toHexString(stat));
        } catch (NumberFormatException e) {
            Slog.w(TAG, "cannot parse fs_stat: partition:" + partition + " stat:" + match.group(2));
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [866=8, 867=7] */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x0083, code lost:
        if (1 == 0) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x0138, code lost:
        if (0 != 0) goto L29;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static HashMap<String, Long> readTimestamps() {
        HashMap<String, Long> timestamps;
        int type;
        AtomicFile atomicFile = sFile;
        synchronized (atomicFile) {
            timestamps = new HashMap<>();
            try {
                FileInputStream stream = atomicFile.openRead();
                try {
                    TypedXmlPullParser parser = Xml.resolvePullParser(stream);
                    while (true) {
                        type = parser.next();
                        if (type == 2 || type == 1) {
                            break;
                        }
                    }
                    if (type != 2) {
                        throw new IllegalStateException("no start tag found");
                    }
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int type2 = parser.next();
                        if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                            break;
                        } else if (type2 != 3 && type2 != 4) {
                            String tagName = parser.getName();
                            if (tagName.equals("log")) {
                                String filename = parser.getAttributeValue((String) null, "filename");
                                long timestamp = parser.getAttributeLong((String) null, WatchlistLoggingHandler.WatchlistEventKeys.TIMESTAMP);
                                timestamps.put(filename, Long.valueOf(timestamp));
                            } else {
                                Slog.w(TAG, "Unknown tag: " + parser.getName());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                    if (stream != null) {
                        stream.close();
                    }
                } catch (Throwable th) {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e) {
                Slog.i(TAG, "No existing last log timestamp file " + sFile.getBaseFile() + "; starting empty");
            } catch (IOException e2) {
                Slog.w(TAG, "Failed parsing " + e2);
                if (0 == 0) {
                    timestamps.clear();
                }
                return timestamps;
            } catch (IllegalStateException e3) {
                Slog.w(TAG, "Failed parsing " + e3);
                if (0 == 0) {
                    timestamps.clear();
                }
                return timestamps;
            } catch (NullPointerException e4) {
                Slog.w(TAG, "Failed parsing " + e4);
                if (0 == 0) {
                    timestamps.clear();
                }
                return timestamps;
            } catch (XmlPullParserException e5) {
                Slog.w(TAG, "Failed parsing " + e5);
                if (0 == 0) {
                    timestamps.clear();
                }
                return timestamps;
            }
        }
        return timestamps;
    }

    private static void writeTimestamps(HashMap<String, Long> timestamps) {
        AtomicFile atomicFile = sFile;
        synchronized (atomicFile) {
            try {
                try {
                    FileOutputStream stream = atomicFile.startWrite();
                    try {
                        TypedXmlSerializer out = Xml.resolveSerializer(stream);
                        out.startDocument((String) null, true);
                        out.startTag((String) null, "log-files");
                        for (String filename : timestamps.keySet()) {
                            out.startTag((String) null, "log");
                            out.attribute((String) null, "filename", filename);
                            out.attributeLong((String) null, WatchlistLoggingHandler.WatchlistEventKeys.TIMESTAMP, timestamps.get(filename).longValue());
                            out.endTag((String) null, "log");
                        }
                        out.endTag((String) null, "log-files");
                        out.endDocument();
                        sFile.finishWrite(stream);
                    } catch (IOException e) {
                        Slog.w(TAG, "Failed to write timestamp file, using the backup: " + e);
                        sFile.failWrite(stream);
                    }
                } catch (IOException e2) {
                    Slog.w(TAG, "Failed to write timestamp file: " + e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }
}
