package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.content.res.Configuration;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import com.android.server.usage.IntervalStats;
import defpackage.CompanionAppsPermissions;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
final class UsageStatsProto {
    private static String TAG = "UsageStatsProto";

    private UsageStatsProto() {
    }

    private static List<String> readStringPool(ProtoInputStream proto) throws IOException {
        List<String> stringPool;
        long token = proto.start(1146756268034L);
        if (proto.nextField((long) CompanionMessage.MESSAGE_ID)) {
            stringPool = new ArrayList<>(proto.readInt((long) CompanionMessage.MESSAGE_ID));
        } else {
            stringPool = new ArrayList<>();
        }
        while (proto.nextField() != -1) {
            switch (proto.getFieldNumber()) {
                case 2:
                    stringPool.add(proto.readString(2237677961218L));
                    break;
            }
        }
        proto.end(token);
        return stringPool;
    }

    private static void loadUsageStats(ProtoInputStream proto, long fieldId, IntervalStats statsOut, List<String> stringPool) throws IOException {
        UsageStats stats;
        long token = proto.start(fieldId);
        if (proto.nextField(1120986464258L)) {
            stats = statsOut.getOrCreateUsageStats(stringPool.get(proto.readInt(1120986464258L) - 1));
        } else if (proto.nextField((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME)) {
            stats = statsOut.getOrCreateUsageStats(proto.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME));
        } else {
            stats = new UsageStats();
        }
        while (proto.nextField() != -1) {
            switch (proto.getFieldNumber()) {
                case 1:
                    UsageStats tempPackage = statsOut.getOrCreateUsageStats(proto.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME));
                    tempPackage.mLastTimeUsed = stats.mLastTimeUsed;
                    tempPackage.mTotalTimeInForeground = stats.mTotalTimeInForeground;
                    tempPackage.mLastEvent = stats.mLastEvent;
                    tempPackage.mAppLaunchCount = stats.mAppLaunchCount;
                    stats = tempPackage;
                    break;
                case 2:
                    UsageStats tempPackageIndex = statsOut.getOrCreateUsageStats(stringPool.get(proto.readInt(1120986464258L) - 1));
                    tempPackageIndex.mLastTimeUsed = stats.mLastTimeUsed;
                    tempPackageIndex.mTotalTimeInForeground = stats.mTotalTimeInForeground;
                    tempPackageIndex.mLastEvent = stats.mLastEvent;
                    tempPackageIndex.mAppLaunchCount = stats.mAppLaunchCount;
                    stats = tempPackageIndex;
                    break;
                case 3:
                    stats.mLastTimeUsed = statsOut.beginTime + proto.readLong(1112396529667L);
                    break;
                case 4:
                    stats.mTotalTimeInForeground = proto.readLong(1112396529668L);
                    break;
                case 5:
                    stats.mLastEvent = proto.readInt(1120986464261L);
                    break;
                case 6:
                    stats.mAppLaunchCount = proto.readInt(1120986464262L);
                    break;
                case 7:
                    try {
                        long chooserToken = proto.start(2246267895815L);
                        loadChooserCounts(proto, stats);
                        proto.end(chooserToken);
                        break;
                    } catch (IOException e) {
                        Slog.e(TAG, "Unable to read chooser counts for " + stats.mPackageName, e);
                        break;
                    }
                case 8:
                    stats.mLastTimeForegroundServiceUsed = statsOut.beginTime + proto.readLong(1112396529672L);
                    break;
                case 9:
                    stats.mTotalTimeForegroundServiceUsed = proto.readLong(1112396529673L);
                    break;
                case 10:
                    stats.mLastTimeVisible = statsOut.beginTime + proto.readLong(1112396529674L);
                    break;
                case 11:
                    stats.mTotalTimeVisible = proto.readLong(1112396529675L);
                    break;
                case 12:
                    stats.mLastTimeComponentUsed = statsOut.beginTime + proto.readLong(1112396529676L);
                    break;
            }
        }
        proto.end(token);
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
        ArrayMap<String, Integer> counts;
        if (usageStats.mChooserCounts == null) {
            usageStats.mChooserCounts = new ArrayMap();
        }
        String action = null;
        if (proto.nextField((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME)) {
            action = proto.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME);
            counts = (ArrayMap) usageStats.mChooserCounts.get(action);
            if (counts == null) {
                counts = new ArrayMap<>();
                usageStats.mChooserCounts.put(action, counts);
            }
        } else {
            counts = new ArrayMap<>();
        }
        while (true) {
            switch (proto.nextField()) {
                case 1:
                    action = proto.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME);
                    usageStats.mChooserCounts.put(action, counts);
                    break;
                case 3:
                    long token = proto.start(2246267895811L);
                    loadCountsForAction(proto, counts);
                    proto.end(token);
                    break;
            }
        }
        if (action == null) {
            usageStats.mChooserCounts.put("", counts);
        }
    }

    private static void loadCountsForAction(ProtoInputStream proto, ArrayMap<String, Integer> counts) throws IOException {
        String category = null;
        int count = 0;
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    if (category == null) {
                        counts.put("", Integer.valueOf(count));
                        return;
                    } else {
                        counts.put(category, Integer.valueOf(count));
                        return;
                    }
                case 1:
                    category = proto.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME);
                    break;
                case 3:
                    count = proto.readInt(1120986464259L);
                    break;
            }
        }
    }

    private static void loadConfigStats(ProtoInputStream proto, long fieldId, IntervalStats statsOut) throws IOException {
        ConfigurationStats configStats;
        long token = proto.start(fieldId);
        boolean configActive = false;
        Configuration config = new Configuration();
        if (proto.nextField(1146756268033L)) {
            config.readFromProto(proto, 1146756268033L);
            configStats = statsOut.getOrCreateConfigurationStats(config);
        } else {
            configStats = new ConfigurationStats();
        }
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    if (configActive) {
                        statsOut.activeConfiguration = configStats.mConfiguration;
                    }
                    proto.end(token);
                    return;
                case 1:
                    config.readFromProto(proto, 1146756268033L);
                    ConfigurationStats temp = statsOut.getOrCreateConfigurationStats(config);
                    temp.mLastTimeActive = configStats.mLastTimeActive;
                    temp.mTotalTimeActive = configStats.mTotalTimeActive;
                    temp.mActivationCount = configStats.mActivationCount;
                    configStats = temp;
                    break;
                case 2:
                    configStats.mLastTimeActive = statsOut.beginTime + proto.readLong(1112396529666L);
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

    private static void loadEvent(ProtoInputStream proto, long fieldId, IntervalStats statsOut, List<String> stringPool) throws IOException {
        long token = proto.start(fieldId);
        UsageEvents.Event event = statsOut.buildEvent(proto, stringPool);
        proto.end(token);
        if (event.mPackage == null) {
            throw new ProtocolException("no package field present");
        }
        statsOut.events.insert(event);
    }

    private static void writeStringPool(ProtoOutputStream proto, IntervalStats stats) throws IllegalArgumentException {
        long token = proto.start(1146756268034L);
        int size = stats.mStringCache.size();
        proto.write(CompanionMessage.MESSAGE_ID, size);
        for (int i = 0; i < size; i++) {
            proto.write(2237677961218L, stats.mStringCache.valueAt(i));
        }
        proto.end(token);
    }

    private static void writeUsageStats(ProtoOutputStream proto, long fieldId, IntervalStats stats, UsageStats usageStats) throws IllegalArgumentException {
        long token = proto.start(fieldId);
        int packageIndex = stats.mStringCache.indexOf(usageStats.mPackageName);
        if (packageIndex >= 0) {
            proto.write(1120986464258L, packageIndex + 1);
        } else {
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, usageStats.mPackageName);
        }
        UsageStatsProtoV2.writeOffsetTimestamp(proto, 1112396529667L, usageStats.mLastTimeUsed, stats.beginTime);
        proto.write(1112396529668L, usageStats.mTotalTimeInForeground);
        proto.write(1120986464261L, usageStats.mLastEvent);
        UsageStatsProtoV2.writeOffsetTimestamp(proto, 1112396529672L, usageStats.mLastTimeForegroundServiceUsed, stats.beginTime);
        proto.write(1112396529673L, usageStats.mTotalTimeForegroundServiceUsed);
        UsageStatsProtoV2.writeOffsetTimestamp(proto, 1112396529674L, usageStats.mLastTimeVisible, stats.beginTime);
        proto.write(1112396529675L, usageStats.mTotalTimeVisible);
        UsageStatsProtoV2.writeOffsetTimestamp(proto, 1112396529676L, usageStats.mLastTimeComponentUsed, stats.beginTime);
        proto.write(1120986464262L, usageStats.mAppLaunchCount);
        try {
            writeChooserCounts(proto, usageStats);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Unable to write chooser counts for " + usageStats.mPackageName, e);
        }
        proto.end(token);
    }

    private static void writeCountAndTime(ProtoOutputStream proto, long fieldId, int count, long time) throws IllegalArgumentException {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, count);
        proto.write(1112396529666L, time);
        proto.end(token);
    }

    private static void writeChooserCounts(ProtoOutputStream proto, UsageStats usageStats) throws IllegalArgumentException {
        if (usageStats == null || usageStats.mChooserCounts == null || usageStats.mChooserCounts.keySet().isEmpty()) {
            return;
        }
        int chooserCountSize = usageStats.mChooserCounts.size();
        for (int i = 0; i < chooserCountSize; i++) {
            String action = (String) usageStats.mChooserCounts.keyAt(i);
            ArrayMap<String, Integer> counts = (ArrayMap) usageStats.mChooserCounts.valueAt(i);
            if (action != null && counts != null && !counts.isEmpty()) {
                long token = proto.start(2246267895815L);
                proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, action);
                writeCountsForAction(proto, counts);
                proto.end(token);
            }
        }
    }

    private static void writeCountsForAction(ProtoOutputStream proto, ArrayMap<String, Integer> counts) throws IllegalArgumentException {
        int countsSize = counts.size();
        for (int i = 0; i < countsSize; i++) {
            String key = counts.keyAt(i);
            int count = counts.valueAt(i).intValue();
            if (count > 0) {
                long token = proto.start(2246267895811L);
                proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, key);
                proto.write(1120986464259L, count);
                proto.end(token);
            }
        }
    }

    private static void writeConfigStats(ProtoOutputStream proto, long fieldId, IntervalStats stats, ConfigurationStats configStats, boolean isActive) throws IllegalArgumentException {
        long token = proto.start(fieldId);
        configStats.mConfiguration.dumpDebug(proto, 1146756268033L);
        UsageStatsProtoV2.writeOffsetTimestamp(proto, 1112396529666L, configStats.mLastTimeActive, stats.beginTime);
        proto.write(1112396529667L, configStats.mTotalTimeActive);
        proto.write(1120986464260L, configStats.mActivationCount);
        proto.write(1133871366149L, isActive);
        proto.end(token);
    }

    private static void writeEvent(ProtoOutputStream proto, long fieldId, IntervalStats stats, UsageEvents.Event event) throws IllegalArgumentException {
        int locusIdIndex;
        int taskRootClassIndex;
        int taskRootPackageIndex;
        long token = proto.start(fieldId);
        int packageIndex = stats.mStringCache.indexOf(event.mPackage);
        if (packageIndex >= 0) {
            proto.write(1120986464258L, packageIndex + 1);
        } else {
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, event.mPackage);
        }
        if (event.mClass != null) {
            int classIndex = stats.mStringCache.indexOf(event.mClass);
            if (classIndex >= 0) {
                proto.write(1120986464260L, classIndex + 1);
            } else {
                proto.write(1138166333443L, event.mClass);
            }
        }
        UsageStatsProtoV2.writeOffsetTimestamp(proto, 1112396529669L, event.mTimeStamp, stats.beginTime);
        proto.write(1120986464262L, event.mFlags);
        proto.write(1120986464263L, event.mEventType);
        proto.write(1120986464270L, event.mInstanceId);
        if (event.mTaskRootPackage != null && (taskRootPackageIndex = stats.mStringCache.indexOf(event.mTaskRootPackage)) >= 0) {
            proto.write(1120986464271L, taskRootPackageIndex + 1);
        }
        if (event.mTaskRootClass != null && (taskRootClassIndex = stats.mStringCache.indexOf(event.mTaskRootClass)) >= 0) {
            proto.write(1120986464272L, taskRootClassIndex + 1);
        }
        switch (event.mEventType) {
            case 5:
                if (event.mConfiguration != null) {
                    event.mConfiguration.dumpDebug(proto, 1146756268040L);
                    break;
                }
                break;
            case 8:
                if (event.mShortcutId != null) {
                    proto.write(1138166333449L, event.mShortcutId);
                    break;
                }
                break;
            case 11:
                if (event.mBucketAndReason != 0) {
                    proto.write(1120986464267L, event.mBucketAndReason);
                    break;
                }
                break;
            case 12:
                if (event.mNotificationChannelId != null) {
                    int channelIndex = stats.mStringCache.indexOf(event.mNotificationChannelId);
                    if (channelIndex >= 0) {
                        proto.write(1120986464269L, channelIndex + 1);
                        break;
                    } else {
                        proto.write(1138166333452L, event.mNotificationChannelId);
                        break;
                    }
                }
                break;
            case 30:
                if (event.mLocusId != null && (locusIdIndex = stats.mStringCache.indexOf(event.mLocusId)) >= 0) {
                    proto.write(1120986464273L, locusIdIndex + 1);
                    break;
                }
                break;
        }
        proto.end(token);
    }

    public static void read(InputStream in, IntervalStats statsOut) throws IOException {
        ProtoInputStream proto = new ProtoInputStream(in);
        List<String> stringPool = null;
        statsOut.packageStats.clear();
        statsOut.configurations.clear();
        statsOut.activeConfiguration = null;
        statsOut.events.clear();
        while (true) {
            switch (proto.nextField()) {
                case -1:
                    statsOut.upgradeIfNeeded();
                    return;
                case 1:
                    statsOut.endTime = statsOut.beginTime + proto.readLong(1112396529665L);
                    break;
                case 2:
                    try {
                        stringPool = readStringPool(proto);
                        statsOut.mStringCache.addAll(stringPool);
                        break;
                    } catch (IOException e) {
                        Slog.e(TAG, "Unable to read string pool from proto.", e);
                        break;
                    }
                case 3:
                    statsOut.majorVersion = proto.readInt(1120986464259L);
                    break;
                case 4:
                    statsOut.minorVersion = proto.readInt(1120986464260L);
                    break;
                case 10:
                    loadCountAndTime(proto, 1146756268042L, statsOut.interactiveTracker);
                    break;
                case 11:
                    loadCountAndTime(proto, 1146756268043L, statsOut.nonInteractiveTracker);
                    break;
                case 12:
                    loadCountAndTime(proto, 1146756268044L, statsOut.keyguardShownTracker);
                    break;
                case 13:
                    loadCountAndTime(proto, 1146756268045L, statsOut.keyguardHiddenTracker);
                    break;
                case 20:
                    try {
                        loadUsageStats(proto, 2246267895828L, statsOut, stringPool);
                        break;
                    } catch (IOException e2) {
                        Slog.e(TAG, "Unable to read some usage stats from proto.", e2);
                        break;
                    }
                case 21:
                    try {
                        loadConfigStats(proto, 2246267895829L, statsOut);
                        break;
                    } catch (IOException e3) {
                        Slog.e(TAG, "Unable to read some configuration stats from proto.", e3);
                        break;
                    }
                case 22:
                    try {
                        loadEvent(proto, 2246267895830L, statsOut, stringPool);
                        break;
                    } catch (IOException e4) {
                        Slog.e(TAG, "Unable to read some events from proto.", e4);
                        break;
                    }
            }
        }
    }

    public static void write(OutputStream out, IntervalStats stats) throws IOException, IllegalArgumentException {
        ProtoOutputStream proto = new ProtoOutputStream(out);
        proto.write(1112396529665L, UsageStatsProtoV2.getOffsetTimestamp(stats.endTime, stats.beginTime));
        proto.write(1120986464259L, stats.majorVersion);
        proto.write(1120986464260L, stats.minorVersion);
        try {
            writeStringPool(proto, stats);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Unable to write string pool to proto.", e);
        }
        try {
            writeCountAndTime(proto, 1146756268042L, stats.interactiveTracker.count, stats.interactiveTracker.duration);
            writeCountAndTime(proto, 1146756268043L, stats.nonInteractiveTracker.count, stats.nonInteractiveTracker.duration);
            writeCountAndTime(proto, 1146756268044L, stats.keyguardShownTracker.count, stats.keyguardShownTracker.duration);
            writeCountAndTime(proto, 1146756268045L, stats.keyguardHiddenTracker.count, stats.keyguardHiddenTracker.duration);
        } catch (IllegalArgumentException e2) {
            Slog.e(TAG, "Unable to write some interval stats trackers to proto.", e2);
        }
        int statsCount = stats.packageStats.size();
        for (int i = 0; i < statsCount; i++) {
            try {
                writeUsageStats(proto, 2246267895828L, stats, stats.packageStats.valueAt(i));
            } catch (IllegalArgumentException e3) {
                Slog.e(TAG, "Unable to write some usage stats to proto.", e3);
            }
        }
        int configCount = stats.configurations.size();
        for (int i2 = 0; i2 < configCount; i2++) {
            boolean active = stats.activeConfiguration.equals(stats.configurations.keyAt(i2));
            try {
                writeConfigStats(proto, 2246267895829L, stats, stats.configurations.valueAt(i2), active);
            } catch (IllegalArgumentException e4) {
                Slog.e(TAG, "Unable to write some configuration stats to proto.", e4);
            }
        }
        int eventCount = stats.events.size();
        for (int i3 = 0; i3 < eventCount; i3++) {
            try {
                writeEvent(proto, 2246267895830L, stats, stats.events.get(i3));
            } catch (IllegalArgumentException e5) {
                Slog.e(TAG, "Unable to write some events to proto.", e5);
            }
        }
        proto.flush();
    }
}
