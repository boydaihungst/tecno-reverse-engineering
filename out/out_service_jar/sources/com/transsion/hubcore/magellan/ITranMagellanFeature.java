package com.transsion.hubcore.magellan;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranMagellanFeature {
    public static final int AUDIO_SCENE = 3;
    public static final int FAST_SLIDE_SCENE = 5;
    public static final int IDLE_SCENE = 7;
    public static final int INPUT_METHOD_SCENE = 1;
    public static final int NAVI_SCENE = 0;
    public static final int PICTURE_SCENE = 4;
    public static final int SLOW_SLIDE_SCENE = 6;
    public static final int VIDEO_SCENE = 2;
    public static final TranClassInfo<ITranMagellanFeature> classInfo = new TranClassInfo<>("com.transsion.hubcore.magellan.TranMagellanFeatureImpl", ITranMagellanFeature.class, new Supplier() { // from class: com.transsion.hubcore.magellan.ITranMagellanFeature$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranMagellanFeature.lambda$static$0();
        }
    });

    static /* synthetic */ ITranMagellanFeature lambda$static$0() {
        return new ITranMagellanFeature() { // from class: com.transsion.hubcore.magellan.ITranMagellanFeature.1
        };
    }

    static ITranMagellanFeature instance() {
        return (ITranMagellanFeature) classInfo.getImpl();
    }

    default void sceneTrack(int sceneType, boolean begin) {
    }

    default boolean isSupportInputMethod() {
        return false;
    }

    default boolean isSupportNavigation() {
        return false;
    }

    default boolean isSupportVideo() {
        return false;
    }

    default boolean isSupportAudio() {
        return false;
    }
}
