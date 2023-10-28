package com.transsion.hubcore.server.wm.tranrefreshrate;

import android.app.ActivityManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.server.LocalServices;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.DisplayPolicy;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowProcessController;
import com.android.server.wm.WindowState;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.griffin.lib.provider.TranStateListener;
import com.transsion.hubcore.magellan.ITranMagellanFeature;
import com.transsion.hubcore.server.wm.ITranRefreshRatePolicyProxy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;
import org.json.JSONException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TranBaseRefreshRatePolicy {
    protected static final int BIG_SCREEN_APP_WIDTH = 2000;
    private static final String DEFAULT_VALUE = "unKnown";
    protected static final int FLING_SPEED_FAST = 1;
    protected static final int FLING_SPEED_SLOW = 2;
    protected static final int FLING_SPEED_STOP = 0;
    protected static final float FLOAT_TOLERANCE = 0.01f;
    protected static final float INPUT_METHOD_CPU_TEMP_THRESHOLD = 40.0f;
    private static final String ITEM_ACTIVITY_SCENE = "ActivityStateScene";
    private static final String ITEM_DISPLAY_STATE_SCENE = "DisplayStateScene";
    private static final String ITEM_INPUT_METHOD_SCENE = "input_method_scene";
    private static final String ITEM_NATIVE_VIDEO_SCENE = "native_video_scene";
    private static final String ITEM_NAVIGATION_SCENE = "navigation_scene";
    private static final String ITEM_OPTICAL_DATA_ACQUISITION_SCENE = "OpticalDataAcquisitionScene";
    private static final String ITEM_SCREEN_DIM_SCENE = "ScreenDimScene";
    private static final String ITEM_SCREEN_LIGHT_ANIMATOR_SCENE = "ScreenLightAnimatorScene";
    private static final String ITEM_SLIDE_SCENE = "slide_scene";
    private static final String ITEM_VOICE_CALL_SCENE = "voice_call_scene";
    private static final String KEY_DISPLAY_NEW_STATE = "display_new_state";
    private static final String KEY_DISPLAY_OLD_STATE = "display_old_state";
    private static final String KEY_FG_SERVICE_TYPES = "fg_service_types";
    private static final String KEY_HAS_FOREGROUND_SERVICES = "has_foreground_services";
    private static final String KEY_IM_SHOW = "im_show";
    private static final String KEY_OPTICAL_DATA_ACQUISITION_STATE = "OpticalDataAcquisitionState";
    private static final String KEY_PACKAGE_NAME = "pkg_name";
    private static final String KEY_SCENE_NAME = "scene_name";
    private static final String KEY_SCREEN_POLICY_STATE = "policy_state";
    private static final String KEY_SCREEN_SCREEN_LIGHT_CHANGE_ANIMATOR_STATE = "animator_state";
    private static final String KEY_SLICE_SCENE_STATE = "slice_scene_state";
    protected static final String KEY_SYS_DREAM = "Sys2044:dream";
    protected static final String KEY_TRANSSION_FINGER_SENSOR_WINDOW = "TranssionFingerSensorWindow";
    private static final String KEY_UID = "uid";
    private static final String KEY_VIDEO_FPS = "video_fps";
    private static final String KEY_VIDEO_PACKAGE_PID = "video_pkg_pid";
    private static final String KEY_VIDEO_SCENE_STATE = "video_scene_state";
    private static final String KEY_VOICE_CALL_SCENE_STATE = "voice_call_scene_state";
    protected static final int NATIVE_VIDEO_PLAY_INIT_VALUE = 3;
    protected static final int NATIVE_VIDEO_PLAY_START = 4;
    protected static final int NATIVE_VIDEO_PLAY_STOPED = 6;
    protected static final int NATIVE_VIDEO_PLAY_STOPING = 5;
    protected static final int SCREEN_LIGHT_CHANGE_ANIMATOR_STATE_END = 2;
    protected static final int SCREEN_LIGHT_CHANGE_ANIMATOR_STATE_START = 1;
    private static final String TAG = TranBaseRefreshRatePolicy.class.getSimpleName();
    public static final int TRAN_ACTIVITY_STATE_CHANGED_INTERVAL = 800;
    protected static final boolean TRAN_COMPATIBILITY_DEFAULT_REFRESH_SUPPORT;
    protected static final boolean TRAN_DEFAULT_AUTO_REFRESH_SUPPORT;
    public static final int TRAN_INPUT_METHOD_INTERVAL = 700;
    protected static final int TRAN_LONG_BATTERY_OFF = 0;
    protected static final int TRAN_LONG_BATTERY_ON = 1;
    protected static final boolean TRAN_NOTIFICATION_REFRESH_SUPPORT;
    public static final int TRAN_OPTICAL_DATA_ACQUISITION_INTERVAL = 5000;
    protected static final int TRAN_REFRESH_120HZ = 120;
    protected static final int TRAN_REFRESH_60HZ = 60;
    protected static final int TRAN_REFRESH_90HZ = 90;
    protected static final int TRAN_REFRESH_AUTO = 10000;
    protected static final int TRAN_REFRESH_OFF = -1;
    protected static final boolean TRAN_REFRESH_RATE_VIDEO_SCENE_DETECTOR_SUPPORT;
    public static final int TRAN_SCREEN_BRIGHT_FROM_OFF_TO_ON_INTERVAL = 5000;
    public static final int TRAN_TOUCH_IDLE_TIMER_SEC = 1200;
    protected static final boolean TSSI_TRAN_FASTSLOWSLIDE_SUPPORT;
    protected static final int VIDEO_PLAY_START = 1;
    protected static final int VIDEO_PLAY_STOPED = 3;
    protected static final int VIDEO_PLAY_STOPING = 2;
    protected static boolean mDebug;
    protected static boolean mTranLauncherAnimationing;
    protected int m90HZRefreshRateId;
    protected final Context mContext;
    protected int mHighRefreshRateId;
    protected int mLowRefreshRateId;
    protected BaseRefreshRateSettingsObserver mSettingsObserver;
    protected WindowManagerService mWms;
    private WindowManagerService.TranWindowManagerServiceProxy mWmsProxy;
    protected TranBaseRefreshRateConfig mTranRefreshRateConfig = null;
    protected int mSlideSceneState = 0;
    protected int mVideoSceneState = 0;
    protected int mNativeVideoSceneState = 0;
    protected int mVideoFPS = 0;
    protected int mVideoAppPid = 0;
    protected String mScenePkgName = DEFAULT_VALUE;
    public TaskTapPointerEventListener mListener = new TaskTapPointerEventListener();
    public TouchTimer mTouchTimer = new TouchTimer();
    public boolean mIsTouchScreen = false;
    public boolean mIsRegisterListener = false;
    public boolean mIsInVoiceCall = false;
    private final ArraySet<String> mNaviAppPackages = new ArraySet<>();
    protected boolean mInputMethodShown = false;
    protected int mInputMethodRefreshRateId = 0;
    protected int mAodRefreshRateId = -1;
    protected int m45HzRefreshRateId = -1;
    protected int m30HzRefreshRateId = -1;
    protected int m10HzRefreshRateId = -1;
    protected int mLatestLowRefreshRateId = -1;
    protected float mInitMinRefreshRate = 10.0f;
    protected int mCurrentAppWidth = 0;
    protected int mCurrentAppHeight = 0;
    protected int mDisplayOldState = 0;
    protected int mDisplayNewState = 0;
    private Handler mHandler = new Handler();
    private Runnable mInputMethodRunnable = new Runnable() { // from class: com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.1
        @Override // java.lang.Runnable
        public void run() {
            TranBaseRefreshRatePolicy.this.mInputMethodShown = true;
            ITranMagellanFeature.instance().sceneTrack(1, true);
            if (TranBaseRefreshRatePolicy.this.mWmsProxy != null) {
                TranBaseRefreshRatePolicy.this.mWmsProxy.requestTraversal();
            }
            TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "mInputMethodRunnable  mInputMethodShown true.");
        }
    };
    private Runnable mScreenFromOffToOnRunnable = new Runnable() { // from class: com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.2
        @Override // java.lang.Runnable
        public void run() {
            TranBaseRefreshRatePolicy.this.updateRefreshRateAfterBrightnessChange(false, 60.0f);
            TranBaseRefreshRatePolicy.this.mSettingsObserver.isDisplayStateChangeFromOffToOn = false;
            TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "mScreenFromOffToOnRunnable end");
        }
    };
    private Runnable mOpticalDataAcquisitionSceneComeRunnable = new Runnable() { // from class: com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.3
        @Override // java.lang.Runnable
        public void run() {
            TranBaseRefreshRatePolicy.this.updateRefreshRateAfterBrightnessChange(false, 60.0f);
            TranBaseRefreshRatePolicy.this.mSettingsObserver.isOpticalDataAcquisitionOn = false;
            TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "mScreenFromOffToOnRunnable end");
        }
    };
    private Runnable mHighRefreshRateLimitTimeSceneComeRunnable = new Runnable() { // from class: com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.4
        @Override // java.lang.Runnable
        public void run() {
            TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "mHighRefreshRateLimitTimeSceneComeRunnable enter");
            TranBaseRefreshRatePolicy.this.mSettingsObserver.mIsHighRefreshRateLimitTimeOn = false;
            if (TranBaseRefreshRatePolicy.this.mWmsProxy != null) {
                TranBaseRefreshRatePolicy.this.mWmsProxy.requestTraversal();
            }
        }
    };
    protected DisplayManagerInternal mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
    ActivityTaskManagerInternal mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);

    static {
        TRAN_NOTIFICATION_REFRESH_SUPPORT = 1 == SystemProperties.getInt("ro.tran_notification_refresh.support", 0);
        TRAN_DEFAULT_AUTO_REFRESH_SUPPORT = 1 == SystemProperties.getInt("ro.tran_default_auto_refresh.support", 0);
        TRAN_COMPATIBILITY_DEFAULT_REFRESH_SUPPORT = 1 == SystemProperties.getInt("ro.tran_compatibility_default_refresh.support", 0);
        TRAN_REFRESH_RATE_VIDEO_SCENE_DETECTOR_SUPPORT = 1 == SystemProperties.getInt("ro.tran_refresh_rate_video_detector.support", 0);
        TSSI_TRAN_FASTSLOWSLIDE_SUPPORT = 1 == SystemProperties.getInt("ro.transsion.fastslowslide.support", 0);
        mTranLauncherAnimationing = false;
        mDebug = false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public TranBaseRefreshRatePolicy(WindowManagerService wmService, DisplayInfo displayInfo, Context context) {
        this.mWms = wmService;
        this.mContext = context;
        this.mWmsProxy = new WindowManagerService.TranWindowManagerServiceProxy(wmService);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static void logd(String tag, String msg) {
        if (mDebug) {
            Slog.d(tag, "" + msg);
        }
    }

    private int getInputMethodRefreshId() {
        int i = this.mLowRefreshRateId;
        String str = TAG;
        logd(str, "InputMethodShown, BinderUtils.queryMainboardTemperature() = " + BinderUtils.queryMainboardTemperature());
        if (BinderUtils.queryMainboardTemperature() > INPUT_METHOD_CPU_TEMP_THRESHOLD) {
            logd(str, "InputMethodShown, return modeId = " + this.mLowRefreshRateId);
            int refreshRateId = this.mLowRefreshRateId;
            return refreshRateId;
        }
        int curSettingModelHighRefreshRateId = getCurSettingModeHighRefreshRateId();
        logd(str, "InputMethodShown, return modeId = " + curSettingModelHighRefreshRateId);
        return curSettingModelHighRefreshRateId;
    }

    private void inputMethodSceneCome(boolean shown) {
        if (shown) {
            this.mHandler.removeCallbacks(this.mInputMethodRunnable);
            this.mHandler.postDelayed(this.mInputMethodRunnable, 700L);
            this.mInputMethodRefreshRateId = getInputMethodRefreshId();
            return;
        }
        this.mHandler.removeCallbacks(this.mInputMethodRunnable);
        this.mInputMethodShown = false;
        logd(TAG, "inputMethodSceneCome  mInputMethodShown false.");
        ITranMagellanFeature.instance().sceneTrack(1, false);
    }

    private void screenFromOffToOnSceneCome() {
        try {
            logd(TAG, "screenFromOffToOnSceneCome enter");
            this.mSettingsObserver.isDisplayStateChangeFromOffToOn = true;
            this.mHandler.removeCallbacks(this.mScreenFromOffToOnRunnable);
            this.mHandler.postDelayed(this.mScreenFromOffToOnRunnable, 5000L);
        } catch (Exception e) {
            this.mSettingsObserver.isDisplayStateChangeFromOffToOn = false;
            logd(TAG, "screenFromOffToOnSceneCome mHandler exception");
        }
    }

    private void opticalDataAcquisitionSceneCome() {
        try {
            logd(TAG, "opticalDataAcquisitionSceneCome enter");
            this.mSettingsObserver.isOpticalDataAcquisitionOn = true;
            this.mHandler.removeCallbacks(this.mOpticalDataAcquisitionSceneComeRunnable);
            this.mHandler.postDelayed(this.mOpticalDataAcquisitionSceneComeRunnable, 5000L);
        } catch (Exception e) {
            this.mSettingsObserver.isOpticalDataAcquisitionOn = false;
            logd(TAG, "opticalDataAcquisitionSceneCome mHandler exception");
        }
    }

    public boolean isInDisplayStateChangeFromOffToOn() {
        return this.mSettingsObserver.isInDisplayStateChangeFromOffToOn();
    }

    public boolean isInScreenAnimationState() {
        return this.mSettingsObserver.isInScreenAnimationState();
    }

    public boolean isInScreenDimState() {
        return this.mSettingsObserver.isInScreenDimState();
    }

    public boolean isInOpticalDataAcquisitionState() {
        return this.mSettingsObserver.isInOpticalDataAcquisitionState();
    }

    public void hookUpdateRefreshRateForScene(Bundle b) {
        int i;
        if (b == null || !isSystemReady()) {
            return;
        }
        String sceneName = b.getString(KEY_SCENE_NAME);
        boolean isNeedSendRequest = true;
        char c = 65535;
        switch (sceneName.hashCode()) {
            case -1035338352:
                if (sceneName.equals(ITEM_SCREEN_DIM_SCENE)) {
                    c = 4;
                    break;
                }
                break;
            case -583798658:
                if (sceneName.equals(ITEM_SLIDE_SCENE)) {
                    c = 0;
                    break;
                }
                break;
            case -38665701:
                if (sceneName.equals(ITEM_SCREEN_LIGHT_ANIMATOR_SCENE)) {
                    c = 5;
                    break;
                }
                break;
            case 875424259:
                if (sceneName.equals(ITEM_INPUT_METHOD_SCENE)) {
                    c = 2;
                    break;
                }
                break;
            case 1158590657:
                if (sceneName.equals(ITEM_OPTICAL_DATA_ACQUISITION_SCENE)) {
                    c = 7;
                    break;
                }
                break;
            case 1534240417:
                if (sceneName.equals(ITEM_NAVIGATION_SCENE)) {
                    c = 1;
                    break;
                }
                break;
            case 1626528522:
                if (sceneName.equals(ITEM_ACTIVITY_SCENE)) {
                    c = 3;
                    break;
                }
                break;
            case 1925923005:
                if (sceneName.equals(ITEM_DISPLAY_STATE_SCENE)) {
                    c = 6;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                String scenePkgName = b.getString(KEY_PACKAGE_NAME);
                if (!TextUtils.isEmpty(scenePkgName)) {
                    this.mScenePkgName = b.getString(KEY_PACKAGE_NAME, DEFAULT_VALUE);
                    this.mSlideSceneState = b.getInt(KEY_SLICE_SCENE_STATE);
                    logd(TAG, "Slice_SCENE PkgName" + this.mScenePkgName + " SlideSceneState" + this.mSlideSceneState);
                    break;
                } else {
                    isNeedSendRequest = false;
                    logd(TAG, "Slice_SCENE PkgName is empty ");
                    break;
                }
            case 1:
                boolean hasFgServices = b.getBoolean(KEY_HAS_FOREGROUND_SERVICES, false);
                String pkgName = b.getString(KEY_PACKAGE_NAME, DEFAULT_VALUE);
                int fgTypes = b.getInt(KEY_FG_SERVICE_TYPES);
                String str = TAG;
                logd(str, "pkg_name " + pkgName + " uid " + b.getInt("uid") + " has_foreground_services " + hasFgServices + " fg_service_types " + fgTypes);
                if (hasFgServices && (fgTypes & 8) != 0) {
                    ITranMagellanFeature.instance().sceneTrack(0, true);
                    this.mNaviAppPackages.add(pkgName);
                } else if (!hasFgServices) {
                    ITranMagellanFeature.instance().sceneTrack(0, false);
                    this.mNaviAppPackages.remove(pkgName);
                }
                logd(str, "mNaviAppPackages " + this.mNaviAppPackages);
                break;
            case 2:
                boolean shown = b.getBoolean(KEY_IM_SHOW, false);
                logd(TAG, "InputMethod " + shown);
                inputMethodSceneCome(shown);
                break;
            case 3:
                isNeedSendRequest = activityStateSceneCome(b);
                break;
            case 4:
                isNeedSendRequest = false;
                if (isLTPOScreen()) {
                    this.mSettingsObserver.mPolicyState = b.getInt(KEY_SCREEN_POLICY_STATE);
                    String str2 = TAG;
                    logd(str2, "ScreenDimScene mSettingsObserver.mPolicyState=" + this.mSettingsObserver.mPolicyState);
                    if (isInOpticalDataAcquisitionState()) {
                        logd(str2, "In OpticalDataAcquisitionState, don't handle ITEM_SCREEN_DIM_SCENE");
                        break;
                    } else if (isInDisplayStateChangeFromOffToOn()) {
                        logd(str2, "is In DisplayStateChangeFromOffToOn,don't handle ITEM_SCREEN_DIM_SCENE");
                        break;
                    } else {
                        boolean isBoostRefreshRate = 2 == this.mSettingsObserver.mPolicyState;
                        updateRefreshRateAfterBrightnessChange(isBoostRefreshRate, 60.0f);
                        break;
                    }
                }
                break;
            case 5:
                isNeedSendRequest = false;
                if (isLTPOScreen()) {
                    this.mSettingsObserver.mScreenLightChangeAnimatorSceneState = b.getInt(KEY_SCREEN_SCREEN_LIGHT_CHANGE_ANIMATOR_STATE);
                    String str3 = TAG;
                    logd(str3, "ScreenLightAnimatorScene mScreenLightChangeAnimatorSceneState=" + this.mSettingsObserver.mScreenLightChangeAnimatorSceneState);
                    if (isInScreenDimState()) {
                        logd(str3, "In isInScreenDimState, don't handle ITEM_SCREEN_LIGHT_ANIMATOR_SCENE");
                        break;
                    } else if (isInOpticalDataAcquisitionState()) {
                        logd(str3, "In OpticalDataAcquisitionState, don't handle ITEM_SCREEN_LIGHT_ANIMATOR_SCENE");
                        break;
                    } else if (isInDisplayStateChangeFromOffToOn()) {
                        logd(str3, "is In DisplayStateChangeFromOffToOn,don't handle ITEM_SCREEN_LIGHT_ANIMATOR_SCENE");
                        break;
                    } else {
                        boolean isBoostRefreshRate2 = isInScreenAnimationState();
                        updateRefreshRateAfterBrightnessChange(isBoostRefreshRate2, 60.0f);
                        break;
                    }
                }
                break;
            case 6:
                isNeedSendRequest = false;
                if (isLTPOScreen()) {
                    this.mDisplayOldState = b.getInt(KEY_DISPLAY_OLD_STATE);
                    this.mDisplayNewState = b.getInt(KEY_DISPLAY_NEW_STATE);
                    String str4 = TAG;
                    logd(str4, "DisplayStateScene mDisplayOldState=" + this.mDisplayOldState + " mDisplayNewState=" + this.mDisplayNewState);
                    if (isInOpticalDataAcquisitionState()) {
                        logd(str4, "In OpticalDataAcquisitionState, don't handle ITEM_SCREEN_DIM_SCENE");
                        break;
                    } else if (this.mDisplayNewState == 2 && ((i = this.mDisplayOldState) == 1 || i == 3 || i == 4)) {
                        updateRefreshRateAfterBrightnessChange(true, getCurSettingModeHighRefreshRate());
                        screenFromOffToOnSceneCome();
                        break;
                    }
                }
                break;
            case 7:
                isNeedSendRequest = false;
                if (isLTPOScreen()) {
                    logd(TAG, "OpticalDataAcquisitionScene come");
                    updateRefreshRateAfterBrightnessChange(true, getCurSettingModeHighRefreshRate());
                    opticalDataAcquisitionSceneCome();
                    break;
                }
                break;
        }
        WindowManagerService.TranWindowManagerServiceProxy tranWindowManagerServiceProxy = this.mWmsProxy;
        if (tranWindowManagerServiceProxy != null && isNeedSendRequest) {
            tranWindowManagerServiceProxy.requestTraversal();
        }
    }

    private void highRefreshRateLimitTime(int time_out) {
        String str = TAG;
        logd(str, "highRefreshRateLimitTime time_out: " + time_out);
        try {
            logd(str, "highRefreshRateLimitTime enter");
            this.mSettingsObserver.mIsHighRefreshRateLimitTimeOn = true;
            this.mHandler.removeCallbacks(this.mHighRefreshRateLimitTimeSceneComeRunnable);
            this.mHandler.postDelayed(this.mHighRefreshRateLimitTimeSceneComeRunnable, time_out);
        } catch (Exception e) {
            this.mSettingsObserver.mIsHighRefreshRateLimitTimeOn = false;
            logd(TAG, "highRefreshRateLimitTime mHandler exception");
        }
    }

    public boolean activityStateSceneCome(Bundle b) {
        int old_state = b.getInt("old_state");
        int new_state = b.getInt("new_state");
        String pkgName = b.getString(KEY_PACKAGE_NAME);
        String activityName = b.getString("activity_name");
        String str = TAG;
        logd(str, "activityStateSceneCome old_state:" + old_state + " new_state:" + new_state + " pkgName:" + pkgName + " activityName:" + activityName);
        TranBaseRefreshRateConfig tranBaseRefreshRateConfig = this.mTranRefreshRateConfig;
        if (tranBaseRefreshRateConfig != null && tranBaseRefreshRateConfig.getTransitionAnimationList().contains(activityName)) {
            logd(str, "activityStateSceneCome highRefreshRateLimitTime");
            highRefreshRateLimitTime(800);
            return false;
        }
        return false;
    }

    public void updateRefreshRateAfterBrightnessChange(boolean isBoostRefreshRate, float refreshRate) {
        if (isBoostRefreshRate) {
            logd(TAG, "updateRefreshRateAfterBrightnessChange isBoostRefreshRate is true");
            BinderUtils.setScreenDimToSF();
            this.mSettingsObserver.updateRefreshRateMinValueAfterBrightnessChange(refreshRate, false);
            return;
        }
        logd(TAG, "updateRefreshRateAfterBrightnessChange mMinRefreshRate:" + this.mSettingsObserver.getMinRefreshRate());
        BaseRefreshRateSettingsObserver baseRefreshRateSettingsObserver = this.mSettingsObserver;
        baseRefreshRateSettingsObserver.updateRefreshRateMinValueAfterBrightnessChange(baseRefreshRateSettingsObserver.getMinRefreshRate(), true);
    }

    public boolean isLTPOScreen() {
        return this.mSettingsObserver.isLTPOScreen();
    }

    public int getCurSettingModeHighRefreshRateId() {
        if (is60HzMode()) {
            return this.mLowRefreshRateId;
        }
        if (is90HzMode()) {
            return this.m90HZRefreshRateId;
        }
        return this.mHighRefreshRateId;
    }

    public float getCurSettingModeHighRefreshRate() {
        return this.mSettingsObserver.getCurSettingModeHighRefreshRate();
    }

    public boolean isBigScreen() {
        return this.mCurrentAppWidth >= 2000 && this.mCurrentAppHeight >= 2000;
    }

    public boolean isSupportLessThan60hz() {
        if (isInOpticalDataAcquisitionState()) {
            logd(TAG, "In isInOpticalDataAcquisitionState");
            return false;
        } else if (isInScreenAnimationState()) {
            logd(TAG, "in isInScreenAnimationState");
            return false;
        } else if (isInDisplayStateChangeFromOffToOn()) {
            logd(TAG, "in isInDisplayStateChangeFromOffToOn");
            return false;
        } else if (isLowerThanBrightnessThreshold()) {
            logd(TAG, "in isLowerThanBrightnessThreshold");
            return false;
        } else {
            logd(TAG, "isSupportLessThan60hz is true");
            return true;
        }
    }

    public boolean isLowerThanBrightnessThreshold() {
        return this.mSettingsObserver.isLowerThanBrightnessThreshold();
    }

    private boolean isPlayingNativeVideo(int videoState) {
        if (4 == videoState || 5 == videoState) {
            return true;
        }
        return false;
    }

    private boolean isPlayingVideo(int videoState) {
        if (1 == videoState || 2 == videoState) {
            return true;
        }
        return false;
    }

    private void resetVideoandVoiceState() {
        this.mNativeVideoSceneState = 6;
        this.mVideoSceneState = 3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isNeedTouchEvent() {
        boolean isInVoiceCall = isVoiceActivityInTop();
        logd(TAG, "isNeedTouchEvent  video_state:" + this.mVideoSceneState + " mVideoAppPid:" + this.mVideoAppPid + " mNativeVideoSceneState:" + this.mNativeVideoSceneState + " isInVoiceCall:" + isInVoiceCall + " mSlideSceneState:" + this.mSlideSceneState);
        if (this.mSlideSceneState != 0) {
            resetVideoandVoiceState();
            return false;
        } else if (!isPlayingVideo(this.mVideoSceneState) && !isPlayingNativeVideo(this.mNativeVideoSceneState) && !isInVoiceCall) {
            return false;
        } else {
            return true;
        }
    }

    public void hookupdateRefreshRateForVideoScene(int videoState, int videoFps, int videoSessionId) {
        if (TRAN_REFRESH_RATE_VIDEO_SCENE_DETECTOR_SUPPORT) {
            if (!isSystemReady()) {
                Slog.d(TAG, "isSystemReady is false");
            } else if (this.mSlideSceneState != 0) {
                logd(TAG, "updateRefreshRateForVideoScene resetVideoandVoiceState is false");
                resetVideoandVoiceState();
            } else {
                String str = TAG;
                logd(str, "updateRefreshRateForVideoScene  video_state:" + videoState + " mVideoAppPid:" + videoSessionId + " mVideoFPS:" + videoFps + " mNativeVideoSceneState:" + this.mNativeVideoSceneState);
                if (isPlayingVideo(this.mVideoSceneState) && isPlayingNativeVideo(videoState)) {
                    return;
                }
                if (isPlayingNativeVideo(this.mNativeVideoSceneState) && isPlayingVideo(videoState)) {
                    return;
                }
                if ((this.mNativeVideoSceneState == videoState && this.mVideoAppPid == videoSessionId && this.mVideoFPS == videoFps) || (this.mVideoSceneState == videoState && this.mVideoAppPid == videoSessionId && this.mVideoFPS == videoFps)) {
                    logd(str, "updateRefreshRateForVideoScene same video play state, don't need to change requestTraversal");
                } else if (!isVideoAppInTop()) {
                    logd(str, "isVideoAppInTop is false");
                } else {
                    if (videoState > 3) {
                        this.mVideoSceneState = 0;
                        this.mNativeVideoSceneState = videoState;
                        this.mVideoFPS = videoFps;
                        this.mVideoAppPid = videoSessionId;
                    } else {
                        this.mNativeVideoSceneState = 0;
                        this.mVideoSceneState = videoState;
                        this.mVideoFPS = videoFps;
                        this.mVideoAppPid = videoSessionId;
                    }
                    if (this.mWmsProxy != null) {
                        logd(str, "[video-detector] hookupdateRefreshRateForVideoScene requestTraversal");
                        this.mWmsProxy.requestTraversal();
                    }
                }
            }
        }
    }

    private boolean isVideoAppInTop() {
        String topApp = this.mTranRefreshRateConfig.mResumedPkgName;
        logd(TAG, "isVideoAppInTop topApp:" + topApp);
        for (String pkgName : this.mTranRefreshRateConfig.getVideoAppSupportedList()) {
            if (pkgName.equals(topApp)) {
                logd(TAG, "isVideoAppInTop is true");
                return true;
            }
        }
        logd(TAG, "isVideoAppInTop is false");
        return false;
    }

    private boolean isVoiceActivityInTop() {
        boolean isInVoiceApp = false;
        String topApp = this.mTranRefreshRateConfig.mResumedPkgName;
        logd(TAG, "isVoiceActivityInTop topApp:" + topApp);
        Iterator<String> it = this.mTranRefreshRateConfig.getVoiceCallWhiteList().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            String activityName = it.next();
            String[] pkgNames = activityName.split(SliceClientPermissions.SliceAuthority.DELIMITER);
            String str = TAG;
            logd(str, "isVoiceActivityInTop pkgNames:" + pkgNames);
            if (pkgNames.length > 0) {
                String pkgName = pkgNames[0];
                logd(str, "isVoiceActivityInTop pkgName:" + pkgName);
                if (pkgName.equals(topApp)) {
                    logd(str, "isVoiceActivityInTop the top app is voice app");
                    isInVoiceApp = true;
                    break;
                }
            }
        }
        if (isInVoiceApp) {
            String topActivity = getTopActivity();
            for (String pkgName2 : this.mTranRefreshRateConfig.getVoiceCallWhiteList()) {
                if (pkgName2.equals(topActivity)) {
                    logd(TAG, "isVoiceActivityInTop is true");
                    return true;
                }
            }
        }
        String topActivity2 = TAG;
        logd(topActivity2, "isVoiceActivityInTop is false");
        return false;
    }

    public boolean inNaviAppPackages(String pkg) {
        if (!ITranMagellanFeature.instance().isSupportNavigation()) {
            logd(TAG, "[NavigationScene] navigation scene is closed by cloud!");
            return false;
        }
        return this.mNaviAppPackages.contains(pkg);
    }

    public boolean inVoiceCallScene(WindowState w, String lastTitle, boolean isVoiceCall) {
        if (!ITranMagellanFeature.instance().isSupportAudio()) {
            logd(TAG, "[VoiceCallScene] voice call scene is closed by cloud!");
            return false;
        } else if (!TRAN_REFRESH_RATE_VIDEO_SCENE_DETECTOR_SUPPORT || w.isAnimatingLw()) {
            return false;
        } else {
            this.mIsInVoiceCall = false;
            if (isVoiceCall) {
                this.mNativeVideoSceneState = 6;
                this.mVideoSceneState = 3;
                registerPointerEventListener();
                this.mIsInVoiceCall = true;
                if (this.mTouchTimer.isInTouchPeroid()) {
                    logd(TAG, "[video-detector] voice app isInTouchPeroid");
                } else {
                    this.mTouchTimer.reset();
                    logd(TAG, "[video-detector]  voice app touchTimer.reset()");
                    return true;
                }
            }
            return false;
        }
    }

    public int getVideoRefreshRateId(boolean isNativeVideo) {
        int refreshRateId;
        int initRefreshRateId = this.m30HzRefreshRateId;
        if (!isNativeVideo) {
            initRefreshRateId = this.m45HzRefreshRateId;
        }
        int i = this.mVideoFPS;
        if (i <= 50) {
            refreshRateId = initRefreshRateId;
        } else if (i <= 60) {
            refreshRateId = this.mLowRefreshRateId;
        } else if (i <= 90) {
            refreshRateId = this.m90HZRefreshRateId;
        } else {
            refreshRateId = this.mHighRefreshRateId;
        }
        logd(TAG, "getVideoRefreshRateId refreshRateId:" + refreshRateId);
        return refreshRateId;
    }

    public boolean inNativeVideoScene(WindowState w, String lastTitle) {
        if (!ITranMagellanFeature.instance().isSupportVideo()) {
            logd(TAG, "[NativeVideoScene] native video scene is closed by cloud!");
            return false;
        } else if (!TRAN_REFRESH_RATE_VIDEO_SCENE_DETECTOR_SUPPORT || w.isAnimatingLw()) {
            return false;
        } else {
            int i = this.mNativeVideoSceneState;
            if (i == 4 || i == 5) {
                this.mVideoSceneState = 3;
                if (isForegroundApp(this.mVideoAppPid)) {
                    registerPointerEventListener();
                    if (!this.mTouchTimer.isInTouchPeroid()) {
                        this.mTouchTimer.reset();
                        return true;
                    }
                } else {
                    unregisterPointerEventListener();
                    this.mNativeVideoSceneState = 6;
                }
            }
            return false;
        }
    }

    public boolean inVideoScene(WindowState w, String lastTitle) {
        if (!ITranMagellanFeature.instance().isSupportVideo()) {
            logd(TAG, "[VideoScene]  video scene is closed by cloud!");
            return false;
        } else if (!TRAN_REFRESH_RATE_VIDEO_SCENE_DETECTOR_SUPPORT || w.isAnimatingLw()) {
            return false;
        } else {
            int i = this.mVideoSceneState;
            if (i == 1 || i == 2) {
                this.mNativeVideoSceneState = 6;
                if (isForegroundApp(this.mVideoAppPid)) {
                    registerPointerEventListener();
                    if (this.mTouchTimer.isInTouchPeroid()) {
                        logd(TAG, "[video-detector]  video app isInTouchPeroid");
                    } else {
                        this.mTouchTimer.reset();
                        logd(TAG, "[video-detector]  video app touchTimer.reset()");
                        return true;
                    }
                } else {
                    unregisterPointerEventListener();
                    this.mVideoSceneState = 3;
                }
            }
            return false;
        }
    }

    public boolean inputMethodScene() {
        if (!ITranMagellanFeature.instance().isSupportInputMethod()) {
            logd(TAG, "[inputMethodScene]  input method scene is closed by cloud!");
            return false;
        }
        return this.mInputMethodShown;
    }

    protected boolean isForegroundApp(int pid) {
        ActivityTaskManagerInternal activityTaskManagerInternal = this.mActivityTaskManagerInternal;
        if (activityTaskManagerInternal != null) {
            WindowProcessController wpControl = activityTaskManagerInternal.getTopApp();
            String str = TAG;
            logd(str, "[video-detector] refreshrate isForegroundApp wpControl=" + wpControl);
            if (wpControl != null) {
                logd(str, "[video-detector] refreshrate isForegroundApp taskInfo1=" + wpControl.getPid());
                return wpControl.getPid() == pid;
            }
        }
        return false;
    }

    protected boolean isTopApp(String pkgName) {
        ActivityTaskManagerInternal activityTaskManagerInternal = this.mActivityTaskManagerInternal;
        if (activityTaskManagerInternal != null) {
            return activityTaskManagerInternal.inTopVisiblePackages(pkgName);
        }
        return false;
    }

    protected String getTopActivity() {
        ActivityTaskManagerInternal activityTaskManagerInternal = this.mActivityTaskManagerInternal;
        if (activityTaskManagerInternal != null) {
            return activityTaskManagerInternal.getTopActivity();
        }
        return "";
    }

    protected boolean isSystemReady() {
        return this.mSettingsObserver.isSystemReady();
    }

    protected void initRefreshRate(DisplayInfo displayInfo) {
    }

    protected boolean isDefaultMode() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean is60HzMode() {
        return this.mSettingsObserver.is60HzMode();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isAutoMode() {
        return this.mSettingsObserver.isAutoMode();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean is90HzMode() {
        return this.mSettingsObserver.is90HzMode();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isLowPowerMode() {
        return this.mSettingsObserver.mLowPowerMode;
    }

    public void dump(PrintWriter pw) {
    }

    public void setDebug(PrintWriter pw, boolean on) {
        mDebug = on;
        pw.println("TranRefreshRate policy debugable:" + on);
    }

    public void start() {
    }

    public int getPreferredModeId(WindowState w, ITranRefreshRatePolicyProxy proxy) {
        return 0;
    }

    public String getFeatureInfo() {
        return "";
    }

    public String getCurrentState(ITranRefreshRatePolicyProxy proxy) {
        return "";
    }

    public float getPreferredMinRefreshRate(WindowState w, ITranRefreshRatePolicyProxy proxy) {
        if (w != null && proxy != null) {
            if (w.getAttrs().preferredMinDisplayRefreshRate > 0.0f) {
                return w.getAttrs().preferredMinDisplayRefreshRate;
            }
            w.getOwningPackage();
            WindowState.TranWindowStateProxy windowStateProxy = new WindowState.TranWindowStateProxy(w);
            String lastTitle = windowStateProxy.getLastTitle();
            if (lastTitle != null && ((lastTitle.equals(KEY_TRANSSION_FINGER_SENSOR_WINDOW) || lastTitle.equals(KEY_SYS_DREAM)) && w.isVisible())) {
                return proxy.getPreferredRefreshRate(w);
            }
            if (!w.isInMultiWindow()) {
                return 0.0f;
            }
            return proxy.getPreferredRefreshRate(w);
        }
        String packageName = TAG;
        Slog.d(packageName, "getPreferredMinRefreshRate w or proxy is null.");
        return 0.0f;
    }

    public float getPreferredMaxRefreshRate(WindowState w, ITranRefreshRatePolicyProxy proxy) {
        if (w != null && proxy != null) {
            if (w.getAttrs().preferredMaxDisplayRefreshRate > 0.0f) {
                return w.getAttrs().preferredMaxDisplayRefreshRate;
            }
            w.getOwningPackage();
            WindowState.TranWindowStateProxy windowStateProxy = new WindowState.TranWindowStateProxy(w);
            String lastTitle = windowStateProxy.getLastTitle();
            if (lastTitle != null && ((lastTitle.equals(KEY_TRANSSION_FINGER_SENSOR_WINDOW) || lastTitle.equals(KEY_SYS_DREAM)) && w.isVisible())) {
                return proxy.getPreferredRefreshRate(w);
            }
            if (!w.isInMultiWindow()) {
                return 0.0f;
            }
            return proxy.getPreferredRefreshRate(w);
        }
        String packageName = TAG;
        Slog.d(packageName, "getPreferredMaxRefreshRate w or proxy is null.");
        return 0.0f;
    }

    /* loaded from: classes2.dex */
    private final class RefreshRateHandler extends Handler {
        RefreshRateHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "[video-detector] RefreshRateHandler  handleMessage");
                    if (TranBaseRefreshRatePolicy.this.mWmsProxy != null) {
                        TranBaseRefreshRatePolicy.this.mWmsProxy.requestTraversal();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes2.dex */
    class TouchTimer {
        private RefreshRateHandler handler;
        private long startTimeMillis;
        private boolean started;
        private Timer timer = null;
        private TimerTask task = null;

        public TouchTimer() {
            this.handler = null;
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }
            this.handler = new RefreshRateHandler();
        }

        private void startTime() {
            try {
                this.timer = new Timer();
                TimerTask timerTask = new TimerTask() { // from class: com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.TouchTimer.1
                    @Override // java.util.TimerTask, java.lang.Runnable
                    public void run() {
                        try {
                            Message message = new Message();
                            message.what = 1;
                            TouchTimer.this.handler.sendMessage(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                this.task = timerTask;
                this.timer.schedule(timerTask, 1200L);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void stopTime() {
            Timer timer = this.timer;
            if (timer != null) {
                timer.cancel();
            }
            this.timer = null;
        }

        public void reset() {
            this.started = false;
            stopTime();
        }

        public void start() {
            if (!this.started) {
                this.startTimeMillis = SystemClock.uptimeMillis();
                this.started = true;
                stopTime();
                startTime();
            }
        }

        public boolean isRunning() {
            return this.started;
        }

        public boolean isInTouchPeroid() {
            if (this.started) {
                if (totalDurationSec() < 1200) {
                    TranBaseRefreshRatePolicy.this.mIsTouchScreen = true;
                    return true;
                }
                TranBaseRefreshRatePolicy.this.mIsTouchScreen = false;
                return false;
            }
            TranBaseRefreshRatePolicy.this.mIsTouchScreen = false;
            return false;
        }

        public long totalDurationSec() {
            if (this.started) {
                return SystemClock.uptimeMillis() - this.startTimeMillis;
            }
            return 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class TaskTapPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
        TaskTapPointerEventListener() {
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (!TranBaseRefreshRatePolicy.this.isNeedTouchEvent()) {
                TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "[video-detector] isNeedTouchEvent is false");
                TranBaseRefreshRatePolicy.this.unregisterPointerEventListener();
                return;
            }
            TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "[video-detector] isNeedTouchEvent is true");
            if (TranBaseRefreshRatePolicy.this.mWmsProxy != null && !TranBaseRefreshRatePolicy.this.mIsTouchScreen) {
                TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "[video-detector] onPointerEvent requestTraversal");
                TranBaseRefreshRatePolicy.this.mWmsProxy.requestTraversal();
            }
            TranBaseRefreshRatePolicy.this.mIsTouchScreen = true;
            TranBaseRefreshRatePolicy.this.mTouchTimer.reset();
            TranBaseRefreshRatePolicy.this.mTouchTimer.start();
        }
    }

    public void registerPointerEventListener() {
        if (!this.mIsRegisterListener) {
            unregisterPointerEventListener();
            try {
                WindowManagerService.TranWindowManagerServiceProxy tranWindowManagerServiceProxy = this.mWmsProxy;
                if (tranWindowManagerServiceProxy != null) {
                    tranWindowManagerServiceProxy.registerPointerEventListener(this.mListener);
                }
                this.mIsRegisterListener = true;
            } catch (Exception e) {
                Slog.d(TAG, "[video-detector] refreshrate registerPointerEventListener onPointerEvent: e=" + e);
            }
        }
    }

    public void unregisterPointerEventListener() {
        if (this.mIsRegisterListener) {
            try {
                WindowManagerService.TranWindowManagerServiceProxy tranWindowManagerServiceProxy = this.mWmsProxy;
                if (tranWindowManagerServiceProxy != null) {
                    tranWindowManagerServiceProxy.unregisterPointerEventListener(this.mListener);
                }
                this.mIsRegisterListener = false;
            } catch (Exception e) {
                Slog.d(TAG, "[video-detector] refreshrate unregisterPointerEventListener onPointerEvent: e=" + e);
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class BaseRefreshRateSettingsObserver extends ContentObserver {
        protected static final String LAST_TRAN_REFRESH_MODE_IN_REFRESH_SETTING = "last_tran_refresh_mode_in_refresh_setting";
        protected static final int SCREEN_BRIGHTNESS_THRESHOLD_VALUE_FOR_LOWEST_HZ = 46;
        protected static final String SCREEN_RECORD_RUNNING = "screen_record_running";
        private static final String TAG = "RefreshRateSettingsObserver";
        protected static final String TRAN_LONG_BATTERY_MODE = "tran_settings_long_battery_mode";
        protected static final String TRAN_NEED_RECOVERY_REFRESH_MODE = "tran_need_recovery_refresh_mode";
        protected static final String TRAN_REFRESH_MODE = "tran_refresh_mode";
        protected boolean isDisplayStateChangeFromOffToOn;
        protected boolean isOpticalDataAcquisitionOn;
        protected final Context mContext;
        protected float mCurrentMinRefreshRate;
        protected boolean mIsHighRefreshRateLimitTimeOn;
        public boolean mIsSystemReady;
        protected final Uri mLongBatteryModeSettings;
        protected boolean mLowPowerMode;
        protected final Uri mLowPowerModeSetting;
        protected float mMinRefreshRate;
        protected float mPeakRefreshRate;
        protected final Uri mPeakRefreshRateSetting;
        protected int mPolicyState;
        protected int mRefreshRateMode;
        protected final Uri mRefreshRateModeSettings;
        protected int mScreenBright;
        protected final Uri mScreenBrightSetting;
        protected int mScreenLightChangeAnimatorSceneState;
        protected boolean mScreenRecordRunning;
        protected final Uri mScreenRecordRunningSetting;
        protected final Uri mSetupCompleteSetting;
        protected boolean mUnconsciousSwitch;
        protected boolean mUserSetupCompleted;
        protected final WindowManagerService mWms;
        private WindowManagerService.TranWindowManagerServiceProxy mWmsProxy;

        /* JADX INFO: Access modifiers changed from: package-private */
        public BaseRefreshRateSettingsObserver(Context context, WindowManagerService wm, Handler handler) {
            super(handler);
            this.mRefreshRateModeSettings = Settings.System.getUriFor(TRAN_REFRESH_MODE);
            this.mLongBatteryModeSettings = Settings.System.getUriFor(TRAN_LONG_BATTERY_MODE);
            this.mPeakRefreshRateSetting = Settings.System.getUriFor("peak_refresh_rate");
            this.mLowPowerModeSetting = Settings.Global.getUriFor("low_power");
            this.mScreenRecordRunningSetting = Settings.Global.getUriFor(SCREEN_RECORD_RUNNING);
            this.mScreenBrightSetting = Settings.System.getUriFor("screen_brightness");
            this.mMinRefreshRate = 10.0f;
            this.mCurrentMinRefreshRate = 10.0f;
            this.isDisplayStateChangeFromOffToOn = false;
            this.isOpticalDataAcquisitionOn = false;
            this.mPolicyState = 0;
            this.mIsHighRefreshRateLimitTimeOn = false;
            this.mSetupCompleteSetting = Settings.Secure.getUriFor("user_setup_complete");
            this.mUserSetupCompleted = true;
            this.mUnconsciousSwitch = false;
            this.mIsSystemReady = false;
            this.mWms = wm;
            this.mWmsProxy = new WindowManagerService.TranWindowManagerServiceProxy(wm);
            this.mContext = context;
            this.mPeakRefreshRate = Settings.System.getFloatForUser(context.getContentResolver(), "peak_refresh_rate", -1.0f, 0);
            this.mRefreshRateMode = getRefreshRateMode();
            this.mLowPowerMode = Settings.Global.getInt(context.getContentResolver(), "low_power", 0) != 0;
            this.mScreenBright = Settings.System.getInt(context.getContentResolver(), "screen_brightness", 0);
            TranBaseRefreshRatePolicy.logd(TAG, "construct curtRefreshRate:" + this.mPeakRefreshRate + ", curtRefreshMode:" + this.mRefreshRateMode + ", lowPowerMode:" + this.mLowPowerMode + ", mScreenBright:" + this.mScreenBright);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public float getRefreshRate() {
            DisplayInfo di;
            if (DisplayManagerGlobal.getInstance() != null && (di = DisplayManagerGlobal.getInstance().getDisplayInfo(0)) != null) {
                Display.Mode mode = di.getMode();
                if (mode != null) {
                    return di.getMode().getRefreshRate();
                }
                return 60.0f;
            }
            return 60.0f;
        }

        protected int getInitRefreshRateMode() {
            return 60;
        }

        public float getCurSettingModeHighRefreshRate() {
            if (is60HzMode()) {
                return 60.0f;
            }
            if (is90HzMode()) {
                return 90.0f;
            }
            return 120.0f;
        }

        protected boolean is60HzMode() {
            return 60 == this.mRefreshRateMode;
        }

        protected boolean isAutoMode() {
            return 10000 == this.mRefreshRateMode;
        }

        protected boolean is90HzMode() {
            return 90 == this.mRefreshRateMode;
        }

        protected boolean isFloatEqual(float a, float b) {
            return a > b - 0.01f && a < 0.01f + b;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public void observe() {
            ContentResolver cr = this.mContext.getContentResolver();
            cr.registerContentObserver(this.mRefreshRateModeSettings, false, this, 0);
            cr.registerContentObserver(this.mPeakRefreshRateSetting, false, this, 0);
            cr.registerContentObserver(this.mLowPowerModeSetting, false, this, 0);
            cr.registerContentObserver(this.mLongBatteryModeSettings, false, this, 0);
            cr.registerContentObserver(this.mScreenRecordRunningSetting, false, this, 0);
            cr.registerContentObserver(this.mSetupCompleteSetting, false, this, -1);
            if (isLTPOScreen()) {
                cr.registerContentObserver(this.mScreenBrightSetting, false, this, 0);
                updateScreenBrightSetting();
            }
            updateRefreshRateModeSettings();
            updatePeakRefreshRateSettings();
            updateLowPowerModeSettings();
            updateScreenRecordStatus();
            updateSetupCompleteSetting();
        }

        protected boolean isSystemReady() {
            ActivityManagerInternal activityManagerInternal;
            if (!this.mIsSystemReady && (activityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)) != null) {
                this.mIsSystemReady = activityManagerInternal.isSystemReady();
                TranBaseRefreshRatePolicy.logd(TAG, "[video-detector] activityManagerInternal.isSystemReady():" + this.mIsSystemReady);
                return this.mIsSystemReady;
            }
            return true;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mRefreshRateModeSettings.equals(uri)) {
                updateRefreshRateModeSettings();
            } else if (this.mPeakRefreshRateSetting.equals(uri)) {
                updatePeakRefreshRateSettings();
            } else if (this.mLowPowerModeSetting.equals(uri)) {
                updateLowPowerModeSettings();
            } else if (this.mLongBatteryModeSettings.equals(uri)) {
                updateLongBatteryModeSettings();
            } else if (this.mScreenRecordRunningSetting.equals(uri)) {
                updateScreenRecordStatus();
            } else if (this.mSetupCompleteSetting.equals(uri)) {
                updateSetupCompleteSetting();
            } else if (this.mScreenBrightSetting.equals(uri)) {
                updateScreenBrightSetting();
            }
        }

        protected int getRecoverRefreshRateMode() {
            return -1;
        }

        protected float getMinRefreshRate() {
            return this.mMinRefreshRate;
        }

        protected void updateLongBatteryModeSettings() {
            int longBatteryMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), TRAN_LONG_BATTERY_MODE, 1, 0);
            if (longBatteryMode == 0) {
                int recoverRefreshRateMode = getRecoverRefreshRateMode();
                Slog.d(TAG, "updateLongBatteryModeSettings, Need Recover LAST_TRAN_REFRESH_MODE_IN_SETTING = " + recoverRefreshRateMode);
                Settings.System.putIntForUser(this.mContext.getContentResolver(), TRAN_REFRESH_MODE, recoverRefreshRateMode, 0);
            }
        }

        protected void updateRefreshRateModeSettingsInternel() {
        }

        protected void updateRefreshRateModeSettings() {
            int refreshMode = getRefreshRateMode();
            if (DisplayPolicy.TRAN_AIPOWERLAB_SUPPORT) {
                if (60 == refreshMode && 60 == this.mRefreshRateMode) {
                    Slog.d(TAG, "Current refresh rate is 60Hz, refreshRate changed is 60Hz too, recoverRefreshRate Keep previous values = " + Settings.System.getIntForUser(this.mContext.getContentResolver(), TRAN_NEED_RECOVERY_REFRESH_MODE, -1, 0));
                } else {
                    int recoverRefreshRateMode = 60 == refreshMode ? this.mRefreshRateMode : refreshMode;
                    Slog.d(TAG, "Need Recover RefreshRateMode = " + recoverRefreshRateMode);
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), TRAN_NEED_RECOVERY_REFRESH_MODE, recoverRefreshRateMode, 0);
                }
            }
            this.mRefreshRateMode = refreshMode;
            updateRefreshRateModeSettingsInternel();
            WindowManagerService.TranWindowManagerServiceProxy tranWindowManagerServiceProxy = this.mWmsProxy;
            if (tranWindowManagerServiceProxy != null) {
                tranWindowManagerServiceProxy.requestTraversal();
            }
        }

        protected void updatePeakRefreshRateSettings() {
            float peakRefreshRate = Settings.System.getFloat(this.mContext.getContentResolver(), "peak_refresh_rate", getRefreshRate());
            this.mPeakRefreshRate = peakRefreshRate;
            Slog.d(TAG, "PeakRefreshRate changed:" + this.mPeakRefreshRate);
        }

        protected void updateLowPowerModeSettings() {
            boolean inLowPowerMode = Settings.Global.getInt(this.mContext.getContentResolver(), "low_power", 0) != 0;
            this.mLowPowerMode = inLowPowerMode;
            Slog.d(TAG, "LowPowerMode changed:" + this.mLowPowerMode);
        }

        protected void updateScreenRecordStatus() {
            boolean mScreenRecording = Settings.Global.getInt(this.mContext.getContentResolver(), SCREEN_RECORD_RUNNING, 0) == 1;
            this.mScreenRecordRunning = mScreenRecording;
            Slog.d(TAG, "Screen Record status changed: " + this.mScreenRecordRunning);
        }

        public boolean isLowerThanBrightnessThreshold() {
            return isLTPOScreen() && this.mScreenBright < 46;
        }

        public boolean isInDisplayStateChangeFromOffToOn() {
            TranBaseRefreshRatePolicy.logd(TAG, "isInDisplayStateChangeFromOffToOn isDisplayStateChangeFromOffToOn:" + this.isDisplayStateChangeFromOffToOn);
            return this.isDisplayStateChangeFromOffToOn;
        }

        public boolean isInOpticalDataAcquisitionState() {
            TranBaseRefreshRatePolicy.logd(TAG, "isInOpticalDataAcquisitionState isOpticalDataAcquisitionOn:" + this.isOpticalDataAcquisitionOn);
            return this.isOpticalDataAcquisitionOn;
        }

        public boolean isInScreenAnimationState() {
            TranBaseRefreshRatePolicy.logd(TAG, "isInScreenAnimationState mScreenLightChangeAnimatorSceneState:" + this.mScreenLightChangeAnimatorSceneState);
            return 1 == this.mScreenLightChangeAnimatorSceneState;
        }

        public boolean isInScreenDimState() {
            TranBaseRefreshRatePolicy.logd(TAG, "isInScreenDimState mPolicyState:" + this.mPolicyState);
            return 2 == this.mPolicyState;
        }

        public boolean isLTPOScreen() {
            if (this.mMinRefreshRate < 59.99f) {
                return true;
            }
            return false;
        }

        public boolean isInHighRefreshRateLimitTimeOn() {
            TranBaseRefreshRatePolicy.logd(TAG, "isInHighRefreshRateLimitTimeOn mIsHighRefreshRateLimitTimeOn:" + this.mIsHighRefreshRateLimitTimeOn);
            return this.mIsHighRefreshRateLimitTimeOn;
        }

        public boolean updateRefreshRateMinValueAfterBrightnessChange(float refreshRate, boolean isCheckBrightness) {
            float minRefreshRate = refreshRate;
            TranBaseRefreshRatePolicy.logd(TAG, "updateRefreshRateMinValueAfterBrightnessChange mScreenLightChangeAnimatorSceneState=" + this.mScreenLightChangeAnimatorSceneState);
            if (isCheckBrightness && isLowerThanBrightnessThreshold()) {
                minRefreshRate = 60.0f;
            }
            TranBaseRefreshRatePolicy.logd(TAG, "mScreenBright=" + this.mScreenBright + " minRefreshRate=" + minRefreshRate + " mCurrentMinRefreshRate=" + this.mCurrentMinRefreshRate);
            float f = this.mCurrentMinRefreshRate;
            if (minRefreshRate >= f - 0.01f && minRefreshRate <= f + 0.01f) {
                return false;
            }
            Settings.System.putFloat(this.mContext.getContentResolver(), "min_refresh_rate", minRefreshRate);
            this.mCurrentMinRefreshRate = minRefreshRate;
            return true;
        }

        public void updateScreenBrightSetting() {
            this.mScreenBright = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", 0, 0);
            if (isInOpticalDataAcquisitionState()) {
                TranBaseRefreshRatePolicy.logd(TAG, "isInOpticalDataAcquisitionState is started, don't need to change min refresh rate according to screen bright");
            } else if (isInScreenAnimationState()) {
                TranBaseRefreshRatePolicy.logd(TAG, "mScreenLightChangeAnimatorSceneState is started, don't need to change min refresh rate according to screen bright");
            } else if (isInDisplayStateChangeFromOffToOn()) {
                TranBaseRefreshRatePolicy.logd(TAG, "isInDisplayStateChangeFromOffToOn is true, don't need to change min refresh rate according to screen bright");
            } else {
                updateRefreshRateMinValueAfterBrightnessChange(this.mMinRefreshRate, true);
                TranBaseRefreshRatePolicy.logd(TAG, "ScreenBright changed:" + this.mScreenBright);
            }
        }

        public boolean isScreenRecordRunning() {
            return this.mScreenRecordRunning;
        }

        public boolean isUserSetupCompleted() {
            return this.mUserSetupCompleted;
        }

        public int getNeedRecoverRefreshState() {
            return Settings.System.getIntForUser(this.mContext.getContentResolver(), TRAN_NEED_RECOVERY_REFRESH_MODE, -1, 0);
        }

        public int checkRefreshChoose(int refreshChoose) {
            return refreshChoose;
        }

        private void updateSetupCompleteSetting() {
            boolean userSetupComplete = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) == 1;
            this.mUserSetupCompleted = userSetupComplete;
            Slog.d(TAG, "updateSetupCompleteSetting : changed " + this.mUserSetupCompleted);
        }

        protected int getRefreshRateMode() {
            int originRefreshRateMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), TRAN_REFRESH_MODE, -2, 0);
            int cmptRefreshRateMode = checkRefreshChoose(originRefreshRateMode);
            Slog.d(TAG, "RefreshRateMode orgin = " + originRefreshRateMode + " cmp" + cmptRefreshRateMode);
            return cmptRefreshRateMode;
        }
    }

    /* loaded from: classes2.dex */
    public static class TranBaseRefreshRateConfig {
        protected static final String AC8 = "TECNO-AC8";
        protected static final String ACTIVE_SWITCH_PROBLEM_PACKAGE_LIST = "active_switch_package_list";
        protected static final String APP_ABOVE_RESUMED_WINDOW_LIST = "app_above_resumed_window_list";
        protected static final String AUTO_REFRESH_RATE_WHITE_LIST = "auto_refresh_rate_whitelist";
        protected static final String CLOUD_TRANREFERSHRATE_120HZ_FILENAME = "/refresh_rate_config.json";
        protected static final String CLOUD_TRANREFRESHRATE_120HZ_BLACKLIST_FILE_KEY = "1001000008";
        protected static final String CLOUD_TRANREFRESHRATE_120HZ_SWITCH_CONFIG_KEY = "1001000009";
        protected static final String DETECT_VIDEO_APP_SCOPE = "detect_video_app_scope";
        protected static final String FAST_SLOW_SLIDE_BLACK_LIST = "fast_slow_slide_blacklist";
        protected static final String G80 = "G80";
        protected static final String G85 = "G85";
        protected static final String G95 = "G95";
        protected static final String HIGH_REFRESH_RATE_BLACK_LIST = "refresh_rate_blacklist_in_120hz_mode";
        protected static final String MIN_REFRESH_RATE_90HZ_IN_AUTO_LIST = "min_refresh_rate_90hz_in_auto_mode";
        protected static final String PLATFORM_PKG_SET_ENTRYS = "platform_pkg_set_entrys";
        protected static final String PROJECT_PKG_SET_ENTRYS = "project_pkg_set_entrys";
        protected static final String PROJECT_PKG_SET_ENTRY_INDEX = "project_pkg_set_entry_index";
        protected static final String PROJECT_PKG_SET_VALUES = "values";
        protected static final String PROJECT_PKG_USED_MODE = "used_mode";
        protected static final String PUBG_PACKAGE_LIST = "pubg_package_list";
        protected static final String REFRESH_RATE_90HZ_BLACK_LIST = "refresh_rate_blacklist_in_90hz_mode";
        protected static final String REFRESH_RATE_90HZ_WHITE_LIST = "refresh_rate_90hz_whitelist_in_120hz_mode";
        protected static final String TRANSITION_ANIMATION_LIST = "transition_animation_whitelist";
        public static final long TRAN_LOW_BATTERY_LEVEL = 20;
        public static final long TRAN_RECOVERY_BATTERY_LEVEL = 30;
        protected static final String VOICE_AND_VIDEO_CALL_LIST = "voice_and_video_call_whitelist";
        protected static final String sProjectName = Build.BOARD;
        protected String mCpuName;
        protected String mResumedPkgName = null;
        protected int mConfigLastMajorVersion = 0;
        protected int mConfigLastVersion = 0;
        protected int mUpdateTime = 0;
        protected boolean mExistCustomizedConfig = false;
        protected boolean mExistParseCfgException = false;
        protected final Object mPolicyModeLock = new Object();
        protected final Object mHighRefreshRateLock = new Object();
        protected final Object mPubgPkgSetLock = new Object();
        protected Set<String> mHighRefreshRateBlackList = new HashSet();
        protected Set<String> mPubgPkgSet = new HashSet();
        protected Set<String> mPlatformHighRefreshRateBlackList = new HashSet();
        protected Set<String> mAccumulatedHighRefreshRateBlackList = new HashSet();
        protected Set<String> mTransitionAnimationList = new HashSet();
        protected Set<String> mEmpty = new HashSet();
        protected long mLowBatteryLevel = 20;
        protected long mRecoveryBatteryLevel = 30;
        protected final Object mCloudConfigLock = new Object();
        protected Set<String> m90HZRefreshRateWhiteListIn120HzMode = new HashSet();
        protected Set<String> mRefreshRateBlackListIn90HzMode = new HashSet();
        protected Set<String> mFastSlowSlideBlackList = new HashSet();
        public boolean mProjectListUseWhite = false;
        protected String mProjectName = Build.BOARD;
        protected boolean mLoadCommonCfgSuccessed = false;
        protected Set<String> mProjectRefreshRateList = new HashSet();
        protected Set<String> mAutoRefreshRateWhiteList = new HashSet();
        protected Set<String> mMinRefreshRate90HzInAutoModeList = new HashSet();
        protected Set<String> mAboveResumedWindowAppList = new HashSet();
        protected Set<String> mVoiceCallWhiteList = new HashSet();
        protected Set<String> mVideoAppSupportedList = new HashSet();
        protected Set<String> mActiveSwitchProblemPackageList = new HashSet();

        /* JADX INFO: Access modifiers changed from: package-private */
        public TranBaseRefreshRateConfig() {
            this.mCpuName = TranBaseRefreshRatePolicy.DEFAULT_VALUE;
            this.mCpuName = getCpuName();
            Slog.d(TranBaseRefreshRatePolicy.TAG, "Cpu name: " + this.mCpuName + " ProjectName: " + sProjectName);
            if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                listenAppLaunch();
            }
        }

        public Object getCloudConfigLock() {
            return this.mCloudConfigLock;
        }

        public Set<String> getFastSlowSlideBlackList() {
            return this.mFastSlowSlideBlackList;
        }

        public Set<String> getVoiceCallWhiteList() {
            return this.mVoiceCallWhiteList;
        }

        public Set<String> getVideoAppSupportedList() {
            return this.mVideoAppSupportedList;
        }

        public Set<String> getActiveSwitchProblemPackageList() {
            return this.mActiveSwitchProblemPackageList;
        }

        public Set<String> getProjectRefreshRateList() {
            return this.mProjectRefreshRateList;
        }

        public Set<String> get90HZRefreshRateWhiteList() {
            return this.m90HZRefreshRateWhiteListIn120HzMode;
        }

        public Set<String> getTransitionAnimationList() {
            return this.mTransitionAnimationList;
        }

        public Set<String> get90HZRefreshRateBlackList() {
            return this.mRefreshRateBlackListIn90HzMode;
        }

        public long getLowBatteryLevel() {
            long j;
            synchronized (this.mCloudConfigLock) {
                j = this.mLowBatteryLevel;
            }
            return j;
        }

        public long getRecoveryBatteryLevel() {
            long j;
            synchronized (this.mCloudConfigLock) {
                j = this.mRecoveryBatteryLevel;
            }
            return j;
        }

        public Object getPolicyModeLock() {
            return this.mPolicyModeLock;
        }

        public Set<String> getAboveResumedWindowAppList() {
            return this.mAboveResumedWindowAppList;
        }

        public Set<String> getAutoRefreshRateWhiteList() {
            Set<String> set;
            synchronized (this.mHighRefreshRateLock) {
                set = this.mAutoRefreshRateWhiteList;
            }
            return set;
        }

        public Set<String> getMinRefreshRate90HzInAutoModeList() {
            return this.mMinRefreshRate90HzInAutoModeList;
        }

        public Object getHighRefreshRateLock() {
            return this.mHighRefreshRateLock;
        }

        public boolean isLoadCommonCfgSuccess() {
            return this.mLoadCommonCfgSuccessed;
        }

        public Set<String> getAccumulatedHighRefreshRateBlackList() {
            return this.mAccumulatedHighRefreshRateBlackList;
        }

        public Set<String> getHighRefreshRateBlackList() {
            return this.mHighRefreshRateBlackList;
        }

        public Set<String> getPlatformHighRefreshRateBlackList() {
            return this.mPlatformHighRefreshRateBlackList;
        }

        public void loadTranRefreshRateConfig() {
        }

        protected void updateTranRefreshRateConfig(String jsonStr, boolean ignoreMinor) {
        }

        protected void loadCloudTranRefreshRateConfig() {
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public int getMajorVersion() {
            return this.mConfigLastMajorVersion;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public int getVersion() {
            return this.mConfigLastVersion;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public int getUpdateTime() {
            return this.mUpdateTime;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public Set<String> jsonArray2Set(JSONArray array) throws JSONException {
            if (array != null) {
                Set<String> result = new HashSet<>(array.length());
                for (int n = 0; n < array.length(); n++) {
                    String pairStr = array.getString(n);
                    result.add(pairStr);
                }
                return result;
            }
            return Collections.EMPTY_SET;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public String readFileString(String filePath) {
            String fileString = "";
            File file = new File(filePath);
            if (file.exists() && file.canRead()) {
                try {
                    InputStreamReader in = new InputStreamReader(new FileInputStream(file));
                    BufferedReader bf = new BufferedReader(in);
                    StringBuffer buffer = new StringBuffer();
                    while (true) {
                        String line = bf.readLine();
                        if (line == null) {
                            break;
                        }
                        buffer.append(line);
                    }
                    fileString = buffer.toString();
                    bf.close();
                    in.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            return fileString;
        }

        private void listenAppLaunch() {
            TranStateListener listener = new TranStateListener() { // from class: com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.TranBaseRefreshRateConfig.1
                @Override // com.transsion.hubcore.griffin.lib.provider.TranStateListener
                public void onAppLaunch(ActivityInfo current, ActivityInfo next, int nextPid, boolean pausing, int nextType) {
                    if (ITranGriffinFeature.Instance().isGriffinSupport() && current != null && next != null) {
                        String currPackName = current.getComponentName().getPackageName();
                        String nextPackName = next.getComponentName().getPackageName();
                        TranBaseRefreshRateConfig.this.mResumedPkgName = nextPackName;
                        TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "Applaunch from " + currPackName + " to " + nextPackName);
                    }
                }

                @Override // com.transsion.hubcore.griffin.lib.provider.TranStateListener
                public void onScreenChanged(boolean status) {
                }
            };
            boolean succeed = ITranGriffinFeature.Instance().getHooker().registerListener(listener);
            TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "registerHookerListener:" + succeed);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1753=6, 1754=6, 1755=6, 1756=6, 1758=4, 1760=6, 1761=6, 1762=6, 1763=4] */
        /* JADX WARN: Code restructure failed: missing block: B:12:0x0051, code lost:
            r0 = r6[1];
         */
        /* JADX WARN: Code restructure failed: missing block: B:13:0x0055, code lost:
            r5.close();
         */
        /* JADX WARN: Code restructure failed: missing block: B:15:0x0059, code lost:
            r8 = move-exception;
         */
        /* JADX WARN: Code restructure failed: missing block: B:16:0x005a, code lost:
            android.util.Slog.e(com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.TAG, "close bufferedReader exception: " + r8.toString());
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private String getCpuName() {
            String str;
            StringBuilder sb;
            String str2;
            FileReader fr = null;
            BufferedReader br = null;
            try {
                try {
                    fr = new FileReader("/proc/cpuinfo");
                    br = new BufferedReader(fr);
                    while (true) {
                        String contentLine = br.readLine();
                        if (contentLine == null) {
                            try {
                                br.close();
                            } catch (IOException e) {
                                Slog.e(TranBaseRefreshRatePolicy.TAG, "close bufferedReader exception: " + e.toString());
                            }
                            try {
                                fr.close();
                            } catch (IOException e2) {
                                Slog.e(TranBaseRefreshRatePolicy.TAG, "close fileReader exception: " + e2.toString());
                            }
                            return TranBaseRefreshRatePolicy.DEFAULT_VALUE;
                        }
                        String[] array = contentLine.split(":\\s+");
                        TranBaseRefreshRatePolicy.logd(TranBaseRefreshRatePolicy.TAG, "contentLine: " + contentLine);
                        if (array != null && array.length >= 2 && array[0].contains("Hardware")) {
                            break;
                        }
                    }
                    try {
                        fr.close();
                    } catch (IOException e3) {
                        Slog.e(TranBaseRefreshRatePolicy.TAG, "close fileReader exception: " + e3.toString());
                    }
                    return str2;
                    return str2;
                } catch (FileNotFoundException e4) {
                    Slog.e(TranBaseRefreshRatePolicy.TAG, "getCpuName(): " + e4.toString());
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e5) {
                            Slog.e(TranBaseRefreshRatePolicy.TAG, "close bufferedReader exception: " + e5.toString());
                        }
                    }
                    if (fr != null) {
                        try {
                            fr.close();
                        } catch (IOException e6) {
                            e = e6;
                            str = TranBaseRefreshRatePolicy.TAG;
                            sb = new StringBuilder();
                            Slog.e(str, sb.append("close fileReader exception: ").append(e.toString()).toString());
                            return TranBaseRefreshRatePolicy.DEFAULT_VALUE;
                        }
                    }
                    return TranBaseRefreshRatePolicy.DEFAULT_VALUE;
                } catch (IOException e7) {
                    Slog.e(TranBaseRefreshRatePolicy.TAG, "getCpuName(): " + e7.toString());
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e8) {
                            Slog.e(TranBaseRefreshRatePolicy.TAG, "close bufferedReader exception: " + e8.toString());
                        }
                    }
                    if (fr != null) {
                        try {
                            fr.close();
                        } catch (IOException e9) {
                            e = e9;
                            str = TranBaseRefreshRatePolicy.TAG;
                            sb = new StringBuilder();
                            Slog.e(str, sb.append("close fileReader exception: ").append(e.toString()).toString());
                            return TranBaseRefreshRatePolicy.DEFAULT_VALUE;
                        }
                    }
                    return TranBaseRefreshRatePolicy.DEFAULT_VALUE;
                } catch (IndexOutOfBoundsException e10) {
                    Slog.e(TranBaseRefreshRatePolicy.TAG, "getCpuName(): " + e10.toString());
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e11) {
                            Slog.e(TranBaseRefreshRatePolicy.TAG, "close bufferedReader exception: " + e11.toString());
                        }
                    }
                    if (fr != null) {
                        try {
                            fr.close();
                        } catch (IOException e12) {
                            e = e12;
                            str = TranBaseRefreshRatePolicy.TAG;
                            sb = new StringBuilder();
                            Slog.e(str, sb.append("close fileReader exception: ").append(e.toString()).toString());
                            return TranBaseRefreshRatePolicy.DEFAULT_VALUE;
                        }
                    }
                    return TranBaseRefreshRatePolicy.DEFAULT_VALUE;
                }
            } catch (Throwable th) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e13) {
                        Slog.e(TranBaseRefreshRatePolicy.TAG, "close bufferedReader exception: " + e13.toString());
                    }
                }
                if (fr != null) {
                    try {
                        fr.close();
                    } catch (IOException e14) {
                        Slog.e(TranBaseRefreshRatePolicy.TAG, "close fileReader exception: " + e14.toString());
                    }
                }
                throw th;
            }
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public String getPlatformAlias(String p) {
            return TranBaseRefreshRatePolicy.DEFAULT_VALUE;
        }
    }
}
