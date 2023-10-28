package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AnrController;
import android.app.ApplicationErrorReport;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.VersionedPackage;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.ProcessMap;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.PackageWatchdog;
import com.android.server.am.AppErrorDialog;
import com.android.server.am.AppNotRespondingDialog;
import com.android.server.usage.AppStandbyInternal;
import com.android.server.wm.WindowProcessController;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import com.transsion.hubcore.server.lowStorage.ITranLowStorageManager;
import com.transsion.hubcore.server.lowStorage.ITranLowStorageService;
import defpackage.CompanionAppsPermissions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AppErrors {
    private static final String TAG = "ActivityManager";
    private ArraySet<String> mAppsNotReportingCrashes;
    private final Context mContext;
    private final PackageWatchdog mPackageWatchdog;
    private final ActivityManagerGlobalLock mProcLock;
    private final ActivityManagerService mService;
    private final ProcessMap<Long> mProcessCrashTimes = new ProcessMap<>();
    private final ProcessMap<Long> mProcessCrashTimesPersistent = new ProcessMap<>();
    private final ProcessMap<Long> mProcessCrashShowDialogTimes = new ProcessMap<>();
    private final ProcessMap<Pair<Long, Integer>> mProcessCrashCounts = new ProcessMap<>();
    private volatile ProcessMap<BadProcessInfo> mBadProcesses = new ProcessMap<>();
    private final Object mBadProcessLock = new Object();

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppErrors(Context context, ActivityManagerService service, PackageWatchdog watchdog) {
        context.assertRuntimeOverlayThemable();
        this.mService = service;
        this.mProcLock = service.mProcLock;
        this.mContext = context;
        this.mPackageWatchdog = watchdog;
    }

    public void resetState() {
        Slog.i(TAG, "Resetting AppErrors");
        synchronized (this.mBadProcessLock) {
            this.mAppsNotReportingCrashes.clear();
            this.mProcessCrashTimes.clear();
            this.mProcessCrashTimesPersistent.clear();
            this.mProcessCrashShowDialogTimes.clear();
            this.mProcessCrashCounts.clear();
            this.mBadProcesses = new ProcessMap<>();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebugLPr(ProtoOutputStream proto, long fieldId, String dumpPackage) {
        long token;
        ArrayMap<String, SparseArray<Long>> pmap;
        String pname;
        SparseArray<Long> uids;
        ProcessMap<BadProcessInfo> badProcesses;
        String pname2;
        SparseArray<BadProcessInfo> uids2;
        int uidCount;
        int processCount;
        long token2;
        AppErrors appErrors = this;
        ProcessMap<BadProcessInfo> badProcesses2 = appErrors.mBadProcesses;
        if (appErrors.mProcessCrashTimes.getMap().isEmpty() && badProcesses2.getMap().isEmpty()) {
            return;
        }
        long token3 = proto.start(fieldId);
        long now = SystemClock.uptimeMillis();
        proto.write(1112396529665L, now);
        if (badProcesses2.getMap().isEmpty()) {
            token = token3;
        } else {
            ArrayMap<String, SparseArray<BadProcessInfo>> pmap2 = badProcesses2.getMap();
            int processCount2 = pmap2.size();
            int ip = 0;
            while (ip < processCount2) {
                long btoken = proto.start(2246267895811L);
                String pname3 = pmap2.keyAt(ip);
                SparseArray<BadProcessInfo> uids3 = pmap2.valueAt(ip);
                int uidCount2 = uids3.size();
                long now2 = now;
                proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, pname3);
                int i = 0;
                while (i < uidCount2) {
                    int puid = uids3.keyAt(i);
                    ArrayMap<String, SparseArray<BadProcessInfo>> pmap3 = pmap2;
                    ProcessRecord r = (ProcessRecord) appErrors.mService.getProcessNamesLOSP().get(pname3, puid);
                    if (dumpPackage != null) {
                        if (r == null) {
                            badProcesses = badProcesses2;
                            token2 = token3;
                            pname2 = pname3;
                            uids2 = uids3;
                            uidCount = uidCount2;
                            processCount = processCount2;
                        } else {
                            badProcesses = badProcesses2;
                            if (!r.getPkgList().containsKey(dumpPackage)) {
                                token2 = token3;
                                pname2 = pname3;
                                uids2 = uids3;
                                uidCount = uidCount2;
                                processCount = processCount2;
                            }
                        }
                        i++;
                        pmap2 = pmap3;
                        badProcesses2 = badProcesses;
                        pname3 = pname2;
                        uids3 = uids2;
                        processCount2 = processCount;
                        uidCount2 = uidCount;
                        token3 = token2;
                    } else {
                        badProcesses = badProcesses2;
                    }
                    BadProcessInfo info = uids3.valueAt(i);
                    pname2 = pname3;
                    uids2 = uids3;
                    uidCount = uidCount2;
                    processCount = processCount2;
                    long etoken = proto.start(2246267895810L);
                    proto.write(CompanionMessage.MESSAGE_ID, puid);
                    token2 = token3;
                    proto.write(1112396529666L, info.time);
                    proto.write(1138166333443L, info.shortMsg);
                    proto.write(1138166333444L, info.longMsg);
                    proto.write(1138166333445L, info.stack);
                    proto.end(etoken);
                    i++;
                    pmap2 = pmap3;
                    badProcesses2 = badProcesses;
                    pname3 = pname2;
                    uids3 = uids2;
                    processCount2 = processCount;
                    uidCount2 = uidCount;
                    token3 = token2;
                }
                proto.end(btoken);
                ip++;
                now = now2;
            }
            token = token3;
        }
        synchronized (appErrors.mBadProcessLock) {
            try {
                if (!appErrors.mProcessCrashTimes.getMap().isEmpty()) {
                    try {
                        ArrayMap<String, SparseArray<Long>> pmap4 = appErrors.mProcessCrashTimes.getMap();
                        int procCount = pmap4.size();
                        int ip2 = 0;
                        while (ip2 < procCount) {
                            long ctoken = proto.start(2246267895810L);
                            String pname4 = pmap4.keyAt(ip2);
                            SparseArray<Long> uids4 = pmap4.valueAt(ip2);
                            int uidCount3 = uids4.size();
                            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, pname4);
                            int i2 = 0;
                            while (i2 < uidCount3) {
                                int puid2 = uids4.keyAt(i2);
                                ProcessRecord r2 = (ProcessRecord) appErrors.mService.getProcessNamesLOSP().get(pname4, puid2);
                                if (dumpPackage != null) {
                                    if (r2 == null) {
                                        pmap = pmap4;
                                        pname = pname4;
                                        uids = uids4;
                                    } else if (!r2.getPkgList().containsKey(dumpPackage)) {
                                        pmap = pmap4;
                                        pname = pname4;
                                        uids = uids4;
                                    }
                                    i2++;
                                    appErrors = this;
                                    pmap4 = pmap;
                                    pname4 = pname;
                                    uids4 = uids;
                                }
                                pmap = pmap4;
                                long etoken2 = proto.start(2246267895810L);
                                proto.write(CompanionMessage.MESSAGE_ID, puid2);
                                pname = pname4;
                                uids = uids4;
                                proto.write(1112396529666L, uids4.valueAt(i2).longValue());
                                proto.end(etoken2);
                                i2++;
                                appErrors = this;
                                pmap4 = pmap;
                                pname4 = pname;
                                uids4 = uids;
                            }
                            ArrayMap<String, SparseArray<Long>> pmap5 = pmap4;
                            proto.end(ctoken);
                            ip2++;
                            appErrors = this;
                            pmap4 = pmap5;
                        }
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
                }
                proto.end(token);
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [310=5] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpLPr(FileDescriptor fd, PrintWriter pw, boolean needSep, String dumpPackage) {
        boolean needSep2;
        long now;
        int processCount;
        boolean needSep3;
        int processCount2;
        AppErrors appErrors = this;
        String str = dumpPackage;
        long now2 = SystemClock.uptimeMillis();
        synchronized (appErrors.mBadProcessLock) {
            try {
                if (appErrors.mProcessCrashTimes.getMap().isEmpty()) {
                    needSep2 = needSep;
                } else {
                    boolean printed = false;
                    try {
                        ArrayMap<String, SparseArray<Long>> pmap = appErrors.mProcessCrashTimes.getMap();
                        int processCount3 = pmap.size();
                        needSep2 = needSep;
                        for (int ip = 0; ip < processCount3; ip++) {
                            try {
                                String pname = pmap.keyAt(ip);
                                SparseArray<Long> uids = pmap.valueAt(ip);
                                int uidCount = uids.size();
                                int i = 0;
                                while (i < uidCount) {
                                    int puid = uids.keyAt(i);
                                    ArrayMap<String, SparseArray<Long>> pmap2 = pmap;
                                    ProcessRecord r = (ProcessRecord) appErrors.mService.getProcessNamesLOSP().get(pname, puid);
                                    try {
                                        if (str != null) {
                                            if (r != null) {
                                                processCount2 = processCount3;
                                                if (!r.getPkgList().containsKey(str)) {
                                                }
                                            } else {
                                                processCount2 = processCount3;
                                            }
                                            i++;
                                            pmap = pmap2;
                                            processCount3 = processCount2;
                                        } else {
                                            processCount2 = processCount3;
                                        }
                                        pw.print("    Process ");
                                        pw.print(pname);
                                        pw.print(" uid ");
                                        pw.print(puid);
                                        pw.print(": last crashed ");
                                        needSep2 = needSep3;
                                        TimeUtils.formatDuration(now2 - uids.valueAt(i).longValue(), pw);
                                        pw.println(" ago");
                                        i++;
                                        pmap = pmap2;
                                        processCount3 = processCount2;
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
                                    if (printed) {
                                        needSep3 = needSep2;
                                    } else {
                                        if (needSep2) {
                                            pw.println();
                                        }
                                        needSep3 = true;
                                        try {
                                            pw.println("  Time since processes crashed:");
                                            printed = true;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            while (true) {
                                                break;
                                                break;
                                            }
                                            throw th;
                                        }
                                    }
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                }
                try {
                    if (!appErrors.mProcessCrashCounts.getMap().isEmpty()) {
                        boolean printed2 = false;
                        ArrayMap<String, SparseArray<Pair<Long, Integer>>> pmap3 = appErrors.mProcessCrashCounts.getMap();
                        int processCount4 = pmap3.size();
                        for (int ip2 = 0; ip2 < processCount4; ip2++) {
                            String pname2 = pmap3.keyAt(ip2);
                            SparseArray<Pair<Long, Integer>> uids2 = pmap3.valueAt(ip2);
                            int uidCount2 = uids2.size();
                            int i2 = 0;
                            while (i2 < uidCount2) {
                                int puid2 = uids2.keyAt(i2);
                                ArrayMap<String, SparseArray<Pair<Long, Integer>>> pmap4 = pmap3;
                                ProcessRecord r2 = (ProcessRecord) appErrors.mService.getProcessNamesLOSP().get(pname2, puid2);
                                if (str != null) {
                                    if (r2 != null) {
                                        processCount = processCount4;
                                        if (!r2.getPkgList().containsKey(str)) {
                                        }
                                    } else {
                                        processCount = processCount4;
                                    }
                                    i2++;
                                    pmap3 = pmap4;
                                    processCount4 = processCount;
                                } else {
                                    processCount = processCount4;
                                }
                                if (printed2) {
                                    needSep3 = needSep2;
                                } else {
                                    if (needSep2) {
                                        pw.println();
                                    }
                                    needSep3 = true;
                                    pw.println("  First time processes crashed and counts:");
                                    printed2 = true;
                                }
                                pw.print("    Process ");
                                pw.print(pname2);
                                pw.print(" uid ");
                                pw.print(puid2);
                                pw.print(": first crashed ");
                                needSep2 = needSep3;
                                TimeUtils.formatDuration(now2 - ((Long) uids2.valueAt(i2).first).longValue(), pw);
                                pw.print(" ago; crashes since then: ");
                                pw.println(uids2.valueAt(i2).second);
                                i2++;
                                pmap3 = pmap4;
                                processCount4 = processCount;
                            }
                        }
                    }
                    ProcessMap<BadProcessInfo> badProcesses = appErrors.mBadProcesses;
                    if (!badProcesses.getMap().isEmpty()) {
                        boolean printed3 = false;
                        ArrayMap<String, SparseArray<BadProcessInfo>> pmap5 = badProcesses.getMap();
                        int processCount5 = pmap5.size();
                        int ip3 = 0;
                        while (ip3 < processCount5) {
                            String pname3 = pmap5.keyAt(ip3);
                            SparseArray<BadProcessInfo> uids3 = pmap5.valueAt(ip3);
                            int uidCount3 = uids3.size();
                            int i3 = 0;
                            while (i3 < uidCount3) {
                                int puid3 = uids3.keyAt(i3);
                                ProcessMap<BadProcessInfo> badProcesses2 = badProcesses;
                                ProcessRecord r3 = (ProcessRecord) appErrors.mService.getProcessNamesLOSP().get(pname3, puid3);
                                if (str == null || (r3 != null && r3.getPkgList().containsKey(str))) {
                                    if (!printed3) {
                                        if (needSep2) {
                                            pw.println();
                                        }
                                        needSep2 = true;
                                        pw.println("  Bad processes:");
                                        printed3 = true;
                                    }
                                    BadProcessInfo info = uids3.valueAt(i3);
                                    pw.print("    Bad process ");
                                    pw.print(pname3);
                                    pw.print(" uid ");
                                    pw.print(puid3);
                                    pw.print(": crashed at time ");
                                    now = now2;
                                    pw.println(info.time);
                                    if (info.shortMsg != null) {
                                        pw.print("      Short msg: ");
                                        pw.println(info.shortMsg);
                                    }
                                    if (info.longMsg != null) {
                                        pw.print("      Long msg: ");
                                        pw.println(info.longMsg);
                                    }
                                    if (info.stack != null) {
                                        pw.println("      Stack:");
                                        int lastPos = 0;
                                        for (int pos = 0; pos < info.stack.length(); pos++) {
                                            if (info.stack.charAt(pos) == '\n') {
                                                pw.print("        ");
                                                pw.write(info.stack, lastPos, pos - lastPos);
                                                pw.println();
                                                lastPos = pos + 1;
                                            }
                                        }
                                        if (lastPos < info.stack.length()) {
                                            pw.print("        ");
                                            pw.write(info.stack, lastPos, info.stack.length() - lastPos);
                                            pw.println();
                                        }
                                    }
                                } else {
                                    now = now2;
                                }
                                i3++;
                                appErrors = this;
                                str = dumpPackage;
                                badProcesses = badProcesses2;
                                now2 = now;
                            }
                            ip3++;
                            appErrors = this;
                            str = dumpPackage;
                        }
                    }
                    return needSep2;
                } catch (Throwable th6) {
                    th = th6;
                    while (true) {
                        break;
                        break;
                    }
                    throw th;
                }
            } catch (Throwable th7) {
                th = th7;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBadProcess(String processName, int uid) {
        return this.mBadProcesses.get(processName, uid) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearBadProcess(String processName, int uid) {
        synchronized (this.mBadProcessLock) {
            ProcessMap<BadProcessInfo> badProcesses = new ProcessMap<>();
            badProcesses.putAll(this.mBadProcesses);
            badProcesses.remove(processName, uid);
            this.mBadProcesses = badProcesses;
        }
    }

    void markBadProcess(String processName, int uid, BadProcessInfo info) {
        synchronized (this.mBadProcessLock) {
            ProcessMap<BadProcessInfo> badProcesses = new ProcessMap<>();
            badProcesses.putAll(this.mBadProcesses);
            badProcesses.put(processName, uid, info);
            this.mBadProcesses = badProcesses;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetProcessCrashTime(String processName, int uid) {
        synchronized (this.mBadProcessLock) {
            this.mProcessCrashTimes.remove(processName, uid);
            this.mProcessCrashCounts.remove(processName, uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetProcessCrashTime(boolean resetEntireUser, int appId, int userId) {
        synchronized (this.mBadProcessLock) {
            ArrayMap<String, SparseArray<Long>> pTimeMap = this.mProcessCrashTimes.getMap();
            for (int ip = pTimeMap.size() - 1; ip >= 0; ip--) {
                SparseArray<Long> ba = pTimeMap.valueAt(ip);
                resetProcessCrashMapLBp(ba, resetEntireUser, appId, userId);
                if (ba.size() == 0) {
                    pTimeMap.removeAt(ip);
                }
            }
            ArrayMap<String, SparseArray<Pair<Long, Integer>>> pCountMap = this.mProcessCrashCounts.getMap();
            for (int ip2 = pCountMap.size() - 1; ip2 >= 0; ip2--) {
                SparseArray<Pair<Long, Integer>> ba2 = pCountMap.valueAt(ip2);
                resetProcessCrashMapLBp(ba2, resetEntireUser, appId, userId);
                if (ba2.size() == 0) {
                    pCountMap.removeAt(ip2);
                }
            }
        }
    }

    private void resetProcessCrashMapLBp(SparseArray<?> ba, boolean resetEntireUser, int appId, int userId) {
        for (int i = ba.size() - 1; i >= 0; i--) {
            boolean remove = false;
            int entUid = ba.keyAt(i);
            if (!resetEntireUser) {
                if (userId == -1) {
                    if (UserHandle.getAppId(entUid) == appId) {
                        remove = true;
                    }
                } else if (entUid == UserHandle.getUid(userId, appId)) {
                    remove = true;
                }
            } else if (UserHandle.getUserId(entUid) == userId) {
                remove = true;
            }
            if (remove) {
                ba.removeAt(i);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadAppsNotReportingCrashesFromConfig(String appsNotReportingCrashesConfig) {
        if (appsNotReportingCrashesConfig != null) {
            String[] split = appsNotReportingCrashesConfig.split(",");
            if (split.length > 0) {
                synchronized (this.mBadProcessLock) {
                    ArraySet<String> arraySet = new ArraySet<>();
                    this.mAppsNotReportingCrashes = arraySet;
                    Collections.addAll(arraySet, split);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killAppAtUserRequestLocked(ProcessRecord app) {
        ErrorDialogController controller = app.mErrorState.getDialogController();
        int reasonCode = 6;
        int subReason = 0;
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                if (controller.hasDebugWaitingDialog()) {
                    reasonCode = 13;
                    subReason = 1;
                }
                controller.clearAllErrorDialogs();
                killAppImmediateLSP(app, reasonCode, subReason, "user-terminated", "user request after error");
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }

    private void killAppImmediateLSP(ProcessRecord app, int reasonCode, int subReason, String reason, String killReason) {
        ProcessErrorStateRecord errState = app.mErrorState;
        errState.setCrashing(false);
        errState.setCrashingReport(null);
        errState.setNotResponding(false);
        errState.setNotRespondingReport(null);
        int pid = errState.mApp.getPid();
        if (pid > 0 && pid != ActivityManagerService.MY_PID) {
            synchronized (this.mBadProcessLock) {
                handleAppCrashLSPB(app, reason, null, null, null, null);
            }
            app.killLocked(killReason, reasonCode, subReason, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleAppCrashLocked(int uid, int initialPid, String packageName, int userId, String message, boolean force, int exceptionTypeId, Bundle extras) {
        ProcessRecord proc = null;
        synchronized (this.mService.mPidsSelfLocked) {
            int i = 0;
            while (true) {
                if (i >= this.mService.mPidsSelfLocked.size()) {
                    break;
                }
                ProcessRecord p = this.mService.mPidsSelfLocked.valueAt(i);
                if (uid < 0 || p.uid == uid) {
                    if (p.getPid() == initialPid) {
                        proc = p;
                        break;
                    } else if (p.getPkgList().containsKey(packageName) && (userId < 0 || p.userId == userId)) {
                        proc = p;
                    }
                }
                i++;
            }
        }
        if (proc == null) {
            Slog.w(TAG, "crashApplication: nothing for uid=" + uid + " initialPid=" + initialPid + " packageName=" + packageName + " userId=" + userId);
            return;
        }
        if (exceptionTypeId == 6) {
            String[] packages = proc.getPackageList();
            for (int i2 = 0; i2 < packages.length; i2++) {
                if (this.mService.mPackageManagerInt.isPackageStateProtected(packages[i2], proc.userId)) {
                    Slog.w(TAG, "crashApplication: Can not crash protected package " + packages[i2]);
                    return;
                }
            }
        }
        proc.scheduleCrashLocked(message, exceptionTypeId, extras);
        if (force) {
            final ProcessRecord p2 = proc;
            this.mService.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.AppErrors$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AppErrors.this.m1107lambda$scheduleAppCrashLocked$0$comandroidserveramAppErrors(p2);
                }
            }, 5000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleAppCrashLocked$0$com-android-server-am-AppErrors  reason: not valid java name */
    public /* synthetic */ void m1107lambda$scheduleAppCrashLocked$0$comandroidserveramAppErrors(ProcessRecord p) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                synchronized (this.mProcLock) {
                    ActivityManagerService.boostPriorityForProcLockedSection();
                    killAppImmediateLSP(p, 13, 14, "forced", "killed for invalid state");
                }
                ActivityManagerService.resetPriorityAfterProcLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void crashApplication(ProcessRecord r, ApplicationErrorReport.CrashInfo crashInfo) {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            crashApplicationInner(r, crashInfo, callingPid, callingUid);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    private void crashApplicationInner(ProcessRecord r, ApplicationErrorReport.CrashInfo crashInfo, int callingPid, int callingUid) {
        String longMsg;
        int i;
        ActivityManagerService activityManagerService;
        int i2;
        long timeMillis = System.currentTimeMillis();
        String shortMsg = crashInfo.exceptionClassName;
        String longMsg2 = crashInfo.exceptionMessage;
        String stackTrace = crashInfo.stackTrace;
        if (shortMsg != null && longMsg2 != null) {
            longMsg = shortMsg + ": " + longMsg2;
        } else if (shortMsg == null) {
            longMsg = longMsg2;
        } else {
            longMsg = shortMsg;
        }
        if (r != null) {
            this.mPackageWatchdog.onPackageFailure(r.getPackageListWithVersionCode(), 3);
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ProcessList processList = this.mService.mProcessList;
                    if (crashInfo != null && "Native crash".equals(crashInfo.exceptionClassName)) {
                        i2 = 5;
                    } else {
                        i2 = 4;
                    }
                    processList.noteAppKill(r, i2, 0, "crash");
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
        if (r == null) {
            i = 0;
        } else {
            i = r.getWindowProcessController().computeRelaunchReason();
        }
        int relaunchReason = i;
        BatteryStatsImpl.BatteryCallback appErrorResult = new AppErrorResult();
        ActivityManagerService activityManagerService2 = this.mService;
        synchronized (activityManagerService2) {
            try {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    activityManagerService = activityManagerService2;
                    String longMsg3 = longMsg;
                    BatteryStatsImpl.BatteryCallback batteryCallback = appErrorResult;
                    try {
                        try {
                            if (handleAppCrashInActivityController(r, crashInfo, shortMsg, longMsg, stackTrace, timeMillis, callingPid, callingUid)) {
                            } else if (relaunchReason == 2) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            } else if (r != null && r.getActiveInstrumentation() != null) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            } else {
                                if (r != null) {
                                    this.mService.mBatteryStatsService.noteProcessCrash(r.processName, r.uid);
                                }
                                AppErrorDialog.Data data = new AppErrorDialog.Data();
                                try {
                                    data.result = batteryCallback;
                                    data.proc = r;
                                    try {
                                        data.exceptionMsg = longMsg3;
                                        if (r != null && makeAppCrashingLocked(r, shortMsg, longMsg3, stackTrace, data)) {
                                            Message msg = Message.obtain();
                                            msg.what = 1;
                                            int taskId = data.taskId;
                                            msg.obj = data;
                                            this.mService.mUiHandler.sendMessage(msg);
                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                            int res = batteryCallback.get();
                                            Intent appErrorIntent = null;
                                            MetricsLogger.action(this.mContext, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_SHELL, res);
                                            switch ((res == 6 || res == 7) ? 1 : res) {
                                                case 1:
                                                    long orig = Binder.clearCallingIdentity();
                                                    try {
                                                        this.mService.mAtmInternal.onHandleAppCrash(r.getWindowProcessController());
                                                        if (!r.isPersistent()) {
                                                            try {
                                                                synchronized (this.mService) {
                                                                    try {
                                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                                        this.mService.mProcessList.removeProcessLocked(r, false, false, 4, "crash");
                                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                                        this.mService.mAtmInternal.resumeTopActivities(false);
                                                                    } catch (Throwable th) {
                                                                        th = th;
                                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                                        throw th;
                                                                    }
                                                                }
                                                            } catch (Throwable th2) {
                                                                th = th2;
                                                            }
                                                        }
                                                        break;
                                                    } finally {
                                                        Binder.restoreCallingIdentity(orig);
                                                    }
                                                case 2:
                                                    synchronized (this.mProcLock) {
                                                        try {
                                                            try {
                                                                ActivityManagerService.boostPriorityForProcLockedSection();
                                                                appErrorIntent = createAppErrorIntentLOSP(r, timeMillis, crashInfo);
                                                                ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                                break;
                                                            } catch (Throwable th3) {
                                                                th = th3;
                                                                ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                                throw th;
                                                            }
                                                        } catch (Throwable th4) {
                                                            th = th4;
                                                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                            throw th;
                                                        }
                                                    }
                                                case 3:
                                                    BatteryStatsImpl.BatteryCallback batteryCallback2 = this.mService;
                                                    synchronized (batteryCallback2) {
                                                        try {
                                                            try {
                                                                ActivityManagerService.boostPriorityForLockedSection();
                                                                this.mService.mProcessList.removeProcessLocked(r, false, true, 4, "crash");
                                                            } catch (Throwable th5) {
                                                                th = th5;
                                                                batteryCallback = batteryCallback2;
                                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                                                throw th;
                                                            }
                                                        } catch (Throwable th6) {
                                                            th = th6;
                                                        }
                                                    }
                                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                                    if (taskId == -1) {
                                                        break;
                                                    } else {
                                                        try {
                                                            this.mService.startActivityFromRecents(taskId, ActivityOptions.makeBasic().toBundle());
                                                        } catch (IllegalArgumentException e) {
                                                            Slog.e(TAG, "Could not restart taskId=" + taskId, e);
                                                        }
                                                        break;
                                                    }
                                                case 5:
                                                    synchronized (this.mBadProcessLock) {
                                                        stopReportingCrashesLBp(r);
                                                    }
                                                    break;
                                                case 8:
                                                    appErrorIntent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                                                    appErrorIntent.setData(Uri.parse("package:" + r.info.packageName));
                                                    appErrorIntent.addFlags(268435456);
                                                    break;
                                            }
                                            if (appErrorIntent != null) {
                                                try {
                                                    this.mContext.startActivityAsUser(appErrorIntent, new UserHandle(r.userId));
                                                    return;
                                                } catch (ActivityNotFoundException e2) {
                                                    Slog.w(TAG, "bug report receiver dissappeared", e2);
                                                    return;
                                                }
                                            }
                                            return;
                                        }
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                    } catch (Throwable th7) {
                                        th = th7;
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                            }
                        } catch (Throwable th9) {
                            th = th9;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                    }
                } catch (Throwable th11) {
                    th = th11;
                }
            } catch (Throwable th12) {
                th = th12;
                activityManagerService = activityManagerService2;
            }
        }
    }

    private boolean handleAppCrashInActivityController(final ProcessRecord r, final ApplicationErrorReport.CrashInfo crashInfo, final String shortMsg, final String longMsg, final String stackTrace, long timeMillis, int callingPid, int callingUid) {
        final String name = r != null ? r.processName : null;
        final int pid = r != null ? r.getPid() : callingPid;
        final int uid = r != null ? r.info.uid : callingUid;
        return this.mService.mAtmInternal.handleAppCrashInActivityController(name, pid, shortMsg, longMsg, timeMillis, crashInfo.stackTrace, new Runnable() { // from class: com.android.server.am.AppErrors$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AppErrors.this.m1105x21578eaf(crashInfo, name, pid, r, shortMsg, longMsg, stackTrace, uid);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleAppCrashInActivityController$1$com-android-server-am-AppErrors  reason: not valid java name */
    public /* synthetic */ void m1105x21578eaf(ApplicationErrorReport.CrashInfo crashInfo, String name, int pid, ProcessRecord r, String shortMsg, String longMsg, String stackTrace, int uid) {
        if (Build.IS_DEBUGGABLE && "Native crash".equals(crashInfo.exceptionClassName)) {
            Slog.w(TAG, "Skip killing native crashed app " + name + "(" + pid + ") during testing");
            return;
        }
        Slog.w(TAG, "Force-killing crashed app " + name + " at watcher's request");
        if (r != null) {
            if (!makeAppCrashingLocked(r, shortMsg, longMsg, stackTrace, null)) {
                r.killLocked("crash", 4, true);
                return;
            }
            return;
        }
        Process.killProcess(pid);
        ProcessList.killProcessGroup(uid, pid);
        this.mService.mProcessList.noteAppKill(pid, uid, 4, 0, "crash");
    }

    private boolean makeAppCrashingLocked(ProcessRecord app, String shortMsg, String longMsg, String stackTrace, AppErrorDialog.Data data) {
        boolean handleAppCrashLSPB;
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                ProcessErrorStateRecord errState = app.mErrorState;
                errState.setCrashing(true);
                errState.setCrashingReport(generateProcessError(app, 1, null, shortMsg, longMsg, stackTrace));
                errState.startAppProblemLSP();
                app.getWindowProcessController().stopFreezingActivities();
                synchronized (this.mBadProcessLock) {
                    handleAppCrashLSPB = handleAppCrashLSPB(app, "force-crash", shortMsg, longMsg, stackTrace, data);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        return handleAppCrashLSPB;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManager.ProcessErrorStateInfo generateProcessError(ProcessRecord app, int condition, String activity, String shortMsg, String longMsg, String stackTrace) {
        ActivityManager.ProcessErrorStateInfo report = new ActivityManager.ProcessErrorStateInfo();
        report.condition = condition;
        report.processName = app.processName;
        report.pid = app.getPid();
        report.uid = app.info.uid;
        report.tag = activity;
        report.shortMsg = shortMsg;
        report.longMsg = longMsg;
        report.stackTrace = stackTrace;
        return report;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Intent createAppErrorIntentLOSP(ProcessRecord r, long timeMillis, ApplicationErrorReport.CrashInfo crashInfo) {
        ApplicationErrorReport report = createAppErrorReportLOSP(r, timeMillis, crashInfo);
        if (report == null) {
            return null;
        }
        Intent result = new Intent("android.intent.action.APP_ERROR");
        result.setComponent(r.mErrorState.getErrorReportReceiver());
        result.putExtra("android.intent.extra.BUG_REPORT", report);
        result.addFlags(268435456);
        return result;
    }

    private ApplicationErrorReport createAppErrorReportLOSP(ProcessRecord r, long timeMillis, ApplicationErrorReport.CrashInfo crashInfo) {
        ProcessErrorStateRecord errState = r.mErrorState;
        if (errState.getErrorReportReceiver() == null) {
            return null;
        }
        if (errState.isCrashing() || errState.isNotResponding() || errState.isForceCrashReport()) {
            ApplicationErrorReport report = new ApplicationErrorReport();
            report.packageName = r.info.packageName;
            report.installerPackageName = errState.getErrorReportReceiver().getPackageName();
            report.processName = r.processName;
            report.time = timeMillis;
            report.systemApp = (r.info.flags & 1) != 0;
            if (errState.isCrashing() || errState.isForceCrashReport()) {
                report.type = 1;
                report.crashInfo = crashInfo;
            } else if (errState.isNotResponding()) {
                ActivityManager.ProcessErrorStateInfo anrReport = errState.getNotRespondingReport();
                if (anrReport == null) {
                    return null;
                }
                report.type = 2;
                report.anrInfo = new ApplicationErrorReport.AnrInfo();
                report.anrInfo.activity = anrReport.tag;
                report.anrInfo.cause = anrReport.shortMsg;
                report.anrInfo.info = anrReport.longMsg;
            }
            return report;
        }
        return null;
    }

    /* JADX WARN: Removed duplicated region for block: B:65:0x01ef  */
    /* JADX WARN: Removed duplicated region for block: B:66:0x0209  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x0213  */
    /* JADX WARN: Removed duplicated region for block: B:72:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean handleAppCrashLSPB(ProcessRecord app, String reason, String shortMsg, String longMsg, String stackTrace, AppErrorDialog.Data data) {
        Long crashTime;
        Long crashTimePersistent;
        ProcessErrorStateRecord errState;
        long now;
        boolean isolated;
        WindowProcessController proc;
        int uid;
        ProcessErrorStateRecord errState2;
        boolean z;
        int userId;
        long now2 = SystemClock.uptimeMillis();
        boolean showBackground = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "anr_show_background", 0, this.mService.mUserController.getCurrentUserId()) != 0;
        String processName = app.processName;
        int uid2 = app.uid;
        int userId2 = app.userId;
        boolean isolated2 = app.isolated;
        boolean persistent = app.isPersistent();
        WindowProcessController proc2 = app.getWindowProcessController();
        ProcessErrorStateRecord errState3 = app.mErrorState;
        if (!app.isolated) {
            Long crashTime2 = (Long) this.mProcessCrashTimes.get(processName, uid2);
            crashTime = crashTime2;
            crashTimePersistent = (Long) this.mProcessCrashTimesPersistent.get(processName, uid2);
        } else {
            crashTime = null;
            crashTimePersistent = null;
        }
        boolean tryAgain = app.mServices.incServiceCrashCountLocked(now2);
        boolean quickCrash = crashTime != null && now2 < crashTime.longValue() + ((long) ActivityManagerConstants.MIN_CRASH_INTERVAL);
        if (quickCrash) {
            errState = errState3;
        } else if (!isProcOverCrashLimitLBp(app, now2)) {
            int affectedTaskId = this.mService.mAtmInternal.finishTopCrashedActivities(proc2, reason);
            if (data != null) {
                data.taskId = affectedTaskId;
            }
            if (data == null || crashTimePersistent == null) {
                isolated = isolated2;
                now = now2;
                uid = uid2;
                errState2 = errState3;
                proc = proc2;
            } else if (now2 >= crashTimePersistent.longValue() + ActivityManagerConstants.MIN_CRASH_INTERVAL) {
                isolated = isolated2;
                now = now2;
                uid = uid2;
                errState2 = errState3;
                proc = proc2;
            } else {
                data.repeating = true;
                isolated = isolated2;
                now = now2;
                uid = uid2;
                errState2 = errState3;
                proc = proc2;
            }
            if (data != null && tryAgain) {
                data.isRestartableForService = true;
            }
            if (proc.isHomeProcess() && proc.hasActivities() && (app.info.flags & 1) == 0) {
                proc.clearPackagePreferredForHomeActivities();
            }
            if (isolated) {
                int uid3 = uid;
                this.mProcessCrashTimes.put(processName, uid3, Long.valueOf(now));
                this.mProcessCrashTimesPersistent.put(processName, uid3, Long.valueOf(now));
                updateProcessCrashCountLBp(processName, uid3, now);
            }
            if (errState2.getCrashHandler() == null) {
                this.mService.mHandler.post(errState2.getCrashHandler());
                return true;
            }
            return true;
        } else {
            errState = errState3;
        }
        Slog.w(TAG, "Process " + processName + " has crashed too many times, killing! Reason: " + (quickCrash ? "crashed quickly" : "over process crash limit"));
        EventLog.writeEvent((int) EventLogTags.AM_PROCESS_CRASHED_TOO_MUCH, Integer.valueOf(userId2), processName, Integer.valueOf(uid2));
        this.mService.mAtmInternal.onHandleAppCrash(proc2);
        if (persistent) {
            isolated = isolated2;
            now = now2;
            uid = uid2;
            errState2 = errState;
            z = false;
            proc = proc2;
        } else {
            EventLog.writeEvent((int) EventLogTags.AM_PROC_BAD, Integer.valueOf(userId2), Integer.valueOf(uid2), processName);
            if (!isolated2) {
                uid = uid2;
                errState2 = errState;
                proc = proc2;
                isolated = isolated2;
                now = now2;
                userId = userId2;
                markBadProcess(processName, app.uid, new BadProcessInfo(now2, shortMsg, longMsg, stackTrace));
                this.mProcessCrashTimes.remove(processName, app.uid);
                this.mProcessCrashCounts.remove(processName, app.uid);
            } else {
                isolated = isolated2;
                now = now2;
                uid = uid2;
                errState2 = errState;
                proc = proc2;
                userId = userId2;
            }
            errState2.setBad(true);
            app.setRemoved(true);
            AppStandbyInternal appStandbyInternal = (AppStandbyInternal) LocalServices.getService(AppStandbyInternal.class);
            if (appStandbyInternal != null) {
                appStandbyInternal.restrictApp(app.info != null ? app.info.packageName : processName, userId, 4);
            }
            this.mService.mProcessList.removeProcessLocked(app, false, tryAgain, 4, "crash");
            z = false;
            this.mService.mAtmInternal.resumeTopActivities(false);
            if (!showBackground) {
                return false;
            }
        }
        this.mService.mAtmInternal.resumeTopActivities(z);
        if (data != null) {
            data.isRestartableForService = true;
        }
        if (proc.isHomeProcess()) {
            proc.clearPackagePreferredForHomeActivities();
        }
        if (isolated) {
        }
        if (errState2.getCrashHandler() == null) {
        }
    }

    private void updateProcessCrashCountLBp(String processName, int uid, long now) {
        Pair<Long, Integer> count;
        Pair<Long, Integer> count2 = (Pair) this.mProcessCrashCounts.get(processName, uid);
        if (count2 == null || ((Long) count2.first).longValue() + ActivityManagerConstants.PROCESS_CRASH_COUNT_RESET_INTERVAL < now) {
            count = new Pair<>(Long.valueOf(now), 1);
        } else {
            count = new Pair<>((Long) count2.first, Integer.valueOf(((Integer) count2.second).intValue() + 1));
        }
        this.mProcessCrashCounts.put(processName, uid, count);
    }

    private boolean isProcOverCrashLimitLBp(ProcessRecord app, long now) {
        Pair<Long, Integer> crashCount = (Pair) this.mProcessCrashCounts.get(app.processName, app.uid);
        return !app.isolated && crashCount != null && now < ((Long) crashCount.first).longValue() + ActivityManagerConstants.PROCESS_CRASH_COUNT_RESET_INTERVAL && ((Integer) crashCount.second).intValue() >= ActivityManagerConstants.PROCESS_CRASH_COUNT_LIMIT;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1112=6] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x01bd, code lost:
        if (r3.repeating != false) goto L112;
     */
    /* JADX WARN: Removed duplicated region for block: B:101:0x01b5 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:133:0x0248 A[Catch: all -> 0x0253, TryCatch #9 {all -> 0x0253, blocks: (B:133:0x0248, B:134:0x024d, B:117:0x01e2, B:119:0x01ec, B:120:0x01f8, B:125:0x0200, B:127:0x022c, B:129:0x0230, B:126:0x0221), top: B:180:0x01a4 }] */
    /* JADX WARN: Removed duplicated region for block: B:176:0x0180 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:92:0x0197  */
    /* JADX WARN: Removed duplicated region for block: B:97:0x01a6 A[ADDED_TO_REGION] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void handleShowAppErrorUi(Message msg) {
        final ProcessRecord proc;
        AppErrorResult res;
        boolean crashSilenced;
        Long crashShowErrorTime;
        boolean z;
        AppErrorDialog.Data data = (AppErrorDialog.Data) msg.obj;
        boolean showBackground = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "anr_show_background", 0, this.mService.mUserController.getCurrentUserId()) != 0;
        synchronized (this.mProcLock) {
            try {
                try {
                    ActivityManagerService.boostPriorityForProcLockedSection();
                    proc = data.proc;
                    res = data.result;
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            try {
                if (proc == null) {
                    Slog.e(TAG, "handleShowAppErrorUi: proc is null");
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    return;
                }
                ProcessErrorStateRecord errState = proc.mErrorState;
                int userId = proc.userId;
                if (errState.getDialogController().hasCrashDialogs()) {
                    Slog.e(TAG, "App already has crash dialog: " + proc);
                    if (res != null) {
                        res.set(AppErrorDialog.ALREADY_SHOWING);
                    }
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    return;
                }
                boolean isBackground = UserHandle.getAppId(proc.uid) >= 10000 && proc.getPid() != ActivityManagerService.MY_PID;
                int[] currentProfileIds = this.mService.mUserController.getCurrentProfileIds();
                int length = currentProfileIds.length;
                boolean isBackground2 = isBackground;
                for (int i = 0; i < length; i++) {
                    int profileId = currentProfileIds[i];
                    isBackground2 &= userId != profileId;
                }
                boolean isForeground = proc.isForegroundToUserLocked();
                if (!isForeground && !showBackground) {
                    Slog.w(TAG, "Skipping crash dialog of " + proc + ": background");
                    if (res != null) {
                        res.set(AppErrorDialog.BACKGROUND_USER);
                    }
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    return;
                }
                Long crashShowErrorTime2 = null;
                synchronized (this.mBadProcessLock) {
                    try {
                        if (!proc.isolated) {
                            try {
                                crashShowErrorTime2 = (Long) this.mProcessCrashShowDialogTimes.get(proc.processName, proc.uid);
                            } catch (Throwable th3) {
                                th = th3;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                }
                                throw th;
                            }
                        }
                        try {
                            final CtaManager manager = CtaManagerFactory.getInstance().makeCtaManager();
                            boolean showFirstCrash = manager.isCtaSupported() ? true : Settings.Global.getInt(this.mContext.getContentResolver(), "show_first_crash_dialog", 0) != 0;
                            try {
                                try {
                                    boolean showFirstCrashDevOption = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "show_first_crash_dialog_dev_option", 0, this.mService.mUserController.getCurrentUserId()) != 0;
                                    String str = proc.info.packageName;
                                    ArraySet<String> arraySet = this.mAppsNotReportingCrashes;
                                    try {
                                        try {
                                            try {
                                                if (arraySet != null) {
                                                    try {
                                                        if (arraySet.contains(proc.info.packageName)) {
                                                            crashSilenced = true;
                                                            ITranLowStorageService.Instance().init(this.mContext);
                                                            boolean isIoException = ITranLowStorageService.Instance().reprotPkgCurStateForIoExp(proc.info.packageName, data.exceptionMsg.toString());
                                                            long now = SystemClock.uptimeMillis();
                                                            if (crashShowErrorTime2 == null) {
                                                                try {
                                                                    crashShowErrorTime = crashShowErrorTime2;
                                                                    if (now < crashShowErrorTime2.longValue() + ActivityManagerConstants.MIN_CRASH_INTERVAL) {
                                                                        z = true;
                                                                        boolean shouldThottle = z;
                                                                        if (!this.mService.mAtmInternal.canShowErrorDialogs() || showBackground) {
                                                                            if (!crashSilenced || shouldThottle) {
                                                                                if (res != null) {
                                                                                    res.set(AppErrorDialog.CANT_SHOW);
                                                                                }
                                                                                ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                                                return;
                                                                            }
                                                                            if (!showFirstCrash && !showFirstCrashDevOption) {
                                                                                try {
                                                                                } catch (Throwable th5) {
                                                                                    th = th5;
                                                                                    while (true) {
                                                                                        break;
                                                                                        break;
                                                                                    }
                                                                                    throw th;
                                                                                }
                                                                            }
                                                                            String exceptionMsg = "";
                                                                            if (errState.getCrashingReport() != null) {
                                                                                exceptionMsg = errState.getCrashingReport().longMsg;
                                                                            }
                                                                            boolean display = manager.needShowMtkPermDialog(this.mContext, proc.uid, proc.info.packageName, exceptionMsg);
                                                                            if (display) {
                                                                                this.mService.mUiHandler.post(new Runnable() { // from class: com.android.server.am.AppErrors$$ExternalSyntheticLambda2
                                                                                    @Override // java.lang.Runnable
                                                                                    public final void run() {
                                                                                        AppErrors.this.m1106lambda$handleShowAppErrorUi$2$comandroidserveramAppErrors(manager, proc);
                                                                                    }
                                                                                });
                                                                                ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                                                return;
                                                                            }
                                                                            if (isIoException) {
                                                                                AppErrorResultWrap errorResultWrap = new AppErrorResultWrap(data.result);
                                                                                ITranLowStorageManager.Instance().init(this.mContext, 0);
                                                                                ITranLowStorageManager.Instance().showDialog(this.mService.mUiHandler, errorResultWrap);
                                                                            } else {
                                                                                errState.getDialogController().showCrashDialogs(data);
                                                                            }
                                                                            if (!proc.isolated) {
                                                                                this.mProcessCrashShowDialogTimes.put(proc.processName, proc.uid, Long.valueOf(now));
                                                                            }
                                                                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                                            return;
                                                                        }
                                                                        if (res != null) {
                                                                        }
                                                                        ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                                        return;
                                                                    }
                                                                } catch (Throwable th6) {
                                                                    th = th6;
                                                                    while (true) {
                                                                        break;
                                                                        break;
                                                                    }
                                                                    throw th;
                                                                }
                                                            } else {
                                                                crashShowErrorTime = crashShowErrorTime2;
                                                            }
                                                            z = false;
                                                            boolean shouldThottle2 = z;
                                                            if (!this.mService.mAtmInternal.canShowErrorDialogs()) {
                                                            }
                                                            if (crashSilenced) {
                                                            }
                                                            if (res != null) {
                                                            }
                                                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                            return;
                                                        }
                                                    } catch (Throwable th7) {
                                                        th = th7;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                }
                                                if (!this.mService.mAtmInternal.canShowErrorDialogs()) {
                                                }
                                                if (crashSilenced) {
                                                }
                                                if (res != null) {
                                                }
                                                ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                return;
                                            } catch (Throwable th8) {
                                                th = th8;
                                            }
                                        } catch (Throwable th9) {
                                            th = th9;
                                        }
                                        boolean isIoException2 = ITranLowStorageService.Instance().reprotPkgCurStateForIoExp(proc.info.packageName, data.exceptionMsg.toString());
                                        long now2 = SystemClock.uptimeMillis();
                                        if (crashShowErrorTime2 == null) {
                                        }
                                        z = false;
                                        boolean shouldThottle22 = z;
                                    } catch (Throwable th10) {
                                        th = th10;
                                    }
                                    crashSilenced = false;
                                    ITranLowStorageService.Instance().init(this.mContext);
                                } catch (Throwable th11) {
                                    th = th11;
                                }
                            } catch (Throwable th12) {
                                th = th12;
                            }
                        } catch (Throwable th13) {
                            th = th13;
                        }
                    } catch (Throwable th14) {
                        th = th14;
                    }
                }
            } catch (Throwable th15) {
                th = th15;
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleShowAppErrorUi$2$com-android-server-am-AppErrors  reason: not valid java name */
    public /* synthetic */ void m1106lambda$handleShowAppErrorUi$2$comandroidserveramAppErrors(CtaManager manager, ProcessRecord proc) {
        manager.showPermErrorDialog(this.mContext, proc.processName, proc.info.packageName);
    }

    private void stopReportingCrashesLBp(ProcessRecord proc) {
        if (this.mAppsNotReportingCrashes == null) {
            this.mAppsNotReportingCrashes = new ArraySet<>();
        }
        this.mAppsNotReportingCrashes.add(proc.info.packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:37:0x00fa  */
    /* JADX WARN: Removed duplicated region for block: B:39:0x0101  */
    /* JADX WARN: Removed duplicated region for block: B:46:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void handleShowAnrUi(Message msg) {
        boolean doKill = false;
        AppNotRespondingDialog.Data data = (AppNotRespondingDialog.Data) msg.obj;
        ProcessRecord proc = data.proc;
        if (proc == null) {
            Slog.e(TAG, "handleShowAnrUi: proc is null");
            return;
        }
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                ProcessErrorStateRecord errState = proc.mErrorState;
                errState.setAnrData(data);
                List<VersionedPackage> packageList = proc.isPersistent() ? null : proc.getPackageListWithVersionCode();
                if (errState.getDialogController().hasAnrDialogs()) {
                    Slog.e(TAG, "App already has anr dialog: " + proc);
                    MetricsLogger.action(this.mContext, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_MEDIA_SESSION_CALLBACK, -2);
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    return;
                }
                boolean showBackground = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "anr_show_background", 0, this.mService.mUserController.getCurrentUserId()) != 0;
                if (!this.mService.mAtmInternal.canShowErrorDialogs() && !showBackground) {
                    MetricsLogger.action(this.mContext, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_MEDIA_SESSION_CALLBACK, -1);
                    doKill = true;
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    if (doKill) {
                        this.mService.killAppAtUsersRequest(proc);
                    }
                    if (packageList == null) {
                        this.mPackageWatchdog.onPackageFailure(packageList, 4);
                        return;
                    }
                    return;
                }
                AnrController anrController = errState.getDialogController().getAnrController();
                if (anrController == null) {
                    errState.getDialogController().showAnrDialogs(data);
                } else {
                    String packageName = proc.info.packageName;
                    int uid = proc.info.uid;
                    boolean showDialog = anrController.onAnrDelayCompleted(packageName, uid);
                    if (showDialog) {
                        Slog.d(TAG, "ANR delay completed. Showing ANR dialog for package: " + packageName);
                        errState.getDialogController().showAnrDialogs(data);
                    } else {
                        Slog.d(TAG, "ANR delay completed. Cancelling ANR dialog for package: " + packageName);
                        errState.setNotResponding(false);
                        errState.setNotRespondingReport(null);
                        errState.getDialogController().clearAnrDialogs();
                    }
                }
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                if (doKill) {
                }
                if (packageList == null) {
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleDismissAnrDialogs(ProcessRecord proc) {
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                ProcessErrorStateRecord errState = proc.mErrorState;
                this.mService.mUiHandler.removeMessages(2, errState.getAnrData());
                if (errState.getDialogController().hasAnrDialogs()) {
                    errState.setNotResponding(false);
                    errState.setNotRespondingReport(null);
                    errState.getDialogController().clearAnrDialogs();
                }
                proc.mErrorState.setAnrData(null);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class BadProcessInfo {
        final String longMsg;
        final String shortMsg;
        final String stack;
        final long time;

        BadProcessInfo(long time, String shortMsg, String longMsg, String stack) {
            this.time = time;
            this.shortMsg = shortMsg;
            this.longMsg = longMsg;
            this.stack = stack;
        }
    }
}
