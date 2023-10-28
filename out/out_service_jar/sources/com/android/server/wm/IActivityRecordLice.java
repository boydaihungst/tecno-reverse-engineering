package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.pm.ActivityInfo;
import com.android.server.wm.IActivityRecordLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface IActivityRecordLice {
    public static final LiceInfo<IActivityRecordLice> LICE_INFO = new LiceInfo<>("com.transsion.server.wm.ActivityRecordLice", IActivityRecordLice.class, new Supplier() { // from class: com.android.server.wm.IActivityRecordLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IActivityRecordLice.Default();
        }
    });

    /* loaded from: classes2.dex */
    public static class Default implements IActivityRecordLice {
    }

    static IActivityRecordLice instance() {
        return (IActivityRecordLice) LICE_INFO.getImpl();
    }

    default void onConstruct(ActivityRecord record, ActivityOptions options, ActivityInfo info, ActivityRecord sourceRecord, Context context) {
    }

    default boolean onIsResizeable(ActivityRecord record) {
        return false;
    }
}
