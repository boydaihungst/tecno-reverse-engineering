package com.mediatek.boostfwk.identify.scroll;

import android.app.ActivityTaskManager;
import android.content.Context;
import android.os.Process;
import android.os.SystemProperties;
import android.os.Trace;
import android.view.GestureDetector;
import android.view.MotionEvent;
import com.mediatek.boostfwk.identify.BaseIdentify;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.info.ScrollState;
import com.mediatek.boostfwk.policy.frame.ScrollingFramePrefetcher;
import com.mediatek.boostfwk.policy.refreshrate.RefreshRateInfo;
import com.mediatek.boostfwk.policy.scroll.ScrollPolicy;
import com.mediatek.boostfwk.policy.touch.TouchPolicy;
import com.mediatek.boostfwk.scenario.BasicScenario;
import com.mediatek.boostfwk.scenario.scroll.ScrollScenario;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import com.mediatek.boostfwk.utils.TasksUtil;
import com.mediatek.boostfwk.utils.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class ScrollIdentify extends BaseIdentify {
    private static final float DISPLAY_RATE_60 = 60.0f;
    public static final ArrayList<String> LAUNCHER_PACKAGE;
    protected static final float MOVE_DISTANCE = 50.0f;
    public static final int NO_CHECKED_STATUS = -1;
    public static final int PAGE_TYPE_AOSP_DESIGN = 0;
    public static final int PAGE_TYPE_NO_ACTIVITY = 4;
    public static final int PAGE_TYPE_SPECIAL_DESIGN = 1;
    public static final int PAGE_TYPE_SPECIAL_DESIGN_WEB_ON_60 = 2;
    private static final boolean RENDER_THREAD_BOOST_SUPPORT;
    private static final String TAG = "ScrollIdentify";
    private static int mIsSpecialPageDesign = 0;
    private static final boolean sAUTO_SWITCH_FPSGO = true;
    private final Map<String, List<String>> BOOST_RENDER_THREAD_MAP;
    private GestureDetector mGestureDetector;
    private static ScrollIdentify sInstance = null;
    private static Object lock = new Object();
    private static final Object SCROLL_LOCK = new Object();
    public static final boolean TRAN_LAUNCHER_BOOST_SCENE_SUPPORT = SystemProperties.get("ro.transsion.launcher_boost_scene_support").equals(Config.USER_CONFIG_DEFAULT_TYPE);
    private Object scrollerLock = null;
    private boolean mIsInput = false;
    private String mDispatcherPkgName = null;
    private boolean mIsSystemApp = false;
    private String mInputPkgName = "";
    private float mRefreshRate = RefreshRateInfo.DECELERATION_RATE;
    private long mFrameIntervalMs = 0;
    private long mLimitVsyncTime = 0;
    protected final float FLING_DISTANCE_VERTICAL_DP = SystemProperties.getInt("vendor.boostfwk.fling_distance_dp", 70);
    protected final int FLING_DISTANCE_HORIZONTAL_DP = 48;
    protected final int FLING_SPEED_VERTICAL_DP = 350;
    protected final int FLING_SPEED_HORIZONTAL_DP = 400;
    private float minVelocityHorizontal = -1.0f;
    private float minVelocityVertical = -1.0f;
    private float minTouchDistanceHorizontal = -1.0f;
    private float minTouchDistanceVertical = -1.0f;
    private boolean mHaveMoveEvent = false;
    private boolean mIsInputLockAcquired = false;
    private boolean mIsScrollLockAcquired = false;
    private boolean mIsScrolling = false;
    private boolean mIsUserTouched = false;
    private boolean mLastScrollerEnd = false;
    private final int PAGE_TYPE_FULLSCREEN_GLTHREAD = 3;
    private int mApplicationType = -1;
    private final int APP_TYPE_GAME = 1;
    private final int APP_TYPE_READER = 2;
    private final int APP_TYPE_MAP = 3;
    private final int APP_TYPE_SYSTEM = 4;
    private final int APP_TYPE_STRICT_MODE = 5;
    private final int APP_TYPE_NORMAL = 6;
    private ActivityInfo activityInfo = null;
    private TouchPolicy mTouchPolicy = null;
    private MotionEvent mCurrentMotionEvent = null;
    private ActivityInfo.ActivityChangeListener activityChangeListener = null;
    private List<TouchEventListener> mTouchEventListeners = new ArrayList();

    /* loaded from: classes.dex */
    public interface TouchEventListener {
        void onTouchEvent(MotionEvent motionEvent);
    }

    static {
        RENDER_THREAD_BOOST_SUPPORT = SystemProperties.getInt("ro.transsion.ins.fling.boost.support", 0) > 0 ? sAUTO_SWITCH_FPSGO : false;
        mIsSpecialPageDesign = -1;
        LAUNCHER_PACKAGE = new ArrayList<String>() { // from class: com.mediatek.boostfwk.identify.scroll.ScrollIdentify.1
            {
                add("com.transsion.XOSLauncher");
                add("com.transsion.hilauncher");
                add("com.transsion.XOSLauncher.upgrade");
                add("com.transsion.hilauncher.upgrade");
            }
        };
    }

    /* loaded from: classes.dex */
    private static class ActivityListener implements ActivityInfo.ActivityChangeListener {
        private ActivityListener() {
        }

        @Override // com.mediatek.boostfwk.info.ActivityInfo.ActivityChangeListener
        public void onChange(Context c) {
            ScrollIdentify.mIsSpecialPageDesign = -1;
        }
    }

    public static ScrollIdentify getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new ScrollIdentify();
                }
            }
        }
        return sInstance;
    }

    private ScrollIdentify() {
        HashMap hashMap = new HashMap();
        this.BOOST_RENDER_THREAD_MAP = hashMap;
        hashMap.put("com.instagram.android", Arrays.asList("com.instagram.mainactivity.MainActivity"));
        hashMap.put("com.google.android.apps.photos", Arrays.asList("com.google.android.apps.photos.home.HomeActivity", "com.google.android.apps.photos.localmedia.ui.LocalPhotosActivity"));
        hashMap.put("com.gallery20", Arrays.asList("com.gallery20.HomeActivity", "com.gallery20.app.album.AlbumVerticalActivity"));
    }

    @Override // com.mediatek.boostfwk.identify.BaseIdentify
    public boolean dispatchScenario(BasicScenario basicScenario) {
        if (!Config.isBoostFwkScrollIdentify() || basicScenario == null) {
            return false;
        }
        ScrollScenario scenario = (ScrollScenario) basicScenario;
        int action = scenario.getScenarioAction();
        int status = scenario.getBoostStatus();
        MotionEvent event = scenario.getScenarioInputEvent();
        Object object = scenario.getScenarioObj();
        Context viewContext = null;
        if (scenario.getScenarioContext() != null) {
            viewContext = scenario.getScenarioContext();
        }
        if (viewContext == null) {
            return false;
        }
        if (this.activityInfo == null) {
            this.activityInfo = ActivityInfo.getInstance();
        }
        String packageName = this.activityInfo.getPackageName();
        if (packageName == null) {
            packageName = viewContext.getPackageName();
        }
        if (packageName == null) {
            return false;
        }
        String str = this.mDispatcherPkgName;
        if (str == null || !str.equals(packageName)) {
            this.mApplicationType = -1;
        }
        if (this.mApplicationType == -1) {
            checkAppType(packageName);
        }
        int i = this.mApplicationType;
        if (i == 1 || i == 4 || i == 5) {
            this.mDispatcherPkgName = packageName;
            return false;
        }
        if (this.mGestureDetector == null) {
            try {
                this.mGestureDetector = new GestureDetector(viewContext, new ScrollGestureListener());
            } catch (Exception e) {
                if (Config.isBoostFwkLogEnable()) {
                    LogUtil.mLoge(TAG, "layout not inflate, cannot create GestureDetector,try to next time.");
                }
                this.mGestureDetector = null;
                return false;
            }
        }
        if (this.activityChangeListener == null) {
            ActivityListener activityListener = new ActivityListener();
            this.activityChangeListener = activityListener;
            this.activityInfo.registerActivityListener(activityListener);
        }
        updateRefreshRate();
        this.mDispatcherPkgName = packageName;
        if (Config.isBoostFwkLogEnable()) {
            LogUtil.mLogd(TAG, packageName + ", Scroll action dispatcher to = " + action + " status = " + status + ", viewContext = " + viewContext);
        }
        scenario.setSFPEnable(ScrollingFramePrefetcher.PRE_ANIM_ENABLE);
        switch (action) {
            case 0:
                if (event != null) {
                    inputEventCheck(status, event);
                    break;
                }
                break;
            case 1:
                inputDrawCheck(status, packageName);
                break;
            case 2:
                inertialScrollCheck(status, packageName, object);
                break;
            case 3:
                if (this.mHaveMoveEvent) {
                    if (Config.isBoostFwkLogEnable()) {
                        LogUtil.mLogd(TAG, "using scroller when HORIZONTAL scroll");
                    }
                    inertialScrollCheck(-2, packageName, object);
                    break;
                }
                break;
            default:
                LogUtil.mLogw(TAG, "Not found dispatcher scroll action.");
                break;
        }
        return sAUTO_SWITCH_FPSGO;
    }

    private void updateRefreshRate() {
        float refreshRate = ScrollState.getRefreshRate();
        if (refreshRate == this.mRefreshRate) {
            return;
        }
        this.mRefreshRate = refreshRate;
        this.mFrameIntervalMs = 1000.0f / refreshRate;
    }

    public boolean disableForSpecialRate() {
        boolean result = (Config.isBoostFwkScrollIdentifyOn60hz() && this.mRefreshRate == DISPLAY_RATE_60) ? sAUTO_SWITCH_FPSGO : false;
        if (Config.isBoostFwkLogEnable() && result) {
            LogUtil.mLogd(TAG, "filter specila rate when scrolling: " + this.mRefreshRate);
        }
        return result;
    }

    /* loaded from: classes.dex */
    class ScrollGestureListener extends GestureDetector.SimpleOnGestureListener {
        ScrollGestureListener() {
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onDown(MotionEvent e) {
            ScrollingFramePrefetcher.getInstance().disableAndLockSFP(ScrollIdentify.sAUTO_SWITCH_FPSGO);
            return ScrollIdentify.sAUTO_SWITCH_FPSGO;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null) {
                return false;
            }
            float eventDistancX = Math.abs(e1.getX() - e2.getX());
            float eventDistancY = Math.abs(e1.getY() - e2.getY());
            if (eventDistancX > ScrollIdentify.MOVE_DISTANCE || eventDistancY > ScrollIdentify.MOVE_DISTANCE) {
                ScrollIdentify.this.checkSpecialPageType();
                if (Config.isBoostFwkLogEnable()) {
                    LogUtil.mLogd(ScrollIdentify.TAG, "mIsSpecialPageDesign = " + ScrollIdentify.mIsSpecialPageDesign);
                }
                if (ScrollIdentify.mIsSpecialPageDesign == 3) {
                    return false;
                }
                if (Config.isBoostFwkLogEnable()) {
                    LogUtil.mLogd(ScrollIdentify.TAG, "onScroll - " + (eventDistancX > eventDistancY ? "horizontal" : "vertical"));
                }
                ScrollIdentify.this.mHaveMoveEvent = ScrollIdentify.sAUTO_SWITCH_FPSGO;
                ScrollIdentify.this.sbeHint(0, "Boost when move");
            }
            return ScrollIdentify.sAUTO_SWITCH_FPSGO;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null || ScrollIdentify.mIsSpecialPageDesign == 3) {
                return false;
            }
            float distanceX = Math.abs(e2.getX() - e1.getX());
            float distanceY = Math.abs(e2.getY() - e1.getY());
            if (Config.isBoostFwkLogEnable()) {
                LogUtil.mLogd(ScrollIdentify.TAG, "onFling --> distanceX: " + distanceX + ", Math.abs(velocityX):" + Math.abs(velocityX) + ", distanceY: " + distanceY + ", Math.abs(velocityY): " + Math.abs(velocityY));
            }
            LogUtil.mLogi(ScrollIdentify.TAG, "on fling");
            ScrollIdentify.this.initMinValuesIfNeeded();
            if (distanceX > ScrollIdentify.this.minTouchDistanceHorizontal && Math.abs(velocityX) > ScrollIdentify.this.minVelocityHorizontal && 0.5f * distanceX > distanceY) {
                ScrollIdentify.this.sbeHint(3, "onFling Boost when FLING_HORIZONTAL");
                return ScrollIdentify.sAUTO_SWITCH_FPSGO;
            } else if (distanceY > ScrollIdentify.this.minTouchDistanceVertical && Math.abs(velocityY) > ScrollIdentify.this.minVelocityVertical) {
                ScrollIdentify.this.sbeHint(2, "onFling Boost when FLING_VERTICAL");
                return ScrollIdentify.sAUTO_SWITCH_FPSGO;
            } else {
                return ScrollIdentify.sAUTO_SWITCH_FPSGO;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initMinValuesIfNeeded() {
        if (this.minVelocityHorizontal == -1.0f || this.minTouchDistanceHorizontal == -1.0f || this.minTouchDistanceVertical == -1.0f || this.minVelocityVertical == -1.0f) {
            float density = this.activityInfo.getDensity();
            this.minVelocityHorizontal = density > RefreshRateInfo.DECELERATION_RATE ? 400.0f * density : 1200.0f;
            this.minTouchDistanceHorizontal = density > RefreshRateInfo.DECELERATION_RATE ? 48.0f * density : 150.0f;
            this.minVelocityVertical = density > RefreshRateInfo.DECELERATION_RATE ? 350.0f * density : 1000.0f;
            this.minTouchDistanceVertical = density > RefreshRateInfo.DECELERATION_RATE ? this.FLING_DISTANCE_VERTICAL_DP * density : 200.0f;
            if (Config.isBoostFwkLogEnable()) {
                LogUtil.mLogd(TAG, "onFling density=" + density + " minTouchDistanceVertical=" + this.minTouchDistanceVertical + " minVelocityVertical=" + this.minVelocityVertical + " minVelocityHorizontal=" + this.minVelocityHorizontal + "minTouchDistanceHorizontal=" + this.minTouchDistanceHorizontal);
            }
        }
    }

    private void inputEventCheck(int status, MotionEvent motionEvent) {
        int actionCur;
        boolean begin = boostBeginEndCheck(status);
        if (begin) {
            this.mCurrentMotionEvent = motionEvent.copy();
            if (motionEvent.getActionMasked() == 0) {
                this.mIsUserTouched = sAUTO_SWITCH_FPSGO;
                return;
            }
            return;
        }
        MotionEvent event = motionEvent;
        int action = event.getActionMasked();
        MotionEvent motionEvent2 = this.mCurrentMotionEvent;
        if (motionEvent2 != null && action != (actionCur = motionEvent2.getActionMasked()) && action == 3) {
            event = this.mCurrentMotionEvent;
            action = actionCur;
        }
        switch (action) {
            case 0:
                if (TRAN_LAUNCHER_BOOST_SCENE_SUPPORT && LAUNCHER_PACKAGE.contains(this.mDispatcherPkgName)) {
                    try {
                        ActivityTaskManager.getService().boostStartForLauncher();
                    } catch (Exception e) {
                        LogUtil.mLoge(TAG, "boost fling failed");
                    }
                }
                this.mLastScrollerEnd = false;
                if (this.mIsScrollLockAcquired && this.scrollerLock != null) {
                    this.scrollerLock = null;
                    this.mIsScrollLockAcquired = false;
                }
                this.mIsUserTouched = sAUTO_SWITCH_FPSGO;
                break;
            case 1:
            case 3:
                if (Config.isBoostFwkLogEnable()) {
                    LogUtil.mLogd(TAG, "touch up/cancel ");
                }
                if (this.mHaveMoveEvent) {
                    if (TRAN_LAUNCHER_BOOST_SCENE_SUPPORT && LAUNCHER_PACKAGE.contains(this.mDispatcherPkgName)) {
                        try {
                            ActivityTaskManager.getService().boostEndForLauncher();
                        } catch (Exception e2) {
                            LogUtil.mLoge(TAG, "boost fling failed");
                        }
                    }
                    this.mHaveMoveEvent = false;
                    sbeHint(1, "Boost when up/cancel " + action);
                } else if (TRAN_LAUNCHER_BOOST_SCENE_SUPPORT && LAUNCHER_PACKAGE.contains(this.mDispatcherPkgName)) {
                    try {
                        ActivityTaskManager.getService().boostSceneEnd(4);
                    } catch (Exception e3) {
                        LogUtil.mLoge(TAG, "boost fling failed");
                    }
                }
                this.mIsInputLockAcquired = false;
                ScrollingFramePrefetcher.getInstance().disableAndLockSFP(false);
                break;
        }
        GestureDetector gestureDetector = this.mGestureDetector;
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }
        if (this.mTouchPolicy == null) {
            this.mTouchPolicy = TouchPolicy.getInstance();
        }
        for (TouchEventListener touchEventListener : this.mTouchEventListeners) {
            touchEventListener.onTouchEvent(event);
        }
        if (action == 1 || action == 3) {
            this.mCurrentMotionEvent = null;
        }
    }

    private void inputDrawCheck(int action, String pkgName) {
        if (this.mLastScrollerEnd || (this.mHaveMoveEvent && !this.mIsInputLockAcquired)) {
            this.mLimitVsyncTime = this.mFrameIntervalMs >> 1;
            this.mIsInput = boostBeginEndCheck(action);
            this.mInputPkgName = pkgName;
            if (Config.isBoostFwkLogEnable()) {
                LogUtil.mLogd(TAG, "Vendor::inputDrawCheck begin, pkgName = " + pkgName + ", refresh rate = " + this.mRefreshRate + ", mFrameIntervalMs = " + this.mFrameIntervalMs);
            }
            this.mIsInputLockAcquired = sAUTO_SWITCH_FPSGO;
            this.mIsUserTouched = sAUTO_SWITCH_FPSGO;
        }
    }

    public void inertialScrollCheck(int action, String pkgName, Object obj) {
        if (!this.mIsUserTouched || mIsSpecialPageDesign == 4) {
            if (Config.isBoostFwkLogEnable()) {
                LogUtil.mLogd(TAG, "inertialScrollCheck mIsUserTouched=" + this.mIsUserTouched + " mIsSpecialPageDesign=" + mIsSpecialPageDesign);
            }
        } else if (action == -2) {
            ScrollPolicy.getInstance().switchToFPSGo(sAUTO_SWITCH_FPSGO);
            this.mIsScrollLockAcquired = false;
        } else {
            if (Config.isBoostFwkLogEnable()) {
                LogUtil.mLogd(TAG, "inertialScrollCheck action=" + action + " pkgName=" + pkgName + " obj=" + obj + " " + this.scrollerLock);
            }
            boolean shouldBoost = boostBeginEndCheck(action);
            if (!checkScroller(shouldBoost, obj)) {
                return;
            }
            boolean z = this.mIsScrollLockAcquired;
            if (!z && shouldBoost) {
                if (RENDER_THREAD_BOOST_SUPPORT && action != -2 && this.BOOST_RENDER_THREAD_MAP.containsKey(pkgName)) {
                    ActivityTaskManager.getInstance().handleDrawThreadsBoost((boolean) sAUTO_SWITCH_FPSGO, (boolean) sAUTO_SWITCH_FPSGO, this.BOOST_RENDER_THREAD_MAP.get(pkgName));
                }
                inertialScrollHint(sAUTO_SWITCH_FPSGO, pkgName);
                this.mIsScrollLockAcquired = sAUTO_SWITCH_FPSGO;
            } else if (z && !shouldBoost) {
                if (RENDER_THREAD_BOOST_SUPPORT && action != -2 && this.BOOST_RENDER_THREAD_MAP.containsKey(pkgName)) {
                    ActivityTaskManager.getInstance().resetBoostThreads();
                }
                inertialScrollHint(false, pkgName);
                this.mIsScrollLockAcquired = false;
            }
        }
    }

    private void inertialScrollHint(boolean enable, String pkgName) {
        if (!this.mIsInput && this.mInputPkgName.equals("") && !this.mInputPkgName.equals(pkgName) && mIsSpecialPageDesign == 0) {
            resetInputFlag(sAUTO_SWITCH_FPSGO);
            return;
        }
        ScrollPolicy.getInstance().switchToFPSGo(enable);
        LogUtil.mLogi(TAG, "update state to " + (enable ? "fling" : "finish"));
        ScrollState.setFling(enable);
        ScrollingFramePrefetcher.getInstance().setFling(enable);
        ScrollPolicy.getInstance().releaseTargetFPS(enable);
        resetInputFlag(enable ^ sAUTO_SWITCH_FPSGO);
    }

    private void resetInputFlag(boolean reset) {
        if (reset) {
            this.mIsInput = false;
            this.mInputPkgName = "";
            this.mIsUserTouched = false;
        }
        this.mLastScrollerEnd = reset;
    }

    private boolean boostBeginEndCheck(int action) {
        switch (action) {
            case 0:
                return sAUTO_SWITCH_FPSGO;
            case 1:
                return false;
            default:
                LogUtil.mLoge(TAG, "Unknown define action inputed, exit now.");
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkSpecialPageType() {
        updatePageType();
        switch (mIsSpecialPageDesign) {
            case 0:
                return;
            case 1:
            case 2:
            case 3:
                this.mIsInputLockAcquired = sAUTO_SWITCH_FPSGO;
                return;
            default:
                LogUtil.mLoge(TAG, "Unknown special app design status. flag = " + mIsSpecialPageDesign);
                return;
        }
    }

    public void updatePageType() {
        if (mIsSpecialPageDesign != -1) {
            return;
        }
        Context context = this.activityInfo.getContext();
        if (context == null) {
            mIsSpecialPageDesign = 4;
        } else if (LAUNCHER_PACKAGE.contains(context.getPackageName())) {
            mIsSpecialPageDesign = 1;
        } else if (TasksUtil.fullscreenAndGl(this.activityInfo.getWindowLayoutAttr())) {
            mIsSpecialPageDesign = 3;
        } else if (!TasksUtil.isSpeicalAPP(context)) {
            mIsSpecialPageDesign = 0;
        } else {
            mIsSpecialPageDesign = 1;
            if (this.mRefreshRate == DISPLAY_RATE_60 && !TasksUtil.isSpeicalAPPWOWebView(context)) {
                mIsSpecialPageDesign = 2;
            }
        }
    }

    private void checkAppType(String packageName) {
        String log = "APP_TYPE_NORMAL";
        if (TasksUtil.isAPPInStrictMode(packageName)) {
            this.mApplicationType = 5;
            log = "APP_TYPE_STRICT_MODE";
        } else if (TasksUtil.isGameAPP(packageName)) {
            this.mApplicationType = 1;
            log = "APP_TYPE_GAME";
        } else if (isSystemApp(packageName)) {
            this.mApplicationType = 4;
            log = "APP_TYPE_SYSTEM";
        } else {
            this.mApplicationType = 6;
        }
        if (Config.isBoostFwkLogEnable()) {
            LogUtil.mLogd(TAG, "onScroll -- " + log);
        }
    }

    private boolean isSystemApp(String packageName) {
        if (Config.SYSTEM_PACKAGE_ARRAY.contains(packageName) || "system_server".equals(Process.myProcessName())) {
            return sAUTO_SWITCH_FPSGO;
        }
        return false;
    }

    private boolean checkScroller(boolean shouldBoost, Object obj) {
        if (obj == null) {
            return false;
        }
        if (shouldBoost) {
            if (this.scrollerLock == null) {
                this.scrollerLock = obj;
                return sAUTO_SWITCH_FPSGO;
            }
        } else {
            Object obj2 = this.scrollerLock;
            if (obj2 != null && obj2 == obj) {
                this.scrollerLock = null;
                return sAUTO_SWITCH_FPSGO;
            }
        }
        return false;
    }

    private boolean checkSystemAPP(String pkgName) {
        String str = this.mDispatcherPkgName;
        if (str == null && str != pkgName) {
            this.mIsSystemApp = Util.isSystemApp(pkgName);
        }
        boolean isSystemApp = this.mIsSystemApp;
        return isSystemApp;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sbeHint(int whichStep, String logStr) {
        int i;
        if (Config.isBoostFwkLogEnable() && logStr != null) {
            LogUtil.mLogd(TAG, "step=" + whichStep + " " + logStr);
        }
        if ((whichStep == 2 || whichStep == 3) && (i = mIsSpecialPageDesign) != -1 && i != 4) {
            ScrollPolicy.getInstance().scrollHint(whichStep, mIsSpecialPageDesign);
        } else if (this.mIsInputLockAcquired) {
            ScrollPolicy.getInstance().scrollHint(whichStep, mIsSpecialPageDesign);
        }
    }

    public boolean isScroll() {
        boolean z;
        synchronized (SCROLL_LOCK) {
            z = this.mIsScrolling;
        }
        return z;
    }

    public void setScrolling(boolean scrolling, String msg) {
        synchronized (SCROLL_LOCK) {
            this.mIsScrolling = scrolling;
            if (Config.isBoostFwkLogEnable()) {
                Trace.traceBegin(8L, msg + " curScrollingState=" + String.valueOf(this.mIsScrolling));
                Trace.traceEnd(8L);
            }
        }
    }

    public int getPageDesign() {
        return mIsSpecialPageDesign;
    }

    public void registerTouchEventListener(TouchEventListener listener) {
        if (listener == null) {
            return;
        }
        this.mTouchEventListeners.add(listener);
    }
}
