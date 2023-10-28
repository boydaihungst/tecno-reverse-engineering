package android.hardware.display;

import android.companion.virtual.IVirtualDevice;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Handler;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.window.DisplayWindowPolicyController;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes.dex */
public abstract class DisplayManagerInternal {
    public static final int REFRESH_RATE_LIMIT_HIGH_BRIGHTNESS_MODE = 1;

    /* loaded from: classes.dex */
    public interface DisplayGroupListener {
        void onDisplayGroupAdded(int i);

        void onDisplayGroupChanged(int i);

        void onDisplayGroupRemoved(int i);
    }

    /* loaded from: classes.dex */
    public interface DisplayPowerCallbacks {
        void acquireSuspendBlocker(String str);

        void onDisplayStateChange(boolean z, boolean z2);

        void onProximityNegative();

        void onProximityPositive();

        void onStateChanged();

        void releaseSuspendBlocker(String str);
    }

    /* loaded from: classes.dex */
    public interface DisplayTransactionListener {
        void onDisplayTransaction(SurfaceControl.Transaction transaction);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface RefreshRateLimitType {
    }

    public abstract int createVirtualDisplay(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback iVirtualDisplayCallback, IVirtualDevice iVirtualDevice, DisplayWindowPolicyController displayWindowPolicyController, String str);

    public abstract float getDefaultDisplayBrightness();

    public abstract DisplayInfo getDisplayInfo(int i);

    public abstract Point getDisplayPosition(int i);

    public abstract Point getDisplaySurfaceDefaultSize(int i);

    public abstract DisplayWindowPolicyController getDisplayWindowPolicyController(int i);

    public abstract DisplayedContentSample getDisplayedContentSample(int i, long j, long j2);

    public abstract DisplayedContentSamplingAttributes getDisplayedContentSamplingAttributes(int i);

    public abstract void getNonOverrideDisplayInfo(int i, DisplayInfo displayInfo);

    public abstract Set<DisplayInfo> getPossibleDisplayInfo(int i);

    public abstract RefreshRateRange getRefreshRateForDisplayAndSensor(int i, String str, String str2);

    public abstract List<RefreshRateLimitation> getRefreshRateLimitations(int i);

    public abstract int getRefreshRateSwitchingType();

    public abstract void ignoreProximitySensorUntilChanged();

    public abstract void initPowerManagement(DisplayPowerCallbacks displayPowerCallbacks, Handler handler, SensorManager sensorManager);

    public abstract boolean isProximitySensorAvailable();

    public abstract void onEarlyInteractivityChange(boolean z);

    public abstract void onOverlayChanged();

    public abstract void onWakeDisplayCalled();

    public abstract void performTraversal(SurfaceControl.Transaction transaction);

    public abstract void persistBrightnessTrackerState();

    public abstract void preResumeLcm();

    public abstract void registerDisplayGroupListener(DisplayGroupListener displayGroupListener);

    public abstract void registerDisplayTransactionListener(DisplayTransactionListener displayTransactionListener);

    public abstract boolean requestPowerState(int i, DisplayPowerRequest displayPowerRequest, boolean z);

    public abstract void setDisplayAccessUIDs(SparseArray<IntArray> sparseArray);

    public abstract void setDisplayInfoOverrideFromWindowManager(int i, DisplayInfo displayInfo);

    public abstract void setDisplayOffsets(int i, int i2, int i3);

    public abstract void setDisplayProperties(int i, boolean z, float f, int i2, float f2, float f3, boolean z2, boolean z3);

    public abstract void setDisplayScalingDisabled(int i, boolean z);

    public abstract boolean setDisplayedContentSamplingEnabled(int i, boolean z, int i2, int i3);

    public abstract void setMsyncFps(int i, float f, float f2);

    public abstract void setWindowManagerMirroring(int i, boolean z);

    public abstract boolean shouldAlwaysRespectAppRequestedMode();

    public abstract SurfaceControl.ScreenshotHardwareBuffer systemScreenshot(int i);

    public abstract void unregisterDisplayGroupListener(DisplayGroupListener displayGroupListener);

    public abstract void unregisterDisplayTransactionListener(DisplayTransactionListener displayTransactionListener);

    public abstract void updatePowerStateForLucid();

    public abstract SurfaceControl.ScreenshotHardwareBuffer userScreenshot(int i);

    /* loaded from: classes.dex */
    public static final class DisplayPowerRequest {
        public static final int POLICY_BRIGHT = 3;
        public static final int POLICY_CONNECT = 5;
        public static final int POLICY_DIM = 2;
        public static final int POLICY_DOZE = 1;
        public static final int POLICY_OFF = 0;
        public static final int POLICY_VR = 4;
        public boolean blockScreenOn;
        public boolean boostScreenBrightness;
        public float dozeScreenBrightness;
        public int dozeScreenState;
        public boolean lowPowerMode;
        public int policy;
        public float screenAutoBrightnessAdjustmentOverride;
        public float screenBrightnessOverride;
        public float screenLowPowerBrightnessFactor;
        public boolean useAutoBrightness;
        public boolean useProximitySensor;

