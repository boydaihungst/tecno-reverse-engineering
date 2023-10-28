package com.mediatek.boostfwk.policy.frame;

import android.content.Context;
import android.view.Choreographer;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.info.ScrollState;
import com.mediatek.boostfwk.scenario.frame.FrameScenario;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import java.lang.ref.WeakReference;
/* loaded from: classes.dex */
public class ScrollingFramePrefetcher {
    private static final double ANIM_THRESHOLD = 1.0d;
    private static final float DISPLAY_RATE_90 = 90.0f;
    public static final long ESTIMATION_FRAME_ID = -1;
    public static final int ESTIMATION_FRAME_LIMIT = 5;
    private static final double FRAME_THRESHOLD = 1.4d;
    private static final String TAG = "ScrollingFramePrefetcher";
    private ActivityInfo.ActivityChangeListener mActivityChangeListener;
    private ScrollState.RefreshRateChangedListener mRefreshRateChangedListener;
    public static final boolean FEATURE_ENABLE = Config.isEnableFramePrefetcher();
    public static final boolean PRE_ANIM_ENABLE = Config.isEnablePreAnimation();
    public static boolean mFeatureLocked = false;
    private static final Object lock = new Object();
    private static ScrollingFramePrefetcher sInstance = null;
    private WeakReference<Choreographer> mChoreographer = null;
    private long mFirstFlingFrameTimeNano = -1;
    private long mLastFrameTimeNano = -1;
    private long mLastOrigFrameTimeNano = -1;
    private long mLastAnimFrameTimeNano = -1;
    private long mFrameEndTimeNano = -1;
    private long mPreAnimEndTimeNano = -1;
    private boolean mIsEstimationFrame = false;
    private boolean mIsFling = false;
    private boolean mIsPreAnim = false;
    private boolean mIsAnimDrop = false;
    private boolean mAppRequestVsync = false;
    private boolean mIsPreAnimationRunning = false;
    private boolean mNeedScheduleNextFrame = false;
    private int mEstimationFrameCount = 0;
    private long mFrameIntervalNanos = -1;
    private final int FIRST_FRAME = 0;
    private final int DROP_FRAME = 1;
    private final int RESET_FRAME = 2;

