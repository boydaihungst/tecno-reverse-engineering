package com.android.server.display;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.BrightnessCorrection;
import android.text.TextUtils;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.Spline;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.util.Preconditions;
import com.android.server.display.utils.Plog;
import com.android.server.display.whitebalance.DisplayWhiteBalanceController;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
/* loaded from: classes.dex */
public abstract class BrightnessMappingStrategy {
    private static final float LUX_GRAD_SMOOTHING = 0.25f;
    private static final float MAX_GRAD = 1.0f;
    private static final float MIN_PERMISSABLE_INCREASE = 0.004f;
    private static final float SHORT_TERM_MODEL_THRESHOLD_RATIO = 0.6f;
    protected boolean mLoggingEnabled;
    private static final String TAG = "BrightnessMappingStrategy";
    private static final Plog PLOG = Plog.createSystemPlog(TAG);

    public abstract void addUserDataPoint(float f, float f2);

    public abstract void clearUserDataPoints();

    public abstract float convertToNits(float f);

    public abstract void dump(PrintWriter printWriter, float f);

    public abstract float getAutoBrightnessAdjustment();

    public abstract float getBrightness(float f, String str, int i);

    public abstract BrightnessConfiguration getBrightnessConfiguration();

    public abstract BrightnessConfiguration getDefaultConfig();

    public abstract long getShortTermModelTimeout();

    public abstract boolean hasUserDataPoints();

    public abstract boolean isDefaultConfig();

    public abstract boolean isForIdleMode();

    public abstract void recalculateSplines(boolean z, float[] fArr);

    public abstract boolean setAutoBrightnessAdjustment(float f);

