package com.mediatek.boostfwk;

import com.mediatek.boostfwk.scenario.BasicScenario;
/* loaded from: classes4.dex */
public class BoostFwkManager {
    public static final int BOOST_BEGIN = 0;
    public static final int BOOST_END = 1;
    public static final int UX_FRAME = 2;
    public static final int UX_IME = 5;
    public static final int UX_LAUNCH = 3;
    public static final int UX_MSG = 4;
    public static final int UX_REFRESHRATE = 6;
    public static final int UX_SCROLL = 1;
    public static final int UX_VIEWEVENT = 7;

    /* loaded from: classes4.dex */
    public class Scroll {
        public static final int HORIZONTAL = 3;
        public static final int INPUT_EVENT = 0;
        public static final int PREFILING = 1;
        public static final int VERTICAL = 2;

        public Scroll() {
        }
    }

    /* loaded from: classes4.dex */
    public class Draw {
        public static final int FRAME_DRAW = 0;
        public static final int FRAME_DRAW_STEP = 1;
        public static final int FRAME_PREFETCHER = 4;
        public static final int FRAME_PRE_ANIM = 5;
        public static final int FRAME_RENDER_INFO = 3;
        public static final int FRAME_REQUEST_VSYNC = 2;

        public Draw() {
        }
    }

    /* loaded from: classes4.dex */
    public class Launch {
        public static final int ACTIVITY_SWITCH = 3;
        public static final int LAUNCH_COLD = 1;
        public static final int LAUNCH_HOT = 2;

        public Launch() {
        }
    }

    /* loaded from: classes4.dex */
    public class Message {
        public static final int AUDIO_MSG = 0;

        public Message() {
        }
    }

    /* loaded from: classes4.dex */
    public class IME {
        public static final int IME_HIDE = 2;
        public static final int IME_INIT = 3;
        public static final int IME_SHOW = 1;

        public IME() {
        }
    }

    /* loaded from: classes4.dex */
    public class RefreshRate {
        public static final int FLING_FINISH = 2;
        public static final int FLING_FRICTION_UPDATE = 5;
        public static final int FLING_START = 0;
        public static final int FLING_UPDATE = 1;
        public static final int SCROLLER_INIT = 3;
        public static final int TOUCH_SCROLL_ENABLE = 4;

        public RefreshRate() {
        }
    }

    /* loaded from: classes4.dex */
    public class ViewEvent {
        public static final int SURFACEVIEW_VISIBILITY = 1;
        public static final int TEXTUREVIEW_VISIBILITY = 0;

        public ViewEvent() {
        }
    }

    public void perfHint(BasicScenario scenario) {
    }

    public void perfHint(BasicScenario... scenarios) {
    }
}
