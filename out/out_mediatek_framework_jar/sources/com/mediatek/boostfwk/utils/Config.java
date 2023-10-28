package com.mediatek.boostfwk.utils;

import android.os.SystemProperties;
import java.util.ArrayList;
/* loaded from: classes.dex */
public final class Config {
    private static final String BOOST_FWK_TOUCH_DURATION_PROPERTY_NAME = "vendor.boostfwk.touch.duration";
    private static final String BOOST_FWK_VERSION = "vendor.boostfwk.version";
    public static final int BOOST_FWK_VERSION_1 = 1;
    public static final int BOOST_FWK_VERSION_2 = 2;
    private static final String DISABLE_BOOST_FWK = "0";
    private static final String DISABLE_BOOST_FWK_FRAME_PREFETCHER = "0";
    private static final String DISABLE_BOOST_FWK_PRE_ANIMATION = "0";
    private static final String DISABLE_BOOST_FWK_TOUCH = "0";
    private static final String DISABLE_FRAME_IDENTIFY = "0";
    private static final String DISABLE_FRAME_RESCUE = "0";
    private static final String DISABLE_SCROLL_COMMON_POLICY = "0";
    private static final String DISABLE_SCROLL_IDENTIFY = "0";
    private static final String ENABLE_BOOST_DISPLAY_60 = "0";
    private static final String ENABLE_ENHANCE_LOG = "1";
    private static final String ENABLE_MSYNC3 = "1";
    private static final String ENABLE_MSYNC3_SMOOTHFLING = "1";
    public static final float FLING_DEFAULT_GAP_CHANGE_REFRESHRATE = 1.83f;
    public static final int FRAME_HINT_RESCUE_STRENGTH = 50;
    public static final int SCROLLING_FING_HORIZONTAL_HINT_DURATION = 500;
    private static final String SCROLL_HINT_DURATION = "vendor.boostfwk.scroll.duration";
    public static final int TOUCH_HINT_DURATION_DEFAULT = 1000;
    public static final String USER_CONFIG_DEFAULT_TYPE = "1";
    private static final String ENHANCE_LOG_PROPERTY_NAME = "vendor.boostfwk.log.enable";
    private static final boolean BOOLEAN_ENABLE_LOG = USER_CONFIG_DEFAULT_TYPE.equals(SystemProperties.get(ENHANCE_LOG_PROPERTY_NAME));
    private static final String DISABLE_SCROLL_IDENTIFY_PROPERTY_NAME = "vendor.boostfwk.scrollidentify.option";
    private static final boolean BOOLEAN_DISABLE_SCROLL_IDENTIFY = !"0".equals(SystemProperties.get(DISABLE_SCROLL_IDENTIFY_PROPERTY_NAME));
    private static final String DISABLE_FRAME_IDENTIFY_PROPERTY_NAME = "vendor.boostfwk.frameidentify.option";
    private static final boolean BOOLEAN_DISABLE_FRAME_IDENTIFY = "0".equals(SystemProperties.get(DISABLE_FRAME_IDENTIFY_PROPERTY_NAME));
    private static final String DISABLE_BOOST_FWK_PROPERTY_NAME = "vendor.boostfwk.option";
    private static final boolean BOOLEAN_DISABLE_BOOST_FWK = "0".equals(SystemProperties.get(DISABLE_BOOST_FWK_PROPERTY_NAME));
    private static final String ENABLE_BOOST_DISPLAY_60_PROPERTY_NAME = "vendor.boostfwk.display60";
    private static final boolean BOOLEAN_ENABLE_BOOST_DISPLAY_60 = !"0".equals(SystemProperties.get(ENABLE_BOOST_DISPLAY_60_PROPERTY_NAME));
    private static final String DISABLE_BOOST_FWK_FRAME_PREFETCHER_NAME = "vendor.boostfwk.frameprefetcher";
    private static final boolean BOOLEAN_DISABLE_BOOST_FWK_FRAME_PREFETCHER = !"0".equals(SystemProperties.get(DISABLE_BOOST_FWK_FRAME_PREFETCHER_NAME));
    private static final String DISABLE_BOOST_FWK_PRE_ANIMATION_NAME = "vendor.boostfwk.preanimation";
    private static final boolean BOOLEAN_BOOST_FWK_PRE_ANIMATION = !"0".equals(SystemProperties.get(DISABLE_BOOST_FWK_PRE_ANIMATION_NAME));
    private static final String FRAME_RESCUE_PROPERTY_NAME = "vendor.boostfwk.rescue";
    private static final boolean BOOLEAN_ENABLE_FRAME_RESCUE = !"0".equals(SystemProperties.get(FRAME_RESCUE_PROPERTY_NAME));
    private static final String BOOST_FWK_TOUCH_PROPERTY_NAME = "vendor.boostfwk.touch";
    private static final boolean BOOLEAN_ENABLE_BOOST_FWK_TOUCH = !"0".equals(SystemProperties.get(BOOST_FWK_TOUCH_PROPERTY_NAME));
    private static final String SCROLL_COMMON_POLICY_PROPERTY_NAME = "vendor.boostfwk.scroll.common.policy";
    private static final boolean BOOLEAN_ENABLE_SCROLL_COMMON_POLICY = !"0".equals(SystemProperties.get(SCROLL_COMMON_POLICY_PROPERTY_NAME));
    public static int sSCROLLING_HINT_DURATION = 0;
    private static int mTouchHintDuration = 0;
    private static int mBoostFwkVersion = 0;
    private static final String ENABLE_MSYNC3_NAME = "vendor.msync3.enable";
    public static final String USER_CONFIG_DEFAULT_VALUE = "-1";
    private static final boolean BOOLEAN_ENABLE_MSYNC_MSYNC3 = USER_CONFIG_DEFAULT_TYPE.equals(SystemProperties.get(ENABLE_MSYNC3_NAME, USER_CONFIG_DEFAULT_VALUE));
    private static final String ENABLE_MSYNC3_SMOOTHFLING_NAME = "vendor.msync3.smoothfling";
    private static final boolean BOOLEAN_ENABLE_MSYNC_SMOOTHFLING_MSYNC3 = USER_CONFIG_DEFAULT_TYPE.equals(SystemProperties.get(ENABLE_MSYNC3_SMOOTHFLING_NAME, USER_CONFIG_DEFAULT_VALUE));
    private static final String MSYNC3_FLING_SUPPORT_REFRESHRATE_NAME = "vendor.msync3.flingrefreshrate";
    public static final String MSYNC3_FLING_SUPPORT_REFRESHRATE = SystemProperties.get(MSYNC3_FLING_SUPPORT_REFRESHRATE_NAME, USER_CONFIG_DEFAULT_VALUE);
    private static final String MSYNC3_FLING_REFRESHRATE_GAT_NAME = "vendor.msync3.flingrefreshrategap";
    public static final String MSYNC3_FLING_REFRESHRATE_CHANGE_GAP = SystemProperties.get(MSYNC3_FLING_REFRESHRATE_GAT_NAME, USER_CONFIG_DEFAULT_VALUE);
    public static final String MSYNC3_TOUCHSCROLL_REFRESHRATE_SPEED_LOW_NAME = "vendor.msync3.touchscrolllow";
    public static final String MSYNC3_TOUCHSCROLL_REFRESHRATE_SPEED_LOW = SystemProperties.get(MSYNC3_TOUCHSCROLL_REFRESHRATE_SPEED_LOW_NAME, USER_CONFIG_DEFAULT_VALUE);
    public static final String MSYNC3_VIDEO_FLOOR_FPS_NAME = "vendor.msync3.videofloor";
    public static final String MSYNC3_VIDEO_FLOOR_FPS = SystemProperties.get(MSYNC3_VIDEO_FLOOR_FPS_NAME, USER_CONFIG_DEFAULT_VALUE);
    public static final String MSYNC3_TOUCH_SCROLL_VELOCITY_NAME = "vendor.msync3.touchscrollvelocity";
    public static final String MSYNC3_TOUCH_SCROLL_VELOCITY = SystemProperties.get(MSYNC3_TOUCH_SCROLL_VELOCITY_NAME, USER_CONFIG_DEFAULT_VALUE);
    public static final ArrayList<String> SYSTEM_PACKAGE_ARRAY = new ArrayList<String>() { // from class: com.mediatek.boostfwk.utils.Config.1
        {
            add("android");
            add("com.android.systemui");
        }
    };

