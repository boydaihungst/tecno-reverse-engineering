package com.android.server.display;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.EventLog;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.os.BackgroundThread;
import com.android.server.EventLogTags;
import com.android.server.display.DisplayPowerController;
import com.android.server.job.controllers.JobStatus;
import com.transsion.hubcore.server.display.ITranDisplayPowerController;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AutomaticBrightnessController {
    private static final long AMBIENT_LIGHT_PREDICTION_TIME_MILLIS = 100;
    private static final int AMBIENT_LUX_JUMP_HIGH = 2000;
    public static final int AUTO_BRIGHTNESS_DISABLED = 2;
    public static final int AUTO_BRIGHTNESS_ENABLED = 1;
    public static final int AUTO_BRIGHTNESS_OFF_DUE_TO_DISPLAY_STATE = 3;
    private static final int BRIGHTNESS_ADJUSTMENT_SAMPLE_DEBOUNCE_MILLIS = 10000;
    private static final boolean DEBUG_PRETEND_LIGHT_SENSOR_ABSENT = false;
    private static final int MSG_BRIGHTNESS_ADJUSTMENT_SAMPLE = 2;
    private static final int MSG_INVALIDATE_SHORT_TERM_MODEL = 3;
    private static final int MSG_RUN_UPDATE = 6;
    private static final int MSG_UPDATE_AMBIENT_LUX = 1;
    private static final int MSG_UPDATE_FOREGROUND_APP = 4;
    private static final int MSG_UPDATE_FOREGROUND_APP_SYNC = 5;
    private static final String TAG = "AutomaticBrightnessController";
    private static final boolean TRAN_BACKLIGHT_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("ro.transsion.backlight.optimization", "0"));
    private IActivityTaskManager mActivityTaskManager;
    private float mAmbientBrighteningThreshold;
    private final HysteresisLevels mAmbientBrightnessThresholds;
    private float mAmbientDarkeningThreshold;
    private final int mAmbientLightHorizonLong;
    private final int mAmbientLightHorizonShort;
    private AmbientLightRingBuffer mAmbientLightRingBuffer;
    private float mAmbientLux;
    private boolean mAmbientLuxValid;
    private final long mBrighteningLightDebounceConfig;
    private float mBrightnessAdjustmentSampleOldBrightness;
    private float mBrightnessAdjustmentSampleOldLux;
    private boolean mBrightnessAdjustmentSamplePending;
    private BrightnessThrottler mBrightnessThrottler;
    private final Callbacks mCallbacks;
    private Clock mClock;
    private Context mContext;
    private BrightnessMappingStrategy mCurrentBrightnessMapper;
    private int mCurrentLightSensorRate;
    private final long mDarkeningLightDebounceConfig;
    private int mDisplayPolicy;
    private final float mDozeScaleFactor;
    private int mForegroundAppCategory;
    private String mForegroundAppPackageName;
    private AutomaticBrightnessHandler mHandler;
    private HighBrightnessModeController mHbmController;
    private final BrightnessMappingStrategy mIdleModeBrightnessMapper;
    private final int mInitialLightSensorRate;
    private final Injector mInjector;
    private final BrightnessMappingStrategy mInteractiveModeBrightnessMapper;
    private boolean mIsBrightnessThrottled;
    private float mLastObservedLux;
    private long mLastObservedLuxTime;
    private final Sensor mLightSensor;
    private long mLightSensorEnableTime;
    private boolean mLightSensorEnabled;
    private final SensorEventListener mLightSensorListener;
    private int mLightSensorWarmUpTimeConfig;
    private boolean mLoggingEnabled;
    private float mLucidAmbientLux;
    private boolean mLucidNeedSave;
    private float mLucidNewBrightness;
    private float mLucidOldBrightness;
    private boolean mLucidSaved;
    private final int mNormalLightSensorRate;
    private PackageManager mPackageManager;
    private int mPendingForegroundAppCategory;
    private String mPendingForegroundAppPackageName;
    private float mPreThresholdBrightness;
    private float mPreThresholdLux;
    private int mRecentLightSamples;
    private final boolean mResetAmbientLuxAfterWarmUpConfig;
    private float mScreenAutoBrightness;
    private float mScreenBrighteningThreshold;
    private final float mScreenBrightnessRangeMaximum;
    private final float mScreenBrightnessRangeMinimum;
    private final HysteresisLevels mScreenBrightnessThresholds;
    private float mScreenDarkeningThreshold;
    private final SensorManager mSensorManager;
    private float mShortTermModelAnchor;
    private boolean mShortTermModelValid;
    private int mState;
    private TaskStackListenerImpl mTaskStackListener;
    private final int mWeightingIntercept;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Callbacks {
        boolean proximityPositiveToNegative();

        void updateBrightness();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Clock {
        long uptimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AutomaticBrightnessController(Callbacks callbacks, Looper looper, SensorManager sensorManager, Sensor lightSensor, BrightnessMappingStrategy interactiveModeBrightnessMapper, int lightSensorWarmUpTime, float brightnessMin, float brightnessMax, float dozeScaleFactor, int lightSensorRate, int initialLightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, boolean resetAmbientLuxAfterWarmUpConfig, HysteresisLevels ambientBrightnessThresholds, HysteresisLevels screenBrightnessThresholds, Context context, HighBrightnessModeController hbmController, BrightnessThrottler brightnessThrottler, BrightnessMappingStrategy idleModeBrightnessMapper, int ambientLightHorizonShort, int ambientLightHorizonLong) {
        this(new Injector(), callbacks, looper, sensorManager, lightSensor, interactiveModeBrightnessMapper, lightSensorWarmUpTime, brightnessMin, brightnessMax, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounceConfig, darkeningLightDebounceConfig, resetAmbientLuxAfterWarmUpConfig, ambientBrightnessThresholds, screenBrightnessThresholds, context, hbmController, brightnessThrottler, idleModeBrightnessMapper, ambientLightHorizonShort, ambientLightHorizonLong);
    }

    AutomaticBrightnessController(Injector injector, Callbacks callbacks, Looper looper, SensorManager sensorManager, Sensor lightSensor, BrightnessMappingStrategy interactiveModeBrightnessMapper, int lightSensorWarmUpTime, float brightnessMin, float brightnessMax, float dozeScaleFactor, int lightSensorRate, int initialLightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, boolean resetAmbientLuxAfterWarmUpConfig, HysteresisLevels ambientBrightnessThresholds, HysteresisLevels screenBrightnessThresholds, Context context, HighBrightnessModeController hbmController, BrightnessThrottler brightnessThrottler, BrightnessMappingStrategy idleModeBrightnessMapper, int ambientLightHorizonShort, int ambientLightHorizonLong) {
        this.mScreenAutoBrightness = Float.NaN;
        this.mDisplayPolicy = 0;
        this.mState = 2;
        this.mLucidNeedSave = false;
        this.mLucidSaved = false;
        this.mLightSensorListener = new SensorEventListener() { // from class: com.android.server.display.AutomaticBrightnessController.2
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                if (AutomaticBrightnessController.this.mLightSensorEnabled) {
                    long time = AutomaticBrightnessController.this.mClock.uptimeMillis();
                    float lux = event.values[0];
                    AutomaticBrightnessController.this.handleLightSensorEvent(time, lux);
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mInjector = injector;
        this.mClock = injector.createClock();
        this.mContext = context;
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mCurrentBrightnessMapper = interactiveModeBrightnessMapper;
        this.mScreenBrightnessRangeMinimum = brightnessMin;
        this.mScreenBrightnessRangeMaximum = brightnessMax;
        this.mLightSensorWarmUpTimeConfig = lightSensorWarmUpTime;
        this.mDozeScaleFactor = dozeScaleFactor;
        this.mNormalLightSensorRate = lightSensorRate;
        this.mInitialLightSensorRate = initialLightSensorRate;
        this.mCurrentLightSensorRate = -1;
        this.mBrighteningLightDebounceConfig = brighteningLightDebounceConfig;
        this.mDarkeningLightDebounceConfig = darkeningLightDebounceConfig;
        this.mResetAmbientLuxAfterWarmUpConfig = resetAmbientLuxAfterWarmUpConfig;
        this.mAmbientLightHorizonLong = ambientLightHorizonLong;
        this.mAmbientLightHorizonShort = ambientLightHorizonShort;
        this.mWeightingIntercept = ambientLightHorizonLong;
        this.mAmbientBrightnessThresholds = ambientBrightnessThresholds;
        this.mScreenBrightnessThresholds = screenBrightnessThresholds;
        this.mShortTermModelValid = true;
        this.mShortTermModelAnchor = -1.0f;
        this.mHandler = new AutomaticBrightnessHandler(looper);
        this.mAmbientLightRingBuffer = new AmbientLightRingBuffer(lightSensorRate, ambientLightHorizonLong, this.mClock);
        this.mLightSensor = lightSensor;
        this.mActivityTaskManager = ActivityTaskManager.getService();
        this.mPackageManager = this.mContext.getPackageManager();
        this.mTaskStackListener = new TaskStackListenerImpl();
        this.mForegroundAppPackageName = null;
        this.mPendingForegroundAppPackageName = null;
        this.mForegroundAppCategory = -1;
        this.mPendingForegroundAppCategory = -1;
        this.mHbmController = hbmController;
        this.mBrightnessThrottler = brightnessThrottler;
        this.mInteractiveModeBrightnessMapper = interactiveModeBrightnessMapper;
        this.mIdleModeBrightnessMapper = idleModeBrightnessMapper;
        switchToInteractiveScreenBrightnessMode();
    }

    public boolean setLoggingEnabled(boolean loggingEnabled) {
        if (this.mLoggingEnabled == loggingEnabled) {
            return false;
        }
        BrightnessMappingStrategy brightnessMappingStrategy = this.mInteractiveModeBrightnessMapper;
        if (brightnessMappingStrategy != null) {
            brightnessMappingStrategy.setLoggingEnabled(loggingEnabled);
        }
        BrightnessMappingStrategy brightnessMappingStrategy2 = this.mIdleModeBrightnessMapper;
        if (brightnessMappingStrategy2 != null) {
            brightnessMappingStrategy2.setLoggingEnabled(loggingEnabled);
        }
        this.mLoggingEnabled = loggingEnabled;
        return true;
    }

    public float getAutomaticScreenBrightness() {
        return getAutomaticScreenBrightness(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getAutomaticScreenBrightness(DisplayPowerController.BrightnessEvent brightnessEvent) {
        if (brightnessEvent != null) {
            brightnessEvent.lux = this.mAmbientLuxValid ? this.mAmbientLux : Float.NaN;
            brightnessEvent.preThresholdLux = this.mPreThresholdLux;
            brightnessEvent.preThresholdBrightness = this.mPreThresholdBrightness;
            brightnessEvent.recommendedBrightness = this.mScreenAutoBrightness;
            brightnessEvent.flags |= (!this.mAmbientLuxValid ? 2 : 0) | (this.mDisplayPolicy == 1 ? 3 : 0);
        }
        if (!this.mAmbientLuxValid) {
            return Float.NaN;
        }
        if (this.mDisplayPolicy == 1) {
            return this.mScreenAutoBrightness * this.mDozeScaleFactor;
        }
        return this.mScreenAutoBrightness;
    }

    public boolean hasValidAmbientLux() {
        return this.mAmbientLuxValid;
    }

    public float getAutomaticScreenBrightnessAdjustment() {
        return this.mCurrentBrightnessMapper.getAutoBrightnessAdjustment();
    }

    public void configure(int state, BrightnessConfiguration configuration, float brightness, boolean userChangedBrightness, float adjustment, boolean userChangedAutoBrightnessAdjustment, int displayPolicy) {
        this.mState = state;
        this.mHbmController.setAutoBrightnessEnabled(state);
        boolean z = true;
        boolean dozing = displayPolicy == 1;
        boolean changed = setBrightnessConfiguration(configuration) | setDisplayPolicy(displayPolicy);
        if (userChangedAutoBrightnessAdjustment) {
            changed |= setAutoBrightnessAdjustment(adjustment);
        }
        boolean enable = this.mState == 1;
        if (userChangedBrightness && enable) {
            changed |= setScreenBrightnessByUser(brightness);
        }
        boolean userInitiatedChange = userChangedBrightness || userChangedAutoBrightnessAdjustment;
        if (userInitiatedChange && enable && !dozing) {
            prepareBrightnessAdjustmentSample();
        }
        if (!enable || dozing) {
            z = false;
        }
        boolean changed2 = setLightSensorEnabled(z) | changed;
        if (this.mIsBrightnessThrottled != this.mBrightnessThrottler.isThrottled()) {
            this.mIsBrightnessThrottled = this.mBrightnessThrottler.isThrottled();
            changed2 = true;
        }
        if (changed2) {
            updateAutoBrightness(false, userInitiatedChange);
        }
    }

    public void stop() {
        setLightSensorEnabled(false);
    }

    public boolean hasUserDataPoints() {
        return this.mCurrentBrightnessMapper.hasUserDataPoints();
    }

    public boolean isDefaultConfig() {
        if (isInIdleMode()) {
            return false;
        }
        return this.mInteractiveModeBrightnessMapper.isDefaultConfig();
    }

    public BrightnessConfiguration getDefaultConfig() {
        return this.mInteractiveModeBrightnessMapper.getDefaultConfig();
    }

    public void update() {
        this.mHandler.sendEmptyMessage(6);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getAmbientLux() {
        return this.mAmbientLux;
    }

    private boolean setDisplayPolicy(int policy) {
        if (this.mDisplayPolicy == policy) {
            return false;
        }
        int oldPolicy = this.mDisplayPolicy;
        this.mDisplayPolicy = policy;
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "Display policy transitioning from " + oldPolicy + " to " + policy);
        }
        if (!isInteractivePolicy(policy) && isInteractivePolicy(oldPolicy) && !isInIdleMode()) {
            this.mHandler.sendEmptyMessageDelayed(3, this.mCurrentBrightnessMapper.getShortTermModelTimeout());
            return true;
        } else if (isInteractivePolicy(policy) && !isInteractivePolicy(oldPolicy)) {
            this.mHandler.removeMessages(3);
            return true;
        } else {
            return true;
        }
    }

    private static boolean isInteractivePolicy(int policy) {
        return policy == 3 || policy == 2 || policy == 4;
    }

    private boolean setScreenBrightnessByUser(float brightness) {
        if (this.mAmbientLuxValid) {
            if (this.mLucidNeedSave && brightness == this.mLucidNewBrightness) {
                this.mLucidAmbientLux = this.mAmbientLux;
                this.mLucidNeedSave = false;
                this.mLucidSaved = true;
            } else {
                this.mLucidNeedSave = false;
                this.mLucidSaved = false;
            }
            this.mCurrentBrightnessMapper.addUserDataPoint(this.mAmbientLux, brightness);
            this.mShortTermModelValid = true;
            this.mShortTermModelAnchor = this.mAmbientLux;
            if (this.mLoggingEnabled) {
                Slog.d(TAG, "ShortTermModel: anchor=" + this.mShortTermModelAnchor);
            }
            return true;
        }
        return false;
    }

    public void resetShortTermModel() {
        this.mLucidNeedSave = false;
        this.mLucidSaved = false;
        this.mCurrentBrightnessMapper.clearUserDataPoints();
        this.mShortTermModelValid = true;
        this.mShortTermModelAnchor = -1.0f;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invalidateShortTermModel() {
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "ShortTermModel: invalidate user data");
        }
        this.mShortTermModelValid = false;
    }

    public boolean setBrightnessConfiguration(BrightnessConfiguration configuration) {
        if (this.mInteractiveModeBrightnessMapper.setBrightnessConfiguration(configuration)) {
            if (!isInIdleMode()) {
                resetShortTermModel();
                return true;
            }
            return true;
        }
        return false;
    }

    public void saveLucidData(float oldBrightness, float newBrightness) {
        this.mLucidNeedSave = true;
        this.mLucidOldBrightness = oldBrightness;
        this.mLucidNewBrightness = newBrightness;
    }

    public void recoverLucidData() {
        if (this.mLucidSaved) {
            Slog.d(TAG, "recoverLucidData: mLucidAmbientLux=" + this.mLucidAmbientLux + " mLucidOldBrightness=" + this.mLucidOldBrightness);
            this.mCurrentBrightnessMapper.addUserDataPoint(this.mLucidAmbientLux, this.mLucidOldBrightness);
            this.mLucidSaved = false;
        }
    }

    public boolean isInIdleMode() {
        return this.mCurrentBrightnessMapper.isForIdleMode();
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Automatic Brightness Controller Configuration:");
        pw.println("  mState=" + configStateToString(this.mState));
        pw.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        pw.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        pw.println("  mDozeScaleFactor=" + this.mDozeScaleFactor);
        pw.println("  mInitialLightSensorRate=" + this.mInitialLightSensorRate);
        pw.println("  mNormalLightSensorRate=" + this.mNormalLightSensorRate);
        pw.println("  mLightSensorWarmUpTimeConfig=" + this.mLightSensorWarmUpTimeConfig);
        pw.println("  mBrighteningLightDebounceConfig=" + this.mBrighteningLightDebounceConfig);
        pw.println("  mDarkeningLightDebounceConfig=" + this.mDarkeningLightDebounceConfig);
        pw.println("  mResetAmbientLuxAfterWarmUpConfig=" + this.mResetAmbientLuxAfterWarmUpConfig);
        pw.println("  mAmbientLightHorizonLong=" + this.mAmbientLightHorizonLong);
        pw.println("  mAmbientLightHorizonShort=" + this.mAmbientLightHorizonShort);
        pw.println("  mWeightingIntercept=" + this.mWeightingIntercept);
        pw.println();
        pw.println("Automatic Brightness Controller State:");
        pw.println("  mLightSensor=" + this.mLightSensor);
        pw.println("  mLightSensorEnabled=" + this.mLightSensorEnabled);
        pw.println("  mLightSensorEnableTime=" + TimeUtils.formatUptime(this.mLightSensorEnableTime));
        pw.println("  mCurrentLightSensorRate=" + this.mCurrentLightSensorRate);
        pw.println("  mAmbientLux=" + this.mAmbientLux);
        pw.println("  mAmbientLuxValid=" + this.mAmbientLuxValid);
        pw.println("  mPreThesholdLux=" + this.mPreThresholdLux);
        pw.println("  mPreThesholdBrightness=" + this.mPreThresholdBrightness);
        pw.println("  mAmbientBrighteningThreshold=" + this.mAmbientBrighteningThreshold);
        pw.println("  mAmbientDarkeningThreshold=" + this.mAmbientDarkeningThreshold);
        pw.println("  mScreenBrighteningThreshold=" + this.mScreenBrighteningThreshold);
        pw.println("  mScreenDarkeningThreshold=" + this.mScreenDarkeningThreshold);
        pw.println("  mLastObservedLux=" + this.mLastObservedLux);
        pw.println("  mLastObservedLuxTime=" + TimeUtils.formatUptime(this.mLastObservedLuxTime));
        pw.println("  mRecentLightSamples=" + this.mRecentLightSamples);
        pw.println("  mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer);
        pw.println("  mScreenAutoBrightness=" + this.mScreenAutoBrightness);
        pw.println("  mDisplayPolicy=" + DisplayManagerInternal.DisplayPowerRequest.policyToString(this.mDisplayPolicy));
        pw.println("  mShortTermModelTimeout(active)=" + this.mInteractiveModeBrightnessMapper.getShortTermModelTimeout());
        if (this.mIdleModeBrightnessMapper != null) {
            pw.println("  mShortTermModelTimeout(idle)=" + this.mIdleModeBrightnessMapper.getShortTermModelTimeout());
        }
        pw.println("  mShortTermModelAnchor=" + this.mShortTermModelAnchor);
        pw.println("  mShortTermModelValid=" + this.mShortTermModelValid);
        pw.println("  mBrightnessAdjustmentSamplePending=" + this.mBrightnessAdjustmentSamplePending);
        pw.println("  mBrightnessAdjustmentSampleOldLux=" + this.mBrightnessAdjustmentSampleOldLux);
        pw.println("  mBrightnessAdjustmentSampleOldBrightness=" + this.mBrightnessAdjustmentSampleOldBrightness);
        pw.println("  mForegroundAppPackageName=" + this.mForegroundAppPackageName);
        pw.println("  mPendingForegroundAppPackageName=" + this.mPendingForegroundAppPackageName);
        pw.println("  mForegroundAppCategory=" + this.mForegroundAppCategory);
        pw.println("  mPendingForegroundAppCategory=" + this.mPendingForegroundAppCategory);
        pw.println("  Idle mode active=" + this.mCurrentBrightnessMapper.isForIdleMode());
        pw.println();
        pw.println("  mInteractiveMapper=");
        this.mInteractiveModeBrightnessMapper.dump(pw, this.mHbmController.getNormalBrightnessMax());
        if (this.mIdleModeBrightnessMapper != null) {
            pw.println("  mIdleMapper=");
            this.mIdleModeBrightnessMapper.dump(pw, this.mHbmController.getNormalBrightnessMax());
        }
        pw.println();
        this.mAmbientBrightnessThresholds.dump(pw);
        this.mScreenBrightnessThresholds.dump(pw);
    }

    private String configStateToString(int state) {
        switch (state) {
            case 1:
                return "AUTO_BRIGHTNESS_ENABLED";
            case 2:
                return "AUTO_BRIGHTNESS_DISABLED";
            case 3:
                return "AUTO_BRIGHTNESS_OFF_DUE_TO_DISPLAY_STATE";
            default:
                return String.valueOf(state);
        }
    }

    private boolean setLightSensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mLightSensorEnabled) {
                this.mLightSensorEnabled = true;
                ITranDisplayPowerController.Instance().hookOpticalDataAcquisitionState();
                this.mLightSensorEnableTime = this.mClock.uptimeMillis();
                if (this.mCallbacks.proximityPositiveToNegative()) {
                    this.mCurrentLightSensorRate = this.mInitialLightSensorRate;
                } else {
                    this.mCurrentLightSensorRate = (int) (this.mInitialLightSensorRate * 0.68d);
                }
                registerForegroundAppUpdater();
                this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, this.mCurrentLightSensorRate * 1000, this.mHandler);
                return true;
            }
        } else if (this.mLightSensorEnabled) {
            this.mLightSensorEnabled = false;
            boolean z = !this.mResetAmbientLuxAfterWarmUpConfig;
            this.mAmbientLuxValid = z;
            if (!z) {
                this.mPreThresholdLux = Float.NaN;
            }
            this.mScreenAutoBrightness = Float.NaN;
            this.mPreThresholdBrightness = Float.NaN;
            this.mRecentLightSamples = 0;
            this.mAmbientLightRingBuffer.clear();
            this.mCurrentLightSensorRate = -1;
            this.mHandler.removeMessages(1);
            unregisterForegroundAppUpdater();
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLightSensorEvent(long time, float lux) {
        Trace.traceCounter(131072L, "ALS", (int) lux);
        this.mHandler.removeMessages(1);
        if (this.mAmbientLightRingBuffer.size() == 0) {
            adjustLightSensorRate(this.mNormalLightSensorRate);
        }
        applyLightSensorMeasurement(time, lux);
        updateAmbientLux(time);
    }

    private void applyLightSensorMeasurement(long time, float lux) {
        this.mRecentLightSamples++;
        this.mAmbientLightRingBuffer.prune(time - this.mAmbientLightHorizonLong);
        this.mAmbientLightRingBuffer.push(time, lux);
        this.mLastObservedLux = lux;
        this.mLastObservedLuxTime = time;
    }

    private void adjustLightSensorRate(int lightSensorRate) {
        if (lightSensorRate != this.mCurrentLightSensorRate) {
            if (this.mLoggingEnabled) {
                Slog.d(TAG, "adjustLightSensorRate: previousRate=" + this.mCurrentLightSensorRate + ", currentRate=" + lightSensorRate);
            }
            this.mCurrentLightSensorRate = lightSensorRate;
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
            this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, lightSensorRate * 1000, this.mHandler);
        }
    }

    private boolean setAutoBrightnessAdjustment(float adjustment) {
        return this.mCurrentBrightnessMapper.setAutoBrightnessAdjustment(adjustment);
    }

    private void setAmbientLux(float lux) {
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "setAmbientLux(" + lux + ")");
        }
        if (lux < 0.0f) {
            Slog.w(TAG, "Ambient lux was negative, ignoring and setting to 0");
            lux = 0.0f;
        }
        if (TRAN_BACKLIGHT_OPTIMIZATION_SUPPORT && MathUtils.abs(this.mAmbientLux - lux) > 2000.0f) {
            resetShortTermModel();
        }
        this.mAmbientLux = lux;
        this.mAmbientBrighteningThreshold = this.mAmbientBrightnessThresholds.getBrighteningThreshold(lux);
        this.mAmbientDarkeningThreshold = this.mAmbientBrightnessThresholds.getDarkeningThreshold(lux);
        this.mHbmController.onAmbientLuxChange(this.mAmbientLux);
        if (this.mShortTermModelValid) {
            return;
        }
        float f = this.mShortTermModelAnchor;
        if (f != -1.0f) {
            if (this.mCurrentBrightnessMapper.shouldResetShortTermModel(this.mAmbientLux, f)) {
                resetShortTermModel();
            } else {
                this.mShortTermModelValid = true;
            }
        }
    }

    private float calculateAmbientLux(long now, long horizon) {
        long j = now;
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "calculateAmbientLux(" + j + ", " + horizon + ")");
        }
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "calculateAmbientLux: No ambient light readings available");
            return -1.0f;
        }
        int endIndex = 0;
        long horizonStartTime = j - horizon;
        for (int i = 0; i < N - 1 && this.mAmbientLightRingBuffer.getTime(i + 1) <= horizonStartTime; i++) {
            endIndex++;
        }
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "calculateAmbientLux: selected endIndex=" + endIndex + ", point=(" + this.mAmbientLightRingBuffer.getTime(endIndex) + ", " + this.mAmbientLightRingBuffer.getLux(endIndex) + ")");
        }
        float sum = 0.0f;
        float totalWeight = 0.0f;
        long endTime = AMBIENT_LIGHT_PREDICTION_TIME_MILLIS;
        int i2 = N - 1;
        while (i2 >= endIndex) {
            long eventTime = this.mAmbientLightRingBuffer.getTime(i2);
            if (i2 == endIndex && eventTime < horizonStartTime) {
                eventTime = horizonStartTime;
            }
            long horizonStartTime2 = horizonStartTime;
            int endIndex2 = endIndex;
            long startTime = eventTime - j;
            float weight = calculateWeight(startTime, endTime);
            float lux = this.mAmbientLightRingBuffer.getLux(i2);
            if (this.mLoggingEnabled) {
                Slog.d(TAG, "calculateAmbientLux: [" + startTime + ", " + endTime + "]: lux=" + lux + ", weight=" + weight);
            }
            totalWeight += weight;
            sum += lux * weight;
            endTime = startTime;
            i2--;
            j = now;
            endIndex = endIndex2;
            horizonStartTime = horizonStartTime2;
        }
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "calculateAmbientLux: totalWeight=" + totalWeight + ", newAmbientLux=" + (sum / totalWeight));
        }
        return sum / totalWeight;
    }

    private float calculateWeight(long startDelta, long endDelta) {
        return weightIntegral(endDelta) - weightIntegral(startDelta);
    }

    private float weightIntegral(long x) {
        return ((float) x) * ((((float) x) * 0.5f) + this.mWeightingIntercept);
    }

    private long nextAmbientLightBrighteningTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) > this.mAmbientBrighteningThreshold; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return this.mBrighteningLightDebounceConfig + earliestValidTime;
    }

    private long nextAmbientLightDarkeningTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) < this.mAmbientDarkeningThreshold; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return this.mDarkeningLightDebounceConfig + earliestValidTime;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAmbientLux() {
        long time = this.mClock.uptimeMillis();
        this.mAmbientLightRingBuffer.prune(time - this.mAmbientLightHorizonLong);
        updateAmbientLux(time);
    }

    /* JADX WARN: Code restructure failed: missing block: B:26:0x00a5, code lost:
        if (r7 <= r13) goto L21;
     */
    /* JADX WARN: Removed duplicated region for block: B:37:0x0109  */
    /* JADX WARN: Removed duplicated region for block: B:38:0x010b  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x0114  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void updateAmbientLux(long time) {
        if (!this.mAmbientLuxValid) {
            long timeWhenSensorWarmedUp = this.mLightSensorWarmUpTimeConfig + this.mLightSensorEnableTime;
            if (time < timeWhenSensorWarmedUp) {
                if (this.mLoggingEnabled) {
                    Slog.d(TAG, "updateAmbientLux: Sensor not ready yet: time=" + time + ", timeWhenSensorWarmedUp=" + timeWhenSensorWarmedUp);
                }
                this.mHandler.sendEmptyMessageAtTime(1, timeWhenSensorWarmedUp);
                return;
            }
            setAmbientLux(calculateAmbientLux(time, this.mAmbientLightHorizonShort));
            this.mAmbientLuxValid = true;
            if (this.mLoggingEnabled) {
                Slog.d(TAG, "updateAmbientLux: Initializing: mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer + ", mAmbientLux=" + this.mAmbientLux);
            }
            updateAutoBrightness(true, false);
        }
        long nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
        long nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        float slowAmbientLux = calculateAmbientLux(time, this.mAmbientLightHorizonLong);
        float fastAmbientLux = calculateAmbientLux(time, this.mAmbientLightHorizonShort);
        float f = this.mAmbientBrighteningThreshold;
        if (slowAmbientLux < f || fastAmbientLux < f || nextBrightenTransition > time) {
            float f2 = this.mAmbientDarkeningThreshold;
            if (slowAmbientLux <= f2) {
                if (fastAmbientLux <= f2) {
                }
            }
            long nextTransitionTime = Math.min(nextDarkenTransition, nextBrightenTransition);
            long nextTransitionTime2 = nextTransitionTime <= time ? nextTransitionTime : this.mNormalLightSensorRate + time;
            if (this.mLoggingEnabled) {
                Slog.d(TAG, "updateAmbientLux: Scheduling ambient lux update for " + nextTransitionTime2 + TimeUtils.formatUptime(nextTransitionTime2));
            }
            this.mHandler.sendEmptyMessageAtTime(1, nextTransitionTime2);
        }
        this.mPreThresholdLux = this.mAmbientLux;
        setAmbientLux(fastAmbientLux);
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "updateAmbientLux: " + (fastAmbientLux > this.mAmbientLux ? "Brightened" : "Darkened") + ": mBrighteningLuxThreshold=" + this.mAmbientBrighteningThreshold + ", mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer + ", mAmbientLux=" + this.mAmbientLux);
        }
        updateAutoBrightness(true, false);
        nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
        nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        long nextTransitionTime3 = Math.min(nextDarkenTransition, nextBrightenTransition);
        long nextTransitionTime22 = nextTransitionTime3 <= time ? nextTransitionTime3 : this.mNormalLightSensorRate + time;
        if (this.mLoggingEnabled) {
        }
        this.mHandler.sendEmptyMessageAtTime(1, nextTransitionTime22);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAutoBrightness(boolean sendUpdate, boolean isManuallySet) {
        if (!this.mAmbientLuxValid) {
            return;
        }
        float value = this.mCurrentBrightnessMapper.getBrightness(this.mAmbientLux, this.mForegroundAppPackageName, this.mForegroundAppCategory);
        float newScreenAutoBrightness = clampScreenBrightness(value);
        float f = this.mScreenAutoBrightness;
        boolean currentBrightnessWithinAllowedRange = BrightnessSynchronizer.floatEquals(f, clampScreenBrightness(f));
        boolean withinThreshold = !Float.isNaN(this.mScreenAutoBrightness) && newScreenAutoBrightness > this.mScreenDarkeningThreshold && newScreenAutoBrightness < this.mScreenBrighteningThreshold;
        if (withinThreshold && !isManuallySet && currentBrightnessWithinAllowedRange) {
            if (this.mLoggingEnabled) {
                Slog.d(TAG, "ignoring newScreenAutoBrightness: " + this.mScreenDarkeningThreshold + " < " + newScreenAutoBrightness + " < " + this.mScreenBrighteningThreshold);
            }
        } else if (!BrightnessSynchronizer.floatEquals(this.mScreenAutoBrightness, newScreenAutoBrightness)) {
            Slog.d(TAG, "updateAutoBrightness: mScreenAutoBrightness=" + this.mScreenAutoBrightness + ", newScreenAutoBrightness=" + newScreenAutoBrightness + ", mAmbientLux=" + this.mAmbientLux + ", mScreenDarkeningThreshold= " + this.mScreenDarkeningThreshold + ", mScreenBrighteningThreshold=" + this.mScreenBrighteningThreshold + ", mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer);
            if (!withinThreshold) {
                this.mPreThresholdBrightness = this.mScreenAutoBrightness;
            }
            this.mScreenAutoBrightness = newScreenAutoBrightness;
            this.mScreenBrighteningThreshold = clampScreenBrightness(this.mScreenBrightnessThresholds.getBrighteningThreshold(newScreenAutoBrightness));
            this.mScreenDarkeningThreshold = clampScreenBrightness(this.mScreenBrightnessThresholds.getDarkeningThreshold(newScreenAutoBrightness));
            if (sendUpdate) {
                this.mCallbacks.updateBrightness();
            }
        }
    }

    private float clampScreenBrightness(float value) {
        float minBrightness = Math.min(this.mHbmController.getCurrentBrightnessMin(), this.mBrightnessThrottler.getBrightnessCap());
        float maxBrightness = Math.min(this.mHbmController.getCurrentBrightnessMax(), this.mBrightnessThrottler.getBrightnessCap());
        return MathUtils.constrain(value, minBrightness, maxBrightness);
    }

    private void prepareBrightnessAdjustmentSample() {
        if (!this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = true;
            this.mBrightnessAdjustmentSampleOldLux = this.mAmbientLuxValid ? this.mAmbientLux : -1.0f;
            this.mBrightnessAdjustmentSampleOldBrightness = this.mScreenAutoBrightness;
        } else {
            this.mHandler.removeMessages(2);
        }
        this.mHandler.sendEmptyMessageDelayed(2, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private void cancelBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = false;
            this.mHandler.removeMessages(2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void collectBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = false;
            if (this.mAmbientLuxValid) {
                float f = this.mScreenAutoBrightness;
                if (f >= 0.0f || f == -1.0f) {
                    if (this.mLoggingEnabled) {
                        Slog.d(TAG, "Auto-brightness adjustment changed by user: lux=" + this.mAmbientLux + ", brightness=" + this.mScreenAutoBrightness + ", ring=" + this.mAmbientLightRingBuffer);
                    }
                    EventLog.writeEvent((int) EventLogTags.AUTO_BRIGHTNESS_ADJ, Float.valueOf(this.mBrightnessAdjustmentSampleOldLux), Float.valueOf(this.mBrightnessAdjustmentSampleOldBrightness), Float.valueOf(this.mAmbientLux), Float.valueOf(this.mScreenAutoBrightness));
                }
            }
        }
    }

    private void registerForegroundAppUpdater() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
            updateForegroundApp();
        } catch (RemoteException e) {
            if (this.mLoggingEnabled) {
                Slog.e(TAG, "Failed to register foreground app updater: " + e);
            }
        }
    }

    private void unregisterForegroundAppUpdater() {
        try {
            this.mActivityTaskManager.unregisterTaskStackListener(this.mTaskStackListener);
        } catch (RemoteException e) {
        }
        this.mForegroundAppPackageName = null;
        this.mForegroundAppCategory = -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundApp() {
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "Attempting to update foreground app");
        }
        this.mInjector.getBackgroundThreadHandler().post(new Runnable() { // from class: com.android.server.display.AutomaticBrightnessController.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityTaskManager.RootTaskInfo info = AutomaticBrightnessController.this.mActivityTaskManager.getFocusedRootTaskInfo();
                    if (info != null && info.topActivity != null) {
                        String packageName = info.topActivity.getPackageName();
                        if (AutomaticBrightnessController.this.mForegroundAppPackageName != null && AutomaticBrightnessController.this.mForegroundAppPackageName.equals(packageName)) {
                            return;
                        }
                        AutomaticBrightnessController.this.mPendingForegroundAppPackageName = packageName;
                        AutomaticBrightnessController.this.mPendingForegroundAppCategory = -1;
                        try {
                            ApplicationInfo app = AutomaticBrightnessController.this.mPackageManager.getApplicationInfo(packageName, 4194304);
                            AutomaticBrightnessController.this.mPendingForegroundAppCategory = app.category;
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                        AutomaticBrightnessController.this.mHandler.sendEmptyMessage(5);
                    }
                } catch (RemoteException e2) {
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForegroundAppSync() {
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "Updating foreground app: packageName=" + this.mPendingForegroundAppPackageName + ", category=" + this.mPendingForegroundAppCategory);
        }
        this.mForegroundAppPackageName = this.mPendingForegroundAppPackageName;
        this.mPendingForegroundAppPackageName = null;
        this.mForegroundAppCategory = this.mPendingForegroundAppCategory;
        this.mPendingForegroundAppCategory = -1;
        updateAutoBrightness(true, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void switchToIdleMode() {
        if (this.mIdleModeBrightnessMapper == null || this.mCurrentBrightnessMapper.isForIdleMode()) {
            return;
        }
        Slog.i(TAG, "Switching to Idle Screen Brightness Mode");
        this.mCurrentBrightnessMapper = this.mIdleModeBrightnessMapper;
        resetShortTermModel();
        update();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void switchToInteractiveScreenBrightnessMode() {
        if (!this.mCurrentBrightnessMapper.isForIdleMode()) {
            return;
        }
        Slog.i(TAG, "Switching to Interactive Screen Brightness Mode");
        this.mCurrentBrightnessMapper = this.mInteractiveModeBrightnessMapper;
        resetShortTermModel();
        update();
    }

    public float convertToNits(float brightness) {
        BrightnessMappingStrategy brightnessMappingStrategy = this.mCurrentBrightnessMapper;
        if (brightnessMappingStrategy != null) {
            return brightnessMappingStrategy.convertToNits(brightness);
        }
        return -1.0f;
    }

    public void recalculateSplines(boolean applyAdjustment, float[] adjustment) {
        this.mCurrentBrightnessMapper.recalculateSplines(applyAdjustment, adjustment);
        resetShortTermModel();
        if (applyAdjustment) {
            setScreenBrightnessByUser(getAutomaticScreenBrightness());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class AutomaticBrightnessHandler extends Handler {
        public AutomaticBrightnessHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AutomaticBrightnessController.this.updateAmbientLux();
                    return;
                case 2:
                    AutomaticBrightnessController.this.collectBrightnessAdjustmentSample();
                    return;
                case 3:
                    AutomaticBrightnessController.this.invalidateShortTermModel();
                    return;
                case 4:
                    AutomaticBrightnessController.this.updateForegroundApp();
                    return;
                case 5:
                    AutomaticBrightnessController.this.updateForegroundAppSync();
                    return;
                case 6:
                    AutomaticBrightnessController.this.updateAutoBrightness(true, false);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class TaskStackListenerImpl extends TaskStackListener {
        TaskStackListenerImpl() {
        }

        public void onTaskStackChanged() {
            AutomaticBrightnessController.this.mHandler.sendEmptyMessage(4);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AmbientLightRingBuffer {
        private static final float BUFFER_SLACK = 1.5f;
        private int mCapacity;
        Clock mClock;
        private int mCount;
        private int mEnd;
        private float[] mRingLux;
        private long[] mRingTime;
        private int mStart;

        public AmbientLightRingBuffer(long lightSensorRate, int ambientLightHorizon, Clock clock) {
            if (lightSensorRate <= 0) {
                throw new IllegalArgumentException("lightSensorRate must be above 0");
            }
            int ceil = (int) Math.ceil((ambientLightHorizon * BUFFER_SLACK) / ((float) lightSensorRate));
            this.mCapacity = ceil;
            this.mRingLux = new float[ceil];
            this.mRingTime = new long[ceil];
            this.mClock = clock;
        }

        public float getLux(int index) {
            return this.mRingLux[offsetOf(index)];
        }

        public long getTime(int index) {
            return this.mRingTime[offsetOf(index)];
        }

        public void push(long time, float lux) {
            int next = this.mEnd;
            int i = this.mCount;
            int i2 = this.mCapacity;
            if (i == i2) {
                int newSize = i2 * 2;
                float[] newRingLux = new float[newSize];
                long[] newRingTime = new long[newSize];
                int i3 = this.mStart;
                int length = i2 - i3;
                System.arraycopy(this.mRingLux, i3, newRingLux, 0, length);
                System.arraycopy(this.mRingTime, this.mStart, newRingTime, 0, length);
                int i4 = this.mStart;
                if (i4 != 0) {
                    System.arraycopy(this.mRingLux, 0, newRingLux, length, i4);
                    System.arraycopy(this.mRingTime, 0, newRingTime, length, this.mStart);
                }
                this.mRingLux = newRingLux;
                this.mRingTime = newRingTime;
                next = this.mCapacity;
                this.mCapacity = newSize;
                this.mStart = 0;
            }
            this.mRingTime[next] = time;
            this.mRingLux[next] = lux;
            int i5 = next + 1;
            this.mEnd = i5;
            if (i5 == this.mCapacity) {
                this.mEnd = 0;
            }
            this.mCount++;
        }

        public void prune(long horizon) {
            if (this.mCount == 0) {
                return;
            }
            while (true) {
                int i = this.mCount;
                if (i <= 1) {
                    break;
                }
                int next = this.mStart + 1;
                int i2 = this.mCapacity;
                if (next >= i2) {
                    next -= i2;
                }
                if (this.mRingTime[next] > horizon) {
                    break;
                }
                this.mStart = next;
                this.mCount = i - 1;
            }
            long[] jArr = this.mRingTime;
            int i3 = this.mStart;
            if (jArr[i3] < horizon) {
                jArr[i3] = horizon;
            }
        }

        public int size() {
            return this.mCount;
        }

        public void clear() {
            this.mStart = 0;
            this.mEnd = 0;
            this.mCount = 0;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append('[');
            int i = 0;
            while (true) {
                int i2 = this.mCount;
                if (i < i2) {
                    long next = i + 1 < i2 ? getTime(i + 1) : this.mClock.uptimeMillis();
                    if (i != 0) {
                        buf.append(", ");
                    }
                    buf.append(getLux(i));
                    buf.append(" / ");
                    buf.append(next - getTime(i));
                    buf.append("ms");
                    i++;
                } else {
                    buf.append(']');
                    return buf.toString();
                }
            }
        }

        private int offsetOf(int index) {
            if (index >= this.mCount || index < 0) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            int index2 = index + this.mStart;
            int i = this.mCapacity;
            if (index2 >= i) {
                return index2 - i;
            }
            return index2;
        }
    }

    /* loaded from: classes.dex */
    public static class Injector {
        public Handler getBackgroundThreadHandler() {
            return BackgroundThread.getHandler();
        }

        Clock createClock() {
            return new Clock() { // from class: com.android.server.display.AutomaticBrightnessController$Injector$$ExternalSyntheticLambda0
                @Override // com.android.server.display.AutomaticBrightnessController.Clock
                public final long uptimeMillis() {
                    return SystemClock.uptimeMillis();
                }
            };
        }
    }

    public float getTrackAmbientLux() {
        return this.mAmbientLux;
    }
}
