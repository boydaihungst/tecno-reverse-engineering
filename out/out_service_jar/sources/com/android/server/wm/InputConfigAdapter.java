package com.android.server.wm;

import java.util.List;
/* loaded from: classes2.dex */
class InputConfigAdapter {
    private static final List<FlagMapping> INPUT_FEATURE_TO_CONFIG_MAP;
    private static final int INPUT_FEATURE_TO_CONFIG_MASK;
    private static final List<FlagMapping> LAYOUT_PARAM_FLAG_TO_CONFIG_MAP;
    private static final int LAYOUT_PARAM_FLAG_TO_CONFIG_MASK;

    private InputConfigAdapter() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class FlagMapping {
        final int mFlag;
        final int mInputConfig;
        final boolean mInverted;

        FlagMapping(int flag, int inputConfig, boolean inverted) {
            this.mFlag = flag;
            this.mInputConfig = inputConfig;
            this.mInverted = inverted;
        }
    }

    static {
        List<FlagMapping> of = List.of(new FlagMapping(1, 1, false), new FlagMapping(2, 2048, false), new FlagMapping(4, 16384, false));
        INPUT_FEATURE_TO_CONFIG_MAP = of;
        INPUT_FEATURE_TO_CONFIG_MASK = computeMask(of);
        List<FlagMapping> of2 = List.of(new FlagMapping(16, 8, false), new FlagMapping(8388608, 16, true), new FlagMapping(262144, 512, false), new FlagMapping(536870912, 1024, false));
        LAYOUT_PARAM_FLAG_TO_CONFIG_MAP = of2;
        LAYOUT_PARAM_FLAG_TO_CONFIG_MASK = computeMask(of2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getMask() {
        return LAYOUT_PARAM_FLAG_TO_CONFIG_MASK | INPUT_FEATURE_TO_CONFIG_MASK | 64;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getInputConfigFromWindowParams(int type, int flags, int inputFeatures) {
        return (type == 2013 ? 64 : 0) | applyMapping(flags, LAYOUT_PARAM_FLAG_TO_CONFIG_MAP) | applyMapping(inputFeatures, INPUT_FEATURE_TO_CONFIG_MAP);
    }

    private static int applyMapping(int flags, List<FlagMapping> flagToConfigMap) {
        int inputConfig = 0;
        for (FlagMapping mapping : flagToConfigMap) {
            boolean flagSet = (mapping.mFlag & flags) != 0;
            if (flagSet != mapping.mInverted) {
                inputConfig |= mapping.mInputConfig;
            }
        }
        return inputConfig;
    }

    private static int computeMask(List<FlagMapping> flagToConfigMap) {
        int mask = 0;
        for (FlagMapping mapping : flagToConfigMap) {
            mask |= mapping.mInputConfig;
        }
        return mask;
    }
}