    public static boolean isBoostFwkLogEnable() {
        return BOOLEAN_ENABLE_LOG;
    }

    public static boolean isBoostFwkScrollIdentify() {
        return BOOLEAN_DISABLE_SCROLL_IDENTIFY;
    }

    public static boolean disableFrameIdentify() {
        return BOOLEAN_DISABLE_FRAME_IDENTIFY;
    }

    public static boolean disableSBE() {
        return BOOLEAN_DISABLE_BOOST_FWK;
    }

    public static boolean isBoostFwkScrollIdentifyOn60hz() {
        return BOOLEAN_ENABLE_BOOST_DISPLAY_60;
    }

    public static int getScrollDuration() {
        if (sSCROLLING_HINT_DURATION == 0) {
            String duration = SystemProperties.get(SCROLL_HINT_DURATION);
            sSCROLLING_HINT_DURATION = 3000;
            if (duration != null && duration != "") {
                try {
                    sSCROLLING_HINT_DURATION = Integer.valueOf(duration).intValue();
                } catch (Exception e) {
                    sSCROLLING_HINT_DURATION = 3000;
                }
            }
        }
        return sSCROLLING_HINT_DURATION;
    }

    public static int getBoostFwkVersion() {
        if (mBoostFwkVersion == 0) {
            String version = SystemProperties.get(BOOST_FWK_VERSION);
            mBoostFwkVersion = 1;
            if (version != null && version != "") {
                try {
                    mBoostFwkVersion = Integer.valueOf(version).intValue();
                } catch (Exception e) {
                    mBoostFwkVersion = 1;
                }
            }
        }
        return mBoostFwkVersion;
    }

