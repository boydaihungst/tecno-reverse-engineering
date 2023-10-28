package com.android.server.wm;

import com.android.server.wm.ITaskFragmentLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface ITaskFragmentLice {
    public static final LiceInfo<ITaskFragmentLice> LICE_INFO = new LiceInfo<>("com.transsion.server.wm.TaskFragmentLice", ITaskFragmentLice.class, new Supplier() { // from class: com.android.server.wm.ITaskFragmentLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITaskFragmentLice.Default();
        }
    });

    /* loaded from: classes2.dex */
    public static class Default implements ITaskFragmentLice {
    }

    static ITaskFragmentLice instance() {
        return (ITaskFragmentLice) LICE_INFO.getImpl();
    }

    default boolean onIsAllowedToEmbedActivityInTrustedMode(ActivityRecord a, int uid, int hookFlag) {
        return false;
    }

    default void onAddChild(Object taskFragment, ActivityRecord addRecord) {
    }

    default void onRemoveChild(Object taskFragment, ActivityRecord removeRecord) {
    }
}
