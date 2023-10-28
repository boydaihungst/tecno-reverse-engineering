package com.mediatek.boostfwk.policy.refreshrate;

import android.app.Activity;
import android.content.Context;
import android.view.Display;
import android.view.ViewConfiguration;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import java.util.ArrayList;
import java.util.Comparator;
/* loaded from: classes.dex */
public class RefreshRateInfo {
    public static final float DECELERATION_RATE = (float) (Math.log(0.78d) / Math.log(0.9d));
    private static final int DEFAULT_MIN_SUPPORT_REFRESHRATE_NUMBER = 3;
    private static final int DEFAULT_MIN_SUPPORT_WHEN_FLING = 30;
    private static final int DEFAULT_MIN_SUPPORT_WHEN_FLING_WITH_VIDEO = 60;
    private static final int DEFAULT_SUPPORT_REFRESHRATE_NUMBER = 4;
    private static final int DEFAULT_TOUCH_SCROLL_VELOCITY_THRESHOLED = 150;
    public static final float DISTANCE_M1 = 3.5f;
    public static final float DISTANCE_M2 = 1.65f;
    public static final float DURATION_M1 = 9.0f;
    public static final float DURATION_M2 = 1.65f;
    private static final float END_TENSION = 1.0f;
    private static final float INFLEXION_DEFAULT = 0.35f;
    private static final float INFLEXION_MSYNC = 0.26f;
    public static final int NB_SAMPLES = 100;
    private static final float START_TENSION = 0.5f;
    private static final String TAG = "MSYNC3-VariableRefreshRate";
    private static final String USER_CONFIG_DEFAULT_VALUE = "-1";
    private String mCurrentActivityName;
    private float mInflextion;
    private float mMaximumVelocity;
    private String mPackageName;
    private float p1;
    private float p2;
    private ArrayList<Integer> mHardwareSupportedRefreshRate = new ArrayList<>();
    private ArrayList<Integer> mFlingSupportedRefreshRate = new ArrayList<>();
    private ArrayList<Float> mFlingRefreshRateChangeGap = new ArrayList<>();
    private ArrayList<Float> mFlingRefreshRateChangeTimeOffset = new ArrayList<>();
    private ArrayList<Float> mFlingRefreshRateVSyncTime = new ArrayList<>();
    private float[] mSplinePosition = new float[101];
    private float[] mSplineTime = new float[101];
    private boolean mIsDataInited = false;
    private int mFlingSupportedRefreshRateSize = 0;
    private int mTouchScrollSpeedLow = -1;
    private int mVideoFloorRefreshRate = -1;
    private int mTouchScrollVelocityThreshold = -1;
    private float[] mSplinePositionSpecial = {DECELERATION_RATE, 0.02501396f, 0.076272935f, 0.12270388f, 0.16693656f, 0.20906013f, 0.24916118f, 0.28732365f, 0.32362896f, 0.35815603f, 0.39098135f, 0.42217886f, 0.4518203f, 0.47997496f, 0.5067098f, 0.5320897f, 0.556177f, 0.5790322f, 0.60071343f, 0.6212768f, 0.64077634f, 0.6592641f, 0.6767901f, 0.6934025f, 0.7091475f, 0.7240694f, 0.7382108f, 0.75161237f, 0.7643133f, 0.77635074f, 0.7877604f, 0.79857635f, 0.8088311f, 0.8185556f, 0.8277792f, 0.83652997f, 0.84483445f, 0.8527179f, 0.86020416f, 0.86731577f, 0.87407404f, 0.88049924f, 0.8866101f, 0.8924246f, 0.89795935f, 0.90323f, 0.90825135f, 0.91303694f, 0.91759956f, 0.92195106f, 0.9261025f, 0.9300641f, 0.9338452f, 0.93745464f, 0.9409003f, 0.94418967f, 0.9473294f, 0.9503258f, 0.9531845f, 0.9559106f, 0.95850897f, 0.9609838f, 0.9633392f, 0.9655788f, 0.96770585f, 0.96972346f, 0.9716347f, 0.97344214f, 0.97514844f, 0.9767562f, 0.97826785f, 0.9796859f, 0.98101294f, 0.98225147f, 0.9834043f, 0.98447424f, 0.98546445f, 0.98637825f, 0.98721915f, 0.98799115f, 0.98869854f, 0.98934597f, 0.98993856f, 0.990482f, 0.99098223f, 0.99144614f, 0.99188095f, 0.9922946f, 0.9926957f, 0.99309355f, 0.9934984f, 0.993921f, 0.99437314f, 0.9947675f, 0.9951381f, 0.99544436f, 0.99577314f, 0.99619675f, 0.99633807f, 0.99654436f, 0.9967531f, 0.9969731f, 0.99716747f, 0.9973176f, 0.9975381f, 0.9977444f, 0.99795306f, 0.9981731f, 0.9983675f, 0.99851763f, 0.9987381f, 0.9989531f, 0.99914956f, 0.99935f, 0.9995f, 0.999625f, 0.999735f, 0.999835f, 0.99999f, END_TENSION};

