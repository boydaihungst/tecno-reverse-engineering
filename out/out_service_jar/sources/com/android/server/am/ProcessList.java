package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.IProcessObserver;
import android.app.IUidObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.res.Resources;
import android.graphics.Point;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.NetworkPolicyManager;
import android.os.AppZygote;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManagerInternal;
import android.provider.Settings;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.EventLog;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.ProcessMap;
import com.android.internal.os.Zygote;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.MemInfoReader;
import com.android.server.AppStateTracker;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.LmkdConnection;
import com.android.server.am.ProcessList;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.compat.PlatformCompat;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.ActivityServiceConnectionsHolder;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowProcessController;
import com.mediatek.internal.os.ZygoteConfigExt;
import com.transsion.griffin.Griffin;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.server.am.ITranProcessList;
import dalvik.system.VMRuntime;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes.dex */
public final class ProcessList {
    static final String ANDROID_APP_DATA_ISOLATION_ENABLED_PROPERTY = "persist.zygote.app_data_isolation";
    static final String ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY = "persist.sys.vold_app_data_isolation_enabled";
    private static final long APP_DATA_DIRECTORY_ISOLATION = 143937733;
    public static final int BACKUP_APP_ADJ = 300;
    static final int CACHED_APP_IMPORTANCE_LEVELS = 5;
    public static final int CACHED_APP_LMK_FIRST_ADJ = 950;
    public static final int CACHED_APP_MAX_ADJ = 999;
    public static final int CACHED_APP_MIN_ADJ = 900;
    public static final int FOREGROUND_APP_ADJ = 0;
    public static final int HEAVY_WEIGHT_APP_ADJ = 400;
    public static final int HOME_APP_ADJ = 600;
    public static final int INVALID_ADJ = -10000;
    private static final long LMKD_RECONNECT_DELAY_MS = 1000;
    static final int LMK_ASYNC_EVENT_KILL = 0;
    static final int LMK_ASYNC_EVENT_STAT = 1;
    static final byte LMK_GETKILLCNT = 4;
    static final byte LMK_KILL_OCCURRED = 8;
    static final byte LMK_PROCKILL = 6;
    static final byte LMK_PROCPRIO = 1;
    static final byte LMK_PROCPURGE = 3;
    static final byte LMK_PROCREMOVE = 2;
    static final byte LMK_STATE_CHANGED = 9;
    static final byte LMK_SUBSCRIBE = 5;
    static final byte LMK_TARGET = 0;
    static final byte LMK_UPDATE_PROPS = 7;
    private static final int MAX_ZYGOTE_UNSOLICITED_MESSAGE_SIZE = 16;
    static final int MIN_CACHED_APPS = 2;
    public static final int NATIVE_ADJ = -1000;
    static final int NETWORK_STATE_BLOCK = 1;
    static final int NETWORK_STATE_NO_CHANGE = 0;
    static final int NETWORK_STATE_UNBLOCK = 2;
    static final int PAGE_SIZE = 4096;
    public static final int PERCEPTIBLE_APP_ADJ = 200;
    public static final int PERCEPTIBLE_LOW_APP_ADJ = 250;
    public static final int PERCEPTIBLE_MEDIUM_APP_ADJ = 225;
    public static final int PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ = 50;
    public static final int PERSISTENT_PROC_ADJ = -800;
    public static final int PERSISTENT_SERVICE_ADJ = -700;
    public static final int PREVIOUS_APP_ADJ = 700;
    public static final int PROC_MEM_CACHED = 4;
    public static final int PROC_MEM_IMPORTANT = 2;
    public static final int PROC_MEM_NUM = 5;
    public static final int PROC_MEM_PERSISTENT = 0;
    public static final int PROC_MEM_SERVICE = 3;
    public static final int PROC_MEM_TOP = 1;
    private static final String PROPERTY_USE_APP_IMAGE_STARTUP_CACHE = "persist.device_config.runtime_native.use_app_image_startup_cache";
    public static final int PSS_ALL_INTERVAL = 1200000;
    private static final int PSS_FIRST_ASLEEP_BACKGROUND_INTERVAL = 30000;
    private static final int PSS_FIRST_ASLEEP_CACHED_INTERVAL = 60000;
    private static final int PSS_FIRST_ASLEEP_PERSISTENT_INTERVAL = 60000;
    private static final int PSS_FIRST_ASLEEP_TOP_INTERVAL = 20000;
    private static final int PSS_FIRST_BACKGROUND_INTERVAL = 20000;
    private static final int PSS_FIRST_CACHED_INTERVAL = 20000;
    private static final int PSS_FIRST_PERSISTENT_INTERVAL = 30000;
    private static final int PSS_FIRST_TOP_INTERVAL = 10000;
    public static final int PSS_MAX_INTERVAL = 3600000;
    public static final int PSS_MIN_TIME_FROM_STATE_CHANGE = 15000;
    public static final int PSS_SAFE_TIME_FROM_STATE_CHANGE = 1000;
    private static final int PSS_SAME_CACHED_INTERVAL = 600000;
    private static final int PSS_SAME_IMPORTANT_INTERVAL = 600000;
    private static final int PSS_SAME_PERSISTENT_INTERVAL = 600000;
    private static final int PSS_SAME_SERVICE_INTERVAL = 300000;
    private static final int PSS_SAME_TOP_INTERVAL = 60000;
    private static final int PSS_TEST_FIRST_BACKGROUND_INTERVAL = 5000;
    private static final int PSS_TEST_FIRST_TOP_INTERVAL = 3000;
    public static final int PSS_TEST_MIN_TIME_FROM_STATE_CHANGE = 10000;
    private static final int PSS_TEST_SAME_BACKGROUND_INTERVAL = 15000;
    private static final int PSS_TEST_SAME_IMPORTANT_INTERVAL = 10000;
    static final int SCHED_GROUP_BACKGROUND = 0;
    static final int SCHED_GROUP_DEFAULT = 2;
    static final int SCHED_GROUP_RESTRICTED = 1;
    public static final int SCHED_GROUP_TOP_APP = 3;
    static final int SCHED_GROUP_TOP_APP_BOUND = 4;
    public static final int SERVICE_ADJ = 500;
    public static final int SERVICE_B_ADJ = 800;
    public static final int SYSTEM_ADJ = -900;
    static final int TRIM_CRITICAL_THRESHOLD = 3;
    static final int TRIM_LOW_THRESHOLD = 5;
    public static final int UNKNOWN_ADJ = 1001;
    private static final String UNSOL_ZYGOTE_MSG_SOCKET_PATH = "/data/system/unsolzygotesocket";
    public static final int VISIBLE_APP_ADJ = 100;
    static final int VISIBLE_APP_LAYER_MAX = 99;
    private ActivityManagerService.ProcessChangeItem[] mActiveProcessChanges;
    ActiveUids mActiveUids;
    private ArrayList<String> mAppDataIsolationAllowlistedApps;
    private boolean mAppDataIsolationEnabled;
    public final AppExitInfoTracker mAppExitInfoTracker;
    IsolatedUidRangeAllocator mAppIsolatedUidRangeAllocator;
    final ArrayMap<AppZygote, ArrayList<ProcessRecord>> mAppZygoteProcesses;
    final ProcessMap<AppZygote> mAppZygotes;
    final ArraySet<ProcessRecord> mAppsInBackgroundRestricted;
    final ArrayList<ActivityManagerService.ProcessChangeItem> mAvailProcessChanges;
    private long mCachedRestoreLevel;
    final ProcessMap<ProcessRecord> mDyingProcesses;
    IsolatedUidRange mGlobalIsolatedUids;
    private boolean mHaveDisplaySize;
    ImperceptibleKillRunner mImperceptibleKillRunner;
    final SparseArray<ProcessRecord> mIsolatedProcesses;
    private boolean mKillAllBackgroundProcessesByShell;
    private int mLruProcessActivityStart;
    private int mLruProcessServiceStart;
    public final ArrayList<ProcessRecord> mLruProcesses;
    private int mLruSeq;
    Map<Integer, ArrayList<String>> mMuteList;
    private final int[] mOomAdj;
    private boolean mOomLevelsSet;
    private final int[] mOomMinFree;
    private final int[] mOomMinFreeHigh;
    private final int[] mOomMinFreeLow;
    private final ArrayList<ActivityManagerService.ProcessChangeItem> mPendingProcessChanges;
    final LongSparseArray<ProcessRecord> mPendingStarts;
    private PlatformCompat mPlatformCompat;
    ActivityManagerGlobalLock mProcLock;
    private long mProcStartSeqCounter;
    volatile long mProcStateSeqCounter;
    private final Object mProcessChangeLock;
    private final MyProcessMap mProcessNames;
    private final RemoteCallbackList<IProcessObserver> mProcessObservers;
    final ArrayList<ProcessRecord> mRemovedProcesses;
    ActivityManagerService mService = null;
    final StringBuilder mStringBuilder;
    private LocalSocket mSystemServerSocketForZygote;
    private final long mTotalMemMb;
    private boolean mVoldAppDataIsolationEnabled;
    private final int[] mZygoteSigChldMessage;
    private final byte[] mZygoteUnsolicitedMessage;
    static final String TAG = "ActivityManager";
    static final String TAG_PROCESS_OBSERVERS = TAG + ActivityManagerDebugConfig.POSTFIX_PROCESS_OBSERVERS;
    static KillHandler sKillHandler = null;
    static ServiceThread sKillThread = null;
    private static LmkdConnection sLmkdConnection = null;
    private static final int[] sProcStateToProcMem = {0, 0, 1, 2, 1, 2, 2, 2, 2, 2, 3, 4, 1, 2, 4, 4, 4, 4, 4, 4};
    private static final long[] sFirstAwakePssTimes = {30000, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, 20000, 20000, 20000};
    private static final long[] sSameAwakePssTimes = {600000, 60000, 600000, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS, 600000};
    private static final long[] sFirstAsleepPssTimes = {60000, 20000, 30000, 30000, 60000};
    private static final long[] sSameAsleepPssTimes = {600000, 60000, 600000, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS, 600000};
    private static final long[] sTestFirstPssTimes = {BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS, BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS, 5000, 5000, 5000};
    private static final long[] sTestSamePssTimes = {15000, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, 15000, 15000};

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class IsolatedUidRange {
        public final int mFirstUid;
        public final int mLastUid;
        private int mNextUid;
        private final SparseBooleanArray mUidUsed = new SparseBooleanArray();

        IsolatedUidRange(int firstUid, int lastUid) {
            this.mFirstUid = firstUid;
            this.mLastUid = lastUid;
            this.mNextUid = firstUid;
        }

        int allocateIsolatedUidLocked(int userId) {
            int stepsLeft = (this.mLastUid - this.mFirstUid) + 1;
            for (int i = 0; i < stepsLeft; i++) {
                int i2 = this.mNextUid;
                int i3 = this.mFirstUid;
                if (i2 < i3 || i2 > this.mLastUid) {
                    this.mNextUid = i3;
                }
                int uid = UserHandle.getUid(userId, this.mNextUid);
                this.mNextUid++;
                if (!this.mUidUsed.get(uid, false)) {
                    this.mUidUsed.put(uid, true);
                    return uid;
                }
            }
            return -1;
        }

        void freeIsolatedUidLocked(int uid) {
            this.mUidUsed.delete(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class IsolatedUidRangeAllocator {
        private final ProcessMap<IsolatedUidRange> mAppRanges = new ProcessMap<>();
        private final BitSet mAvailableUidRanges;
        private final int mFirstUid;
        private final int mNumUidRanges;
        private final int mNumUidsPerRange;

        IsolatedUidRangeAllocator(int firstUid, int lastUid, int numUidsPerRange) {
            this.mFirstUid = firstUid;
            this.mNumUidsPerRange = numUidsPerRange;
            int i = ((lastUid - firstUid) + 1) / numUidsPerRange;
            this.mNumUidRanges = i;
            BitSet bitSet = new BitSet(i);
            this.mAvailableUidRanges = bitSet;
            bitSet.set(0, i);
        }

        IsolatedUidRange getIsolatedUidRangeLocked(String processName, int uid) {
            return (IsolatedUidRange) this.mAppRanges.get(processName, uid);
        }

        IsolatedUidRange getOrCreateIsolatedUidRangeLocked(String processName, int uid) {
            IsolatedUidRange range = getIsolatedUidRangeLocked(processName, uid);
            if (range == null) {
                int uidRangeIndex = this.mAvailableUidRanges.nextSetBit(0);
                if (uidRangeIndex < 0) {
                    return null;
                }
                this.mAvailableUidRanges.clear(uidRangeIndex);
                int i = this.mFirstUid;
                int i2 = this.mNumUidsPerRange;
                int actualUid = i + (uidRangeIndex * i2);
                IsolatedUidRange range2 = new IsolatedUidRange(actualUid, (i2 + actualUid) - 1);
                this.mAppRanges.put(processName, uid, range2);
                return range2;
            }
            return range;
        }

        void freeUidRangeLocked(ApplicationInfo info) {
            IsolatedUidRange range = (IsolatedUidRange) this.mAppRanges.get(info.processName, info.uid);
            if (range != null) {
                int uidRangeIndex = (range.mFirstUid - this.mFirstUid) / this.mNumUidsPerRange;
                this.mAvailableUidRanges.set(uidRangeIndex);
                this.mAppRanges.remove(info.processName, info.uid);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class MyProcessMap extends ProcessMap<ProcessRecord> {
        MyProcessMap() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        public ProcessRecord put(String name, int uid, ProcessRecord value) {
            ProcessRecord r = (ProcessRecord) super.put(name, uid, value);
            ProcessList.this.mService.mAtmInternal.onProcessAdded(r.getWindowProcessController());
            return r;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* renamed from: remove */
        public ProcessRecord m1479remove(String name, int uid) {
            ProcessRecord r = (ProcessRecord) super.remove(name, uid);
            ProcessList.this.mService.mAtmInternal.onProcessRemoved(name, uid);
            return r;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class KillHandler extends Handler {
        static final int KILL_PROCESS_GROUP_MSG = 4000;
        static final int LMKD_RECONNECT_MSG = 4001;

        public KillHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KILL_PROCESS_GROUP_MSG /* 4000 */:
                    Trace.traceBegin(64L, "killProcessGroup");
                    Process.killProcessGroup(msg.arg1, msg.arg2);
                    Trace.traceEnd(64L);
                    return;
                case LMKD_RECONNECT_MSG /* 4001 */:
                    if (!ProcessList.sLmkdConnection.connect()) {
                        Slog.i(ProcessList.TAG, "Failed to connect to lmkd, retry after 1000 ms");
                        ProcessList.sKillHandler.sendMessageDelayed(ProcessList.sKillHandler.obtainMessage(LMKD_RECONNECT_MSG), 1000L);
                        return;
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessList() {
        int[] iArr = {0, 100, 200, 250, 900, CACHED_APP_LMK_FIRST_ADJ};
        this.mOomAdj = iArr;
        this.mOomMinFreeLow = new int[]{12288, 18432, 24576, 36864, 43008, 49152};
        this.mOomMinFreeHigh = new int[]{73728, 92160, 110592, 129024, 147456, 184320};
        this.mOomMinFree = new int[iArr.length];
        this.mOomLevelsSet = false;
        this.mAppDataIsolationEnabled = false;
        this.mVoldAppDataIsolationEnabled = false;
        this.mStringBuilder = new StringBuilder(256);
        this.mProcStateSeqCounter = 0L;
        this.mProcStartSeqCounter = 0L;
        this.mPendingStarts = new LongSparseArray<>();
        this.mLruProcesses = new ArrayList<>();
        this.mLruProcessActivityStart = 0;
        this.mLruProcessServiceStart = 0;
        this.mLruSeq = 0;
        this.mIsolatedProcesses = new SparseArray<>();
        this.mAppZygotes = new ProcessMap<>();
        this.mAppExitInfoTracker = new AppExitInfoTracker();
        this.mAppZygoteProcesses = new ArrayMap<>();
        this.mAppsInBackgroundRestricted = new ArraySet<>();
        this.mPlatformCompat = null;
        this.mZygoteUnsolicitedMessage = new byte[16];
        this.mZygoteSigChldMessage = new int[3];
        this.mKillAllBackgroundProcessesByShell = false;
        this.mGlobalIsolatedUids = new IsolatedUidRange(99000, 99999);
        this.mAppIsolatedUidRangeAllocator = new IsolatedUidRangeAllocator(90000, 98999, 100);
        this.mRemovedProcesses = new ArrayList<>();
        this.mDyingProcesses = new ProcessMap<>();
        this.mProcessObservers = new RemoteCallbackList<>();
        this.mActiveProcessChanges = new ActivityManagerService.ProcessChangeItem[5];
        this.mPendingProcessChanges = new ArrayList<>();
        this.mAvailProcessChanges = new ArrayList<>();
        this.mProcessChangeLock = new Object();
        this.mProcessNames = new MyProcessMap();
        this.mMuteList = new HashMap();
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        this.mTotalMemMb = minfo.getTotalSize() / 1048576;
        updateOomLevels(0, 0, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(ActivityManagerService service, ActiveUids activeUids, PlatformCompat platformCompat) {
        this.mService = service;
        this.mActiveUids = activeUids;
        this.mPlatformCompat = platformCompat;
        this.mProcLock = service.mProcLock;
        this.mAppDataIsolationEnabled = SystemProperties.getBoolean(ANDROID_APP_DATA_ISOLATION_ENABLED_PROPERTY, true);
        this.mVoldAppDataIsolationEnabled = SystemProperties.getBoolean(ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY, false);
        this.mAppDataIsolationAllowlistedApps = new ArrayList<>(SystemConfig.getInstance().getAppDataIsolationWhitelistedApps());
        if (sKillHandler == null) {
            ServiceThread serviceThread = new ServiceThread("ActivityManager:kill", 10, true);
            sKillThread = serviceThread;
            serviceThread.start();
            sKillHandler = new KillHandler(sKillThread.getLooper());
            sLmkdConnection = new LmkdConnection(sKillThread.getLooper().getQueue(), new LmkdConnection.LmkdConnectionListener() { // from class: com.android.server.am.ProcessList.1
                @Override // com.android.server.am.LmkdConnection.LmkdConnectionListener
                public boolean onConnect(OutputStream ostream) {
                    Slog.i(ProcessList.TAG, "Connection with lmkd established");
                    return ProcessList.this.onLmkdConnect(ostream);
                }

                @Override // com.android.server.am.LmkdConnection.LmkdConnectionListener
                public void onDisconnect() {
                    Slog.w(ProcessList.TAG, "Lost connection to lmkd");
                    ProcessList.sKillHandler.sendMessageDelayed(ProcessList.sKillHandler.obtainMessage(4001), 1000L);
                }

                @Override // com.android.server.am.LmkdConnection.LmkdConnectionListener
                public boolean isReplyExpected(ByteBuffer replyBuf, ByteBuffer dataReceived, int receivedLen) {
                    return receivedLen == replyBuf.array().length && dataReceived.getInt(0) == replyBuf.getInt(0);
                }

                @Override // com.android.server.am.LmkdConnection.LmkdConnectionListener
                public boolean handleUnsolicitedMessage(DataInputStream inputData, int receivedLen) {
                    if (receivedLen < 4) {
                        return false;
                    }
                    try {
                        switch (inputData.readInt()) {
                            case 6:
                                if (receivedLen != 12) {
                                    return false;
                                }
                                int pid = inputData.readInt();
                                int uid = inputData.readInt();
                                ProcessList.this.mAppExitInfoTracker.scheduleNoteLmkdProcKilled(pid, uid);
                                return true;
                            case 7:
                            default:
                                return false;
                            case 8:
                                if (receivedLen < 80) {
                                    return false;
                                }
                                LmkdStatsReporter.logKillOccurred(inputData);
                                return true;
                            case 9:
                                if (receivedLen != 8) {
                                    return false;
                                }
                                int state = inputData.readInt();
                                LmkdStatsReporter.logStateChanged(state);
                                return true;
                        }
                    } catch (IOException e) {
                        Slog.e(ProcessList.TAG, "Invalid buffer data. Failed to log LMK_KILL_OCCURRED");
                        return false;
                    }
                }
            });
            LocalSocket createSystemServerSocketForZygote = createSystemServerSocketForZygote();
            this.mSystemServerSocketForZygote = createSystemServerSocketForZygote;
            if (createSystemServerSocketForZygote != null) {
                sKillHandler.getLooper().getQueue().addOnFileDescriptorEventListener(this.mSystemServerSocketForZygote.getFileDescriptor(), 1, new MessageQueue.OnFileDescriptorEventListener() { // from class: com.android.server.am.ProcessList$$ExternalSyntheticLambda4
                    @Override // android.os.MessageQueue.OnFileDescriptorEventListener
                    public final int onFileDescriptorEvents(FileDescriptor fileDescriptor, int i) {
                        int handleZygoteMessages;
                        handleZygoteMessages = ProcessList.this.handleZygoteMessages(fileDescriptor, i);
                        return handleZygoteMessages;
                    }
                });
            }
            this.mAppExitInfoTracker.init(this.mService);
            this.mImperceptibleKillRunner = new ImperceptibleKillRunner(sKillThread.getLooper());
        }
        ITranProcessList.Instance().onConstruct(TAG, this.mService.mContext);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        this.mAppExitInfoTracker.onSystemReady();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyDisplaySize(WindowManagerService wm) {
        if (!this.mHaveDisplaySize) {
            Point p = new Point();
            wm.getBaseDisplaySize(0, p);
            if (p.x != 0 && p.y != 0) {
                updateOomLevels(p.x, p.y, true);
                this.mHaveDisplaySize = true;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<Integer, String> getProcessesWithPendingBindMounts(int userId) {
        Map<Integer, String> pidPackageMap = new HashMap<>();
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                    ProcessRecord record = this.mLruProcesses.get(i);
                    if (record.userId == userId && record.isBindMountPending()) {
                        int pid = record.getPid();
                        if (pid == 0) {
                            throw new IllegalStateException("Pending process is not started yet,retry later");
                        }
                        pidPackageMap.put(Integer.valueOf(pid), record.info.packageName);
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        return pidPackageMap;
    }

    private void updateOomLevels(int displayWidth, int displayHeight, boolean write) {
        float scaleMem = ((float) (this.mTotalMemMb - 350)) / 350.0f;
        float scaleDisp = ((displayWidth * displayHeight) - 384000) / (1024000 - 384000);
        float scale = scaleMem > scaleDisp ? scaleMem : scaleDisp;
        if (scale < 0.0f) {
            scale = 0.0f;
        } else if (scale > 1.0f) {
            scale = 1.0f;
        }
        int minfree_adj = Resources.getSystem().getInteger(17694865);
        int minfree_abs = Resources.getSystem().getInteger(17694864);
        boolean is64bit = Build.SUPPORTED_64_BIT_ABIS.length > 0;
        for (int i = 0; i < this.mOomAdj.length; i++) {
            int low = this.mOomMinFreeLow[i];
            int high = this.mOomMinFreeHigh[i];
            if (is64bit) {
                if (i == 4) {
                    high = (high * 3) / 2;
                } else if (i == 5) {
                    high = (high * 7) / 4;
                }
            }
            this.mOomMinFree[i] = (int) (low + ((high - low) * scale));
        }
        if (minfree_abs >= 0) {
            int i2 = 0;
            while (true) {
                int[] iArr = this.mOomAdj;
                if (i2 >= iArr.length) {
                    break;
                }
                int[] iArr2 = this.mOomMinFree;
                iArr2[i2] = (int) ((minfree_abs * iArr2[i2]) / iArr2[iArr.length - 1]);
                i2++;
            }
        }
        if (minfree_adj != 0) {
            int i3 = 0;
            while (true) {
                int[] iArr3 = this.mOomAdj;
                if (i3 >= iArr3.length) {
                    break;
                }
                int[] iArr4 = this.mOomMinFree;
                int i4 = iArr4[i3];
                int i5 = i4 + ((int) ((minfree_adj * i4) / iArr4[iArr3.length - 1]));
                iArr4[i3] = i5;
                if (i5 < 0) {
                    iArr4[i3] = 0;
                }
                i3++;
            }
        }
        this.mCachedRestoreLevel = (getMemLevel(999) / GadgetFunction.NCM) / 3;
        int reserve = (((displayWidth * displayHeight) * 4) * 3) / 1024;
        int reserve_adj = Resources.getSystem().getInteger(17694830);
        int reserve_abs = Resources.getSystem().getInteger(17694829);
        if (reserve_abs >= 0) {
            reserve = reserve_abs;
        }
        if (reserve_adj != 0 && (reserve = reserve + reserve_adj) < 0) {
            reserve = 0;
        }
        if (write) {
            ByteBuffer buf = ByteBuffer.allocate(((this.mOomAdj.length * 2) + 1) * 4);
            buf.putInt(0);
            for (int i6 = 0; i6 < this.mOomAdj.length; i6++) {
                buf.putInt((this.mOomMinFree[i6] * 1024) / 4096);
                buf.putInt(this.mOomAdj[i6]);
            }
            writeLmkd(buf, null);
            SystemProperties.set("sys.sysctl.extra_free_kbytes", Integer.toString(reserve));
            this.mOomLevelsSet = true;
        }
    }

    public static int computeEmptyProcessLimit(int totalProcessLimit) {
        return totalProcessLimit / 2;
    }

    private static String buildOomTag(String prefix, String compactPrefix, String space, int val, int base, boolean compact) {
        int diff = val - base;
        if (diff == 0) {
            if (compact) {
                return compactPrefix;
            }
            return space == null ? prefix : prefix + space;
        }
        if (diff < 10) {
            return prefix + (compact ? "+" : "+ ") + Integer.toString(diff);
        }
        return prefix + "+" + Integer.toString(diff);
    }

    public static String makeOomAdjString(int setAdj, boolean compact) {
        if (setAdj >= 900) {
            return buildOomTag("cch", "cch", "   ", setAdj, 900, compact);
        }
        if (setAdj >= 800) {
            return buildOomTag("svcb  ", "svcb", null, setAdj, 800, compact);
        }
        if (setAdj >= 700) {
            return buildOomTag("prev  ", "prev", null, setAdj, 700, compact);
        }
        if (setAdj >= 600) {
            return buildOomTag("home  ", "home", null, setAdj, 600, compact);
        }
        if (setAdj >= 500) {
            return buildOomTag("svc   ", "svc", null, setAdj, 500, compact);
        }
        if (setAdj >= 400) {
            return buildOomTag("hvy   ", "hvy", null, setAdj, 400, compact);
        }
        if (setAdj >= 300) {
            return buildOomTag("bkup  ", "bkup", null, setAdj, 300, compact);
        }
        if (setAdj >= 250) {
            return buildOomTag("prcl  ", "prcl", null, setAdj, 250, compact);
        }
        if (setAdj >= 225) {
            return buildOomTag("prcm  ", "prcm", null, setAdj, PERCEPTIBLE_MEDIUM_APP_ADJ, compact);
        }
        if (setAdj >= 200) {
            return buildOomTag("prcp  ", "prcp", null, setAdj, 200, compact);
        }
        if (setAdj >= 100) {
            return buildOomTag("vis", "vis", "   ", setAdj, 100, compact);
        }
        if (setAdj >= 0) {
            return buildOomTag("fg ", "fg ", "   ", setAdj, 0, compact);
        }
        if (setAdj >= -700) {
            return buildOomTag("psvc  ", "psvc", null, setAdj, -700, compact);
        }
        if (setAdj >= -800) {
            return buildOomTag("pers  ", "pers", null, setAdj, -800, compact);
        }
        if (setAdj >= -900) {
            return buildOomTag("sys   ", Griffin.SYS, null, setAdj, -900, compact);
        }
        if (setAdj >= -1000) {
            return buildOomTag("ntv  ", "ntv", null, setAdj, -1000, compact);
        }
        return Integer.toString(setAdj);
    }

    public static String makeProcStateString(int curProcState) {
        return ActivityManager.procStateToString(curProcState);
    }

    public static int makeProcStateProtoEnum(int curProcState) {
        switch (curProcState) {
            case -1:
                return 999;
            case 0:
                return 1000;
            case 1:
                return 1001;
            case 2:
                return 1002;
            case 3:
                return 1020;
            case 4:
                return 1003;
            case 5:
                return 1004;
            case 6:
                return 1005;
            case 7:
                return 1006;
            case 8:
                return 1007;
            case 9:
                return 1008;
            case 10:
                return 1009;
            case 11:
                return 1010;
            case 12:
                return 1011;
            case 13:
                return 1012;
            case 14:
                return 1013;
            case 15:
                return 1014;
            case 16:
                return 1015;
            case 17:
                return 1016;
            case 18:
                return 1017;
            case 19:
                return 1018;
            case 20:
                return 1019;
            default:
                return 998;
        }
    }

    public static void appendRamKb(StringBuilder sb, long ramKb) {
        int j = 0;
        int fact = 10;
        while (j < 6) {
            if (ramKb < fact) {
                sb.append(' ');
            }
            j++;
            fact *= 10;
        }
        sb.append(ramKb);
    }

    /* loaded from: classes.dex */
    public static final class ProcStateMemTracker {
        int mPendingHighestMemState;
        int mPendingMemState;
        float mPendingScalingFactor;
        final int[] mHighestMem = new int[5];
        final float[] mScalingFactor = new float[5];
        int mTotalHighestMem = 4;

        public ProcStateMemTracker() {
            for (int i = 0; i < 5; i++) {
                this.mHighestMem[i] = 5;
                this.mScalingFactor[i] = 1.0f;
            }
            this.mPendingMemState = -1;
        }

        public void dumpLine(PrintWriter pw) {
            pw.print("best=");
            pw.print(this.mTotalHighestMem);
            pw.print(" (");
            boolean needSep = false;
            for (int i = 0; i < 5; i++) {
                if (this.mHighestMem[i] < 5) {
                    if (needSep) {
                        pw.print(", ");
                    }
                    pw.print(i);
                    pw.print("=");
                    pw.print(this.mHighestMem[i]);
                    pw.print(" ");
                    pw.print(this.mScalingFactor[i]);
                    pw.print("x");
                    needSep = true;
                }
            }
            pw.print(")");
            if (this.mPendingMemState >= 0) {
                pw.print(" / pending state=");
                pw.print(this.mPendingMemState);
                pw.print(" highest=");
                pw.print(this.mPendingHighestMemState);
                pw.print(" ");
                pw.print(this.mPendingScalingFactor);
                pw.print("x");
            }
            pw.println();
        }
    }

    public static boolean procStatesDifferForMem(int procState1, int procState2) {
        int[] iArr = sProcStateToProcMem;
        return iArr[procState1] != iArr[procState2];
    }

    public static long minTimeFromStateChange(boolean test) {
        if (test) {
            return JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        }
        return 15000L;
    }

    public static long computeNextPssTime(int procState, ProcStateMemTracker tracker, boolean test, boolean sleeping, long now) {
        boolean first;
        float scalingFactor;
        long[] table;
        int memState = sProcStateToProcMem[procState];
        if (tracker != null) {
            int highestMemState = memState < tracker.mTotalHighestMem ? memState : tracker.mTotalHighestMem;
            first = highestMemState < tracker.mHighestMem[memState];
            tracker.mPendingMemState = memState;
            tracker.mPendingHighestMemState = highestMemState;
            if (first) {
                scalingFactor = 1.0f;
                tracker.mPendingScalingFactor = 1.0f;
            } else {
                scalingFactor = tracker.mScalingFactor[memState];
                tracker.mPendingScalingFactor = 1.5f * scalingFactor;
            }
        } else {
            first = true;
            scalingFactor = 1.0f;
        }
        if (test) {
            if (first) {
                table = sTestFirstPssTimes;
            } else {
                table = sTestSamePssTimes;
            }
        } else if (first) {
            table = sleeping ? sFirstAsleepPssTimes : sFirstAwakePssTimes;
        } else {
            table = sleeping ? sSameAsleepPssTimes : sSameAwakePssTimes;
        }
        long delay = ((float) table[memState]) * scalingFactor;
        if (delay > 3600000) {
            delay = 3600000;
        }
        return now + delay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getMemLevel(int adjustment) {
        int i = 0;
        while (true) {
            int[] iArr = this.mOomAdj;
            if (i < iArr.length) {
                if (adjustment > iArr[i]) {
                    i++;
                } else {
                    return this.mOomMinFree[i] * 1024;
                }
            } else {
                return this.mOomMinFree[iArr.length - 1] * 1024;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getCachedRestoreThresholdKb() {
        if (SystemProperties.get("ro.os_go.support").equals("1")) {
            this.mCachedRestoreLevel /= 2;
        }
        return this.mCachedRestoreLevel;
    }

    public static void setOomAdj(int pid, int uid, int amt) {
        if (pid <= 0 || amt == 1001) {
            return;
        }
        long start = SystemClock.elapsedRealtime();
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putInt(1);
        buf.putInt(pid);
        buf.putInt(uid);
        buf.putInt(amt);
        writeLmkd(buf, null);
        long now = SystemClock.elapsedRealtime();
        if (now - start > 250) {
            Slog.w(TAG, "SLOW OOM ADJ: " + (now - start) + "ms for pid " + pid + " = " + amt);
        }
    }

    public static final void remove(int pid) {
        if (pid <= 0) {
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putInt(2);
        buf.putInt(pid);
        writeLmkd(buf, null);
    }

    public static final Integer getLmkdKillCount(int min_oom_adj, int max_oom_adj) {
        ByteBuffer buf = ByteBuffer.allocate(12);
        ByteBuffer repl = ByteBuffer.allocate(8);
        buf.putInt(4);
        buf.putInt(min_oom_adj);
        buf.putInt(max_oom_adj);
        repl.putInt(4);
        repl.rewind();
        if (writeLmkd(buf, repl) && repl.getInt() == 4) {
            return new Integer(repl.getInt());
        }
        return null;
    }

    public boolean onLmkdConnect(OutputStream ostream) {
        try {
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.putInt(3);
            ostream.write(buf.array(), 0, buf.position());
            if (this.mOomLevelsSet) {
                ByteBuffer buf2 = ByteBuffer.allocate(((this.mOomAdj.length * 2) + 1) * 4);
                buf2.putInt(0);
                for (int i = 0; i < this.mOomAdj.length; i++) {
                    buf2.putInt((this.mOomMinFree[i] * 1024) / 4096);
                    buf2.putInt(this.mOomAdj[i]);
                }
                ostream.write(buf2.array(), 0, buf2.position());
            }
            ByteBuffer buf3 = ByteBuffer.allocate(8);
            buf3.putInt(5);
            buf3.putInt(0);
            ostream.write(buf3.array(), 0, buf3.position());
            ByteBuffer buf4 = ByteBuffer.allocate(8);
            buf4.putInt(5);
            buf4.putInt(1);
            ostream.write(buf4.array(), 0, buf4.position());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean writeLmkd(ByteBuffer buf, ByteBuffer repl) {
        if (!sLmkdConnection.isConnected()) {
            KillHandler killHandler = sKillHandler;
            killHandler.sendMessage(killHandler.obtainMessage(4001));
            if (!sLmkdConnection.waitForConnection(BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS)) {
                return false;
            }
        }
        return sLmkdConnection.exchange(buf, repl);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void killProcessGroup(int uid, int pid) {
        KillHandler killHandler = sKillHandler;
        if (killHandler != null) {
            killHandler.sendMessage(killHandler.obtainMessage(4000, uid, pid));
            return;
        }
        Slog.w(TAG, "Asked to kill process group before system bringup!");
        Process.killProcessGroup(uid, pid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord getProcessRecordLocked(String processName, int uid) {
        if (uid == 1000) {
            SparseArray<ProcessRecord> procs = (SparseArray) this.mProcessNames.getMap().get(processName);
            if (procs == null) {
                return null;
            }
            int procCount = procs.size();
            for (int i = 0; i < procCount; i++) {
                int procUid = procs.keyAt(i);
                if (UserHandle.isCore(procUid) && UserHandle.isSameUser(procUid, uid)) {
                    return procs.valueAt(i);
                }
            }
        }
        return (ProcessRecord) this.mProcessNames.get(processName, uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getMemoryInfo(ActivityManager.MemoryInfo outInfo) {
        long homeAppMem = getMemLevel(600);
        long cachedAppMem = getMemLevel(900);
        outInfo.availMem = Process.getFreeMemory();
        outInfo.totalMem = Process.getTotalMemory();
        outInfo.threshold = homeAppMem;
        outInfo.lowMemory = outInfo.availMem < ((cachedAppMem - homeAppMem) / 2) + homeAppMem;
        outInfo.hiddenAppThreshold = cachedAppMem;
        outInfo.secondaryServerThreshold = getMemLevel(500);
        outInfo.visibleAppThreshold = getMemLevel(100);
        outInfo.foregroundAppThreshold = getMemLevel(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord findAppProcessLOSP(IBinder app, String reason) {
        int NP = this.mProcessNames.getMap().size();
        for (int ip = 0; ip < NP; ip++) {
            SparseArray<ProcessRecord> apps = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
            int NA = apps.size();
            for (int ia = 0; ia < NA; ia++) {
                ProcessRecord p = apps.valueAt(ia);
                IApplicationThread thread = p.getThread();
                if (thread != null && thread.asBinder() == app) {
                    return p;
                }
            }
        }
        Slog.w(TAG, "Can't find mystery application for " + reason + " from pid=" + Binder.getCallingPid() + " uid=" + Binder.getCallingUid() + ": " + app);
        return null;
    }

    private void checkSlow(long startTime, String where) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime > 50) {
            Slog.w(TAG, "Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    private int[] computeGidsForProcess(int mountExternal, int uid, int[] permGids, boolean externalStorageAccess) {
        ArrayList<Integer> gidList = new ArrayList<>(permGids.length + 5);
        int sharedAppGid = UserHandle.getSharedAppGid(UserHandle.getAppId(uid));
        int cacheAppGid = UserHandle.getCacheAppGid(UserHandle.getAppId(uid));
        int userGid = UserHandle.getUserGid(UserHandle.getUserId(uid));
        for (int permGid : permGids) {
            gidList.add(Integer.valueOf(permGid));
        }
        if (sharedAppGid != -1) {
            gidList.add(Integer.valueOf(sharedAppGid));
        }
        if (cacheAppGid != -1) {
            gidList.add(Integer.valueOf(cacheAppGid));
        }
        if (userGid != -1) {
            gidList.add(Integer.valueOf(userGid));
        }
        if (mountExternal == 4 || mountExternal == 3) {
            gidList.add(Integer.valueOf(UserHandle.getUid(UserHandle.getUserId(uid), 1015)));
            gidList.add(1078);
            gidList.add(1079);
        }
        if (mountExternal == 2) {
            gidList.add(1079);
        }
        if (mountExternal == 3) {
            gidList.add(1023);
        }
        if (externalStorageAccess) {
            gidList.add(1077);
        }
        int[] gidArray = new int[gidList.size()];
        for (int i = 0; i < gidArray.length; i++) {
            gidArray[i] = gidList.get(i).intValue();
        }
        return gidArray;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1931=6] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startProcessLocked(ProcessRecord app, HostingRecord hostingRecord, int zygotePolicyFlags, boolean disableHiddenApiChecks, boolean disableTestApiChecks, String abiOverride) {
        boolean z;
        int[] gids;
        int mountExternal;
        int uid;
        boolean debuggableFlag;
        boolean isProfileableByShell;
        boolean isProfileable;
        int runtimeFlags;
        String invokeWith;
        boolean z2;
        String requiredAbi;
        String requiredAbi2;
        ApplicationInfo definingAppInfo;
        String instructionSet;
        ApplicationInfo definingAppInfo2;
        ApplicationInfo clientInfo;
        ProcessList processList = this;
        if (!processList.mService.mAmsExt.checkAutoBootPermission(processList.mService.mContext, app.processName, app.userId, processList.mLruProcesses, processList.mService.mCallingPid)) {
            processList.mService.mCallingPid = 0;
            Slog.d(TAG, "startProcessLocked, checkAutoBootPermission is false, start process processName:" + app.processName + " return directly!");
            return true;
        } else if (app.isPendingStart()) {
            return true;
        } else {
            long startUptime = SystemClock.uptimeMillis();
            long startElapsedTime = SystemClock.elapsedRealtime();
            if (app.getPid() > 0 && app.getPid() != ActivityManagerService.MY_PID) {
                processList.checkSlow(startUptime, "startProcess: removing from pids map");
                processList.mService.removePidLocked(app.getPid(), app);
                app.setBindMountPending(false);
                processList.mService.mHandler.removeMessages(20, app);
                processList.checkSlow(startUptime, "startProcess: done removing from pids map");
                app.setPid(0);
                app.setStartSeq(0L);
            }
            app.unlinkDeathRecipient();
            app.setDyingPid(0);
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES && processList.mService.mProcessesOnHold.contains(app)) {
                Slog.v(ActivityManagerService.TAG_PROCESSES, "startProcessLocked removing on hold: " + app);
            }
            processList.mService.mProcessesOnHold.remove(app);
            processList.checkSlow(startUptime, "startProcess: starting to update cpu stats");
            processList.mService.updateCpuStats();
            processList.checkSlow(startUptime, "startProcess: done updating cpu stats");
            processList.mService.mAmsExt.onStartProcess(hostingRecord.getType(), app.info.packageName);
            try {
                int userId = UserHandle.getUserId(app.uid);
                try {
                    try {
                        AppGlobals.getPackageManager().checkPackageStartable(app.info.packageName, userId);
                        int uid2 = app.uid;
                        if (app.isolated) {
                            gids = null;
                            mountExternal = 0;
                        } else {
                            try {
                                processList.checkSlow(startUptime, "startProcess: getting gids from package manager");
                                IPackageManager pm = AppGlobals.getPackageManager();
                                try {
                                    try {
                                        int[] permGids = pm.getPackageGids(app.info.packageName, 268435456L, app.userId);
                                        StorageManagerInternal storageManagerInternal = (StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class);
                                        int mountExternal2 = storageManagerInternal.getExternalStorageMountMode(uid2, app.info.packageName);
                                        boolean externalStorageAccess = storageManagerInternal.hasExternalStorageAccess(uid2, app.info.packageName);
                                        if (pm.checkPermission("android.permission.INSTALL_PACKAGES", app.info.packageName, userId) == 0) {
                                            try {
                                                Slog.i(TAG, app.info.packageName + " is exempt from freezer");
                                                app.mOptRecord.setFreezeExempt(true);
                                            } catch (RuntimeException e) {
                                                e = e;
                                                z = false;
                                                Slog.e(TAG, "Failure starting process " + app.processName, e);
                                                this.mService.forceStopPackageLocked(app.info.packageName, UserHandle.getAppId(app.uid), false, false, true, false, false, app.userId, "start failure");
                                                return z;
                                            }
                                        }
                                        if (app.processInfo == null || app.processInfo.deniedPermissions == null) {
                                            processList = this;
                                        } else {
                                            for (int i = app.processInfo.deniedPermissions.size() - 1; i >= 0; i--) {
                                                int[] denyGids = this.mService.mPackageManagerInt.getPermissionGids((String) app.processInfo.deniedPermissions.valueAt(i), app.userId);
                                                if (denyGids != null) {
                                                    for (int gid : denyGids) {
                                                        permGids = ArrayUtils.removeInt(permGids, gid);
                                                    }
                                                }
                                            }
                                            processList = this;
                                        }
                                        int[] gids2 = processList.computeGidsForProcess(mountExternal2, uid2, permGids, externalStorageAccess);
                                        gids = gids2;
                                        mountExternal = mountExternal2;
                                    } catch (RuntimeException e2) {
                                        e = e2;
                                    }
                                } catch (RemoteException e3) {
                                    e = e3;
                                    throw e.rethrowAsRuntimeException();
                                }
                            } catch (RemoteException e4) {
                                e = e4;
                            } catch (RuntimeException e5) {
                                e = e5;
                                z = false;
                            }
                        }
                        try {
                            app.setMountMode(mountExternal);
                            processList.checkSlow(startUptime, "startProcess: building args");
                            uid = processList.mService.mAtmInternal.isFactoryTestProcess(app.getWindowProcessController()) ? 0 : uid2;
                            int runtimeFlags2 = 0;
                            boolean debuggableFlag2 = (app.info.flags & 2) != 0;
                            boolean isProfileableByShell2 = app.info.isProfileableByShell();
                            boolean isProfileable2 = app.info.isProfileable();
                            if (!app.isSdkSandbox || (clientInfo = app.getClientInfoForSdkSandbox()) == null) {
                                debuggableFlag = debuggableFlag2;
                                isProfileableByShell = isProfileableByShell2;
                                isProfileable = isProfileable2;
                            } else {
                                debuggableFlag = debuggableFlag2 | ((clientInfo.flags & 2) != 0);
                                isProfileableByShell = isProfileableByShell2 | clientInfo.isProfileableByShell();
                                isProfileable = isProfileable2 | clientInfo.isProfileable();
                            }
                            if (debuggableFlag) {
                                int runtimeFlags3 = 0 | 1;
                                runtimeFlags2 = runtimeFlags3 | 256 | 2;
                                if (Settings.Global.getInt(processList.mService.mContext.getContentResolver(), "art_verifier_verify_debuggable", 1) == 0) {
                                    runtimeFlags2 |= 512;
                                    Slog.w(ActivityManagerService.TAG_PROCESSES, app + ": ART verification disabled");
                                }
                            }
                            if ((app.info.flags & 16384) != 0 || processList.mService.mSafeMode) {
                                runtimeFlags2 |= 8;
                            }
                            if (isProfileableByShell) {
                                runtimeFlags2 |= 32768;
                            }
                            if (isProfileable) {
                                runtimeFlags2 |= 16777216;
                            }
                            if ("1".equals(SystemProperties.get("debug.checkjni"))) {
                                runtimeFlags2 |= 2;
                            }
                            String genDebugInfoProperty = SystemProperties.get("debug.generate-debug-info");
                            if ("1".equals(genDebugInfoProperty) || "true".equals(genDebugInfoProperty)) {
                                runtimeFlags2 |= 32;
                            }
                            String genMiniDebugInfoProperty = SystemProperties.get("dalvik.vm.minidebuginfo");
                            if ("1".equals(genMiniDebugInfoProperty) || "true".equals(genMiniDebugInfoProperty)) {
                                runtimeFlags2 |= 2048;
                            }
                            if ("1".equals(SystemProperties.get("debug.jni.logging"))) {
                                runtimeFlags2 |= 16;
                            }
                            if ("1".equals(SystemProperties.get("debug.assert"))) {
                                runtimeFlags2 |= 4;
                            }
                            if ("1".equals(SystemProperties.get("debug.ignoreappsignalhandler"))) {
                                runtimeFlags2 |= 131072;
                            }
                            if (processList.mService.mNativeDebuggingApp == null || !processList.mService.mNativeDebuggingApp.equals(app.processName)) {
                                runtimeFlags = runtimeFlags2;
                            } else {
                                int runtimeFlags4 = runtimeFlags2 | 64 | 32 | 128;
                                processList.mService.mNativeDebuggingApp = null;
                                runtimeFlags = runtimeFlags4;
                            }
                            if (app.info.isEmbeddedDexUsed()) {
                                runtimeFlags |= 1024;
                            } else if (app.info.isPrivilegedApp()) {
                                PackageList pkgList = app.getPkgList();
                                synchronized (pkgList) {
                                    if (DexManager.isPackageSelectedToRunOob(pkgList.getPackageListLocked().keySet())) {
                                        runtimeFlags |= 1024;
                                    }
                                }
                            }
                            if (!disableHiddenApiChecks && !processList.mService.mHiddenApiBlacklist.isDisabled()) {
                                app.info.maybeUpdateHiddenApiEnforcementPolicy(processList.mService.mHiddenApiBlacklist.getPolicy());
                                int policy = IActivityManagerServiceLice.Instance().startProcessLocked(app.info.getHiddenApiEnforcementPolicy(), app.info);
                                int policyBits = policy << Zygote.API_ENFORCEMENT_POLICY_SHIFT;
                                if ((policyBits & 12288) != policyBits) {
                                    throw new IllegalStateException("Invalid API policy: " + policy);
                                }
                                runtimeFlags |= policyBits;
                                if (disableTestApiChecks) {
                                    runtimeFlags |= 262144;
                                }
                            }
                            String useAppImageCache = SystemProperties.get(PROPERTY_USE_APP_IMAGE_STARTUP_CACHE, "");
                            if (!TextUtils.isEmpty(useAppImageCache) && !useAppImageCache.equals("false")) {
                                runtimeFlags |= 65536;
                            }
                            String invokeWith2 = null;
                            if (debuggableFlag) {
                                String wrapperFileName = app.info.nativeLibraryDir + "/wrap.sh";
                                StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
                                try {
                                    if (new File(wrapperFileName).exists()) {
                                        try {
                                            invokeWith2 = "/system/bin/logwrapper " + wrapperFileName;
                                        } catch (Throwable th) {
                                            th = th;
                                            StrictMode.setThreadPolicy(oldPolicy);
                                            throw th;
                                        }
                                    }
                                    StrictMode.setThreadPolicy(oldPolicy);
                                    invokeWith = invokeWith2;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } else {
                                invokeWith = null;
                            }
                            String requiredAbi3 = abiOverride != null ? abiOverride : app.info.primaryCpuAbi;
                            if (requiredAbi3 == null) {
                                try {
                                    z2 = false;
                                } catch (RuntimeException e6) {
                                    e = e6;
                                    z2 = false;
                                    z = z2;
                                    Slog.e(TAG, "Failure starting process " + app.processName, e);
                                    this.mService.forceStopPackageLocked(app.info.packageName, UserHandle.getAppId(app.uid), false, false, true, false, false, app.userId, "start failure");
                                    return z;
                                }
                                try {
                                    requiredAbi = Build.SUPPORTED_ABIS[0];
                                } catch (RuntimeException e7) {
                                    e = e7;
                                    z = z2;
                                    Slog.e(TAG, "Failure starting process " + app.processName, e);
                                    this.mService.forceStopPackageLocked(app.info.packageName, UserHandle.getAppId(app.uid), false, false, true, false, false, app.userId, "start failure");
                                    return z;
                                }
                            } else {
                                z2 = false;
                                requiredAbi = requiredAbi3;
                            }
                        } catch (RuntimeException e8) {
                            e = e8;
                            z = false;
                        }
                    } catch (RuntimeException e9) {
                        e = e9;
                    }
                    try {
                        String instructionSet2 = app.info.primaryCpuAbi != null ? VMRuntime.getInstructionSet(requiredAbi) : null;
                        app.setGids(gids);
                        app.setRequiredAbi(requiredAbi);
                        app.setInstructionSet(instructionSet2);
                        if (hostingRecord.getDefiningPackageName() != null) {
                            requiredAbi2 = requiredAbi;
                            definingAppInfo = new ApplicationInfo(app.info);
                            definingAppInfo.packageName = hostingRecord.getDefiningPackageName();
                            definingAppInfo.uid = uid;
                        } else {
                            requiredAbi2 = requiredAbi;
                            definingAppInfo = app.info;
                        }
                        int runtimeFlags5 = runtimeFlags | Zygote.getMemorySafetyRuntimeFlags(definingAppInfo, app.processInfo, instructionSet2, processList.mPlatformCompat);
                        if (TextUtils.isEmpty(app.info.seInfoUser)) {
                            instructionSet = instructionSet2;
                            definingAppInfo2 = definingAppInfo;
                            Slog.wtf(TAG, "SELinux tag not defined", new IllegalStateException("SELinux tag not defined for " + app.info.packageName + " (uid " + app.uid + ")"));
                        } else {
                            instructionSet = instructionSet2;
                            definingAppInfo2 = definingAppInfo;
                        }
                        String seInfo = app.info.seInfo + (TextUtils.isEmpty(app.info.seInfoUser) ? "" : app.info.seInfoUser);
                        return startProcessLocked(hostingRecord, "android.app.ActivityThread", app, uid, gids, runtimeFlags5, zygotePolicyFlags, mountExternal, seInfo, requiredAbi2, instructionSet, invokeWith, startUptime, startElapsedTime);
                    } catch (RuntimeException e10) {
                        e = e10;
                        z = z2;
                        Slog.e(TAG, "Failure starting process " + app.processName, e);
                        this.mService.forceStopPackageLocked(app.info.packageName, UserHandle.getAppId(app.uid), false, false, true, false, false, app.userId, "start failure");
                        return z;
                    }
                } catch (RemoteException e11) {
                    throw e11.rethrowAsRuntimeException();
                }
            } catch (RuntimeException e12) {
                e = e12;
                z = false;
            }
        }
    }

    boolean startProcessLocked(HostingRecord hostingRecord, final String entryPoint, final ProcessRecord app, int uid, final int[] gids, final int runtimeFlags, final int zygotePolicyFlags, final int mountExternal, String seInfo, final String requiredAbi, final String instructionSet, final String invokeWith, long startUptime, long startElapsedTime) {
        app.setPendingStart(true);
        app.setRemoved(false);
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                app.setKilledByAm(false);
                app.setKilled(false);
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        if (app.getStartSeq() != 0) {
            Slog.wtf(TAG, "startProcessLocked processName:" + app.processName + " with non-zero startSeq:" + app.getStartSeq());
        }
        if (app.getPid() != 0) {
            Slog.wtf(TAG, "startProcessLocked processName:" + app.processName + " with non-zero pid:" + app.getPid());
        }
        app.setDisabledCompatChanges(null);
        PlatformCompat platformCompat = this.mPlatformCompat;
        if (platformCompat != null) {
            app.setDisabledCompatChanges(platformCompat.getDisabledChanges(app.info));
        }
        final long startSeq = this.mProcStartSeqCounter + 1;
        this.mProcStartSeqCounter = startSeq;
        app.setStartSeq(startSeq);
        app.setStartParams(uid, hostingRecord, seInfo, startUptime, startElapsedTime);
        app.setUsingWrapper((invokeWith == null && Zygote.getWrapProperty(app.processName) == null) ? false : true);
        this.mPendingStarts.put(startSeq, app);
        if (this.mService.mConstants.FLAG_PROCESS_START_ASYNC) {
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.i(ActivityManagerService.TAG_PROCESSES, "Posting procStart msg for " + app.toShortString());
            }
            this.mService.mProcStartHandler.post(new Runnable() { // from class: com.android.server.am.ProcessList$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    ProcessList.this.m1473lambda$startProcessLocked$0$comandroidserveramProcessList(app, entryPoint, gids, runtimeFlags, zygotePolicyFlags, mountExternal, requiredAbi, instructionSet, invokeWith, startSeq);
                }
            });
            return true;
        }
        try {
            Process.ProcessStartResult startResult = startProcess(hostingRecord, entryPoint, app, uid, gids, runtimeFlags, zygotePolicyFlags, mountExternal, seInfo, requiredAbi, instructionSet, invokeWith, startUptime);
            handleProcessStartedLocked(app, startResult.pid, startResult.usingWrapper, startSeq, false);
        } catch (RuntimeException e) {
            Slog.e(TAG, "Failure starting process " + app.processName, e);
            app.setPendingStart(false);
            this.mService.forceStopPackageLocked(app.info.packageName, UserHandle.getAppId(app.uid), false, false, true, false, false, app.userId, "start failure");
        }
        return app.getPid() > 0;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleProcessStart */
    public void m1473lambda$startProcessLocked$0$comandroidserveramProcessList(final ProcessRecord app, final String entryPoint, final int[] gids, final int runtimeFlags, final int zygotePolicyFlags, final int mountExternal, final String requiredAbi, final String instructionSet, final String invokeWith, final long startSeq) {
        Runnable startRunnable = new Runnable() { // from class: com.android.server.am.ProcessList$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ProcessList.this.m1471lambda$handleProcessStart$1$comandroidserveramProcessList(app, entryPoint, gids, runtimeFlags, zygotePolicyFlags, mountExternal, requiredAbi, instructionSet, invokeWith, startSeq);
            }
        };
        ProcessRecord predecessor = app.mPredecessor;
        if (predecessor == null || predecessor.getDyingPid() <= 0) {
            startRunnable.run();
        } else {
            handleProcessStartWithPredecessor(predecessor, startRunnable);
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:22:0x0051 */
    /* JADX DEBUG: Multi-variable search result rejected for r5v1, resolved type: long */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:48:0x0065 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r5v0 */
    /* JADX WARN: Type inference failed for: r5v2, types: [int] */
    /* JADX WARN: Type inference failed for: r5v3 */
    /* renamed from: lambda$handleProcessStart$1$com-android-server-am-ProcessList  reason: not valid java name */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public /* synthetic */ void m1471lambda$handleProcessStart$1$comandroidserveramProcessList(ProcessRecord app, String entryPoint, int[] gids, int runtimeFlags, int zygotePolicyFlags, int mountExternal, String requiredAbi, String instructionSet, String invokeWith, long startSeq) {
        long j;
        ProcessList processList;
        ProcessRecord processRecord;
        Process.ProcessStartResult startResult;
        try {
            HostingRecord hostingRecord = app.getHostingRecord();
            j = app.getStartUid();
            processRecord = app;
            try {
                startResult = startProcess(hostingRecord, entryPoint, processRecord, j, gids, runtimeFlags, zygotePolicyFlags, mountExternal, app.getSeInfo(), requiredAbi, instructionSet, invokeWith, app.getStartTime());
                processList = this;
            } catch (RuntimeException e) {
                e = e;
                processList = this;
            }
            try {
                try {
                    try {
                        synchronized (processList.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                processList.handleProcessStartedLocked(app, startResult, startSeq);
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                    } catch (RuntimeException e2) {
                        e = e2;
                        RuntimeException e3 = e;
                        synchronized (processList.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                Slog.e(TAG, "Failure starting process " + processRecord.processName, e3);
                                processList.mPendingStarts.remove(j);
                                processRecord.setPendingStart(false);
                                processList.mService.forceStopPackageLocked(processRecord.info.packageName, UserHandle.getAppId(processRecord.uid), false, false, true, false, false, processRecord.userId, "start failure");
                            } finally {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (RuntimeException e4) {
                e = e4;
                processRecord = app;
                j = startSeq;
                RuntimeException e32 = e;
                synchronized (processList.mService) {
                }
            }
        } catch (RuntimeException e5) {
            e = e5;
            j = startSeq;
            processList = this;
            processRecord = app;
        }
    }

    private void handleProcessStartWithPredecessor(ProcessRecord predecessor, Runnable successorStartRunnable) {
        if (predecessor.mSuccessorStartRunnable != null) {
            Slog.wtf(TAG, "We've been watching for the death of " + predecessor);
            return;
        }
        predecessor.mSuccessorStartRunnable = successorStartRunnable;
        this.mService.mProcStartHandler.sendMessageDelayed(this.mService.mProcStartHandler.obtainMessage(2, predecessor), this.mService.mConstants.mProcessKillTimeoutMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class ProcStartHandler extends Handler {
        static final int MSG_PROCESS_DIED = 1;
        static final int MSG_PROCESS_KILL_TIMEOUT = 2;
        private final ActivityManagerService mService;

        /* JADX INFO: Access modifiers changed from: package-private */
        public ProcStartHandler(ActivityManagerService service, Looper looper) {
            super(looper);
            this.mService = service;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    this.mService.mProcessList.handlePredecessorProcDied((ProcessRecord) msg.obj);
                    return;
                case 2:
                    synchronized (this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            this.mService.handleProcessStartOrKillTimeoutLocked((ProcessRecord) msg.obj, true);
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

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePredecessorProcDied(ProcessRecord app) {
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.i(TAG, app.toString() + " is really gone now");
        }
        Runnable start = app.mSuccessorStartRunnable;
        if (start != null) {
            app.mSuccessorStartRunnable = null;
            start.run();
        }
    }

    public void killAppZygoteIfNeededLocked(AppZygote appZygote, boolean force) {
        ApplicationInfo appInfo = appZygote.getAppInfo();
        ArrayList<ProcessRecord> zygoteProcesses = this.mAppZygoteProcesses.get(appZygote);
        if (zygoteProcesses != null) {
            if (force || zygoteProcesses.size() == 0) {
                this.mAppZygotes.remove(appInfo.processName, appInfo.uid);
                this.mAppZygoteProcesses.remove(appZygote);
                this.mAppIsolatedUidRangeAllocator.freeUidRangeLocked(appInfo);
                appZygote.stopZygote();
            }
        }
    }

    private void removeProcessFromAppZygoteLocked(ProcessRecord app) {
        IsolatedUidRange appUidRange = this.mAppIsolatedUidRangeAllocator.getIsolatedUidRangeLocked(app.info.processName, app.getHostingRecord().getDefiningUid());
        if (appUidRange != null) {
            appUidRange.freeIsolatedUidLocked(app.uid);
        }
        AppZygote appZygote = (AppZygote) this.mAppZygotes.get(app.info.processName, app.getHostingRecord().getDefiningUid());
        if (appZygote != null) {
            ArrayList<ProcessRecord> zygoteProcesses = this.mAppZygoteProcesses.get(appZygote);
            zygoteProcesses.remove(app);
            if (zygoteProcesses.size() == 0) {
                this.mService.mHandler.removeMessages(71);
                if (app.isRemoved()) {
                    killAppZygoteIfNeededLocked(appZygote, false);
                    return;
                }
                Message msg = this.mService.mHandler.obtainMessage(71);
                msg.obj = appZygote;
                this.mService.mHandler.sendMessageDelayed(msg, 5000L);
            }
        }
    }

    private AppZygote createAppZygoteForProcessIfNeeded(ProcessRecord app) {
        AppZygote appZygote;
        ArrayList<ProcessRecord> zygoteProcessList;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                int uid = app.getHostingRecord().getDefiningUid();
                appZygote = (AppZygote) this.mAppZygotes.get(app.info.processName, uid);
                if (appZygote == null) {
                    if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                        Slog.d(ActivityManagerService.TAG_PROCESSES, "Creating new app zygote.");
                    }
                    IsolatedUidRange uidRange = this.mAppIsolatedUidRangeAllocator.getIsolatedUidRangeLocked(app.info.processName, app.getHostingRecord().getDefiningUid());
                    int userId = UserHandle.getUserId(uid);
                    int firstUid = UserHandle.getUid(userId, uidRange.mFirstUid);
                    int lastUid = UserHandle.getUid(userId, uidRange.mLastUid);
                    ApplicationInfo appInfo = new ApplicationInfo(app.info);
                    appInfo.packageName = app.getHostingRecord().getDefiningPackageName();
                    appInfo.uid = uid;
                    appZygote = new AppZygote(appInfo, app.processInfo, uid, firstUid, lastUid);
                    this.mAppZygotes.put(app.info.processName, uid, appZygote);
                    zygoteProcessList = new ArrayList<>();
                    this.mAppZygoteProcesses.put(appZygote, zygoteProcessList);
                } else {
                    if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                        Slog.d(ActivityManagerService.TAG_PROCESSES, "Reusing existing app zygote.");
                    }
                    this.mService.mHandler.removeMessages(71, appZygote);
                    zygoteProcessList = this.mAppZygoteProcesses.get(appZygote);
                }
                zygoteProcessList.add(app);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return appZygote;
    }

    private Map<String, Pair<String, Long>> getPackageAppDataInfoMap(PackageManagerInternal pmInt, String[] packages, int uid) {
        Map<String, Pair<String, Long>> result = new ArrayMap<>(packages.length);
        int userId = UserHandle.getUserId(uid);
        for (String packageName : packages) {
            PackageStateInternal packageState = pmInt.getPackageStateInternal(packageName);
            if (packageState == null) {
                Slog.w(TAG, "Unknown package:" + packageName);
            } else {
                String volumeUuid = packageState.getVolumeUuid();
                long inode = packageState.getUserStateOrDefault(userId).getCeDataInode();
                if (inode == 0) {
                    Slog.w(TAG, packageName + " inode == 0 (b/152760674)");
                    return null;
                }
                result.put(packageName, Pair.create(volumeUuid, Long.valueOf(inode)));
            }
        }
        return result;
    }

    private boolean needsStorageDataIsolation(StorageManagerInternal storageManagerInternal, ProcessRecord app) {
        int mountMode = app.getMountMode();
        return (!this.mVoldAppDataIsolationEnabled || !UserHandle.isApp(app.uid) || storageManagerInternal.isExternalStorageService(app.uid) || mountMode == 4 || mountMode == 3 || mountMode == 2 || mountMode == 0) ? false : true;
    }

    private Process.ProcessStartResult startProcess(HostingRecord hostingRecord, String entryPoint, ProcessRecord app, int uid, int[] gids, int runtimeFlags, int zygotePolicyFlags, int mountExternal, String seInfo, String requiredAbi, String instructionSet, String invokeWith, long startTime) {
        long j;
        boolean bindMountAppsData;
        boolean bindMountAppStorageDirs;
        Map<String, Pair<String, Long>> allowlistedAppDataInfoMap;
        Map<String, Pair<String, Long>> pkgDataInfoMap;
        StorageManagerInternal storageManagerInternal;
        int userId;
        int zygotePolicyFlags2;
        Process.ProcessStartResult startResult;
        boolean regularZygote;
        try {
            Trace.traceBegin(64L, "Start proc: " + app.processName);
            checkSlow(startTime, "startProcess: asking zygote to start proc");
            boolean isTopApp = hostingRecord.isTopApp();
            if (isTopApp) {
                app.mState.setHasForegroundActivities(true);
            }
            boolean bindMountAppsData2 = this.mAppDataIsolationEnabled && (UserHandle.isApp(app.uid) || UserHandle.isIsolated(app.uid)) && this.mPlatformCompat.isChangeEnabled(APP_DATA_DIRECTORY_ISOLATION, app.info);
            PackageManagerInternal pmInt = this.mService.getPackageManagerInternal();
            String[] sharedPackages = pmInt.getSharedUserPackagesForPackage(app.info.packageName, app.userId);
            String[] targetPackagesList = sharedPackages.length == 0 ? new String[]{app.info.packageName} : sharedPackages;
            boolean hasAppStorage = hasAppStorage(pmInt, app.info.packageName);
            Map<String, Pair<String, Long>> pkgDataInfoMap2 = getPackageAppDataInfoMap(pmInt, targetPackagesList, uid);
            if (pkgDataInfoMap2 == null) {
                bindMountAppsData2 = false;
            }
            try {
                Set<String> allowlistedApps = new ArraySet<>(this.mAppDataIsolationAllowlistedApps);
                for (String pkg : targetPackagesList) {
                    allowlistedApps.remove(pkg);
                }
                Map<String, Pair<String, Long>> allowlistedAppDataInfoMap2 = getPackageAppDataInfoMap(pmInt, (String[]) allowlistedApps.toArray(new String[0]), uid);
                if (allowlistedAppDataInfoMap2 == null) {
                    bindMountAppsData2 = false;
                }
                if (hasAppStorage) {
                    bindMountAppsData = bindMountAppsData2;
                } else {
                    pkgDataInfoMap2 = null;
                    allowlistedAppDataInfoMap2 = null;
                    bindMountAppsData = false;
                }
                int userId2 = UserHandle.getUserId(uid);
                StorageManagerInternal storageManagerInternal2 = (StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class);
                if (!needsStorageDataIsolation(storageManagerInternal2, app)) {
                    bindMountAppStorageDirs = false;
                } else if (pkgDataInfoMap2 != null && storageManagerInternal2.isFuseMounted(userId2)) {
                    bindMountAppStorageDirs = true;
                } else {
                    app.setBindMountPending(true);
                    bindMountAppStorageDirs = false;
                }
                boolean bindMountAppStorageDirs2 = app.isolated;
                if (!bindMountAppStorageDirs2) {
                    allowlistedAppDataInfoMap = allowlistedAppDataInfoMap2;
                    pkgDataInfoMap = pkgDataInfoMap2;
                } else {
                    allowlistedAppDataInfoMap = null;
                    pkgDataInfoMap = null;
                }
                AppStateTracker ast = this.mService.mServices.mAppStateTracker;
                if (ast != null) {
                    boolean inBgRestricted = ast.isAppBackgroundRestricted(app.info.uid, app.info.packageName);
                    if (inBgRestricted) {
                        synchronized (this.mService) {
                            ActivityManagerService.boostPriorityForLockedSection();
                            this.mAppsInBackgroundRestricted.add(app);
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                    app.mState.setBackgroundRestricted(inBgRestricted);
                }
                try {
                    if (hostingRecord.usesWebviewZygote()) {
                        try {
                            storageManagerInternal = storageManagerInternal2;
                            userId = userId2;
                            j = 64;
                            zygotePolicyFlags2 = zygotePolicyFlags;
                            startResult = Process.startWebView(entryPoint, app.processName, uid, uid, gids, runtimeFlags, mountExternal, app.info.targetSdkVersion, seInfo, requiredAbi, instructionSet, app.info.dataDir, null, app.info.packageName, app.getDisabledCompatChanges(), new String[]{"seq=" + app.getStartSeq()});
                            regularZygote = false;
                        } catch (Throwable th) {
                            th = th;
                            j = 64;
                            Trace.traceEnd(j);
                            throw th;
                        }
                    } else {
                        storageManagerInternal = storageManagerInternal2;
                        userId = userId2;
                        j = 64;
                        boolean regularZygote2 = hostingRecord.usesAppZygote();
                        if (regularZygote2) {
                            AppZygote appZygote = createAppZygoteForProcessIfNeeded(app);
                            startResult = appZygote.getProcess().start(entryPoint, app.processName, uid, uid, gids, runtimeFlags, mountExternal, app.info.targetSdkVersion, seInfo, requiredAbi, instructionSet, app.info.dataDir, (String) null, app.info.packageName, 0, isTopApp, app.getDisabledCompatChanges(), pkgDataInfoMap, allowlistedAppDataInfoMap, false, false, new String[]{"seq=" + app.getStartSeq()});
                            zygotePolicyFlags2 = zygotePolicyFlags;
                            regularZygote = false;
                        } else {
                            try {
                                zygotePolicyFlags2 = ZygoteConfigExt.updateZygotePolicyFlags(this.mService.mContext.getContentResolver(), zygotePolicyFlags);
                                regularZygote = true;
                            } catch (Throwable th2) {
                                th = th2;
                                Trace.traceEnd(j);
                                throw th;
                            }
                            try {
                                startResult = Process.start(entryPoint, app.processName, uid, uid, gids, runtimeFlags, mountExternal, app.info.targetSdkVersion, seInfo, requiredAbi, instructionSet, app.info.dataDir, invokeWith, app.info.packageName, zygotePolicyFlags2, isTopApp, app.getDisabledCompatChanges(), pkgDataInfoMap, allowlistedAppDataInfoMap, bindMountAppsData, bindMountAppStorageDirs, new String[]{"seq=" + app.getStartSeq()});
                            } catch (Throwable th3) {
                                th = th3;
                                Trace.traceEnd(j);
                                throw th;
                            }
                        }
                    }
                    if (!regularZygote) {
                        try {
                            if (Process.createProcessGroup(uid, startResult.pid) < 0) {
                                Slog.e(TAG, "Unable to create process group for " + app.processName + " (" + startResult.pid + ")");
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            Trace.traceEnd(j);
                            throw th;
                        }
                    }
                    if (bindMountAppStorageDirs) {
                        storageManagerInternal.prepareStorageDirs(userId, pkgDataInfoMap.keySet(), app.processName);
                    }
                    try {
                        checkSlow(startTime, "startProcess: returned from zygote!");
                        Trace.traceEnd(j);
                        return startResult;
                    } catch (Throwable th5) {
                        th = th5;
                        Trace.traceEnd(j);
                        throw th;
                    }
                } catch (Throwable th6) {
                    th = th6;
                }
            } catch (Throwable th7) {
                th = th7;
                j = 64;
            }
        } catch (Throwable th8) {
            th = th8;
            j = 64;
        }
    }

    private boolean hasAppStorage(PackageManagerInternal pmInt, String packageName) {
        AndroidPackage pkg = pmInt.getPackage(packageName);
        if (pkg == null) {
            Slog.w(TAG, "Unknown package " + packageName);
            return false;
        }
        PackageManager.Property noAppStorageProp = pkg.getProperties().get("android.internal.PROPERTY_NO_APP_DATA_STORAGE");
        return noAppStorageProp == null || !noAppStorageProp.getBoolean();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startProcessLocked(ProcessRecord app, HostingRecord hostingRecord, int zygotePolicyFlags) {
        startProcessLocked(app, hostingRecord, zygotePolicyFlags, null);
    }

    boolean startProcessLocked(ProcessRecord app, HostingRecord hostingRecord, int zygotePolicyFlags, String abiOverride) {
        return startProcessLocked(app, hostingRecord, zygotePolicyFlags, false, false, abiOverride);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord startProcessLocked(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, HostingRecord hostingRecord, int zygotePolicyFlags, boolean allowWhileBooting, boolean isolated, int isolatedUid, boolean isSdkSandbox, int sdkSandboxUid, String sdkSandboxClientAppPackage, String abiOverride, String entryPoint, String[] entryPointArgs, Runnable crashHandler) {
        ProcessRecord app;
        ProcessRecord app2;
        ProcessRecord predecessor;
        ProcessList processList;
        long startTime;
        ProcessRecord app3;
        long startTime2 = SystemClock.uptimeMillis();
        if (!isolated) {
            app = getProcessRecordLocked(processName, info.uid);
            checkSlow(startTime2, "startProcess: after getProcessRecord");
            if ((intentFlags & 4) != 0) {
                if (this.mService.mAppErrors.isBadProcess(processName, info.uid)) {
                    if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                        Slog.v(TAG, "Bad process: " + info.uid + SliceClientPermissions.SliceAuthority.DELIMITER + processName);
                    }
                    return null;
                }
            } else {
                if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.v(TAG, "Clearing bad process: " + info.uid + SliceClientPermissions.SliceAuthority.DELIMITER + processName);
                }
                this.mService.mAppErrors.resetProcessCrashTime(processName, info.uid);
                if (this.mService.mAppErrors.isBadProcess(processName, info.uid)) {
                    EventLog.writeEvent((int) EventLogTags.AM_PROC_GOOD, Integer.valueOf(UserHandle.getUserId(info.uid)), Integer.valueOf(info.uid), info.processName);
                    this.mService.mAppErrors.clearBadProcess(processName, info.uid);
                    if (app != null) {
                        app.mErrorState.setBad(false);
                    }
                }
            }
        } else {
            app = null;
        }
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.v(ActivityManagerService.TAG_PROCESSES, "startProcess: name=" + processName + " app=" + app + " knownToBeDead=" + knownToBeDead + " thread=" + (app != null ? app.getThread() : null) + " pid=" + (app != null ? app.getPid() : -1));
        }
        if (app != null && app.getPid() > 0) {
            if ((!knownToBeDead && !app.isKilled()) || app.getThread() == null) {
                if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.v(ActivityManagerService.TAG_PROCESSES, "App already running: " + app);
                }
                app.addPackage(info.packageName, info.longVersionCode, this.mService.mProcessStats);
                checkSlow(startTime2, "startProcess: done, added package to proc");
                return app;
            }
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.v(ActivityManagerService.TAG_PROCESSES, "App died: " + app);
            }
            checkSlow(startTime2, "startProcess: bad proc running, killing");
            killProcessGroup(app.uid, app.getPid());
            checkSlow(startTime2, "startProcess: done killing old proc");
            if (!app.isKilled()) {
                Slog.wtf(ActivityManagerService.TAG_PROCESSES, app.toString() + " is attached to a previous process");
            } else {
                Slog.w(ActivityManagerService.TAG_PROCESSES, app.toString() + " is attached to a previous process");
            }
            ProcessRecord predecessor2 = app;
            app2 = null;
            predecessor = predecessor2;
        } else if (isolated) {
            app2 = app;
            predecessor = null;
        } else {
            ProcessRecord predecessor3 = (ProcessRecord) this.mDyingProcesses.get(processName, info.uid);
            if (predecessor3 != null) {
                if (app != null) {
                    app.mPredecessor = predecessor3;
                    predecessor3.mSuccessor = app;
                }
                Slog.w(ActivityManagerService.TAG_PROCESSES, predecessor3.toString() + " is attached to a previous process " + predecessor3.getDyingPid());
            }
            app2 = app;
            predecessor = predecessor3;
        }
        if (app2 == null) {
            checkSlow(startTime2, "startProcess: creating new process record");
            ProcessRecord predecessor4 = predecessor;
            app3 = newProcessRecordLocked(info, processName, isolated, isolatedUid, isSdkSandbox, sdkSandboxUid, sdkSandboxClientAppPackage, hostingRecord);
            if (app3 == null) {
                Slog.w(TAG, "Failed making new process record for " + processName + SliceClientPermissions.SliceAuthority.DELIMITER + info.uid + " isolated=" + isolated);
                return null;
            }
            app3.mErrorState.setCrashHandler(crashHandler);
            app3.setIsolatedEntryPoint(entryPoint);
            app3.setIsolatedEntryPointArgs(entryPointArgs);
            if (predecessor4 != null) {
                app3.mPredecessor = predecessor4;
                predecessor4.mSuccessor = app3;
            }
            processList = this;
            startTime = startTime2;
            processList.checkSlow(startTime, "startProcess: done creating new process record");
        } else {
            ProcessRecord app4 = app2;
            processList = this;
            startTime = startTime2;
            app4.addPackage(info.packageName, info.longVersionCode, processList.mService.mProcessStats);
            processList.checkSlow(startTime, "startProcess: added package to existing proc");
            app3 = app4;
        }
        if (!processList.mService.mProcessesReady && !processList.mService.isAllowedWhileBooting(info) && !allowWhileBooting) {
            if (!processList.mService.mProcessesOnHold.contains(app3)) {
                processList.mService.mProcessesOnHold.add(app3);
            }
            if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.v(ActivityManagerService.TAG_PROCESSES, "System not ready, putting on hold: " + app3);
            }
            processList.checkSlow(startTime, "startProcess: returning with proc on hold");
            return app3;
        }
        processList.checkSlow(startTime, "startProcess: stepping in to startProcess");
        boolean success = processList.startProcessLocked(app3, hostingRecord, zygotePolicyFlags, abiOverride);
        processList.checkSlow(startTime, "startProcess: done starting proc!");
        if (success) {
            return app3;
        }
        return null;
    }

    private String isProcStartValidLocked(ProcessRecord app, long expectedStartSeq) {
        if (app.isKilledByAm()) {
            sb = 0 == 0 ? new StringBuilder() : null;
            sb.append("killedByAm=true;");
        }
        if (this.mProcessNames.get(app.processName, app.uid) != app) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            sb.append("No entry in mProcessNames;");
        }
        if (!app.isPendingStart()) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            sb.append("pendingStart=false;");
        }
        if (app.getStartSeq() > expectedStartSeq) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            sb.append("seq=" + app.getStartSeq() + ",expected=" + expectedStartSeq + ";");
        }
        try {
            AppGlobals.getPackageManager().checkPackageStartable(app.info.packageName, app.userId);
        } catch (RemoteException e) {
        } catch (SecurityException e2) {
            if (this.mService.mConstants.FLAG_PROCESS_START_ASYNC) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append("Package is frozen;");
            } else {
                throw e2;
            }
        }
        if (sb == null) {
            return null;
        }
        return sb.toString();
    }

    private boolean handleProcessStartedLocked(ProcessRecord pending, Process.ProcessStartResult startResult, long expectedStartSeq) {
        if (this.mPendingStarts.get(expectedStartSeq) == null) {
            if (pending.getPid() == startResult.pid) {
                pending.setUsingWrapper(startResult.usingWrapper);
                return false;
            }
            return false;
        }
        return handleProcessStartedLocked(pending, startResult.pid, startResult.usingWrapper, expectedStartSeq, false);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[MOVE_EXCEPTION] complete} */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleProcessStartedLocked(ProcessRecord app, int pid, boolean usingWrapper, long expectedStartSeq, boolean procAttached) {
        ProcessRecord oldApp;
        this.mPendingStarts.remove(expectedStartSeq);
        String reason = isProcStartValidLocked(app, expectedStartSeq);
        if (reason != null) {
            Slog.w(ActivityManagerService.TAG_PROCESSES, app + " start not valid, killing pid=" + pid + ", " + reason);
            app.setPendingStart(false);
            Process.killProcessQuiet(pid);
            Process.killProcessGroup(app.uid, app.getPid());
            noteAppKill(app, 13, 13, reason);
            return false;
        }
        this.mService.mBatteryStatsService.noteProcessStart(app.processName, app.info.uid);
        checkSlow(app.getStartTime(), "startProcess: done updating battery stats");
        Object[] objArr = new Object[6];
        objArr[0] = Integer.valueOf(UserHandle.getUserId(app.getStartUid()));
        objArr[1] = Integer.valueOf(pid);
        objArr[2] = Integer.valueOf(app.getStartUid());
        objArr[3] = app.processName;
        objArr[4] = app.getHostingRecord().getType();
        objArr[5] = app.getHostingRecord().getName() != null ? app.getHostingRecord().getName() : "";
        EventLog.writeEvent((int) EventLogTags.AM_PROC_START, objArr);
        try {
            AppGlobals.getPackageManager().logAppProcessStartIfNeeded(app.info.packageName, app.processName, app.uid, app.getSeInfo(), app.info.sourceDir, pid);
        } catch (RemoteException e) {
        }
        ActivityManagerService.sMtkSystemServerIns.addBootEvent("AP_Init:[" + app.getHostingRecord().getType() + "]:[" + app.processName + (app.getHostingRecord().getName() != null ? "]:[" + app.getHostingRecord().getName() : "") + "]:pid:" + pid + (app.isPersistent() ? ":(PersistAP)" : ""));
        Watchdog.getInstance().processStarted(app.processName, pid);
        checkSlow(app.getStartTime(), "startProcess: building log message");
        StringBuilder buf = this.mStringBuilder;
        buf.setLength(0);
        buf.append("Start proc ");
        buf.append(pid);
        buf.append(':');
        buf.append(app.processName);
        buf.append('/');
        UserHandle.formatUid(buf, app.getStartUid());
        if (app.getIsolatedEntryPoint() != null) {
            buf.append(" [");
            buf.append(app.getIsolatedEntryPoint());
            buf.append("]");
        }
        buf.append(" for ");
        buf.append(app.getHostingRecord().getType());
        if (app.getHostingRecord().getName() != null) {
            buf.append(" ");
            buf.append(app.getHostingRecord().getName());
        }
        this.mService.reportUidInfoMessageLocked(TAG, buf.toString(), app.getStartUid());
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                app.setPid(pid);
                app.setUsingWrapper(usingWrapper);
                app.setPendingStart(false);
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        checkSlow(app.getStartTime(), "startProcess: starting to update pids map");
        synchronized (this.mService.mPidsSelfLocked) {
            try {
                oldApp = this.mService.mPidsSelfLocked.get(pid);
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
        if (oldApp != null && !app.isolated) {
            Slog.wtf(TAG, "handleProcessStartedLocked process:" + app.processName + " startSeq:" + app.getStartSeq() + " pid:" + pid + " belongs to another existing app:" + oldApp.processName + " startSeq:" + oldApp.getStartSeq());
            this.mService.cleanUpApplicationRecordLocked(oldApp, pid, false, false, -1, true, false);
        }
        this.mService.addPidLocked(app);
        synchronized (this.mService.mPidsSelfLocked) {
            if (!procAttached) {
                Message msg = this.mService.mHandler.obtainMessage(20);
                msg.obj = app;
                this.mService.mHandler.sendMessageDelayed(msg, usingWrapper ? 1200000L : ActivityManagerService.PROC_START_TIMEOUT);
            }
        }
        ITranProcessList.Instance().hookProcStart(app.processWrapper, (app.getHostingRecord() == null || app.getHostingRecord().getType() == null) ? "" : app.getHostingRecord().getType(), (app.getHostingRecord() == null || app.getHostingRecord().getName() == null) ? "" : app.getHostingRecord().getName());
        checkSlow(app.getStartTime(), "startProcess: done updating pids map");
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeLruProcessLocked(ProcessRecord app) {
        int lrui = this.mLruProcesses.lastIndexOf(app);
        if (lrui >= 0) {
            synchronized (this.mProcLock) {
                try {
                    ActivityManagerService.boostPriorityForProcLockedSection();
                    if (!app.isKilled()) {
                        if (app.isPersistent()) {
                            Slog.w(TAG, "Removing persistent process that hasn't been killed: " + app);
                        } else {
                            Slog.wtfStack(TAG, "Removing process that hasn't been killed: " + app);
                            if (app.getPid() > 0) {
                                Process.killProcessQuiet(app.getPid());
                                killProcessGroup(app.uid, app.getPid());
                                noteAppKill(app, 13, 16, "hasn't been killed");
                            } else {
                                app.setPendingStart(false);
                            }
                        }
                    }
                    int i = this.mLruProcessActivityStart;
                    if (lrui < i) {
                        this.mLruProcessActivityStart = i - 1;
                    }
                    int i2 = this.mLruProcessServiceStart;
                    if (lrui < i2) {
                        this.mLruProcessServiceStart = i2 - 1;
                    }
                    this.mLruProcesses.remove(lrui);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterProcLockedSection();
        }
        this.mService.removeOomAdjTargetLocked(app, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean killPackageProcessesLSP(String packageName, int appId, int userId, int minOomAdj, int reasonCode, int subReason, String reason) {
        return killPackageProcessesLSP(packageName, appId, userId, minOomAdj, false, true, true, false, false, false, reasonCode, subReason, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killAppZygotesLocked(String packageName, int appId, int userId, boolean force) {
        ArrayList<AppZygote> zygotesToKill = new ArrayList<>();
        for (SparseArray<AppZygote> appZygotes : this.mAppZygotes.getMap().values()) {
            for (int i = 0; i < appZygotes.size(); i++) {
                int appZygoteUid = appZygotes.keyAt(i);
                if ((userId == -1 || UserHandle.getUserId(appZygoteUid) == userId) && (appId < 0 || UserHandle.getAppId(appZygoteUid) == appId)) {
                    AppZygote appZygote = appZygotes.valueAt(i);
                    if (packageName == null || packageName.equals(appZygote.getAppInfo().packageName)) {
                        zygotesToKill.add(appZygote);
                    }
                }
            }
        }
        Iterator<AppZygote> it = zygotesToKill.iterator();
        while (it.hasNext()) {
            killAppZygoteIfNeededLocked(it.next(), force);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean killPackageProcessesLSP(String packageName, int appId, int userId, int minOomAdj, boolean callerWillRestart, boolean allowRestart, boolean doit, boolean evenPersistent, boolean setRemoved, boolean uninstalling, int reasonCode, int subReason, String reason) {
        SparseArray<ProcessRecord> apps;
        boolean z;
        int NA;
        boolean shouldAllowRestart;
        boolean z2;
        PackageManagerInternal pm = this.mService.getPackageManagerInternal();
        ArrayList<Pair<ProcessRecord, Boolean>> procs = new ArrayList<>();
        int NP = this.mProcessNames.getMap().size();
        for (int ip = 0; ip < NP; ip++) {
            SparseArray<ProcessRecord> apps2 = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
            int NA2 = apps2.size();
            int ia = 0;
            while (ia < NA2) {
                ProcessRecord app = apps2.valueAt(ia);
                if (app.isPersistent() && !evenPersistent) {
                    apps = apps2;
                    NA = NA2;
                } else if (app.isRemoved()) {
                    if (!doit) {
                        apps = apps2;
                        NA = NA2;
                    } else {
                        boolean shouldAllowRestart2 = false;
                        if (uninstalling || packageName == null) {
                            apps = apps2;
                        } else {
                            if (app.getPkgList().containsKey(packageName)) {
                                apps = apps2;
                            } else if (app.getPkgDeps() == null) {
                                apps = apps2;
                            } else if (!app.getPkgDeps().contains(packageName) || app.info == null) {
                                apps = apps2;
                            } else {
                                apps = apps2;
                                if (!pm.isPackageFrozen(app.info.packageName, app.uid, app.userId)) {
                                    z2 = true;
                                    shouldAllowRestart2 = z2;
                                }
                            }
                            z2 = false;
                            shouldAllowRestart2 = z2;
                        }
                        procs.add(new Pair<>(app, Boolean.valueOf(shouldAllowRestart2)));
                        NA = NA2;
                    }
                } else {
                    apps = apps2;
                    if (app.mState.getSetAdj() < minOomAdj) {
                        if (app.mState.isAgaresComputeOomAdj() || "com.transsion.tower".equals(app.processName)) {
                            Slog.w(TAG, "killAllBackgroundProcesses no continue app= " + app);
                        } else if (!this.mKillAllBackgroundProcessesByShell || minOomAdj != 900) {
                            NA = NA2;
                        } else if (!TranAmHooker.isKeepAliveSupport() || !ITranGriffinFeature.Instance().getPM().isPromoteToBS(app.processName)) {
                            NA = NA2;
                        } else {
                            Slog.w(TAG, "killAllBackgroundProcesses no continue app= " + app + "reason : skip aurora 1.1");
                        }
                    }
                    if (packageName == null) {
                        if (userId != -1 && app.userId != userId) {
                            NA = NA2;
                        } else if (appId < 0 || UserHandle.getAppId(app.uid) == appId) {
                            z = false;
                            NA = NA2;
                            shouldAllowRestart = z;
                        } else {
                            NA = NA2;
                        }
                    } else {
                        boolean isDep = app.getPkgDeps() != null && app.getPkgDeps().contains(packageName);
                        if (!isDep && UserHandle.getAppId(app.uid) != appId) {
                            NA = NA2;
                        } else if (userId != -1 && app.userId != userId) {
                            NA = NA2;
                        } else {
                            boolean isInPkgList = app.getPkgList().containsKey(packageName);
                            if (!isInPkgList && !isDep) {
                                NA = NA2;
                            } else {
                                if (!isInPkgList && isDep && !uninstalling && app.info != null) {
                                    String str = app.info.packageName;
                                    z = false;
                                    int i = app.uid;
                                    NA = NA2;
                                    int NA3 = app.userId;
                                    shouldAllowRestart = pm.isPackageFrozen(str, i, NA3) ? true : true;
                                } else {
                                    z = false;
                                    NA = NA2;
                                }
                                shouldAllowRestart = z;
                            }
                        }
                    }
                    if (!doit) {
                        return true;
                    }
                    if (setRemoved) {
                        app.setRemoved(true);
                    }
                    procs.add(new Pair<>(app, Boolean.valueOf(shouldAllowRestart)));
                }
                ia++;
                apps2 = apps;
                NA2 = NA;
            }
        }
        boolean z3 = true;
        int N = procs.size();
        int i2 = 0;
        while (i2 < N) {
            Pair<ProcessRecord, Boolean> proc = procs.get(i2);
            removeProcessLocked((ProcessRecord) proc.first, callerWillRestart, (allowRestart || ((Boolean) proc.second).booleanValue()) ? z3 : false, reasonCode, subReason, reason);
            i2++;
            z3 = z3;
        }
        boolean z4 = z3;
        killAppZygotesLocked(packageName, appId, userId, false);
        this.mService.updateOomAdjLocked("updateOomAdj_processEnd");
        if (N > 0) {
            return z4;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeProcessLocked(ProcessRecord app, boolean callerWillRestart, boolean allowRestart, int reasonCode, String reason) {
        return removeProcessLocked(app, callerWillRestart, allowRestart, reasonCode, 0, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeProcessLocked(ProcessRecord app, boolean callerWillRestart, boolean allowRestart, int reasonCode, int subReason, String reason) {
        boolean willRestart;
        boolean needRestart;
        String name = app.processName;
        int uid = app.uid;
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.d(ActivityManagerService.TAG_PROCESSES, "Force removing proc " + app.toShortString() + " (" + name + SliceClientPermissions.SliceAuthority.DELIMITER + uid + ")");
        }
        ProcessRecord old = (ProcessRecord) this.mProcessNames.get(name, uid);
        if (old != app) {
            Slog.w(TAG, "Ignoring remove of inactive process: " + app);
            return false;
        }
        removeProcessNameLocked(name, uid);
        this.mService.mAtmInternal.clearHeavyWeightProcessIfEquals(app.getWindowProcessController());
        int pid = app.getPid();
        if ((pid > 0 && pid != ActivityManagerService.MY_PID) || (pid == 0 && app.isPendingStart())) {
            if (pid > 0) {
                this.mService.removePidLocked(pid, app);
                app.setBindMountPending(false);
                this.mService.mHandler.removeMessages(20, app);
                this.mService.mBatteryStatsService.noteProcessFinish(app.processName, app.info.uid);
                if (app.isolated) {
                    this.mService.mBatteryStatsService.removeIsolatedUid(app.uid, app.info.uid);
                    this.mService.getPackageManagerInternal().removeIsolatedUid(app.uid);
                }
            }
            if (app.isPersistent() && !app.isolated) {
                if (callerWillRestart) {
                    willRestart = false;
                    needRestart = true;
                } else {
                    willRestart = true;
                    needRestart = false;
                }
            } else {
                willRestart = false;
                needRestart = false;
            }
            app.killLocked(reason, reasonCode, subReason, true);
            this.mService.handleAppDiedLocked(app, pid, willRestart, allowRestart, false);
            if (willRestart) {
                removeLruProcessLocked(app);
                this.mService.addAppLocked(app.info, null, false, null, 0);
            }
            return needRestart;
        }
        this.mRemovedProcesses.add(app);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addProcessNameLocked(ProcessRecord proc) {
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                ProcessRecord old = removeProcessNameLocked(proc.processName, proc.uid);
                if (old == proc && proc.isPersistent()) {
                    Slog.w(TAG, "Re-adding persistent process " + proc);
                } else if (old != null) {
                    if (old.isKilled()) {
                        Slog.w(TAG, "Existing proc " + old + " was killed " + (SystemClock.uptimeMillis() - old.getKillTime()) + "ms ago when adding " + proc);
                    } else {
                        Slog.wtf(TAG, "Already have existing proc " + old + " when adding " + proc);
                    }
                }
                UidRecord uidRec = this.mActiveUids.get(proc.uid);
                if (uidRec == null) {
                    uidRec = new UidRecord(proc.uid, this.mService);
                    if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                        Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "Creating new process uid: " + uidRec);
                    }
                    if (Arrays.binarySearch(this.mService.mDeviceIdleTempAllowlist, UserHandle.getAppId(proc.uid)) >= 0 || this.mService.mPendingTempAllowlist.indexOfKey(proc.uid) >= 0) {
                        uidRec.setCurAllowListed(true);
                        uidRec.setSetAllowListed(true);
                    }
                    uidRec.updateHasInternetPermission();
                    this.mActiveUids.put(proc.uid, uidRec);
                    ITranProcessList.Instance().hookUidChangedRunning(proc.uid);
                    EventLogTags.writeAmUidRunning(uidRec.getUid());
                    this.mService.noteUidProcessState(uidRec.getUid(), uidRec.getCurProcState(), uidRec.getCurCapability());
                }
                proc.setUidRecord(uidRec);
                uidRec.addProcess(proc);
                proc.setRenderThreadTid(0);
                this.mProcessNames.put(proc.processName, proc.uid, proc);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        if (proc.isolated) {
            this.mIsolatedProcesses.put(proc.uid, proc);
        }
    }

    private IsolatedUidRange getOrCreateIsolatedUidRangeLocked(ApplicationInfo info, HostingRecord hostingRecord) {
        if (hostingRecord == null || !hostingRecord.usesAppZygote()) {
            return this.mGlobalIsolatedUids;
        }
        return this.mAppIsolatedUidRangeAllocator.getOrCreateIsolatedUidRangeLocked(info.processName, hostingRecord.getDefiningUid());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Integer> getIsolatedProcessesLocked(int uid) {
        List<Integer> ret = null;
        int size = this.mIsolatedProcesses.size();
        for (int i = 0; i < size; i++) {
            ProcessRecord app = this.mIsolatedProcesses.valueAt(i);
            if (app.info.uid == uid) {
                if (ret == null) {
                    ret = new ArrayList<>();
                }
                ret.add(Integer.valueOf(app.getPid()));
            }
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord newProcessRecordLocked(ApplicationInfo info, String customProcess, boolean isolated, int isolatedUid, boolean isSdkSandbox, int sdkSandboxUid, String sdkSandboxClientAppPackage, HostingRecord hostingRecord) {
        int uid;
        int uid2;
        String proc = customProcess != null ? customProcess : info.processName;
        int userId = UserHandle.getUserId(info.uid);
        int uid3 = info.uid;
        if (isSdkSandbox) {
            uid3 = sdkSandboxUid;
        }
        if (isolated) {
            if (isolatedUid == 0) {
                IsolatedUidRange uidRange = getOrCreateIsolatedUidRangeLocked(info, hostingRecord);
                if (uidRange == null || (uid2 = uidRange.allocateIsolatedUidLocked(userId)) == -1) {
                    return null;
                }
            } else {
                uid2 = isolatedUid;
            }
            this.mAppExitInfoTracker.mIsolatedUidRecords.addIsolatedUid(uid2, info.uid);
            this.mService.getPackageManagerInternal().addIsolatedUid(uid2, info.uid);
            this.mService.mBatteryStatsService.addIsolatedUid(uid2, info.uid);
            FrameworkStatsLog.write(43, info.uid, uid2, 1);
            uid = uid2;
        } else {
            uid = uid3;
        }
        ProcessRecord r = new ProcessRecord(this.mService, info, proc, uid, sdkSandboxClientAppPackage, hostingRecord.getDefiningUid(), hostingRecord.getDefiningProcessName());
        ProcessStateRecord state = r.mState;
        if (!this.mService.mBooted && !this.mService.mBooting && userId == 0 && (info.flags & 9) == 9) {
            state.setCurrentSchedulingGroup(2);
            state.setSetSchedGroup(2);
            r.setPersistent(true);
            state.setMaxAdj(-800);
        }
        if (isolated && isolatedUid != 0) {
            state.setMaxAdj(-700);
        }
        TranAmHooker.improveAdjForGame(r);
        setMaxOomAdjForGriffin(r);
        TranAmHooker.improveAdjForAgares(r);
        if (TranAmHooker.ifNeedImproveAdj(r)) {
            TranAmHooker.improveAdjIfNeed(r);
        }
        addProcessNameLocked(r);
        return r;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord removeProcessNameLocked(String name, int uid) {
        return removeProcessNameLocked(name, uid, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord removeProcessNameLocked(String name, int uid, ProcessRecord expecting) {
        UidRecord uidRecord;
        ProcessRecord old = (ProcessRecord) this.mProcessNames.get(name, uid);
        ProcessRecord record = expecting != null ? expecting : old;
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                if (expecting != null && old != expecting) {
                    Slog.d(TAG, "it doesn't match and we don't want to destroy the new one");
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    return old;
                }
                this.mProcessNames.m1479remove(name, uid);
                if (record != null && (uidRecord = record.getUidRecord()) != null) {
                    uidRecord.removeProcess(record);
                    if (uidRecord.getNumOfProcs() == 0) {
                        if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                            Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "No more processes in " + uidRecord);
                        }
                        this.mService.enqueueUidChangeLocked(uidRecord, -1, -2147483647);
                        EventLogTags.writeAmUidStopped(uid);
                        this.mActiveUids.remove(uid);
                        this.mService.mFgsStartTempAllowList.removeUid(record.info.uid);
                        this.mService.noteUidProcessState(uid, 20, 0);
                        ITranProcessList.Instance().hookUidChangedStoped(uid);
                    }
                    record.setUidRecord(null);
                }
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                this.mIsolatedProcesses.remove(uid);
                this.mGlobalIsolatedUids.freeIsolatedUidLocked(uid);
                if (record != null && record.appZygote) {
                    removeProcessFromAppZygoteLocked(record);
                }
                this.mAppsInBackgroundRestricted.remove(record);
                return old;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateCoreSettingsLOSP(Bundle settings) {
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord processRecord = this.mLruProcesses.get(i);
            IApplicationThread thread = processRecord.getThread();
            if (thread != null) {
                try {
                    thread.setCoreSettings(settings);
                } catch (RemoteException e) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killAllBackgroundProcessesExceptLSP(int minTargetSdk, int maxProcState) {
        ArrayList<ProcessRecord> procs = new ArrayList<>();
        int NP = this.mProcessNames.getMap().size();
        for (int ip = 0; ip < NP; ip++) {
            SparseArray<ProcessRecord> apps = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
            int NA = apps.size();
            for (int ia = 0; ia < NA; ia++) {
                ProcessRecord app = apps.valueAt(ia);
                if (app.isRemoved() || ((minTargetSdk < 0 || app.info.targetSdkVersion < minTargetSdk) && (maxProcState < 0 || app.mState.getSetProcState() > maxProcState))) {
                    procs.add(app);
                }
            }
        }
        int N = procs.size();
        for (int i = 0; i < N; i++) {
            removeProcessLocked(procs.get(i), false, true, 13, 10, "kill all background except");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAllTimePrefsLOSP(int timePref) {
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord r = this.mLruProcesses.get(i);
            IApplicationThread thread = r.getThread();
            if (thread != null) {
                try {
                    thread.updateTimePrefs(timePref);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to update preferences for: " + r.info.processName);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllHttpProxy() {
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                    ProcessRecord r = this.mLruProcesses.get(i);
                    IApplicationThread thread = r.getThread();
                    if (r.getPid() != ActivityManagerService.MY_PID && thread != null && !r.isolated) {
                        try {
                            thread.updateHttpProxy();
                        } catch (RemoteException e) {
                            Slog.w(TAG, "Failed to update http proxy for: " + r.info.processName);
                        }
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        ActivityThread.updateHttpProxy(this.mService.mContext);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAllDnsCacheLOSP() {
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord r = this.mLruProcesses.get(i);
            IApplicationThread thread = r.getThread();
            if (thread != null) {
                try {
                    thread.clearDnsCache();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to clear dns cache for: " + r.info.processName);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleAllTrustStorageUpdateLOSP() {
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord r = this.mLruProcesses.get(i);
            IApplicationThread thread = r.getThread();
            if (thread != null) {
                try {
                    thread.handleTrustStorageUpdate();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to handle trust storage update for: " + r.info.processName);
                }
            }
        }
    }

    private int updateLruProcessInternalLSP(ProcessRecord app, long now, int index, int lruSeq, String what, Object obj, ProcessRecord srcApp) {
        app.setLastActivityTime(now);
        if (app.hasActivitiesOrRecentTasks()) {
            return index;
        }
        int lrui = this.mLruProcesses.lastIndexOf(app);
        if (lrui < 0) {
            Slog.wtf(TAG, "Adding dependent process " + app + " not on LRU list: " + what + " " + obj + " from " + srcApp);
            return index;
        } else if (lrui >= index) {
            return index;
        } else {
            int i = this.mLruProcessActivityStart;
            if (lrui >= i && index < i) {
                return index;
            }
            this.mLruProcesses.remove(lrui);
            if (index > 0) {
                index--;
            }
            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                Slog.d(ActivityManagerService.TAG_LRU, "Moving dep from " + lrui + " to " + index + " in LRU list: " + app);
            }
            this.mLruProcesses.add(index, app);
            app.setLruSeq(lruSeq);
            return index;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:85:0x0274, code lost:
        if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_LRU == false) goto L118;
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x0276, code lost:
        android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG_LRU, "Already found a different group: connGroup=" + r12 + " group=" + r9);
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x029a, code lost:
        if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_LRU == false) goto L118;
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x029c, code lost:
        android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG_LRU, "Already found a different activity: connUid=" + r11 + " uid=" + r3.info.uid);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void updateClientActivitiesOrderingLSP(ProcessRecord topApp, int topI, int bottomI, int endIndex) {
        int endIndex2;
        ProcessRecord nextEndProc;
        int nextConnectionGroup;
        ProcessServiceRecord topPsr;
        int topConnectionGroup;
        int endImportance;
        boolean moved;
        ProcessList processList = this;
        ProcessServiceRecord topPsr2 = topApp.mServices;
        if (!topApp.hasActivitiesOrRecentTasks() && !topPsr2.isTreatedLikeActivity()) {
            if (!topPsr2.hasClientActivities()) {
                return;
            }
            int uid = topApp.info.uid;
            int topConnectionGroup2 = topPsr2.getConnectionGroup();
            if (topConnectionGroup2 > 0) {
                int endImportance2 = topPsr2.getConnectionImportance();
                int i = endIndex;
                int endImportance3 = endImportance2;
                endIndex2 = endIndex;
                while (i >= bottomI) {
                    ProcessRecord subProc = processList.mLruProcesses.get(i);
                    ProcessServiceRecord subPsr = subProc.mServices;
                    int subConnectionGroup = subPsr.getConnectionGroup();
                    int subConnectionImportance = subPsr.getConnectionImportance();
                    if (subProc.info.uid != uid || subConnectionGroup != topConnectionGroup2) {
                        topPsr = topPsr2;
                        topConnectionGroup = topConnectionGroup2;
                        endImportance3 = endImportance3;
                    } else if (i != endIndex2 || subConnectionImportance < endImportance3) {
                        topPsr = topPsr2;
                        if (ActivityManagerDebugConfig.DEBUG_LRU) {
                            Slog.d(ActivityManagerService.TAG_LRU, "Pulling up " + subProc + " to position in group with importance=" + subConnectionImportance);
                        }
                        boolean moved2 = false;
                        int pos = topI;
                        while (true) {
                            boolean moved3 = moved2;
                            if (pos <= endIndex2) {
                                topConnectionGroup = topConnectionGroup2;
                                endImportance = endImportance3;
                                moved = moved3;
                                break;
                            }
                            topConnectionGroup = topConnectionGroup2;
                            ProcessRecord posProc = processList.mLruProcesses.get(pos);
                            endImportance = endImportance3;
                            if (subConnectionImportance > posProc.mServices.getConnectionImportance()) {
                                pos--;
                                moved2 = moved3;
                                topConnectionGroup2 = topConnectionGroup;
                                endImportance3 = endImportance;
                            } else {
                                processList.mLruProcesses.remove(i);
                                processList.mLruProcesses.add(pos, subProc);
                                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                    Slog.d(ActivityManagerService.TAG_LRU, "Moving " + subProc + " from position " + i + " to above " + posProc + " @ " + pos);
                                }
                                moved = true;
                                endIndex2--;
                            }
                        }
                        if (moved) {
                            endImportance3 = endImportance;
                        } else {
                            processList.mLruProcesses.remove(i);
                            processList.mLruProcesses.add(endIndex2, subProc);
                            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                Slog.d(ActivityManagerService.TAG_LRU, "Moving " + subProc + " from position " + i + " to end of group @ " + endIndex2);
                            }
                            endIndex2--;
                            endImportance3 = subConnectionImportance;
                        }
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_LRU) {
                            topPsr = topPsr2;
                            Slog.d(ActivityManagerService.TAG_LRU, "Keeping in-place above " + subProc + " endImportance=" + endImportance3 + " group=" + subConnectionGroup + " importance=" + subConnectionImportance);
                        } else {
                            topPsr = topPsr2;
                        }
                        endIndex2--;
                        endImportance3 = subConnectionImportance;
                        topConnectionGroup = topConnectionGroup2;
                    }
                    i--;
                    topPsr2 = topPsr;
                    topConnectionGroup2 = topConnectionGroup;
                }
            } else {
                endIndex2 = endIndex;
            }
            int i2 = endIndex2;
            while (i2 >= bottomI) {
                ProcessRecord subProc2 = processList.mLruProcesses.get(i2);
                ProcessServiceRecord subPsr2 = subProc2.mServices;
                int subConnectionGroup2 = subPsr2.getConnectionGroup();
                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                    Slog.d(ActivityManagerService.TAG_LRU, "Looking to spread old procs, at " + subProc2 + " @ " + i2);
                }
                if (subProc2.info.uid != uid) {
                    if (i2 < endIndex2) {
                        boolean hasActivity = false;
                        int connUid = 0;
                        int connGroup = 0;
                        while (true) {
                            if (i2 < bottomI) {
                                break;
                            }
                            processList.mLruProcesses.remove(i2);
                            processList.mLruProcesses.add(endIndex2, subProc2);
                            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                Slog.d(ActivityManagerService.TAG_LRU, "Different app, moving to " + endIndex2);
                            }
                            i2--;
                            if (i2 < bottomI) {
                                break;
                            }
                            subProc2 = processList.mLruProcesses.get(i2);
                            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                Slog.d(ActivityManagerService.TAG_LRU, "Looking at next app at " + i2 + ": " + subProc2);
                            }
                            if (subProc2.hasActivitiesOrRecentTasks() || subPsr2.isTreatedLikeActivity()) {
                                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                    Slog.d(ActivityManagerService.TAG_LRU, "This is hosting an activity!");
                                }
                                if (hasActivity) {
                                    if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                        Slog.d(ActivityManagerService.TAG_LRU, "Already found an activity, done");
                                    }
                                } else {
                                    hasActivity = true;
                                    endIndex2--;
                                }
                            } else {
                                if (subPsr2.hasClientActivities()) {
                                    if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                        Slog.d(ActivityManagerService.TAG_LRU, "This is a client of an activity");
                                    }
                                    if (hasActivity) {
                                        if (connUid == 0 || connUid != subProc2.info.uid) {
                                            break;
                                        } else if (connGroup == 0 || connGroup != subConnectionGroup2) {
                                            break;
                                        }
                                    } else {
                                        if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                            Slog.d(ActivityManagerService.TAG_LRU, "This is an activity client!  uid=" + subProc2.info.uid + " group=" + subConnectionGroup2);
                                        }
                                        hasActivity = true;
                                        connUid = subProc2.info.uid;
                                        connGroup = subConnectionGroup2;
                                    }
                                } else {
                                    continue;
                                }
                                endIndex2--;
                            }
                        }
                    }
                    while (true) {
                        endIndex2--;
                        if (endIndex2 < bottomI) {
                            break;
                        }
                        ProcessRecord endProc = processList.mLruProcesses.get(endIndex2);
                        if (endProc.info.uid == uid) {
                            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                                Slog.d(ActivityManagerService.TAG_LRU, "Found next group of app: " + endProc + " @ " + endIndex2);
                            }
                        }
                    }
                    if (endIndex2 >= bottomI) {
                        ProcessServiceRecord endPsr = processList.mLruProcesses.get(endIndex2).mServices;
                        int endConnectionGroup = endPsr.getConnectionGroup();
                        do {
                            endIndex2--;
                            if (endIndex2 < bottomI) {
                                break;
                            }
                            nextEndProc = processList.mLruProcesses.get(endIndex2);
                            nextConnectionGroup = nextEndProc.mServices.getConnectionGroup();
                            if (nextEndProc.info.uid != uid) {
                                break;
                            }
                        } while (nextConnectionGroup == endConnectionGroup);
                        if (ActivityManagerDebugConfig.DEBUG_LRU) {
                            Slog.d(ActivityManagerService.TAG_LRU, "Found next group or app: " + nextEndProc + " @ " + endIndex2 + " group=" + nextConnectionGroup);
                        }
                    }
                    if (ActivityManagerDebugConfig.DEBUG_LRU) {
                        Slog.d(ActivityManagerService.TAG_LRU, "Bumping scan position to " + endIndex2);
                    }
                    i2 = endIndex2;
                } else {
                    i2--;
                }
                processList = this;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLruProcessLocked(ProcessRecord app, boolean activityChange, ProcessRecord client) {
        ProcessServiceRecord psr = app.mServices;
        boolean hasActivity = app.hasActivitiesOrRecentTasks() || psr.hasClientActivities() || psr.isTreatedLikeActivity();
        if (!activityChange && hasActivity) {
            return;
        }
        if (app.getPid() == 0 && !app.isPendingStart()) {
            return;
        }
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                updateLruProcessLSP(app, client, hasActivity, false);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }

    private void updateLruProcessLSP(ProcessRecord app, ProcessRecord client, boolean hasActivity, boolean hasService) {
        int nextIndex;
        ProcessProviderRecord ppr;
        int j;
        int lrui;
        this.mLruSeq++;
        long now = SystemClock.uptimeMillis();
        ProcessServiceRecord psr = app.mServices;
        app.setLastActivityTime(now);
        if (hasActivity) {
            int N = this.mLruProcesses.size();
            if (N > 0 && this.mLruProcesses.get(N - 1) == app) {
                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                    Slog.d(ActivityManagerService.TAG_LRU, "Not moving, already top activity: " + app);
                    return;
                }
                return;
            }
        } else {
            int i = this.mLruProcessServiceStart;
            if (i > 0 && this.mLruProcesses.get(i - 1) == app) {
                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                    Slog.d(ActivityManagerService.TAG_LRU, "Not moving, already top other: " + app);
                    return;
                }
                return;
            }
        }
        int lrui2 = this.mLruProcesses.lastIndexOf(app);
        if (app.isPersistent() && lrui2 >= 0) {
            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                Slog.d(ActivityManagerService.TAG_LRU, "Not moving, persistent: " + app);
                return;
            }
            return;
        }
        if (lrui2 >= 0) {
            int i2 = this.mLruProcessActivityStart;
            if (lrui2 < i2) {
                this.mLruProcessActivityStart = i2 - 1;
            }
            int i3 = this.mLruProcessServiceStart;
            if (lrui2 < i3) {
                this.mLruProcessServiceStart = i3 - 1;
            }
            this.mLruProcesses.remove(lrui2);
        }
        int nextActivityIndex = -1;
        if (hasActivity) {
            int N2 = this.mLruProcesses.size();
            nextIndex = this.mLruProcessServiceStart;
            if (!app.hasActivitiesOrRecentTasks() && !psr.isTreatedLikeActivity() && this.mLruProcessActivityStart < N2 - 1) {
                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                    Slog.d(ActivityManagerService.TAG_LRU, "Adding to second-top of LRU activity list: " + app + " group=" + psr.getConnectionGroup() + " importance=" + psr.getConnectionImportance());
                }
                int pos = N2 - 1;
                while (pos > this.mLruProcessActivityStart) {
                    ProcessRecord posproc = this.mLruProcesses.get(pos);
                    if (posproc.info.uid == app.info.uid) {
                        break;
                    }
                    pos--;
                }
                this.mLruProcesses.add(pos, app);
                int endIndex = pos - 1;
                int i4 = this.mLruProcessActivityStart;
                if (endIndex < i4) {
                    endIndex = this.mLruProcessActivityStart;
                }
                nextActivityIndex = endIndex;
                updateClientActivitiesOrderingLSP(app, pos, i4, endIndex);
            } else {
                if (ActivityManagerDebugConfig.DEBUG_LRU) {
                    Slog.d(ActivityManagerService.TAG_LRU, "Adding to top of LRU activity list: " + app);
                }
                this.mLruProcesses.add(app);
                nextActivityIndex = this.mLruProcesses.size() - 1;
            }
        } else if (hasService) {
            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                Slog.d(ActivityManagerService.TAG_LRU, "Adding to top of LRU service list: " + app);
            }
            this.mLruProcesses.add(this.mLruProcessActivityStart, app);
            nextIndex = this.mLruProcessServiceStart;
            this.mLruProcessActivityStart++;
        } else {
            int index = this.mLruProcessServiceStart;
            if (client != null) {
                int clientIndex = this.mLruProcesses.lastIndexOf(client);
                if (ActivityManagerDebugConfig.DEBUG_LRU && clientIndex < 0) {
                    Slog.d(ActivityManagerService.TAG_LRU, "Unknown client " + client + " when updating " + app);
                }
                if (clientIndex <= lrui2) {
                    clientIndex = lrui2;
                }
                if (clientIndex >= 0 && index > clientIndex) {
                    index = clientIndex;
                }
            }
            if (ActivityManagerDebugConfig.DEBUG_LRU) {
                Slog.d(ActivityManagerService.TAG_LRU, "Adding at " + index + " of LRU list: " + app);
            }
            this.mLruProcesses.add(index, app);
            nextIndex = index - 1;
            this.mLruProcessActivityStart++;
            int i5 = this.mLruProcessServiceStart + 1;
            this.mLruProcessServiceStart = i5;
            if (index > 1) {
                updateClientActivitiesOrderingLSP(app, i5 - 1, 0, index - 1);
            }
        }
        app.setLruSeq(this.mLruSeq);
        int nextActivityIndex2 = nextActivityIndex;
        int j2 = psr.numberOfConnections() - 1;
        int nextIndex2 = nextIndex;
        while (j2 >= 0) {
            ConnectionRecord cr = psr.getConnectionAt(j2);
            if (cr.binding == null || cr.serviceDead || cr.binding.service == null || cr.binding.service.app == null) {
                j = j2;
                lrui = lrui2;
            } else if (cr.binding.service.app.getLruSeq() == this.mLruSeq || (cr.flags & 1073742128) != 0) {
                j = j2;
                lrui = lrui2;
            } else if (cr.binding.service.app.isPersistent()) {
                j = j2;
                lrui = lrui2;
            } else if (cr.binding.service.app.mServices.hasClientActivities()) {
                if (nextActivityIndex2 >= 0) {
                    j = j2;
                    lrui = lrui2;
                    nextActivityIndex2 = updateLruProcessInternalLSP(cr.binding.service.app, now, nextActivityIndex2, this.mLruSeq, "service connection", cr, app);
                } else {
                    j = j2;
                    lrui = lrui2;
                }
            } else {
                j = j2;
                lrui = lrui2;
                nextIndex2 = updateLruProcessInternalLSP(cr.binding.service.app, now, nextIndex2, this.mLruSeq, "service connection", cr, app);
            }
            j2 = j - 1;
            lrui2 = lrui;
        }
        ProcessProviderRecord ppr2 = app.mProviders;
        int j3 = ppr2.numberOfProviderConnections() - 1;
        while (j3 >= 0) {
            ContentProviderRecord cpr = ppr2.getProviderConnectionAt(j3).provider;
            if (cpr.proc == null || cpr.proc.getLruSeq() == this.mLruSeq || cpr.proc.isPersistent()) {
                ppr = ppr2;
            } else {
                ppr = ppr2;
                nextIndex2 = updateLruProcessInternalLSP(cpr.proc, now, nextIndex2, this.mLruSeq, "provider reference", cpr, app);
            }
            j3--;
            ppr2 = ppr;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord getLRURecordForAppLOSP(IApplicationThread thread) {
        if (thread == null) {
            return null;
        }
        IBinder threadBinder = thread.asBinder();
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord rec = this.mLruProcesses.get(i);
            IApplicationThread t = rec.getThread();
            if (t != null && t.asBinder() == threadBinder) {
                return rec;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean haveBackgroundProcessLOSP() {
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord rec = this.mLruProcesses.get(i);
            if (rec.getThread() != null && rec.mState.getSetProcState() >= 16) {
                return true;
            }
        }
        return false;
    }

    private static int procStateToImportance(int procState, int memAdj, ActivityManager.RunningAppProcessInfo currApp, int clientTargetSdk) {
        int imp = ActivityManager.RunningAppProcessInfo.procStateToImportanceForTargetSdk(procState, clientTargetSdk);
        if (imp == 400) {
            currApp.lru = memAdj;
        } else {
            currApp.lru = 0;
        }
        return imp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void fillInProcMemInfoLOSP(ProcessRecord app, ActivityManager.RunningAppProcessInfo outInfo, int clientTargetSdk) {
        outInfo.pid = app.getPid();
        outInfo.uid = app.info.uid;
        if (app.getWindowProcessController().isHeavyWeightProcess()) {
            outInfo.flags |= 1;
        }
        if (app.isPersistent()) {
            outInfo.flags |= 2;
        }
        if (app.hasActivities()) {
            outInfo.flags |= 4;
        }
        outInfo.lastTrimLevel = app.mProfile.getTrimMemoryLevel();
        ProcessStateRecord state = app.mState;
        int adj = state.getCurAdj();
        int procState = state.getCurProcState();
        outInfo.importance = procStateToImportance(procState, adj, outInfo, clientTargetSdk);
        outInfo.importanceReasonCode = state.getAdjTypeCode();
        outInfo.processState = procState;
        outInfo.isFocused = app == this.mService.getTopApp();
        outInfo.lastActivityTime = app.getLastActivityTime();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcessesLOSP(boolean allUsers, int userId, boolean allUids, int callingUid, int clientTargetSdk) {
        List<ActivityManager.RunningAppProcessInfo> runList = null;
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord app = this.mLruProcesses.get(i);
            ProcessStateRecord state = app.mState;
            ProcessErrorStateRecord errState = app.mErrorState;
            if ((allUsers || app.userId == userId) && ((allUids || app.uid == callingUid) && app.getThread() != null && !errState.isCrashing() && !errState.isNotResponding())) {
                ActivityManager.RunningAppProcessInfo currApp = new ActivityManager.RunningAppProcessInfo(app.processName, app.getPid(), app.getPackageList());
                fillInProcMemInfoLOSP(app, currApp, clientTargetSdk);
                if (state.getAdjSource() instanceof ProcessRecord) {
                    currApp.importanceReasonPid = ((ProcessRecord) state.getAdjSource()).getPid();
                    currApp.importanceReasonImportance = ActivityManager.RunningAppProcessInfo.procStateToImportance(state.getAdjSourceProcState());
                } else if (state.getAdjSource() instanceof ActivityServiceConnectionsHolder) {
                    ActivityServiceConnectionsHolder r = (ActivityServiceConnectionsHolder) state.getAdjSource();
                    int pid = r.getActivityPid();
                    if (pid != -1) {
                        currApp.importanceReasonPid = pid;
                    }
                }
                if (state.getAdjTarget() instanceof ComponentName) {
                    currApp.importanceReasonComponent = (ComponentName) state.getAdjTarget();
                }
                if (runList == null) {
                    runList = new ArrayList<>();
                }
                runList.add(currApp);
            }
        }
        return runList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLruSizeLOSP() {
        return this.mLruProcesses.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<ProcessRecord> getLruProcessesLOSP() {
        return this.mLruProcesses;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<ProcessRecord> getLruProcessesLSP() {
        return this.mLruProcesses;
    }

    void setLruProcessServiceStartLSP(int pos) {
        this.mLruProcessServiceStart = pos;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLruProcessServiceStartLOSP() {
        return this.mLruProcessServiceStart;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachLruProcessesLOSP(boolean iterateForward, Consumer<ProcessRecord> callback) {
        if (iterateForward) {
            int size = this.mLruProcesses.size();
            for (int i = 0; i < size; i++) {
                callback.accept(this.mLruProcesses.get(i));
            }
            return;
        }
        for (int i2 = this.mLruProcesses.size() - 1; i2 >= 0; i2--) {
            callback.accept(this.mLruProcesses.get(i2));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public <R> R searchEachLruProcessesLOSP(boolean iterateForward, Function<ProcessRecord, R> callback) {
        if (iterateForward) {
            int size = this.mLruProcesses.size();
            for (int i = 0; i < size; i++) {
                R r = callback.apply(this.mLruProcesses.get(i));
                if (r != null) {
                    return r;
                }
            }
            return null;
        }
        for (int i2 = this.mLruProcesses.size() - 1; i2 >= 0; i2--) {
            R r2 = callback.apply(this.mLruProcesses.get(i2));
            if (r2 != null) {
                return r2;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInLruListLOSP(ProcessRecord app) {
        return this.mLruProcesses.contains(app);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLruSeqLOSP() {
        return this.mLruSeq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MyProcessMap getProcessNamesLOSP() {
        return this.mProcessNames;
    }

    void dumpLruListHeaderLocked(PrintWriter pw) {
        pw.print("  Process LRU list (sorted by oom_adj, ");
        pw.print(this.mLruProcesses.size());
        pw.print(" total, non-act at ");
        pw.print(this.mLruProcesses.size() - this.mLruProcessActivityStart);
        pw.print(", non-svc at ");
        pw.print(this.mLruProcesses.size() - this.mLruProcessServiceStart);
        pw.println("):");
    }

    private void dumpLruEntryLocked(PrintWriter pw, int index, ProcessRecord proc, String prefix) {
        pw.print(prefix);
        pw.print('#');
        if (index < 10) {
            pw.print(' ');
        }
        pw.print(index);
        pw.print(": ");
        pw.print(makeOomAdjString(proc.mState.getSetAdj(), false));
        pw.print(' ');
        pw.print(makeProcStateString(proc.mState.getCurProcState()));
        pw.print(' ');
        ActivityManager.printCapabilitiesSummary(pw, proc.mState.getCurCapability());
        pw.print(' ');
        pw.print(proc.toShortString());
        ProcessServiceRecord psr = proc.mServices;
        if (proc.hasActivitiesOrRecentTasks() || psr.hasClientActivities() || psr.isTreatedLikeActivity()) {
            pw.print(" act:");
            boolean printed = false;
            if (proc.hasActivities()) {
                pw.print(ActivityTaskManagerService.DUMP_ACTIVITIES_CMD);
                printed = true;
            }
            if (proc.hasRecentTasks()) {
                if (printed) {
                    pw.print("|");
                }
                pw.print(ActivityTaskManagerService.DUMP_RECENTS_CMD);
                printed = true;
            }
            if (psr.hasClientActivities()) {
                if (printed) {
                    pw.print("|");
                }
                pw.print("client");
                printed = true;
            }
            if (psr.isTreatedLikeActivity()) {
                if (printed) {
                    pw.print("|");
                }
                pw.print("treated");
            }
        }
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpLruLocked(PrintWriter pw, String dumpPackage, String prefix) {
        String innerPrefix;
        int lruSize = this.mLruProcesses.size();
        if (prefix == null) {
            pw.println("ACTIVITY MANAGER LRU PROCESSES (dumpsys activity lru)");
            innerPrefix = "  ";
        } else {
            boolean haveAny = false;
            for (int i = lruSize - 1; i >= 0; i--) {
                ProcessRecord r = this.mLruProcesses.get(i);
                if (dumpPackage == null || r.getPkgList().containsKey(dumpPackage)) {
                    haveAny = true;
                    break;
                }
            }
            if (!haveAny) {
                return false;
            }
            pw.print(prefix);
            pw.println("Raw LRU list (dumpsys activity lru):");
            innerPrefix = prefix + "  ";
        }
        boolean first = true;
        int i2 = lruSize - 1;
        while (i2 >= this.mLruProcessActivityStart) {
            ProcessRecord r2 = this.mLruProcesses.get(i2);
            if (dumpPackage == null || r2.getPkgList().containsKey(dumpPackage)) {
                if (first) {
                    pw.print(innerPrefix);
                    pw.println("Activities:");
                    first = false;
                }
                dumpLruEntryLocked(pw, i2, r2, innerPrefix);
            }
            i2--;
        }
        boolean first2 = true;
        while (i2 >= this.mLruProcessServiceStart) {
            ProcessRecord r3 = this.mLruProcesses.get(i2);
            if (dumpPackage == null || r3.getPkgList().containsKey(dumpPackage)) {
                if (first2) {
                    pw.print(innerPrefix);
                    pw.println("Services:");
                    first2 = false;
                }
                dumpLruEntryLocked(pw, i2, r3, innerPrefix);
            }
            i2--;
        }
        boolean first3 = true;
        while (i2 >= 0) {
            ProcessRecord r4 = this.mLruProcesses.get(i2);
            if (dumpPackage == null || r4.getPkgList().containsKey(dumpPackage)) {
                if (first3) {
                    pw.print(innerPrefix);
                    pw.println("Other:");
                    first3 = false;
                }
                dumpLruEntryLocked(pw, i2, r4, innerPrefix);
            }
            i2--;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpProcessesLSP(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage, int dumpAppId) {
        int numPers;
        boolean needSep = false;
        int numPers2 = 0;
        pw.println("ACTIVITY MANAGER RUNNING PROCESSES (dumpsys activity processes)");
        if (!dumpAll && dumpPackage == null) {
            numPers = 0;
        } else {
            int numOfNames = this.mProcessNames.getMap().size();
            for (int ip = 0; ip < numOfNames; ip++) {
                SparseArray<ProcessRecord> procs = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
                int size = procs.size();
                for (int ia = 0; ia < size; ia++) {
                    ProcessRecord r = procs.valueAt(ia);
                    if (dumpPackage == null || r.getPkgList().containsKey(dumpPackage)) {
                        if (!needSep) {
                            pw.println("  All known processes:");
                            needSep = true;
                        }
                        pw.print(r.isPersistent() ? "  *PERS*" : "  *APP*");
                        pw.print(" UID ");
                        pw.print(procs.keyAt(ia));
                        pw.print(" ");
                        pw.println(r);
                        r.dump(pw, "    ");
                        if (r.isPersistent()) {
                            numPers2++;
                        }
                    }
                }
            }
            numPers = numPers2;
        }
        if (this.mIsolatedProcesses.size() > 0) {
            boolean printed = false;
            int size2 = this.mIsolatedProcesses.size();
            for (int i = 0; i < size2; i++) {
                ProcessRecord r2 = this.mIsolatedProcesses.valueAt(i);
                if (dumpPackage == null || r2.getPkgList().containsKey(dumpPackage)) {
                    if (!printed) {
                        if (needSep) {
                            pw.println();
                        }
                        pw.println("  Isolated process list (sorted by uid):");
                        printed = true;
                        needSep = true;
                    }
                    pw.print("    Isolated #");
                    pw.print(i);
                    pw.print(": ");
                    pw.println(r2);
                }
            }
        }
        boolean needSep2 = this.mService.dumpActiveInstruments(pw, dumpPackage, needSep);
        if (dumpOomLocked(fd, pw, needSep2, args, opti, dumpAll, dumpPackage, false)) {
            needSep2 = true;
        }
        if (this.mActiveUids.size() > 0) {
            needSep2 |= this.mActiveUids.dump(pw, dumpPackage, dumpAppId, "UID states:", needSep2);
        }
        if (dumpAll) {
            needSep2 |= this.mService.mUidObserverController.dumpValidateUids(pw, dumpPackage, dumpAppId, "UID validation:", needSep2);
        }
        if (needSep2) {
            pw.println();
        }
        if (dumpLruLocked(pw, dumpPackage, "  ")) {
            needSep2 = true;
        }
        if (getLruSizeLOSP() > 0) {
            if (needSep2) {
                pw.println();
            }
            dumpLruListHeaderLocked(pw);
            dumpProcessOomList(pw, this.mService, this.mLruProcesses, "    ", "Proc", "PERS", false, dumpPackage);
            needSep2 = true;
        }
        ITranProcessList.Instance().dumpMuteList(this.mMuteList, pw);
        this.mService.dumpOtherProcessesInfoLSP(fd, pw, dumpAll, dumpPackage, dumpAppId, numPers, needSep2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeProcessesToProtoLSP(ProtoOutputStream proto, String dumpPackage) {
        int numOfNames = this.mProcessNames.getMap().size();
        int numPers = 0;
        for (int ip = 0; ip < numOfNames; ip++) {
            SparseArray<ProcessRecord> procs = (SparseArray) this.mProcessNames.getMap().valueAt(ip);
            int size = procs.size();
            for (int ia = 0; ia < size; ia++) {
                ProcessRecord r = procs.valueAt(ia);
                if (dumpPackage == null || r.getPkgList().containsKey(dumpPackage)) {
                    r.dumpDebug(proto, CompanionAppsPermissions.APP_PERMISSIONS, this.mLruProcesses.indexOf(r));
                    if (r.isPersistent()) {
                        numPers++;
                    }
                }
            }
        }
        int size2 = this.mIsolatedProcesses.size();
        for (int i = 0; i < size2; i++) {
            ProcessRecord r2 = this.mIsolatedProcesses.valueAt(i);
            if (dumpPackage == null || r2.getPkgList().containsKey(dumpPackage)) {
                r2.dumpDebug(proto, 2246267895810L, this.mLruProcesses.indexOf(r2));
            }
        }
        int dumpAppId = this.mService.getAppId(dumpPackage);
        this.mActiveUids.dumpProto(proto, dumpPackage, dumpAppId, 2246267895812L);
        if (getLruSizeLOSP() > 0) {
            long lruToken = proto.start(1146756268038L);
            int total = getLruSizeLOSP();
            proto.write(CompanionMessage.MESSAGE_ID, total);
            proto.write(1120986464258L, total - this.mLruProcessActivityStart);
            proto.write(1120986464259L, total - this.mLruProcessServiceStart);
            writeProcessOomListToProto(proto, 2246267895812L, this.mService, this.mLruProcesses, false, dumpPackage);
            proto.end(lruToken);
        }
        this.mService.writeOtherProcessesInfoToProtoLSP(proto, dumpPackage, dumpAppId, numPers);
    }

    private static ArrayList<Pair<ProcessRecord, Integer>> sortProcessOomList(List<ProcessRecord> origList, String dumpPackage) {
        ArrayList<Pair<ProcessRecord, Integer>> list = new ArrayList<>(origList.size());
        int size = origList.size();
        for (int i = 0; i < size; i++) {
            ProcessRecord r = origList.get(i);
            if (dumpPackage == null || r.getPkgList().containsKey(dumpPackage)) {
                list.add(new Pair<>(origList.get(i), Integer.valueOf(i)));
            }
        }
        Comparator<Pair<ProcessRecord, Integer>> comparator = new Comparator<Pair<ProcessRecord, Integer>>() { // from class: com.android.server.am.ProcessList.2
            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.Comparator
            public int compare(Pair<ProcessRecord, Integer> object1, Pair<ProcessRecord, Integer> object2) {
                int adj = ((ProcessRecord) object2.first).mState.getSetAdj() - ((ProcessRecord) object1.first).mState.getSetAdj();
                if (adj != 0) {
                    return adj;
                }
                int procState = ((ProcessRecord) object2.first).mState.getSetProcState() - ((ProcessRecord) object1.first).mState.getSetProcState();
                if (procState != 0) {
                    return procState;
                }
                int val = ((Integer) object2.second).intValue() - ((Integer) object1.second).intValue();
                if (val != 0) {
                    return val;
                }
                return 0;
            }
        };
        Collections.sort(list, comparator);
        return list;
    }

    private static boolean writeProcessOomListToProto(ProtoOutputStream proto, long fieldId, ActivityManagerService service, List<ProcessRecord> origList, boolean inclDetails, String dumpPackage) {
        ArrayList<Pair<ProcessRecord, Integer>> list;
        long curUptime;
        ArrayList<Pair<ProcessRecord, Integer>> list2 = sortProcessOomList(origList, dumpPackage);
        if (list2.isEmpty()) {
            return false;
        }
        long curUptime2 = SystemClock.uptimeMillis();
        boolean z = true;
        int i = list2.size() - 1;
        while (i >= 0) {
            ProcessRecord r = (ProcessRecord) list2.get(i).first;
            ProcessStateRecord state = r.mState;
            ProcessServiceRecord psr = r.mServices;
            long token = proto.start(fieldId);
            String oomAdj = makeOomAdjString(state.getSetAdj(), z);
            proto.write(1133871366145L, r.isPersistent());
            proto.write(1120986464258L, (origList.size() - 1) - ((Integer) list2.get(i).second).intValue());
            proto.write(1138166333443L, oomAdj);
            int schedGroup = -1;
            switch (state.getSetSchedGroup()) {
                case 0:
                    schedGroup = 0;
                    break;
                case 2:
                    schedGroup = 1;
                    break;
                case 3:
                    schedGroup = 2;
                    break;
                case 4:
                    schedGroup = 3;
                    break;
            }
            if (schedGroup != -1) {
                proto.write(1159641169924L, schedGroup);
            }
            if (state.hasForegroundActivities()) {
                proto.write(1133871366149L, true);
            } else if (psr.hasForegroundServices()) {
                proto.write(1133871366150L, true);
            }
            proto.write(1159641169927L, makeProcStateProtoEnum(state.getCurProcState()));
            proto.write(1120986464264L, r.mProfile.getTrimMemoryLevel());
            r.dumpDebug(proto, 1146756268041L);
            proto.write(1138166333450L, state.getAdjType());
            if (state.getAdjSource() != null || state.getAdjTarget() != null) {
                if (state.getAdjTarget() instanceof ComponentName) {
                    ComponentName cn = (ComponentName) state.getAdjTarget();
                    cn.dumpDebug(proto, 1146756268043L);
                } else if (state.getAdjTarget() != null) {
                    proto.write(1138166333452L, state.getAdjTarget().toString());
                }
                if (state.getAdjSource() instanceof ProcessRecord) {
                    ProcessRecord p = (ProcessRecord) state.getAdjSource();
                    p.dumpDebug(proto, 1146756268045L);
                } else if (state.getAdjSource() != null) {
                    proto.write(1138166333454L, state.getAdjSource().toString());
                }
            }
            if (inclDetails) {
                long detailToken = proto.start(1146756268047L);
                list = list2;
                proto.write(CompanionMessage.MESSAGE_ID, state.getMaxAdj());
                proto.write(1120986464258L, state.getCurRawAdj());
                proto.write(1120986464259L, state.getSetRawAdj());
                proto.write(1120986464260L, state.getCurAdj());
                proto.write(1120986464261L, state.getSetAdj());
                proto.write(1159641169927L, makeProcStateProtoEnum(state.getCurProcState()));
                proto.write(1159641169928L, makeProcStateProtoEnum(state.getSetProcState()));
                proto.write(1138166333449L, DebugUtils.sizeValueToString(r.mProfile.getLastPss() * GadgetFunction.NCM, new StringBuilder()));
                proto.write(1138166333450L, DebugUtils.sizeValueToString(r.mProfile.getLastSwapPss() * GadgetFunction.NCM, new StringBuilder()));
                proto.write(1138166333451L, DebugUtils.sizeValueToString(r.mProfile.getLastCachedPss() * GadgetFunction.NCM, new StringBuilder()));
                proto.write(1133871366156L, state.isCached());
                proto.write(1133871366157L, state.isEmpty());
                proto.write(1133871366158L, psr.hasAboveClient());
                if (state.getSetProcState() < 10) {
                    curUptime = curUptime2;
                } else {
                    long lastCpuTime = r.mProfile.mLastCpuTime.get();
                    long uptimeSince = curUptime2 - service.mLastPowerCheckUptime;
                    if (lastCpuTime == 0 || uptimeSince <= 0) {
                        curUptime = curUptime2;
                    } else {
                        curUptime = curUptime2;
                        long timeUsed = r.mProfile.mCurCpuTime.get() - lastCpuTime;
                        long cpuTimeToken = proto.start(1146756268047L);
                        proto.write(1112396529665L, uptimeSince);
                        proto.write(1112396529666L, timeUsed);
                        proto.write(1108101562371L, (timeUsed * 100.0d) / uptimeSince);
                        proto.end(cpuTimeToken);
                    }
                }
                proto.end(detailToken);
            } else {
                list = list2;
                curUptime = curUptime2;
            }
            proto.end(token);
            i--;
            curUptime2 = curUptime;
            list2 = list;
            z = true;
        }
        return true;
    }

    private static boolean dumpProcessOomList(PrintWriter pw, ActivityManagerService service, List<ProcessRecord> origList, String prefix, String normalLabel, String persistentLabel, boolean inclDetails, String dumpPackage) {
        char schedGroup;
        char foreground;
        String str = prefix;
        ArrayList<Pair<ProcessRecord, Integer>> list = sortProcessOomList(origList, dumpPackage);
        boolean z = false;
        if (list.isEmpty()) {
            return false;
        }
        long curUptime = SystemClock.uptimeMillis();
        long uptimeSince = curUptime - service.mLastPowerCheckUptime;
        int i = list.size() - 1;
        while (i >= 0) {
            ProcessRecord r = (ProcessRecord) list.get(i).first;
            ProcessStateRecord state = r.mState;
            ProcessServiceRecord psr = r.mServices;
            String oomAdj = makeOomAdjString(state.getSetAdj(), z);
            switch (state.getSetSchedGroup()) {
                case 0:
                    schedGroup = 'b';
                    break;
                case 1:
                    schedGroup = 'R';
                    break;
                case 2:
                    schedGroup = 'F';
                    break;
                case 3:
                    schedGroup = 'T';
                    break;
                case 4:
                    schedGroup = 'B';
                    break;
                default:
                    schedGroup = '?';
                    break;
            }
            if (state.hasForegroundActivities()) {
                foreground = 'A';
            } else if (psr.hasForegroundServices()) {
                foreground = 'S';
            } else {
                foreground = ' ';
            }
            String procState = makeProcStateString(state.getCurProcState());
            pw.print(str);
            pw.print(r.isPersistent() ? persistentLabel : normalLabel);
            pw.print(" #");
            ArrayList<Pair<ProcessRecord, Integer>> list2 = list;
            int num = (origList.size() - 1) - ((Integer) list.get(i).second).intValue();
            long curUptime2 = curUptime;
            if (num < 10) {
                pw.print(' ');
            }
            pw.print(num);
            pw.print(": ");
            pw.print(oomAdj);
            pw.print(' ');
            pw.print(schedGroup);
            pw.print('/');
            pw.print(foreground);
            pw.print('/');
            pw.print(procState);
            pw.print(' ');
            ActivityManager.printCapabilitiesSummary(pw, state.getCurCapability());
            pw.print(' ');
            pw.print(" t:");
            if (r.mProfile.getTrimMemoryLevel() < 10) {
                pw.print(' ');
            }
            pw.print(r.mProfile.getTrimMemoryLevel());
            pw.print(' ');
            pw.print(r.toShortString());
            pw.print(" (");
            pw.print(state.getAdjType());
            pw.println(')');
            if (state.getAdjSource() != null || state.getAdjTarget() != null) {
                pw.print(str);
                pw.print("    ");
                if (state.getAdjTarget() instanceof ComponentName) {
                    pw.print(((ComponentName) state.getAdjTarget()).flattenToShortString());
                } else if (state.getAdjTarget() != null) {
                    pw.print(state.getAdjTarget().toString());
                } else {
                    pw.print("{null}");
                }
                pw.print("<=");
                if (state.getAdjSource() instanceof ProcessRecord) {
                    pw.print("Proc{");
                    pw.print(((ProcessRecord) state.getAdjSource()).toShortString());
                    pw.println("}");
                } else if (state.getAdjSource() != null) {
                    pw.println(state.getAdjSource().toString());
                } else {
                    pw.println("{null}");
                }
            }
            if (inclDetails) {
                pw.print(str);
                pw.print("    ");
                pw.print("oom: max=");
                pw.print(state.getMaxAdj());
                pw.print(" curRaw=");
                pw.print(state.getCurRawAdj());
                pw.print(" setRaw=");
                pw.print(state.getSetRawAdj());
                pw.print(" cur=");
                pw.print(state.getCurAdj());
                pw.print(" set=");
                pw.println(state.getSetAdj());
                pw.print(str);
                pw.print("    ");
                pw.print("state: cur=");
                pw.print(makeProcStateString(state.getCurProcState()));
                pw.print(" set=");
                pw.print(makeProcStateString(state.getSetProcState()));
                pw.print(" lastPss=");
                DebugUtils.printSizeValue(pw, r.mProfile.getLastPss() * GadgetFunction.NCM);
                pw.print(" lastSwapPss=");
                DebugUtils.printSizeValue(pw, r.mProfile.getLastSwapPss() * GadgetFunction.NCM);
                pw.print(" lastCachedPss=");
                DebugUtils.printSizeValue(pw, r.mProfile.getLastCachedPss() * GadgetFunction.NCM);
                pw.println();
                pw.print(str);
                pw.print("    ");
                pw.print("cached=");
                pw.print(state.isCached());
                pw.print(" empty=");
                pw.print(state.isEmpty());
                pw.print(" hasAboveClient=");
                pw.println(psr.hasAboveClient());
                if (state.getSetProcState() >= 10) {
                    long lastCpuTime = r.mProfile.mLastCpuTime.get();
                    if (lastCpuTime != 0 && uptimeSince > 0) {
                        long timeUsed = r.mProfile.mCurCpuTime.get() - lastCpuTime;
                        pw.print(str);
                        pw.print("    ");
                        pw.print("run cpu over ");
                        TimeUtils.formatDuration(uptimeSince, pw);
                        pw.print(" used ");
                        TimeUtils.formatDuration(timeUsed, pw);
                        pw.print(" (");
                        pw.print((100 * timeUsed) / uptimeSince);
                        pw.println("%)");
                    }
                }
            }
            i--;
            str = prefix;
            list = list2;
            curUptime = curUptime2;
            z = false;
        }
        return true;
    }

    private void printOomLevel(PrintWriter pw, String name, int adj) {
        pw.print("    ");
        if (adj >= 0) {
            pw.print(' ');
            if (adj < 10) {
                pw.print(' ');
            }
        } else if (adj > -10) {
            pw.print(' ');
        }
        pw.print(adj);
        pw.print(": ");
        pw.print(name);
        pw.print(" (");
        pw.print(ActivityManagerService.stringifySize(getMemLevel(adj), 1024));
        pw.println(")");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpOomLocked(FileDescriptor fd, PrintWriter pw, boolean needSep, String[] args, int opti, boolean dumpAll, String dumpPackage, boolean inclGc) {
        boolean needSep2;
        if (getLruSizeLOSP() <= 0) {
            needSep2 = needSep;
        } else {
            if (needSep) {
                pw.println();
            }
            pw.println("  OOM levels:");
            printOomLevel(pw, "SYSTEM_ADJ", -900);
            printOomLevel(pw, "PERSISTENT_PROC_ADJ", -800);
            printOomLevel(pw, "PERSISTENT_SERVICE_ADJ", -700);
            printOomLevel(pw, "FOREGROUND_APP_ADJ", 0);
            printOomLevel(pw, "VISIBLE_APP_ADJ", 100);
            printOomLevel(pw, "PERCEPTIBLE_APP_ADJ", 200);
            printOomLevel(pw, "PERCEPTIBLE_MEDIUM_APP_ADJ", PERCEPTIBLE_MEDIUM_APP_ADJ);
            printOomLevel(pw, "PERCEPTIBLE_LOW_APP_ADJ", 250);
            printOomLevel(pw, "BACKUP_APP_ADJ", 300);
            printOomLevel(pw, "HEAVY_WEIGHT_APP_ADJ", 400);
            printOomLevel(pw, "SERVICE_ADJ", 500);
            printOomLevel(pw, "HOME_APP_ADJ", 600);
            printOomLevel(pw, "PREVIOUS_APP_ADJ", 700);
            printOomLevel(pw, "SERVICE_B_ADJ", 800);
            printOomLevel(pw, "CACHED_APP_MIN_ADJ", 900);
            printOomLevel(pw, "CACHED_APP_MAX_ADJ", 999);
            if (1 != 0) {
                pw.println();
            }
            pw.print("  Process OOM control (");
            pw.print(getLruSizeLOSP());
            pw.print(" total, non-act at ");
            pw.print(getLruSizeLOSP() - this.mLruProcessActivityStart);
            pw.print(", non-svc at ");
            pw.print(getLruSizeLOSP() - this.mLruProcessServiceStart);
            pw.println("):");
            dumpProcessOomList(pw, this.mService, this.mLruProcesses, "    ", "Proc", "PERS", true, dumpPackage);
            needSep2 = true;
        }
        synchronized (this.mService.mAppProfiler.mProfilerLock) {
            try {
                try {
                    this.mService.mAppProfiler.dumpProcessesToGc(pw, needSep2, dumpPackage);
                    pw.println();
                    this.mService.mAtmInternal.dumpForOom(pw);
                    return true;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerProcessObserver(IProcessObserver observer) {
        this.mProcessObservers.register(observer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterProcessObserver(IProcessObserver observer) {
        this.mProcessObservers.unregister(observer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchProcessesChanged() {
        int numOfChanges;
        synchronized (this.mProcessChangeLock) {
            numOfChanges = this.mPendingProcessChanges.size();
            if (this.mActiveProcessChanges.length < numOfChanges) {
                this.mActiveProcessChanges = new ActivityManagerService.ProcessChangeItem[numOfChanges];
            }
            this.mPendingProcessChanges.toArray(this.mActiveProcessChanges);
            this.mPendingProcessChanges.clear();
            if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                Slog.i(TAG_PROCESS_OBSERVERS, "*** Delivering " + numOfChanges + " process changes");
            }
        }
        int i = this.mProcessObservers.beginBroadcast();
        while (i > 0) {
            i--;
            IProcessObserver observer = this.mProcessObservers.getBroadcastItem(i);
            if (observer != null) {
                for (int j = 0; j < numOfChanges; j++) {
                    try {
                        ActivityManagerService.ProcessChangeItem item = this.mActiveProcessChanges[j];
                        if ((item.changes & 1) != 0) {
                            if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                                Slog.i(TAG_PROCESS_OBSERVERS, "ACTIVITIES CHANGED pid=" + item.pid + " uid=" + item.uid + ": " + item.foregroundActivities);
                            }
                            observer.onForegroundActivitiesChanged(item.pid, item.uid, item.foregroundActivities);
                        }
                        if ((item.changes & 2) != 0) {
                            if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                                Slog.i(TAG_PROCESS_OBSERVERS, "FOREGROUND SERVICES CHANGED pid=" + item.pid + " uid=" + item.uid + ": " + item.foregroundServiceTypes);
                            }
                            observer.onForegroundServicesChanged(item.pid, item.uid, item.foregroundServiceTypes);
                        }
                    } catch (RemoteException e) {
                    }
                }
            }
        }
        this.mProcessObservers.finishBroadcast();
        synchronized (this.mProcessChangeLock) {
            for (int j2 = 0; j2 < numOfChanges; j2++) {
                this.mAvailProcessChanges.add(this.mActiveProcessChanges[j2]);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManagerService.ProcessChangeItem enqueueProcessChangeItemLocked(int pid, int uid) {
        ActivityManagerService.ProcessChangeItem item;
        synchronized (this.mProcessChangeLock) {
            int i = this.mPendingProcessChanges.size() - 1;
            item = null;
            while (true) {
                if (i < 0) {
                    break;
                }
                item = this.mPendingProcessChanges.get(i);
                if (item.pid == pid) {
                    if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                        Slog.i(TAG_PROCESS_OBSERVERS, "Re-using existing item: " + item);
                    }
                } else {
                    i--;
                }
            }
            if (i < 0) {
                int num = this.mAvailProcessChanges.size();
                if (num > 0) {
                    item = this.mAvailProcessChanges.remove(num - 1);
                    if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                        Slog.i(TAG_PROCESS_OBSERVERS, "Retrieving available item: " + item);
                    }
                } else {
                    item = new ActivityManagerService.ProcessChangeItem();
                    if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                        Slog.i(TAG_PROCESS_OBSERVERS, "Allocating new item: " + item);
                    }
                }
                item.changes = 0;
                item.pid = pid;
                item.uid = uid;
                if (this.mPendingProcessChanges.size() == 0) {
                    if (ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS) {
                        Slog.i(TAG_PROCESS_OBSERVERS, "*** Enqueueing dispatch processes changed!");
                    }
                    this.mService.mUiHandler.obtainMessage(31).sendToTarget();
                }
                this.mPendingProcessChanges.add(item);
            }
        }
        return item;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleDispatchProcessDiedLocked(int pid, int uid) {
        synchronized (this.mProcessChangeLock) {
            for (int i = this.mPendingProcessChanges.size() - 1; i >= 0; i--) {
                ActivityManagerService.ProcessChangeItem item = this.mPendingProcessChanges.get(i);
                if (pid > 0 && item.pid == pid) {
                    this.mPendingProcessChanges.remove(i);
                    this.mAvailProcessChanges.add(item);
                }
            }
            this.mService.mUiHandler.obtainMessage(32, pid, uid, null).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchProcessDied(int pid, int uid) {
        int i = this.mProcessObservers.beginBroadcast();
        while (i > 0) {
            i--;
            IProcessObserver observer = this.mProcessObservers.getBroadcastItem(i);
            if (observer != null) {
                try {
                    observer.onProcessDied(pid, uid);
                } catch (RemoteException e) {
                }
            }
        }
        this.mProcessObservers.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<ProcessRecord> collectProcessesLOSP(int start, boolean allPkgs, String[] args) {
        if (args != null && args.length > start && args[start].charAt(0) != '-') {
            ArrayList<ProcessRecord> procs = new ArrayList<>();
            int pid = -1;
            try {
                pid = Integer.parseInt(args[start]);
            } catch (NumberFormatException e) {
            }
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord proc = this.mLruProcesses.get(i);
                if (proc.getPid() > 0 && proc.getPid() == pid) {
                    procs.add(proc);
                } else if (allPkgs && proc.getPkgList() != null && proc.getPkgList().containsKey(args[start])) {
                    procs.add(proc);
                } else if (proc.processName.equals(args[start])) {
                    procs.add(proc);
                }
            }
            int i2 = procs.size();
            if (i2 <= 0) {
                return null;
            }
            return procs;
        }
        return new ArrayList<>(this.mLruProcesses);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateApplicationInfoLOSP(final List<String> packagesToUpdate, int userId, final boolean updateFrameworkRes) {
        final ArrayList<WindowProcessController> targetProcesses = new ArrayList<>();
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            final ProcessRecord app = this.mLruProcesses.get(i);
            if (app.getThread() != null && (userId == -1 || app.userId == userId)) {
                app.getPkgList().forEachPackage(new Consumer() { // from class: com.android.server.am.ProcessList$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ProcessList.lambda$updateApplicationInfoLOSP$2(updateFrameworkRes, packagesToUpdate, app, targetProcesses, (String) obj);
                    }
                });
            }
        }
        this.mService.mActivityTaskManager.updateAssetConfiguration(targetProcesses, updateFrameworkRes);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$updateApplicationInfoLOSP$2(boolean updateFrameworkRes, List packagesToUpdate, ProcessRecord app, ArrayList targetProcesses, String packageName) {
        if (updateFrameworkRes || packagesToUpdate.contains(packageName)) {
            try {
                ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, (long) GadgetFunction.NCM, app.userId);
                if (ai != null) {
                    if (ai.packageName.equals(app.info.packageName)) {
                        app.info = ai;
                        PlatformCompatCache.getInstance().onApplicationInfoChanged(ai);
                    }
                    app.getThread().scheduleApplicationInfoChanged(ai);
                    targetProcesses.add(app.getWindowProcessController());
                }
            } catch (RemoteException e) {
                Slog.w(TAG, String.format("Failed to update %s ApplicationInfo for %s", packageName, app));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendPackageBroadcastLocked(int cmd, String[] packages, int userId) {
        boolean foundProcess = false;
        for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord r = this.mLruProcesses.get(i);
            IApplicationThread thread = r.getThread();
            if (thread != null && (userId == -1 || r.userId == userId)) {
                try {
                    for (int index = packages.length - 1; index >= 0 && !foundProcess; index--) {
                        if (packages[index].equals(r.info.packageName)) {
                            foundProcess = true;
                        }
                    }
                    thread.dispatchPackageBroadcast(cmd, packages);
                } catch (RemoteException e) {
                }
            }
        }
        if (!foundProcess) {
            try {
                AppGlobals.getPackageManager().notifyPackagesReplacedReceived(packages);
            } catch (RemoteException e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUidProcStateLOSP(int uid) {
        UidRecord uidRec = this.mActiveUids.get(uid);
        if (uidRec == null) {
            return 20;
        }
        return uidRec.getCurProcState();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUidProcessCapabilityLOSP(int uid) {
        UidRecord uidRec = this.mActiveUids.get(uid);
        if (uidRec == null) {
            return 0;
        }
        return uidRec.getCurCapability();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UidRecord getUidRecordLOSP(int uid) {
        return this.mActiveUids.get(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void doStopUidForIdleUidsLocked() {
        int size = this.mActiveUids.size();
        for (int i = 0; i < size; i++) {
            int uid = this.mActiveUids.keyAt(i);
            if (!UserHandle.isCore(uid)) {
                UidRecord uidRec = this.mActiveUids.valueAt(i);
                if (uidRec.isIdle()) {
                    this.mService.doStopUidLocked(uidRec.getUid(), uidRec);
                }
            }
        }
    }

    int getBlockStateForUid(UidRecord uidRec) {
        boolean isAllowed = NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(uidRec.getCurProcState(), uidRec.getCurCapability()) || NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(uidRec.getCurProcState());
        boolean wasAllowed = NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(uidRec.getSetProcState(), uidRec.getSetCapability()) || NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(uidRec.getSetProcState());
        if (wasAllowed || !isAllowed) {
            return (!wasAllowed || isAllowed) ? 0 : 2;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void incrementProcStateSeqAndNotifyAppsLOSP(ActiveUids activeUids) {
        int blockState;
        for (int i = activeUids.size() - 1; i >= 0; i--) {
            activeUids.valueAt(i).curProcStateSeq = getNextProcStateSeq();
        }
        if (this.mService.mConstants.mNetworkAccessTimeoutMs <= 0) {
            return;
        }
        ArrayList<Integer> blockingUids = null;
        for (int i2 = activeUids.size() - 1; i2 >= 0; i2--) {
            UidRecord uidRec = activeUids.valueAt(i2);
            if (this.mService.mInjector.isNetworkRestrictedForUid(uidRec.getUid()) && UserHandle.isApp(uidRec.getUid()) && uidRec.hasInternetPermission && ((uidRec.getSetProcState() != uidRec.getCurProcState() || uidRec.getSetCapability() != uidRec.getCurCapability()) && (blockState = getBlockStateForUid(uidRec)) != 0)) {
                synchronized (uidRec.networkStateLock) {
                    if (blockState == 1) {
                        if (blockingUids == null) {
                            blockingUids = new ArrayList<>();
                        }
                        blockingUids.add(Integer.valueOf(uidRec.getUid()));
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_NETWORK) {
                            Slog.d("ActivityManager_Network", "uid going to background, notifying all blocking threads for uid: " + uidRec);
                        }
                        if (uidRec.procStateSeqWaitingForNetwork != 0) {
                            uidRec.networkStateLock.notifyAll();
                        }
                    }
                }
            }
        }
        if (blockingUids == null) {
            return;
        }
        for (int i3 = this.mLruProcesses.size() - 1; i3 >= 0; i3--) {
            ProcessRecord app = this.mLruProcesses.get(i3);
            if (blockingUids.contains(Integer.valueOf(app.uid))) {
                IApplicationThread thread = app.getThread();
                if (!app.isKilledByAm() && thread != null) {
                    UidRecord uidRec2 = getUidRecordLOSP(app.uid);
                    try {
                        if (ActivityManagerDebugConfig.DEBUG_NETWORK) {
                            Slog.d("ActivityManager_Network", "Informing app thread that it needs to block: " + uidRec2);
                        }
                        if (uidRec2 != null) {
                            thread.setNetworkBlockSeq(uidRec2.curProcStateSeq);
                        }
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getNextProcStateSeq() {
        long j = this.mProcStateSeqCounter + 1;
        this.mProcStateSeqCounter = j;
        return j;
    }

    private LocalSocket createSystemServerSocketForZygote() {
        File socketFile = new File(UNSOL_ZYGOTE_MSG_SOCKET_PATH);
        if (socketFile.exists()) {
            socketFile.delete();
        }
        LocalSocket serverSocket = null;
        try {
            serverSocket = new LocalSocket(1);
            serverSocket.bind(new LocalSocketAddress(UNSOL_ZYGOTE_MSG_SOCKET_PATH, LocalSocketAddress.Namespace.FILESYSTEM));
            Os.chmod(UNSOL_ZYGOTE_MSG_SOCKET_PATH, 438);
            return serverSocket;
        } catch (Exception e) {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e2) {
                }
                return null;
            }
            return serverSocket;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int handleZygoteMessages(FileDescriptor fd, int events) {
        fd.getInt$();
        if ((events & 1) != 0) {
            try {
                byte[] bArr = this.mZygoteUnsolicitedMessage;
                int len = Os.read(fd, bArr, 0, bArr.length);
                if (len > 0) {
                    int[] iArr = this.mZygoteSigChldMessage;
                    if (iArr.length == Zygote.nativeParseSigChld(this.mZygoteUnsolicitedMessage, len, iArr)) {
                        AppExitInfoTracker appExitInfoTracker = this.mAppExitInfoTracker;
                        int[] iArr2 = this.mZygoteSigChldMessage;
                        appExitInfoTracker.handleZygoteSigChld(iArr2[0], iArr2[1], iArr2[2]);
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Exception in reading unsolicited zygote message: " + e);
            }
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleDyingAppDeathLocked(ProcessRecord app, int pid) {
        if (this.mProcessNames.get(app.processName, app.uid) == app || this.mDyingProcesses.get(app.processName, app.uid) != app) {
            return false;
        }
        Slog.v(TAG, "Got obituary of " + pid + ":" + app.processName);
        app.unlinkDeathRecipient();
        this.mDyingProcesses.remove(app.processName, app.uid);
        app.setDyingPid(0);
        handlePrecedingAppDiedLocked(app);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handlePrecedingAppDiedLocked(ProcessRecord app) {
        if (app.mSuccessor != null) {
            if (app.isPersistent() && !app.isRemoved() && this.mService.mPersistentStartingProcesses.indexOf(app.mSuccessor) < 0) {
                this.mService.mPersistentStartingProcesses.add(app.mSuccessor);
            }
            app.mSuccessor.mPredecessor = null;
            app.mSuccessor = null;
            this.mService.mProcStartHandler.removeMessages(2, app);
            this.mService.mProcStartHandler.obtainMessage(1, app).sendToTarget();
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateBackgroundRestrictedForUidPackageLocked(int uid, final String packageName, final boolean restricted) {
        UidRecord uidRec = getUidRecordLOSP(uid);
        if (uidRec != null) {
            final long nowElapsed = SystemClock.elapsedRealtime();
            uidRec.forEachProcess(new Consumer() { // from class: com.android.server.am.ProcessList$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ProcessList.this.m1474xfd7bb491(packageName, restricted, nowElapsed, (ProcessRecord) obj);
                }
            });
            this.mService.updateOomAdjPendingTargetsLocked("updateOomAdj_meh");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateBackgroundRestrictedForUidPackageLocked$3$com-android-server-am-ProcessList  reason: not valid java name */
    public /* synthetic */ void m1474xfd7bb491(String packageName, boolean restricted, long nowElapsed, ProcessRecord app) {
        if (TextUtils.equals(app.info.packageName, packageName)) {
            app.mState.setBackgroundRestricted(restricted);
            if (restricted) {
                this.mAppsInBackgroundRestricted.add(app);
                long future = m1472x5c5ad3cc(app, nowElapsed);
                if (future > 0 && !this.mService.mHandler.hasMessages(58)) {
                    this.mService.mHandler.sendEmptyMessageDelayed(58, future - nowElapsed);
                }
            } else {
                this.mAppsInBackgroundRestricted.remove(app);
            }
            if (!app.isKilledByAm()) {
                this.mService.enqueueOomAdjTargetLocked(app);
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: killAppIfBgRestrictedAndCachedIdleLocked */
    public long m1472x5c5ad3cc(ProcessRecord app, long nowElapsed) {
        UidRecord uidRec = app.getUidRecord();
        long lastCanKillTime = app.mState.getLastCanKillOnBgRestrictedAndIdleTime();
        if (!this.mService.mConstants.mKillBgRestrictedAndCachedIdle || app.isKilled() || app.getThread() == null || uidRec == null || !uidRec.isIdle() || !app.isCached() || app.mState.shouldNotKillOnBgRestrictedAndIdle() || !app.mState.isBackgroundRestricted() || lastCanKillTime == 0) {
            return 0L;
        }
        long future = this.mService.mConstants.mKillBgRestrictedAndCachedIdleSettleTimeMs + lastCanKillTime;
        if (future <= nowElapsed) {
            app.killLocked("cached idle & background restricted", 13, 18, true);
            return 0L;
        }
        return future;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killAppIfBgRestrictedAndCachedIdleLocked(UidRecord uidRec) {
        final long nowElapsed = SystemClock.elapsedRealtime();
        uidRec.forEachProcess(new Consumer() { // from class: com.android.server.am.ProcessList$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ProcessList.this.m1472x5c5ad3cc(nowElapsed, (ProcessRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteProcessDiedLocked(ProcessRecord app) {
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.i(TAG, "note: " + app + " died, saving the exit info");
        }
        Watchdog.getInstance().processDied(app.processName, app.getPid());
        if (app.getDeathRecipient() == null && this.mDyingProcesses.get(app.processName, app.uid) == app) {
            this.mDyingProcesses.remove(app.processName, app.uid);
            app.setDyingPid(0);
        }
        this.mAppExitInfoTracker.scheduleNoteProcessDied(app);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteAppKill(ProcessRecord app, int reason, int subReason, String msg) {
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.i(TAG, "note: " + app + " is being killed, reason: " + reason + ", sub-reason: " + subReason + ", message: " + msg);
        }
        if (app.getPid() > 0 && !app.isolated && app.getDeathRecipient() != null) {
            this.mDyingProcesses.put(app.processName, app.uid, app);
            app.setDyingPid(app.getPid());
        }
        this.mAppExitInfoTracker.scheduleNoteAppKill(app, reason, subReason, msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteAppKill(int pid, int uid, int reason, int subReason, String msg) {
        ProcessRecord app;
        if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
            Slog.i(TAG, "note: " + pid + " is being killed, reason: " + reason + ", sub-reason: " + subReason + ", message: " + msg);
        }
        synchronized (this.mService.mPidsSelfLocked) {
            app = this.mService.mPidsSelfLocked.get(pid);
        }
        if (app != null && app.uid == uid && !app.isolated && app.getDeathRecipient() != null) {
            this.mDyingProcesses.put(app.processName, uid, app);
            app.setDyingPid(app.getPid());
        }
        this.mAppExitInfoTracker.scheduleNoteAppKill(pid, uid, reason, subReason, msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killProcessesWhenImperceptible(int[] pids, String reason, int requester) {
        ProcessRecord app;
        if (ArrayUtils.isEmpty(pids)) {
            return;
        }
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                for (int i : pids) {
                    synchronized (this.mService.mPidsSelfLocked) {
                        app = this.mService.mPidsSelfLocked.get(i);
                    }
                    if (app != null) {
                        this.mImperceptibleKillRunner.enqueueLocked(app, reason, requester);
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ImperceptibleKillRunner extends IUidObserver.Stub {
        private static final String DROPBOX_TAG_IMPERCEPTIBLE_KILL = "imperceptible_app_kill";
        private static final String EXTRA_PID = "pid";
        private static final String EXTRA_REASON = "reason";
        private static final String EXTRA_REQUESTER = "requester";
        private static final String EXTRA_TIMESTAMP = "timestamp";
        private static final String EXTRA_UID = "uid";
        private Handler mHandler;
        private volatile boolean mIdle;
        private IdlenessReceiver mReceiver;
        private boolean mUidObserverEnabled;
        private SparseArray<List<Bundle>> mWorkItems = new SparseArray<>();
        private ProcessMap<Long> mLastProcessKillTimes = new ProcessMap<>();

        /* loaded from: classes.dex */
        private final class H extends Handler {
            static final int MSG_DEVICE_IDLE = 0;
            static final int MSG_UID_GONE = 1;
            static final int MSG_UID_STATE_CHANGED = 2;

            H(Looper looper) {
                super(looper);
            }

            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        ImperceptibleKillRunner.this.handleDeviceIdle();
                        return;
                    case 1:
                        ImperceptibleKillRunner.this.handleUidGone(msg.arg1);
                        return;
                    case 2:
                        ImperceptibleKillRunner.this.handleUidStateChanged(msg.arg1, msg.arg2);
                        return;
                    default:
                        return;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public final class IdlenessReceiver extends BroadcastReceiver {
            private IdlenessReceiver() {
            }

            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                char c;
                PowerManager pm = (PowerManager) ProcessList.this.mService.mContext.getSystemService(PowerManager.class);
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case 498807504:
                        if (action.equals("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 870701415:
                        if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
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
                        ImperceptibleKillRunner.this.notifyDeviceIdleness(pm.isLightDeviceIdleMode());
                        return;
                    case 1:
                        ImperceptibleKillRunner.this.notifyDeviceIdleness(pm.isDeviceIdleMode());
                        return;
                    default:
                        return;
                }
            }
        }

        ImperceptibleKillRunner(Looper looper) {
            this.mHandler = new H(looper);
        }

        boolean enqueueLocked(ProcessRecord app, String reason, int requester) {
            Long last = app.isolated ? null : (Long) this.mLastProcessKillTimes.get(app.processName, app.uid);
            if (last != null && SystemClock.uptimeMillis() < last.longValue() + ActivityManagerConstants.MIN_CRASH_INTERVAL) {
                return false;
            }
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_PID, app.getPid());
            bundle.putInt("uid", app.uid);
            bundle.putLong("timestamp", app.getStartTime());
            bundle.putString("reason", reason);
            bundle.putInt(EXTRA_REQUESTER, requester);
            List<Bundle> list = this.mWorkItems.get(app.uid);
            if (list == null) {
                list = new ArrayList();
                this.mWorkItems.put(app.uid, list);
            }
            list.add(bundle);
            if (this.mReceiver == null) {
                this.mReceiver = new IdlenessReceiver();
                IntentFilter filter = new IntentFilter("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
                filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
                ProcessList.this.mService.mContext.registerReceiver(this.mReceiver, filter);
                return true;
            }
            return true;
        }

        void notifyDeviceIdleness(boolean idle) {
            boolean diff = this.mIdle != idle;
            this.mIdle = idle;
            if (diff && idle) {
                synchronized (ProcessList.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (this.mWorkItems.size() > 0) {
                            this.mHandler.sendEmptyMessage(0);
                        }
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void handleDeviceIdle() {
            DropBoxManager dbox = (DropBoxManager) ProcessList.this.mService.mContext.getSystemService(DropBoxManager.class);
            boolean logToDropbox = dbox != null && dbox.isTagEnabled(DROPBOX_TAG_IMPERCEPTIBLE_KILL);
            synchronized (ProcessList.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    int j = this.mWorkItems.size();
                    int i = j - 1;
                    while (this.mIdle && i >= 0) {
                        List<Bundle> list = this.mWorkItems.valueAt(i);
                        int len = list.size();
                        int j2 = len - 1;
                        while (this.mIdle && j2 >= 0) {
                            Bundle bundle = list.get(j2);
                            int i2 = bundle.getInt(EXTRA_PID);
                            int i3 = bundle.getInt("uid");
                            long j3 = bundle.getLong("timestamp");
                            String string = bundle.getString("reason");
                            int size = bundle.getInt(EXTRA_REQUESTER);
                            int size2 = j;
                            int size3 = j2;
                            if (killProcessLocked(i2, i3, j3, string, size, dbox, logToDropbox)) {
                                list.remove(size3);
                            }
                            j2 = size3 - 1;
                            j = size2;
                        }
                        int size4 = j;
                        int size5 = list.size();
                        if (size5 == 0) {
                            this.mWorkItems.removeAt(i);
                        }
                        i--;
                        j = size4;
                    }
                    registerUidObserverIfNecessaryLocked();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        private void registerUidObserverIfNecessaryLocked() {
            if (!this.mUidObserverEnabled && this.mWorkItems.size() > 0) {
                this.mUidObserverEnabled = true;
                ProcessList.this.mService.registerUidObserver(this, 3, -1, PackageManagerService.PLATFORM_PACKAGE_NAME);
            } else if (this.mUidObserverEnabled && this.mWorkItems.size() == 0) {
                this.mUidObserverEnabled = false;
                ProcessList.this.mService.unregisterUidObserver(this);
            }
        }

        private boolean killProcessLocked(int pid, int uid, long timestamp, String reason, int requester, DropBoxManager dbox, boolean logToDropbox) {
            ProcessRecord app;
            synchronized (ProcessList.this.mService.mPidsSelfLocked) {
                app = ProcessList.this.mService.mPidsSelfLocked.get(pid);
            }
            if (app == null || app.getPid() != pid || app.uid != uid || app.getStartTime() != timestamp || app.getPkgList().searchEachPackage(new Function() { // from class: com.android.server.am.ProcessList$ImperceptibleKillRunner$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ProcessList.ImperceptibleKillRunner.this.m1478x9537f5ed((String) obj);
                }
            }) != null) {
                return true;
            }
            if (!ProcessList.this.mService.mConstants.IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES.contains(Integer.valueOf(app.mState.getReportedProcState()))) {
                app.killLocked(reason, 13, 15, true);
                if (!app.isolated) {
                    this.mLastProcessKillTimes.put(app.processName, app.uid, Long.valueOf(SystemClock.uptimeMillis()));
                }
                if (logToDropbox) {
                    SystemClock.elapsedRealtime();
                    StringBuilder sb = new StringBuilder();
                    ProcessList.this.mService.appendDropBoxProcessHeaders(app, app.processName, sb);
                    sb.append("Reason: " + reason).append("\n");
                    sb.append("Requester UID: " + requester).append("\n");
                    dbox.addText(DROPBOX_TAG_IMPERCEPTIBLE_KILL, sb.toString());
                }
                return true;
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$killProcessLocked$0$com-android-server-am-ProcessList$ImperceptibleKillRunner  reason: not valid java name */
        public /* synthetic */ Boolean m1478x9537f5ed(String pkgName) {
            if (ProcessList.this.mService.mConstants.IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES.contains(pkgName)) {
                return Boolean.TRUE;
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void handleUidStateChanged(int uid, int procState) {
            List<Bundle> list;
            DropBoxManager dbox = (DropBoxManager) ProcessList.this.mService.mContext.getSystemService(DropBoxManager.class);
            boolean logToDropbox = dbox != null && dbox.isTagEnabled(DROPBOX_TAG_IMPERCEPTIBLE_KILL);
            synchronized (ProcessList.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (this.mIdle && !ProcessList.this.mService.mConstants.IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES.contains(Integer.valueOf(procState)) && (list = this.mWorkItems.get(uid)) != null) {
                        int len = list.size();
                        for (int j = len - 1; this.mIdle && j >= 0; j--) {
                            Bundle bundle = list.get(j);
                            if (killProcessLocked(bundle.getInt(EXTRA_PID), bundle.getInt("uid"), bundle.getLong("timestamp"), bundle.getString("reason"), bundle.getInt(EXTRA_REQUESTER), dbox, logToDropbox)) {
                                list.remove(j);
                            }
                        }
                        if (list.size() == 0) {
                            this.mWorkItems.remove(uid);
                        }
                        registerUidObserverIfNecessaryLocked();
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void handleUidGone(int uid) {
            synchronized (ProcessList.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mWorkItems.remove(uid);
                    registerUidObserverIfNecessaryLocked();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public void onUidGone(int uid, boolean disabled) {
            this.mHandler.obtainMessage(1, uid, 0).sendToTarget();
        }

        public void onUidActive(int uid) {
        }

        public void onUidIdle(int uid, boolean disabled) {
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
            this.mHandler.obtainMessage(2, uid, procState).sendToTarget();
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }

        public void onUidProcAdjChanged(int uid) {
        }
    }

    public void addToMuteProcessList(int pid, String packageName, int multiWindowId) {
        ITranProcessList.Instance().addToMuteProcessList(this, getLruProcessesLOSP(), pid, packageName, multiWindowId);
    }

    public void removeFromMuteProcessList(String packageName, int pid, int multiWindowId) {
        ITranProcessList.Instance().removeFromMuteProcessList(this, getLruProcessesLOSP(), packageName, pid, multiWindowId);
    }

    public void clearMuteProcessList(int multiWindowId) {
        ITranProcessList.Instance().clearMuteProcessList(this, multiWindowId);
    }

    public void inMuteProcessList(String packageName, int pid, int uid) {
        ITranProcessList.Instance().inMuteProcessList(this, packageName, pid, uid);
    }

    public boolean isNoneEmptyMuteProcessList() {
        return ITranProcessList.Instance().isNoneEmptyMuteProcessList(this);
    }

    public Map<Integer, ArrayList<String>> getMuteList() {
        return this.mMuteList;
    }

    private void setMaxOomAdjForGriffin(ProcessRecord r) {
        if (ITranGriffinFeature.Instance().isGriffinSupport()) {
            int fixedMaxAdj = ITranProcessList.Instance().setMaxOomAdj(r, r.processWrapper, r.mState.getMaxAdj());
            r.mState.setMaxAdj(fixedMaxAdj);
        }
    }

    /* loaded from: classes.dex */
    public static class TranProcessListProxy {
        private ProcessList mProcessList;

        public TranProcessListProxy(ProcessList processList) {
            this.mProcessList = null;
            this.mProcessList = processList;
        }

        public boolean writeLmkd(ByteBuffer buf, ByteBuffer repl) {
            if (this.mProcessList != null) {
                return ProcessList.writeLmkd(buf, repl);
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setKillAllBackgroundProcessesByShell() {
        this.mKillAllBackgroundProcessesByShell = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reSetKillAllBackgroundProcessesByShell() {
        this.mKillAllBackgroundProcessesByShell = false;
    }
}
