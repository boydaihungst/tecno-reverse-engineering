package com.android.server.display;

import android.hardware.display.BrightnessInfo;
import android.os.Handler;
import android.os.IThermalEventListener;
import android.os.IThermalService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Temperature;
import android.util.Slog;
import com.android.server.display.BrightnessThrottler;
import com.android.server.display.DisplayDeviceConfig;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BrightnessThrottler {
    private static final boolean DEBUG = false;
    private static final String TAG = "BrightnessThrottler";
    private static final int THROTTLING_INVALID = -1;
    private float mBrightnessCap;
    private int mBrightnessMaxReason;
    private final Handler mHandler;
    private final Injector mInjector;
    private final SkinThermalStatusObserver mSkinThermalStatusObserver;
    private final Runnable mThrottlingChangeCallback;
    private DisplayDeviceConfig.BrightnessThrottlingData mThrottlingData;
    private int mThrottlingStatus;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BrightnessThrottler(Handler handler, DisplayDeviceConfig.BrightnessThrottlingData throttlingData, Runnable throttlingChangeCallback) {
        this(new Injector(), handler, throttlingData, throttlingChangeCallback);
    }

    BrightnessThrottler(Injector injector, Handler handler, DisplayDeviceConfig.BrightnessThrottlingData throttlingData, Runnable throttlingChangeCallback) {
        this.mBrightnessCap = 1.0f;
        this.mBrightnessMaxReason = 0;
        this.mInjector = injector;
        this.mHandler = handler;
        this.mThrottlingData = throttlingData;
        this.mThrottlingChangeCallback = throttlingChangeCallback;
        this.mSkinThermalStatusObserver = new SkinThermalStatusObserver(injector, handler);
        resetThrottlingData(this.mThrottlingData);
    }

    boolean deviceSupportsThrottling() {
        return this.mThrottlingData != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getBrightnessCap() {
        return this.mBrightnessCap;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getBrightnessMaxReason() {
        return this.mBrightnessMaxReason;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isThrottled() {
        return this.mBrightnessMaxReason != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stop() {
        this.mSkinThermalStatusObserver.stopObserving();
        this.mBrightnessCap = 1.0f;
        this.mBrightnessMaxReason = 0;
        this.mThrottlingStatus = -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetThrottlingData(DisplayDeviceConfig.BrightnessThrottlingData throttlingData) {
        stop();
        this.mThrottlingData = throttlingData;
        if (deviceSupportsThrottling()) {
            this.mSkinThermalStatusObserver.startObserving();
        }
    }

    private float verifyAndConstrainBrightnessCap(float brightness) {
        if (brightness < 0.0f) {
            Slog.e(TAG, "brightness " + brightness + " is lower than the minimum possible brightness 0.0");
            brightness = 0.0f;
        }
        if (brightness > 1.0f) {
            Slog.e(TAG, "brightness " + brightness + " is higher than the maximum possible brightness 1.0");
            return 1.0f;
        }
        return brightness;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void thermalStatusChanged(int newStatus) {
        if (this.mThrottlingStatus != newStatus) {
            this.mThrottlingStatus = newStatus;
            updateThrottling();
        }
    }

    private void updateThrottling() {
        if (!deviceSupportsThrottling()) {
            return;
        }
        float brightnessCap = 1.0f;
        int brightnessMaxReason = 0;
        if (this.mThrottlingStatus != -1) {
            for (DisplayDeviceConfig.BrightnessThrottlingData.ThrottlingLevel level : this.mThrottlingData.throttlingLevels) {
                if (level.thermalStatus > this.mThrottlingStatus) {
                    break;
                }
                brightnessCap = level.brightness;
                brightnessMaxReason = 1;
            }
        }
        if (this.mBrightnessCap != brightnessCap || this.mBrightnessMaxReason != brightnessMaxReason) {
            this.mBrightnessCap = verifyAndConstrainBrightnessCap(brightnessCap);
            this.mBrightnessMaxReason = brightnessMaxReason;
            Runnable runnable = this.mThrottlingChangeCallback;
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(final PrintWriter pw) {
        this.mHandler.runWithScissors(new Runnable() { // from class: com.android.server.display.BrightnessThrottler$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessThrottler.this.m3204lambda$dump$0$comandroidserverdisplayBrightnessThrottler(pw);
            }
        }, 1000L);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: dumpLocal */
    public void m3204lambda$dump$0$comandroidserverdisplayBrightnessThrottler(PrintWriter pw) {
        pw.println("BrightnessThrottler:");
        pw.println("  mThrottlingData=" + this.mThrottlingData);
        pw.println("  mThrottlingStatus=" + this.mThrottlingStatus);
        pw.println("  mBrightnessCap=" + this.mBrightnessCap);
        pw.println("  mBrightnessMaxReason=" + BrightnessInfo.briMaxReasonToString(this.mBrightnessMaxReason));
        this.mSkinThermalStatusObserver.dump(pw);
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
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.BrightnessThrottler$SkinThermalStatusObserver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BrightnessThrottler.SkinThermalStatusObserver.this.m3205xe0c19a35(temp);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$notifyThrottling$0$com-android-server-display-BrightnessThrottler$SkinThermalStatusObserver  reason: not valid java name */
        public /* synthetic */ void m3205xe0c19a35(Temperature temp) {
            int status = temp.getStatus();
            BrightnessThrottler.this.thermalStatusChanged(status);
        }

        void startObserving() {
            if (this.mStarted) {
                return;
            }
            IThermalService thermalService = this.mInjector.getThermalService();
            this.mThermalService = thermalService;
            if (thermalService == null) {
                Slog.e(BrightnessThrottler.TAG, "Could not observe thermal status. Service not available");
                return;
            }
            try {
                thermalService.registerThermalEventListenerWithType(this, 3);
                this.mStarted = true;
            } catch (RemoteException e) {
                Slog.e(BrightnessThrottler.TAG, "Failed to register thermal status listener", e);
            }
        }

        void stopObserving() {
            if (!this.mStarted) {
                return;
            }
            try {
                this.mThermalService.unregisterThermalEventListener(this);
                this.mStarted = false;
            } catch (RemoteException e) {
                Slog.e(BrightnessThrottler.TAG, "Failed to unregister thermal status listener", e);
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

    /* loaded from: classes.dex */
    public static class Injector {
        public IThermalService getThermalService() {
            return IThermalService.Stub.asInterface(ServiceManager.getService("thermalservice"));
        }
    }
}