    public RefreshRateInfo() {
        float f = Config.isMSync3SmoothFlingEnabled() ? INFLEXION_MSYNC : INFLEXION_DEFAULT;
        this.mInflextion = f;
        float f2 = START_TENSION * f;
        this.p1 = f2;
        float f3 = END_TENSION - ((END_TENSION - f) * END_TENSION);
        this.p2 = f3;
        initSmoothFlingInfo(f2, f3);
    }

    public void updateCurrentActivityName(Context activityContext) {
        Activity activity = (Activity) activityContext;
        this.mCurrentActivityName = activity.getLocalClassName();
        LogUtil.mLogd(TAG, "mCurrentActivityName = " + this.mCurrentActivityName);
    }

    private void initSmoothFlingInfo(float p1, float p2) {
        float f;
        float x;
        float f2;
        float coef;
        float y;
        float coef2;
        float x_min = DECELERATION_RATE;
        float y_min = DECELERATION_RATE;
        for (int i = 0; i < 100; i++) {
            float alpha = i / 100.0f;
            float x_max = END_TENSION;
            while (true) {
                f = 2.0f;
                x = ((x_max - x_min) / 2.0f) + x_min;
                f2 = 3.5f;
                coef = x * 3.5f * (END_TENSION - x);
                float tx = ((((END_TENSION - x) * p1) + (x * p2)) * coef) + (x * x * x);
                if (Math.abs(tx - alpha) < 1.0E-5d) {
                    break;
                } else if (tx > alpha) {
                    x_max = x;
                } else {
                    x_min = x;
                }
            }
            this.mSplinePosition[i] = ((((END_TENSION - x) * START_TENSION) + x) * coef) + (x * x * x);
            float y_max = END_TENSION;
            while (true) {
                y = ((y_max - y_min) / f) + y_min;
                coef2 = y * f2 * (END_TENSION - y);
                float dy = ((((END_TENSION - y) * START_TENSION) + y) * coef2) + (y * y * y);
                float coef3 = coef;
                if (Math.abs(dy - alpha) < 1.0E-5d) {
                    break;
                }
                if (dy > alpha) {
                    y_max = y;
                } else {
                    y_min = y;
                }
                coef = coef3;
                f = 2.0f;
                f2 = 3.5f;
            }
            this.mSplineTime[i] = ((((END_TENSION - y) * p1) + (y * p2)) * coef2) + (y * y * y);
        }
        float[] fArr = this.mSplinePosition;
        this.mSplineTime[100] = 1.0f;
        fArr[100] = 1.0f;
    }

    public void initPackageInfo(Context activityContext) {
        ViewConfiguration configuration = ViewConfiguration.get(activityContext);
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mPackageName = activityContext.getApplicationInfo().packageName;
    }