    public static int getTouchDuration() {
        if (mTouchHintDuration <= 0) {
            String duration = SystemProperties.get(BOOST_FWK_TOUCH_DURATION_PROPERTY_NAME);
            mTouchHintDuration = TOUCH_HINT_DURATION_DEFAULT;
            if (duration != null && duration != "") {
                try {
                    mTouchHintDuration = Integer.valueOf(duration).intValue();
                } catch (Exception e) {
                    mTouchHintDuration = TOUCH_HINT_DURATION_DEFAULT;
                }
            }
        }
        return mTouchHintDuration;
    }

    public static boolean isVariableRefreshRateSupported() {
        return BOOLEAN_ENABLE_MSYNC_MSYNC3;
    }

    public static boolean isMSync3SmoothFlingEnabled() {
        return BOOLEAN_ENABLE_MSYNC_SMOOTHFLING_MSYNC3;
    }

    public static boolean isEnableFramePrefetcher() {
        return BOOLEAN_DISABLE_BOOST_FWK_FRAME_PREFETCHER;
    }

    public static boolean isEnablePreAnimation() {
        return BOOLEAN_BOOST_FWK_PRE_ANIMATION;
    }

    public static boolean isEnableFrameRescue() {
        return BOOLEAN_ENABLE_FRAME_RESCUE;
    }

    public static boolean isEnableScrollCommonPolicy() {
        return BOOLEAN_ENABLE_SCROLL_COMMON_POLICY;
    }

    public static boolean isEnableTouchPolicy() {
        return BOOLEAN_ENABLE_BOOST_FWK_TOUCH;
    }
}
