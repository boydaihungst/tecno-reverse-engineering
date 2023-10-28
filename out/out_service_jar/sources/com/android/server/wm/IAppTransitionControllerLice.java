package com.android.server.wm;

import com.android.server.wm.IAppTransitionControllerLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface IAppTransitionControllerLice {
    public static final LiceInfo<IAppTransitionControllerLice> LICE_INFO = new LiceInfo<>("com.transsion.server.wm.AppTransitionControllerLice", IAppTransitionControllerLice.class, new Supplier() { // from class: com.android.server.wm.IAppTransitionControllerLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IAppTransitionControllerLice.Default();
        }
    });

    /* loaded from: classes2.dex */
    public static class Default implements IAppTransitionControllerLice {
    }

    static IAppTransitionControllerLice instance() {
        return (IAppTransitionControllerLice) LICE_INFO.getImpl();
    }

    default int onGetTransitCompatType(AppTransition appTransition, int openingType, int closingType, ActivityRecord topOpeningApp, ActivityRecord topClosingApp) {
        return -1;
    }
}