    public void initHardwareSupportRefreshRate(Context activityContext) {
        Display display = ((Activity) activityContext).getWindowManager().getDefaultDisplay();
        Display.Mode[] displayMode = display.getSupportedModes();
        for (Display.Mode mode : displayMode) {
            int refreshRate = (int) mode.getRefreshRate();
            if (!this.mHardwareSupportedRefreshRate.contains(Integer.valueOf(refreshRate))) {
                this.mHardwareSupportedRefreshRate.add(Integer.valueOf(refreshRate));
            }
        }
        this.mHardwareSupportedRefreshRate.sort(Comparator.reverseOrder());
        LogUtil.mLogd(TAG, "mHardwareSupportedRefreshRate = " + this.mHardwareSupportedRefreshRate.toString());
    }

    public boolean initMSync3SupportRefreshRate() {
        int i;
        boolean isInitSuccess = false;
        String userConfigRefreshRate = Config.MSYNC3_FLING_SUPPORT_REFRESHRATE;
        String userConfigRefreshChangeGap = Config.MSYNC3_FLING_REFRESHRATE_CHANGE_GAP;
        LogUtil.mLogd(TAG, "userConfigRefreshRate = " + userConfigRefreshRate + " userConfigRefreshChangeGap = " + userConfigRefreshChangeGap);
        if (!"-1".equals(userConfigRefreshRate) && !"-1".equals(userConfigRefreshChangeGap)) {
            String[] userConfigRefreshRates = userConfigRefreshRate.split(",");
            String[] userConfigRefreshChangeGaps = userConfigRefreshChangeGap.split(",");
            if (userConfigRefreshRates.length < 3) {
                isInitSuccess = false;
            } else {
                for (String str : userConfigRefreshRates) {
                    this.mFlingSupportedRefreshRate.add(Integer.valueOf(str));
                }
                for (String str2 : userConfigRefreshChangeGaps) {
                    this.mFlingRefreshRateChangeGap.add(Float.valueOf(str2));
                }
                isInitSuccess = true;
            }
            LogUtil.mLogd(TAG, "Userconfig mMSyncSupportedRefreshRate = " + this.mFlingSupportedRefreshRate.toString() + " mFlingRefreshRateChangeGap = " + this.mFlingRefreshRateChangeGap.toString() + " isInitSuccess = " + isInitSuccess);
        }
        this.mTouchScrollSpeedLow = Integer.valueOf(Config.MSYNC3_TOUCHSCROLL_REFRESHRATE_SPEED_LOW).intValue();
        this.mVideoFloorRefreshRate = Integer.valueOf(Config.MSYNC3_VIDEO_FLOOR_FPS).intValue();
        this.mTouchScrollVelocityThreshold = Integer.valueOf(Config.MSYNC3_TOUCH_SCROLL_VELOCITY).intValue();
        LogUtil.mLogd(TAG, "Userconfig  = mTouchScrollSpeedLow = " + this.mTouchScrollSpeedLow + " mVideoFloorRefreshRate = " + this.mVideoFloorRefreshRate + " mTouchScrollVelocityThreshold = " + this.mTouchScrollVelocityThreshold);
        if (!isInitSuccess) {
            int totalSupportRefreshRateSize = this.mHardwareSupportedRefreshRate.size();
            if (totalSupportRefreshRateSize < 3) {
                isInitSuccess = false;
            } else if ((totalSupportRefreshRateSize == 3 || totalSupportRefreshRateSize == 4) && this.mHardwareSupportedRefreshRate.get(totalSupportRefreshRateSize - 1).intValue() >= DEFAULT_MIN_SUPPORT_WHEN_FLING) {
                this.mFlingSupportedRefreshRate.addAll(this.mHardwareSupportedRefreshRate);
                for (int i2 = 0; i2 < totalSupportRefreshRateSize; i2++) {
                    this.mFlingRefreshRateChangeGap.add(Float.valueOf(1.83f));
                }
                isInitSuccess = true;
            } else if (totalSupportRefreshRateSize > 4) {
                for (int i3 = 0; i3 < 4; i3++) {
                    this.mFlingSupportedRefreshRate.add(this.mHardwareSupportedRefreshRate.get(i3));
                    this.mFlingRefreshRateChangeGap.add(Float.valueOf(1.83f));
                }
                isInitSuccess = true;
            }
        }
        if (isInitSuccess) {
            this.mFlingSupportedRefreshRateSize = this.mFlingSupportedRefreshRate.size();
            int i4 = 0;
            while (true) {
                if (i4 >= this.mFlingSupportedRefreshRateSize - 1) {
                    break;
                }
                float currentRefreshRateVsyncIntervalMS = (END_TENSION / Float.valueOf(this.mFlingSupportedRefreshRate.get(i4).intValue()).floatValue()) * 1000.0f;
                float nextLevelRefreshRateVsyncIntervalMS = (END_TENSION / Float.valueOf(this.mFlingSupportedRefreshRate.get(i4 + 1).intValue()).floatValue()) * 1000.0f;
                this.mFlingRefreshRateChangeTimeOffset.add(Float.valueOf(nextLevelRefreshRateVsyncIntervalMS - currentRefreshRateVsyncIntervalMS));
                this.mFlingRefreshRateVSyncTime.add(Float.valueOf(currentRefreshRateVsyncIntervalMS));
                i4++;
            }
            this.mFlingRefreshRateVSyncTime.add(Float.valueOf((END_TENSION / Float.valueOf(this.mFlingSupportedRefreshRate.get(i - 1).intValue()).floatValue()) * 1000.0f));
        }
        LogUtil.mLogd(TAG, "SupportedRefreshRate = " + this.mFlingSupportedRefreshRate.toString() + " mFlingChangeGap = " + this.mFlingRefreshRateChangeGap.toString() + " mFlingChangeTimeOffset = " + this.mFlingRefreshRateChangeTimeOffset.toString() + " mFlingRefreshRateVSyncTime = " + this.mFlingRefreshRateVSyncTime.toString() + " isInitSuccess = " + isInitSuccess);
        return isInitSuccess;
    }

