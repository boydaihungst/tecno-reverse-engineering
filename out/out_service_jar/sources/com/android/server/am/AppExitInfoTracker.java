package com.android.server.am;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.app.IAppTraceRetriever;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.SimpleDateFormat;
import android.os.Binder;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Pools;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import android.util.proto.WireTypeMismatchException;
import com.android.internal.app.ProcessMap;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.function.HexConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemServiceManager;
import com.android.server.am.AppExitInfoTracker;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.os.NativeTombstoneManager;
import defpackage.CompanionAppsPermissions;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;
/* loaded from: classes.dex */
public final class AppExitInfoTracker {
    static final String APP_EXIT_INFO_FILE = "procexitinfo";
    private static final long APP_EXIT_INFO_PERSIST_INTERVAL = TimeUnit.MINUTES.toMillis(30);
    private static final long APP_EXIT_INFO_STATSD_LOG_DEBOUNCE = TimeUnit.SECONDS.toMillis(15);
    private static final int APP_EXIT_RAW_INFO_POOL_SIZE = 8;
    static final String APP_EXIT_STORE_DIR = "procexitstore";
    private static final String APP_TRACE_FILE_SUFFIX = ".gz";
    private static final int FOREACH_ACTION_NONE = 0;
    private static final int FOREACH_ACTION_REMOVE_ITEM = 1;
    private static final int FOREACH_ACTION_STOP_ITERATION = 2;
    private static final String TAG = "ActivityManager";
    private int mAppExitInfoHistoryListSize;
    private KillHandler mKillHandler;
    File mProcExitInfoFile;
    File mProcExitStoreDir;
    private ActivityManagerService mService;
    private final Object mLock = new Object();
    private Runnable mAppExitInfoPersistTask = null;
    private long mLastAppExitInfoPersistTimestamp = 0;
    AtomicBoolean mAppExitInfoLoaded = new AtomicBoolean();
    final ArrayList<ApplicationExitInfo> mTmpInfoList = new ArrayList<>();
    final ArrayList<ApplicationExitInfo> mTmpInfoList2 = new ArrayList<>();
    final IsolatedUidRecords mIsolatedUidRecords = new IsolatedUidRecords();
    final AppExitInfoExternalSource mAppExitInfoSourceZygote = new AppExitInfoExternalSource("zygote", null);
    final AppExitInfoExternalSource mAppExitInfoSourceLmkd = new AppExitInfoExternalSource("lmkd", 3);
    final SparseArray<SparseArray<byte[]>> mActiveAppStateSummary = new SparseArray<>();
    final SparseArray<SparseArray<File>> mActiveAppTraces = new SparseArray<>();
    final AppTraceRetriever mAppTraceRetriever = new AppTraceRetriever();
    private final ProcessMap<AppExitInfoContainer> mData = new ProcessMap<>();
    private final Pools.SynchronizedPool<ApplicationExitInfo> mRawRecordsPool = new Pools.SynchronizedPool<>(8);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface LmkdKillListener {
        void onLmkdKillOccurred(int i, int i2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(ActivityManagerService service) {
        this.mService = service;
        ServiceThread thread = new ServiceThread("ActivityManager:killHandler", 10, true);
        thread.start();
        this.mKillHandler = new KillHandler(thread.getLooper());
        File file = new File(SystemServiceManager.ensureSystemDir(), APP_EXIT_STORE_DIR);
        this.mProcExitStoreDir = file;
        if (!FileUtils.createDir(file)) {
            Slog.e(TAG, "Unable to create " + this.mProcExitStoreDir);
            return;
        }
        this.mProcExitInfoFile = new File(this.mProcExitStoreDir, APP_EXIT_INFO_FILE);
        this.mAppExitInfoHistoryListSize = service.mContext.getResources().getInteger(17694734);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        registerForUserRemoval();
        registerForPackageRemoval();
        IoThread.getHandler().post(new Runnable() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                AppExitInfoTracker.this.m1116lambda$onSystemReady$0$comandroidserveramAppExitInfoTracker();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$0$com-android-server-am-AppExitInfoTracker  reason: not valid java name */
    public /* synthetic */ void m1116lambda$onSystemReady$0$comandroidserveramAppExitInfoTracker() {
        SystemProperties.set("persist.sys.lmk.reportkills", Boolean.toString(SystemProperties.getBoolean("sys.lmk.reportkills", false)));
        loadExistingProcessExitInfo();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleNoteProcessDied(ProcessRecord app) {
        if (app == null || app.info == null || !this.mAppExitInfoLoaded.get()) {
            return;
        }
        this.mKillHandler.obtainMessage(4103, obtainRawRecord(app, System.currentTimeMillis())).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleNoteAppKill(ProcessRecord app, int reason, int subReason, String msg) {
        if (!this.mAppExitInfoLoaded.get() || app == null || app.info == null) {
            return;
        }
        ApplicationExitInfo raw = obtainRawRecord(app, System.currentTimeMillis());
        raw.setReason(reason);
        raw.setSubReason(subReason);
        raw.setDescription(msg);
        this.mKillHandler.obtainMessage(4104, raw).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleNoteAppKill(int pid, int uid, int reason, int subReason, String msg) {
        ProcessRecord app;
        if (!this.mAppExitInfoLoaded.get()) {
            return;
        }
        synchronized (this.mService.mPidsSelfLocked) {
            app = this.mService.mPidsSelfLocked.get(pid);
        }
        if (app == null) {
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.w(TAG, "Skipping saving the kill reason for pid " + pid + "(uid=" + uid + ") since its process record is not found");
                return;
            }
            return;
        }
        scheduleNoteAppKill(app, reason, subReason, msg);
    }

    void setLmkdKillListener(final LmkdKillListener listener) {
        synchronized (this.mLock) {
            this.mAppExitInfoSourceLmkd.setOnProcDiedListener(new BiConsumer() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda4
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    AppExitInfoTracker.LmkdKillListener.this.onLmkdKillOccurred(((Integer) obj).intValue(), ((Integer) obj2).intValue());
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleNoteLmkdProcKilled(int pid, int uid) {
        this.mKillHandler.obtainMessage(4101, pid, uid).sendToTarget();
    }

    private void scheduleChildProcDied(int pid, int uid, int status) {
        this.mKillHandler.obtainMessage(4102, pid, uid, Integer.valueOf(status)).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleZygoteSigChld(int pid, int uid, int status) {
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.i(TAG, "Got SIGCHLD from zygote: pid=" + pid + ", uid=" + uid + ", status=" + Integer.toHexString(status));
        }
        scheduleChildProcDied(pid, uid, status);
    }

    void handleNoteProcessDiedLocked(ApplicationExitInfo raw) {
        if (raw != null) {
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.i(TAG, "Update process exit info for " + raw.getPackageName() + "(" + raw.getPid() + "/u" + raw.getRealUid() + ")");
            }
            ApplicationExitInfo info = getExitInfoLocked(raw.getPackageName(), raw.getPackageUid(), raw.getPid());
            Pair<Long, Object> zygote = this.mAppExitInfoSourceZygote.remove(raw.getPid(), raw.getRealUid());
            Pair<Long, Object> lmkd = this.mAppExitInfoSourceLmkd.remove(raw.getPid(), raw.getRealUid());
            this.mIsolatedUidRecords.removeIsolatedUidLocked(raw.getRealUid());
            if (info == null) {
                info = addExitInfoLocked(raw);
            }
            if (lmkd != null) {
                updateExistingExitInfoRecordLocked(info, null, 3);
            } else if (zygote != null) {
                updateExistingExitInfoRecordLocked(info, (Integer) zygote.second, null);
            } else {
                scheduleLogToStatsdLocked(info, false);
            }
        }
    }

    void handleNoteAppKillLocked(ApplicationExitInfo raw) {
        ApplicationExitInfo info = getExitInfoLocked(raw.getPackageName(), raw.getPackageUid(), raw.getPid());
        if (info == null) {
            info = addExitInfoLocked(raw);
        } else {
            info.setReason(raw.getReason());
            info.setSubReason(raw.getSubReason());
            info.setStatus(0);
            info.setTimestamp(System.currentTimeMillis());
            info.setDescription(raw.getDescription());
        }
        scheduleLogToStatsdLocked(info, true);
    }

    private ApplicationExitInfo addExitInfoLocked(ApplicationExitInfo raw) {
        Integer k;
        if (!this.mAppExitInfoLoaded.get()) {
            Slog.w(TAG, "Skipping saving the exit info due to ongoing loading from storage");
            return null;
        }
        ApplicationExitInfo info = new ApplicationExitInfo(raw);
        String[] packages = raw.getPackageList();
        int uid = raw.getRealUid();
        if (UserHandle.isIsolated(uid) && (k = this.mIsolatedUidRecords.getUidByIsolatedUid(uid)) != null) {
            uid = k.intValue();
        }
        for (String str : packages) {
            addExitInfoInnerLocked(str, uid, info);
        }
        schedulePersistProcessExitInfo(false);
        return info;
    }

    private void updateExistingExitInfoRecordLocked(ApplicationExitInfo info, Integer status, Integer reason) {
        if (info == null || !isFresh(info.getTimestamp())) {
            return;
        }
        boolean immediateLog = false;
        if (status != null) {
            if (OsConstants.WIFEXITED(status.intValue())) {
                info.setReason(1);
                info.setStatus(OsConstants.WEXITSTATUS(status.intValue()));
                immediateLog = true;
            } else if (OsConstants.WIFSIGNALED(status.intValue())) {
                if (info.getReason() == 0) {
                    info.setReason(2);
                    info.setStatus(OsConstants.WTERMSIG(status.intValue()));
                } else if (info.getReason() == 5) {
                    info.setStatus(OsConstants.WTERMSIG(status.intValue()));
                    immediateLog = true;
                }
            }
        }
        if (reason != null) {
            info.setReason(reason.intValue());
            if (reason.intValue() == 3) {
                immediateLog = true;
            }
        }
        scheduleLogToStatsdLocked(info, immediateLog);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateExitInfoIfNecessaryLocked(final int pid, int uid, final Integer status, final Integer reason) {
        Integer k = this.mIsolatedUidRecords.getUidByIsolatedUid(uid);
        if (k != null) {
            uid = k.intValue();
        }
        final ArrayList<ApplicationExitInfo> tlist = this.mTmpInfoList;
        tlist.clear();
        final int targetUid = uid;
        forEachPackageLocked(new BiFunction() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda6
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return AppExitInfoTracker.this.m1117x2ee875f8(targetUid, tlist, pid, status, reason, (String) obj, (SparseArray) obj2);
            }
        });
        return tlist.size() > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateExitInfoIfNecessaryLocked$2$com-android-server-am-AppExitInfoTracker  reason: not valid java name */
    public /* synthetic */ Integer m1117x2ee875f8(int targetUid, ArrayList tlist, int pid, Integer status, Integer reason, String packageName, SparseArray records) {
        AppExitInfoContainer container = (AppExitInfoContainer) records.get(targetUid);
        if (container == null) {
            return 0;
        }
        tlist.clear();
        container.getExitInfoLocked(pid, 1, tlist);
        if (tlist.size() == 0) {
            return 0;
        }
        ApplicationExitInfo info = (ApplicationExitInfo) tlist.get(0);
        if (info.getRealUid() != targetUid) {
            tlist.clear();
            return 0;
        }
        updateExistingExitInfoRecordLocked(info, status, reason);
        return 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getExitInfo(String packageName, final int filterUid, final int filterPid, int maxNum, ArrayList<ApplicationExitInfo> results) {
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                boolean emptyPackageName = TextUtils.isEmpty(packageName);
                if (!emptyPackageName) {
                    AppExitInfoContainer container = (AppExitInfoContainer) this.mData.get(packageName, filterUid);
                    if (container != null) {
                        container.getExitInfoLocked(filterPid, maxNum, results);
                    }
                } else {
                    final ArrayList<ApplicationExitInfo> list = this.mTmpInfoList2;
                    list.clear();
                    forEachPackageLocked(new BiFunction() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda2
                        @Override // java.util.function.BiFunction
                        public final Object apply(Object obj, Object obj2) {
                            return AppExitInfoTracker.this.m1115lambda$getExitInfo$3$comandroidserveramAppExitInfoTracker(filterUid, list, filterPid, (String) obj, (SparseArray) obj2);
                        }
                    });
                    Collections.sort(list, new Comparator() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda3
                        @Override // java.util.Comparator
                        public final int compare(Object obj, Object obj2) {
                            int compare;
                            compare = Long.compare(((ApplicationExitInfo) obj2).getTimestamp(), ((ApplicationExitInfo) obj).getTimestamp());
                            return compare;
                        }
                    });
                    int size = list.size();
                    if (maxNum > 0) {
                        size = Math.min(size, maxNum);
                    }
                    for (int i = 0; i < size; i++) {
                        results.add(list.get(i));
                    }
                    list.clear();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getExitInfo$3$com-android-server-am-AppExitInfoTracker  reason: not valid java name */
    public /* synthetic */ Integer m1115lambda$getExitInfo$3$comandroidserveramAppExitInfoTracker(int filterUid, ArrayList list, int filterPid, String name, SparseArray records) {
        AppExitInfoContainer container = (AppExitInfoContainer) records.get(filterUid);
        if (container != null) {
            this.mTmpInfoList.clear();
            list.addAll(container.toListLocked(this.mTmpInfoList, filterPid));
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ApplicationExitInfo getExitInfoLocked(String packageName, int filterUid, int filterPid) {
        ArrayList<ApplicationExitInfo> list = this.mTmpInfoList;
        list.clear();
        getExitInfo(packageName, filterUid, filterPid, 1, list);
        ApplicationExitInfo info = list.size() > 0 ? list.get(0) : null;
        list.clear();
        return info;
    }

    void onUserRemoved(int userId) {
        this.mAppExitInfoSourceZygote.removeByUserId(userId);
        this.mAppExitInfoSourceLmkd.removeByUserId(userId);
        this.mIsolatedUidRecords.removeByUserId(userId);
        synchronized (this.mLock) {
            removeByUserIdLocked(userId);
            schedulePersistProcessExitInfo(true);
        }
    }

    void onPackageRemoved(String packageName, int uid, boolean allUsers) {
        if (packageName != null) {
            boolean removeUid = TextUtils.isEmpty(this.mService.mPackageManagerInt.getNameForUid(uid));
            synchronized (this.mLock) {
                if (removeUid) {
                    try {
                        this.mAppExitInfoSourceZygote.removeByUidLocked(uid, allUsers);
                        this.mAppExitInfoSourceLmkd.removeByUidLocked(uid, allUsers);
                        this.mIsolatedUidRecords.removeAppUid(uid, allUsers);
                    } finally {
                    }
                }
                removePackageLocked(packageName, uid, removeUid, allUsers ? -1 : UserHandle.getUserId(uid));
                schedulePersistProcessExitInfo(true);
            }
        }
    }

    private void registerForUserRemoval() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_REMOVED");
        this.mService.mContext.registerReceiverForAllUsers(new BroadcastReceiver() { // from class: com.android.server.am.AppExitInfoTracker.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userId < 1) {
                    return;
                }
                AppExitInfoTracker.this.onUserRemoved(userId);
            }
        }, filter, null, this.mKillHandler);
    }

    private void registerForPackageRemoval() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        this.mService.mContext.registerReceiverForAllUsers(new BroadcastReceiver() { // from class: com.android.server.am.AppExitInfoTracker.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                if (replacing) {
                    return;
                }
                int uid = intent.getIntExtra("android.intent.extra.UID", -10000);
                boolean allUsers = intent.getBooleanExtra("android.intent.extra.REMOVED_FOR_ALL_USERS", false);
                AppExitInfoTracker.this.onPackageRemoved(intent.getData().getSchemeSpecificPart(), uid, allUsers);
            }
        }, filter, null, this.mKillHandler);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, MOVE_EXCEPTION, INVOKE, MOVE_EXCEPTION] complete} */
    void loadExistingProcessExitInfo() {
        if (!this.mProcExitInfoFile.canRead()) {
            this.mAppExitInfoLoaded.set(true);
            return;
        }
        FileInputStream fin = null;
        try {
            try {
                AtomicFile af = new AtomicFile(this.mProcExitInfoFile);
                fin = af.openRead();
                ProtoInputStream proto = new ProtoInputStream(fin);
                for (int next = proto.nextField(); next != -1; next = proto.nextField()) {
                    switch (next) {
                        case 1:
                            synchronized (this.mLock) {
                                this.mLastAppExitInfoPersistTimestamp = proto.readLong(1112396529665L);
                            }
                            break;
                        case 2:
                            loadPackagesFromProto(proto, next);
                            break;
                    }
                }
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                    }
                }
            } catch (Throwable th) {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e2) {
                    }
                }
                throw th;
            }
        } catch (IOException | IllegalArgumentException | WireTypeMismatchException e3) {
            Slog.w(TAG, "Error in loading historical app exit info from persistent storage: " + e3);
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e4) {
                }
            }
        }
        synchronized (this.mLock) {
            pruneAnrTracesIfNecessaryLocked();
            this.mAppExitInfoLoaded.set(true);
        }
    }

    private void loadPackagesFromProto(ProtoInputStream proto, long fieldId) throws IOException, WireTypeMismatchException {
        long token = proto.start(fieldId);
        String pkgName = "";
        int next = proto.nextField();
        while (next != -1) {
            switch (next) {
                case 1:
                    pkgName = proto.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME);
                    break;
                case 2:
                    AppExitInfoContainer container = new AppExitInfoContainer(this.mAppExitInfoHistoryListSize);
                    int uid = container.readFromProto(proto, 2246267895810L);
                    synchronized (this.mLock) {
                        this.mData.put(pkgName, uid, container);
                    }
                    break;
            }
            next = proto.nextField();
        }
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void persistProcessExitInfo() {
        AtomicFile af = new AtomicFile(this.mProcExitInfoFile);
        FileOutputStream out = null;
        long now = System.currentTimeMillis();
        try {
            out = af.startWrite();
            final ProtoOutputStream proto = new ProtoOutputStream(out);
            proto.write(1112396529665L, now);
            synchronized (this.mLock) {
                forEachPackageLocked(new BiFunction() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda18
                    @Override // java.util.function.BiFunction
                    public final Object apply(Object obj, Object obj2) {
                        return AppExitInfoTracker.lambda$persistProcessExitInfo$5(proto, (String) obj, (SparseArray) obj2);
                    }
                });
                this.mLastAppExitInfoPersistTimestamp = now;
            }
            proto.flush();
            af.finishWrite(out);
        } catch (IOException e) {
            Slog.w(TAG, "Unable to write historical app exit info into persistent storage: " + e);
            af.failWrite(out);
        }
        synchronized (this.mLock) {
            this.mAppExitInfoPersistTask = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Integer lambda$persistProcessExitInfo$5(ProtoOutputStream proto, String packageName, SparseArray records) {
        long token = proto.start(2246267895810L);
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, packageName);
        int uidArraySize = records.size();
        for (int j = 0; j < uidArraySize; j++) {
            ((AppExitInfoContainer) records.valueAt(j)).writeToProto(proto, 2246267895810L);
        }
        proto.end(token);
        return 0;
    }

    void schedulePersistProcessExitInfo(boolean immediately) {
        synchronized (this.mLock) {
            Runnable runnable = this.mAppExitInfoPersistTask;
            if (runnable == null || immediately) {
                if (runnable != null) {
                    IoThread.getHandler().removeCallbacks(this.mAppExitInfoPersistTask);
                }
                this.mAppExitInfoPersistTask = new Runnable() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        AppExitInfoTracker.this.persistProcessExitInfo();
                    }
                };
                IoThread.getHandler().postDelayed(this.mAppExitInfoPersistTask, immediately ? 0L : APP_EXIT_INFO_PERSIST_INTERVAL);
            }
        }
    }

    void clearProcessExitInfo(boolean removeFile) {
        File file;
        synchronized (this.mLock) {
            if (this.mAppExitInfoPersistTask != null) {
                IoThread.getHandler().removeCallbacks(this.mAppExitInfoPersistTask);
                this.mAppExitInfoPersistTask = null;
            }
            if (removeFile && (file = this.mProcExitInfoFile) != null) {
                file.delete();
            }
            this.mData.getMap().clear();
            this.mActiveAppStateSummary.clear();
            this.mActiveAppTraces.clear();
            pruneAnrTracesIfNecessaryLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearHistoryProcessExitInfo(String packageName, int userId) {
        NativeTombstoneManager tombstoneService = (NativeTombstoneManager) LocalServices.getService(NativeTombstoneManager.class);
        Optional<Integer> appId = Optional.empty();
        if (TextUtils.isEmpty(packageName)) {
            synchronized (this.mLock) {
                removeByUserIdLocked(userId);
            }
        } else {
            int uid = this.mService.mPackageManagerInt.getPackageUid(packageName, 131072L, userId);
            appId = Optional.of(Integer.valueOf(UserHandle.getAppId(uid)));
            synchronized (this.mLock) {
                removePackageLocked(packageName, uid, true, userId);
            }
        }
        tombstoneService.purge(Optional.of(Integer.valueOf(userId)), appId);
        schedulePersistProcessExitInfo(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpHistoryProcessExitInfo(final PrintWriter pw, String packageName) {
        pw.println("ACTIVITY MANAGER PROCESS EXIT INFO (dumpsys activity exit-info)");
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        synchronized (this.mLock) {
            pw.println("Last Timestamp of Persistence Into Persistent Storage: " + sdf.format(new Date(this.mLastAppExitInfoPersistTimestamp)));
            if (TextUtils.isEmpty(packageName)) {
                forEachPackageLocked(new BiFunction() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda11
                    @Override // java.util.function.BiFunction
                    public final Object apply(Object obj, Object obj2) {
                        return AppExitInfoTracker.this.m1114x9ab7fd86(pw, sdf, (String) obj, (SparseArray) obj2);
                    }
                });
            } else {
                SparseArray<AppExitInfoContainer> array = (SparseArray) this.mData.getMap().get(packageName);
                if (array != null) {
                    dumpHistoryProcessExitInfoLocked(pw, "  ", packageName, array, sdf);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dumpHistoryProcessExitInfo$6$com-android-server-am-AppExitInfoTracker  reason: not valid java name */
    public /* synthetic */ Integer m1114x9ab7fd86(PrintWriter pw, SimpleDateFormat sdf, String name, SparseArray records) {
        dumpHistoryProcessExitInfoLocked(pw, "  ", name, records, sdf);
        return 0;
    }

    private void dumpHistoryProcessExitInfoLocked(PrintWriter pw, String prefix, String packageName, SparseArray<AppExitInfoContainer> array, SimpleDateFormat sdf) {
        pw.println(prefix + "package: " + packageName);
        int size = array.size();
        for (int i = 0; i < size; i++) {
            pw.println(prefix + "  Historical Process Exit for uid=" + array.keyAt(i));
            array.valueAt(i).dumpLocked(pw, prefix + "    ", sdf);
        }
    }

    private void addExitInfoInnerLocked(String packageName, int uid, ApplicationExitInfo info) {
        AppExitInfoContainer container = (AppExitInfoContainer) this.mData.get(packageName, uid);
        if (container == null) {
            container = new AppExitInfoContainer(this.mAppExitInfoHistoryListSize);
            if (UserHandle.isIsolated(info.getRealUid())) {
                Integer k = this.mIsolatedUidRecords.getUidByIsolatedUid(info.getRealUid());
                if (k != null) {
                    container.mUid = k.intValue();
                }
            } else {
                container.mUid = info.getRealUid();
            }
            this.mData.put(packageName, uid, container);
        }
        container.addExitInfoLocked(info);
    }

    private void scheduleLogToStatsdLocked(ApplicationExitInfo info, boolean immediate) {
        if (info.isLoggedInStatsd()) {
            return;
        }
        if (immediate) {
            this.mKillHandler.removeMessages(4105, info);
            performLogToStatsdLocked(info);
        } else if (!this.mKillHandler.hasMessages(4105, info)) {
            KillHandler killHandler = this.mKillHandler;
            killHandler.sendMessageDelayed(killHandler.obtainMessage(4105, info), APP_EXIT_INFO_STATSD_LOG_DEBOUNCE);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performLogToStatsdLocked(ApplicationExitInfo info) {
        if (info.isLoggedInStatsd()) {
            return;
        }
        info.setLoggedInStatsd(true);
        String pkgName = info.getPackageName();
        String processName = info.getProcessName();
        if (TextUtils.equals(pkgName, processName)) {
            processName = null;
        } else if (processName != null && pkgName != null && processName.startsWith(pkgName)) {
            processName = processName.substring(pkgName.length());
        }
        FrameworkStatsLog.write((int) FrameworkStatsLog.APP_PROCESS_DIED, info.getPackageUid(), processName, info.getReason(), info.getSubReason(), info.getImportance(), (int) info.getPss(), (int) info.getRss(), info.hasForegroundServices());
    }

    private void forEachPackageLocked(BiFunction<String, SparseArray<AppExitInfoContainer>, Integer> callback) {
        if (callback != null) {
            ArrayMap<String, SparseArray<AppExitInfoContainer>> map = this.mData.getMap();
            int i = map.size() - 1;
            while (i >= 0) {
                switch (callback.apply(map.keyAt(i), map.valueAt(i)).intValue()) {
                    case 1:
                        SparseArray<AppExitInfoContainer> records = map.valueAt(i);
                        for (int j = records.size() - 1; j >= 0; j--) {
                            records.valueAt(j).destroyLocked();
                        }
                        map.removeAt(i);
                        break;
                    case 2:
                        i = 0;
                        break;
                }
                i--;
            }
        }
    }

    private void removePackageLocked(String packageName, int uid, boolean removeUid, int userId) {
        if (removeUid) {
            this.mActiveAppStateSummary.remove(uid);
            int idx = this.mActiveAppTraces.indexOfKey(uid);
            if (idx >= 0) {
                SparseArray<File> array = this.mActiveAppTraces.valueAt(idx);
                for (int i = array.size() - 1; i >= 0; i--) {
                    array.valueAt(i).delete();
                }
                this.mActiveAppTraces.removeAt(idx);
            }
        }
        ArrayMap<String, SparseArray<AppExitInfoContainer>> map = this.mData.getMap();
        SparseArray<AppExitInfoContainer> array2 = map.get(packageName);
        if (array2 == null) {
            return;
        }
        if (userId == -1) {
            for (int i2 = array2.size() - 1; i2 >= 0; i2--) {
                array2.valueAt(i2).destroyLocked();
            }
            this.mData.getMap().remove(packageName);
            return;
        }
        int i3 = array2.size() - 1;
        while (true) {
            if (i3 >= 0) {
                if (UserHandle.getUserId(array2.keyAt(i3)) == userId) {
                    array2.valueAt(i3).destroyLocked();
                    array2.removeAt(i3);
                    break;
                } else {
                    i3--;
                }
            } else {
                break;
            }
        }
        int i4 = array2.size();
        if (i4 == 0) {
            map.remove(packageName);
        }
    }

    private void removeByUserIdLocked(final int userId) {
        if (userId == -1) {
            this.mData.getMap().clear();
            this.mActiveAppStateSummary.clear();
            this.mActiveAppTraces.clear();
            pruneAnrTracesIfNecessaryLocked();
            return;
        }
        removeFromSparse2dArray(this.mActiveAppStateSummary, new Predicate() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda12
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AppExitInfoTracker.lambda$removeByUserIdLocked$7(userId, (Integer) obj);
            }
        }, null, null);
        removeFromSparse2dArray(this.mActiveAppTraces, new Predicate() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AppExitInfoTracker.lambda$removeByUserIdLocked$8(userId, (Integer) obj);
            }
        }, null, new Consumer() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda14
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((File) obj).delete();
            }
        });
        forEachPackageLocked(new BiFunction() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda15
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return AppExitInfoTracker.lambda$removeByUserIdLocked$10(userId, (String) obj, (SparseArray) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeByUserIdLocked$7(int userId, Integer v) {
        return UserHandle.getUserId(v.intValue()) == userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeByUserIdLocked$8(int userId, Integer v) {
        return UserHandle.getUserId(v.intValue()) == userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Integer lambda$removeByUserIdLocked$10(int userId, String packageName, SparseArray records) {
        int i = records.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            } else if (UserHandle.getUserId(records.keyAt(i)) != userId) {
                i--;
            } else {
                ((AppExitInfoContainer) records.valueAt(i)).destroyLocked();
                records.removeAt(i);
                break;
            }
        }
        int i2 = records.size();
        return Integer.valueOf(i2 != 0 ? 0 : 1);
    }

    ApplicationExitInfo obtainRawRecord(ProcessRecord app, long timestamp) {
        ApplicationExitInfo info = (ApplicationExitInfo) this.mRawRecordsPool.acquire();
        if (info == null) {
            info = new ApplicationExitInfo();
        }
        synchronized (this.mService.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                int definingUid = app.getHostingRecord() != null ? app.getHostingRecord().getDefiningUid() : 0;
                info.setPid(app.getPid());
                info.setRealUid(app.uid);
                info.setPackageUid(app.info.uid);
                info.setDefiningUid(definingUid > 0 ? definingUid : app.info.uid);
                info.setProcessName(app.processName);
                info.setConnectionGroup(app.mServices.getConnectionGroup());
                info.setPackageName(app.info.packageName);
                info.setPackageList(app.getPackageList());
                info.setReason(0);
                info.setSubReason(0);
                info.setStatus(0);
                info.setImportance(ActivityManager.RunningAppProcessInfo.procStateToImportance(app.mState.getReportedProcState()));
                info.setPss(app.mProfile.getLastPss());
                info.setRss(app.mProfile.getLastRss());
                info.setTimestamp(timestamp);
                info.setHasForegroundServices(app.mServices.hasReportedForegroundServices());
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        return info;
    }

    void recycleRawRecord(ApplicationExitInfo info) {
        info.setProcessName(null);
        info.setDescription(null);
        info.setPackageList(null);
        this.mRawRecordsPool.release(info);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setProcessStateSummary(int uid, int pid, byte[] data) {
        synchronized (this.mLock) {
            Integer k = this.mIsolatedUidRecords.getUidByIsolatedUid(uid);
            if (k != null) {
                uid = k.intValue();
            }
            putToSparse2dArray(this.mActiveAppStateSummary, uid, pid, data, new AppExitInfoTracker$$ExternalSyntheticLambda0(), null);
        }
    }

    byte[] getProcessStateSummary(int uid, int pid) {
        synchronized (this.mLock) {
            Integer k = this.mIsolatedUidRecords.getUidByIsolatedUid(uid);
            if (k != null) {
                uid = k.intValue();
            }
            int index = this.mActiveAppStateSummary.indexOfKey(uid);
            if (index < 0) {
                return null;
            }
            return this.mActiveAppStateSummary.valueAt(index).get(pid);
        }
    }

    public void scheduleLogAnrTrace(int pid, int uid, String[] packageList, File traceFile, long startOff, long endOff) {
        this.mKillHandler.sendMessage(PooledLambda.obtainMessage(new HexConsumer() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda16
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
                AppExitInfoTracker.this.handleLogAnrTrace(((Integer) obj).intValue(), ((Integer) obj2).intValue(), (String[]) obj3, (File) obj4, ((Long) obj5).longValue(), ((Long) obj6).longValue());
            }
        }, Integer.valueOf(pid), Integer.valueOf(uid), packageList, traceFile, Long.valueOf(startOff), Long.valueOf(endOff)));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:55:0x00ed -> B:53:0x00eb). Please submit an issue!!! */
    public void handleLogAnrTrace(int pid, int uid, String[] packageList, File traceFile, long startOff, long endOff) {
        int uid2;
        int uid3;
        int uid4;
        if (traceFile.exists() && !ArrayUtils.isEmpty(packageList)) {
            long size = traceFile.length();
            long length = endOff - startOff;
            if (startOff >= size || endOff > size) {
                return;
            }
            if (length <= 0) {
                return;
            }
            File outFile = new File(this.mProcExitStoreDir, traceFile.getName() + ".gz");
            if (copyToGzFile(traceFile, outFile, startOff, length)) {
                synchronized (this.mLock) {
                    try {
                        uid2 = uid;
                        try {
                            Integer k = this.mIsolatedUidRecords.getUidByIsolatedUid(uid2);
                            if (k == null) {
                                uid3 = uid2;
                            } else {
                                uid3 = k.intValue();
                            }
                            try {
                                if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                                    try {
                                        Slog.i(TAG, "Stored ANR traces of " + pid + "/u" + uid3 + " in " + outFile);
                                    } catch (Throwable th) {
                                        th = th;
                                        throw th;
                                    }
                                }
                                boolean pending = true;
                                for (String str : packageList) {
                                    AppExitInfoContainer container = (AppExitInfoContainer) this.mData.get(str, uid3);
                                    if (container != null && container.appendTraceIfNecessaryLocked(pid, outFile)) {
                                        pending = false;
                                    }
                                }
                                if (!pending) {
                                    uid4 = uid3;
                                } else {
                                    uid4 = uid3;
                                    try {
                                        putToSparse2dArray(this.mActiveAppTraces, uid3, pid, outFile, new AppExitInfoTracker$$ExternalSyntheticLambda0(), new Consumer() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda1
                                            @Override // java.util.function.Consumer
                                            public final void accept(Object obj) {
                                                ((File) obj).delete();
                                            }
                                        });
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                }
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        uid2 = uid;
                    }
                }
            }
        }
    }

    private static boolean copyToGzFile(File inFile, File outFile, long start, long length) {
        long remaining = length;
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));
            GZIPOutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
            try {
                byte[] buffer = new byte[8192];
                in.skip(start);
                while (remaining > 0) {
                    int t = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (t < 0) {
                        break;
                    }
                    out.write(buffer, 0, t);
                    remaining -= t;
                }
                out.close();
                in.close();
                return remaining == 0 && outFile.exists();
            } catch (Throwable th) {
                try {
                    out.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException e) {
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.e(TAG, "Error in copying ANR trace from " + inFile + " to " + outFile, e);
            }
            return false;
        }
    }

    private void pruneAnrTracesIfNecessaryLocked() {
        final ArraySet<String> allFiles = new ArraySet<>();
        File[] files = this.mProcExitStoreDir.listFiles(new FileFilter() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda7
            @Override // java.io.FileFilter
            public final boolean accept(File file) {
                return AppExitInfoTracker.lambda$pruneAnrTracesIfNecessaryLocked$12(allFiles, file);
            }
        });
        if (ArrayUtils.isEmpty(files)) {
            return;
        }
        forEachPackageLocked(new BiFunction() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda8
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return AppExitInfoTracker.lambda$pruneAnrTracesIfNecessaryLocked$14(allFiles, (String) obj, (SparseArray) obj2);
            }
        });
        forEachSparse2dArray(this.mActiveAppTraces, new Consumer() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                allFiles.remove(((File) obj).getName());
            }
        });
        for (int i = allFiles.size() - 1; i >= 0; i--) {
            new File(this.mProcExitStoreDir, allFiles.valueAt(i)).delete();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$pruneAnrTracesIfNecessaryLocked$12(ArraySet allFiles, File f) {
        String name = f.getName();
        boolean trace = name.startsWith("anr_") && name.endsWith(".gz");
        if (trace) {
            allFiles.add(name);
        }
        return trace;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Integer lambda$pruneAnrTracesIfNecessaryLocked$14(final ArraySet allFiles, String name, SparseArray records) {
        for (int i = records.size() - 1; i >= 0; i--) {
            AppExitInfoContainer container = (AppExitInfoContainer) records.valueAt(i);
            container.forEachRecordLocked(new BiFunction() { // from class: com.android.server.am.AppExitInfoTracker$$ExternalSyntheticLambda10
                @Override // java.util.function.BiFunction
                public final Object apply(Object obj, Object obj2) {
                    return AppExitInfoTracker.lambda$pruneAnrTracesIfNecessaryLocked$13(allFiles, (Integer) obj, (ApplicationExitInfo) obj2);
                }
            });
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Integer lambda$pruneAnrTracesIfNecessaryLocked$13(ArraySet allFiles, Integer pid, ApplicationExitInfo info) {
        File traceFile = info.getTraceFile();
        if (traceFile != null) {
            allFiles.remove(traceFile.getName());
        }
        return 0;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: java.util.function.Consumer<U> */
    /* JADX WARN: Multi-variable type inference failed */
    private static <T extends SparseArray<U>, U> void putToSparse2dArray(SparseArray<T> array, int outerKey, int innerKey, U value, Supplier<T> newInstance, Consumer<U> actionToOldValue) {
        T innerArray;
        int idx = array.indexOfKey(outerKey);
        if (idx < 0) {
            T innerArray2 = newInstance.get();
            innerArray = innerArray2;
            array.put(outerKey, innerArray);
        } else {
            T innerArray3 = array.valueAt(idx);
            innerArray = innerArray3;
        }
        int idx2 = innerArray.indexOfKey(innerKey);
        if (idx2 >= 0) {
            if (actionToOldValue != 0) {
                actionToOldValue.accept(innerArray.valueAt(idx2));
            }
            innerArray.setValueAt(idx2, value);
            return;
        }
        innerArray.put(innerKey, value);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: java.util.function.Consumer<U> */
    /* JADX WARN: Multi-variable type inference failed */
    private static <T extends SparseArray<U>, U> void forEachSparse2dArray(SparseArray<T> array, Consumer<U> action) {
        if (action != 0) {
            for (int i = array.size() - 1; i >= 0; i--) {
                T innerArray = array.valueAt(i);
                if (innerArray != null) {
                    for (int j = innerArray.size() - 1; j >= 0; j--) {
                        action.accept(innerArray.valueAt(j));
                    }
                }
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r7v0, resolved type: java.util.function.Consumer<U> */
    /* JADX WARN: Multi-variable type inference failed */
    private static <T extends SparseArray<U>, U> void removeFromSparse2dArray(SparseArray<T> array, Predicate<Integer> outerPredicate, Predicate<Integer> innerPredicate, Consumer<U> action) {
        T innerArray;
        for (int i = array.size() - 1; i >= 0; i--) {
            if ((outerPredicate == null || outerPredicate.test(Integer.valueOf(array.keyAt(i)))) && (innerArray = array.valueAt(i)) != null) {
                for (int j = innerArray.size() - 1; j >= 0; j--) {
                    if (innerPredicate == null || innerPredicate.test(Integer.valueOf(innerArray.keyAt(j)))) {
                        if (action != 0) {
                            action.accept(innerArray.valueAt(j));
                        }
                        innerArray.removeAt(j);
                    }
                }
                int j2 = innerArray.size();
                if (j2 == 0) {
                    array.removeAt(i);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T extends SparseArray<U>, U> U findAndRemoveFromSparse2dArray(SparseArray<T> array, int outerKey, int innerKey) {
        T p;
        int innerIdx;
        int idx = array.indexOfKey(outerKey);
        if (idx < 0 || (p = array.valueAt(idx)) == null || (innerIdx = p.indexOfKey(innerKey)) < 0) {
            return null;
        }
        U ret = (U) p.valueAt(innerIdx);
        p.removeAt(innerIdx);
        if (p.size() == 0) {
            array.removeAt(idx);
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class AppExitInfoContainer {
        private SparseArray<ApplicationExitInfo> mInfos = new SparseArray<>();
        private int mMaxCapacity;
        private int mUid;

        AppExitInfoContainer(int maxCapacity) {
            this.mMaxCapacity = maxCapacity;
        }

        void getExitInfoLocked(int filterPid, int maxNum, ArrayList<ApplicationExitInfo> results) {
            if (filterPid > 0) {
                ApplicationExitInfo r = this.mInfos.get(filterPid);
                if (r != null) {
                    results.add(r);
                    return;
                }
                return;
            }
            int numRep = this.mInfos.size();
            if (maxNum <= 0 || numRep <= maxNum) {
                for (int i = 0; i < numRep; i++) {
                    results.add(this.mInfos.valueAt(i));
                }
                Collections.sort(results, new Comparator() { // from class: com.android.server.am.AppExitInfoTracker$AppExitInfoContainer$$ExternalSyntheticLambda0
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        int compare;
                        compare = Long.compare(((ApplicationExitInfo) obj2).getTimestamp(), ((ApplicationExitInfo) obj).getTimestamp());
                        return compare;
                    }
                });
            } else if (maxNum == 1) {
                ApplicationExitInfo r2 = this.mInfos.valueAt(0);
                for (int i2 = 1; i2 < numRep; i2++) {
                    ApplicationExitInfo t = this.mInfos.valueAt(i2);
                    if (r2.getTimestamp() < t.getTimestamp()) {
                        r2 = t;
                    }
                }
                results.add(r2);
            } else {
                ArrayList<ApplicationExitInfo> list = AppExitInfoTracker.this.mTmpInfoList2;
                list.clear();
                for (int i3 = 0; i3 < numRep; i3++) {
                    list.add(this.mInfos.valueAt(i3));
                }
                Collections.sort(list, new Comparator() { // from class: com.android.server.am.AppExitInfoTracker$AppExitInfoContainer$$ExternalSyntheticLambda1
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        int compare;
                        compare = Long.compare(((ApplicationExitInfo) obj2).getTimestamp(), ((ApplicationExitInfo) obj).getTimestamp());
                        return compare;
                    }
                });
                for (int i4 = 0; i4 < maxNum; i4++) {
                    results.add(list.get(i4));
                }
                list.clear();
            }
        }

        void addExitInfoLocked(ApplicationExitInfo info) {
            int size = this.mInfos.size();
            if (size >= this.mMaxCapacity) {
                int oldestIndex = -1;
                long oldestTimeStamp = JobStatus.NO_LATEST_RUNTIME;
                for (int i = 0; i < size; i++) {
                    ApplicationExitInfo r = this.mInfos.valueAt(i);
                    if (r.getTimestamp() < oldestTimeStamp) {
                        oldestTimeStamp = r.getTimestamp();
                        oldestIndex = i;
                    }
                }
                if (oldestIndex >= 0) {
                    File traceFile = this.mInfos.valueAt(oldestIndex).getTraceFile();
                    if (traceFile != null) {
                        traceFile.delete();
                    }
                    this.mInfos.removeAt(oldestIndex);
                }
            }
            int uid = info.getPackageUid();
            int pid = info.getPid();
            info.setProcessStateSummary((byte[]) AppExitInfoTracker.findAndRemoveFromSparse2dArray(AppExitInfoTracker.this.mActiveAppStateSummary, uid, pid));
            info.setTraceFile((File) AppExitInfoTracker.findAndRemoveFromSparse2dArray(AppExitInfoTracker.this.mActiveAppTraces, uid, pid));
            info.setAppTraceRetriever(AppExitInfoTracker.this.mAppTraceRetriever);
            this.mInfos.append(pid, info);
        }

        boolean appendTraceIfNecessaryLocked(int pid, File traceFile) {
            ApplicationExitInfo r = this.mInfos.get(pid);
            if (r != null) {
                r.setTraceFile(traceFile);
                r.setAppTraceRetriever(AppExitInfoTracker.this.mAppTraceRetriever);
                return true;
            }
            return false;
        }

        void destroyLocked() {
            for (int i = this.mInfos.size() - 1; i >= 0; i--) {
                ApplicationExitInfo ai = this.mInfos.valueAt(i);
                File traceFile = ai.getTraceFile();
                if (traceFile != null) {
                    traceFile.delete();
                }
                ai.setTraceFile(null);
                ai.setAppTraceRetriever(null);
            }
        }

        void forEachRecordLocked(BiFunction<Integer, ApplicationExitInfo, Integer> callback) {
            if (callback != null) {
                int i = this.mInfos.size() - 1;
                while (i >= 0) {
                    switch (callback.apply(Integer.valueOf(this.mInfos.keyAt(i)), this.mInfos.valueAt(i)).intValue()) {
                        case 1:
                            File traceFile = this.mInfos.valueAt(i).getTraceFile();
                            if (traceFile != null) {
                                traceFile.delete();
                            }
                            this.mInfos.removeAt(i);
                            break;
                        case 2:
                            i = 0;
                            break;
                    }
                    i--;
                }
            }
        }

        void dumpLocked(PrintWriter pw, String prefix, SimpleDateFormat sdf) {
            ArrayList<ApplicationExitInfo> list = new ArrayList<>();
            for (int i = this.mInfos.size() - 1; i >= 0; i--) {
                list.add(this.mInfos.valueAt(i));
            }
            Collections.sort(list, new Comparator() { // from class: com.android.server.am.AppExitInfoTracker$AppExitInfoContainer$$ExternalSyntheticLambda2
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    int compare;
                    compare = Long.compare(((ApplicationExitInfo) obj2).getTimestamp(), ((ApplicationExitInfo) obj).getTimestamp());
                    return compare;
                }
            });
            int size = list.size();
            for (int i2 = 0; i2 < size; i2++) {
                list.get(i2).dump(pw, prefix + "  ", "#" + i2, sdf);
            }
        }

        void writeToProto(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, this.mUid);
            int size = this.mInfos.size();
            for (int i = 0; i < size; i++) {
                this.mInfos.valueAt(i).writeToProto(proto, 2246267895810L);
            }
            proto.end(token);
        }

        int readFromProto(ProtoInputStream proto, long fieldId) throws IOException, WireTypeMismatchException {
            long token = proto.start(fieldId);
            int next = proto.nextField();
            while (next != -1) {
                switch (next) {
                    case 1:
                        this.mUid = proto.readInt((long) CompanionMessage.MESSAGE_ID);
                        break;
                    case 2:
                        ApplicationExitInfo info = new ApplicationExitInfo();
                        info.readFromProto(proto, 2246267895810L);
                        this.mInfos.put(info.getPid(), info);
                        break;
                }
                next = proto.nextField();
            }
            proto.end(token);
            return this.mUid;
        }

        List<ApplicationExitInfo> toListLocked(List<ApplicationExitInfo> list, int filterPid) {
            if (list == null) {
                list = new ArrayList();
            }
            for (int i = this.mInfos.size() - 1; i >= 0; i--) {
                if (filterPid == 0 || filterPid == this.mInfos.keyAt(i)) {
                    list.add(this.mInfos.valueAt(i));
                }
            }
            return list;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class IsolatedUidRecords {
        private final SparseArray<ArraySet<Integer>> mUidToIsolatedUidMap = new SparseArray<>();
        private final SparseArray<Integer> mIsolatedUidToUidMap = new SparseArray<>();

        IsolatedUidRecords() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void addIsolatedUid(int isolatedUid, int uid) {
            synchronized (AppExitInfoTracker.this.mLock) {
                ArraySet<Integer> set = this.mUidToIsolatedUidMap.get(uid);
                if (set == null) {
                    set = new ArraySet<>();
                    this.mUidToIsolatedUidMap.put(uid, set);
                }
                set.add(Integer.valueOf(isolatedUid));
                this.mIsolatedUidToUidMap.put(isolatedUid, Integer.valueOf(uid));
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void removeIsolatedUid(int isolatedUid, int uid) {
            synchronized (AppExitInfoTracker.this.mLock) {
                int index = this.mUidToIsolatedUidMap.indexOfKey(uid);
                if (index >= 0) {
                    ArraySet<Integer> set = this.mUidToIsolatedUidMap.valueAt(index);
                    set.remove(Integer.valueOf(isolatedUid));
                    if (set.isEmpty()) {
                        this.mUidToIsolatedUidMap.removeAt(index);
                    }
                }
                this.mIsolatedUidToUidMap.remove(isolatedUid);
            }
        }

        Integer getUidByIsolatedUid(int isolatedUid) {
            Integer num;
            if (UserHandle.isIsolated(isolatedUid)) {
                synchronized (AppExitInfoTracker.this.mLock) {
                    num = this.mIsolatedUidToUidMap.get(isolatedUid);
                }
                return num;
            }
            return Integer.valueOf(isolatedUid);
        }

        private void removeAppUidLocked(int uid) {
            ArraySet<Integer> set = this.mUidToIsolatedUidMap.get(uid);
            if (set != null) {
                for (int i = set.size() - 1; i >= 0; i--) {
                    int isolatedUid = set.removeAt(i).intValue();
                    this.mIsolatedUidToUidMap.remove(isolatedUid);
                }
            }
        }

        void removeAppUid(int uid, boolean allUsers) {
            synchronized (AppExitInfoTracker.this.mLock) {
                if (allUsers) {
                    int uid2 = UserHandle.getAppId(uid);
                    for (int i = this.mUidToIsolatedUidMap.size() - 1; i >= 0; i--) {
                        int u = this.mUidToIsolatedUidMap.keyAt(i);
                        if (uid2 == UserHandle.getAppId(u)) {
                            removeAppUidLocked(u);
                        }
                        this.mUidToIsolatedUidMap.removeAt(i);
                    }
                } else {
                    removeAppUidLocked(uid);
                    this.mUidToIsolatedUidMap.remove(uid);
                }
            }
        }

        int removeIsolatedUidLocked(int isolatedUid) {
            if (!UserHandle.isIsolated(isolatedUid)) {
                return isolatedUid;
            }
            int uid = this.mIsolatedUidToUidMap.get(isolatedUid, -1).intValue();
            if (uid == -1) {
                return isolatedUid;
            }
            this.mIsolatedUidToUidMap.remove(isolatedUid);
            ArraySet<Integer> set = this.mUidToIsolatedUidMap.get(uid);
            if (set != null) {
                set.remove(Integer.valueOf(isolatedUid));
            }
            return uid;
        }

        void removeByUserId(int userId) {
            if (userId == -2) {
                userId = AppExitInfoTracker.this.mService.mUserController.getCurrentUserId();
            }
            synchronized (AppExitInfoTracker.this.mLock) {
                if (userId == -1) {
                    this.mIsolatedUidToUidMap.clear();
                    this.mUidToIsolatedUidMap.clear();
                    return;
                }
                for (int i = this.mIsolatedUidToUidMap.size() - 1; i >= 0; i--) {
                    this.mIsolatedUidToUidMap.keyAt(i);
                    int uid = this.mIsolatedUidToUidMap.valueAt(i).intValue();
                    if (UserHandle.getUserId(uid) == userId) {
                        this.mIsolatedUidToUidMap.removeAt(i);
                        this.mUidToIsolatedUidMap.remove(uid);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class KillHandler extends Handler {
        static final int MSG_APP_KILL = 4104;
        static final int MSG_CHILD_PROC_DIED = 4102;
        static final int MSG_LMKD_PROC_KILLED = 4101;
        static final int MSG_PROC_DIED = 4103;
        static final int MSG_STATSD_LOG = 4105;

        KillHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LMKD_PROC_KILLED /* 4101 */:
                    AppExitInfoTracker.this.mAppExitInfoSourceLmkd.onProcDied(msg.arg1, msg.arg2, null);
                    return;
                case MSG_CHILD_PROC_DIED /* 4102 */:
                    AppExitInfoTracker.this.mAppExitInfoSourceZygote.onProcDied(msg.arg1, msg.arg2, (Integer) msg.obj);
                    return;
                case MSG_PROC_DIED /* 4103 */:
                    ApplicationExitInfo raw = (ApplicationExitInfo) msg.obj;
                    synchronized (AppExitInfoTracker.this.mLock) {
                        AppExitInfoTracker.this.handleNoteProcessDiedLocked(raw);
                    }
                    AppExitInfoTracker.this.recycleRawRecord(raw);
                    return;
                case MSG_APP_KILL /* 4104 */:
                    ApplicationExitInfo raw2 = (ApplicationExitInfo) msg.obj;
                    synchronized (AppExitInfoTracker.this.mLock) {
                        AppExitInfoTracker.this.handleNoteAppKillLocked(raw2);
                    }
                    AppExitInfoTracker.this.recycleRawRecord(raw2);
                    return;
                case MSG_STATSD_LOG /* 4105 */:
                    synchronized (AppExitInfoTracker.this.mLock) {
                        AppExitInfoTracker.this.performLogToStatsdLocked((ApplicationExitInfo) msg.obj);
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    }

    boolean isFresh(long timestamp) {
        long now = System.currentTimeMillis();
        return BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS + timestamp >= now;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class AppExitInfoExternalSource {
        private static final long APP_EXIT_INFO_FRESHNESS_MS = 300000;
        private final SparseArray<SparseArray<Pair<Long, Object>>> mData = new SparseArray<>();
        private final Integer mPresetReason;
        private BiConsumer<Integer, Integer> mProcDiedListener;
        private final String mTag;

        AppExitInfoExternalSource(String tag, Integer reason) {
            this.mTag = tag;
            this.mPresetReason = reason;
        }

        private void addLocked(int pid, int uid, Object extra) {
            Integer k = AppExitInfoTracker.this.mIsolatedUidRecords.getUidByIsolatedUid(uid);
            if (k != null) {
                uid = k.intValue();
            }
            SparseArray<Pair<Long, Object>> array = this.mData.get(uid);
            if (array == null) {
                array = new SparseArray<>();
                this.mData.put(uid, array);
            }
            array.put(pid, new Pair<>(Long.valueOf(System.currentTimeMillis()), extra));
        }

        Pair<Long, Object> remove(int pid, int uid) {
            Pair<Long, Object> p;
            synchronized (AppExitInfoTracker.this.mLock) {
                Integer k = AppExitInfoTracker.this.mIsolatedUidRecords.getUidByIsolatedUid(uid);
                if (k != null) {
                    uid = k.intValue();
                }
                SparseArray<Pair<Long, Object>> array = this.mData.get(uid);
                if (array == null || (p = array.get(pid)) == null) {
                    return null;
                }
                array.remove(pid);
                return AppExitInfoTracker.this.isFresh(((Long) p.first).longValue()) ? p : null;
            }
        }

        void removeByUserId(int userId) {
            if (userId == -2) {
                userId = AppExitInfoTracker.this.mService.mUserController.getCurrentUserId();
            }
            synchronized (AppExitInfoTracker.this.mLock) {
                if (userId == -1) {
                    this.mData.clear();
                    return;
                }
                for (int i = this.mData.size() - 1; i >= 0; i--) {
                    int uid = this.mData.keyAt(i);
                    if (UserHandle.getUserId(uid) == userId) {
                        this.mData.removeAt(i);
                    }
                }
            }
        }

        void removeByUidLocked(int uid, boolean allUsers) {
            Integer k;
            if (UserHandle.isIsolated(uid) && (k = AppExitInfoTracker.this.mIsolatedUidRecords.getUidByIsolatedUid(uid)) != null) {
                uid = k.intValue();
            }
            if (allUsers) {
                int uid2 = UserHandle.getAppId(uid);
                for (int i = this.mData.size() - 1; i >= 0; i--) {
                    if (UserHandle.getAppId(this.mData.keyAt(i)) == uid2) {
                        this.mData.removeAt(i);
                    }
                }
                return;
            }
            this.mData.remove(uid);
        }

        void setOnProcDiedListener(BiConsumer<Integer, Integer> listener) {
            synchronized (AppExitInfoTracker.this.mLock) {
                this.mProcDiedListener = listener;
            }
        }

        void onProcDied(final int pid, final int uid, Integer status) {
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.i(AppExitInfoTracker.TAG, this.mTag + ": proc died: pid=" + pid + " uid=" + uid + ", status=" + status);
            }
            if (AppExitInfoTracker.this.mService == null) {
                return;
            }
            synchronized (AppExitInfoTracker.this.mLock) {
                if (!AppExitInfoTracker.this.updateExitInfoIfNecessaryLocked(pid, uid, status, this.mPresetReason)) {
                    addLocked(pid, uid, status);
                }
                final BiConsumer<Integer, Integer> listener = this.mProcDiedListener;
                if (listener != null) {
                    AppExitInfoTracker.this.mService.mHandler.post(new Runnable() { // from class: com.android.server.am.AppExitInfoTracker$AppExitInfoExternalSource$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            listener.accept(Integer.valueOf(pid), Integer.valueOf(uid));
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AppTraceRetriever extends IAppTraceRetriever.Stub {
        AppTraceRetriever() {
        }

        public ParcelFileDescriptor getTraceFileDescriptor(String packageName, int uid, int pid) {
            AppExitInfoTracker.this.mService.enforceNotIsolatedCaller("getTraceFileDescriptor");
            if (TextUtils.isEmpty(packageName)) {
                throw new IllegalArgumentException("Invalid package name");
            }
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            UserHandle.getCallingUserId();
            int userId = UserHandle.getUserId(uid);
            AppExitInfoTracker.this.mService.mUserController.handleIncomingUser(callingPid, callingUid, userId, true, 0, "getTraceFileDescriptor", null);
            if (AppExitInfoTracker.this.mService.enforceDumpPermissionForPackage(packageName, userId, callingUid, "getTraceFileDescriptor") != -1) {
                synchronized (AppExitInfoTracker.this.mLock) {
                    ApplicationExitInfo info = AppExitInfoTracker.this.getExitInfoLocked(packageName, uid, pid);
                    if (info == null) {
                        return null;
                    }
                    File traceFile = info.getTraceFile();
                    if (traceFile == null) {
                        return null;
                    }
                    long identity = Binder.clearCallingIdentity();
                    try {
                        return ParcelFileDescriptor.open(traceFile, 268435456);
                    } catch (FileNotFoundException e) {
                        return null;
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
            return null;
        }
    }
}
