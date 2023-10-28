package com.android.server.appop;

import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.os.AtomicDirectory;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.app.GameManagerService;
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class HistoricalRegistry {
    private static final boolean DEBUG = false;
    private static final long DEFAULT_COMPRESSION_STEP = 10;
    private static final int DEFAULT_MODE = 1;
    private static final String HISTORY_FILE_SUFFIX = ".xml";
    private static final int MSG_WRITE_PENDING_HISTORY = 1;
    private static final String PARAMETER_ASSIGNMENT = "=";
    private static final String PARAMETER_DELIMITER = ",";
    private static final String PROPERTY_PERMISSIONS_HUB_ENABLED = "permissions_hub_enabled";
    private long mBaseSnapshotInterval;
    private AppOpsManager.HistoricalOps mCurrentHistoricalOps;
    private volatile DiscreteRegistry mDiscreteRegistry;
    private final Object mInMemoryLock;
    private long mIntervalCompressionMultiplier;
    private int mMode;
    private long mNextPersistDueTimeMillis;
    private final Object mOnDiskLock;
    private long mPendingHistoryOffsetMillis;
    private LinkedList<AppOpsManager.HistoricalOps> mPendingWrites;
    private Persistence mPersistence;
    private static final boolean KEEP_WTF_LOG = Build.IS_DEBUGGABLE;
    private static final String LOG_TAG = HistoricalRegistry.class.getSimpleName();
    private static final long DEFAULT_SNAPSHOT_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(15);

    /* JADX INFO: Access modifiers changed from: package-private */
    public HistoricalRegistry(Object lock) {
        this.mPendingWrites = new LinkedList<>();
        this.mOnDiskLock = new Object();
        this.mMode = 1;
        this.mBaseSnapshotInterval = DEFAULT_SNAPSHOT_INTERVAL_MILLIS;
        this.mIntervalCompressionMultiplier = DEFAULT_COMPRESSION_STEP;
        this.mInMemoryLock = lock;
        this.mDiscreteRegistry = new DiscreteRegistry(lock);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HistoricalRegistry(HistoricalRegistry other) {
        this(other.mInMemoryLock);
        this.mMode = other.mMode;
        this.mBaseSnapshotInterval = other.mBaseSnapshotInterval;
        this.mIntervalCompressionMultiplier = other.mIntervalCompressionMultiplier;
        this.mDiscreteRegistry = other.mDiscreteRegistry;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemReady(final ContentResolver resolver) {
        this.mDiscreteRegistry.systemReady();
        Uri uri = Settings.Global.getUriFor("appop_history_parameters");
        resolver.registerContentObserver(uri, false, new ContentObserver(FgThread.getHandler()) { // from class: com.android.server.appop.HistoricalRegistry.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                HistoricalRegistry.this.updateParametersFromSetting(resolver);
            }
        });
        updateParametersFromSetting(resolver);
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                if (this.mMode != 0) {
                    if (!isPersistenceInitializedMLocked()) {
                        this.mPersistence = new Persistence(this.mBaseSnapshotInterval, this.mIntervalCompressionMultiplier);
                    }
                    long lastPersistTimeMills = this.mPersistence.getLastPersistTimeMillisDLocked();
                    if (lastPersistTimeMills > 0) {
                        this.mPendingHistoryOffsetMillis = System.currentTimeMillis() - lastPersistTimeMills;
                    }
                }
            }
        }
    }

    private boolean isPersistenceInitializedMLocked() {
        return this.mPersistence != null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0054, code lost:
        if (r11.equals("intervalMultiplier") != false) goto L14;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void updateParametersFromSetting(ContentResolver resolver) {
        String setting = Settings.Global.getString(resolver, "appop_history_parameters");
        if (setting == null) {
            return;
        }
        String[] parameters = setting.split(PARAMETER_DELIMITER);
        String intervalMultiplierValue = null;
        String baseSnapshotIntervalValue = null;
        String modeValue = null;
        for (String parameter : parameters) {
            String[] parts = parameter.split(PARAMETER_ASSIGNMENT);
            char c = 2;
            if (parts.length == 2) {
                String key = parts[0].trim();
                switch (key.hashCode()) {
                    case -190198682:
                        break;
                    case 3357091:
                        if (key.equals(GameManagerService.GamePackageConfiguration.GameModeConfiguration.MODE_KEY)) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 245634204:
                        if (key.equals("baseIntervalMillis")) {
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
                        modeValue = parts[1].trim();
                        continue;
                    case 1:
                        baseSnapshotIntervalValue = parts[1].trim();
                        continue;
                    case 2:
                        intervalMultiplierValue = parts[1].trim();
                        continue;
                    default:
                        Slog.w(LOG_TAG, "Unknown parameter: " + parameter);
                        continue;
                }
            }
        }
        if (modeValue != null && baseSnapshotIntervalValue != null && intervalMultiplierValue != null) {
            try {
                int mode = AppOpsManager.parseHistoricalMode(modeValue);
                long baseSnapshotInterval = Long.parseLong(baseSnapshotIntervalValue);
                int intervalCompressionMultiplier = Integer.parseInt(intervalMultiplierValue);
                setHistoryParameters(mode, baseSnapshotInterval, intervalCompressionMultiplier);
                return;
            } catch (NumberFormatException e) {
            }
        }
        Slog.w(LOG_TAG, "Bad value forappop_history_parameters=" + setting + " resetting!");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw, int filterUid, String filterPackage, String filterAttributionTag, int filterOp, int filter) {
        if (!isApiEnabled()) {
            return;
        }
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                pw.println();
                pw.print(prefix);
                pw.print("History:");
                pw.print("  mode=");
                pw.println(AppOpsManager.historicalModeToString(this.mMode));
                StringDumpVisitor visitor = new StringDumpVisitor(prefix + "  ", pw, filterUid, filterPackage, filterAttributionTag, filterOp, filter);
                long nowMillis = System.currentTimeMillis();
                AppOpsManager.HistoricalOps currentOps = getUpdatedPendingHistoricalOpsMLocked(nowMillis);
                makeRelativeToEpochStart(currentOps, nowMillis);
                currentOps.accept(visitor);
                if (!isPersistenceInitializedMLocked()) {
                    Slog.e(LOG_TAG, "Interaction before persistence initialized");
                    return;
                }
                List<AppOpsManager.HistoricalOps> ops = this.mPersistence.readHistoryDLocked();
                if (ops != null) {
                    long remainingToFillBatchMillis = (this.mNextPersistDueTimeMillis - nowMillis) - this.mBaseSnapshotInterval;
                    int opCount = ops.size();
                    for (int i = 0; i < opCount; i++) {
                        AppOpsManager.HistoricalOps op = ops.get(i);
                        op.offsetBeginAndEndTime(remainingToFillBatchMillis);
                        makeRelativeToEpochStart(op, nowMillis);
                        op.accept(visitor);
                    }
                } else {
                    pw.println("  Empty");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDiscreteData(PrintWriter pw, int uidFilter, String packageNameFilter, String attributionTagFilter, int filter, int dumpOp, SimpleDateFormat sdf, Date date, String prefix, int nDiscreteOps) {
        this.mDiscreteRegistry.dump(pw, uidFilter, packageNameFilter, attributionTagFilter, filter, dumpOp, sdf, date, prefix, nDiscreteOps);
    }

    int getMode() {
        int i;
        synchronized (this.mInMemoryLock) {
            i = this.mMode;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getHistoricalOpsFromDiskRaw(int uid, String packageName, String attributionTag, String[] opNames, int historyFlags, int filter, long beginTimeMillis, long endTimeMillis, int flags, String[] attributionExemptedPackages, RemoteCallback callback) {
        AppOpsManager.HistoricalOps result;
        Object obj;
        if (!isApiEnabled()) {
            callback.sendResult(new Bundle());
            return;
        }
        Parcelable historicalOps = new AppOpsManager.HistoricalOps(beginTimeMillis, endTimeMillis);
        if ((historyFlags & 1) == 0) {
            result = historicalOps;
        } else {
            Object obj2 = this.mOnDiskLock;
            synchronized (obj2) {
                try {
                    try {
                        synchronized (this.mInMemoryLock) {
                            try {
                                if (!isPersistenceInitializedMLocked()) {
                                    try {
                                        Slog.e(LOG_TAG, "Interaction before persistence initialized");
                                        callback.sendResult(new Bundle());
                                        try {
                                            return;
                                        } catch (Throwable th) {
                                            th = th;
                                            obj = obj2;
                                            throw th;
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th3) {
                                                th = th3;
                                            }
                                        }
                                        throw th;
                                    }
                                }
                                result = historicalOps;
                                this.mPersistence.collectHistoricalOpsDLocked(historicalOps, uid, packageName, attributionTag, opNames, filter, beginTimeMillis, endTimeMillis, flags);
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        obj = obj2;
                    }
                } catch (Throwable th6) {
                    th = th6;
                }
            }
        }
        if ((historyFlags & 2) != 0) {
            this.mDiscreteRegistry.addFilteredDiscreteOpsToHistoricalOps(result, beginTimeMillis, endTimeMillis, filter, uid, packageName, opNames, attributionTag, flags, new ArraySet(attributionExemptedPackages));
        }
        Bundle payload = new Bundle();
        payload.putParcelable("historical_ops", result);
        callback.sendResult(payload);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [463=5, 481=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void getHistoricalOps(int uid, String packageName, String attributionTag, String[] opNames, int historyFlags, int filter, long beginTimeMillis, long endTimeMillis, int flags, String[] attributionExemptPkgs, RemoteCallback callback) {
        long inMemoryAdjEndTimeMillis;
        long inMemoryAdjBeginTimeMillis;
        Bundle payload;
        long endTimeMillis2;
        long currentTimeMillis;
        RemoteCallback remoteCallback;
        AppOpsManager.HistoricalOps result;
        RemoteCallback remoteCallback2;
        Object obj;
        long currentTimeMillis2;
        Object obj2;
        if (!isApiEnabled()) {
            callback.sendResult(new Bundle());
            return;
        }
        long currentTimeMillis3 = System.currentTimeMillis();
        long endTimeMillis3 = endTimeMillis == JobStatus.NO_LATEST_RUNTIME ? currentTimeMillis3 : endTimeMillis;
        Bundle payload2 = new Bundle();
        long inMemoryAdjBeginTimeMillis2 = Math.max(currentTimeMillis3 - endTimeMillis3, 0L);
        long inMemoryAdjEndTimeMillis2 = Math.max(currentTimeMillis3 - beginTimeMillis, 0L);
        AppOpsManager.HistoricalOps result2 = new AppOpsManager.HistoricalOps(inMemoryAdjBeginTimeMillis2, inMemoryAdjEndTimeMillis2);
        if ((historyFlags & 2) != 0) {
            inMemoryAdjEndTimeMillis = inMemoryAdjEndTimeMillis2;
            inMemoryAdjBeginTimeMillis = inMemoryAdjBeginTimeMillis2;
            payload = payload2;
            endTimeMillis2 = endTimeMillis3;
            currentTimeMillis = currentTimeMillis3;
            remoteCallback = callback;
            this.mDiscreteRegistry.addFilteredDiscreteOpsToHistoricalOps(result2, beginTimeMillis, endTimeMillis3, filter, uid, packageName, opNames, attributionTag, flags, new ArraySet(attributionExemptPkgs));
        } else {
            inMemoryAdjEndTimeMillis = inMemoryAdjEndTimeMillis2;
            inMemoryAdjBeginTimeMillis = inMemoryAdjBeginTimeMillis2;
            payload = payload2;
            endTimeMillis2 = endTimeMillis3;
            currentTimeMillis = currentTimeMillis3;
            remoteCallback = callback;
        }
        if ((historyFlags & 1) != 0) {
            Object obj3 = this.mOnDiskLock;
            synchronized (obj3) {
                try {
                    try {
                        synchronized (this.mInMemoryLock) {
                            try {
                                if (isPersistenceInitializedMLocked()) {
                                    long currentTimeMillis4 = currentTimeMillis;
                                    try {
                                        AppOpsManager.HistoricalOps currentOps = getUpdatedPendingHistoricalOpsMLocked(currentTimeMillis4);
                                        if (inMemoryAdjBeginTimeMillis < currentOps.getEndTimeMillis()) {
                                            try {
                                                if (inMemoryAdjEndTimeMillis > currentOps.getBeginTimeMillis()) {
                                                    AppOpsManager.HistoricalOps currentOpsCopy = new AppOpsManager.HistoricalOps(currentOps);
                                                    currentTimeMillis2 = currentTimeMillis4;
                                                    try {
                                                        currentOpsCopy.filter(uid, packageName, attributionTag, opNames, historyFlags, filter, inMemoryAdjBeginTimeMillis, inMemoryAdjEndTimeMillis);
                                                        result = result2;
                                                        try {
                                                            result.merge(currentOpsCopy);
                                                        } catch (Throwable th) {
                                                            th = th;
                                                            while (true) {
                                                                break;
                                                                break;
                                                            }
                                                            throw th;
                                                        }
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                } else {
                                                    result = result2;
                                                    currentTimeMillis2 = currentTimeMillis4;
                                                }
                                            } catch (Throwable th3) {
                                                th = th3;
                                            }
                                        } else {
                                            result = result2;
                                            currentTimeMillis2 = currentTimeMillis4;
                                        }
                                        List<AppOpsManager.HistoricalOps> pendingWrites = new ArrayList<>(this.mPendingWrites);
                                        this.mPendingWrites.clear();
                                        boolean collectOpsFromDisk = inMemoryAdjEndTimeMillis > currentOps.getEndTimeMillis();
                                        if (collectOpsFromDisk) {
                                            try {
                                                persistPendingHistory(pendingWrites);
                                                long onDiskAndInMemoryOffsetMillis = (currentTimeMillis2 - this.mNextPersistDueTimeMillis) + this.mBaseSnapshotInterval;
                                                long onDiskAdjBeginTimeMillis = Math.max(inMemoryAdjBeginTimeMillis - onDiskAndInMemoryOffsetMillis, 0L);
                                                long onDiskAdjEndTimeMillis = Math.max(inMemoryAdjEndTimeMillis - onDiskAndInMemoryOffsetMillis, 0L);
                                                obj2 = obj3;
                                                remoteCallback2 = callback;
                                                this.mPersistence.collectHistoricalOpsDLocked(result, uid, packageName, attributionTag, opNames, filter, onDiskAdjBeginTimeMillis, onDiskAdjEndTimeMillis, flags);
                                            } catch (Throwable th4) {
                                                th = th4;
                                                obj = obj3;
                                                throw th;
                                            }
                                        } else {
                                            remoteCallback2 = callback;
                                            obj2 = obj3;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                    }
                                } else {
                                    try {
                                        Slog.e(LOG_TAG, "Interaction before persistence initialized");
                                        remoteCallback.sendResult(new Bundle());
                                        try {
                                        } catch (Throwable th6) {
                                            th = th6;
                                            obj = obj3;
                                            throw th;
                                        }
                                    } catch (Throwable th7) {
                                        th = th7;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th8) {
                                                th = th8;
                                            }
                                        }
                                        throw th;
                                    }
                                }
                            } catch (Throwable th9) {
                                th = th9;
                            }
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        obj = obj3;
                    }
                } catch (Throwable th11) {
                    th = th11;
                }
            }
            return;
        }
        result = result2;
        remoteCallback2 = remoteCallback;
        result.setBeginAndEndTime(beginTimeMillis, endTimeMillis2);
        Bundle payload3 = payload;
        payload3.putParcelable("historical_ops", result);
        remoteCallback2.sendResult(payload3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void incrementOpAccessedCount(int op, int uid, String packageName, String attributionTag, int uidState, int flags, long accessTime, int attributionFlags, int attributionChainId) {
        synchronized (this.mInMemoryLock) {
            if (this.mMode == 1) {
                if (!isPersistenceInitializedMLocked()) {
                    Slog.v(LOG_TAG, "Interaction before persistence initialized");
                } else {
                    getUpdatedPendingHistoricalOpsMLocked(System.currentTimeMillis()).increaseAccessCount(op, uid, packageName, attributionTag, uidState, flags, 1L);
                    this.mDiscreteRegistry.recordDiscreteAccess(uid, packageName, op, attributionTag, flags, uidState, accessTime, -1L, attributionFlags, attributionChainId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void incrementOpRejected(int op, int uid, String packageName, String attributionTag, int uidState, int flags) {
        synchronized (this.mInMemoryLock) {
            if (this.mMode == 1) {
                if (!isPersistenceInitializedMLocked()) {
                    Slog.v(LOG_TAG, "Interaction before persistence initialized");
                    return;
                }
                getUpdatedPendingHistoricalOpsMLocked(System.currentTimeMillis()).increaseRejectCount(op, uid, packageName, attributionTag, uidState, flags, 1L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void increaseOpAccessDuration(int op, int uid, String packageName, String attributionTag, int uidState, int flags, long eventStartTime, long increment, int attributionFlags, int attributionChainId) {
        synchronized (this.mInMemoryLock) {
            if (this.mMode == 1) {
                if (!isPersistenceInitializedMLocked()) {
                    Slog.v(LOG_TAG, "Interaction before persistence initialized");
                } else {
                    getUpdatedPendingHistoricalOpsMLocked(System.currentTimeMillis()).increaseAccessDuration(op, uid, packageName, attributionTag, uidState, flags, increment);
                    this.mDiscreteRegistry.recordDiscreteAccess(uid, packageName, op, attributionTag, flags, uidState, eventStartTime, increment, attributionFlags, attributionChainId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHistoryParameters(int mode, long baseSnapshotInterval, long intervalCompressionMultiplier) {
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                boolean resampleHistory = false;
                Slog.i(LOG_TAG, "New history parameters: mode:" + AppOpsManager.historicalModeToString(mode) + " baseSnapshotInterval:" + baseSnapshotInterval + " intervalCompressionMultiplier:" + intervalCompressionMultiplier);
                if (this.mMode != mode) {
                    this.mMode = mode;
                    if (mode == 0) {
                        clearHistoryOnDiskDLocked();
                    }
                    if (this.mMode == 2) {
                        this.mDiscreteRegistry.setDebugMode(true);
                    }
                }
                if (this.mBaseSnapshotInterval != baseSnapshotInterval) {
                    this.mBaseSnapshotInterval = baseSnapshotInterval;
                    resampleHistory = true;
                }
                if (this.mIntervalCompressionMultiplier != intervalCompressionMultiplier) {
                    this.mIntervalCompressionMultiplier = intervalCompressionMultiplier;
                    resampleHistory = true;
                }
                if (resampleHistory) {
                    resampleHistoryOnDiskInMemoryDMLocked(0L);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void offsetHistory(long offsetMillis) {
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                if (!isPersistenceInitializedMLocked()) {
                    Slog.e(LOG_TAG, "Interaction before persistence initialized");
                    return;
                }
                List<AppOpsManager.HistoricalOps> history = this.mPersistence.readHistoryDLocked();
                clearHistoricalRegistry();
                if (history != null) {
                    int historySize = history.size();
                    for (int i = 0; i < historySize; i++) {
                        AppOpsManager.HistoricalOps ops = history.get(i);
                        ops.offsetBeginAndEndTime(offsetMillis);
                    }
                    if (offsetMillis < 0) {
                        pruneFutureOps(history);
                    }
                    this.mPersistence.persistHistoricalOpsDLocked(history);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void offsetDiscreteHistory(long offsetMillis) {
        this.mDiscreteRegistry.offsetHistory(offsetMillis);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addHistoricalOps(AppOpsManager.HistoricalOps ops) {
        synchronized (this.mInMemoryLock) {
            if (!isPersistenceInitializedMLocked()) {
                Slog.d(LOG_TAG, "Interaction before persistence initialized");
                return;
            }
            ops.offsetBeginAndEndTime(this.mBaseSnapshotInterval);
            this.mPendingWrites.offerFirst(ops);
            List<AppOpsManager.HistoricalOps> pendingWrites = new ArrayList<>(this.mPendingWrites);
            this.mPendingWrites.clear();
            persistPendingHistory(pendingWrites);
        }
    }

    private void resampleHistoryOnDiskInMemoryDMLocked(long offsetMillis) {
        this.mPersistence = new Persistence(this.mBaseSnapshotInterval, this.mIntervalCompressionMultiplier);
        offsetHistory(offsetMillis);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetHistoryParameters() {
        if (!isPersistenceInitializedMLocked()) {
            Slog.d(LOG_TAG, "Interaction before persistence initialized");
            return;
        }
        setHistoryParameters(1, DEFAULT_SNAPSHOT_INTERVAL_MILLIS, DEFAULT_COMPRESSION_STEP);
        this.mDiscreteRegistry.setDebugMode(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearHistory(int uid, String packageName) {
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                if (!isPersistenceInitializedMLocked()) {
                    Slog.d(LOG_TAG, "Interaction before persistence initialized");
                } else if (this.mMode == 1) {
                    for (int index = 0; index < this.mPendingWrites.size(); index++) {
                        this.mPendingWrites.get(index).clearHistory(uid, packageName);
                    }
                    getUpdatedPendingHistoricalOpsMLocked(System.currentTimeMillis()).clearHistory(uid, packageName);
                    this.mPersistence.clearHistoryDLocked(uid, packageName);
                    this.mDiscreteRegistry.clearHistory(uid, packageName);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeAndClearDiscreteHistory() {
        this.mDiscreteRegistry.writeAndClearAccessHistory();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAllHistory() {
        clearHistoricalRegistry();
        this.mDiscreteRegistry.clearHistory();
    }

    void clearHistoricalRegistry() {
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                if (!isPersistenceInitializedMLocked()) {
                    Slog.d(LOG_TAG, "Interaction before persistence initialized");
                    return;
                }
                clearHistoryOnDiskDLocked();
                this.mNextPersistDueTimeMillis = 0L;
                this.mPendingHistoryOffsetMillis = 0L;
                this.mCurrentHistoricalOps = null;
            }
        }
    }

    private void clearHistoryOnDiskDLocked() {
        BackgroundThread.getHandler().removeMessages(1);
        synchronized (this.mInMemoryLock) {
            this.mCurrentHistoricalOps = null;
            this.mNextPersistDueTimeMillis = System.currentTimeMillis();
            this.mPendingWrites.clear();
        }
        Persistence.clearHistoryDLocked();
    }

    private AppOpsManager.HistoricalOps getUpdatedPendingHistoricalOpsMLocked(long now) {
        AppOpsManager.HistoricalOps historicalOps = this.mCurrentHistoricalOps;
        if (historicalOps != null) {
            long remainingTimeMillis = this.mNextPersistDueTimeMillis - now;
            long j = this.mBaseSnapshotInterval;
            if (remainingTimeMillis > j) {
                this.mPendingHistoryOffsetMillis = remainingTimeMillis - j;
            }
            long elapsedTimeMillis = j - remainingTimeMillis;
            historicalOps.setEndTime(elapsedTimeMillis);
            if (remainingTimeMillis > 0) {
                return this.mCurrentHistoricalOps;
            }
            if (this.mCurrentHistoricalOps.isEmpty()) {
                this.mCurrentHistoricalOps.setBeginAndEndTime(0L, 0L);
                this.mNextPersistDueTimeMillis = this.mBaseSnapshotInterval + now;
                return this.mCurrentHistoricalOps;
            }
            this.mCurrentHistoricalOps.offsetBeginAndEndTime(this.mBaseSnapshotInterval);
            AppOpsManager.HistoricalOps historicalOps2 = this.mCurrentHistoricalOps;
            historicalOps2.setBeginTime(historicalOps2.getEndTimeMillis() - this.mBaseSnapshotInterval);
            long overdueTimeMillis = Math.abs(remainingTimeMillis);
            this.mCurrentHistoricalOps.offsetBeginAndEndTime(overdueTimeMillis);
            schedulePersistHistoricalOpsMLocked(this.mCurrentHistoricalOps);
        }
        AppOpsManager.HistoricalOps historicalOps3 = new AppOpsManager.HistoricalOps(0L, 0L);
        this.mCurrentHistoricalOps = historicalOps3;
        this.mNextPersistDueTimeMillis = this.mBaseSnapshotInterval + now;
        return historicalOps3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void shutdown() {
        synchronized (this.mInMemoryLock) {
            if (this.mMode == 0) {
                return;
            }
            persistPendingHistory();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void persistPendingHistory() {
        List<AppOpsManager.HistoricalOps> pendingWrites;
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                pendingWrites = new ArrayList<>(this.mPendingWrites);
                this.mPendingWrites.clear();
                long j = this.mPendingHistoryOffsetMillis;
                if (j != 0) {
                    resampleHistoryOnDiskInMemoryDMLocked(j);
                    this.mPendingHistoryOffsetMillis = 0L;
                }
            }
            persistPendingHistory(pendingWrites);
        }
        this.mDiscreteRegistry.writeAndClearAccessHistory();
    }

    private void persistPendingHistory(List<AppOpsManager.HistoricalOps> pendingWrites) {
        synchronized (this.mOnDiskLock) {
            BackgroundThread.getHandler().removeMessages(1);
            if (pendingWrites.isEmpty()) {
                return;
            }
            int opCount = pendingWrites.size();
            for (int i = 0; i < opCount; i++) {
                AppOpsManager.HistoricalOps current = pendingWrites.get(i);
                if (i > 0) {
                    AppOpsManager.HistoricalOps previous = pendingWrites.get(i - 1);
                    current.offsetBeginAndEndTime(previous.getBeginTimeMillis());
                }
            }
            this.mPersistence.persistHistoricalOpsDLocked(pendingWrites);
        }
    }

    private void schedulePersistHistoricalOpsMLocked(AppOpsManager.HistoricalOps ops) {
        Message message = PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.appop.HistoricalRegistry$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((HistoricalRegistry) obj).persistPendingHistory();
            }
        }, this);
        message.what = 1;
        BackgroundThread.getHandler().sendMessage(message);
        this.mPendingWrites.offerFirst(ops);
    }

    private static void makeRelativeToEpochStart(AppOpsManager.HistoricalOps ops, long nowMillis) {
        ops.setBeginAndEndTime(nowMillis - ops.getEndTimeMillis(), nowMillis - ops.getBeginTimeMillis());
    }

    private void pruneFutureOps(List<AppOpsManager.HistoricalOps> ops) {
        int opCount = ops.size();
        for (int i = opCount - 1; i >= 0; i--) {
            AppOpsManager.HistoricalOps op = ops.get(i);
            if (op.getEndTimeMillis() <= this.mBaseSnapshotInterval) {
                ops.remove(i);
            } else if (op.getBeginTimeMillis() < this.mBaseSnapshotInterval) {
                double filterScale = (op.getEndTimeMillis() - this.mBaseSnapshotInterval) / op.getDurationMillis();
                Persistence.spliceFromBeginning(op, filterScale);
            }
        }
    }

    private static boolean isApiEnabled() {
        return Binder.getCallingUid() == Process.myUid() || DeviceConfig.getBoolean("privacy", PROPERTY_PERMISSIONS_HUB_ENABLED, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class Persistence {
        private static final String ATTR_ACCESS_COUNT = "ac";
        private static final String ATTR_ACCESS_DURATION = "du";
        private static final String ATTR_BEGIN_TIME = "beg";
        private static final String ATTR_END_TIME = "end";
        private static final String ATTR_NAME = "na";
        private static final String ATTR_OVERFLOW = "ov";
        private static final String ATTR_REJECT_COUNT = "rc";
        private static final String ATTR_VERSION = "ver";
        private static final int CURRENT_VERSION = 2;
        private static final boolean DEBUG = false;
        private static final String TAG_ATTRIBUTION = "ftr";
        private static final String TAG_OP = "op";
        private static final String TAG_OPS = "ops";
        private static final String TAG_PACKAGE = "pkg";
        private static final String TAG_STATE = "st";
        private static final String TAG_UID = "uid";
        private final long mBaseSnapshotInterval;
        private final long mIntervalCompressionMultiplier;
        private static final String LOG_TAG = Persistence.class.getSimpleName();
        private static final String TAG_HISTORY = "history";
        private static final AtomicDirectory sHistoricalAppOpsDir = new AtomicDirectory(new File(new File(Environment.getDataSystemDirectory(), "appops"), TAG_HISTORY));

        Persistence(long baseSnapshotInterval, long intervalCompressionMultiplier) {
            this.mBaseSnapshotInterval = baseSnapshotInterval;
            this.mIntervalCompressionMultiplier = intervalCompressionMultiplier;
        }

        private File generateFile(File baseDir, int depth) {
            long globalBeginMillis = computeGlobalIntervalBeginMillis(depth);
            return new File(baseDir, Long.toString(globalBeginMillis) + HistoricalRegistry.HISTORY_FILE_SUFFIX);
        }

        void clearHistoryDLocked(int uid, String packageName) {
            List<AppOpsManager.HistoricalOps> historicalOps = readHistoryDLocked();
            if (historicalOps == null) {
                return;
            }
            for (int index = 0; index < historicalOps.size(); index++) {
                historicalOps.get(index).clearHistory(uid, packageName);
            }
            clearHistoryDLocked();
            persistHistoricalOpsDLocked(historicalOps);
        }

        static void clearHistoryDLocked() {
            sHistoricalAppOpsDir.delete();
        }

        void persistHistoricalOpsDLocked(List<AppOpsManager.HistoricalOps> ops) {
            try {
                AtomicDirectory atomicDirectory = sHistoricalAppOpsDir;
                File newBaseDir = atomicDirectory.startWrite();
                File oldBaseDir = atomicDirectory.getBackupDirectory();
                Set<String> oldFileNames = getHistoricalFileNames(oldBaseDir);
                handlePersistHistoricalOpsRecursiveDLocked(newBaseDir, oldBaseDir, ops, oldFileNames, 0);
                atomicDirectory.finishWrite();
            } catch (Throwable t) {
                if (t instanceof OutOfMemoryError) {
                    throw new Error(t);
                }
                HistoricalRegistry.wtf("Failed to write historical app ops, restoring backup", t, null);
                sHistoricalAppOpsDir.failWrite();
            }
        }

        List<AppOpsManager.HistoricalOps> readHistoryRawDLocked() {
            return collectHistoricalOpsBaseDLocked(-1, null, null, null, 0, 0L, JobStatus.NO_LATEST_RUNTIME, 31);
        }

        List<AppOpsManager.HistoricalOps> readHistoryDLocked() {
            List<AppOpsManager.HistoricalOps> result = readHistoryRawDLocked();
            if (result != null) {
                int opCount = result.size();
                for (int i = 0; i < opCount; i++) {
                    result.get(i).offsetBeginAndEndTime(this.mBaseSnapshotInterval);
                }
            }
            return result;
        }

        long getLastPersistTimeMillisDLocked() {
            AtomicDirectory atomicDirectory;
            File[] files;
            try {
                atomicDirectory = sHistoricalAppOpsDir;
                File baseDir = atomicDirectory.startRead();
                files = baseDir.listFiles();
            } catch (Throwable e) {
                HistoricalRegistry.wtf("Error reading historical app ops. Deleting history.", e, null);
                sHistoricalAppOpsDir.delete();
            }
            if (files != null && files.length > 0) {
                File shortestFile = null;
                for (File candidate : files) {
                    String candidateName = candidate.getName();
                    if (candidateName.endsWith(HistoricalRegistry.HISTORY_FILE_SUFFIX)) {
                        if (shortestFile == null) {
                            shortestFile = candidate;
                        } else if (candidateName.length() < shortestFile.getName().length()) {
                            shortestFile = candidate;
                        }
                    }
                }
                if (shortestFile == null) {
                    return 0L;
                }
                return shortestFile.lastModified();
            }
            atomicDirectory.finishRead();
            return 0L;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void collectHistoricalOpsDLocked(AppOpsManager.HistoricalOps currentOps, int filterUid, String filterPackageName, String filterAttributionTag, String[] filterOpNames, int filter, long filterBeingMillis, long filterEndMillis, int filterFlags) {
            List<AppOpsManager.HistoricalOps> readOps = collectHistoricalOpsBaseDLocked(filterUid, filterPackageName, filterAttributionTag, filterOpNames, filter, filterBeingMillis, filterEndMillis, filterFlags);
            if (readOps != null) {
                int readCount = readOps.size();
                for (int i = 0; i < readCount; i++) {
                    AppOpsManager.HistoricalOps readOp = readOps.get(i);
                    currentOps.merge(readOp);
                }
            }
        }

        private LinkedList<AppOpsManager.HistoricalOps> collectHistoricalOpsBaseDLocked(int filterUid, String filterPackageName, String filterAttributionTag, String[] filterOpNames, int filter, long filterBeginTimeMillis, long filterEndTimeMillis, int filterFlags) {
            File baseDir = null;
            try {
                AtomicDirectory atomicDirectory = sHistoricalAppOpsDir;
                File baseDir2 = atomicDirectory.startRead();
                try {
                    Set<String> historyFiles = getHistoricalFileNames(baseDir2);
                    long[] globalContentOffsetMillis = {0};
                    LinkedList<AppOpsManager.HistoricalOps> ops = collectHistoricalOpsRecursiveDLocked(baseDir2, filterUid, filterPackageName, filterAttributionTag, filterOpNames, filter, filterBeginTimeMillis, filterEndTimeMillis, filterFlags, globalContentOffsetMillis, null, 0, historyFiles);
                    atomicDirectory.finishRead();
                    return ops;
                } catch (Throwable th) {
                    t = th;
                    baseDir = baseDir2;
                    HistoricalRegistry.wtf("Error reading historical app ops. Deleting history.", t, baseDir);
                    sHistoricalAppOpsDir.delete();
                    return null;
                }
            } catch (Throwable th2) {
                t = th2;
            }
        }

        private LinkedList<AppOpsManager.HistoricalOps> collectHistoricalOpsRecursiveDLocked(File baseDir, int filterUid, String filterPackageName, String filterAttributionTag, String[] filterOpNames, int filter, long filterBeginTimeMillis, long filterEndTimeMillis, int filterFlags, long[] globalContentOffsetMillis, LinkedList<AppOpsManager.HistoricalOps> outOps, int depth, Set<String> historyFiles) throws IOException, XmlPullParserException {
            long previousIntervalEndMillis = ((long) Math.pow(this.mIntervalCompressionMultiplier, depth)) * this.mBaseSnapshotInterval;
            long currentIntervalEndMillis = this.mBaseSnapshotInterval * ((long) Math.pow(this.mIntervalCompressionMultiplier, depth + 1));
            long filterBeginTimeMillis2 = Math.max(filterBeginTimeMillis - previousIntervalEndMillis, 0L);
            long filterEndTimeMillis2 = filterEndTimeMillis - previousIntervalEndMillis;
            List<AppOpsManager.HistoricalOps> readOps = readHistoricalOpsLocked(baseDir, previousIntervalEndMillis, currentIntervalEndMillis, filterUid, filterPackageName, filterAttributionTag, filterOpNames, filter, filterBeginTimeMillis2, filterEndTimeMillis2, filterFlags, globalContentOffsetMillis, depth, historyFiles);
            if (readOps != null && readOps.isEmpty()) {
                return outOps;
            }
            LinkedList<AppOpsManager.HistoricalOps> outOps2 = collectHistoricalOpsRecursiveDLocked(baseDir, filterUid, filterPackageName, filterAttributionTag, filterOpNames, filter, filterBeginTimeMillis2, filterEndTimeMillis2, filterFlags, globalContentOffsetMillis, outOps, depth + 1, historyFiles);
            if (outOps2 != null) {
                int opCount = outOps2.size();
                for (int i = 0; i < opCount; i++) {
                    AppOpsManager.HistoricalOps collectedOp = outOps2.get(i);
                    collectedOp.offsetBeginAndEndTime(currentIntervalEndMillis);
                }
            }
            if (readOps != null) {
                if (outOps2 == null) {
                    outOps2 = new LinkedList<>();
                }
                int opCount2 = readOps.size();
                for (int i2 = opCount2 - 1; i2 >= 0; i2--) {
                    outOps2.offerFirst(readOps.get(i2));
                }
            }
            return outOps2;
        }

        private void handlePersistHistoricalOpsRecursiveDLocked(File newBaseDir, File oldBaseDir, List<AppOpsManager.HistoricalOps> passedOps, Set<String> oldFileNames, int depth) throws IOException, XmlPullParserException {
            int i;
            Set<String> set;
            Persistence persistence;
            File file;
            List<AppOpsManager.HistoricalOps> list;
            AppOpsManager.HistoricalOps persistedOp;
            AppOpsManager.HistoricalOps overflowedOp;
            long previousIntervalEndMillis = ((long) Math.pow(this.mIntervalCompressionMultiplier, depth)) * this.mBaseSnapshotInterval;
            long currentIntervalEndMillis = ((long) Math.pow(this.mIntervalCompressionMultiplier, depth + 1)) * this.mBaseSnapshotInterval;
            if (passedOps == null) {
                i = depth;
                set = oldFileNames;
                persistence = this;
                file = newBaseDir;
            } else if (passedOps.isEmpty()) {
                i = depth;
                set = oldFileNames;
                persistence = this;
                file = newBaseDir;
            } else {
                int passedOpCount = passedOps.size();
                for (int i2 = 0; i2 < passedOpCount; i2++) {
                    AppOpsManager.HistoricalOps passedOp = passedOps.get(i2);
                    passedOp.offsetBeginAndEndTime(-previousIntervalEndMillis);
                }
                List<AppOpsManager.HistoricalOps> existingOps = readHistoricalOpsLocked(oldBaseDir, previousIntervalEndMillis, currentIntervalEndMillis, -1, null, null, null, 0, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, 31, null, depth, null);
                if (existingOps == null) {
                    list = passedOps;
                } else {
                    int existingOpCount = existingOps.size();
                    if (existingOpCount <= 0) {
                        list = passedOps;
                    } else {
                        list = passedOps;
                        long elapsedTimeMillis = list.get(passedOps.size() - 1).getEndTimeMillis();
                        for (int i3 = 0; i3 < existingOpCount; i3++) {
                            AppOpsManager.HistoricalOps existingOp = existingOps.get(i3);
                            existingOp.offsetBeginAndEndTime(elapsedTimeMillis);
                        }
                    }
                }
                List<AppOpsManager.HistoricalOps> allOps = new LinkedList<>(list);
                if (existingOps != null) {
                    allOps.addAll(existingOps);
                }
                int opCount = allOps.size();
                List<AppOpsManager.HistoricalOps> persistedOps = null;
                List<AppOpsManager.HistoricalOps> overflowedOps = null;
                long intervalOverflowMillis = 0;
                for (int i4 = 0; i4 < opCount; i4++) {
                    AppOpsManager.HistoricalOps op = allOps.get(i4);
                    if (op.getEndTimeMillis() <= currentIntervalEndMillis) {
                        persistedOp = op;
                        overflowedOp = null;
                    } else if (op.getBeginTimeMillis() < currentIntervalEndMillis) {
                        persistedOp = op;
                        long intervalOverflowMillis2 = op.getEndTimeMillis() - currentIntervalEndMillis;
                        if (intervalOverflowMillis2 > previousIntervalEndMillis) {
                            double splitScale = intervalOverflowMillis2 / op.getDurationMillis();
                            overflowedOp = spliceFromEnd(op, splitScale);
                            long intervalOverflowMillis3 = op.getEndTimeMillis() - currentIntervalEndMillis;
                            persistedOp = persistedOp;
                            intervalOverflowMillis = intervalOverflowMillis3;
                        } else {
                            overflowedOp = null;
                            intervalOverflowMillis = intervalOverflowMillis2;
                        }
                    } else {
                        persistedOp = null;
                        overflowedOp = op;
                    }
                    if (persistedOp != null) {
                        if (persistedOps == null) {
                            persistedOps = new ArrayList<>();
                        }
                        persistedOps.add(persistedOp);
                    }
                    if (overflowedOp != null) {
                        if (overflowedOps == null) {
                            overflowedOps = new ArrayList<>();
                        }
                        overflowedOps.add(overflowedOp);
                    }
                }
                File newFile = generateFile(newBaseDir, depth);
                oldFileNames.remove(newFile.getName());
                if (persistedOps != null) {
                    normalizeSnapshotForSlotDuration(persistedOps, previousIntervalEndMillis);
                    writeHistoricalOpsDLocked(persistedOps, intervalOverflowMillis, newFile);
                }
                handlePersistHistoricalOpsRecursiveDLocked(newBaseDir, oldBaseDir, overflowedOps, oldFileNames, depth + 1);
                return;
            }
            if (!oldFileNames.isEmpty()) {
                File oldFile = persistence.generateFile(oldBaseDir, i);
                if (set.remove(oldFile.getName())) {
                    Files.createLink(persistence.generateFile(file, i).toPath(), oldFile.toPath());
                }
                handlePersistHistoricalOpsRecursiveDLocked(newBaseDir, oldBaseDir, passedOps, oldFileNames, i + 1);
            }
        }

        private List<AppOpsManager.HistoricalOps> readHistoricalOpsLocked(File baseDir, long intervalBeginMillis, long intervalEndMillis, int filterUid, String filterPackageName, String filterAttributionTag, String[] filterOpNames, int filter, long filterBeginTimeMillis, long filterEndTimeMillis, int filterFlags, long[] cumulativeOverflowMillis, int depth, Set<String> historyFiles) throws IOException, XmlPullParserException {
            File file = generateFile(baseDir, depth);
            if (historyFiles != null) {
                historyFiles.remove(file.getName());
            }
            if (filterBeginTimeMillis >= filterEndTimeMillis || filterEndTimeMillis < intervalBeginMillis) {
                return Collections.emptyList();
            }
            if (filterBeginTimeMillis >= intervalEndMillis + ((intervalEndMillis - intervalBeginMillis) / this.mIntervalCompressionMultiplier) + (cumulativeOverflowMillis != null ? cumulativeOverflowMillis[0] : 0L) || !file.exists()) {
                if (historyFiles == null || historyFiles.isEmpty()) {
                    return Collections.emptyList();
                }
                return null;
            }
            return readHistoricalOpsLocked(file, filterUid, filterPackageName, filterAttributionTag, filterOpNames, filter, filterBeginTimeMillis, filterEndTimeMillis, filterFlags, cumulativeOverflowMillis);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1259=4] */
        private List<AppOpsManager.HistoricalOps> readHistoricalOpsLocked(File file, int filterUid, String filterPackageName, String filterAttributionTag, String[] filterOpNames, int filter, long filterBeginTimeMillis, long filterEndTimeMillis, int filterFlags, long[] cumulativeOverflowMillis) throws IOException, XmlPullParserException {
            Throwable th;
            int depth;
            int version;
            try {
                FileInputStream stream = new FileInputStream(file);
                try {
                    TypedXmlPullParser parser = Xml.resolvePullParser(stream);
                    XmlUtils.beginDocument(parser, TAG_HISTORY);
                    int version2 = parser.getAttributeInt((String) null, ATTR_VERSION);
                    if (version2 >= 2) {
                        long overflowMillis = parser.getAttributeLong((String) null, ATTR_OVERFLOW, 0L);
                        int depth2 = parser.getDepth();
                        List<AppOpsManager.HistoricalOps> allOps = null;
                        while (XmlUtils.nextElementWithin(parser, depth2)) {
                            try {
                                if (TAG_OPS.equals(parser.getName())) {
                                    depth = depth2;
                                    version = version2;
                                    AppOpsManager.HistoricalOps ops = readeHistoricalOpsDLocked(parser, filterUid, filterPackageName, filterAttributionTag, filterOpNames, filter, filterBeginTimeMillis, filterEndTimeMillis, filterFlags, cumulativeOverflowMillis);
                                    if (ops != null) {
                                        if (ops.isEmpty()) {
                                            XmlUtils.skipCurrentTag(parser);
                                        } else {
                                            List<AppOpsManager.HistoricalOps> allOps2 = allOps == null ? new ArrayList<>() : allOps;
                                            try {
                                                allOps2.add(ops);
                                                allOps = allOps2;
                                                depth2 = depth;
                                                version2 = version;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                stream.close();
                                                throw th;
                                            }
                                        }
                                    }
                                } else {
                                    depth = depth2;
                                    version = version2;
                                }
                                depth2 = depth;
                                version2 = version;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                        if (cumulativeOverflowMillis != null) {
                            cumulativeOverflowMillis[0] = cumulativeOverflowMillis[0] + overflowMillis;
                        }
                        try {
                            stream.close();
                            return allOps;
                        } catch (FileNotFoundException e) {
                            Slog.i(LOG_TAG, "No history file: " + file.getName());
                            return Collections.emptyList();
                        }
                    }
                    throw new IllegalStateException("Dropping unsupported history version 1 for file:" + file);
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (FileNotFoundException e2) {
            }
        }

        private AppOpsManager.HistoricalOps readeHistoricalOpsDLocked(TypedXmlPullParser parser, int filterUid, String filterPackageName, String filterAttributionTag, String[] filterOpNames, int filter, long filterBeginTimeMillis, long filterEndTimeMillis, int filterFlags, long[] cumulativeOverflowMillis) throws IOException, XmlPullParserException {
            TypedXmlPullParser typedXmlPullParser = parser;
            long beginTimeMillis = typedXmlPullParser.getAttributeLong((String) null, ATTR_BEGIN_TIME, 0L) + (cumulativeOverflowMillis != null ? cumulativeOverflowMillis[0] : 0L);
            long endTimeMillis = typedXmlPullParser.getAttributeLong((String) null, ATTR_END_TIME, 0L) + (cumulativeOverflowMillis != null ? cumulativeOverflowMillis[0] : 0L);
            if (filterEndTimeMillis < beginTimeMillis) {
                return null;
            }
            if (filterBeginTimeMillis > endTimeMillis) {
                return new AppOpsManager.HistoricalOps(0L, 0L);
            }
            long filteredBeginTimeMillis = Math.max(beginTimeMillis, filterBeginTimeMillis);
            long filteredEndTimeMillis = Math.min(endTimeMillis, filterEndTimeMillis);
            long filteredEndTimeMillis2 = filteredEndTimeMillis;
            double filterScale = (filteredEndTimeMillis - filteredBeginTimeMillis) / (endTimeMillis - beginTimeMillis);
            int depth = parser.getDepth();
            AppOpsManager.HistoricalOps ops = null;
            while (XmlUtils.nextElementWithin(typedXmlPullParser, depth)) {
                if ("uid".equals(parser.getName())) {
                    AppOpsManager.HistoricalOps ops2 = ops;
                    long filteredEndTimeMillis3 = filteredEndTimeMillis2;
                    int depth2 = depth;
                    long filteredBeginTimeMillis2 = filteredBeginTimeMillis;
                    long endTimeMillis2 = endTimeMillis;
                    long beginTimeMillis2 = beginTimeMillis;
                    AppOpsManager.HistoricalOps returnedOps = readHistoricalUidOpsDLocked(ops, parser, filterUid, filterPackageName, filterAttributionTag, filterOpNames, filter, filterFlags, filterScale);
                    if (ops2 != null) {
                        ops = ops2;
                    } else {
                        ops = returnedOps;
                    }
                    typedXmlPullParser = parser;
                    filteredBeginTimeMillis = filteredBeginTimeMillis2;
                    depth = depth2;
                    endTimeMillis = endTimeMillis2;
                    beginTimeMillis = beginTimeMillis2;
                    filteredEndTimeMillis2 = filteredEndTimeMillis3;
                } else {
                    typedXmlPullParser = parser;
                    filteredEndTimeMillis2 = filteredEndTimeMillis2;
                }
            }
            AppOpsManager.HistoricalOps ops3 = ops;
            long filteredBeginTimeMillis3 = filteredBeginTimeMillis;
            long filteredEndTimeMillis4 = filteredEndTimeMillis2;
            if (ops3 != null) {
                ops3.setBeginAndEndTime(filteredBeginTimeMillis3, filteredEndTimeMillis4);
            }
            return ops3;
        }

        private AppOpsManager.HistoricalOps readHistoricalUidOpsDLocked(AppOpsManager.HistoricalOps ops, TypedXmlPullParser parser, int filterUid, String filterPackageName, String filterAttributionTag, String[] filterOpNames, int filter, int filterFlags, double filterScale) throws IOException, XmlPullParserException {
            int uid = parser.getAttributeInt((String) null, ATTR_NAME);
            if ((filter & 1) != 0 && filterUid != uid) {
                XmlUtils.skipCurrentTag(parser);
                return null;
            }
            int depth = parser.getDepth();
            AppOpsManager.HistoricalOps ops2 = ops;
            while (XmlUtils.nextElementWithin(parser, depth)) {
                if (TAG_PACKAGE.equals(parser.getName())) {
                    AppOpsManager.HistoricalOps returnedOps = readHistoricalPackageOpsDLocked(ops2, uid, parser, filterPackageName, filterAttributionTag, filterOpNames, filter, filterFlags, filterScale);
                    if (ops2 == null) {
                        ops2 = returnedOps;
                    }
                }
            }
            return ops2;
        }

        private AppOpsManager.HistoricalOps readHistoricalPackageOpsDLocked(AppOpsManager.HistoricalOps ops, int uid, TypedXmlPullParser parser, String filterPackageName, String filterAttributionTag, String[] filterOpNames, int filter, int filterFlags, double filterScale) throws IOException, XmlPullParserException {
            String packageName = XmlUtils.readStringAttribute(parser, ATTR_NAME);
            if ((filter & 2) != 0 && !filterPackageName.equals(packageName)) {
                XmlUtils.skipCurrentTag(parser);
                return null;
            }
            int depth = parser.getDepth();
            AppOpsManager.HistoricalOps ops2 = ops;
            while (XmlUtils.nextElementWithin(parser, depth)) {
                if (TAG_ATTRIBUTION.equals(parser.getName())) {
                    AppOpsManager.HistoricalOps returnedOps = readHistoricalAttributionOpsDLocked(ops2, uid, packageName, parser, filterAttributionTag, filterOpNames, filter, filterFlags, filterScale);
                    if (ops2 == null) {
                        ops2 = returnedOps;
                    }
                }
            }
            return ops2;
        }

        private AppOpsManager.HistoricalOps readHistoricalAttributionOpsDLocked(AppOpsManager.HistoricalOps ops, int uid, String packageName, TypedXmlPullParser parser, String filterAttributionTag, String[] filterOpNames, int filter, int filterFlags, double filterScale) throws IOException, XmlPullParserException {
            String attributionTag = XmlUtils.readStringAttribute(parser, ATTR_NAME);
            if ((filter & 4) != 0 && !Objects.equals(filterAttributionTag, attributionTag)) {
                XmlUtils.skipCurrentTag(parser);
                return null;
            }
            int depth = parser.getDepth();
            AppOpsManager.HistoricalOps ops2 = ops;
            while (XmlUtils.nextElementWithin(parser, depth)) {
                if (TAG_OP.equals(parser.getName())) {
                    AppOpsManager.HistoricalOps returnedOps = readHistoricalOpDLocked(ops2, uid, packageName, attributionTag, parser, filterOpNames, filter, filterFlags, filterScale);
                    if (ops2 == null) {
                        ops2 = returnedOps;
                    }
                }
            }
            return ops2;
        }

        private AppOpsManager.HistoricalOps readHistoricalOpDLocked(AppOpsManager.HistoricalOps ops, int uid, String packageName, String attributionTag, TypedXmlPullParser parser, String[] filterOpNames, int filter, int filterFlags, double filterScale) throws IOException, XmlPullParserException {
            int op = parser.getAttributeInt((String) null, ATTR_NAME);
            if ((filter & 8) != 0 && !ArrayUtils.contains(filterOpNames, AppOpsManager.opToPublicName(op))) {
                XmlUtils.skipCurrentTag(parser);
                return null;
            }
            int depth = parser.getDepth();
            AppOpsManager.HistoricalOps ops2 = ops;
            while (XmlUtils.nextElementWithin(parser, depth)) {
                if (TAG_STATE.equals(parser.getName())) {
                    AppOpsManager.HistoricalOps returnedOps = readStateDLocked(ops2, uid, packageName, attributionTag, op, parser, filterFlags, filterScale);
                    if (ops2 == null) {
                        ops2 = returnedOps;
                    }
                }
            }
            return ops2;
        }

        private AppOpsManager.HistoricalOps readStateDLocked(AppOpsManager.HistoricalOps ops, int uid, String packageName, String attributionTag, int op, TypedXmlPullParser parser, int filterFlags, double filterScale) throws IOException, XmlPullParserException {
            long key;
            AppOpsManager.HistoricalOps ops2;
            long accessDuration;
            long rejectCount;
            long accessCount;
            long key2 = parser.getAttributeLong((String) null, ATTR_NAME);
            int flags = AppOpsManager.extractFlagsFromKey(key2) & filterFlags;
            if (flags == 0) {
                return null;
            }
            int uidState = AppOpsManager.extractUidStateFromKey(key2);
            long accessCount2 = parser.getAttributeLong((String) null, ATTR_ACCESS_COUNT, 0L);
            if (accessCount2 > 0) {
                if (Double.isNaN(filterScale)) {
                    accessCount = accessCount2;
                } else {
                    accessCount = (long) AppOpsManager.HistoricalOps.round(accessCount2 * filterScale);
                }
                if (ops != null) {
                    ops2 = ops;
                } else {
                    ops2 = new AppOpsManager.HistoricalOps(0L, 0L);
                }
                key = 0;
                ops2.increaseAccessCount(op, uid, packageName, attributionTag, uidState, flags, accessCount);
            } else {
                key = 0;
                ops2 = ops;
            }
            long rejectCount2 = parser.getAttributeLong((String) null, ATTR_REJECT_COUNT, key);
            if (rejectCount2 > key) {
                if (Double.isNaN(filterScale)) {
                    rejectCount = rejectCount2;
                } else {
                    rejectCount = (long) AppOpsManager.HistoricalOps.round(rejectCount2 * filterScale);
                }
                if (ops2 == null) {
                    ops2 = new AppOpsManager.HistoricalOps(key, key);
                }
                ops2.increaseRejectCount(op, uid, packageName, attributionTag, uidState, flags, rejectCount);
            }
            long accessDuration2 = parser.getAttributeLong((String) null, ATTR_ACCESS_DURATION, key);
            if (accessDuration2 > key) {
                if (Double.isNaN(filterScale)) {
                    accessDuration = accessDuration2;
                } else {
                    accessDuration = (long) AppOpsManager.HistoricalOps.round(accessDuration2 * filterScale);
                }
                if (ops2 == null) {
                    ops2 = new AppOpsManager.HistoricalOps(key, key);
                }
                ops2.increaseAccessDuration(op, uid, packageName, attributionTag, uidState, flags, accessDuration);
            }
            return ops2;
        }

        private void writeHistoricalOpsDLocked(List<AppOpsManager.HistoricalOps> allOps, long intervalOverflowMillis, File file) throws IOException {
            FileOutputStream output = sHistoricalAppOpsDir.openWrite(file);
            try {
                TypedXmlSerializer serializer = Xml.resolveSerializer(output);
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                serializer.startDocument((String) null, true);
                serializer.startTag((String) null, TAG_HISTORY);
                serializer.attributeInt((String) null, ATTR_VERSION, 2);
                if (intervalOverflowMillis != 0) {
                    serializer.attributeLong((String) null, ATTR_OVERFLOW, intervalOverflowMillis);
                }
                if (allOps != null) {
                    int opsCount = allOps.size();
                    for (int i = 0; i < opsCount; i++) {
                        AppOpsManager.HistoricalOps ops = allOps.get(i);
                        writeHistoricalOpDLocked(ops, serializer);
                    }
                }
                serializer.endTag((String) null, TAG_HISTORY);
                serializer.endDocument();
                sHistoricalAppOpsDir.closeWrite(output);
            } catch (IOException e) {
                sHistoricalAppOpsDir.failWrite(output);
                throw e;
            }
        }

        private void writeHistoricalOpDLocked(AppOpsManager.HistoricalOps ops, TypedXmlSerializer serializer) throws IOException {
            serializer.startTag((String) null, TAG_OPS);
            serializer.attributeLong((String) null, ATTR_BEGIN_TIME, ops.getBeginTimeMillis());
            serializer.attributeLong((String) null, ATTR_END_TIME, ops.getEndTimeMillis());
            int uidCount = ops.getUidCount();
            for (int i = 0; i < uidCount; i++) {
                AppOpsManager.HistoricalUidOps uidOp = ops.getUidOpsAt(i);
                writeHistoricalUidOpsDLocked(uidOp, serializer);
            }
            serializer.endTag((String) null, TAG_OPS);
        }

        private void writeHistoricalUidOpsDLocked(AppOpsManager.HistoricalUidOps uidOps, TypedXmlSerializer serializer) throws IOException {
            serializer.startTag((String) null, "uid");
            serializer.attributeInt((String) null, ATTR_NAME, uidOps.getUid());
            int packageCount = uidOps.getPackageCount();
            for (int i = 0; i < packageCount; i++) {
                AppOpsManager.HistoricalPackageOps packageOps = uidOps.getPackageOpsAt(i);
                writeHistoricalPackageOpsDLocked(packageOps, serializer);
            }
            serializer.endTag((String) null, "uid");
        }

        private void writeHistoricalPackageOpsDLocked(AppOpsManager.HistoricalPackageOps packageOps, TypedXmlSerializer serializer) throws IOException {
            serializer.startTag((String) null, TAG_PACKAGE);
            serializer.attributeInterned((String) null, ATTR_NAME, packageOps.getPackageName());
            int numAttributions = packageOps.getAttributedOpsCount();
            for (int i = 0; i < numAttributions; i++) {
                AppOpsManager.AttributedHistoricalOps op = packageOps.getAttributedOpsAt(i);
                writeHistoricalAttributionOpsDLocked(op, serializer);
            }
            serializer.endTag((String) null, TAG_PACKAGE);
        }

        private void writeHistoricalAttributionOpsDLocked(AppOpsManager.AttributedHistoricalOps attributionOps, TypedXmlSerializer serializer) throws IOException {
            serializer.startTag((String) null, TAG_ATTRIBUTION);
            XmlUtils.writeStringAttribute(serializer, ATTR_NAME, attributionOps.getTag());
            int opCount = attributionOps.getOpCount();
            for (int i = 0; i < opCount; i++) {
                AppOpsManager.HistoricalOp op = attributionOps.getOpAt(i);
                writeHistoricalOpDLocked(op, serializer);
            }
            serializer.endTag((String) null, TAG_ATTRIBUTION);
        }

        private void writeHistoricalOpDLocked(AppOpsManager.HistoricalOp op, TypedXmlSerializer serializer) throws IOException {
            LongSparseArray keys = op.collectKeys();
            if (keys == null || keys.size() <= 0) {
                return;
            }
            serializer.startTag((String) null, TAG_OP);
            serializer.attributeInt((String) null, ATTR_NAME, op.getOpCode());
            int keyCount = keys.size();
            for (int i = 0; i < keyCount; i++) {
                writeStateOnLocked(op, keys.keyAt(i), serializer);
            }
            serializer.endTag((String) null, TAG_OP);
        }

        private void writeStateOnLocked(AppOpsManager.HistoricalOp op, long key, TypedXmlSerializer serializer) throws IOException {
            int uidState = AppOpsManager.extractUidStateFromKey(key);
            int flags = AppOpsManager.extractFlagsFromKey(key);
            long accessCount = op.getAccessCount(uidState, uidState, flags);
            long rejectCount = op.getRejectCount(uidState, uidState, flags);
            long accessDuration = op.getAccessDuration(uidState, uidState, flags);
            if (accessCount <= 0 && rejectCount <= 0 && accessDuration <= 0) {
                return;
            }
            serializer.startTag((String) null, TAG_STATE);
            serializer.attributeLong((String) null, ATTR_NAME, key);
            if (accessCount > 0) {
                serializer.attributeLong((String) null, ATTR_ACCESS_COUNT, accessCount);
            }
            if (rejectCount > 0) {
                serializer.attributeLong((String) null, ATTR_REJECT_COUNT, rejectCount);
            }
            if (accessDuration > 0) {
                serializer.attributeLong((String) null, ATTR_ACCESS_DURATION, accessDuration);
            }
            serializer.endTag((String) null, TAG_STATE);
        }

        private static void enforceOpsWellFormed(List<AppOpsManager.HistoricalOps> ops) {
            if (ops == null) {
                return;
            }
            AppOpsManager.HistoricalOps current = null;
            int opsCount = ops.size();
            for (int i = 0; i < opsCount; i++) {
                AppOpsManager.HistoricalOps previous = current;
                AppOpsManager.HistoricalOps current2 = ops.get(i);
                current = current2;
                if (current.isEmpty()) {
                    throw new IllegalStateException("Empty ops:\n" + opsToDebugString(ops));
                }
                if (current.getEndTimeMillis() < current.getBeginTimeMillis()) {
                    throw new IllegalStateException("Begin after end:\n" + opsToDebugString(ops));
                }
                if (previous != null) {
                    if (previous.getEndTimeMillis() > current.getBeginTimeMillis()) {
                        throw new IllegalStateException("Intersecting ops:\n" + opsToDebugString(ops));
                    }
                    if (previous.getBeginTimeMillis() > current.getBeginTimeMillis()) {
                        throw new IllegalStateException("Non increasing ops:\n" + opsToDebugString(ops));
                    }
                }
            }
        }

        private long computeGlobalIntervalBeginMillis(int depth) {
            long beginTimeMillis = 0;
            for (int i = 0; i < depth + 1; i++) {
                beginTimeMillis = (long) (beginTimeMillis + Math.pow(this.mIntervalCompressionMultiplier, i));
            }
            return this.mBaseSnapshotInterval * beginTimeMillis;
        }

        private static AppOpsManager.HistoricalOps spliceFromEnd(AppOpsManager.HistoricalOps ops, double spliceRatio) {
            AppOpsManager.HistoricalOps splice = ops.spliceFromEnd(spliceRatio);
            return splice;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static AppOpsManager.HistoricalOps spliceFromBeginning(AppOpsManager.HistoricalOps ops, double spliceRatio) {
            AppOpsManager.HistoricalOps splice = ops.spliceFromBeginning(spliceRatio);
            return splice;
        }

        private static void normalizeSnapshotForSlotDuration(List<AppOpsManager.HistoricalOps> ops, long slotDurationMillis) {
            int opCount = ops.size();
            int processedIdx = opCount - 1;
            while (processedIdx >= 0) {
                AppOpsManager.HistoricalOps processedOp = ops.get(processedIdx);
                long slotBeginTimeMillis = Math.max(processedOp.getEndTimeMillis() - slotDurationMillis, 0L);
                for (int candidateIdx = processedIdx - 1; candidateIdx >= 0; candidateIdx--) {
                    AppOpsManager.HistoricalOps candidateOp = ops.get(candidateIdx);
                    long candidateSlotIntersectionMillis = candidateOp.getEndTimeMillis() - Math.min(slotBeginTimeMillis, processedOp.getBeginTimeMillis());
                    if (candidateSlotIntersectionMillis <= 0) {
                        break;
                    }
                    float candidateSplitRatio = ((float) candidateSlotIntersectionMillis) / ((float) candidateOp.getDurationMillis());
                    if (Float.compare(candidateSplitRatio, 1.0f) >= 0) {
                        ops.remove(candidateIdx);
                        processedIdx--;
                        processedOp.merge(candidateOp);
                    } else {
                        AppOpsManager.HistoricalOps endSplice = spliceFromEnd(candidateOp, candidateSplitRatio);
                        if (endSplice != null) {
                            processedOp.merge(endSplice);
                        }
                        if (candidateOp.isEmpty()) {
                            ops.remove(candidateIdx);
                            processedIdx--;
                        }
                    }
                }
                processedIdx--;
            }
        }

        private static String opsToDebugString(List<AppOpsManager.HistoricalOps> ops) {
            StringBuilder builder = new StringBuilder();
            int opCount = ops.size();
            for (int i = 0; i < opCount; i++) {
                builder.append("  ");
                builder.append(ops.get(i));
                if (i < opCount - 1) {
                    builder.append('\n');
                }
            }
            return builder.toString();
        }

        private static Set<String> getHistoricalFileNames(File historyDir) {
            File[] files = historyDir.listFiles();
            if (files == null) {
                return Collections.emptySet();
            }
            ArraySet<String> fileNames = new ArraySet<>(files.length);
            for (File file : files) {
                fileNames.add(file.getName());
            }
            return fileNames;
        }
    }

    /* loaded from: classes.dex */
    private static class HistoricalFilesInvariant {
        private final List<File> mBeginFiles = new ArrayList();

        private HistoricalFilesInvariant() {
        }

        public void startTracking(File folder) {
            File[] files = folder.listFiles();
            if (files != null) {
                Collections.addAll(this.mBeginFiles, files);
            }
        }

        public void stopTracking(File folder) {
            List<File> endFiles = new ArrayList<>();
            File[] files = folder.listFiles();
            if (files != null) {
                Collections.addAll(endFiles, files);
            }
            long beginOldestFileOffsetMillis = getOldestFileOffsetMillis(this.mBeginFiles);
            long endOldestFileOffsetMillis = getOldestFileOffsetMillis(endFiles);
            if (endOldestFileOffsetMillis < beginOldestFileOffsetMillis) {
                String message = "History loss detected!\nold files: " + this.mBeginFiles;
                HistoricalRegistry.wtf(message, null, folder);
                throw new IllegalStateException(message);
            }
        }

        private static long getOldestFileOffsetMillis(List<File> files) {
            if (files.isEmpty()) {
                return 0L;
            }
            String longestName = files.get(0).getName();
            int fileCount = files.size();
            for (int i = 1; i < fileCount; i++) {
                File file = files.get(i);
                if (file.getName().length() > longestName.length()) {
                    longestName = file.getName();
                }
            }
            return Long.parseLong(longestName.replace(HistoricalRegistry.HISTORY_FILE_SUFFIX, ""));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class StringDumpVisitor implements AppOpsManager.HistoricalOpsVisitor {
        private final String mAttributionPrefix;
        private final String mEntryPrefix;
        private final int mFilter;
        private final String mFilterAttributionTag;
        private final int mFilterOp;
        private final String mFilterPackage;
        private final int mFilterUid;
        private final String mOpsPrefix;
        private final String mPackagePrefix;
        private final String mUidPrefix;
        private final String mUidStatePrefix;
        private final PrintWriter mWriter;
        private final long mNow = System.currentTimeMillis();
        private final SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        private final Date mDate = new Date();

        StringDumpVisitor(String prefix, PrintWriter writer, int filterUid, String filterPackage, String filterAttributionTag, int filterOp, int filter) {
            String str = prefix + "  ";
            this.mOpsPrefix = str;
            String str2 = str + "  ";
            this.mUidPrefix = str2;
            String str3 = str2 + "  ";
            this.mPackagePrefix = str3;
            String str4 = str3 + "  ";
            this.mAttributionPrefix = str4;
            String str5 = str4 + "  ";
            this.mEntryPrefix = str5;
            this.mUidStatePrefix = str5 + "  ";
            this.mWriter = writer;
            this.mFilterUid = filterUid;
            this.mFilterPackage = filterPackage;
            this.mFilterAttributionTag = filterAttributionTag;
            this.mFilterOp = filterOp;
            this.mFilter = filter;
        }

        public void visitHistoricalOps(AppOpsManager.HistoricalOps ops) {
            this.mWriter.println();
            this.mWriter.print(this.mOpsPrefix);
            this.mWriter.println("snapshot:");
            this.mWriter.print(this.mUidPrefix);
            this.mWriter.print("begin = ");
            this.mDate.setTime(ops.getBeginTimeMillis());
            this.mWriter.print(this.mDateFormatter.format(this.mDate));
            this.mWriter.print("  (");
            TimeUtils.formatDuration(ops.getBeginTimeMillis() - this.mNow, this.mWriter);
            this.mWriter.println(")");
            this.mWriter.print(this.mUidPrefix);
            this.mWriter.print("end = ");
            this.mDate.setTime(ops.getEndTimeMillis());
            this.mWriter.print(this.mDateFormatter.format(this.mDate));
            this.mWriter.print("  (");
            TimeUtils.formatDuration(ops.getEndTimeMillis() - this.mNow, this.mWriter);
            this.mWriter.println(")");
        }

        public void visitHistoricalUidOps(AppOpsManager.HistoricalUidOps ops) {
            if ((this.mFilter & 1) != 0 && this.mFilterUid != ops.getUid()) {
                return;
            }
            this.mWriter.println();
            this.mWriter.print(this.mUidPrefix);
            this.mWriter.print("Uid ");
            UserHandle.formatUid(this.mWriter, ops.getUid());
            this.mWriter.println(":");
        }

        public void visitHistoricalPackageOps(AppOpsManager.HistoricalPackageOps ops) {
            if ((this.mFilter & 2) != 0 && !this.mFilterPackage.equals(ops.getPackageName())) {
                return;
            }
            this.mWriter.print(this.mPackagePrefix);
            this.mWriter.print("Package ");
            this.mWriter.print(ops.getPackageName());
            this.mWriter.println(":");
        }

        public void visitHistoricalAttributionOps(AppOpsManager.AttributedHistoricalOps ops) {
            if ((this.mFilter & 4) != 0 && !Objects.equals(this.mFilterPackage, ops.getTag())) {
                return;
            }
            this.mWriter.print(this.mAttributionPrefix);
            this.mWriter.print("Attribution ");
            this.mWriter.print(ops.getTag());
            this.mWriter.println(":");
        }

        public void visitHistoricalOp(AppOpsManager.HistoricalOp ops) {
            int keyCount;
            if ((this.mFilter & 8) == 0 || this.mFilterOp == ops.getOpCode()) {
                this.mWriter.print(this.mEntryPrefix);
                this.mWriter.print(AppOpsManager.opToName(ops.getOpCode()));
                this.mWriter.println(":");
                LongSparseArray keys = ops.collectKeys();
                int keyCount2 = keys.size();
                int i = 0;
                while (i < keyCount2) {
                    long key = keys.keyAt(i);
                    int uidState = AppOpsManager.extractUidStateFromKey(key);
                    int flags = AppOpsManager.extractFlagsFromKey(key);
                    boolean printedUidState = false;
                    long accessCount = ops.getAccessCount(uidState, uidState, flags);
                    if (accessCount > 0) {
                        if (0 == 0) {
                            this.mWriter.print(this.mUidStatePrefix);
                            this.mWriter.print(AppOpsManager.keyToString(key));
                            this.mWriter.print(" = ");
                            printedUidState = true;
                        }
                        this.mWriter.print("access=");
                        this.mWriter.print(accessCount);
                    }
                    long rejectCount = ops.getRejectCount(uidState, uidState, flags);
                    LongSparseArray keys2 = keys;
                    if (rejectCount <= 0) {
                        keyCount = keyCount2;
                    } else {
                        if (!printedUidState) {
                            keyCount = keyCount2;
                            this.mWriter.print(this.mUidStatePrefix);
                            this.mWriter.print(AppOpsManager.keyToString(key));
                            this.mWriter.print(" = ");
                            printedUidState = true;
                        } else {
                            keyCount = keyCount2;
                            this.mWriter.print(", ");
                        }
                        this.mWriter.print("reject=");
                        this.mWriter.print(rejectCount);
                    }
                    long accessDuration = ops.getAccessDuration(uidState, uidState, flags);
                    if (accessDuration > 0) {
                        if (!printedUidState) {
                            this.mWriter.print(this.mUidStatePrefix);
                            this.mWriter.print(AppOpsManager.keyToString(key));
                            this.mWriter.print(" = ");
                            printedUidState = true;
                        } else {
                            this.mWriter.print(", ");
                        }
                        this.mWriter.print("duration=");
                        TimeUtils.formatDuration(accessDuration, this.mWriter);
                    }
                    if (printedUidState) {
                        this.mWriter.println("");
                    }
                    i++;
                    keys = keys2;
                    keyCount2 = keyCount;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void wtf(String message, Throwable t, File storage) {
        Slog.wtf(LOG_TAG, message, t);
        if (KEEP_WTF_LOG) {
            try {
                File file = new File(new File(Environment.getDataSystemDirectory(), "appops"), "wtf" + TimeUtils.formatForLogging(System.currentTimeMillis()));
                if (file.createNewFile()) {
                    PrintWriter writer = new PrintWriter(file);
                    if (t != null) {
                        writer.append('\n').append((CharSequence) t.toString());
                    }
                    writer.append('\n').append((CharSequence) Debug.getCallers(10));
                    if (storage != null) {
                        writer.append((CharSequence) ("\nfiles: " + Arrays.toString(storage.listFiles())));
                    } else {
                        writer.append((CharSequence) "\nfiles: none");
                    }
                    writer.close();
                }
            } catch (IOException e) {
            }
        }
    }
}
