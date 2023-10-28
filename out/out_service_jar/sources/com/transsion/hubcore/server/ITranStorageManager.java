package com.transsion.hubcore.server;

import android.content.Context;
import android.content.Intent;
import android.os.storage.VolumeInfo;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.UUID;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranStorageManager {
    public static final TranClassInfo<ITranStorageManager> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.TranStorageManagerImpl", ITranStorageManager.class, new Supplier() { // from class: com.transsion.hubcore.server.ITranStorageManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranStorageManager.lambda$static$0();
        }
    });

    static /* synthetic */ ITranStorageManager lambda$static$0() {
        return new ITranStorageManager() { // from class: com.transsion.hubcore.server.ITranStorageManager.1
        };
    }

    static ITranStorageManager Instance() {
        return (ITranStorageManager) classInfo.getImpl();
    }

    default void onDeviceStorageMonitorServiceConstruct(Context context) {
    }

    default void onEnterLowStorage(Context context, UUID uuid, Intent lowIntent, CharSequence title) {
    }

    default void onCheckStorageLevel(Context context, VolumeInfo vol, long usableBytes, long lowBytes) {
    }
}