        public DisplayPowerRequest() {
            this.policy = 3;
            this.useProximitySensor = false;
            this.screenBrightnessOverride = Float.NaN;
            this.useAutoBrightness = false;
            this.screenAutoBrightnessAdjustmentOverride = Float.NaN;
            this.screenLowPowerBrightnessFactor = 0.5f;
            this.blockScreenOn = false;
            this.dozeScreenBrightness = Float.NaN;
            this.dozeScreenState = 0;
        }

        public DisplayPowerRequest(DisplayPowerRequest other) {
            copyFrom(other);
        }

        public boolean isBrightOrDim() {
            int i = this.policy;
            return i == 3 || i == 2;
        }

        public boolean isVr() {
            return this.policy == 4;
        }

        public void copyFrom(DisplayPowerRequest other) {
            this.policy = other.policy;
            this.useProximitySensor = other.useProximitySensor;
            this.screenBrightnessOverride = other.screenBrightnessOverride;
            this.useAutoBrightness = other.useAutoBrightness;
            this.screenAutoBrightnessAdjustmentOverride = other.screenAutoBrightnessAdjustmentOverride;
            this.screenLowPowerBrightnessFactor = other.screenLowPowerBrightnessFactor;
            this.blockScreenOn = other.blockScreenOn;
            this.lowPowerMode = other.lowPowerMode;
            this.boostScreenBrightness = other.boostScreenBrightness;
            this.dozeScreenBrightness = other.dozeScreenBrightness;
            this.dozeScreenState = other.dozeScreenState;
        }

        public boolean equals(Object o) {
            return (o instanceof DisplayPowerRequest) && equals((DisplayPowerRequest) o);
        }

        public boolean equals(DisplayPowerRequest other) {
            return other != null && this.policy == other.policy && this.useProximitySensor == other.useProximitySensor && floatEquals(this.screenBrightnessOverride, other.screenBrightnessOverride) && this.useAutoBrightness == other.useAutoBrightness && floatEquals(this.screenAutoBrightnessAdjustmentOverride, other.screenAutoBrightnessAdjustmentOverride) && this.screenLowPowerBrightnessFactor == other.screenLowPowerBrightnessFactor && this.blockScreenOn == other.blockScreenOn && this.lowPowerMode == other.lowPowerMode && this.boostScreenBrightness == other.boostScreenBrightness && floatEquals(this.dozeScreenBrightness, other.dozeScreenBrightness) && this.dozeScreenState == other.dozeScreenState;
        }

        private boolean floatEquals(float f1, float f2) {
            return f1 == f2 || (Float.isNaN(f1) && Float.isNaN(f2));
        }

        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "policy=" + policyToString(this.policy) + ", useProximitySensor=" + this.useProximitySensor + ", screenBrightnessOverride=" + this.screenBrightnessOverride + ", useAutoBrightness=" + this.useAutoBrightness + ", screenAutoBrightnessAdjustmentOverride=" + this.screenAutoBrightnessAdjustmentOverride + ", screenLowPowerBrightnessFactor=" + this.screenLowPowerBrightnessFactor + ", blockScreenOn=" + this.blockScreenOn + ", lowPowerMode=" + this.lowPowerMode + ", boostScreenBrightness=" + this.boostScreenBrightness + ", dozeScreenBrightness=" + this.dozeScreenBrightness + ", dozeScreenState=" + Display.stateToString(this.dozeScreenState);
        }

        public static String policyToString(int policy) {
            switch (policy) {
                case 0:
                    return "OFF";
                case 1:
                    return "DOZE";
                case 2:
                    return "DIM";
                case 3:
                    return "BRIGHT";
                case 4:
                    return "VR";
                default:
                    return Integer.toString(policy);
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class RefreshRateRange {
        public static final float FLOAT_TOLERANCE = 0.01f;
        public static final String TAG = "RefreshRateRange";
        public float max;
        public float min;

        public RefreshRateRange() {
        }

        public RefreshRateRange(float min, float max) {
            if (min < 0.0f || max < 0.0f || min > 0.01f + max) {
                Slog.e(TAG, "Wrong values for min and max when initializing RefreshRateRange : " + min + " " + max);
                this.max = 0.0f;
                this.min = 0.0f;
                return;
            }
            if (min > max) {
                min = max;
                max = min;
            }
            this.min = min;
            this.max = max;
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other instanceof RefreshRateRange) {
                RefreshRateRange refreshRateRange = (RefreshRateRange) other;
                return this.min == refreshRateRange.min && this.max == refreshRateRange.max;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Float.valueOf(this.min), Float.valueOf(this.max));
        }

        public String toString() {
            return NavigationBarInflaterView.KEY_CODE_START + this.min + " " + this.max + NavigationBarInflaterView.KEY_CODE_END;
        }
    }

    /* loaded from: classes.dex */
    public static final class RefreshRateLimitation {
        public RefreshRateRange range;
        public int type;

        public RefreshRateLimitation(int type, float min, float max) {
            this.type = type;
            this.range = new RefreshRateRange(min, max);
        }

        public String toString() {
            return "RefreshRateLimitation(" + this.type + ": " + this.range + NavigationBarInflaterView.KEY_CODE_END;
        }
    }
}
