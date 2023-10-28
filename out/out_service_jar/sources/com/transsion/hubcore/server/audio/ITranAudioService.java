package com.transsion.hubcore.server.audio;

import android.content.Context;
import com.transsion.hubcore.server.audio.ITranAudioService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAudioService {
    public static final TranClassInfo<ITranAudioService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.audio.TranAudioServiceImpl", ITranAudioService.class, new Supplier() { // from class: com.transsion.hubcore.server.audio.ITranAudioService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAudioService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAudioService {
    }

    static ITranAudioService Instance() {
        return (ITranAudioService) classInfo.getImpl();
    }

    default void notifyModeChange(int mode, String callingPackage) {
    }

    default boolean isTheftAlertRinging(Context context) {
        return false;
    }
}
