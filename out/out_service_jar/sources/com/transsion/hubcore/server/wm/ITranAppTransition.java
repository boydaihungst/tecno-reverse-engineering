package com.transsion.hubcore.server.wm;

import com.android.server.wm.DisplayPolicy;
import com.transsion.hubcore.server.wm.ITranAppTransition;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAppTransition {
    public static final TranClassInfo<ITranAppTransition> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranAppTransitionImpl", ITranAppTransition.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranAppTransition$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAppTransition.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAppTransition {
    }

    static ITranAppTransition Instance() {
        return (ITranAppTransition) classInfo.getImpl();
    }

    default int getAnimAttr(int transit) {
        return 0;
    }

    default int getWallpaperUnlockAnimAttr(int animAttr, boolean enter) {
        return 0;
    }

    default int hookAppTransitSuccess(DisplayPolicy displayPolicy) {
        return 0;
    }

    default boolean isThunderbackCloseTransition(ArrayList<Integer> mNextAppTransitionRequests) {
        return false;
    }

    default boolean isThunderbackOpenTransition(ArrayList<Integer> mNextAppTransitionRequests) {
        return false;
    }
}
