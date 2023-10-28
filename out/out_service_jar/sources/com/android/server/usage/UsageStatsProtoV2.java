package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import com.android.server.usage.IntervalStats;
import defpackage.CompanionAppsPermissions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
final class UsageStatsProtoV2 {
    private static final String TAG = "UsageStatsProtoV2";
    private static final long ONE_HOUR_MS = TimeUnit.HOURS.toMillis(1);
    private static int lastSize = 0;

    private UsageStatsProtoV2() {
    }

    private static UsageStats parseUsageStats(ProtoInputStream proto, long beginTime) throws IOException {
        UsageStats stats = new UsageStats();
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return stats;
                case 1:
                    stats.mPackageToken = proto.readInt((long) CompanionMessage.MESSAGE_ID) - 1;
                    break;
                case 3:
                    stats.mLastTimeUsed = proto.readLong(1112396529667L) + beginTime;
                    break;
                case 4:
                    stats.mTotalTimeInForeground = proto.readLong(1112396529668L);
                    break;
                case 6:
                    stats.mAppLaunchCount = proto.readInt(1120986464262L);
                    break;
                case 7:
                    try {
                        long token = proto.start(2246267895815L);
                        loadChooserCounts(proto, stats);
                        proto.end(token);
                        break;
                    } catch (IOException e) {
                        Slog.e(TAG, "Unable to read chooser counts for " + stats.mPackageToken);
                        break;
                    }
                case 8:
                    stats.mLastTimeForegroundServiceUsed = proto.readLong(1112396529672L) + beginTime;
                    break;
                case 9:
                    stats.mTotalTimeForegroundServiceUsed = proto.readLong(1112396529673L);
                    break;
                case 10:
                    stats.mLastTimeVisible = proto.readLong(1112396529674L) + beginTime;
                    break;
                case 11:
                    stats.mTotalTimeVisible = proto.readLong(1112396529675L);
                    break;
                case 12:
                    stats.mLastTimeComponentUsed = proto.readLong(1112396529676L) + beginTime;
                    break;
            }
        }
    }

    private static void loadCountAndTime(ProtoInputStream proto, long fieldId, IntervalStats.EventTracker tracker) {
        try {
            long token = proto.start(fieldId);
            while (true) {
                switch (proto.nextField()) {
                    case -1:
                        proto.end(token);
                        return;
                    case 1:
                        tracker.count = proto.readInt((long) CompanionMessage.MESSAGE_ID);
                        break;
                    case 2:
                        tracker.duration = proto.readLong(1112396529666L);
                        break;
                }
            }
        } catch (IOException e) {
            Slog.e(TAG, "Unable to read event tracker " + fieldId, e);
        }
    }

    private static void loadChooserCounts(ProtoInputStream proto, UsageStats usageStats) throws IOException {
        SparseIntArray counts;
        if (proto.nextField((long) CompanionMessage.MESSAGE_ID)) {
            int actionToken = proto.readInt((long) CompanionMessage.MESSAGE_ID) - 1;
            counts = (SparseIntArray) usageStats.mChooserCountsObfuscated.get(actionToken);
            if (counts == null) {
                counts = new SparseIntArray();
                usageStats.mChooserCountsObfuscated.put(actionToken, counts);
            }
        } else {
            counts = new SparseIntArray();
        }
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return;
                case 1:
                    usageStats.mChooserCountsObfuscated.put(proto.readInt((long) CompanionMessage.MESSAGE_ID) - 1, counts);
                    break;
                case 2:
                    long token = proto.start(2246267895810L);
                    loadCountsForAction(proto, counts);
                    proto.end(token);
                    break;
            }
        }
    }

    private static void loadCountsForAction(ProtoInputStream proto, SparseIntArray counts) throws IOException {
        int categoryToken = -1;
        int count = 0;
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    if (categoryToken != -1) {
                        counts.put(categoryToken, count);
                        return;
                    }
                    return;
                case 1:
                    int categoryToken2 = proto.readInt((long) CompanionMessage.MESSAGE_ID) - 1;
                    categoryToken = categoryToken2;
                    break;
                case 2:
                    count = proto.readInt(1120986464258L);
                    break;
            }
        }
    }

    private static void loadConfigStats(ProtoInputStream proto, IntervalStats stats) throws IOException {
        boolean configActive = false;
        Configuration config = new Configuration();
        ConfigurationStats configStats = new ConfigurationStats();
        if (proto.nextField(1146756268033L)) {
            config.readFromProto(proto, 1146756268033L);
            configStats = stats.getOrCreateConfigurationStats(config);
        }
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    if (configActive) {
                        stats.activeConfiguration = configStats.mConfiguration;
                        return;
                    }
                    return;
                case 1:
                    config.readFromProto(proto, 1146756268033L);
                    ConfigurationStats temp = stats.getOrCreateConfigurationStats(config);
                    temp.mLastTimeActive = configStats.mLastTimeActive;
                    temp.mTotalTimeActive = configStats.mTotalTimeActive;
                    temp.mActivationCount = configStats.mActivationCount;
                    configStats = temp;
                    break;
                case 2:
                    configStats.mLastTimeActive = stats.beginTime + proto.readLong(1112396529666L);
                    break;
                case 3:
                    configStats.mTotalTimeActive = proto.readLong(1112396529667L);
                    break;
                case 4:
                    configStats.mActivationCount = proto.readInt(1120986464260L);
                    break;
                case 5:
                    configActive = proto.readBoolean(1133871366149L);
                    break;
            }
        }
    }

    private static UsageEvents.Event parseEvent(ProtoInputStream proto, long beginTime) throws IOException {
        UsageEvents.Event event = new UsageEvents.Event();
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    if (event.mPackageToken == -1) {
                        return null;
                    }
                    return event;
                case 1:
                    event.mPackageToken = proto.readInt((long) CompanionMessage.MESSAGE_ID) - 1;
                    break;
                case 2:
                    event.mClassToken = proto.readInt(1120986464258L) - 1;
                    break;
                case 3:
                    event.mTimeStamp = proto.readLong(1112396529667L) + beginTime;
                    break;
                case 4:
                    event.mFlags = proto.readInt(1120986464260L);
                    break;
                case 5:
                    event.mEventType = proto.readInt(1120986464261L);
                    break;
                case 6:
                    event.mConfiguration = new Configuration();
                    event.mConfiguration.readFromProto(proto, 1146756268038L);
                    break;
                case 7:
                    event.mShortcutIdToken = proto.readInt(1120986464263L) - 1;
                    break;
                case 8:
                    event.mBucketAndReason = proto.readInt(1120986464264L);
                    break;
                case 9:
                    event.mNotificationChannelIdToken = proto.readInt(1120986464265L) - 1;
                    break;
                case 10:
                    event.mInstanceId = proto.readInt(1120986464266L);
                    break;
                case 11:
                    event.mTaskRootPackageToken = proto.readInt(1120986464267L) - 1;
                    break;
                case 12:
                    event.mTaskRootClassToken = proto.readInt(1120986464268L) - 1;
                    break;
                case 13:
                    event.mLocusIdToken = proto.readInt(1120986464269L) - 1;
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeOffsetTimestamp(ProtoOutputStream proto, long fieldId, long timestamp, long beginTime) {
        long rolloverGracePeriod = beginTime - ONE_HOUR_MS;
        if (timestamp > rolloverGracePeriod) {
            proto.write(fieldId, getOffsetTimestamp(timestamp, beginTime));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static long getOffsetTimestamp(long timestamp, long offset) {
        long offsetTimestamp = timestamp - offset;
        return offsetTimestamp == 0 ? 1 + offsetTimestamp : offsetTimestamp;
    }

    private static void writeUsageStats(ProtoOutputStream proto, long beginTime, UsageStats stats) throws IllegalArgumentException {
        proto.write(CompanionMessage.MESSAGE_ID, stats.mPackageToken + 1);
        writeOffsetTimestamp(proto, 1112396529667L, stats.mLastTimeUsed, beginTime);
        proto.write(1112396529668L, stats.mTotalTimeInForeground);
        writeOffsetTimestamp(proto, 1112396529672L, stats.mLastTimeForegroundServiceUsed, beginTime);
        proto.write(1112396529673L, stats.mTotalTimeForegroundServiceUsed);
        writeOffsetTimestamp(proto, 1112396529674L, stats.mLastTimeVisible, beginTime);
        proto.write(1112396529675L, stats.mTotalTimeVisible);
        writeOffsetTimestamp(proto, 1112396529676L, stats.mLastTimeComponentUsed, beginTime);
        proto.write(1120986464262L, stats.mAppLaunchCount);
        try {
            writeChooserCounts(proto, stats);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Unable to write chooser counts for " + stats.mPackageName, e);
        }
    }

    private static void writeCountAndTime(ProtoOutputStream proto, long fieldId, int count, long time) throws IllegalArgumentException {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, count);
        proto.write(1112396529666L, time);
        proto.end(token);
    }

    private static void writeChooserCounts(ProtoOutputStream proto, UsageStats stats) throws IllegalArgumentException {
        if (stats == null || stats.mChooserCountsObfuscated.size() == 0) {
            return;
        }
        int chooserCountSize = stats.mChooserCountsObfuscated.size();
        for (int i = 0; i < chooserCountSize; i++) {
            int action = stats.mChooserCountsObfuscated.keyAt(i);
            SparseIntArray counts = (SparseIntArray) stats.mChooserCountsObfuscated.valueAt(i);
            if (counts != null && counts.size() != 0) {
                long token = proto.start(2246267895815L);
                proto.write(CompanionMessage.MESSAGE_ID, action + 1);
                writeCountsForAction(proto, counts);
                proto.end(token);
            }
        }
    }

    private static void writeCountsForAction(ProtoOutputStream proto, SparseIntArray counts) throws IllegalArgumentException {
        int countsSize = counts.size();
        for (int i = 0; i < countsSize; i++) {
            int category = counts.keyAt(i);
            int count = counts.valueAt(i);
            if (count > 0) {
                long token = proto.start(2246267895810L);
                proto.write(CompanionMessage.MESSAGE_ID, category + 1);
                proto.write(1120986464258L, count);
                proto.end(token);
            }
        }
    }

    private static void writeConfigStats(ProtoOutputStream proto, long statsBeginTime, ConfigurationStats configStats, boolean isActive) throws IllegalArgumentException {
        configStats.mConfiguration.dumpDebug(proto, 1146756268033L);
        writeOffsetTimestamp(proto, 1112396529666L, configStats.mLastTimeActive, statsBeginTime);
        proto.write(1112396529667L, configStats.mTotalTimeActive);
        proto.write(1120986464260L, configStats.mActivationCount);
        proto.write(1133871366149L, isActive);
    }

    private static void writeEvent(ProtoOutputStream proto, long statsBeginTime, UsageEvents.Event event) throws IllegalArgumentException {
        proto.write(CompanionMessage.MESSAGE_ID, event.mPackageToken + 1);
        if (event.mClassToken != -1) {
            proto.write(1120986464258L, event.mClassToken + 1);
        }
        writeOffsetTimestamp(proto, 1112396529667L, event.mTimeStamp, statsBeginTime);
        proto.write(1120986464260L, event.mFlags);
        proto.write(1120986464261L, event.mEventType);
        proto.write(1120986464266L, event.mInstanceId);
        if (event.mTaskRootPackageToken != -1) {
            proto.write(1120986464267L, event.mTaskRootPackageToken + 1);
        }
        if (event.mTaskRootClassToken != -1) {
            proto.write(1120986464268L, event.mTaskRootClassToken + 1);
        }
        switch (event.mEventType) {
            case 5:
                if (event.mConfiguration != null) {
                    event.mConfiguration.dumpDebug(proto, 1146756268038L);
                    return;
                }
                return;
            case 8:
                if (event.mShortcutIdToken != -1) {
                    proto.write(1120986464263L, event.mShortcutIdToken + 1);
                    return;
                }
                return;
            case 11:
                if (event.mBucketAndReason != 0) {
                    proto.write(1120986464264L, event.mBucketAndReason);
                    return;
                }
                return;
            case 12:
                if (event.mNotificationChannelIdToken != -1) {
                    proto.write(1120986464265L, event.mNotificationChannelIdToken + 1);
                    return;
                }
                return;
            case 30:
                if (event.mLocusIdToken != -1) {
                    proto.write(1120986464269L, event.mLocusIdToken + 1);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public static void read(InputStream in, IntervalStats stats) throws IOException {
        ProtoInputStream proto = new ProtoInputStream(in);
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    int usageStatsSize = stats.packageStatsObfuscated.size();
                    for (int i = 0; i < usageStatsSize; i++) {
                        UsageStats usageStats = stats.packageStatsObfuscated.valueAt(i);
                        usageStats.mBeginTimeStamp = stats.beginTime;
                        usageStats.mEndTimeStamp = stats.endTime;
                    }
                    return;
                case 1:
                    stats.endTime = stats.beginTime + proto.readLong(1112396529665L);
                    break;
                case 2:
                    stats.majorVersion = proto.readInt(1120986464258L);
                    break;
                case 3:
                    stats.minorVersion = proto.readInt(1120986464259L);
                    break;
                case 10:
                    loadCountAndTime(proto, 1146756268042L, stats.interactiveTracker);
                    break;
                case 11:
                    loadCountAndTime(proto, 1146756268043L, stats.nonInteractiveTracker);
                    break;
                case 12:
                    loadCountAndTime(proto, 1146756268044L, stats.keyguardShownTracker);
                    break;
                case 13:
                    loadCountAndTime(proto, 1146756268045L, stats.keyguardHiddenTracker);
                    break;
                case 20:
                    try {
                        long packagesToken = proto.start(2246267895828L);
                        UsageStats usageStats2 = parseUsageStats(proto, stats.beginTime);
                        proto.end(packagesToken);
                        if (usageStats2.mPackageToken != -1) {
                            stats.packageStatsObfuscated.put(usageStats2.mPackageToken, usageStats2);
                            break;
                        } else {
                            break;
                        }
                    } catch (IOException e) {
                        Slog.e(TAG, "Unable to read some usage stats from proto.", e);
                        break;
                    }
                case 21:
                    try {
                        long configsToken = proto.start(2246267895829L);
                        loadConfigStats(proto, stats);
                        proto.end(configsToken);
                        break;
                    } catch (IOException e2) {
                        Slog.e(TAG, "Unable to read some configuration stats from proto.", e2);
                        break;
                    }
                case 22:
                    try {
                        long eventsToken = proto.start(2246267895830L);
                        UsageEvents.Event event = parseEvent(proto, stats.beginTime);
                        proto.end(eventsToken);
                        if (event != null) {
                            stats.events.insert(event);
                            break;
                        } else {
                            break;
                        }
                    } catch (IOException e3) {
                        Slog.e(TAG, "Unable to read some events from proto.", e3);
                        break;
                    }
            }
        }
    }

    public static void write(OutputStream out, IntervalStats stats) throws IOException, IllegalArgumentException {
        ProtoOutputStream proto = new ProtoOutputStream(out);
        proto.write(1112396529665L, getOffsetTimestamp(stats.endTime, stats.beginTime));
        proto.write(1120986464258L, stats.majorVersion);
        proto.write(1120986464259L, stats.minorVersion);
        try {
            writeCountAndTime(proto, 1146756268042L, stats.interactiveTracker.count, stats.interactiveTracker.duration);
            writeCountAndTime(proto, 1146756268043L, stats.nonInteractiveTracker.count, stats.nonInteractiveTracker.duration);
            writeCountAndTime(proto, 1146756268044L, stats.keyguardShownTracker.count, stats.keyguardShownTracker.duration);
            writeCountAndTime(proto, 1146756268045L, stats.keyguardHiddenTracker.count, stats.keyguardHiddenTracker.duration);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Unable to write some interval stats trackers to proto.", e);
        }
        int statsCount = stats.packageStatsObfuscated.size();
        for (int i = 0; i < statsCount; i++) {
            try {
                long token = proto.start(2246267895828L);
                writeUsageStats(proto, stats.beginTime, stats.packageStatsObfuscated.valueAt(i));
                proto.end(token);
            } catch (IllegalArgumentException e2) {
                Slog.e(TAG, "Unable to write some usage stats to proto.", e2);
            }
        }
        int configCount = stats.configurations.size();
        for (int i2 = 0; i2 < configCount; i2++) {
            boolean active = stats.activeConfiguration.equals(stats.configurations.keyAt(i2));
            try {
                long token2 = proto.start(2246267895829L);
                writeConfigStats(proto, stats.beginTime, stats.configurations.valueAt(i2), active);
                proto.end(token2);
            } catch (IllegalArgumentException e3) {
                Slog.e(TAG, "Unable to write some configuration stats to proto.", e3);
            }
        }
        int eventCount = stats.events.size();
        if (eventCount / 5000 >= 1) {
            if (Build.IS_DEBUG_ENABLE) {
                Slog.d(TAG, "UsageStatsProtoV2.write eventCount: " + eventCount);
            }
            if (eventCount / 5000 != lastSize) {
                writeTranLog(eventCount, 0);
                lastSize = eventCount / 5000;
            }
        }
        for (int i3 = 0; i3 < eventCount; i3++) {
            try {
                long token3 = proto.start(2246267895830L);
                writeEvent(proto, stats.beginTime, stats.events.get(i3));
                proto.end(token3);
            } catch (IllegalArgumentException e4) {
                Slog.e(TAG, "Unable to write some events to proto.", e4);
            }
        }
        proto.flush();
    }

    private static void loadPackagesMap(ProtoInputStream proto, SparseArray<ArrayList<String>> tokensToPackagesMap) throws IOException {
        int key = -1;
        ArrayList<String> strings = new ArrayList<>();
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    if (key != -1) {
                        tokensToPackagesMap.put(key, strings);
                        return;
                    }
                    return;
                case 1:
                    int key2 = proto.readInt((long) CompanionMessage.MESSAGE_ID) - 1;
                    key = key2;
                    break;
                case 2:
                    strings.add(proto.readString(2237677961218L));
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void readObfuscatedData(InputStream in, PackagesTokenData packagesTokenData) throws IOException {
        ProtoInputStream proto = new ProtoInputStream(in);
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return;
                case 1:
                    packagesTokenData.counter = proto.readInt((long) CompanionMessage.MESSAGE_ID);
                    break;
                case 2:
                    long token = proto.start(2246267895810L);
                    loadPackagesMap(proto, packagesTokenData.tokensToPackagesMap);
                    proto.end(token);
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeObfuscatedData(OutputStream out, PackagesTokenData packagesTokenData) throws IOException, IllegalArgumentException {
        ProtoOutputStream proto = new ProtoOutputStream(out);
        proto.write(CompanionMessage.MESSAGE_ID, packagesTokenData.counter);
        int mapSize = packagesTokenData.tokensToPackagesMap.size();
        for (int i = 0; i < mapSize; i++) {
            long token = proto.start(2246267895810L);
            int packageToken = packagesTokenData.tokensToPackagesMap.keyAt(i);
            proto.write(CompanionMessage.MESSAGE_ID, packageToken + 1);
            ArrayList<String> strings = packagesTokenData.tokensToPackagesMap.valueAt(i);
            int listSize = strings.size();
            for (int j = 0; j < listSize; j++) {
                proto.write(2237677961218L, strings.get(j));
            }
            proto.end(token);
        }
        proto.flush();
    }

    private static UsageEvents.Event parsePendingEvent(ProtoInputStream proto) throws IOException {
        UsageEvents.Event event = new UsageEvents.Event();
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    switch (event.mEventType) {
                        case 5:
                            if (event.mConfiguration == null) {
                                event.mConfiguration = new Configuration();
                                break;
                            }
                            break;
                        case 8:
                            if (event.mShortcutId == null) {
                                event.mShortcutId = "";
                                break;
                            }
                            break;
                        case 12:
                            if (event.mNotificationChannelId == null) {
                                event.mNotificationChannelId = "";
                                break;
                            }
                            break;
                    }
                    if (event.mPackage == null) {
                        return null;
                    }
                    return event;
                case 1:
                    event.mPackage = proto.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME);
                    break;
                case 2:
                    event.mClass = proto.readString(1138166333442L);
                    break;
                case 3:
                    event.mTimeStamp = proto.readLong(1112396529667L);
                    break;
                case 4:
                    event.mFlags = proto.readInt(1120986464260L);
                    break;
                case 5:
                    event.mEventType = proto.readInt(1120986464261L);
                    break;
                case 6:
                    event.mConfiguration = new Configuration();
                    event.mConfiguration.readFromProto(proto, 1146756268038L);
                    break;
                case 7:
                    event.mShortcutId = proto.readString(1138166333447L);
                    break;
                case 8:
                    event.mBucketAndReason = proto.readInt(1120986464264L);
                    break;
                case 9:
                    event.mNotificationChannelId = proto.readString(1138166333449L);
                    break;
                case 10:
                    event.mInstanceId = proto.readInt(1120986464266L);
                    break;
                case 11:
                    event.mTaskRootPackage = proto.readString(1138166333451L);
                    break;
                case 12:
                    event.mTaskRootClass = proto.readString(1138166333452L);
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void readPendingEvents(InputStream in, LinkedList<UsageEvents.Event> events) throws IOException {
        ProtoInputStream proto = new ProtoInputStream(in);
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return;
                case 23:
                    try {
                        long token = proto.start(2246267895831L);
                        UsageEvents.Event event = parsePendingEvent(proto);
                        proto.end(token);
                        if (event == null) {
                            break;
                        } else {
                            events.add(event);
                            break;
                        }
                    } catch (IOException e) {
                        Slog.e(TAG, "Unable to parse some pending events from proto.", e);
                        break;
                    }
            }
        }
    }

    private static void writePendingEvent(ProtoOutputStream proto, UsageEvents.Event event) throws IllegalArgumentException {
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, event.mPackage);
        if (event.mClass != null) {
            proto.write(1138166333442L, event.mClass);
        }
        proto.write(1112396529667L, event.mTimeStamp);
        proto.write(1120986464260L, event.mFlags);
        proto.write(1120986464261L, event.mEventType);
        proto.write(1120986464266L, event.mInstanceId);
        if (event.mTaskRootPackage != null) {
            proto.write(1138166333451L, event.mTaskRootPackage);
        }
        if (event.mTaskRootClass != null) {
            proto.write(1138166333452L, event.mTaskRootClass);
        }
        switch (event.mEventType) {
            case 5:
                if (event.mConfiguration != null) {
                    event.mConfiguration.dumpDebug(proto, 1146756268038L);
                    return;
                }
                return;
            case 8:
                if (event.mShortcutId != null) {
                    proto.write(1138166333447L, event.mShortcutId);
                    return;
                }
                return;
            case 11:
                if (event.mBucketAndReason != 0) {
                    proto.write(1120986464264L, event.mBucketAndReason);
                    return;
                }
                return;
            case 12:
                if (event.mNotificationChannelId != null) {
                    proto.write(1138166333449L, event.mNotificationChannelId);
                    return;
                }
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writePendingEvents(OutputStream out, LinkedList<UsageEvents.Event> events) throws IOException, IllegalArgumentException {
        ProtoOutputStream proto = new ProtoOutputStream(out);
        int eventCount = events.size();
        for (int i = 0; i < eventCount; i++) {
            try {
                long token = proto.start(2246267895831L);
                writePendingEvent(proto, events.get(i));
                proto.end(token);
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "Unable to write some pending events to proto.", e);
            }
        }
        proto.flush();
    }

    private static Pair<String, Long> parseGlobalComponentUsage(ProtoInputStream proto) throws IOException {
        String packageName = "";
        long time = 0;
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return new Pair<>(packageName, Long.valueOf(time));
                case 1:
                    packageName = proto.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME);
                    break;
                case 2:
                    time = proto.readLong(1112396529666L);
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void readGlobalComponentUsage(InputStream in, Map<String, Long> lastTimeComponentUsedGlobal) throws IOException {
        ProtoInputStream proto = new ProtoInputStream(in);
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    return;
                case 24:
                    try {
                        long token = proto.start(2246267895832L);
                        Pair<String, Long> usage = parseGlobalComponentUsage(proto);
                        proto.end(token);
                        if (!((String) usage.first).isEmpty() && ((Long) usage.second).longValue() > 0) {
                            lastTimeComponentUsedGlobal.put((String) usage.first, (Long) usage.second);
                            break;
                        }
                    } catch (IOException e) {
                        Slog.e(TAG, "Unable to parse some package usage from proto.", e);
                        break;
                    }
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeGlobalComponentUsage(OutputStream out, Map<String, Long> lastTimeComponentUsedGlobal) {
        ProtoOutputStream proto = new ProtoOutputStream(out);
        Map.Entry<String, Long>[] entries = (Map.Entry[]) lastTimeComponentUsedGlobal.entrySet().toArray();
        int size = entries.length;
        for (int i = 0; i < size; i++) {
            if (entries[i].getValue().longValue() > 0) {
                long token = proto.start(2246267895832L);
                proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, entries[i].getKey());
                proto.write(1112396529666L, entries[i].getValue().longValue());
                proto.end(token);
            }
        }
    }

    public static void writeTranLog(int writeSize, int readSize) {
    }
}
