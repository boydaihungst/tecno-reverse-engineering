package com.android.server.power;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.os.Process;
import android.os.RemoteException;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
/* loaded from: classes2.dex */
public final class ShutdownCheckPoints {
    private static final int MAX_CHECK_POINTS = 100;
    private static final int MAX_DUMP_FILES = 20;
    private static final String TAG = "ShutdownCheckPoints";
    private final LinkedList<CheckPoint> mCheckPoints;
    private final Injector mInjector;
    private static final ShutdownCheckPoints INSTANCE = new ShutdownCheckPoints();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Injector {
        IActivityManager activityManager();

        long currentTimeMillis();

        int maxCheckPoints();

        int maxDumpFiles();
    }

    private ShutdownCheckPoints() {
        this(new Injector() { // from class: com.android.server.power.ShutdownCheckPoints.1
            @Override // com.android.server.power.ShutdownCheckPoints.Injector
            public long currentTimeMillis() {
                return System.currentTimeMillis();
            }

            @Override // com.android.server.power.ShutdownCheckPoints.Injector
            public int maxCheckPoints() {
                return 100;
            }

            @Override // com.android.server.power.ShutdownCheckPoints.Injector
            public int maxDumpFiles() {
                return 20;
            }

            @Override // com.android.server.power.ShutdownCheckPoints.Injector
            public IActivityManager activityManager() {
                return ActivityManager.getService();
            }
        });
    }

    ShutdownCheckPoints(Injector injector) {
        this.mCheckPoints = new LinkedList<>();
        this.mInjector = injector;
    }

    public static void recordCheckPoint(String reason) {
        INSTANCE.recordCheckPointInternal(reason);
    }

    public static void recordCheckPoint(int callerProcessId, String reason) {
        INSTANCE.recordCheckPointInternal(callerProcessId, reason);
    }

    public static void recordCheckPoint(String intentName, String packageName, String reason) {
        INSTANCE.recordCheckPointInternal(intentName, packageName, reason);
    }

    public static void dump(PrintWriter printWriter) {
        INSTANCE.dumpInternal(printWriter);
    }

    public static Thread newDumpThread(File baseFile) {
        return INSTANCE.newDumpThreadInternal(baseFile);
    }

    void recordCheckPointInternal(String reason) {
        recordCheckPointInternal(new SystemServerCheckPoint(this.mInjector, reason));
        Slog.v(TAG, "System server shutdown checkpoint recorded");
    }

    void recordCheckPointInternal(int callerProcessId, String reason) {
        CheckPoint binderCheckPoint;
        if (callerProcessId == Process.myPid()) {
            binderCheckPoint = new SystemServerCheckPoint(this.mInjector, reason);
        } else {
            binderCheckPoint = new BinderCheckPoint(this.mInjector, callerProcessId, reason);
        }
        recordCheckPointInternal(binderCheckPoint);
        Slog.v(TAG, "Binder shutdown checkpoint recorded with pid=" + callerProcessId);
    }

