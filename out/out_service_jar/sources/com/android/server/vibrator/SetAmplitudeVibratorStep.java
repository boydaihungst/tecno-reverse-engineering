package com.android.server.vibrator;

import android.os.SystemClock;
import android.os.Trace;
import android.os.VibrationEffect;
import android.os.vibrator.StepSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.Slog;
import java.util.Arrays;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class SetAmplitudeVibratorStep extends AbstractVibratorStep {
    private static final int REPEATING_EFFECT_ON_DURATION = 5000;
    private long mNextOffTime;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SetAmplitudeVibratorStep(VibrationStepConductor conductor, long startTime, VibratorController controller, VibrationEffect.Composed effect, int index, long previousStepVibratorOffTimeout) {
        super(conductor, startTime, controller, effect, index, previousStepVibratorOffTimeout);
        this.mNextOffTime = previousStepVibratorOffTimeout;
    }

    @Override // com.android.server.vibrator.AbstractVibratorStep, com.android.server.vibrator.Step
    public boolean acceptVibratorCompleteCallback(int vibratorId) {
        if (this.controller.getVibratorInfo().getId() == vibratorId) {
            this.mVibratorCompleteCallbackReceived = true;
            this.mNextOffTime = SystemClock.uptimeMillis();
        }
        return this.mNextOffTime < this.startTime && this.controller.getCurrentAmplitude() > 0.0f;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [124=5] */
    @Override // com.android.server.vibrator.Step
    public List<Step> play() {
        Trace.traceBegin(8388608L, "SetAmplitudeVibratorStep");
        try {
            long now = SystemClock.uptimeMillis();
            long latency = now - this.startTime;
            if (this.mVibratorCompleteCallbackReceived && latency < 0) {
                this.mNextOffTime = turnVibratorBackOn(-latency);
                return Arrays.asList(new SetAmplitudeVibratorStep(this.conductor, this.startTime, this.controller, this.effect, this.segmentIndex, this.mNextOffTime));
            }
            StepSegment stepSegment = (VibrationEffectSegment) this.effect.getSegments().get(this.segmentIndex);
            if (!(stepSegment instanceof StepSegment)) {
                Slog.w("VibrationThread", "Ignoring wrong segment for a SetAmplitudeVibratorStep: " + stepSegment);
                return skipToNextSteps(1);
            }
            StepSegment stepSegment2 = stepSegment;
            if (stepSegment2.getDuration() == 0) {
                return skipToNextSteps(1);
            }
            float amplitude = stepSegment2.getAmplitude();
            if (amplitude != 0.0f) {
                if (this.startTime >= this.mNextOffTime) {
                    long onDuration = getVibratorOnDuration(this.effect, this.segmentIndex);
                    if (onDuration > 0) {
                        this.mVibratorOnResult = startVibrating(onDuration);
                        this.mNextOffTime = now + onDuration + 1000;
                    }
                }
                changeAmplitude(amplitude);
            } else if (this.previousStepVibratorOffTimeout > now) {
                stopVibrating();
                this.mNextOffTime = now;
            }
            long nextStartTime = this.startTime + stepSegment.getDuration();
            return nextSteps(nextStartTime, this.mNextOffTime, 1);
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private long turnVibratorBackOn(long remainingDuration) {
        long onDuration = getVibratorOnDuration(this.effect, this.segmentIndex);
        if (onDuration <= 0) {
            return this.previousStepVibratorOffTimeout;
        }
        long onDuration2 = onDuration + remainingDuration;
        float expectedAmplitude = this.controller.getCurrentAmplitude();
        this.mVibratorOnResult = startVibrating(onDuration2);
        if (this.mVibratorOnResult > 0) {
            changeAmplitude(expectedAmplitude);
        }
        return SystemClock.uptimeMillis() + onDuration2 + 1000;
    }

    private long startVibrating(long duration) {
        return this.controller.on(duration, getVibration().id);
    }

    private long getVibratorOnDuration(VibrationEffect.Composed effect, int startIndex) {
        List<VibrationEffectSegment> segments = effect.getSegments();
        int segmentCount = segments.size();
        int repeatIndex = effect.getRepeatIndex();
        int i = startIndex;
        long timing = 0;
        while (i < segmentCount) {
            VibrationEffectSegment vibrationEffectSegment = segments.get(i);
            if (!(vibrationEffectSegment instanceof StepSegment) || ((StepSegment) vibrationEffectSegment).getAmplitude() == 0.0f) {
                break;
            }
            timing += vibrationEffectSegment.getDuration();
            i++;
            if (i == segmentCount && repeatIndex >= 0) {
                i = repeatIndex;
                repeatIndex = -1;
                continue;
            }
            if (i == startIndex) {
                return Math.max(timing, 5000L);
            }
        }
        if (i == segmentCount && effect.getRepeatIndex() < 0) {
            return timing + this.conductor.vibrationSettings.getRampDownDuration();
        }
        return timing;
    }
}
