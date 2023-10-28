package com.transsion.hubcore.server.usage;

import android.app.usage.EventList;
import android.app.usage.StorageStats;
import android.content.ComponentName;
import com.android.server.pm.Installer;
import com.transsion.hubcore.server.usage.ITranUsageStatsService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranUsageStatsService {
    public static final boolean DEBUG = true;
    public static final String TAG = "ITranUsageStatsService";
    public static final TranClassInfo<ITranUsageStatsService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.usage.TranUsageStatsServiceImpl", ITranUsageStatsService.class, new Supplier() { // from class: com.transsion.hubcore.server.usage.ITranUsageStatsService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranUsageStatsService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranUsageStatsService {
    }

    static ITranUsageStatsService Instance() {
        return (ITranUsageStatsService) classInfo.getImpl();
    }

    default void onReportEvent(ComponentName component, int userId, int eventType, int instanceId, ComponentName taskRoot) {
    }

    default void addEventlog(EventList events) {
    }

    default <T> void addQueryUsageStatslog(ArrayList<T> results) {
    }

    default void addWritelog(int eventCount) {
    }

    default StorageStats adjustSystemStatsForUid(StorageStats storageStats, String TAG2, String volumeUuid, int uid, int userId, int appId, String[] packageNames, String[] codePaths, long[] ceDataInodes, int flag, Installer mInstaller) {
        return storageStats;
    }

    default void setsysCodePaths(int appId, String path, String[] sCodePaths) {
    }

    default String[] getsysCodePaths() {
        return new String[0];
    }

    default void writeOrThrowException(Exception e) throws RuntimeException {
    }
}
