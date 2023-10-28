package com.mediatek.boostfwk.identify.launch;

import java.util.HashMap;
import java.util.Map;
/* loaded from: classes.dex */
public class LaunchConfig {
    public static final long ACTIVITY_CONSIDERED_IDLE = 1000;
    public static final long ACTIVITY_CONSIDERED_RESUME = 2000;
    public static final int COUNT_DEFAULT = 1;
    public static Map<String, String> SPECIAL_MAP = new HashMap<String, String>() { // from class: com.mediatek.boostfwk.identify.launch.LaunchConfig.1
        {
            put("com.tencent.mm", "3");
        }
    };
}
