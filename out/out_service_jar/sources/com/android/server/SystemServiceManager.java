package com.android.server;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Trace;
import android.util.ArraySet;
import android.util.Dumpable;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.SystemServerClassLoaderFactory;
import com.android.internal.util.Preconditions;
import com.android.server.SystemService;
import com.android.server.am.HostingRecord;
import com.android.server.pm.ApexManager;
import com.android.server.pm.UserManagerInternal;
import com.android.server.utils.TimingsTraceAndSlog;
import com.transsion.hubcore.server.ITranSystemServer;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public final class SystemServiceManager implements Dumpable {
    private static final boolean DEBUG = false;
    private static final int DEFAULT_MAX_USER_POOL_THREADS = 3;
    private static final int SERVICE_CALL_WARN_TIME_MS = 50;
    private static final String USER_COMPLETED_EVENT = "CompletedEvent";
    private static final long USER_POOL_SHUTDOWN_TIMEOUT_SECONDS = 30;
    private static final String USER_STARTING = "Start";
    private static final String USER_STOPPED = "Cleanup";
    private static final String USER_STOPPING = "Stop";
    private static final String USER_SWITCHING = "Switch";
    private static final String USER_UNLOCKED = "Unlocked";
    private static final String USER_UNLOCKING = "Unlocking";
    private static volatile int sOtherServicesStartIndex;
    private static File sSystemDir;
    private final Context mContext;
    private SystemService.TargetUser mCurrentUser;
    private final int mNumUserPoolThreads;
    private boolean mRuntimeRestarted;
    private long mRuntimeStartElapsedTime;
    private long mRuntimeStartUptime;
    private boolean mSafeMode;
    private UserManagerInternal mUserManagerInternal;
    private static final String TAG = SystemServiceManager.class.getSimpleName();
    private static boolean sUseLifecycleThreadPool = true;
    private int mCurrentPhase = -1;
    private final SparseArray<SystemService.TargetUser> mTargetUsers = new SparseArray<>();
    private List<SystemService> mServices = new ArrayList();
    private Set<String> mServiceClassnames = new ArraySet();

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemServiceManager(Context context) {
        this.mContext = context;
        sUseLifecycleThreadPool = sUseLifecycleThreadPool && !ActivityManager.isLowRamDeviceStatic();
        this.mNumUserPoolThreads = Math.min(Runtime.getRuntime().availableProcessors(), 3);
    }

    public SystemService startService(String className) {
        Class<SystemService> serviceClass = loadClassFromLoader(className, getClass().getClassLoader());
        return startService(serviceClass);
    }

    public SystemService startServiceFromJar(String className, String path) {
        PathClassLoader pathClassLoader = SystemServerClassLoaderFactory.getOrCreateClassLoader(path, getClass().getClassLoader(), isJarInTestApex(path));
        Class<SystemService> serviceClass = loadClassFromLoader(className, pathClassLoader);
        return startService(serviceClass);
    }

    private static boolean isJarInTestApex(String pathStr) {
        Path path = Paths.get(pathStr, new String[0]);
        if (path.getNameCount() >= 2 && path.getName(0).toString().equals("apex")) {
            String apexModuleName = path.getName(1).toString();
            ApexManager apexManager = ApexManager.getInstance();
            String packageName = apexManager.getActivePackageNameForApexModuleName(apexModuleName);
            PackageInfo packageInfo = apexManager.getPackageInfo(packageName, 1);
            if (packageInfo == null || (packageInfo.applicationInfo.flags & 256) == 0) {
                return false;
            }
            return true;
        }
        return false;
    }

    /* JADX DEBUG: Type inference failed for r0v2. Raw type applied. Possible types: java.lang.Class<?>, java.lang.Class<com.android.server.SystemService> */
    private static Class<SystemService> loadClassFromLoader(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Failed to create service " + className + " from class loader " + classLoader.toString() + ": service class not found, usually indicates that the caller should have called PackageManager.hasSystemFeature() to check whether the feature is available on this device before trying to start the services that implement it. Also ensure that the correct path for the classloader is supplied, if applicable.", ex);
        }
    }

    public <T extends SystemService> T startService(Class<T> serviceClass) {
        try {
            String name = serviceClass.getName();
            Slog.i(TAG, "Starting " + name);
            Trace.traceBegin(524288L, "StartService " + name);
            if (!SystemService.class.isAssignableFrom(serviceClass)) {
                throw new RuntimeException("Failed to create " + name + ": service must extend " + SystemService.class.getName());
            }
            try {
                try {
                    try {
                        Constructor<T> constructor = serviceClass.getConstructor(Context.class);
                        T service = constructor.newInstance(this.mContext);
                        startService(service);
                        return service;
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException("Failed to create service " + name + ": service must have a public constructor with a Context argument", ex);
                    }
                } catch (InstantiationException ex2) {
                    throw new RuntimeException("Failed to create service " + name + ": service could not be instantiated", ex2);
                }
            } catch (NoSuchMethodException ex3) {
                throw new RuntimeException("Failed to create service " + name + ": service must have a public constructor with a Context argument", ex3);
            } catch (InvocationTargetException ex4) {
                throw new RuntimeException("Failed to create service " + name + ": service constructor threw an exception", ex4);
            }
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    public void startService(SystemService service) {
        String className = service.getClass().getName();
        if (this.mServiceClassnames.contains(className)) {
            Slog.i(TAG, "Not starting an already started service " + className);
            return;
        }
        this.mServiceClassnames.add(className);
        this.mServices.add(service);
        long time = SystemClock.elapsedRealtime();
        try {
            try {
                service.onStart();
                ITranSystemServer.Instance().onStartSystemService(service);
                warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onStart");
            } catch (RuntimeException ex) {
                throw new RuntimeException("Failed to start service " + service.getClass().getName() + ": onStart threw an exception", ex);
            }
        } catch (Throwable th) {
            ITranSystemServer.Instance().onStartSystemService(service);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sealStartedServices() {
        this.mServiceClassnames = Collections.emptySet();
        this.mServices = Collections.unmodifiableList(this.mServices);
    }

    public void startBootPhase(TimingsTraceAndSlog t, int phase) {
        if (phase <= this.mCurrentPhase) {
            throw new IllegalArgumentException("Next phase must be larger than previous");
        }
        this.mCurrentPhase = phase;
        Slog.i(TAG, "Starting phase " + this.mCurrentPhase);
        try {
            t.traceBegin("OnBootPhase_" + phase);
            int serviceLen = this.mServices.size();
            for (int i = 0; i < serviceLen; i++) {
                SystemService service = this.mServices.get(i);
                long time = SystemClock.elapsedRealtime();
                t.traceBegin("OnBootPhase_" + phase + "_" + service.getClass().getName());
                try {
                    service.onBootPhase(this.mCurrentPhase);
                    warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onBootPhase");
                    t.traceEnd();
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to boot service " + service.getClass().getName() + ": onBootPhase threw an exception during phase " + this.mCurrentPhase, ex);
                }
            }
            t.traceEnd();
            ITranSystemServer.Instance().onStartBootPhase(phase);
            if (phase == 1000) {
                long totalBootTime = SystemClock.uptimeMillis() - this.mRuntimeStartUptime;
                t.logDuration("TotalBootTime", totalBootTime);
                SystemServerInitThreadPool.shutdown();
            }
        } catch (Throwable th) {
            t.traceEnd();
            ITranSystemServer.Instance().onStartBootPhase(phase);
            throw th;
        }
    }

    public boolean isBootCompleted() {
        return this.mCurrentPhase >= 1000;
    }

    public void updateOtherServicesStartIndex() {
        if (!isBootCompleted()) {
            sOtherServicesStartIndex = this.mServices.size();
        }
    }

    public void preSystemReady() {
        this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
    }

    private SystemService.TargetUser getTargetUser(int userId) {
        SystemService.TargetUser targetUser;
        synchronized (this.mTargetUsers) {
            targetUser = this.mTargetUsers.get(userId);
        }
        Preconditions.checkState(targetUser != null, "No TargetUser for " + userId);
        return targetUser;
    }

    private SystemService.TargetUser newTargetUser(int userId) {
        UserInfo userInfo = this.mUserManagerInternal.getUserInfo(userId);
        Preconditions.checkState(userInfo != null, "No UserInfo for " + userId);
        return new SystemService.TargetUser(userInfo);
    }

    public void onUserStarting(TimingsTraceAndSlog t, int userId) {
        EventLog.writeEvent((int) com.android.server.am.EventLogTags.SSM_USER_STARTING, userId);
        SystemService.TargetUser targetUser = newTargetUser(userId);
        synchronized (this.mTargetUsers) {
            this.mTargetUsers.put(userId, targetUser);
        }
        onUser(t, USER_STARTING, null, targetUser);
    }

    public void onUserUnlocking(int userId) {
        EventLog.writeEvent((int) com.android.server.am.EventLogTags.SSM_USER_UNLOCKING, userId);
        onUser(USER_UNLOCKING, userId);
    }

    public void onUserUnlocked(int userId) {
        EventLog.writeEvent((int) com.android.server.am.EventLogTags.SSM_USER_UNLOCKED, userId);
        onUser(USER_UNLOCKED, userId);
    }

    public void onUserSwitching(int from, int to) {
        SystemService.TargetUser prevUser;
        SystemService.TargetUser curUser;
        EventLog.writeEvent((int) com.android.server.am.EventLogTags.SSM_USER_SWITCHING, Integer.valueOf(from), Integer.valueOf(to));
        synchronized (this.mTargetUsers) {
            SystemService.TargetUser prevUser2 = this.mCurrentUser;
            if (prevUser2 == null) {
                prevUser = newTargetUser(from);
            } else {
                if (from != prevUser2.getUserIdentifier()) {
                    Slog.wtf(TAG, "switchUser(" + from + "," + to + "): mCurrentUser is " + this.mCurrentUser + ", it should be " + from);
                }
                prevUser = this.mCurrentUser;
            }
            curUser = getTargetUser(to);
            this.mCurrentUser = curUser;
        }
        onUser(TimingsTraceAndSlog.newAsyncLog(), USER_SWITCHING, prevUser, curUser);
        ITranWindowManagerService.Instance().onUserSwitching(prevUser, curUser);
    }

    public void onUserStopping(int userId) {
        EventLog.writeEvent((int) com.android.server.am.EventLogTags.SSM_USER_STOPPING, userId);
        onUser(USER_STOPPING, userId);
    }

    public void onUserStopped(int userId) {
        EventLog.writeEvent((int) com.android.server.am.EventLogTags.SSM_USER_STOPPED, userId);
        onUser(USER_STOPPED, userId);
        synchronized (this.mTargetUsers) {
            this.mTargetUsers.remove(userId);
        }
    }

    public void onUserCompletedEvent(int userId, int eventFlags) {
        EventLog.writeEvent((int) com.android.server.am.EventLogTags.SSM_USER_COMPLETED_EVENT, Integer.valueOf(userId), Integer.valueOf(eventFlags));
        if (eventFlags == 0) {
            return;
        }
        onUser(TimingsTraceAndSlog.newAsyncLog(), USER_COMPLETED_EVENT, null, getTargetUser(userId), new SystemService.UserCompletedEventType(eventFlags));
    }

    private void onUser(String onWhat, int userId) {
        onUser(TimingsTraceAndSlog.newAsyncLog(), onWhat, null, getTargetUser(userId));
    }

    private void onUser(TimingsTraceAndSlog t, String onWhat, SystemService.TargetUser prevUser, SystemService.TargetUser curUser) {
        onUser(t, onWhat, prevUser, curUser, null);
    }

    /*  JADX ERROR: JadxOverflowException in pass: LoopRegionVisitor
        jadx.core.utils.exceptions.JadxOverflowException: LoopRegionVisitor.assignOnlyInLoop endless recursion
        	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:56)
        	at jadx.core.utils.ErrorsCounter.error(ErrorsCounter.java:30)
        	at jadx.core.dex.attributes.nodes.NotificationAttrNode.addError(NotificationAttrNode.java:18)
        */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:98:0x018a */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:104:0x0254 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:56:0x017d  */
    /* JADX WARN: Removed duplicated region for block: B:59:0x01a7 A[Catch: Exception -> 0x0202, TryCatch #3 {Exception -> 0x0202, blocks: (B:57:0x0186, B:71:0x0204, B:72:0x021a, B:58:0x018a, B:59:0x01a7, B:60:0x01b4, B:61:0x01c1, B:62:0x01ce, B:65:0x01e6, B:66:0x01ee, B:67:0x01f2), top: B:98:0x018a }] */
    /* JADX WARN: Removed duplicated region for block: B:60:0x01b4 A[Catch: Exception -> 0x0202, TryCatch #3 {Exception -> 0x0202, blocks: (B:57:0x0186, B:71:0x0204, B:72:0x021a, B:58:0x018a, B:59:0x01a7, B:60:0x01b4, B:61:0x01c1, B:62:0x01ce, B:65:0x01e6, B:66:0x01ee, B:67:0x01f2), top: B:98:0x018a }] */
    /* JADX WARN: Removed duplicated region for block: B:61:0x01c1 A[Catch: Exception -> 0x0202, TryCatch #3 {Exception -> 0x0202, blocks: (B:57:0x0186, B:71:0x0204, B:72:0x021a, B:58:0x018a, B:59:0x01a7, B:60:0x01b4, B:61:0x01c1, B:62:0x01ce, B:65:0x01e6, B:66:0x01ee, B:67:0x01f2), top: B:98:0x018a }] */
    /* JADX WARN: Removed duplicated region for block: B:62:0x01ce A[Catch: Exception -> 0x0202, TryCatch #3 {Exception -> 0x0202, blocks: (B:57:0x0186, B:71:0x0204, B:72:0x021a, B:58:0x018a, B:59:0x01a7, B:60:0x01b4, B:61:0x01c1, B:62:0x01ce, B:65:0x01e6, B:66:0x01ee, B:67:0x01f2), top: B:98:0x018a }] */
    /* JADX WARN: Removed duplicated region for block: B:63:0x01db  */
    /* JADX WARN: Removed duplicated region for block: B:67:0x01f2 A[Catch: Exception -> 0x0202, TryCatch #3 {Exception -> 0x0202, blocks: (B:57:0x0186, B:71:0x0204, B:72:0x021a, B:58:0x018a, B:59:0x01a7, B:60:0x01b4, B:61:0x01c1, B:62:0x01ce, B:65:0x01e6, B:66:0x01ee, B:67:0x01f2), top: B:98:0x018a }] */
    /* JADX WARN: Removed duplicated region for block: B:77:0x022c  */
    /* JADX WARN: Removed duplicated region for block: B:98:0x018a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void onUser(com.android.server.utils.TimingsTraceAndSlog r25, java.lang.String r26, com.android.server.SystemService.TargetUser r27, com.android.server.SystemService.TargetUser r28, com.android.server.SystemService.UserCompletedEventType r29) {
        /*
            r24 = this;
            r7 = r24
            r8 = r25
            r9 = r26
            r10 = r27
            r11 = r28
            int r12 = r28.getUserIdentifier()
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "ssm."
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.StringBuilder r0 = r0.append(r9)
            java.lang.String r13 = "User-"
            java.lang.StringBuilder r0 = r0.append(r13)
            java.lang.StringBuilder r0 = r0.append(r12)
            java.lang.String r0 = r0.toString()
            r8.traceBegin(r0)
            java.lang.String r0 = com.android.server.SystemServiceManager.TAG
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "Calling on"
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r9)
            java.lang.String r2 = "User "
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r12)
            if (r10 == 0) goto L66
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = " (from "
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r10)
            java.lang.String r3 = ")"
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r2 = r2.toString()
            goto L68
        L66:
            java.lang.String r2 = ""
        L68:
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Slog.i(r0, r1)
            boolean r14 = r7.useThreadPool(r12, r9)
            if (r14 == 0) goto L80
            int r0 = r7.mNumUserPoolThreads
            java.util.concurrent.ExecutorService r0 = java.util.concurrent.Executors.newFixedThreadPool(r0)
            goto L81
        L80:
            r0 = 0
        L81:
            r15 = r0
            java.util.List<com.android.server.SystemService> r0 = r7.mServices
            int r6 = r0.size()
            r0 = 0
            r5 = r0
        L8a:
            java.lang.String r1 = "CompletedEvent"
            if (r5 >= r6) goto L25c
            java.util.List<com.android.server.SystemService> r0 = r7.mServices
            java.lang.Object r0 = r0.get(r5)
            r4 = r0
            com.android.server.SystemService r4 = (com.android.server.SystemService) r4
            java.lang.Class r0 = r4.getClass()
            java.lang.String r3 = r0.getName()
            boolean r0 = r4.isUserSupported(r11)
            if (r0 != 0) goto Lae
            if (r10 == 0) goto Lae
            boolean r0 = r4.isUserSupported(r10)
            r16 = r0
            goto Lb0
        Lae:
            r16 = r0
        Lb0:
            if (r16 != 0) goto Le4
            java.lang.String r0 = com.android.server.SystemServiceManager.TAG
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "Skipping "
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r9)
            java.lang.StringBuilder r1 = r1.append(r13)
            java.lang.StringBuilder r1 = r1.append(r12)
            java.lang.String r2 = " on "
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.String r1 = r1.toString()
            android.util.Slog.i(r0, r1)
            r22 = r5
            r23 = r6
            r21 = r14
            goto L254
        Le4:
            if (r14 == 0) goto Lef
            boolean r17 = r7.useThreadPoolForService(r9, r5)
            if (r17 == 0) goto Lef
            r17 = 1
            goto Lf1
        Lef:
            r17 = 0
        Lf1:
            if (r17 != 0) goto L11c
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r2 = "ssm.on"
            java.lang.StringBuilder r0 = r0.append(r2)
            java.lang.StringBuilder r0 = r0.append(r9)
            java.lang.StringBuilder r0 = r0.append(r13)
            java.lang.StringBuilder r0 = r0.append(r12)
            java.lang.String r2 = "_"
            java.lang.StringBuilder r0 = r0.append(r2)
            java.lang.StringBuilder r0 = r0.append(r3)
            java.lang.String r0 = r0.toString()
            r8.traceBegin(r0)
        L11c:
            long r19 = android.os.SystemClock.elapsedRealtime()
            r0 = -1
            int r2 = r26.hashCode()     // Catch: java.lang.Exception -> L21b
            switch(r2) {
                case -1805606060: goto L163;
                case -1773539708: goto L159;
                case -240492034: goto L14f;
                case -146305277: goto L145;
                case 2587682: goto L13b;
                case 80204866: goto L131;
                case 537825071: goto L129;
                default: goto L128;
            }
        L128:
            goto L179
        L129:
            boolean r1 = r9.equals(r1)     // Catch: java.lang.Exception -> L16d
            if (r1 == 0) goto L128
            r2 = 6
            goto L17a
        L131:
            java.lang.String r1 = "Start"
            boolean r1 = r9.equals(r1)     // Catch: java.lang.Exception -> L16d
            if (r1 == 0) goto L128
            r2 = 1
            goto L17a
        L13b:
            java.lang.String r1 = "Stop"
            boolean r1 = r9.equals(r1)     // Catch: java.lang.Exception -> L16d
            if (r1 == 0) goto L128
            r2 = 4
            goto L17a
        L145:
            java.lang.String r1 = "Unlocked"
            boolean r1 = r9.equals(r1)     // Catch: java.lang.Exception -> L16d
            if (r1 == 0) goto L128
            r2 = 3
            goto L17a
        L14f:
            java.lang.String r1 = "Unlocking"
            boolean r1 = r9.equals(r1)     // Catch: java.lang.Exception -> L16d
            if (r1 == 0) goto L128
            r2 = 2
            goto L17a
        L159:
            java.lang.String r1 = "Cleanup"
            boolean r1 = r9.equals(r1)     // Catch: java.lang.Exception -> L16d
            if (r1 == 0) goto L128
            r2 = 5
            goto L17a
        L163:
            java.lang.String r1 = "Switch"
            boolean r1 = r9.equals(r1)     // Catch: java.lang.Exception -> L16d
            if (r1 == 0) goto L128
            r2 = 0
            goto L17a
        L16d:
            r0 = move-exception
            r18 = r3
            r22 = r5
            r23 = r6
            r21 = r14
            r14 = r4
            goto L225
        L179:
            r2 = r0
        L17a:
            switch(r2) {
                case 0: goto L1f2;
                case 1: goto L1db;
                case 2: goto L1ce;
                case 3: goto L1c1;
                case 4: goto L1b4;
                case 5: goto L1a7;
                case 6: goto L18a;
                default: goto L17d;
            }
        L17d:
            r18 = r3
            r22 = r5
            r23 = r6
            r21 = r14
            r14 = r4
            java.lang.IllegalArgumentException r0 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> L202
            goto L204
        L18a:
            r1 = r24
            r2 = r25
            r18 = r3
            r3 = r4
            r21 = r14
            r14 = r4
            r4 = r18
            r22 = r5
            r5 = r28
            r23 = r6
            r6 = r29
            java.lang.Runnable r0 = r1.getOnUserCompletedEventRunnable(r2, r3, r4, r5, r6)     // Catch: java.lang.Exception -> L202
            r15.submit(r0)     // Catch: java.lang.Exception -> L202
            goto L1ff
        L1a7:
            r18 = r3
            r22 = r5
            r23 = r6
            r21 = r14
            r14 = r4
            r14.onUserStopped(r11)     // Catch: java.lang.Exception -> L202
            goto L1ff
        L1b4:
            r18 = r3
            r22 = r5
            r23 = r6
            r21 = r14
            r14 = r4
            r14.onUserStopping(r11)     // Catch: java.lang.Exception -> L202
            goto L1ff
        L1c1:
            r18 = r3
            r22 = r5
            r23 = r6
            r21 = r14
            r14 = r4
            r14.onUserUnlocked(r11)     // Catch: java.lang.Exception -> L202
            goto L1ff
        L1ce:
            r18 = r3
            r22 = r5
            r23 = r6
            r21 = r14
            r14 = r4
            r14.onUserUnlocking(r11)     // Catch: java.lang.Exception -> L202
            goto L1ff
        L1db:
            r18 = r3
            r22 = r5
            r23 = r6
            r21 = r14
            r14 = r4
            if (r17 == 0) goto L1ee
            java.lang.Runnable r0 = r7.getOnUserStartingRunnable(r8, r14, r11)     // Catch: java.lang.Exception -> L202
            r15.submit(r0)     // Catch: java.lang.Exception -> L202
            goto L1ff
        L1ee:
            r14.onUserStarting(r11)     // Catch: java.lang.Exception -> L202
            goto L1ff
        L1f2:
            r18 = r3
            r22 = r5
            r23 = r6
            r21 = r14
            r14 = r4
            r14.onUserSwitching(r10, r11)     // Catch: java.lang.Exception -> L202
        L1ff:
            r1 = r18
            goto L22a
        L202:
            r0 = move-exception
            goto L225
        L204:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> L202
            r1.<init>()     // Catch: java.lang.Exception -> L202
            java.lang.StringBuilder r1 = r1.append(r9)     // Catch: java.lang.Exception -> L202
            java.lang.String r2 = " what?"
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Exception -> L202
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Exception -> L202
            r0.<init>(r1)     // Catch: java.lang.Exception -> L202
            throw r0     // Catch: java.lang.Exception -> L202
        L21b:
            r0 = move-exception
            r18 = r3
            r22 = r5
            r23 = r6
            r21 = r14
            r14 = r4
        L225:
            r1 = r18
            r7.logFailure(r9, r11, r1, r0)
        L22a:
            if (r17 != 0) goto L254
            long r2 = android.os.SystemClock.elapsedRealtime()
            long r2 = r2 - r19
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r4 = "on"
            java.lang.StringBuilder r0 = r0.append(r4)
            java.lang.StringBuilder r0 = r0.append(r9)
            java.lang.StringBuilder r0 = r0.append(r13)
            java.lang.StringBuilder r0 = r0.append(r12)
            java.lang.String r0 = r0.toString()
            r7.warnIfTooLong(r2, r14, r0)
            r25.traceEnd()
        L254:
            int r5 = r22 + 1
            r14 = r21
            r6 = r23
            goto L8a
        L25c:
            r22 = r5
            r23 = r6
            r21 = r14
            if (r21 == 0) goto L2ac
            r2 = 0
            r15.shutdown()
            r3 = 30
            java.util.concurrent.TimeUnit r0 = java.util.concurrent.TimeUnit.SECONDS     // Catch: java.lang.InterruptedException -> L272
            boolean r0 = r15.awaitTermination(r3, r0)     // Catch: java.lang.InterruptedException -> L272
            r2 = r0
            goto L2a3
        L272:
            r0 = move-exception
            java.lang.String r3 = com.android.server.SystemServiceManager.TAG
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "User lifecycle thread pool was interrupted while awaiting completion of "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r9)
            java.lang.String r5 = " of user "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r11)
            java.lang.String r4 = r4.toString()
            android.util.Slog.wtf(r3, r4, r0)
            boolean r1 = r9.equals(r1)
            if (r1 != 0) goto L2a3
            java.lang.String r1 = "Couldn't terminate, disabling thread pool. Please capture a bug report."
            android.util.Slog.e(r3, r1)
            r1 = 0
            com.android.server.SystemServiceManager.sUseLifecycleThreadPool = r1
        L2a3:
            if (r2 != 0) goto L2ac
            java.lang.String r0 = com.android.server.SystemServiceManager.TAG
            java.lang.String r1 = "User lifecycle thread pool was not terminated."
            android.util.Slog.wtf(r0, r1)
        L2ac:
            r25.traceEnd()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.SystemServiceManager.onUser(com.android.server.utils.TimingsTraceAndSlog, java.lang.String, com.android.server.SystemService$TargetUser, com.android.server.SystemService$TargetUser, com.android.server.SystemService$UserCompletedEventType):void");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean useThreadPool(int userId, String onWhat) {
        char c;
        switch (onWhat.hashCode()) {
            case 80204866:
                if (onWhat.equals(USER_STARTING)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 537825071:
                if (onWhat.equals(USER_COMPLETED_EVENT)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return sUseLifecycleThreadPool && userId != 0;
            case 1:
                return true;
            default:
                return false;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean useThreadPoolForService(String onWhat, int serviceIndex) {
        char c;
        switch (onWhat.hashCode()) {
            case 80204866:
                if (onWhat.equals(USER_STARTING)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 537825071:
                if (onWhat.equals(USER_COMPLETED_EVENT)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return serviceIndex >= sOtherServicesStartIndex;
            case 1:
                return true;
            default:
                return false;
        }
    }

    private Runnable getOnUserStartingRunnable(final TimingsTraceAndSlog oldTrace, final SystemService service, final SystemService.TargetUser curUser) {
        return new Runnable() { // from class: com.android.server.SystemServiceManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SystemServiceManager.this.m430x9075992f(oldTrace, service, curUser);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getOnUserStartingRunnable$0$com-android-server-SystemServiceManager  reason: not valid java name */
    public /* synthetic */ void m430x9075992f(TimingsTraceAndSlog oldTrace, SystemService service, SystemService.TargetUser curUser) {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog(oldTrace);
        String serviceName = service.getClass().getName();
        int curUserId = curUser.getUserIdentifier();
        t.traceBegin("ssm.onStartUser-" + curUserId + "_" + serviceName);
        try {
            try {
                long time = SystemClock.elapsedRealtime();
                service.onUserStarting(curUser);
                warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onStartUser-" + curUserId);
            } catch (Exception e) {
                logFailure(USER_STARTING, curUser, serviceName, e);
                Slog.e(TAG, "Disabling thread pool - please capture a bug report.");
                sUseLifecycleThreadPool = false;
            }
        } finally {
            t.traceEnd();
        }
    }

    private Runnable getOnUserCompletedEventRunnable(final TimingsTraceAndSlog oldTrace, final SystemService service, final String serviceName, final SystemService.TargetUser curUser, final SystemService.UserCompletedEventType eventType) {
        return new Runnable() { // from class: com.android.server.SystemServiceManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SystemServiceManager.this.m429x5df1a6ff(oldTrace, curUser, eventType, serviceName, service);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getOnUserCompletedEventRunnable$1$com-android-server-SystemServiceManager  reason: not valid java name */
    public /* synthetic */ void m429x5df1a6ff(TimingsTraceAndSlog oldTrace, SystemService.TargetUser curUser, SystemService.UserCompletedEventType eventType, String serviceName, SystemService service) {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog(oldTrace);
        int curUserId = curUser.getUserIdentifier();
        t.traceBegin("ssm.onCompletedEventUser-" + curUserId + "_" + eventType + "_" + serviceName);
        try {
            try {
                long time = SystemClock.elapsedRealtime();
                service.onUserCompletedEvent(curUser, eventType);
                warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onCompletedEventUser-" + curUserId);
            } catch (Exception e) {
                logFailure(USER_COMPLETED_EVENT, curUser, serviceName, e);
                throw e;
            }
        } finally {
            t.traceEnd();
        }
    }

    private void logFailure(String onWhat, SystemService.TargetUser curUser, String serviceName, Exception ex) {
        Slog.wtf(TAG, "SystemService failure: Failure reporting " + onWhat + " of user " + curUser + " to service " + serviceName, ex);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
    }

    public boolean isSafeMode() {
        return this.mSafeMode;
    }

    public boolean isRuntimeRestarted() {
        return this.mRuntimeRestarted;
    }

    public long getRuntimeStartElapsedTime() {
        return this.mRuntimeStartElapsedTime;
    }

    public long getRuntimeStartUptime() {
        return this.mRuntimeStartUptime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStartInfo(boolean runtimeRestarted, long runtimeStartElapsedTime, long runtimeStartUptime) {
        this.mRuntimeRestarted = runtimeRestarted;
        this.mRuntimeStartElapsedTime = runtimeStartElapsedTime;
        this.mRuntimeStartUptime = runtimeStartUptime;
    }

    private void warnIfTooLong(long duration, SystemService service, String operation) {
        if (duration > 50) {
            Slog.w(TAG, "Service " + service.getClass().getName() + " took " + duration + " ms in " + operation);
        }
    }

    @Deprecated
    public static File ensureSystemDir() {
        if (sSystemDir == null) {
            File dataDir = Environment.getDataDirectory();
            File file = new File(dataDir, HostingRecord.HOSTING_TYPE_SYSTEM);
            sSystemDir = file;
            file.mkdirs();
        }
        File dataDir2 = sSystemDir;
        return dataDir2;
    }

    public String getDumpableName() {
        return SystemServiceManager.class.getSimpleName();
    }

    public void dump(PrintWriter pw, String[] args) {
        pw.printf("Current phase: %d\n", Integer.valueOf(this.mCurrentPhase));
        synchronized (this.mTargetUsers) {
            if (this.mCurrentUser != null) {
                pw.print("Current user: ");
                this.mCurrentUser.dump(pw);
                pw.println();
            } else {
                pw.println("Current user not set!");
            }
            int targetUsersSize = this.mTargetUsers.size();
            if (targetUsersSize > 0) {
                pw.printf("%d target users: ", Integer.valueOf(targetUsersSize));
                for (int i = 0; i < targetUsersSize; i++) {
                    this.mTargetUsers.valueAt(i).dump(pw);
                    if (i != targetUsersSize - 1) {
                        pw.print(", ");
                    }
                }
                pw.println();
            } else {
                pw.println("No target users");
            }
        }
        int startedLen = this.mServices.size();
        if (startedLen > 0) {
            pw.printf("%d started services:\n", Integer.valueOf(startedLen));
            for (int i2 = 0; i2 < startedLen; i2++) {
                SystemService service = this.mServices.get(i2);
                pw.print("  ");
                pw.println(service.getClass().getCanonicalName());
            }
            return;
        }
        pw.println("No started services");
    }
}
