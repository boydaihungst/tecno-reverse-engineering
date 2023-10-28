package com.android.server.wm;

import com.android.server.wm.ITaskFragmentOrganizerControllerLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface ITaskFragmentOrganizerControllerLice {
    public static final LiceInfo<ITaskFragmentOrganizerControllerLice> LICE_INFO = new LiceInfo<>("com.transsion.server.wm.TaskFragmentOrganizerControllerLice", ITaskFragmentOrganizerControllerLice.class, new Supplier() { // from class: com.android.server.wm.ITaskFragmentOrganizerControllerLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITaskFragmentOrganizerControllerLice.Default();
        }
    });

    /* loaded from: classes2.dex */
    public static class Default implements ITaskFragmentOrganizerControllerLice {
    }

    static ITaskFragmentOrganizerControllerLice instance() {
        return (ITaskFragmentOrganizerControllerLice) LICE_INFO.getImpl();
    }

    default void onTaskFragmentAppeared(Object taskFragment) {
    }
}
