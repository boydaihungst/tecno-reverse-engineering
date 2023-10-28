package com.transsion.hubcore.server.utils;

import com.transsion.hubcore.server.utils.ITranMediaUtils;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranMediaUtils {
    public static final TranClassInfo<ITranMediaUtils> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.utils.TranMediaUtilsImpl", ITranMediaUtils.class, new Supplier() { // from class: com.transsion.hubcore.server.utils.ITranMediaUtils$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranMediaUtils.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranMediaUtils {
    }

    static ITranMediaUtils Instance() {
        return (ITranMediaUtils) classInfo.getImpl();
    }

    default void hookAudioPlayerChanged(int uid, int pid, int playId, int oldState, int newState) {
    }

    default void hookAudioPlayerChanged(int uid, int pid, int playId, int playState, String aPConfString) {
    }

    default void hookAudioRecordChanged(int uid, String packageName, int event) {
    }
}