    void recordCheckPointInternal(String intentName, String packageName, String reason) {
        CheckPoint intentCheckPoint;
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName)) {
            intentCheckPoint = new SystemServerCheckPoint(this.mInjector, reason);
        } else {
            intentCheckPoint = new IntentCheckPoint(this.mInjector, intentName, packageName, reason);
        }
        recordCheckPointInternal(intentCheckPoint);
        Slog.v(TAG, String.format("Shutdown intent checkpoint recorded intent=%s from package=%s", intentName, packageName));
    }

    private void recordCheckPointInternal(CheckPoint checkPoint) {
        synchronized (this.mCheckPoints) {
            this.mCheckPoints.addLast(checkPoint);
            if (this.mCheckPoints.size() > this.mInjector.maxCheckPoints()) {
                this.mCheckPoints.removeFirst();
            }
        }
    }

    void dumpInternal(PrintWriter printWriter) {
        List<CheckPoint> records;
        synchronized (this.mCheckPoints) {
            records = new ArrayList<>(this.mCheckPoints);
        }
        for (CheckPoint record : records) {
            record.dump(printWriter);
            printWriter.println();
        }
    }

    Thread newDumpThreadInternal(File baseFile) {
        return new FileDumperThread(this, baseFile, this.mInjector.maxDumpFiles());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static abstract class CheckPoint {
        private final String mReason;
        private final long mTimestamp;

        abstract void dumpDetails(PrintWriter printWriter);

        abstract String getOrigin();

        CheckPoint(Injector injector, String reason) {
            this.mTimestamp = injector.currentTimeMillis();
            this.mReason = reason;
        }

        final void dump(PrintWriter printWriter) {
            printWriter.print("Shutdown request from ");
            printWriter.print(getOrigin());
            if (this.mReason != null) {
                printWriter.print(" for reason ");
                printWriter.print(this.mReason);
            }
            printWriter.print(" at ");
            printWriter.print(ShutdownCheckPoints.DATE_FORMAT.format(new Date(this.mTimestamp)));
            printWriter.println(" (epoch=" + this.mTimestamp + ")");
            dumpDetails(printWriter);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class SystemServerCheckPoint extends CheckPoint {
        private final StackTraceElement[] mStackTraceElements;

        SystemServerCheckPoint(Injector injector, String reason) {
            super(injector, reason);
            this.mStackTraceElements = Thread.currentThread().getStackTrace();
        }

        @Override // com.android.server.power.ShutdownCheckPoints.CheckPoint
        String getOrigin() {
            return "SYSTEM";
        }

        @Override // com.android.server.power.ShutdownCheckPoints.CheckPoint
        void dumpDetails(PrintWriter printWriter) {
            String methodName = getMethodName();
            printWriter.println(methodName == null ? "Failed to get method name" : methodName);
            printStackTrace(printWriter);
        }

        String getMethodName() {
            int idx = findCallSiteIndex();
            StackTraceElement[] stackTraceElementArr = this.mStackTraceElements;
            if (idx < stackTraceElementArr.length) {
                StackTraceElement element = stackTraceElementArr[idx];
                return String.format("%s.%s", element.getClassName(), element.getMethodName());
            }
            return null;
        }

        void printStackTrace(PrintWriter printWriter) {
            int i = findCallSiteIndex();
            while (true) {
                i++;
                if (i < this.mStackTraceElements.length) {
                    printWriter.print(" at ");
                    printWriter.println(this.mStackTraceElements[i]);
                } else {
                    return;
                }
            }
        }

        private int findCallSiteIndex() {
            String className = ShutdownCheckPoints.class.getCanonicalName();
            int idx = 0;
            while (true) {
                StackTraceElement[] stackTraceElementArr = this.mStackTraceElements;
                if (idx >= stackTraceElementArr.length || stackTraceElementArr[idx].getClassName().equals(className)) {
                    break;
                }
                idx++;
            }
            while (true) {
                StackTraceElement[] stackTraceElementArr2 = this.mStackTraceElements;
                if (idx >= stackTraceElementArr2.length || !stackTraceElementArr2[idx].getClassName().equals(className)) {
                    break;
                }
                idx++;
            }
            return idx;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class BinderCheckPoint extends SystemServerCheckPoint {
        private final IActivityManager mActivityManager;
        private final int mCallerProcessId;

        BinderCheckPoint(Injector injector, int callerProcessId, String reason) {
            super(injector, reason);
            this.mCallerProcessId = callerProcessId;
            this.mActivityManager = injector.activityManager();
        }

        @Override // com.android.server.power.ShutdownCheckPoints.SystemServerCheckPoint, com.android.server.power.ShutdownCheckPoints.CheckPoint
        String getOrigin() {
            return "BINDER";
        }

        @Override // com.android.server.power.ShutdownCheckPoints.SystemServerCheckPoint, com.android.server.power.ShutdownCheckPoints.CheckPoint
        void dumpDetails(PrintWriter printWriter) {
            String methodName = getMethodName();
            printWriter.println(methodName == null ? "Failed to get method name" : methodName);
            String processName = getProcessName();
            printWriter.print("From process ");
            printWriter.print(processName == null ? "?" : processName);
            printWriter.println(" (pid=" + this.mCallerProcessId + ")");
        }

        String getProcessName() {
            try {
                List<ActivityManager.RunningAppProcessInfo> runningProcesses = this.mActivityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    if (processInfo.pid == this.mCallerProcessId) {
                        return processInfo.processName;
                    }
                }
                return null;
            } catch (RemoteException e) {
                Slog.e(ShutdownCheckPoints.TAG, "Failed to get running app processes from ActivityManager", e);
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class IntentCheckPoint extends CheckPoint {
        private final String mIntentName;
        private final String mPackageName;

        IntentCheckPoint(Injector injector, String intentName, String packageName, String reason) {
            super(injector, reason);
            this.mIntentName = intentName;
            this.mPackageName = packageName;
        }

        @Override // com.android.server.power.ShutdownCheckPoints.CheckPoint
        String getOrigin() {
            return "INTENT";
        }

        @Override // com.android.server.power.ShutdownCheckPoints.CheckPoint
        void dumpDetails(PrintWriter printWriter) {
            printWriter.print("Intent: ");
            printWriter.println(this.mIntentName);
            printWriter.print("Package: ");
            printWriter.println(this.mPackageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class FileDumperThread extends Thread {
        private final File mBaseFile;
        private final int mFileCountLimit;
        private final ShutdownCheckPoints mInstance;

        FileDumperThread(ShutdownCheckPoints instance, File baseFile, int fileCountLimit) {
            this.mInstance = instance;
            this.mBaseFile = baseFile;
            this.mFileCountLimit = fileCountLimit;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            this.mBaseFile.getParentFile().mkdirs();
            File[] checkPointFiles = listCheckPointsFiles();
            int filesToDelete = (checkPointFiles.length - this.mFileCountLimit) + 1;
            for (int i = 0; i < filesToDelete; i++) {
                checkPointFiles[i].delete();
            }
            File nextCheckPointsFile = new File(String.format("%s-%d", this.mBaseFile.getAbsolutePath(), Long.valueOf(System.currentTimeMillis())));
            writeCheckpoints(nextCheckPointsFile);
        }

        private File[] listCheckPointsFiles() {
            final String filePrefix = this.mBaseFile.getName() + "-";
            File[] files = this.mBaseFile.getParentFile().listFiles(new FilenameFilter() { // from class: com.android.server.power.ShutdownCheckPoints.FileDumperThread.1
                @Override // java.io.FilenameFilter
                public boolean accept(File dir, String name) {
                    if (name.startsWith(filePrefix)) {
                        try {
                            Long.valueOf(name.substring(filePrefix.length()));
                            return true;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                    return false;
                }
            });
            Arrays.sort(files);
            return files;
        }

        private void writeCheckpoints(File file) {
            AtomicFile tmpFile = new AtomicFile(this.mBaseFile);
            FileOutputStream fos = null;
            try {
                fos = tmpFile.startWrite();
                PrintWriter pw = new PrintWriter(fos);
                this.mInstance.dumpInternal(pw);
                pw.flush();
                tmpFile.finishWrite(fos);
            } catch (IOException e) {
                Log.e(ShutdownCheckPoints.TAG, "Failed to write shutdown checkpoints", e);
                if (fos != null) {
                    tmpFile.failWrite(fos);
                }
            }
            this.mBaseFile.renameTo(file);
        }
    }
}
