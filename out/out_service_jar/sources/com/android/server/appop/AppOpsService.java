package com.android.server.appop;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AppOpsManagerInternal;
import android.app.AsyncNotedAppOp;
import android.app.RuntimeAppOpAccessMessage;
import android.app.SyncNotedAppOp;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.AttributionSource;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PermissionInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PackageTagsList;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManagerInternal;
import android.permission.PermissionManager;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.IndentingPrintWriter;
import android.util.KeyValueListParser;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.Pools;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.app.IAppOpsActiveCallback;
import com.android.internal.app.IAppOpsAsyncNotedCallback;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsNotedCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IAppOpsStartedCallback;
import com.android.internal.app.MessageSamplingConfig;
import com.android.internal.compat.IPlatformCompat;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.function.DodecConsumer;
import com.android.internal.util.function.HeptFunction;
import com.android.internal.util.function.HexFunction;
import com.android.internal.util.function.NonaConsumer;
import com.android.internal.util.function.OctConsumer;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuadFunction;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.QuintFunction;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.UndecConsumer;
import com.android.internal.util.function.UndecFunction;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.appop.AppOpsService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.PackageList;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.component.ParsedAttribution;
import com.android.server.policy.AppOpsPolicy;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.vibrator.VibratorManagerService;
import com.android.server.wm.ActivityTaskManagerService;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import dalvik.annotation.optimization.NeverCompile;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import libcore.util.EmptyArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class AppOpsService extends IAppOpsService.Stub {
    private static final int CURRENT_VERSION = 1;
    static final boolean DEBUG = false;
    private static final int MAX_UNFORWARDED_OPS = 10;
    private static final int MAX_UNUSED_POOLED_OBJECTS = 3;
    private static final int NO_VERSION = -1;
    private static final int RARELY_USED_PACKAGES_INITIALIZATION_DELAY_MILLIS = 300000;
    static final String TAG = "AppOps";
    private static final int UID_ANY = -2;
    static final long WRITE_DELAY = 1800000;
    private ActivityManagerInternal mActivityManagerInternal;
    private RuntimeAppOpAccessMessage mCollectedRuntimePermissionMessage;
    final Constants mConstants;
    final Context mContext;
    boolean mFastWriteScheduled;
    final AtomicFile mFile;
    final Handler mHandler;
    long mLastRealtime;
    private float mMessagesCollectedCount;
    private final File mNoteOpCallerStacktracesFile;
    private PackageManagerInternal mPackageManagerInternal;
    SparseIntArray mProfileOwners;
    private int mSamplingStrategy;
    boolean mWriteNoteOpsScheduled;
    boolean mWriteScheduled;
    private static final CtaManager sCtaManager = CtaManagerFactory.getInstance().makeCtaManager();
    private static final int[] PROCESS_STATE_TO_UID_STATE = {100, 100, 200, 500, 400, 500, 600, 600, 600, 600, 600, 600, 700, 700, 700, 700, 700, 700, 700, 700, 700};
    private static final int[] OPS_RESTRICTED_ON_SUSPEND = {28, 27, 26};
    private final ArraySet<NoteOpTrace> mNoteOpCallerStacktraces = new ArraySet<>();
    private final OpEventProxyInfoPool mOpEventProxyInfoPool = new OpEventProxyInfoPool();
    private final InProgressStartOpEventPool mInProgressStartOpEventPool = new InProgressStartOpEventPool();
    private final AppOpsManagerInternalImpl mAppOpsManagerInternal = new AppOpsManagerInternalImpl();
    private final DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
    private final IPlatformCompat mPlatformCompat = IPlatformCompat.Stub.asInterface(ServiceManager.getService("platform_compat"));
    private final ArrayMap<Pair<String, Integer>, RemoteCallbackList<IAppOpsAsyncNotedCallback>> mAsyncOpWatchers = new ArrayMap<>();
    private final ArrayMap<Pair<String, Integer>, ArrayList<AsyncNotedAppOp>> mUnforwardedAsyncNotedOps = new ArrayMap<>();
    final Runnable mWriteRunner = new Runnable() { // from class: com.android.server.appop.AppOpsService.1
        {
            AppOpsService.this = this;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mWriteScheduled = false;
                AppOpsService.this.mFastWriteScheduled = false;
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() { // from class: com.android.server.appop.AppOpsService.1.1
                    {
                        AnonymousClass1.this = this;
                    }

                    @Override // android.os.AsyncTask
                    public Void doInBackground(Void... params) {
                        AppOpsService.this.writeState();
                        return null;
                    }
                };
                Void[] voidArr = null;
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            }
        }
    };
    final SparseArray<UidState> mUidStates = new SparseArray<>();
    volatile HistoricalRegistry mHistoricalRegistry = new HistoricalRegistry(this);
    private final ArrayMap<IBinder, ClientUserRestrictionState> mOpUserRestrictions = new ArrayMap<>();
    private final ArrayMap<IBinder, ClientGlobalRestrictionState> mOpGlobalRestrictions = new ArrayMap<>();
    private volatile CheckOpsDelegateDispatcher mCheckOpsDelegateDispatcher = new CheckOpsDelegateDispatcher(null, null);
    private final SparseArray<int[]> mSwitchedOps = new SparseArray<>();
    private String mSampledPackage = null;
    private int mSampledAppOpCode = -1;
    private int mAcceptableLeftDistance = 0;
    private ArraySet<String> mRarelyUsedPackages = new ArraySet<>();
    final SparseArray<ArraySet<ModeCallback>> mOpModeWatchers = new SparseArray<>();
    final ArrayMap<String, ArraySet<ModeCallback>> mPackageModeWatchers = new ArrayMap<>();
    final ArrayMap<IBinder, ModeCallback> mModeWatchers = new ArrayMap<>();
    final ArrayMap<IBinder, SparseArray<ActiveCallback>> mActiveWatchers = new ArrayMap<>();
    final ArrayMap<IBinder, SparseArray<StartedCallback>> mStartedWatchers = new ArrayMap<>();
    final ArrayMap<IBinder, SparseArray<NotedCallback>> mNotedWatchers = new ArrayMap<>();
    final AudioRestrictionManager mAudioRestrictionManager = new AudioRestrictionManager();
    private BroadcastReceiver mOnPackageUpdatedReceiver = new BroadcastReceiver() { // from class: com.android.server.appop.AppOpsService.2
        {
            AppOpsService.this = this;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            AndroidPackage pkg;
            String action = intent.getAction();
            String pkgName = intent.getData().getEncodedSchemeSpecificPart();
            int uid = intent.getIntExtra("android.intent.extra.UID", -1);
            if (action.equals("android.intent.action.PACKAGE_REMOVED") && !intent.hasExtra("android.intent.extra.REPLACING")) {
                synchronized (AppOpsService.this) {
                    UidState uidState = AppOpsService.this.mUidStates.get(uid);
                    if (uidState != null && uidState.pkgOps != null) {
                        Ops removedOps = uidState.pkgOps.remove(pkgName);
                        if (removedOps != null) {
                            AppOpsService.this.scheduleFastWriteLocked();
                        }
                    }
                }
            } else if (action.equals("android.intent.action.PACKAGE_REPLACED") && (pkg = AppOpsService.this.getPackageManagerInternal().getPackage(pkgName)) != null) {
                ArrayMap<String, String> dstAttributionTags = new ArrayMap<>();
                ArraySet<String> attributionTags = new ArraySet<>();
                attributionTags.add(null);
                if (pkg.getAttributions() != null) {
                    int numAttributions = pkg.getAttributions().size();
                    for (int attributionNum = 0; attributionNum < numAttributions; attributionNum++) {
                        ParsedAttribution attribution = pkg.getAttributions().get(attributionNum);
                        attributionTags.add(attribution.getTag());
                        int numInheritFrom = attribution.getInheritFrom().size();
                        for (int inheritFromNum = 0; inheritFromNum < numInheritFrom; inheritFromNum++) {
                            dstAttributionTags.put(attribution.getInheritFrom().get(inheritFromNum), attribution.getTag());
                        }
                    }
                }
                synchronized (AppOpsService.this) {
                    UidState uidState2 = AppOpsService.this.mUidStates.get(uid);
                    if (uidState2 != null && uidState2.pkgOps != null) {
                        Ops ops = uidState2.pkgOps.get(pkgName);
                        if (ops == null) {
                            return;
                        }
                        ops.bypass = null;
                        ops.knownAttributionTags.clear();
                        int numOps = ops.size();
                        for (int opNum = 0; opNum < numOps; opNum++) {
                            Op op = ops.valueAt(opNum);
                            int numAttributions2 = op.mAttributions.size();
                            int attributionNum2 = numAttributions2 - 1;
                            while (attributionNum2 >= 0) {
                                int numOps2 = numOps;
                                String attributionTag = op.mAttributions.keyAt(attributionNum2);
                                if (!attributionTags.contains(attributionTag)) {
                                    String newAttributionTag = dstAttributionTags.get(attributionTag);
                                    AttributedOp newAttributedOp = op.getOrCreateAttribution(op, newAttributionTag);
                                    newAttributedOp.add(op.mAttributions.valueAt(attributionNum2));
                                    op.mAttributions.removeAt(attributionNum2);
                                    AppOpsService.this.scheduleFastWriteLocked();
                                }
                                attributionNum2--;
                                numOps = numOps2;
                            }
                        }
                    }
                }
            }
        }
    };

    /* loaded from: classes.dex */
    public class OpEventProxyInfoPool extends Pools.SimplePool<AppOpsManager.OpEventProxyInfo> {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        OpEventProxyInfoPool() {
            super(3);
            AppOpsService.this = r1;
        }

        AppOpsManager.OpEventProxyInfo acquire(int uid, String packageName, String attributionTag) {
            AppOpsManager.OpEventProxyInfo recycled = (AppOpsManager.OpEventProxyInfo) acquire();
            if (recycled != null) {
                recycled.reinit(uid, packageName, attributionTag);
                return recycled;
            }
            return new AppOpsManager.OpEventProxyInfo(uid, packageName, attributionTag);
        }
    }

    /* loaded from: classes.dex */
    public class InProgressStartOpEventPool extends Pools.SimplePool<InProgressStartOpEvent> {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        InProgressStartOpEventPool() {
            super(3);
            AppOpsService.this = r1;
        }

        InProgressStartOpEvent acquire(long startTime, long elapsedTime, IBinder clientId, String attributionTag, Runnable onDeath, int proxyUid, String proxyPackageName, String proxyAttributionTag, int uidState, int flags, int attributionFlags, int attributionChainId) throws RemoteException {
            AppOpsManager.OpEventProxyInfo proxyInfo;
            InProgressStartOpEvent recycled = (InProgressStartOpEvent) acquire();
            if (proxyUid == -1) {
                proxyInfo = null;
            } else {
                AppOpsManager.OpEventProxyInfo proxyInfo2 = AppOpsService.this.mOpEventProxyInfoPool.acquire(proxyUid, proxyPackageName, proxyAttributionTag);
                proxyInfo = proxyInfo2;
            }
            if (recycled != null) {
                recycled.reinit(startTime, elapsedTime, clientId, attributionTag, onDeath, uidState, flags, proxyInfo, attributionFlags, attributionChainId, AppOpsService.this.mOpEventProxyInfoPool);
                return recycled;
            }
            return new InProgressStartOpEvent(startTime, elapsedTime, clientId, attributionTag, onDeath, uidState, proxyInfo, flags, attributionFlags, attributionChainId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class Constants extends ContentObserver {
        public long BG_STATE_SETTLE_TIME;
        public long FG_SERVICE_STATE_SETTLE_TIME;
        public long TOP_STATE_SETTLE_TIME;
        private final KeyValueListParser mParser;
        private ContentResolver mResolver;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public Constants(Handler handler) {
            super(handler);
            AppOpsService.this = this$0;
            this.mParser = new KeyValueListParser(',');
            updateConstants();
        }

        public void startMonitoring(ContentResolver resolver) {
            this.mResolver = resolver;
            resolver.registerContentObserver(Settings.Global.getUriFor("app_ops_constants"), false, this);
            updateConstants();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            ContentResolver contentResolver = this.mResolver;
            String value = contentResolver != null ? Settings.Global.getString(contentResolver, "app_ops_constants") : "";
            synchronized (AppOpsService.this) {
                try {
                    this.mParser.setString(value);
                } catch (IllegalArgumentException e) {
                    Slog.e(AppOpsService.TAG, "Bad app ops settings", e);
                }
                this.TOP_STATE_SETTLE_TIME = this.mParser.getDurationMillis("top_state_settle_time", 5000L);
                this.FG_SERVICE_STATE_SETTLE_TIME = this.mParser.getDurationMillis("fg_service_state_settle_time", 5000L);
                this.BG_STATE_SETTLE_TIME = this.mParser.getDurationMillis("bg_state_settle_time", 1000L);
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings:");
            pw.print("    ");
            pw.print("top_state_settle_time");
            pw.print("=");
            TimeUtils.formatDuration(this.TOP_STATE_SETTLE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print("fg_service_state_settle_time");
            pw.print("=");
            TimeUtils.formatDuration(this.FG_SERVICE_STATE_SETTLE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print("bg_state_settle_time");
            pw.print("=");
            TimeUtils.formatDuration(this.BG_STATE_SETTLE_TIME, pw);
            pw.println();
        }
    }

    /* loaded from: classes.dex */
    public final class UidState {
        public boolean appWidgetVisible;
        public int capability;
        public SparseBooleanArray foregroundOps;
        public boolean hasForegroundWatchers;
        public SparseIntArray opModes;
        public boolean pendingAppWidgetVisible;
        public int pendingCapability;
        public long pendingStateCommitTime;
        public ArrayMap<String, Ops> pkgOps;
        public final int uid;
        public int state = 700;
        public int pendingState = 700;

        public UidState(int uid) {
            AppOpsService.this = this$0;
            this.uid = uid;
        }

        public void clear() {
            this.pkgOps = null;
            this.opModes = null;
        }

        public boolean isDefault() {
            SparseIntArray sparseIntArray;
            ArrayMap<String, Ops> arrayMap = this.pkgOps;
            return (arrayMap == null || arrayMap.isEmpty()) && ((sparseIntArray = this.opModes) == null || sparseIntArray.size() <= 0) && this.state == 700 && this.pendingState == 700;
        }

        int evalMode(int op, int mode) {
            int i;
            if (mode == 4) {
                if (this.appWidgetVisible) {
                    return 0;
                }
                if (AppOpsService.this.mActivityManagerInternal == null || !AppOpsService.this.mActivityManagerInternal.isPendingTopUid(this.uid)) {
                    if ((AppOpsService.this.mActivityManagerInternal == null || !AppOpsService.this.mActivityManagerInternal.isTempAllowlistedForFgsWhileInUse(this.uid)) && (i = this.state) > 200) {
                        if (i <= AppOpsManager.resolveFirstUnrestrictedUidState(op)) {
                            switch (op) {
                                case 0:
                                case 1:
                                case 41:
                                case 42:
                                    return (this.capability & 1) != 0 ? 0 : 1;
                                case 26:
                                    return (this.capability & 2) != 0 ? 0 : 1;
                                case 27:
                                    return (4 & this.capability) != 0 ? 0 : 1;
                                default:
                                    return 0;
                            }
                        }
                        return 1;
                    }
                    return 0;
                }
                return 0;
            }
            return mode;
        }

        private void evalForegroundWatchers(int op, SparseArray<ArraySet<ModeCallback>> watchers, SparseBooleanArray which) {
            boolean curValue = which.get(op, false);
            ArraySet<ModeCallback> callbacks = watchers.get(op);
            if (callbacks != null) {
                for (int cbi = callbacks.size() - 1; !curValue && cbi >= 0; cbi--) {
                    if ((callbacks.valueAt(cbi).mFlags & 1) != 0) {
                        this.hasForegroundWatchers = true;
                        curValue = true;
                    }
                }
            }
            which.put(op, curValue);
        }

        public void evalForegroundOps(SparseArray<ArraySet<ModeCallback>> watchers) {
            SparseBooleanArray which = null;
            this.hasForegroundWatchers = false;
            SparseIntArray sparseIntArray = this.opModes;
            if (sparseIntArray != null) {
                for (int i = sparseIntArray.size() - 1; i >= 0; i--) {
                    if (this.opModes.valueAt(i) == 4) {
                        if (which == null) {
                            which = new SparseBooleanArray();
                        }
                        evalForegroundWatchers(this.opModes.keyAt(i), watchers, which);
                    }
                }
            }
            ArrayMap<String, Ops> arrayMap = this.pkgOps;
            if (arrayMap != null) {
                for (int i2 = arrayMap.size() - 1; i2 >= 0; i2--) {
                    Ops ops = this.pkgOps.valueAt(i2);
                    for (int j = ops.size() - 1; j >= 0; j--) {
                        if (ops.valueAt(j).mode == 4) {
                            if (which == null) {
                                which = new SparseBooleanArray();
                            }
                            evalForegroundWatchers(ops.keyAt(j), watchers, which);
                        }
                    }
                }
            }
            this.foregroundOps = which;
        }
    }

    /* loaded from: classes.dex */
    public static final class Ops extends SparseArray<Op> {
        AppOpsManager.RestrictionBypass bypass;
        final String packageName;
        final UidState uidState;
        final ArraySet<String> knownAttributionTags = new ArraySet<>();
        final ArraySet<String> validAttributionTags = new ArraySet<>();

        Ops(String _packageName, UidState _uidState) {
            this.packageName = _packageName;
            this.uidState = _uidState;
        }
    }

    /* loaded from: classes.dex */
    public static final class PackageVerificationResult {
        final AppOpsManager.RestrictionBypass bypass;
        final boolean isAttributionTagValid;

        PackageVerificationResult(AppOpsManager.RestrictionBypass bypass, boolean isAttributionTagValid) {
            this.bypass = bypass;
            this.isAttributionTagValid = isAttributionTagValid;
        }
    }

    /* loaded from: classes.dex */
    public static final class InProgressStartOpEvent implements IBinder.DeathRecipient {
        private int mAttributionChainId;
        private int mAttributionFlags;
        private String mAttributionTag;
        private IBinder mClientId;
        private int mFlags;
        private Runnable mOnDeath;
        private AppOpsManager.OpEventProxyInfo mProxy;
        private long mStartElapsedTime;
        private long mStartTime;
        private int mUidState;
        int numUnfinishedStarts;

        private InProgressStartOpEvent(long startTime, long startElapsedTime, IBinder clientId, String attributionTag, Runnable onDeath, int uidState, AppOpsManager.OpEventProxyInfo proxy, int flags, int attributionFlags, int attributionChainId) throws RemoteException {
            this.mStartTime = startTime;
            this.mStartElapsedTime = startElapsedTime;
            this.mClientId = clientId;
            this.mAttributionTag = attributionTag;
            this.mOnDeath = onDeath;
            this.mUidState = uidState;
            this.mProxy = proxy;
            this.mFlags = flags;
            this.mAttributionFlags = attributionFlags;
            this.mAttributionChainId = attributionChainId;
            clientId.linkToDeath(this, 0);
        }

        public void finish() {
            try {
                this.mClientId.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            this.mOnDeath.run();
        }

        public void reinit(long startTime, long startElapsedTime, IBinder clientId, String attributionTag, Runnable onDeath, int uidState, int flags, AppOpsManager.OpEventProxyInfo proxy, int attributionFlags, int attributionChainId, Pools.Pool<AppOpsManager.OpEventProxyInfo> proxyPool) throws RemoteException {
            this.mStartTime = startTime;
            this.mStartElapsedTime = startElapsedTime;
            this.mClientId = clientId;
            this.mAttributionTag = attributionTag;
            this.mOnDeath = onDeath;
            this.mUidState = uidState;
            this.mFlags = flags;
            AppOpsManager.OpEventProxyInfo opEventProxyInfo = this.mProxy;
            if (opEventProxyInfo != null) {
                proxyPool.release(opEventProxyInfo);
            }
            this.mProxy = proxy;
            this.mAttributionFlags = attributionFlags;
            this.mAttributionChainId = attributionChainId;
            clientId.linkToDeath(this, 0);
        }

        public long getStartTime() {
            return this.mStartTime;
        }

        public long getStartElapsedTime() {
            return this.mStartElapsedTime;
        }

        public IBinder getClientId() {
            return this.mClientId;
        }

        public int getUidState() {
            return this.mUidState;
        }

        public AppOpsManager.OpEventProxyInfo getProxy() {
            return this.mProxy;
        }

        public int getFlags() {
            return this.mFlags;
        }

        public int getAttributionFlags() {
            return this.mAttributionFlags;
        }

        public int getAttributionChainId() {
            return this.mAttributionChainId;
        }
    }

    /* loaded from: classes.dex */
    public final class AttributedOp {
        private LongSparseArray<AppOpsManager.NoteOpEvent> mAccessEvents;
        private ArrayMap<IBinder, InProgressStartOpEvent> mInProgressEvents;
        private ArrayMap<IBinder, InProgressStartOpEvent> mPausedInProgressEvents;
        private LongSparseArray<AppOpsManager.NoteOpEvent> mRejectEvents;
        public final Op parent;
        public final String tag;

        AttributedOp(String tag, Op parent) {
            AppOpsService.this = r1;
            this.tag = tag;
            this.parent = parent;
        }

        public void accessed(int proxyUid, String proxyPackageName, String proxyAttributionTag, int uidState, int flags) {
            long accessTime = System.currentTimeMillis();
            accessed(accessTime, -1L, proxyUid, proxyPackageName, proxyAttributionTag, uidState, flags);
            AppOpsService.this.mHistoricalRegistry.incrementOpAccessedCount(this.parent.op, this.parent.uid, this.parent.packageName, this.tag, uidState, flags, accessTime, 0, -1);
        }

        public void accessed(long noteTime, long duration, int proxyUid, String proxyPackageName, String proxyAttributionTag, int uidState, int flags) {
            long key = AppOpsManager.makeKey(uidState, flags);
            if (this.mAccessEvents == null) {
                this.mAccessEvents = new LongSparseArray<>(1);
            }
            AppOpsManager.OpEventProxyInfo proxyInfo = null;
            if (proxyUid != -1) {
                proxyInfo = AppOpsService.this.mOpEventProxyInfoPool.acquire(proxyUid, proxyPackageName, proxyAttributionTag);
            }
            AppOpsManager.NoteOpEvent existingEvent = this.mAccessEvents.get(key);
            if (existingEvent != null) {
                existingEvent.reinit(noteTime, duration, proxyInfo, AppOpsService.this.mOpEventProxyInfoPool);
                return;
            }
            this.mAccessEvents.put(key, new AppOpsManager.NoteOpEvent(noteTime, duration, proxyInfo));
        }

        public void rejected(int uidState, int flags) {
            rejected(System.currentTimeMillis(), uidState, flags);
            AppOpsService.this.mHistoricalRegistry.incrementOpRejected(this.parent.op, this.parent.uid, this.parent.packageName, this.tag, uidState, flags);
        }

        public void rejected(long noteTime, int uidState, int flags) {
            long key = AppOpsManager.makeKey(uidState, flags);
            if (this.mRejectEvents == null) {
                this.mRejectEvents = new LongSparseArray<>(1);
            }
            AppOpsManager.NoteOpEvent existingEvent = this.mRejectEvents.get(key);
            if (existingEvent != null) {
                existingEvent.reinit(noteTime, -1L, (AppOpsManager.OpEventProxyInfo) null, AppOpsService.this.mOpEventProxyInfoPool);
            } else {
                this.mRejectEvents.put(key, new AppOpsManager.NoteOpEvent(noteTime, -1L, (AppOpsManager.OpEventProxyInfo) null));
            }
        }

        public void started(IBinder clientId, int proxyUid, String proxyPackageName, String proxyAttributionTag, int uidState, int flags, int attributionFlags, int attributionChainId) throws RemoteException {
            started(clientId, proxyUid, proxyPackageName, proxyAttributionTag, uidState, flags, true, attributionFlags, attributionChainId);
        }

        private void started(IBinder clientId, int proxyUid, String proxyPackageName, String proxyAttributionTag, int uidState, int flags, boolean triggerCallbackIfNeeded, int attributionFlags, int attributionChainId) throws RemoteException {
            startedOrPaused(clientId, proxyUid, proxyPackageName, proxyAttributionTag, uidState, flags, triggerCallbackIfNeeded, true, attributionFlags, attributionChainId);
        }

        private void startedOrPaused(IBinder clientId, int proxyUid, String proxyPackageName, String proxyAttributionTag, int uidState, int flags, boolean triggerCallbackIfNeeded, boolean isStarted, int attributionFlags, int attributionChainId) throws RemoteException {
            ArrayMap<IBinder, InProgressStartOpEvent> arrayMap;
            AttributedOp attributedOp;
            InProgressStartOpEvent event;
            if (triggerCallbackIfNeeded && !this.parent.isRunning() && isStarted) {
                AppOpsService.this.scheduleOpActiveChangedIfNeededLocked(this.parent.op, this.parent.uid, this.parent.packageName, this.tag, true, attributionFlags, attributionChainId);
            }
            if (isStarted && this.mInProgressEvents == null) {
                this.mInProgressEvents = new ArrayMap<>(1);
            } else if (!isStarted && this.mPausedInProgressEvents == null) {
                this.mPausedInProgressEvents = new ArrayMap<>(1);
            }
            if (!isStarted) {
                arrayMap = this.mPausedInProgressEvents;
            } else {
                arrayMap = this.mInProgressEvents;
            }
            ArrayMap<IBinder, InProgressStartOpEvent> events = arrayMap;
            long startTime = System.currentTimeMillis();
            InProgressStartOpEvent event2 = events.get(clientId);
            if (event2 == null) {
                InProgressStartOpEvent event3 = AppOpsService.this.mInProgressStartOpEventPool.acquire(startTime, SystemClock.elapsedRealtime(), clientId, this.tag, PooledLambda.obtainRunnable(new BiConsumer() { // from class: com.android.server.appop.AppOpsService$AttributedOp$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        AppOpsService.onClientDeath((AppOpsService.AttributedOp) obj, (IBinder) obj2);
                    }
                }, this, clientId), proxyUid, proxyPackageName, proxyAttributionTag, uidState, flags, attributionFlags, attributionChainId);
                events.put(clientId, event3);
                event = event3;
                attributedOp = this;
            } else {
                if (uidState != event2.mUidState) {
                    attributedOp = this;
                    attributedOp.onUidStateChanged(uidState);
                } else {
                    attributedOp = this;
                }
                event = event2;
            }
            event.numUnfinishedStarts++;
            if (isStarted) {
                AppOpsService.this.mHistoricalRegistry.incrementOpAccessedCount(attributedOp.parent.op, attributedOp.parent.uid, attributedOp.parent.packageName, attributedOp.tag, uidState, flags, startTime, attributionFlags, attributionChainId);
            }
        }

        public void finished(IBinder clientId) {
            finished(clientId, true);
        }

        private void finished(IBinder clientId, boolean triggerCallbackIfNeeded) {
            finishOrPause(clientId, triggerCallbackIfNeeded, false);
        }

        private void finishOrPause(IBinder clientId, boolean triggerCallbackIfNeeded, boolean isPausing) {
            int indexOfToken = isRunning() ? this.mInProgressEvents.indexOfKey(clientId) : -1;
            if (indexOfToken < 0) {
                finishPossiblyPaused(clientId, isPausing);
                return;
            }
            InProgressStartOpEvent event = this.mInProgressEvents.valueAt(indexOfToken);
            if (!isPausing) {
                event.numUnfinishedStarts--;
            }
            if (event.numUnfinishedStarts == 0 || isPausing) {
                if (!isPausing) {
                    event.finish();
                    this.mInProgressEvents.removeAt(indexOfToken);
                }
                if (this.mAccessEvents == null) {
                    this.mAccessEvents = new LongSparseArray<>(1);
                }
                AppOpsManager.OpEventProxyInfo proxyCopy = event.getProxy() != null ? new AppOpsManager.OpEventProxyInfo(event.getProxy()) : null;
                long accessDurationMillis = SystemClock.elapsedRealtime() - event.getStartElapsedTime();
                AppOpsManager.NoteOpEvent finishedEvent = new AppOpsManager.NoteOpEvent(event.getStartTime(), accessDurationMillis, proxyCopy);
                this.mAccessEvents.put(AppOpsManager.makeKey(event.getUidState(), event.getFlags()), finishedEvent);
                AppOpsService.this.mHistoricalRegistry.increaseOpAccessDuration(this.parent.op, this.parent.uid, this.parent.packageName, this.tag, event.getUidState(), event.getFlags(), finishedEvent.getNoteTime(), finishedEvent.getDuration(), event.getAttributionFlags(), event.getAttributionChainId());
                if (!isPausing) {
                    AppOpsService.this.mInProgressStartOpEventPool.release(event);
                    if (this.mInProgressEvents.isEmpty()) {
                        this.mInProgressEvents = null;
                        if (triggerCallbackIfNeeded && !this.parent.isRunning()) {
                            AppOpsService.this.scheduleOpActiveChangedIfNeededLocked(this.parent.op, this.parent.uid, this.parent.packageName, this.tag, false, event.getAttributionFlags(), event.getAttributionChainId());
                        }
                    }
                }
            }
        }

        private void finishPossiblyPaused(IBinder clientId, boolean isPausing) {
            if (!isPaused()) {
                Slog.wtf(AppOpsService.TAG, "No ops running or paused");
                return;
            }
            int indexOfToken = this.mPausedInProgressEvents.indexOfKey(clientId);
            if (indexOfToken < 0) {
                Slog.wtf(AppOpsService.TAG, "No op running or paused for the client");
            } else if (isPausing) {
            } else {
                InProgressStartOpEvent event = this.mPausedInProgressEvents.valueAt(indexOfToken);
                event.numUnfinishedStarts--;
                if (event.numUnfinishedStarts == 0) {
                    this.mPausedInProgressEvents.removeAt(indexOfToken);
                    AppOpsService.this.mInProgressStartOpEventPool.release(event);
                    if (this.mPausedInProgressEvents.isEmpty()) {
                        this.mPausedInProgressEvents = null;
                    }
                }
            }
        }

        public void createPaused(IBinder clientId, int proxyUid, String proxyPackageName, String proxyAttributionTag, int uidState, int flags, int attributionFlags, int attributionChainId) throws RemoteException {
            startedOrPaused(clientId, proxyUid, proxyPackageName, proxyAttributionTag, uidState, flags, true, false, attributionFlags, attributionChainId);
        }

        public void pause() {
            if (!isRunning()) {
                return;
            }
            if (this.mPausedInProgressEvents == null) {
                this.mPausedInProgressEvents = new ArrayMap<>(1);
            }
            for (int i = 0; i < this.mInProgressEvents.size(); i++) {
                InProgressStartOpEvent event = this.mInProgressEvents.valueAt(i);
                this.mPausedInProgressEvents.put(event.mClientId, event);
                finishOrPause(event.mClientId, true, true);
                AppOpsService.this.scheduleOpActiveChangedIfNeededLocked(this.parent.op, this.parent.uid, this.parent.packageName, this.tag, false, event.getAttributionFlags(), event.getAttributionChainId());
            }
            this.mInProgressEvents = null;
        }

        public void resume() {
            if (!isPaused()) {
                return;
            }
            if (this.mInProgressEvents == null) {
                this.mInProgressEvents = new ArrayMap<>(this.mPausedInProgressEvents.size());
            }
            boolean shouldSendActive = !this.mPausedInProgressEvents.isEmpty() && this.mInProgressEvents.isEmpty();
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < this.mPausedInProgressEvents.size(); i++) {
                InProgressStartOpEvent event = this.mPausedInProgressEvents.valueAt(i);
                this.mInProgressEvents.put(event.mClientId, event);
                event.mStartElapsedTime = SystemClock.elapsedRealtime();
                event.mStartTime = startTime;
                AppOpsService.this.mHistoricalRegistry.incrementOpAccessedCount(this.parent.op, this.parent.uid, this.parent.packageName, this.tag, event.mUidState, event.mFlags, startTime, event.getAttributionFlags(), event.getAttributionChainId());
                if (shouldSendActive) {
                    AppOpsService.this.scheduleOpActiveChangedIfNeededLocked(this.parent.op, this.parent.uid, this.parent.packageName, this.tag, true, event.getAttributionFlags(), event.getAttributionChainId());
                }
                AppOpsService.this.scheduleOpStartedIfNeededLocked(this.parent.op, this.parent.uid, this.parent.packageName, this.tag, event.getFlags(), 0, 2, event.getAttributionFlags(), event.getAttributionChainId());
            }
            this.mPausedInProgressEvents = null;
        }

        void onClientDeath(IBinder clientId) {
            synchronized (AppOpsService.this) {
                if (isPaused() || isRunning()) {
                    ArrayMap<IBinder, InProgressStartOpEvent> events = isPaused() ? this.mPausedInProgressEvents : this.mInProgressEvents;
                    InProgressStartOpEvent deadEvent = events.get(clientId);
                    if (deadEvent != null) {
                        deadEvent.numUnfinishedStarts = 1;
                    }
                    finished(clientId);
                }
            }
        }

        public void onUidStateChanged(int newState) {
            int i;
            int i2;
            ArrayMap<IBinder, InProgressStartOpEvent> events;
            ArrayMap<IBinder, InProgressStartOpEvent> arrayMap;
            if (!isPaused() && !isRunning()) {
                return;
            }
            boolean isRunning = isRunning();
            ArrayMap<IBinder, InProgressStartOpEvent> events2 = isRunning ? this.mInProgressEvents : this.mPausedInProgressEvents;
            int numInProgressEvents = events2.size();
            List<IBinder> binders = new ArrayList<>(events2.keySet());
            ArrayMap<IBinder, InProgressStartOpEvent> events3 = events2;
            int i3 = 0;
            while (i3 < numInProgressEvents) {
                InProgressStartOpEvent event = events3.get(binders.get(i3));
                if (event == null || event.getUidState() == newState) {
                    i = i3;
                    events3 = events3;
                } else {
                    try {
                        int numPreviousUnfinishedStarts = event.numUnfinishedStarts;
                        event.numUnfinishedStarts = 1;
                        AppOpsManager.OpEventProxyInfo proxy = event.getProxy();
                        finished(event.getClientId(), false);
                        if (proxy != null) {
                            try {
                                i2 = i3;
                                events = events3;
                                try {
                                    startedOrPaused(event.getClientId(), proxy.getUid(), proxy.getPackageName(), proxy.getAttributionTag(), newState, event.getFlags(), false, isRunning, event.getAttributionFlags(), event.getAttributionChainId());
                                } catch (RemoteException e) {
                                    events3 = events;
                                    i = i2;
                                }
                            } catch (RemoteException e2) {
                                i = i3;
                            }
                        } else {
                            i2 = i3;
                            events = events3;
                            startedOrPaused(event.getClientId(), -1, null, null, newState, event.getFlags(), false, isRunning, event.getAttributionFlags(), event.getAttributionChainId());
                        }
                        if (isRunning) {
                            arrayMap = this.mInProgressEvents;
                        } else {
                            try {
                                arrayMap = this.mPausedInProgressEvents;
                            } catch (RemoteException e3) {
                                i = i2;
                                events3 = events;
                            }
                        }
                        events3 = arrayMap;
                        i = i2;
                        try {
                            InProgressStartOpEvent newEvent = events3.get(binders.get(i));
                            if (newEvent != null) {
                                newEvent.numUnfinishedStarts += numPreviousUnfinishedStarts - 1;
                            }
                        } catch (RemoteException e4) {
                        }
                    } catch (RemoteException e5) {
                        i = i3;
                    }
                }
                i3 = i + 1;
            }
        }

        private LongSparseArray<AppOpsManager.NoteOpEvent> add(LongSparseArray<AppOpsManager.NoteOpEvent> a, LongSparseArray<AppOpsManager.NoteOpEvent> b) {
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            int numEventsToAdd = b.size();
            for (int i = 0; i < numEventsToAdd; i++) {
                long keyOfEventToAdd = b.keyAt(i);
                AppOpsManager.NoteOpEvent bEvent = b.valueAt(i);
                AppOpsManager.NoteOpEvent aEvent = a.get(keyOfEventToAdd);
                if (aEvent == null || bEvent.getNoteTime() > aEvent.getNoteTime()) {
                    a.put(keyOfEventToAdd, bEvent);
                }
            }
            return a;
        }

        public void add(AttributedOp opToAdd) {
            if (opToAdd.isRunning() || opToAdd.isPaused()) {
                ArrayMap<IBinder, InProgressStartOpEvent> ignoredEvents = opToAdd.isRunning() ? opToAdd.mInProgressEvents : opToAdd.mPausedInProgressEvents;
                Slog.w(AppOpsService.TAG, "Ignoring " + ignoredEvents.size() + " app-ops, running: " + opToAdd.isRunning());
                int numInProgressEvents = ignoredEvents.size();
                for (int i = 0; i < numInProgressEvents; i++) {
                    InProgressStartOpEvent event = ignoredEvents.valueAt(i);
                    event.finish();
                    AppOpsService.this.mInProgressStartOpEventPool.release(event);
                }
            }
            this.mAccessEvents = add(this.mAccessEvents, opToAdd.mAccessEvents);
            this.mRejectEvents = add(this.mRejectEvents, opToAdd.mRejectEvents);
        }

        public boolean isRunning() {
            ArrayMap<IBinder, InProgressStartOpEvent> arrayMap = this.mInProgressEvents;
            return (arrayMap == null || arrayMap.isEmpty()) ? false : true;
        }

        public boolean isPaused() {
            ArrayMap<IBinder, InProgressStartOpEvent> arrayMap = this.mPausedInProgressEvents;
            return (arrayMap == null || arrayMap.isEmpty()) ? false : true;
        }

        boolean hasAnyTime() {
            LongSparseArray<AppOpsManager.NoteOpEvent> longSparseArray;
            LongSparseArray<AppOpsManager.NoteOpEvent> longSparseArray2 = this.mAccessEvents;
            return (longSparseArray2 != null && longSparseArray2.size() > 0) || ((longSparseArray = this.mRejectEvents) != null && longSparseArray.size() > 0);
        }

        private LongSparseArray<AppOpsManager.NoteOpEvent> deepClone(LongSparseArray<AppOpsManager.NoteOpEvent> original) {
            if (original == null) {
                return original;
            }
            int size = original.size();
            LongSparseArray<AppOpsManager.NoteOpEvent> clone = new LongSparseArray<>(size);
            for (int i = 0; i < size; i++) {
                clone.put(original.keyAt(i), new AppOpsManager.NoteOpEvent(original.valueAt(i)));
            }
            return clone;
        }

        AppOpsManager.AttributedOpEntry createAttributedOpEntryLocked() {
            LongSparseArray<AppOpsManager.NoteOpEvent> accessEvents = deepClone(this.mAccessEvents);
            if (isRunning()) {
                long now = SystemClock.elapsedRealtime();
                int numInProgressEvents = this.mInProgressEvents.size();
                if (accessEvents == null) {
                    accessEvents = new LongSparseArray<>(numInProgressEvents);
                }
                for (int i = 0; i < numInProgressEvents; i++) {
                    InProgressStartOpEvent event = this.mInProgressEvents.valueAt(i);
                    accessEvents.append(AppOpsManager.makeKey(event.getUidState(), event.getFlags()), new AppOpsManager.NoteOpEvent(event.getStartTime(), now - event.getStartElapsedTime(), event.getProxy()));
                }
            }
            LongSparseArray<AppOpsManager.NoteOpEvent> rejectEvents = deepClone(this.mRejectEvents);
            return new AppOpsManager.AttributedOpEntry(this.parent.op, isRunning(), accessEvents, rejectEvents);
        }
    }

    /* loaded from: classes.dex */
    public final class Op {
        final ArrayMap<String, AttributedOp> mAttributions = new ArrayMap<>(1);
        private int mode;
        int op;
        final String packageName;
        int uid;
        final UidState uidState;

        Op(UidState uidState, String packageName, int op, int uid) {
            AppOpsService.this = this$0;
            this.op = op;
            this.uid = uid;
            this.uidState = uidState;
            this.packageName = packageName;
            this.mode = AppOpsManager.opToDefaultMode(op);
        }

        int getMode() {
            return this.mode;
        }

        int evalMode() {
            return this.uidState.evalMode(this.op, this.mode);
        }

        void removeAttributionsWithNoTime() {
            for (int i = this.mAttributions.size() - 1; i >= 0; i--) {
                if (!this.mAttributions.valueAt(i).hasAnyTime()) {
                    this.mAttributions.removeAt(i);
                }
            }
        }

        public AttributedOp getOrCreateAttribution(Op parent, String attributionTag) {
            AttributedOp attributedOp = this.mAttributions.get(attributionTag);
            if (attributedOp == null) {
                AttributedOp attributedOp2 = new AttributedOp(attributionTag, parent);
                this.mAttributions.put(attributionTag, attributedOp2);
                return attributedOp2;
            }
            return attributedOp;
        }

        AppOpsManager.OpEntry createEntryLocked() {
            int numAttributions = this.mAttributions.size();
            ArrayMap<String, AppOpsManager.AttributedOpEntry> attributionEntries = new ArrayMap<>(numAttributions);
            for (int i = 0; i < numAttributions; i++) {
                attributionEntries.put(this.mAttributions.keyAt(i), this.mAttributions.valueAt(i).createAttributedOpEntryLocked());
            }
            return new AppOpsManager.OpEntry(this.op, this.mode, attributionEntries);
        }

        AppOpsManager.OpEntry createSingleAttributionEntryLocked(String attributionTag) {
            int numAttributions = this.mAttributions.size();
            ArrayMap<String, AppOpsManager.AttributedOpEntry> attributionEntries = new ArrayMap<>(1);
            int i = 0;
            while (true) {
                if (i >= numAttributions) {
                    break;
                } else if (!Objects.equals(this.mAttributions.keyAt(i), attributionTag)) {
                    i++;
                } else {
                    attributionEntries.put(this.mAttributions.keyAt(i), this.mAttributions.valueAt(i).createAttributedOpEntryLocked());
                    break;
                }
            }
            return new AppOpsManager.OpEntry(this.op, this.mode, attributionEntries);
        }

        boolean isRunning() {
            int numAttributions = this.mAttributions.size();
            for (int i = 0; i < numAttributions; i++) {
                if (this.mAttributions.valueAt(i).isRunning()) {
                    return true;
                }
            }
            return false;
        }
    }

    /* loaded from: classes.dex */
    public final class ModeCallback implements IBinder.DeathRecipient {
        public static final int ALL_OPS = -2;
        final IAppOpsCallback mCallback;
        final int mCallingPid;
        final int mCallingUid;
        final int mFlags;
        final int mWatchedOpCode;
        final int mWatchingUid;

        ModeCallback(IAppOpsCallback callback, int watchingUid, int flags, int watchedOp, int callingUid, int callingPid) {
            AppOpsService.this = this$0;
            this.mCallback = callback;
            this.mWatchingUid = watchingUid;
            this.mFlags = flags;
            this.mWatchedOpCode = watchedOp;
            this.mCallingUid = callingUid;
            this.mCallingPid = callingPid;
            try {
                callback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public boolean isWatchingUid(int uid) {
            int i;
            return uid == -2 || (i = this.mWatchingUid) < 0 || i == uid;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ModeCallback{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" watchinguid=");
            UserHandle.formatUid(sb, this.mWatchingUid);
            sb.append(" flags=0x");
            sb.append(Integer.toHexString(this.mFlags));
            switch (this.mWatchedOpCode) {
                case -2:
                    sb.append(" op=(all)");
                    break;
                case -1:
                    break;
                default:
                    sb.append(" op=");
                    sb.append(AppOpsManager.opToName(this.mWatchedOpCode));
                    break;
            }
            sb.append(" from uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append(" pid=");
            sb.append(this.mCallingPid);
            sb.append('}');
            return sb.toString();
        }

        void unlinkToDeath() {
            this.mCallback.asBinder().unlinkToDeath(this, 0);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            AppOpsService.this.stopWatchingMode(this.mCallback);
        }
    }

    /* loaded from: classes.dex */
    public final class ActiveCallback implements IBinder.DeathRecipient {
        final IAppOpsActiveCallback mCallback;
        final int mCallingPid;
        final int mCallingUid;
        final int mWatchingUid;

        ActiveCallback(IAppOpsActiveCallback callback, int watchingUid, int callingUid, int callingPid) {
            AppOpsService.this = this$0;
            this.mCallback = callback;
            this.mWatchingUid = watchingUid;
            this.mCallingUid = callingUid;
            this.mCallingPid = callingPid;
            try {
                callback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ActiveCallback{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" watchinguid=");
            UserHandle.formatUid(sb, this.mWatchingUid);
            sb.append(" from uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append(" pid=");
            sb.append(this.mCallingPid);
            sb.append('}');
            return sb.toString();
        }

        void destroy() {
            this.mCallback.asBinder().unlinkToDeath(this, 0);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            AppOpsService.this.stopWatchingActive(this.mCallback);
        }
    }

    /* loaded from: classes.dex */
    public final class StartedCallback implements IBinder.DeathRecipient {
        final IAppOpsStartedCallback mCallback;
        final int mCallingPid;
        final int mCallingUid;
        final int mWatchingUid;

        StartedCallback(IAppOpsStartedCallback callback, int watchingUid, int callingUid, int callingPid) {
            AppOpsService.this = this$0;
            this.mCallback = callback;
            this.mWatchingUid = watchingUid;
            this.mCallingUid = callingUid;
            this.mCallingPid = callingPid;
            try {
                callback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("StartedCallback{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" watchinguid=");
            UserHandle.formatUid(sb, this.mWatchingUid);
            sb.append(" from uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append(" pid=");
            sb.append(this.mCallingPid);
            sb.append('}');
            return sb.toString();
        }

        void destroy() {
            this.mCallback.asBinder().unlinkToDeath(this, 0);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            AppOpsService.this.stopWatchingStarted(this.mCallback);
        }
    }

    /* loaded from: classes.dex */
    public final class NotedCallback implements IBinder.DeathRecipient {
        final IAppOpsNotedCallback mCallback;
        final int mCallingPid;
        final int mCallingUid;
        final int mWatchingUid;

        NotedCallback(IAppOpsNotedCallback callback, int watchingUid, int callingUid, int callingPid) {
            AppOpsService.this = this$0;
            this.mCallback = callback;
            this.mWatchingUid = watchingUid;
            this.mCallingUid = callingUid;
            this.mCallingPid = callingPid;
            try {
                callback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("NotedCallback{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" watchinguid=");
            UserHandle.formatUid(sb, this.mWatchingUid);
            sb.append(" from uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append(" pid=");
            sb.append(this.mCallingPid);
            sb.append('}');
            return sb.toString();
        }

        void destroy() {
            this.mCallback.asBinder().unlinkToDeath(this, 0);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            AppOpsService.this.stopWatchingNoted(this.mCallback);
        }
    }

    public static void onClientDeath(AttributedOp attributedOp, IBinder clientId) {
        attributedOp.onClientDeath(clientId);
    }

    private void readNoteOpCallerStackTraces() {
        try {
            if (!this.mNoteOpCallerStacktracesFile.exists()) {
                this.mNoteOpCallerStacktracesFile.createNewFile();
                return;
            }
            Scanner read = new Scanner(this.mNoteOpCallerStacktracesFile);
            read.useDelimiter("\\},");
            while (read.hasNext()) {
                String jsonOps = read.next();
                this.mNoteOpCallerStacktraces.add(NoteOpTrace.fromJson(jsonOps));
            }
            read.close();
        } catch (Exception e) {
            Slog.e(TAG, "Cannot parse traces noteOps", e);
        }
    }

    public AppOpsService(File storagePath, Handler handler, Context context) {
        this.mContext = context;
        LockGuard.installLock(this, 0);
        this.mFile = new AtomicFile(storagePath, "appops");
        this.mNoteOpCallerStacktracesFile = null;
        this.mHandler = handler;
        this.mConstants = new Constants(handler);
        readState();
        CtaManager ctaManager = sCtaManager;
        int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
        for (int switchedCode = 0; switchedCode < opNum; switchedCode++) {
            int switchCode = AppOpsManager.opToSwitch(switchedCode);
            SparseArray<int[]> sparseArray = this.mSwitchedOps;
            sparseArray.put(switchCode, ArrayUtils.appendInt(sparseArray.get(switchCode), switchedCode));
        }
    }

    public void publish() {
        ServiceManager.addService("appops", asBinder());
        LocalServices.addService(AppOpsManagerInternal.class, this.mAppOpsManagerInternal);
    }

    public void systemReady() {
        final String action;
        this.mConstants.startMonitoring(this.mContext.getContentResolver());
        this.mHistoricalRegistry.systemReady(this.mContext.getContentResolver());
        IntentFilter packageUpdateFilter = new IntentFilter();
        packageUpdateFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageUpdateFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        packageUpdateFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mOnPackageUpdatedReceiver, UserHandle.ALL, packageUpdateFilter, null, null);
        synchronized (this) {
            for (int uidNum = this.mUidStates.size() - 1; uidNum >= 0; uidNum--) {
                final int uid = this.mUidStates.keyAt(uidNum);
                UidState uidState = this.mUidStates.valueAt(uidNum);
                String[] pkgsInUid = getPackagesForUid(uidState.uid);
                if (ArrayUtils.isEmpty(pkgsInUid)) {
                    uidState.clear();
                    this.mUidStates.removeAt(uidNum);
                    scheduleFastWriteLocked();
                } else {
                    ArrayMap<String, Ops> pkgs = uidState.pkgOps;
                    if (pkgs != null) {
                        int numPkgs = pkgs.size();
                        for (int pkgNum = 0; pkgNum < numPkgs; pkgNum++) {
                            final String pkg = pkgs.keyAt(pkgNum);
                            if (!ArrayUtils.contains(pkgsInUid, pkg)) {
                                action = "android.intent.action.PACKAGE_REMOVED";
                            } else {
                                action = "android.intent.action.PACKAGE_REPLACED";
                            }
                            SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda9
                                @Override // java.lang.Runnable
                                public final void run() {
                                    AppOpsService.this.m1643lambda$systemReady$0$comandroidserverappopAppOpsService(action, pkg, uid);
                                }
                            }, "Update app-ops uidState in case package " + pkg + " changed");
                        }
                    }
                }
            }
        }
        IntentFilter packageSuspendFilter = new IntentFilter();
        packageSuspendFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        packageSuspendFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.appop.AppOpsService.3
            {
                AppOpsService.this = this;
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int[] iArr;
                int[] changedUids = intent.getIntArrayExtra("android.intent.extra.changed_uid_list");
                String[] changedPkgs = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                for (int code : AppOpsService.OPS_RESTRICTED_ON_SUSPEND) {
                    synchronized (AppOpsService.this) {
                        ArraySet<ModeCallback> callbacks = AppOpsService.this.mOpModeWatchers.get(code);
                        if (callbacks != null) {
                            ArraySet<ModeCallback> callbacks2 = new ArraySet<>(callbacks);
                            for (int i = 0; i < changedUids.length; i++) {
                                int changedUid = changedUids[i];
                                String changedPkg = changedPkgs[i];
                                AppOpsService.this.notifyOpChanged(callbacks2, code, changedUid, changedPkg);
                            }
                        }
                    }
                }
            }
        }, UserHandle.ALL, packageSuspendFilter, null, null);
        IntentFilter packageAddedFilter = new IntentFilter();
        packageAddedFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageAddedFilter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.appop.AppOpsService.4
            {
                AppOpsService.this = this;
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                Uri data = intent.getData();
                String packageName = data.getSchemeSpecificPart();
                PackageInfo pi = AppOpsService.this.getPackageManagerInternal().getPackageInfo(packageName, 4096L, Process.myUid(), AppOpsService.this.mContext.getUserId());
                if (AppOpsService.this.isSamplingTarget(pi)) {
                    synchronized (this) {
                        AppOpsService.this.mRarelyUsedPackages.add(packageName);
                    }
                }
            }
        }, packageAddedFilter);
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.appop.AppOpsService.5
            {
                AppOpsService.this = this;
            }

            @Override // java.lang.Runnable
            public void run() {
                List<String> packageNames = AppOpsService.this.getPackageListAndResample();
                AppOpsService.this.initializeRarelyUsedPackagesList(new ArraySet(packageNames));
            }
        }, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
        getPackageManagerInternal().setExternalSourcesPolicy(new PackageManagerInternal.ExternalSourcesPolicy() { // from class: com.android.server.appop.AppOpsService.6
            {
                AppOpsService.this = this;
            }

            @Override // android.content.pm.PackageManagerInternal.ExternalSourcesPolicy
            public int getPackageTrustedToInstallApps(String packageName, int uid2) {
                int appOpMode = AppOpsService.this.checkOperation(66, uid2, packageName);
                switch (appOpMode) {
                    case 0:
                        return 0;
                    case 1:
                    default:
                        return 2;
                    case 2:
                        return 1;
                }
            }
        });
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
    }

    /* renamed from: lambda$systemReady$0$com-android-server-appop-AppOpsService */
    public /* synthetic */ void m1643lambda$systemReady$0$comandroidserverappopAppOpsService(String action, String pkg, int uid) {
        this.mOnPackageUpdatedReceiver.onReceive(this.mContext, new Intent(action).setData(Uri.fromParts("package", pkg, null)).putExtra("android.intent.extra.UID", uid));
    }

    public void setAppOpsPolicy(AppOpsManagerInternal.CheckOpsDelegate policy) {
        CheckOpsDelegateDispatcher oldDispatcher = this.mCheckOpsDelegateDispatcher;
        AppOpsManagerInternal.CheckOpsDelegate delegate = oldDispatcher != null ? oldDispatcher.mCheckOpsDelegate : null;
        this.mCheckOpsDelegateDispatcher = new CheckOpsDelegateDispatcher(policy, delegate);
    }

    public void packageRemoved(int uid, String packageName) {
        synchronized (this) {
            UidState uidState = this.mUidStates.get(uid);
            if (uidState == null) {
                return;
            }
            Ops ops = null;
            if (uidState.pkgOps != null) {
                ops = uidState.pkgOps.remove(packageName);
            }
            if (ops != null && uidState.pkgOps.isEmpty() && getPackagesForUid(uid).length <= 0) {
                this.mUidStates.remove(uid);
            }
            if (ops != null) {
                scheduleFastWriteLocked();
                int numOps = ops.size();
                for (int opNum = 0; opNum < numOps; opNum++) {
                    Op op = ops.valueAt(opNum);
                    int numAttributions = op.mAttributions.size();
                    for (int attributionNum = 0; attributionNum < numAttributions; attributionNum++) {
                        AttributedOp attributedOp = op.mAttributions.valueAt(attributionNum);
                        while (attributedOp.isRunning()) {
                            attributedOp.finished((IBinder) attributedOp.mInProgressEvents.keyAt(0));
                        }
                        while (attributedOp.isPaused()) {
                            attributedOp.finished((IBinder) attributedOp.mPausedInProgressEvents.keyAt(0));
                        }
                    }
                }
            }
            this.mHandler.post(PooledLambda.obtainRunnable(new TriConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda13
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((HistoricalRegistry) obj).clearHistory(((Integer) obj2).intValue(), (String) obj3);
                }
            }, this.mHistoricalRegistry, Integer.valueOf(uid), packageName));
        }
    }

    public void uidRemoved(int uid) {
        synchronized (this) {
            if (this.mUidStates.indexOfKey(uid) >= 0) {
                this.mUidStates.remove(uid);
                scheduleFastWriteLocked();
            }
        }
    }

    public void updatePendingState(long currentTime, int uid) {
        synchronized (this) {
            this.mLastRealtime = Long.max(currentTime, this.mLastRealtime);
            updatePendingStateIfNeededLocked(this.mUidStates.get(uid));
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:91:0x0085 A[Catch: all -> 0x00c2, TryCatch #0 {, blocks: (B:60:0x0008, B:62:0x0012, B:64:0x0016, B:66:0x001a, B:70:0x0028, B:73:0x002d, B:75:0x0031, B:77:0x0035, B:89:0x0081, B:91:0x0085, B:93:0x008e, B:95:0x009d, B:97:0x00ac, B:98:0x00ba, B:99:0x00bd, B:78:0x0039, B:80:0x0041, B:82:0x0047, B:87:0x005b, B:83:0x004c, B:85:0x0052, B:86:0x0057, B:88:0x007e, B:100:0x00c0), top: B:105:0x0008 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void updateUidProcState(int uid, int procState, int capability) {
        long settleTime;
        synchronized (this) {
            UidState uidState = getUidStateLocked(uid, true);
            int newState = PROCESS_STATE_TO_UID_STATE[procState];
            if (uidState != null && (uidState.pendingState != newState || uidState.pendingCapability != capability)) {
                int i = uidState.pendingState;
                uidState.pendingState = newState;
                uidState.pendingCapability = capability;
                if (newState >= uidState.state && (newState > 500 || uidState.state <= 500)) {
                    if (newState == uidState.state && capability != uidState.capability) {
                        commitUidPendingStateLocked(uidState);
                    } else if (uidState.pendingStateCommitTime == 0) {
                        if (uidState.state <= 200) {
                            settleTime = this.mConstants.TOP_STATE_SETTLE_TIME;
                        } else if (uidState.state <= 400) {
                            settleTime = this.mConstants.FG_SERVICE_STATE_SETTLE_TIME;
                        } else {
                            settleTime = this.mConstants.BG_STATE_SETTLE_TIME;
                        }
                        long commitTime = SystemClock.elapsedRealtime() + settleTime;
                        uidState.pendingStateCommitTime = commitTime;
                        this.mHandler.sendMessageDelayed(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda1
                            public final void accept(Object obj, Object obj2, Object obj3) {
                                ((AppOpsService) obj).updatePendingState(((Long) obj2).longValue(), ((Integer) obj3).intValue());
                            }
                        }, this, Long.valueOf(commitTime + 1), Integer.valueOf(uid)), 1 + settleTime);
                    }
                    if (uidState.pkgOps != null) {
                        int numPkgs = uidState.pkgOps.size();
                        for (int pkgNum = 0; pkgNum < numPkgs; pkgNum++) {
                            Ops ops = uidState.pkgOps.valueAt(pkgNum);
                            int numOps = ops.size();
                            for (int opNum = 0; opNum < numOps; opNum++) {
                                Op op = ops.valueAt(opNum);
                                int numAttributions = op.mAttributions.size();
                                for (int attributionNum = 0; attributionNum < numAttributions; attributionNum++) {
                                    AttributedOp attributedOp = op.mAttributions.valueAt(attributionNum);
                                    attributedOp.onUidStateChanged(newState);
                                }
                            }
                        }
                    }
                }
                commitUidPendingStateLocked(uidState);
                if (uidState.pkgOps != null) {
                }
            }
        }
    }

    public void shutdown() {
        Slog.w(TAG, "Writing app ops before shutdown...");
        boolean doWrite = false;
        synchronized (this) {
            if (this.mWriteScheduled) {
                this.mWriteScheduled = false;
                this.mFastWriteScheduled = false;
                this.mHandler.removeCallbacks(this.mWriteRunner);
                doWrite = true;
            }
        }
        if (doWrite) {
            writeState();
        }
        this.mHistoricalRegistry.shutdown();
    }

    private ArrayList<AppOpsManager.OpEntry> collectOps(Ops pkgOps, int[] ops) {
        ArrayList<AppOpsManager.OpEntry> resOps = null;
        long elapsedNow = SystemClock.elapsedRealtime();
        if (ops == null) {
            resOps = new ArrayList<>();
            for (int j = 0; j < pkgOps.size(); j++) {
                resOps.add(getOpEntryForResult(pkgOps.valueAt(j), elapsedNow));
            }
        } else {
            for (int i : ops) {
                Op curOp = pkgOps.get(i);
                if (curOp != null) {
                    if (resOps == null) {
                        resOps = new ArrayList<>();
                    }
                    resOps.add(getOpEntryForResult(curOp, elapsedNow));
                }
            }
        }
        return resOps;
    }

    private ArrayList<AppOpsManager.OpEntry> collectUidOps(UidState uidState, int[] ops) {
        int opModeCount;
        if (uidState.opModes == null || (opModeCount = uidState.opModes.size()) == 0) {
            return null;
        }
        ArrayList<AppOpsManager.OpEntry> resOps = null;
        if (ops == null) {
            resOps = new ArrayList<>();
            for (int i = 0; i < opModeCount; i++) {
                int code = uidState.opModes.keyAt(i);
                resOps.add(new AppOpsManager.OpEntry(code, uidState.opModes.get(code), Collections.emptyMap()));
            }
        } else {
            for (int code2 : ops) {
                if (uidState.opModes.indexOfKey(code2) >= 0) {
                    if (resOps == null) {
                        resOps = new ArrayList<>();
                    }
                    resOps.add(new AppOpsManager.OpEntry(code2, uidState.opModes.get(code2), Collections.emptyMap()));
                }
            }
        }
        return resOps;
    }

    private static AppOpsManager.OpEntry getOpEntryForResult(Op op, long elapsedNow) {
        return op.createEntryLocked();
    }

    public List<AppOpsManager.PackageOps> getPackagesForOps(int[] ops) {
        int callingUid = Binder.getCallingUid();
        boolean hasAllPackageAccess = this.mContext.checkPermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null) == 0;
        ArrayList<AppOpsManager.PackageOps> res = null;
        synchronized (this) {
            int uidStateCount = this.mUidStates.size();
            for (int i = 0; i < uidStateCount; i++) {
                UidState uidState = this.mUidStates.valueAt(i);
                if (uidState.pkgOps != null && !uidState.pkgOps.isEmpty()) {
                    ArrayMap<String, Ops> packages = uidState.pkgOps;
                    int packageCount = packages.size();
                    for (int j = 0; j < packageCount; j++) {
                        Ops pkgOps = packages.valueAt(j);
                        ArrayList<AppOpsManager.OpEntry> resOps = collectOps(pkgOps, ops);
                        if (resOps != null) {
                            if (res == null) {
                                res = new ArrayList<>();
                            }
                            AppOpsManager.PackageOps resPackage = new AppOpsManager.PackageOps(pkgOps.packageName, pkgOps.uidState.uid, resOps);
                            if (hasAllPackageAccess || callingUid == pkgOps.uidState.uid) {
                                res.add(resPackage);
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public List<AppOpsManager.PackageOps> getOpsForPackage(int uid, String packageName, int[] ops) {
        enforceGetAppOpsStatsPermissionIfNeeded(uid, packageName);
        String resolvedPackageName = AppOpsManager.resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return Collections.emptyList();
        }
        synchronized (this) {
            Ops pkgOps = getOpsLocked(uid, resolvedPackageName, null, false, null, false);
            if (pkgOps == null) {
                return null;
            }
            ArrayList<AppOpsManager.OpEntry> resOps = collectOps(pkgOps, ops);
            if (resOps == null) {
                return null;
            }
            ArrayList<AppOpsManager.PackageOps> res = new ArrayList<>();
            AppOpsManager.PackageOps resPackage = new AppOpsManager.PackageOps(pkgOps.packageName, pkgOps.uidState.uid, resOps);
            res.add(resPackage);
            return res;
        }
    }

    private void enforceGetAppOpsStatsPermissionIfNeeded(int uid, String packageName) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myPid()) {
            return;
        }
        if (uid == callingUid && packageName != null && checkPackage(uid, packageName) == 0) {
            return;
        }
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), callingUid, null);
    }

    private void ensureHistoricalOpRequestIsValid(int uid, String packageName, String attributionTag, List<String> opNames, int filter, long beginTimeMillis, long endTimeMillis, int flags) {
        if ((filter & 1) != 0) {
            Preconditions.checkArgument(uid != -1);
        } else {
            Preconditions.checkArgument(uid == -1);
        }
        if ((filter & 2) != 0) {
            Objects.requireNonNull(packageName);
        } else {
            Preconditions.checkArgument(packageName == null);
        }
        if ((filter & 4) == 0) {
            Preconditions.checkArgument(attributionTag == null);
        }
        if ((filter & 8) != 0) {
            Objects.requireNonNull(opNames);
        } else {
            Preconditions.checkArgument(opNames == null);
        }
        Preconditions.checkFlagsArgument(filter, 15);
        Preconditions.checkArgumentNonnegative(beginTimeMillis);
        Preconditions.checkArgument(endTimeMillis > beginTimeMillis);
        Preconditions.checkFlagsArgument(flags, 31);
    }

    /* JADX WARN: Removed duplicated region for block: B:61:0x0046  */
    /* JADX WARN: Removed duplicated region for block: B:88:0x00c0  */
    /* JADX WARN: Removed duplicated region for block: B:89:0x00cf  */
    /* JADX WARN: Removed duplicated region for block: B:92:0x00d6  */
    /* JADX WARN: Removed duplicated region for block: B:94:0x00df  */
    /* JADX WARN: Removed duplicated region for block: B:95:0x00ee  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void getHistoricalOps(int uid, String packageName, String attributionTag, List<String> opNames, int dataType, int filter, long beginTimeMillis, long endTimeMillis, int flags, final RemoteCallback callback) {
        boolean z;
        boolean isSelfRequest;
        String[] opNamesArray;
        Set<String> attributionChainExemptPackages;
        String[] chainExemptPkgArray;
        PackageManager pm = this.mContext.getPackageManager();
        ensureHistoricalOpRequestIsValid(uid, packageName, attributionTag, opNames, filter, beginTimeMillis, endTimeMillis, flags);
        Objects.requireNonNull(callback, "callback cannot be null");
        ActivityManagerInternal ami = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        if ((filter & 1) != 0 && uid == Binder.getCallingUid()) {
            z = true;
            isSelfRequest = z;
            if (!isSelfRequest) {
                boolean isCallerInstrumented = ami.getInstrumentationSourceUid(Binder.getCallingUid()) != -1;
                boolean isCallerSystem = Binder.getCallingPid() == Process.myPid();
                try {
                    boolean isCallerPermissionController = pm.getPackageUidAsUser(this.mContext.getPackageManager().getPermissionControllerPackageName(), 0, UserHandle.getUserId(Binder.getCallingUid())) == Binder.getCallingUid();
                    boolean doesCallerHavePermission = this.mContext.checkPermission("android.permission.GET_HISTORICAL_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid()) == 0;
                    if (!isCallerSystem && !isCallerInstrumented && !isCallerPermissionController && !doesCallerHavePermission) {
                        this.mHandler.post(new Runnable() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda3
                            @Override // java.lang.Runnable
                            public final void run() {
                                callback.sendResult(new Bundle());
                            }
                        });
                        return;
                    }
                    this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), "getHistoricalOps");
                } catch (PackageManager.NameNotFoundException e) {
                    return;
                }
            }
            if (opNames != null) {
                opNamesArray = null;
            } else {
                opNamesArray = (String[]) opNames.toArray(new String[opNames.size()]);
            }
            attributionChainExemptPackages = null;
            if ((dataType & 4) != 0) {
                attributionChainExemptPackages = PermissionManager.getIndicatorExemptedPackages(this.mContext);
            }
            if (attributionChainExemptPackages == null) {
                chainExemptPkgArray = (String[]) attributionChainExemptPackages.toArray(new String[attributionChainExemptPackages.size()]);
            } else {
                chainExemptPkgArray = null;
            }
            this.mHandler.post(PooledLambda.obtainRunnable(new DodecConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda4
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11, Object obj12) {
                    ((HistoricalRegistry) obj).getHistoricalOps(((Integer) obj2).intValue(), (String) obj3, (String) obj4, (String[]) obj5, ((Integer) obj6).intValue(), ((Integer) obj7).intValue(), ((Long) obj8).longValue(), ((Long) obj9).longValue(), ((Integer) obj10).intValue(), (String[]) obj11, (RemoteCallback) obj12);
                }
            }, this.mHistoricalRegistry, Integer.valueOf(uid), packageName, attributionTag, opNamesArray, Integer.valueOf(dataType), Integer.valueOf(filter), Long.valueOf(beginTimeMillis), Long.valueOf(endTimeMillis), Integer.valueOf(flags), chainExemptPkgArray, callback).recycleOnUse());
        }
        z = false;
        isSelfRequest = z;
        if (!isSelfRequest) {
        }
        if (opNames != null) {
        }
        attributionChainExemptPackages = null;
        if ((dataType & 4) != 0) {
        }
        if (attributionChainExemptPackages == null) {
        }
        this.mHandler.post(PooledLambda.obtainRunnable(new DodecConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda4
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11, Object obj12) {
                ((HistoricalRegistry) obj).getHistoricalOps(((Integer) obj2).intValue(), (String) obj3, (String) obj4, (String[]) obj5, ((Integer) obj6).intValue(), ((Integer) obj7).intValue(), ((Long) obj8).longValue(), ((Long) obj9).longValue(), ((Integer) obj10).intValue(), (String[]) obj11, (RemoteCallback) obj12);
            }
        }, this.mHistoricalRegistry, Integer.valueOf(uid), packageName, attributionTag, opNamesArray, Integer.valueOf(dataType), Integer.valueOf(filter), Long.valueOf(beginTimeMillis), Long.valueOf(endTimeMillis), Integer.valueOf(flags), chainExemptPkgArray, callback).recycleOnUse());
    }

    public void getHistoricalOpsFromDiskRaw(int uid, String packageName, String attributionTag, List<String> opNames, int dataType, int filter, long beginTimeMillis, long endTimeMillis, int flags, RemoteCallback callback) {
        String[] opNamesArray;
        String[] chainExemptPkgArray;
        ensureHistoricalOpRequestIsValid(uid, packageName, attributionTag, opNames, filter, beginTimeMillis, endTimeMillis, flags);
        Objects.requireNonNull(callback, "callback cannot be null");
        this.mContext.enforcePermission("android.permission.MANAGE_APPOPS", Binder.getCallingPid(), Binder.getCallingUid(), "getHistoricalOps");
        if (opNames == null) {
            opNamesArray = null;
        } else {
            opNamesArray = (String[]) opNames.toArray(new String[opNames.size()]);
        }
        Set<String> attributionChainExemptPackages = null;
        if ((dataType & 4) != 0) {
            attributionChainExemptPackages = PermissionManager.getIndicatorExemptedPackages(this.mContext);
        }
        if (attributionChainExemptPackages != null) {
            chainExemptPkgArray = (String[]) attributionChainExemptPackages.toArray(new String[attributionChainExemptPackages.size()]);
        } else {
            chainExemptPkgArray = null;
        }
        this.mHandler.post(PooledLambda.obtainRunnable(new DodecConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda8
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11, Object obj12) {
                ((HistoricalRegistry) obj).getHistoricalOpsFromDiskRaw(((Integer) obj2).intValue(), (String) obj3, (String) obj4, (String[]) obj5, ((Integer) obj6).intValue(), ((Integer) obj7).intValue(), ((Long) obj8).longValue(), ((Long) obj9).longValue(), ((Integer) obj10).intValue(), (String[]) obj11, (RemoteCallback) obj12);
            }
        }, this.mHistoricalRegistry, Integer.valueOf(uid), packageName, attributionTag, opNamesArray, Integer.valueOf(dataType), Integer.valueOf(filter), Long.valueOf(beginTimeMillis), Long.valueOf(endTimeMillis), Integer.valueOf(flags), chainExemptPkgArray, callback).recycleOnUse());
    }

    public void reloadNonHistoricalState() {
        this.mContext.enforcePermission("android.permission.MANAGE_APPOPS", Binder.getCallingPid(), Binder.getCallingUid(), "reloadNonHistoricalState");
        writeState();
        readState();
    }

    public List<AppOpsManager.PackageOps> getUidOps(int uid, int[] ops) {
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        synchronized (this) {
            UidState uidState = getUidStateLocked(uid, false);
            if (uidState == null) {
                return null;
            }
            ArrayList<AppOpsManager.OpEntry> resOps = collectUidOps(uidState, ops);
            if (resOps == null) {
                return null;
            }
            ArrayList<AppOpsManager.PackageOps> res = new ArrayList<>();
            AppOpsManager.PackageOps resPackage = new AppOpsManager.PackageOps((String) null, uidState.uid, resOps);
            res.add(resPackage);
            return res;
        }
    }

    private void pruneOpLocked(Op op, int uid, String packageName) {
        Ops ops;
        UidState uidState;
        ArrayMap<String, Ops> pkgOps;
        op.removeAttributionsWithNoTime();
        if (op.mAttributions.isEmpty() && (ops = getOpsLocked(uid, packageName, null, false, null, false)) != null) {
            ops.remove(op.op);
            if (ops.size() <= 0 && (pkgOps = (uidState = ops.uidState).pkgOps) != null) {
                pkgOps.remove(ops.packageName);
                if (pkgOps.isEmpty()) {
                    uidState.pkgOps = null;
                }
                if (uidState.isDefault()) {
                    this.mUidStates.remove(uid);
                }
            }
        }
    }

    private void enforceManageAppOpsModes(int callingPid, int callingUid, int targetUid) {
        if (callingPid == Process.myPid()) {
            return;
        }
        int callingUser = UserHandle.getUserId(callingUid);
        synchronized (this) {
            SparseIntArray sparseIntArray = this.mProfileOwners;
            if (sparseIntArray == null || sparseIntArray.get(callingUser, -1) != callingUid || targetUid < 0 || callingUser != UserHandle.getUserId(targetUid)) {
                this.mContext.enforcePermission("android.permission.MANAGE_APP_OPS_MODES", Binder.getCallingPid(), Binder.getCallingUid(), null);
            }
        }
    }

    public void setUidMode(int code, int uid, int mode) {
        setUidMode(code, uid, mode, null);
    }

    public void setUidMode(int code, int uid, int mode, IAppOpsCallback permissionPolicyCallback) {
        int previousMode;
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), uid);
        verifyIncomingOp(code);
        int code2 = AppOpsManager.opToSwitch(code);
        if (permissionPolicyCallback == null) {
            updatePermissionRevokedCompat(uid, code2, mode);
        }
        synchronized (this) {
            int defaultMode = AppOpsManager.opToDefaultMode(code2);
            UidState uidState = getUidStateLocked(uid, false);
            if (uidState == null) {
                if (mode == defaultMode) {
                    return;
                }
                uidState = new UidState(uid);
                uidState.opModes = new SparseIntArray();
                uidState.opModes.put(code2, mode);
                this.mUidStates.put(uid, uidState);
                scheduleWriteLocked();
                previousMode = 3;
            } else if (uidState.opModes == null) {
                if (mode != defaultMode) {
                    uidState.opModes = new SparseIntArray();
                    uidState.opModes.put(code2, mode);
                    scheduleWriteLocked();
                }
                previousMode = 3;
            } else if (uidState.opModes.indexOfKey(code2) >= 0 && uidState.opModes.get(code2) == mode) {
                return;
            } else {
                int previousMode2 = uidState.opModes.get(code2);
                if (mode == defaultMode) {
                    uidState.opModes.delete(code2);
                    if (uidState.opModes.size() <= 0) {
                        uidState.opModes = null;
                    }
                } else {
                    uidState.opModes.put(code2, mode);
                }
                scheduleWriteLocked();
                previousMode = previousMode2;
            }
            uidState.evalForegroundOps(this.mOpModeWatchers);
            if (mode != 2 && mode != previousMode) {
                boolean z = true;
                if (mode != 1) {
                    z = false;
                }
                updateStartedOpModeForUidLocked(code2, z, uid);
            }
            notifyOpChangedForAllPkgsInUid(code2, uid, false, permissionPolicyCallback);
            notifyOpChangedSync(code2, uid, null, mode, previousMode);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2658=5] */
    public void notifyOpChangedForAllPkgsInUid(int code, int uid, boolean onlyForeground, IAppOpsCallback callbackToIgnore) {
        String[] uidPackageNames = getPackagesForUid(uid);
        ArrayMap<ModeCallback, ArraySet<String>> callbackSpecs = null;
        synchronized (this) {
            try {
                try {
                    ArraySet<ModeCallback> callbacks = this.mOpModeWatchers.get(code);
                    if (callbacks != null) {
                        try {
                            int callbackCount = callbacks.size();
                            for (int i = 0; i < callbackCount; i++) {
                                ModeCallback callback = callbacks.valueAt(i);
                                if (!onlyForeground || (callback.mFlags & 1) != 0) {
                                    ArraySet<String> changedPackages = new ArraySet<>();
                                    Collections.addAll(changedPackages, uidPackageNames);
                                    if (callbackSpecs == null) {
                                        callbackSpecs = new ArrayMap<>();
                                    }
                                    callbackSpecs.put(callback, changedPackages);
                                }
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
                    ArrayMap<ModeCallback, ArraySet<String>> callbackSpecs2 = callbackSpecs;
                    for (String uidPackageName : uidPackageNames) {
                        try {
                            ArraySet<ModeCallback> callbacks2 = this.mPackageModeWatchers.get(uidPackageName);
                            if (callbacks2 != null) {
                                ArrayMap<ModeCallback, ArraySet<String>> callbackSpecs3 = callbackSpecs2 == null ? new ArrayMap<>() : callbackSpecs2;
                                try {
                                    int callbackCount2 = callbacks2.size();
                                    for (int i2 = 0; i2 < callbackCount2; i2++) {
                                        ModeCallback callback2 = callbacks2.valueAt(i2);
                                        if (!onlyForeground || (callback2.mFlags & 1) != 0) {
                                            ArraySet<String> changedPackages2 = callbackSpecs3.get(callback2);
                                            if (changedPackages2 == null) {
                                                changedPackages2 = new ArraySet<>();
                                                callbackSpecs3.put(callback2, changedPackages2);
                                            }
                                            changedPackages2.add(uidPackageName);
                                        }
                                    }
                                    callbackSpecs2 = callbackSpecs3;
                                } catch (Throwable th3) {
                                    th = th3;
                                    while (true) {
                                        break;
                                        break;
                                    }
                                    throw th;
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                        }
                    }
                    if (callbackSpecs2 != null && callbackToIgnore != null) {
                        callbackSpecs2.remove(this.mModeWatchers.get(callbackToIgnore.asBinder()));
                    }
                    try {
                        if (callbackSpecs2 == null) {
                            return;
                        }
                        int i3 = 0;
                        while (i3 < callbackSpecs2.size()) {
                            ModeCallback callback3 = callbackSpecs2.keyAt(i3);
                            ArraySet<String> reportedPackageNames = callbackSpecs2.valueAt(i3);
                            if (reportedPackageNames == null) {
                                this.mHandler.sendMessage(PooledLambda.obtainMessage(new AppOpsService$$ExternalSyntheticLambda5(), this, callback3, Integer.valueOf(code), Integer.valueOf(uid), (Object) null));
                            } else {
                                int reportedPackageCount = reportedPackageNames.size();
                                int j = 0;
                                while (j < reportedPackageCount) {
                                    String reportedPackageName = reportedPackageNames.valueAt(j);
                                    this.mHandler.sendMessage(PooledLambda.obtainMessage(new AppOpsService$$ExternalSyntheticLambda5(), this, callback3, Integer.valueOf(code), Integer.valueOf(uid), reportedPackageName));
                                    j++;
                                    uidPackageNames = uidPackageNames;
                                }
                            }
                            i3++;
                            uidPackageNames = uidPackageNames;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                } catch (Throwable th6) {
                    th = th6;
                }
            } catch (Throwable th7) {
                th = th7;
            }
        }
    }

    private void updatePermissionRevokedCompat(int uid, int switchCode, int mode) {
        int i;
        int i2;
        int[] ops;
        String packageName;
        String[] packageNames;
        String permissionName;
        String str;
        String str2;
        String str3;
        boolean isRevokedCompat;
        String permissionName2;
        long identity;
        boolean isBackgroundRevokedCompat;
        String str4;
        PermissionInfo permissionInfo;
        String str5;
        String str6;
        PermissionInfo permissionInfo2;
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager == null) {
            return;
        }
        String[] packageNames2 = packageManager.getPackagesForUid(uid);
        if (ArrayUtils.isEmpty(packageNames2)) {
            return;
        }
        int i3 = 0;
        String packageName2 = packageNames2[0];
        int[] ops2 = this.mSwitchedOps.get(switchCode);
        int length = ops2.length;
        int i4 = 0;
        while (i4 < length) {
            int code = ops2[i4];
            String permissionName3 = AppOpsManager.opToPermission(code);
            if (permissionName3 == null) {
                i = i4;
                i2 = length;
                ops = ops2;
                packageName = packageName2;
                packageNames = packageNames2;
            } else if (packageManager.checkPermission(permissionName3, packageName2) != 0) {
                i = i4;
                i2 = length;
                ops = ops2;
                packageName = packageName2;
                packageNames = packageNames2;
            } else {
                try {
                    PermissionInfo permissionInfo3 = packageManager.getPermissionInfo(permissionName3, i3);
                    if (!permissionInfo3.isRuntime()) {
                        i = i4;
                        i2 = length;
                        ops = ops2;
                        packageName = packageName2;
                        packageNames = packageNames2;
                    } else {
                        int i5 = getPackageManagerInternal().getUidTargetSdkVersion(uid) >= 23 ? 1 : i3;
                        UserHandle user = UserHandle.getUserHandleForUid(uid);
                        packageNames = packageNames2;
                        if (permissionInfo3.backgroundPermission != null) {
                            if (packageManager.checkPermission(permissionInfo3.backgroundPermission, packageName2) != 0) {
                                permissionName = permissionName3;
                                i = i4;
                                i2 = length;
                                ops = ops2;
                                packageName = packageName2;
                                str = TAG;
                                str2 = ", mode=";
                                str3 = ", permission=";
                            } else {
                                boolean isBackgroundRevokedCompat2 = mode != 0;
                                if (!sCtaManager.isCtaSupported() || i5 != 0) {
                                    isBackgroundRevokedCompat = isBackgroundRevokedCompat2;
                                    str4 = ", permission=";
                                    permissionInfo = permissionInfo3;
                                    permissionName = permissionName3;
                                    i = i4;
                                    i2 = length;
                                    ops = ops2;
                                    packageName = packageName2;
                                    str5 = TAG;
                                    str2 = ", mode=";
                                } else {
                                    PackageManagerInternal packageManagerInternal = getPackageManagerInternal();
                                    String str7 = permissionInfo3.backgroundPermission;
                                    str5 = TAG;
                                    str2 = ", mode=";
                                    str4 = ", permission=";
                                    permissionInfo = permissionInfo3;
                                    permissionName = permissionName3;
                                    i = i4;
                                    i2 = length;
                                    ops = ops2;
                                    packageName = packageName2;
                                    isBackgroundRevokedCompat = isBackgroundRevokedCompat(packageManagerInternal, packageManager, uid, packageName2, permissionName, str7, mode);
                                }
                                if (!isBackgroundRevokedCompat || i5 == 0) {
                                    str6 = str5;
                                    str3 = str4;
                                    permissionInfo2 = permissionInfo;
                                } else {
                                    str3 = str4;
                                    permissionInfo2 = permissionInfo;
                                    str6 = str5;
                                    Slog.w(str6, "setUidMode() called with a mode inconsistent with runtime permission state, this is discouraged and you should revoke the runtime permission instead: uid=" + uid + ", switchCode=" + switchCode + str2 + mode + str3 + permissionInfo2.backgroundPermission);
                                }
                                identity = Binder.clearCallingIdentity();
                                try {
                                    str = str6;
                                    packageManager.updatePermissionFlags(permissionInfo2.backgroundPermission, packageName, 8, isBackgroundRevokedCompat ? 8 : 0, user);
                                    Binder.restoreCallingIdentity(identity);
                                } finally {
                                }
                            }
                            isRevokedCompat = (mode == 0 || mode == 4) ? false : false;
                        } else {
                            permissionName = permissionName3;
                            i = i4;
                            i2 = length;
                            ops = ops2;
                            packageName = packageName2;
                            str = TAG;
                            str2 = ", mode=";
                            str3 = ", permission=";
                            isRevokedCompat = mode != 0;
                        }
                        if (!isRevokedCompat || i5 == 0) {
                            permissionName2 = permissionName;
                        } else {
                            StringBuilder append = new StringBuilder().append("setUidMode() called with a mode inconsistent with runtime permission state, this is discouraged and you should revoke the runtime permission instead: uid=").append(uid).append(", switchCode=").append(switchCode).append(str2).append(mode).append(str3);
                            permissionName2 = permissionName;
                            Slog.w(str, append.append(permissionName2).toString());
                        }
                        identity = Binder.clearCallingIdentity();
                        try {
                            packageManager.updatePermissionFlags(permissionName2, packageName, 8, isRevokedCompat ? 8 : 0, user);
                        } finally {
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    i = i4;
                    i2 = length;
                    ops = ops2;
                    packageName = packageName2;
                    packageNames = packageNames2;
                    e.printStackTrace();
                }
            }
            i4 = i + 1;
            packageNames2 = packageNames;
            length = i2;
            ops2 = ops;
            packageName2 = packageName;
            i3 = 0;
        }
    }

    private boolean isBackgroundRevokedCompat(PackageManagerInternal packageManagerInternal, PackageManager packageManager, int uid, String packageName, String permissionName, String backgroundPermission, int mode) {
        int permissionCount;
        char c;
        boolean isBackgroundRevokedCompat = false;
        boolean isIgnoreAll = false;
        int i = 0;
        if (mode == 0) {
            return false;
        }
        char c2 = 4;
        if (mode == 4) {
            return true;
        }
        if (mode == 1) {
            isIgnoreAll = true;
        }
        PackageInfo packageInfo = packageManagerInternal.getPackageInfo(packageName, 4096L, uid, this.mContext.getUserId());
        if (packageInfo.requestedPermissions == null) {
            permissionCount = 0;
        } else {
            permissionCount = packageInfo.requestedPermissions.length;
        }
        int i2 = 0;
        boolean isIgnoreAll2 = isIgnoreAll;
        while (true) {
            if (i2 >= permissionCount) {
                break;
            }
            String requestedPermission = packageInfo.requestedPermissions[i2];
            if (permissionName.equals(requestedPermission)) {
                c = c2;
            } else {
                try {
                    PermissionInfo requestedPermissionInfo = packageManager.getPermissionInfo(requestedPermission, i);
                    if (requestedPermissionInfo == null || requestedPermissionInfo.backgroundPermission == null) {
                        c = c2;
                    } else {
                        try {
                            if (requestedPermissionInfo.backgroundPermission.equals(backgroundPermission)) {
                                try {
                                    int requestedMode = checkOperationRaw(permissionToOpCode(requestedPermission), uid, packageName, null);
                                    c = 4;
                                    if (requestedMode == 4) {
                                        isBackgroundRevokedCompat = true;
                                        break;
                                    } else if (requestedMode == 0) {
                                        isIgnoreAll2 = false;
                                    }
                                } catch (PackageManager.NameNotFoundException e) {
                                    e = e;
                                    c = 4;
                                    e.printStackTrace();
                                    i2++;
                                    c2 = c;
                                    i = 0;
                                }
                            } else {
                                c = c2;
                            }
                        } catch (PackageManager.NameNotFoundException e2) {
                            e = e2;
                            c = c2;
                            e.printStackTrace();
                            i2++;
                            c2 = c;
                            i = 0;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e3) {
                    e = e3;
                }
            }
            i2++;
            c2 = c;
            i = 0;
        }
        if (!isBackgroundRevokedCompat && isIgnoreAll2) {
            return true;
        }
        return isBackgroundRevokedCompat;
    }

    private void notifyOpChangedSync(int code, int uid, String packageName, int mode, int previousMode) {
        StorageManagerInternal storageManagerInternal = (StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class);
        if (storageManagerInternal != null) {
            storageManagerInternal.onAppOpsChanged(code, uid, packageName, mode, previousMode);
        }
    }

    public void setMode(int code, int uid, String packageName, int mode) {
        setMode(code, uid, packageName, mode, null);
    }

    public void setMode(int code, int uid, String packageName, int mode, IAppOpsCallback permissionPolicyCallback) {
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), uid);
        verifyIncomingOp(code);
        verifyIncomingPackage(packageName, UserHandle.getUserId(uid));
        ArraySet<ModeCallback> repCbs = null;
        int code2 = AppOpsManager.opToSwitch(code);
        try {
            PackageVerificationResult pvr = verifyAndGetBypass(uid, packageName, null);
            int previousMode = 3;
            synchronized (this) {
                UidState uidState = getUidStateLocked(uid, false);
                Op op = getOpLocked(code2, uid, packageName, null, false, pvr.bypass, true);
                if (op != null && op.mode != mode) {
                    previousMode = op.mode;
                    op.mode = mode;
                    if (uidState != null) {
                        uidState.evalForegroundOps(this.mOpModeWatchers);
                    }
                    ArraySet<? extends ModeCallback> arraySet = this.mOpModeWatchers.get(code2);
                    if (arraySet != null) {
                        if (0 == 0) {
                            repCbs = new ArraySet<>();
                        }
                        repCbs.addAll(arraySet);
                    }
                    ArraySet<? extends ModeCallback> arraySet2 = this.mPackageModeWatchers.get(packageName);
                    if (arraySet2 != null) {
                        if (repCbs == null) {
                            repCbs = new ArraySet<>();
                        }
                        repCbs.addAll(arraySet2);
                    }
                    if (repCbs != null && permissionPolicyCallback != null) {
                        repCbs.remove(this.mModeWatchers.get(permissionPolicyCallback.asBinder()));
                    }
                    if (mode == AppOpsManager.opToDefaultMode(op.op)) {
                        pruneOpLocked(op, uid, packageName);
                    }
                    scheduleFastWriteLocked();
                    if (mode != 2) {
                        boolean z = true;
                        if (mode != 1) {
                            z = false;
                        }
                        updateStartedOpModeForUidLocked(code2, z, uid);
                    }
                }
            }
            if (repCbs != null) {
                this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda15
                    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                        ((AppOpsService) obj).notifyOpChanged((ArraySet) obj2, ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), (String) obj5);
                    }
                }, this, repCbs, Integer.valueOf(code2), Integer.valueOf(uid), packageName));
            }
            notifyOpChangedSync(code2, uid, packageName, mode, previousMode);
        } catch (SecurityException e) {
            Slog.e(TAG, "Cannot setMode", e);
        }
    }

    public void notifyOpChanged(ArraySet<ModeCallback> callbacks, int code, int uid, String packageName) {
        for (int i = 0; i < callbacks.size(); i++) {
            ModeCallback callback = callbacks.valueAt(i);
            notifyOpChanged(callback, code, uid, packageName);
        }
    }

    public void notifyOpChanged(ModeCallback callback, int code, int uid, String packageName) {
        int[] switchedCodes;
        if (uid == -2 || callback.mWatchingUid < 0 || callback.mWatchingUid == uid) {
            if (callback.mWatchedOpCode == -2) {
                switchedCodes = this.mSwitchedOps.get(code);
            } else {
                switchedCodes = callback.mWatchedOpCode == -1 ? new int[]{code} : new int[]{callback.mWatchedOpCode};
            }
            for (int switchedCode : switchedCodes) {
                long identity = Binder.clearCallingIdentity();
                try {
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
                if (!shouldIgnoreCallback(switchedCode, callback.mCallingPid, callback.mCallingUid)) {
                    callback.mCallback.opChanged(switchedCode, uid, packageName);
                    Binder.restoreCallingIdentity(identity);
                } else {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    private static ArrayList<ChangeRec> addChange(ArrayList<ChangeRec> reports, int op, int uid, String packageName, int previousMode) {
        boolean duplicate = false;
        if (reports == null) {
            reports = new ArrayList<>();
        } else {
            int reportCount = reports.size();
            int j = 0;
            while (true) {
                if (j >= reportCount) {
                    break;
                }
                ChangeRec report = reports.get(j);
                if (report.op != op || !report.pkg.equals(packageName)) {
                    j++;
                } else {
                    duplicate = true;
                    break;
                }
            }
        }
        if (!duplicate) {
            reports.add(new ChangeRec(op, uid, packageName, previousMode));
        }
        return reports;
    }

    private static HashMap<ModeCallback, ArrayList<ChangeRec>> addCallbacks(HashMap<ModeCallback, ArrayList<ChangeRec>> callbacks, int op, int uid, String packageName, int previousMode, ArraySet<ModeCallback> cbs) {
        if (cbs == null) {
            return callbacks;
        }
        if (callbacks == null) {
            callbacks = new HashMap<>();
        }
        int N = cbs.size();
        for (int i = 0; i < N; i++) {
            ModeCallback cb = cbs.valueAt(i);
            ArrayList<ChangeRec> reports = callbacks.get(cb);
            ArrayList<ChangeRec> changed = addChange(reports, op, uid, packageName, previousMode);
            if (changed != reports) {
                callbacks.put(cb, changed);
            }
        }
        return callbacks;
    }

    /* loaded from: classes.dex */
    public static final class ChangeRec {
        final int op;
        final String pkg;
        final int previous_mode;
        final int uid;

        ChangeRec(int _op, int _uid, String _pkg, int _previous_mode) {
            this.op = _op;
            this.uid = _uid;
            this.pkg = _pkg;
            this.previous_mode = _previous_mode;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3092=4, 3133=10, 3044=4] */
    /* JADX WARN: Code restructure failed: missing block: B:279:0x0154, code lost:
        if (r9 != android.os.UserHandle.getUserId(r1.uid)) goto L115;
     */
    /* JADX WARN: Removed duplicated region for block: B:227:0x0037  */
    /* JADX WARN: Removed duplicated region for block: B:276:0x014b  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void resetAllModes(int reqUserId, String reqPackageName) {
        int reqUid;
        int callingUid;
        ArrayList<ChangeRec> allChanges;
        SparseIntArray opModes;
        int callingUid2;
        String packageName;
        int i;
        String[] strArr;
        HashMap<ModeCallback, ArrayList<ChangeRec>> callbacks;
        int callingPid;
        int reqUserId2;
        int reqUid2;
        int reqUserId3;
        int reqUid3;
        Ops pkgOps;
        ArrayList<ChangeRec> allChanges2;
        String str = reqPackageName;
        int callingPid2 = Binder.getCallingPid();
        int callingUid3 = Binder.getCallingUid();
        int reqUserId4 = ActivityManager.handleIncomingUser(callingPid2, callingUid3, reqUserId, true, true, "resetAllModes", null);
        if (str != null) {
            try {
                int reqUid4 = AppGlobals.getPackageManager().getPackageUid(str, 8192L, reqUserId4);
                reqUid = reqUid4;
            } catch (RemoteException e) {
            }
            enforceManageAppOpsModes(callingPid2, callingUid3, reqUid);
            ArrayList<ChangeRec> allChanges3 = new ArrayList<>();
            synchronized (this) {
                boolean changed = false;
                try {
                    int i2 = this.mUidStates.size() - 1;
                    HashMap<ModeCallback, ArrayList<ChangeRec>> callbacks2 = null;
                    ArrayList<ChangeRec> allChanges4 = allChanges3;
                    while (i2 >= 0) {
                        try {
                            UidState uidState = this.mUidStates.valueAt(i2);
                            SparseIntArray opModes2 = uidState.opModes;
                            try {
                                if (opModes2 != null) {
                                    try {
                                        if (uidState.uid != reqUid && reqUid != -1) {
                                            callingUid = callingUid3;
                                        }
                                        int uidOpCount = opModes2.size();
                                        int j = uidOpCount - 1;
                                        while (j >= 0) {
                                            int code = opModes2.keyAt(j);
                                            if (AppOpsManager.opAllowsReset(code)) {
                                                int previousMode = opModes2.valueAt(j);
                                                opModes2.removeAt(j);
                                                if (opModes2.size() <= 0) {
                                                    try {
                                                        uidState.opModes = null;
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
                                                String[] packagesForUid = getPackagesForUid(uidState.uid);
                                                opModes = opModes2;
                                                int previousMode2 = packagesForUid.length;
                                                callingUid2 = callingUid3;
                                                int callingUid4 = 0;
                                                while (callingUid4 < previousMode2) {
                                                    try {
                                                        packageName = packagesForUid[callingUid4];
                                                        i = previousMode2;
                                                        strArr = packagesForUid;
                                                        try {
                                                            callbacks = addCallbacks(addCallbacks(callbacks2, code, uidState.uid, packageName, previousMode, this.mOpModeWatchers.get(code)), code, uidState.uid, packageName, previousMode, this.mPackageModeWatchers.get(packageName));
                                                        } catch (Throwable th3) {
                                                            th = th3;
                                                        }
                                                    } catch (Throwable th4) {
                                                        th = th4;
                                                    }
                                                    try {
                                                        int previousMode3 = previousMode;
                                                        allChanges4 = addChange(allChanges4, code, uidState.uid, packageName, previousMode3);
                                                        callingUid4++;
                                                        previousMode = previousMode3;
                                                        callbacks2 = callbacks;
                                                        previousMode2 = i;
                                                        packagesForUid = strArr;
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                }
                                            } else {
                                                opModes = opModes2;
                                                callingUid2 = callingUid3;
                                            }
                                            j--;
                                            opModes2 = opModes;
                                            callingUid3 = callingUid2;
                                        }
                                        callingUid = callingUid3;
                                        allChanges = allChanges4;
                                        if (uidState.pkgOps != null) {
                                            if (reqUserId4 != -1) {
                                                try {
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    while (true) {
                                                        break;
                                                        break;
                                                    }
                                                    throw th;
                                                }
                                            }
                                            Map<String, Ops> packages = uidState.pkgOps;
                                            Iterator<Map.Entry<String, Ops>> it = packages.entrySet().iterator();
                                            boolean uidChanged = false;
                                            while (it.hasNext()) {
                                                Map.Entry<String, Ops> ent = it.next();
                                                String packageName2 = ent.getKey();
                                                if (str == null || str.equals(packageName2)) {
                                                    Ops pkgOps2 = ent.getValue();
                                                    boolean changed2 = changed;
                                                    int j2 = pkgOps2.size() - 1;
                                                    while (j2 >= 0) {
                                                        Op curOp = pkgOps2.valueAt(j2);
                                                        Map<String, Ops> packages2 = packages;
                                                        int callingPid3 = callingPid2;
                                                        try {
                                                            if (shouldDeferResetOpToDpm(curOp.op)) {
                                                                try {
                                                                    deferResetOpToDpm(curOp.op, str, reqUserId4);
                                                                    reqUserId3 = reqUserId4;
                                                                    reqUid3 = reqUid;
                                                                    pkgOps = pkgOps2;
                                                                } catch (Throwable th7) {
                                                                    th = th7;
                                                                    while (true) {
                                                                        break;
                                                                        break;
                                                                    }
                                                                    throw th;
                                                                }
                                                            } else if (!AppOpsManager.opAllowsReset(curOp.op)) {
                                                                reqUserId3 = reqUserId4;
                                                                reqUid3 = reqUid;
                                                                pkgOps = pkgOps2;
                                                            } else if (curOp.mode != AppOpsManager.opToDefaultMode(curOp.op)) {
                                                                int previousMode4 = curOp.mode;
                                                                curOp.mode = AppOpsManager.opToDefaultMode(curOp.op);
                                                                changed2 = true;
                                                                int uid = curOp.uidState.uid;
                                                                reqUserId3 = reqUserId4;
                                                                try {
                                                                    int reqUserId5 = curOp.op;
                                                                    reqUid3 = reqUid;
                                                                    try {
                                                                        pkgOps = pkgOps2;
                                                                        try {
                                                                            HashMap<ModeCallback, ArrayList<ChangeRec>> callbacks3 = addCallbacks(addCallbacks(callbacks2, reqUserId5, uid, packageName2, previousMode4, this.mOpModeWatchers.get(curOp.op)), curOp.op, uid, packageName2, previousMode4, this.mPackageModeWatchers.get(packageName2));
                                                                            try {
                                                                                ArrayList<ChangeRec> allChanges5 = addChange(allChanges, curOp.op, uid, packageName2, previousMode4);
                                                                                curOp.removeAttributionsWithNoTime();
                                                                                if (curOp.mAttributions.isEmpty()) {
                                                                                    pkgOps.removeAt(j2);
                                                                                }
                                                                                allChanges2 = allChanges5;
                                                                                callbacks2 = callbacks3;
                                                                                uidChanged = true;
                                                                                boolean allChanges6 = changed2;
                                                                                j2--;
                                                                                str = reqPackageName;
                                                                                changed2 = allChanges6;
                                                                                allChanges = allChanges2;
                                                                                pkgOps2 = pkgOps;
                                                                                callingPid2 = callingPid3;
                                                                                packages = packages2;
                                                                                reqUserId4 = reqUserId3;
                                                                                reqUid = reqUid3;
                                                                            } catch (Throwable th8) {
                                                                                th = th8;
                                                                                while (true) {
                                                                                    break;
                                                                                    break;
                                                                                }
                                                                                throw th;
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
                                                            } else {
                                                                reqUserId3 = reqUserId4;
                                                                reqUid3 = reqUid;
                                                                pkgOps = pkgOps2;
                                                            }
                                                            allChanges2 = allChanges;
                                                            boolean allChanges62 = changed2;
                                                            j2--;
                                                            str = reqPackageName;
                                                            changed2 = allChanges62;
                                                            allChanges = allChanges2;
                                                            pkgOps2 = pkgOps;
                                                            callingPid2 = callingPid3;
                                                            packages = packages2;
                                                            reqUserId4 = reqUserId3;
                                                            reqUid = reqUid3;
                                                        } catch (Throwable th12) {
                                                            th = th12;
                                                        }
                                                    }
                                                    Map<String, Ops> packages3 = packages;
                                                    int callingPid4 = callingPid2;
                                                    int reqUserId6 = reqUserId4;
                                                    int reqUid5 = reqUid;
                                                    int j3 = pkgOps2.size();
                                                    if (j3 == 0) {
                                                        it.remove();
                                                    }
                                                    changed = changed2;
                                                    str = reqPackageName;
                                                    callingPid2 = callingPid4;
                                                    packages = packages3;
                                                    reqUserId4 = reqUserId6;
                                                    reqUid = reqUid5;
                                                }
                                            }
                                            callingPid = callingPid2;
                                            reqUserId2 = reqUserId4;
                                            reqUid2 = reqUid;
                                            if (uidState.isDefault()) {
                                                this.mUidStates.remove(uidState.uid);
                                            }
                                            if (uidChanged) {
                                                uidState.evalForegroundOps(this.mOpModeWatchers);
                                            }
                                            allChanges4 = allChanges;
                                            i2--;
                                            str = reqPackageName;
                                            callingPid2 = callingPid;
                                            callingUid3 = callingUid;
                                            reqUserId4 = reqUserId2;
                                            reqUid = reqUid2;
                                        }
                                        allChanges4 = allChanges;
                                        callingPid = callingPid2;
                                        reqUserId2 = reqUserId4;
                                        reqUid2 = reqUid;
                                        i2--;
                                        str = reqPackageName;
                                        callingPid2 = callingPid;
                                        callingUid3 = callingUid;
                                        reqUserId4 = reqUserId2;
                                        reqUid = reqUid2;
                                    } catch (Throwable th13) {
                                        th = th13;
                                    }
                                } else {
                                    callingUid = callingUid3;
                                }
                                if (uidState.pkgOps != null) {
                                }
                                allChanges4 = allChanges;
                                callingPid = callingPid2;
                                reqUserId2 = reqUserId4;
                                reqUid2 = reqUid;
                                i2--;
                                str = reqPackageName;
                                callingPid2 = callingPid;
                                callingUid3 = callingUid;
                                reqUserId4 = reqUserId2;
                                reqUid = reqUid2;
                            } catch (Throwable th14) {
                                th = th14;
                            }
                            allChanges = allChanges4;
                        } catch (Throwable th15) {
                            th = th15;
                        }
                    }
                    if (changed) {
                        try {
                            scheduleFastWriteLocked();
                        } catch (Throwable th16) {
                            th = th16;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                    }
                    try {
                        if (callbacks2 != null) {
                            for (Map.Entry<ModeCallback, ArrayList<ChangeRec>> ent2 : callbacks2.entrySet()) {
                                ModeCallback cb = ent2.getKey();
                                ArrayList<ChangeRec> reports = ent2.getValue();
                                for (int i3 = 0; i3 < reports.size(); i3++) {
                                    ChangeRec rep = reports.get(i3);
                                    this.mHandler.sendMessage(PooledLambda.obtainMessage(new AppOpsService$$ExternalSyntheticLambda5(), this, cb, Integer.valueOf(rep.op), Integer.valueOf(rep.uid), rep.pkg));
                                }
                            }
                        }
                        int numChanges = allChanges4.size();
                        for (int i4 = 0; i4 < numChanges; i4++) {
                            ChangeRec change = allChanges4.get(i4);
                            notifyOpChangedSync(change.op, change.uid, change.pkg, AppOpsManager.opToDefaultMode(change.op), change.previous_mode);
                        }
                        return;
                    } catch (Throwable th17) {
                        th = th17;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                } catch (Throwable th18) {
                    th = th18;
                }
            }
        }
        reqUid = -1;
        enforceManageAppOpsModes(callingPid2, callingUid3, reqUid);
        ArrayList<ChangeRec> allChanges32 = new ArrayList<>();
        synchronized (this) {
        }
    }

    private boolean shouldDeferResetOpToDpm(int op) {
        DevicePolicyManagerInternal devicePolicyManagerInternal = this.dpmi;
        return devicePolicyManagerInternal != null && devicePolicyManagerInternal.supportsResetOp(op);
    }

    private void deferResetOpToDpm(int op, String packageName, int userId) {
        this.dpmi.resetOp(op, packageName, userId);
    }

    private void evalAllForegroundOpsLocked() {
        for (int uidi = this.mUidStates.size() - 1; uidi >= 0; uidi--) {
            UidState uidState = this.mUidStates.valueAt(uidi);
            if (uidState.foregroundOps != null) {
                uidState.evalForegroundOps(this.mOpModeWatchers);
            }
        }
    }

    public void startWatchingMode(int op, String packageName, IAppOpsCallback callback) {
        startWatchingModeWithFlags(op, packageName, 0, callback);
    }

    public void startWatchingModeWithFlags(int op, String packageName, int flags, IAppOpsCallback callback) {
        int switchOp;
        int notifiedOps;
        int i;
        ModeCallback cb;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        CtaManager ctaManager = sCtaManager;
        int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
        Preconditions.checkArgumentInRange(op, -1, opNum - 1, "Invalid op code: " + op);
        if (callback == null) {
            return;
        }
        boolean mayWatchPackageName = (packageName == null || filterAppAccessUnlocked(packageName)) ? false : true;
        synchronized (this) {
            if (op == -1) {
                switchOp = op;
            } else {
                try {
                    switchOp = AppOpsManager.opToSwitch(op);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((flags & 2) == 0) {
                if (op == -1) {
                    notifiedOps = -2;
                } else {
                    notifiedOps = op;
                }
            } else {
                int notifiedOps2 = switchOp;
                notifiedOps = notifiedOps2;
            }
            ModeCallback cb2 = this.mModeWatchers.get(callback.asBinder());
            if (cb2 != null) {
                i = -1;
                cb = cb2;
            } else {
                i = -1;
                cb = new ModeCallback(callback, -1, flags, notifiedOps, callingUid, callingPid);
                this.mModeWatchers.put(callback.asBinder(), cb);
            }
            if (switchOp != i) {
                ArraySet<ModeCallback> cbs = this.mOpModeWatchers.get(switchOp);
                if (cbs == null) {
                    cbs = new ArraySet<>();
                    this.mOpModeWatchers.put(switchOp, cbs);
                }
                cbs.add(cb);
            }
            if (mayWatchPackageName) {
                ArraySet<ModeCallback> cbs2 = this.mPackageModeWatchers.get(packageName);
                if (cbs2 == null) {
                    cbs2 = new ArraySet<>();
                    this.mPackageModeWatchers.put(packageName, cbs2);
                }
                cbs2.add(cb);
            }
            evalAllForegroundOpsLocked();
        }
    }

    public void stopWatchingMode(IAppOpsCallback callback) {
        if (callback == null) {
            return;
        }
        synchronized (this) {
            ModeCallback cb = this.mModeWatchers.remove(callback.asBinder());
            if (cb != null) {
                cb.unlinkToDeath();
                for (int i = this.mOpModeWatchers.size() - 1; i >= 0; i--) {
                    ArraySet<ModeCallback> cbs = this.mOpModeWatchers.valueAt(i);
                    cbs.remove(cb);
                    if (cbs.size() <= 0) {
                        this.mOpModeWatchers.removeAt(i);
                    }
                }
                for (int i2 = this.mPackageModeWatchers.size() - 1; i2 >= 0; i2--) {
                    ArraySet<ModeCallback> cbs2 = this.mPackageModeWatchers.valueAt(i2);
                    cbs2.remove(cb);
                    if (cbs2.size() <= 0) {
                        this.mPackageModeWatchers.removeAt(i2);
                    }
                }
            }
            evalAllForegroundOpsLocked();
        }
    }

    public AppOpsManagerInternal.CheckOpsDelegate getAppOpsServiceDelegate() {
        AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate;
        synchronized (this) {
            CheckOpsDelegateDispatcher dispatcher = this.mCheckOpsDelegateDispatcher;
            checkOpsDelegate = dispatcher != null ? dispatcher.getCheckOpsDelegate() : null;
        }
        return checkOpsDelegate;
    }

    public void setAppOpsServiceDelegate(AppOpsManagerInternal.CheckOpsDelegate delegate) {
        synchronized (this) {
            CheckOpsDelegateDispatcher oldDispatcher = this.mCheckOpsDelegateDispatcher;
            AppOpsManagerInternal.CheckOpsDelegate policy = oldDispatcher != null ? oldDispatcher.mPolicy : null;
            this.mCheckOpsDelegateDispatcher = new CheckOpsDelegateDispatcher(policy, delegate);
        }
    }

    public int checkOperationRaw(int code, int uid, String packageName, String attributionTag) {
        return this.mCheckOpsDelegateDispatcher.checkOperation(code, uid, packageName, attributionTag, true);
    }

    public int checkOperation(int code, int uid, String packageName) {
        return this.mCheckOpsDelegateDispatcher.checkOperation(code, uid, packageName, null, false);
    }

    public int checkOperationImpl(int code, int uid, String packageName, String attributionTag, boolean raw) {
        verifyIncomingOp(code);
        verifyIncomingPackage(packageName, UserHandle.getUserId(uid));
        String resolvedPackageName = AppOpsManager.resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return 1;
        }
        return checkOperationUnchecked(code, uid, resolvedPackageName, attributionTag, raw);
    }

    private int checkOperationUnchecked(int code, int uid, String packageName, String attributionTag, boolean raw) {
        try {
            PackageVerificationResult pvr = verifyAndGetBypass(uid, packageName, null);
            if (isOpRestrictedDueToSuspend(code, packageName, uid)) {
                return 1;
            }
            synchronized (this) {
                if (isOpRestrictedLocked(uid, code, packageName, attributionTag, pvr.bypass, true)) {
                    return 1;
                }
                int code2 = AppOpsManager.opToSwitch(code);
                UidState uidState = getUidStateLocked(uid, false);
                if (uidState != null && uidState.opModes != null && uidState.opModes.indexOfKey(code2) >= 0) {
                    int rawMode = uidState.opModes.get(code2);
                    return raw ? rawMode : uidState.evalMode(code2, rawMode);
                }
                Op op = getOpLocked(code2, uid, packageName, null, false, pvr.bypass, false);
                if (op == null) {
                    return AppOpsManager.opToDefaultMode(code2);
                }
                return raw ? op.mode : op.evalMode();
            }
        } catch (SecurityException e) {
            Slog.e(TAG, "checkOperation", e);
            return AppOpsManager.opToDefaultMode(code);
        }
    }

    private boolean isSuspendedWhitelist(String pkg) {
        if ("com.transsion.deskclock".equals(pkg) || "com.transsion.smartpanel".equals(pkg)) {
            return true;
        }
        return false;
    }

    public int checkAudioOperation(int code, int usage, int uid, String packageName) {
        return this.mCheckOpsDelegateDispatcher.checkAudioOperation(code, usage, uid, packageName);
    }

    public int checkAudioOperationImpl(int code, int usage, int uid, String packageName) {
        int mode = this.mAudioRestrictionManager.checkAudioOperation(code, usage, uid, packageName);
        if (mode != 0) {
            return mode;
        }
        return checkOperation(code, uid, packageName);
    }

    public void setAudioRestriction(int code, int usage, int uid, int mode, String[] exceptionPackages) {
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), uid);
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        this.mAudioRestrictionManager.setZenModeAudioRestriction(code, usage, uid, mode, exceptionPackages);
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new AppOpsService$$ExternalSyntheticLambda2(), this, Integer.valueOf(code), -2));
    }

    public void setCameraAudioRestriction(int mode) {
        enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), -1);
        this.mAudioRestrictionManager.setCameraAudioRestriction(mode);
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new AppOpsService$$ExternalSyntheticLambda2(), this, 28, -2));
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new AppOpsService$$ExternalSyntheticLambda2(), this, 3, -2));
    }

    public int checkPackage(int uid, String packageName) {
        Objects.requireNonNull(packageName);
        try {
            verifyAndGetBypass(uid, packageName, null);
            if (resolveUid(packageName) != uid) {
                if (isPackageExisted(packageName)) {
                    if (!filterAppAccessUnlocked(packageName)) {
                        return 0;
                    }
                }
                return 2;
            }
            return 0;
        } catch (SecurityException e) {
            return 2;
        }
    }

    private boolean isPackageExisted(String packageName) {
        return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageStateInternal(packageName) != null;
    }

    private boolean filterAppAccessUnlocked(String packageName) {
        int callingUid = Binder.getCallingUid();
        return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).filterAppAccess(packageName, callingUid, UserHandle.getUserId(callingUid));
    }

    public SyncNotedAppOp noteProxyOperation(int code, AttributionSource attributionSource, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation) {
        return this.mCheckOpsDelegateDispatcher.noteProxyOperation(code, attributionSource, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation);
    }

    public SyncNotedAppOp noteProxyOperationImpl(int code, AttributionSource attributionSource, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation) {
        int proxiedUid;
        String proxiedAttributionTag;
        String proxyPackageName;
        int proxyUid = attributionSource.getUid();
        String proxyPackageName2 = attributionSource.getPackageName();
        String proxyAttributionTag = attributionSource.getAttributionTag();
        int proxiedUid2 = attributionSource.getNextUid();
        String proxiedPackageName = attributionSource.getNextPackageName();
        String proxiedAttributionTag2 = attributionSource.getNextAttributionTag();
        verifyIncomingProxyUid(attributionSource);
        verifyIncomingOp(code);
        verifyIncomingPackage(proxiedPackageName, UserHandle.getUserId(proxiedUid2));
        verifyIncomingPackage(proxyPackageName2, UserHandle.getUserId(proxyUid));
        boolean skipProxyOperation2 = skipProxyOperation && isCallerAndAttributionTrusted(attributionSource);
        String resolveProxyPackageName = AppOpsManager.resolvePackageName(proxyUid, proxyPackageName2);
        if (resolveProxyPackageName == null) {
            return new SyncNotedAppOp(1, code, proxiedAttributionTag2, proxiedPackageName);
        }
        boolean isSelfBlame = Binder.getCallingUid() == proxiedUid2;
        boolean isProxyTrusted = this.mContext.checkPermission("android.permission.UPDATE_APP_OPS_STATS", -1, proxyUid) == 0 || isSelfBlame;
        if (skipProxyOperation2) {
            proxiedUid = proxiedUid2;
            proxiedAttributionTag = proxiedAttributionTag2;
            proxyPackageName = proxiedPackageName;
        } else {
            int proxyFlags = isProxyTrusted ? 2 : 4;
            proxiedUid = proxiedUid2;
            SyncNotedAppOp proxyReturn = noteOperationUnchecked(code, proxyUid, resolveProxyPackageName, proxyAttributionTag, -1, null, null, proxyFlags, !isProxyTrusted, "proxy " + message, shouldCollectMessage);
            if (proxyReturn.getOpMode() == 0) {
                proxiedAttributionTag = proxiedAttributionTag2;
                proxyPackageName = proxiedPackageName;
            } else {
                return new SyncNotedAppOp(proxyReturn.getOpMode(), code, proxiedAttributionTag2, proxiedPackageName);
            }
        }
        int proxiedUid3 = proxiedUid;
        String resolveProxiedPackageName = AppOpsManager.resolvePackageName(proxiedUid3, proxyPackageName);
        if (resolveProxiedPackageName == null) {
            return new SyncNotedAppOp(1, code, proxiedAttributionTag, proxyPackageName);
        }
        int proxiedFlags = isProxyTrusted ? 8 : 16;
        return noteOperationUnchecked(code, proxiedUid3, resolveProxiedPackageName, proxiedAttributionTag, proxyUid, resolveProxyPackageName, proxyAttributionTag, proxiedFlags, shouldCollectAsyncNotedOp, message, shouldCollectMessage);
    }

    public SyncNotedAppOp noteOperation(int code, int uid, String packageName, String attributionTag, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage) {
        return this.mCheckOpsDelegateDispatcher.noteOperation(code, uid, packageName, attributionTag, shouldCollectAsyncNotedOp, message, shouldCollectMessage);
    }

    public SyncNotedAppOp noteOperationImpl(int code, int uid, String packageName, String attributionTag, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        verifyIncomingPackage(packageName, UserHandle.getUserId(uid));
        String resolvedPackageName = AppOpsManager.resolvePackageName(uid, packageName);
        return resolvedPackageName == null ? new SyncNotedAppOp(1, code, attributionTag, packageName) : noteOperationUnchecked(code, uid, resolvedPackageName, attributionTag, -1, null, null, 1, shouldCollectAsyncNotedOp, message, shouldCollectMessage);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3622=6] */
    /* JADX WARN: Removed duplicated region for block: B:215:0x01c2 A[Catch: all -> 0x01fe, TryCatch #14 {all -> 0x01fe, blocks: (B:232:0x01fc, B:213:0x01a3, B:215:0x01c2, B:217:0x01d9, B:218:0x01df, B:207:0x018f, B:208:0x0199), top: B:265:0x0024 }] */
    /* JADX WARN: Removed duplicated region for block: B:216:0x01d7  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private SyncNotedAppOp noteOperationUnchecked(int code, int uid, String packageName, String attributionTag, int proxyUid, String proxyPackageName, String proxyAttributionTag, int flags, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage) {
        String attributionTag2;
        Op switchOp;
        try {
            PackageVerificationResult pvr = verifyAndGetBypass(uid, packageName, attributionTag, proxyPackageName);
            if (attributionTag == null) {
            }
            String attributionTag3 = !pvr.isAttributionTagValid ? null : attributionTag;
            synchronized (this) {
                try {
                    try {
                        String attributionTag4 = attributionTag3;
                        try {
                            Ops opsLocked = getOpsLocked(uid, packageName, attributionTag3, pvr.isAttributionTagValid, pvr.bypass, true);
                            try {
                                if (opsLocked == null) {
                                    try {
                                        scheduleOpNotedIfNeededLocked(code, uid, packageName, attributionTag4, flags, 1);
                                        return new SyncNotedAppOp(2, code, attributionTag4, packageName);
                                    } catch (Throwable th) {
                                        th = th;
                                    }
                                } else {
                                    try {
                                        Op op = getOpLocked(opsLocked, code, uid, true);
                                        AttributedOp attributedOp = op.getOrCreateAttribution(op, attributionTag4);
                                        if (attributedOp.isRunning()) {
                                            Slog.w(TAG, "Noting op not finished: uid " + uid + " pkg " + packageName + " code " + code + " startTime of in progress event=" + ((InProgressStartOpEvent) attributedOp.mInProgressEvents.valueAt(0)).getStartTime());
                                        }
                                        int switchCode = AppOpsManager.opToSwitch(code);
                                        UidState uidState = opsLocked.uidState;
                                        try {
                                            if (isOpRestrictedLocked(uid, code, packageName, attributionTag4, pvr.bypass, false)) {
                                                try {
                                                    attributedOp.rejected(uidState.state, flags);
                                                    scheduleOpNotedIfNeededLocked(code, uid, packageName, attributionTag4, flags, 1);
                                                    try {
                                                        return new SyncNotedAppOp(1, code, attributionTag4, packageName);
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                    }
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                }
                                            } else {
                                                try {
                                                    try {
                                                        try {
                                                            if (uidState.opModes != null) {
                                                                try {
                                                                    if (uidState.opModes.indexOfKey(switchCode) >= 0) {
                                                                        int uidMode = uidState.evalMode(code, uidState.opModes.get(switchCode));
                                                                        if (uidMode != 0) {
                                                                            attributedOp.rejected(uidState.state, flags);
                                                                            scheduleOpNotedIfNeededLocked(code, uid, packageName, attributionTag4, flags, uidMode);
                                                                            return new SyncNotedAppOp(uidMode, code, attributionTag4, packageName);
                                                                        }
                                                                        attributionTag2 = attributionTag4;
                                                                        scheduleOpNotedIfNeededLocked(code, uid, packageName, attributionTag2, flags, 0);
                                                                        attributedOp.accessed(proxyUid, proxyPackageName, proxyAttributionTag, uidState.state, flags);
                                                                        if (!shouldCollectAsyncNotedOp) {
                                                                            collectAsyncNotedOp(uid, packageName, code, attributionTag2, flags, message, shouldCollectMessage);
                                                                        }
                                                                        return new SyncNotedAppOp(0, code, attributionTag2, packageName);
                                                                    }
                                                                } catch (Throwable th4) {
                                                                    th = th4;
                                                                }
                                                            }
                                                            int mode = switchOp.evalMode();
                                                            if (mode == 0) {
                                                                attributionTag2 = attributionTag4;
                                                                scheduleOpNotedIfNeededLocked(code, uid, packageName, attributionTag2, flags, 0);
                                                                attributedOp.accessed(proxyUid, proxyPackageName, proxyAttributionTag, uidState.state, flags);
                                                                if (!shouldCollectAsyncNotedOp) {
                                                                }
                                                                return new SyncNotedAppOp(0, code, attributionTag2, packageName);
                                                            }
                                                            try {
                                                                attributedOp.rejected(uidState.state, flags);
                                                                scheduleOpNotedIfNeededLocked(code, uid, packageName, attributionTag4, flags, mode);
                                                                return new SyncNotedAppOp(mode, code, attributionTag4, packageName);
                                                            } catch (Throwable th5) {
                                                                th = th5;
                                                            }
                                                        } catch (Throwable th6) {
                                                            th = th6;
                                                        }
                                                        switchOp = switchCode != code ? getOpLocked(opsLocked, switchCode, uid, true) : op;
                                                    } catch (Throwable th7) {
                                                        th = th7;
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
                                }
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
                throw th;
            }
        } catch (SecurityException e) {
            Slog.e(TAG, "noteOperation", e);
            return new SyncNotedAppOp(2, code, attributionTag, packageName);
        }
    }

    public void startWatchingActive(int[] ops, IAppOpsActiveCallback callback) {
        SparseArray<ActiveCallback> callbacks;
        int watchedUid = -1;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WATCH_APPOPS") != 0) {
            watchedUid = callingUid;
        }
        if (ops != null) {
            CtaManager ctaManager = sCtaManager;
            int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
            Preconditions.checkArrayElementsInRange(ops, 0, opNum - 1, "Invalid op code in: " + Arrays.toString(ops));
        }
        if (callback == null) {
            return;
        }
        synchronized (this) {
            SparseArray<ActiveCallback> callbacks2 = this.mActiveWatchers.get(callback.asBinder());
            if (callbacks2 != null) {
                callbacks = callbacks2;
            } else {
                SparseArray<ActiveCallback> callbacks3 = new SparseArray<>();
                this.mActiveWatchers.put(callback.asBinder(), callbacks3);
                callbacks = callbacks3;
            }
            ActiveCallback activeCallback = new ActiveCallback(callback, watchedUid, callingUid, callingPid);
            for (int op : ops) {
                callbacks.put(op, activeCallback);
            }
        }
    }

    public void stopWatchingActive(IAppOpsActiveCallback callback) {
        if (callback == null) {
            return;
        }
        synchronized (this) {
            SparseArray<ActiveCallback> activeCallbacks = this.mActiveWatchers.remove(callback.asBinder());
            if (activeCallbacks == null) {
                return;
            }
            int callbackCount = activeCallbacks.size();
            for (int i = 0; i < callbackCount; i++) {
                activeCallbacks.valueAt(i).destroy();
            }
        }
    }

    public void startWatchingStarted(int[] ops, IAppOpsStartedCallback callback) {
        SparseArray<StartedCallback> callbacks;
        int watchedUid = -1;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WATCH_APPOPS") != 0) {
            watchedUid = callingUid;
        }
        Preconditions.checkArgument(!ArrayUtils.isEmpty(ops), "Ops cannot be null or empty");
        CtaManager ctaManager = sCtaManager;
        int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
        Preconditions.checkArrayElementsInRange(ops, 0, opNum - 1, "Invalid op code in: " + Arrays.toString(ops));
        Objects.requireNonNull(callback, "Callback cannot be null");
        synchronized (this) {
            SparseArray<StartedCallback> callbacks2 = this.mStartedWatchers.get(callback.asBinder());
            if (callbacks2 != null) {
                callbacks = callbacks2;
            } else {
                SparseArray<StartedCallback> callbacks3 = new SparseArray<>();
                this.mStartedWatchers.put(callback.asBinder(), callbacks3);
                callbacks = callbacks3;
            }
            StartedCallback startedCallback = new StartedCallback(callback, watchedUid, callingUid, callingPid);
            for (int op : ops) {
                callbacks.put(op, startedCallback);
            }
        }
    }

    public void stopWatchingStarted(IAppOpsStartedCallback callback) {
        Objects.requireNonNull(callback, "Callback cannot be null");
        synchronized (this) {
            SparseArray<StartedCallback> startedCallbacks = this.mStartedWatchers.remove(callback.asBinder());
            if (startedCallbacks == null) {
                return;
            }
            int callbackCount = startedCallbacks.size();
            for (int i = 0; i < callbackCount; i++) {
                startedCallbacks.valueAt(i).destroy();
            }
        }
    }

    public void startWatchingNoted(int[] ops, IAppOpsNotedCallback callback) {
        SparseArray<NotedCallback> callbacks;
        int watchedUid = -1;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WATCH_APPOPS") != 0) {
            watchedUid = callingUid;
        }
        Preconditions.checkArgument(!ArrayUtils.isEmpty(ops), "Ops cannot be null or empty");
        Preconditions.checkArrayElementsInRange(ops, 0, 120, "Invalid op code in: " + Arrays.toString(ops));
        Objects.requireNonNull(callback, "Callback cannot be null");
        synchronized (this) {
            SparseArray<NotedCallback> callbacks2 = this.mNotedWatchers.get(callback.asBinder());
            if (callbacks2 != null) {
                callbacks = callbacks2;
            } else {
                SparseArray<NotedCallback> callbacks3 = new SparseArray<>();
                this.mNotedWatchers.put(callback.asBinder(), callbacks3);
                callbacks = callbacks3;
            }
            NotedCallback notedCallback = new NotedCallback(callback, watchedUid, callingUid, callingPid);
            for (int op : ops) {
                callbacks.put(op, notedCallback);
            }
        }
    }

    public void stopWatchingNoted(IAppOpsNotedCallback callback) {
        Objects.requireNonNull(callback, "Callback cannot be null");
        synchronized (this) {
            SparseArray<NotedCallback> notedCallbacks = this.mNotedWatchers.remove(callback.asBinder());
            if (notedCallbacks == null) {
                return;
            }
            int callbackCount = notedCallbacks.size();
            for (int i = 0; i < callbackCount; i++) {
                notedCallbacks.valueAt(i).destroy();
            }
        }
    }

    private void collectAsyncNotedOp(final int uid, final String packageName, final int opCode, final String attributionTag, int flags, String message, boolean shouldCollectMessage) {
        RemoteCallbackList<IAppOpsAsyncNotedCallback> callbacks;
        int i;
        int i2;
        AsyncNotedAppOp asyncNotedOp;
        Objects.requireNonNull(message);
        int callingUid = Binder.getCallingUid();
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                Pair<String, Integer> key = getAsyncNotedOpsKey(packageName, uid);
                RemoteCallbackList<IAppOpsAsyncNotedCallback> callbacks2 = this.mAsyncOpWatchers.get(key);
                final AsyncNotedAppOp asyncNotedOp2 = new AsyncNotedAppOp(opCode, callingUid, attributionTag, message, System.currentTimeMillis());
                final boolean[] wasNoteForwarded = {false};
                if ((flags & 9) == 0 || !shouldCollectMessage) {
                    callbacks = callbacks2;
                } else {
                    callbacks = callbacks2;
                    reportRuntimeAppOpAccessMessageAsyncLocked(uid, packageName, opCode, attributionTag, message);
                }
                if (callbacks != null) {
                    i = 0;
                    i2 = 1;
                    asyncNotedOp = asyncNotedOp2;
                    callbacks.broadcast(new Consumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda6
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            AppOpsService.lambda$collectAsyncNotedOp$2(asyncNotedOp2, wasNoteForwarded, opCode, packageName, uid, attributionTag, (IAppOpsAsyncNotedCallback) obj);
                        }
                    });
                } else {
                    i = 0;
                    i2 = 1;
                    asyncNotedOp = asyncNotedOp2;
                }
                if (!wasNoteForwarded[i]) {
                    ArrayList<AsyncNotedAppOp> unforwardedOps = this.mUnforwardedAsyncNotedOps.get(key);
                    if (unforwardedOps == null) {
                        unforwardedOps = new ArrayList<>(i2);
                        this.mUnforwardedAsyncNotedOps.put(key, unforwardedOps);
                    }
                    unforwardedOps.add(asyncNotedOp);
                    if (unforwardedOps.size() > 10) {
                        unforwardedOps.remove(i);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static /* synthetic */ void lambda$collectAsyncNotedOp$2(AsyncNotedAppOp asyncNotedOp, boolean[] wasNoteForwarded, int opCode, String packageName, int uid, String attributionTag, IAppOpsAsyncNotedCallback cb) {
        try {
            cb.opNoted(asyncNotedOp);
            wasNoteForwarded[0] = true;
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not forward noteOp of " + opCode + " to " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + uid + "(" + attributionTag + ")", e);
        }
    }

    private Pair<String, Integer> getAsyncNotedOpsKey(String packageName, int uid) {
        return new Pair<>(packageName, Integer.valueOf(uid));
    }

    public void startWatchingAsyncNoted(String packageName, IAppOpsAsyncNotedCallback callback) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(callback);
        int uid = Binder.getCallingUid();
        final Pair<String, Integer> key = getAsyncNotedOpsKey(packageName, uid);
        verifyAndGetBypass(uid, packageName, null);
        synchronized (this) {
            RemoteCallbackList<IAppOpsAsyncNotedCallback> callbacks = this.mAsyncOpWatchers.get(key);
            if (callbacks == null) {
                callbacks = new RemoteCallbackList<IAppOpsAsyncNotedCallback>() { // from class: com.android.server.appop.AppOpsService.7
                    {
                        AppOpsService.this = this;
                    }

                    @Override // android.os.RemoteCallbackList
                    public void onCallbackDied(IAppOpsAsyncNotedCallback callback2) {
                        synchronized (AppOpsService.this) {
                            if (getRegisteredCallbackCount() == 0) {
                                AppOpsService.this.mAsyncOpWatchers.remove(key);
                            }
                        }
                    }
                };
                this.mAsyncOpWatchers.put(key, callbacks);
            }
            callbacks.register(callback);
        }
    }

    public void stopWatchingAsyncNoted(String packageName, IAppOpsAsyncNotedCallback callback) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(callback);
        int uid = Binder.getCallingUid();
        Pair<String, Integer> key = getAsyncNotedOpsKey(packageName, uid);
        verifyAndGetBypass(uid, packageName, null);
        synchronized (this) {
            RemoteCallbackList<IAppOpsAsyncNotedCallback> callbacks = this.mAsyncOpWatchers.get(key);
            if (callbacks != null) {
                callbacks.unregister(callback);
                if (callbacks.getRegisteredCallbackCount() == 0) {
                    this.mAsyncOpWatchers.remove(key);
                }
            }
        }
    }

    public List<AsyncNotedAppOp> extractAsyncOps(String packageName) {
        ArrayList<AsyncNotedAppOp> remove;
        Objects.requireNonNull(packageName);
        int uid = Binder.getCallingUid();
        verifyAndGetBypass(uid, packageName, null);
        synchronized (this) {
            remove = this.mUnforwardedAsyncNotedOps.remove(getAsyncNotedOpsKey(packageName, uid));
        }
        return remove;
    }

    public SyncNotedAppOp startOperation(IBinder token, int code, int uid, String packageName, String attributionTag, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, int attributionFlags, int attributionChainId) {
        return this.mCheckOpsDelegateDispatcher.startOperation(token, code, uid, packageName, attributionTag, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, attributionFlags, attributionChainId);
    }

    public SyncNotedAppOp startOperationImpl(IBinder clientId, int code, int uid, String packageName, String attributionTag, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, int attributionFlags, int attributionChainId) {
        int result;
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        verifyIncomingPackage(packageName, UserHandle.getUserId(uid));
        String resolvedPackageName = AppOpsManager.resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return new SyncNotedAppOp(1, code, attributionTag, packageName);
        }
        if ((code == 102 || code == 120) && (result = checkOperation(27, uid, packageName)) != 0) {
            return new SyncNotedAppOp(result, code, attributionTag, packageName);
        }
        return startOperationUnchecked(clientId, code, uid, packageName, attributionTag, -1, null, null, 1, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, attributionFlags, attributionChainId, false);
    }

    public SyncNotedAppOp startProxyOperation(IBinder clientId, int code, AttributionSource attributionSource, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation, int proxyAttributionFlags, int proxiedAttributionFlags, int attributionChainId) {
        return this.mCheckOpsDelegateDispatcher.startProxyOperation(clientId, code, attributionSource, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId);
    }

    /* JADX WARN: Removed duplicated region for block: B:82:0x006e  */
    /* JADX WARN: Removed duplicated region for block: B:83:0x0070  */
    /* JADX WARN: Removed duplicated region for block: B:93:0x008f  */
    /* JADX WARN: Removed duplicated region for block: B:95:0x0095  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public SyncNotedAppOp startProxyOperationImpl(IBinder clientId, int code, AttributionSource attributionSource, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation, int proxyAttributionFlags, int proxiedAttributionFlags, int attributionChainId) {
        boolean z;
        String resolvedProxiedPackageName;
        String proxiedAttributionTag;
        int proxiedUid;
        int proxyUid;
        int proxyUid2 = attributionSource.getUid();
        String proxyPackageName = attributionSource.getPackageName();
        String proxyAttributionTag = attributionSource.getAttributionTag();
        int proxiedUid2 = attributionSource.getNextUid();
        String proxiedPackageName = attributionSource.getNextPackageName();
        String proxiedAttributionTag2 = attributionSource.getNextAttributionTag();
        verifyIncomingProxyUid(attributionSource);
        verifyIncomingOp(code);
        verifyIncomingPackage(proxyPackageName, UserHandle.getUserId(proxyUid2));
        verifyIncomingPackage(proxiedPackageName, UserHandle.getUserId(proxiedUid2));
        boolean isCallerTrusted = isCallerAndAttributionTrusted(attributionSource);
        boolean skipProxyOperation2 = isCallerTrusted && skipProxyOperation;
        String resolvedProxyPackageName = AppOpsManager.resolvePackageName(proxyUid2, proxyPackageName);
        if (resolvedProxyPackageName == null) {
            return new SyncNotedAppOp(1, code, proxiedAttributionTag2, proxiedPackageName);
        }
        if (isCallerTrusted && attributionChainId != -1 && ((proxyAttributionFlags & 8) != 0 || (proxiedAttributionFlags & 8) != 0)) {
            z = true;
            boolean isChainTrusted = z;
            boolean isSelfBlame = Binder.getCallingUid() != proxiedUid2;
            boolean isProxyTrusted = this.mContext.checkPermission("android.permission.UPDATE_APP_OPS_STATS", -1, proxyUid2) != 0 || isSelfBlame || isChainTrusted;
            resolvedProxiedPackageName = AppOpsManager.resolvePackageName(proxiedUid2, proxiedPackageName);
            if (resolvedProxiedPackageName != null) {
                return new SyncNotedAppOp(1, code, proxiedAttributionTag2, proxiedPackageName);
            }
            int proxiedFlags = isProxyTrusted ? 8 : 16;
            if (skipProxyOperation2) {
                proxiedAttributionTag = proxiedAttributionTag2;
                proxiedUid = proxiedUid2;
                proxyUid = proxyUid2;
            } else {
                proxiedAttributionTag = proxiedAttributionTag2;
                proxiedUid = proxiedUid2;
                proxyUid = proxyUid2;
                SyncNotedAppOp testProxiedOp = startOperationUnchecked(clientId, code, proxiedUid2, resolvedProxiedPackageName, proxiedAttributionTag2, proxyUid2, resolvedProxyPackageName, proxyAttributionTag, proxiedFlags, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, proxiedAttributionFlags, attributionChainId, true);
                if (!shouldStartForMode(testProxiedOp.getOpMode(), startIfModeDefault)) {
                    return testProxiedOp;
                }
                int proxyFlags = isProxyTrusted ? 2 : 4;
                SyncNotedAppOp proxyAppOp = startOperationUnchecked(clientId, code, proxyUid, resolvedProxyPackageName, proxyAttributionTag, -1, null, null, proxyFlags, startIfModeDefault, !isProxyTrusted, "proxy " + message, shouldCollectMessage, proxyAttributionFlags, attributionChainId, false);
                if (!shouldStartForMode(proxyAppOp.getOpMode(), startIfModeDefault)) {
                    return proxyAppOp;
                }
            }
            return startOperationUnchecked(clientId, code, proxiedUid, resolvedProxiedPackageName, proxiedAttributionTag, proxyUid, resolvedProxyPackageName, proxyAttributionTag, proxiedFlags, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, proxiedAttributionFlags, attributionChainId, false);
        }
        z = false;
        boolean isChainTrusted2 = z;
        boolean isSelfBlame2 = Binder.getCallingUid() != proxiedUid2;
        boolean isProxyTrusted2 = this.mContext.checkPermission("android.permission.UPDATE_APP_OPS_STATS", -1, proxyUid2) != 0 || isSelfBlame2 || isChainTrusted2;
        resolvedProxiedPackageName = AppOpsManager.resolvePackageName(proxiedUid2, proxiedPackageName);
        if (resolvedProxiedPackageName != null) {
        }
    }

    private boolean shouldStartForMode(int mode, boolean startIfModeDefault) {
        return mode == 0 || (mode == 3 && startIfModeDefault);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4148=4] */
    /* JADX WARN: Code restructure failed: missing block: B:198:0x015d, code lost:
        r1.rejected(r8.state, r33);
        r0 = r1;
        scheduleOpStartedIfNeededLocked(r26, r27, r28, r15, r33, r1, 0, r38, r39);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private SyncNotedAppOp startOperationUnchecked(IBinder clientId, int code, int uid, String packageName, String attributionTag, int proxyUid, String proxyPackageName, String proxyAttributionTag, int flags, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, int attributionFlags, int attributionChainId, boolean dryRun) {
        String attributionTag2;
        Ops ops;
        UidState uidState;
        Op op;
        AttributedOp attributedOp;
        UidState uidState2;
        int uidMode;
        String attributionTag3;
        try {
            PackageVerificationResult pvr = verifyAndGetBypass(uid, packageName, attributionTag, proxyPackageName);
            String attributionTag4 = !pvr.isAttributionTagValid ? null : attributionTag;
            int startType = 0;
            synchronized (this) {
                try {
                    try {
                        String attributionTag5 = attributionTag4;
                        try {
                            Ops ops2 = getOpsLocked(uid, packageName, attributionTag4, pvr.isAttributionTagValid, pvr.bypass, true);
                            if (ops2 == null) {
                                if (!dryRun) {
                                    try {
                                        scheduleOpStartedIfNeededLocked(code, uid, packageName, attributionTag5, flags, 1, 0, attributionFlags, attributionChainId);
                                    } catch (Throwable th) {
                                        th = th;
                                    }
                                }
                                try {
                                    try {
                                        return new SyncNotedAppOp(2, code, attributionTag5, packageName);
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            } else {
                                try {
                                    Op op2 = getOpLocked(ops2, code, uid, true);
                                    AttributedOp attributedOp2 = op2.getOrCreateAttribution(op2, attributionTag5);
                                    UidState uidState3 = ops2.uidState;
                                    try {
                                        try {
                                            boolean isRestricted = isOpRestrictedLocked(uid, code, packageName, attributionTag5, pvr.bypass, false);
                                            int switchCode = AppOpsManager.opToSwitch(code);
                                            if (uidState3.opModes == null || uidState3.opModes.indexOfKey(switchCode) < 0) {
                                                attributionTag2 = attributionTag5;
                                                if (switchCode != code) {
                                                    ops = ops2;
                                                    uidState = uidState3;
                                                    op = getOpLocked(ops, switchCode, uid, true);
                                                } else {
                                                    ops = ops2;
                                                    uidState = uidState3;
                                                    op = op2;
                                                }
                                                Op switchOp = op;
                                                int mode = switchOp.evalMode();
                                                if (mode != 0) {
                                                    if (startIfModeDefault && mode == 3) {
                                                        attributedOp = attributedOp2;
                                                        uidState2 = uidState;
                                                    }
                                                    int mode2 = mode;
                                                    return new SyncNotedAppOp(mode2, code, attributionTag2, packageName);
                                                }
                                                attributedOp = attributedOp2;
                                                uidState2 = uidState;
                                            } else {
                                                int uidMode2 = uidState3.evalMode(code, uidState3.opModes.get(switchCode));
                                                if (!shouldStartForMode(uidMode2, startIfModeDefault)) {
                                                    if (dryRun) {
                                                        uidMode = uidMode2;
                                                        attributionTag3 = attributionTag5;
                                                    } else {
                                                        attributedOp2.rejected(uidState3.state, flags);
                                                        uidMode = uidMode2;
                                                        attributionTag3 = attributionTag5;
                                                        scheduleOpStartedIfNeededLocked(code, uid, packageName, attributionTag5, flags, uidMode, 0, attributionFlags, attributionChainId);
                                                    }
                                                    return new SyncNotedAppOp(uidMode, code, attributionTag3, packageName);
                                                }
                                                attributionTag2 = attributionTag5;
                                                attributedOp = attributedOp2;
                                                uidState2 = uidState3;
                                            }
                                            if (!dryRun) {
                                                try {
                                                    if (isRestricted) {
                                                        attributedOp.createPaused(clientId, proxyUid, proxyPackageName, proxyAttributionTag, uidState2.state, flags, attributionFlags, attributionChainId);
                                                    } else {
                                                        attributedOp.started(clientId, proxyUid, proxyPackageName, proxyAttributionTag, uidState2.state, flags, attributionFlags, attributionChainId);
                                                        startType = 1;
                                                    }
                                                    scheduleOpStartedIfNeededLocked(code, uid, packageName, attributionTag2, flags, isRestricted ? 1 : 0, startType, attributionFlags, attributionChainId);
                                                } catch (RemoteException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }
                                            if (shouldCollectAsyncNotedOp && !dryRun && !isRestricted) {
                                                collectAsyncNotedOp(uid, packageName, code, attributionTag2, 1, message, shouldCollectMessage);
                                            }
                                            return new SyncNotedAppOp(isRestricted ? 1 : 0, code, attributionTag2, packageName);
                                        } catch (Throwable th4) {
                                            th = th4;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                }
                            }
                        } catch (Throwable th7) {
                            th = th7;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                    }
                } catch (Throwable th9) {
                    th = th9;
                }
                throw th;
            }
        } catch (SecurityException e2) {
            Slog.e(TAG, "startOperation", e2);
            return new SyncNotedAppOp(2, code, attributionTag, packageName);
        }
    }

    public void finishOperation(IBinder clientId, int code, int uid, String packageName, String attributionTag) {
        this.mCheckOpsDelegateDispatcher.finishOperation(clientId, code, uid, packageName, attributionTag);
    }

    public void finishOperationImpl(IBinder clientId, int code, int uid, String packageName, String attributionTag) {
        verifyIncomingUid(uid);
        verifyIncomingOp(code);
        verifyIncomingPackage(packageName, UserHandle.getUserId(uid));
        String resolvedPackageName = AppOpsManager.resolvePackageName(uid, packageName);
        if (resolvedPackageName == null) {
            return;
        }
        finishOperationUnchecked(clientId, code, uid, resolvedPackageName, attributionTag);
    }

    public void finishProxyOperation(IBinder clientId, int code, AttributionSource attributionSource, boolean skipProxyOperation) {
        this.mCheckOpsDelegateDispatcher.finishProxyOperation(clientId, code, attributionSource, skipProxyOperation);
    }

    public Void finishProxyOperationImpl(IBinder clientId, int code, AttributionSource attributionSource, boolean skipProxyOperation) {
        int proxyUid = attributionSource.getUid();
        String proxyPackageName = attributionSource.getPackageName();
        String proxyAttributionTag = attributionSource.getAttributionTag();
        int proxiedUid = attributionSource.getNextUid();
        String proxiedPackageName = attributionSource.getNextPackageName();
        String proxiedAttributionTag = attributionSource.getNextAttributionTag();
        boolean skipProxyOperation2 = skipProxyOperation && isCallerAndAttributionTrusted(attributionSource);
        verifyIncomingProxyUid(attributionSource);
        verifyIncomingOp(code);
        verifyIncomingPackage(proxyPackageName, UserHandle.getUserId(proxyUid));
        verifyIncomingPackage(proxiedPackageName, UserHandle.getUserId(proxiedUid));
        String resolvedProxyPackageName = AppOpsManager.resolvePackageName(proxyUid, proxyPackageName);
        if (resolvedProxyPackageName == null) {
            return null;
        }
        if (!skipProxyOperation2) {
            finishOperationUnchecked(clientId, code, proxyUid, resolvedProxyPackageName, proxyAttributionTag);
        }
        String resolvedProxiedPackageName = AppOpsManager.resolvePackageName(proxiedUid, proxiedPackageName);
        if (resolvedProxiedPackageName == null) {
            return null;
        }
        finishOperationUnchecked(clientId, code, proxiedUid, resolvedProxiedPackageName, proxiedAttributionTag);
        return null;
    }

    private void finishOperationUnchecked(IBinder clientId, int code, int uid, String packageName, String attributionTag) {
        String attributionTag2;
        try {
            PackageVerificationResult pvr = verifyAndGetBypass(uid, packageName, attributionTag);
            if (pvr.isAttributionTagValid) {
                attributionTag2 = attributionTag;
            } else {
                attributionTag2 = null;
            }
            synchronized (this) {
                Op op = getOpLocked(code, uid, packageName, attributionTag2, pvr.isAttributionTagValid, pvr.bypass, true);
                if (op == null) {
                    Slog.e(TAG, "Operation not found: uid=" + uid + " pkg=" + packageName + "(" + attributionTag2 + ") op=" + AppOpsManager.opToName(code));
                    return;
                }
                AttributedOp attributedOp = op.mAttributions.get(attributionTag2);
                if (attributedOp == null) {
                    Slog.e(TAG, "Attribution not found: uid=" + uid + " pkg=" + packageName + "(" + attributionTag2 + ") op=" + AppOpsManager.opToName(code));
                    return;
                }
                if (!attributedOp.isRunning() && !attributedOp.isPaused()) {
                    Slog.e(TAG, "Operation not started: uid=" + uid + " pkg=" + packageName + "(" + attributionTag2 + ") op=" + AppOpsManager.opToName(code));
                }
                attributedOp.finished(clientId);
            }
        } catch (SecurityException e) {
            Slog.e(TAG, "Cannot finishOperation", e);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:39:0x002e, code lost:
        r12 = new android.util.ArraySet<>();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void scheduleOpActiveChangedIfNeededLocked(int code, int uid, String packageName, String attributionTag, boolean active, int attributionFlags, int attributionChainId) {
        int callbackListCount = this.mActiveWatchers.size();
        ArraySet<ActiveCallback> dispatchedCallbacks = null;
        for (int i = 0; i < callbackListCount; i++) {
            SparseArray<ActiveCallback> callbacks = this.mActiveWatchers.valueAt(i);
            ActiveCallback callback = callbacks.get(code);
            if (callback != null) {
                if (callback.mWatchingUid >= 0 && callback.mWatchingUid != uid) {
                }
                dispatchedCallbacks.add(callback);
            }
        }
        if (dispatchedCallbacks == null) {
            return;
        }
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new NonaConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda0
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9) {
                ((AppOpsService) obj).notifyOpActiveChanged((ArraySet) obj2, ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), (String) obj5, (String) obj6, ((Boolean) obj7).booleanValue(), ((Integer) obj8).intValue(), ((Integer) obj9).intValue());
            }
        }, this, dispatchedCallbacks, Integer.valueOf(code), Integer.valueOf(uid), packageName, attributionTag, Boolean.valueOf(active), Integer.valueOf(attributionFlags), Integer.valueOf(attributionChainId)));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4313=4] */
    public void notifyOpActiveChanged(ArraySet<ActiveCallback> callbacks, int code, int uid, String packageName, String attributionTag, boolean active, int attributionFlags, int attributionChainId) {
        ActiveCallback callback;
        long identity = Binder.clearCallingIdentity();
        try {
            int callbackCount = callbacks.size();
            for (int i = 0; i < callbackCount; i++) {
                try {
                    callback = callbacks.valueAt(i);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    try {
                        if (!shouldIgnoreCallback(code, callback.mCallingPid, callback.mCallingUid)) {
                            callback.mCallback.opActiveChanged(code, uid, packageName, attributionTag, active, attributionFlags, attributionChainId);
                        }
                    } catch (RemoteException e) {
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(identity);
                        throw th;
                    }
                } catch (RemoteException e2) {
                }
            }
            Binder.restoreCallingIdentity(identity);
        } catch (Throwable th3) {
            th = th3;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:39:0x002e, code lost:
        r14 = new android.util.ArraySet<>();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void scheduleOpStartedIfNeededLocked(int code, int uid, String pkgName, String attributionTag, int flags, int result, int startedType, int attributionFlags, int attributionChainId) {
        int callbackListCount = this.mStartedWatchers.size();
        ArraySet<StartedCallback> dispatchedCallbacks = null;
        for (int i = 0; i < callbackListCount; i++) {
            SparseArray<StartedCallback> callbacks = this.mStartedWatchers.valueAt(i);
            StartedCallback callback = callbacks.get(code);
            if (callback != null) {
                if (callback.mWatchingUid >= 0 && callback.mWatchingUid != uid) {
                }
                dispatchedCallbacks.add(callback);
            }
        }
        if (dispatchedCallbacks == null) {
            return;
        }
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new UndecConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda10
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11) {
                ((AppOpsService) obj).notifyOpStarted((ArraySet) obj2, ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), (String) obj5, (String) obj6, ((Integer) obj7).intValue(), ((Integer) obj8).intValue(), ((Integer) obj9).intValue(), ((Integer) obj10).intValue(), ((Integer) obj11).intValue());
            }
        }, this, dispatchedCallbacks, Integer.valueOf(code), Integer.valueOf(uid), pkgName, attributionTag, Integer.valueOf(flags), Integer.valueOf(result), Integer.valueOf(startedType), Integer.valueOf(attributionFlags), Integer.valueOf(attributionChainId)));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4369=4] */
    public void notifyOpStarted(ArraySet<StartedCallback> callbacks, int code, int uid, String packageName, String attributionTag, int flags, int result, int startedType, int attributionFlags, int attributionChainId) {
        StartedCallback callback;
        long identity = Binder.clearCallingIdentity();
        try {
            int callbackCount = callbacks.size();
            for (int i = 0; i < callbackCount; i++) {
                try {
                    callback = callbacks.valueAt(i);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    if (!shouldIgnoreCallback(code, callback.mCallingPid, callback.mCallingUid)) {
                        callback.mCallback.opStarted(code, uid, packageName, attributionTag, flags, result, startedType, attributionFlags, attributionChainId);
                    }
                } catch (RemoteException e) {
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(identity);
        } catch (Throwable th3) {
            th = th3;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:39:0x002d, code lost:
        r11 = new android.util.ArraySet<>();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void scheduleOpNotedIfNeededLocked(int code, int uid, String packageName, String attributionTag, int flags, int result) {
        int callbackListCount = this.mNotedWatchers.size();
        ArraySet<NotedCallback> dispatchedCallbacks = null;
        for (int i = 0; i < callbackListCount; i++) {
            SparseArray<NotedCallback> callbacks = this.mNotedWatchers.valueAt(i);
            NotedCallback callback = callbacks.get(code);
            if (callback != null) {
                if (callback.mWatchingUid >= 0 && callback.mWatchingUid != uid) {
                }
                dispatchedCallbacks.add(callback);
            }
        }
        if (dispatchedCallbacks == null) {
            return;
        }
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new OctConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda14
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8) {
                ((AppOpsService) obj).notifyOpChecked((ArraySet) obj2, ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), (String) obj5, (String) obj6, ((Integer) obj7).intValue(), ((Integer) obj8).intValue());
            }
        }, this, dispatchedCallbacks, Integer.valueOf(code), Integer.valueOf(uid), packageName, attributionTag, Integer.valueOf(flags), Integer.valueOf(result)));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4420=4] */
    public void notifyOpChecked(ArraySet<NotedCallback> callbacks, int code, int uid, String packageName, String attributionTag, int flags, int result) {
        NotedCallback callback;
        long identity = Binder.clearCallingIdentity();
        try {
            int callbackCount = callbacks.size();
            for (int i = 0; i < callbackCount; i++) {
                try {
                    callback = callbacks.valueAt(i);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    try {
                        if (!shouldIgnoreCallback(code, callback.mCallingPid, callback.mCallingUid)) {
                            callback.mCallback.opNoted(code, uid, packageName, attributionTag, flags, result);
                        }
                    } catch (RemoteException e) {
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(identity);
                        throw th;
                    }
                } catch (RemoteException e2) {
                }
            }
            Binder.restoreCallingIdentity(identity);
        } catch (Throwable th3) {
            th = th3;
        }
    }

    public int permissionToOpCode(String permission) {
        if (permission == null) {
            return -1;
        }
        return AppOpsManager.permissionToOpCode(permission);
    }

    public boolean shouldCollectNotes(int opCode) {
        CtaManager ctaManager = sCtaManager;
        int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
        Preconditions.checkArgumentInRange(opCode, 0, opNum - 1, "opCode");
        String perm = AppOpsManager.opToPermission(opCode);
        if (perm == null) {
            return false;
        }
        try {
            PermissionInfo permInfo = this.mContext.getPackageManager().getPermissionInfo(perm, 0);
            return permInfo.getProtection() == 1 || (permInfo.getProtectionFlags() & 64) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void verifyIncomingProxyUid(AttributionSource attributionSource) {
        if (attributionSource.getUid() == Binder.getCallingUid() || Binder.getCallingPid() == Process.myPid() || attributionSource.isTrusted(this.mContext)) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    private void verifyIncomingUid(int uid) {
        if (uid == Binder.getCallingUid() || Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    private boolean shouldIgnoreCallback(int op, int watcherPid, int watcherUid) {
        return AppOpsManager.opRestrictsRead(op) && this.mContext.checkPermission("android.permission.MANAGE_APPOPS", watcherPid, watcherUid) != 0;
    }

    private void verifyIncomingOp(int op) {
        CtaManager ctaManager = sCtaManager;
        int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
        if (op >= 0 && op < opNum) {
            if (AppOpsManager.opRestrictsRead(op)) {
                this.mContext.enforcePermission("android.permission.MANAGE_APPOPS", Binder.getCallingPid(), Binder.getCallingUid(), "verifyIncomingOp");
                return;
            }
            return;
        }
        throw new IllegalArgumentException("Bad operation #" + op);
    }

    private void verifyIncomingPackage(String packageName, int userId) {
        if (packageName != null && getPackageManagerInternal().filterAppAccess(packageName, Binder.getCallingUid(), userId)) {
            throw new IllegalArgumentException(packageName + " not found from " + Binder.getCallingUid());
        }
    }

    private boolean isCallerAndAttributionTrusted(AttributionSource attributionSource) {
        return (attributionSource.getUid() != Binder.getCallingUid() && attributionSource.isTrusted(this.mContext)) || this.mContext.checkPermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null) == 0;
    }

    private UidState getUidStateLocked(int uid, boolean edit) {
        UidState uidState = this.mUidStates.get(uid);
        if (uidState == null) {
            if (!edit) {
                return null;
            }
            UidState uidState2 = new UidState(uid);
            this.mUidStates.put(uid, uidState2);
            return uidState2;
        }
        updatePendingStateIfNeededLocked(uidState);
        return uidState;
    }

    private void updatePendingStateIfNeededLocked(UidState uidState) {
        if (uidState != null && uidState.pendingStateCommitTime != 0) {
            if (uidState.pendingStateCommitTime < this.mLastRealtime) {
                commitUidPendingStateLocked(uidState);
                return;
            }
            this.mLastRealtime = SystemClock.elapsedRealtime();
            if (uidState.pendingStateCommitTime < this.mLastRealtime) {
                commitUidPendingStateLocked(uidState);
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r8v18 */
    /* JADX WARN: Type inference failed for: r8v4, types: [int] */
    /* JADX WARN: Type inference failed for: r8v7 */
    private void commitUidPendingStateLocked(UidState uidState) {
        ArraySet<ModeCallback> callbacks;
        int pkgi;
        ModeCallback callback;
        int cbi;
        ArraySet<ModeCallback> callbacks2;
        AppOpsService appOpsService = this;
        if (uidState.hasForegroundWatchers) {
            boolean z = true;
            int fgi = uidState.foregroundOps.size() - 1;
            while (fgi >= 0) {
                if (uidState.foregroundOps.valueAt(fgi)) {
                    int code = uidState.foregroundOps.keyAt(fgi);
                    long firstUnrestrictedUidState = AppOpsManager.resolveFirstUnrestrictedUidState(code);
                    boolean resolvedLastFg = ((long) uidState.state) <= firstUnrestrictedUidState ? z ? 1 : 0 : false;
                    boolean resolvedNowFg = ((long) uidState.pendingState) <= firstUnrestrictedUidState ? z ? 1 : 0 : false;
                    if (resolvedLastFg != resolvedNowFg || uidState.capability != uidState.pendingCapability || uidState.appWidgetVisible != uidState.pendingAppWidgetVisible) {
                        int i = 4;
                        if (uidState.opModes != null && uidState.opModes.indexOfKey(code) >= 0 && uidState.opModes.get(code) == 4) {
                            appOpsService.mHandler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda7
                                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                                    ((AppOpsService) obj).notifyOpChangedForAllPkgsInUid(((Integer) obj2).intValue(), ((Integer) obj3).intValue(), ((Boolean) obj4).booleanValue(), (IAppOpsCallback) obj5);
                                }
                            }, this, Integer.valueOf(code), Integer.valueOf(uidState.uid), Boolean.valueOf(z), (Object) null));
                        } else if (uidState.pkgOps != null && (callbacks = appOpsService.mOpModeWatchers.get(code)) != null) {
                            int cbi2 = callbacks.size() - (z ? 1 : 0);
                            ?? r8 = z;
                            while (cbi2 >= 0) {
                                ModeCallback callback2 = callbacks.valueAt(cbi2);
                                if ((callback2.mFlags & r8) != 0 && callback2.isWatchingUid(uidState.uid)) {
                                    int pkgi2 = uidState.pkgOps.size() - r8;
                                    while (pkgi2 >= 0) {
                                        Op op = uidState.pkgOps.valueAt(pkgi2).get(code);
                                        if (op == null) {
                                            pkgi = pkgi2;
                                            callback = callback2;
                                            cbi = cbi2;
                                            callbacks2 = callbacks;
                                        } else if (op.mode != i) {
                                            pkgi = pkgi2;
                                            callback = callback2;
                                            cbi = cbi2;
                                            callbacks2 = callbacks;
                                        } else {
                                            pkgi = pkgi2;
                                            callback = callback2;
                                            cbi = cbi2;
                                            callbacks2 = callbacks;
                                            appOpsService.mHandler.sendMessage(PooledLambda.obtainMessage(new AppOpsService$$ExternalSyntheticLambda5(), this, callback2, Integer.valueOf(code), Integer.valueOf(uidState.uid), uidState.pkgOps.keyAt(pkgi2)));
                                        }
                                        pkgi2 = pkgi - 1;
                                        i = 4;
                                        appOpsService = this;
                                        callbacks = callbacks2;
                                        cbi2 = cbi;
                                        callback2 = callback;
                                    }
                                }
                                cbi2--;
                                i = 4;
                                appOpsService = this;
                                callbacks = callbacks;
                                r8 = 1;
                            }
                        }
                    }
                }
                fgi--;
                z = true;
                appOpsService = this;
            }
        }
        uidState.state = uidState.pendingState;
        uidState.capability = uidState.pendingCapability;
        uidState.appWidgetVisible = uidState.pendingAppWidgetVisible;
        uidState.pendingStateCommitTime = 0L;
    }

    public void updateAppWidgetVisibility(SparseArray<String> uidPackageNames, boolean visible) {
        synchronized (this) {
            for (int i = uidPackageNames.size() - 1; i >= 0; i--) {
                int uid = uidPackageNames.keyAt(i);
                UidState uidState = getUidStateLocked(uid, true);
                if (uidState != null && uidState.pendingAppWidgetVisible != visible) {
                    uidState.pendingAppWidgetVisible = visible;
                    if (uidState.pendingAppWidgetVisible != uidState.appWidgetVisible) {
                        commitUidPendingStateLocked(uidState);
                    }
                }
            }
        }
    }

    public PackageManagerInternal getPackageManagerInternal() {
        if (this.mPackageManagerInternal == null) {
            this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }
        return this.mPackageManagerInternal;
    }

    private AppOpsManager.RestrictionBypass getBypassforPackage(AndroidPackage pkg) {
        return new AppOpsManager.RestrictionBypass(pkg.getUid() == 1000, pkg.isPrivileged(), this.mContext.checkPermission("android.permission.EXEMPT_FROM_AUDIO_RECORD_RESTRICTIONS", -1, pkg.getUid()) == 0);
    }

    private PackageVerificationResult verifyAndGetBypass(int uid, String packageName, String attributionTag) {
        return verifyAndGetBypass(uid, packageName, attributionTag, null);
    }

    /* JADX WARN: Removed duplicated region for block: B:172:0x0041 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private PackageVerificationResult verifyAndGetBypass(int uid, String packageName, String attributionTag, String proxyPackageName) {
        int uid2;
        int pkgUid;
        String msg;
        Ops ops;
        if (uid == 0) {
            return new PackageVerificationResult(null, true);
        }
        if (Process.isSdkSandboxUid(uid)) {
            try {
                PackageManager pm = this.mContext.getPackageManager();
                String supplementalPackageName = pm.getSdkSandboxPackageName();
                if (!Objects.equals(packageName, supplementalPackageName)) {
                    uid2 = uid;
                } else {
                    uid2 = pm.getPackageUidAsUser(supplementalPackageName, PackageManager.PackageInfoFlags.of(0L), UserHandle.getUserId(uid));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            synchronized (this) {
                UidState uidState = this.mUidStates.get(uid2);
                if (uidState != null && uidState.pkgOps != null && (ops = uidState.pkgOps.get(packageName)) != null && ((attributionTag == null || ops.knownAttributionTags.contains(attributionTag)) && ops.bypass != null)) {
                    return new PackageVerificationResult(ops.bypass, ops.validAttributionTags.contains(attributionTag));
                }
                int callingUid = Binder.getCallingUid();
                if (Objects.equals(packageName, VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME)) {
                    pkgUid = 2000;
                } else {
                    int pkgUid2 = resolveUid(packageName);
                    pkgUid = pkgUid2;
                }
                if (pkgUid != -1) {
                    if (pkgUid != UserHandle.getAppId(uid2)) {
                        Slog.e(TAG, "Bad call made by uid " + callingUid + ". Package \"" + packageName + "\" does not belong to uid " + uid2 + ".");
                        throw new SecurityException("Specified package \"" + packageName + "\" under uid " + UserHandle.getAppId(uid2) + " but it is not");
                    }
                    return new PackageVerificationResult(AppOpsManager.RestrictionBypass.UNRESTRICTED, true);
                }
                int userId = UserHandle.getUserId(uid2);
                AppOpsManager.RestrictionBypass bypass = null;
                boolean isAttributionTagValid = false;
                long ident = Binder.clearCallingIdentity();
                try {
                    PackageManagerInternal pmInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                    AndroidPackage pkg = pmInt.getPackage(packageName);
                    if (pkg != null) {
                        isAttributionTagValid = isAttributionInPackage(pkg, attributionTag);
                        pkgUid = UserHandle.getUid(userId, UserHandle.getAppId(pkg.getUid()));
                        bypass = getBypassforPackage(pkg);
                    }
                    if (!isAttributionTagValid) {
                        AndroidPackage proxyPkg = proxyPackageName != null ? pmInt.getPackage(proxyPackageName) : null;
                        isAttributionTagValid = isAttributionInPackage(proxyPkg, attributionTag);
                        if (pkg != null && isAttributionTagValid) {
                            msg = "attributionTag " + attributionTag + " declared in manifest of the proxy package " + proxyPackageName + ", this is not advised";
                        } else if (pkg != null) {
                            msg = "attributionTag " + attributionTag + " not declared in manifest of " + packageName;
                        } else {
                            msg = "package " + packageName + " not found, can't check for attributionTag " + attributionTag;
                        }
                        try {
                            if (!this.mPlatformCompat.isChangeEnabledByPackageName(151105954L, packageName, userId) || !this.mPlatformCompat.isChangeEnabledByUid(151105954L, callingUid)) {
                                isAttributionTagValid = true;
                            }
                            Slog.e(TAG, msg);
                        } catch (RemoteException e2) {
                        }
                    }
                    if (pkgUid != uid2) {
                        Slog.e(TAG, "Bad call made by uid " + callingUid + ". Package \"" + packageName + "\" does not belong to uid " + uid2 + ".");
                        throw new SecurityException("Specified package \"" + packageName + "\" under uid " + uid2 + " but it is not");
                    }
                    return new PackageVerificationResult(bypass, isAttributionTagValid);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
        uid2 = uid;
        synchronized (this) {
        }
    }

    private boolean isAttributionInPackage(AndroidPackage pkg, String attributionTag) {
        if (pkg == null) {
            return false;
        }
        if (attributionTag == null) {
            return true;
        }
        if (pkg.getAttributions() != null) {
            int numAttributions = pkg.getAttributions().size();
            for (int i = 0; i < numAttributions; i++) {
                if (pkg.getAttributions().get(i).getTag().equals(attributionTag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Ops getOpsLocked(int uid, String packageName, String attributionTag, boolean isAttributionTagValid, AppOpsManager.RestrictionBypass bypass, boolean edit) {
        UidState uidState = getUidStateLocked(uid, edit);
        if (uidState == null) {
            return null;
        }
        if (uidState.pkgOps == null) {
            if (!edit) {
                return null;
            }
            uidState.pkgOps = new ArrayMap<>();
        }
        Ops ops = uidState.pkgOps.get(packageName);
        if (ops == null) {
            if (!edit) {
                return null;
            }
            ops = new Ops(packageName, uidState);
            uidState.pkgOps.put(packageName, ops);
        }
        if (edit) {
            if (bypass != null) {
                ops.bypass = bypass;
            }
            if (attributionTag != null) {
                ops.knownAttributionTags.add(attributionTag);
                if (isAttributionTagValid) {
                    ops.validAttributionTags.add(attributionTag);
                } else {
                    ops.validAttributionTags.remove(attributionTag);
                }
            }
        }
        return ops;
    }

    private void scheduleWriteLocked() {
        if (!this.mWriteScheduled) {
            this.mWriteScheduled = true;
            this.mHandler.postDelayed(this.mWriteRunner, 1800000L);
        }
    }

    public void scheduleFastWriteLocked() {
        if (!this.mFastWriteScheduled) {
            this.mWriteScheduled = true;
            this.mFastWriteScheduled = true;
            this.mHandler.removeCallbacks(this.mWriteRunner);
            this.mHandler.postDelayed(this.mWriteRunner, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    private Op getOpLocked(int code, int uid, String packageName, String attributionTag, boolean isAttributionTagValid, AppOpsManager.RestrictionBypass bypass, boolean edit) {
        Ops ops = getOpsLocked(uid, packageName, attributionTag, isAttributionTagValid, bypass, edit);
        if (ops == null) {
            return null;
        }
        return getOpLocked(ops, code, uid, edit);
    }

    private Op getOpLocked(Ops ops, int code, int uid, boolean edit) {
        Op op = ops.get(code);
        if (op == null) {
            if (!edit) {
                return null;
            }
            op = new Op(ops.uidState, ops.packageName, code, uid);
            ops.put(code, op);
        }
        if (edit) {
            scheduleWriteLocked();
        }
        return op;
    }

    private boolean isOpRestrictedDueToSuspend(int code, String packageName, int uid) {
        if (!ArrayUtils.contains(OPS_RESTRICTED_ON_SUSPEND, code)) {
            return false;
        }
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        return pmi.isPackageSuspended(packageName, UserHandle.getUserId(uid));
    }

    private boolean isOpRestrictedLocked(int uid, int code, String packageName, String attributionTag, AppOpsManager.RestrictionBypass appBypass, boolean isCheckOp) {
        int restrictionSetCount = this.mOpGlobalRestrictions.size();
        for (int i = 0; i < restrictionSetCount; i++) {
            ClientGlobalRestrictionState restrictionState = this.mOpGlobalRestrictions.valueAt(i);
            if (restrictionState.hasRestriction(code)) {
                return true;
            }
        }
        int userHandle = UserHandle.getUserId(uid);
        int restrictionSetCount2 = this.mOpUserRestrictions.size();
        for (int i2 = 0; i2 < restrictionSetCount2; i2++) {
            ClientUserRestrictionState restrictionState2 = this.mOpUserRestrictions.valueAt(i2);
            if (restrictionState2.hasRestriction(code, packageName, attributionTag, userHandle, isCheckOp)) {
                AppOpsManager.RestrictionBypass opBypass = AppOpsManager.opAllowSystemBypassRestriction(code);
                if (opBypass != null) {
                    synchronized (this) {
                        if (opBypass.isSystemUid && appBypass != null && appBypass.isSystemUid) {
                            return false;
                        }
                        if (opBypass.isPrivileged && appBypass != null && appBypass.isPrivileged) {
                            return false;
                        }
                        if (opBypass.isRecordAudioRestrictionExcept && appBypass != null && appBypass.isRecordAudioRestrictionExcept) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5033=9, 5034=8, 5037=8] */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:173:0x0093 -> B:215:0x017d). Please submit an issue!!! */
    void readState() {
        TypedXmlPullParser parser;
        int type;
        int oldVersion = -1;
        synchronized (this.mFile) {
            synchronized (this) {
                try {
                    FileInputStream stream = this.mFile.openRead();
                    try {
                        this.mUidStates.clear();
                    } catch (IOException e) {
                    }
                    try {
                        try {
                            try {
                                try {
                                    parser = Xml.resolvePullParser(stream);
                                    while (true) {
                                        type = parser.next();
                                        if (type == 2 || type == 1) {
                                            break;
                                        }
                                    }
                                } catch (IllegalStateException e2) {
                                    Slog.w(TAG, "Failed parsing " + e2);
                                    if (0 == 0) {
                                        this.mUidStates.clear();
                                    }
                                    stream.close();
                                } catch (XmlPullParserException e3) {
                                    Slog.w(TAG, "Failed parsing " + e3);
                                    if (0 == 0) {
                                        this.mUidStates.clear();
                                    }
                                    stream.close();
                                }
                            } catch (IOException e4) {
                                Slog.w(TAG, "Failed parsing " + e4);
                                if (0 == 0) {
                                    this.mUidStates.clear();
                                }
                                stream.close();
                            } catch (NumberFormatException e5) {
                                Slog.w(TAG, "Failed parsing " + e5);
                                if (0 == 0) {
                                    this.mUidStates.clear();
                                }
                                stream.close();
                            }
                        } catch (IndexOutOfBoundsException e6) {
                            Slog.w(TAG, "Failed parsing " + e6);
                            if (0 == 0) {
                                this.mUidStates.clear();
                            }
                            stream.close();
                        } catch (NullPointerException e7) {
                            Slog.w(TAG, "Failed parsing " + e7);
                            if (0 == 0) {
                                this.mUidStates.clear();
                            }
                            stream.close();
                        }
                        if (type != 2) {
                            throw new IllegalStateException("no start tag found");
                        }
                        oldVersion = parser.getAttributeInt((String) null, "v", -1);
                        int outerDepth = parser.getDepth();
                        while (true) {
                            int type2 = parser.next();
                            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                                break;
                            } else if (type2 != 3 && type2 != 4) {
                                String tagName = parser.getName();
                                if (tagName.equals("pkg")) {
                                    readPackage(parser);
                                } else if (tagName.equals(WatchlistLoggingHandler.WatchlistEventKeys.UID)) {
                                    readUidOps(parser);
                                } else {
                                    Slog.w(TAG, "Unknown element under <app-ops>: " + parser.getName());
                                    XmlUtils.skipCurrentTag(parser);
                                }
                            }
                        }
                        if (1 == 0) {
                            this.mUidStates.clear();
                        }
                        stream.close();
                    } catch (Throwable th) {
                        if (0 == 0) {
                            this.mUidStates.clear();
                        }
                        try {
                            stream.close();
                        } catch (IOException e8) {
                        }
                        throw th;
                    }
                } catch (FileNotFoundException e9) {
                    Slog.i(TAG, "No existing app ops " + this.mFile.getBaseFile() + "; starting empty");
                    return;
                }
            }
        }
        synchronized (this) {
            upgradeLocked(oldVersion);
        }
    }

    private void upgradeRunAnyInBackgroundLocked() {
        Op op;
        int idx;
        for (int i = 0; i < this.mUidStates.size(); i++) {
            UidState uidState = this.mUidStates.valueAt(i);
            if (uidState != null) {
                if (uidState.opModes != null && (idx = uidState.opModes.indexOfKey(63)) >= 0) {
                    uidState.opModes.put(70, uidState.opModes.valueAt(idx));
                }
                if (uidState.pkgOps != null) {
                    boolean changed = false;
                    for (int j = 0; j < uidState.pkgOps.size(); j++) {
                        Ops ops = uidState.pkgOps.valueAt(j);
                        if (ops != null && (op = ops.get(63)) != null && op.mode != AppOpsManager.opToDefaultMode(op.op)) {
                            Op copy = new Op(op.uidState, op.packageName, 70, uidState.uid);
                            copy.mode = op.mode;
                            ops.put(70, copy);
                            changed = true;
                        }
                    }
                    if (changed) {
                        uidState.evalForegroundOps(this.mOpModeWatchers);
                    }
                }
            }
        }
    }

    private void upgradeLocked(int oldVersion) {
        if (oldVersion < 1) {
            Slog.d(TAG, "Upgrading app-ops xml from version " + oldVersion + " to 1");
            switch (oldVersion) {
                case -1:
                    upgradeRunAnyInBackgroundLocked();
                    break;
            }
            scheduleFastWriteLocked();
        }
    }

    private void readUidOps(TypedXmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        int uid = parser.getAttributeInt((String) null, "n");
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals("op") || (sCtaManager.isCtaSupported() && tagName.equals("ctaop"))) {
                            int code = Integer.parseInt(parser.getAttributeValue((String) null, "n"));
                            int mode = Integer.parseInt(parser.getAttributeValue((String) null, "m"));
                            setUidMode(code, uid, mode);
                        } else {
                            Slog.w(TAG, "Unknown element under <uid-ops>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readPackage(TypedXmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        String pkgName = parser.getAttributeValue((String) null, "n");
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(WatchlistLoggingHandler.WatchlistEventKeys.UID)) {
                            readUid(parser, pkgName);
                        } else {
                            Slog.w(TAG, "Unknown element under <pkg>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readUid(TypedXmlPullParser parser, String pkgName) throws NumberFormatException, XmlPullParserException, IOException {
        int uid = parser.getAttributeInt((String) null, "n");
        UidState uidState = getUidStateLocked(uid, true);
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals("op") || (sCtaManager.isCtaSupported() && tagName.equals("ctaop"))) {
                    readOp(parser, uidState, pkgName);
                } else {
                    Slog.w(TAG, "Unknown element under <pkg>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        uidState.evalForegroundOps(this.mOpModeWatchers);
    }

    private void readAttributionOp(TypedXmlPullParser parser, Op parent, String attribution) throws NumberFormatException, IOException, XmlPullParserException {
        long rejectTime;
        long j;
        int opFlags;
        int uidState;
        AttributedOp attributedOp = parent.getOrCreateAttribution(parent, attribution);
        long key = parser.getAttributeLong((String) null, "n");
        int uidState2 = AppOpsManager.extractUidStateFromKey(key);
        int opFlags2 = AppOpsManager.extractFlagsFromKey(key);
        long accessTime = parser.getAttributeLong((String) null, "t", 0L);
        long rejectTime2 = parser.getAttributeLong((String) null, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD, 0L);
        long accessDuration = parser.getAttributeLong((String) null, "d", -1L);
        String proxyPkg = XmlUtils.readStringAttribute(parser, "pp");
        int proxyUid = parser.getAttributeInt((String) null, "pu", -1);
        String proxyAttributionTag = XmlUtils.readStringAttribute(parser, "pc");
        if (accessTime > 0) {
            rejectTime = rejectTime2;
            j = 0;
            opFlags = opFlags2;
            uidState = uidState2;
            attributedOp.accessed(accessTime, accessDuration, proxyUid, proxyPkg, proxyAttributionTag, uidState2, opFlags);
        } else {
            rejectTime = rejectTime2;
            j = 0;
            opFlags = opFlags2;
            uidState = uidState2;
        }
        if (rejectTime > j) {
            attributedOp.rejected(rejectTime, uidState, opFlags);
        }
    }

    private void readOp(TypedXmlPullParser parser, UidState uidState, String pkgName) throws NumberFormatException, XmlPullParserException, IOException {
        int opCode = parser.getAttributeInt((String) null, "n");
        Op op = new Op(uidState, pkgName, opCode, uidState.uid);
        int mode = parser.getAttributeInt((String) null, "m", AppOpsManager.opToDefaultMode(op.op));
        op.mode = mode;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals("st")) {
                    readAttributionOp(parser, op, XmlUtils.readStringAttribute(parser, "id"));
                } else {
                    Slog.w(TAG, "Unknown element under <op>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        if (uidState.pkgOps == null) {
            uidState.pkgOps = new ArrayMap<>();
        }
        Ops ops = uidState.pkgOps.get(pkgName);
        if (ops == null) {
            ops = new Ops(pkgName, uidState);
            uidState.pkgOps.put(pkgName, ops);
        }
        ops.put(op.op, op);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5409=8, 5413=4] */
    /* JADX WARN: Can't wrap try/catch for region: R(21:11|12|13|(4:15|16|(3:20|(2:22|23)|24)|25)|39|40|41|42|(8:43|(4:45|46|(2:62|63)(5:50|(3:52|(2:54|55)(1:57)|56)|58|59|60)|61)(1:70)|64|65|66|67|68|69)|71|(5:73|(8:77|(3:(1:80)|81|(1:83))|85|(11:88|89|(2:91|92)|93|94|(1:96)|97|(5:100|101|(2:102|(23:104|105|106|107|(2:109|(2:111|(3:114|115|116))(1:150))(1:151)|117|(3:145|146|147)(1:119)|120|121|122|(1:124)|125|(1:127)|128|(1:130)|131|(1:133)|(1:135)|(1:137)|(1:139)|140|141|116))|156|98)|157|158|86)|159|160|74|75)|161|162|(1:164))(1:186)|166|167|168|169|170|171|172|67|68|69) */
    /* JADX WARN: Code restructure failed: missing block: B:344:0x03b8, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:348:0x03be, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:349:0x03bf, code lost:
        r1 = r38;
     */
    /* JADX WARN: Code restructure failed: missing block: B:350:0x03c1, code lost:
        r3 = r29;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void writeState() {
        TypedXmlSerializer out;
        FileOutputStream stream;
        List<AppOpsManager.PackageOps> allOps;
        AppOpsManager.OpEntry op;
        String opTag;
        long rejectTime;
        long accessDuration;
        String proxyPkg;
        Iterator it;
        String proxyAttributionTag;
        AppOpsManager.AttributedOpEntry attribution;
        int proxyUid;
        int i;
        int j;
        int j2;
        String attributionTag;
        int uidStateCount;
        AppOpsService appOpsService = this;
        synchronized (appOpsService.mFile) {
            try {
                try {
                    FileOutputStream stream2 = appOpsService.mFile.startWrite();
                    List<AppOpsManager.PackageOps> allOps2 = appOpsService.getPackagesForOps(null);
                    try {
                        out = Xml.resolveSerializer(stream2);
                        out.startDocument((String) null, true);
                        out.startTag((String) null, "app-ops");
                        out.attributeInt((String) null, "v", 1);
                        try {
                        } catch (IOException e) {
                            e = e;
                        }
                    } catch (IOException e2) {
                        e = e2;
                    }
                    synchronized (this) {
                        try {
                            SparseArray<SparseIntArray> uidStatesClone = new SparseArray<>(appOpsService.mUidStates.size());
                            int uidStateCount2 = appOpsService.mUidStates.size();
                            for (int uidStateNum = 0; uidStateNum < uidStateCount2; uidStateNum++) {
                                try {
                                    int uid = appOpsService.mUidStates.keyAt(uidStateNum);
                                    SparseIntArray opModes = appOpsService.mUidStates.valueAt(uidStateNum).opModes;
                                    if (opModes != null && opModes.size() > 0) {
                                        uidStatesClone.put(uid, new SparseIntArray(opModes.size()));
                                        int opCount = opModes.size();
                                        for (int opCountNum = 0; opCountNum < opCount; opCountNum++) {
                                            uidStatesClone.get(uid).put(opModes.keyAt(opCountNum), opModes.valueAt(opCountNum));
                                        }
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
                            int uidStateCount3 = uidStatesClone.size();
                            int uidStateNum2 = 0;
                            while (true) {
                                int i2 = FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
                                if (uidStateNum2 >= uidStateCount3) {
                                    break;
                                }
                                try {
                                    SparseIntArray opModes2 = uidStatesClone.valueAt(uidStateNum2);
                                    if (opModes2 == null || opModes2.size() <= 0) {
                                        uidStateCount = uidStateCount3;
                                    } else {
                                        out.startTag((String) null, WatchlistLoggingHandler.WatchlistEventKeys.UID);
                                        out.attributeInt((String) null, "n", uidStatesClone.keyAt(uidStateNum2));
                                        int opCount2 = opModes2.size();
                                        int opCountNum2 = 0;
                                        while (opCountNum2 < opCount2) {
                                            int op2 = opModes2.keyAt(opCountNum2);
                                            int mode = opModes2.valueAt(opCountNum2);
                                            String opTag2 = "op";
                                            if (op2 >= i2) {
                                                opTag2 = "ctaop";
                                            }
                                            out.startTag((String) null, opTag2);
                                            out.attribute((String) null, "n", Integer.toString(op2));
                                            out.attribute((String) null, "m", Integer.toString(mode));
                                            out.endTag((String) null, opTag2);
                                            opCountNum2++;
                                            uidStateCount3 = uidStateCount3;
                                            i2 = FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
                                        }
                                        uidStateCount = uidStateCount3;
                                        out.endTag((String) null, WatchlistLoggingHandler.WatchlistEventKeys.UID);
                                    }
                                    uidStateNum2++;
                                    uidStateCount3 = uidStateCount;
                                } catch (IOException e3) {
                                    e = e3;
                                }
                                e = e3;
                                Slog.w(TAG, "Failed to write state, restoring backup.", e);
                                appOpsService.mFile.failWrite(stream2);
                                appOpsService.mHistoricalRegistry.writeAndClearDiscreteHistory();
                            }
                            if (allOps2 != null) {
                                String lastPkg = null;
                                int i3 = 0;
                                while (i3 < allOps2.size()) {
                                    try {
                                        try {
                                            AppOpsManager.PackageOps pkg = allOps2.get(i3);
                                            if (!Objects.equals(pkg.getPackageName(), lastPkg)) {
                                                if (lastPkg != null) {
                                                    out.endTag((String) null, "pkg");
                                                }
                                                lastPkg = pkg.getPackageName();
                                                if (lastPkg != null) {
                                                    out.startTag((String) null, "pkg");
                                                    out.attribute((String) null, "n", lastPkg);
                                                }
                                            }
                                            out.startTag((String) null, WatchlistLoggingHandler.WatchlistEventKeys.UID);
                                            out.attributeInt((String) null, "n", pkg.getUid());
                                            List<AppOpsManager.OpEntry> ops = pkg.getOps();
                                            int k = 0;
                                            while (k < ops.size()) {
                                                AppOpsManager.OpEntry op3 = ops.get(k);
                                                String opTag3 = op3.getOp() >= 121 ? "ctaop" : "op";
                                                out.startTag((String) null, opTag3);
                                                out.attribute((String) null, "n", Integer.toString(op3.getOp()));
                                                if (op3.getMode() != AppOpsManager.opToDefaultMode(op3.getOp())) {
                                                    out.attributeInt((String) null, "m", op3.getMode());
                                                }
                                                Iterator it2 = op3.getAttributedOpEntries().keySet().iterator();
                                                while (it2.hasNext()) {
                                                    String attributionTag2 = (String) it2.next();
                                                    AppOpsManager.AttributedOpEntry attribution2 = (AppOpsManager.AttributedOpEntry) op3.getAttributedOpEntries().get(attributionTag2);
                                                    ArraySet<Long> keys = attribution2.collectKeys();
                                                    int keyCount = keys.size();
                                                    String lastPkg2 = lastPkg;
                                                    int k2 = 0;
                                                    while (true) {
                                                        allOps = allOps2;
                                                        int keyCount2 = keyCount;
                                                        if (k2 < keyCount2) {
                                                            keyCount = keyCount2;
                                                            ArraySet<Long> keys2 = keys;
                                                            try {
                                                                long key = keys2.valueAt(k2).longValue();
                                                                int uidState = AppOpsManager.extractUidStateFromKey(key);
                                                                int flags = AppOpsManager.extractFlagsFromKey(key);
                                                                keys = keys2;
                                                                SparseArray<SparseIntArray> uidStatesClone2 = uidStatesClone;
                                                                long accessTime = attribution2.getLastAccessTime(uidState, uidState, flags);
                                                                long rejectTime2 = attribution2.getLastRejectTime(uidState, uidState, flags);
                                                                long accessDuration2 = attribution2.getLastDuration(uidState, uidState, flags);
                                                                AppOpsManager.OpEventProxyInfo proxy = attribution2.getLastProxyInfo(uidState, uidState, flags);
                                                                AppOpsManager.PackageOps pkg2 = pkg;
                                                                List<AppOpsManager.OpEntry> ops2 = ops;
                                                                try {
                                                                    if (accessTime <= 0) {
                                                                        op = op3;
                                                                        opTag = opTag3;
                                                                        rejectTime = rejectTime2;
                                                                        if (rejectTime <= 0) {
                                                                            stream = stream2;
                                                                            accessDuration = accessDuration2;
                                                                            if (accessDuration <= 0 && proxy == null) {
                                                                                i = i3;
                                                                                j = k;
                                                                                it = it2;
                                                                                attributionTag = attributionTag2;
                                                                                attribution = attribution2;
                                                                                j2 = k2;
                                                                                k2 = j2 + 1;
                                                                                appOpsService = this;
                                                                                allOps2 = allOps;
                                                                                uidStatesClone = uidStatesClone2;
                                                                                attributionTag2 = attributionTag;
                                                                                op3 = op;
                                                                                opTag3 = opTag;
                                                                                stream2 = stream;
                                                                                it2 = it;
                                                                                attribution2 = attribution;
                                                                                pkg = pkg2;
                                                                                ops = ops2;
                                                                                i3 = i;
                                                                                k = j;
                                                                            }
                                                                        } else {
                                                                            stream = stream2;
                                                                            accessDuration = accessDuration2;
                                                                        }
                                                                    } else {
                                                                        op = op3;
                                                                        opTag = opTag3;
                                                                        rejectTime = rejectTime2;
                                                                        stream = stream2;
                                                                        accessDuration = accessDuration2;
                                                                    }
                                                                    j = k;
                                                                    out.startTag((String) null, "st");
                                                                    if (attributionTag2 != null) {
                                                                        out.attribute((String) null, "id", attributionTag2);
                                                                    }
                                                                    j2 = k2;
                                                                    attributionTag = attributionTag2;
                                                                    out.attributeLong((String) null, "n", key);
                                                                    if (accessTime > 0) {
                                                                        out.attributeLong((String) null, "t", accessTime);
                                                                    }
                                                                    if (rejectTime > 0) {
                                                                        out.attributeLong((String) null, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD, rejectTime);
                                                                    }
                                                                    if (accessDuration > 0) {
                                                                        out.attributeLong((String) null, "d", accessDuration);
                                                                    }
                                                                    if (proxyPkg != null) {
                                                                        out.attribute((String) null, "pp", proxyPkg);
                                                                    }
                                                                    if (proxyAttributionTag != null) {
                                                                        out.attribute((String) null, "pc", proxyAttributionTag);
                                                                    }
                                                                    if (proxyUid >= 0) {
                                                                        out.attributeInt((String) null, "pu", proxyUid);
                                                                    }
                                                                    out.endTag((String) null, "st");
                                                                    k2 = j2 + 1;
                                                                    appOpsService = this;
                                                                    allOps2 = allOps;
                                                                    uidStatesClone = uidStatesClone2;
                                                                    attributionTag2 = attributionTag;
                                                                    op3 = op;
                                                                    opTag3 = opTag;
                                                                    stream2 = stream;
                                                                    it2 = it;
                                                                    attribution2 = attribution;
                                                                    pkg = pkg2;
                                                                    ops = ops2;
                                                                    i3 = i;
                                                                    k = j;
                                                                } catch (IOException e4) {
                                                                    e = e4;
                                                                    appOpsService = this;
                                                                    stream2 = stream;
                                                                    Slog.w(TAG, "Failed to write state, restoring backup.", e);
                                                                    appOpsService.mFile.failWrite(stream2);
                                                                    appOpsService.mHistoricalRegistry.writeAndClearDiscreteHistory();
                                                                }
                                                                if (proxy != null) {
                                                                    try {
                                                                        String proxyPkg2 = proxy.getPackageName();
                                                                        String proxyAttributionTag2 = proxy.getAttributionTag();
                                                                        int proxyUid2 = proxy.getUid();
                                                                        proxyPkg = proxyPkg2;
                                                                        it = it2;
                                                                        proxyAttributionTag = proxyAttributionTag2;
                                                                        attribution = attribution2;
                                                                        proxyUid = proxyUid2;
                                                                    } catch (IOException e5) {
                                                                        e = e5;
                                                                        stream2 = stream;
                                                                        Slog.w(TAG, "Failed to write state, restoring backup.", e);
                                                                        appOpsService.mFile.failWrite(stream2);
                                                                        appOpsService.mHistoricalRegistry.writeAndClearDiscreteHistory();
                                                                    }
                                                                } else {
                                                                    proxyPkg = null;
                                                                    it = it2;
                                                                    proxyAttributionTag = null;
                                                                    attribution = attribution2;
                                                                    proxyUid = -1;
                                                                }
                                                                i = i3;
                                                            } catch (IOException e6) {
                                                                e = e6;
                                                                appOpsService = this;
                                                            }
                                                        }
                                                    }
                                                    int j3 = k;
                                                    appOpsService = this;
                                                    allOps2 = allOps;
                                                    lastPkg = lastPkg2;
                                                    k = j3;
                                                }
                                                int j4 = k;
                                                out.endTag((String) null, "op");
                                                k = j4 + 1;
                                                appOpsService = this;
                                                allOps2 = allOps2;
                                                lastPkg = lastPkg;
                                                uidStatesClone = uidStatesClone;
                                                stream2 = stream2;
                                                pkg = pkg;
                                                ops = ops;
                                                i3 = i3;
                                            }
                                            out.endTag((String) null, WatchlistLoggingHandler.WatchlistEventKeys.UID);
                                            i3++;
                                            appOpsService = this;
                                            allOps2 = allOps2;
                                            lastPkg = lastPkg;
                                            uidStatesClone = uidStatesClone;
                                            stream2 = stream2;
                                        } catch (IOException e7) {
                                            e = e7;
                                            appOpsService = this;
                                        }
                                    } catch (Throwable th3) {
                                        e = th3;
                                        throw e;
                                    }
                                }
                                stream = stream2;
                                if (lastPkg != null) {
                                    out.endTag((String) null, "pkg");
                                }
                            } else {
                                stream = stream2;
                            }
                            out.endTag((String) null, "app-ops");
                            out.endDocument();
                            appOpsService = this;
                            appOpsService.mFile.finishWrite(stream);
                            appOpsService.mHistoricalRegistry.writeAndClearDiscreteHistory();
                        } catch (Throwable th4) {
                            th = th4;
                        }
                    }
                } catch (Throwable th5) {
                    e = th5;
                }
            } catch (IOException e8) {
                Slog.w(TAG, "Failed to write state: " + e8);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class Shell extends ShellCommand {
        static final Binder sBinder = new Binder();
        String attributionTag;
        final IAppOpsService mInterface;
        final AppOpsService mInternal;
        int mode;
        String modeStr;
        int nonpackageUid;
        int op;
        String opStr;
        String packageName;
        int packageUid;
        boolean targetsUid;
        int userId = 0;
        IBinder mToken = AppOpsManager.getClientId();

        Shell(IAppOpsService iface, AppOpsService internal) {
            this.mInterface = iface;
            this.mInternal = internal;
        }

        public int onCommand(String cmd) {
            return AppOpsService.onShellCommand(this, cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            AppOpsService.dumpCommandHelp(pw);
        }

        public static int strOpToOp(String op, PrintWriter err) {
            try {
                return AppOpsManager.strOpToOp(op);
            } catch (IllegalArgumentException e) {
                try {
                    return Integer.parseInt(op);
                } catch (NumberFormatException e2) {
                    try {
                        return AppOpsManager.strDebugOpToOp(op);
                    } catch (IllegalArgumentException e3) {
                        err.println("Error: " + e3.getMessage());
                        return -1;
                    }
                }
            }
        }

        static int strModeToMode(String modeStr, PrintWriter err) {
            for (int i = AppOpsManager.MODE_NAMES.length - 1; i >= 0; i--) {
                if (AppOpsManager.MODE_NAMES[i].equals(modeStr)) {
                    return i;
                }
            }
            try {
                int i2 = Integer.parseInt(modeStr);
                return i2;
            } catch (NumberFormatException e) {
                err.println("Error: Mode " + modeStr + " is not valid");
                return -1;
            }
        }

        int parseUserOpMode(int defMode, PrintWriter err) throws RemoteException {
            this.userId = -2;
            this.opStr = null;
            this.modeStr = null;
            while (true) {
                String argument = getNextArg();
                if (argument == null) {
                    break;
                } else if ("--user".equals(argument)) {
                    this.userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if (this.opStr == null) {
                    this.opStr = argument;
                } else if (this.modeStr == null) {
                    this.modeStr = argument;
                    break;
                }
            }
            String str = this.opStr;
            if (str == null) {
                err.println("Error: Operation not specified.");
                return -1;
            }
            int strOpToOp = strOpToOp(str, err);
            this.op = strOpToOp;
            if (strOpToOp < 0) {
                return -1;
            }
            String str2 = this.modeStr;
            if (str2 != null) {
                int strModeToMode = strModeToMode(str2, err);
                this.mode = strModeToMode;
                return strModeToMode < 0 ? -1 : 0;
            }
            this.mode = defMode;
            return 0;
        }

        /* JADX WARN: Code restructure failed: missing block: B:180:0x00cd, code lost:
            if (r0 >= r11.packageName.length()) goto L89;
         */
        /* JADX WARN: Code restructure failed: missing block: B:181:0x00cf, code lost:
            r4 = r11.packageName.substring(1, r0);
         */
        /* JADX WARN: Code restructure failed: missing block: B:182:0x00d5, code lost:
            r5 = java.lang.Integer.parseInt(r4);
            r8 = r11.packageName.charAt(r0);
            r0 = r0 + 1;
         */
        /* JADX WARN: Code restructure failed: missing block: B:184:0x00e8, code lost:
            if (r0 >= r11.packageName.length()) goto L85;
         */
        /* JADX WARN: Code restructure failed: missing block: B:186:0x00f0, code lost:
            if (r11.packageName.charAt(r0) < '0') goto L84;
         */
        /* JADX WARN: Code restructure failed: missing block: B:188:0x00f8, code lost:
            if (r11.packageName.charAt(r0) > '9') goto L70;
         */
        /* JADX WARN: Code restructure failed: missing block: B:189:0x00fa, code lost:
            r0 = r0 + 1;
         */
        /* JADX WARN: Code restructure failed: missing block: B:190:0x00fd, code lost:
            if (r0 <= r0) goto L89;
         */
        /* JADX WARN: Code restructure failed: missing block: B:191:0x00ff, code lost:
            r6 = r11.packageName.substring(r0, r0);
         */
        /* JADX WARN: Code restructure failed: missing block: B:192:0x0105, code lost:
            r7 = java.lang.Integer.parseInt(r6);
         */
        /* JADX WARN: Code restructure failed: missing block: B:193:0x010b, code lost:
            if (r8 != 'a') goto L79;
         */
        /* JADX WARN: Code restructure failed: missing block: B:194:0x010d, code lost:
            r11.nonpackageUid = android.os.UserHandle.getUid(r5, r7 + 10000);
         */
        /* JADX WARN: Code restructure failed: missing block: B:196:0x0118, code lost:
            if (r8 != 's') goto L77;
         */
        /* JADX WARN: Code restructure failed: missing block: B:197:0x011a, code lost:
            r11.nonpackageUid = android.os.UserHandle.getUid(r5, r7);
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        int parseUserPackageOp(boolean reqOp, PrintWriter err) throws RemoteException {
            this.userId = -2;
            this.packageName = null;
            this.opStr = null;
            while (true) {
                String argument = getNextArg();
                if (argument == null) {
                    break;
                } else if ("--user".equals(argument)) {
                    this.userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if ("--uid".equals(argument)) {
                    this.targetsUid = true;
                } else if ("--attribution".equals(argument)) {
                    this.attributionTag = getNextArgRequired();
                } else if (this.packageName == null) {
                    this.packageName = argument;
                } else if (this.opStr == null) {
                    this.opStr = argument;
                    break;
                }
            }
            if (this.packageName == null) {
                err.println("Error: Package name not specified.");
                return -1;
            }
            String str = this.opStr;
            if (str == null && reqOp) {
                err.println("Error: Operation not specified.");
                return -1;
            }
            if (str != null) {
                int strOpToOp = strOpToOp(str, err);
                this.op = strOpToOp;
                if (strOpToOp < 0) {
                    return -1;
                }
            } else {
                this.op = -1;
            }
            if (this.userId == -2) {
                this.userId = ActivityManager.getCurrentUser();
            }
            this.nonpackageUid = -1;
            try {
                this.nonpackageUid = Integer.parseInt(this.packageName);
            } catch (NumberFormatException e) {
            }
            if (this.nonpackageUid == -1 && this.packageName.length() > 1 && this.packageName.charAt(0) == 'u' && this.packageName.indexOf(46) < 0) {
                int i = 1;
                while (i < this.packageName.length() && this.packageName.charAt(i) >= '0' && this.packageName.charAt(i) <= '9') {
                    i++;
                }
            }
            int i2 = this.nonpackageUid;
            if (i2 != -1) {
                this.packageName = null;
            } else {
                int resolveUid = AppOpsService.resolveUid(this.packageName);
                this.packageUid = resolveUid;
                if (resolveUid < 0) {
                    this.packageUid = AppGlobals.getPackageManager().getPackageUid(this.packageName, 8192L, this.userId);
                }
                if (this.packageUid < 0) {
                    err.println("Error: No UID for " + this.packageName + " in user " + this.userId);
                    return -1;
                }
            }
            return 0;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.appop.AppOpsService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new Shell(this, this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    static void dumpCommandHelp(PrintWriter pw) {
        pw.println("AppOps service (appops) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  start [--user <USER_ID>] [--attribution <ATTRIBUTION_TAG>] <PACKAGE | UID> <OP> ");
        pw.println("    Starts a given operation for a particular application.");
        pw.println("  stop [--user <USER_ID>] [--attribution <ATTRIBUTION_TAG>] <PACKAGE | UID> <OP> ");
        pw.println("    Stops a given operation for a particular application.");
        pw.println("  set [--user <USER_ID>] <[--uid] PACKAGE | UID> <OP> <MODE>");
        pw.println("    Set the mode for a particular application and operation.");
        pw.println("  get [--user <USER_ID>] [--attribution <ATTRIBUTION_TAG>] <PACKAGE | UID> [<OP>]");
        pw.println("    Return the mode for a particular application and optional operation.");
        pw.println("  query-op [--user <USER_ID>] <OP> [<MODE>]");
        pw.println("    Print all packages that currently have the given op in the given mode.");
        pw.println("  reset [--user <USER_ID>] [<PACKAGE>]");
        pw.println("    Reset the given application or all applications to default modes.");
        pw.println("  write-settings");
        pw.println("    Immediately write pending changes to storage.");
        pw.println("  read-settings");
        pw.println("    Read the last written settings, replacing current state in RAM.");
        pw.println("  options:");
        pw.println("    <PACKAGE> an Android package name or its UID if prefixed by --uid");
        pw.println("    <OP>      an AppOps operation.");
        pw.println("    <MODE>    one of allow, ignore, deny, or default");
        pw.println("    <USER_ID> the user id under which the package is installed. If --user is");
        pw.println("              not specified, the current user is assumed.");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Multi-variable type inference failed */
    static int onShellCommand(Shell shell, String cmd) {
        char c;
        List<AppOpsManager.PackageOps> ops;
        int i;
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        PrintWriter err = shell.getErrPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case -1703718319:
                    if (cmd.equals("write-settings")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case -1166702330:
                    if (cmd.equals("query-op")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 102230:
                    if (cmd.equals("get")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 113762:
                    if (cmd.equals("set")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 3540994:
                    if (cmd.equals("stop")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case 108404047:
                    if (cmd.equals("reset")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 109757538:
                    if (cmd.equals("start")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 2085703290:
                    if (cmd.equals("read-settings")) {
                        c = 5;
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
                    int res = shell.parseUserPackageOp(true, err);
                    if (res < 0) {
                        return res;
                    }
                    String modeStr = shell.getNextArg();
                    if (modeStr == null) {
                        err.println("Error: Mode not specified.");
                        return -1;
                    }
                    int mode = Shell.strModeToMode(modeStr, err);
                    if (mode < 0) {
                        return -1;
                    }
                    if (!shell.targetsUid && shell.packageName != null) {
                        shell.mInterface.setMode(shell.op, shell.packageUid, shell.packageName, mode);
                        return 0;
                    } else if (!shell.targetsUid || shell.packageName == null) {
                        shell.mInterface.setUidMode(shell.op, shell.nonpackageUid, mode);
                        return 0;
                    } else {
                        try {
                            int uid = shell.mInternal.mContext.getPackageManager().getPackageUidAsUser(shell.packageName, shell.userId);
                            shell.mInterface.setUidMode(shell.op, uid, mode);
                            return 0;
                        } catch (PackageManager.NameNotFoundException e) {
                            return -1;
                        }
                    }
                case 1:
                    int res2 = shell.parseUserPackageOp(false, err);
                    if (res2 < 0) {
                        return res2;
                    }
                    List<AppOpsManager.PackageOps> ops2 = new ArrayList<>();
                    if (shell.packageName != null) {
                        List<AppOpsManager.PackageOps> r = shell.mInterface.getUidOps(shell.packageUid, shell.op != -1 ? new int[]{shell.op} : null);
                        if (r != null) {
                            ops2.addAll(r);
                        }
                        List<AppOpsManager.PackageOps> r2 = shell.mInterface.getOpsForPackage(shell.packageUid, shell.packageName, shell.op != -1 ? new int[]{shell.op} : null);
                        if (r2 != null) {
                            ops2.addAll(r2);
                        }
                    } else {
                        ops2 = shell.mInterface.getUidOps(shell.nonpackageUid, shell.op != -1 ? new int[]{shell.op} : null);
                    }
                    if (ops2 != null && ops2.size() > 0) {
                        long now = System.currentTimeMillis();
                        int i2 = 0;
                        while (i2 < ops2.size()) {
                            AppOpsManager.PackageOps packageOps = ops2.get(i2);
                            if (packageOps.getPackageName() == null) {
                                pw.print("Uid mode: ");
                            }
                            List<AppOpsManager.OpEntry> entries = packageOps.getOps();
                            int j = 0;
                            while (j < entries.size()) {
                                AppOpsManager.OpEntry ent = entries.get(j);
                                pw.print(AppOpsManager.opToName(ent.getOp()));
                                pw.print(": ");
                                pw.print(AppOpsManager.modeToName(ent.getMode()));
                                if (shell.attributionTag == null) {
                                    if (ent.getLastAccessTime(31) == -1) {
                                        ops = ops2;
                                    } else {
                                        pw.print("; time=");
                                        ops = ops2;
                                        TimeUtils.formatDuration(now - ent.getLastAccessTime(31), pw);
                                        pw.print(" ago");
                                    }
                                    if (ent.getLastRejectTime(31) != -1) {
                                        pw.print("; rejectTime=");
                                        TimeUtils.formatDuration(now - ent.getLastRejectTime(31), pw);
                                        pw.print(" ago");
                                    }
                                    if (ent.isRunning()) {
                                        pw.print(" (running)");
                                        i = i2;
                                    } else if (ent.getLastDuration(31) == -1) {
                                        i = i2;
                                    } else {
                                        pw.print("; duration=");
                                        TimeUtils.formatDuration(ent.getLastDuration(31), pw);
                                        i = i2;
                                    }
                                } else {
                                    ops = ops2;
                                    AppOpsManager.AttributedOpEntry attributionEnt = (AppOpsManager.AttributedOpEntry) ent.getAttributedOpEntries().get(shell.attributionTag);
                                    if (attributionEnt == null) {
                                        i = i2;
                                    } else {
                                        if (attributionEnt.getLastAccessTime(31) == -1) {
                                            i = i2;
                                        } else {
                                            pw.print("; time=");
                                            i = i2;
                                            TimeUtils.formatDuration(now - attributionEnt.getLastAccessTime(31), pw);
                                            pw.print(" ago");
                                        }
                                        if (attributionEnt.getLastRejectTime(31) != -1) {
                                            pw.print("; rejectTime=");
                                            TimeUtils.formatDuration(now - attributionEnt.getLastRejectTime(31), pw);
                                            pw.print(" ago");
                                        }
                                        if (attributionEnt.isRunning()) {
                                            pw.print(" (running)");
                                        } else if (attributionEnt.getLastDuration(31) != -1) {
                                            pw.print("; duration=");
                                            TimeUtils.formatDuration(attributionEnt.getLastDuration(31), pw);
                                        }
                                    }
                                }
                                pw.println();
                                j++;
                                i2 = i;
                                ops2 = ops;
                            }
                            i2++;
                            ops2 = ops2;
                        }
                        return 0;
                    }
                    pw.println("No operations.");
                    CtaManager ctaManager = sCtaManager;
                    int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
                    if (shell.op > -1 && shell.op < opNum) {
                        pw.println("Default mode: " + AppOpsManager.modeToName(AppOpsManager.opToDefaultMode(shell.op)));
                        return 0;
                    }
                    return 0;
                case 2:
                    int res3 = shell.parseUserOpMode(1, err);
                    if (res3 < 0) {
                        return res3;
                    }
                    List<AppOpsManager.PackageOps> ops3 = shell.mInterface.getPackagesForOps(new int[]{shell.op});
                    if (ops3 != null && ops3.size() > 0) {
                        for (int i3 = 0; i3 < ops3.size(); i3++) {
                            AppOpsManager.PackageOps pkg = ops3.get(i3);
                            boolean hasMatch = false;
                            List<AppOpsManager.OpEntry> entries2 = ops3.get(i3).getOps();
                            int j2 = 0;
                            while (true) {
                                if (j2 < entries2.size()) {
                                    AppOpsManager.OpEntry ent2 = entries2.get(j2);
                                    if (ent2.getOp() != shell.op || ent2.getMode() != shell.mode) {
                                        j2++;
                                    } else {
                                        hasMatch = true;
                                    }
                                }
                            }
                            if (hasMatch) {
                                pw.println(pkg.getPackageName());
                            }
                        }
                        return 0;
                    }
                    pw.println("No operations.");
                    return 0;
                case 3:
                    String packageName = null;
                    int userId = -2;
                    while (true) {
                        String argument = shell.getNextArg();
                        if (argument != null) {
                            if ("--user".equals(argument)) {
                                String userStr = shell.getNextArgRequired();
                                userId = UserHandle.parseUserArg(userStr);
                            } else if (packageName == null) {
                                packageName = argument;
                            } else {
                                err.println("Error: Unsupported argument: " + argument);
                                return -1;
                            }
                        } else {
                            if (userId == -2) {
                                userId = ActivityManager.getCurrentUser();
                            }
                            shell.mInterface.resetAllModes(userId, packageName);
                            pw.print("Reset all modes for: ");
                            if (userId == -1) {
                                pw.print("all users");
                            } else {
                                pw.print("user ");
                                pw.print(userId);
                            }
                            pw.print(", ");
                            if (packageName == null) {
                                pw.println("all packages");
                            } else {
                                pw.print("package ");
                                pw.println(packageName);
                            }
                            return 0;
                        }
                    }
                case 4:
                    shell.mInternal.enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), -1);
                    long token = Binder.clearCallingIdentity();
                    synchronized (shell.mInternal) {
                        shell.mInternal.mHandler.removeCallbacks(shell.mInternal.mWriteRunner);
                    }
                    shell.mInternal.writeState();
                    pw.println("Current settings written.");
                    Binder.restoreCallingIdentity(token);
                    return 0;
                case 5:
                    shell.mInternal.enforceManageAppOpsModes(Binder.getCallingPid(), Binder.getCallingUid(), -1);
                    long token2 = Binder.clearCallingIdentity();
                    shell.mInternal.readState();
                    pw.println("Last settings read.");
                    Binder.restoreCallingIdentity(token2);
                    return 0;
                case 6:
                    int res4 = shell.parseUserPackageOp(true, err);
                    if (res4 < 0) {
                        return res4;
                    }
                    if (shell.packageName != null) {
                        shell.mInterface.startOperation(shell.mToken, shell.op, shell.packageUid, shell.packageName, shell.attributionTag, true, true, "appops start shell command", true, 1, -1);
                        return 0;
                    }
                    return -1;
                case 7:
                    int res5 = shell.parseUserPackageOp(true, err);
                    if (res5 < 0) {
                        return res5;
                    }
                    if (shell.packageName != null) {
                        shell.mInterface.finishOperation(shell.mToken, shell.op, shell.packageUid, shell.packageName, shell.attributionTag);
                        return 0;
                    }
                    return -1;
                default:
                    return shell.handleDefaultCommands(cmd);
            }
        } catch (RemoteException e2) {
            pw.println("Remote exception: " + e2);
            return -1;
        }
    }

    private void dumpHelp(PrintWriter pw) {
        pw.println("AppOps service (appops) dump options:");
        pw.println("  -h");
        pw.println("    Print this help text.");
        pw.println("  --op [OP]");
        pw.println("    Limit output to data associated with the given app op code.");
        pw.println("  --mode [MODE]");
        pw.println("    Limit output to data associated with the given app op mode.");
        pw.println("  --package [PACKAGE]");
        pw.println("    Limit output to data associated with the given package name.");
        pw.println("  --attributionTag [attributionTag]");
        pw.println("    Limit output to data associated with the given attribution tag.");
        pw.println("  --include-discrete [n]");
        pw.println("    Include discrete ops limited to n per dimension. Use zero for no limit.");
        pw.println("  --watchers");
        pw.println("    Only output the watcher sections.");
        pw.println("  --history");
        pw.println("    Only output history.");
    }

    private void dumpStatesLocked(PrintWriter pw, String filterAttributionTag, int filter, long nowElapsed, Op op, long now, SimpleDateFormat sdf, Date date, String prefix) {
        int i;
        int numAttributions = op.mAttributions.size();
        for (i = 0; i < numAttributions; i = i + 1) {
            i = ((filter & 4) == 0 || Objects.equals(op.mAttributions.keyAt(i), filterAttributionTag)) ? 0 : i + 1;
            pw.print(prefix + op.mAttributions.keyAt(i) + "=[\n");
            dumpStatesLocked(pw, nowElapsed, op, op.mAttributions.keyAt(i), now, sdf, date, prefix + "  ");
            pw.print(prefix + "]\n");
        }
    }

    private void dumpStatesLocked(PrintWriter pw, long nowElapsed, Op op, String attributionTag, long now, SimpleDateFormat sdf, Date date, String prefix) {
        AppOpsManager.AttributedOpEntry entry;
        String proxyPkg;
        String proxyAttributionTag;
        int flags;
        String proxyAttributionTag2;
        String str;
        String str2;
        String str3;
        Date date2 = date;
        String str4 = prefix;
        AppOpsManager.AttributedOpEntry entry2 = (AppOpsManager.AttributedOpEntry) op.createSingleAttributionEntryLocked(attributionTag).getAttributedOpEntries().get(attributionTag);
        ArraySet<Long> keys = entry2.collectKeys();
        int keyCount = keys.size();
        int k = 0;
        while (k < keyCount) {
            long key = keys.valueAt(k).longValue();
            int uidState = AppOpsManager.extractUidStateFromKey(key);
            int flags2 = AppOpsManager.extractFlagsFromKey(key);
            long accessTime = entry2.getLastAccessTime(uidState, uidState, flags2);
            long rejectTime = entry2.getLastRejectTime(uidState, uidState, flags2);
            ArraySet<Long> keys2 = keys;
            int keyCount2 = keyCount;
            long accessDuration = entry2.getLastDuration(uidState, uidState, flags2);
            AppOpsManager.OpEventProxyInfo proxy = entry2.getLastProxyInfo(uidState, uidState, flags2);
            if (proxy == null) {
                entry = entry2;
                proxyPkg = null;
                proxyAttributionTag = null;
                flags = -1;
            } else {
                String proxyPkg2 = proxy.getPackageName();
                String proxyAttributionTag3 = proxy.getAttributionTag();
                int proxyUid = proxy.getUid();
                entry = entry2;
                proxyPkg = proxyPkg2;
                proxyAttributionTag = proxyAttributionTag3;
                flags = proxyUid;
            }
            int k2 = k;
            String proxyAttributionTag4 = proxyAttributionTag;
            if (accessTime <= 0) {
                proxyAttributionTag2 = proxyAttributionTag4;
                str = ", attributionTag=";
                str2 = "]";
            } else {
                pw.print(str4);
                pw.print("Access: ");
                pw.print(AppOpsManager.keyToString(key));
                pw.print(" ");
                date2.setTime(accessTime);
                pw.print(sdf.format(date));
                pw.print(" (");
                TimeUtils.formatDuration(accessTime - now, pw);
                pw.print(")");
                if (accessDuration > 0) {
                    pw.print(" duration=");
                    TimeUtils.formatDuration(accessDuration, pw);
                }
                if (flags < 0) {
                    proxyAttributionTag2 = proxyAttributionTag4;
                    str = ", attributionTag=";
                    str2 = "]";
                } else {
                    pw.print(" proxy[");
                    pw.print("uid=");
                    pw.print(flags);
                    pw.print(", pkg=");
                    pw.print(proxyPkg);
                    str = ", attributionTag=";
                    pw.print(str);
                    proxyAttributionTag2 = proxyAttributionTag4;
                    pw.print(proxyAttributionTag2);
                    str2 = "]";
                    pw.print(str2);
                }
                pw.println();
            }
            if (rejectTime <= 0) {
                str3 = prefix;
            } else {
                str3 = prefix;
                pw.print(str3);
                pw.print("Reject: ");
                pw.print(AppOpsManager.keyToString(key));
                date.setTime(rejectTime);
                pw.print(sdf.format(date));
                pw.print(" (");
                TimeUtils.formatDuration(rejectTime - now, pw);
                pw.print(")");
                if (flags >= 0) {
                    pw.print(" proxy[");
                    pw.print("uid=");
                    pw.print(flags);
                    pw.print(", pkg=");
                    pw.print(proxyPkg);
                    pw.print(str);
                    pw.print(proxyAttributionTag2);
                    pw.print(str2);
                }
                pw.println();
            }
            k = k2 + 1;
            date2 = date;
            str4 = str3;
            keys = keys2;
            keyCount = keyCount2;
            entry2 = entry;
        }
        String str5 = str4;
        AttributedOp attributedOp = op.mAttributions.get(attributionTag);
        if (attributedOp.isRunning()) {
            long earliestElapsedTime = JobStatus.NO_LATEST_RUNTIME;
            long maxNumStarts = 0;
            int numInProgressEvents = attributedOp.mInProgressEvents.size();
            for (int i = 0; i < numInProgressEvents; i++) {
                InProgressStartOpEvent event = (InProgressStartOpEvent) attributedOp.mInProgressEvents.valueAt(i);
                earliestElapsedTime = Math.min(earliestElapsedTime, event.getStartElapsedTime());
                maxNumStarts = Math.max(maxNumStarts, event.numUnfinishedStarts);
            }
            pw.print(str5 + "Running start at: ");
            TimeUtils.formatDuration(nowElapsed - earliestElapsedTime, pw);
            pw.println();
            if (maxNumStarts > 1) {
                pw.print(str5 + "startNesting=");
                pw.println(maxNumStarts);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [6392=4, 6580=4, 6674=10] */
    /* JADX WARN: Code restructure failed: missing block: B:1217:0x0930, code lost:
        if (r6 != r5) goto L459;
     */
    /* JADX WARN: Code restructure failed: missing block: B:950:0x0417, code lost:
        if (r5 != android.os.UserHandle.getAppId(r22.mWatchingUid)) goto L232;
     */
    /* JADX WARN: Code restructure failed: missing block: B:990:0x04e6, code lost:
        if (r5 != android.os.UserHandle.getAppId(r22.mWatchingUid)) goto L281;
     */
    /* JADX WARN: Removed duplicated region for block: B:1080:0x06ac A[Catch: all -> 0x0565, TRY_LEAVE, TryCatch #3 {all -> 0x0565, blocks: (B:961:0x0459, B:963:0x045f, B:965:0x046e, B:966:0x0473, B:967:0x0478, B:978:0x04ae, B:980:0x04b7, B:983:0x04c8, B:985:0x04d3, B:989:0x04de, B:994:0x04f0, B:995:0x04f6, B:998:0x0524, B:1000:0x052e, B:1002:0x053d, B:1003:0x0542, B:1004:0x0547, B:1014:0x057e, B:1016:0x0586, B:1019:0x0595, B:1021:0x059e, B:1025:0x05a7, B:1029:0x05ba, B:1030:0x05c0, B:1033:0x05ee, B:1035:0x05fa, B:1037:0x0609, B:1039:0x0611, B:1040:0x0616, B:1049:0x0641, B:1057:0x0652, B:1072:0x069a, B:1074:0x069e, B:1080:0x06ac), top: B:1376:0x0459 }] */
    /* JADX WARN: Removed duplicated region for block: B:1084:0x06b9  */
    /* JADX WARN: Removed duplicated region for block: B:1087:0x06be  */
    /* JADX WARN: Removed duplicated region for block: B:1088:0x06c1  */
    /* JADX WARN: Removed duplicated region for block: B:1090:0x06c5 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:1103:0x06ef  */
    /* JADX WARN: Removed duplicated region for block: B:1136:0x0769  */
    /* JADX WARN: Removed duplicated region for block: B:1139:0x0773 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:1144:0x0780 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:1399:0x0097 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:793:0x00ae  */
    @NeverCompile
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        boolean z;
        int dumpOp;
        String dumpAttributionTag;
        boolean dumpWatchers;
        boolean dumpHistory;
        boolean includeDiscreteOps;
        int nDiscreteOps;
        int dumpFilter;
        String dumpPackage;
        int dumpMode;
        int dumpMode2;
        Throwable th;
        boolean needSep;
        long now;
        boolean needSep2;
        SimpleDateFormat sdf;
        String dumpPackage2;
        int dumpOp2;
        int userRestrictionCount;
        long now2;
        String dumpPackage3;
        int dumpOp3;
        ClientUserRestrictionState restrictionState;
        long now3;
        int dumpOp4;
        int restrictedOpCount;
        boolean[] restrictedOps;
        UidState uidState;
        SparseIntArray opModes;
        ArrayMap<String, Ops> pkgOps;
        boolean needSep3;
        long nowElapsed;
        int dumpUid;
        int dumpMode3;
        int i;
        long now4;
        SimpleDateFormat sdf2;
        boolean hasOp;
        boolean hasOp2;
        boolean hasPackage;
        boolean hasMode;
        boolean hasPackage2;
        int dumpUid2;
        boolean hasOp3;
        boolean hasOp4;
        int i2;
        boolean z2;
        int pkgi;
        boolean printedPackage;
        Ops ops;
        boolean needSep4;
        int dumpUid3;
        int dumpUid4;
        String dumpPackage4;
        int dumpOp5;
        UidState uidState2;
        SparseIntArray opModes2;
        long now5;
        ArrayMap<String, Ops> pkgOps2;
        SimpleDateFormat sdf3;
        int opModeCount;
        int opModeCount2;
        long nowElapsed2;
        boolean z3;
        boolean needSep5;
        boolean printedHeader;
        boolean printedHeader2;
        boolean needSep6;
        int watchersSize;
        int watchersSize2;
        boolean needSep7;
        SimpleDateFormat sdf4;
        boolean needSep8;
        long now6;
        ArraySet<ModeCallback> callbacks;
        String dumpAttributionTag2;
        int dumpUid5;
        int dumpMode4;
        int dumpUid6;
        if (!DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            return;
        }
        int dumpOp6 = -1;
        String dumpPackage5 = null;
        String dumpAttributionTag3 = null;
        int dumpUid7 = -1;
        int dumpMode5 = -1;
        boolean dumpWatchers2 = false;
        boolean dumpHistory2 = false;
        boolean includeDiscreteOps2 = false;
        int nDiscreteOps2 = 10;
        int dumpFilter2 = 0;
        if (args != null) {
            int i3 = 0;
            while (i3 < args.length) {
                String arg = args[i3];
                int dumpOp7 = dumpOp6;
                if ("-h".equals(arg)) {
                    dumpHelp(pw);
                    return;
                }
                if ("-a".equals(arg)) {
                    dumpOp6 = dumpOp7;
                } else if ("--op".equals(arg)) {
                    i3++;
                    if (i3 >= args.length) {
                        pw.println("No argument for --op option");
                        return;
                    }
                    dumpOp6 = Shell.strOpToOp(args[i3], pw);
                    dumpFilter2 |= 8;
                    if (dumpOp6 < 0) {
                        return;
                    }
                } else if ("--package".equals(arg)) {
                    i3++;
                    if (i3 >= args.length) {
                        pw.println("No argument for --package option");
                        return;
                    }
                    String dumpPackage6 = args[i3];
                    int dumpFilter3 = dumpFilter2 | 2;
                    try {
                        dumpAttributionTag2 = dumpAttributionTag3;
                        dumpUid5 = dumpUid7;
                        dumpMode4 = dumpMode5;
                        try {
                            dumpUid6 = AppGlobals.getPackageManager().getPackageUid(dumpPackage6, 12591104L, 0);
                        } catch (RemoteException e) {
                            dumpUid6 = dumpUid5;
                            if (dumpUid6 >= 0) {
                            }
                        }
                    } catch (RemoteException e2) {
                        dumpAttributionTag2 = dumpAttributionTag3;
                        dumpUid5 = dumpUid7;
                        dumpMode4 = dumpMode5;
                    }
                    if (dumpUid6 >= 0) {
                        pw.println("Unknown package: " + dumpPackage6);
                        return;
                    }
                    dumpFilter2 = dumpFilter3 | 1;
                    dumpUid7 = UserHandle.getAppId(dumpUid6);
                    dumpAttributionTag3 = dumpAttributionTag2;
                    dumpMode5 = dumpMode4;
                    dumpPackage5 = dumpPackage6;
                    dumpOp6 = dumpOp7;
                } else {
                    String dumpAttributionTag4 = dumpAttributionTag3;
                    int dumpUid8 = dumpUid7;
                    int dumpMode6 = dumpMode5;
                    if ("--attributionTag".equals(arg)) {
                        i3++;
                        if (i3 >= args.length) {
                            pw.println("No argument for --attributionTag option");
                            return;
                        }
                        String dumpAttributionTag5 = args[i3];
                        dumpFilter2 |= 4;
                        dumpUid7 = dumpUid8;
                        dumpMode5 = dumpMode6;
                        dumpAttributionTag3 = dumpAttributionTag5;
                        dumpOp6 = dumpOp7;
                    } else if ("--mode".equals(arg)) {
                        i3++;
                        if (i3 >= args.length) {
                            pw.println("No argument for --mode option");
                            return;
                        }
                        int dumpMode7 = Shell.strModeToMode(args[i3], pw);
                        if (dumpMode7 < 0) {
                            return;
                        }
                        dumpMode5 = dumpMode7;
                        dumpOp6 = dumpOp7;
                        dumpAttributionTag3 = dumpAttributionTag4;
                        dumpUid7 = dumpUid8;
                    } else if ("--watchers".equals(arg)) {
                        dumpWatchers2 = true;
                        dumpOp6 = dumpOp7;
                        dumpAttributionTag3 = dumpAttributionTag4;
                        dumpUid7 = dumpUid8;
                        dumpMode5 = dumpMode6;
                    } else if ("--include-discrete".equals(arg)) {
                        i3++;
                        if (i3 >= args.length) {
                            pw.println("No argument for --include-discrete option");
                            return;
                        }
                        try {
                            int nDiscreteOps3 = Integer.valueOf(args[i3]).intValue();
                            nDiscreteOps2 = nDiscreteOps3;
                            includeDiscreteOps2 = true;
                            dumpOp6 = dumpOp7;
                            dumpAttributionTag3 = dumpAttributionTag4;
                            dumpUid7 = dumpUid8;
                            dumpMode5 = dumpMode6;
                        } catch (NumberFormatException e3) {
                            pw.println("Wrong parameter: " + args[i3]);
                            return;
                        }
                    } else if (!"--history".equals(arg)) {
                        int dumpOp8 = arg.length();
                        if (dumpOp8 <= 0 || arg.charAt(0) != '-') {
                            pw.println("Unknown command: " + arg);
                            return;
                        } else {
                            pw.println("Unknown option: " + arg);
                            return;
                        }
                    } else {
                        dumpHistory2 = true;
                        dumpOp6 = dumpOp7;
                        dumpAttributionTag3 = dumpAttributionTag4;
                        dumpUid7 = dumpUid8;
                        dumpMode5 = dumpMode6;
                    }
                }
                i3++;
            }
            int dumpMode8 = dumpMode5;
            z = true;
            dumpWatchers = dumpWatchers2;
            dumpHistory = dumpHistory2;
            includeDiscreteOps = includeDiscreteOps2;
            nDiscreteOps = nDiscreteOps2;
            dumpFilter = dumpFilter2;
            dumpOp = dumpOp6;
            dumpAttributionTag = dumpAttributionTag3;
            dumpMode2 = dumpUid7;
            dumpMode = dumpMode8;
            dumpPackage = dumpPackage5;
        } else {
            z = true;
            dumpOp = -1;
            dumpAttributionTag = null;
            dumpWatchers = false;
            dumpHistory = false;
            includeDiscreteOps = false;
            nDiscreteOps = 10;
            dumpFilter = 0;
            dumpPackage = null;
            dumpMode = -1;
            dumpMode2 = -1;
        }
        SimpleDateFormat sdf5 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date();
        synchronized (this) {
            try {
                try {
                    pw.println("Current AppOps Service state:");
                    if (!dumpHistory && !dumpWatchers) {
                        try {
                            this.mConstants.dump(pw);
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                    pw.println();
                    long now7 = System.currentTimeMillis();
                    long nowElapsed3 = SystemClock.elapsedRealtime();
                    SystemClock.uptimeMillis();
                    boolean needSep9 = false;
                    if (dumpFilter != 0 || dumpMode >= 0 || this.mProfileOwners == null || dumpWatchers || dumpHistory) {
                        needSep = false;
                    } else {
                        pw.println("  Profile owners:");
                        int poi = 0;
                        while (true) {
                            needSep = needSep9;
                            if (poi >= this.mProfileOwners.size()) {
                                break;
                            }
                            pw.print("    User #");
                            pw.print(this.mProfileOwners.keyAt(poi));
                            pw.print(": ");
                            UserHandle.formatUid(pw, this.mProfileOwners.valueAt(poi));
                            pw.println();
                            poi++;
                            needSep9 = needSep;
                        }
                        pw.println();
                    }
                    if (this.mOpModeWatchers.size() <= 0 || dumpHistory) {
                        now = now7;
                        needSep2 = needSep;
                    } else {
                        boolean printedHeader3 = false;
                        int i4 = 0;
                        while (true) {
                            boolean printedHeader4 = printedHeader3;
                            if (i4 >= this.mOpModeWatchers.size()) {
                                break;
                            }
                            if (dumpOp < 0 || dumpOp == this.mOpModeWatchers.keyAt(i4)) {
                                boolean printedOpHeader = false;
                                ArraySet<ModeCallback> callbacks2 = this.mOpModeWatchers.valueAt(i4);
                                int j = 0;
                                while (true) {
                                    now6 = now7;
                                    if (j >= callbacks2.size()) {
                                        break;
                                    }
                                    ModeCallback cb = callbacks2.valueAt(j);
                                    if (dumpPackage == null || dumpMode2 == UserHandle.getAppId(cb.mWatchingUid)) {
                                        if (printedHeader4) {
                                            callbacks = callbacks2;
                                        } else {
                                            callbacks = callbacks2;
                                            pw.println("  Op mode watchers:");
                                            printedHeader4 = true;
                                        }
                                        if (!printedOpHeader) {
                                            pw.print("    Op ");
                                            pw.print(AppOpsManager.opToName(this.mOpModeWatchers.keyAt(i4)));
                                            pw.println(":");
                                            printedOpHeader = true;
                                        }
                                        pw.print("      #");
                                        pw.print(j);
                                        pw.print(": ");
                                        pw.println(cb);
                                        needSep = true;
                                    } else {
                                        callbacks = callbacks2;
                                    }
                                    j++;
                                    now7 = now6;
                                    callbacks2 = callbacks;
                                }
                                printedHeader3 = printedHeader4;
                            } else {
                                now6 = now7;
                                printedHeader3 = printedHeader4;
                            }
                            i4++;
                            now7 = now6;
                        }
                        now = now7;
                        needSep2 = needSep;
                    }
                    if (this.mPackageModeWatchers.size() > 0 && dumpOp < 0 && !dumpHistory) {
                        boolean printedHeader5 = false;
                        for (int i5 = 0; i5 < this.mPackageModeWatchers.size(); i5++) {
                            if (dumpPackage == null || dumpPackage.equals(this.mPackageModeWatchers.keyAt(i5))) {
                                boolean needSep10 = true;
                                if (!printedHeader5) {
                                    pw.println("  Package mode watchers:");
                                    printedHeader5 = true;
                                }
                                pw.print("    Pkg ");
                                pw.print(this.mPackageModeWatchers.keyAt(i5));
                                pw.println(":");
                                ArraySet<ModeCallback> callbacks3 = this.mPackageModeWatchers.valueAt(i5);
                                int j2 = 0;
                                while (true) {
                                    needSep8 = needSep10;
                                    if (j2 >= callbacks3.size()) {
                                        break;
                                    }
                                    pw.print("      #");
                                    pw.print(j2);
                                    pw.print(": ");
                                    pw.println(callbacks3.valueAt(j2));
                                    j2++;
                                    needSep10 = needSep8;
                                }
                                needSep2 = needSep8;
                            }
                        }
                    }
                    if (this.mModeWatchers.size() > 0 && dumpOp < 0 && !dumpHistory) {
                        boolean printedHeader6 = false;
                        for (int i6 = 0; i6 < this.mModeWatchers.size(); i6++) {
                            ModeCallback cb2 = this.mModeWatchers.valueAt(i6);
                            if (dumpPackage == null || dumpMode2 == UserHandle.getAppId(cb2.mWatchingUid)) {
                                needSep2 = true;
                                if (!printedHeader6) {
                                    pw.println("  All op mode watchers:");
                                    printedHeader6 = true;
                                }
                                pw.print("    ");
                                pw.print(Integer.toHexString(System.identityHashCode(this.mModeWatchers.keyAt(i6))));
                                pw.print(": ");
                                pw.println(cb2);
                            }
                        }
                    }
                    if (this.mActiveWatchers.size() <= 0 || dumpMode >= 0) {
                        sdf = sdf5;
                    } else {
                        needSep2 = true;
                        boolean printedHeader7 = false;
                        int watcherNum = 0;
                        while (watcherNum < this.mActiveWatchers.size()) {
                            try {
                                SparseArray<ActiveCallback> activeWatchers = this.mActiveWatchers.valueAt(watcherNum);
                                if (activeWatchers.size() <= 0) {
                                    needSep7 = needSep2;
                                } else {
                                    ActiveCallback cb3 = activeWatchers.valueAt(0);
                                    if (dumpOp >= 0) {
                                        if (activeWatchers.indexOfKey(dumpOp) < 0) {
                                            needSep7 = needSep2;
                                        }
                                    }
                                    if (dumpPackage != null) {
                                        needSep7 = needSep2;
                                    } else {
                                        needSep7 = needSep2;
                                    }
                                    if (!printedHeader7) {
                                        pw.println("  All op active watchers:");
                                        printedHeader7 = true;
                                    }
                                    pw.print("    ");
                                    pw.print(Integer.toHexString(System.identityHashCode(this.mActiveWatchers.keyAt(watcherNum))));
                                    pw.println(" ->");
                                    pw.print("        [");
                                    int opCount = activeWatchers.size();
                                    boolean printedHeader8 = printedHeader7;
                                    int opNum = 0;
                                    while (opNum < opCount) {
                                        if (opNum > 0) {
                                            sdf = sdf5;
                                            try {
                                                pw.print(' ');
                                            } catch (Throwable th3) {
                                                th = th3;
                                                throw th;
                                            }
                                        } else {
                                            sdf = sdf5;
                                        }
                                        pw.print(AppOpsManager.opToName(activeWatchers.keyAt(opNum)));
                                        if (opNum < opCount - 1) {
                                            pw.print(',');
                                        }
                                        opNum++;
                                        sdf5 = sdf;
                                    }
                                    sdf4 = sdf5;
                                    pw.println("]");
                                    pw.print("        ");
                                    pw.println(cb3);
                                    printedHeader7 = printedHeader8;
                                    watcherNum++;
                                    needSep2 = needSep7;
                                    sdf5 = sdf4;
                                }
                                sdf4 = sdf5;
                                watcherNum++;
                                needSep2 = needSep7;
                                sdf5 = sdf4;
                            } catch (Throwable th4) {
                                th = th4;
                                throw th;
                            }
                        }
                        sdf = sdf5;
                    }
                    try {
                        if (this.mStartedWatchers.size() > 0 && dumpMode < 0) {
                            needSep2 = true;
                            boolean printedHeader9 = false;
                            int watchersSize3 = this.mStartedWatchers.size();
                            int watcherNum2 = 0;
                            while (watcherNum2 < watchersSize3) {
                                SparseArray<StartedCallback> startedWatchers = this.mStartedWatchers.valueAt(watcherNum2);
                                if (startedWatchers.size() <= 0) {
                                    needSep6 = needSep2;
                                } else {
                                    StartedCallback cb4 = startedWatchers.valueAt(0);
                                    if (dumpOp < 0 || startedWatchers.indexOfKey(dumpOp) >= 0) {
                                        if (dumpPackage != null) {
                                            needSep6 = needSep2;
                                        } else {
                                            needSep6 = needSep2;
                                        }
                                        if (!printedHeader9) {
                                            pw.println("  All op started watchers:");
                                            printedHeader9 = true;
                                        }
                                        pw.print("    ");
                                        pw.print(Integer.toHexString(System.identityHashCode(this.mStartedWatchers.keyAt(watcherNum2))));
                                        pw.println(" ->");
                                        pw.print("        [");
                                        int opCount2 = startedWatchers.size();
                                        boolean printedHeader10 = printedHeader9;
                                        int opNum2 = 0;
                                        while (opNum2 < opCount2) {
                                            if (opNum2 > 0) {
                                                watchersSize2 = watchersSize3;
                                                pw.print(' ');
                                            } else {
                                                watchersSize2 = watchersSize3;
                                            }
                                            int watchersSize4 = startedWatchers.keyAt(opNum2);
                                            pw.print(AppOpsManager.opToName(watchersSize4));
                                            if (opNum2 < opCount2 - 1) {
                                                pw.print(',');
                                            }
                                            opNum2++;
                                            watchersSize3 = watchersSize2;
                                        }
                                        watchersSize = watchersSize3;
                                        pw.println("]");
                                        pw.print("        ");
                                        pw.println(cb4);
                                        printedHeader9 = printedHeader10;
                                        watcherNum2++;
                                        needSep2 = needSep6;
                                        watchersSize3 = watchersSize;
                                    } else {
                                        needSep6 = needSep2;
                                    }
                                }
                                watchersSize = watchersSize3;
                                watcherNum2++;
                                needSep2 = needSep6;
                                watchersSize3 = watchersSize;
                            }
                        }
                        if (this.mNotedWatchers.size() > 0 && dumpMode < 0) {
                            needSep2 = true;
                            boolean printedHeader11 = false;
                            int watcherNum3 = 0;
                            while (watcherNum3 < this.mNotedWatchers.size()) {
                                SparseArray<NotedCallback> notedWatchers = this.mNotedWatchers.valueAt(watcherNum3);
                                if (notedWatchers.size() > 0) {
                                    NotedCallback cb5 = notedWatchers.valueAt(0);
                                    if ((dumpOp < 0 || notedWatchers.indexOfKey(dumpOp) >= 0) && (dumpPackage == null || dumpMode2 == UserHandle.getAppId(cb5.mWatchingUid))) {
                                        if (!printedHeader11) {
                                            pw.println("  All op noted watchers:");
                                            printedHeader11 = true;
                                        }
                                        pw.print("    ");
                                        pw.print(Integer.toHexString(System.identityHashCode(this.mNotedWatchers.keyAt(watcherNum3))));
                                        pw.println(" ->");
                                        pw.print("        [");
                                        int opCount3 = notedWatchers.size();
                                        needSep5 = needSep2;
                                        int opNum3 = 0;
                                        while (opNum3 < opCount3) {
                                            if (opNum3 > 0) {
                                                printedHeader2 = printedHeader11;
                                                pw.print(' ');
                                            } else {
                                                printedHeader2 = printedHeader11;
                                            }
                                            pw.print(AppOpsManager.opToName(notedWatchers.keyAt(opNum3)));
                                            if (opNum3 < opCount3 - 1) {
                                                pw.print(',');
                                            }
                                            opNum3++;
                                            printedHeader11 = printedHeader2;
                                        }
                                        printedHeader = printedHeader11;
                                        pw.println("]");
                                        pw.print("        ");
                                        pw.println(cb5);
                                        watcherNum3++;
                                        printedHeader11 = printedHeader;
                                        needSep2 = needSep5;
                                    }
                                }
                                needSep5 = needSep2;
                                printedHeader = printedHeader11;
                                watcherNum3++;
                                printedHeader11 = printedHeader;
                                needSep2 = needSep5;
                            }
                        }
                        if (this.mAudioRestrictionManager.hasActiveRestrictions() && dumpOp < 0 && dumpPackage != null && dumpMode < 0 && !dumpWatchers) {
                            if (!this.mAudioRestrictionManager.dump(pw) && !needSep2) {
                                z3 = false;
                                needSep2 = z3;
                            }
                            z3 = true;
                            needSep2 = z3;
                        }
                        if (needSep2) {
                            pw.println();
                        }
                        int i7 = 0;
                        while (i7 < this.mUidStates.size()) {
                            try {
                                uidState = this.mUidStates.valueAt(i7);
                                opModes = uidState.opModes;
                                pkgOps = uidState.pkgOps;
                            } catch (Throwable th5) {
                                th = th5;
                            }
                            try {
                                if (dumpWatchers) {
                                    needSep3 = needSep2;
                                    nowElapsed = nowElapsed3;
                                    dumpUid = dumpMode2;
                                    dumpMode3 = dumpMode;
                                    dumpPackage2 = dumpPackage;
                                    dumpOp2 = dumpOp;
                                    i = i7;
                                    now4 = now;
                                    sdf2 = sdf;
                                } else if (dumpHistory) {
                                    needSep3 = needSep2;
                                    nowElapsed = nowElapsed3;
                                    dumpUid = dumpMode2;
                                    dumpMode3 = dumpMode;
                                    dumpPackage2 = dumpPackage;
                                    dumpOp2 = dumpOp;
                                    i = i7;
                                    now4 = now;
                                    sdf2 = sdf;
                                } else {
                                    if (dumpOp >= 0 || dumpPackage != null || dumpMode >= 0) {
                                        if (dumpOp >= 0) {
                                            if (uidState.opModes == null || uidState.opModes.indexOfKey(dumpOp) < 0) {
                                                hasOp = false;
                                                if (dumpPackage == null) {
                                                    hasOp2 = hasOp;
                                                    if (dumpMode2 != this.mUidStates.keyAt(i7)) {
                                                        hasPackage = false;
                                                        hasMode = dumpMode < 0;
                                                        if (hasMode && opModes != null) {
                                                            hasPackage2 = hasPackage;
                                                            int opi = 0;
                                                            while (true) {
                                                                if (hasMode) {
                                                                    dumpUid2 = dumpMode2;
                                                                    break;
                                                                }
                                                                dumpUid2 = dumpMode2;
                                                                int dumpUid9 = opModes.size();
                                                                if (opi >= dumpUid9) {
                                                                    break;
                                                                }
                                                                if (opModes.valueAt(opi) == dumpMode) {
                                                                    hasMode = true;
                                                                }
                                                                opi++;
                                                                dumpMode2 = dumpUid2;
                                                            }
                                                        } else {
                                                            hasPackage2 = hasPackage;
                                                            dumpUid2 = dumpMode2;
                                                        }
                                                        if (pkgOps != null) {
                                                            int pkgi2 = 0;
                                                            hasOp3 = hasOp2;
                                                            while (true) {
                                                                if (hasOp3 && hasPackage2 && hasMode) {
                                                                    needSep3 = needSep2;
                                                                    i = i7;
                                                                    break;
                                                                }
                                                                needSep3 = needSep2;
                                                                if (pkgi2 >= pkgOps.size()) {
                                                                    i = i7;
                                                                    break;
                                                                }
                                                                Ops ops2 = pkgOps.valueAt(pkgi2);
                                                                if (!hasOp3 && ops2 != null && ops2.indexOfKey(dumpOp) >= 0) {
                                                                    hasOp3 = true;
                                                                }
                                                                if (!hasMode) {
                                                                    hasOp4 = hasOp3;
                                                                    int opi2 = 0;
                                                                    while (true) {
                                                                        if (hasMode) {
                                                                            i2 = i7;
                                                                            break;
                                                                        }
                                                                        i2 = i7;
                                                                        int i8 = ops2.size();
                                                                        if (opi2 >= i8) {
                                                                            break;
                                                                        }
                                                                        if (ops2.valueAt(opi2).mode == dumpMode) {
                                                                            hasMode = true;
                                                                        }
                                                                        opi2++;
                                                                        i7 = i2;
                                                                    }
                                                                } else {
                                                                    hasOp4 = hasOp3;
                                                                    i2 = i7;
                                                                }
                                                                if (!hasPackage2 && dumpPackage.equals(ops2.packageName)) {
                                                                    hasPackage2 = true;
                                                                }
                                                                pkgi2++;
                                                                needSep2 = needSep3;
                                                                hasOp3 = hasOp4;
                                                                i7 = i2;
                                                            }
                                                        } else {
                                                            needSep3 = needSep2;
                                                            i = i7;
                                                            hasOp3 = hasOp2;
                                                        }
                                                        if (uidState.foregroundOps != null && !hasOp3 && uidState.foregroundOps.indexOfKey(dumpOp) > 0) {
                                                            hasOp3 = true;
                                                        }
                                                        if (hasOp3 || !hasPackage2) {
                                                            nowElapsed = nowElapsed3;
                                                            dumpPackage2 = dumpPackage;
                                                            dumpOp2 = dumpOp;
                                                            dumpUid = dumpUid2;
                                                            dumpMode3 = dumpMode;
                                                            now4 = now;
                                                            sdf2 = sdf;
                                                        } else if (!hasMode) {
                                                            nowElapsed = nowElapsed3;
                                                            dumpPackage2 = dumpPackage;
                                                            dumpOp2 = dumpOp;
                                                            now4 = now;
                                                            sdf2 = sdf;
                                                            dumpUid = dumpUid2;
                                                            dumpMode3 = dumpMode;
                                                        }
                                                    }
                                                } else {
                                                    hasOp2 = hasOp;
                                                }
                                                hasPackage = true;
                                                if (dumpMode < 0) {
                                                }
                                                if (hasMode) {
                                                }
                                                hasPackage2 = hasPackage;
                                                dumpUid2 = dumpMode2;
                                                if (pkgOps != null) {
                                                }
                                                if (uidState.foregroundOps != null) {
                                                    hasOp3 = true;
                                                }
                                                if (hasOp3) {
                                                }
                                                nowElapsed = nowElapsed3;
                                                dumpPackage2 = dumpPackage;
                                                dumpOp2 = dumpOp;
                                                dumpUid = dumpUid2;
                                                dumpMode3 = dumpMode;
                                                now4 = now;
                                                sdf2 = sdf;
                                            }
                                        }
                                        hasOp = true;
                                        if (dumpPackage == null) {
                                        }
                                        hasPackage = true;
                                        if (dumpMode < 0) {
                                        }
                                        if (hasMode) {
                                        }
                                        hasPackage2 = hasPackage;
                                        dumpUid2 = dumpMode2;
                                        if (pkgOps != null) {
                                        }
                                        if (uidState.foregroundOps != null) {
                                        }
                                        if (hasOp3) {
                                        }
                                        nowElapsed = nowElapsed3;
                                        dumpPackage2 = dumpPackage;
                                        dumpOp2 = dumpOp;
                                        dumpUid = dumpUid2;
                                        dumpMode3 = dumpMode;
                                        now4 = now;
                                        sdf2 = sdf;
                                    } else {
                                        dumpUid2 = dumpMode2;
                                        i = i7;
                                    }
                                    try {
                                        pw.print("  Uid ");
                                        UserHandle.formatUid(pw, uidState.uid);
                                        pw.println(":");
                                        pw.print("    state=");
                                        pw.println(AppOpsManager.getUidStateName(uidState.state));
                                        if (uidState.state != uidState.pendingState) {
                                            try {
                                                pw.print("    pendingState=");
                                                pw.println(AppOpsManager.getUidStateName(uidState.pendingState));
                                            } catch (Throwable th6) {
                                                th = th6;
                                                throw th;
                                            }
                                        }
                                        pw.print("    capability=");
                                        ActivityManager.printCapabilitiesFull(pw, uidState.capability);
                                        pw.println();
                                        if (uidState.capability != uidState.pendingCapability) {
                                            pw.print("    pendingCapability=");
                                            ActivityManager.printCapabilitiesFull(pw, uidState.pendingCapability);
                                            pw.println();
                                        }
                                        pw.print("    appWidgetVisible=");
                                        pw.println(uidState.appWidgetVisible);
                                        if (uidState.appWidgetVisible != uidState.pendingAppWidgetVisible) {
                                            pw.print("    pendingAppWidgetVisible=");
                                            pw.println(uidState.pendingAppWidgetVisible);
                                        }
                                        if (uidState.pendingStateCommitTime != 0) {
                                            pw.print("    pendingStateCommitTime=");
                                            TimeUtils.formatDuration(uidState.pendingStateCommitTime, nowElapsed3, pw);
                                            pw.println();
                                        }
                                        if (uidState.foregroundOps != null && (dumpMode < 0 || dumpMode == 4)) {
                                            pw.println("    foregroundOps:");
                                            for (int j3 = 0; j3 < uidState.foregroundOps.size(); j3++) {
                                                if (dumpOp < 0 || dumpOp == uidState.foregroundOps.keyAt(j3)) {
                                                    pw.print("      ");
                                                    pw.print(AppOpsManager.opToName(uidState.foregroundOps.keyAt(j3)));
                                                    pw.print(": ");
                                                    pw.println(uidState.foregroundOps.valueAt(j3) ? "WATCHER" : "SILENT");
                                                }
                                            }
                                            pw.print("    hasForegroundWatchers=");
                                            pw.println(uidState.hasForegroundWatchers);
                                        }
                                        boolean needSep11 = true;
                                        if (opModes != null) {
                                            int opModeCount3 = opModes.size();
                                            int j4 = 0;
                                            while (j4 < opModeCount3) {
                                                int code = opModes.keyAt(j4);
                                                int mode = opModes.valueAt(j4);
                                                if (dumpOp < 0 || dumpOp == code) {
                                                    if (dumpMode >= 0) {
                                                        opModeCount = opModeCount3;
                                                        opModeCount2 = mode;
                                                        if (dumpMode != opModeCount2) {
                                                            nowElapsed2 = nowElapsed3;
                                                        }
                                                    } else {
                                                        opModeCount = opModeCount3;
                                                        opModeCount2 = mode;
                                                    }
                                                    nowElapsed2 = nowElapsed3;
                                                    pw.print("      ");
                                                    pw.print(AppOpsManager.opToName(code));
                                                    pw.print(": mode=");
                                                    pw.println(AppOpsManager.modeToName(opModeCount2));
                                                } else {
                                                    opModeCount = opModeCount3;
                                                    nowElapsed2 = nowElapsed3;
                                                }
                                                j4++;
                                                opModeCount3 = opModeCount;
                                                nowElapsed3 = nowElapsed2;
                                            }
                                            nowElapsed = nowElapsed3;
                                        } else {
                                            nowElapsed = nowElapsed3;
                                        }
                                        if (pkgOps == null) {
                                            dumpPackage2 = dumpPackage;
                                            dumpOp2 = dumpOp;
                                            z2 = true;
                                            now4 = now;
                                            sdf2 = sdf;
                                            dumpUid = dumpUid2;
                                            dumpMode3 = dumpMode;
                                        } else {
                                            int pkgi3 = 0;
                                            while (pkgi3 < pkgOps.size()) {
                                                Ops ops3 = pkgOps.valueAt(pkgi3);
                                                if (dumpPackage == null || dumpPackage.equals(ops3.packageName)) {
                                                    boolean printedPackage2 = false;
                                                    int j5 = 0;
                                                    while (j5 < ops3.size()) {
                                                        Op op = ops3.valueAt(j5);
                                                        int j6 = j5;
                                                        int j7 = op.op;
                                                        if (dumpOp < 0 || dumpOp == j7) {
                                                            if (dumpMode >= 0) {
                                                                pkgi = pkgi3;
                                                                int pkgi4 = op.mode;
                                                            } else {
                                                                pkgi = pkgi3;
                                                            }
                                                            if (printedPackage2) {
                                                                printedPackage = printedPackage2;
                                                            } else {
                                                                pw.print("    Package ");
                                                                pw.print(ops3.packageName);
                                                                pw.println(":");
                                                                printedPackage = true;
                                                            }
                                                            pw.print("      ");
                                                            pw.print(AppOpsManager.opToName(j7));
                                                            pw.print(" (");
                                                            pw.print(AppOpsManager.modeToName(op.mode));
                                                            int switchOp = AppOpsManager.opToSwitch(j7);
                                                            if (switchOp != j7) {
                                                                pw.print(" / switch ");
                                                                pw.print(AppOpsManager.opToName(switchOp));
                                                                Op switchObj = ops3.get(switchOp);
                                                                int mode2 = switchObj != null ? switchObj.mode : AppOpsManager.opToDefaultMode(switchOp);
                                                                pw.print("=");
                                                                pw.print(AppOpsManager.modeToName(mode2));
                                                            }
                                                            pw.println("): ");
                                                            ops = ops3;
                                                            needSep4 = needSep11;
                                                            dumpUid3 = dumpUid2;
                                                            dumpUid4 = dumpMode;
                                                            dumpPackage4 = dumpPackage;
                                                            dumpOp5 = dumpOp;
                                                            long j8 = now;
                                                            uidState2 = uidState;
                                                            opModes2 = opModes;
                                                            now5 = j8;
                                                            SimpleDateFormat simpleDateFormat = sdf;
                                                            pkgOps2 = pkgOps;
                                                            sdf3 = simpleDateFormat;
                                                            dumpStatesLocked(pw, dumpAttributionTag, dumpFilter, nowElapsed, op, now5, sdf3, date, "        ");
                                                            printedPackage2 = printedPackage;
                                                            j5 = j6 + 1;
                                                            ops3 = ops;
                                                            pkgi3 = pkgi;
                                                            dumpMode = dumpUid4;
                                                            needSep11 = needSep4;
                                                            dumpUid2 = dumpUid3;
                                                            dumpPackage = dumpPackage4;
                                                            dumpOp = dumpOp5;
                                                            ArrayMap<String, Ops> arrayMap = pkgOps2;
                                                            sdf = sdf3;
                                                            pkgOps = arrayMap;
                                                            long j9 = now5;
                                                            uidState = uidState2;
                                                            opModes = opModes2;
                                                            now = j9;
                                                        } else {
                                                            pkgi = pkgi3;
                                                        }
                                                        ops = ops3;
                                                        dumpPackage4 = dumpPackage;
                                                        dumpOp5 = dumpOp;
                                                        needSep4 = needSep11;
                                                        dumpUid3 = dumpUid2;
                                                        dumpUid4 = dumpMode;
                                                        long j10 = now;
                                                        uidState2 = uidState;
                                                        opModes2 = opModes;
                                                        now5 = j10;
                                                        SimpleDateFormat simpleDateFormat2 = sdf;
                                                        pkgOps2 = pkgOps;
                                                        sdf3 = simpleDateFormat2;
                                                        j5 = j6 + 1;
                                                        ops3 = ops;
                                                        pkgi3 = pkgi;
                                                        dumpMode = dumpUid4;
                                                        needSep11 = needSep4;
                                                        dumpUid2 = dumpUid3;
                                                        dumpPackage = dumpPackage4;
                                                        dumpOp = dumpOp5;
                                                        ArrayMap<String, Ops> arrayMap2 = pkgOps2;
                                                        sdf = sdf3;
                                                        pkgOps = arrayMap2;
                                                        long j92 = now5;
                                                        uidState = uidState2;
                                                        opModes = opModes2;
                                                        now = j92;
                                                    }
                                                }
                                                int dumpUid10 = dumpUid2;
                                                int dumpUid11 = dumpMode;
                                                pkgi3++;
                                                dumpMode = dumpUid11;
                                                needSep11 = needSep11;
                                                dumpUid2 = dumpUid10;
                                                dumpPackage = dumpPackage;
                                                dumpOp = dumpOp;
                                                sdf = sdf;
                                                pkgOps = pkgOps;
                                                uidState = uidState;
                                                opModes = opModes;
                                                now = now;
                                            }
                                            dumpPackage2 = dumpPackage;
                                            dumpOp2 = dumpOp;
                                            z2 = needSep11;
                                            dumpUid = dumpUid2;
                                            dumpMode3 = dumpMode;
                                            now4 = now;
                                            sdf2 = sdf;
                                        }
                                        needSep2 = z2;
                                        i7 = i + 1;
                                        now = now4;
                                        sdf = sdf2;
                                        dumpMode = dumpMode3;
                                        nowElapsed3 = nowElapsed;
                                        dumpMode2 = dumpUid;
                                        dumpPackage = dumpPackage2;
                                        dumpOp = dumpOp2;
                                    } catch (Throwable th7) {
                                        th = th7;
                                    }
                                }
                                i7 = i + 1;
                                now = now4;
                                sdf = sdf2;
                                dumpMode = dumpMode3;
                                nowElapsed3 = nowElapsed;
                                dumpMode2 = dumpUid;
                                dumpPackage = dumpPackage2;
                                dumpOp = dumpOp2;
                            } catch (Throwable th8) {
                                th = th8;
                                throw th;
                            }
                            needSep2 = needSep3;
                        }
                        int dumpUid12 = dumpMode2;
                        int dumpMode9 = dumpMode;
                        dumpPackage2 = dumpPackage;
                        dumpOp2 = dumpOp;
                        long now8 = now;
                        SimpleDateFormat sdf6 = sdf;
                        if (needSep2) {
                            pw.println();
                        }
                        try {
                            int globalRestrictionCount = this.mOpGlobalRestrictions.size();
                            int i9 = 0;
                            while (i9 < globalRestrictionCount) {
                                ArraySet<Integer> restrictedOps2 = this.mOpGlobalRestrictions.valueAt(i9).mRestrictedOps;
                                pw.println("  Global restrictions for token " + this.mOpGlobalRestrictions.keyAt(i9) + ":");
                                StringBuilder restrictedOpsValue = new StringBuilder();
                                restrictedOpsValue.append("[");
                                int restrictedOpCount2 = restrictedOps2.size();
                                int j11 = 0;
                                while (j11 < restrictedOpCount2) {
                                    int globalRestrictionCount2 = globalRestrictionCount;
                                    if (restrictedOpsValue.length() > 1) {
                                        restrictedOpsValue.append(", ");
                                    }
                                    restrictedOpsValue.append(AppOpsManager.opToName(restrictedOps2.valueAt(j11).intValue()));
                                    j11++;
                                    globalRestrictionCount = globalRestrictionCount2;
                                }
                                int globalRestrictionCount3 = globalRestrictionCount;
                                restrictedOpsValue.append("]");
                                pw.println("      Restricted ops: " + ((Object) restrictedOpsValue));
                                i9++;
                                globalRestrictionCount = globalRestrictionCount3;
                            }
                            int userRestrictionCount2 = this.mOpUserRestrictions.size();
                            int i10 = 0;
                            while (i10 < userRestrictionCount2) {
                                IBinder token = this.mOpUserRestrictions.keyAt(i10);
                                ClientUserRestrictionState restrictionState2 = this.mOpUserRestrictions.valueAt(i10);
                                boolean printedTokenHeader = false;
                                if (dumpMode9 >= 0 || dumpWatchers) {
                                    userRestrictionCount = userRestrictionCount2;
                                    now2 = now8;
                                    dumpPackage3 = dumpPackage2;
                                    dumpOp3 = dumpOp2;
                                } else if (dumpHistory) {
                                    userRestrictionCount = userRestrictionCount2;
                                    now2 = now8;
                                    dumpPackage3 = dumpPackage2;
                                    dumpOp3 = dumpOp2;
                                } else {
                                    int restrictionCount = restrictionState2.perUserRestrictions != null ? restrictionState2.perUserRestrictions.size() : 0;
                                    if (restrictionCount > 0) {
                                        dumpPackage3 = dumpPackage2;
                                        if (dumpPackage3 == null) {
                                            boolean printedOpsHeader = false;
                                            int j12 = 0;
                                            while (j12 < restrictionCount) {
                                                int userRestrictionCount3 = userRestrictionCount2;
                                                try {
                                                    int userId = restrictionState2.perUserRestrictions.keyAt(j12);
                                                    int restrictionCount2 = restrictionCount;
                                                    boolean[] restrictedOps3 = restrictionState2.perUserRestrictions.valueAt(j12);
                                                    if (restrictedOps3 == null) {
                                                        now3 = now8;
                                                        dumpOp4 = dumpOp2;
                                                    } else {
                                                        now3 = now8;
                                                        dumpOp4 = dumpOp2;
                                                        if (dumpOp4 < 0 || (dumpOp4 < restrictedOps3.length && restrictedOps3[dumpOp4])) {
                                                            if (!printedTokenHeader) {
                                                                pw.println("  User restrictions for token " + token + ":");
                                                                printedTokenHeader = true;
                                                            }
                                                            if (!printedOpsHeader) {
                                                                pw.println("      Restricted ops:");
                                                                printedOpsHeader = true;
                                                            }
                                                            StringBuilder restrictedOpsValue2 = new StringBuilder();
                                                            boolean printedTokenHeader2 = printedTokenHeader;
                                                            restrictedOpsValue2.append("[");
                                                            int restrictedOpCount3 = restrictedOps3.length;
                                                            boolean printedOpsHeader2 = printedOpsHeader;
                                                            int k = 0;
                                                            while (k < restrictedOpCount3) {
                                                                if (restrictedOps3[k]) {
                                                                    restrictedOpCount = restrictedOpCount3;
                                                                    int restrictedOpCount4 = restrictedOpsValue2.length();
                                                                    restrictedOps = restrictedOps3;
                                                                    if (restrictedOpCount4 > 1) {
                                                                        restrictedOpsValue2.append(", ");
                                                                    }
                                                                    restrictedOpsValue2.append(AppOpsManager.opToName(k));
                                                                } else {
                                                                    restrictedOpCount = restrictedOpCount3;
                                                                    restrictedOps = restrictedOps3;
                                                                }
                                                                k++;
                                                                restrictedOpCount3 = restrictedOpCount;
                                                                restrictedOps3 = restrictedOps;
                                                            }
                                                            restrictedOpsValue2.append("]");
                                                            pw.print("        ");
                                                            pw.print("user: ");
                                                            pw.print(userId);
                                                            pw.print(" restricted ops: ");
                                                            pw.println(restrictedOpsValue2);
                                                            printedTokenHeader = printedTokenHeader2;
                                                            printedOpsHeader = printedOpsHeader2;
                                                            j12++;
                                                            dumpOp2 = dumpOp4;
                                                            userRestrictionCount2 = userRestrictionCount3;
                                                            restrictionCount = restrictionCount2;
                                                            now8 = now3;
                                                        }
                                                    }
                                                    j12++;
                                                    dumpOp2 = dumpOp4;
                                                    userRestrictionCount2 = userRestrictionCount3;
                                                    restrictionCount = restrictionCount2;
                                                    now8 = now3;
                                                } catch (Throwable th9) {
                                                    th = th9;
                                                    throw th;
                                                }
                                            }
                                            userRestrictionCount = userRestrictionCount2;
                                            now2 = now8;
                                            dumpOp3 = dumpOp2;
                                        } else {
                                            userRestrictionCount = userRestrictionCount2;
                                            now2 = now8;
                                            dumpOp3 = dumpOp2;
                                        }
                                    } else {
                                        userRestrictionCount = userRestrictionCount2;
                                        now2 = now8;
                                        dumpPackage3 = dumpPackage2;
                                        dumpOp3 = dumpOp2;
                                    }
                                    int excludedPackageCount = restrictionState2.perUserExcludedPackageTags != null ? restrictionState2.perUserExcludedPackageTags.size() : 0;
                                    if (excludedPackageCount > 0 && dumpOp3 < 0) {
                                        IndentingPrintWriter ipw = new IndentingPrintWriter(pw);
                                        ipw.increaseIndent();
                                        boolean printedPackagesHeader = false;
                                        int j13 = 0;
                                        while (j13 < excludedPackageCount) {
                                            int userId2 = restrictionState2.perUserExcludedPackageTags.keyAt(j13);
                                            int excludedPackageCount2 = excludedPackageCount;
                                            PackageTagsList packageNames = restrictionState2.perUserExcludedPackageTags.valueAt(j13);
                                            if (packageNames != null) {
                                                boolean hasPackage3 = dumpPackage3 != null ? packageNames.includes(dumpPackage3) : true;
                                                if (hasPackage3) {
                                                    if (printedTokenHeader) {
                                                        restrictionState = restrictionState2;
                                                    } else {
                                                        restrictionState = restrictionState2;
                                                        ipw.println("User restrictions for token " + token + ":");
                                                        printedTokenHeader = true;
                                                    }
                                                    ipw.increaseIndent();
                                                    if (!printedPackagesHeader) {
                                                        ipw.println("Excluded packages:");
                                                        printedPackagesHeader = true;
                                                    }
                                                    ipw.increaseIndent();
                                                    ipw.print("user: ");
                                                    ipw.print(userId2);
                                                    ipw.println(" packages: ");
                                                    ipw.increaseIndent();
                                                    packageNames.dump(ipw);
                                                    ipw.decreaseIndent();
                                                    ipw.decreaseIndent();
                                                    ipw.decreaseIndent();
                                                    j13++;
                                                    excludedPackageCount = excludedPackageCount2;
                                                    restrictionState2 = restrictionState;
                                                }
                                            }
                                            restrictionState = restrictionState2;
                                            j13++;
                                            excludedPackageCount = excludedPackageCount2;
                                            restrictionState2 = restrictionState;
                                        }
                                        ipw.decreaseIndent();
                                    }
                                }
                                i10++;
                                dumpOp2 = dumpOp3;
                                dumpPackage2 = dumpPackage3;
                                userRestrictionCount2 = userRestrictionCount;
                                now8 = now2;
                            }
                            String dumpPackage7 = dumpPackage2;
                            int dumpOp9 = dumpOp2;
                            if (!dumpHistory && !dumpWatchers) {
                                pw.println();
                                if (this.mCheckOpsDelegateDispatcher.mPolicy == null || !(this.mCheckOpsDelegateDispatcher.mPolicy instanceof AppOpsPolicy)) {
                                    pw.println("  AppOps policy not set.");
                                } else {
                                    AppOpsPolicy policy = (AppOpsPolicy) this.mCheckOpsDelegateDispatcher.mPolicy;
                                    policy.dumpTags(pw);
                                }
                            }
                            if (dumpHistory && !dumpWatchers) {
                                this.mHistoricalRegistry.dump("  ", pw, dumpUid12, dumpPackage7, dumpAttributionTag, dumpOp9, dumpFilter);
                            }
                            if (includeDiscreteOps) {
                                pw.println("Discrete accesses: ");
                                this.mHistoricalRegistry.dumpDiscreteData(pw, dumpUid12, dumpPackage7, dumpAttributionTag, dumpFilter, dumpOp9, sdf6, date, "  ", nDiscreteOps);
                            }
                        } catch (Throwable th10) {
                            th = th10;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                    }
                } catch (Throwable th12) {
                    th = th12;
                }
            } catch (Throwable th13) {
                th = th13;
            }
        }
    }

    public void setUserRestrictions(Bundle restrictions, IBinder token, int userHandle) {
        checkSystemUid("setUserRestrictions");
        Objects.requireNonNull(restrictions);
        Objects.requireNonNull(token);
        CtaManager ctaManager = sCtaManager;
        int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
        for (int i = 0; i < opNum; i++) {
            String restriction = AppOpsManager.opToRestriction(i);
            if (restriction != null) {
                setUserRestrictionNoCheck(i, restrictions.getBoolean(restriction, false), token, userHandle, null);
            }
        }
    }

    public void setUserRestriction(int code, boolean restricted, IBinder token, int userHandle, PackageTagsList excludedPackageTags) {
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.MANAGE_APP_OPS_RESTRICTIONS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
        if (userHandle != UserHandle.getCallingUserId() && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") != 0) {
            throw new SecurityException("Need INTERACT_ACROSS_USERS_FULL or INTERACT_ACROSS_USERS to interact cross user ");
        }
        verifyIncomingOp(code);
        Objects.requireNonNull(token);
        setUserRestrictionNoCheck(code, restricted, token, userHandle, excludedPackageTags);
    }

    private void setUserRestrictionNoCheck(int code, boolean restricted, IBinder token, int userHandle, PackageTagsList excludedPackageTags) {
        synchronized (this) {
            ClientUserRestrictionState restrictionState = this.mOpUserRestrictions.get(token);
            if (restrictionState == null) {
                try {
                    restrictionState = new ClientUserRestrictionState(token);
                    this.mOpUserRestrictions.put(token, restrictionState);
                } catch (RemoteException e) {
                    return;
                }
            }
            if (restrictionState.setRestriction(code, restricted, excludedPackageTags, userHandle)) {
                this.mHandler.sendMessage(PooledLambda.obtainMessage(new AppOpsService$$ExternalSyntheticLambda2(), this, Integer.valueOf(code), -2));
                this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda12
                    public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                        ((AppOpsService) obj).updateStartedOpModeForUser(((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue(), ((Integer) obj4).intValue());
                    }
                }, this, Integer.valueOf(code), Boolean.valueOf(restricted), Integer.valueOf(userHandle)));
            }
            if (restrictionState.isDefault()) {
                this.mOpUserRestrictions.remove(token);
                restrictionState.destroy();
            }
        }
    }

    public void updateStartedOpModeForUser(int code, boolean restricted, int userId) {
        synchronized (this) {
            int numUids = this.mUidStates.size();
            for (int uidNum = 0; uidNum < numUids; uidNum++) {
                int uid = this.mUidStates.keyAt(uidNum);
                if (userId == -1 || UserHandle.getUserId(uid) == userId) {
                    updateStartedOpModeForUidLocked(code, restricted, uid);
                }
            }
        }
    }

    private void updateStartedOpModeForUidLocked(int code, boolean restricted, int uid) {
        UidState uidState = this.mUidStates.get(uid);
        if (uidState == null || uidState.pkgOps == null) {
            return;
        }
        int numPkgOps = uidState.pkgOps.size();
        for (int pkgNum = 0; pkgNum < numPkgOps; pkgNum++) {
            Ops ops = uidState.pkgOps.valueAt(pkgNum);
            Op op = ops != null ? ops.get(code) : null;
            if (op != null && (op.mode == 0 || op.mode == 4)) {
                int numAttrTags = op.mAttributions.size();
                for (int attrNum = 0; attrNum < numAttrTags; attrNum++) {
                    AttributedOp attrOp = op.mAttributions.valueAt(attrNum);
                    if (restricted && attrOp.isRunning()) {
                        attrOp.pause();
                    } else if (attrOp.isPaused()) {
                        attrOp.resume();
                    }
                }
            }
        }
    }

    public void notifyWatchersOfChange(int code, int uid) {
        synchronized (this) {
            ArraySet<ModeCallback> callbacks = this.mOpModeWatchers.get(code);
            if (callbacks == null) {
                return;
            }
            ArraySet<ModeCallback> clonedCallbacks = new ArraySet<>(callbacks);
            notifyOpChanged(clonedCallbacks, code, uid, (String) null);
        }
    }

    public void removeUser(int userHandle) throws RemoteException {
        checkSystemUid("removeUser");
        synchronized (this) {
            int tokenCount = this.mOpUserRestrictions.size();
            for (int i = tokenCount - 1; i >= 0; i--) {
                ClientUserRestrictionState opRestrictions = this.mOpUserRestrictions.valueAt(i);
                opRestrictions.removeUser(userHandle);
            }
            removeUidsForUserLocked(userHandle);
        }
    }

    public boolean isOperationActive(int code, int uid, String packageName) {
        if (Binder.getCallingUid() == uid || this.mContext.checkCallingOrSelfPermission("android.permission.WATCH_APPOPS") == 0) {
            verifyIncomingOp(code);
            verifyIncomingPackage(packageName, UserHandle.getUserId(uid));
            String resolvedPackageName = AppOpsManager.resolvePackageName(uid, packageName);
            if (resolvedPackageName == null) {
                return false;
            }
            synchronized (this) {
                Ops pkgOps = getOpsLocked(uid, resolvedPackageName, null, false, null, false);
                if (pkgOps == null) {
                    return false;
                }
                Op op = pkgOps.get(code);
                if (op == null) {
                    return false;
                }
                return op.isRunning();
            }
        }
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [6880=7] */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x0079, code lost:
        if (java.util.Objects.equals(r19, r12.getAttributionTag()) != false) goto L38;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean isProxying(int op, String proxyPackageName, String proxyAttributionTag, int proxiedUid, String proxiedPackageName) {
        Objects.requireNonNull(proxyPackageName);
        Objects.requireNonNull(proxiedPackageName);
        long callingUid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        boolean z = true;
        try {
            try {
                List<AppOpsManager.PackageOps> packageOps = getOpsForPackage(proxiedUid, proxiedPackageName, new int[]{op});
                if (packageOps != null && !packageOps.isEmpty()) {
                    List<AppOpsManager.OpEntry> opEntries = packageOps.get(0).getOps();
                    if (opEntries.isEmpty()) {
                        Binder.restoreCallingIdentity(identity);
                        return false;
                    }
                    AppOpsManager.OpEntry opEntry = opEntries.get(0);
                    if (!opEntry.isRunning()) {
                        Binder.restoreCallingIdentity(identity);
                        return false;
                    }
                    AppOpsManager.OpEventProxyInfo proxyInfo = opEntry.getLastProxyInfo(24);
                    if (proxyInfo != null && callingUid == proxyInfo.getUid()) {
                        try {
                            if (proxyPackageName.equals(proxyInfo.getPackageName())) {
                                try {
                                } catch (Throwable th) {
                                    th = th;
                                    Binder.restoreCallingIdentity(identity);
                                    throw th;
                                }
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    }
                    z = false;
                    Binder.restoreCallingIdentity(identity);
                    return z;
                }
                Binder.restoreCallingIdentity(identity);
                return false;
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    public void resetPackageOpsNoHistory(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_APPOPS", "resetPackageOpsNoHistory");
        synchronized (this) {
            int uid = this.mPackageManagerInternal.getPackageUid(packageName, 0L, UserHandle.getCallingUserId());
            if (uid == -1) {
                return;
            }
            UidState uidState = this.mUidStates.get(uid);
            if (uidState != null && uidState.pkgOps != null) {
                Ops removedOps = uidState.pkgOps.remove(packageName);
                if (removedOps != null) {
                    scheduleFastWriteLocked();
                }
            }
        }
    }

    public void setHistoryParameters(int mode, long baseSnapshotInterval, int compressionStep) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_APPOPS", "setHistoryParameters");
        this.mHistoricalRegistry.setHistoryParameters(mode, baseSnapshotInterval, compressionStep);
    }

    public void offsetHistory(long offsetMillis) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_APPOPS", "offsetHistory");
        this.mHistoricalRegistry.offsetHistory(offsetMillis);
        this.mHistoricalRegistry.offsetDiscreteHistory(offsetMillis);
    }

    public void addHistoricalOps(AppOpsManager.HistoricalOps ops) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_APPOPS", "addHistoricalOps");
        this.mHistoricalRegistry.addHistoricalOps(ops);
    }

    public void resetHistoryParameters() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_APPOPS", "resetHistoryParameters");
        this.mHistoricalRegistry.resetHistoryParameters();
    }

    public void clearHistory() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_APPOPS", "clearHistory");
        this.mHistoricalRegistry.clearAllHistory();
    }

    public void rebootHistory(long offlineDurationMillis) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_APPOPS", "rebootHistory");
        Preconditions.checkArgument(offlineDurationMillis >= 0);
        this.mHistoricalRegistry.shutdown();
        if (offlineDurationMillis > 0) {
            SystemClock.sleep(offlineDurationMillis);
        }
        this.mHistoricalRegistry = new HistoricalRegistry(this.mHistoricalRegistry);
        this.mHistoricalRegistry.systemReady(this.mContext.getContentResolver());
        this.mHistoricalRegistry.persistPendingHistory();
    }

    public MessageSamplingConfig reportRuntimeAppOpAccessMessageAndGetConfig(String packageName, SyncNotedAppOp notedAppOp, String message) {
        int uid = Binder.getCallingUid();
        Objects.requireNonNull(packageName);
        synchronized (this) {
            switchPackageIfBootTimeOrRarelyUsedLocked(packageName);
            if (!packageName.equals(this.mSampledPackage)) {
                return new MessageSamplingConfig(-1, 0, Instant.now().plus(1L, (TemporalUnit) ChronoUnit.HOURS).toEpochMilli());
            }
            Objects.requireNonNull(notedAppOp);
            Objects.requireNonNull(message);
            reportRuntimeAppOpAccessMessageInternalLocked(uid, packageName, AppOpsManager.strOpToOp(notedAppOp.getOp()), notedAppOp.getAttributionTag(), message);
            return new MessageSamplingConfig(this.mSampledAppOpCode, this.mAcceptableLeftDistance, Instant.now().plus(1L, (TemporalUnit) ChronoUnit.HOURS).toEpochMilli());
        }
    }

    private void reportRuntimeAppOpAccessMessageAsyncLocked(int uid, String packageName, int opCode, String attributionTag, String message) {
        switchPackageIfBootTimeOrRarelyUsedLocked(packageName);
        if (!Objects.equals(this.mSampledPackage, packageName)) {
            return;
        }
        reportRuntimeAppOpAccessMessageInternalLocked(uid, packageName, opCode, attributionTag, message);
    }

    private void reportRuntimeAppOpAccessMessageInternalLocked(int uid, String packageName, int opCode, String attributionTag, String message) {
        CtaManager ctaManager = sCtaManager;
        int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
        int newLeftDistance = AppOpsManager.leftCircularDistance(opCode, this.mSampledAppOpCode, opNum);
        int i = this.mAcceptableLeftDistance;
        if (i < newLeftDistance && this.mSamplingStrategy != 4) {
            return;
        }
        if (i > newLeftDistance && this.mSamplingStrategy != 4) {
            this.mAcceptableLeftDistance = newLeftDistance;
            this.mMessagesCollectedCount = 0.0f;
        }
        this.mMessagesCollectedCount += 1.0f;
        if (ThreadLocalRandom.current().nextFloat() <= 1.0f / this.mMessagesCollectedCount) {
            this.mCollectedRuntimePermissionMessage = new RuntimeAppOpAccessMessage(uid, opCode, packageName, attributionTag, message, this.mSamplingStrategy);
        }
    }

    public RuntimeAppOpAccessMessage collectRuntimeAppOpAccessMessage() {
        RuntimeAppOpAccessMessage result;
        ActivityManagerInternal ami = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        boolean isCallerInstrumented = ami.getInstrumentationSourceUid(Binder.getCallingUid()) != -1;
        boolean isCallerSystem = Binder.getCallingPid() == Process.myPid();
        if (!isCallerSystem && !isCallerInstrumented) {
            return null;
        }
        this.mContext.enforcePermission("android.permission.GET_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        synchronized (this) {
            result = this.mCollectedRuntimePermissionMessage;
            this.mCollectedRuntimePermissionMessage = null;
        }
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.appop.AppOpsService$$ExternalSyntheticLambda16
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((AppOpsService) obj).getPackageListAndResample();
            }
        }, this));
        return result;
    }

    private void switchPackageIfBootTimeOrRarelyUsedLocked(String packageName) {
        if (this.mSampledPackage == null) {
            if (ThreadLocalRandom.current().nextFloat() < 0.5f) {
                this.mSamplingStrategy = 3;
                resampleAppOpForPackageLocked(packageName, true);
            }
        } else if (this.mRarelyUsedPackages.contains(packageName)) {
            this.mRarelyUsedPackages.remove(packageName);
            if (ThreadLocalRandom.current().nextFloat() < 0.5f) {
                this.mSamplingStrategy = 2;
                resampleAppOpForPackageLocked(packageName, true);
            }
        }
    }

    public List<String> getPackageListAndResample() {
        List<String> packageNames = getPackageNamesForSampling();
        synchronized (this) {
            resamplePackageAndAppOpLocked(packageNames);
        }
        return packageNames;
    }

    private void resamplePackageAndAppOpLocked(List<String> packageNames) {
        if (!packageNames.isEmpty()) {
            if (ThreadLocalRandom.current().nextFloat() < 0.5f) {
                this.mSamplingStrategy = 1;
                resampleAppOpForPackageLocked(packageNames.get(ThreadLocalRandom.current().nextInt(packageNames.size())), true);
                return;
            }
            this.mSamplingStrategy = 4;
            resampleAppOpForPackageLocked(packageNames.get(ThreadLocalRandom.current().nextInt(packageNames.size())), false);
        }
    }

    private void resampleAppOpForPackageLocked(String packageName, boolean pickOp) {
        this.mMessagesCollectedCount = 0.0f;
        CtaManager ctaManager = sCtaManager;
        int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
        this.mSampledAppOpCode = pickOp ? ThreadLocalRandom.current().nextInt(opNum) : -1;
        this.mAcceptableLeftDistance = opNum - 1;
        this.mSampledPackage = packageName;
    }

    public void initializeRarelyUsedPackagesList(final ArraySet<String> candidates) {
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        List<String> runtimeAppOpsList = getRuntimeAppOpsList();
        AppOpsManager.HistoricalOpsRequest histOpsRequest = new AppOpsManager.HistoricalOpsRequest.Builder(Math.max(Instant.now().minus(7L, (TemporalUnit) ChronoUnit.DAYS).toEpochMilli(), 0L), (long) JobStatus.NO_LATEST_RUNTIME).setOpNames(runtimeAppOpsList).setFlags(9).build();
        appOps.getHistoricalOps(histOpsRequest, AsyncTask.THREAD_POOL_EXECUTOR, new Consumer<AppOpsManager.HistoricalOps>() { // from class: com.android.server.appop.AppOpsService.8
            {
                AppOpsService.this = this;
            }

            @Override // java.util.function.Consumer
            public void accept(AppOpsManager.HistoricalOps histOps) {
                int uidCount = histOps.getUidCount();
                for (int uidIdx = 0; uidIdx < uidCount; uidIdx++) {
                    AppOpsManager.HistoricalUidOps uidOps = histOps.getUidOpsAt(uidIdx);
                    int pkgCount = uidOps.getPackageCount();
                    for (int pkgIdx = 0; pkgIdx < pkgCount; pkgIdx++) {
                        String packageName = uidOps.getPackageOpsAt(pkgIdx).getPackageName();
                        if (candidates.contains(packageName)) {
                            AppOpsManager.HistoricalPackageOps packageOps = uidOps.getPackageOpsAt(pkgIdx);
                            if (packageOps.getOpCount() != 0) {
                                candidates.remove(packageName);
                            }
                        }
                    }
                }
                synchronized (this) {
                    int numPkgs = AppOpsService.this.mRarelyUsedPackages.size();
                    for (int i = 0; i < numPkgs; i++) {
                        candidates.add((String) AppOpsService.this.mRarelyUsedPackages.valueAt(i));
                    }
                    AppOpsService.this.mRarelyUsedPackages = candidates;
                }
            }
        });
    }

    private List<String> getRuntimeAppOpsList() {
        ArrayList<String> result = new ArrayList<>();
        CtaManager ctaManager = sCtaManager;
        int opNum = ctaManager.isCtaSupported() ? ctaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
        for (int i = 0; i < opNum; i++) {
            if (shouldCollectNotes(i)) {
                result.add(AppOpsManager.opToPublicName(i));
            }
        }
        return result;
    }

    private List<String> getPackageNamesForSampling() {
        List<String> packageNames = new ArrayList<>();
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        PackageList packages = packageManagerInternal.getPackageList();
        for (String packageName : packages.getPackageNames()) {
            PackageInfo pkg = packageManagerInternal.getPackageInfo(packageName, 4096L, Process.myUid(), this.mContext.getUserId());
            if (isSamplingTarget(pkg)) {
                packageNames.add(pkg.packageName);
            }
        }
        return packageNames;
    }

    public boolean isSamplingTarget(PackageInfo pkg) {
        String[] requestedPermissions;
        PermissionInfo permissionInfo;
        if (pkg == null || (requestedPermissions = pkg.requestedPermissions) == null) {
            return false;
        }
        for (String permission : requestedPermissions) {
            try {
                permissionInfo = this.mContext.getPackageManager().getPermissionInfo(permission, 0);
            } catch (PackageManager.NameNotFoundException e) {
            }
            if (permissionInfo.getProtection() == 1) {
                return true;
            }
        }
        return false;
    }

    private void removeUidsForUserLocked(int userHandle) {
        for (int i = this.mUidStates.size() - 1; i >= 0; i--) {
            int uid = this.mUidStates.keyAt(i);
            if (UserHandle.getUserId(uid) == userHandle) {
                this.mUidStates.removeAt(i);
            }
        }
    }

    private void checkSystemUid(String function) {
        int uid = Binder.getCallingUid();
        if (uid != 1000) {
            throw new SecurityException(function + " must by called by the system");
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static int resolveUid(String packageName) {
        char c;
        if (packageName == null) {
            return -1;
        }
        switch (packageName.hashCode()) {
            case -1336564963:
                if (packageName.equals("dumpstate")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -31178072:
                if (packageName.equals("cameraserver")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 3506402:
                if (packageName.equals("root")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 103772132:
                if (packageName.equals("media")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 109403696:
                if (packageName.equals("shell")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1344606873:
                if (packageName.equals("audioserver")) {
                    c = 4;
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
                return 0;
            case 1:
            case 2:
                return 2000;
            case 3:
                return 1013;
            case 4:
                return 1041;
            case 5:
                return 1047;
            default:
                return -1;
        }
    }

    private static String[] getPackagesForUid(int uid) {
        String[] packageNames = null;
        if (AppGlobals.getPackageManager() != null) {
            try {
                packageNames = AppGlobals.getPackageManager().getPackagesForUid(uid);
            } catch (RemoteException e) {
            }
        }
        if (packageNames == null) {
            return EmptyArray.STRING;
        }
        return packageNames;
    }

    /* loaded from: classes.dex */
    public final class ClientUserRestrictionState implements IBinder.DeathRecipient {
        SparseArray<PackageTagsList> perUserExcludedPackageTags;
        SparseArray<boolean[]> perUserRestrictions;
        private final IBinder token;

        ClientUserRestrictionState(IBinder token) throws RemoteException {
            AppOpsService.this = r1;
            token.linkToDeath(this, 0);
            this.token = token;
        }

        public boolean setRestriction(int code, boolean restricted, PackageTagsList excludedPackageTags, int userId) {
            int[] users;
            boolean changed = false;
            if (this.perUserRestrictions == null && restricted) {
                this.perUserRestrictions = new SparseArray<>();
            }
            if (userId == -1) {
                List<UserInfo> liveUsers = UserManager.get(AppOpsService.this.mContext).getUsers();
                users = new int[liveUsers.size()];
                for (int i = 0; i < liveUsers.size(); i++) {
                    users[i] = liveUsers.get(i).id;
                }
            } else {
                users = new int[]{userId};
            }
            if (this.perUserRestrictions != null) {
                for (int thisUserId : users) {
                    boolean[] userRestrictions = this.perUserRestrictions.get(thisUserId);
                    if (userRestrictions == null && restricted) {
                        int opNum = AppOpsService.sCtaManager.isCtaSupported() ? AppOpsService.sCtaManager.getOpNum() : FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__PROVISIONING_PREPARE_TOTAL_TIME_MS;
                        userRestrictions = new boolean[opNum];
                        this.perUserRestrictions.put(thisUserId, userRestrictions);
                    }
                    if (userRestrictions != null && userRestrictions[code] != restricted) {
                        userRestrictions[code] = restricted;
                        if (!restricted && isDefault(userRestrictions)) {
                            this.perUserRestrictions.remove(thisUserId);
                            userRestrictions = null;
                        }
                        changed = true;
                    }
                    if (userRestrictions != null) {
                        boolean noExcludedPackages = excludedPackageTags == null || excludedPackageTags.isEmpty();
                        if (this.perUserExcludedPackageTags == null && !noExcludedPackages) {
                            this.perUserExcludedPackageTags = new SparseArray<>();
                        }
                        SparseArray<PackageTagsList> sparseArray = this.perUserExcludedPackageTags;
                        if (sparseArray != null) {
                            if (noExcludedPackages) {
                                sparseArray.remove(thisUserId);
                                if (this.perUserExcludedPackageTags.size() <= 0) {
                                    this.perUserExcludedPackageTags = null;
                                }
                            } else {
                                sparseArray.put(thisUserId, excludedPackageTags);
                            }
                            changed = true;
                        }
                    }
                }
            }
            return changed;
        }

        public boolean hasRestriction(int restriction, String packageName, String attributionTag, int userId, boolean isCheckOp) {
            boolean[] restrictions;
            PackageTagsList perUserExclusions;
            SparseArray<boolean[]> sparseArray = this.perUserRestrictions;
            if (sparseArray == null || (restrictions = sparseArray.get(userId)) == null || !restrictions[restriction]) {
                return false;
            }
            SparseArray<PackageTagsList> sparseArray2 = this.perUserExcludedPackageTags;
            if (sparseArray2 == null || (perUserExclusions = sparseArray2.get(userId)) == null) {
                return true;
            }
            if (isCheckOp) {
                return true ^ perUserExclusions.includes(packageName);
            }
            return true ^ perUserExclusions.contains(packageName, attributionTag);
        }

        public void removeUser(int userId) {
            SparseArray<PackageTagsList> sparseArray = this.perUserExcludedPackageTags;
            if (sparseArray != null) {
                sparseArray.remove(userId);
                if (this.perUserExcludedPackageTags.size() <= 0) {
                    this.perUserExcludedPackageTags = null;
                }
            }
            SparseArray<boolean[]> sparseArray2 = this.perUserRestrictions;
            if (sparseArray2 != null) {
                sparseArray2.remove(userId);
                if (this.perUserRestrictions.size() <= 0) {
                    this.perUserRestrictions = null;
                }
            }
        }

        public boolean isDefault() {
            SparseArray<boolean[]> sparseArray = this.perUserRestrictions;
            return sparseArray == null || sparseArray.size() <= 0;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mOpUserRestrictions.remove(this.token);
                SparseArray<boolean[]> sparseArray = this.perUserRestrictions;
                if (sparseArray == null) {
                    return;
                }
                int userCount = sparseArray.size();
                for (int i = 0; i < userCount; i++) {
                    boolean[] restrictions = this.perUserRestrictions.valueAt(i);
                    int restrictionCount = restrictions.length;
                    for (int j = 0; j < restrictionCount; j++) {
                        if (restrictions[j]) {
                            final int changedCode = j;
                            AppOpsService.this.mHandler.post(new Runnable() { // from class: com.android.server.appop.AppOpsService$ClientUserRestrictionState$$ExternalSyntheticLambda0
                                @Override // java.lang.Runnable
                                public final void run() {
                                    AppOpsService.ClientUserRestrictionState.this.m1649x27fce619(changedCode);
                                }
                            });
                        }
                    }
                }
                destroy();
            }
        }

        /* renamed from: lambda$binderDied$0$com-android-server-appop-AppOpsService$ClientUserRestrictionState */
        public /* synthetic */ void m1649x27fce619(int changedCode) {
            AppOpsService.this.notifyWatchersOfChange(changedCode, -2);
        }

        public void destroy() {
            this.token.unlinkToDeath(this, 0);
        }

        private boolean isDefault(boolean[] array) {
            if (ArrayUtils.isEmpty(array)) {
                return true;
            }
            for (boolean value : array) {
                if (value) {
                    return false;
                }
            }
            return true;
        }
    }

    /* loaded from: classes.dex */
    public final class ClientGlobalRestrictionState implements IBinder.DeathRecipient {
        final ArraySet<Integer> mRestrictedOps = new ArraySet<>();
        final IBinder mToken;

        ClientGlobalRestrictionState(IBinder token) throws RemoteException {
            AppOpsService.this = r1;
            token.linkToDeath(this, 0);
            this.mToken = token;
        }

        boolean setRestriction(int code, boolean restricted) {
            if (restricted) {
                return this.mRestrictedOps.add(Integer.valueOf(code));
            }
            return this.mRestrictedOps.remove(Integer.valueOf(code));
        }

        boolean hasRestriction(int code) {
            return this.mRestrictedOps.contains(Integer.valueOf(code));
        }

        boolean isDefault() {
            return this.mRestrictedOps.isEmpty();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            destroy();
        }

        void destroy() {
            this.mToken.unlinkToDeath(this, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class AppOpsManagerInternalImpl extends AppOpsManagerInternal {
        private AppOpsManagerInternalImpl() {
            AppOpsService.this = r1;
        }

        public void setDeviceAndProfileOwners(SparseIntArray owners) {
            synchronized (AppOpsService.this) {
                AppOpsService.this.mProfileOwners = owners;
            }
        }

        public void updateAppWidgetVisibility(SparseArray<String> uidPackageNames, boolean visible) {
            AppOpsService.this.updateAppWidgetVisibility(uidPackageNames, visible);
        }

        public void setUidModeFromPermissionPolicy(int code, int uid, int mode, IAppOpsCallback callback) {
            AppOpsService.this.setUidMode(code, uid, mode, callback);
        }

        public void setModeFromPermissionPolicy(int code, int uid, String packageName, int mode, IAppOpsCallback callback) {
            AppOpsService.this.setMode(code, uid, packageName, mode, callback);
        }

        public void setGlobalRestriction(int code, boolean restricted, IBinder token) {
            if (Binder.getCallingPid() != Process.myPid()) {
                throw new SecurityException("Only the system can set global restrictions");
            }
            synchronized (AppOpsService.this) {
                ClientGlobalRestrictionState restrictionState = (ClientGlobalRestrictionState) AppOpsService.this.mOpGlobalRestrictions.get(token);
                if (restrictionState == null) {
                    try {
                        restrictionState = new ClientGlobalRestrictionState(token);
                        AppOpsService.this.mOpGlobalRestrictions.put(token, restrictionState);
                    } catch (RemoteException e) {
                        return;
                    }
                }
                if (restrictionState.setRestriction(code, restricted)) {
                    AppOpsService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.appop.AppOpsService$AppOpsManagerInternalImpl$$ExternalSyntheticLambda0
                        public final void accept(Object obj, Object obj2, Object obj3) {
                            ((AppOpsService) obj).notifyWatchersOfChange(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
                        }
                    }, AppOpsService.this, Integer.valueOf(code), -2));
                    AppOpsService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.appop.AppOpsService$AppOpsManagerInternalImpl$$ExternalSyntheticLambda1
                        public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                            ((AppOpsService) obj).updateStartedOpModeForUser(((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue(), ((Integer) obj4).intValue());
                        }
                    }, AppOpsService.this, Integer.valueOf(code), Boolean.valueOf(restricted), -1));
                }
                if (restrictionState.isDefault()) {
                    AppOpsService.this.mOpGlobalRestrictions.remove(token);
                    restrictionState.destroy();
                }
            }
        }

        public int getOpRestrictionCount(int code, UserHandle user, String pkg, String attributionTag) {
            int number = 0;
            synchronized (AppOpsService.this) {
                int numRestrictions = AppOpsService.this.mOpUserRestrictions.size();
                for (int i = 0; i < numRestrictions; i++) {
                    if (((ClientUserRestrictionState) AppOpsService.this.mOpUserRestrictions.valueAt(i)).hasRestriction(code, pkg, attributionTag, user.getIdentifier(), false)) {
                        number++;
                    }
                }
                int numRestrictions2 = AppOpsService.this.mOpGlobalRestrictions.size();
                for (int i2 = 0; i2 < numRestrictions2; i2++) {
                    if (((ClientGlobalRestrictionState) AppOpsService.this.mOpGlobalRestrictions.valueAt(i2)).hasRestriction(code)) {
                        number++;
                    }
                }
            }
            return number;
        }
    }

    public void writeNoteOps() {
        synchronized (this) {
            this.mWriteNoteOpsScheduled = false;
        }
        synchronized (this.mNoteOpCallerStacktracesFile) {
            try {
                FileWriter writer = new FileWriter(this.mNoteOpCallerStacktracesFile);
                try {
                    int numTraces = this.mNoteOpCallerStacktraces.size();
                    for (int i = 0; i < numTraces; i++) {
                        writer.write(this.mNoteOpCallerStacktraces.valueAt(i).asJson());
                        writer.write(",");
                    }
                    writer.close();
                } catch (Throwable th) {
                    try {
                        writer.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed to load opsValidation file for FileWriter", e);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class NoteOpTrace {
        static final String OP = "op";
        static final String PACKAGENAME = "packageName";
        static final String STACKTRACE = "stackTrace";
        static final String VERSION = "version";
        private final int mOp;
        private final String mPackageName;
        private final String mStackTrace;
        private final long mVersion;

        static NoteOpTrace fromJson(String jsonTrace) {
            try {
                JSONObject obj = new JSONObject(jsonTrace.concat("}"));
                return new NoteOpTrace(obj.getString(STACKTRACE), obj.getInt(OP), obj.getString("packageName"), obj.getLong(VERSION));
            } catch (JSONException e) {
                Slog.e(AppOpsService.TAG, "Error constructing NoteOpTrace object JSON trace format incorrect", e);
                return null;
            }
        }

        NoteOpTrace(String stackTrace, int op, String packageName, long version) {
            this.mStackTrace = stackTrace;
            this.mOp = op;
            this.mPackageName = packageName;
            this.mVersion = version;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NoteOpTrace that = (NoteOpTrace) o;
            if (this.mOp == that.mOp && this.mVersion == that.mVersion && this.mStackTrace.equals(that.mStackTrace) && Objects.equals(this.mPackageName, that.mPackageName)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.mStackTrace, Integer.valueOf(this.mOp), this.mPackageName, Long.valueOf(this.mVersion));
        }

        public String asJson() {
            return "{\"stackTrace\":\"" + this.mStackTrace.replace("\n", "\\n") + "\",\"" + OP + "\":" + this.mOp + ",\"packageName\":\"" + this.mPackageName + "\",\"" + VERSION + "\":" + this.mVersion + '}';
        }
    }

    public void collectNoteOpCallsForValidation(String stackTrace, int op, String packageName, long version) {
    }

    /* loaded from: classes.dex */
    public final class CheckOpsDelegateDispatcher {
        private final AppOpsManagerInternal.CheckOpsDelegate mCheckOpsDelegate;
        private final AppOpsManagerInternal.CheckOpsDelegate mPolicy;

        CheckOpsDelegateDispatcher(AppOpsManagerInternal.CheckOpsDelegate policy, AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate) {
            AppOpsService.this = r1;
            this.mPolicy = policy;
            this.mCheckOpsDelegate = checkOpsDelegate;
        }

        public AppOpsManagerInternal.CheckOpsDelegate getCheckOpsDelegate() {
            return this.mCheckOpsDelegate;
        }

        public int checkOperation(int code, int uid, String packageName, String attributionTag, boolean raw) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mPolicy;
            if (checkOpsDelegate != null) {
                if (this.mCheckOpsDelegate != null) {
                    return checkOpsDelegate.checkOperation(code, uid, packageName, attributionTag, raw, new QuintFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda13
                        public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                            int checkDelegateOperationImpl;
                            checkDelegateOperationImpl = AppOpsService.CheckOpsDelegateDispatcher.this.checkDelegateOperationImpl(((Integer) obj).intValue(), ((Integer) obj2).intValue(), (String) obj3, (String) obj4, ((Boolean) obj5).booleanValue());
                            return Integer.valueOf(checkDelegateOperationImpl);
                        }
                    });
                }
                final AppOpsService appOpsService = AppOpsService.this;
                return checkOpsDelegate.checkOperation(code, uid, packageName, attributionTag, raw, new QuintFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda14
                    public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                        Integer valueOf;
                        valueOf = Integer.valueOf(AppOpsService.this.checkOperationImpl(((Integer) obj).intValue(), ((Integer) obj2).intValue(), (String) obj3, (String) obj4, ((Boolean) obj5).booleanValue()));
                        return valueOf;
                    }
                });
            } else if (this.mCheckOpsDelegate != null) {
                return checkDelegateOperationImpl(code, uid, packageName, attributionTag, raw);
            } else {
                return AppOpsService.this.checkOperationImpl(code, uid, packageName, attributionTag, raw);
            }
        }

        public int checkDelegateOperationImpl(int code, int uid, String packageName, String attributionTag, boolean raw) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mCheckOpsDelegate;
            final AppOpsService appOpsService = AppOpsService.this;
            return checkOpsDelegate.checkOperation(code, uid, packageName, attributionTag, raw, new QuintFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda18
                public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    Integer valueOf;
                    valueOf = Integer.valueOf(AppOpsService.this.checkOperationImpl(((Integer) obj).intValue(), ((Integer) obj2).intValue(), (String) obj3, (String) obj4, ((Boolean) obj5).booleanValue()));
                    return valueOf;
                }
            });
        }

        public int checkAudioOperation(int code, int usage, int uid, String packageName) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mPolicy;
            if (checkOpsDelegate != null) {
                if (this.mCheckOpsDelegate != null) {
                    return checkOpsDelegate.checkAudioOperation(code, usage, uid, packageName, new QuadFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda22
                        public final Object apply(Object obj, Object obj2, Object obj3, Object obj4) {
                            int checkDelegateAudioOperationImpl;
                            checkDelegateAudioOperationImpl = AppOpsService.CheckOpsDelegateDispatcher.this.checkDelegateAudioOperationImpl(((Integer) obj).intValue(), ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (String) obj4);
                            return Integer.valueOf(checkDelegateAudioOperationImpl);
                        }
                    });
                }
                final AppOpsService appOpsService = AppOpsService.this;
                return checkOpsDelegate.checkAudioOperation(code, usage, uid, packageName, new QuadFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda23
                    public final Object apply(Object obj, Object obj2, Object obj3, Object obj4) {
                        Integer valueOf;
                        valueOf = Integer.valueOf(AppOpsService.this.checkAudioOperationImpl(((Integer) obj).intValue(), ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (String) obj4));
                        return valueOf;
                    }
                });
            } else if (this.mCheckOpsDelegate != null) {
                return checkDelegateAudioOperationImpl(code, usage, uid, packageName);
            } else {
                return AppOpsService.this.checkAudioOperationImpl(code, usage, uid, packageName);
            }
        }

        public int checkDelegateAudioOperationImpl(int code, int usage, int uid, String packageName) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mCheckOpsDelegate;
            final AppOpsService appOpsService = AppOpsService.this;
            return checkOpsDelegate.checkAudioOperation(code, usage, uid, packageName, new QuadFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda6
                public final Object apply(Object obj, Object obj2, Object obj3, Object obj4) {
                    Integer valueOf;
                    valueOf = Integer.valueOf(AppOpsService.this.checkAudioOperationImpl(((Integer) obj).intValue(), ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (String) obj4));
                    return valueOf;
                }
            });
        }

        public SyncNotedAppOp noteOperation(int code, int uid, String packageName, String attributionTag, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mPolicy;
            if (checkOpsDelegate != null) {
                if (this.mCheckOpsDelegate != null) {
                    return checkOpsDelegate.noteOperation(code, uid, packageName, attributionTag, shouldCollectAsyncNotedOp, message, shouldCollectMessage, new HeptFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda3
                        public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7) {
                            SyncNotedAppOp noteDelegateOperationImpl;
                            noteDelegateOperationImpl = AppOpsService.CheckOpsDelegateDispatcher.this.noteDelegateOperationImpl(((Integer) obj).intValue(), ((Integer) obj2).intValue(), (String) obj3, (String) obj4, ((Boolean) obj5).booleanValue(), (String) obj6, ((Boolean) obj7).booleanValue());
                            return noteDelegateOperationImpl;
                        }
                    });
                }
                final AppOpsService appOpsService = AppOpsService.this;
                return checkOpsDelegate.noteOperation(code, uid, packageName, attributionTag, shouldCollectAsyncNotedOp, message, shouldCollectMessage, new HeptFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda4
                    public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7) {
                        SyncNotedAppOp noteOperationImpl;
                        noteOperationImpl = AppOpsService.this.noteOperationImpl(((Integer) obj).intValue(), ((Integer) obj2).intValue(), (String) obj3, (String) obj4, ((Boolean) obj5).booleanValue(), (String) obj6, ((Boolean) obj7).booleanValue());
                        return noteOperationImpl;
                    }
                });
            } else if (this.mCheckOpsDelegate != null) {
                return noteDelegateOperationImpl(code, uid, packageName, attributionTag, shouldCollectAsyncNotedOp, message, shouldCollectMessage);
            } else {
                return AppOpsService.this.noteOperationImpl(code, uid, packageName, attributionTag, shouldCollectAsyncNotedOp, message, shouldCollectMessage);
            }
        }

        public SyncNotedAppOp noteDelegateOperationImpl(int code, int uid, String packageName, String featureId, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mCheckOpsDelegate;
            final AppOpsService appOpsService = AppOpsService.this;
            return checkOpsDelegate.noteOperation(code, uid, packageName, featureId, shouldCollectAsyncNotedOp, message, shouldCollectMessage, new HeptFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda2
                public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7) {
                    SyncNotedAppOp noteOperationImpl;
                    noteOperationImpl = AppOpsService.this.noteOperationImpl(((Integer) obj).intValue(), ((Integer) obj2).intValue(), (String) obj3, (String) obj4, ((Boolean) obj5).booleanValue(), (String) obj6, ((Boolean) obj7).booleanValue());
                    return noteOperationImpl;
                }
            });
        }

        public SyncNotedAppOp noteProxyOperation(int code, AttributionSource attributionSource, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mPolicy;
            if (checkOpsDelegate != null) {
                if (this.mCheckOpsDelegate != null) {
                    return checkOpsDelegate.noteProxyOperation(code, attributionSource, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation, new HexFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda7
                        public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
                            SyncNotedAppOp noteDelegateProxyOperationImpl;
                            noteDelegateProxyOperationImpl = AppOpsService.CheckOpsDelegateDispatcher.this.noteDelegateProxyOperationImpl(((Integer) obj).intValue(), (AttributionSource) obj2, ((Boolean) obj3).booleanValue(), (String) obj4, ((Boolean) obj5).booleanValue(), ((Boolean) obj6).booleanValue());
                            return noteDelegateProxyOperationImpl;
                        }
                    });
                }
                final AppOpsService appOpsService = AppOpsService.this;
                return checkOpsDelegate.noteProxyOperation(code, attributionSource, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation, new HexFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda8
                    public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
                        SyncNotedAppOp noteProxyOperationImpl;
                        noteProxyOperationImpl = AppOpsService.this.noteProxyOperationImpl(((Integer) obj).intValue(), (AttributionSource) obj2, ((Boolean) obj3).booleanValue(), (String) obj4, ((Boolean) obj5).booleanValue(), ((Boolean) obj6).booleanValue());
                        return noteProxyOperationImpl;
                    }
                });
            } else if (this.mCheckOpsDelegate != null) {
                return noteDelegateProxyOperationImpl(code, attributionSource, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation);
            } else {
                return AppOpsService.this.noteProxyOperationImpl(code, attributionSource, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation);
            }
        }

        public SyncNotedAppOp noteDelegateProxyOperationImpl(int code, AttributionSource attributionSource, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mCheckOpsDelegate;
            final AppOpsService appOpsService = AppOpsService.this;
            return checkOpsDelegate.noteProxyOperation(code, attributionSource, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation, new HexFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda5
                public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
                    SyncNotedAppOp noteProxyOperationImpl;
                    noteProxyOperationImpl = AppOpsService.this.noteProxyOperationImpl(((Integer) obj).intValue(), (AttributionSource) obj2, ((Boolean) obj3).booleanValue(), (String) obj4, ((Boolean) obj5).booleanValue(), ((Boolean) obj6).booleanValue());
                    return noteProxyOperationImpl;
                }
            });
        }

        public SyncNotedAppOp startOperation(IBinder token, int code, int uid, String packageName, String attributionTag, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, int attributionFlags, int attributionChainId) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mPolicy;
            if (checkOpsDelegate != null) {
                if (this.mCheckOpsDelegate != null) {
                    return checkOpsDelegate.startOperation(token, code, uid, packageName, attributionTag, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, attributionFlags, attributionChainId, new UndecFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda16
                        public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11) {
                            SyncNotedAppOp startDelegateOperationImpl;
                            startDelegateOperationImpl = AppOpsService.CheckOpsDelegateDispatcher.this.startDelegateOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (String) obj4, (String) obj5, ((Boolean) obj6).booleanValue(), ((Boolean) obj7).booleanValue(), (String) obj8, ((Boolean) obj9).booleanValue(), ((Integer) obj10).intValue(), ((Integer) obj11).intValue());
                            return startDelegateOperationImpl;
                        }
                    });
                }
                final AppOpsService appOpsService = AppOpsService.this;
                return checkOpsDelegate.startOperation(token, code, uid, packageName, attributionTag, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, attributionFlags, attributionChainId, new UndecFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda17
                    public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11) {
                        SyncNotedAppOp startOperationImpl;
                        startOperationImpl = AppOpsService.this.startOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (String) obj4, (String) obj5, ((Boolean) obj6).booleanValue(), ((Boolean) obj7).booleanValue(), (String) obj8, ((Boolean) obj9).booleanValue(), ((Integer) obj10).intValue(), ((Integer) obj11).intValue());
                        return startOperationImpl;
                    }
                });
            } else if (this.mCheckOpsDelegate != null) {
                return startDelegateOperationImpl(token, code, uid, packageName, attributionTag, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, attributionFlags, attributionChainId);
            } else {
                return AppOpsService.this.startOperationImpl(token, code, uid, packageName, attributionTag, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, attributionFlags, attributionChainId);
            }
        }

        public SyncNotedAppOp startDelegateOperationImpl(IBinder token, int code, int uid, String packageName, String attributionTag, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, int attributionFlags, int attributionChainId) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mCheckOpsDelegate;
            final AppOpsService appOpsService = AppOpsService.this;
            return checkOpsDelegate.startOperation(token, code, uid, packageName, attributionTag, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, attributionFlags, attributionChainId, new UndecFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda15
                public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11) {
                    SyncNotedAppOp startOperationImpl;
                    startOperationImpl = AppOpsService.this.startOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (String) obj4, (String) obj5, ((Boolean) obj6).booleanValue(), ((Boolean) obj7).booleanValue(), (String) obj8, ((Boolean) obj9).booleanValue(), ((Integer) obj10).intValue(), ((Integer) obj11).intValue());
                    return startOperationImpl;
                }
            });
        }

        public SyncNotedAppOp startProxyOperation(IBinder clientId, int code, AttributionSource attributionSource, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation, int proxyAttributionFlags, int proxiedAttributionFlags, int attributionChainId) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mPolicy;
            if (checkOpsDelegate != null) {
                if (this.mCheckOpsDelegate != null) {
                    return checkOpsDelegate.startProxyOperation(clientId, code, attributionSource, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId, new UndecFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda0
                        public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11) {
                            SyncNotedAppOp startDelegateProxyOperationImpl;
                            startDelegateProxyOperationImpl = AppOpsService.CheckOpsDelegateDispatcher.this.startDelegateProxyOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), (AttributionSource) obj3, ((Boolean) obj4).booleanValue(), ((Boolean) obj5).booleanValue(), (String) obj6, ((Boolean) obj7).booleanValue(), ((Boolean) obj8).booleanValue(), ((Integer) obj9).intValue(), ((Integer) obj10).intValue(), ((Integer) obj11).intValue());
                            return startDelegateProxyOperationImpl;
                        }
                    });
                }
                final AppOpsService appOpsService = AppOpsService.this;
                return checkOpsDelegate.startProxyOperation(clientId, code, attributionSource, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId, new UndecFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda1
                    public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11) {
                        SyncNotedAppOp startProxyOperationImpl;
                        startProxyOperationImpl = AppOpsService.this.startProxyOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), (AttributionSource) obj3, ((Boolean) obj4).booleanValue(), ((Boolean) obj5).booleanValue(), (String) obj6, ((Boolean) obj7).booleanValue(), ((Boolean) obj8).booleanValue(), ((Integer) obj9).intValue(), ((Integer) obj10).intValue(), ((Integer) obj11).intValue());
                        return startProxyOperationImpl;
                    }
                });
            } else if (this.mCheckOpsDelegate != null) {
                return startDelegateProxyOperationImpl(clientId, code, attributionSource, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId);
            } else {
                return AppOpsService.this.startProxyOperationImpl(clientId, code, attributionSource, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId);
            }
        }

        public SyncNotedAppOp startDelegateProxyOperationImpl(IBinder clientId, int code, AttributionSource attributionSource, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation, int proxyAttributionFlags, int proxiedAttributionFlsgs, int attributionChainId) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mCheckOpsDelegate;
            final AppOpsService appOpsService = AppOpsService.this;
            return checkOpsDelegate.startProxyOperation(clientId, code, attributionSource, startIfModeDefault, shouldCollectAsyncNotedOp, message, shouldCollectMessage, skipProxyOperation, proxyAttributionFlags, proxiedAttributionFlsgs, attributionChainId, new UndecFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda9
                public final Object apply(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10, Object obj11) {
                    SyncNotedAppOp startProxyOperationImpl;
                    startProxyOperationImpl = AppOpsService.this.startProxyOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), (AttributionSource) obj3, ((Boolean) obj4).booleanValue(), ((Boolean) obj5).booleanValue(), (String) obj6, ((Boolean) obj7).booleanValue(), ((Boolean) obj8).booleanValue(), ((Integer) obj9).intValue(), ((Integer) obj10).intValue(), ((Integer) obj11).intValue());
                    return startProxyOperationImpl;
                }
            });
        }

        public void finishOperation(IBinder clientId, int code, int uid, String packageName, String attributionTag) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mPolicy;
            if (checkOpsDelegate != null) {
                if (this.mCheckOpsDelegate != null) {
                    checkOpsDelegate.finishOperation(clientId, code, uid, packageName, attributionTag, new QuintConsumer() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda19
                        public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                            AppOpsService.CheckOpsDelegateDispatcher.this.finishDelegateOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (String) obj4, (String) obj5);
                        }
                    });
                    return;
                }
                final AppOpsService appOpsService = AppOpsService.this;
                checkOpsDelegate.finishOperation(clientId, code, uid, packageName, attributionTag, new QuintConsumer() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda20
                    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                        AppOpsService.this.finishOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (String) obj4, (String) obj5);
                    }
                });
            } else if (this.mCheckOpsDelegate != null) {
                finishDelegateOperationImpl(clientId, code, uid, packageName, attributionTag);
            } else {
                AppOpsService.this.finishOperationImpl(clientId, code, uid, packageName, attributionTag);
            }
        }

        public void finishDelegateOperationImpl(IBinder clientId, int code, int uid, String packageName, String attributionTag) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mCheckOpsDelegate;
            final AppOpsService appOpsService = AppOpsService.this;
            checkOpsDelegate.finishOperation(clientId, code, uid, packageName, attributionTag, new QuintConsumer() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda21
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    AppOpsService.this.finishOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (String) obj4, (String) obj5);
                }
            });
        }

        public void finishProxyOperation(IBinder clientId, int code, AttributionSource attributionSource, boolean skipProxyOperation) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mPolicy;
            if (checkOpsDelegate != null) {
                if (this.mCheckOpsDelegate != null) {
                    checkOpsDelegate.finishProxyOperation(clientId, code, attributionSource, skipProxyOperation, new QuadFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda11
                        public final Object apply(Object obj, Object obj2, Object obj3, Object obj4) {
                            Void finishDelegateProxyOperationImpl;
                            finishDelegateProxyOperationImpl = AppOpsService.CheckOpsDelegateDispatcher.this.finishDelegateProxyOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), (AttributionSource) obj3, ((Boolean) obj4).booleanValue());
                            return finishDelegateProxyOperationImpl;
                        }
                    });
                    return;
                }
                final AppOpsService appOpsService = AppOpsService.this;
                checkOpsDelegate.finishProxyOperation(clientId, code, attributionSource, skipProxyOperation, new QuadFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda12
                    public final Object apply(Object obj, Object obj2, Object obj3, Object obj4) {
                        Void finishProxyOperationImpl;
                        finishProxyOperationImpl = AppOpsService.this.finishProxyOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), (AttributionSource) obj3, ((Boolean) obj4).booleanValue());
                        return finishProxyOperationImpl;
                    }
                });
            } else if (this.mCheckOpsDelegate != null) {
                finishDelegateProxyOperationImpl(clientId, code, attributionSource, skipProxyOperation);
            } else {
                AppOpsService.this.finishProxyOperationImpl(clientId, code, attributionSource, skipProxyOperation);
            }
        }

        public Void finishDelegateProxyOperationImpl(IBinder clientId, int code, AttributionSource attributionSource, boolean skipProxyOperation) {
            AppOpsManagerInternal.CheckOpsDelegate checkOpsDelegate = this.mCheckOpsDelegate;
            final AppOpsService appOpsService = AppOpsService.this;
            checkOpsDelegate.finishProxyOperation(clientId, code, attributionSource, skipProxyOperation, new QuadFunction() { // from class: com.android.server.appop.AppOpsService$CheckOpsDelegateDispatcher$$ExternalSyntheticLambda10
                public final Object apply(Object obj, Object obj2, Object obj3, Object obj4) {
                    Void finishProxyOperationImpl;
                    finishProxyOperationImpl = AppOpsService.this.finishProxyOperationImpl((IBinder) obj, ((Integer) obj2).intValue(), (AttributionSource) obj3, ((Boolean) obj4).booleanValue());
                    return finishProxyOperationImpl;
                }
            });
            return null;
        }
    }
}
