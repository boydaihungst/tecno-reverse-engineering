package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.FloatProperty;
import android.util.Slog;
import android.view.Choreographer;
import android.view.Display;
import com.transsion.hubcore.server.display.ITranDisplayPowerState;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DisplayPowerState {
    private static final String TAG = "DisplayPowerState";
    private final DisplayBlanker mBlanker;
    private Runnable mCleanListener;
    private final ColorFade mColorFade;
    private boolean mColorFadeDrawPending;
    private float mColorFadeLevel;
    private boolean mColorFadePrepared;
    private boolean mColorFadeReady;
    private final int mDisplayId;
    private final PhotonicModulator mPhotonicModulator;
    private float mScreenBrightness;
    private boolean mScreenReady;
    private int mScreenState;
    private boolean mScreenUpdatePending;
    private float mSdrScreenBrightness;
    private volatile boolean mStopped;
    private static boolean DEBUG = SystemProperties.getBoolean("dbg.dms.dps", false);
    private static final boolean AOD_NODOZE_SUPPORT = "1".equals(SystemProperties.get("ro.aod_nodoze_support", ""));
    private static String COUNTER_COLOR_FADE = "ColorFadeLevel";
    public static final FloatProperty<DisplayPowerState> COLOR_FADE_LEVEL = new FloatProperty<DisplayPowerState>("electronBeamLevel") { // from class: com.android.server.display.DisplayPowerState.1
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.util.FloatProperty
        public void setValue(DisplayPowerState object, float value) {
            object.setColorFadeLevel(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.util.Property
        public Float get(DisplayPowerState object) {
            return Float.valueOf(object.getColorFadeLevel());
        }
    };
    public static final FloatProperty<DisplayPowerState> SCREEN_BRIGHTNESS_FLOAT = new FloatProperty<DisplayPowerState>("screenBrightnessFloat") { // from class: com.android.server.display.DisplayPowerState.2
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.util.FloatProperty
        public void setValue(DisplayPowerState object, float value) {
            object.setScreenBrightness(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.util.Property
        public Float get(DisplayPowerState object) {
            return Float.valueOf(object.getScreenBrightness());
        }
    };
    public static final FloatProperty<DisplayPowerState> SCREEN_SDR_BRIGHTNESS_FLOAT = new FloatProperty<DisplayPowerState>("sdrScreenBrightnessFloat") { // from class: com.android.server.display.DisplayPowerState.3
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.util.FloatProperty
        public void setValue(DisplayPowerState object, float value) {
            object.setSdrScreenBrightness(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.util.Property
        public Float get(DisplayPowerState object) {
            return Float.valueOf(object.getSdrScreenBrightness());
        }
    };
    private boolean mPreResumePending = false;
    private final Runnable mScreenUpdateRunnable = new Runnable() { // from class: com.android.server.display.DisplayPowerState.4
        @Override // java.lang.Runnable
        public void run() {
            DisplayPowerState.this.mScreenUpdatePending = false;
            if (DisplayPowerState.this.mPreResumePending && DisplayPowerState.this.mScreenState == 1) {
                if (DisplayPowerState.DEBUG) {
                    Slog.d(DisplayPowerState.TAG, "Screen ready");
                }
                DisplayPowerState.this.mScreenReady = true;
                DisplayPowerState.this.invokeCleanListenerIfNeeded();
                return;
            }
            DisplayPowerState.this.mPreResumePending = false;
            float f = -1.0f;
            float brightnessState = ITranDisplayPowerState.Instance().getConnectPolicyBrightness((DisplayPowerState.this.mScreenState == 1 || DisplayPowerState.this.mColorFadeLevel <= 0.0f) ? -1.0f : DisplayPowerState.this.mScreenBrightness);
            if (DisplayPowerState.this.mScreenState != 1 && DisplayPowerState.this.mColorFadeLevel > 0.0f) {
                f = DisplayPowerState.this.mSdrScreenBrightness;
            }
            float sdrBrightnessState = f;
            int tempScreenState = DisplayPowerState.this.mScreenState;
            if (DisplayPowerState.AOD_NODOZE_SUPPORT && tempScreenState == 3) {
                tempScreenState = 2;
                Slog.d(DisplayPowerState.TAG, "mScreenUpdateRunnable mScreenState = " + DisplayPowerState.this.mScreenState + ", brightnessState = " + brightnessState + ", sdrBrightnessState = " + sdrBrightnessState);
            }
            if (DisplayPowerState.this.mPhotonicModulator.setState(tempScreenState, brightnessState, sdrBrightnessState)) {
                if (DisplayPowerState.DEBUG) {
                    Slog.d(DisplayPowerState.TAG, "Screen ready");
                }
                DisplayPowerState.this.mScreenReady = true;
                DisplayPowerState.this.invokeCleanListenerIfNeeded();
            } else if (DisplayPowerState.DEBUG) {
                Slog.d(DisplayPowerState.TAG, "Screen not ready");
            }
        }
    };
    private final Runnable mPreWakeupRunnable = new Runnable() { // from class: com.android.server.display.DisplayPowerState.5
        @Override // java.lang.Runnable
        public void run() {
            if (DisplayPowerState.this.mScreenState == 1) {
                Slog.i(DisplayPowerState.TAG, "   mPreWakeupRunnable handle");
                DisplayPowerState.this.mPreResumePending = true;
                DisplayPowerState.this.mPhotonicModulator.setState(2, -1.0f, -1.0f);
            }
        }
    };
    private final Runnable mPreWakeupFinishRunnable = new Runnable() { // from class: com.android.server.display.DisplayPowerState.6
        @Override // java.lang.Runnable
        public void run() {
            if (DisplayPowerState.this.mScreenState == 1) {
                Slog.i(DisplayPowerState.TAG, "   mPreWakeupFinishRunnable handle");
                DisplayPowerState.this.mPreResumePending = false;
                DisplayPowerState.this.mPhotonicModulator.setState(1, -1.0f, -1.0f);
            }
        }
    };
    private final Runnable mColorFadeDrawRunnable = new Runnable() { // from class: com.android.server.display.DisplayPowerState.7
        @Override // java.lang.Runnable
        public void run() {
            DisplayPowerState.this.mColorFadeDrawPending = false;
            if (DisplayPowerState.this.mColorFadePrepared) {
                DisplayPowerState.this.mColorFade.draw(DisplayPowerState.this.mColorFadeLevel);
                Trace.traceCounter(131072L, DisplayPowerState.COUNTER_COLOR_FADE, Math.round(DisplayPowerState.this.mColorFadeLevel * 100.0f));
            }
            DisplayPowerState.this.mColorFadeReady = true;
            DisplayPowerState.this.invokeCleanListenerIfNeeded();
        }
    };
    private final Handler mHandler = new Handler(true);
    private final Choreographer mChoreographer = Choreographer.getInstance();

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayPowerState(DisplayBlanker blanker, ColorFade colorFade, int displayId, int displayState) {
        float f;
        this.mBlanker = blanker;
        this.mColorFade = colorFade;
        PhotonicModulator photonicModulator = new PhotonicModulator();
        this.mPhotonicModulator = photonicModulator;
        photonicModulator.start();
        this.mDisplayId = displayId;
        this.mScreenState = displayState;
        if (displayState != 1) {
            f = 1.0f;
        } else {
            f = -1.0f;
        }
        this.mScreenBrightness = f;
        this.mSdrScreenBrightness = f;
        scheduleScreenUpdate();
        this.mColorFadePrepared = false;
        this.mColorFadeLevel = 1.0f;
        this.mColorFadeReady = true;
    }

    public void setScreenState(int state) {
        if (this.mScreenState != state) {
            if (DEBUG) {
                Slog.d(TAG, "setScreenState: state=" + state);
            }
            this.mScreenState = state;
            this.mScreenReady = false;
            scheduleScreenUpdate();
        }
    }

    public void startPreWakeup() {
        this.mHandler.removeCallbacks(this.mPreWakeupRunnable);
        this.mHandler.removeCallbacks(this.mPreWakeupFinishRunnable);
        this.mHandler.post(this.mPreWakeupRunnable);
        this.mHandler.postDelayed(this.mPreWakeupFinishRunnable, 1200L);
    }

    public void stopPreWakeup() {
        this.mHandler.removeCallbacks(this.mPreWakeupFinishRunnable);
        this.mHandler.post(this.mPreWakeupFinishRunnable);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPreResumePending() {
        return this.mPreResumePending;
    }

    public int getScreenState() {
        return this.mScreenState;
    }

    public void setSdrScreenBrightness(float brightness) {
        if (this.mSdrScreenBrightness != brightness) {
            if (DEBUG) {
                Slog.d(TAG, "setSdrScreenBrightness: brightness=" + brightness);
            }
            this.mSdrScreenBrightness = brightness;
            if (this.mScreenState != 1) {
                this.mScreenReady = false;
                scheduleScreenUpdate();
            }
        }
    }

    public float getSdrScreenBrightness() {
        return this.mSdrScreenBrightness;
    }

    public void setScreenBrightness(float brightness) {
        if (this.mScreenBrightness != brightness) {
            if (DEBUG) {
                Slog.d(TAG, "setScreenBrightness: brightness=" + brightness);
            }
            this.mScreenBrightness = brightness;
            if (this.mScreenState != 1) {
                this.mScreenReady = false;
                scheduleScreenUpdate();
            }
        }
    }

    public float getScreenBrightness() {
        return this.mScreenBrightness;
    }

    public boolean prepareColorFade(Context context, int mode) {
        ColorFade colorFade = this.mColorFade;
        if (colorFade == null || !colorFade.prepare(context, mode)) {
            this.mColorFadePrepared = false;
            this.mColorFadeReady = true;
            return false;
        }
        this.mColorFadePrepared = true;
        this.mColorFadeReady = false;
        scheduleColorFadeDraw();
        return true;
    }

    public void dismissColorFade() {
        Trace.traceCounter(131072L, COUNTER_COLOR_FADE, 100);
        ColorFade colorFade = this.mColorFade;
        if (colorFade != null) {
            colorFade.dismiss();
        }
        this.mColorFadePrepared = false;
        this.mColorFadeReady = true;
    }

    public void dismissColorFadeResources() {
        ColorFade colorFade = this.mColorFade;
        if (colorFade != null) {
            colorFade.dismissResources();
            TranFoldDisplayCustody.instance().dismissColorFadeResources(this.mDisplayId, this);
        }
    }

    public void setColorFadeLevel(float level) {
        if (this.mColorFadeLevel != level) {
            if (DEBUG) {
                Slog.d(TAG, "setColorFadeLevel: level=" + level);
            }
            this.mColorFadeLevel = level;
            if (this.mScreenState != 1) {
                this.mScreenReady = false;
                scheduleScreenUpdate();
            }
            if (this.mColorFadePrepared) {
                this.mColorFadeReady = false;
                scheduleColorFadeDraw();
            }
        }
    }

    public float getColorFadeLevel() {
        return this.mColorFadeLevel;
    }

    public boolean waitUntilClean(Runnable listener) {
        if (!this.mScreenReady || !this.mColorFadeReady) {
            this.mCleanListener = listener;
            return false;
        }
        this.mCleanListener = null;
        return true;
    }

    public void stop() {
        this.mStopped = true;
        this.mPhotonicModulator.interrupt();
        dismissColorFade();
        this.mCleanListener = null;
        this.mHandler.removeCallbacksAndMessages(null);
        ColorFade colorFade = this.mColorFade;
        if (colorFade != null) {
            colorFade.destroy();
        }
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Display Power State:");
        pw.println("  mStopped=" + this.mStopped);
        pw.println("  mScreenState=" + Display.stateToString(this.mScreenState));
        pw.println("  mScreenBrightness=" + this.mScreenBrightness);
        pw.println("  mSdrScreenBrightness=" + this.mSdrScreenBrightness);
        pw.println("  mScreenReady=" + this.mScreenReady);
        pw.println("  mScreenUpdatePending=" + this.mScreenUpdatePending);
        pw.println("  mColorFadePrepared=" + this.mColorFadePrepared);
        pw.println("  mColorFadeLevel=" + this.mColorFadeLevel);
        pw.println("  mColorFadeReady=" + this.mColorFadeReady);
        pw.println("  mColorFadeDrawPending=" + this.mColorFadeDrawPending);
        this.mPhotonicModulator.dump(pw);
        ColorFade colorFade = this.mColorFade;
        if (colorFade != null) {
            colorFade.dump(pw);
        }
    }

    private void scheduleScreenUpdate() {
        if (!this.mScreenUpdatePending) {
            this.mScreenUpdatePending = true;
            postScreenUpdateThreadSafe();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postScreenUpdateThreadSafe() {
        this.mHandler.removeCallbacks(this.mScreenUpdateRunnable);
        this.mHandler.post(this.mScreenUpdateRunnable);
    }

    private void scheduleColorFadeDraw() {
        if (!this.mColorFadeDrawPending) {
            this.mColorFadeDrawPending = true;
            this.mChoreographer.postCallback(3, this.mColorFadeDrawRunnable, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invokeCleanListenerIfNeeded() {
        Runnable listener = this.mCleanListener;
        if (listener != null && this.mScreenReady && this.mColorFadeReady) {
            this.mCleanListener = null;
            listener.run();
        }
    }

    /* loaded from: classes.dex */
    private final class PhotonicModulator extends Thread {
        private static final float INITIAL_BACKLIGHT_FLOAT = Float.NaN;
        private static final int INITIAL_SCREEN_STATE = 0;
        private float mActualBacklight;
        private float mActualSdrBacklight;
        private int mActualState;
        private boolean mBacklightChangeInProgress;
        private final Object mLock;
        private float mPendingBacklight;
        private float mPendingSdrBacklight;
        private int mPendingState;
        private boolean mStateChangeInProgress;

        public PhotonicModulator() {
            super("PhotonicModulator");
            this.mLock = new Object();
            this.mPendingState = 0;
            this.mPendingBacklight = Float.NaN;
            this.mPendingSdrBacklight = Float.NaN;
            this.mActualState = 0;
            this.mActualBacklight = Float.NaN;
            this.mActualSdrBacklight = Float.NaN;
        }

        /* JADX WARN: Removed duplicated region for block: B:19:0x0026 A[Catch: all -> 0x0083, TryCatch #0 {, blocks: (B:4:0x0003, B:8:0x000c, B:10:0x0012, B:42:0x007b, B:46:0x0081, B:17:0x0020, B:19:0x0026, B:20:0x004c, B:22:0x0056, B:32:0x0066, B:34:0x006a, B:39:0x0072, B:41:0x0076), top: B:51:0x0003 }] */
        /* JADX WARN: Removed duplicated region for block: B:28:0x0060 A[ADDED_TO_REGION] */
        /* JADX WARN: Removed duplicated region for block: B:34:0x006a A[Catch: all -> 0x0083, TryCatch #0 {, blocks: (B:4:0x0003, B:8:0x000c, B:10:0x0012, B:42:0x007b, B:46:0x0081, B:17:0x0020, B:19:0x0026, B:20:0x004c, B:22:0x0056, B:32:0x0066, B:34:0x006a, B:39:0x0072, B:41:0x0076), top: B:51:0x0003 }] */
        /* JADX WARN: Removed duplicated region for block: B:41:0x0076 A[Catch: all -> 0x0083, TryCatch #0 {, blocks: (B:4:0x0003, B:8:0x000c, B:10:0x0012, B:42:0x007b, B:46:0x0081, B:17:0x0020, B:19:0x0026, B:20:0x004c, B:22:0x0056, B:32:0x0066, B:34:0x006a, B:39:0x0072, B:41:0x0076), top: B:51:0x0003 }] */
        /* JADX WARN: Removed duplicated region for block: B:44:0x007f  */
        /* JADX WARN: Removed duplicated region for block: B:45:0x0080  */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public boolean setState(int state, float brightnessState, float sdrBrightnessState) {
            boolean z;
            boolean backlightChanged;
            boolean z2;
            boolean changeInProgress;
            boolean z3;
            boolean z4;
            synchronized (this.mLock) {
                z = true;
                boolean stateChanged = state != this.mPendingState;
                if (brightnessState == this.mPendingBacklight && sdrBrightnessState == this.mPendingSdrBacklight) {
                    backlightChanged = false;
                    if (!stateChanged || backlightChanged) {
                        if (DisplayPowerState.DEBUG) {
                            Slog.d(DisplayPowerState.TAG, "Requesting new screen state: state=" + Display.stateToString(state) + ", backlight=" + brightnessState);
                        }
                        this.mPendingState = state;
                        this.mPendingBacklight = brightnessState;
                        this.mPendingSdrBacklight = sdrBrightnessState;
                        z2 = this.mStateChangeInProgress;
                        if (!z2 && !this.mBacklightChangeInProgress) {
                            changeInProgress = false;
                            if (!stateChanged && !z2) {
                                z3 = false;
                                this.mStateChangeInProgress = z3;
                                if (!backlightChanged && !this.mBacklightChangeInProgress) {
                                    z4 = false;
                                    this.mBacklightChangeInProgress = z4;
                                    if (!changeInProgress) {
                                        this.mLock.notifyAll();
                                    }
                                }
                                z4 = true;
                                this.mBacklightChangeInProgress = z4;
                                if (!changeInProgress) {
                                }
                            }
                            z3 = true;
                            this.mStateChangeInProgress = z3;
                            if (!backlightChanged) {
                                z4 = false;
                                this.mBacklightChangeInProgress = z4;
                                if (!changeInProgress) {
                                }
                            }
                            z4 = true;
                            this.mBacklightChangeInProgress = z4;
                            if (!changeInProgress) {
                            }
                        }
                        changeInProgress = true;
                        if (!stateChanged) {
                            z3 = false;
                            this.mStateChangeInProgress = z3;
                            if (!backlightChanged) {
                            }
                            z4 = true;
                            this.mBacklightChangeInProgress = z4;
                            if (!changeInProgress) {
                            }
                        }
                        z3 = true;
                        this.mStateChangeInProgress = z3;
                        if (!backlightChanged) {
                        }
                        z4 = true;
                        this.mBacklightChangeInProgress = z4;
                        if (!changeInProgress) {
                        }
                    }
                    if (this.mStateChangeInProgress) {
                        z = false;
                    }
                }
                backlightChanged = true;
                if (!stateChanged) {
                }
                if (DisplayPowerState.DEBUG) {
                }
                this.mPendingState = state;
                this.mPendingBacklight = brightnessState;
                this.mPendingSdrBacklight = sdrBrightnessState;
                z2 = this.mStateChangeInProgress;
                if (!z2) {
                    changeInProgress = false;
                    if (!stateChanged) {
                    }
                    z3 = true;
                    this.mStateChangeInProgress = z3;
                    if (!backlightChanged) {
                    }
                    z4 = true;
                    this.mBacklightChangeInProgress = z4;
                    if (!changeInProgress) {
                    }
                    if (this.mStateChangeInProgress) {
                    }
                }
                changeInProgress = true;
                if (!stateChanged) {
                }
                z3 = true;
                this.mStateChangeInProgress = z3;
                if (!backlightChanged) {
                }
                z4 = true;
                this.mBacklightChangeInProgress = z4;
                if (!changeInProgress) {
                }
                if (this.mStateChangeInProgress) {
                }
            }
            return z;
        }

        public void dump(PrintWriter pw) {
            synchronized (this.mLock) {
                pw.println();
                pw.println("Photonic Modulator State:");
                pw.println("  mPendingState=" + Display.stateToString(this.mPendingState));
                pw.println("  mPendingBacklight=" + this.mPendingBacklight);
                pw.println("  mPendingSdrBacklight=" + this.mPendingSdrBacklight);
                pw.println("  mActualState=" + Display.stateToString(this.mActualState));
                pw.println("  mActualBacklight=" + this.mActualBacklight);
                pw.println("  mActualSdrBacklight=" + this.mActualSdrBacklight);
                pw.println("  mStateChangeInProgress=" + this.mStateChangeInProgress);
                pw.println("  mBacklightChangeInProgress=" + this.mBacklightChangeInProgress);
            }
        }

        /* JADX WARN: Can't wrap try/catch for region: R(16:3|4|(1:6)(1:54)|7|(10:12|(1:14)|(1:16)|(1:52)(1:20)|(1:24)|38|39|40|41|42)|53|(0)|(0)|(1:18)|52|(1:24)|38|39|40|41|42) */
        /* JADX WARN: Code restructure failed: missing block: B:29:0x0042, code lost:
            if (r3 != false) goto L29;
         */
        /* JADX WARN: Code restructure failed: missing block: B:31:0x0045, code lost:
            r10.mActualState = r1;
            r10.mActualBacklight = r5;
            r10.mActualSdrBacklight = r6;
         */
        /* JADX WARN: Code restructure failed: missing block: B:33:0x004c, code lost:
            com.android.server.display.ScreenStateController.getInstance().hookScreenUpdate(r10.this$0.mDisplayId, r1);
         */
        /* JADX WARN: Code restructure failed: missing block: B:34:0x005d, code lost:
            if (com.android.server.display.DisplayPowerState.DEBUG == false) goto L35;
         */
        /* JADX WARN: Code restructure failed: missing block: B:35:0x005f, code lost:
            android.util.Slog.d(com.android.server.display.DisplayPowerState.TAG, "Updating screen state: id=" + r10.this$0.mDisplayId + ", state=" + android.view.Display.stateToString(r1) + ", backlight=" + r5 + ", sdrBacklight=" + r6);
         */
        /* JADX WARN: Code restructure failed: missing block: B:36:0x009f, code lost:
            r10.this$0.mBlanker.requestDisplayState(r10.this$0.mDisplayId, r1, r5, r6);
         */
        /* JADX WARN: Code restructure failed: missing block: B:41:0x00bd, code lost:
            if (r10.this$0.mStopped != false) goto L47;
         */
        /* JADX WARN: Code restructure failed: missing block: B:43:0x00c0, code lost:
            return;
         */
        /* JADX WARN: Removed duplicated region for block: B:16:0x0024 A[Catch: all -> 0x00c4, TryCatch #1 {, blocks: (B:4:0x0003, B:8:0x000e, B:10:0x0018, B:16:0x0024, B:18:0x002d, B:20:0x0031, B:31:0x0045, B:32:0x004b, B:37:0x00b0, B:40:0x00b7, B:42:0x00bf, B:44:0x00c1), top: B:51:0x0003, inners: #0 }] */
        /* JADX WARN: Removed duplicated region for block: B:18:0x002d A[Catch: all -> 0x00c4, TryCatch #1 {, blocks: (B:4:0x0003, B:8:0x000e, B:10:0x0018, B:16:0x0024, B:18:0x002d, B:20:0x0031, B:31:0x0045, B:32:0x004b, B:37:0x00b0, B:40:0x00b7, B:42:0x00bf, B:44:0x00c1), top: B:51:0x0003, inners: #0 }] */
        @Override // java.lang.Thread, java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void run() {
            boolean backlightChanged;
            while (true) {
                synchronized (this.mLock) {
                    int state = this.mPendingState;
                    boolean changed = true;
                    boolean stateChanged = state != this.mActualState;
                    float brightnessState = this.mPendingBacklight;
                    float sdrBrightnessState = this.mPendingSdrBacklight;
                    if (brightnessState == this.mActualBacklight && sdrBrightnessState == this.mActualSdrBacklight) {
                        backlightChanged = false;
                        if (!stateChanged) {
                            DisplayPowerState.this.postScreenUpdateThreadSafe();
                            this.mStateChangeInProgress = false;
                        }
                        if (!backlightChanged) {
                            this.mBacklightChangeInProgress = false;
                        }
                        boolean valid = state == 0 && !Float.isNaN(brightnessState);
                        if (!stateChanged && !backlightChanged) {
                            changed = false;
                        }
                        this.mLock.wait();
                    }
                    backlightChanged = true;
                    if (!stateChanged) {
                    }
                    if (!backlightChanged) {
                    }
                    if (state == 0) {
                    }
                    if (!stateChanged) {
                        changed = false;
                    }
                    this.mLock.wait();
                }
            }
        }
    }
}