    public abstract boolean setBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration);

    public static BrightnessMappingStrategy create(Resources resources, DisplayDeviceConfig displayDeviceConfig, DisplayWhiteBalanceController displayWhiteBalanceController) {
        return create(resources, displayDeviceConfig, false, displayWhiteBalanceController);
    }

    public static BrightnessMappingStrategy createForIdleMode(Resources resources, DisplayDeviceConfig displayDeviceConfig, DisplayWhiteBalanceController displayWhiteBalanceController) {
        return create(resources, displayDeviceConfig, true, displayWhiteBalanceController);
    }

    private static BrightnessMappingStrategy create(Resources resources, DisplayDeviceConfig displayDeviceConfig, boolean isForIdleMode, DisplayWhiteBalanceController displayWhiteBalanceController) {
        float[] brightnessLevelsNits;
        float[] luxLevels;
        if (isForIdleMode) {
            float[] brightnessLevelsNits2 = getFloatArray(resources.obtainTypedArray(17235988));
            brightnessLevelsNits = brightnessLevelsNits2;
            luxLevels = getLuxLevels(resources.getIntArray(17235993));
        } else {
            float[] brightnessLevelsNits3 = getFloatArray(resources.obtainTypedArray(17235987));
            brightnessLevelsNits = brightnessLevelsNits3;
            luxLevels = getLuxLevels(resources.getIntArray(17235992));
        }
        int[] brightnessLevelsBacklight = resources.getIntArray(17235990);
        float autoBrightnessAdjustmentMaxGamma = resources.getFraction(18022400, 1, 1);
        long shortTermModelTimeout = resources.getInteger(17694742);
        float[] nitsRange = displayDeviceConfig.getNits();
        float[] brightnessRange = displayDeviceConfig.getBrightness();
        if (isValidMapping(nitsRange, brightnessRange) && isValidMapping(luxLevels, brightnessLevelsNits)) {
            BrightnessConfiguration.Builder builder = new BrightnessConfiguration.Builder(luxLevels, brightnessLevelsNits);
            builder.setShortTermModelTimeoutMillis(shortTermModelTimeout);
            builder.setShortTermModelLowerLuxMultiplier((float) SHORT_TERM_MODEL_THRESHOLD_RATIO);
            builder.setShortTermModelUpperLuxMultiplier((float) SHORT_TERM_MODEL_THRESHOLD_RATIO);
            return new PhysicalMappingStrategy(builder.build(), nitsRange, brightnessRange, autoBrightnessAdjustmentMaxGamma, isForIdleMode, displayWhiteBalanceController);
        } else if (isValidMapping(luxLevels, brightnessLevelsBacklight) && !isForIdleMode) {
            return new SimpleMappingStrategy(luxLevels, brightnessLevelsBacklight, autoBrightnessAdjustmentMaxGamma, shortTermModelTimeout);
        } else {
            return null;
        }
    }

    private static float[] getLuxLevels(int[] lux) {
        float[] levels = new float[lux.length + 1];
        for (int i = 0; i < lux.length; i++) {
            levels[i + 1] = lux[i];
        }
        return levels;
    }

    public static float[] getFloatArray(TypedArray array) {
        int N = array.length();
        float[] vals = new float[N];
        for (int i = 0; i < N; i++) {
            vals[i] = array.getFloat(i, -1.0f);
        }
        array.recycle();
        return vals;
    }

    private static boolean isValidMapping(float[] x, float[] y) {
        if (x == null || y == null || x.length == 0 || y.length == 0 || x.length != y.length) {
            return false;
        }
        int N = x.length;
        float prevX = x[0];
        float prevY = y[0];
        if (prevX < 0.0f || prevY < 0.0f || Float.isNaN(prevX) || Float.isNaN(prevY)) {
            return false;
        }
        for (int i = 1; i < N; i++) {
            if (prevX >= x[i] || prevY > y[i] || Float.isNaN(x[i]) || Float.isNaN(y[i])) {
                return false;
            }
            prevX = x[i];
            prevY = y[i];
        }
        return true;
    }

    private static boolean isValidMapping(float[] x, int[] y) {
        if (x == null || y == null || x.length == 0 || y.length == 0 || x.length != y.length) {
            return false;
        }
        int N = x.length;
        float prevX = x[0];
        int prevY = y[0];
        if (prevX < 0.0f || prevY < 0 || Float.isNaN(prevX)) {
            return false;
        }
        for (int i = 1; i < N; i++) {
            if (prevX >= x[i] || prevY > y[i] || Float.isNaN(x[i])) {
                return false;
            }
            prevX = x[i];
            prevY = y[i];
        }
        return true;
    }

    public boolean setLoggingEnabled(boolean loggingEnabled) {
        if (this.mLoggingEnabled == loggingEnabled) {
            return false;
        }
        this.mLoggingEnabled = loggingEnabled;
        return true;
    }

    public float getBrightness(float lux) {
        return getBrightness(lux, null, -1);
    }

    public boolean shouldResetShortTermModel(float ambientLux, float shortTermModelAnchor) {
        BrightnessConfiguration config = getBrightnessConfiguration();
        float minThresholdRatio = SHORT_TERM_MODEL_THRESHOLD_RATIO;
        float maxThresholdRatio = SHORT_TERM_MODEL_THRESHOLD_RATIO;
        if (config != null) {
            if (!Float.isNaN(config.getShortTermModelLowerLuxMultiplier())) {
                minThresholdRatio = config.getShortTermModelLowerLuxMultiplier();
            }
            if (!Float.isNaN(config.getShortTermModelUpperLuxMultiplier())) {
                maxThresholdRatio = config.getShortTermModelUpperLuxMultiplier();
            }
        }
        float minAmbientLux = shortTermModelAnchor - (shortTermModelAnchor * minThresholdRatio);
        float maxAmbientLux = (shortTermModelAnchor * maxThresholdRatio) + shortTermModelAnchor;
        if (minAmbientLux < ambientLux && ambientLux <= maxAmbientLux) {
            if (this.mLoggingEnabled) {
                Slog.d(TAG, "ShortTermModel: re-validate user data, ambient lux is " + minAmbientLux + " < " + ambientLux + " < " + maxAmbientLux);
                return false;
            }
            return false;
        }
        Slog.d(TAG, "ShortTermModel: reset data, ambient lux is " + ambientLux + "(" + minAmbientLux + ", " + maxAmbientLux + ")");
        return true;
    }

    protected static float normalizeAbsoluteBrightness(int brightness) {
        return BrightnessSynchronizer.brightnessIntToFloat(brightness);
    }

    private Pair<float[], float[]> insertControlPoint(float[] luxLevels, float[] brightnessLevels, float lux, float brightness) {
        float[] newLuxLevels;
        float[] newBrightnessLevels;
        int idx = findInsertionPoint(luxLevels, lux);
        if (idx == luxLevels.length) {
            newLuxLevels = Arrays.copyOf(luxLevels, luxLevels.length + 1);
            newBrightnessLevels = Arrays.copyOf(brightnessLevels, brightnessLevels.length + 1);
            newLuxLevels[idx] = lux;
            newBrightnessLevels[idx] = brightness;
        } else if (luxLevels[idx] == lux) {
            newLuxLevels = Arrays.copyOf(luxLevels, luxLevels.length);
            newBrightnessLevels = Arrays.copyOf(brightnessLevels, brightnessLevels.length);
            newBrightnessLevels[idx] = brightness;
        } else {
            newLuxLevels = Arrays.copyOf(luxLevels, luxLevels.length + 1);
            System.arraycopy(newLuxLevels, idx, newLuxLevels, idx + 1, luxLevels.length - idx);
            newLuxLevels[idx] = lux;
            newBrightnessLevels = Arrays.copyOf(brightnessLevels, brightnessLevels.length + 1);
            System.arraycopy(newBrightnessLevels, idx, newBrightnessLevels, idx + 1, brightnessLevels.length - idx);
            newBrightnessLevels[idx] = brightness;
        }
        smoothCurve(newLuxLevels, newBrightnessLevels, idx);
        return Pair.create(newLuxLevels, newBrightnessLevels);
    }

    private int findInsertionPoint(float[] arr, float val) {
        for (int i = 0; i < arr.length; i++) {
            if (val <= arr[i]) {
                return i;
            }
        }
        int i2 = arr.length;
        return i2;
    }

    private void smoothCurve(float[] lux, float[] brightness, int idx) {
        if (this.mLoggingEnabled) {
            PLOG.logCurve("unsmoothed curve", lux, brightness);
        }
        float prevLux = lux[idx];
        float prevBrightness = brightness[idx];
        for (int i = idx + 1; i < lux.length; i++) {
            float currLux = lux[i];
            float currBrightness = brightness[i];
            float maxBrightness = MathUtils.max(permissibleRatio(currLux, prevLux) * prevBrightness, MIN_PERMISSABLE_INCREASE + prevBrightness);
            float newBrightness = MathUtils.constrain(currBrightness, prevBrightness, maxBrightness);
            if (newBrightness == currBrightness) {
                break;
            }
            prevLux = currLux;
            prevBrightness = newBrightness;
            brightness[i] = newBrightness;
        }
        float prevLux2 = lux[idx];
        float prevBrightness2 = brightness[idx];
        for (int i2 = idx - 1; i2 >= 0; i2--) {
            float currLux2 = lux[i2];
            float currBrightness2 = brightness[i2];
            float minBrightness = permissibleRatio(currLux2, prevLux2) * prevBrightness2;
            float newBrightness2 = MathUtils.constrain(currBrightness2, minBrightness, prevBrightness2);
            if (newBrightness2 == currBrightness2) {
                break;
            }
            prevLux2 = currLux2;
            prevBrightness2 = newBrightness2;
            brightness[i2] = newBrightness2;
        }
        if (this.mLoggingEnabled) {
            PLOG.logCurve("smoothed curve", lux, brightness);
        }
    }

    private float permissibleRatio(float currLux, float prevLux) {
        return MathUtils.pow((currLux + LUX_GRAD_SMOOTHING) / (LUX_GRAD_SMOOTHING + prevLux), 1.0f);
    }

    protected float inferAutoBrightnessAdjustment(float maxGamma, float desiredBrightness, float currentBrightness) {
        float adjustment;
        float gamma = Float.NaN;
        if (currentBrightness <= 0.1f || currentBrightness >= 0.9f) {
            adjustment = desiredBrightness - currentBrightness;
        } else if (desiredBrightness == 0.0f) {
            adjustment = -1.0f;
        } else if (desiredBrightness == 1.0f) {
            adjustment = 1.0f;
        } else {
            gamma = MathUtils.log(desiredBrightness) / MathUtils.log(currentBrightness);
            adjustment = (-MathUtils.log(gamma)) / MathUtils.log(maxGamma);
        }
        float adjustment2 = MathUtils.constrain(adjustment, -1.0f, 1.0f);
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "inferAutoBrightnessAdjustment: " + maxGamma + "^" + (-adjustment2) + "=" + MathUtils.pow(maxGamma, -adjustment2) + " == " + gamma);
            Slog.d(TAG, "inferAutoBrightnessAdjustment: " + currentBrightness + "^" + gamma + "=" + MathUtils.pow(currentBrightness, gamma) + " == " + desiredBrightness);
        }
        return adjustment2;
    }

    protected Pair<float[], float[]> getAdjustedCurve(float[] lux, float[] brightness, float userLux, float userBrightness, float adjustment, float maxGamma) {
        float[] newLux = lux;
        float[] newBrightness = Arrays.copyOf(brightness, brightness.length);
        if (this.mLoggingEnabled) {
            PLOG.logCurve("unadjusted curve", newLux, newBrightness);
        }
        float adjustment2 = MathUtils.constrain(adjustment, -1.0f, 1.0f);
        float gamma = MathUtils.pow(maxGamma, -adjustment2);
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "getAdjustedCurve: " + maxGamma + "^" + (-adjustment2) + "=" + MathUtils.pow(maxGamma, -adjustment2) + " == " + gamma);
        }
        if (gamma != 1.0f) {
            for (int i = 0; i < newBrightness.length; i++) {
                newBrightness[i] = MathUtils.pow(newBrightness[i], gamma);
            }
        }
        if (this.mLoggingEnabled) {
            PLOG.logCurve("gamma adjusted curve", newLux, newBrightness);
        }
        if (userLux != -1.0f) {
            Pair<float[], float[]> curve = insertControlPoint(newLux, newBrightness, userLux, userBrightness);
            newLux = (float[]) curve.first;
            newBrightness = (float[]) curve.second;
            if (this.mLoggingEnabled) {
                Plog plog = PLOG;
                plog.logCurve("gamma and user adjusted curve", newLux, newBrightness);
                Pair<float[], float[]> curve2 = insertControlPoint(lux, brightness, userLux, userBrightness);
                plog.logCurve("user adjusted curve", (float[]) curve2.first, (float[]) curve2.second);
            }
        }
        return Pair.create(newLux, newBrightness);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class SimpleMappingStrategy extends BrightnessMappingStrategy {
        private float mAutoBrightnessAdjustment;
        private final float[] mBrightness;
        private final float[] mLux;
        private float mMaxGamma;
        private long mShortTermModelTimeout;
        private Spline mSpline;
        private float mUserBrightness;
        private float mUserLux;

        private SimpleMappingStrategy(float[] lux, int[] brightness, float maxGamma, long timeout) {
            Preconditions.checkArgument((lux.length == 0 || brightness.length == 0) ? false : true, "Lux and brightness arrays must not be empty!");
            Preconditions.checkArgument(lux.length == brightness.length, "Lux and brightness arrays must be the same length!");
            Preconditions.checkArrayElementsInRange(lux, 0.0f, Float.MAX_VALUE, "lux");
            Preconditions.checkArrayElementsInRange(brightness, 0, Integer.MAX_VALUE, "brightness");
            int N = brightness.length;
            this.mLux = new float[N];
            this.mBrightness = new float[N];
            for (int i = 0; i < N; i++) {
                this.mLux[i] = lux[i];
                this.mBrightness[i] = normalizeAbsoluteBrightness(brightness[i]);
            }
            this.mMaxGamma = maxGamma;
            this.mAutoBrightnessAdjustment = 0.0f;
            this.mUserLux = -1.0f;
            this.mUserBrightness = -1.0f;
            if (this.mLoggingEnabled) {
                BrightnessMappingStrategy.PLOG.start("simple mapping strategy");
            }
            computeSpline();
            this.mShortTermModelTimeout = timeout;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public long getShortTermModelTimeout() {
            return this.mShortTermModelTimeout;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean setBrightnessConfiguration(BrightnessConfiguration config) {
            return false;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public BrightnessConfiguration getBrightnessConfiguration() {
            return null;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public float getBrightness(float lux, String packageName, int category) {
            return this.mSpline.interpolate(lux);
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public float getAutoBrightnessAdjustment() {
            return this.mAutoBrightnessAdjustment;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean setAutoBrightnessAdjustment(float adjustment) {
            float adjustment2 = MathUtils.constrain(adjustment, -1.0f, 1.0f);
            if (adjustment2 == this.mAutoBrightnessAdjustment) {
                return false;
            }
            if (this.mLoggingEnabled) {
                Slog.d(BrightnessMappingStrategy.TAG, "setAutoBrightnessAdjustment: " + this.mAutoBrightnessAdjustment + " => " + adjustment2);
                BrightnessMappingStrategy.PLOG.start("auto-brightness adjustment");
            }
            this.mAutoBrightnessAdjustment = adjustment2;
            computeSpline();
            return true;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public float convertToNits(float brightness) {
            return -1.0f;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public void addUserDataPoint(float lux, float brightness) {
            float unadjustedBrightness = getUnadjustedBrightness(lux);
            if (this.mLoggingEnabled) {
                Slog.d(BrightnessMappingStrategy.TAG, "addUserDataPoint: (" + lux + "," + brightness + ")");
                BrightnessMappingStrategy.PLOG.start("add user data point").logPoint("user data point", lux, brightness).logPoint("current brightness", lux, unadjustedBrightness);
            }
            float adjustment = inferAutoBrightnessAdjustment(this.mMaxGamma, brightness, unadjustedBrightness);
            if (this.mLoggingEnabled) {
                Slog.d(BrightnessMappingStrategy.TAG, "addUserDataPoint: " + this.mAutoBrightnessAdjustment + " => " + adjustment);
            }
            this.mAutoBrightnessAdjustment = adjustment;
            this.mUserLux = lux;
            this.mUserBrightness = brightness;
            computeSpline();
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public void clearUserDataPoints() {
            if (this.mUserLux != -1.0f) {
                if (this.mLoggingEnabled) {
                    Slog.d(BrightnessMappingStrategy.TAG, "clearUserDataPoints: " + this.mAutoBrightnessAdjustment + " => 0");
                    BrightnessMappingStrategy.PLOG.start("clear user data points").logPoint("user data point", this.mUserLux, this.mUserBrightness);
                }
                this.mAutoBrightnessAdjustment = 0.0f;
                this.mUserLux = -1.0f;
                this.mUserBrightness = -1.0f;
                computeSpline();
            }
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean hasUserDataPoints() {
            return this.mUserLux != -1.0f;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean isDefaultConfig() {
            return true;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public BrightnessConfiguration getDefaultConfig() {
            return null;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public void recalculateSplines(boolean applyAdjustment, float[] adjustment) {
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public void dump(PrintWriter pw, float hbmTransition) {
            pw.println("SimpleMappingStrategy");
            pw.println("  mSpline=" + this.mSpline);
            pw.println("  mMaxGamma=" + this.mMaxGamma);
            pw.println("  mAutoBrightnessAdjustment=" + this.mAutoBrightnessAdjustment);
            pw.println("  mUserLux=" + this.mUserLux);
            pw.println("  mUserBrightness=" + this.mUserBrightness);
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean isForIdleMode() {
            return false;
        }

        private void computeSpline() {
            Pair<float[], float[]> curve = getAdjustedCurve(this.mLux, this.mBrightness, this.mUserLux, this.mUserBrightness, this.mAutoBrightnessAdjustment, this.mMaxGamma);
            this.mSpline = Spline.createSpline((float[]) curve.first, (float[]) curve.second);
        }

        private float getUnadjustedBrightness(float lux) {
            Spline spline = Spline.createSpline(this.mLux, this.mBrightness);
            return spline.interpolate(lux);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class PhysicalMappingStrategy extends BrightnessMappingStrategy {
        private float mAutoBrightnessAdjustment;
        private final float[] mBrightness;
        private boolean mBrightnessRangeAdjustmentApplied;
        private Spline mBrightnessSpline;
        private Spline mBrightnessToNitsSpline;
        private BrightnessConfiguration mConfig;
        private final BrightnessConfiguration mDefaultConfig;
        private final DisplayWhiteBalanceController mDisplayWhiteBalanceController;
        private final boolean mIsForIdleMode;
        private final float mMaxGamma;
        private final float[] mNits;
        private Spline mNitsToBrightnessSpline;
        private float mUserBrightness;
        private float mUserLux;

        public PhysicalMappingStrategy(BrightnessConfiguration config, float[] nits, float[] brightness, float maxGamma, boolean isForIdleMode, DisplayWhiteBalanceController displayWhiteBalanceController) {
            Preconditions.checkArgument((nits.length == 0 || brightness.length == 0) ? false : true, "Nits and brightness arrays must not be empty!");
            Preconditions.checkArgument(nits.length == brightness.length, "Nits and brightness arrays must be the same length!");
            Objects.requireNonNull(config);
            Preconditions.checkArrayElementsInRange(nits, 0.0f, Float.MAX_VALUE, "nits");
            Preconditions.checkArrayElementsInRange(brightness, 0.0f, 1.0f, "brightness");
            this.mIsForIdleMode = isForIdleMode;
            this.mMaxGamma = maxGamma;
            this.mAutoBrightnessAdjustment = 0.0f;
            this.mUserLux = -1.0f;
            this.mUserBrightness = -1.0f;
            this.mDisplayWhiteBalanceController = displayWhiteBalanceController;
            this.mNits = nits;
            this.mBrightness = brightness;
            computeNitsBrightnessSplines(nits);
            this.mDefaultConfig = config;
            if (this.mLoggingEnabled) {
                BrightnessMappingStrategy.PLOG.start("physical mapping strategy");
            }
            this.mConfig = config;
            computeSpline();
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public long getShortTermModelTimeout() {
            if (this.mConfig.getShortTermModelTimeoutMillis() >= 0) {
                return this.mConfig.getShortTermModelTimeoutMillis();
            }
            return this.mDefaultConfig.getShortTermModelTimeoutMillis();
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean setBrightnessConfiguration(BrightnessConfiguration config) {
            if (config == null) {
                config = this.mDefaultConfig;
            }
            if (config.equals(this.mConfig)) {
                return false;
            }
            if (this.mLoggingEnabled) {
                BrightnessMappingStrategy.PLOG.start("brightness configuration");
            }
            this.mConfig = config;
            computeSpline();
            return true;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public BrightnessConfiguration getBrightnessConfiguration() {
            return this.mConfig;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public float getBrightness(float lux, String packageName, int category) {
            float nits = this.mBrightnessSpline.interpolate(lux);
            DisplayWhiteBalanceController displayWhiteBalanceController = this.mDisplayWhiteBalanceController;
            if (displayWhiteBalanceController != null) {
                nits = displayWhiteBalanceController.calculateAdjustedBrightnessNits(nits);
            }
            float brightness = this.mNitsToBrightnessSpline.interpolate(nits);
            if (this.mUserLux == -1.0f) {
                return correctBrightness(brightness, packageName, category);
            }
            if (this.mLoggingEnabled) {
                Slog.d(BrightnessMappingStrategy.TAG, "user point set, correction not applied");
                return brightness;
            }
            return brightness;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public float getAutoBrightnessAdjustment() {
            return this.mAutoBrightnessAdjustment;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean setAutoBrightnessAdjustment(float adjustment) {
            float adjustment2 = MathUtils.constrain(adjustment, -1.0f, 1.0f);
            if (adjustment2 == this.mAutoBrightnessAdjustment) {
                return false;
            }
            if (this.mLoggingEnabled) {
                Slog.d(BrightnessMappingStrategy.TAG, "setAutoBrightnessAdjustment: " + this.mAutoBrightnessAdjustment + " => " + adjustment2);
                BrightnessMappingStrategy.PLOG.start("auto-brightness adjustment");
            }
            this.mAutoBrightnessAdjustment = adjustment2;
            computeSpline();
            return true;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public float convertToNits(float brightness) {
            return this.mBrightnessToNitsSpline.interpolate(brightness);
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public void addUserDataPoint(float lux, float brightness) {
            float unadjustedBrightness = getUnadjustedBrightness(lux);
            if (this.mLoggingEnabled) {
                Slog.d(BrightnessMappingStrategy.TAG, "addUserDataPoint: (" + lux + "," + brightness + ")");
                BrightnessMappingStrategy.PLOG.start("add user data point").logPoint("user data point", lux, brightness).logPoint("current brightness", lux, unadjustedBrightness);
            }
            float adjustment = inferAutoBrightnessAdjustment(this.mMaxGamma, brightness, unadjustedBrightness);
            if (this.mLoggingEnabled) {
                Slog.d(BrightnessMappingStrategy.TAG, "addUserDataPoint: " + this.mAutoBrightnessAdjustment + " => " + adjustment);
            }
            this.mAutoBrightnessAdjustment = adjustment;
            this.mUserLux = lux;
            this.mUserBrightness = brightness;
            computeSpline();
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public void clearUserDataPoints() {
            if (this.mUserLux != -1.0f) {
                if (this.mLoggingEnabled) {
                    Slog.d(BrightnessMappingStrategy.TAG, "clearUserDataPoints: " + this.mAutoBrightnessAdjustment + " => 0");
                    BrightnessMappingStrategy.PLOG.start("clear user data points").logPoint("user data point", this.mUserLux, this.mUserBrightness);
                }
                this.mAutoBrightnessAdjustment = 0.0f;
                this.mUserLux = -1.0f;
                this.mUserBrightness = -1.0f;
                computeSpline();
            }
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean hasUserDataPoints() {
            return this.mUserLux != -1.0f;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean isDefaultConfig() {
            return this.mDefaultConfig.equals(this.mConfig);
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public BrightnessConfiguration getDefaultConfig() {
            return this.mDefaultConfig;
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public void recalculateSplines(boolean applyAdjustment, float[] adjustedNits) {
            this.mBrightnessRangeAdjustmentApplied = applyAdjustment;
            computeNitsBrightnessSplines(applyAdjustment ? adjustedNits : this.mNits);
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public void dump(PrintWriter pw, float hbmTransition) {
            pw.println("PhysicalMappingStrategy");
            pw.println("  mConfig=" + this.mConfig);
            pw.println("  mBrightnessSpline=" + this.mBrightnessSpline);
            pw.println("  mNitsToBrightnessSpline=" + this.mNitsToBrightnessSpline);
            pw.println("  mBrightnessToNitsSpline=" + this.mBrightnessToNitsSpline);
            pw.println("  mMaxGamma=" + this.mMaxGamma);
            pw.println("  mAutoBrightnessAdjustment=" + this.mAutoBrightnessAdjustment);
            pw.println("  mUserLux=" + this.mUserLux);
            pw.println("  mUserBrightness=" + this.mUserBrightness);
            pw.println("  mDefaultConfig=" + this.mDefaultConfig);
            pw.println("  mBrightnessRangeAdjustmentApplied=" + this.mBrightnessRangeAdjustmentApplied);
            dumpConfigDiff(pw, hbmTransition);
        }

        @Override // com.android.server.display.BrightnessMappingStrategy
        public boolean isForIdleMode() {
            return this.mIsForIdleMode;
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r8v7 */
        private void dumpConfigDiff(PrintWriter pw, float hbmTransition) {
            int i;
            float[] luxes;
            PrintWriter printWriter;
            PhysicalMappingStrategy physicalMappingStrategy = this;
            pw.println("  Difference between current config and default: ");
            Pair<float[], float[]> currentCurve = physicalMappingStrategy.mConfig.getCurve();
            Spline currSpline = Spline.createSpline((float[]) currentCurve.first, (float[]) currentCurve.second);
            Pair<float[], float[]> defaultCurve = physicalMappingStrategy.mDefaultConfig.getCurve();
            Spline defaultSpline = Spline.createSpline((float[]) defaultCurve.first, (float[]) defaultCurve.second);
            float[] luxes2 = (float[]) currentCurve.first;
            if (physicalMappingStrategy.mUserLux >= 0.0f) {
                luxes2 = Arrays.copyOf((float[]) currentCurve.first, ((float[]) currentCurve.first).length + 1);
                luxes2[luxes2.length - 1] = physicalMappingStrategy.mUserLux;
                Arrays.sort(luxes2);
            }
            StringBuilder sbLong = null;
            StringBuilder sbShort = null;
            StringBuilder sbBrightness = null;
            StringBuilder sbPercent = null;
            StringBuilder sbPercentHbm = null;
            StringBuilder sbPercent2 = null;
            StringBuilder sbPercentHbm2 = null;
            boolean needsHeaders = true;
            String separator = "";
            int i2 = 0;
            while (true) {
                Pair<float[], float[]> defaultCurve2 = defaultCurve;
                if (i2 < luxes2.length) {
                    float lux = luxes2[i2];
                    if (needsHeaders) {
                        StringBuilder sbLux = new StringBuilder("            lux: ");
                        StringBuilder sbNits = new StringBuilder("        default: ");
                        StringBuilder sbLong2 = new StringBuilder("      long-term: ");
                        StringBuilder sbShort2 = new StringBuilder("        current: ");
                        StringBuilder sbBrightness2 = new StringBuilder("    current(bl): ");
                        StringBuilder sbPercent3 = new StringBuilder("     current(%): ");
                        StringBuilder sbPercentHbm3 = new StringBuilder("  current(hbm%): ");
                        needsHeaders = false;
                        sbPercent2 = sbPercent3;
                        sbPercentHbm2 = sbPercentHbm3;
                        sbPercent = sbShort2;
                        sbPercentHbm = sbBrightness2;
                        sbShort = sbNits;
                        sbBrightness = sbLong2;
                        sbLong = sbLux;
                    }
                    boolean needsHeaders2 = needsHeaders;
                    float defaultNits = defaultSpline.interpolate(lux);
                    Spline defaultSpline2 = defaultSpline;
                    float longTermNits = currSpline.interpolate(lux);
                    Spline currSpline2 = currSpline;
                    Spline currSpline3 = physicalMappingStrategy.mBrightnessSpline;
                    float shortTermNits = currSpline3.interpolate(lux);
                    float brightness = physicalMappingStrategy.mNitsToBrightnessSpline.interpolate(shortTermNits);
                    int i3 = i2;
                    float[] luxes3 = luxes2;
                    String luxPrefix = lux == physicalMappingStrategy.mUserLux ? "^" : "";
                    StringBuilder append = new StringBuilder().append(luxPrefix);
                    String luxPrefix2 = physicalMappingStrategy.toStrFloatForDump(lux);
                    String strLux = append.append(luxPrefix2).toString();
                    String strNits = physicalMappingStrategy.toStrFloatForDump(defaultNits);
                    String strLong = physicalMappingStrategy.toStrFloatForDump(longTermNits);
                    String strShort = physicalMappingStrategy.toStrFloatForDump(shortTermNits);
                    String strBrightness = physicalMappingStrategy.toStrFloatForDump(brightness);
                    String strPercent = String.valueOf(Math.round(BrightnessUtils.convertLinearToGamma(brightness / hbmTransition) * 100.0f));
                    String strPercentHbm = String.valueOf(Math.round(BrightnessUtils.convertLinearToGamma(brightness) * 100.0f));
                    int maxLen = Math.max(strLux.length(), Math.max(strNits.length(), Math.max(strBrightness.length(), Math.max(strPercent.length(), Math.max(strPercentHbm.length(), Math.max(strLong.length(), strShort.length()))))));
                    String format = separator + "%" + maxLen + "s";
                    String separator2 = ", ";
                    sbLong.append(TextUtils.formatSimple(format, new Object[]{strLux}));
                    sbShort.append(TextUtils.formatSimple(format, new Object[]{strNits}));
                    sbBrightness.append(TextUtils.formatSimple(format, new Object[]{strLong}));
                    sbPercent.append(TextUtils.formatSimple(format, new Object[]{strShort}));
                    sbPercentHbm.append(TextUtils.formatSimple(format, new Object[]{strBrightness}));
                    sbPercent2.append(TextUtils.formatSimple(format, new Object[]{strPercent}));
                    sbPercentHbm2 = sbPercentHbm2;
                    sbPercentHbm2.append(TextUtils.formatSimple(format, new Object[]{strPercentHbm}));
                    if (sbLong.length() <= 80) {
                        luxes = luxes3;
                        i = i3;
                        if (i != luxes.length - 1) {
                            printWriter = pw;
                            i2 = i + 1;
                            physicalMappingStrategy = this;
                            luxes2 = luxes;
                            defaultCurve = defaultCurve2;
                            needsHeaders = needsHeaders2;
                            defaultSpline = defaultSpline2;
                            separator = separator2;
                            currSpline = currSpline2;
                        }
                    } else {
                        i = i3;
                        luxes = luxes3;
                    }
                    printWriter = pw;
                    printWriter.println(sbLong);
                    printWriter.println(sbShort);
                    printWriter.println(sbBrightness);
                    printWriter.println(sbPercent);
                    printWriter.println(sbPercentHbm);
                    printWriter.println(sbPercent2);
                    if (hbmTransition < 1.0f) {
                        printWriter.println(sbPercentHbm2);
                    }
                    printWriter.println("");
                    separator2 = "";
                    needsHeaders2 = true;
                    i2 = i + 1;
                    physicalMappingStrategy = this;
                    luxes2 = luxes;
                    defaultCurve = defaultCurve2;
                    needsHeaders = needsHeaders2;
                    defaultSpline = defaultSpline2;
                    separator = separator2;
                    currSpline = currSpline2;
                } else {
                    return;
                }
            }
        }

        private String toStrFloatForDump(float value) {
            if (value == 0.0f) {
                return "0";
            }
            return value < 0.1f ? String.format(Locale.US, "%.3f", Float.valueOf(value)) : value < 1.0f ? String.format(Locale.US, "%.2f", Float.valueOf(value)) : value < 10.0f ? String.format(Locale.US, "%.1f", Float.valueOf(value)) : TextUtils.formatSimple("%d", new Object[]{Integer.valueOf(Math.round(value))});
        }

        private void computeNitsBrightnessSplines(float[] nits) {
            this.mNitsToBrightnessSpline = Spline.createSpline(nits, this.mBrightness);
            this.mBrightnessToNitsSpline = Spline.createSpline(this.mBrightness, nits);
        }

        private void computeSpline() {
            Pair<float[], float[]> defaultCurve = this.mConfig.getCurve();
            float[] defaultLux = (float[]) defaultCurve.first;
            float[] defaultNits = (float[]) defaultCurve.second;
            float[] defaultBrightness = new float[defaultNits.length];
            for (int i = 0; i < defaultBrightness.length; i++) {
                defaultBrightness[i] = this.mNitsToBrightnessSpline.interpolate(defaultNits[i]);
            }
            Pair<float[], float[]> curve = getAdjustedCurve(defaultLux, defaultBrightness, this.mUserLux, this.mUserBrightness, this.mAutoBrightnessAdjustment, this.mMaxGamma);
            float[] lux = (float[]) curve.first;
            float[] brightness = (float[]) curve.second;
            float[] nits = new float[brightness.length];
            for (int i2 = 0; i2 < nits.length; i2++) {
                nits[i2] = this.mBrightnessToNitsSpline.interpolate(brightness[i2]);
            }
            this.mBrightnessSpline = Spline.createSpline(lux, nits);
        }

        private float getUnadjustedBrightness(float lux) {
            Pair<float[], float[]> curve = this.mConfig.getCurve();
            Spline spline = Spline.createSpline((float[]) curve.first, (float[]) curve.second);
            return this.mNitsToBrightnessSpline.interpolate(spline.interpolate(lux));
        }

        private float correctBrightness(float brightness, String packageName, int category) {
            BrightnessCorrection correction;
            BrightnessCorrection correction2;
            if (packageName != null && (correction2 = this.mConfig.getCorrectionByPackageName(packageName)) != null) {
                return correction2.apply(brightness);
            }
            if (category != -1 && (correction = this.mConfig.getCorrectionByCategory(category)) != null) {
                return correction.apply(brightness);
            }
            return brightness;
        }
    }
}
