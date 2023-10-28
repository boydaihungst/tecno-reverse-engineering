package com.android.server.wm;

import com.android.server.wm.IWindowOrganizerControllerLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface IWindowOrganizerControllerLice {
    public static final LiceInfo<IWindowOrganizerControllerLice> LICE_INFO = new LiceInfo<>("com.transsion.server.wm.WindowOrganizerControllerLice", IWindowOrganizerControllerLice.class, new Supplier() { // from class: com.android.server.wm.IWindowOrganizerControllerLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IWindowOrganizerControllerLice.Default();
        }
    });

    /* loaded from: classes2.dex */
    public static class Default implements IWindowOrganizerControllerLice {
    }

    static IWindowOrganizerControllerLice instance() {
        return (IWindowOrganizerControllerLice) LICE_INFO.getImpl();
    }

    default boolean onCreateTaskFragment(ActivityRecord activityRecord, int hookFlag) {
        return false;
    }
}
