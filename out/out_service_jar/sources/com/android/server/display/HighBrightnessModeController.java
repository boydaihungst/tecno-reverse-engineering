package com.android.server.display;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.BrightnessInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.IThermalEventListener;
import android.os.IThermalService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Temperature;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.SurfaceControlHdrLayerInfoListener;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.HighBrightnessModeController;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class HighBrightnessModeController {
    private static final boolean DEBUG = false;
    static final float HBM_TRANSITION_POINT_INVALID = Float.POSITIVE_INFINITY;
    private static final String TAG = "HighBrightnessModeController";
    private float mAmbientLux;
    private float mBrightness;
    private final float mBrightnessMax;
    private final float mBrightnessMin;
    private final DisplayManagerService.Clock mClock;
    private final Context mContext;
    private int mDisplayStatsId;
    private LinkedList<HbmEvent> mEvents;
    private final Handler mHandler;
    private final Runnable mHbmChangeCallback;
    private DisplayDeviceConfig.HighBrightnessModeData mHbmData;
    private int mHbmMode;
    private int mHbmStatsState;
    private HdrBrightnessDeviceConfig mHdrBrightnessCfg;
    private HdrListener mHdrListener;
    private int mHeight;
    private final Injector mInjector;
    private boolean mIsAutoBrightnessEnabled;
    private boolean mIsAutoBrightnessOffByState;
    private boolean mIsBlockedByLowPowerMode;
    private boolean mIsHdrLayerPresent;
    private boolean mIsInAllowedAmbientRange;
    private boolean mIsThermalStatusWithinLimit;
    private boolean mIsTimeAvailable;
    private final Runnable mRecalcRunnable;
    private IBinder mRegisteredDisplayToken;
    private long mRunningStartTimeMillis;
    private final SettingsObserver mSettingsObserver;
    private final SkinThermalStatusObserver mSkinThermalStatusObserver;
    private int mThrottlingReason;
    private float mUnthrottledBrightness;
    private int mWidth;

    /* loaded from: classes.dex */
    public interface HdrBrightnessDeviceConfig {
        float getHdrBrightnessFromSdr(float f);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HighBrightnessModeController(Handler handler, int width, int height, IBinder displayToken, String displayUniqueId, float brightnessMin, float brightnessMax, DisplayDeviceConfig.HighBrightnessModeData hbmData, HdrBrightnessDeviceConfig hdrBrightnessCfg, Runnable hbmChangeCallback, Context context) {
        this(new Injector(), handler, width, height, displayToken, displayUniqueId, brightnessMin, brightnessMax, hbmData, hdrBrightnessCfg, hbmChangeCallback, context);
    }

    HighBrightnessModeController(Injector injector, Handler handler, int width, int height, IBinder displayToken, String displayUniqueId, float brightnessMin, float brightnessMax, DisplayDeviceConfig.HighBrightnessModeData hbmData, HdrBrightnessDeviceConfig hdrBrightnessCfg, Runnable hbmChangeCallback, Context context) {
        this.mIsInAllowedAmbientRange = false;
        this.mIsTimeAvailable = false;
        this.mIsAutoBrightnessEnabled = false;
        this.mIsAutoBrightnessOffByState = false;
        this.mThrottlingReason = 0;
        this.mHbmMode = 0;
        this.mIsHdrLayerPresent = false;
        this.mIsThermalStatusWithinLimit = true;
        this.mIsBlockedByLowPowerMode = false;
        this.mHbmStatsState = 1;
        this.mRunningStartTimeMillis = -1L;
        this.mEvents = new LinkedList<>();
        this.mInjector = injector;
        this.mContext = context;
        this.mClock = injector.getClock();
        this.mHandler = handler;
        this.mBrightness = brightnessMin;
        this.mBrightnessMin = brightnessMin;
        this.mBrightnessMax = brightnessMax;
        this.mHbmChangeCallback = hbmChangeCallback;
        this.mSkinThermalStatusObserver = new SkinThermalStatusObserver(injector, handler);
        this.mSettingsObserver = new SettingsObserver(handler);
        this.mRecalcRunnable = new Runnable() { // from class: com.android.server.display.HighBrightnessModeController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                HighBrightnessModeController.this.recalculateTimeAllowance();
            }
        };
        this.mHdrListener = new HdrListener();
        resetHbmData(width, height, displayToken, displayUniqueId, hbmData, hdrBrightnessCfg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAutoBrightnessEnabled(int state) {
        boolean isEnabled = state == 1;
        this.mIsAutoBrightnessOffByState = state == 3;
        if (!deviceSupportsHbm() || isEnabled == this.mIsAutoBrightnessEnabled) {
            return;
        }
        this.mIsAutoBrightnessEnabled = isEnabled;
        this.mIsInAllowedAmbientRange = false;
        recalculateTimeAllowance();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getCurrentBrightnessMin() {
        return this.mBrightnessMin;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getCurrentBrightnessMax() {
        if (!deviceSupportsHbm() || isCurrentlyAllowed()) {
            return this.mBrightnessMax;
        }
        return this.mHbmData.transitionPoint;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getNormalBrightnessMax() {
        return deviceSupportsHbm() ? this.mHbmData.transitionPoint : this.mBrightnessMax;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getHdrBrightnessValue() {
        HdrBrightnessDeviceConfig hdrBrightnessDeviceConfig = this.mHdrBrightnessCfg;
        if (hdrBrightnessDeviceConfig != null) {
            float hdrBrightness = hdrBrightnessDeviceConfig.getHdrBrightnessFromSdr(this.mBrightness);
            if (hdrBrightness != -1.0f) {
                return hdrBrightness;
            }
        }
        return MathUtils.map(getCurrentBrightnessMin(), getCurrentBrightnessMax(), this.mBrightnessMin, this.mBrightnessMax, this.mBrightness);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAmbientLuxChange(float ambientLux) {
        this.mAmbientLux = ambientLux;
        if (!deviceSupportsHbm() || !this.mIsAutoBrightnessEnabled) {
            return;
        }
        boolean isHighLux = ambientLux >= this.mHbmData.minimumLux;
        if (isHighLux != this.mIsInAllowedAmbientRange) {
            this.mIsInAllowedAmbientRange = isHighLux;
            recalculateTimeAllowance();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBrightnessChanged(float brightness, float unthrottledBrightness, int throttlingReason) {
        if (!deviceSupportsHbm()) {
            return;
        }
        this.mBrightness = brightness;
        this.mUnthrottledBrightness = unthrottledBrightness;
        this.mThrottlingReason = throttlingReason;
        boolean shouldHbmDrainAvailableTime = true;
        boolean wasHbmDrainingAvailableTime = this.mRunningStartTimeMillis != -1;
        if (brightness <= this.mHbmData.transitionPoint || this.mIsHdrLayerPresent) {
            shouldHbmDrainAvailableTime = false;
        }
        if (wasHbmDrainingAvailableTime != shouldHbmDrainAvailableTime) {
            long currentTime = this.mClock.uptimeMillis();
            if (shouldHbmDrainAvailableTime) {
                this.mRunningStartTimeMillis = currentTime;
            } else {
                this.mEvents.addFirst(new HbmEvent(this.mRunningStartTimeMillis, currentTime));
                this.mRunningStartTimeMillis = -1L;
            }
        }
        recalculateTimeAllowance();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getHighBrightnessMode() {
        return this.mHbmMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getTransitionPoint() {
        if (deviceSupportsHbm()) {
            return this.mHbmData.transitionPoint;
        }
        return HBM_TRANSITION_POINT_INVALID;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stop() {
        registerHdrListener(null);
        this.mSkinThermalStatusObserver.stopObserving();
        this.mSettingsObserver.stopObserving();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetHbmData(int width, int height, IBinder displayToken, String displayUniqueId, DisplayDeviceConfig.HighBrightnessModeData hbmData, HdrBrightnessDeviceConfig hdrBrightnessCfg) {
        this.mWidth = width;
        this.mHeight = height;
        this.mHbmData = hbmData;
        this.mHdrBrightnessCfg = hdrBrightnessCfg;
        this.mDisplayStatsId = displayUniqueId.hashCode();
        unregisterHdrListener();
        this.mSkinThermalStatusObserver.stopObserving();
        this.mSettingsObserver.stopObserving();
        if (deviceSupportsHbm()) {
            registerHdrListener(displayToken);
            recalculateTimeAllowance();
            if (this.mHbmData.thermalStatusLimit > 0) {
                this.mIsThermalStatusWithinLimit = true;
                this.mSkinThermalStatusObserver.startObserving();
            }
            if (!this.mHbmData.allowInLowPowerMode) {
                this.mIsBlockedByLowPowerMode = false;
                this.mSettingsObserver.startObserving();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(final PrintWriter pw) {
        this.mHandler.runWithScissors(new Runnable() { // from class: com.android.server.display.HighBrightnessModeController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                HighBrightnessModeController.this.m3454xc23e3656(pw);
            }
        }, 1000L);
    }

    HdrListener getHdrListener() {
        return this.mHdrListener;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: dumpLocal */
    public void m3454xc23e3656(PrintWriter pw) {
        pw.println("HighBrightnessModeController:");
        pw.println("  mBrightness=" + this.mBrightness);
        pw.println("  mUnthrottledBrightness=" + this.mUnthrottledBrightness);
        pw.println("  mThrottlingReason=" + BrightnessInfo.briMaxReasonToString(this.mThrottlingReason));
        pw.println("  mCurrentMin=" + getCurrentBrightnessMin());
        pw.println("  mCurrentMax=" + getCurrentBrightnessMax());
        pw.println("  mHbmMode=" + BrightnessInfo.hbmToString(this.mHbmMode) + (this.mHbmMode == 2 ? "(" + getHdrBrightnessValue() + ")" : ""));
        pw.println("  mHbmStatsState=" + hbmStatsStateToString(this.mHbmStatsState));
        pw.println("  mHbmData=" + this.mHbmData);
        pw.println("  mAmbientLux=" + this.mAmbientLux + (this.mIsAutoBrightnessEnabled ? "" : " (old/invalid)"));
        pw.println("  mIsInAllowedAmbientRange=" + this.mIsInAllowedAmbientRange);
        pw.println("  mIsAutoBrightnessEnabled=" + this.mIsAutoBrightnessEnabled);
        pw.println("  mIsAutoBrightnessOffByState=" + this.mIsAutoBrightnessOffByState);
        pw.println("  mIsHdrLayerPresent=" + this.mIsHdrLayerPresent);
        pw.println("  mBrightnessMin=" + this.mBrightnessMin);
        pw.println("  mBrightnessMax=" + this.mBrightnessMax);
        pw.println("  remainingTime=" + calculateRemainingTime(this.mClock.uptimeMillis()));
        pw.println("  mIsTimeAvailable= " + this.mIsTimeAvailable);
        pw.println("  mRunningStartTimeMillis=" + TimeUtils.formatUptime(this.mRunningStartTimeMillis));
        pw.println("  mIsThermalStatusWithinLimit=" + this.mIsThermalStatusWithinLimit);
        pw.println("  mIsBlockedByLowPowerMode=" + this.mIsBlockedByLowPowerMode);
        pw.println("  width*height=" + this.mWidth + "*" + this.mHeight);
        pw.println("  mEvents=");
        long currentTime = this.mClock.uptimeMillis();
        long lastStartTime = currentTime;
        long j = this.mRunningStartTimeMillis;
        if (j != -1) {
            lastStartTime = dumpHbmEvent(pw, new HbmEvent(j, currentTime));
        }
        Iterator<HbmEvent> it = this.mEvents.iterator();
        while (it.hasNext()) {
            HbmEvent event = it.next();
            if (lastStartTime > event.endTimeMillis) {
                pw.println("    event: [normal brightness]: " + TimeUtils.formatDuration(lastStartTime - event.endTimeMillis));
            }
            lastStartTime = dumpHbmEvent(pw, event);
        }
        this.mSkinThermalStatusObserver.dump(pw);
    }

    private long dumpHbmEvent(PrintWriter pw, HbmEvent event) {
        long duration = event.endTimeMillis - event.startTimeMillis;
        pw.println("    event: [" + TimeUtils.formatUptime(event.startTimeMillis) + ", " + TimeUtils.formatUptime(event.endTimeMillis) + "] (" + TimeUtils.formatDuration(duration) + ")");
        return event.startTimeMillis;
    }

    private boolean isCurrentlyAllowed() {
        return !this.mIsHdrLayerPresent && this.mIsAutoBrightnessEnabled && this.mIsTimeAvailable && this.mIsInAllowedAmbientRange && this.mIsThermalStatusWithinLimit && !this.mIsBlockedByLowPowerMode;
    }

    private boolean deviceSupportsHbm() {
        return this.mHbmData != null;
    }

    private long calculateRemainingTime(long currentTime) {
        if (deviceSupportsHbm()) {
            long timeAlreadyUsed = 0;
            long j = this.mRunningStartTimeMillis;
            if (j > 0) {
                if (j > currentTime) {
                    Slog.e(TAG, "Start time set to the future. curr: " + currentTime + ", start: " + this.mRunningStartTimeMillis);
                    this.mRunningStartTimeMillis = currentTime;
                }
                timeAlreadyUsed = currentTime - this.mRunningStartTimeMillis;
            }
            long windowstartTimeMillis = currentTime - this.mHbmData.timeWindowMillis;
            Iterator<HbmEvent> it = this.mEvents.iterator();
            while (it.hasNext()) {
                HbmEvent event = it.next();
                if (event.endTimeMillis < windowstartTimeMillis) {
                    it.remove();
                } else {
                    long startTimeMillis = Math.max(event.startTimeMillis, windowstartTimeMillis);
                    timeAlreadyUsed += event.endTimeMillis - startTimeMillis;
                }
            }
            return Math.max(0L, this.mHbmData.timeMaxMillis - timeAlreadyUsed);
        }
        return 0L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recalculateTimeAllowance() {
        long currentTime = this.mClock.uptimeMillis();
        long remainingTime = calculateRemainingTime(currentTime);
        boolean z = true;
        boolean isAllowedWithoutRestrictions = remainingTime >= this.mHbmData.timeMinMillis;
        boolean isOnlyAllowedToStayOn = !isAllowedWithoutRestrictions && remainingTime > 0 && this.mBrightness > this.mHbmData.transitionPoint;
        if (!isAllowedWithoutRestrictions && !isOnlyAllowedToStayOn) {
            z = false;
        }
        this.mIsTimeAvailable = z;
        long nextTimeout = -1;
        if (this.mBrightness > this.mHbmData.transitionPoint) {
            nextTimeout = currentTime + remainingTime;
        } else if (!this.mIsTimeAvailable && this.mEvents.size() > 0) {
            long windowstartTimeMillis = currentTime - this.mHbmData.timeWindowMillis;
            HbmEvent lastEvent = this.mEvents.getLast();
            long startTimePlusMinMillis = Math.max(windowstartTimeMillis, lastEvent.startTimeMillis) + this.mHbmData.timeMinMillis;
            long timeWhenMinIsGainedBack = ((startTimePlusMinMillis - windowstartTimeMillis) + currentTime) - remainingTime;
            nextTimeout = timeWhenMinIsGainedBack;
        }
        if (nextTimeout != -1) {
            this.mHandler.removeCallbacks(this.mRecalcRunnable);
            this.mHandler.postAtTime(this.mRecalcRunnable, 1 + nextTimeout);
        }
        updateHbmMode();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateHbmMode() {
        int newHbmMode = calculateHighBrightnessMode();
        updateHbmStats(newHbmMode);
        if (this.mHbmMode != newHbmMode) {
            this.mHbmMode = newHbmMode;
            this.mHbmChangeCallback.run();
        }
    }

    private void updateHbmStats(int newMode) {
        float transitionPoint = this.mHbmData.transitionPoint;
        int state = 1;
        boolean externalThermalThrottling = true;
        if (newMode == 2 && getHdrBrightnessValue() > transitionPoint) {
            state = 2;
        } else if (newMode == 1 && this.mBrightness > transitionPoint) {
            state = 3;
        }
        int i = this.mHbmStatsState;
        if (state == i) {
            return;
        }
        int reason = 0;
        boolean oldHbmSv = i == 3;
        boolean newHbmSv = state == 3;
        if (oldHbmSv && !newHbmSv) {
            boolean internalThermalThrottling = !this.mIsThermalStatusWithinLimit;
            if (this.mUnthrottledBrightness <= transitionPoint || this.mBrightness > transitionPoint || this.mThrottlingReason != 1) {
                externalThermalThrottling = false;
            }
            boolean z = this.mIsAutoBrightnessEnabled;
            if (!z && this.mIsAutoBrightnessOffByState) {
                reason = 6;
            } else if (!z) {
                reason = 7;
            } else if (!this.mIsInAllowedAmbientRange) {
                reason = 1;
            } else if (!this.mIsTimeAvailable) {
                reason = 2;
            } else if (internalThermalThrottling || externalThermalThrottling) {
                reason = 3;
            } else if (this.mIsHdrLayerPresent) {
                reason = 4;
            } else if (this.mIsBlockedByLowPowerMode) {
                reason = 5;
            } else if (this.mBrightness <= this.mHbmData.transitionPoint) {
                reason = 9;
            }
        }
        this.mInjector.reportHbmStateChange(this.mDisplayStatsId, state, reason);
        this.mHbmStatsState = state;
    }

    private String hbmStatsStateToString(int hbmStatsState) {
        switch (hbmStatsState) {
            case 1:
                return "HBM_OFF";
            case 2:
                return "HBM_ON_HDR";
            case 3:
                return "HBM_ON_SUNLIGHT";
            default:
                return String.valueOf(hbmStatsState);
        }
    }

    private int calculateHighBrightnessMode() {
        if (deviceSupportsHbm()) {
            if (this.mIsHdrLayerPresent) {
                return 2;
            }
            return isCurrentlyAllowed() ? 1 : 0;
        }
        return 0;
    }

    private void registerHdrListener(IBinder displayToken) {
        if (this.mRegisteredDisplayToken == displayToken) {
            return;
        }
        unregisterHdrListener();
        this.mRegisteredDisplayToken = displayToken;
        if (displayToken != null) {
            this.mHdrListener.register(displayToken);
        }
    }

    private void unregisterHdrListener() {
        IBinder iBinder = this.mRegisteredDisplayToken;
        if (iBinder != null) {
            this.mHdrListener.unregister(iBinder);
            this.mIsHdrLayerPresent = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class HbmEvent {
        public long endTimeMillis;
        public long startTimeMillis;

        HbmEvent(long startTimeMillis, long endTimeMillis) {
            this.startTimeMillis = startTimeMillis;
            this.endTimeMillis = endTimeMillis;
        }

        public String toString() {
            return "[Event: {" + this.startTimeMillis + ", " + this.endTimeMillis + "}, total: " + ((this.endTimeMillis - this.startTimeMillis) / 1000) + "]";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class HdrListener extends SurfaceControlHdrLayerInfoListener {
        HdrListener() {
        }

        public void onHdrInfoChanged(IBinder displayToken, final int numberOfHdrLayers, final int maxW, final int maxH, int flags) {
            HighBrightnessModeController.this.mHandler.post(new Runnable() { // from class: com.android.server.display.HighBrightnessModeController$HdrListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    HighBrightnessModeController.HdrListener.this.m3455xd1478bb7(numberOfHdrLayers, maxW, maxH);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onHdrInfoChanged$0$com-android-server-display-HighBrightnessModeController$HdrListener  reason: not valid java name */
        public /* synthetic */ void m3455xd1478bb7(int numberOfHdrLayers, int maxW, int maxH) {
            HighBrightnessModeController highBrightnessModeController = HighBrightnessModeController.this;
            highBrightnessModeController.mIsHdrLayerPresent = numberOfHdrLayers > 0 && ((float) (maxW * maxH)) >= ((float) (highBrightnessModeController.mWidth * HighBrightnessModeController.this.mHeight)) * HighBrightnessModeController.this.mHbmData.minimumHdrPercentOfScreen;
            HighBrightnessModeController highBrightnessModeController2 = HighBrightnessModeController.this;
            highBrightnessModeController2.onBrightnessChanged(highBrightnessModeController2.mBrightness, HighBrightnessModeController.this.mUnthrottledBrightness, HighBrightnessModeController.this.mThrottlingReason);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SkinThermalStatusObserver extends IThermalEventListener.Stub {
        private final Handler mHandler;
        private final Injector mInjector;
        private boolean mStarted;
        private IThermalService mThermalService;

        SkinThermalStatusObserver(Injector injector, Handler handler) {
            this.mInjector = injector;
            this.mHandler = handler;
        }

        public void notifyThrottling(final Temperature temp) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.HighBrightnessModeController$SkinThermalStatusObserver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    HighBrightnessModeController.SkinThermalStatusObserver.this.m3456xb8e71410(temp);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyThrottling$0$com-android-server-display-HighBrightnessModeController$SkinThermalStatusObserver  reason: not valid java name */
        public /* synthetic */ void m3456xb8e71410(Temperature temp) {
            HighBrightnessModeController.this.mIsThermalStatusWithinLimit = temp.getStatus() <= HighBrightnessModeController.this.mHbmData.thermalStatusLimit;
            HighBrightnessModeController.this.updateHbmMode();
        }

        void startObserving() {
            if (this.mStarted) {
                return;
            }
            IThermalService thermalService = this.mInjector.getThermalService();
            this.mThermalService = thermalService;
            if (thermalService == null) {
                Slog.w(HighBrightnessModeController.TAG, "Could not observe thermal status. Service not available");
                return;
            }
            try {
                thermalService.registerThermalEventListenerWithType(this, 3);
                this.mStarted = true;
            } catch (RemoteException e) {
                Slog.e(HighBrightnessModeController.TAG, "Failed to register thermal status listener", e);
            }
        }

        void stopObserving() {
            HighBrightnessModeController.this.mIsThermalStatusWithinLimit = true;
            if (!this.mStarted) {
                return;
            }
            try {
                this.mThermalService.unregisterThermalEventListener(this);
                this.mStarted = false;
            } catch (RemoteException e) {
                Slog.e(HighBrightnessModeController.TAG, "Failed to unregister thermal status listener", e);
            }
            this.mThermalService = null;
        }

        void dump(PrintWriter writer) {
            writer.println("  SkinThermalStatusObserver:");
            writer.println("    mStarted: " + this.mStarted);
            if (this.mThermalService != null) {
                writer.println("    ThermalService available");
            } else {
                writer.println("    ThermalService not available");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        private final Uri mLowPowerModeSetting;
        private boolean mStarted;

        SettingsObserver(Handler handler) {
            super(handler);
            this.mLowPowerModeSetting = Settings.Global.getUriFor("low_power");
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            updateLowPower();
        }

        void startObserving() {
            if (!this.mStarted) {
                HighBrightnessModeController.this.mContext.getContentResolver().registerContentObserver(this.mLowPowerModeSetting, false, this, -1);
                this.mStarted = true;
                updateLowPower();
            }
        }

        void stopObserving() {
            HighBrightnessModeController.this.mIsBlockedByLowPowerMode = false;
            if (this.mStarted) {
                HighBrightnessModeController.this.mContext.getContentResolver().unregisterContentObserver(this);
                this.mStarted = false;
            }
        }

        private void updateLowPower() {
            boolean isLowPowerMode = isLowPowerMode();
            if (isLowPowerMode == HighBrightnessModeController.this.mIsBlockedByLowPowerMode) {
                return;
            }
            HighBrightnessModeController.this.mIsBlockedByLowPowerMode = isLowPowerMode;
            HighBrightnessModeController.this.updateHbmMode();
        }

        private boolean isLowPowerMode() {
            return Settings.Global.getInt(HighBrightnessModeController.this.mContext.getContentResolver(), "low_power", 0) != 0;
        }
    }

    /* loaded from: classes.dex */
    public static class Injector {
        public DisplayManagerService.Clock getClock() {
            return new DisplayManagerService.Clock() { // from class: com.android.server.display.HighBrightnessModeController$Injector$$ExternalSyntheticLambda0
                @Override // com.android.server.display.DisplayManagerService.Clock
                public final long uptimeMillis() {
                    return SystemClock.uptimeMillis();
                }
            };
        }

        public IThermalService getThermalService() {
            return IThermalService.Stub.asInterface(ServiceManager.getService("thermalservice"));
        }

        public void reportHbmStateChange(int display, int state, int reason) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.DISPLAY_HBM_STATE_CHANGED, display, state, reason);
        }
    }
}
