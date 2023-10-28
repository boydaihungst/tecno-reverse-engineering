package com.android.server.vibrator;

import android.os.SystemClock;
import android.os.VibrationEffect;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
abstract class AbstractVibratorStep extends Step {
    public final VibratorController controller;
    public final VibrationEffect.Composed effect;
    boolean mVibratorCompleteCallbackReceived;
    long mVibratorOnResult;
    public final long previousStepVibratorOffTimeout;
    public final int segmentIndex;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AbstractVibratorStep(VibrationStepConductor conductor, long startTime, VibratorController controller, VibrationEffect.Composed effect, int index, long previousStepVibratorOffTimeout) {
        super(conductor, startTime);
        this.controller = controller;
        this.effect = effect;
        this.segmentIndex = index;
        this.previousStepVibratorOffTimeout = previousStepVibratorOffTimeout;
    }

    public int getVibratorId() {
        return this.controller.getVibratorInfo().getId();
    }

    @Override // com.android.server.vibrator.Step
    public long getVibratorOnDuration() {
        return this.mVibratorOnResult;
    }

    @Override // com.android.server.vibrator.Step
    public boolean acceptVibratorCompleteCallback(int vibratorId) {
        boolean isSameVibrator = this.controller.getVibratorInfo().getId() == vibratorId;
        this.mVibratorCompleteCallbackReceived |= isSameVibrator;
        return isSameVibrator && this.previousStepVibratorOffTimeout > SystemClock.uptimeMillis();
    }

    @Override // com.android.server.vibrator.Step
    public List<Step> cancel() {
        return Arrays.asList(new CompleteEffectVibratorStep(this.conductor, SystemClock.uptimeMillis(), true, this.controller, this.previousStepVibratorOffTimeout));
    }

    @Override // com.android.server.vibrator.Step
    public void cancelImmediately() {
        if (this.previousStepVibratorOffTimeout > SystemClock.uptimeMillis()) {
            stopVibrating();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void stopVibrating() {
        this.controller.off();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void changeAmplitude(float amplitude) {
        this.controller.setAmplitude(amplitude);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<Step> skipToNextSteps(int segmentsSkipped) {
        return nextSteps(this.startTime, this.previousStepVibratorOffTimeout, segmentsSkipped);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<Step> nextSteps(int segmentsPlayed) {
        if (this.mVibratorOnResult <= 0) {
            return skipToNextSteps(segmentsPlayed);
        }
        long nextStartTime = SystemClock.uptimeMillis() + this.mVibratorOnResult;
        long nextVibratorOffTimeout = 1000 + nextStartTime;
        return nextSteps(nextStartTime, nextVibratorOffTimeout, segmentsPlayed);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<Step> nextSteps(long nextStartTime, long vibratorOffTimeout, int segmentsPlayed) {
        int nextSegmentIndex = this.segmentIndex + segmentsPlayed;
        int effectSize = this.effect.getSegments().size();
        int repeatIndex = this.effect.getRepeatIndex();
        if (nextSegmentIndex >= effectSize && repeatIndex >= 0) {
            int loopSize = effectSize - repeatIndex;
            nextSegmentIndex = repeatIndex + ((nextSegmentIndex - effectSize) % loopSize);
        }
        Step nextStep = this.conductor.nextVibrateStep(nextStartTime, this.controller, this.effect, nextSegmentIndex, vibratorOffTimeout);
        return nextStep == null ? VibrationStepConductor.EMPTY_STEP_LIST : Arrays.asList(nextStep);
    }
}