    public static ScrollingFramePrefetcher getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new ScrollingFramePrefetcher();
                }
            }
        }
        return sInstance;
    }

    private ScrollingFramePrefetcher() {
        this.mRefreshRateChangedListener = null;
        this.mActivityChangeListener = null;
        this.mRefreshRateChangedListener = new ScrollState.RefreshRateChangedListener() { // from class: com.mediatek.boostfwk.policy.frame.ScrollingFramePrefetcher.1
            @Override // com.mediatek.boostfwk.info.ScrollState.RefreshRateChangedListener
            public void onDisplayRefreshRateChanged(int displayId, float refreshRate, float frameIntervalNanos) {
                ScrollingFramePrefetcher.this.onRefreshRateChanged(refreshRate, frameIntervalNanos);
            }
        };
        this.mActivityChangeListener = new ActivityInfo.ActivityChangeListener() { // from class: com.mediatek.boostfwk.policy.frame.ScrollingFramePrefetcher.2
            @Override // com.mediatek.boostfwk.info.ActivityInfo.ActivityChangeListener
            public void onChange(Context c) {
                ScrollingFramePrefetcher.this.resetStatusWhenScroll();
            }
        };
        ActivityInfo.getInstance().registerActivityListener(this.mActivityChangeListener);
        ScrollState.registerRefreshRateChangedListener(this.mRefreshRateChangedListener);
    }

    public void doScrollingFramePrefetcher(FrameScenario scenario) {
        estimationFrameTime(scenario.getOrigFrameTime(), scenario.getFrameData(), scenario);
    }

    private boolean disableSFP() {
        return !FEATURE_ENABLE || !this.mIsFling || ScrollState.getRefreshRate() < DISPLAY_RATE_90 || mFeatureLocked;
    }

    private void estimationFrameTime(long origFrameTimeNano, Choreographer.FrameData frameData, FrameScenario scenario) {
        long result = frameData.getFrameTimeNanos();
        Choreographer choreographer = scenario.getChoreographer();
        if (disableSFP() || choreographer == null) {
            this.mIsPreAnim = false;
            scenario.setFrameTimeResult(result);
            return;
        }
        this.mChoreographer = new WeakReference<>(choreographer);
        updateFrameIntervalNanos();
        long result2 = estimationFrameTimeInternel(origFrameTimeNano, frameData);
        scenario.setFrameTimeResult(result2);
    }

    private long estimationFrameTimeInternel(long origFrameTimeNano, Choreographer.FrameData frameData) {
        long result;
        long frameTimeNano = frameData.getFrameTimeNanos();
        if (this.mFirstFlingFrameTimeNano == -1) {
            updateDynaFrameStatus(0, frameTimeNano);
            result = frameTimeNano;
        } else {
            LogUtil.trace("ScrollingFramePrefetcher#estimationFrameTimeInternel-before, orig frame time = " + origFrameTimeNano + ", last orig frame time = " + this.mLastOrigFrameTimeNano + ", last frame time = " + this.mLastFrameTimeNano);
            if (isPreAnimation()) {
                this.mLastFrameTimeNano = this.mLastAnimFrameTimeNano;
            } else {
                this.mLastFrameTimeNano = correctionFrameTime(frameTimeNano, this.mLastFrameTimeNano);
            }
            result = this.mLastFrameTimeNano;
        }
        this.mLastOrigFrameTimeNano = origFrameTimeNano;
        LogUtil.trace("ScrollingFramePrefetcher#estimationFrameTimeInternel, orig frame time = " + (origFrameTimeNano / 1000000) + ", frame time = " + (frameTimeNano / 1000000) + ", last frame time = " + (this.mLastFrameTimeNano / 1000000) + ", last anim frame time = " + (this.mLastAnimFrameTimeNano / 1000000) + ", result = " + (result / 1000000));
        return result;
    }

    private long predictPreAnimTime(long frameTimeNano, long frameIntervalNanos) {
        long result = frameTimeNano + frameIntervalNanos;
        LogUtil.trace("ScrollingFramePrefetcher#predictPreAnimTime, orig frame Time = " + (frameTimeNano / 1000000) + ", frameIntervalNanos = " + (frameIntervalNanos / 1000000) + ", next frame time = " + (result / 1000000));
        return result;
    }

    public void onFrameEnd(boolean isFrameBegin, FrameScenario scenario) {
        String dropReason;
        if (disableSFP() || isFrameBegin || this.mChoreographer == null) {
            LogUtil.trace("ScrollingFramePrefetcher#onFrameEnd, isFrameBegin = " + isFrameBegin + ", mIsFling = " + this.mIsFling + ", mChoreographer = " + this.mChoreographer);
        } else if (ScrollState.getRefreshRate() < DISPLAY_RATE_90) {
            if (this.mNeedScheduleNextFrame) {
                this.mNeedScheduleNextFrame = false;
                forceScheduleNexFrame();
            }
        } else {
            long nowTimeNano = System.nanoTime();
            boolean isDropFrame = isDropFrame(this.mLastFrameTimeNano, nowTimeNano, FRAME_THRESHOLD);
            if (!isDropFrame) {
                dropReason = "";
            } else {
                dropReason = "drop frame because this frame too long, will insert new frame when this frame end.";
            }
            if (!isDropFrame) {
                long j = this.mFrameEndTimeNano;
                if (j != -1 && (isDropFrame = isDropFrame(j, nowTimeNano, FRAME_THRESHOLD))) {
                    dropReason = "drop frame because time too long between last frame end with this,will insert new frame when this frame end.";
                }
            }
            if (isDropFrame && !this.mIsAnimDrop) {
                long tmpLastTime = this.mLastFrameTimeNano;
                updateDynaFrameStatus(1, tmpLastTime);
            }
            LogUtil.trace("ScrollingFramePrefetcher#onFrameEnd, is drop frame = " + isDropFrame + ", mIsEstimationFrame = " + this.mIsEstimationFrame + ", drop reason = " + dropReason);
            this.mFrameEndTimeNano = nowTimeNano;
            long frameId = scenario.getFrameId();
            if (frameId == -1) {
                this.mEstimationFrameCount++;
            } else {
                this.mEstimationFrameCount = 0;
            }
            if (this.mIsEstimationFrame) {
                this.mIsEstimationFrame = false;
                LogUtil.trace("ScrollingFramePrefetcher#onFrameEnd, estimation frame time = " + (this.mLastFrameTimeNano / 1000000));
                if (this.mEstimationFrameCount < 5 && !this.mIsAnimDrop) {
                    this.mIsPreAnim = false;
                    this.mAppRequestVsync = false;
                    doEstimationFrameHook(this.mLastFrameTimeNano);
                    doPreAnimation(this.mLastFrameTimeNano);
                    this.mIsPreAnim = true;
                    return;
                }
            }
            if (this.mIsPreAnim && !this.mIsAnimDrop) {
                doPreAnimation(this.mLastAnimFrameTimeNano);
            }
        }
    }

    private void doPreAnimation(long frameTimeNano) {
        if (PRE_ANIM_ENABLE && this.mChoreographer != null) {
            long newAnimFrameTime = predictPreAnimTime(frameTimeNano, this.mFrameIntervalNanos);
            LogUtil.trace("ScrollingFramePrefetcher#doPreAnimation, pre anim frame time = " + (newAnimFrameTime / 1000000) + ", mLastAnimFrameTimeNano = " + (this.mLastAnimFrameTimeNano / 1000000));
            this.mIsPreAnimationRunning = true;
            long startTimeNano = System.nanoTime();
            doPreAnimationHook(newAnimFrameTime);
            long endTimeNano = System.nanoTime();
            this.mIsPreAnimationRunning = false;
            this.mPreAnimEndTimeNano = endTimeNano;
            this.mLastAnimFrameTimeNano = newAnimFrameTime;
            boolean isDropFrame = isDropFrame(startTimeNano, endTimeNano, ANIM_THRESHOLD);
            this.mIsAnimDrop = isDropFrame;
            if (isDropFrame) {
                doEstimationFrameHook(this.mLastAnimFrameTimeNano);
                updateDynaFrameStatus(1, frameTimeNano);
                return;
            }
            return;
        }
        this.mIsPreAnim = false;
    }

    private boolean isDropFrame(long start, long end, double threshold) {
        if (start == -1) {
            return false;
        }
        long dropTimeNanos = (long) (this.mFrameIntervalNanos * threshold);
        if (end - start > dropTimeNanos) {
            return true;
        }
        return false;
    }

    public boolean isPreAnimation() {
        if (!PRE_ANIM_ENABLE) {
            return false;
        }
        return this.mIsPreAnim;
    }

    public void setPreAnimation(boolean isPreAnim) {
        if (!PRE_ANIM_ENABLE) {
            this.mIsPreAnim = false;
        }
        this.mIsPreAnim = isPreAnim;
    }

    private void updateFlingStatus(boolean isFling, long frameTimeNano) {
        if (this.mIsFling != isFling) {
            LogUtil.trace("ScrollingFramePrefetcher#updateFlingStatus, mIsFling = " + this.mIsFling + ", isFling = " + isFling);
            this.mIsFling = isFling;
            updateDynaFrameStatus(2, 0L);
            if (!this.mIsFling) {
                this.mChoreographer = null;
            }
        }
    }

    public void setFling(boolean isFling) {
        updateFlingStatus(isFling, 0L);
    }

    private void updateDynaFrameStatus(int status, long frameTimeNanos) {
        switch (status) {
            case 0:
                firstFrameStatus(frameTimeNanos);
                return;
            case 1:
                dropFrameStatus(frameTimeNanos);
                return;
            case 2:
                resetStatus();
                return;
            default:
                return;
        }
    }

    private void firstFrameStatus(long frameTimeNanos) {
        synchronized (lock) {
            this.mFirstFlingFrameTimeNano = frameTimeNanos;
            this.mIsEstimationFrame = true;
            this.mLastFrameTimeNano = frameTimeNanos;
            this.mLastAnimFrameTimeNano = frameTimeNanos;
            this.mIsPreAnim = false;
            this.mIsAnimDrop = false;
            LogUtil.trace("ScrollingFramePrefetcher#firstFrameStatus, mIsEstimationFrame = " + this.mIsEstimationFrame);
        }
    }

    private void dropFrameStatus(long frameTimeNanos) {
        synchronized (lock) {
            this.mLastFrameTimeNano = frameTimeNanos;
            this.mLastAnimFrameTimeNano = frameTimeNanos;
            this.mIsEstimationFrame = true;
            this.mIsPreAnim = false;
            this.mIsAnimDrop = false;
            LogUtil.trace("ScrollingFramePrefetcher#dropFrameStatus, mFirstFlingFrameTimeNano = " + this.mFirstFlingFrameTimeNano + ", mIsEstimationFrame = " + this.mIsEstimationFrame);
        }
    }

    private void resetStatus() {
        synchronized (lock) {
            this.mFirstFlingFrameTimeNano = -1L;
            this.mLastFrameTimeNano = -1L;
            this.mLastOrigFrameTimeNano = -1L;
            this.mLastAnimFrameTimeNano = -1L;
            this.mFrameEndTimeNano = -1L;
            this.mPreAnimEndTimeNano = -1L;
            this.mIsEstimationFrame = false;
            this.mIsPreAnim = false;
            this.mIsAnimDrop = false;
            LogUtil.trace("ScrollingFramePrefetcher#resetStatus, mFirstFlingFrameTimeNano = " + this.mFirstFlingFrameTimeNano);
        }
    }

    private void updateFrameIntervalNanos() {
        long temp = 1.0E9f / ScrollState.getRefreshRate();
        if (this.mFrameIntervalNanos != temp) {
            this.mFrameIntervalNanos = temp;
        }
    }

    private long correctionFrameTime(long frameTimeNano, long lastFrameTimeNano) {
        long targetFrameTime = this.mFrameIntervalNanos + lastFrameTimeNano;
        long diff = frameTimeNano - targetFrameTime;
        LogUtil.trace("ScrollingFramePrefetcher#correctionFrameTime, frameTimeNano = " + frameTimeNano + ", lastFrameTimeNano = " + lastFrameTimeNano + ", targetFrameTime = " + targetFrameTime + ", diff = " + diff);
        return diff > 0 ? frameTimeNano : targetFrameTime;
    }

    private void doEstimationFrameHook(long frameTimeNano) {
        Choreographer choreographer;
        WeakReference<Choreographer> weakReference = this.mChoreographer;
        if (weakReference == null || (choreographer = weakReference.get()) == null) {
            return;
        }
        if (choreographer.isEmptyCallback()) {
            LogUtil.trace("ScrollingFramePrefetcher#no draw at this time, skip.");
            return;
        }
        choreographer.forceFrameScheduled();
        choreographer.doEstimationFrame(frameTimeNano);
        if (this.mAppRequestVsync) {
            this.mAppRequestVsync = false;
            choreographer.forceFrameScheduled();
        }
    }

    private void doPreAnimationHook(long frameTimeNano) {
        Choreographer choreographer;
        WeakReference<Choreographer> weakReference = this.mChoreographer;
        if (weakReference == null || (choreographer = weakReference.get()) == null) {
            return;
        }
        choreographer.doPreAnimation(frameTimeNano, this.mFrameIntervalNanos);
    }

    public void disableAndLockSFP(boolean locked) {
        LogUtil.traceBegin("resetAndLockSFP");
        mFeatureLocked = locked;
        resetStatusWhenScroll();
        LogUtil.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetStatusWhenScroll() {
        setFling(false);
        setPreAnimation(false);
    }

    public void onRequestNextVsync() {
        this.mAppRequestVsync = true;
    }

    public boolean isPreAnimationRunning() {
        return this.mIsPreAnimationRunning;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRefreshRateChanged(float refreshRate, float frameTimeNanos) {
        if (!Config.isVariableRefreshRateSupported() || !this.mIsFling || !PRE_ANIM_ENABLE || !FEATURE_ENABLE || refreshRate < 58.0f) {
            return;
        }
        LogUtil.traceAndMLogd(TAG, "#onRefreshRateChanged mark NeedScheduleNextFrame whenchange to refreshRate=" + refreshRate);
        this.mNeedScheduleNextFrame = true;
    }

    private void forceScheduleNexFrame() {
        Choreographer choreographer;
        WeakReference<Choreographer> weakReference = this.mChoreographer;
        if (weakReference == null || (choreographer = weakReference.get()) == null) {
            return;
        }
        LogUtil.traceBegin("forceScheduleNexFrame");
        choreographer.forceScheduleNexFrame();
        LogUtil.traceEnd();
    }
}
