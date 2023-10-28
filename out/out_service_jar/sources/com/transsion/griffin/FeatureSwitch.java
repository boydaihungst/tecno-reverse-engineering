package com.transsion.griffin;

import android.os.SystemProperties;
/* loaded from: classes2.dex */
public final class FeatureSwitch {
    public static final boolean DEFAULT_GRIFFIN_CORE_SUPPORT;
    public static final boolean DEFAULT_GRIFFIN_PM_SUPPORT;
    public static final boolean DEFAULT_GRIFFIN_SUPPORT;

    /* loaded from: classes2.dex */
    public interface SwitchListener {
        int onSwitched(String str);
    }

    static {
        DEFAULT_GRIFFIN_SUPPORT = SystemProperties.getInt("ro.griffin.support", 0) != 0;
        DEFAULT_GRIFFIN_CORE_SUPPORT = SystemProperties.getInt("ro.griffin.core", 0) != 0;
        DEFAULT_GRIFFIN_PM_SUPPORT = SystemProperties.getInt("ro.griffin.pm", 0) != 0;
    }
}
