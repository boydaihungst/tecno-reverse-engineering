package com.transsion.hubcore.media;

import com.transsion.hubcore.media.ITranAudioSystemCallJni;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranAudioSystemCallJni {
    public static final TranClassInfo<ITranAudioSystemCallJni> classInfo = new TranClassInfo<>("com.transsion.hubcore.media.TranAudioSystemCallJniImpl", ITranAudioSystemCallJni.class, new Supplier() { // from class: com.transsion.hubcore.media.ITranAudioSystemCallJni$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAudioSystemCallJni.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranAudioSystemCallJni {
    }

    static ITranAudioSystemCallJni Instance() {
        return classInfo.getImpl();
    }

    default void setCurrentAudioTrackMuteByPid(int pid, int multiWindowId) {
    }

    default void removeCurrentAudioTrackMuteByPid(int pid, int multiWindowId) {
    }

    default void clearCurrentAudioTrackMute(int multiWindowId) {
    }

    default void setMuteState(boolean state, int multiWindowId) {
    }
}
