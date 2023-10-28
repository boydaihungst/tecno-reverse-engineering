package com.android.server.am;

import android.app.ActivityThread;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.DebugUtils;
import android.util.FeatureFlagUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.ProcessMap;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.MemInfoReader;
import com.android.server.SystemUtil;
import com.android.server.am.ActivityManagerService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.utils.PriorityDump;
import com.android.server.vibrator.VibratorManagerService;
import com.android.server.wm.ActivityTaskManagerDebugConfig;
import com.android.server.wm.ActivityTaskManagerService;
import com.transsion.hubcore.resmonitor.ITranResmonitor;
import com.transsion.hubcore.server.am.ITranAppProfiler;
import defpackage.CompanionAppsPermissions;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class AppProfiler {
    private static final String ACTION_HEAP_DUMP_FINISHED = "com.android.internal.intent.action.HEAP_DUMP_FINISHED";
    static final String ACTIVITY_START_PSS_DEFER_CONFIG = "activity_start_pss_defer";
    static final long BATTERY_STATS_TIME = 1800000;
    private static final String EXTRA_HEAP_DUMP_IS_USER_INITIATED = "com.android.internal.extra.heap_dump.IS_USER_INITIATED";
    private static final String EXTRA_HEAP_DUMP_PROCESS_NAME = "com.android.internal.extra.heap_dump.PROCESS_NAME";
    private static final String EXTRA_HEAP_DUMP_REPORT_PACKAGE = "com.android.internal.extra.heap_dump.REPORT_PACKAGE";
    private static final String EXTRA_HEAP_DUMP_SIZE_BYTES = "com.android.internal.extra.heap_dump.SIZE_BYTES";
    static final long MONITOR_CPU_MAX_TIME = 268435455;
    static final long MONITOR_CPU_MIN_TIME = 5000;
    static final boolean MONITOR_CPU_USAGE = true;
    static final boolean MONITOR_THREAD_CPU_USAGE = false;
    private final Handler mBgHandler;
    boolean mHasHomeProcess;
    boolean mHasPreviousProcess;
    private int mLastNumProcesses;
    private final LowMemDetector mLowMemDetector;
    private int mMemWatchDumpPid;
    private String mMemWatchDumpProcName;
    private int mMemWatchDumpUid;
    private Uri mMemWatchDumpUri;
    private boolean mMemWatchIsUserInitiated;
    final ActivityManagerGlobalLock mProcLock;
    private final ActivityManagerService mService;
    private SystemUtil mStl;
    private static final String TAG = "ActivityManager";
    static final String TAG_PSS = TAG + ActivityManagerDebugConfig.POSTFIX_PSS;
    static final String TAG_OOM_ADJ = ActivityManagerService.TAG_OOM_ADJ;
    public static final boolean IS_ROOT_ENABLE = "1".equals(SystemProperties.get("persist.user.root.support", "0"));
    private volatile long mPssDeferralTime = 0;
    private final ArrayList<ProcessProfileRecord> mPendingPssProfiles = new ArrayList<>();
    private final AtomicInteger mActivityStartingNesting = new AtomicInteger(0);
    private long mLastFullPssTime = SystemClock.uptimeMillis();
    private boolean mFullPssPending = false;
    private volatile boolean mTestPssMode = false;
    private boolean mAllowLowerMemLevel = false;
    private int mLastMemoryLevel = 0;
    private int mMemFactorOverride = -1;
    private long mLowRamTimeSinceLastIdle = 0;
    private long mLowRamStartTime = 0;
    private long mLastMemUsageReportTime = 0;
    private final ArrayList<ProcessRecord> mProcessesToGc = new ArrayList<>();
    private Map<String, String> mAppAgentMap = null;
    private int mProfileType = 0;
    private final ProfileData mProfileData = new ProfileData();
    private final ProcessMap<Pair<Long, String>> mMemWatchProcesses = new ProcessMap<>();
    private final ProcessCpuTracker mProcessCpuTracker = new ProcessCpuTracker(false);
    private final AtomicLong mLastCpuTime = new AtomicLong(0);
    private final AtomicBoolean mProcessCpuMutexFree = new AtomicBoolean(true);
    private final CountDownLatch mProcessCpuInitLatch = new CountDownLatch(1);
    private volatile long mLastWriteTime = 0;
    final Object mProfilerLock = new Object();
    private final DeviceConfig.OnPropertiesChangedListener mPssDelayConfigListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.am.AppProfiler.1
        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            if (properties.getKeyset().contains(AppProfiler.ACTIVITY_START_PSS_DEFER_CONFIG)) {
                AppProfiler.this.mPssDeferralTime = properties.getLong(AppProfiler.ACTIVITY_START_PSS_DEFER_CONFIG, 0L);
                if (ActivityManagerDebugConfig.DEBUG_PSS) {
                    Slog.d(AppProfiler.TAG_PSS, "Activity-start PSS delay now " + AppProfiler.this.mPssDeferralTime + " ms");
                }
            }
        }
    };
    private final Thread mProcessCpuThread = new ProcessCpuThread("CpuTracker");

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ProfileData {
        private String mProfileApp;
        private ProcessRecord mProfileProc;
        private ProfilerInfo mProfilerInfo;

        private ProfileData() {
            this.mProfileApp = null;
            this.mProfileProc = null;
            this.mProfilerInfo = null;
        }

        void setProfileApp(String profileApp) {
            this.mProfileApp = profileApp;
            if (AppProfiler.this.mService.mAtmInternal != null) {
                AppProfiler.this.mService.mAtmInternal.setProfileApp(profileApp);
            }
        }

        String getProfileApp() {
            return this.mProfileApp;
        }

        void setProfileProc(ProcessRecord profileProc) {
            this.mProfileProc = profileProc;
            if (AppProfiler.this.mService.mAtmInternal != null) {
                AppProfiler.this.mService.mAtmInternal.setProfileProc(profileProc == null ? null : profileProc.getWindowProcessController());
            }
        }

        ProcessRecord getProfileProc() {
            return this.mProfileProc;
        }

        void setProfilerInfo(ProfilerInfo profilerInfo) {
            this.mProfilerInfo = profilerInfo;
            if (AppProfiler.this.mService.mAtmInternal != null) {
                AppProfiler.this.mService.mAtmInternal.setProfilerInfo(profilerInfo);
            }
        }

        ProfilerInfo getProfilerInfo() {
            return this.mProfilerInfo;
        }
    }

    /* loaded from: classes.dex */
    private class BgHandler extends Handler {
        static final int COLLECT_PSS_BG_MSG = 1;
        static final int DEFER_PSS_MSG = 2;
        static final int MEMORY_PRESSURE_CHANGED = 4;
        static final int STOP_DEFERRING_PSS_MSG = 3;

        BgHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AppProfiler.this.collectPssInBackground();
                    return;
                case 2:
                    AppProfiler.this.deferPssForActivityStart();
                    return;
                case 3:
                    AppProfiler.this.stopDeferPss();
                    return;
                case 4:
                    synchronized (AppProfiler.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            AppProfiler.this.handleMemoryPressureChangedLocked(msg.arg1, msg.arg2);
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                default:
                    return;
            }
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:113:0x0284
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [545=5] */
    /* JADX INFO: Access modifiers changed from: private */
    public void collectPssInBackground() {
        /*
            r45 = this;
            r15 = r45
            long r17 = android.os.SystemClock.uptimeMillis()
            r1 = 0
            java.lang.Object r2 = r15.mProfilerLock
            monitor-enter(r2)
            boolean r0 = r15.mFullPssPending     // Catch: java.lang.Throwable -> L316
            r13 = 0
            if (r0 == 0) goto L19
            r15.mFullPssPending = r13     // Catch: java.lang.Throwable -> L316
            com.android.internal.util.MemInfoReader r0 = new com.android.internal.util.MemInfoReader     // Catch: java.lang.Throwable -> L316
            r0.<init>()     // Catch: java.lang.Throwable -> L316
            r19 = r0
            goto L1b
        L19:
            r19 = r1
        L1b:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L312
            r0 = 0
            if (r19 == 0) goto Ld7
            r45.updateCpuStatsNow()
            r1 = 0
            com.android.internal.os.ProcessCpuTracker r3 = r15.mProcessCpuTracker
            monitor-enter(r3)
            com.android.internal.os.ProcessCpuTracker r4 = r15.mProcessCpuTracker     // Catch: java.lang.Throwable -> Ld4
            com.android.server.am.AppProfiler$$ExternalSyntheticLambda0 r5 = new com.android.server.am.AppProfiler$$ExternalSyntheticLambda0     // Catch: java.lang.Throwable -> Ld4
            r5.<init>()     // Catch: java.lang.Throwable -> Ld4
            java.util.List r4 = r4.getStats(r5)     // Catch: java.lang.Throwable -> Ld4
            monitor-exit(r3)     // Catch: java.lang.Throwable -> Ld4
            int r5 = r4.size()
            r3 = 0
            r6 = r1
        L39:
            if (r3 >= r5) goto L68
            com.android.server.am.ActivityManagerService r1 = r15.mService
            com.android.server.am.ActivityManagerService$PidMap r1 = r1.mPidsSelfLocked
            monitor-enter(r1)
            com.android.server.am.ActivityManagerService r2 = r15.mService     // Catch: java.lang.Throwable -> L65
            com.android.server.am.ActivityManagerService$PidMap r2 = r2.mPidsSelfLocked     // Catch: java.lang.Throwable -> L65
            java.lang.Object r8 = r4.get(r3)     // Catch: java.lang.Throwable -> L65
            com.android.internal.os.ProcessCpuTracker$Stats r8 = (com.android.internal.os.ProcessCpuTracker.Stats) r8     // Catch: java.lang.Throwable -> L65
            int r8 = r8.pid     // Catch: java.lang.Throwable -> L65
            int r2 = r2.indexOfKey(r8)     // Catch: java.lang.Throwable -> L65
            if (r2 < 0) goto L54
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L65
            goto L62
        L54:
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L65
            java.lang.Object r1 = r4.get(r3)
            com.android.internal.os.ProcessCpuTracker$Stats r1 = (com.android.internal.os.ProcessCpuTracker.Stats) r1
            int r1 = r1.pid
            long r1 = android.os.Debug.getPss(r1, r0, r0)
            long r6 = r6 + r1
        L62:
            int r3 = r3 + 1
            goto L39
        L65:
            r0 = move-exception
            monitor-exit(r1)     // Catch: java.lang.Throwable -> L65
            throw r0
        L68:
            r19.readMemInfo()
            com.android.server.am.ActivityManagerService r1 = r15.mService
            com.android.server.am.ProcessStatsService r1 = r1.mProcessStats
            java.lang.Object r8 = r1.mLock
            monitor-enter(r8)
            boolean r1 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_PSS     // Catch: java.lang.Throwable -> Ld1
            if (r1 == 0) goto L9b
            java.lang.String r1 = com.android.server.am.AppProfiler.TAG_PSS     // Catch: java.lang.Throwable -> Ld1
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Ld1
            r2.<init>()     // Catch: java.lang.Throwable -> Ld1
            java.lang.String r3 = "Collected native and kernel memory in "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Ld1
            long r9 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> Ld1
            long r9 = r9 - r17
            java.lang.StringBuilder r2 = r2.append(r9)     // Catch: java.lang.Throwable -> Ld1
            java.lang.String r3 = "ms"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Ld1
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Ld1
            android.util.Slog.d(r1, r2)     // Catch: java.lang.Throwable -> Ld1
        L9b:
            long r1 = r19.getCachedSizeKb()     // Catch: java.lang.Throwable -> Ld1
            long r9 = r19.getFreeSizeKb()     // Catch: java.lang.Throwable -> Ld1
            long r11 = r19.getZramTotalSizeKb()     // Catch: java.lang.Throwable -> Ld1
            long r20 = r19.getKernelUsedSizeKb()     // Catch: java.lang.Throwable -> Ld1
            r31 = r20
            r20 = 1024(0x400, double:5.06E-321)
            long r33 = r1 * r20
            long r35 = r9 * r20
            long r37 = r11 * r20
            long r39 = r31 * r20
            long r41 = r6 * r20
            com.android.server.am.EventLogTags.writeAmMeminfo(r33, r35, r37, r39, r41)     // Catch: java.lang.Throwable -> Ld1
            com.android.server.am.ActivityManagerService r3 = r15.mService     // Catch: java.lang.Throwable -> Ld1
            com.android.server.am.ProcessStatsService r3 = r3.mProcessStats     // Catch: java.lang.Throwable -> Ld1
            r20 = r3
            r21 = r1
            r23 = r9
            r25 = r11
            r27 = r31
            r29 = r6
            r20.addSysMemUsageLocked(r21, r23, r25, r27, r29)     // Catch: java.lang.Throwable -> Ld1
            monitor-exit(r8)     // Catch: java.lang.Throwable -> Ld1
            goto Ld7
        Ld1:
            r0 = move-exception
            monitor-exit(r8)     // Catch: java.lang.Throwable -> Ld1
            throw r0
        Ld4:
            r0 = move-exception
            monitor-exit(r3)     // Catch: java.lang.Throwable -> Ld4
            throw r0
        Ld7:
            r1 = 0
            r2 = 3
            long[] r14 = new long[r2]
        Ldb:
            r2 = -1
            java.lang.Object r3 = r15.mProfilerLock
            monitor-enter(r3)
            java.util.ArrayList<com.android.server.am.ProcessProfileRecord> r4 = r15.mPendingPssProfiles     // Catch: java.lang.Throwable -> L30b
            int r4 = r4.size()     // Catch: java.lang.Throwable -> L30b
            if (r4 > 0) goto L12a
            boolean r0 = r15.mTestPssMode     // Catch: java.lang.Throwable -> L125
            if (r0 != 0) goto Lef
            boolean r0 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_PSS     // Catch: java.lang.Throwable -> L125
            if (r0 == 0) goto L11e
        Lef:
            java.lang.String r0 = com.android.server.am.AppProfiler.TAG_PSS     // Catch: java.lang.Throwable -> L125
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L125
            r4.<init>()     // Catch: java.lang.Throwable -> L125
            java.lang.String r5 = "Collected pss of "
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L125
            java.lang.StringBuilder r4 = r4.append(r1)     // Catch: java.lang.Throwable -> L125
            java.lang.String r5 = " processes in "
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L125
            long r5 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> L125
            long r5 = r5 - r17
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L125
            java.lang.String r5 = "ms"
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L125
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L125
            android.util.Slog.d(r0, r4)     // Catch: java.lang.Throwable -> L125
        L11e:
            java.util.ArrayList<com.android.server.am.ProcessProfileRecord> r0 = r15.mPendingPssProfiles     // Catch: java.lang.Throwable -> L125
            r0.clear()     // Catch: java.lang.Throwable -> L125
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L125
            return
        L125:
            r0 = move-exception
            r31 = r14
            goto L30e
        L12a:
            java.util.ArrayList<com.android.server.am.ProcessProfileRecord> r4 = r15.mPendingPssProfiles     // Catch: java.lang.Throwable -> L30b
            java.lang.Object r4 = r4.remove(r13)     // Catch: java.lang.Throwable -> L30b
            com.android.server.am.ProcessProfileRecord r4 = (com.android.server.am.ProcessProfileRecord) r4     // Catch: java.lang.Throwable -> L30b
            int r5 = r4.getPssProcState()     // Catch: java.lang.Throwable -> L30b
            r10 = r5
            int r12 = r4.getPssStatType()     // Catch: java.lang.Throwable -> L30b
            long r5 = r4.getLastPssTime()     // Catch: java.lang.Throwable -> L30b
            r20 = r5
            long r5 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> L30b
            android.app.IApplicationThread r7 = r4.getThread()     // Catch: java.lang.Throwable -> L30b
            r8 = 1000(0x3e8, double:4.94E-321)
            if (r7 == 0) goto L161
            int r7 = r4.getSetProcState()     // Catch: java.lang.Throwable -> L125
            if (r10 != r7) goto L161
            long r22 = r20 + r8
            int r7 = (r22 > r5 ? 1 : (r22 == r5 ? 0 : -1))
            if (r7 >= 0) goto L161
            int r7 = r4.getPid()     // Catch: java.lang.Throwable -> L125
            r2 = r7
            r13 = r2
            r11 = r4
            goto L198
        L161:
            r4.abortNextPssTime()     // Catch: java.lang.Throwable -> L30b
            boolean r7 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_PSS     // Catch: java.lang.Throwable -> L30b
            if (r7 == 0) goto L194
            java.lang.String r7 = com.android.server.am.AppProfiler.TAG_PSS     // Catch: java.lang.Throwable -> L125
            java.lang.StringBuilder r11 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L125
            r11.<init>()     // Catch: java.lang.Throwable -> L125
            java.lang.String r13 = "Skipped pss collection of "
            java.lang.StringBuilder r11 = r11.append(r13)     // Catch: java.lang.Throwable -> L125
            java.lang.StringBuilder r11 = r11.append(r2)     // Catch: java.lang.Throwable -> L125
            java.lang.String r13 = ": still need "
            java.lang.StringBuilder r11 = r11.append(r13)     // Catch: java.lang.Throwable -> L125
            long r8 = r20 + r8
            long r8 = r8 - r5
            java.lang.StringBuilder r8 = r11.append(r8)     // Catch: java.lang.Throwable -> L125
            java.lang.String r9 = "ms until safe"
            java.lang.StringBuilder r8 = r8.append(r9)     // Catch: java.lang.Throwable -> L125
            java.lang.String r8 = r8.toString()     // Catch: java.lang.Throwable -> L125
            android.util.Slog.d(r7, r8)     // Catch: java.lang.Throwable -> L125
        L194:
            r4 = 0
            r2 = 0
            r13 = r2
            r11 = r4
        L198:
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L305
            if (r11 == 0) goto L2f5
            long r22 = android.os.SystemClock.currentThreadTimeMillis()
            com.android.server.am.ActivityManagerService r2 = r15.mService
            com.android.server.am.ProcessRecord r3 = r11.mApp
            int r3 = r3.uid
            boolean r24 = r2.isCameraActiveForUid(r3)
            r2 = 0
            if (r24 == 0) goto L1af
            r4 = r2
            goto L1b3
        L1af:
            long r4 = android.os.Debug.getPss(r13, r14, r0)
        L1b3:
            r25 = r4
            long r27 = android.os.SystemClock.currentThreadTimeMillis()
            java.lang.Object r8 = r15.mProfilerLock
            monitor-enter(r8)
            int r2 = (r25 > r2 ? 1 : (r25 == r2 ? 0 : -1))
            if (r2 == 0) goto L24f
            android.app.IApplicationThread r2 = r11.getThread()     // Catch: java.lang.Throwable -> L244
            if (r2 == 0) goto L24f
            int r2 = r11.getSetProcState()     // Catch: java.lang.Throwable -> L244
            if (r2 != r10) goto L237
            int r2 = r11.getPid()     // Catch: java.lang.Throwable -> L244
            if (r2 != r13) goto L22a
            long r2 = r11.getLastPssTime()     // Catch: java.lang.Throwable -> L244
            int r2 = (r2 > r20 ? 1 : (r2 == r20 ? 0 : -1))
            if (r2 != 0) goto L22a
            int r29 = r1 + 1
            r11.commitNextPssTime()     // Catch: java.lang.Throwable -> L21d
            r16 = 0
            r6 = r14[r16]     // Catch: java.lang.Throwable -> L21d
            r1 = 1
            r30 = r14[r1]     // Catch: java.lang.Throwable -> L21d
            r1 = 2
            r32 = r14[r1]     // Catch: java.lang.Throwable -> L21d
            long r34 = r27 - r22
            long r36 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> L21d
            r1 = r45
            r2 = r11
            r3 = r10
            r4 = r25
            r38 = r8
            r8 = r30
            r43 = r10
            r30 = r11
            r10 = r32
            r44 = r13
            r31 = r14
            r32 = r16
            r13 = r34
            r15 = r36
            r1.recordPssSampleLPf(r2, r3, r4, r6, r8, r10, r12, r13, r15)     // Catch: java.lang.Throwable -> L214
            r1 = r29
            r5 = r43
            r4 = r44
            goto L2ea
        L214:
            r0 = move-exception
            r1 = r29
            r5 = r43
            r4 = r44
            goto L2f1
        L21d:
            r0 = move-exception
            r38 = r8
            r30 = r11
            r31 = r14
            r5 = r10
            r4 = r13
            r1 = r29
            goto L2f1
        L22a:
            r38 = r8
            r43 = r10
            r30 = r11
            r44 = r13
            r31 = r14
            r32 = 0
            goto L25b
        L237:
            r38 = r8
            r43 = r10
            r30 = r11
            r44 = r13
            r31 = r14
            r32 = 0
            goto L25b
        L244:
            r0 = move-exception
            r38 = r8
            r30 = r11
            r31 = r14
            r5 = r10
            r4 = r13
            goto L2f1
        L24f:
            r38 = r8
            r43 = r10
            r30 = r11
            r44 = r13
            r31 = r14
            r32 = 0
        L25b:
            r30.abortNextPssTime()     // Catch: java.lang.Throwable -> L2ec
            boolean r2 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_PSS     // Catch: java.lang.Throwable -> L2ec
            if (r2 == 0) goto L2e6
            java.lang.String r2 = com.android.server.am.AppProfiler.TAG_PSS     // Catch: java.lang.Throwable -> L2ec
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L2ec
            r3.<init>()     // Catch: java.lang.Throwable -> L2ec
            java.lang.String r4 = "Skipped pss collection of "
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L2ec
            r4 = r44
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L2e2
            java.lang.String r5 = ": "
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> L2e2
            android.app.IApplicationThread r5 = r30.getThread()     // Catch: java.lang.Throwable -> L2e2
            if (r5 != 0) goto L289
            java.lang.String r5 = "NO_THREAD "
            goto L28b
        L284:
            r0 = move-exception
            r5 = r43
            goto L2f1
        L289:
            java.lang.String r5 = ""
        L28b:
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> L2e2
            if (r24 == 0) goto L294
            java.lang.String r5 = "CAMERA "
            goto L296
        L294:
            java.lang.String r5 = ""
        L296:
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> L2e2
            int r5 = r30.getPid()     // Catch: java.lang.Throwable -> L2e2
            if (r5 == r4) goto L2a3
            java.lang.String r5 = "PID_CHANGED "
            goto L2a5
        L2a3:
            java.lang.String r5 = ""
        L2a5:
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> L2e2
            java.lang.String r5 = " initState="
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> L2e2
            r5 = r43
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> L2f3
            java.lang.String r6 = " curState="
            java.lang.StringBuilder r3 = r3.append(r6)     // Catch: java.lang.Throwable -> L2f3
            int r6 = r30.getSetProcState()     // Catch: java.lang.Throwable -> L2f3
            java.lang.StringBuilder r3 = r3.append(r6)     // Catch: java.lang.Throwable -> L2f3
            java.lang.String r6 = " "
            java.lang.StringBuilder r3 = r3.append(r6)     // Catch: java.lang.Throwable -> L2f3
            long r6 = r30.getLastPssTime()     // Catch: java.lang.Throwable -> L2f3
            int r6 = (r6 > r20 ? 1 : (r6 == r20 ? 0 : -1))
            if (r6 == 0) goto L2d4
            java.lang.String r6 = "TIME_CHANGED"
            goto L2d6
        L2d4:
            java.lang.String r6 = ""
        L2d6:
            java.lang.StringBuilder r3 = r3.append(r6)     // Catch: java.lang.Throwable -> L2f3
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L2f3
            android.util.Slog.d(r2, r3)     // Catch: java.lang.Throwable -> L2f3
            goto L2ea
        L2e2:
            r0 = move-exception
            r5 = r43
            goto L2f1
        L2e6:
            r5 = r43
            r4 = r44
        L2ea:
            monitor-exit(r38)     // Catch: java.lang.Throwable -> L2f3
            goto L2fd
        L2ec:
            r0 = move-exception
            r5 = r43
            r4 = r44
        L2f1:
            monitor-exit(r38)     // Catch: java.lang.Throwable -> L2f3
            throw r0
        L2f3:
            r0 = move-exception
            goto L2f1
        L2f5:
            r5 = r10
            r30 = r11
            r4 = r13
            r31 = r14
            r32 = 0
        L2fd:
            r15 = r45
            r14 = r31
            r13 = r32
            goto Ldb
        L305:
            r0 = move-exception
            r4 = r13
            r31 = r14
            r2 = r4
            goto L30e
        L30b:
            r0 = move-exception
            r31 = r14
        L30e:
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L310
            throw r0
        L310:
            r0 = move-exception
            goto L30e
        L312:
            r0 = move-exception
            r1 = r19
            goto L317
        L316:
            r0 = move-exception
        L317:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L316
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.AppProfiler.collectPssInBackground():void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$collectPssInBackground$0(ProcessCpuTracker.Stats st) {
        return st.vsize > 0 && st.uid < 10000;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateNextPssTimeLPf(int procState, ProcessProfileRecord profile, long now, boolean forceUpdate) {
        if (!forceUpdate && ((now <= profile.getNextPssTime() && now <= Math.max(profile.getLastPssTime() + 3600000, profile.getLastStateTime() + ProcessList.minTimeFromStateChange(this.mTestPssMode))) || !requestPssLPf(profile, procState))) {
            return;
        }
        profile.setNextPssTime(profile.computeNextPssTime(procState, this.mTestPssMode, this.mService.mAtmInternal.isSleeping(), now));
    }

    private void recordPssSampleLPf(final ProcessProfileRecord profile, int procState, final long pss, final long uss, long swapPss, final long rss, final int statType, final long pssDuration, long now) {
        ProcessProfileRecord processProfileRecord;
        final AppProfiler appProfiler;
        final ProcessRecord proc;
        final ProcessRecord proc2 = profile.mApp;
        EventLogTags.writeAmPss(profile.getPid(), proc2.uid, proc2.processName, pss * GadgetFunction.NCM, uss * GadgetFunction.NCM, swapPss * GadgetFunction.NCM, rss * GadgetFunction.NCM, statType, procState, pssDuration);
        if (Build.TRAN_RM2_SUPPORT && proc2.mState != null) {
            int adj = proc2.mState.getCurAdj();
            ITranResmonitor.Instance().recordPss(profile.getPid(), proc2.uid, proc2.processName, adj, pss);
        }
        profile.setLastPssTime(now);
        profile.addPss(pss, uss, rss, true, statType, pssDuration);
        proc2.getPkgList().forEachPackageProcessStats(new Consumer() { // from class: com.android.server.am.AppProfiler$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                FrameworkStatsLog.write(18, ProcessRecord.this.info.uid, r11.state.getName(), r11.state.getPackage(), pss, uss, rss, statType, pssDuration, ((ProcessStats.ProcessStateHolder) obj).appVersion, r10.getCurrentHostingComponentTypes(), profile.getHistoricalHostingComponentTypes());
            }
        });
        if (ActivityManagerDebugConfig.DEBUG_PSS) {
            Slog.d(TAG_PSS, "pss of " + proc2.toShortString() + ": " + pss + " lastPss=" + profile.getLastPss() + " state=" + ProcessList.makeProcStateString(procState));
        }
        if (profile.getInitialIdlePss() != 0) {
            processProfileRecord = profile;
        } else {
            processProfileRecord = profile;
            processProfileRecord.setInitialIdlePss(pss);
        }
        processProfileRecord.setLastPss(pss);
        processProfileRecord.setLastSwapPss(swapPss);
        if (!ITranAppProfiler.Instance().isAppMemoryEnable()) {
            appProfiler = this;
            proc = proc2;
        } else {
            proc = proc2;
            if (!ITranAppProfiler.Instance().shouldLimitPssOfProcess(proc, pss, proc.mState.getCurAdj(), now)) {
                appProfiler = this;
            } else {
                appProfiler = this;
                ITranAppProfiler.Instance().startCycleAbnormal(new Runnable() { // from class: com.android.server.am.AppProfiler$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        AppProfiler.this.m1155lambda$recordPssSampleLPf$2$comandroidserveramAppProfiler(proc);
                    }
                });
            }
        }
        if (ITranAppProfiler.Instance().isAgaresEnable()) {
            TranAmHooker.recoveryAdjforAgares(profile);
        }
        if (ITranAppProfiler.Instance().isKeepAliveSupport()) {
            TranAmHooker.recoveryAdjIfNeed(profile);
        }
        if (ITranAppProfiler.Instance().isGameBoosterEnable()) {
            TranAmHooker.recoveryAdjforGame(profile);
        }
        if (procState >= 14) {
            processProfileRecord.setLastCachedPss(pss);
            processProfileRecord.setLastCachedSwapPss(swapPss);
        }
        processProfileRecord.setLastRss(rss);
        SparseArray<Pair<Long, String>> watchUids = (SparseArray) appProfiler.mMemWatchProcesses.getMap().get(proc.processName);
        Long check = null;
        if (watchUids != null) {
            Pair<Long, String> val = watchUids.get(proc.uid);
            if (val == null) {
                val = watchUids.get(0);
            }
            if (val != null) {
                check = (Long) val.first;
            }
        }
        if (check != null && pss * GadgetFunction.NCM >= check.longValue() && profile.getThread() != null && appProfiler.mMemWatchDumpProcName == null) {
            if (Build.IS_DEBUGGABLE || proc.isDebuggable()) {
                Slog.w(TAG, "Process " + proc + " exceeded pss limit " + check + "; reporting");
                appProfiler.startHeapDumpLPf(processProfileRecord, false);
                return;
            }
            Slog.w(TAG, "Process " + proc + " exceeded pss limit " + check + ", but debugging not enabled");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$recordPssSampleLPf$2$com-android-server-am-AppProfiler  reason: not valid java name */
    public /* synthetic */ void m1155lambda$recordPssSampleLPf$2$comandroidserveramAppProfiler(ProcessRecord proc) {
        this.mService.mInternal.killProcess(proc.processName, proc.uid, "app memory limit");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class RecordPssRunnable implements Runnable {
        private final ContentResolver mContentResolver;
        private final Uri mDumpUri;
        private final ProcessProfileRecord mProfile;

        RecordPssRunnable(ProcessProfileRecord profile, Uri dumpUri, ContentResolver contentResolver) {
            this.mProfile = profile;
            this.mDumpUri = dumpUri;
            this.mContentResolver = contentResolver;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                ParcelFileDescriptor fd = this.mContentResolver.openFileDescriptor(this.mDumpUri, "rw");
                IApplicationThread thread = this.mProfile.getThread();
                if (thread != null) {
                    try {
                        if (ActivityManagerDebugConfig.DEBUG_PSS) {
                            Slog.d(AppProfiler.TAG_PSS, "Requesting dump heap from " + this.mProfile.mApp + " to " + this.mDumpUri.getPath());
                        }
                        thread.dumpHeap(true, false, false, this.mDumpUri.getPath(), fd, (RemoteCallback) null);
                    } catch (RemoteException e) {
                    }
                }
                if (fd != null) {
                    fd.close();
                }
            } catch (IOException e2) {
                Slog.e(AppProfiler.TAG, "Failed to dump heap", e2);
                AppProfiler.this.abortHeapDump(this.mProfile.mApp.processName);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startHeapDumpLPf(ProcessProfileRecord profile, boolean isUserInitiated) {
        ProcessRecord proc = profile.mApp;
        this.mMemWatchDumpProcName = proc.processName;
        this.mMemWatchDumpUri = makeHeapDumpUri(proc.processName);
        this.mMemWatchDumpPid = profile.getPid();
        this.mMemWatchDumpUid = proc.uid;
        this.mMemWatchIsUserInitiated = isUserInitiated;
        try {
            Context ctx = this.mService.mContext.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, UserHandle.getUserHandleForUid(this.mMemWatchDumpUid));
            BackgroundThread.getHandler().post(new RecordPssRunnable(profile, this.mMemWatchDumpUri, ctx.getContentResolver()));
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("android package not found.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpHeapFinished(String path, int callerPid) {
        synchronized (this.mProfilerLock) {
            if (callerPid != this.mMemWatchDumpPid) {
                Slog.w(TAG, "dumpHeapFinished: Calling pid " + Binder.getCallingPid() + " does not match last pid " + this.mMemWatchDumpPid);
                return;
            }
            Uri uri = this.mMemWatchDumpUri;
            if (uri != null && uri.getPath().equals(path)) {
                if (ActivityManagerDebugConfig.DEBUG_PSS) {
                    Slog.d(TAG_PSS, "Dump heap finished for " + path);
                }
                this.mService.mHandler.sendEmptyMessage(50);
                Runtime.getRuntime().gc();
                return;
            }
            Slog.w(TAG, "dumpHeapFinished: Calling path " + path + " does not match last path " + this.mMemWatchDumpUri);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handlePostDumpHeapNotification() {
        int uid;
        String procName;
        long memLimit;
        String reportPackage;
        boolean isUserInitiated;
        synchronized (this.mProfilerLock) {
            uid = this.mMemWatchDumpUid;
            procName = this.mMemWatchDumpProcName;
            Pair<Long, String> val = (Pair) this.mMemWatchProcesses.get(procName, uid);
            if (val == null) {
                val = (Pair) this.mMemWatchProcesses.get(procName, 0);
            }
            if (val != null) {
                memLimit = ((Long) val.first).longValue();
                reportPackage = (String) val.second;
            } else {
                memLimit = 0;
                reportPackage = null;
            }
            isUserInitiated = this.mMemWatchIsUserInitiated;
            this.mMemWatchDumpUri = null;
            this.mMemWatchDumpProcName = null;
            this.mMemWatchDumpPid = -1;
            this.mMemWatchDumpUid = -1;
        }
        if (procName == null) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_PSS) {
            Slog.d(TAG_PSS, "Showing dump heap notification from " + procName + SliceClientPermissions.SliceAuthority.DELIMITER + uid);
        }
        Intent dumpFinishedIntent = new Intent(ACTION_HEAP_DUMP_FINISHED);
        dumpFinishedIntent.setPackage(VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME);
        dumpFinishedIntent.putExtra("android.intent.extra.UID", uid);
        dumpFinishedIntent.putExtra(EXTRA_HEAP_DUMP_IS_USER_INITIATED, isUserInitiated);
        dumpFinishedIntent.putExtra(EXTRA_HEAP_DUMP_SIZE_BYTES, memLimit);
        dumpFinishedIntent.putExtra(EXTRA_HEAP_DUMP_REPORT_PACKAGE, reportPackage);
        dumpFinishedIntent.putExtra(EXTRA_HEAP_DUMP_PROCESS_NAME, procName);
        this.mService.mContext.sendBroadcastAsUser(dumpFinishedIntent, UserHandle.getUserHandleForUid(uid));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDumpHeapDebugLimit(String processName, int uid, long maxMemSize, String reportPackage) {
        synchronized (this.mProfilerLock) {
            try {
                if (maxMemSize > 0) {
                    this.mMemWatchProcesses.put(processName, uid, new Pair(Long.valueOf(maxMemSize), reportPackage));
                } else if (uid != 0) {
                    this.mMemWatchProcesses.remove(processName, uid);
                } else {
                    this.mMemWatchProcesses.getMap().remove(processName);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void abortHeapDump(String procName) {
        Message msg = this.mService.mHandler.obtainMessage(51);
        msg.obj = procName;
        this.mService.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleAbortDumpHeap(String procName) {
        if (procName != null) {
            synchronized (this.mProfilerLock) {
                if (procName.equals(this.mMemWatchDumpProcName)) {
                    this.mMemWatchDumpProcName = null;
                    this.mMemWatchDumpUri = null;
                    this.mMemWatchDumpPid = -1;
                    this.mMemWatchDumpUid = -1;
                }
            }
        }
    }

    private static Uri makeHeapDumpUri(String procName) {
        return Uri.parse("content://com.android.shell.heapdump/" + procName + "_javaheap.bin");
    }

    private boolean requestPssLPf(ProcessProfileRecord profile, int procState) {
        if (this.mPendingPssProfiles.contains(profile)) {
            return false;
        }
        if (this.mPendingPssProfiles.size() == 0) {
            long deferral = (this.mPssDeferralTime <= 0 || this.mActivityStartingNesting.get() <= 0) ? 0L : this.mPssDeferralTime;
            if (ActivityManagerDebugConfig.DEBUG_PSS && deferral > 0) {
                Slog.d(TAG_PSS, "requestPssLPf() deferring PSS request by " + deferral + " ms");
            }
            this.mBgHandler.sendEmptyMessageDelayed(1, deferral);
        }
        if (ActivityManagerDebugConfig.DEBUG_PSS) {
            Slog.d(TAG_PSS, "Requesting pss of: " + profile.mApp);
        }
        profile.setPssProcState(procState);
        profile.setPssStatType(0);
        this.mPendingPssProfiles.add(profile);
        return true;
    }

    private void deferPssIfNeededLPf() {
        if (this.mPendingPssProfiles.size() > 0) {
            this.mBgHandler.removeMessages(1);
            this.mBgHandler.sendEmptyMessageDelayed(1, this.mPssDeferralTime);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deferPssForActivityStart() {
        if (this.mPssDeferralTime > 0) {
            if (ActivityManagerDebugConfig.DEBUG_PSS) {
                Slog.d(TAG_PSS, "Deferring PSS collection for activity start");
            }
            synchronized (this.mProfilerLock) {
                deferPssIfNeededLPf();
            }
            this.mActivityStartingNesting.getAndIncrement();
            this.mBgHandler.sendEmptyMessageDelayed(3, this.mPssDeferralTime);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopDeferPss() {
        int nesting = this.mActivityStartingNesting.decrementAndGet();
        if (nesting <= 0) {
            if (ActivityManagerDebugConfig.DEBUG_PSS) {
                Slog.d(TAG_PSS, "PSS activity start deferral interval ended; now " + nesting);
            }
            if (nesting < 0) {
                Slog.wtf(TAG, "Activity start nesting undercount!");
                this.mActivityStartingNesting.incrementAndGet();
            }
        } else if (ActivityManagerDebugConfig.DEBUG_PSS) {
            Slog.d(TAG_PSS, "Still deferring PSS, nesting=" + nesting);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestPssAllProcsLPr(final long now, final boolean always, final boolean memLowered) {
        synchronized (this.mProfilerLock) {
            if (!always) {
                if (now < this.mLastFullPssTime + (memLowered ? this.mService.mConstants.FULL_PSS_LOWERED_INTERVAL : this.mService.mConstants.FULL_PSS_MIN_INTERVAL)) {
                    return;
                }
            }
            if (ActivityManagerDebugConfig.DEBUG_PSS) {
                Slog.d(TAG_PSS, "Requesting pss of all procs!  memLowered=" + memLowered);
            }
            this.mLastFullPssTime = now;
            this.mFullPssPending = true;
            for (int i = this.mPendingPssProfiles.size() - 1; i >= 0; i--) {
                this.mPendingPssProfiles.get(i).abortNextPssTime();
            }
            this.mPendingPssProfiles.ensureCapacity(this.mService.mProcessList.getLruSizeLOSP());
            this.mPendingPssProfiles.clear();
            this.mService.mProcessList.forEachLruProcessesLOSP(false, new Consumer() { // from class: com.android.server.am.AppProfiler$$ExternalSyntheticLambda6
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppProfiler.this.m1156lambda$requestPssAllProcsLPr$3$comandroidserveramAppProfiler(memLowered, always, now, (ProcessRecord) obj);
                }
            });
            if (!this.mBgHandler.hasMessages(1)) {
                this.mBgHandler.sendEmptyMessage(1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestPssAllProcsLPr$3$com-android-server-am-AppProfiler  reason: not valid java name */
    public /* synthetic */ void m1156lambda$requestPssAllProcsLPr$3$comandroidserveramAppProfiler(boolean memLowered, boolean always, long now, ProcessRecord app) {
        ProcessProfileRecord profile = app.mProfile;
        if (profile.getThread() == null || profile.getSetProcState() == 20) {
            return;
        }
        long lastStateTime = profile.getLastStateTime();
        if (memLowered || ((always && now > 1000 + lastStateTime) || now > 1200000 + lastStateTime)) {
            profile.setPssProcState(profile.getSetProcState());
            profile.setPssStatType(always ? 2 : 1);
            updateNextPssTimeLPf(profile.getSetProcState(), profile, now, true);
            this.mPendingPssProfiles.add(profile);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTestPssMode(boolean enabled) {
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                this.mTestPssMode = enabled;
                if (enabled) {
                    requestPssAllProcsLPr(SystemClock.uptimeMillis(), true, true);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getTestPssMode() {
        return this.mTestPssMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLastMemoryLevelLocked() {
        int i = this.mMemFactorOverride;
        if (i != -1) {
            return i;
        }
        return this.mLastMemoryLevel;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLastMemoryLevelNormal() {
        int i = this.mMemFactorOverride;
        return i != -1 ? i <= 0 : this.mLastMemoryLevel <= 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLowRamTimestampLPr(long now) {
        this.mLowRamTimeSinceLastIdle = 0L;
        if (this.mLowRamStartTime != 0) {
            this.mLowRamStartTime = now;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllowLowerMemLevelLocked(boolean allowLowerMemLevel) {
        this.mAllowLowerMemLevel = allowLowerMemLevel;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMemFactorOverrideLocked(int factor) {
        this.mMemFactorOverride = factor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateLowMemStateLSP(int numCached, int numEmpty, int numTrimming) {
        int memFactor;
        long now;
        boolean z;
        final boolean allChanged;
        final int trackerMemFactor;
        int fgTrimLevel;
        LowMemDetector lowMemDetector = this.mLowMemDetector;
        if (lowMemDetector != null && lowMemDetector.isAvailable()) {
            memFactor = this.mLowMemDetector.getMemFactor();
        } else if (numCached <= this.mService.mConstants.CUR_TRIM_CACHED_PROCESSES && numEmpty <= this.mService.mConstants.CUR_TRIM_EMPTY_PROCESSES) {
            int numCachedAndEmpty = numCached + numEmpty;
            if (numCachedAndEmpty <= 3) {
                memFactor = 3;
            } else if (numCachedAndEmpty <= 5) {
                memFactor = 2;
            } else {
                memFactor = 1;
            }
        } else {
            memFactor = 0;
        }
        if (Build.ENABLE_GB_MON_RECLAIM && memFactor >= 2) {
            this.mService.mGBMonitor.reclaimGB(true);
        }
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            Slog.d(TAG_OOM_ADJ, "oom: memFactor=" + memFactor + " override=" + this.mMemFactorOverride + " last=" + this.mLastMemoryLevel + " allowLow=" + this.mAllowLowerMemLevel + " numProcs=" + this.mService.mProcessList.getLruSizeLOSP() + " last=" + this.mLastNumProcesses);
        }
        boolean z2 = this.mMemFactorOverride != -1;
        boolean override = z2;
        if (z2) {
            memFactor = this.mMemFactorOverride;
        }
        if (memFactor > this.mLastMemoryLevel && !override && (!this.mAllowLowerMemLevel || this.mService.mProcessList.getLruSizeLOSP() >= this.mLastNumProcesses)) {
            memFactor = this.mLastMemoryLevel;
            if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                Slog.d(TAG_OOM_ADJ, "Keeping last mem factor!");
            }
        }
        int memFactor2 = memFactor;
        int memFactor3 = this.mLastMemoryLevel;
        if (memFactor2 != memFactor3) {
            EventLogTags.writeAmMemFactor(memFactor2, memFactor3);
            FrameworkStatsLog.write(15, memFactor2);
            this.mBgHandler.obtainMessage(4, this.mLastMemoryLevel, memFactor2).sendToTarget();
        }
        this.mLastMemoryLevel = memFactor2;
        this.mLastNumProcesses = this.mService.mProcessList.getLruSizeLOSP();
        synchronized (this.mService.mProcessStats.mLock) {
            now = SystemClock.uptimeMillis();
            ProcessStatsService processStatsService = this.mService.mProcessStats;
            if (this.mService.mAtmInternal != null && this.mService.mAtmInternal.isSleeping()) {
                z = false;
                allChanged = processStatsService.setMemFactorLocked(memFactor2, z, now);
                trackerMemFactor = this.mService.mProcessStats.getMemFactorLocked();
            }
            z = true;
            allChanged = processStatsService.setMemFactorLocked(memFactor2, z, now);
            trackerMemFactor = this.mService.mProcessStats.getMemFactorLocked();
        }
        if (memFactor2 == 0) {
            long j = this.mLowRamStartTime;
            if (j != 0) {
                this.mLowRamTimeSinceLastIdle += now - j;
                this.mLowRamStartTime = 0L;
            }
            this.mService.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.AppProfiler$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppProfiler.this.m1158lambda$updateLowMemStateLSP$5$comandroidserveramAppProfiler(allChanged, trackerMemFactor, (ProcessRecord) obj);
                }
            });
        } else {
            if (this.mLowRamStartTime == 0) {
                this.mLowRamStartTime = now;
            }
            switch (memFactor2) {
                case 2:
                    fgTrimLevel = 10;
                    break;
                case 3:
                    fgTrimLevel = 15;
                    break;
                default:
                    fgTrimLevel = 5;
                    break;
            }
            int factor = numTrimming / 3;
            int minFactor = this.mHasHomeProcess ? 2 + 1 : 2;
            if (this.mHasPreviousProcess) {
                minFactor++;
            }
            int minFactor2 = minFactor;
            if (factor < minFactor2) {
                factor = minFactor2;
            }
            final int factor2 = factor;
            final int[] step = {0};
            final int[] curLevel = {80};
            final int minFactor3 = fgTrimLevel;
            this.mService.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.AppProfiler$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppProfiler.this.m1157lambda$updateLowMemStateLSP$4$comandroidserveramAppProfiler(allChanged, trackerMemFactor, curLevel, step, factor2, minFactor3, (ProcessRecord) obj);
                }
            });
        }
        return allChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x0088, code lost:
        if (r0 != false) goto L24;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x0099, code lost:
        if (com.android.server.am.AppProfiler.IS_ROOT_ENABLE == false) goto L36;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x009b, code lost:
        r16.mStl.sendBinderMessage();
     */
    /* renamed from: lambda$updateLowMemStateLSP$4$com-android-server-am-AppProfiler  reason: not valid java name */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public /* synthetic */ void m1157lambda$updateLowMemStateLSP$4$comandroidserveramAppProfiler(boolean allChanged, int trackerMemFactor, int[] curLevel, int[] step, int actualFactor, int fgTrimLevel, ProcessRecord app) {
        IApplicationThread thread;
        IApplicationThread thread2;
        IApplicationThread thread3;
        ProcessProfileRecord profile = app.mProfile;
        int trimMemoryLevel = profile.getTrimMemoryLevel();
        ProcessStateRecord state = app.mState;
        int curProcState = state.getCurProcState();
        if (allChanged || state.hasProcStateChanged()) {
            this.mService.setProcessTrackerStateLOSP(app, trackerMemFactor);
            state.setProcStateChanged(false);
        }
        trimMemoryUiHiddenIfNecessaryLSP(app);
        if (curProcState >= 14 && !app.isKilledByAm()) {
            if (trimMemoryLevel < curLevel[0] && (thread3 = app.getThread()) != null) {
                try {
                    if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                        Slog.v(TAG_OOM_ADJ, "Trimming memory of " + app.processName + " to " + curLevel[0]);
                    }
                    boolean z = IS_ROOT_ENABLE;
                    if (z) {
                        this.mStl.sendNoBinderMessage(app.getPid(), app.processName, "scheduleTrimMemory");
                    }
                    thread3.scheduleTrimMemory(curLevel[0]);
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    if (IS_ROOT_ENABLE) {
                        this.mStl.sendBinderMessage();
                    }
                    throw th;
                }
            }
            profile.setTrimMemoryLevel(curLevel[0]);
            step[0] = step[0] + 1;
            if (step[0] >= actualFactor) {
                step[0] = 0;
                switch (curLevel[0]) {
                    case 60:
                        curLevel[0] = 40;
                        return;
                    case 80:
                        curLevel[0] = 60;
                        return;
                    default:
                        return;
                }
            }
        } else if (curProcState == 13 && !app.isKilledByAm()) {
            if (trimMemoryLevel < 40 && (thread2 = app.getThread()) != null) {
                try {
                    if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                        Slog.v(TAG_OOM_ADJ, "Trimming memory of heavy-weight " + app.processName + " to 40");
                    }
                    thread2.scheduleTrimMemory(40);
                } catch (RemoteException e2) {
                }
            }
            profile.setTrimMemoryLevel(40);
        } else {
            if (trimMemoryLevel < fgTrimLevel && (thread = app.getThread()) != null) {
                try {
                    if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                        Slog.v(TAG_OOM_ADJ, "Trimming memory of fg " + app.processName + " to " + fgTrimLevel);
                    }
                    thread.scheduleTrimMemory(fgTrimLevel);
                } catch (RemoteException e3) {
                }
            }
            profile.setTrimMemoryLevel(fgTrimLevel);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateLowMemStateLSP$5$com-android-server-am-AppProfiler  reason: not valid java name */
    public /* synthetic */ void m1158lambda$updateLowMemStateLSP$5$comandroidserveramAppProfiler(boolean allChanged, int trackerMemFactor, ProcessRecord app) {
        ProcessProfileRecord profile = app.mProfile;
        ProcessStateRecord state = app.mState;
        if (allChanged || state.hasProcStateChanged()) {
            this.mService.setProcessTrackerStateLOSP(app, trackerMemFactor);
            state.setProcStateChanged(false);
        }
        trimMemoryUiHiddenIfNecessaryLSP(app);
        profile.setTrimMemoryLevel(0);
    }

    private void trimMemoryUiHiddenIfNecessaryLSP(ProcessRecord app) {
        IApplicationThread thread;
        if ((app.mState.getCurProcState() >= 7 || app.mState.isSystemNoUi()) && app.mProfile.hasPendingUiClean()) {
            if (app.mProfile.getTrimMemoryLevel() < 20 && (thread = app.getThread()) != null) {
                try {
                    if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                        Slog.v(TAG_OOM_ADJ, "Trimming memory of bg-ui " + app.processName + " to 20");
                    }
                    thread.scheduleTrimMemory(20);
                } catch (RemoteException e) {
                }
            }
            app.mProfile.setPendingUiClean(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLowRamTimeSinceIdleLPr(long now) {
        long j = this.mLowRamTimeSinceLastIdle;
        long j2 = this.mLowRamStartTime;
        return j + (j2 > 0 ? now - j2 : 0L);
    }

    private void performAppGcLPf(ProcessRecord app) {
        try {
            ProcessProfileRecord profile = app.mProfile;
            profile.setLastRequestedGc(SystemClock.uptimeMillis());
            IApplicationThread thread = profile.getThread();
            if (thread != null) {
                if (profile.getReportLowMemory()) {
                    profile.setReportLowMemory(false);
                    thread.scheduleLowMemory();
                } else {
                    thread.processInBackground();
                }
            }
        } catch (Exception e) {
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:7:0x0011  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void performAppGcsLPf() {
        if (this.mProcessesToGc.size() <= 0) {
            return;
        }
        while (this.mProcessesToGc.size() > 0) {
            ProcessRecord proc = this.mProcessesToGc.remove(0);
            ProcessProfileRecord profile = proc.mProfile;
            if (profile.getCurRawAdj() > 200 || profile.getReportLowMemory()) {
                if (profile.getLastRequestedGc() + this.mService.mConstants.GC_MIN_INTERVAL <= SystemClock.uptimeMillis()) {
                    performAppGcLPf(proc);
                    scheduleAppGcsLPf();
                    return;
                }
                addProcessToGcListLPf(proc);
                scheduleAppGcsLPf();
            }
            while (this.mProcessesToGc.size() > 0) {
            }
        }
        scheduleAppGcsLPf();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void performAppGcsIfAppropriateLocked() {
        synchronized (this.mProfilerLock) {
            if (this.mService.canGcNowLocked()) {
                performAppGcsLPf();
            } else {
                scheduleAppGcsLPf();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void scheduleAppGcsLPf() {
        this.mService.mHandler.removeMessages(5);
        if (this.mProcessesToGc.size() > 0) {
            ProcessRecord proc = this.mProcessesToGc.get(0);
            Message msg = this.mService.mHandler.obtainMessage(5);
            long when = proc.mProfile.getLastRequestedGc() + this.mService.mConstants.GC_MIN_INTERVAL;
            long now = SystemClock.uptimeMillis();
            if (when < this.mService.mConstants.GC_TIMEOUT + now) {
                when = now + this.mService.mConstants.GC_TIMEOUT;
            }
            this.mService.mHandler.sendMessageAtTime(msg, when);
        }
    }

    private void addProcessToGcListLPf(ProcessRecord proc) {
        boolean added = false;
        int i = this.mProcessesToGc.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            } else if (this.mProcessesToGc.get(i).mProfile.getLastRequestedGc() >= proc.mProfile.getLastRequestedGc()) {
                i--;
            } else {
                added = true;
                this.mProcessesToGc.add(i + 1, proc);
                break;
            }
        }
        if (!added) {
            this.mProcessesToGc.add(0, proc);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void doLowMemReportIfNeededLocked(final ProcessRecord dyingProc) {
        if (!this.mService.mProcessList.haveBackgroundProcessLOSP()) {
            boolean doReport = Build.IS_DEBUGGABLE;
            final long now = SystemClock.uptimeMillis();
            if (doReport) {
                if (now < this.mLastMemUsageReportTime + BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS) {
                    doReport = false;
                } else {
                    this.mLastMemUsageReportTime = now;
                }
            }
            int lruSize = this.mService.mProcessList.getLruSizeLOSP();
            final ArrayList<ProcessMemInfo> memInfos = doReport ? new ArrayList<>(lruSize) : null;
            EventLogTags.writeAmLowMemory(lruSize);
            this.mService.mProcessList.forEachLruProcessesLOSP(false, new Consumer() { // from class: com.android.server.am.AppProfiler$$ExternalSyntheticLambda8
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppProfiler.this.m1154x77177fd0(dyingProc, memInfos, now, (ProcessRecord) obj);
                }
            });
            if (doReport) {
                Message msg = this.mService.mHandler.obtainMessage(33, memInfos);
                this.mService.mHandler.sendMessage(msg);
            }
        }
        synchronized (this.mProfilerLock) {
            scheduleAppGcsLPf();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$doLowMemReportIfNeededLocked$6$com-android-server-am-AppProfiler  reason: not valid java name */
    public /* synthetic */ void m1154x77177fd0(ProcessRecord dyingProc, ArrayList memInfos, long now, ProcessRecord rec) {
        if (rec == dyingProc || rec.getThread() == null) {
            return;
        }
        ProcessStateRecord state = rec.mState;
        if (memInfos != null) {
            memInfos.add(new ProcessMemInfo(rec.processName, rec.getPid(), state.getSetAdj(), state.getSetProcState(), state.getAdjType(), state.makeAdjReason()));
        }
        ProcessProfileRecord profile = rec.mProfile;
        if (profile.getLastLowMemory() + this.mService.mConstants.GC_MIN_INTERVAL <= now) {
            synchronized (this.mProfilerLock) {
                if (state.getSetAdj() <= 400) {
                    profile.setLastRequestedGc(0L);
                } else {
                    profile.setLastRequestedGc(profile.getLastLowMemory());
                }
                profile.setReportLowMemory(true);
                profile.setLastLowMemory(now);
                this.mProcessesToGc.remove(rec);
                addProcessToGcListLPf(rec);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:171:0x0587 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void reportMemUsage(ArrayList<ProcessMemInfo> memInfos) {
        long gpuDmaBufUsage;
        long kernelUsed;
        long now;
        long cachedPss;
        long totalMemtrackGraphics;
        SparseArray<ProcessMemInfo> infoMap;
        ArrayList<ProcessMemInfo> arrayList = memInfos;
        SparseArray<ProcessMemInfo> infoMap2 = new SparseArray<>(memInfos.size());
        int size = memInfos.size();
        for (int i = 0; i < size; i++) {
            ProcessMemInfo mi = arrayList.get(i);
            infoMap2.put(mi.pid, mi);
        }
        updateCpuStatsNow();
        long[] memtrackTmp = new long[4];
        long[] swaptrackTmp = new long[2];
        List<ProcessCpuTracker.Stats> stats = getCpuStats(new Predicate() { // from class: com.android.server.am.AppProfiler$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AppProfiler.lambda$reportMemUsage$7((ProcessCpuTracker.Stats) obj);
            }
        });
        int statsCount = stats.size();
        long totalMemtrackGraphics2 = 0;
        long totalMemtrackGl = 0;
        int i2 = 0;
        while (i2 < statsCount) {
            ProcessCpuTracker.Stats st = stats.get(i2);
            long pss = Debug.getPss(st.pid, swaptrackTmp, memtrackTmp);
            if (pss <= 0) {
                infoMap = infoMap2;
            } else if (infoMap2.indexOfKey(st.pid) < 0) {
                infoMap = infoMap2;
                ProcessMemInfo mi2 = new ProcessMemInfo(st.name, st.pid, -1000, -1, "native", null);
                mi2.pss = pss;
                mi2.swapPss = swaptrackTmp[1];
                mi2.memtrack = memtrackTmp[0];
                totalMemtrackGraphics2 += memtrackTmp[1];
                totalMemtrackGl += memtrackTmp[2];
                arrayList.add(mi2);
            } else {
                infoMap = infoMap2;
            }
            i2++;
            infoMap2 = infoMap;
        }
        long totalPss = 0;
        long totalSwapPss = 0;
        long totalMemtrack = 0;
        int i3 = 0;
        int size2 = memInfos.size();
        while (i3 < size2) {
            ProcessMemInfo mi3 = arrayList.get(i3);
            List<ProcessCpuTracker.Stats> stats2 = stats;
            int statsCount2 = statsCount;
            if (mi3.pss == 0) {
                mi3.pss = Debug.getPss(mi3.pid, swaptrackTmp, memtrackTmp);
                mi3.swapPss = swaptrackTmp[1];
                i3 = i3;
                mi3.memtrack = memtrackTmp[0];
                totalMemtrackGraphics2 += memtrackTmp[1];
                totalMemtrackGl += memtrackTmp[2];
            }
            totalPss += mi3.pss;
            totalSwapPss += mi3.swapPss;
            totalMemtrack += mi3.memtrack;
            i3++;
            arrayList = memInfos;
            stats = stats2;
            statsCount = statsCount2;
        }
        Collections.sort(memInfos, new Comparator<ProcessMemInfo>() { // from class: com.android.server.am.AppProfiler.2
            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.Comparator
            public int compare(ProcessMemInfo lhs, ProcessMemInfo rhs) {
                if (lhs.oomAdj != rhs.oomAdj) {
                    return lhs.oomAdj < rhs.oomAdj ? -1 : 1;
                } else if (lhs.pss != rhs.pss) {
                    return lhs.pss < rhs.pss ? 1 : -1;
                } else {
                    return 0;
                }
            }
        });
        StringBuilder tag = new StringBuilder(128);
        StringBuilder stack = new StringBuilder(128);
        tag.append("Low on memory -- ");
        ActivityManagerService.appendMemBucket(tag, totalPss, "total", false);
        ActivityManagerService.appendMemBucket(stack, totalPss, "total", true);
        StringBuilder fullNativeBuilder = new StringBuilder(1024);
        StringBuilder shortNativeBuilder = new StringBuilder(1024);
        StringBuilder fullJavaBuilder = new StringBuilder(1024);
        boolean firstLine = true;
        long cachedPss2 = 0;
        int size3 = memInfos.size();
        long totalSwapPss2 = totalSwapPss;
        int lastOomAdj = Integer.MIN_VALUE;
        long extraNativeRam = 0;
        long extraNativeMemtrack = 0;
        int i4 = 0;
        while (i4 < size3) {
            ProcessMemInfo mi4 = memInfos.get(i4);
            long totalMemtrackGl2 = totalMemtrackGl;
            if (mi4.oomAdj < 900) {
                cachedPss = cachedPss2;
            } else {
                cachedPss = cachedPss2 + mi4.pss;
            }
            long cachedPss3 = cachedPss;
            if (mi4.oomAdj == -1000) {
                totalMemtrackGraphics = totalMemtrackGraphics2;
            } else if (mi4.oomAdj < 500 || mi4.oomAdj == 600 || mi4.oomAdj == 700) {
                if (lastOomAdj != mi4.oomAdj) {
                    lastOomAdj = mi4.oomAdj;
                    if (mi4.oomAdj <= 0) {
                        tag.append(" / ");
                    }
                    if (mi4.oomAdj >= 0) {
                        if (firstLine) {
                            stack.append(":");
                            firstLine = false;
                        }
                        stack.append("\n\t at ");
                    } else {
                        stack.append("$");
                    }
                } else {
                    tag.append(" ");
                    stack.append("$");
                }
                if (mi4.oomAdj > 0) {
                    totalMemtrackGraphics = totalMemtrackGraphics2;
                } else {
                    totalMemtrackGraphics = totalMemtrackGraphics2;
                    ActivityManagerService.appendMemBucket(tag, mi4.pss, mi4.name, false);
                }
                long totalMemtrackGraphics3 = mi4.pss;
                ActivityManagerService.appendMemBucket(stack, totalMemtrackGraphics3, mi4.name, true);
                if (mi4.oomAdj >= 0 && (i4 + 1 >= size3 || memInfos.get(i4 + 1).oomAdj != lastOomAdj)) {
                    stack.append("(");
                    for (int k = 0; k < ActivityManagerService.DUMP_MEM_OOM_ADJ.length; k++) {
                        if (ActivityManagerService.DUMP_MEM_OOM_ADJ[k] == mi4.oomAdj) {
                            stack.append(ActivityManagerService.DUMP_MEM_OOM_LABEL[k]);
                            stack.append(":");
                            stack.append(ActivityManagerService.DUMP_MEM_OOM_ADJ[k]);
                        }
                    }
                    stack.append(")");
                }
            } else {
                totalMemtrackGraphics = totalMemtrackGraphics2;
            }
            boolean firstLine2 = firstLine;
            ActivityManagerService.appendMemInfo(fullNativeBuilder, mi4);
            if (mi4.oomAdj == -1000) {
                if (mi4.pss >= 512) {
                    ActivityManagerService.appendMemInfo(shortNativeBuilder, mi4);
                } else {
                    extraNativeRam += mi4.pss;
                    extraNativeMemtrack += mi4.memtrack;
                }
            } else {
                if (extraNativeRam > 0) {
                    ActivityManagerService.appendBasicMemEntry(shortNativeBuilder, -1000, -1, extraNativeRam, extraNativeMemtrack, "(Other native)");
                    shortNativeBuilder.append('\n');
                    extraNativeRam = 0;
                }
                ActivityManagerService.appendMemInfo(fullJavaBuilder, mi4);
            }
            i4++;
            firstLine = firstLine2;
            totalMemtrackGl = totalMemtrackGl2;
            cachedPss2 = cachedPss3;
            totalMemtrackGraphics2 = totalMemtrackGraphics;
        }
        long totalMemtrackGraphics4 = totalMemtrackGraphics2;
        long totalMemtrackGl3 = totalMemtrackGl;
        fullJavaBuilder.append("           ");
        ProcessList.appendRamKb(fullJavaBuilder, totalPss);
        fullJavaBuilder.append(": TOTAL");
        if (totalMemtrack > 0) {
            fullJavaBuilder.append(" (");
            fullJavaBuilder.append(ActivityManagerService.stringifyKBSize(totalMemtrack));
            fullJavaBuilder.append(" memtrack)");
        }
        fullJavaBuilder.append("\n");
        MemInfoReader memInfo = new MemInfoReader();
        memInfo.readMemInfo();
        long[] infos = memInfo.getRawInfo();
        StringBuilder memInfoBuilder = new StringBuilder(1024);
        Debug.getMemInfo(infos);
        memInfoBuilder.append("  MemInfo: ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[5])).append(" slab, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[4])).append(" shmem, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[12])).append(" vm alloc, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[13])).append(" page tables ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[14])).append(" kernel stack\n");
        memInfoBuilder.append("           ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[2])).append(" buffers, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[3])).append(" cached, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[11])).append(" mapped, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[1])).append(" free\n");
        if (infos[10] != 0) {
            memInfoBuilder.append("  ZRAM: ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[10]));
            memInfoBuilder.append(" RAM, ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[8]));
            memInfoBuilder.append(" swap total, ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[9]));
            memInfoBuilder.append(" swap free\n");
        }
        long[] ksm = ActivityManagerService.getKsmInfo();
        if (ksm[1] != 0 || ksm[0] != 0 || ksm[2] != 0 || ksm[3] != 0) {
            memInfoBuilder.append("  KSM: ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ksm[1]));
            memInfoBuilder.append(" saved from shared ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ksm[0]));
            memInfoBuilder.append("\n       ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ksm[2]));
            memInfoBuilder.append(" unshared; ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ksm[3]));
            memInfoBuilder.append(" volatile\n");
        }
        memInfoBuilder.append("  Free RAM: ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(cachedPss2 + memInfo.getCachedSizeKb() + memInfo.getFreeSizeKb()));
        memInfoBuilder.append("\n");
        long kernelUsed2 = memInfo.getKernelUsedSizeKb();
        long ionHeap = Debug.getIonHeapsSizeKb();
        long ionPool = Debug.getIonPoolsSizeKb();
        long dmabufMapped = Debug.getDmabufMappedSizeKb();
        if (ionHeap >= 0 && ionPool >= 0) {
            long ionUnmapped = ionHeap - dmabufMapped;
            memInfoBuilder.append("       ION: ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ionHeap + ionPool));
            memInfoBuilder.append("\n");
            kernelUsed2 += ionUnmapped;
            totalPss = (totalPss - totalMemtrackGraphics4) + dmabufMapped;
        } else {
            long totalExportedDmabuf = Debug.getDmabufTotalExportedKb();
            if (totalExportedDmabuf >= 0) {
                long dmabufUnmapped = totalExportedDmabuf - dmabufMapped;
                memInfoBuilder.append("DMA-BUF: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(totalExportedDmabuf));
                memInfoBuilder.append("\n");
                kernelUsed2 += dmabufUnmapped;
                totalPss = (totalPss - totalMemtrackGraphics4) + dmabufMapped;
            }
            long totalExportedDmabufHeap = Debug.getDmabufHeapTotalExportedKb();
            if (totalExportedDmabufHeap >= 0) {
                memInfoBuilder.append("DMA-BUF Heap: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(totalExportedDmabufHeap));
                memInfoBuilder.append("\n");
            }
            long totalDmabufHeapPool = Debug.getDmabufHeapPoolsSizeKb();
            if (totalDmabufHeapPool >= 0) {
                memInfoBuilder.append("DMA-BUF Heaps pool: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(totalDmabufHeapPool));
                memInfoBuilder.append("\n");
            }
        }
        long gpuUsage = Debug.getGpuTotalUsageKb();
        if (gpuUsage >= 0) {
            long gpuPrivateUsage = Debug.getGpuPrivateMemoryKb();
            if (gpuPrivateUsage >= 0) {
                long gpuDmaBufUsage2 = gpuUsage - gpuPrivateUsage;
                memInfoBuilder.append("      GPU: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(gpuUsage));
                memInfoBuilder.append(" (");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(gpuDmaBufUsage2));
                memInfoBuilder.append(" dmabuf + ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(gpuPrivateUsage));
                memInfoBuilder.append(" private)\n");
                gpuDmaBufUsage = kernelUsed2 + gpuPrivateUsage;
                kernelUsed = totalPss - totalMemtrackGl3;
                memInfoBuilder.append("  Used RAM: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize((kernelUsed - cachedPss2) + gpuDmaBufUsage));
                memInfoBuilder.append("\n");
                memInfoBuilder.append("  Lost RAM: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(((((memInfo.getTotalSizeKb() - (kernelUsed - totalSwapPss2)) - memInfo.getFreeSizeKb()) - memInfo.getCachedSizeKb()) - gpuDmaBufUsage) - memInfo.getZramTotalSizeKb()));
                memInfoBuilder.append("\n");
                Slog.i(TAG, "Low on memory:");
                Slog.i(TAG, shortNativeBuilder.toString());
                Slog.i(TAG, fullJavaBuilder.toString());
                Slog.i(TAG, memInfoBuilder.toString());
                StringBuilder dropBuilder = new StringBuilder(1024);
                dropBuilder.append("Low on memory:");
                dropBuilder.append((CharSequence) stack);
                dropBuilder.append('\n');
                dropBuilder.append((CharSequence) fullNativeBuilder);
                dropBuilder.append((CharSequence) fullJavaBuilder);
                dropBuilder.append('\n');
                dropBuilder.append((CharSequence) memInfoBuilder);
                dropBuilder.append('\n');
                StringWriter catSw = new StringWriter();
                synchronized (this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        try {
                            PrintWriter catPw = new FastPrintWriter(catSw, false, 256);
                            String[] emptyArgs = new String[0];
                            catPw.println();
                            synchronized (this.mProcLock) {
                                try {
                                    ActivityManagerService.boostPriorityForProcLockedSection();
                                    this.mService.mProcessList.dumpProcessesLSP(null, catPw, emptyArgs, 0, false, null, -1);
                                }
                            }
                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                            catPw.println();
                            this.mService.mServices.newServiceDumperLocked(null, catPw, emptyArgs, 0, false, null).dumpLocked();
                            catPw.println();
                            this.mService.mAtmInternal.dump(ActivityTaskManagerService.DUMP_ACTIVITIES_CMD, null, catPw, emptyArgs, 0, false, false, null);
                            catPw.flush();
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            dropBuilder.append(catSw.toString());
                            FrameworkStatsLog.write(81);
                            this.mService.addErrorToDropBox("lowmem", null, "system_server", null, null, null, tag.toString(), dropBuilder.toString(), null, null, null, null, null);
                            synchronized (this.mService) {
                                try {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        now = SystemClock.uptimeMillis();
                                    } catch (Throwable th) {
                                        th = th;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                                try {
                                    if (this.mLastMemUsageReportTime < now) {
                                        this.mLastMemUsageReportTime = now;
                                    }
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    return;
                                } catch (Throwable th3) {
                                    th = th3;
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                    }
                }
            } else {
                memInfoBuilder.append("       GPU: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(gpuUsage));
                memInfoBuilder.append("\n");
            }
        }
        gpuDmaBufUsage = kernelUsed2;
        kernelUsed = totalPss;
        memInfoBuilder.append("  Used RAM: ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize((kernelUsed - cachedPss2) + gpuDmaBufUsage));
        memInfoBuilder.append("\n");
        memInfoBuilder.append("  Lost RAM: ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(((((memInfo.getTotalSizeKb() - (kernelUsed - totalSwapPss2)) - memInfo.getFreeSizeKb()) - memInfo.getCachedSizeKb()) - gpuDmaBufUsage) - memInfo.getZramTotalSizeKb()));
        memInfoBuilder.append("\n");
        Slog.i(TAG, "Low on memory:");
        Slog.i(TAG, shortNativeBuilder.toString());
        Slog.i(TAG, fullJavaBuilder.toString());
        Slog.i(TAG, memInfoBuilder.toString());
        StringBuilder dropBuilder2 = new StringBuilder(1024);
        dropBuilder2.append("Low on memory:");
        dropBuilder2.append((CharSequence) stack);
        dropBuilder2.append('\n');
        dropBuilder2.append((CharSequence) fullNativeBuilder);
        dropBuilder2.append((CharSequence) fullJavaBuilder);
        dropBuilder2.append('\n');
        dropBuilder2.append((CharSequence) memInfoBuilder);
        dropBuilder2.append('\n');
        StringWriter catSw2 = new StringWriter();
        synchronized (this.mService) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$reportMemUsage$7(ProcessCpuTracker.Stats st) {
        return st.vsize > 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMemoryPressureChangedLocked(int oldMemFactor, int newMemFactor) {
        this.mService.mServices.rescheduleServiceRestartOnMemoryPressureIfNeededLocked(oldMemFactor, newMemFactor, "mem-pressure-event", SystemClock.uptimeMillis());
    }

    private void stopProfilerLPf(ProcessRecord proc, int profileType) {
        IApplicationThread thread;
        if (proc == null || proc == this.mProfileData.getProfileProc()) {
            proc = this.mProfileData.getProfileProc();
            profileType = this.mProfileType;
            clearProfilerLPf();
        }
        if (proc == null || (thread = proc.mProfile.getThread()) == null) {
            return;
        }
        try {
            thread.profilerControl(false, (ProfilerInfo) null, profileType);
        } catch (RemoteException e) {
            throw new IllegalStateException("Process disappeared");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearProfilerLPf() {
        if (this.mProfileData.getProfilerInfo() != null && this.mProfileData.getProfilerInfo().profileFd != null) {
            try {
                this.mProfileData.getProfilerInfo().profileFd.close();
            } catch (IOException e) {
            }
        }
        this.mProfileData.setProfileApp(null);
        this.mProfileData.setProfileProc(null);
        this.mProfileData.setProfilerInfo(null);
    }

    void clearProfilerLPf(ProcessRecord app) {
        if (this.mProfileData.getProfileProc() == null || this.mProfileData.getProfilerInfo() == null || this.mProfileData.getProfileProc() != app) {
            return;
        }
        clearProfilerLPf();
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, MOVE_EXCEPTION, IGET, INVOKE, MOVE_EXCEPTION, IF, IGET] complete} */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean profileControlLPf(ProcessRecord proc, boolean start, ProfilerInfo profilerInfo, int profileType) {
        ParcelFileDescriptor fd;
        try {
            try {
                if (start) {
                    stopProfilerLPf(null, 0);
                    this.mService.setProfileApp(proc.info, proc.processName, profilerInfo, proc.isSdkSandbox ? proc.getClientInfoForSdkSandbox() : null);
                    this.mProfileData.setProfileProc(proc);
                    this.mProfileType = profileType;
                    ParcelFileDescriptor fd2 = profilerInfo.profileFd;
                    try {
                        fd = fd2.dup();
                    } catch (IOException e) {
                        fd = null;
                    }
                    profilerInfo.profileFd = fd;
                    proc.mProfile.getThread().profilerControl(start, profilerInfo, profileType);
                    try {
                        this.mProfileData.getProfilerInfo().profileFd.close();
                    } catch (IOException e2) {
                    }
                    this.mProfileData.getProfilerInfo().profileFd = null;
                    if (proc.getPid() == ActivityManagerService.MY_PID) {
                        profilerInfo = null;
                    }
                } else {
                    stopProfilerLPf(proc, profileType);
                    if (profilerInfo != null && profilerInfo.profileFd != null) {
                        try {
                            profilerInfo.profileFd.close();
                        } catch (IOException e3) {
                        }
                    }
                }
                return true;
            } finally {
                if (profilerInfo != null && profilerInfo.profileFd != null) {
                    try {
                        profilerInfo.profileFd.close();
                    } catch (IOException e4) {
                    }
                }
            }
        } catch (RemoteException e5) {
            throw new IllegalStateException("Process disappeared");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setProfileAppLPf(String processName, ProfilerInfo profilerInfo) {
        this.mProfileData.setProfileApp(processName);
        if (this.mProfileData.getProfilerInfo() != null && this.mProfileData.getProfilerInfo().profileFd != null) {
            try {
                this.mProfileData.getProfilerInfo().profileFd.close();
            } catch (IOException e) {
            }
        }
        this.mProfileData.setProfilerInfo(new ProfilerInfo(profilerInfo));
        this.mProfileType = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setProfileProcLPf(ProcessRecord proc) {
        this.mProfileData.setProfileProc(proc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAgentAppLPf(String packageName, String agent) {
        if (agent == null) {
            Map<String, String> map = this.mAppAgentMap;
            if (map != null) {
                map.remove(packageName);
                if (this.mAppAgentMap.isEmpty()) {
                    this.mAppAgentMap = null;
                    return;
                }
                return;
            }
            return;
        }
        if (this.mAppAgentMap == null) {
            this.mAppAgentMap = new HashMap();
        }
        if (this.mAppAgentMap.size() >= 100) {
            Slog.e(TAG, "App agent map has too many entries, cannot add " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + agent);
        } else {
            this.mAppAgentMap.put(packageName, agent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateCpuStats() {
        long now = SystemClock.uptimeMillis();
        if (this.mLastCpuTime.get() < now - MONITOR_CPU_MIN_TIME && this.mProcessCpuMutexFree.compareAndSet(true, false)) {
            synchronized (this.mProcessCpuThread) {
                this.mProcessCpuThread.notify();
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1962=6, 1979=4] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:101:0x0259 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:161:0x01da */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r9v24 */
    /* JADX WARN: Type inference failed for: r9v33 */
    /* JADX WARN: Type inference failed for: r9v5 */
    /* JADX WARN: Type inference failed for: r9v6 */
    public void updateCpuStatsNow() {
        long now;
        boolean monitorPhantomProcs;
        ProcessProfileRecord profile;
        BatteryStatsImpl.Uid.Proc ps;
        int totalUTime;
        long now2;
        ProcessProfileRecord profile2;
        ProcessCpuTracker.Stats st;
        BatteryStatsImpl.Uid.Proc ps2;
        int statsCount;
        int i;
        String str;
        boolean monitorPhantomProcs2 = this.mService.mSystemReady && FeatureFlagUtils.isEnabled(this.mService.mContext, "settings_enable_monitor_phantom_procs");
        synchronized (this.mProcessCpuTracker) {
            try {
                try {
                    this.mProcessCpuMutexFree.set(false);
                    long now3 = SystemClock.uptimeMillis();
                    boolean haveNewCpuStats = false;
                    long j = this.mLastCpuTime.get();
                    long j2 = now3 - MONITOR_CPU_MIN_TIME;
                    int i2 = (j > j2 ? 1 : (j == j2 ? 0 : -1));
                    int i3 = j2;
                    if (i2 < 0) {
                        try {
                            this.mLastCpuTime.set(now3);
                            this.mProcessCpuTracker.update();
                            i3 = j2;
                            if (this.mProcessCpuTracker.hasGoodLastStats()) {
                                haveNewCpuStats = true;
                                i3 = j2;
                                if ("true".equals(SystemProperties.get("events.cpu"))) {
                                    int user = this.mProcessCpuTracker.getLastUserTime();
                                    int system = this.mProcessCpuTracker.getLastSystemTime();
                                    int iowait = this.mProcessCpuTracker.getLastIoWaitTime();
                                    int irq = this.mProcessCpuTracker.getLastIrqTime();
                                    int softIrq = this.mProcessCpuTracker.getLastSoftIrqTime();
                                    int idle = this.mProcessCpuTracker.getLastIdleTime();
                                    int total = user + system + iowait + irq + softIrq + idle;
                                    if (total == 0) {
                                        total = 1;
                                    }
                                    EventLogTags.writeCpu((((((user + system) + iowait) + irq) + softIrq) * 100) / total, (user * 100) / total, (system * 100) / total, (iowait * 100) / total, (irq * 100) / total, (softIrq * 100) / total);
                                    i3 = irq;
                                }
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    boolean haveNewCpuStats2 = haveNewCpuStats;
                    if (monitorPhantomProcs2 && haveNewCpuStats2) {
                        this.mService.mPhantomProcessList.updateProcessCpuStatesLocked(this.mProcessCpuTracker);
                    }
                    BatteryStatsImpl bstats = this.mService.mBatteryStatsService.getActiveStatistics();
                    synchronized (bstats) {
                        try {
                            if (haveNewCpuStats2) {
                                try {
                                    if (bstats.startAddingCpuLocked()) {
                                        int statsCount2 = this.mProcessCpuTracker.countStats();
                                        long elapsedRealtime = SystemClock.elapsedRealtime();
                                        long uptime = SystemClock.uptimeMillis();
                                        ActivityManagerService.PidMap pidMap = this.mService.mPidsSelfLocked;
                                        synchronized (pidMap) {
                                            int totalSTime = 0;
                                            int totalUTime2 = 0;
                                            int totalSTime2 = 0;
                                            ActivityManagerService.PidMap pidMap2 = i3;
                                            while (totalUTime2 < statsCount2) {
                                                try {
                                                    try {
                                                        try {
                                                            ProcessCpuTracker.Stats st2 = this.mProcessCpuTracker.getStats(totalUTime2);
                                                            boolean haveNewCpuStats3 = haveNewCpuStats2;
                                                            try {
                                                                if (st2.working) {
                                                                    monitorPhantomProcs = monitorPhantomProcs2;
                                                                    try {
                                                                        ProcessRecord pr = this.mService.mPidsSelfLocked.get(st2.pid);
                                                                        int totalUTime3 = st2.rel_utime + totalSTime;
                                                                        try {
                                                                            int totalUTime4 = st2.rel_stime;
                                                                            int totalSTime3 = totalSTime2 + totalUTime4;
                                                                            if (pr != null) {
                                                                                try {
                                                                                    profile = pr.mProfile;
                                                                                    ps = profile.getCurProcBatteryStats();
                                                                                } catch (Throwable th2) {
                                                                                    th = th2;
                                                                                    pidMap2 = pidMap;
                                                                                }
                                                                                try {
                                                                                    try {
                                                                                        if (ps != null) {
                                                                                            try {
                                                                                                if (ps.isActive()) {
                                                                                                    totalUTime = totalUTime3;
                                                                                                    now2 = now3;
                                                                                                    profile2 = profile;
                                                                                                    st = st2;
                                                                                                    ps2 = ps;
                                                                                                    ps2.addCpuTimeLocked(st.rel_utime, st.rel_stime);
                                                                                                    long curCpuTime = profile2.mCurCpuTime.addAndGet(st.rel_utime + st.rel_stime);
                                                                                                    profile2.mLastCpuTime.compareAndSet(0L, curCpuTime);
                                                                                                    pidMap2 = pidMap;
                                                                                                    statsCount = statsCount2;
                                                                                                }
                                                                                            } catch (Throwable th3) {
                                                                                                th = th3;
                                                                                                pidMap2 = pidMap;
                                                                                                throw th;
                                                                                            }
                                                                                        }
                                                                                        BatteryStatsImpl.Uid.Proc ps3 = bstats.getProcessStatsLocked(i, str, elapsedRealtime, uptime);
                                                                                        profile2.setCurProcBatteryStats(ps3);
                                                                                        ps2 = ps3;
                                                                                        ps2.addCpuTimeLocked(st.rel_utime, st.rel_stime);
                                                                                        long curCpuTime2 = profile2.mCurCpuTime.addAndGet(st.rel_utime + st.rel_stime);
                                                                                        profile2.mLastCpuTime.compareAndSet(0L, curCpuTime2);
                                                                                        pidMap2 = pidMap;
                                                                                        statsCount = statsCount2;
                                                                                    } catch (Throwable th4) {
                                                                                        th = th4;
                                                                                        pidMap2 = pidMap;
                                                                                        throw th;
                                                                                    }
                                                                                    str = pr.processName;
                                                                                    profile2 = profile;
                                                                                    now2 = now3;
                                                                                    st = st2;
                                                                                } catch (Throwable th5) {
                                                                                    th = th5;
                                                                                    pidMap2 = pidMap;
                                                                                    throw th;
                                                                                }
                                                                                i = pr.info.uid;
                                                                                totalUTime = totalUTime3;
                                                                            } else {
                                                                                totalUTime = totalUTime3;
                                                                                now2 = now3;
                                                                                try {
                                                                                    BatteryStatsImpl.Uid.Proc ps4 = st2.batteryStats;
                                                                                    if (ps4 == null || !ps4.isActive()) {
                                                                                        int i4 = st2.uid;
                                                                                        String str2 = st2.name;
                                                                                        pidMap2 = pidMap;
                                                                                        statsCount = statsCount2;
                                                                                        try {
                                                                                            BatteryStatsImpl.Uid.Proc processStatsLocked = bstats.getProcessStatsLocked(i4, str2, elapsedRealtime, uptime);
                                                                                            ps4 = processStatsLocked;
                                                                                            st2.batteryStats = processStatsLocked;
                                                                                        } catch (Throwable th6) {
                                                                                            th = th6;
                                                                                            throw th;
                                                                                        }
                                                                                    } else {
                                                                                        pidMap2 = pidMap;
                                                                                        statsCount = statsCount2;
                                                                                    }
                                                                                    ps4.addCpuTimeLocked(st2.rel_utime, st2.rel_stime);
                                                                                } catch (Throwable th7) {
                                                                                    th = th7;
                                                                                    pidMap2 = pidMap;
                                                                                }
                                                                            }
                                                                            totalSTime2 = totalSTime3;
                                                                            totalSTime = totalUTime;
                                                                        } catch (Throwable th8) {
                                                                            th = th8;
                                                                            pidMap2 = pidMap;
                                                                        }
                                                                    } catch (Throwable th9) {
                                                                        th = th9;
                                                                        pidMap2 = pidMap;
                                                                    }
                                                                } else {
                                                                    monitorPhantomProcs = monitorPhantomProcs2;
                                                                    now2 = now3;
                                                                    pidMap2 = pidMap;
                                                                    statsCount = statsCount2;
                                                                }
                                                                totalUTime2++;
                                                                pidMap = pidMap2;
                                                                haveNewCpuStats2 = haveNewCpuStats3;
                                                                monitorPhantomProcs2 = monitorPhantomProcs;
                                                                now3 = now2;
                                                                statsCount2 = statsCount;
                                                                pidMap2 = pidMap2;
                                                            } catch (Throwable th10) {
                                                                th = th10;
                                                                pidMap2 = pidMap;
                                                            }
                                                        } catch (Throwable th11) {
                                                            th = th11;
                                                            pidMap2 = pidMap;
                                                        }
                                                    } catch (Throwable th12) {
                                                        th = th12;
                                                        throw th;
                                                    }
                                                } catch (Throwable th13) {
                                                    th = th13;
                                                }
                                            }
                                            now = now3;
                                            int userTime = this.mProcessCpuTracker.getLastUserTime();
                                            int systemTime = this.mProcessCpuTracker.getLastSystemTime();
                                            int iowaitTime = this.mProcessCpuTracker.getLastIoWaitTime();
                                            int irqTime = this.mProcessCpuTracker.getLastIrqTime();
                                            int softIrqTime = this.mProcessCpuTracker.getLastSoftIrqTime();
                                            int idleTime = this.mProcessCpuTracker.getLastIdleTime();
                                            bstats.finishAddingCpuLocked(totalSTime, totalSTime2, userTime, systemTime, iowaitTime, irqTime, softIrqTime, idleTime);
                                        }
                                    } else {
                                        now = now3;
                                    }
                                } catch (Throwable th14) {
                                    th = th14;
                                }
                            } else {
                                now = now3;
                            }
                            try {
                                if (this.mLastWriteTime < now - 1800000) {
                                    this.mLastWriteTime = now;
                                    this.mService.mBatteryStatsService.scheduleWriteToDisk();
                                }
                            } catch (Throwable th15) {
                                th = th15;
                                throw th;
                            }
                        } catch (Throwable th16) {
                            th = th16;
                        }
                    }
                } catch (Throwable th17) {
                    th = th17;
                }
            } catch (Throwable th18) {
                th = th18;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getCpuTimeForPid(int pid) {
        long cpuTimeForPid;
        synchronized (this.mProcessCpuTracker) {
            cpuTimeForPid = this.mProcessCpuTracker.getCpuTimeForPid(pid);
        }
        return cpuTimeForPid;
    }

    List<ProcessCpuTracker.Stats> getCpuStats(final Predicate<ProcessCpuTracker.Stats> predicate) {
        List<ProcessCpuTracker.Stats> stats;
        synchronized (this.mProcessCpuTracker) {
            stats = this.mProcessCpuTracker.getStats(new ProcessCpuTracker.FilterStats() { // from class: com.android.server.am.AppProfiler$$ExternalSyntheticLambda7
                public final boolean needed(ProcessCpuTracker.Stats stats2) {
                    boolean test;
                    test = predicate.test(stats2);
                    return test;
                }
            });
        }
        return stats;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllCpuStats(Consumer<ProcessCpuTracker.Stats> consumer) {
        synchronized (this.mProcessCpuTracker) {
            int numOfStats = this.mProcessCpuTracker.countStats();
            for (int i = 0; i < numOfStats; i++) {
                consumer.accept(this.mProcessCpuTracker.getStats(i));
            }
        }
    }

    public float getTotalCpuPercent() {
        float result;
        synchronized (this.mProcessCpuTracker) {
            result = 100.0f;
            ProcessCpuTracker processCpuTracker = this.mProcessCpuTracker;
            if (processCpuTracker != null) {
                result = processCpuTracker.getTotalCpuPercent();
            } else {
                Slog.e(TAG, "cpu tracker is null");
            }
        }
        return result;
    }

    /* loaded from: classes.dex */
    private class ProcessCpuThread extends Thread {
        ProcessCpuThread(String name) {
            super(name);
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            synchronized (AppProfiler.this.mProcessCpuTracker) {
                try {
                    AppProfiler.this.mProcessCpuInitLatch.countDown();
                    AppProfiler.this.mProcessCpuTracker.init();
                } finally {
                    th = th;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th) {
                            th = th;
                        }
                    }
                }
            }
            while (true) {
                try {
                    try {
                        synchronized (this) {
                            long now = SystemClock.uptimeMillis();
                            long nextCpuDelay = (AppProfiler.this.mLastCpuTime.get() + AppProfiler.MONITOR_CPU_MAX_TIME) - now;
                            long nextWriteDelay = (AppProfiler.this.mLastWriteTime + 1800000) - now;
                            if (nextWriteDelay < nextCpuDelay) {
                                nextCpuDelay = nextWriteDelay;
                            }
                            if (nextCpuDelay > 0) {
                                AppProfiler.this.mProcessCpuMutexFree.set(true);
                                wait(nextCpuDelay);
                            }
                        }
                    } catch (Exception e) {
                        Slog.e(AppProfiler.TAG, "Unexpected exception collecting process stats", e);
                    }
                } catch (InterruptedException e2) {
                }
                AppProfiler.this.updateCpuStatsNow();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class CpuBinder extends Binder {
        private final PriorityDump.PriorityDumper mPriorityDumper = new PriorityDump.PriorityDumper() { // from class: com.android.server.am.AppProfiler.CpuBinder.1
            @Override // com.android.server.utils.PriorityDump.PriorityDumper
            public void dumpCritical(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
                if (!DumpUtils.checkDumpAndUsageStatsPermission(AppProfiler.this.mService.mContext, "cpuinfo", pw)) {
                    return;
                }
                synchronized (AppProfiler.this.mProcessCpuTracker) {
                    if (asProto) {
                        AppProfiler.this.mProcessCpuTracker.dumpProto(fd);
                        return;
                    }
                    pw.print(AppProfiler.this.mProcessCpuTracker.printCurrentLoad());
                    pw.print(AppProfiler.this.mProcessCpuTracker.printCurrentState(SystemClock.uptimeMillis()));
                }
            }
        };

        CpuBinder() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            PriorityDump.dump(this.mPriorityDumper, fd, pw, args);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCpuInfoService() {
        ServiceManager.addService("cpuinfo", new CpuBinder(), false, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppProfiler(ActivityManagerService service, Looper bgLooper, LowMemDetector detector) {
        this.mService = service;
        this.mProcLock = service.mProcLock;
        this.mBgHandler = new BgHandler(bgLooper);
        this.mLowMemDetector = detector;
        if (IS_ROOT_ENABLE) {
            HandlerThread handlerThread = new HandlerThread("KillProcessHandlerOomAdjuster");
            handlerThread.start();
            this.mStl = new SystemUtil(handlerThread.getLooper());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void retrieveSettings() {
        long pssDeferralMs = DeviceConfig.getLong("activity_manager", ACTIVITY_START_PSS_DEFER_CONFIG, 0L);
        DeviceConfig.addOnPropertiesChangedListener("activity_manager", ActivityThread.currentApplication().getMainExecutor(), this.mPssDelayConfigListener);
        this.mPssDeferralTime = pssDeferralMs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityManagerInternalAdded() {
        this.mProcessCpuThread.start();
        try {
            this.mProcessCpuInitLatch.await();
        } catch (InterruptedException e) {
            Slog.wtf(TAG, "Interrupted wait during start", e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted wait during start");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityLaunched() {
        if (this.mPssDeferralTime > 0) {
            Message msg = this.mBgHandler.obtainMessage(2);
            this.mBgHandler.sendMessageAtFrontOfQueue(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:18:0x004c A[Catch: all -> 0x0187, TryCatch #0 {, blocks: (B:4:0x000f, B:6:0x0019, B:8:0x0025, B:10:0x0032, B:12:0x003c, B:18:0x004c, B:20:0x0059, B:22:0x0064, B:28:0x0086, B:30:0x008a, B:32:0x0090, B:34:0x0096, B:36:0x00a0, B:37:0x00bc, B:39:0x00c0, B:41:0x00cf, B:43:0x00d3, B:45:0x00e7, B:47:0x00ef, B:48:0x00f2, B:25:0x0070, B:27:0x0074), top: B:89:0x000f }] */
    /* JADX WARN: Removed duplicated region for block: B:19:0x0058  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x0064 A[Catch: all -> 0x0187, TryCatch #0 {, blocks: (B:4:0x000f, B:6:0x0019, B:8:0x0025, B:10:0x0032, B:12:0x003c, B:18:0x004c, B:20:0x0059, B:22:0x0064, B:28:0x0086, B:30:0x008a, B:32:0x0090, B:34:0x0096, B:36:0x00a0, B:37:0x00bc, B:39:0x00c0, B:41:0x00cf, B:43:0x00d3, B:45:0x00e7, B:47:0x00ef, B:48:0x00f2, B:25:0x0070, B:27:0x0074), top: B:89:0x000f }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public ProfilerInfo setupProfilerInfoLocked(IApplicationThread thread, ProcessRecord app, ActiveInstrumentation instr) throws IOException, RemoteException {
        boolean needsInfo;
        ProfilerInfo profilerInfo = null;
        String preBindAgent = null;
        String processName = app.processName;
        synchronized (this.mProfilerLock) {
            if (this.mProfileData.getProfileApp() != null && this.mProfileData.getProfileApp().equals(processName)) {
                this.mProfileData.setProfileProc(app);
                if (this.mProfileData.getProfilerInfo() != null) {
                    if (this.mProfileData.getProfilerInfo().profileFile == null && !this.mProfileData.getProfilerInfo().attachAgentDuringBind) {
                        needsInfo = false;
                        profilerInfo = !needsInfo ? new ProfilerInfo(this.mProfileData.getProfilerInfo()) : null;
                        if (this.mProfileData.getProfilerInfo().agent != null) {
                            preBindAgent = this.mProfileData.getProfilerInfo().agent;
                        }
                    }
                    needsInfo = true;
                    profilerInfo = !needsInfo ? new ProfilerInfo(this.mProfileData.getProfilerInfo()) : null;
                    if (this.mProfileData.getProfilerInfo().agent != null) {
                    }
                }
            } else if (instr != null && instr.mProfileFile != null) {
                profilerInfo = new ProfilerInfo(instr.mProfileFile, (ParcelFileDescriptor) null, 0, false, false, (String) null, false);
            }
            Map<String, String> map = this.mAppAgentMap;
            if (map != null && map.containsKey(processName) && app.isDebuggable()) {
                this.mAppAgentMap.get(processName);
                if (profilerInfo == null) {
                    profilerInfo = new ProfilerInfo((String) null, (ParcelFileDescriptor) null, 0, false, false, this.mAppAgentMap.get(processName), true);
                } else if (profilerInfo.agent == null) {
                    profilerInfo = profilerInfo.setAgent(this.mAppAgentMap.get(processName), true);
                }
            }
            if (profilerInfo != null && profilerInfo.profileFd != null) {
                profilerInfo.profileFd = profilerInfo.profileFd.dup();
                if (TextUtils.equals(this.mProfileData.getProfileApp(), processName) && this.mProfileData.getProfilerInfo() != null) {
                    clearProfilerLPf();
                }
            }
        }
        if (this.mService.mActiveInstrumentation.size() > 0 && instr == null) {
            for (int i = this.mService.mActiveInstrumentation.size() - 1; i >= 0 && app.getActiveInstrumentation() == null; i--) {
                ActiveInstrumentation aInstr = this.mService.mActiveInstrumentation.get(i);
                if (!aInstr.mFinished && aInstr.mTargetInfo.uid == app.uid) {
                    synchronized (this.mProcLock) {
                        try {
                            ActivityManagerService.boostPriorityForProcLockedSection();
                            if (aInstr.mTargetProcesses.length == 0) {
                                if (aInstr.mTargetInfo.packageName.equals(app.info.packageName)) {
                                    app.setActiveInstrumentation(aInstr);
                                    aInstr.mRunningProcesses.add(app);
                                }
                            } else {
                                String[] strArr = aInstr.mTargetProcesses;
                                int length = strArr.length;
                                int i2 = 0;
                                while (true) {
                                    if (i2 >= length) {
                                        break;
                                    }
                                    String proc = strArr[i2];
                                    if (!proc.equals(app.processName)) {
                                        i2++;
                                    } else {
                                        app.setActiveInstrumentation(aInstr);
                                        aInstr.mRunningProcesses.add(app);
                                        break;
                                    }
                                }
                            }
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                }
            }
        }
        if (preBindAgent != null) {
            thread.attachAgent(preBindAgent);
        }
        if (app.isDebuggable()) {
            thread.attachStartupAgents(app.info.dataDir);
        }
        return profilerInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCleanupApplicationRecordLocked(ProcessRecord app) {
        synchronized (this.mProfilerLock) {
            ProcessProfileRecord profile = app.mProfile;
            this.mProcessesToGc.remove(app);
            this.mPendingPssProfiles.remove(profile);
            profile.abortNextPssTime();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppDiedLocked(ProcessRecord app) {
        synchronized (this.mProfilerLock) {
            if (this.mProfileData.getProfileProc() == app) {
                clearProfilerLPf();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpMemWatchProcessesLPf(PrintWriter pw, boolean needSep) {
        if (this.mMemWatchProcesses.getMap().size() > 0) {
            pw.println("  Mem watch processes:");
            ArrayMap<String, SparseArray<Pair<Long, String>>> procs = this.mMemWatchProcesses.getMap();
            for (int i = procs.size() - 1; i >= 0; i--) {
                String proc = procs.keyAt(i);
                SparseArray<Pair<Long, String>> uids = procs.valueAt(i);
                for (int j = uids.size() - 1; j >= 0; j--) {
                    if (needSep) {
                        pw.println();
                        needSep = false;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("    ").append(proc).append('/');
                    UserHandle.formatUid(sb, uids.keyAt(j));
                    Pair<Long, String> val = uids.valueAt(j);
                    sb.append(": ");
                    DebugUtils.sizeValueToString(((Long) val.first).longValue(), sb);
                    if (val.second != null) {
                        sb.append(", report to ").append((String) val.second);
                    }
                    pw.println(sb.toString());
                }
            }
            pw.print("  mMemWatchDumpProcName=");
            pw.println(this.mMemWatchDumpProcName);
            pw.print("  mMemWatchDumpUri=");
            pw.println(this.mMemWatchDumpUri);
            pw.print("  mMemWatchDumpPid=");
            pw.println(this.mMemWatchDumpPid);
            pw.print("  mMemWatchDumpUid=");
            pw.println(this.mMemWatchDumpUid);
            pw.print("  mMemWatchIsUserInitiated=");
            pw.println(this.mMemWatchIsUserInitiated);
        }
        return needSep;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpProfileDataLocked(PrintWriter pw, String dumpPackage, boolean needSep) {
        if ((this.mProfileData.getProfileApp() != null || this.mProfileData.getProfileProc() != null || (this.mProfileData.getProfilerInfo() != null && (this.mProfileData.getProfilerInfo().profileFile != null || this.mProfileData.getProfilerInfo().profileFd != null))) && (dumpPackage == null || dumpPackage.equals(this.mProfileData.getProfileApp()))) {
            if (needSep) {
                pw.println();
                needSep = false;
            }
            pw.println("  mProfileApp=" + this.mProfileData.getProfileApp() + " mProfileProc=" + this.mProfileData.getProfileProc());
            if (this.mProfileData.getProfilerInfo() != null) {
                pw.println("  mProfileFile=" + this.mProfileData.getProfilerInfo().profileFile + " mProfileFd=" + this.mProfileData.getProfilerInfo().profileFd);
                pw.println("  mSamplingInterval=" + this.mProfileData.getProfilerInfo().samplingInterval + " mAutoStopProfiler=" + this.mProfileData.getProfilerInfo().autoStopProfiler + " mStreamingOutput=" + this.mProfileData.getProfilerInfo().streamingOutput);
                pw.println("  mProfileType=" + this.mProfileType);
            }
        }
        return needSep;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpLastMemoryLevelLocked(PrintWriter pw) {
        int i = this.mLastMemoryLevel;
        switch (i) {
            case 0:
                pw.println("normal)");
                return;
            case 1:
                pw.println("moderate)");
                return;
            case 2:
                pw.println("low)");
                return;
            case 3:
                pw.println("critical)");
                return;
            default:
                pw.print(i);
                pw.println(")");
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpMemoryLevelsLocked(PrintWriter pw) {
        pw.println("  mAllowLowerMemLevel=" + this.mAllowLowerMemLevel + " mLastMemoryLevel=" + this.mLastMemoryLevel + " mLastNumProcesses=" + this.mLastNumProcesses);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeMemWatchProcessToProtoLPf(ProtoOutputStream proto) {
        if (this.mMemWatchProcesses.getMap().size() > 0) {
            long token = proto.start(1146756268064L);
            ArrayMap<String, SparseArray<Pair<Long, String>>> procs = this.mMemWatchProcesses.getMap();
            for (int i = 0; i < procs.size(); i++) {
                String proc = procs.keyAt(i);
                SparseArray<Pair<Long, String>> uids = procs.valueAt(i);
                long ptoken = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
                proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, proc);
                int j = uids.size() - 1;
                while (j >= 0) {
                    long utoken = proto.start(2246267895810L);
                    Pair<Long, String> val = uids.valueAt(j);
                    proto.write(CompanionMessage.MESSAGE_ID, uids.keyAt(j));
                    proto.write(1138166333442L, DebugUtils.sizeValueToString(((Long) val.first).longValue(), new StringBuilder()));
                    proto.write(1138166333443L, (String) val.second);
                    proto.end(utoken);
                    j--;
                    procs = procs;
                }
                proto.end(ptoken);
            }
            long dtoken = proto.start(1146756268034L);
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.mMemWatchDumpProcName);
            proto.write(1138166333446L, this.mMemWatchDumpUri.toString());
            proto.write(1120986464259L, this.mMemWatchDumpPid);
            proto.write(1120986464260L, this.mMemWatchDumpUid);
            proto.write(1133871366149L, this.mMemWatchIsUserInitiated);
            proto.end(dtoken);
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeProfileDataToProtoLocked(ProtoOutputStream proto, String dumpPackage) {
        if (this.mProfileData.getProfileApp() == null && this.mProfileData.getProfileProc() == null) {
            if (this.mProfileData.getProfilerInfo() != null) {
                if (this.mProfileData.getProfilerInfo().profileFile == null && this.mProfileData.getProfilerInfo().profileFd == null) {
                    return;
                }
            } else {
                return;
            }
        }
        if (dumpPackage == null || dumpPackage.equals(this.mProfileData.getProfileApp())) {
            long token = proto.start(1146756268066L);
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.mProfileData.getProfileApp());
            this.mProfileData.getProfileProc().dumpDebug(proto, 1146756268034L);
            if (this.mProfileData.getProfilerInfo() != null) {
                this.mProfileData.getProfilerInfo().dumpDebug(proto, 1146756268035L);
                proto.write(1120986464260L, this.mProfileType);
            }
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeMemoryLevelsToProtoLocked(ProtoOutputStream proto) {
        proto.write(1133871366199L, this.mAllowLowerMemLevel);
        proto.write(1120986464312L, this.mLastMemoryLevel);
        proto.write(1120986464313L, this.mLastNumProcesses);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void printCurrentCpuState(StringBuilder report, long time) {
        synchronized (this.mProcessCpuTracker) {
            report.append(this.mProcessCpuTracker.printCurrentState(time));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Pair<String, String> getAppProfileStatsForDebugging(long time, int linesOfStats) {
        String cpuLoad;
        String stats;
        synchronized (this.mProcessCpuTracker) {
            updateCpuStatsNow();
            cpuLoad = this.mProcessCpuTracker.printCurrentLoad();
            stats = this.mProcessCpuTracker.printCurrentState(time);
        }
        int toIndex = 0;
        int i = 0;
        while (true) {
            if (i > linesOfStats) {
                break;
            }
            int nextIndex = stats.indexOf(10, toIndex);
            if (nextIndex == -1) {
                toIndex = stats.length();
                break;
            }
            toIndex = nextIndex + 1;
            i++;
        }
        return new Pair<>(cpuLoad, stats.substring(0, toIndex));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeProcessesToGcToProto(ProtoOutputStream proto, long fieldId, String dumpPackage) {
        if (this.mProcessesToGc.size() > 0) {
            long now = SystemClock.uptimeMillis();
            int size = this.mProcessesToGc.size();
            for (int i = 0; i < size; i++) {
                ProcessRecord r = this.mProcessesToGc.get(i);
                if (dumpPackage == null || dumpPackage.equals(r.info.packageName)) {
                    long token = proto.start(fieldId);
                    ProcessProfileRecord profile = r.mProfile;
                    r.dumpDebug(proto, 1146756268033L);
                    proto.write(1133871366146L, profile.getReportLowMemory());
                    proto.write(1112396529667L, now);
                    proto.write(1112396529668L, profile.getLastRequestedGc());
                    proto.write(1112396529669L, profile.getLastLowMemory());
                    proto.end(token);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpProcessesToGc(PrintWriter pw, boolean needSep, String dumpPackage) {
        if (this.mProcessesToGc.size() > 0) {
            boolean printed = false;
            long now = SystemClock.uptimeMillis();
            int size = this.mProcessesToGc.size();
            for (int i = 0; i < size; i++) {
                ProcessRecord proc = this.mProcessesToGc.get(i);
                if (dumpPackage == null || dumpPackage.equals(proc.info.packageName)) {
                    if (!printed) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Processes that are waiting to GC:");
                        printed = true;
                    }
                    pw.print("    Process ");
                    pw.println(proc);
                    ProcessProfileRecord profile = proc.mProfile;
                    pw.print("      lowMem=");
                    pw.print(profile.getReportLowMemory());
                    pw.print(", last gced=");
                    pw.print(now - profile.getLastRequestedGc());
                    pw.print(" ms ago, last lowMem=");
                    pw.print(now - profile.getLastLowMemory());
                    pw.println(" ms ago");
                }
            }
        }
        return needSep;
    }
}