    public float getMaximumVelocity() {
        return this.mMaximumVelocity;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getCurrentActivityName() {
        return this.mCurrentActivityName;
    }

    public float[] getSmoothFlingSplinePosition() {
        return this.mSplinePositionSpecial;
    }

    public float[] getSmoothFlingSplineTime() {
        return this.mSplineTime;
    }

    public ArrayList getFlingSupportedRefreshRate() {
        return this.mFlingSupportedRefreshRate;
    }

    public ArrayList getFlingRefreshRateChangeGap() {
        return this.mFlingRefreshRateChangeGap;
    }

    public ArrayList getFlingRefreshRateChangeTimeOffset() {
        return this.mFlingRefreshRateChangeTimeOffset;
    }

    public ArrayList getFlingRefreshRateVSyncTime() {
        return this.mFlingRefreshRateVSyncTime;
    }

    public int getMaxFlingSupportedRefreshRate() {
        if (this.mFlingSupportedRefreshRate.size() < 3) {
            return -1;
        }
        return this.mFlingSupportedRefreshRate.get(0).intValue();
    }

    public int getSlowScrollRefreshRate() {
        if (this.mFlingSupportedRefreshRate.size() < 3) {
            return -1;
        }
        int i = this.mTouchScrollSpeedLow;
        return i != -1 ? i : this.mFlingSupportedRefreshRate.get(1).intValue();
    }

    public int getVideoFloorRefreshRate() {
        int i = this.mVideoFloorRefreshRate;
        return i != -1 ? i : DEFAULT_MIN_SUPPORT_WHEN_FLING_WITH_VIDEO;
    }

    public int getFlingSupportedRefreshRateCount() {
        return this.mFlingSupportedRefreshRateSize;
    }

    public int getTouchScrollVelocityThreshold() {
        int i = this.mTouchScrollVelocityThreshold;
        return i != -1 ? i : DEFAULT_TOUCH_SCROLL_VELOCITY_THRESHOLED;
    }

    public boolean getIsDataInited() {
        return this.mIsDataInited;
    }

    public float getInflextion() {
        return this.mInflextion;
    }

    public int getSmoothFlingSplinePositionCount() {
        return this.mSplinePositionSpecial.length - 1;
    }

    public int getSmoothFlingSplineTimeCount() {
        return 100;
    }
}
