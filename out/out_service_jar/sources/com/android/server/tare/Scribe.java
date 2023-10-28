package com.android.server.tare;

import android.content.pm.PackageInfo;
import android.os.Environment;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseArrayMap;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.server.job.controllers.JobStatus;
import com.android.server.tare.Analyst;
import com.android.server.tare.Ledger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class Scribe {
    private static final boolean DEBUG;
    private static final int MAX_NUM_TRANSACTION_DUMP = 25;
    private static final long MAX_TRANSACTION_AGE_MS = 86400000;
    private static final int STATE_FILE_VERSION = 0;
    private static final String TAG;
    private static final long WRITE_DELAY = 30000;
    private static final String XML_ATTR_CONSUMPTION_LIMIT = "consumptionLimit";
    private static final String XML_ATTR_CTP = "ctp";
    private static final String XML_ATTR_CURRENT_BALANCE = "currentBalance";
    private static final String XML_ATTR_DELTA = "delta";
    private static final String XML_ATTR_END_TIME = "endTime";
    private static final String XML_ATTR_EVENT_ID = "eventId";
    private static final String XML_ATTR_LAST_RECLAMATION_TIME = "lastReclamationTime";
    private static final String XML_ATTR_PACKAGE_NAME = "pkgName";
    private static final String XML_ATTR_PR_BATTERY_LEVEL = "batteryLevel";
    private static final String XML_ATTR_PR_DISCHARGE = "discharge";
    private static final String XML_ATTR_PR_LOSS = "loss";
    private static final String XML_ATTR_PR_NEG_REGULATIONS = "negRegulations";
    private static final String XML_ATTR_PR_NUM_LOSS = "numLoss";
    private static final String XML_ATTR_PR_NUM_NEG_REGULATIONS = "numNegRegulations";
    private static final String XML_ATTR_PR_NUM_POS_REGULATIONS = "numPosRegulations";
    private static final String XML_ATTR_PR_NUM_PROFIT = "numProfits";
    private static final String XML_ATTR_PR_NUM_REWARDS = "numRewards";
    private static final String XML_ATTR_PR_POS_REGULATIONS = "posRegulations";
    private static final String XML_ATTR_PR_PROFIT = "profit";
    private static final String XML_ATTR_PR_REWARDS = "rewards";
    private static final String XML_ATTR_REMAINING_CONSUMABLE_CAKES = "remainingConsumableCakes";
    private static final String XML_ATTR_START_TIME = "startTime";
    private static final String XML_ATTR_TAG = "tag";
    private static final String XML_ATTR_USER_ID = "userId";
    private static final String XML_ATTR_VERSION = "version";
    private static final String XML_TAG_HIGH_LEVEL_STATE = "irs-state";
    private static final String XML_TAG_LEDGER = "ledger";
    private static final String XML_TAG_PERIOD_REPORT = "report";
    private static final String XML_TAG_TARE = "tare";
    private static final String XML_TAG_TRANSACTION = "transaction";
    private static final String XML_TAG_USER = "user";
    private final Analyst mAnalyst;
    private final Runnable mCleanRunnable;
    private final InternalResourceService mIrs;
    private long mLastReclamationTime;
    private final SparseArrayMap<String, Ledger> mLedgers;
    private long mRemainingConsumableCakes;
    private long mSatiatedConsumptionLimit;
    private final AtomicFile mStateFile;
    private final Runnable mWriteRunnable;

    static {
        String str = "TARE-" + Scribe.class.getSimpleName();
        TAG = str;
        DEBUG = InternalResourceService.DEBUG || Log.isLoggable(str, 3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Scribe(InternalResourceService irs, Analyst analyst) {
        this(irs, analyst, Environment.getDataSystemDirectory());
    }

    Scribe(InternalResourceService irs, Analyst analyst, File dataDir) {
        this.mLedgers = new SparseArrayMap<>();
        this.mCleanRunnable = new Runnable() { // from class: com.android.server.tare.Scribe$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                Scribe.this.cleanupLedgers();
            }
        };
        this.mWriteRunnable = new Runnable() { // from class: com.android.server.tare.Scribe$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Scribe.this.writeState();
            }
        };
        this.mIrs = irs;
        this.mAnalyst = analyst;
        File tareDir = new File(dataDir, XML_TAG_TARE);
        tareDir.mkdirs();
        this.mStateFile = new AtomicFile(new File(tareDir, "state.xml"), XML_TAG_TARE);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void adjustRemainingConsumableCakesLocked(long delta) {
        if (delta != 0) {
            this.mRemainingConsumableCakes += delta;
            postWrite();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void discardLedgerLocked(int userId, String pkgName) {
        this.mLedgers.delete(userId, pkgName);
        postWrite();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getSatiatedConsumptionLimitLocked() {
        return this.mSatiatedConsumptionLimit;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastReclamationTimeLocked() {
        return this.mLastReclamationTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Ledger getLedgerLocked(int userId, String pkgName) {
        Ledger ledger = (Ledger) this.mLedgers.get(userId, pkgName);
        if (ledger == null) {
            Ledger ledger2 = new Ledger();
            this.mLedgers.add(userId, pkgName, ledger2);
            return ledger2;
        }
        return ledger;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SparseArrayMap<String, Ledger> getLedgersLocked() {
        return this.mLedgers;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getCakesInCirculationForLoggingLocked() {
        long sum = 0;
        for (int uIdx = this.mLedgers.numMaps() - 1; uIdx >= 0; uIdx--) {
            for (int pIdx = this.mLedgers.numElementsForKeyAt(uIdx) - 1; pIdx >= 0; pIdx--) {
                sum += ((Ledger) this.mLedgers.valueAt(uIdx, pIdx)).getCurrentBalance();
            }
        }
        return sum;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getRemainingConsumableCakesLocked() {
        return this.mRemainingConsumableCakes;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [292=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:113:?, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:114:?, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:115:?, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:116:?, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0083, code lost:
        if (r6 != 1) goto L45;
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x0087, code lost:
        if (com.android.server.tare.Scribe.DEBUG == false) goto L41;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x0089, code lost:
        android.util.Slog.w(com.android.server.tare.Scribe.TAG, "No persisted state.");
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x0090, code lost:
        if (r0 == null) goto L44;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x0092, code lost:
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x009d, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x00b2, code lost:
        if (com.android.server.tare.Scribe.XML_TAG_TARE.equals(r0.getName()) == false) goto L56;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00b4, code lost:
        r10 = r0.getAttributeInt((java.lang.String) null, com.android.server.tare.Scribe.XML_ATTR_VERSION);
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x00bb, code lost:
        if (r10 < 0) goto L51;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x00bd, code lost:
        if (r10 <= 0) goto L56;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00bf, code lost:
        android.util.Slog.e(com.android.server.tare.Scribe.TAG, "Invalid version number (" + r10 + "), aborting file read");
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00dd, code lost:
        if (r0 == null) goto L55;
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x00df, code lost:
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x00e2, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00e3, code lost:
        r12 = java.lang.System.currentTimeMillis() - 86400000;
        r14 = com.android.server.job.controllers.JobStatus.NO_LATEST_RUNTIME;
        r6 = r0.next();
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00f5, code lost:
        if (r6 == r8) goto L91;
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x00f7, code lost:
        if (r6 == r7) goto L64;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00f9, code lost:
        r17 = r2;
        r18 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00ff, code lost:
        r10 = r0.getName();
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x0104, code lost:
        if (r10 != null) goto L68;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x0106, code lost:
        r17 = r2;
        r18 = r3;
        r10 = r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x010d, code lost:
        r10 = -1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x0112, code lost:
        switch(r10.hashCode()) {
            case -934521548: goto L76;
            case 3599307: goto L73;
            case 689502574: goto L70;
            default: goto L79;
        };
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x011c, code lost:
        if (r10.equals(com.android.server.tare.Scribe.XML_TAG_HIGH_LEVEL_STATE) == false) goto L79;
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:0x011e, code lost:
        r10 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:60:0x0127, code lost:
        if (r10.equals(com.android.server.tare.Scribe.XML_TAG_USER) == false) goto L79;
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x0129, code lost:
        r10 = r8;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x0132, code lost:
        if (r10.equals(com.android.server.tare.Scribe.XML_TAG_PERIOD_REPORT) == false) goto L79;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x0134, code lost:
        r10 = 2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:0x0135, code lost:
        switch(r10) {
            case 0: goto L86;
            case 1: goto L84;
            case 2: goto L83;
            default: goto L80;
        };
     */
    /* JADX WARN: Code restructure failed: missing block: B:66:0x0138, code lost:
        r17 = r2;
        r18 = r3;
        r10 = r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x0140, code lost:
        r0.add(readReportFromXml(r0));
        r17 = r2;
        r18 = r3;
        r10 = r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:71:0x0157, code lost:
        r17 = r2;
        r18 = r3;
        r14 = java.lang.Math.min(r14, readUserFromXmlLocked(r0, r2, r12));
     */
    /* JADX WARN: Code restructure failed: missing block: B:72:0x015e, code lost:
        r10 = r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:73:0x015f, code lost:
        r19.mLastReclamationTime = r0.getAttributeLong((java.lang.String) null, com.android.server.tare.Scribe.XML_ATTR_LAST_RECLAMATION_TIME);
        r19.mSatiatedConsumptionLimit = r0.getAttributeLong((java.lang.String) null, com.android.server.tare.Scribe.XML_ATTR_CONSUMPTION_LIMIT, r19.mIrs.getInitialSatiatedConsumptionLimitLocked());
        r7 = r19.mIrs.getConsumptionLimitLocked();
     */
    /* JADX WARN: Code restructure failed: missing block: B:74:0x0182, code lost:
        r17 = r2;
        r18 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:75:0x0186, code lost:
        r19.mRemainingConsumableCakes = java.lang.Math.min(r7, r0.getAttributeLong((java.lang.String) null, com.android.server.tare.Scribe.XML_ATTR_REMAINING_CONSUMABLE_CAKES, r7));
     */
    /* JADX WARN: Code restructure failed: missing block: B:76:0x0191, code lost:
        android.util.Slog.e(com.android.server.tare.Scribe.TAG, "Unexpected tag: " + r10);
     */
    /* JADX WARN: Code restructure failed: missing block: B:78:0x01a8, code lost:
        r6 = r0.next();
        r2 = r17;
        r3 = r18;
        r7 = 2;
        r8 = 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:79:0x01b5, code lost:
        r19.mAnalyst.loadReports(r0);
        scheduleCleanup(r14);
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x01c1, code lost:
        if (r0 == null) goto L96;
     */
    /* JADX WARN: Code restructure failed: missing block: B:81:0x01c3, code lost:
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:83:0x01c7, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x01c8, code lost:
        r2 = r0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void loadFromDiskLocked() {
        Throwable th;
        TypedXmlPullParser parser;
        int eventType;
        this.mLedgers.clear();
        if (!recordExists()) {
            this.mSatiatedConsumptionLimit = this.mIrs.getInitialSatiatedConsumptionLimitLocked();
            this.mRemainingConsumableCakes = this.mIrs.getConsumptionLimitLocked();
            return;
        }
        this.mSatiatedConsumptionLimit = 0L;
        this.mRemainingConsumableCakes = 0L;
        SparseArray<ArraySet<String>> installedPackagesPerUser = new SparseArray<>();
        List<PackageInfo> installedPackages = this.mIrs.getInstalledPackages();
        for (int i = 0; i < installedPackages.size(); i++) {
            PackageInfo packageInfo = installedPackages.get(i);
            if (packageInfo.applicationInfo != null) {
                int userId = UserHandle.getUserId(packageInfo.applicationInfo.uid);
                ArraySet<String> pkgsForUser = installedPackagesPerUser.get(userId);
                if (pkgsForUser == null) {
                    pkgsForUser = new ArraySet<>();
                    installedPackagesPerUser.put(userId, pkgsForUser);
                }
                pkgsForUser.add(packageInfo.packageName);
            }
        }
        List<Analyst.Report> reports = new ArrayList<>();
        try {
            FileInputStream fis = this.mStateFile.openRead();
            try {
                try {
                    parser = Xml.resolvePullParser(fis);
                    eventType = parser.getEventType();
                } catch (Throwable th2) {
                    th = th2;
                }
                while (true) {
                    int i2 = 2;
                    int i3 = 1;
                    if (eventType == 2 || eventType == 1) {
                        try {
                            break;
                        } catch (IOException | XmlPullParserException e) {
                            e = e;
                            Slog.wtf(TAG, "Error reading state from disk", e);
                            return;
                        }
                    }
                    try {
                        eventType = parser.next();
                    } catch (Throwable th3) {
                        th = th3;
                    }
                    if (fis != null) {
                        fis.close();
                    }
                    throw th;
                }
            } catch (IOException | XmlPullParserException e2) {
                e = e2;
            }
        } catch (IOException | XmlPullParserException e3) {
            e = e3;
        }
    }

    void postWrite() {
        TareHandlerThread.getHandler().postDelayed(this.mWriteRunnable, 30000L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean recordExists() {
        return this.mStateFile.exists();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setConsumptionLimitLocked(long limit) {
        long j = this.mRemainingConsumableCakes;
        if (j > limit) {
            this.mRemainingConsumableCakes = limit;
        } else {
            long j2 = this.mSatiatedConsumptionLimit;
            if (limit > j2) {
                long diff = j2 - j;
                this.mRemainingConsumableCakes = limit - diff;
            }
        }
        this.mSatiatedConsumptionLimit = limit;
        postWrite();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastReclamationTimeLocked(long time) {
        this.mLastReclamationTime = time;
        postWrite();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void tearDownLocked() {
        TareHandlerThread.getHandler().removeCallbacks(this.mCleanRunnable);
        TareHandlerThread.getHandler().removeCallbacks(this.mWriteRunnable);
        this.mLedgers.clear();
        this.mRemainingConsumableCakes = 0L;
        this.mSatiatedConsumptionLimit = 0L;
        this.mLastReclamationTime = 0L;
    }

    void writeImmediatelyForTesting() {
        this.mWriteRunnable.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupLedgers() {
        synchronized (this.mIrs.getLock()) {
            TareHandlerThread.getHandler().removeCallbacks(this.mCleanRunnable);
            long earliestEndTime = JobStatus.NO_LATEST_RUNTIME;
            for (int uIdx = this.mLedgers.numMaps() - 1; uIdx >= 0; uIdx--) {
                int userId = this.mLedgers.keyAt(uIdx);
                for (int pIdx = this.mLedgers.numElementsForKey(userId) - 1; pIdx >= 0; pIdx--) {
                    String pkgName = (String) this.mLedgers.keyAt(uIdx, pIdx);
                    Ledger ledger = (Ledger) this.mLedgers.get(userId, pkgName);
                    ledger.removeOldTransactions(86400000L);
                    Ledger.Transaction transaction = ledger.getEarliestTransaction();
                    if (transaction != null) {
                        earliestEndTime = Math.min(earliestEndTime, transaction.endTimeMs);
                    }
                }
            }
            scheduleCleanup(earliestEndTime);
        }
    }

    private static Pair<String, Ledger> readLedgerFromXml(TypedXmlPullParser parser, ArraySet<String> validPackages, long endTimeCutoff) throws XmlPullParserException, IOException {
        List<Ledger.Transaction> transactions = new ArrayList<>();
        String pkgName = parser.getAttributeValue((String) null, XML_ATTR_PACKAGE_NAME);
        long curBalance = parser.getAttributeLong((String) null, XML_ATTR_CURRENT_BALANCE);
        boolean isInstalled = validPackages.contains(pkgName);
        if (!isInstalled) {
            Slog.w(TAG, "Invalid pkg " + pkgName + " is saved to disk");
        }
        int eventType = parser.next();
        while (eventType != 1) {
            String tagName = parser.getName();
            if (eventType == 3) {
                if (XML_TAG_LEDGER.equals(tagName)) {
                    break;
                }
            } else if (eventType != 2 || !XML_TAG_TRANSACTION.equals(tagName)) {
                Slog.e(TAG, "Unexpected event: (" + eventType + ") " + tagName);
                return null;
            } else if (isInstalled) {
                boolean z = DEBUG;
                if (z) {
                    Slog.d(TAG, "Starting ledger tag: " + tagName);
                }
                String tag = parser.getAttributeValue((String) null, XML_ATTR_TAG);
                long startTime = parser.getAttributeLong((String) null, XML_ATTR_START_TIME);
                long endTime = parser.getAttributeLong((String) null, XML_ATTR_END_TIME);
                int eventId = parser.getAttributeInt((String) null, XML_ATTR_EVENT_ID);
                long delta = parser.getAttributeLong((String) null, XML_ATTR_DELTA);
                long ctp = parser.getAttributeLong((String) null, XML_ATTR_CTP);
                if (endTime <= endTimeCutoff) {
                    if (z) {
                        Slog.d(TAG, "Skipping event because it's too old.");
                    }
                } else {
                    transactions.add(new Ledger.Transaction(startTime, endTime, eventId, tag, delta, ctp));
                }
            }
            eventType = parser.next();
        }
        if (!isInstalled) {
            return null;
        }
        return Pair.create(pkgName, new Ledger(curBalance, transactions));
    }

    private long readUserFromXmlLocked(TypedXmlPullParser parser, SparseArray<ArraySet<String>> installedPackagesPerUser, long endTimeCutoff) throws XmlPullParserException, IOException {
        Pair<String, Ledger> ledgerData;
        Ledger ledger;
        int curUser = parser.getAttributeInt((String) null, "userId");
        ArraySet<String> installedPackages = installedPackagesPerUser.get(curUser);
        if (installedPackages == null) {
            Slog.w(TAG, "Invalid user " + curUser + " is saved to disk");
            curUser = -10000;
        }
        long earliestEndTime = JobStatus.NO_LATEST_RUNTIME;
        int eventType = parser.next();
        while (eventType != 1) {
            String tagName = parser.getName();
            if (eventType == 3) {
                if (XML_TAG_USER.equals(tagName)) {
                    break;
                }
            } else if (XML_TAG_LEDGER.equals(tagName)) {
                if (curUser != -10000 && (ledgerData = readLedgerFromXml(parser, installedPackages, endTimeCutoff)) != null && (ledger = (Ledger) ledgerData.second) != null) {
                    this.mLedgers.add(curUser, (String) ledgerData.first, ledger);
                    Ledger.Transaction transaction = ledger.getEarliestTransaction();
                    if (transaction != null) {
                        earliestEndTime = Math.min(earliestEndTime, transaction.endTimeMs);
                    }
                }
            } else {
                Slog.e(TAG, "Unknown tag: " + tagName);
            }
            eventType = parser.next();
        }
        return earliestEndTime;
    }

    private static Analyst.Report readReportFromXml(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        Analyst.Report report = new Analyst.Report();
        report.cumulativeBatteryDischarge = parser.getAttributeInt((String) null, XML_ATTR_PR_DISCHARGE);
        report.currentBatteryLevel = parser.getAttributeInt((String) null, XML_ATTR_PR_BATTERY_LEVEL);
        report.cumulativeProfit = parser.getAttributeLong((String) null, XML_ATTR_PR_PROFIT);
        report.numProfitableActions = parser.getAttributeInt((String) null, XML_ATTR_PR_NUM_PROFIT);
        report.cumulativeLoss = parser.getAttributeLong((String) null, XML_ATTR_PR_LOSS);
        report.numUnprofitableActions = parser.getAttributeInt((String) null, XML_ATTR_PR_NUM_LOSS);
        report.cumulativeRewards = parser.getAttributeLong((String) null, XML_ATTR_PR_REWARDS);
        report.numRewards = parser.getAttributeInt((String) null, XML_ATTR_PR_NUM_REWARDS);
        report.cumulativePositiveRegulations = parser.getAttributeLong((String) null, XML_ATTR_PR_POS_REGULATIONS);
        report.numPositiveRegulations = parser.getAttributeInt((String) null, XML_ATTR_PR_NUM_POS_REGULATIONS);
        report.cumulativeNegativeRegulations = parser.getAttributeLong((String) null, XML_ATTR_PR_NEG_REGULATIONS);
        report.numNegativeRegulations = parser.getAttributeInt((String) null, XML_ATTR_PR_NUM_NEG_REGULATIONS);
        return report;
    }

    private void scheduleCleanup(long earliestEndTime) {
        if (earliestEndTime == JobStatus.NO_LATEST_RUNTIME) {
            return;
        }
        long delayMs = Math.max(3600000L, (86400000 + earliestEndTime) - System.currentTimeMillis());
        TareHandlerThread.getHandler().postDelayed(this.mCleanRunnable, delayMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeState() {
        synchronized (this.mIrs.getLock()) {
            TareHandlerThread.getHandler().removeCallbacks(this.mWriteRunnable);
            TareHandlerThread.getHandler().removeCallbacks(this.mCleanRunnable);
            if (this.mIrs.isEnabled()) {
                long earliestStoredEndTime = JobStatus.NO_LATEST_RUNTIME;
                try {
                    FileOutputStream fos = this.mStateFile.startWrite();
                    try {
                        TypedXmlSerializer out = Xml.resolveSerializer(fos);
                        out.startDocument((String) null, true);
                        out.startTag((String) null, XML_TAG_TARE);
                        out.attributeInt((String) null, XML_ATTR_VERSION, 0);
                        out.startTag((String) null, XML_TAG_HIGH_LEVEL_STATE);
                        out.attributeLong((String) null, XML_ATTR_LAST_RECLAMATION_TIME, this.mLastReclamationTime);
                        out.attributeLong((String) null, XML_ATTR_CONSUMPTION_LIMIT, this.mSatiatedConsumptionLimit);
                        out.attributeLong((String) null, XML_ATTR_REMAINING_CONSUMABLE_CAKES, this.mRemainingConsumableCakes);
                        out.endTag((String) null, XML_TAG_HIGH_LEVEL_STATE);
                        for (int uIdx = this.mLedgers.numMaps() - 1; uIdx >= 0; uIdx--) {
                            int userId = this.mLedgers.keyAt(uIdx);
                            earliestStoredEndTime = Math.min(earliestStoredEndTime, writeUserLocked(out, userId));
                        }
                        List<Analyst.Report> reports = this.mAnalyst.getReports();
                        int size = reports.size();
                        for (int i = 0; i < size; i++) {
                            writeReport(out, reports.get(i));
                        }
                        out.endTag((String) null, XML_TAG_TARE);
                        out.endDocument();
                        this.mStateFile.finishWrite(fos);
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (Throwable th) {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                } catch (IOException e) {
                    Slog.e(TAG, "Error writing state to disk", e);
                }
                scheduleCleanup(earliestStoredEndTime);
            }
        }
    }

    private long writeUserLocked(TypedXmlSerializer out, int userId) throws IOException {
        String str;
        int uIdx = this.mLedgers.indexOfKey(userId);
        long earliestStoredEndTime = JobStatus.NO_LATEST_RUNTIME;
        String str2 = null;
        String str3 = XML_TAG_USER;
        out.startTag((String) null, XML_TAG_USER);
        out.attributeInt((String) null, "userId", userId);
        for (int pIdx = this.mLedgers.numElementsForKey(userId) - 1; pIdx >= 0; pIdx--) {
            String pkgName = (String) this.mLedgers.keyAt(uIdx, pIdx);
            Ledger ledger = (Ledger) this.mLedgers.get(userId, pkgName);
            ledger.removeOldTransactions(86400000L);
            out.startTag(str2, XML_TAG_LEDGER);
            out.attribute(str2, XML_ATTR_PACKAGE_NAME, pkgName);
            out.attributeLong(str2, XML_ATTR_CURRENT_BALANCE, ledger.getCurrentBalance());
            List<Ledger.Transaction> transactions = ledger.getTransactions();
            int t = 0;
            while (t < transactions.size()) {
                Ledger.Transaction transaction = transactions.get(t);
                if (t != 0) {
                    str = str3;
                } else {
                    str = str3;
                    earliestStoredEndTime = Math.min(earliestStoredEndTime, transaction.endTimeMs);
                }
                writeTransaction(out, transaction);
                t++;
                str3 = str;
            }
            str2 = null;
            out.endTag((String) null, XML_TAG_LEDGER);
        }
        out.endTag(str2, str3);
        return earliestStoredEndTime;
    }

    private static void writeTransaction(TypedXmlSerializer out, Ledger.Transaction transaction) throws IOException {
        out.startTag((String) null, XML_TAG_TRANSACTION);
        out.attributeLong((String) null, XML_ATTR_START_TIME, transaction.startTimeMs);
        out.attributeLong((String) null, XML_ATTR_END_TIME, transaction.endTimeMs);
        out.attributeInt((String) null, XML_ATTR_EVENT_ID, transaction.eventId);
        if (transaction.tag != null) {
            out.attribute((String) null, XML_ATTR_TAG, transaction.tag);
        }
        out.attributeLong((String) null, XML_ATTR_DELTA, transaction.delta);
        out.attributeLong((String) null, XML_ATTR_CTP, transaction.ctp);
        out.endTag((String) null, XML_TAG_TRANSACTION);
    }

    private static void writeReport(TypedXmlSerializer out, Analyst.Report report) throws IOException {
        out.startTag((String) null, XML_TAG_PERIOD_REPORT);
        out.attributeInt((String) null, XML_ATTR_PR_DISCHARGE, report.cumulativeBatteryDischarge);
        out.attributeInt((String) null, XML_ATTR_PR_BATTERY_LEVEL, report.currentBatteryLevel);
        out.attributeLong((String) null, XML_ATTR_PR_PROFIT, report.cumulativeProfit);
        out.attributeInt((String) null, XML_ATTR_PR_NUM_PROFIT, report.numProfitableActions);
        out.attributeLong((String) null, XML_ATTR_PR_LOSS, report.cumulativeLoss);
        out.attributeInt((String) null, XML_ATTR_PR_NUM_LOSS, report.numUnprofitableActions);
        out.attributeLong((String) null, XML_ATTR_PR_REWARDS, report.cumulativeRewards);
        out.attributeInt((String) null, XML_ATTR_PR_NUM_REWARDS, report.numRewards);
        out.attributeLong((String) null, XML_ATTR_PR_POS_REGULATIONS, report.cumulativePositiveRegulations);
        out.attributeInt((String) null, XML_ATTR_PR_NUM_POS_REGULATIONS, report.numPositiveRegulations);
        out.attributeLong((String) null, XML_ATTR_PR_NEG_REGULATIONS, report.cumulativeNegativeRegulations);
        out.attributeInt((String) null, XML_ATTR_PR_NUM_NEG_REGULATIONS, report.numNegativeRegulations);
        out.endTag((String) null, XML_TAG_PERIOD_REPORT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpLocked(final IndentingPrintWriter pw, final boolean dumpAll) {
        pw.println("Ledgers:");
        pw.increaseIndent();
        this.mLedgers.forEach(new SparseArrayMap.TriConsumer() { // from class: com.android.server.tare.Scribe$$ExternalSyntheticLambda2
            public final void accept(int i, Object obj, Object obj2) {
                Scribe.this.m6778lambda$dumpLocked$0$comandroidservertareScribe(pw, dumpAll, i, (String) obj, (Ledger) obj2);
            }
        });
        pw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dumpLocked$0$com-android-server-tare-Scribe  reason: not valid java name */
    public /* synthetic */ void m6778lambda$dumpLocked$0$comandroidservertareScribe(IndentingPrintWriter pw, boolean dumpAll, int userId, String pkgName, Ledger ledger) {
        pw.print(TareUtils.appToString(userId, pkgName));
        if (this.mIrs.isSystem(userId, pkgName)) {
            pw.print(" (system)");
        }
        pw.println();
        pw.increaseIndent();
        ledger.dump(pw, dumpAll ? Integer.MAX_VALUE : 25);
        pw.decreaseIndent();
    }
}
