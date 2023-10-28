package com.android.server.appop;

import android.app.AppOpsManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.appop.DiscreteRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DiscreteRegistry {
    private static final String ATTR_ATTRIBUTION_FLAGS = "af";
    private static final String ATTR_CHAIN_ID = "ci";
    private static final String ATTR_FLAGS = "f";
    private static final String ATTR_LARGEST_CHAIN_ID = "lc";
    private static final String ATTR_NOTE_DURATION = "nd";
    private static final String ATTR_NOTE_TIME = "nt";
    private static final String ATTR_OP_ID = "op";
    private static final String ATTR_PACKAGE_NAME = "pn";
    private static final String ATTR_TAG = "at";
    private static final String ATTR_UID = "ui";
    private static final String ATTR_UID_STATE = "us";
    private static final String ATTR_VERSION = "v";
    private static final int CURRENT_VERSION = 1;
    private static final String DEFAULT_DISCRETE_OPS = "1,0,26,27,100,101,120";
    static final String DISCRETE_HISTORY_FILE_SUFFIX = "tl";
    private static final int OP_FLAGS_DISCRETE = 11;
    private static final String PROPERTY_DISCRETE_FLAGS = "discrete_history_op_flags";
    private static final String PROPERTY_DISCRETE_HISTORY_CUTOFF = "discrete_history_cutoff_millis";
    private static final String PROPERTY_DISCRETE_HISTORY_QUANTIZATION = "discrete_history_quantization_millis";
    private static final String PROPERTY_DISCRETE_OPS_LIST = "discrete_history_ops_cslist";
    private static final String TAG_ENTRY = "e";
    private static final String TAG_HISTORY = "h";
    private static final String TAG_OP = "o";
    private static final String TAG_PACKAGE = "p";
    private static final String TAG_TAG = "a";
    private static final String TAG_UID = "u";
    private static int sDiscreteFlags;
    private static long sDiscreteHistoryCutoff;
    private static long sDiscreteHistoryQuantization;
    private static int[] sDiscreteOps;
    private DiscreteOps mCachedOps;
    private boolean mDebugMode;
    private File mDiscreteAccessDir;
    private DiscreteOps mDiscreteOps;
    private final Object mInMemoryLock;
    private final Object mOnDiskLock;
    private static final String TAG = DiscreteRegistry.class.getSimpleName();
    private static final long DEFAULT_DISCRETE_HISTORY_CUTOFF = Duration.ofDays(7).toMillis();
    private static final long MAXIMUM_DISCRETE_HISTORY_CUTOFF = Duration.ofDays(30).toMillis();
    private static final long DEFAULT_DISCRETE_HISTORY_QUANTIZATION = Duration.ofMinutes(1).toMillis();

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.server.appop.DiscreteRegistry.DiscreteOps.readFromFile(java.io.File, long):void] */
    /* renamed from: -$$Nest$sfgetTAG  reason: not valid java name */
    static /* bridge */ /* synthetic */ String m1659$$Nest$sfgetTAG() {
        return TAG;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DiscreteRegistry(Object inMemoryLock) {
        Object obj = new Object();
        this.mOnDiskLock = obj;
        this.mCachedOps = null;
        this.mDebugMode = false;
        this.mInMemoryLock = inMemoryLock;
        synchronized (obj) {
            this.mDiscreteAccessDir = new File(new File(Environment.getDataSystemDirectory(), "appops"), "discrete");
            createDiscreteAccessDirLocked();
            int largestChainId = readLargestChainIdFromDiskLocked();
            synchronized (inMemoryLock) {
                this.mDiscreteOps = new DiscreteOps(largestChainId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemReady() {
        DeviceConfig.addOnPropertiesChangedListener("privacy", AsyncTask.THREAD_POOL_EXECUTOR, new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.appop.DiscreteRegistry$$ExternalSyntheticLambda0
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                DiscreteRegistry.this.m1664lambda$systemReady$0$comandroidserverappopDiscreteRegistry(properties);
            }
        });
        m1664lambda$systemReady$0$comandroidserverappopDiscreteRegistry(DeviceConfig.getProperties("privacy", new String[0]));
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: setDiscreteHistoryParameters */
    public void m1664lambda$systemReady$0$comandroidserverappopDiscreteRegistry(DeviceConfig.Properties p) {
        int[] parseOpsList;
        if (p.getKeyset().contains(PROPERTY_DISCRETE_HISTORY_CUTOFF)) {
            sDiscreteHistoryCutoff = p.getLong(PROPERTY_DISCRETE_HISTORY_CUTOFF, DEFAULT_DISCRETE_HISTORY_CUTOFF);
            if (!Build.IS_DEBUGGABLE && !this.mDebugMode) {
                sDiscreteHistoryCutoff = Long.min(MAXIMUM_DISCRETE_HISTORY_CUTOFF, sDiscreteHistoryCutoff);
            }
        } else {
            sDiscreteHistoryCutoff = DEFAULT_DISCRETE_HISTORY_CUTOFF;
        }
        if (p.getKeyset().contains(PROPERTY_DISCRETE_HISTORY_QUANTIZATION)) {
            long j = DEFAULT_DISCRETE_HISTORY_QUANTIZATION;
            sDiscreteHistoryQuantization = p.getLong(PROPERTY_DISCRETE_HISTORY_QUANTIZATION, j);
            if (!Build.IS_DEBUGGABLE && !this.mDebugMode) {
                sDiscreteHistoryQuantization = Math.max(j, sDiscreteHistoryQuantization);
            }
        } else {
            sDiscreteHistoryQuantization = DEFAULT_DISCRETE_HISTORY_QUANTIZATION;
        }
        int i = 11;
        if (p.getKeyset().contains(PROPERTY_DISCRETE_FLAGS)) {
            i = p.getInt(PROPERTY_DISCRETE_FLAGS, 11);
            sDiscreteFlags = i;
        }
        sDiscreteFlags = i;
        if (p.getKeyset().contains(PROPERTY_DISCRETE_OPS_LIST)) {
            parseOpsList = parseOpsList(p.getString(PROPERTY_DISCRETE_OPS_LIST, DEFAULT_DISCRETE_OPS));
        } else {
            parseOpsList = parseOpsList(DEFAULT_DISCRETE_OPS);
        }
        sDiscreteOps = parseOpsList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void recordDiscreteAccess(int uid, String packageName, int op, String attributionTag, int flags, int uidState, long accessTime, long accessDuration, int attributionFlags, int attributionChainId) {
        if (!isDiscreteOp(op, flags)) {
            return;
        }
        synchronized (this.mInMemoryLock) {
            try {
                try {
                    this.mDiscreteOps.addDiscreteAccess(op, uid, packageName, attributionTag, flags, uidState, accessTime, accessDuration, attributionFlags, attributionChainId);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeAndClearAccessHistory() {
        DiscreteOps discreteOps;
        synchronized (this.mOnDiskLock) {
            if (this.mDiscreteAccessDir == null) {
                Slog.d(TAG, "State not saved - persistence not initialized.");
                return;
            }
            synchronized (this.mInMemoryLock) {
                discreteOps = this.mDiscreteOps;
                this.mDiscreteOps = new DiscreteOps(discreteOps.mChainIdOffset);
                this.mCachedOps = null;
            }
            deleteOldDiscreteHistoryFilesLocked();
            if (!discreteOps.isEmpty()) {
                persistDiscreteOpsLocked(discreteOps);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addFilteredDiscreteOpsToHistoricalOps(AppOpsManager.HistoricalOps result, long beginTimeMillis, long endTimeMillis, int filter, int uidFilter, String packageNameFilter, String[] opNamesFilter, String attributionTagFilter, int flagsFilter, Set<String> attributionExemptPkgs) {
        ArrayMap<Integer, AttributionChain> attributionChains;
        boolean assembleChains = attributionExemptPkgs != null;
        DiscreteOps discreteOps = getAllDiscreteOps();
        ArrayMap<Integer, AttributionChain> attributionChains2 = new ArrayMap<>();
        if (assembleChains) {
            ArrayMap<Integer, AttributionChain> attributionChains3 = createAttributionChains(discreteOps, attributionExemptPkgs);
            attributionChains = attributionChains3;
        } else {
            attributionChains = attributionChains2;
        }
        long beginTimeMillis2 = Math.max(beginTimeMillis, Instant.now().minus(sDiscreteHistoryCutoff, (TemporalUnit) ChronoUnit.MILLIS).toEpochMilli());
        ArrayMap<Integer, AttributionChain> attributionChains4 = attributionChains;
        discreteOps.filter(beginTimeMillis2, endTimeMillis, filter, uidFilter, packageNameFilter, opNamesFilter, attributionTagFilter, flagsFilter, attributionChains4);
        discreteOps.applyToHistoricalOps(result, attributionChains4);
    }

    private int readLargestChainIdFromDiskLocked() {
        File[] files = this.mDiscreteAccessDir.listFiles();
        if (files == null || files.length <= 0) {
            return 0;
        }
        File latestFile = null;
        long latestFileTimestamp = 0;
        for (File f : files) {
            String fileName = f.getName();
            if (fileName.endsWith(DISCRETE_HISTORY_FILE_SUFFIX)) {
                long timestamp = Long.valueOf(fileName.substring(0, fileName.length() - DISCRETE_HISTORY_FILE_SUFFIX.length())).longValue();
                if (latestFileTimestamp < timestamp) {
                    latestFile = f;
                    latestFileTimestamp = timestamp;
                }
            }
        }
        if (latestFile == null) {
            return 0;
        }
        try {
            FileInputStream stream = new FileInputStream(latestFile);
            try {
                TypedXmlPullParser parser = Xml.resolvePullParser(stream);
                XmlUtils.beginDocument(parser, TAG_HISTORY);
                int largestChainId = parser.getAttributeInt((String) null, ATTR_LARGEST_CHAIN_ID, 0);
                try {
                    stream.close();
                } catch (IOException e) {
                }
                return largestChainId;
            } catch (Throwable th) {
                try {
                    stream.close();
                } catch (IOException e2) {
                }
                return 0;
            }
        } catch (FileNotFoundException e3) {
            return 0;
        }
    }

    private ArrayMap<Integer, AttributionChain> createAttributionChains(DiscreteOps discreteOps, Set<String> attributionExemptPkgs) {
        ArrayMap<String, DiscretePackageOps> pkgs;
        List<DiscreteOpEvent> opEvents;
        int attrOpNum;
        int nAttrOps;
        ArrayMap<String, List<DiscreteOpEvent>> attrOps;
        int opNum;
        int nOps;
        int nPackages;
        DiscreteOps discreteOps2 = discreteOps;
        ArrayMap<Integer, AttributionChain> chains = new ArrayMap<>();
        int nUids = discreteOps2.mUids.size();
        int uidNum = 0;
        while (uidNum < nUids) {
            ArrayMap<String, DiscretePackageOps> pkgs2 = discreteOps2.mUids.valueAt(uidNum).mPackages;
            int uid = discreteOps2.mUids.keyAt(uidNum).intValue();
            int nPackages2 = pkgs2.size();
            int pkgNum = 0;
            while (pkgNum < nPackages2) {
                ArrayMap<Integer, DiscreteOp> ops = pkgs2.valueAt(pkgNum).mPackageOps;
                String pkg = pkgs2.keyAt(pkgNum);
                int nOps2 = ops.size();
                int opNum2 = 0;
                while (opNum2 < nOps2) {
                    ArrayMap<String, List<DiscreteOpEvent>> attrOps2 = ops.valueAt(opNum2).mAttributedOps;
                    int op = ops.keyAt(opNum2).intValue();
                    int nAttrOps2 = attrOps2.size();
                    int attrOpNum2 = 0;
                    while (attrOpNum2 < nAttrOps2) {
                        List<DiscreteOpEvent> opEvents2 = attrOps2.valueAt(attrOpNum2);
                        String attributionTag = attrOps2.keyAt(attrOpNum2);
                        int nOpEvents = opEvents2.size();
                        int nUids2 = nUids;
                        int nUids3 = 0;
                        while (nUids3 < nOpEvents) {
                            int nOpEvents2 = nOpEvents;
                            DiscreteOpEvent event = opEvents2.get(nUids3);
                            if (event != null) {
                                pkgs = pkgs2;
                                List<DiscreteOpEvent> opEvents3 = opEvents2;
                                if (event.mAttributionChainId == -1) {
                                    nAttrOps = nAttrOps2;
                                    attrOps = attrOps2;
                                    opNum = opNum2;
                                    nOps = nOps2;
                                    nPackages = nPackages2;
                                    opEvents = opEvents3;
                                    attrOpNum = attrOpNum2;
                                } else if ((event.mAttributionFlags & 8) == 0) {
                                    nAttrOps = nAttrOps2;
                                    attrOps = attrOps2;
                                    opNum = opNum2;
                                    nOps = nOps2;
                                    nPackages = nPackages2;
                                    opEvents = opEvents3;
                                    attrOpNum = attrOpNum2;
                                } else {
                                    if (chains.containsKey(Integer.valueOf(event.mAttributionChainId))) {
                                        nPackages = nPackages2;
                                    } else {
                                        nPackages = nPackages2;
                                        chains.put(Integer.valueOf(event.mAttributionChainId), new AttributionChain(attributionExemptPkgs));
                                    }
                                    opEvents = opEvents3;
                                    attrOpNum = attrOpNum2;
                                    nAttrOps = nAttrOps2;
                                    attrOps = attrOps2;
                                    opNum = opNum2;
                                    nOps = nOps2;
                                    chains.get(Integer.valueOf(event.mAttributionChainId)).addEvent(pkg, uid, attributionTag, op, event);
                                }
                            } else {
                                pkgs = pkgs2;
                                opEvents = opEvents2;
                                attrOpNum = attrOpNum2;
                                nAttrOps = nAttrOps2;
                                attrOps = attrOps2;
                                opNum = opNum2;
                                nOps = nOps2;
                                nPackages = nPackages2;
                            }
                            nUids3++;
                            opEvents2 = opEvents;
                            pkgs2 = pkgs;
                            nOpEvents = nOpEvents2;
                            attrOpNum2 = attrOpNum;
                            nPackages2 = nPackages;
                            nAttrOps2 = nAttrOps;
                            attrOps2 = attrOps;
                            opNum2 = opNum;
                            nOps2 = nOps;
                        }
                        attrOpNum2++;
                        pkgs2 = pkgs2;
                        nUids = nUids2;
                        nPackages2 = nPackages2;
                    }
                    opNum2++;
                    nPackages2 = nPackages2;
                }
                pkgNum++;
                nPackages2 = nPackages2;
            }
            uidNum++;
            discreteOps2 = discreteOps;
        }
        return chains;
    }

    private void readDiscreteOpsFromDisk(DiscreteOps discreteOps) {
        synchronized (this.mOnDiskLock) {
            long beginTimeMillis = Instant.now().minus(sDiscreteHistoryCutoff, (TemporalUnit) ChronoUnit.MILLIS).toEpochMilli();
            File[] files = this.mDiscreteAccessDir.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    String fileName = f.getName();
                    if (fileName.endsWith(DISCRETE_HISTORY_FILE_SUFFIX)) {
                        long timestamp = Long.valueOf(fileName.substring(0, fileName.length() - DISCRETE_HISTORY_FILE_SUFFIX.length())).longValue();
                        if (timestamp >= beginTimeMillis) {
                            discreteOps.readFromFile(f, beginTimeMillis);
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearHistory() {
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                this.mDiscreteOps = new DiscreteOps(0);
            }
            clearOnDiskHistoryLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearHistory(int uid, String packageName) {
        DiscreteOps discreteOps;
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                discreteOps = getAllDiscreteOps();
                clearHistory();
            }
            discreteOps.clearHistory(uid, packageName);
            persistDiscreteOpsLocked(discreteOps);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void offsetHistory(long offset) {
        DiscreteOps discreteOps;
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                discreteOps = getAllDiscreteOps();
                clearHistory();
            }
            discreteOps.offsetHistory(offset);
            persistDiscreteOpsLocked(discreteOps);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, int uidFilter, String packageNameFilter, String attributionTagFilter, int filter, int dumpOp, SimpleDateFormat sdf, Date date, String prefix, int nDiscreteOps) {
        DiscreteOps discreteOps = getAllDiscreteOps();
        String[] opNamesFilter = dumpOp == -1 ? null : new String[]{AppOpsManager.opToPublicName(dumpOp)};
        discreteOps.filter(0L, Instant.now().toEpochMilli(), filter, uidFilter, packageNameFilter, opNamesFilter, attributionTagFilter, 31, new ArrayMap());
        pw.print(prefix);
        pw.print("Largest chain id: ");
        pw.print(this.mDiscreteOps.mLargestChainId);
        pw.println();
        discreteOps.dump(pw, sdf, date, prefix, nDiscreteOps);
    }

    private void clearOnDiskHistoryLocked() {
        this.mCachedOps = null;
        FileUtils.deleteContentsAndDir(this.mDiscreteAccessDir);
        createDiscreteAccessDir();
    }

    private DiscreteOps getAllDiscreteOps() {
        DiscreteOps discreteOps = new DiscreteOps(0);
        synchronized (this.mOnDiskLock) {
            synchronized (this.mInMemoryLock) {
                discreteOps.merge(this.mDiscreteOps);
            }
            if (this.mCachedOps == null) {
                DiscreteOps discreteOps2 = new DiscreteOps(0);
                this.mCachedOps = discreteOps2;
                readDiscreteOpsFromDisk(discreteOps2);
            }
            discreteOps.merge(this.mCachedOps);
        }
        return discreteOps;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AttributionChain {
        Set<String> mExemptPkgs;
        ArrayList<OpEvent> mChain = new ArrayList<>();
        OpEvent mStartEvent = null;
        OpEvent mLastVisibleEvent = null;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static final class OpEvent {
            String mAttributionTag;
            int mOpCode;
            DiscreteOpEvent mOpEvent;
            String mPkgName;
            int mUid;

            OpEvent(String pkgName, int uid, String attributionTag, int opCode, DiscreteOpEvent event) {
                this.mPkgName = pkgName;
                this.mUid = uid;
                this.mAttributionTag = attributionTag;
                this.mOpCode = opCode;
                this.mOpEvent = event;
            }

            public boolean matches(String pkgName, int uid, String attributionTag, int opCode, DiscreteOpEvent event) {
                return Objects.equals(pkgName, this.mPkgName) && this.mUid == uid && Objects.equals(attributionTag, this.mAttributionTag) && this.mOpCode == opCode && this.mOpEvent.mAttributionChainId == event.mAttributionChainId && this.mOpEvent.mAttributionFlags == event.mAttributionFlags && this.mOpEvent.mNoteTime == event.mNoteTime;
            }

            public boolean packageOpEquals(OpEvent other) {
                return Objects.equals(other.mPkgName, this.mPkgName) && other.mUid == this.mUid && Objects.equals(other.mAttributionTag, this.mAttributionTag) && this.mOpCode == other.mOpCode;
            }

            public boolean equalsExceptDuration(OpEvent other) {
                return other.mOpEvent.mNoteDuration != this.mOpEvent.mNoteDuration && packageOpEquals(other) && this.mOpEvent.equalsExceptDuration(other.mOpEvent);
            }
        }

        AttributionChain(Set<String> exemptPkgs) {
            this.mExemptPkgs = exemptPkgs;
        }

        boolean isComplete() {
            if (!this.mChain.isEmpty() && getStart() != null) {
                ArrayList<OpEvent> arrayList = this.mChain;
                if (isEnd(arrayList.get(arrayList.size() - 1))) {
                    return true;
                }
            }
            return false;
        }

        boolean isStart(String pkgName, int uid, String attributionTag, int op, DiscreteOpEvent opEvent) {
            OpEvent opEvent2 = this.mStartEvent;
            if (opEvent2 == null || opEvent == null) {
                return false;
            }
            return opEvent2.matches(pkgName, uid, attributionTag, op, opEvent);
        }

        private OpEvent getStart() {
            if (this.mChain.isEmpty() || !isStart(this.mChain.get(0))) {
                return null;
            }
            return this.mChain.get(0);
        }

        private OpEvent getLastVisible() {
            for (int i = this.mChain.size() - 1; i > 0; i--) {
                OpEvent event = this.mChain.get(i);
                if (!this.mExemptPkgs.contains(event.mPkgName)) {
                    return event;
                }
            }
            return null;
        }

        /* JADX WARN: Code restructure failed: missing block: B:31:0x0083, code lost:
            r7.mChain.add(r1, r6);
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        void addEvent(String pkgName, int uid, String attributionTag, int op, DiscreteOpEvent opEvent) {
            OpEvent event = new OpEvent(pkgName, uid, attributionTag, op, opEvent);
            for (int i = 0; i < this.mChain.size(); i++) {
                OpEvent item = this.mChain.get(i);
                if (item.equalsExceptDuration(event)) {
                    if (event.mOpEvent.mNoteDuration != -1) {
                        item.mOpEvent = event.mOpEvent;
                        return;
                    } else {
                        return;
                    }
                }
            }
            if (this.mChain.isEmpty() || isEnd(event)) {
                this.mChain.add(event);
            } else if (isStart(event)) {
                this.mChain.add(0, event);
            } else {
                int i2 = 0;
                while (true) {
                    if (i2 >= this.mChain.size()) {
                        break;
                    }
                    OpEvent currEvent = this.mChain.get(i2);
                    if ((isStart(currEvent) || currEvent.mOpEvent.mNoteTime <= event.mOpEvent.mNoteTime) && (i2 != this.mChain.size() - 1 || !isEnd(currEvent))) {
                        if (i2 != this.mChain.size() - 1) {
                            i2++;
                        } else {
                            this.mChain.add(event);
                            break;
                        }
                    }
                }
            }
            this.mStartEvent = isComplete() ? getStart() : null;
            this.mLastVisibleEvent = isComplete() ? getLastVisible() : null;
        }

        private boolean isEnd(OpEvent event) {
            return (event == null || (event.mOpEvent.mAttributionFlags & 1) == 0) ? false : true;
        }

        private boolean isStart(OpEvent event) {
            return (event == null || (event.mOpEvent.mAttributionFlags & 4) == 0) ? false : true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DiscreteOps {
        int mChainIdOffset;
        int mLargestChainId;
        ArrayMap<Integer, DiscreteUidOps> mUids = new ArrayMap<>();

        DiscreteOps(int chainIdOffset) {
            this.mChainIdOffset = chainIdOffset;
            this.mLargestChainId = chainIdOffset;
        }

        boolean isEmpty() {
            return this.mUids.isEmpty();
        }

        void merge(DiscreteOps other) {
            this.mLargestChainId = Math.max(this.mLargestChainId, other.mLargestChainId);
            int nUids = other.mUids.size();
            for (int i = 0; i < nUids; i++) {
                int uid = other.mUids.keyAt(i).intValue();
                DiscreteUidOps uidOps = other.mUids.valueAt(i);
                getOrCreateDiscreteUidOps(uid).merge(uidOps);
            }
        }

        void addDiscreteAccess(int op, int uid, String packageName, String attributionTag, int flags, int uidState, long accessTime, long accessDuration, int attributionFlags, int attributionChainId) {
            int offsetChainId = attributionChainId;
            if (attributionChainId != -1) {
                offsetChainId = attributionChainId + this.mChainIdOffset;
                if (offsetChainId > this.mLargestChainId) {
                    this.mLargestChainId = offsetChainId;
                } else if (offsetChainId < 0) {
                    offsetChainId = 0;
                    this.mLargestChainId = 0;
                    this.mChainIdOffset = attributionChainId * (-1);
                }
            }
            getOrCreateDiscreteUidOps(uid).addDiscreteAccess(op, packageName, attributionTag, flags, uidState, accessTime, accessDuration, attributionFlags, offsetChainId);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void filter(long beginTimeMillis, long endTimeMillis, int filter, int uidFilter, String packageNameFilter, String[] opNamesFilter, String attributionTagFilter, int flagsFilter, ArrayMap<Integer, AttributionChain> attributionChains) {
            if ((filter & 1) != 0) {
                ArrayMap<Integer, DiscreteUidOps> uids = new ArrayMap<>();
                uids.put(Integer.valueOf(uidFilter), getOrCreateDiscreteUidOps(uidFilter));
                this.mUids = uids;
            }
            int nUids = this.mUids.size();
            for (int i = nUids - 1; i >= 0; i--) {
                this.mUids.valueAt(i).filter(beginTimeMillis, endTimeMillis, filter, packageNameFilter, opNamesFilter, attributionTagFilter, flagsFilter, this.mUids.keyAt(i).intValue(), attributionChains);
                if (this.mUids.valueAt(i).isEmpty()) {
                    this.mUids.removeAt(i);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void offsetHistory(long offset) {
            int nUids = this.mUids.size();
            for (int i = 0; i < nUids; i++) {
                this.mUids.valueAt(i).offsetHistory(offset);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void clearHistory(int uid, String packageName) {
            if (this.mUids.containsKey(Integer.valueOf(uid))) {
                this.mUids.get(Integer.valueOf(uid)).clearPackage(packageName);
                if (this.mUids.get(Integer.valueOf(uid)).isEmpty()) {
                    this.mUids.remove(Integer.valueOf(uid));
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void applyToHistoricalOps(AppOpsManager.HistoricalOps result, ArrayMap<Integer, AttributionChain> attributionChains) {
            int nUids = this.mUids.size();
            for (int i = 0; i < nUids; i++) {
                this.mUids.valueAt(i).applyToHistory(result, this.mUids.keyAt(i).intValue(), attributionChains);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void writeToStream(FileOutputStream stream) throws Exception {
            TypedXmlSerializer out = Xml.resolveSerializer(stream);
            out.startDocument((String) null, true);
            out.startTag((String) null, DiscreteRegistry.TAG_HISTORY);
            out.attributeInt((String) null, DiscreteRegistry.ATTR_VERSION, 1);
            out.attributeInt((String) null, DiscreteRegistry.ATTR_LARGEST_CHAIN_ID, this.mLargestChainId);
            int nUids = this.mUids.size();
            for (int i = 0; i < nUids; i++) {
                out.startTag((String) null, DiscreteRegistry.TAG_UID);
                out.attributeInt((String) null, DiscreteRegistry.ATTR_UID, this.mUids.keyAt(i).intValue());
                this.mUids.valueAt(i).serialize(out);
                out.endTag((String) null, DiscreteRegistry.TAG_UID);
            }
            out.endTag((String) null, DiscreteRegistry.TAG_HISTORY);
            out.endDocument();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw, SimpleDateFormat sdf, Date date, String prefix, int nDiscreteOps) {
            int nUids = this.mUids.size();
            for (int i = 0; i < nUids; i++) {
                pw.print(prefix);
                pw.print("Uid: ");
                pw.print(this.mUids.keyAt(i));
                pw.println();
                this.mUids.valueAt(i).dump(pw, sdf, date, prefix + "  ", nDiscreteOps);
            }
        }

        private DiscreteUidOps getOrCreateDiscreteUidOps(int uid) {
            DiscreteUidOps result = this.mUids.get(Integer.valueOf(uid));
            if (result == null) {
                DiscreteUidOps result2 = new DiscreteUidOps();
                this.mUids.put(Integer.valueOf(uid), result2);
                return result2;
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void readFromFile(File f, long beginTimeMillis) {
            try {
                FileInputStream stream = new FileInputStream(f);
                try {
                    TypedXmlPullParser parser = Xml.resolvePullParser(stream);
                    XmlUtils.beginDocument(parser, DiscreteRegistry.TAG_HISTORY);
                    int version = parser.getAttributeInt((String) null, DiscreteRegistry.ATTR_VERSION);
                    if (version != 1) {
                        throw new IllegalStateException("Dropping unsupported discrete history " + f);
                    }
                    int depth = parser.getDepth();
                    while (XmlUtils.nextElementWithin(parser, depth)) {
                        if (DiscreteRegistry.TAG_UID.equals(parser.getName())) {
                            int uid = parser.getAttributeInt((String) null, DiscreteRegistry.ATTR_UID, -1);
                            getOrCreateDiscreteUidOps(uid).deserialize(parser, beginTimeMillis);
                        }
                    }
                    stream.close();
                } catch (IOException e) {
                }
            } catch (FileNotFoundException e2) {
            }
        }
    }

    private void createDiscreteAccessDir() {
        if (!this.mDiscreteAccessDir.exists()) {
            if (!this.mDiscreteAccessDir.mkdirs()) {
                Slog.e(TAG, "Failed to create DiscreteRegistry directory");
            }
            FileUtils.setPermissions(this.mDiscreteAccessDir.getPath(), 505, -1, -1);
        }
    }

    private void persistDiscreteOpsLocked(DiscreteOps discreteOps) {
        long currentTimeStamp = Instant.now().toEpochMilli();
        AtomicFile file = new AtomicFile(new File(this.mDiscreteAccessDir, currentTimeStamp + DISCRETE_HISTORY_FILE_SUFFIX));
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            discreteOps.writeToStream(stream);
            file.finishWrite(stream);
        } catch (Throwable t) {
            Slog.e(TAG, "Error writing timeline state: " + t.getMessage() + " " + Arrays.toString(t.getStackTrace()));
            if (stream != null) {
                file.failWrite(stream);
            }
        }
    }

    private void deleteOldDiscreteHistoryFilesLocked() {
        File[] files = this.mDiscreteAccessDir.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                String fileName = f.getName();
                if (fileName.endsWith(DISCRETE_HISTORY_FILE_SUFFIX)) {
                    try {
                        long timestamp = Long.valueOf(fileName.substring(0, fileName.length() - DISCRETE_HISTORY_FILE_SUFFIX.length())).longValue();
                        if (Instant.now().minus(sDiscreteHistoryCutoff, (TemporalUnit) ChronoUnit.MILLIS).toEpochMilli() > timestamp) {
                            f.delete();
                            Slog.e(TAG, "Deleting file " + fileName);
                        }
                    } catch (Throwable t) {
                        Slog.e(TAG, "Error while cleaning timeline files: " + t.getMessage() + " " + t.getStackTrace());
                    }
                }
            }
        }
    }

    private void createDiscreteAccessDirLocked() {
        if (!this.mDiscreteAccessDir.exists()) {
            if (!this.mDiscreteAccessDir.mkdirs()) {
                Slog.e(TAG, "Failed to create DiscreteRegistry directory");
            }
            FileUtils.setPermissions(this.mDiscreteAccessDir.getPath(), 505, -1, -1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DiscreteUidOps {
        ArrayMap<String, DiscretePackageOps> mPackages = new ArrayMap<>();

        DiscreteUidOps() {
        }

        boolean isEmpty() {
            return this.mPackages.isEmpty();
        }

        void merge(DiscreteUidOps other) {
            int nPackages = other.mPackages.size();
            for (int i = 0; i < nPackages; i++) {
                String packageName = other.mPackages.keyAt(i);
                DiscretePackageOps p = other.mPackages.valueAt(i);
                getOrCreateDiscretePackageOps(packageName).merge(p);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void filter(long beginTimeMillis, long endTimeMillis, int filter, String packageNameFilter, String[] opNamesFilter, String attributionTagFilter, int flagsFilter, int currentUid, ArrayMap<Integer, AttributionChain> attributionChains) {
            if ((filter & 2) != 0) {
                ArrayMap<String, DiscretePackageOps> packages = new ArrayMap<>();
                packages.put(packageNameFilter, getOrCreateDiscretePackageOps(packageNameFilter));
                this.mPackages = packages;
            }
            int nPackages = this.mPackages.size();
            for (int i = nPackages - 1; i >= 0; i--) {
                this.mPackages.valueAt(i).filter(beginTimeMillis, endTimeMillis, filter, opNamesFilter, attributionTagFilter, flagsFilter, currentUid, this.mPackages.keyAt(i), attributionChains);
                if (this.mPackages.valueAt(i).isEmpty()) {
                    this.mPackages.removeAt(i);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void offsetHistory(long offset) {
            int nPackages = this.mPackages.size();
            for (int i = 0; i < nPackages; i++) {
                this.mPackages.valueAt(i).offsetHistory(offset);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void clearPackage(String packageName) {
            this.mPackages.remove(packageName);
        }

        void addDiscreteAccess(int op, String packageName, String attributionTag, int flags, int uidState, long accessTime, long accessDuration, int attributionFlags, int attributionChainId) {
            getOrCreateDiscretePackageOps(packageName).addDiscreteAccess(op, attributionTag, flags, uidState, accessTime, accessDuration, attributionFlags, attributionChainId);
        }

        private DiscretePackageOps getOrCreateDiscretePackageOps(String packageName) {
            DiscretePackageOps result = this.mPackages.get(packageName);
            if (result == null) {
                DiscretePackageOps result2 = new DiscretePackageOps();
                this.mPackages.put(packageName, result2);
                return result2;
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void applyToHistory(AppOpsManager.HistoricalOps result, int uid, ArrayMap<Integer, AttributionChain> attributionChains) {
            int nPackages = this.mPackages.size();
            for (int i = 0; i < nPackages; i++) {
                this.mPackages.valueAt(i).applyToHistory(result, uid, this.mPackages.keyAt(i), attributionChains);
            }
        }

        void serialize(TypedXmlSerializer out) throws Exception {
            int nPackages = this.mPackages.size();
            for (int i = 0; i < nPackages; i++) {
                out.startTag((String) null, DiscreteRegistry.TAG_PACKAGE);
                out.attribute((String) null, DiscreteRegistry.ATTR_PACKAGE_NAME, this.mPackages.keyAt(i));
                this.mPackages.valueAt(i).serialize(out);
                out.endTag((String) null, DiscreteRegistry.TAG_PACKAGE);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw, SimpleDateFormat sdf, Date date, String prefix, int nDiscreteOps) {
            int nPackages = this.mPackages.size();
            for (int i = 0; i < nPackages; i++) {
                pw.print(prefix);
                pw.print("Package: ");
                pw.print(this.mPackages.keyAt(i));
                pw.println();
                this.mPackages.valueAt(i).dump(pw, sdf, date, prefix + "  ", nDiscreteOps);
            }
        }

        void deserialize(TypedXmlPullParser parser, long beginTimeMillis) throws Exception {
            int depth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, depth)) {
                if (DiscreteRegistry.TAG_PACKAGE.equals(parser.getName())) {
                    String packageName = parser.getAttributeValue((String) null, DiscreteRegistry.ATTR_PACKAGE_NAME);
                    getOrCreateDiscretePackageOps(packageName).deserialize(parser, beginTimeMillis);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DiscretePackageOps {
        ArrayMap<Integer, DiscreteOp> mPackageOps = new ArrayMap<>();

        DiscretePackageOps() {
        }

        boolean isEmpty() {
            return this.mPackageOps.isEmpty();
        }

        void addDiscreteAccess(int op, String attributionTag, int flags, int uidState, long accessTime, long accessDuration, int attributionFlags, int attributionChainId) {
            getOrCreateDiscreteOp(op).addDiscreteAccess(attributionTag, flags, uidState, accessTime, accessDuration, attributionFlags, attributionChainId);
        }

        void merge(DiscretePackageOps other) {
            int nOps = other.mPackageOps.size();
            for (int i = 0; i < nOps; i++) {
                int opId = other.mPackageOps.keyAt(i).intValue();
                DiscreteOp op = other.mPackageOps.valueAt(i);
                getOrCreateDiscreteOp(opId).merge(op);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void filter(long beginTimeMillis, long endTimeMillis, int filter, String[] opNamesFilter, String attributionTagFilter, int flagsFilter, int currentUid, String currentPkgName, ArrayMap<Integer, AttributionChain> attributionChains) {
            int nOps = this.mPackageOps.size();
            for (int i = nOps - 1; i >= 0; i--) {
                int opId = this.mPackageOps.keyAt(i).intValue();
                if ((filter & 8) != 0 && !ArrayUtils.contains(opNamesFilter, AppOpsManager.opToPublicName(opId))) {
                    this.mPackageOps.removeAt(i);
                }
                this.mPackageOps.valueAt(i).filter(beginTimeMillis, endTimeMillis, filter, attributionTagFilter, flagsFilter, currentUid, currentPkgName, this.mPackageOps.keyAt(i).intValue(), attributionChains);
                if (this.mPackageOps.valueAt(i).isEmpty()) {
                    this.mPackageOps.removeAt(i);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void offsetHistory(long offset) {
            int nOps = this.mPackageOps.size();
            for (int i = 0; i < nOps; i++) {
                this.mPackageOps.valueAt(i).offsetHistory(offset);
            }
        }

        private DiscreteOp getOrCreateDiscreteOp(int op) {
            DiscreteOp result = this.mPackageOps.get(Integer.valueOf(op));
            if (result == null) {
                DiscreteOp result2 = new DiscreteOp();
                this.mPackageOps.put(Integer.valueOf(op), result2);
                return result2;
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void applyToHistory(AppOpsManager.HistoricalOps result, int uid, String packageName, ArrayMap<Integer, AttributionChain> attributionChains) {
            int nPackageOps = this.mPackageOps.size();
            for (int i = 0; i < nPackageOps; i++) {
                this.mPackageOps.valueAt(i).applyToHistory(result, uid, packageName, this.mPackageOps.keyAt(i).intValue(), attributionChains);
            }
        }

        void serialize(TypedXmlSerializer out) throws Exception {
            int nOps = this.mPackageOps.size();
            for (int i = 0; i < nOps; i++) {
                out.startTag((String) null, DiscreteRegistry.TAG_OP);
                out.attributeInt((String) null, DiscreteRegistry.ATTR_OP_ID, this.mPackageOps.keyAt(i).intValue());
                this.mPackageOps.valueAt(i).serialize(out);
                out.endTag((String) null, DiscreteRegistry.TAG_OP);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw, SimpleDateFormat sdf, Date date, String prefix, int nDiscreteOps) {
            int nOps = this.mPackageOps.size();
            for (int i = 0; i < nOps; i++) {
                pw.print(prefix);
                pw.print(AppOpsManager.opToName(this.mPackageOps.keyAt(i).intValue()));
                pw.println();
                this.mPackageOps.valueAt(i).dump(pw, sdf, date, prefix + "  ", nDiscreteOps);
            }
        }

        void deserialize(TypedXmlPullParser parser, long beginTimeMillis) throws Exception {
            int depth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, depth)) {
                if (DiscreteRegistry.TAG_OP.equals(parser.getName())) {
                    int op = parser.getAttributeInt((String) null, DiscreteRegistry.ATTR_OP_ID);
                    getOrCreateDiscreteOp(op).deserialize(parser, beginTimeMillis);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DiscreteOp {
        ArrayMap<String, List<DiscreteOpEvent>> mAttributedOps = new ArrayMap<>();

        DiscreteOp() {
        }

        boolean isEmpty() {
            return this.mAttributedOps.isEmpty();
        }

        void merge(DiscreteOp other) {
            int nTags = other.mAttributedOps.size();
            for (int i = 0; i < nTags; i++) {
                String tag = other.mAttributedOps.keyAt(i);
                List<DiscreteOpEvent> otherEvents = other.mAttributedOps.valueAt(i);
                List<DiscreteOpEvent> events = getOrCreateDiscreteOpEventsList(tag);
                this.mAttributedOps.put(tag, DiscreteRegistry.stableListMerge(events, otherEvents));
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void filter(long beginTimeMillis, long endTimeMillis, int filter, String attributionTagFilter, int flagsFilter, int currentUid, String currentPkgName, int currentOp, ArrayMap<Integer, AttributionChain> attributionChains) {
            if ((filter & 4) != 0) {
                ArrayMap<String, List<DiscreteOpEvent>> attributedOps = new ArrayMap<>();
                attributedOps.put(attributionTagFilter, getOrCreateDiscreteOpEventsList(attributionTagFilter));
                this.mAttributedOps = attributedOps;
            }
            int nTags = this.mAttributedOps.size();
            for (int i = nTags - 1; i >= 0; i--) {
                String tag = this.mAttributedOps.keyAt(i);
                List<DiscreteOpEvent> list = DiscreteRegistry.filterEventsList(this.mAttributedOps.valueAt(i), beginTimeMillis, endTimeMillis, flagsFilter, currentUid, currentPkgName, currentOp, this.mAttributedOps.keyAt(i), attributionChains);
                this.mAttributedOps.put(tag, list);
                if (list.size() == 0) {
                    this.mAttributedOps.removeAt(i);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void offsetHistory(long offset) {
            DiscreteOp discreteOp = this;
            int nTags = discreteOp.mAttributedOps.size();
            int i = 0;
            while (i < nTags) {
                List<DiscreteOpEvent> list = discreteOp.mAttributedOps.valueAt(i);
                int n = list.size();
                int j = 0;
                while (j < n) {
                    DiscreteOpEvent event = list.get(j);
                    list.set(j, new DiscreteOpEvent(event.mNoteTime - offset, event.mNoteDuration, event.mUidState, event.mOpFlag, event.mAttributionFlags, event.mAttributionChainId));
                    j++;
                    discreteOp = this;
                    nTags = nTags;
                }
                i++;
                discreteOp = this;
            }
        }

        void addDiscreteAccess(String attributionTag, int flags, int uidState, long accessTime, long accessDuration, int attributionFlags, int attributionChainId) {
            List<DiscreteOpEvent> attributedOps = getOrCreateDiscreteOpEventsList(attributionTag);
            int nAttributedOps = attributedOps.size();
            int i = nAttributedOps;
            while (true) {
                if (i <= 0) {
                    break;
                }
                DiscreteOpEvent previousOp = attributedOps.get(i - 1);
                if (DiscreteRegistry.discretizeTimeStamp(previousOp.mNoteTime) < DiscreteRegistry.discretizeTimeStamp(accessTime)) {
                    break;
                }
                if (previousOp.mOpFlag == flags && previousOp.mUidState == uidState) {
                    if (previousOp.mAttributionFlags == attributionFlags) {
                        if (previousOp.mAttributionChainId != attributionChainId) {
                            i--;
                        } else if (DiscreteRegistry.discretizeDuration(accessDuration) == DiscreteRegistry.discretizeDuration(previousOp.mNoteDuration)) {
                            return;
                        }
                    }
                    i--;
                }
                i--;
            }
            attributedOps.add(i, new DiscreteOpEvent(accessTime, accessDuration, uidState, flags, attributionFlags, attributionChainId));
        }

        private List<DiscreteOpEvent> getOrCreateDiscreteOpEventsList(String attributionTag) {
            List<DiscreteOpEvent> result = this.mAttributedOps.get(attributionTag);
            if (result == null) {
                List<DiscreteOpEvent> result2 = new ArrayList<>();
                this.mAttributedOps.put(attributionTag, result2);
                return result2;
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void applyToHistory(AppOpsManager.HistoricalOps result, int uid, String packageName, int op, ArrayMap<Integer, AttributionChain> attributionChains) {
            DiscreteOp discreteOp = this;
            int nOps = discreteOp.mAttributedOps.size();
            int i = 0;
            while (i < nOps) {
                String tag = discreteOp.mAttributedOps.keyAt(i);
                List<DiscreteOpEvent> events = discreteOp.mAttributedOps.valueAt(i);
                int j = 0;
                for (int nEvents = events.size(); j < nEvents; nEvents = nEvents) {
                    DiscreteOpEvent event = events.get(j);
                    AppOpsManager.OpEventProxyInfo proxy = null;
                    if (event.mAttributionChainId != -1 && attributionChains != null) {
                        AttributionChain chain = attributionChains.get(Integer.valueOf(event.mAttributionChainId));
                        if (chain != null && chain.isComplete()) {
                            if (chain.isStart(packageName, uid, tag, op, event) && chain.mLastVisibleEvent != null) {
                                AttributionChain.OpEvent proxyEvent = chain.mLastVisibleEvent;
                                proxy = new AppOpsManager.OpEventProxyInfo(proxyEvent.mUid, proxyEvent.mPkgName, proxyEvent.mAttributionTag);
                            }
                        }
                    }
                    result.addDiscreteAccess(op, uid, packageName, tag, event.mUidState, event.mOpFlag, DiscreteRegistry.discretizeTimeStamp(event.mNoteTime), DiscreteRegistry.discretizeDuration(event.mNoteDuration), proxy);
                    j++;
                    events = events;
                }
                i++;
                discreteOp = this;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw, SimpleDateFormat sdf, Date date, String prefix, int nDiscreteOps) {
            int nAttributions = this.mAttributedOps.size();
            for (int i = 0; i < nAttributions; i++) {
                pw.print(prefix);
                pw.print("Attribution: ");
                pw.print(this.mAttributedOps.keyAt(i));
                pw.println();
                List<DiscreteOpEvent> ops = this.mAttributedOps.valueAt(i);
                int nOps = ops.size();
                int first = nDiscreteOps >= 1 ? Math.max(0, nOps - nDiscreteOps) : 0;
                for (int j = first; j < nOps; j++) {
                    ops.get(j).dump(pw, sdf, date, prefix + "  ");
                }
            }
        }

        void serialize(TypedXmlSerializer out) throws Exception {
            int nAttributions = this.mAttributedOps.size();
            for (int i = 0; i < nAttributions; i++) {
                out.startTag((String) null, "a");
                String tag = this.mAttributedOps.keyAt(i);
                if (tag != null) {
                    out.attribute((String) null, DiscreteRegistry.ATTR_TAG, this.mAttributedOps.keyAt(i));
                }
                List<DiscreteOpEvent> ops = this.mAttributedOps.valueAt(i);
                int nOps = ops.size();
                for (int j = 0; j < nOps; j++) {
                    out.startTag((String) null, DiscreteRegistry.TAG_ENTRY);
                    ops.get(j).serialize(out);
                    out.endTag((String) null, DiscreteRegistry.TAG_ENTRY);
                }
                out.endTag((String) null, "a");
            }
        }

        void deserialize(TypedXmlPullParser parser, long beginTimeMillis) throws Exception {
            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if ("a".equals(parser.getName())) {
                    String attributionTag = parser.getAttributeValue((String) null, DiscreteRegistry.ATTR_TAG);
                    List<DiscreteOpEvent> events = getOrCreateDiscreteOpEventsList(attributionTag);
                    int innerDepth = parser.getDepth();
                    while (XmlUtils.nextElementWithin(parser, innerDepth)) {
                        if (DiscreteRegistry.TAG_ENTRY.equals(parser.getName())) {
                            long noteTime = parser.getAttributeLong((String) null, DiscreteRegistry.ATTR_NOTE_TIME);
                            long noteDuration = parser.getAttributeLong((String) null, DiscreteRegistry.ATTR_NOTE_DURATION, -1L);
                            int uidState = parser.getAttributeInt((String) null, DiscreteRegistry.ATTR_UID_STATE);
                            int opFlags = parser.getAttributeInt((String) null, DiscreteRegistry.ATTR_FLAGS);
                            int attributionFlags = parser.getAttributeInt((String) null, DiscreteRegistry.ATTR_ATTRIBUTION_FLAGS, 0);
                            int attributionChainId = parser.getAttributeInt((String) null, DiscreteRegistry.ATTR_CHAIN_ID, -1);
                            if (noteTime + noteDuration >= beginTimeMillis) {
                                DiscreteOpEvent event = new DiscreteOpEvent(noteTime, noteDuration, uidState, opFlags, attributionFlags, attributionChainId);
                                events.add(event);
                            }
                        }
                    }
                    Collections.sort(events, new Comparator() { // from class: com.android.server.appop.DiscreteRegistry$DiscreteOp$$ExternalSyntheticLambda0
                        @Override // java.util.Comparator
                        public final int compare(Object obj, Object obj2) {
                            return DiscreteRegistry.DiscreteOp.lambda$deserialize$0((DiscreteRegistry.DiscreteOpEvent) obj, (DiscreteRegistry.DiscreteOpEvent) obj2);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ int lambda$deserialize$0(DiscreteOpEvent a, DiscreteOpEvent b) {
            if (a.mNoteTime < b.mNoteTime) {
                return -1;
            }
            return a.mNoteTime == b.mNoteTime ? 0 : 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DiscreteOpEvent {
        final int mAttributionChainId;
        final int mAttributionFlags;
        final long mNoteDuration;
        final long mNoteTime;
        final int mOpFlag;
        final int mUidState;

        DiscreteOpEvent(long noteTime, long noteDuration, int uidState, int opFlag, int attributionFlags, int attributionChainId) {
            this.mNoteTime = noteTime;
            this.mNoteDuration = noteDuration;
            this.mUidState = uidState;
            this.mOpFlag = opFlag;
            this.mAttributionFlags = attributionFlags;
            this.mAttributionChainId = attributionChainId;
        }

        public boolean equalsExceptDuration(DiscreteOpEvent o) {
            return this.mNoteTime == o.mNoteTime && this.mUidState == o.mUidState && this.mOpFlag == o.mOpFlag && this.mAttributionFlags == o.mAttributionFlags && this.mAttributionChainId == o.mAttributionChainId;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter pw, SimpleDateFormat sdf, Date date, String prefix) {
            pw.print(prefix);
            pw.print("Access [");
            pw.print(AppOpsManager.getUidStateName(this.mUidState));
            pw.print("-");
            pw.print(AppOpsManager.flagsToString(this.mOpFlag));
            pw.print("] at ");
            date.setTime(DiscreteRegistry.discretizeTimeStamp(this.mNoteTime));
            pw.print(sdf.format(date));
            if (this.mNoteDuration != -1) {
                pw.print(" for ");
                pw.print(DiscreteRegistry.discretizeDuration(this.mNoteDuration));
                pw.print(" milliseconds ");
            }
            if (this.mAttributionFlags != 0) {
                pw.print(" attribution flags=");
                pw.print(this.mAttributionFlags);
                pw.print(" with chainId=");
                pw.print(this.mAttributionChainId);
            }
            pw.println();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void serialize(TypedXmlSerializer out) throws Exception {
            out.attributeLong((String) null, DiscreteRegistry.ATTR_NOTE_TIME, this.mNoteTime);
            long j = this.mNoteDuration;
            if (j != -1) {
                out.attributeLong((String) null, DiscreteRegistry.ATTR_NOTE_DURATION, j);
            }
            int i = this.mAttributionFlags;
            if (i != 0) {
                out.attributeInt((String) null, DiscreteRegistry.ATTR_ATTRIBUTION_FLAGS, i);
            }
            int i2 = this.mAttributionChainId;
            if (i2 != -1) {
                out.attributeInt((String) null, DiscreteRegistry.ATTR_CHAIN_ID, i2);
            }
            out.attributeInt((String) null, DiscreteRegistry.ATTR_UID_STATE, this.mUidState);
            out.attributeInt((String) null, DiscreteRegistry.ATTR_FLAGS, this.mOpFlag);
        }
    }

    private static int[] parseOpsList(String opsList) {
        String[] strArr;
        if (opsList.isEmpty()) {
            strArr = new String[0];
        } else {
            strArr = opsList.split(",");
        }
        int nOps = strArr.length;
        int[] result = new int[nOps];
        for (int i = 0; i < nOps; i++) {
            try {
                result[i] = Integer.parseInt(strArr[i]);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Failed to parse Discrete ops list: " + e.getMessage());
                return parseOpsList(DEFAULT_DISCRETE_OPS);
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static List<DiscreteOpEvent> stableListMerge(List<DiscreteOpEvent> a, List<DiscreteOpEvent> b) {
        int nA = a.size();
        int nB = b.size();
        int i = 0;
        int k = 0;
        List<DiscreteOpEvent> result = new ArrayList<>(nA + nB);
        while (true) {
            if (i < nA || k < nB) {
                if (i == nA) {
                    result.add(b.get(k));
                    k++;
                } else if (k == nB) {
                    result.add(a.get(i));
                    i++;
                } else if (a.get(i).mNoteTime < b.get(k).mNoteTime) {
                    result.add(a.get(i));
                    i++;
                } else {
                    int i2 = k + 1;
                    result.add(b.get(k));
                    k = i2;
                }
            } else {
                return result;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static List<DiscreteOpEvent> filterEventsList(List<DiscreteOpEvent> list, long beginTimeMillis, long endTimeMillis, int flagsFilter, int currentUid, String currentPackageName, int currentOp, String currentAttrTag, ArrayMap<Integer, AttributionChain> attributionChains) {
        int n = list.size();
        List<DiscreteOpEvent> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            DiscreteOpEvent event = list.get(i);
            AttributionChain chain = attributionChains.get(Integer.valueOf(event.mAttributionChainId));
            if ((chain == null || chain.isStart(currentPackageName, currentUid, currentAttrTag, currentOp, event) || !chain.isComplete() || event.mAttributionChainId == -1) && (event.mOpFlag & flagsFilter) != 0 && event.mNoteTime + event.mNoteDuration > beginTimeMillis && event.mNoteTime < endTimeMillis) {
                result.add(event);
            }
        }
        return result;
    }

    private static boolean isDiscreteOp(int op, int flags) {
        return ArrayUtils.contains(sDiscreteOps, op) && (sDiscreteFlags & flags) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long discretizeTimeStamp(long timeStamp) {
        long j = sDiscreteHistoryQuantization;
        return (timeStamp / j) * j;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long discretizeDuration(long duration) {
        if (duration == -1) {
            return -1L;
        }
        long j = sDiscreteHistoryQuantization;
        return j * (((duration + j) - 1) / j);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDebugMode(boolean debugMode) {
        this.mDebugMode = debugMode;
    }
}
