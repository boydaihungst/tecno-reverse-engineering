package com.transsion.hubcore.server.lowStorage;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.storage.VolumeInfo;
import com.android.server.am.AppErrorResultWrap;
import com.transsion.hubcore.server.lowStorage.ITranLowStorageManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranLowStorageManager {
    public static final TranClassInfo<ITranLowStorageManager> sClassInfo = new TranClassInfo<>("com.transsion.hubcore.server.lowStorage.TranLowStorageManagerImpl", ITranLowStorageManager.class, new Supplier() { // from class: com.transsion.hubcore.server.lowStorage.ITranLowStorageManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranLowStorageManager.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranLowStorageManager {
    }

    static ITranLowStorageManager Instance() {
        return (ITranLowStorageManager) sClassInfo.getImpl();
    }

    default void init(Context context, int userId) {
    }

    default void init(Context context, Handler handler, int userId) {
    }

    default boolean getLowStorageManagerEnable() {
        return true;
    }

    default boolean checkLowStorageNoStart(String packageName, int userid) {
        return false;
    }

    default void setDefault(String roleName, String packageName, int userId) {
    }

    default void setDefaultHome(IntentFilter filter, String packageName, int userId) {
    }

    default int checkStorageLevel(String FsUUID, long usableBytes, int state, long lowLevel) {
        return 0;
    }

    default void showDialog(String keyName) {
    }

    default void registerLowStorage() {
    }

    default void checkLevel(long usableBytes, long lowBytes, VolumeInfo vol) {
    }

    default Notification buildNotification(Intent lowMemIntent, CharSequence title, String tv_notification_channel_id) {
        return null;
    }

    default void showDialog(Handler uiHandler, AppErrorResultWrap result) {
    }

    default void startDataFullReport(String packageName, String noStartType) {
    }
}
