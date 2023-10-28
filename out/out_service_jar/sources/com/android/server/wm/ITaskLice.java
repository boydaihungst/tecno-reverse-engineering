package com.android.server.wm;

import android.app.TaskInfo;
import com.android.server.wm.ITaskLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface ITaskLice {
    public static final LiceInfo<ITaskLice> LICE_INFO = new LiceInfo<>("com.transsion.server.wm.TaskLice", ITaskLice.class, new Supplier() { // from class: com.android.server.wm.ITaskLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITaskLice.Default();
        }
    });

    /* loaded from: classes2.dex */
    public static class Default implements ITaskLice {
    }

    static ITaskLice instance() {
        return (ITaskLice) LICE_INFO.getImpl();
    }

    default void fillTaskInfo(TaskInfo info) {
    }
}
