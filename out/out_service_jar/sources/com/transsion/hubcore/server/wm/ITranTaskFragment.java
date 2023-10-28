package com.transsion.hubcore.server.wm;

import com.android.server.wm.ActivityRecord;
import com.transsion.hubcore.server.wm.ITranTaskFragment;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranTaskFragment {
    public static final TranClassInfo<ITranTaskFragment> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranTaskFragmentImpl", ITranTaskFragment.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranTaskFragment$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranTaskFragment.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranTaskFragment {
    }

    static ITranTaskFragment Instance() {
        return (ITranTaskFragment) classInfo.getImpl();
    }

    default void hookResumeTopActivity(ActivityRecord last, ActivityRecord next, boolean pausing) {
    }

    default void hookResumeTopActivityEnd(ActivityRecord next) {
    }
}
