package com.android.server.usage;

import android.app.usage.AppStandbyInfo;
import android.app.usage.UsageStatsManager;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.jobs.CollectionUtils;
import com.android.internal.util.jobs.FastXmlSerializer;
import com.android.internal.util.jobs.XmlUtils;
import com.android.server.job.JobPackageTracker;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class AppIdleHistory {
    static final String APP_IDLE_FILENAME = "app_idle_stats.xml";
    private static final String ATTR_BUCKET = "bucket";
    private static final String ATTR_BUCKETING_REASON = "bucketReason";
    private static final String ATTR_BUCKET_ACTIVE_TIMEOUT_TIME = "activeTimeoutTime";
    private static final String ATTR_BUCKET_WORKING_SET_TIMEOUT_TIME = "workingSetTimeoutTime";
    private static final String ATTR_CURRENT_BUCKET = "appLimitBucket";
    private static final String ATTR_ELAPSED_IDLE = "elapsedIdleTime";
    private static final String ATTR_EXPIRY_TIME = "expiry";
    private static final String ATTR_LAST_PREDICTED_TIME = "lastPredictedTime";
    private static final String ATTR_LAST_RESTRICTION_ATTEMPT_ELAPSED = "lastRestrictionAttemptElapsedTime";
    private static final String ATTR_LAST_RESTRICTION_ATTEMPT_REASON = "lastRestrictionAttemptReason";
    private static final String ATTR_LAST_RUN_JOB_TIME = "lastJobRunTime";
    private static final String ATTR_LAST_USED_BY_USER_ELAPSED = "lastUsedByUserElapsedTime";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_NEXT_ESTIMATED_APP_LAUNCH_TIME = "nextEstimatedAppLaunchTime";
    private static final String ATTR_SCREEN_IDLE = "screenIdleTime";
    private static final String ATTR_VERSION = "version";
    private static final boolean DEBUG = false;
    static final int IDLE_BUCKET_CUTOFF = 40;
    private static final long ONE_MINUTE = 60000;
    static final int STANDBY_BUCKET_UNKNOWN = -1;
    private static final String TAG = "AppIdleHistory";
    private static final String TAG_BUCKET_EXPIRY_TIMES = "expiryTimes";
    private static final String TAG_ITEM = "item";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PACKAGES = "packages";
    private static final int XML_VERSION_ADD_BUCKET_EXPIRY_TIMES = 1;
    private static final int XML_VERSION_CURRENT = 1;
    private static final int XML_VERSION_INITIAL = 0;
    private long mElapsedDuration;
    private long mElapsedSnapshot;
    private SparseArray<ArrayMap<String, AppUsageHistory>> mIdleHistory = new SparseArray<>();
    private boolean mScreenOn;
    private long mScreenOnDuration;
    private long mScreenOnSnapshot;
    private final File mStorageDir;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class AppUsageHistory {
        SparseLongArray bucketExpiryTimesMs;
        int bucketingReason;
        int currentBucket;
        int lastInformedBucket;
        long lastJobRunTime;
        int lastPredictedBucket = -1;
        long lastPredictedTime;
        long lastRestrictAttemptElapsedTime;
        int lastRestrictReason;
        long lastUsedByUserElapsedTime;
        long lastUsedElapsedTime;
        long lastUsedScreenTime;
        long nextEstimatedLaunchTime;

        AppUsageHistory() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppIdleHistory(File storageDir, long elapsedRealtime) {
        this.mElapsedSnapshot = elapsedRealtime;
        this.mScreenOnSnapshot = elapsedRealtime;
        this.mStorageDir = storageDir;
        readScreenOnTime();
    }

    public void updateDisplay(boolean screenOn, long elapsedRealtime) {
        if (screenOn == this.mScreenOn) {
            return;
        }
        this.mScreenOn = screenOn;
        if (screenOn) {
            this.mScreenOnSnapshot = elapsedRealtime;
            return;
        }
        this.mScreenOnDuration += elapsedRealtime - this.mScreenOnSnapshot;
        this.mElapsedDuration += elapsedRealtime - this.mElapsedSnapshot;
        this.mElapsedSnapshot = elapsedRealtime;
    }

    public long getScreenOnTime(long elapsedRealtime) {
        long screenOnTime = this.mScreenOnDuration;
        if (this.mScreenOn) {
            return screenOnTime + (elapsedRealtime - this.mScreenOnSnapshot);
        }
        return screenOnTime;
    }

    File getScreenOnTimeFile() {
        return new File(this.mStorageDir, "screen_on_time");
    }

    private void readScreenOnTime() {
        File screenOnTimeFile = getScreenOnTimeFile();
        if (screenOnTimeFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(screenOnTimeFile));
                this.mScreenOnDuration = Long.parseLong(reader.readLine());
                this.mElapsedDuration = Long.parseLong(reader.readLine());
                reader.close();
                return;
            } catch (IOException | NumberFormatException e) {
                return;
            }
        }
        writeScreenOnTime();
    }

    private void writeScreenOnTime() {
        AtomicFile screenOnTimeFile = new AtomicFile(getScreenOnTimeFile());
        FileOutputStream fos = null;
        try {
            fos = screenOnTimeFile.startWrite();
            fos.write((Long.toString(this.mScreenOnDuration) + "\n" + Long.toString(this.mElapsedDuration) + "\n").getBytes());
            screenOnTimeFile.finishWrite(fos);
        } catch (IOException e) {
            screenOnTimeFile.failWrite(fos);
        }
    }

    public void writeAppIdleDurations() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mElapsedDuration += elapsedRealtime - this.mElapsedSnapshot;
        this.mElapsedSnapshot = elapsedRealtime;
        writeScreenOnTime();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppUsageHistory reportUsage(AppUsageHistory appUsageHistory, String packageName, int userId, int newBucket, int usageReason, long nowElapsedRealtimeMs, long expiryElapsedRealtimeMs) {
        int newBucket2 = newBucket;
        int bucketingReason = usageReason | 768;
        boolean isUserUsage = AppStandbyController.isUserUsage(bucketingReason);
        if (appUsageHistory.currentBucket == 45 && !isUserUsage && (appUsageHistory.bucketingReason & JobPackageTracker.EVENT_STOP_REASON_MASK) != 512) {
            newBucket2 = 45;
            bucketingReason = appUsageHistory.bucketingReason;
        } else if (expiryElapsedRealtimeMs > nowElapsedRealtimeMs) {
            long expiryTimeMs = getElapsedTime(expiryElapsedRealtimeMs);
            if (appUsageHistory.bucketExpiryTimesMs == null) {
                appUsageHistory.bucketExpiryTimesMs = new SparseLongArray();
            }
            long currentExpiryTimeMs = appUsageHistory.bucketExpiryTimesMs.get(newBucket2);
            appUsageHistory.bucketExpiryTimesMs.put(newBucket2, Math.max(expiryTimeMs, currentExpiryTimeMs));
            removeElapsedExpiryTimes(appUsageHistory, getElapsedTime(nowElapsedRealtimeMs));
        }
        if (nowElapsedRealtimeMs != 0) {
            appUsageHistory.lastUsedElapsedTime = this.mElapsedDuration + (nowElapsedRealtimeMs - this.mElapsedSnapshot);
            if (isUserUsage) {
                appUsageHistory.lastUsedByUserElapsedTime = appUsageHistory.lastUsedElapsedTime;
            }
            appUsageHistory.lastUsedScreenTime = getScreenOnTime(nowElapsedRealtimeMs);
        }
        if (appUsageHistory.currentBucket >= newBucket2) {
            if (appUsageHistory.currentBucket > newBucket2) {
                appUsageHistory.currentBucket = newBucket2;
                logAppStandbyBucketChanged(packageName, userId, newBucket2, bucketingReason);
            }
            appUsageHistory.bucketingReason = bucketingReason;
        }
        return appUsageHistory;
    }

    private void removeElapsedExpiryTimes(AppUsageHistory appUsageHistory, long elapsedTimeMs) {
        if (appUsageHistory.bucketExpiryTimesMs == null) {
            return;
        }
        for (int i = appUsageHistory.bucketExpiryTimesMs.size() - 1; i >= 0; i--) {
            if (appUsageHistory.bucketExpiryTimesMs.valueAt(i) < elapsedTimeMs) {
                appUsageHistory.bucketExpiryTimesMs.removeAt(i);
            }
        }
    }

    public AppUsageHistory reportUsage(String packageName, int userId, int newBucket, int usageReason, long nowElapsedRealtimeMs, long expiryElapsedRealtimeMs) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory history = getPackageHistory(userHistory, packageName, nowElapsedRealtimeMs, true);
        return reportUsage(history, packageName, userId, newBucket, usageReason, nowElapsedRealtimeMs, expiryElapsedRealtimeMs);
    }

    private ArrayMap<String, AppUsageHistory> getUserHistory(int userId) {
        ArrayMap<String, AppUsageHistory> userHistory = this.mIdleHistory.get(userId);
        if (userHistory == null) {
            ArrayMap<String, AppUsageHistory> userHistory2 = new ArrayMap<>();
            this.mIdleHistory.put(userId, userHistory2);
            readAppIdleTimes(userId, userHistory2);
            return userHistory2;
        }
        return userHistory;
    }

    private AppUsageHistory getPackageHistory(ArrayMap<String, AppUsageHistory> userHistory, String packageName, long elapsedRealtime, boolean create) {
        AppUsageHistory appUsageHistory = userHistory.get(packageName);
        if (appUsageHistory == null && create) {
            AppUsageHistory appUsageHistory2 = new AppUsageHistory();
            appUsageHistory2.lastUsedByUserElapsedTime = -2147483648L;
            appUsageHistory2.lastUsedElapsedTime = -2147483648L;
            appUsageHistory2.lastUsedScreenTime = -2147483648L;
            appUsageHistory2.lastPredictedTime = -2147483648L;
            appUsageHistory2.currentBucket = 50;
            appUsageHistory2.bucketingReason = 256;
            appUsageHistory2.lastInformedBucket = -1;
            appUsageHistory2.lastJobRunTime = Long.MIN_VALUE;
            userHistory.put(packageName, appUsageHistory2);
            return appUsageHistory2;
        }
        return appUsageHistory;
    }

    public void onUserRemoved(int userId) {
        this.mIdleHistory.remove(userId);
    }

    public boolean isIdle(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, true);
        return appUsageHistory.currentBucket >= 40;
    }

    public AppUsageHistory getAppUsageHistory(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, true);
        return appUsageHistory;
    }

    public void setAppStandbyBucket(String packageName, int userId, long elapsedRealtime, int bucket, int reason) {
        setAppStandbyBucket(packageName, userId, elapsedRealtime, bucket, reason, false);
    }

    public void setAppStandbyBucket(String packageName, int userId, long elapsedRealtime, int bucket, int reason, boolean resetExpiryTimes) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, true);
        boolean changed = appUsageHistory.currentBucket != bucket;
        appUsageHistory.currentBucket = bucket;
        appUsageHistory.bucketingReason = reason;
        long elapsed = getElapsedTime(elapsedRealtime);
        if ((65280 & reason) == 1280) {
            appUsageHistory.lastPredictedTime = elapsed;
            appUsageHistory.lastPredictedBucket = bucket;
        }
        if (resetExpiryTimes && appUsageHistory.bucketExpiryTimesMs != null) {
            appUsageHistory.bucketExpiryTimesMs.clear();
        }
        if (changed) {
            logAppStandbyBucketChanged(packageName, userId, bucket, reason);
        }
    }

    public void updateLastPrediction(AppUsageHistory app, long elapsedTimeAdjusted, int bucket) {
        app.lastPredictedTime = elapsedTimeAdjusted;
        app.lastPredictedBucket = bucket;
    }

    public void setEstimatedLaunchTime(String packageName, int userId, long nowElapsed, long launchTime) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, nowElapsed, true);
        appUsageHistory.nextEstimatedLaunchTime = launchTime;
    }

    public void setLastJobRunTime(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, true);
        appUsageHistory.lastJobRunTime = getElapsedTime(elapsedRealtime);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteRestrictionAttempt(String packageName, int userId, long elapsedRealtime, int reason) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, true);
        appUsageHistory.lastRestrictAttemptElapsedTime = getElapsedTime(elapsedRealtime);
        appUsageHistory.lastRestrictReason = reason;
    }

    public long getEstimatedLaunchTime(String packageName, int userId, long nowElapsed) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, nowElapsed, false);
        if (appUsageHistory == null || appUsageHistory.nextEstimatedLaunchTime < System.currentTimeMillis()) {
            return JobStatus.NO_LATEST_RUNTIME;
        }
        return appUsageHistory.nextEstimatedLaunchTime;
    }

    public long getTimeSinceLastJobRun(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, false);
        if (appUsageHistory == null || appUsageHistory.lastJobRunTime == Long.MIN_VALUE) {
            return JobStatus.NO_LATEST_RUNTIME;
        }
        return getElapsedTime(elapsedRealtime) - appUsageHistory.lastJobRunTime;
    }

    public long getTimeSinceLastUsedByUser(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, false);
        if (appUsageHistory == null || appUsageHistory.lastUsedByUserElapsedTime == Long.MIN_VALUE || appUsageHistory.lastUsedByUserElapsedTime <= 0) {
            return JobStatus.NO_LATEST_RUNTIME;
        }
        return getElapsedTime(elapsedRealtime) - appUsageHistory.lastUsedByUserElapsedTime;
    }

    public int getAppStandbyBucket(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, false);
        if (appUsageHistory == null) {
            return 50;
        }
        return appUsageHistory.currentBucket;
    }

    public ArrayList<AppStandbyInfo> getAppStandbyBuckets(int userId, boolean appIdleEnabled) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        int size = userHistory.size();
        ArrayList<AppStandbyInfo> buckets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            buckets.add(new AppStandbyInfo(userHistory.keyAt(i), appIdleEnabled ? userHistory.valueAt(i).currentBucket : 10));
        }
        return buckets;
    }

    public int getAppStandbyReason(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, false);
        if (appUsageHistory != null) {
            return appUsageHistory.bucketingReason;
        }
        return 0;
    }

    public long getElapsedTime(long elapsedRealtime) {
        return (elapsedRealtime - this.mElapsedSnapshot) + this.mElapsedDuration;
    }

    public int setIdle(String packageName, int userId, boolean idle, long elapsedRealtime) {
        int newBucket;
        int reason;
        if (idle) {
            newBucket = 40;
            reason = 1024;
        } else {
            newBucket = 10;
            reason = UsbTerminalTypes.TERMINAL_OUT_HEADMOUNTED;
        }
        setAppStandbyBucket(packageName, userId, elapsedRealtime, newBucket, reason, false);
        return newBucket;
    }

    public void clearUsage(String packageName, int userId) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        userHistory.remove(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldInformListeners(String packageName, int userId, long elapsedRealtime, int bucket) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, true);
        if (appUsageHistory.lastInformedBucket != bucket) {
            appUsageHistory.lastInformedBucket = bucket;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getThresholdIndex(String packageName, int userId, long elapsedRealtime, long[] screenTimeThresholds, long[] elapsedTimeThresholds) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtime, false);
        if (appUsageHistory == null || appUsageHistory.lastUsedElapsedTime < 0 || appUsageHistory.lastUsedScreenTime < 0) {
            return -1;
        }
        long screenOnDelta = getScreenOnTime(elapsedRealtime) - appUsageHistory.lastUsedScreenTime;
        long elapsedDelta = getElapsedTime(elapsedRealtime) - appUsageHistory.lastUsedElapsedTime;
        for (int i = screenTimeThresholds.length - 1; i >= 0; i--) {
            if (screenOnDelta >= screenTimeThresholds[i] && elapsedDelta >= elapsedTimeThresholds[i]) {
                return i;
            }
        }
        return 0;
    }

    private void logAppStandbyBucketChanged(String packageName, int userId, int bucket, int reason) {
        FrameworkStatsLog.write(258, packageName, userId, bucket, reason & JobPackageTracker.EVENT_STOP_REASON_MASK, reason & 255);
    }

    long getBucketExpiryTimeMs(String packageName, int userId, int bucket, long elapsedRealtimeMs) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, elapsedRealtimeMs, false);
        if (appUsageHistory == null || appUsageHistory.bucketExpiryTimesMs == null) {
            return 0L;
        }
        return appUsageHistory.bucketExpiryTimesMs.get(bucket, 0L);
    }

    File getUserFile(int userId) {
        return new File(new File(new File(this.mStorageDir, DatabaseHelper.SoundModelContract.KEY_USERS), Integer.toString(userId)), APP_IDLE_FILENAME);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearLastUsedTimestamps(String packageName, int userId) {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(userId);
        AppUsageHistory appUsageHistory = getPackageHistory(userHistory, packageName, SystemClock.elapsedRealtime(), false);
        if (appUsageHistory != null) {
            appUsageHistory.lastUsedByUserElapsedTime = -2147483648L;
            appUsageHistory.lastUsedElapsedTime = -2147483648L;
            appUsageHistory.lastUsedScreenTime = -2147483648L;
        }
    }

    public boolean userFileExists(int userId) {
        return getUserFile(userId).exists();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [808=4] */
    private void readAppIdleTimes(int userId, ArrayMap<String, AppUsageHistory> userHistory) {
        AtomicFile appIdleFile;
        XmlPullParser parser;
        int type;
        int i;
        int type2;
        AtomicFile appIdleFile2;
        XmlPullParser parser2;
        int type3;
        long bucketWorkingSetTimeoutTime;
        FileInputStream fis = null;
        try {
            try {
                appIdleFile = new AtomicFile(getUserFile(userId));
                fis = appIdleFile.openRead();
                parser = Xml.newPullParser();
                parser.setInput(fis, StandardCharsets.UTF_8.name());
                while (true) {
                    type = parser.next();
                    i = 1;
                    type2 = 2;
                    if (type == 2 || type == 1) {
                        break;
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "Unable to read app idle file for user " + userId, e);
            }
            if (type != 2) {
                Slog.e(TAG, "Unable to read app idle file for user " + userId);
            } else if (parser.getName().equals(TAG_PACKAGES)) {
                int version = getIntValue(parser, ATTR_VERSION, 0);
                while (true) {
                    int type4 = parser.next();
                    if (type4 == i) {
                        break;
                    }
                    if (type4 == type2) {
                        String name = parser.getName();
                        if (name.equals("package")) {
                            String packageName = parser.getAttributeValue(null, "name");
                            AppUsageHistory appUsageHistory = new AppUsageHistory();
                            appUsageHistory.lastUsedElapsedTime = Long.parseLong(parser.getAttributeValue(null, ATTR_ELAPSED_IDLE));
                            appUsageHistory.lastUsedByUserElapsedTime = getLongValue(parser, ATTR_LAST_USED_BY_USER_ELAPSED, appUsageHistory.lastUsedElapsedTime);
                            appUsageHistory.lastUsedScreenTime = Long.parseLong(parser.getAttributeValue(null, ATTR_SCREEN_IDLE));
                            appUsageHistory.lastPredictedTime = getLongValue(parser, ATTR_LAST_PREDICTED_TIME, 0L);
                            String currentBucketString = parser.getAttributeValue(null, ATTR_CURRENT_BUCKET);
                            appUsageHistory.currentBucket = currentBucketString == null ? 10 : Integer.parseInt(currentBucketString);
                            String bucketingReason = parser.getAttributeValue(null, ATTR_BUCKETING_REASON);
                            appUsageHistory.lastJobRunTime = getLongValue(parser, ATTR_LAST_RUN_JOB_TIME, Long.MIN_VALUE);
                            appUsageHistory.bucketingReason = 256;
                            if (bucketingReason != null) {
                                try {
                                    appUsageHistory.bucketingReason = Integer.parseInt(bucketingReason, 16);
                                } catch (NumberFormatException nfe) {
                                    Slog.wtf(TAG, "Unable to read bucketing reason", nfe);
                                }
                            }
                            appUsageHistory.lastRestrictAttemptElapsedTime = getLongValue(parser, ATTR_LAST_RESTRICTION_ATTEMPT_ELAPSED, 0L);
                            String lastRestrictReason = parser.getAttributeValue(null, ATTR_LAST_RESTRICTION_ATTEMPT_REASON);
                            if (lastRestrictReason != null) {
                                try {
                                    appUsageHistory.lastRestrictReason = Integer.parseInt(lastRestrictReason, 16);
                                } catch (NumberFormatException nfe2) {
                                    Slog.wtf(TAG, "Unable to read last restrict reason", nfe2);
                                }
                            }
                            type3 = type4;
                            appUsageHistory.nextEstimatedLaunchTime = getLongValue(parser, ATTR_NEXT_ESTIMATED_APP_LAUNCH_TIME, 0L);
                            appUsageHistory.lastInformedBucket = -1;
                            userHistory.put(packageName, appUsageHistory);
                            if (version >= 1) {
                                int outerDepth = parser.getDepth();
                                while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                                    if (TAG_BUCKET_EXPIRY_TIMES.equals(parser.getName())) {
                                        readBucketExpiryTimes(parser, appUsageHistory);
                                    }
                                }
                                appIdleFile2 = appIdleFile;
                                parser2 = parser;
                            } else {
                                long bucketActiveTimeoutTime = getLongValue(parser, ATTR_BUCKET_ACTIVE_TIMEOUT_TIME, 0L);
                                long bucketWorkingSetTimeoutTime2 = getLongValue(parser, ATTR_BUCKET_WORKING_SET_TIMEOUT_TIME, 0L);
                                appIdleFile2 = appIdleFile;
                                parser2 = parser;
                                if (bucketActiveTimeoutTime == 0) {
                                    bucketWorkingSetTimeoutTime = bucketWorkingSetTimeoutTime2;
                                    if (bucketWorkingSetTimeoutTime != 0) {
                                    }
                                } else {
                                    bucketWorkingSetTimeoutTime = bucketWorkingSetTimeoutTime2;
                                }
                                insertBucketExpiryTime(appUsageHistory, 10, bucketActiveTimeoutTime);
                                insertBucketExpiryTime(appUsageHistory, 20, bucketWorkingSetTimeoutTime);
                            }
                        } else {
                            appIdleFile2 = appIdleFile;
                            parser2 = parser;
                            type3 = type4;
                        }
                    } else {
                        appIdleFile2 = appIdleFile;
                        parser2 = parser;
                        type3 = type4;
                    }
                    appIdleFile = appIdleFile2;
                    parser = parser2;
                    i = 1;
                    type2 = 2;
                }
            }
        } finally {
            IoUtils.closeQuietly((AutoCloseable) null);
        }
    }

    private void readBucketExpiryTimes(XmlPullParser parser, AppUsageHistory appUsageHistory) throws IOException, XmlPullParserException {
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            if ("item".equals(parser.getName())) {
                int bucket = getIntValue(parser, ATTR_BUCKET, -1);
                if (bucket == -1) {
                    Slog.e(TAG, "Error reading the buckets expiry times");
                } else {
                    long expiryTimeMs = getLongValue(parser, ATTR_EXPIRY_TIME, 0L);
                    insertBucketExpiryTime(appUsageHistory, bucket, expiryTimeMs);
                }
            }
        }
    }

    private void insertBucketExpiryTime(AppUsageHistory appUsageHistory, int bucket, long expiryTimeMs) {
        if (expiryTimeMs == 0) {
            return;
        }
        if (appUsageHistory.bucketExpiryTimesMs == null) {
            appUsageHistory.bucketExpiryTimesMs = new SparseLongArray();
        }
        appUsageHistory.bucketExpiryTimesMs.put(bucket, expiryTimeMs);
    }

    private long getLongValue(XmlPullParser parser, String attrName, long defValue) {
        String value = parser.getAttributeValue(null, attrName);
        return value == null ? defValue : Long.parseLong(value);
    }

    private int getIntValue(XmlPullParser parser, String attrName, int defValue) {
        String value = parser.getAttributeValue(null, attrName);
        return value == null ? defValue : Integer.parseInt(value);
    }

    public void writeAppIdleTimes(long elapsedRealtimeMs) {
        int size = this.mIdleHistory.size();
        for (int i = 0; i < size; i++) {
            writeAppIdleTimes(this.mIdleHistory.keyAt(i), elapsedRealtimeMs);
        }
    }

    public void writeAppIdleTimes(int userId, long elapsedRealtimeMs) {
        ArrayMap<String, AppUsageHistory> userHistory;
        String packageName;
        AppUsageHistory history;
        int size;
        FileOutputStream fos = null;
        AtomicFile appIdleFile = new AtomicFile(getUserFile(userId));
        try {
            fos = appIdleFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            FastXmlSerializer xml = new FastXmlSerializer();
            xml.setOutput(bos, StandardCharsets.UTF_8.name());
            xml.startDocument(null, true);
            xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xml.startTag(null, TAG_PACKAGES);
            xml.attribute(null, ATTR_VERSION, String.valueOf(1));
            long elapsedTimeMs = getElapsedTime(elapsedRealtimeMs);
            ArrayMap<String, AppUsageHistory> userHistory2 = getUserHistory(userId);
            int N = userHistory2.size();
            int i = 0;
            while (true) {
                BufferedOutputStream bos2 = bos;
                int N2 = N;
                if (i < N2) {
                    N = N2;
                    String packageName2 = userHistory2.keyAt(i);
                    if (packageName2 == null) {
                        Slog.w(TAG, "Skipping App Idle write for unexpected null package");
                        userHistory = userHistory2;
                    } else {
                        AppUsageHistory history2 = userHistory2.valueAt(i);
                        userHistory = userHistory2;
                        xml.startTag(null, "package");
                        xml.attribute(null, "name", packageName2);
                        xml.attribute(null, ATTR_ELAPSED_IDLE, Long.toString(history2.lastUsedElapsedTime));
                        xml.attribute(null, ATTR_LAST_USED_BY_USER_ELAPSED, Long.toString(history2.lastUsedByUserElapsedTime));
                        xml.attribute(null, ATTR_SCREEN_IDLE, Long.toString(history2.lastUsedScreenTime));
                        xml.attribute(null, ATTR_LAST_PREDICTED_TIME, Long.toString(history2.lastPredictedTime));
                        xml.attribute(null, ATTR_CURRENT_BUCKET, Integer.toString(history2.currentBucket));
                        xml.attribute(null, ATTR_BUCKETING_REASON, Integer.toHexString(history2.bucketingReason));
                        if (history2.lastJobRunTime != Long.MIN_VALUE) {
                            xml.attribute(null, ATTR_LAST_RUN_JOB_TIME, Long.toString(history2.lastJobRunTime));
                        }
                        if (history2.lastRestrictAttemptElapsedTime > 0) {
                            xml.attribute(null, ATTR_LAST_RESTRICTION_ATTEMPT_ELAPSED, Long.toString(history2.lastRestrictAttemptElapsedTime));
                        }
                        xml.attribute(null, ATTR_LAST_RESTRICTION_ATTEMPT_REASON, Integer.toHexString(history2.lastRestrictReason));
                        if (history2.nextEstimatedLaunchTime > 0) {
                            xml.attribute(null, ATTR_NEXT_ESTIMATED_APP_LAUNCH_TIME, Long.toString(history2.nextEstimatedLaunchTime));
                        }
                        if (history2.bucketExpiryTimesMs != null) {
                            xml.startTag(null, TAG_BUCKET_EXPIRY_TIMES);
                            int size2 = history2.bucketExpiryTimesMs.size();
                            int j = 0;
                            while (j < size2) {
                                long expiryTimeMs = history2.bucketExpiryTimesMs.valueAt(j);
                                if (expiryTimeMs < elapsedTimeMs) {
                                    packageName = packageName2;
                                    history = history2;
                                    size = size2;
                                } else {
                                    int bucket = history2.bucketExpiryTimesMs.keyAt(j);
                                    packageName = packageName2;
                                    xml.startTag(null, "item");
                                    history = history2;
                                    size = size2;
                                    xml.attribute(null, ATTR_BUCKET, String.valueOf(bucket));
                                    xml.attribute(null, ATTR_EXPIRY_TIME, String.valueOf(expiryTimeMs));
                                    xml.endTag(null, "item");
                                }
                                j++;
                                packageName2 = packageName;
                                history2 = history;
                                size2 = size;
                            }
                            xml.endTag(null, TAG_BUCKET_EXPIRY_TIMES);
                        }
                        xml.endTag(null, "package");
                    }
                    i++;
                    bos = bos2;
                    userHistory2 = userHistory;
                } else {
                    xml.endTag(null, TAG_PACKAGES);
                    xml.endDocument();
                    appIdleFile.finishWrite(fos);
                    return;
                }
            }
        } catch (Exception e) {
            appIdleFile.failWrite(fos);
            Slog.e(TAG, "Error writing app idle file for user " + userId, e);
        }
    }

    public void dumpUsers(IndentingPrintWriter idpw, int[] userIds, List<String> pkgs) {
        for (int i : userIds) {
            idpw.println();
            dumpUser(idpw, i, pkgs);
        }
    }

    private void dumpUser(IndentingPrintWriter idpw, int userId, List<String> pkgs) {
        ArrayMap<String, AppUsageHistory> userHistory;
        int P;
        int p;
        int i;
        int i2 = userId;
        idpw.print("User ");
        idpw.print(userId);
        idpw.println(" App Standby States:");
        idpw.increaseIndent();
        ArrayMap<String, AppUsageHistory> userHistory2 = this.mIdleHistory.get(i2);
        long now = System.currentTimeMillis();
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long totalElapsedTime = getElapsedTime(elapsedRealtime);
        getScreenOnTime(elapsedRealtime);
        if (userHistory2 == null) {
            return;
        }
        int P2 = userHistory2.size();
        int p2 = 0;
        while (p2 < P2) {
            String packageName = userHistory2.keyAt(p2);
            AppUsageHistory appUsageHistory = userHistory2.valueAt(p2);
            if (!CollectionUtils.isEmpty(pkgs) && !pkgs.contains(packageName)) {
                P = P2;
                p = p2;
                i = i2;
                userHistory = userHistory2;
                p2 = p + 1;
                i2 = i;
                userHistory2 = userHistory;
                P2 = P;
            }
            idpw.print("package=" + packageName);
            idpw.print(" u=" + i2);
            idpw.print(" bucket=" + appUsageHistory.currentBucket + " reason=" + UsageStatsManager.reasonToString(appUsageHistory.bucketingReason));
            idpw.print(" used=");
            userHistory = userHistory2;
            P = P2;
            p = p2;
            printLastActionElapsedTime(idpw, totalElapsedTime, appUsageHistory.lastUsedElapsedTime);
            idpw.print(" usedByUser=");
            printLastActionElapsedTime(idpw, totalElapsedTime, appUsageHistory.lastUsedByUserElapsedTime);
            idpw.print(" usedScr=");
            printLastActionElapsedTime(idpw, totalElapsedTime, appUsageHistory.lastUsedScreenTime);
            idpw.print(" lastPred=");
            printLastActionElapsedTime(idpw, totalElapsedTime, appUsageHistory.lastPredictedTime);
            dumpBucketExpiryTimes(idpw, appUsageHistory, totalElapsedTime);
            idpw.print(" lastJob=");
            TimeUtils.formatDuration(totalElapsedTime - appUsageHistory.lastJobRunTime, idpw);
            idpw.print(" lastInformedBucket=" + appUsageHistory.lastInformedBucket);
            if (appUsageHistory.lastRestrictAttemptElapsedTime > 0) {
                idpw.print(" lastRestrictAttempt=");
                TimeUtils.formatDuration(totalElapsedTime - appUsageHistory.lastRestrictAttemptElapsedTime, idpw);
                idpw.print(" lastRestrictReason=" + UsageStatsManager.reasonToString(appUsageHistory.lastRestrictReason));
            }
            if (appUsageHistory.nextEstimatedLaunchTime > 0) {
                idpw.print(" nextEstimatedLaunchTime=");
                TimeUtils.formatDuration(appUsageHistory.nextEstimatedLaunchTime - now, idpw);
            }
            i = userId;
            idpw.print(" idle=" + (isIdle(packageName, i, elapsedRealtime) ? "y" : "n"));
            idpw.println();
            p2 = p + 1;
            i2 = i;
            userHistory2 = userHistory;
            P2 = P;
        }
        idpw.println();
        idpw.print("totalElapsedTime=");
        TimeUtils.formatDuration(getElapsedTime(elapsedRealtime), idpw);
        idpw.println();
        idpw.print("totalScreenOnTime=");
        TimeUtils.formatDuration(getScreenOnTime(elapsedRealtime), idpw);
        idpw.println();
        idpw.decreaseIndent();
    }

    private void printLastActionElapsedTime(IndentingPrintWriter idpw, long totalElapsedTimeMS, long lastActionTimeMs) {
        if (lastActionTimeMs < 0) {
            idpw.print("<uninitialized>");
        } else {
            TimeUtils.formatDuration(totalElapsedTimeMS - lastActionTimeMs, idpw);
        }
    }

    private void dumpBucketExpiryTimes(IndentingPrintWriter idpw, AppUsageHistory appUsageHistory, long totalElapsedTimeMs) {
        idpw.print(" expiryTimes=");
        if (appUsageHistory.bucketExpiryTimesMs == null || appUsageHistory.bucketExpiryTimesMs.size() == 0) {
            idpw.print("<none>");
            return;
        }
        idpw.print("(");
        int size = appUsageHistory.bucketExpiryTimesMs.size();
        for (int i = 0; i < size; i++) {
            int bucket = appUsageHistory.bucketExpiryTimesMs.keyAt(i);
            long expiryTimeMs = appUsageHistory.bucketExpiryTimesMs.valueAt(i);
            if (i != 0) {
                idpw.print(",");
            }
            idpw.print(bucket + ":");
            TimeUtils.formatDuration(totalElapsedTimeMs - expiryTimeMs, idpw);
        }
        idpw.print(")");
    }
}
